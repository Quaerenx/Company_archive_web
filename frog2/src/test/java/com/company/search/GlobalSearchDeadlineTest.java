package com.company.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.controller.GlobalSearchServlet;
import com.company.customerhistory.CustomerHistoryRepository;
import com.company.filerepo.FileRepositoryService;
import com.company.model.CustomerDAO;
import com.company.model.CustomerDTO;
import com.company.model.MeetingRecordDAO;
import com.company.model.MeetingRecordDTO;
import com.company.model.MaintenanceRecordDAO;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.TroubleshootingDAO;
import com.company.model.TroubleshootingDTO;
import com.company.performance.RequestPerformanceContext;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

@Timeout(5)
class GlobalSearchDeadlineTest {
    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(250);

    @TempDir
    Path temporaryDirectory;

    @Test
    void hungFirstSourceReturnsCompletedCategoriesAndCapturesWorkerMetrics()
            throws Exception {
        BlockingSource blocked = new BlockingSource();
        CustomerDAO customers = customers(query -> {
            RequestPerformanceContext.recordSql(11);
            blocked.awaitRelease();
            return List.of();
        });
        MaintenanceRecordDAO maintenance = maintenance(query -> {
            RequestPerformanceContext.recordSql(17);
            RequestPerformanceContext.recordDbAcquisition(3);
            MaintenanceRecordDTO record = new MaintenanceRecordDTO();
            record.setCustomerName("TLS customer");
            return List.of(record);
        });
        ThreadPoolExecutor workers = workers(6, 6);
        GlobalSearchService service = service(
                customers, maintenance, emptyMeetings(), SHORT_TIMEOUT, workers);
        try (ExecutorService caller = Executors.newSingleThreadExecutor(); service) {
            Future<MeasuredOutcome> request = caller.submit(() -> {
                RequestPerformanceContext.begin();
                RequestPerformanceContext.markOperation(
                        RequestPerformanceContext.Operation.GLOBAL_SEARCH);
                RequestPerformanceContext.recordSql(5);
                try {
                    return new MeasuredOutcome(service.search("TLS"),
                            RequestPerformanceContext.finish());
                } finally {
                    RequestPerformanceContext.finish();
                }
            });
            try {
                assertTrue(blocked.started.await(2, TimeUnit.SECONDS));
                MeasuredOutcome measured = request.get(2, TimeUnit.SECONDS);
                assertEquals(List.of("고객사"), measured.outcome().unavailableCategories());
                assertEquals(List.of("정기점검 이력"), measured.outcome().results()
                        .stream().map(GlobalSearchResult::category).toList());
                assertTrue(blocked.interrupted.await(1, TimeUnit.SECONDS));
                assertEquals(3, measured.metrics().sqlCount());
                assertEquals(33, measured.metrics().sqlDurationNanos());
                assertEquals(17, measured.metrics().maxSqlNanos());
                assertEquals(1, measured.metrics().dbAcquisitionCount());
                assertEquals(RequestPerformanceContext.Operation.GLOBAL_SEARCH,
                        measured.metrics().operation());
            } finally {
                blocked.release.countDown();
            }
        }
    }

    @Test
    void expiredQueuedSourcesNeverRunAndIgnoredInterruptsDoNotGrowWorkers()
            throws Exception {
        BlockingSource blocked = new BlockingSource();
        AtomicInteger customerCalls = new AtomicInteger();
        AtomicInteger maintenanceCalls = new AtomicInteger();
        AtomicInteger meetingCalls = new AtomicInteger();
        CustomerDAO customers = customers(query -> {
            if (customerCalls.incrementAndGet() == 1) {
                blocked.awaitRelease();
            }
            return List.of();
        });
        ThreadPoolExecutor workers = workers(1, 6);
        GlobalSearchService service = service(customers, maintenance(query -> {
            maintenanceCalls.incrementAndGet();
            return List.of();
        }), meetings(query -> {
            meetingCalls.incrementAndGet();
            return List.of();
        }), SHORT_TIMEOUT, workers);
        try (ExecutorService caller = Executors.newSingleThreadExecutor(); service) {
            Future<GlobalSearchOutcome> first = caller.submit(() -> service.search("TLS"));
            try {
                assertTrue(blocked.started.await(2, TimeUnit.SECONDS));
                assertTrue(first.get(2, TimeUnit.SECONDS).allSourcesUnavailable());
                assertTrue(blocked.interrupted.await(1, TimeUnit.SECONDS));
                assertTrue(caller.submit(() -> service.search("again"))
                        .get(2, TimeUnit.SECONDS).allSourcesUnavailable());
                assertEquals(1, customerCalls.get());
                assertEquals(0, maintenanceCalls.get());
                assertEquals(0, meetingCalls.get());
                assertEquals(1, workers.getPoolSize());
                assertEquals(0, workers.getQueue().size());
                blocked.release.countDown();
                assertTrue(blocked.finished.await(1, TimeUnit.SECONDS));
                assertFalse(caller.submit(() -> service.search("later"))
                        .get(2, TimeUnit.SECONDS).partial());
                assertEquals(2, customerCalls.get());
                assertEquals(1, maintenanceCalls.get());
                assertEquals(1, meetingCalls.get());
            } finally {
                blocked.release.countDown();
            }
        }
    }

    @Test
    void fullWorkerQueueRejectsSearchWithoutExecutingSourcesOnCaller()
            throws Exception {
        BlockingSource blocked = new BlockingSource();
        ThreadPoolExecutor workers = workers(1, 1);
        workers.execute(blocked::awaitRelease);
        assertTrue(blocked.started.await(2, TimeUnit.SECONDS));
        workers.execute(() -> { });
        AtomicInteger sourceCalls = new AtomicInteger();
        GlobalSearchService service = service(customers(query -> {
            sourceCalls.incrementAndGet();
            return List.of();
        }), emptyMaintenance(), emptyMeetings(), Duration.ofSeconds(3), workers);
        try (ExecutorService caller = Executors.newSingleThreadExecutor(); service) {
            try {
                GlobalSearchOutcome outcome = caller.submit(() -> service.search("TLS"))
                        .get(1, TimeUnit.SECONDS);
                assertTrue(outcome.allSourcesUnavailable());
                assertEquals(0, sourceCalls.get());
                assertEquals(1, workers.getPoolSize());
                assertEquals(1, workers.getQueue().size());
            } finally {
                blocked.release.countDown();
            }
        }
    }

    @Test
    void servletDestroyShutsDownWorkersAndWakesPendingRequests()
            throws Exception {
        BlockingSource blocked = new BlockingSource();
        CountDownLatch submitted = new CountDownLatch(6);
        ThreadPoolExecutor workers = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(6),
                GlobalSearchDeadlineTest::workerThread) {
            @Override
            public void execute(Runnable task) {
                super.execute(task);
                if (task instanceof FutureTask<?>) {
                    submitted.countDown();
                }
            }
        };
        workers.execute(blocked::awaitRelease);
        assertTrue(blocked.started.await(2, TimeUnit.SECONDS));
        AtomicInteger sourceCalls = new AtomicInteger();
        GlobalSearchService service = service(customers(query -> {
            sourceCalls.incrementAndGet();
            return List.of();
        }), emptyMaintenance(), emptyMeetings(), Duration.ofSeconds(3), workers);
        GlobalSearchServlet servlet = new GlobalSearchServlet();
        Field searchService = GlobalSearchServlet.class.getDeclaredField("searchService");
        searchService.setAccessible(true);
        searchService.set(servlet, service);
        try (ExecutorService caller = Executors.newSingleThreadExecutor(); service) {
            Future<GlobalSearchOutcome> request = caller.submit(() -> service.search("TLS"));
            try {
                assertTrue(submitted.await(1, TimeUnit.SECONDS));
                servlet.destroy();
                assertTrue(workers.isShutdown());
                assertTrue(request.get(1, TimeUnit.SECONDS).allSourcesUnavailable());
                assertEquals(0, sourceCalls.get());
                assertEquals(0, workers.getQueue().size());
                assertTrue(service.search("later").allSourcesUnavailable());
                servlet.destroy();
            } finally {
                blocked.release.countDown();
            }
        }
    }

    @Test
    void failedSourceKeepsItsMetricsWithoutOverwritingRequestOperation()
            throws Exception {
        GlobalSearchService service = service(customers(query -> {
            RequestPerformanceContext.recordSql(23);
            RequestPerformanceContext.markOperation(
                    RequestPerformanceContext.Operation.CUSTOMERS_LIST);
            throw new IllegalStateException("source failure");
        }), emptyMaintenance(), emptyMeetings(), Duration.ofSeconds(3), workers(6, 6));
        RequestPerformanceContext.begin();
        RequestPerformanceContext.markOperation(RequestPerformanceContext.Operation.GLOBAL_SEARCH);
        try (service) {
            assertEquals(List.of("고객사"), service.search("TLS").unavailableCategories());
            RequestPerformanceContext.Snapshot metrics = RequestPerformanceContext.finish();
            assertEquals(1, metrics.sqlCount());
            assertEquals(23, metrics.sqlDurationNanos());
            assertEquals(RequestPerformanceContext.Operation.GLOBAL_SEARCH, metrics.operation());
        } finally {
            RequestPerformanceContext.finish();
        }
    }

    @Test
    void invalidInputDoesNotSubmitAnyWorkAndWorkerErrorsPropagate()
            throws Exception {
        AssertionError failure = new AssertionError("fatal source failure");
        AtomicInteger sourceCalls = new AtomicInteger();
        ThreadPoolExecutor workers = workers(6, 6);
        GlobalSearchService service = service(customers(query -> {
            sourceCalls.incrementAndGet();
            throw failure;
        }), emptyMaintenance(), emptyMeetings(), Duration.ofSeconds(3), workers);
        try (service) {
            for (String query : new String[] {null, " ", "T", "T".repeat(101)}) {
                assertThrows(IllegalArgumentException.class, () -> service.search(query));
            }
            assertEquals(0, sourceCalls.get());
            assertEquals(0, workers.getPoolSize());
            assertSame(failure, assertThrows(AssertionError.class, () -> service.search("TLS")));
        }
    }

    private GlobalSearchService service(
            CustomerDAO customers, MaintenanceRecordDAO maintenance,
            MeetingRecordDAO meetings, Duration timeout, ThreadPoolExecutor workers)
            throws Exception {
        return new GlobalSearchService(customers,
                new CustomerHistoryRepository(temporaryDirectory.resolve("history")),
                new TroubleshootingDAO() {
                    @Override
                    public List<TroubleshootingDTO> searchTroubleshootings(String query, int limit) {
                        return List.of();
                    }
                }, meetings, maintenance,
                new FileRepositoryService(Files.createDirectory(temporaryDirectory.resolve("files"))),
                timeout, workers);
    }

    private static CustomerDAO customers(Function<String, List<CustomerDTO>> load) {
        return new CustomerDAO() {
            @Override
            public List<CustomerDTO> searchCustomers(String query, int limit) {
                return load.apply(query);
            }
        };
    }

    private static MaintenanceRecordDAO maintenance(
            Function<String, List<MaintenanceRecordDTO>> load) {
        return new MaintenanceRecordDAO() {
            @Override
            public List<MaintenanceRecordDTO> searchMaintenanceRecords(String query, int limit) {
                return load.apply(query);
            }
        };
    }

    private static MeetingRecordDAO meetings(Function<String, List<MeetingRecordDTO>> load) {
        return new MeetingRecordDAO() {
            @Override
            public List<MeetingRecordDTO> searchMeetingRecords(String query, int limit) {
                return load.apply(query);
            }
        };
    }

    private static MaintenanceRecordDAO emptyMaintenance() {
        return maintenance(query -> List.of());
    }

    private static MeetingRecordDAO emptyMeetings() {
        return meetings(query -> List.of());
    }

    private static ThreadPoolExecutor workers(int count, int capacity) {
        return new ThreadPoolExecutor(count, count, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), GlobalSearchDeadlineTest::workerThread);
    }

    private static Thread workerThread(Runnable task) {
        Thread thread = new Thread(task, "global-search-test");
        thread.setDaemon(true);
        return thread;
    }

    private record MeasuredOutcome(GlobalSearchOutcome outcome,
            RequestPerformanceContext.Snapshot metrics) {
    }

    private static final class BlockingSource {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch interrupted = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch finished = new CountDownLatch(1);

        private void awaitRelease() {
            started.countDown();
            boolean wasInterrupted = false;
            while (release.getCount() != 0) {
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    wasInterrupted = true;
                    interrupted.countDown();
                }
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
            finished.countDown();
        }
    }
}
