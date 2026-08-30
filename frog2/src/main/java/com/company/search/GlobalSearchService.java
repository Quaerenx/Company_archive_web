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
import com.company.model.TroubleshootingDAO;
import com.company.model.TroubleshootingDTO;
import com.company.util.SearchQueryPolicy;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlobalSearchService {
    private static final Logger logger = LoggerFactory.getLogger(
            GlobalSearchService.class);
    static final int RESULTS_PER_CATEGORY = 5;
    private static final int DESCRIPTION_MAX_CODE_POINTS = 96;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final CustomerDAO customerDAO;
    private final CustomerHistoryRepository historyRepository;
    private final TroubleshootingDAO troubleshootingDAO;
    private final MeetingRecordDAO meetingDAO;
    private final FileRepositoryService fileRepositoryService;

    public GlobalSearchService() throws IOException {
        this(
                new CustomerDAO(),
                new CustomerHistoryRepository(),
                new TroubleshootingDAO(),
                new MeetingRecordDAO(),
                new FileRepositoryService(FileRepositoryConfig.repositoryRoot()));
    }

    GlobalSearchService(
            CustomerDAO customerDAO,
            CustomerHistoryRepository historyRepository,
            TroubleshootingDAO troubleshootingDAO,
            MeetingRecordDAO meetingDAO,
            FileRepositoryService fileRepositoryService) {
        this.customerDAO = Objects.requireNonNull(customerDAO, "customerDAO");
        this.historyRepository = Objects.requireNonNull(
                historyRepository, "historyRepository");
        this.troubleshootingDAO = Objects.requireNonNull(
                troubleshootingDAO, "troubleshootingDAO");
        this.meetingDAO = Objects.requireNonNull(meetingDAO, "meetingDAO");
        this.fileRepositoryService = Objects.requireNonNull(
                fileRepositoryService, "fileRepositoryService");
    }

    public GlobalSearchOutcome search(String rawQuery) {
        String query = SearchQueryPolicy.normalize(rawQuery);
        if (query == null) {
            throw new IllegalArgumentException("검색어를 입력해 주세요.");
        }

        List<GlobalSearchResult> results = new ArrayList<>(
                RESULTS_PER_CATEGORY * 5);
        List<String> unavailableCategories = new ArrayList<>();
        appendSource(
                "고객사",
                () -> customerDAO.searchCustomers(
                                query, RESULTS_PER_CATEGORY).stream()
                        .map(this::customerResult)
                        .toList(),
                results,
                unavailableCategories);
        appendSource(
                "고객사 히스토리",
                () -> historyRepository.findPage(
                                "", "all", query, 1, RESULTS_PER_CATEGORY)
                        .items().stream()
                        .map(record -> historyResult(record, query))
                        .toList(),
                results,
                unavailableCategories);
        appendSource(
                "트러블슈팅",
                () -> troubleshootingDAO.getTroubleshootingPage(
                                query, true, 1, RESULTS_PER_CATEGORY)
                        .items().stream()
                        .map(this::troubleshootingResult)
                        .toList(),
                results,
                unavailableCategories);
        appendSource(
                "회의록",
                () -> meetingDAO.searchMeetingRecords(
                                query, RESULTS_PER_CATEGORY).stream()
                        .map(this::meetingResult)
                        .toList(),
                results,
                unavailableCategories);
        appendSource(
                "자료실",
                () -> fileRepositoryService.search(
                                query, RESULTS_PER_CATEGORY).stream()
                        .map(this::fileResult)
                        .toList(),
                results,
                unavailableCategories);
        return new GlobalSearchOutcome(results, unavailableCategories);
    }

    private static void appendSource(
            String category,
            SearchSource source,
            List<GlobalSearchResult> results,
            List<String> unavailableCategories) {
        try {
            results.addAll(source.load());
        } catch (FileRepositoryException | RuntimeException exception) {
            unavailableCategories.add(category);
            logger.warn(
                    "Global search source unavailable category={} cause={}",
                    category,
                    exception.getClass().getSimpleName());
            logger.debug(
                    "Global search source failure category=" + category,
                    exception);
        }
    }

    private GlobalSearchResult customerResult(CustomerDTO customer) {
        String description = joinDetails(
                prefixed("Vertica", customer.getVerticaVersion()),
                prefixed("DB", customer.getDbName()),
                prefixed("담당", customer.getManagerName()));
        return new GlobalSearchResult(
                "고객사",
                customer.getCustomerName(),
                description,
                "/customers?view=detail&customerName="
                        + encode(customer.getCustomerName()));
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
                        + "&q=" + encode(query));
    }

    private GlobalSearchResult troubleshootingResult(
            TroubleshootingDTO troubleshooting) {
        return new GlobalSearchResult(
                "트러블슈팅",
                troubleshooting.getTitle(),
                joinDetails(
                        troubleshooting.getCustomerName(),
                        businessDate(troubleshooting.getOccurrenceDate())),
                "/troubleshooting?view=view&id=" + troubleshooting.getId());
    }

    private GlobalSearchResult meetingResult(MeetingRecordDTO meeting) {
        LocalDate meetingDate = meeting.getMeetingDatetime() == null
                ? null
                : meeting.getMeetingDatetime().toLocalDateTime().toLocalDate();
        return new GlobalSearchResult(
                "회의록",
                meeting.getTitle(),
                joinDetails(
                        meetingDate == null ? null : meetingDate.toString(),
                        meeting.getMeetingTypeLabel()),
                "/meeting?view=view&id=" + meeting.getMeetingId());
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
                path);
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
