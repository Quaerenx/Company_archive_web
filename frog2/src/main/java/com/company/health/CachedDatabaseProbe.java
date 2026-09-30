package com.company.health;

import com.company.util.DBConnection;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

final class CachedDatabaseProbe implements BooleanSupplier, AutoCloseable {
    private final Supplier<DBConnection.PoolSnapshot> poolSnapshot;
    private final BooleanSupplier validation;
    private final LongSupplier nanoTime;
    private final ExecutorService executor;
    private final long ttlNanos;
    private final long timeoutNanos;
    private CachedResult cached;
    private Attempt inFlight;
    private boolean closed;

    CachedDatabaseProbe() {
        this(DBConnection::getPoolSnapshot, DBConnection::probeDatabase,
                System::nanoTime, Duration.ofSeconds(5), Duration.ofSeconds(1));
    }

    CachedDatabaseProbe(
            Supplier<DBConnection.PoolSnapshot> poolSnapshot,
            BooleanSupplier validation,
            LongSupplier nanoTime,
            Duration ttl,
            Duration timeout) {
        this.poolSnapshot = Objects.requireNonNull(poolSnapshot, "poolSnapshot");
        this.validation = Objects.requireNonNull(validation, "validation");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        ttlNanos = positive(ttl, "ttl");
        timeoutNanos = positive(timeout, "timeout");
        executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "frog2-database-readiness");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public boolean getAsBoolean() {
        Attempt attempt;
        synchronized (this) {
            if (closed) {
                return false;
            }
            long now = nanoTime.getAsLong();
            if (cached != null && now - cached.completedAt() < ttlNanos) {
                return cached.ready();
            }
            if (inFlight == null) {
                DBConnection.PoolSnapshot pool;
                try {
                    pool = poolSnapshot.get();
                } catch (RuntimeException exception) {
                    return false;
                }
                if (pool == null || !pool.available() || pool.idle() <= 0) {
                    return false;
                }
                attempt = new Attempt(now);
                inFlight = attempt;
                try {
                    executor.execute(() -> validate(attempt));
                } catch (RejectedExecutionException exception) {
                    inFlight = null;
                    return false;
                }
            } else {
                attempt = inFlight;
            }
        }

        if (attempt.result.isDone()) {
            return attempt.result.getNow(false);
        }
        long remaining = timeoutNanos
                - (nanoTime.getAsLong() - attempt.startedAt);
        if (remaining <= 0) {
            return timedOut(attempt);
        }
        try {
            return attempt.result.get(remaining, TimeUnit.NANOSECONDS);
        } catch (TimeoutException exception) {
            return timedOut(attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException exception) {
            return false;
        }
    }

    private void validate(Attempt attempt) {
        boolean ready = false;
        try {
            ready = validation.getAsBoolean();
        } catch (RuntimeException exception) {
            ready = false;
        } finally {
            synchronized (this) {
                long completedAt = nanoTime.getAsLong();
                ready = ready && !closed && !attempt.timedOut
                        && completedAt - attempt.startedAt < timeoutNanos;
                if (inFlight == attempt) {
                    cached = new CachedResult(ready, completedAt);
                    inFlight = null;
                }
                attempt.result.complete(ready);
            }
        }
    }

    private synchronized boolean timedOut(Attempt attempt) {
        if (closed) {
            return false;
        }
        if (attempt.result.isDone()) {
            return attempt.result.getNow(false);
        }
        if (inFlight != attempt) {
            return false;
        }
        attempt.timedOut = true;
        cached = new CachedResult(false, nanoTime.getAsLong());
        // Keep the attempt until JDBC returns, so a stalled driver cannot queue more probes.
        return false;
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (inFlight != null) {
            inFlight.result.complete(false);
        }
        executor.shutdownNow();
    }

    private static long positive(Duration duration, String name) {
        long value = Objects.requireNonNull(duration, name).toNanos();
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private record CachedResult(boolean ready, long completedAt) {
    }

    private static final class Attempt {
        private final long startedAt;
        private final CompletableFuture<Boolean> result = new CompletableFuture<>();
        private boolean timedOut;

        private Attempt(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
