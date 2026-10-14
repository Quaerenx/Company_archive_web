package com.company.search;

import com.company.customerhistory.CustomerHistoryRecord;
import com.company.customerhistory.CustomerHistoryRepository;
import com.company.filerepo.FileRepositoryConfig;
import com.company.filerepo.FileRepositoryEntry;
import com.company.filerepo.FileRepositoryException;
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
import com.company.util.SearchQueryPolicy;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlobalSearchService implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(
            GlobalSearchService.class);
    static final int RESULTS_PER_CATEGORY = 5;
    static final Duration SEARCH_TIMEOUT = Duration.ofSeconds(3);
    private static final int WORKER_COUNT = 6;
    private static final int QUEUE_CAPACITY = 24;
    private static final int DESCRIPTION_MAX_CODE_POINTS = 96;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final CustomerDAO customerDAO;
    private final CustomerHistoryRepository historyRepository;
    private final TroubleshootingDAO troubleshootingDAO;
    private final MeetingRecordDAO meetingDAO;
    private final MaintenanceRecordDAO maintenanceDAO;
    private final FileRepositoryService fileRepositoryService;
    private final long timeoutNanos;
    private final ThreadPoolExecutor executor;
    private final Set<FutureTask<SourceResult>> activeTasks =
            ConcurrentHashMap.newKeySet();

    public GlobalSearchService() throws IOException {
        this(
                new CustomerDAO(),
                new CustomerHistoryRepository(),
                new TroubleshootingDAO(),
                new MeetingRecordDAO(),
                new MaintenanceRecordDAO(),
                new FileRepositoryService(FileRepositoryConfig.repositoryRoot()));
    }

    GlobalSearchService(
            CustomerDAO customerDAO,
            CustomerHistoryRepository historyRepository,
            TroubleshootingDAO troubleshootingDAO,
            MeetingRecordDAO meetingDAO,
            MaintenanceRecordDAO maintenanceDAO,
            FileRepositoryService fileRepositoryService) {
        this(customerDAO, historyRepository, troubleshootingDAO, meetingDAO,
                maintenanceDAO, fileRepositoryService, SEARCH_TIMEOUT,
                WORKER_COUNT, QUEUE_CAPACITY);
    }

    GlobalSearchService(
            CustomerDAO customerDAO,
            CustomerHistoryRepository historyRepository,
            TroubleshootingDAO troubleshootingDAO,
            MeetingRecordDAO meetingDAO,
            MaintenanceRecordDAO maintenanceDAO,
            FileRepositoryService fileRepositoryService,
            Duration timeout,
            int workerCount,
            int queueCapacity) {
        this(customerDAO, historyRepository, troubleshootingDAO, meetingDAO,
                maintenanceDAO, fileRepositoryService, timeout,
                newExecutor(workerCount, queueCapacity));
    }

    GlobalSearchService(
            CustomerDAO customerDAO,
            CustomerHistoryRepository historyRepository,
            TroubleshootingDAO troubleshootingDAO,
            MeetingRecordDAO meetingDAO,
            MaintenanceRecordDAO maintenanceDAO,
            FileRepositoryService fileRepositoryService,
            Duration timeout,
            ThreadPoolExecutor executor) {
        this.customerDAO = Objects.requireNonNull(customerDAO, "customerDAO");
        this.historyRepository = Objects.requireNonNull(
                historyRepository, "historyRepository");
        this.troubleshootingDAO = Objects.requireNonNull(
                troubleshootingDAO, "troubleshootingDAO");
        this.meetingDAO = Objects.requireNonNull(meetingDAO, "meetingDAO");
        this.maintenanceDAO = Objects.requireNonNull(
                maintenanceDAO, "maintenanceDAO");
        this.fileRepositoryService = Objects.requireNonNull(
                fileRepositoryService, "fileRepositoryService");
        timeoutNanos = Objects.requireNonNull(timeout, "timeout").toNanos();
        if (timeoutNanos <= 0) {
            throw new IllegalArgumentException("Search timeout must be positive");
        }
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    private static ThreadPoolExecutor newExecutor(int workerCount, int queueCapacity) {
        if (workerCount <= 0 || queueCapacity <= 0) {
            throw new IllegalArgumentException("Search workers and queue capacity must be positive");
        }
        AtomicInteger workerNumber = new AtomicInteger();
        return new ThreadPoolExecutor(
                workerCount, workerCount, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), task -> {
                    Thread worker = new Thread(
                            task, "global-search-" + workerNumber.incrementAndGet());
                    worker.setDaemon(true);
                    return worker;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public GlobalSearchOutcome search(String rawQuery) {
        String query = SearchQueryPolicy.normalize(rawQuery);
        if (query == null) {
            throw new IllegalArgumentException("검색어를 입력해 주세요.");
        }

        long deadline = System.nanoTime() + timeoutNanos;
        List<Source> sources = List.of(
                new Source("고객사",
                () -> customerDAO.searchCustomers(
                                query, RESULTS_PER_CATEGORY).stream()
                        .map(customer -> customerResult(customer, query))
                        .toList()),
                new Source("고객사 히스토리",
                () -> historyRepository.findPage(
                                "", "all", query, 1, RESULTS_PER_CATEGORY)
                        .items().stream()
                        .map(record -> historyResult(record, query))
                        .toList()),
                new Source("정기점검 이력",
                () -> maintenanceDAO.searchMaintenanceRecords(
                                query, RESULTS_PER_CATEGORY).stream()
                        .map(this::maintenanceResult)
                        .toList()),
                new Source("트러블슈팅",
                () -> troubleshootingDAO.searchTroubleshootings(
                                query, RESULTS_PER_CATEGORY).stream()
                        .map(record -> troubleshootingResult(record, query))
                        .toList()),
                new Source("회의록",
                () -> meetingDAO.searchMeetingRecords(
                                query, RESULTS_PER_CATEGORY).stream()
                        .map(record -> meetingResult(record, query))
                        .toList()),
                new Source("자료실",
                () -> fileRepositoryService.search(
                                query, RESULTS_PER_CATEGORY).stream()
                        .map(this::fileResult)
                        .toList()));
        List<PendingSource> pending = new ArrayList<>(sources.size());
        try {
            for (Source source : sources) {
                SourceWork work = new SourceWork(source, deadline);
                FutureTask<SourceResult> future = new FutureTask<>(work) {
                    @Override
                    protected void done() {
                        activeTasks.remove(this);
                    }
                };
                pending.add(new PendingSource(source.category(), work, future));
                activeTasks.add(future);
                try {
                    executor.execute(future);
                } catch (RejectedExecutionException exception) {
                    future.cancel(false);
                    logUnavailable(source.category(), exception);
                }
            }
            List<GlobalSearchResult> results = new ArrayList<>(
                    RESULTS_PER_CATEGORY * sources.size());
            List<String> unavailableCategories = new ArrayList<>();
            for (PendingSource source : pending) {
                SourceResult result = awaitSource(source, deadline);
                if (result == null || result.results() == null
                        || result.completedNanos() - deadline > 0) {
                    unavailableCategories.add(source.category());
                } else {
                    results.addAll(result.results());
                }
            }
            return new GlobalSearchOutcome(results, unavailableCategories);
        } finally {
            for (PendingSource source : pending) {
                source.future().cancel(true);
                executor.remove(source.future());
                RequestPerformanceContext.merge(source.work().capture.snapshot());
            }
        }
    }

    private static SourceResult awaitSource(PendingSource source, long deadline) {
        try {
            if (source.future().isDone()) {
                return source.future().get();
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0 || Thread.currentThread().isInterrupted()) {
                return null;
            }
            return source.future().get(remaining, TimeUnit.NANOSECONDS);
        } catch (TimeoutException | CancellationException exception) {
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Unexpected search worker failure", exception);
        }
    }

    private static void logUnavailable(String category, Exception exception) {
        logger.warn(
                "Global search source unavailable category={} cause={}",
                category, exception.getClass().getSimpleName());
        logger.debug("Global search source failure category=" + category, exception);
    }

    @Override
    public void close() {
        executor.shutdownNow();
        for (FutureTask<SourceResult> task : activeTasks) {
            task.cancel(true);
        }
    }

    private record Source(String category, SearchSource source) {
    }

    private record PendingSource(
            String category, SourceWork work, FutureTask<SourceResult> future) {
    }

    private record SourceResult(List<GlobalSearchResult> results, long completedNanos) {
    }

    private static final class SourceWork implements Callable<SourceResult> {
        private final Source source;
        private final long deadline;
        private final RequestPerformanceContext.Capture capture =
                new RequestPerformanceContext.Capture();

        private SourceWork(Source source, long deadline) {
            this.source = source;
            this.deadline = deadline;
        }

        @Override
        public SourceResult call() {
            RequestPerformanceContext.begin(capture);
            try {
                if (System.nanoTime() - deadline >= 0) {
                    return new SourceResult(null, System.nanoTime());
                }
                List<GlobalSearchResult> results;
                try {
                    results = source.source().load();
                } catch (FileRepositoryException | RuntimeException exception) {
                    logUnavailable(source.category(), exception);
                    results = null;
                }
                return new SourceResult(results, System.nanoTime());
            } finally {
                RequestPerformanceContext.finish();
            }
        }
    }

    private GlobalSearchResult customerResult(
            CustomerDTO customer, String query) {
        String description = joinDetails(
                prefixed("Vertica", customer.getVerticaVersion()),
                prefixed("DB", customer.getDbName()),
                prefixed("담당", customer.getManagerName()));
        return new GlobalSearchResult(
                "고객사",
                customer.getCustomerName(),
                description,
                "/customers?view=detail&customerName="
                        + encode(customer.getCustomerName()),
                "customers",
                "/customers?view=list&q=" + encode(query),
                List.of(
                        new GlobalSearchAction(
                                "정기점검",
                                "/maintenance?view=history&customerName="
                                        + encode(customer.getCustomerName())),
                        new GlobalSearchAction(
                                "히스토리",
                                "/customer-history?customerName="
                                        + encode(customer.getCustomerName()))));
    }

    private GlobalSearchResult historyResult(
            CustomerHistoryRecord record, String query) {
        String description = joinDetails(
                record.getWorkDate().toString(),
                record.getCustomerName(),
                record.getCategory().getLabel(),
                summarize(record.getActionSummary()));
        return new GlobalSearchResult(
                "고객사 히스토리",
                record.getTitle(),
                description,
                "/customer-history?customerName="
                        + encode(record.getCustomerName())
                        + "&q=" + encode(query),
                "history",
                "/customer-history?q=" + encode(query),
                List.of());
    }

    private GlobalSearchResult maintenanceResult(MaintenanceRecordDTO record) {
        return new GlobalSearchResult(
                "정기점검 이력",
                record.getCustomerName(),
                joinDetails(
                        businessDate(record.getInspectionDate()),
                        record.getVerticaVersion(),
                        summarize(record.getNote())),
                "/maintenance?view=history&customerName="
                        + encode(record.getCustomerName()),
                "maintenance",
                "/maintenance",
                List.of());
    }

    private GlobalSearchResult troubleshootingResult(
            TroubleshootingDTO troubleshooting, String query) {
        return new GlobalSearchResult(
                "트러블슈팅",
                troubleshooting.getTitle(),
                joinDetails(
                        troubleshooting.getCustomerName(),
                        businessDate(troubleshooting.getOccurrenceDate())),
                "/troubleshooting?view=view&id=" + troubleshooting.getId(),
                "troubleshooting",
                "/troubleshooting?view=list&scope=content&q=" + encode(query),
                List.of());
    }

    private GlobalSearchResult meetingResult(
            MeetingRecordDTO meeting, String query) {
        LocalDate meetingDate = meeting.getMeetingDatetime() == null
                ? null
                : meeting.getMeetingDatetime().toLocalDateTime().toLocalDate();
        return new GlobalSearchResult(
                "회의록",
                meeting.getTitle(),
                joinDetails(
                        meetingDate == null ? null : meetingDate.toString(),
                        meeting.getMeetingTypeLabel()),
                "/meeting?view=view&id=" + meeting.getMeetingId()
                        + "&returnQ=" + encode(query),
                "meetings",
                "/meeting?view=list&q=" + encode(query),
                List.of());
    }

    private GlobalSearchResult fileResult(FileRepositoryEntry entry) {
        String location = entry.getPath().isEmpty()
                ? "자료실 최상위"
                : entry.getPath();
        String path = entry.isDirectory()
                ? "/file-repository?path=" + encode(entry.getPath())
                : "/file-repository/download?path="
                        + encode(entry.getPath())
                        + "&id=" + encode(entry.getId());
        return new GlobalSearchResult(
                entry.isDirectory() ? "자료실 폴더" : "자료실 파일",
                entry.getName(),
                joinDetails(
                        location,
                        entry.getDescription(),
                        entry.isDirectory() ? null : entry.getSizeText()),
                path,
                "files",
                "/file-repository",
                List.of());
    }

    private static String businessDate(Date value) {
        if (value == null) {
            return null;
        }
        return Instant.ofEpochMilli(value.getTime())
                .atZone(BUSINESS_ZONE)
                .toLocalDate()
                .toString();
    }

    private static String prefixed(String prefix, String value) {
        return value == null || value.isBlank()
                ? null
                : prefix + " " + value.strip();
    }

    private static String joinDetails(String... values) {
        List<String> details = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                details.add(value.strip());
            }
        }
        return String.join(" · ", details);
    }

    private static String summarize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").strip();
        int length = normalized.codePointCount(0, normalized.length());
        if (length <= DESCRIPTION_MAX_CODE_POINTS) {
            return normalized;
        }
        int end = normalized.offsetByCodePoints(
                0, DESCRIPTION_MAX_CODE_POINTS - 1);
        return normalized.substring(0, end).stripTrailing() + "…";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface SearchSource {
        List<GlobalSearchResult> load() throws FileRepositoryException;
    }
}
