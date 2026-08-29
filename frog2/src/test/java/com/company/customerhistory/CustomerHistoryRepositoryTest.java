package com.company.customerhistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.PageResult;
import com.company.performance.RequestPerformanceContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomerHistoryRepositoryTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-20T03:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void emptyReadDoesNotCreateAStorageDirectory() {
        Path root = temporaryDirectory.resolve("history");
        CustomerHistoryRepository repository = new CustomerHistoryRepository(root, CLOCK);

        PageResult<CustomerHistoryRecord> page = repository.findPage(
                "", "all", "", 1, 20);

        assertTrue(page.items().isEmpty());
        assertFalse(Files.exists(root));
    }

    @Test
    void createdUnicodeRecordPersistsAndCanBeFiltered() throws Exception {
        Path root = temporaryDirectory.resolve("history");
        CustomerHistoryRepository repository = new CustomerHistoryRepository(root, CLOCK);
        CustomerHistoryRecord record = repository.create(
                draft(
                        "테크핀 레이팅스",
                        "개발서버 3번 노드 다운",
                        "현장지원 후 서비스 정상화\n원인 로그 전달"),
                "user-1",
                "홍길동");

        CustomerHistoryRepository reloaded = new CustomerHistoryRepository(root, CLOCK);
        PageResult<CustomerHistoryRecord> page = reloaded.findPage(
                "테크핀 레이팅스", "incident", "정상화", 1, 20);

        assertEquals(1, page.totalCount());
        assertEquals(record.getId(), page.items().getFirst().getId());
        assertEquals("현장지원 후 서비스 정상화\n원인 로그 전달",
                page.items().getFirst().getActionSummary());
        assertEquals("홍길동", page.items().getFirst().getCreatorName());
        try (Stream<Path> files = Files.list(root.resolve("records"))) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void onlyStableOwnerCanUpdateOrDelete() {
        Path root = temporaryDirectory.resolve("history");
        CustomerHistoryRepository repository = new CustomerHistoryRepository(root, CLOCK);
        CustomerHistoryRecord record = repository.create(
                draft("고객사", "DB 증설", "노드 두 대 추가"),
                "owner-id",
                "담당자");

        assertEquals(
                CustomerHistoryRepository.MutationResult.FORBIDDEN,
                repository.updateOwned(
                        record.getId(),
                        draft("고객사", "변조", "변조"),
                        "other-id"));
        assertEquals(
                "DB 증설",
                repository.findById(record.getId()).orElseThrow().getTitle());
        assertEquals(
                CustomerHistoryRepository.MutationResult.FORBIDDEN,
                repository.deleteOwned(record.getId(), "other-id"));
        assertEquals(
                CustomerHistoryRepository.MutationResult.DELETED,
                repository.deleteOwned(record.getId(), "owner-id"));
        assertTrue(repository.findById(record.getId()).isEmpty());
    }

    @Test
    void invalidIdentifierCannotEscapeRepository() {
        CustomerHistoryRepository repository = new CustomerHistoryRepository(
                temporaryDirectory.resolve("history"), CLOCK);

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.findById("../../outside"));
    }

    @Test
    void symbolicLinkRecordIsRejected() throws Exception {
        Path root = temporaryDirectory.resolve("history");
        Path records = Files.createDirectories(root.resolve("records"));
        Path outside = temporaryDirectory.resolve("outside.properties");
        Files.writeString(outside, "version=1\n");
        Files.createSymbolicLink(
                records.resolve(UUID.randomUUID() + ".properties"), outside);
        CustomerHistoryRepository repository = new CustomerHistoryRepository(root, CLOCK);

        assertThrows(
                CustomerHistoryStorageException.class,
                () -> repository.findPage("", "all", "", 1, 20));
    }

    @Test
    void listUsesStableNewestFirstPagination() {
        Path root = temporaryDirectory.resolve("history");
        CustomerHistoryRepository repository = new CustomerHistoryRepository(root, CLOCK);
        repository.create(
                new CustomerHistoryDraft(
                        "고객사",
                        LocalDate.of(2026, 7, 1),
                        CustomerHistoryCategory.UPGRADE,
                        "구버전",
                        "업그레이드 완료",
                        CustomerHistoryStatus.COMPLETED),
                "owner-id",
                "담당자");
        repository.create(
                new CustomerHistoryDraft(
                        "고객사",
                        LocalDate.of(2026, 8, 1),
                        CustomerHistoryCategory.EXPANSION,
                        "신규 증설",
                        "노드 추가",
                        CustomerHistoryStatus.COMPLETED),
                "owner-id",
                "담당자");

        RequestPerformanceContext.begin();
        PageResult<CustomerHistoryRecord> first = repository.findPage(
                "", "all", "", 1, 1);
        RequestPerformanceContext.Snapshot performance =
                RequestPerformanceContext.finish();
        PageResult<CustomerHistoryRecord> second = repository.findPage(
                "", "all", "", 2, 1);

        assertEquals("신규 증설", first.items().getFirst().getTitle());
        assertEquals("구버전", second.items().getFirst().getTitle());
        assertEquals(2, first.totalCount());
        assertEquals(
                RequestPerformanceContext.Operation.CUSTOMER_HISTORY_LIST,
                performance.operation());
        assertEquals(0, performance.customerHistoryCacheHits());
        assertEquals(1, performance.customerHistoryCacheMisses());
        assertEquals(1, performance.customerHistoryScanCount());
        assertEquals(2, performance.customerHistoryRecordFileCount());
        assertTrue(performance.customerHistoryScanDurationNanos() >= 0);
    }

    @Test
    void repeatedListsReuseSnapshotAndWritesInvalidateIt() {
        Path root = temporaryDirectory.resolve("history");
        CustomerHistoryRepository repository =
                new CustomerHistoryRepository(root, CLOCK);
        repository.create(
                draft("고객사", "첫 작업", "첫 작업 완료"),
                "owner-id",
                "담당자");

        RequestPerformanceContext.begin();
        PageResult<CustomerHistoryRecord> first = repository.findPage(
                "", "all", "", 1, 20);
        RequestPerformanceContext.Snapshot firstPerformance =
                RequestPerformanceContext.finish();
        RequestPerformanceContext.begin();
        PageResult<CustomerHistoryRecord> cached = repository.findPage(
                "", "all", "", 1, 20);
        RequestPerformanceContext.Snapshot cachedPerformance =
                RequestPerformanceContext.finish();

        repository.create(
                draft("고객사", "두 번째 작업", "두 번째 작업 완료"),
                "owner-id",
                "담당자");
        RequestPerformanceContext.begin();
        PageResult<CustomerHistoryRecord> refreshed = repository.findPage(
                "", "all", "", 1, 20);
        RequestPerformanceContext.Snapshot refreshedPerformance =
                RequestPerformanceContext.finish();

        assertEquals(1, first.totalCount());
        assertEquals(0, firstPerformance.customerHistoryCacheHits());
        assertEquals(1, firstPerformance.customerHistoryCacheMisses());
        assertEquals(1, firstPerformance.customerHistoryScanCount());
        assertEquals(1, cached.totalCount());
        assertEquals(1, cachedPerformance.customerHistoryCacheHits());
        assertEquals(0, cachedPerformance.customerHistoryCacheMisses());
        assertEquals(0, cachedPerformance.customerHistoryScanCount());
        assertEquals(2, refreshed.totalCount());
        assertEquals(0, refreshedPerformance.customerHistoryCacheHits());
        assertEquals(1, refreshedPerformance.customerHistoryCacheMisses());
        assertEquals(1, refreshedPerformance.customerHistoryScanCount());
        assertEquals(2, refreshedPerformance.customerHistoryRecordFileCount());
    }

    private static CustomerHistoryDraft draft(
            String customerName, String title, String action) {
        return new CustomerHistoryDraft(
                customerName,
                LocalDate.of(2026, 8, 19),
                CustomerHistoryCategory.INCIDENT,
                title,
                action,
                CustomerHistoryStatus.COMPLETED);
    }
}
