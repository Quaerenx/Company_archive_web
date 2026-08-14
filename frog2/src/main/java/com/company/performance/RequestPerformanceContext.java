package com.company.performance;

public final class RequestPerformanceContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private RequestPerformanceContext() {
    }

    public static void begin() {
        CURRENT.set(new State());
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
    }

    public static void markOperation(Operation operation) {
        State state = CURRENT.get();
        if (state != null && operation != null) {
            state.operation = operation;
        }
    }

    public static void recordFileSnapshotCacheHit() {
        State state = CURRENT.get();
        if (state != null) {
            state.fileSnapshotCacheHits++;
        }
    }

    public static void recordFileSnapshotCacheMiss() {
        State state = CURRENT.get();
        if (state != null) {
            state.fileSnapshotCacheMisses++;
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
    }

    public static Snapshot finish() {
        State state = CURRENT.get();
        CURRENT.remove();
        if (state == null) {
            return new Snapshot(
                    Operation.NONE,
                    0, 0, 0,
                    0, 0, 0,
                    0, 0, 0, 0, 0);
        }
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
                state.maxFileSnapshotScanNanos);
    }

    public enum Operation {
        NONE("none"),
        TROUBLESHOOTING_SUMMARY_SEARCH("troubleshooting.summarySearch"),
        TROUBLESHOOTING_CONTENT_SEARCH("troubleshooting.contentSearch");

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
            long maxFileSnapshotScanNanos) {
    }

    private static final class State {
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
    }
}
