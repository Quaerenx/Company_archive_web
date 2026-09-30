package com.company.listener;

import com.company.listener.AppLifecycleListener.SchemaStatus;
import com.company.model.DataAccessException;
import com.company.model.DatabaseSchemaReadiness;
import jakarta.servlet.ServletContext;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

final class SchemaReadinessRecovery implements AutoCloseable {
    private final ServletContext context;
    private final Supplier<DatabaseSchemaReadiness.Report> inspection;
    private final ScheduledExecutorService scheduler;
    private final long maximumDelayNanos;
    private long delayNanos;
    private boolean closed;

    SchemaReadinessRecovery(
            ServletContext context,
            Supplier<DatabaseSchemaReadiness.Report> inspection) {
        this(context, inspection, Duration.ofSeconds(5), Duration.ofSeconds(60));
    }

    SchemaReadinessRecovery(
            ServletContext context,
            Supplier<DatabaseSchemaReadiness.Report> inspection,
            Duration initialDelay,
            Duration maximumDelay) {
        this.context = Objects.requireNonNull(context, "context");
        this.inspection = Objects.requireNonNull(inspection, "inspection");
        delayNanos = initialDelay.toNanos();
        maximumDelayNanos = maximumDelay.toNanos();
        if (delayNanos <= 0 || maximumDelayNanos < delayNanos) {
            throw new IllegalArgumentException("Invalid schema recovery delays");
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "frog2-schema-readiness-recovery");
            thread.setDaemon(true);
            return thread;
        });
    }

    synchronized void start() {
        if (!closed && unavailable()) {
            scheduler.schedule(this::recover, delayNanos, TimeUnit.NANOSECONDS);
        }
    }

    private void recover() {
        synchronized (this) {
            if (closed || !unavailable()) {
                return;
            }
        }
        DatabaseSchemaReadiness.Report report;
        DataAccessException failure = null;
        try {
            report = inspection.get();
        } catch (DataAccessException exception) {
            report = null;
            failure = exception;
        }
        DatabaseSchemaReadiness.Report inspected = report;
        DataAccessException inspectionFailure = failure;
        synchronized (this) {
            if (closed) {
                return;
            }
            AppLifecycleListener.publishSchemaReadiness(context, () -> {
                if (inspectionFailure != null) {
                    throw inspectionFailure;
                }
                return inspected;
            });
            if (unavailable()) {
                delayNanos = Math.min(maximumDelayNanos, delayNanos * 2);
                scheduler.schedule(this::recover, delayNanos, TimeUnit.NANOSECONDS);
            } else {
                scheduler.shutdown();
            }
        }
    }

    private boolean unavailable() {
        return context.getAttribute(AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE)
                == SchemaStatus.UNAVAILABLE;
    }

    @Override
    public synchronized void close() {
        closed = true;
        scheduler.shutdownNow();
    }
}
