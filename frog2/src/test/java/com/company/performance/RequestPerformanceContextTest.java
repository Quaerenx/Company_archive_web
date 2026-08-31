package com.company.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RequestPerformanceContextTest {
    @Test
    void aggregatesSqlTimingAndClearsThreadState() {
        RequestPerformanceContext.begin();
        RequestPerformanceContext.markOperation(
                RequestPerformanceContext.Operation.TROUBLESHOOTING_CONTENT_SEARCH);
        RequestPerformanceContext.recordSql(2_000_000);
        RequestPerformanceContext.recordSql(5_000_000);
        RequestPerformanceContext.recordDbAcquisition(3_000_000);
        RequestPerformanceContext.recordFileSnapshotCacheMiss();
        RequestPerformanceContext.recordFileSnapshotCacheHit();
        RequestPerformanceContext.recordFileSnapshotScan(4_000_000);
        RequestPerformanceContext.recordCustomerHistoryCacheMiss();
        RequestPerformanceContext.recordCustomerHistoryCacheHit();
        RequestPerformanceContext.recordCustomerHistoryScan(92, 6_000_000);
        RequestPerformanceContext.recordDataLoad(11_000_000);
        RequestPerformanceContext.recordViewRender(13_000_000);

        RequestPerformanceContext.Snapshot snapshot =
                RequestPerformanceContext.finish();

        assertEquals(2, snapshot.sqlCount());
        assertEquals(7_000_000, snapshot.sqlDurationNanos());
        assertEquals(5_000_000, snapshot.maxSqlNanos());
        assertEquals(
                RequestPerformanceContext.Operation.TROUBLESHOOTING_CONTENT_SEARCH,
                snapshot.operation());
        assertEquals(1, snapshot.dbAcquisitionCount());
        assertEquals(3_000_000, snapshot.dbAcquisitionDurationNanos());
        assertEquals(3_000_000, snapshot.maxDbAcquisitionNanos());
        assertEquals(1, snapshot.fileSnapshotCacheHits());
        assertEquals(1, snapshot.fileSnapshotCacheMisses());
        assertEquals(1, snapshot.fileSnapshotScanCount());
        assertEquals(4_000_000, snapshot.fileSnapshotScanDurationNanos());
        assertEquals(4_000_000, snapshot.maxFileSnapshotScanNanos());
        assertEquals(1, snapshot.customerHistoryCacheHits());
        assertEquals(1, snapshot.customerHistoryCacheMisses());
        assertEquals(1, snapshot.customerHistoryScanCount());
        assertEquals(92, snapshot.customerHistoryRecordFileCount());
        assertEquals(6_000_000, snapshot.customerHistoryScanDurationNanos());
        assertEquals(6_000_000, snapshot.maxCustomerHistoryScanNanos());
        assertEquals(1, snapshot.dataLoadCount());
        assertEquals(11_000_000, snapshot.dataLoadDurationNanos());
        assertEquals(1, snapshot.viewRenderCount());
        assertEquals(13_000_000, snapshot.viewRenderDurationNanos());
        RequestPerformanceContext.Snapshot cleared =
                RequestPerformanceContext.finish();
        assertEquals(0, cleared.sqlCount());
        assertEquals(
                RequestPerformanceContext.Operation.NONE,
                cleared.operation());
    }
}
