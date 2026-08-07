package com.company.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RequestPerformanceContextTest {
    @Test
    void aggregatesSqlTimingAndClearsThreadState() {
        RequestPerformanceContext.begin();
        RequestPerformanceContext.recordSql(2_000_000);
        RequestPerformanceContext.recordSql(5_000_000);

        RequestPerformanceContext.Snapshot snapshot =
                RequestPerformanceContext.finish();

        assertEquals(2, snapshot.sqlCount());
        assertEquals(7_000_000, snapshot.sqlDurationNanos());
        assertEquals(5_000_000, snapshot.maxSqlNanos());
        assertEquals(0, RequestPerformanceContext.finish().sqlCount());
    }
}
