package com.company.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.util.DBConnection;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CachedDatabaseProbeTest {
    private static final DBConnection.PoolSnapshot IDLE_POOL =
            new DBConnection.PoolSnapshot(true, 0, 2, 2, 0);
    private static final DBConnection.PoolSnapshot BUSY_POOL =
            new DBConnection.PoolSnapshot(true, 2, 0, 2, 1);
    private static final Duration TTL = Duration.ofSeconds(5);

    @Test
    void successfulValidationIsReusedUntilTtlExpires() {
        AtomicLong now = new AtomicLong();
        AtomicInteger validations = new AtomicInteger();
        try (CachedDatabaseProbe probe = probe(() -> {
            validations.incrementAndGet();
            return true;
        }, now)) {
            assertTrue(probe.getAsBoolean());
            assertTrue(probe.getAsBoolean());
            assertEquals(1, validations.get());
            now.addAndGet(TTL.toNanos());
            assertTrue(probe.getAsBoolean());
            assertEquals(2, validations.get());
        }
    }

    @Test
    void healthyPoolCountsDoNotHideFailedValidationAndRecovery() {
        AtomicLong now = new AtomicLong();
        AtomicBoolean databaseAvailable = new AtomicBoolean(false);
        AtomicInteger validations = new AtomicInteger();
        try (CachedDatabaseProbe probe = probe(() -> {
            validations.incrementAndGet();
            return databaseAvailable.get();
        }, now)) {
            assertFalse(probe.getAsBoolean());
            databaseAvailable.set(true);
            assertFalse(probe.getAsBoolean());
            assertEquals(1, validations.get());
            now.addAndGet(TTL.toNanos());
            assertTrue(probe.getAsBoolean());
            assertEquals(2, validations.get());
        }
    }

    @Test
    void exhaustedPoolDoesNotStartJdbcAndCanRecoverImmediately() {
        AtomicReference<DBConnection.PoolSnapshot> pool = new AtomicReference<>(BUSY_POOL);
        AtomicInteger validations = new AtomicInteger();
        try (CachedDatabaseProbe probe = new CachedDatabaseProbe(
                pool::get, () -> {
                    validations.incrementAndGet();
                    return true;
                }, System::nanoTime, TTL, Duration.ofSeconds(1))) {
            assertFalse(probe.getAsBoolean());
            assertEquals(0, validations.get());
            pool.set(IDLE_POOL);
            assertTrue(probe.getAsBoolean());
            assertEquals(1, validations.get());
        }
    }

    @Test
    void transientNoIdleConnectionDoesNotDiscardRecentSuccess() {
        AtomicLong now = new AtomicLong();
        AtomicReference<DBConnection.PoolSnapshot> pool = new AtomicReference<>(IDLE_POOL);
        AtomicInteger validations = new AtomicInteger();
        try (CachedDatabaseProbe probe = new CachedDatabaseProbe(
                pool::get, () -> {
                    validations.incrementAndGet();
                    return true;
                }, now::get, TTL, Duration.ofSeconds(1))) {
            assertTrue(probe.getAsBoolean());
            pool.set(BUSY_POOL);
            assertTrue(probe.getAsBoolean());
            now.addAndGet(TTL.toNanos());
            assertFalse(probe.getAsBoolean());
            assertEquals(1, validations.get());
        }
    }

    @Test
    void stalledValidationHasBoundedResponseAndDoesNotAccumulateWork() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger validations = new AtomicInteger();
        AtomicLong now = new AtomicLong();
        try (CachedDatabaseProbe probe = new CachedDatabaseProbe(
                () -> IDLE_POOL, () -> {
                    validations.incrementAndGet();
                    started.countDown();
                    return await(release);
                }, now::get, Duration.ofMillis(1), Duration.ofMillis(30))) {
            long before = System.nanoTime();
            assertFalse(probe.getAsBoolean());
            assertTrue(Duration.ofNanos(System.nanoTime() - before).toMillis() < 1000);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            now.addAndGet(Duration.ofSeconds(2).toNanos());
            assertFalse(probe.getAsBoolean());
            assertFalse(probe.getAsBoolean());
            assertEquals(1, validations.get());
            release.countDown();
        } finally {
            release.countDown();
        }
    }

    @Test
    void concurrentColdRequestsShareOneValidation() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger validations = new AtomicInteger();
        var callers = Executors.newFixedThreadPool(2);
        try (CachedDatabaseProbe probe = new CachedDatabaseProbe(
                () -> IDLE_POOL, () -> {
                    validations.incrementAndGet();
                    started.countDown();
                    return await(release);
                }, System::nanoTime, TTL, Duration.ofSeconds(2))) {
            var first = callers.submit(probe::getAsBoolean);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            var second = callers.submit(probe::getAsBoolean);
            release.countDown();
            assertTrue(first.get(1, TimeUnit.SECONDS));
            assertTrue(second.get(1, TimeUnit.SECONDS));
            assertEquals(1, validations.get());
        } finally {
            release.countDown();
            callers.shutdownNow();
            assertTrue(callers.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void validationExceptionFailsClosed() {
        try (CachedDatabaseProbe probe = probe(() -> {
            throw new IllegalStateException("unavailable");
        }, new AtomicLong())) {
            assertFalse(probe.getAsBoolean());
        }
    }

    @Test
    void delayedTimeoutPreservesCompletedValidationAndRespectsClosure() throws Exception {
        assertDelayedCallerResult(false, false);
        assertDelayedCallerResult(false, true);
    }

    @Test
    void delayedTimeoutDoesNotOverwriteNewerSuccessfulValidation() throws Exception {
        assertDelayedCallerResult(true, false);
    }

    @Test
    void closingInterruptsPendingWorkerAndRejectsNewProbes() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        var caller = Executors.newSingleThreadExecutor();
        CachedDatabaseProbe probe = new CachedDatabaseProbe(
                () -> IDLE_POOL, () -> {
                    started.countDown();
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException exception) {
                        interrupted.countDown();
                        Thread.currentThread().interrupt();
                    }
                    return true;
                }, System::nanoTime, TTL, Duration.ofSeconds(2));
        try {
            var pending = caller.submit(probe::getAsBoolean);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            probe.close();
            assertFalse(pending.get(1, TimeUnit.SECONDS));
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
            assertFalse(probe.getAsBoolean());
        } finally {
            probe.close();
            caller.shutdownNow();
            assertTrue(caller.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    private static CachedDatabaseProbe probe(
            java.util.function.BooleanSupplier validation, AtomicLong now) {
        return new CachedDatabaseProbe(
                () -> IDLE_POOL, validation, now::get, TTL, Duration.ofSeconds(1));
    }

    private static void assertDelayedCallerResult(
            boolean publishSecondSuccess,
            boolean closeBeforeCallerResumes) throws Exception {
        AtomicLong now = new AtomicLong();
        AtomicInteger validations = new AtomicInteger();
        AtomicInteger callerClockReads = new AtomicInteger();
        AtomicReference<Thread> delayedCaller = new AtomicReference<>();
        CountDownLatch callerAtBudgetCheck = new CountDownLatch(1);
        CountDownLatch releaseCaller = new CountDownLatch(1);
        CountDownLatch releaseValidation = new CountDownLatch(1);
        var callers = Executors.newSingleThreadExecutor();
        try (CachedDatabaseProbe probe = new CachedDatabaseProbe(
                () -> IDLE_POOL, () -> {
                    validations.incrementAndGet();
                    return await(releaseValidation);
                }, () -> {
                    if (Thread.currentThread() == delayedCaller.get()
                            && callerClockReads.incrementAndGet() == 2) {
                        callerAtBudgetCheck.countDown();
                        assertTrue(await(releaseCaller));
                    }
                    return now.get();
                }, TTL, Duration.ofSeconds(1))) {
            var pending = callers.submit(() -> {
                delayedCaller.set(Thread.currentThread());
                return probe.getAsBoolean();
            });
            assertTrue(callerAtBudgetCheck.await(1, TimeUnit.SECONDS));
            now.set(Duration.ofMillis(10).toNanos());
            releaseValidation.countDown();
            assertTrue(probe.getAsBoolean());

            if (publishSecondSuccess) {
                now.set(Duration.ofSeconds(6).toNanos());
                assertTrue(probe.getAsBoolean());
                assertEquals(2, validations.get());
            }
            if (closeBeforeCallerResumes) {
                probe.close();
            }
            now.set(Duration.ofSeconds(publishSecondSuccess ? 7 : 2).toNanos());
            releaseCaller.countDown();

            assertEquals(!closeBeforeCallerResumes, pending.get(1, TimeUnit.SECONDS));
            assertEquals(!closeBeforeCallerResumes, probe.getAsBoolean());
            assertEquals(publishSecondSuccess ? 2 : 1, validations.get());
        } finally {
            releaseValidation.countDown();
            releaseCaller.countDown();
            callers.shutdownNow();
            assertTrue(callers.awaitTermination(1, TimeUnit.SECONDS));
        }
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
