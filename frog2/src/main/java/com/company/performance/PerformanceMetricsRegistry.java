package com.company.performance;

import com.company.performance.RequestPerformanceContext.Operation;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class PerformanceMetricsRegistry {
    private static final SearchMetrics SUMMARY_SEARCH = new SearchMetrics();
    private static final SearchMetrics CONTENT_SEARCH = new SearchMetrics();
    private static final SearchMetrics GLOBAL_SEARCH = new SearchMetrics();
    private static final PageMetrics DASHBOARD_PAGE = new PageMetrics();
    private static final PageMetrics CUSTOMERS_PAGE = new PageMetrics();
    private static final PageMetrics MYPAGE_PAGE = new PageMetrics();
    private static final long SLOW_SEARCH_NANOS = TimeUnit.MILLISECONDS.toNanos(
            positiveLongProperty("frog2.performance.slowSearchMs", 500));
    private static final long SLOW_PAGE_NANOS = TimeUnit.MILLISECONDS.toNanos(
            positiveLongProperty("frog2.performance.slowPageMs", 500));

    private PerformanceMetricsRegistry() {
    }

    public static void record(
            Operation operation,
            long requestNanos,
            long sqlNanos) {
        record(operation, requestNanos, sqlNanos, 0, 0);
    }

    public static void record(
            Operation operation,
            long requestNanos,
            long sqlNanos,
            long dataLoadNanos,
            long viewRenderNanos) {
        SearchMetrics metrics = switch (operation) {
            case GLOBAL_SEARCH -> GLOBAL_SEARCH;
            case TROUBLESHOOTING_SUMMARY_SEARCH -> SUMMARY_SEARCH;
            case TROUBLESHOOTING_CONTENT_SEARCH -> CONTENT_SEARCH;
            default -> null;
        };
        if (metrics != null) {
            metrics.record(requestNanos, sqlNanos);
        }
        PageMetrics pageMetrics = switch (operation) {
            case DASHBOARD_VIEW -> DASHBOARD_PAGE;
            case CUSTOMERS_LIST -> CUSTOMERS_PAGE;
            case MYPAGE_OVERVIEW -> MYPAGE_PAGE;
            default -> null;
        };
        if (pageMetrics != null) {
            pageMetrics.record(
                    requestNanos,
                    sqlNanos,
                    dataLoadNanos,
                    viewRenderNanos);
        }
    }

    public static Snapshot snapshot() {
        return new Snapshot(
                SUMMARY_SEARCH.snapshot(),
                CONTENT_SEARCH.snapshot(),
                GLOBAL_SEARCH.snapshot(),
                DASHBOARD_PAGE.snapshot(),
                CUSTOMERS_PAGE.snapshot(),
                MYPAGE_PAGE.snapshot());
    }

    static void resetForTests() {
        SUMMARY_SEARCH.reset();
        CONTENT_SEARCH.reset();
        GLOBAL_SEARCH.reset();
        DASHBOARD_PAGE.reset();
        CUSTOMERS_PAGE.reset();
        MYPAGE_PAGE.reset();
    }

    public record Snapshot(
            SearchSnapshot summarySearch,
            SearchSnapshot contentSearch,
            SearchSnapshot globalSearch,
            PageSnapshot dashboardPage,
            PageSnapshot customersPage,
            PageSnapshot myPage) {
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

    public record PageSnapshot(
            long count,
            long slowCount,
            long totalRequestNanos,
            long totalSqlNanos,
            long totalDataLoadNanos,
            long totalViewRenderNanos,
            long maxRequestNanos) {
        public double averageRequestMillis() {
            return count == 0 ? 0 : millis(totalRequestNanos) / count;
        }

        public double averageSqlMillis() {
            return count == 0 ? 0 : millis(totalSqlNanos) / count;
        }

        public double averageDataLoadMillis() {
            return count == 0 ? 0 : millis(totalDataLoadNanos) / count;
        }

        public double averageViewRenderMillis() {
            return count == 0 ? 0 : millis(totalViewRenderNanos) / count;
        }

        public double averageUnattributedMillis() {
            if (count == 0) {
                return 0;
            }
            long attributed = totalDataLoadNanos + totalViewRenderNanos;
            return millis(Math.max(0, totalRequestNanos - attributed)) / count;
        }

        public double maxRequestMillis() {
            return millis(maxRequestNanos);
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

    private static final class PageMetrics {
        private final LongAdder count = new LongAdder();
        private final LongAdder slowCount = new LongAdder();
        private final LongAdder totalRequestNanos = new LongAdder();
        private final LongAdder totalSqlNanos = new LongAdder();
        private final LongAdder totalDataLoadNanos = new LongAdder();
        private final LongAdder totalViewRenderNanos = new LongAdder();
        private final AtomicLong maxRequestNanos = new AtomicLong();

        private void record(
                long requestNanos,
                long sqlNanos,
                long dataLoadNanos,
                long viewRenderNanos) {
            long safeRequest = Math.max(0, requestNanos);
            count.increment();
            totalRequestNanos.add(safeRequest);
            totalSqlNanos.add(Math.max(0, sqlNanos));
            totalDataLoadNanos.add(Math.max(0, dataLoadNanos));
            totalViewRenderNanos.add(Math.max(0, viewRenderNanos));
            maxRequestNanos.accumulateAndGet(safeRequest, Math::max);
            if (safeRequest >= SLOW_PAGE_NANOS) {
                slowCount.increment();
            }
        }

        private PageSnapshot snapshot() {
            return new PageSnapshot(
                    count.sum(),
                    slowCount.sum(),
                    totalRequestNanos.sum(),
                    totalSqlNanos.sum(),
                    totalDataLoadNanos.sum(),
                    totalViewRenderNanos.sum(),
                    maxRequestNanos.get());
        }

        private void reset() {
            count.reset();
            slowCount.reset();
            totalRequestNanos.reset();
            totalSqlNanos.reset();
            totalDataLoadNanos.reset();
            totalViewRenderNanos.reset();
            maxRequestNanos.set(0);
        }
    }

    private static long positiveLongProperty(String name, long defaultValue) {
        Long configured = Long.getLong(name);
        return configured == null || configured <= 0 ? defaultValue : configured;
    }
}
