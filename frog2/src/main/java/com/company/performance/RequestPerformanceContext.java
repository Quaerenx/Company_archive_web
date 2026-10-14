package com.company.performance;

import java.util.Objects;

public final class RequestPerformanceContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private RequestPerformanceContext() {
    }

    public static void begin() {
        CURRENT.set(new State(null));
    }

    public static void begin(Capture capture) {
        CURRENT.set(new State(Objects.requireNonNull(capture, "capture")));
    }

    public static void recordSql(long elapsedNanos) {
        State state = CURRENT.get();
        if (state == null) {
            return;
        }
        long safeElapsed = Math.max(0, elapsedNanos);
        state.sqlCount++;
        state.sqlDurationNanos += safeElapsed;
        state.maxSqlNanos = Math.max(state.maxSqlNanos, safeElapsed);
        publish(state);
    }

    public static void recordMetadata(long elapsedNanos) {
        State state = CURRENT.get();
        if (state != null) {
            state.metadataCount++;
            state.metadataDurationNanos += Math.max(0, elapsedNanos);
            state.maxMetadataNanos = Math.max(state.maxMetadataNanos, Math.max(0, elapsedNanos));
            publish(state);
        }
    }

    public static void recordDbAcquisition(long elapsedNanos) {
        State state = CURRENT.get();
        if (state == null) {
            return;
        }
        long safeElapsed = Math.max(0, elapsedNanos);
        state.dbAcquisitionCount++;
        state.dbAcquisitionDurationNanos += safeElapsed;
        state.maxDbAcquisitionNanos = Math.max(
                state.maxDbAcquisitionNanos, safeElapsed);
        publish(state);
    }

    public static void markOperation(Operation operation) {
        State state = CURRENT.get();
        if (state != null && operation != null) {
            state.operation = operation;
            publish(state);
        }
    }

    public static void recordDataLoad(long elapsedNanos) {
        State state = CURRENT.get();
        if (state != null) {
            state.dataLoadCount++;
            state.dataLoadDurationNanos += Math.max(0, elapsedNanos);
            publish(state);
        }
    }

    public static void recordViewRender(long elapsedNanos) {
        State state = CURRENT.get();
        if (state != null) {
            state.viewRenderCount++;
            state.viewRenderDurationNanos += Math.max(0, elapsedNanos);
            publish(state);
        }
    }

    public static void recordFileSnapshotCacheHit() {
        State state = CURRENT.get();
        if (state != null) {
            state.fileSnapshotCacheHits++;
            publish(state);
        }
    }

    public static void recordFileSnapshotCacheMiss() {
        State state = CURRENT.get();
        if (state != null) {
            state.fileSnapshotCacheMisses++;
            publish(state);
        }
    }

    public static void recordFileSnapshotScan(long elapsedNanos) {
        State state = CURRENT.get();
        if (state == null) {
            return;
        }
        long safeElapsed = Math.max(0, elapsedNanos);
        state.fileSnapshotScanCount++;
        state.fileSnapshotScanDurationNanos += safeElapsed;
        state.maxFileSnapshotScanNanos = Math.max(
                state.maxFileSnapshotScanNanos, safeElapsed);
        publish(state);
    }

    public static void recordCustomerHistoryScan(
            int recordFileCount, long elapsedNanos) {
        State state = CURRENT.get();
        if (state == null) {
            return;
        }
        long safeElapsed = Math.max(0, elapsedNanos);
        state.customerHistoryScanCount++;
        state.customerHistoryRecordFileCount += Math.max(0, recordFileCount);
        state.customerHistoryScanDurationNanos += safeElapsed;
        state.maxCustomerHistoryScanNanos = Math.max(
                state.maxCustomerHistoryScanNanos, safeElapsed);
        publish(state);
    }

    public static void recordCustomerHistoryCacheHit() {
        State state = CURRENT.get();
        if (state != null) {
            state.customerHistoryCacheHits++;
            publish(state);
        }
    }

    public static void recordCustomerHistoryCacheMiss() {
        State state = CURRENT.get();
        if (state != null) {
            state.customerHistoryCacheMisses++;
            publish(state);
        }
    }

    public static Snapshot finish() {
        State state = CURRENT.get();
        CURRENT.remove();
        if (state == null) {
            return new Snapshot(
                    Operation.NONE,
                    0, 0, 0,
                    0, 0, 0,
                    0, 0, 0, 0, 0,
                    0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0);
        }
        Snapshot snapshot = snapshot(state);
        if (state.capture != null) {
            state.capture.snapshot = snapshot;
        }
        return snapshot;
    }

    public static void merge(Snapshot snapshot) {
        State state = CURRENT.get();
        if (state == null || snapshot == null) {
            return;
        }
        state.sqlCount += snapshot.sqlCount();
        state.sqlDurationNanos += snapshot.sqlDurationNanos();
        state.maxSqlNanos = Math.max(state.maxSqlNanos, snapshot.maxSqlNanos());
        state.dbAcquisitionCount += snapshot.dbAcquisitionCount();
        state.dbAcquisitionDurationNanos += snapshot.dbAcquisitionDurationNanos();
        state.maxDbAcquisitionNanos = Math.max(
                state.maxDbAcquisitionNanos, snapshot.maxDbAcquisitionNanos());
        state.fileSnapshotCacheHits += snapshot.fileSnapshotCacheHits();
        state.fileSnapshotCacheMisses += snapshot.fileSnapshotCacheMisses();
        state.fileSnapshotScanCount += snapshot.fileSnapshotScanCount();
        state.fileSnapshotScanDurationNanos += snapshot.fileSnapshotScanDurationNanos();
        state.maxFileSnapshotScanNanos = Math.max(
                state.maxFileSnapshotScanNanos, snapshot.maxFileSnapshotScanNanos());
        state.customerHistoryCacheHits += snapshot.customerHistoryCacheHits();
        state.customerHistoryCacheMisses += snapshot.customerHistoryCacheMisses();
        state.customerHistoryScanCount += snapshot.customerHistoryScanCount();
        state.customerHistoryRecordFileCount += snapshot.customerHistoryRecordFileCount();
        state.customerHistoryScanDurationNanos += snapshot.customerHistoryScanDurationNanos();
        state.maxCustomerHistoryScanNanos = Math.max(
                state.maxCustomerHistoryScanNanos, snapshot.maxCustomerHistoryScanNanos());
        state.dataLoadCount += snapshot.dataLoadCount();
        state.dataLoadDurationNanos += snapshot.dataLoadDurationNanos();
        state.viewRenderCount += snapshot.viewRenderCount();
        state.viewRenderDurationNanos += snapshot.viewRenderDurationNanos();
        state.metadataCount += snapshot.metadataCount();
        state.metadataDurationNanos += snapshot.metadataDurationNanos();
        state.maxMetadataNanos = Math.max(state.maxMetadataNanos, snapshot.maxMetadataNanos());
        publish(state);
    }

    private static void publish(State state) {
        if (state.capture != null) {
            state.capture.snapshot = snapshot(state);
        }
    }

    private static Snapshot snapshot(State state) {
        return new Snapshot(
                state.operation,
                state.sqlCount,
                state.sqlDurationNanos,
                state.maxSqlNanos,
                state.dbAcquisitionCount,
                state.dbAcquisitionDurationNanos,
                state.maxDbAcquisitionNanos,
                state.fileSnapshotCacheHits,
                state.fileSnapshotCacheMisses,
                state.fileSnapshotScanCount,
                state.fileSnapshotScanDurationNanos,
                state.maxFileSnapshotScanNanos,
                state.customerHistoryCacheHits,
                state.customerHistoryCacheMisses,
                state.customerHistoryScanCount,
                state.customerHistoryRecordFileCount,
                state.customerHistoryScanDurationNanos,
                state.maxCustomerHistoryScanNanos,
                state.dataLoadCount,
                state.dataLoadDurationNanos,
                state.viewRenderCount,
                state.viewRenderDurationNanos,
                state.metadataCount,
                state.metadataDurationNanos,
                state.maxMetadataNanos);
    }

    public static final class Capture {
        private volatile Snapshot snapshot;

        public Snapshot snapshot() {
            return snapshot;
        }
    }

    public enum Operation {
        NONE("none"),
        GLOBAL_SEARCH("globalSearch"),
        TROUBLESHOOTING_SUMMARY_SEARCH("troubleshooting.summarySearch"),
        TROUBLESHOOTING_CONTENT_SEARCH("troubleshooting.contentSearch"),
        CUSTOMER_HISTORY_LIST("customerHistory.list"),
        DASHBOARD_VIEW("page.dashboard"),
        CUSTOMERS_LIST("page.customers"),
        MYPAGE_OVERVIEW("page.mypage");

        private final String logValue;

        Operation(String logValue) {
            this.logValue = logValue;
        }

        public String logValue() {
            return logValue;
        }
    }

    public record Snapshot(
            Operation operation,
            int sqlCount,
            long sqlDurationNanos,
            long maxSqlNanos,
            int dbAcquisitionCount,
            long dbAcquisitionDurationNanos,
            long maxDbAcquisitionNanos,
            int fileSnapshotCacheHits,
            int fileSnapshotCacheMisses,
            int fileSnapshotScanCount,
            long fileSnapshotScanDurationNanos,
            long maxFileSnapshotScanNanos,
            int customerHistoryCacheHits,
            int customerHistoryCacheMisses,
            int customerHistoryScanCount,
            int customerHistoryRecordFileCount,
            long customerHistoryScanDurationNanos,
            long maxCustomerHistoryScanNanos,
            int dataLoadCount,
            long dataLoadDurationNanos,
            int viewRenderCount,
            long viewRenderDurationNanos,
            int metadataCount,
            long metadataDurationNanos,
            long maxMetadataNanos) {
    }

    private static final class State {
        private final Capture capture;
        private Operation operation = Operation.NONE;
        private int sqlCount;
        private long sqlDurationNanos;
        private long maxSqlNanos;
        private int dbAcquisitionCount;
        private long dbAcquisitionDurationNanos;
        private long maxDbAcquisitionNanos;
        private int fileSnapshotCacheHits;
        private int fileSnapshotCacheMisses;
        private int fileSnapshotScanCount;
        private long fileSnapshotScanDurationNanos;
        private long maxFileSnapshotScanNanos;
        private int customerHistoryCacheHits;
        private int customerHistoryCacheMisses;
        private int customerHistoryScanCount;
        private int customerHistoryRecordFileCount;
        private long customerHistoryScanDurationNanos;
        private long maxCustomerHistoryScanNanos;
        private int dataLoadCount;
        private long dataLoadDurationNanos;
        private int viewRenderCount;
        private long viewRenderDurationNanos;
        private int metadataCount;
        private long metadataDurationNanos;
        private long maxMetadataNanos;

        private State(Capture capture) {
            this.capture = capture;
        }
    }
}
