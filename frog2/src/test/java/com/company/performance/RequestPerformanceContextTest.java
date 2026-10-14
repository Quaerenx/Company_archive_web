package com.company.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RequestPerformanceContextTest {
    @Test
    void mergesAllWorkerCountersAndMaximaWithoutReplacingTheRequestOperation()
            throws Exception {
        RequestPerformanceContext.Capture capture = new RequestPerformanceContext.Capture();
        RequestPerformanceContext.begin();
        RequestPerformanceContext.markOperation(RequestPerformanceContext.Operation.GLOBAL_SEARCH);
        recordEveryMetric(1);
        try (ExecutorService worker = Executors.newSingleThreadExecutor()) {
            worker.submit(() -> {
                RequestPerformanceContext.begin(capture);
                RequestPerformanceContext.markOperation(RequestPerformanceContext.Operation.MYPAGE_OVERVIEW);
                recordEveryMetric(2);
                RequestPerformanceContext.finish();
                assertEquals(0, RequestPerformanceContext.finish().sqlCount());
            }).get(1, TimeUnit.SECONDS);
            RequestPerformanceContext.merge(capture.snapshot());
            RequestPerformanceContext.Snapshot merged = RequestPerformanceContext.finish();
            assertEquals(RequestPerformanceContext.Operation.GLOBAL_SEARCH, merged.operation());
            assertEquals(2, merged.sqlCount());
            assertEquals(3, merged.sqlDurationNanos());
            assertEquals(2, merged.maxSqlNanos());
            assertEquals(2, merged.dbAcquisitionCount());
            assertEquals(3, merged.dbAcquisitionDurationNanos());
            assertEquals(2, merged.maxDbAcquisitionNanos());
            assertEquals(2, merged.fileSnapshotCacheHits());
            assertEquals(2, merged.fileSnapshotCacheMisses());
            assertEquals(2, merged.fileSnapshotScanCount());
            assertEquals(3, merged.fileSnapshotScanDurationNanos());
            assertEquals(2, merged.maxFileSnapshotScanNanos());
            assertEquals(2, merged.customerHistoryCacheHits());
            assertEquals(2, merged.customerHistoryCacheMisses());
            assertEquals(2, merged.customerHistoryScanCount());
            assertEquals(3, merged.customerHistoryRecordFileCount());
            assertEquals(3, merged.customerHistoryScanDurationNanos());
            assertEquals(2, merged.maxCustomerHistoryScanNanos());
            assertEquals(2, merged.dataLoadCount());
            assertEquals(3, merged.dataLoadDurationNanos());
            assertEquals(2, merged.viewRenderCount());
            assertEquals(3, merged.viewRenderDurationNanos());
            assertEquals(2, merged.metadataCount());
            assertEquals(3, merged.metadataDurationNanos());
            assertEquals(2, merged.maxMetadataNanos());
        } finally {
            RequestPerformanceContext.finish();
        }
    }

    @Test
    void capturedProgressRemainsImmutableAndCannotLeakIntoTheNextContext() {
        RequestPerformanceContext.Capture capture = new RequestPerformanceContext.Capture();
        RequestPerformanceContext.begin(capture);
        RequestPerformanceContext.recordSql(2);
        RequestPerformanceContext.Snapshot earlier = capture.snapshot();
        RequestPerformanceContext.recordSql(3);
        RequestPerformanceContext.finish();
        assertEquals(1, earlier.sqlCount());
        assertEquals(2, earlier.sqlDurationNanos());
        assertEquals(2, capture.snapshot().sqlCount());
        assertEquals(5, capture.snapshot().sqlDurationNanos());
        RequestPerformanceContext.begin();
        RequestPerformanceContext.recordSql(7);
        assertEquals(7, RequestPerformanceContext.finish().sqlDurationNanos());
        assertEquals(5, capture.snapshot().sqlDurationNanos());
    }

    private static void recordEveryMetric(int value) {
        RequestPerformanceContext.recordSql(value);
        RequestPerformanceContext.recordDbAcquisition(value);
        RequestPerformanceContext.recordMetadata(value);
        RequestPerformanceContext.recordFileSnapshotCacheHit();
        RequestPerformanceContext.recordFileSnapshotCacheMiss();
        RequestPerformanceContext.recordFileSnapshotScan(value);
        RequestPerformanceContext.recordCustomerHistoryCacheHit();
        RequestPerformanceContext.recordCustomerHistoryCacheMiss();
        RequestPerformanceContext.recordCustomerHistoryScan(value, value);
        RequestPerformanceContext.recordDataLoad(value);
        RequestPerformanceContext.recordViewRender(value);
    }

    @Test
    void aggregatesSqlTimingAndClearsThreadState() {
        RequestPerformanceContext.begin();
        RequestPerformanceContext.markOperation(
                RequestPerformanceContext.Operation.TROUBLESHOOTING_CONTENT_SEARCH);
        RequestPerformanceContext.recordSql(2_000_000);
        RequestPerformanceContext.recordSql(5_000_000);
        RequestPerformanceContext.recordDbAcquisition(3_000_000);
        RequestPerformanceContext.recordMetadata(2_000_000);
        RequestPerformanceContext.recordMetadata(4_000_000);
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
        assertEquals(2, snapshot.metadataCount());
        assertEquals(6_000_000, snapshot.metadataDurationNanos());
        assertEquals(4_000_000, snapshot.maxMetadataNanos());
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
        assertEquals(0, cleared.metadataCount());
        assertEquals(
                RequestPerformanceContext.Operation.NONE,
                cleared.operation());
    }
}
