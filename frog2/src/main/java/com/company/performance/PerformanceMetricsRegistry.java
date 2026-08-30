package com.company.performance;

import com.company.performance.RequestPerformanceContext.Operation;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class PerformanceMetricsRegistry {
    private static final SearchMetrics SUMMARY_SEARCH = new SearchMetrics();
    private static final SearchMetrics CONTENT_SEARCH = new SearchMetrics();
    private static final long SLOW_SEARCH_NANOS = TimeUnit.MILLISECONDS.toNanos(
            positiveLongProperty("frog2.performance.slowSearchMs", 500));

    private PerformanceMetricsRegistry() {
    }

    public static void record(
            Operation operation,
            long requestNanos,
            long sqlNanos) {
        SearchMetrics metrics = switch (operation) {
            case TROUBLESHOOTING_SUMMARY_SEARCH -> SUMMARY_SEARCH;
            case TROUBLESHOOTING_CONTENT_SEARCH -> CONTENT_SEARCH;
            default -> null;
        };
        if (metrics != null) {
            metrics.record(requestNanos, sqlNanos);
        }
    }

    public static Snapshot snapshot() {
        return new Snapshot(
                SUMMARY_SEARCH.snapshot(),
                CONTENT_SEARCH.snapshot());
    }

    static void resetForTests() {
        SUMMARY_SEARCH.reset();
        CONTENT_SEARCH.reset();
    }

    public record Snapshot(
            SearchSnapshot summarySearch,
            SearchSnapshot contentSearch) {
    }

    public record SearchSnapshot(
            long count,
            long slowCount,
            long totalRequestNanos,
            long totalSqlNanos,
            long maxRequestNanos,
            long maxSqlNanos) {
        public double averageRequestMillis() {
            return count == 0 ? 0 : millis(totalRequestNanos) / count;
        }

        public double averageSqlMillis() {
            return count == 0 ? 0 : millis(totalSqlNanos) / count;
        }

        public double maxRequestMillis() {
            return millis(maxRequestNanos);
        }

        public double maxSqlMillis() {
            return millis(maxSqlNanos);
        }

        private static double millis(long nanos) {
            return nanos / 1_000_000.0;
        }
    }

    private static final class SearchMetrics {
        private final LongAdder count = new LongAdder();
        private final LongAdder slowCount = new LongAdder();
        private final LongAdder totalRequestNanos = new LongAdder();
        private final LongAdder totalSqlNanos = new LongAdder();
        private final AtomicLong maxRequestNanos = new AtomicLong();
        private final AtomicLong maxSqlNanos = new AtomicLong();

        private void record(long requestNanos, long sqlNanos) {
            long safeRequest = Math.max(0, requestNanos);
            long safeSql = Math.max(0, sqlNanos);
            count.increment();
            totalRequestNanos.add(safeRequest);
            totalSqlNanos.add(safeSql);
            maxRequestNanos.accumulateAndGet(safeRequest, Math::max);
            maxSqlNanos.accumulateAndGet(safeSql, Math::max);
            if (safeRequest >= SLOW_SEARCH_NANOS) {
                slowCount.increment();
            }
        }

        private SearchSnapshot snapshot() {
            return new SearchSnapshot(
                    count.sum(),
                    slowCount.sum(),
                    totalRequestNanos.sum(),
                    totalSqlNanos.sum(),
                    maxRequestNanos.get(),
                    maxSqlNanos.get());
        }

        private void reset() {
            count.reset();
            slowCount.reset();
            totalRequestNanos.reset();
            totalSqlNanos.reset();
            maxRequestNanos.set(0);
            maxSqlNanos.set(0);
        }
    }

    private static long positiveLongProperty(String name, long defaultValue) {
        Long configured = Long.getLong(name);
        return configured == null || configured <= 0 ? defaultValue : configured;
    }
}
