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

    public static Snapshot finish() {
        State state = CURRENT.get();
        CURRENT.remove();
        if (state == null) {
            return new Snapshot(0, 0, 0);
        }
        return new Snapshot(
                state.sqlCount,
                state.sqlDurationNanos,
                state.maxSqlNanos);
    }

    public record Snapshot(
            int sqlCount,
            long sqlDurationNanos,
            long maxSqlNanos) {
    }

    private static final class State {
        private int sqlCount;
        private long sqlDurationNanos;
        private long maxSqlNanos;
    }
}
