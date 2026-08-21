package com.company.customerhistory;

import com.company.model.PageResult;
import com.company.performance.RequestPerformanceContext;
import com.company.util.Pagination;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Stores the small, manually curated customer work history outside the webroot.
 */
public final class CustomerHistoryRepository {
    private static final String RECORDS_DIRECTORY = "records";
    private static final String FILE_SUFFIX = ".properties";
    private static final String FORMAT_VERSION = "1";
    private static final Comparator<CustomerHistoryRecord> NEWEST_FIRST =
            Comparator.comparing(CustomerHistoryRecord::getWorkDate)
                    .reversed()
                    .thenComparing(
                            CustomerHistoryRecord::getCreatedAt,
                            Comparator.reverseOrder())
                    .thenComparing(CustomerHistoryRecord::getId);

    private final Path root;
    private final Clock clock;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public CustomerHistoryRepository() {
        this(CustomerHistoryConfig.repositoryRoot(), Clock.systemUTC());
    }

    public CustomerHistoryRepository(Path root) {
        this(root, Clock.systemUTC());
    }

    CustomerHistoryRepository(Path root, Clock clock) {
        this.root = Objects.requireNonNull(root, "root")
                .toAbsolutePath()
                .normalize();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public PageResult<CustomerHistoryRecord> findPage(
            String customerName,
            String categoryCode,
            String query,
            int requestedPage,
            int pageSize) {
        Pagination.totalPages(0, pageSize);
        String normalizedCustomer = normalizeOptional(customerName);
        CustomerHistoryCategory category = normalizeCategory(categoryCode);
        String normalizedQuery = normalizeQuery(query);
        RequestPerformanceContext.markOperation(
                RequestPerformanceContext.Operation.CUSTOMER_HISTORY_LIST);

        lock.readLock().lock();
        try {
            List<CustomerHistoryRecord> matching = loadAll().stream()
                    .filter(record -> normalizedCustomer.isEmpty()
                            || record.getCustomerName().equals(normalizedCustomer))
                    .filter(record -> category == null
                            || record.getCategory() == category)
                    .filter(record -> matchesQuery(record, normalizedQuery))
                    .sorted(NEWEST_FIRST)
                    .toList();
            int totalCount = matching.size();
            int totalPages = Pagination.totalPages(totalCount, pageSize);
            int page = Pagination.clampPage(requestedPage, totalPages);
            if (matching.isEmpty()) {
                return new PageResult<>(List.of(), 0, 1, pageSize);
            }
            int from = Pagination.offset(page, pageSize);
            int to = Math.min(from + pageSize, totalCount);
            return new PageResult<>(matching.subList(from, to), totalCount, page, pageSize);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<CustomerHistoryRecord> findById(String id) {
        String normalizedId = normalizeId(id);
        lock.readLock().lock();
        try {
            Path path = recordPath(normalizedId, false);
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                return Optional.empty();
            }
            ensureSafeRecordFile(path);
            return Optional.of(readRecord(path));
        } finally {
            lock.readLock().unlock();
        }
    }

    public CustomerHistoryRecord create(
            CustomerHistoryDraft draft,
            String creatorUserId,
            String creatorName) {
        Objects.requireNonNull(draft, "draft");
        String stableUserId = requiredIdentity(creatorUserId);
        String displayName = displayName(creatorName, stableUserId);
        Instant now = clock.instant();
        CustomerHistoryRecord record = new CustomerHistoryRecord(
                UUID.randomUUID().toString(),
                draft.customerName(),
                draft.workDate(),
                draft.category(),
                draft.title(),
                draft.actionSummary(),
                draft.status(),
                stableUserId,
                displayName,
                now,
                now);

        lock.writeLock().lock();
        try {
            writeRecord(record, false);
            return record;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MutationResult updateOwned(
            String id,
            CustomerHistoryDraft draft,
            String ownerUserId) {
        Objects.requireNonNull(draft, "draft");
        String normalizedId = normalizeId(id);
        String stableUserId = requiredIdentity(ownerUserId);
        lock.writeLock().lock();
        try {
            Path path = recordPath(normalizedId, false);
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                return MutationResult.NOT_FOUND;
            }
            ensureSafeRecordFile(path);
            CustomerHistoryRecord current = readRecord(path);
            if (!current.isOwnedBy(stableUserId)) {
                return MutationResult.FORBIDDEN;
            }
            CustomerHistoryRecord updated = new CustomerHistoryRecord(
                    current.getId(),
                    draft.customerName(),
                    draft.workDate(),
                    draft.category(),
                    draft.title(),
                    draft.actionSummary(),
                    draft.status(),
                    current.getCreatorUserId(),
                    current.getCreatorName(),
                    current.getCreatedAt(),
                    clock.instant());
            writeRecord(updated, true);
            return MutationResult.UPDATED;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MutationResult deleteOwned(String id, String ownerUserId) {
        String normalizedId = normalizeId(id);
        String stableUserId = requiredIdentity(ownerUserId);
        lock.writeLock().lock();
        try {
            Path path = recordPath(normalizedId, false);
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                return MutationResult.NOT_FOUND;
            }
            ensureSafeRecordFile(path);
            CustomerHistoryRecord current = readRecord(path);
            if (!current.isOwnedBy(stableUserId)) {
                return MutationResult.FORBIDDEN;
            }
            try {
                Files.delete(path);
                return MutationResult.DELETED;
            } catch (IOException exception) {
                throw storageFailure("고객사 히스토리를 삭제할 수 없습니다.", exception);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<CustomerHistoryRecord> loadAll() {
        Path records = recordsDirectory(false);
        if (!Files.exists(records, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        ensureSafeDirectory(records);
        long startedAt = System.nanoTime();
        int recordFileCount = 0;
        List<CustomerHistoryRecord> result = new ArrayList<>();
        try (Stream<Path> paths = Files.list(records)) {
            List<Path> recordFiles = paths
                    .filter(candidate -> candidate.getFileName().toString().endsWith(FILE_SUFFIX))
                    .sorted()
                    .toList();
            recordFileCount = recordFiles.size();
            for (Path path : recordFiles) {
                ensureSafeRecordFile(path);
                result.add(readRecord(path));
            }
            return result;
        } catch (IOException exception) {
            throw storageFailure("고객사 히스토리를 읽을 수 없습니다.", exception);
        } finally {
            RequestPerformanceContext.recordCustomerHistoryScan(
                    recordFileCount, System.nanoTime() - startedAt);
        }
    }

    private CustomerHistoryRecord readRecord(Path path) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
            if (!FORMAT_VERSION.equals(required(properties, "version"))) {
                throw new CustomerHistoryStorageException(
                        "지원하지 않는 고객사 히스토리 형식입니다.");
            }
            String fileId = path.getFileName().toString();
            fileId = fileId.substring(0, fileId.length() - FILE_SUFFIX.length());
            String id = normalizeId(required(properties, "id"));
            if (!fileId.equals(id)) {
                throw new CustomerHistoryStorageException(
                        "고객사 히스토리 파일 식별자가 일치하지 않습니다.");
            }
            CustomerHistoryDraft draft = new CustomerHistoryDraft(
                    required(properties, "customerName"),
                    LocalDate.parse(required(properties, "workDate")),
                    CustomerHistoryCategory.fromCode(required(properties, "category")),
                    required(properties, "title"),
                    required(properties, "actionSummary"),
                    CustomerHistoryStatus.fromCode(required(properties, "status")));
            return new CustomerHistoryRecord(
                    id,
                    draft.customerName(),
                    draft.workDate(),
                    draft.category(),
                    draft.title(),
                    draft.actionSummary(),
                    draft.status(),
                    required(properties, "creatorUserId"),
                    required(properties, "creatorName"),
                    Instant.parse(required(properties, "createdAt")),
                    Instant.parse(required(properties, "updatedAt")));
        } catch (CustomerHistoryStorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw storageFailure("고객사 히스토리 파일이 손상되었습니다.", exception);
        }
    }

    private void writeRecord(CustomerHistoryRecord record, boolean replace) {
        Path records = recordsDirectory(true);
        Path target = recordPath(record.getId(), true);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(records, ".customer-history-", ".tmp");
            Properties properties = toProperties(record);
            try (Writer writer = Files.newBufferedWriter(
                    temporary,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                properties.store(writer, null);
            }
            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            moveAtomically(temporary, target, replace);
        } catch (IOException exception) {
            throw storageFailure("고객사 히스토리를 저장할 수 없습니다.", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup of a request-local temporary file.
                }
            }
        }
    }

    private Path recordsDirectory(boolean create) {
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            ensureSafeDirectory(root);
        } else if (create) {
            try {
                Files.createDirectories(root);
            } catch (IOException exception) {
                throw storageFailure("고객사 히스토리 저장 경로를 만들 수 없습니다.", exception);
            }
            ensureSafeDirectory(root);
        }

        Path records = root.resolve(RECORDS_DIRECTORY).normalize();
        if (!records.startsWith(root)) {
            throw new CustomerHistoryStorageException(
                    "고객사 히스토리 저장 경로가 올바르지 않습니다.");
        }
        if (Files.exists(records, LinkOption.NOFOLLOW_LINKS)) {
            ensureSafeDirectory(records);
        } else if (create) {
            try {
                Files.createDirectory(records);
            } catch (IOException exception) {
                throw storageFailure("고객사 히스토리 저장 경로를 만들 수 없습니다.", exception);
            }
            ensureSafeDirectory(records);
        }
        return records;
    }

    private Path recordPath(String id, boolean createDirectory) {
        String normalizedId = normalizeId(id);
        Path records = recordsDirectory(createDirectory);
        Path path = records.resolve(normalizedId + FILE_SUFFIX).normalize();
        if (!path.startsWith(records)) {
            throw new IllegalArgumentException("히스토리 식별자가 올바르지 않습니다.");
        }
        return path;
    }

    private static void ensureSafeDirectory(Path directory) {
        if (Files.isSymbolicLink(directory)
                || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new CustomerHistoryStorageException(
                    "고객사 히스토리 저장 경로가 안전하지 않습니다.");
        }
    }

    private static void ensureSafeRecordFile(Path path) {
        if (Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new CustomerHistoryStorageException(
                    "고객사 히스토리 저장소에 안전하지 않은 파일이 있습니다.");
        }
    }

    private static Properties toProperties(CustomerHistoryRecord record) {
        Properties properties = new Properties();
        properties.setProperty("version", FORMAT_VERSION);
        properties.setProperty("id", record.getId());
        properties.setProperty("customerName", record.getCustomerName());
        properties.setProperty("workDate", record.getWorkDate().toString());
        properties.setProperty("category", record.getCategory().getCode());
        properties.setProperty("title", record.getTitle());
        properties.setProperty("actionSummary", record.getActionSummary());
        properties.setProperty("status", record.getStatus().getCode());
        properties.setProperty("creatorUserId", record.getCreatorUserId());
        properties.setProperty("creatorName", record.getCreatorName());
        properties.setProperty("createdAt", record.getCreatedAt().toString());
        properties.setProperty("updatedAt", record.getUpdatedAt().toString());
        return properties;
    }

    private static void moveAtomically(Path source, Path target, boolean replace)
            throws IOException {
        StandardCopyOption[] atomicOptions = replace
                ? new StandardCopyOption[] {
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                }
                : new StandardCopyOption[] {StandardCopyOption.ATOMIC_MOVE};
        StandardCopyOption[] fallbackOptions = replace
                ? new StandardCopyOption[] {StandardCopyOption.REPLACE_EXISTING}
                : new StandardCopyOption[0];
        try {
            Files.move(source, target, atomicOptions);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, fallbackOptions);
        }
    }

    private static boolean matchesQuery(
            CustomerHistoryRecord record, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        return record.getTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || record.getActionSummary().toLowerCase(Locale.ROOT)
                        .contains(normalizedQuery);
    }

    private static String normalizeQuery(String value) {
        String normalized = normalizeOptional(value).toLowerCase(Locale.ROOT);
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("검색어는 100자를 넘을 수 없습니다.");
        }
        return normalized;
    }

    private static CustomerHistoryCategory normalizeCategory(String value) {
        String normalized = normalizeOptional(value);
        return normalized.isEmpty() || "all".equalsIgnoreCase(normalized)
                ? null
                : CustomerHistoryCategory.fromCode(normalized);
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.strip();
    }

    private static String normalizeId(String id) {
        try {
            UUID parsed = UUID.fromString(id == null ? "" : id.trim());
            String normalized = parsed.toString();
            if (!normalized.equals(id == null ? "" : id.trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("히스토리 식별자가 올바르지 않습니다.");
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("히스토리 식별자가 올바르지 않습니다.");
        }
    }

    private static String requiredIdentity(String value) {
        String normalized = normalizeOptional(value);
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new IllegalArgumentException("사용자 식별자가 올바르지 않습니다.");
        }
        return normalized;
    }

    private static String displayName(String value, String fallback) {
        String normalized = normalizeOptional(value);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized.length() > 200 ? normalized.substring(0, 200) : normalized;
    }

    private static String required(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isEmpty()) {
            throw new CustomerHistoryStorageException(
                    "고객사 히스토리 필수 값이 없습니다.");
        }
        return value;
    }

    private static CustomerHistoryStorageException storageFailure(
            String message, Throwable cause) {
        return new CustomerHistoryStorageException(message, cause);
    }

    public enum MutationResult {
        UPDATED,
        DELETED,
        NOT_FOUND,
        FORBIDDEN
    }
}
