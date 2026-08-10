package com.company.filerepo;

import com.company.filerepo.FileRepositoryFilePolicy.ValidatedFile;
import com.company.filerepo.FileRepositoryPathPolicy.ResolvedDirectory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileRepositoryService {
    private static final Logger logger = LoggerFactory.getLogger(FileRepositoryService.class);
    private static final String DATA_SUFFIX = ".data";
    private static final String META_SUFFIX = ".meta";
    private static final int MAX_METADATA_BYTES = 8 * 1024;
    static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_CURSOR_BYTES = 2048;
    private static final int MAX_CACHED_DIRECTORIES = 32;
    private static final int MAX_CACHED_ENTRIES = 50_000;
    private static final long MAX_CACHE_AGE_NANOS =
            Duration.ofSeconds(60).toNanos();
    private static final Pattern METADATA_FILE = Pattern.compile("^\\.frog2-([0-9a-f]{32})\\.meta$");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm", Locale.KOREA)
            .withZone(ZoneId.systemDefault());

    private final FileRepositoryPathPolicy paths;
    private final FileRepositoryFilePolicy files = new FileRepositoryFilePolicy();
    private static final Object SNAPSHOT_CACHE_LOCK = new Object();
    private static final LinkedHashMap<Path, CachedDirectorySnapshot>
            SNAPSHOT_CACHE =
            new LinkedHashMap<>(16, 0.75f, true);
    private final AtomicLong snapshotScanCount = new AtomicLong();
    private static int cachedEntryCount;

    public FileRepositoryService(Path repositoryRoot) throws IOException {
        paths = new FileRepositoryPathPolicy(repositoryRoot);
    }

    public FileRepositoryListing list(String rawPath) throws FileRepositoryException {
        return list(rawPath, null);
    }

    public FileRepositoryListing list(String rawPath, String rawCursor)
            throws FileRepositoryException {
        return list(rawPath, rawCursor, DEFAULT_PAGE_SIZE);
    }

    FileRepositoryListing list(String rawPath, String rawCursor, int pageSize)
            throws FileRepositoryException {
        if (pageSize <= 0 || pageSize > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        ResolvedDirectory directory = paths.resolveExistingDirectory(rawPath);
        SortKey cursor = decodeCursor(rawCursor);
        DirectorySnapshot snapshot = directorySnapshot(directory);
        List<Candidate> candidates = snapshot.candidates();
        int startIndex = firstCandidateAfter(candidates, cursor);
        int endIndex = Math.min(startIndex + pageSize, candidates.size());
        List<Candidate> visible = candidates.subList(startIndex, endIndex);
        boolean hasNext = endIndex < candidates.size();
        List<FileRepositoryEntry> entries = visible.stream()
                .map(Candidate::entry)
                .toList();
        String nextCursor = hasNext && !visible.isEmpty()
                ? encodeCursor(visible.getLast().key())
                : null;
        return new FileRepositoryListing(
                directory.relativePath(),
                parentPath(directory.relativePath()),
                breadcrumbs(directory.relativePath()),
                entries,
                snapshot.directoryCount(),
                snapshot.fileCount(),
                formatSize(snapshot.totalSize()),
                nextCursor,
                hasNext,
                pageSize);
    }

    private DirectorySnapshot directorySnapshot(ResolvedDirectory directory)
            throws FileRepositoryException {
        Path path = directory.path();
        FileTime modified = lastModified(path);
        DirectorySnapshot cached = cachedSnapshot(path, modified);
        if (cached != null) {
            return cached;
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            FileTime before = lastModified(path);
            DirectorySnapshot loaded = scanDirectory(directory);
            FileTime after = lastModified(path);
            if (before.equals(after)) {
                cacheSnapshot(path, after, loaded);
                return loaded;
            }
        }
        return scanDirectory(directory);
    }

    private DirectorySnapshot scanDirectory(ResolvedDirectory directory)
            throws FileRepositoryException {
        snapshotScanCount.incrementAndGet();
        List<Candidate> candidates = new ArrayList<>();
        int directoryCount = 0;
        int fileCount = 0;
        long totalSize = 0;
        try (var children = Files.list(directory.path())) {
            var iterator = children.iterator();
            while (iterator.hasNext()) {
                Path child = iterator.next();
                FileRepositoryEntry entry = entryFor(directory, child);
                if (entry == null) {
                    continue;
                }
                if (entry.isDirectory()) {
                    directoryCount++;
                } else {
                    fileCount++;
                    totalSize += entry.getSize();
                }
                candidates.add(new Candidate(sortKey(entry), entry));
            }
        } catch (IOException e) {
            throw new FileRepositoryException(500, "repository_io_error", "Unable to list repository directory", e);
        }
        candidates.sort(Comparator.comparing(Candidate::key));
        return new DirectorySnapshot(
                List.copyOf(candidates),
                directoryCount,
                fileCount,
                totalSize);
    }

    private static int firstCandidateAfter(
            List<Candidate> candidates, SortKey cursor) {
        if (cursor == null) {
            return 0;
        }
        int low = 0;
        int high = candidates.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (candidates.get(middle).key().compareTo(cursor) <= 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private static FileTime lastModified(Path path)
            throws FileRepositoryException {
        try {
            return Files.getLastModifiedTime(
                    path, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException exception) {
            throw new FileRepositoryException(
                    500,
                    "repository_io_error",
                    "Unable to inspect repository directory",
                    exception);
        }
    }

    private DirectorySnapshot cachedSnapshot(Path path, FileTime modified) {
        synchronized (SNAPSHOT_CACHE_LOCK) {
            CachedDirectorySnapshot cached = SNAPSHOT_CACHE.get(path);
            boolean fresh = cached != null
                    && System.nanoTime() - cached.loadedAtNanos()
                            <= MAX_CACHE_AGE_NANOS;
            return fresh && cached.modified().equals(modified)
                    ? cached.snapshot()
                    : null;
        }
    }

    private void cacheSnapshot(
            Path path, FileTime modified, DirectorySnapshot snapshot) {
        int entryCount = snapshot.candidates().size();
        if (entryCount > MAX_CACHED_ENTRIES) {
            return;
        }
        synchronized (SNAPSHOT_CACHE_LOCK) {
            CachedDirectorySnapshot previous = SNAPSHOT_CACHE.remove(path);
            if (previous != null) {
                cachedEntryCount -= previous.snapshot().candidates().size();
            }
            while (!SNAPSHOT_CACHE.isEmpty()
                    && (SNAPSHOT_CACHE.size() >= MAX_CACHED_DIRECTORIES
                            || cachedEntryCount + entryCount
                                    > MAX_CACHED_ENTRIES)) {
                Path eldest = SNAPSHOT_CACHE.keySet().iterator().next();
                CachedDirectorySnapshot removed = SNAPSHOT_CACHE.remove(eldest);
                cachedEntryCount -= removed.snapshot().candidates().size();
            }
            SNAPSHOT_CACHE.put(
                    path,
                    new CachedDirectorySnapshot(
                            modified, System.nanoTime(), snapshot));
            cachedEntryCount += entryCount;
        }
    }

    private static void invalidateSnapshot(Path path) {
        synchronized (SNAPSHOT_CACHE_LOCK) {
            CachedDirectorySnapshot removed = SNAPSHOT_CACHE.remove(path);
            if (removed != null) {
                cachedEntryCount -= removed.snapshot().candidates().size();
            }
        }
    }

    long snapshotScanCount() {
        return snapshotScanCount.get();
    }

    public ValidatedFile validateUpload(String submittedName, String contentType, long size)
            throws FileRepositoryException {
        return files.validate(submittedName, contentType, size);
    }

    public StoredFile store(String rawPath, ValidatedFile validated, long declaredSize, InputStream input)
            throws FileRepositoryException {
        ResolvedDirectory directory = paths.resolveExistingDirectory(rawPath);
        String storageId = UUID.randomUUID().toString().replace("-", "");
        Path dataPath = paths.managedPathForWrite(directory, storageId, DATA_SUFFIX);
        Path metadataPath = paths.managedPathForWrite(directory, storageId, META_SUFFIX);
        Path uploadTemp = null;
        Path metadataTemp = null;
        boolean dataMoved = false;
        try {
            uploadTemp = Files.createTempFile(directory.path(), ".frog2-upload-", ".tmp");
            CopyResult copied = copyWithLimit(input, uploadTemp);
            if (copied.size() != declaredSize) {
                throw new FileRepositoryException(400, "size_mismatch", "Uploaded file size did not match request metadata");
            }
            files.validateContent(copied.prefix());
            atomicMove(uploadTemp, dataPath);
            uploadTemp = null;
            dataMoved = true;

            Properties metadata = new Properties();
            metadata.setProperty("id", storageId);
            metadata.setProperty("originalName", validated.originalName());
            metadata.setProperty("contentType", validated.contentType());
            metadata.setProperty("size", Long.toString(copied.size()));
            metadata.setProperty("uploadedAt", Instant.now().toString());
            metadataTemp = Files.createTempFile(directory.path(), ".frog2-meta-", ".tmp");
            try (OutputStream output = Files.newOutputStream(metadataTemp)) {
                metadata.store(output, "frog2 file metadata");
            }
            atomicMove(metadataTemp, metadataPath);
            metadataTemp = null;
            invalidateSnapshot(directory.path());
            return new StoredFile(directory.relativePath(), storageId, validated.originalName(), copied.size());
        } catch (FileRepositoryException e) {
            cleanup(uploadTemp, metadataTemp, dataMoved ? dataPath : null, metadataPath);
            throw e;
        } catch (IOException e) {
            cleanup(uploadTemp, metadataTemp, dataMoved ? dataPath : null, metadataPath);
            throw new FileRepositoryException(500, "repository_io_error", "Unable to store uploaded file", e);
        }
    }

    public DownloadFile openDownload(String rawPath, String storageId) throws FileRepositoryException {
        ResolvedDirectory directory = paths.resolveExistingDirectory(rawPath);
        Path metadataPath = paths.resolveManagedFile(directory, storageId, META_SUFFIX);
        Path dataPath = paths.resolveManagedFile(directory, storageId, DATA_SUFFIX);
        StoredMetadata metadata = readMetadata(metadataPath, dataPath, storageId);
        return new DownloadFile(dataPath, metadata.originalName(), metadata.contentType(), metadata.size());
    }

    public void rollback(StoredFile storedFile) {
        try {
            ResolvedDirectory directory = paths.resolveExistingDirectory(storedFile.relativePath());
            Files.deleteIfExists(paths.managedPathForWrite(directory, storedFile.id(), META_SUFFIX));
            Files.deleteIfExists(paths.managedPathForWrite(directory, storedFile.id(), DATA_SUFFIX));
            invalidateSnapshot(directory.path());
        } catch (Exception e) {
            logger.warn("Unable to roll back newly stored repository file id={}", storedFile.id());
        }
    }

    private FileRepositoryEntry entryFor(
            ResolvedDirectory directory, Path child) {
        try {
            if (Files.isSymbolicLink(child)) {
                return null;
            }
            String serverName = child.getFileName().toString();
            if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS) && !serverName.startsWith(".")) {
                String childPath = directory.relativePath().isEmpty()
                        ? serverName
                        : directory.relativePath() + "/" + serverName;
                childPath = FileRepositoryPathPolicy.normalizeRelativePath(childPath);
                return new FileRepositoryEntry(
                        true, null, serverName, childPath,
                        DATE_FORMAT.format(Files.getLastModifiedTime(child).toInstant()),
                        0, "-", "📁", "폴더");
            }
            Matcher matcher = METADATA_FILE.matcher(serverName);
            if (!matcher.matches()) {
                return null;
            }
            String storageId = matcher.group(1);
            Path dataPath = paths.resolveManagedFile(directory, storageId, DATA_SUFFIX);
            StoredMetadata metadata = readMetadata(child, dataPath, storageId);
            return fileEntry(directory.relativePath(), storageId, dataPath, metadata);
        } catch (Exception e) {
            logger.warn("Skipping invalid repository entry {}", child.getFileName());
            return null;
        }
    }

    private static SortKey sortKey(FileRepositoryEntry entry) {
        return new SortKey(
                entry.isDirectory() ? 0 : 1,
                entry.getName().toLowerCase(Locale.ROOT),
                entry.getName(),
                entry.isDirectory() ? entry.getPath() : entry.getId());
    }

    private static String encodeCursor(SortKey key) {
        String value = key.kind() + "\u0000" + key.foldedName() + "\u0000"
                + key.name() + "\u0000" + key.uniqueId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static SortKey decodeCursor(String rawCursor)
            throws FileRepositoryException {
        if (rawCursor == null || rawCursor.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(rawCursor.trim());
            if (decoded.length == 0 || decoded.length > MAX_CURSOR_BYTES) {
                throw new IllegalArgumentException();
            }
            String[] parts = new String(decoded, StandardCharsets.UTF_8)
                    .split("\u0000", -1);
            if (parts.length != 4) {
                throw new IllegalArgumentException();
            }
            int kind = Integer.parseInt(parts[0]);
            if ((kind != 0 && kind != 1)
                    || parts[1].isEmpty()
                    || parts[2].isEmpty()
                    || parts[3].isEmpty()) {
                throw new IllegalArgumentException();
            }
            return new SortKey(kind, parts[1], parts[2], parts[3]);
        } catch (IllegalArgumentException exception) {
            throw new FileRepositoryException(
                    400, "invalid_cursor", "Repository cursor is invalid");
        }
    }

    private FileRepositoryEntry fileEntry(
            String relativePath, String storageId, Path dataPath, StoredMetadata metadata) throws IOException {
        String extension = extension(metadata.originalName());
        String icon = "📄";
        String description = "파일";
        if (SetGroups.IMAGES.contains(extension)) {
            icon = "🖼️";
            description = "이미지 파일";
        } else if ("pdf".equals(extension)) {
            icon = "📋";
            description = "PDF 문서";
        } else if (SetGroups.DOCUMENTS.contains(extension)) {
            icon = "📝";
            description = "문서 파일";
        } else if (SetGroups.SPREADSHEETS.contains(extension)) {
            icon = "📊";
            description = "스프레드시트";
        } else if (SetGroups.ARCHIVES.contains(extension)) {
            icon = "📦";
            description = "압축 파일";
        } else if (SetGroups.TEXT.contains(extension)) {
            icon = "📃";
            description = "텍스트 파일";
        }
        return new FileRepositoryEntry(
                false,
                storageId,
                metadata.originalName(),
                relativePath,
                DATE_FORMAT.format(Files.getLastModifiedTime(dataPath).toInstant()),
                metadata.size(),
                formatSize(metadata.size()),
                icon,
                description);
    }

    private StoredMetadata readMetadata(Path metadataPath, Path dataPath, String expectedId)
            throws FileRepositoryException {
        try (InputStream input = Files.newInputStream(metadataPath)) {
            byte[] encodedMetadata = input.readNBytes(MAX_METADATA_BYTES + 1);
            if (encodedMetadata.length > MAX_METADATA_BYTES) {
                throw invalidMetadata();
            }
            Properties metadata = new Properties();
            metadata.load(new ByteArrayInputStream(encodedMetadata));
            if (!expectedId.equals(metadata.getProperty("id"))) {
                throw invalidMetadata();
            }
            long storedSize = parseStoredSize(metadata.getProperty("size"));
            long actualSize = Files.size(dataPath);
            if (storedSize != actualSize) {
                throw invalidMetadata();
            }
            ValidatedFile validated = files.validate(
                    metadata.getProperty("originalName"),
                    metadata.getProperty("contentType"),
                    storedSize);
            return new StoredMetadata(validated.originalName(), validated.contentType(), storedSize);
        } catch (FileRepositoryException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw invalidMetadata(e);
        }
    }

    private static long parseStoredSize(String rawSize) throws FileRepositoryException {
        if (rawSize == null || rawSize.isBlank() || !rawSize.chars().allMatch(Character::isDigit)) {
            throw invalidMetadata();
        }
        try {
            long size = Long.parseLong(rawSize);
            if (size <= 0) {
                throw invalidMetadata();
            }
            return size;
        } catch (NumberFormatException e) {
            throw invalidMetadata(e);
        }
    }

    private static FileRepositoryException invalidMetadata() {
        return new FileRepositoryException(500, "invalid_metadata", "Stored file metadata is invalid");
    }

    private static FileRepositoryException invalidMetadata(Throwable cause) {
        return new FileRepositoryException(500, "invalid_metadata", "Stored file metadata is invalid", cause);
    }

    private static CopyResult copyWithLimit(InputStream input, Path outputPath)
            throws IOException, FileRepositoryException {
        ByteArrayOutputStream prefix = new ByteArrayOutputStream(4096);
        long total = 0;
        byte[] buffer = new byte[8192];
        try (OutputStream output = Files.newOutputStream(outputPath)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > FileRepositoryFilePolicy.MAX_FILE_SIZE) {
                    throw new FileRepositoryException(413, "file_too_large", "A file exceeds the 10 MB limit");
                }
                if (prefix.size() < 4096) {
                    prefix.write(buffer, 0, Math.min(read, 4096 - prefix.size()));
                }
                output.write(buffer, 0, read);
            }
        }
        return new CopyResult(total, prefix.toByteArray());
    }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    private static void cleanup(Path... paths) {
        for (Path path : paths) {
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best-effort cleanup of files created by the failed request.
                }
            }
        }
    }

    private static String parentPath(String relativePath) {
        int separator = relativePath.lastIndexOf('/');
        return separator < 0 ? "" : relativePath.substring(0, separator);
    }

    private static List<FileRepositoryListing.Breadcrumb> breadcrumbs(String relativePath) {
        List<FileRepositoryListing.Breadcrumb> result = new ArrayList<>();
        if (relativePath.isEmpty()) {
            return result;
        }
        String current = "";
        for (String segment : relativePath.split("/")) {
            current = current.isEmpty() ? segment : current + "/" + segment;
            result.add(new FileRepositoryListing.Breadcrumb(segment, current));
        }
        return result;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = { "KB", "MB", "GB", "TB" };
        double value = bytes;
        int unit = -1;
        do {
            value /= 1024.0;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }

    private record CopyResult(long size, byte[] prefix) {
    }

    private record StoredMetadata(String originalName, String contentType, long size) {
    }

    private record Candidate(SortKey key, FileRepositoryEntry entry) {
    }

    private record DirectorySnapshot(
            List<Candidate> candidates,
            int directoryCount,
            int fileCount,
            long totalSize) {
    }

    private record CachedDirectorySnapshot(
            FileTime modified,
            long loadedAtNanos,
            DirectorySnapshot snapshot) {
    }

    private record SortKey(
            int kind, String foldedName, String name, String uniqueId)
            implements Comparable<SortKey> {
        @Override
        public int compareTo(SortKey other) {
            int comparison = Integer.compare(kind, other.kind);
            if (comparison == 0) {
                comparison = foldedName.compareTo(other.foldedName);
            }
            if (comparison == 0) {
                comparison = name.compareTo(other.name);
            }
            return comparison == 0
                    ? uniqueId.compareTo(other.uniqueId)
                    : comparison;
        }
    }

    private static final class SetGroups {
        private static final java.util.Set<String> IMAGES = java.util.Set.of("jpg", "jpeg", "png", "gif");
        private static final java.util.Set<String> DOCUMENTS = java.util.Set.of("doc", "docx", "ppt", "pptx");
        private static final java.util.Set<String> SPREADSHEETS = java.util.Set.of("xls", "xlsx", "csv");
        private static final java.util.Set<String> ARCHIVES = java.util.Set.of("zip", "7z", "rar", "gz", "tar");
        private static final java.util.Set<String> TEXT = java.util.Set.of("txt", "log");

        private SetGroups() {
        }
    }

    public record StoredFile(String relativePath, String id, String originalName, long size) {
    }

    public record DownloadFile(Path path, String originalName, String contentType, long size) {
    }
}
