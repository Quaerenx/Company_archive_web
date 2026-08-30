package com.company.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.performance.RequestPerformanceContext.Operation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PerformanceMetricsRegistryTest {
    @AfterEach
    void resetMetrics() {
        PerformanceMetricsRegistry.resetForTests();
    }

    @Test
    void summaryAndContentSearchesAreAggregatedSeparately() {
        PerformanceMetricsRegistry.record(
                Operation.TROUBLESHOOTING_SUMMARY_SEARCH,
                100_000_000,
                40_000_000);
        PerformanceMetricsRegistry.record(
                Operation.TROUBLESHOOTING_SUMMARY_SEARCH,
                300_000_000,
                80_000_000);
        PerformanceMetricsRegistry.record(
                Operation.TROUBLESHOOTING_CONTENT_SEARCH,
                700_000_000,
                600_000_000);
        PerformanceMetricsRegistry.record(
                Operation.NONE,
                900_000_000,
                900_000_000);

        PerformanceMetricsRegistry.Snapshot snapshot =
                PerformanceMetricsRegistry.snapshot();

        assertEquals(2, snapshot.summarySearch().count());
        assertEquals(200.0, snapshot.summarySearch().averageRequestMillis());
        assertEquals(60.0, snapshot.summarySearch().averageSqlMillis());
        assertEquals(300.0, snapshot.summarySearch().maxRequestMillis());
        assertEquals(1, snapshot.contentSearch().count());
        assertEquals(1, snapshot.contentSearch().slowCount());
        assertEquals(600.0, snapshot.contentSearch().maxSqlMillis());
    }
}
