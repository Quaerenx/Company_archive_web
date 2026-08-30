package com.company.filerepo;

import com.company.filerepo.FileRepositoryCursorCodec.SortKey;
import com.company.filerepo.FileRepositoryFilePolicy.ValidatedFile;
import com.company.filerepo.FileRepositoryPathPolicy.ResolvedDirectory;
import com.company.performance.RequestPerformanceContext;
import com.company.util.SearchQueryPolicy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileRepositoryService {
    private static final Logger logger = LoggerFactory.getLogger(FileRepositoryService.class);
    private static final String DATA_SUFFIX = ".data";
    private static final String META_SUFFIX = ".meta";
    private static final String QUARANTINE_DIRECTORY = ".frog2-quarantine";
    private static final int MAX_METADATA_BYTES = 8 * 1024;
    static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_CACHED_DIRECTORIES = 32;
    private static final int MAX_CACHED_ENTRIES = 50_000;
    private static final int MAX_SEARCHED_ENTRIES = 50_000;
    private static final int MAX_SNAPSHOT_LOAD_ATTEMPTS = 3;
    private static final long MAX_CACHE_AGE_NANOS =
            Duration.ofSeconds(60).toNanos();
    private static final Duration INTERRUPTED_UPLOAD_GRACE =
            Duration.ofHours(1);
    private static final Pattern METADATA_FILE = Pattern.compile("^\\.frog2-([0-9a-f]{32})\\.meta$");
    private static final Pattern MANAGED_FILE = Pattern.compile(
            "^\\.frog2-([0-9a-f]{32})\\.(data|meta)$");
    private static final Pattern UPLOAD_TEMP_FILE = Pattern.compile(
            "^\\.frog2-(?:upload|meta)-.+\\.tmp$");
    private static final Pattern POTENTIAL_MANAGED_FILE = Pattern.compile(
            "^\\.frog2-.+\\.(?:data|meta)$");
    private final FileRepositoryPathPolicy paths;
    private final FileRepositoryFilePolicy files = new FileRepositoryFilePolicy();
    private static final Object SNAPSHOT_CACHE_LOCK = new Object();
    private static final LinkedHashMap<Path, CachedDirectorySnapshot>
            SNAPSHOT_CACHE =
            new LinkedHashMap<>(16, 0.75f, true);
    private static final ConcurrentHashMap<Path, SnapshotLoad>
            SNAPSHOT_LOADS = new ConcurrentHashMap<>();
    private final AtomicLong snapshotScanCount = new AtomicLong();
    private final SnapshotScanObserver snapshotScanObserver;
    private static int cachedEntryCount;

    public FileRepositoryService(Path repositoryRoot) throws IOException {
        this(repositoryRoot, path -> { });
    }

    FileRepositoryService(
            Path repositoryRoot,
            SnapshotScanObserver snapshotScanObserver) throws IOException {
        paths = new FileRepositoryPathPolicy(repositoryRoot);
        this.snapshotScanObserver = Objects.requireNonNull(
                snapshotScanObserver, "snapshotScanObserver");
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
        SortKey cursor = FileRepositoryCursorCodec.decode(rawCursor);
        DirectorySnapshot snapshot = directorySnapshot(directory);
        List<Candidate> candidates = snapshot.candidates();
        int startIndex = firstCandidateAfter(candidates, cursor);
        if (cursor != null && startIndex >= candidates.size()
                && !candidates.isEmpty()) {
            startIndex = ((candidates.size() - 1) / pageSize) * pageSize;
        }
        int endIndex = Math.min(startIndex + pageSize, candidates.size());
        List<Candidate> visible = candidates.subList(startIndex, endIndex);
        boolean hasPrevious = startIndex > 0;
        boolean hasNext = endIndex < candidates.size();
        List<FileRepositoryEntry> entries = visible.stream()
                .map(Candidate::entry)
                .toList();
        String nextCursor = hasNext && !visible.isEmpty()
                ? FileRepositoryCursorCodec.encode(visible.getLast().key())
                : null;
        int previousStartIndex = Math.max(0, startIndex - pageSize);
        String previousCursor = hasPrevious && previousStartIndex > 0
                ? FileRepositoryCursorCodec.encode(
                        candidates.get(previousStartIndex - 1).key())
                : null;
        int totalCount = candidates.size();
        int totalPages = Math.max(1, (totalCount + pageSize - 1) / pageSize);
        int currentPage = Math.min(totalPages, (startIndex / pageSize) + 1);
        return new FileRepositoryListing(
                directory.relativePath(),
                parentPath(directory.relativePath()),
                breadcrumbs(directory.relativePath()),
                entries,
                snapshot.directoryCount(),
                snapshot.fileCount(),
                snapshot.invalidEntryCount(),
                FileRepositoryPresentation.formatSize(snapshot.totalSize()),
                previousCursor,
                nextCursor,
                hasPrevious,
                hasNext,
                currentPage,
                totalPages,
                totalCount,
                pageSize);
    }

    public List<FileRepositoryEntry> search(String query, int limit)
            throws FileRepositoryException {
        if (limit <= 0 || limit > 20) {
            throw new IllegalArgumentException(
                    "Search limit must be between 1 and 20");
        }
        String normalizedQuery = SearchQueryPolicy.normalize(query);
        if (normalizedQuery == null) {
            return List.of();
        }
        String needle = normalizedQuery.toLowerCase(Locale.ROOT);
        List<FileRepositoryEntry> matches = new ArrayList<>(limit);
        ArrayDeque<String> directories = new ArrayDeque<>();
        directories.add("");
        int inspectedEntries = 0;

        while (!directories.isEmpty()
                && matches.size() < limit
                && inspectedEntries < MAX_SEARCHED_ENTRIES) {
            ResolvedDirectory directory = paths.resolveExistingDirectory(
                    directories.removeFirst());
            for (Candidate candidate : directorySnapshot(directory).candidates()) {
                FileRepositoryEntry entry = candidate.entry();
                inspectedEntries++;
                if (entry.isDirectory()) {
                    directories.addLast(entry.getPath());
                }
                if (matchesSearch(entry, needle)) {
                    matches.add(entry);
                    if (matches.size() == limit) {
                        break;
                    }
                }
                if (inspectedEntries == MAX_SEARCHED_ENTRIES) {
                    break;
                }
            }
        }
        return List.copyOf(matches);
    }

    private static boolean matchesSearch(
            FileRepositoryEntry entry, String needle) {
        return entry.getName().toLowerCase(Locale.ROOT).contains(needle)
                || entry.getDescription().toLowerCase(Locale.ROOT)
                        .contains(needle)
                || (!entry.isDirectory()
                        && entry.getPath().toLowerCase(Locale.ROOT)
                                .contains(needle));
    }

    private DirectorySnapshot directorySnapshot(ResolvedDirectory directory)
            throws FileRepositoryException {
        Path path = directory.path();
        FileTime modified = lastModified(path);
        DirectorySnapshot cached = cachedSnapshot(path, modified);
        if (cached != null) {
            RequestPerformanceContext.recordFileSnapshotCacheHit();
            return cached;
        }
        RequestPerformanceContext.recordFileSnapshotCacheMiss();

        SnapshotLoad proposed = new SnapshotLoad();
        SnapshotLoad active = SNAPSHOT_LOADS.putIfAbsent(path, proposed);
        if (active != null) {
            return awaitSnapshot(active);
        }

        try {
            DirectorySnapshot racedCache = cachedSnapshot(
                    path, lastModified(path));
            if (racedCache != null
                    && completeCachedSnapshotIfCurrent(
                            path, proposed, racedCache)) {
                return racedCache;
            }
            return loadAndCompleteSnapshot(directory, proposed);
        } catch (FileRepositoryException | RuntimeException | Error exception) {
            proposed.future().completeExceptionally(exception);
            throw exception;
        } finally {
            SNAPSHOT_LOADS.remove(path, proposed);
        }
    }

    private static boolean completeCachedSnapshotIfCurrent(
            Path path,
            SnapshotLoad load,
            DirectorySnapshot snapshot) {
        synchronized (load) {
            if (SNAPSHOT_LOADS.get(path) != load) {
                return false;
            }
            load.future().complete(snapshot);
            return true;
        }
    }

    private DirectorySnapshot loadAndCompleteSnapshot(
            ResolvedDirectory directory,
            SnapshotLoad load) throws FileRepositoryException {
        for (int attempt = 0;
                attempt < MAX_SNAPSHOT_LOAD_ATTEMPTS;
                attempt++) {
            long generation = load.generation();
            LoadedDirectorySnapshot loaded = loadStableSnapshot(directory);
            synchronized (load) {
                boolean generationMatches =
                        load.generation() == generation;
                boolean ownsLoad =
                        SNAPSHOT_LOADS.get(directory.path()) == load;
                if (generationMatches && ownsLoad && loaded.cacheable()) {
                    cacheSnapshot(
                            directory.path(),
                            loaded.modified(),
                            loaded.snapshot());
                }
                if (generationMatches
                        || attempt == MAX_SNAPSHOT_LOAD_ATTEMPTS - 1) {
                    load.future().complete(loaded.snapshot());
                    return loaded.snapshot();
                }
            }
        }
        throw new IllegalStateException("Snapshot load attempts exhausted");
    }

    private LoadedDirectorySnapshot loadStableSnapshot(
            ResolvedDirectory directory) throws FileRepositoryException {
        for (int attempt = 0; attempt < 2; attempt++) {
            FileTime before = lastModified(directory.path());
            DirectorySnapshot loaded = scanDirectory(directory);
            FileTime after = lastModified(directory.path());
            if (before.equals(after)) {
                return new LoadedDirectorySnapshot(after, loaded, true);
            }
        }
        return new LoadedDirectorySnapshot(
                null, scanDirectory(directory), false);
    }

    private static DirectorySnapshot awaitSnapshot(SnapshotLoad load)
            throws FileRepositoryException {
        try {
            return load.future().get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FileRepositoryException(
                    500,
                    "repository_io_error",
                    "Repository directory scan was interrupted",
                    exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof FileRepositoryException repositoryException) {
                throw repositoryException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new FileRepositoryException(
                    500,
                    "repository_io_error",
                    "Unable to list repository directory",
                    cause);
        }
    }

    private DirectorySnapshot scanDirectory(ResolvedDirectory directory)
            throws FileRepositoryException {
        long started = System.nanoTime();
        snapshotScanCount.incrementAndGet();
        try {
            snapshotScanObserver.beforeScan(directory.path());
            List<Candidate> candidates = new ArrayList<>();
            int directoryCount = 0;
            int fileCount = 0;
            long totalSize = 0;
            Set<String> invalidManagedEntries = new HashSet<>();
            try (var children = Files.list(directory.path())) {
                var iterator = children.iterator();
                while (iterator.hasNext()) {
                    Path child = iterator.next();
                    EntryInspection inspection = inspectEntry(directory, child);
                    if (inspection.invalidManagedKey() != null) {
                        invalidManagedEntries.add(
                                inspection.invalidManagedKey());
                    }
                    FileRepositoryEntry entry = inspection.entry();
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
                throw new FileRepositoryException(
                        500,
                        "repository_io_error",
                        "Unable to list repository directory",
                        e);
            }
            candidates.sort(Comparator.comparing(Candidate::key));
            if (!invalidManagedEntries.isEmpty()) {
                logger.warn(
                        "File repository directory contains {} invalid managed entries",
                        invalidManagedEntries.size());
            }
            return new DirectorySnapshot(
                    List.copyOf(candidates),
                    directoryCount,
                    fileCount,
                    totalSize,
                    invalidManagedEntries.size());
        } finally {
            RequestPerformanceContext.recordFileSnapshotScan(
                    Math.max(0, System.nanoTime() - started));
        }
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

    private static void cacheSnapshot(
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

    static void invalidateSnapshot(Path path) {
        SnapshotLoad active = SNAPSHOT_LOADS.get(path);
        if (active != null) {
            synchronized (active) {
                if (SNAPSHOT_LOADS.get(path) != active) {
                    removeCachedSnapshot(path);
                    return;
                }
                active.invalidate();
                removeCachedSnapshot(path);
                SNAPSHOT_LOADS.remove(path, active);
            }
            return;
        }
        removeCachedSnapshot(path);
    }

    private static void removeCachedSnapshot(Path path) {
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

    /**
     * Converts ordinary files copied into the repository by an administrator to
     * the same opaque data/metadata pair used by browser uploads. The selected
     * directory and its safe, visible descendants are processed recursively.
     */
    public ImportResult importUnmanaged(String rawPath)
            throws FileRepositoryException {
        FileRepositoryImporter.Result result = new FileRepositoryImporter(
                paths,
                files,
                this::readMetadata,
                FileRepositoryService::invalidateSnapshot)
                .importUnmanaged(rawPath);
        return new ImportResult(
                result.relativePath(),
                result.importedCount(),
                result.conflictCount(),
                result.rejectedCount(),
                result.deferredCount(),
                result.failedCount());
    }

    public StoredFile store(String rawPath, ValidatedFile validated, long declaredSize, InputStream input)
            throws FileRepositoryException {
        ResolvedDirectory directory = paths.resolveExistingDirectory(rawPath);
        quarantineStaleInterruptedUploads(directory);
        String storageId = UUID.randomUUID().toString().replace("-", "");
        Path dataPath = paths.managedPathForWrite(directory, storageId, DATA_SUFFIX);
        Path metadataPath = paths.managedPathForWrite(directory, storageId, META_SUFFIX);
        Path uploadTemp = null;
        Path metadataTemp = null;
        boolean dataMoved = false;
        try {
            uploadTemp = Files.createTempFile(directory.path(), ".frog2-upload-", ".tmp");
            CopyResult copied = copyWithLimit(input, uploadTemp);
            forceFile(uploadTemp);
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
            forceFile(metadataTemp);
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

    private void quarantineStaleInterruptedUploads(
            ResolvedDirectory directory) throws FileRepositoryException {
        Instant cutoff = Instant.now().minus(INTERRUPTED_UPLOAD_GRACE);
        List<Path> candidates = new ArrayList<>();
        try (var children = Files.list(directory.path())) {
            var iterator = children.iterator();
            while (iterator.hasNext()) {
                Path child = iterator.next();
                if (Files.isSymbolicLink(child)
                        || !Files.isRegularFile(
                                child, LinkOption.NOFOLLOW_LINKS)
                        || !isInterruptedUpload(directory, child)
                        || !Files.getLastModifiedTime(
                                child, LinkOption.NOFOLLOW_LINKS)
                                .toInstant()
                                .isBefore(cutoff)) {
                    continue;
                }
                candidates.add(child);
            }
        } catch (IOException exception) {
            throw new FileRepositoryException(
                    500,
                    "repository_recovery_failed",
                    "Unable to inspect interrupted repository uploads",
                    exception);
        }
        if (candidates.isEmpty()) {
            return;
        }

        Path quarantine = ensureQuarantineDirectory(directory);
        try {
            for (Path candidate : candidates) {
                if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)
                        || !isInterruptedUpload(directory, candidate)
                        || !Files.getLastModifiedTime(
                                candidate, LinkOption.NOFOLLOW_LINKS)
                                .toInstant()
                                .isBefore(cutoff)) {
                    continue;
                }
                String targetName = candidate.getFileName()
                        + "."
                        + UUID.randomUUID().toString().replace("-", "")
                        + ".quarantine";
                atomicMove(candidate, quarantine.resolve(targetName));
                logger.warn(
                        "Quarantined stale interrupted repository file {}",
                        candidate.getFileName());
            }
        } catch (IOException exception) {
            throw new FileRepositoryException(
                    500,
                    "repository_recovery_failed",
                    "Unable to quarantine interrupted repository uploads",
                    exception);
        }
    }

    private boolean isInterruptedUpload(
            ResolvedDirectory directory, Path candidate)
            throws FileRepositoryException {
        String name = candidate.getFileName().toString();
        if (UPLOAD_TEMP_FILE.matcher(name).matches()) {
            return true;
        }
        Matcher managed = MANAGED_FILE.matcher(name);
        if (!managed.matches()) {
            return false;
        }
        String otherSuffix = "data".equals(managed.group(2))
                ? META_SUFFIX
                : DATA_SUFFIX;
        Path companion = paths.managedPathForWrite(
                directory, managed.group(1), otherSuffix);
        return Files.isSymbolicLink(companion)
                || !Files.isRegularFile(
                        companion, LinkOption.NOFOLLOW_LINKS);
    }

    private static Path ensureQuarantineDirectory(
            ResolvedDirectory directory) throws FileRepositoryException {
        Path quarantine = directory.path()
                .resolve(QUARANTINE_DIRECTORY)
                .normalize();
        if (!directory.path().equals(quarantine.getParent())) {
            throw new FileRepositoryException(
                    500,
                    "repository_recovery_failed",
                    "Repository quarantine path is invalid");
        }
        try {
            if (Files.notExists(quarantine, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(quarantine);
            }
            if (Files.isSymbolicLink(quarantine)
                    || !Files.isDirectory(
                            quarantine, LinkOption.NOFOLLOW_LINKS)) {
                throw new FileRepositoryException(
                        500,
                        "repository_recovery_failed",
                        "Repository quarantine path is unsafe");
            }
            return quarantine;
        } catch (FileRepositoryException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new FileRepositoryException(
                    500,
                    "repository_recovery_failed",
                    "Unable to create repository quarantine",
                    exception);
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

    private EntryInspection inspectEntry(
            ResolvedDirectory directory, Path child) {
        String serverName = child.getFileName().toString();
        try {
            if (Files.isSymbolicLink(child)) {
                return new EntryInspection(
                        null, invalidManagedKey(serverName));
            }
            if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS) && !serverName.startsWith(".")) {
                String childPath = directory.relativePath().isEmpty()
                        ? serverName
                        : directory.relativePath() + "/" + serverName;
                childPath = FileRepositoryPathPolicy.normalizeRelativePath(childPath);
                return new EntryInspection(
                        new FileRepositoryEntry(
                                true, null, serverName, childPath,
                                FileRepositoryPresentation.modifiedText(child),
                                0, "-", "📁", "폴더"),
                        null);
            }
            Matcher matcher = METADATA_FILE.matcher(serverName);
            if (!matcher.matches()) {
                Matcher managed = MANAGED_FILE.matcher(serverName);
                if (managed.matches()
                        && "data".equals(managed.group(2))) {
                    Path metadataPath = paths.managedPathForWrite(
                            directory, managed.group(1), META_SUFFIX);
                    if (Files.isSymbolicLink(metadataPath)
                            || !Files.isRegularFile(
                                    metadataPath,
                                    LinkOption.NOFOLLOW_LINKS)) {
                        return new EntryInspection(
                                null, managed.group(1));
                    }
                    return new EntryInspection(null, null);
                }
                return new EntryInspection(
                        null, invalidManagedKey(serverName));
            }
            String storageId = matcher.group(1);
            Path dataPath = paths.resolveManagedFile(directory, storageId, DATA_SUFFIX);
            StoredMetadata metadata = readMetadata(child, dataPath, storageId);
            return new EntryInspection(
                    FileRepositoryPresentation.fileEntry(
                            directory.relativePath(),
                            storageId,
                            dataPath,
                            metadata),
                    null);
        } catch (Exception e) {
            return new EntryInspection(
                    null, invalidManagedKey(serverName));
        }
    }

    private static String invalidManagedKey(String serverName) {
        Matcher managed = MANAGED_FILE.matcher(serverName);
        if (managed.matches()) {
            return managed.group(1);
        }
        return POTENTIAL_MANAGED_FILE.matcher(serverName).matches()
                ? serverName
                : null;
    }

    private static SortKey sortKey(FileRepositoryEntry entry) {
        return new SortKey(
                entry.isDirectory() ? 0 : 1,
                entry.getName().toLowerCase(Locale.ROOT),
                entry.getName(),
                entry.isDirectory() ? entry.getPath() : entry.getId());
    }

    StoredMetadata readMetadata(Path metadataPath, Path dataPath, String expectedId)
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
            boolean serverImported = "server-import".equals(
                    metadata.getProperty("source"));
            ValidatedFile validated = serverImported
                    ? files.validateImportedStored(
                            metadata.getProperty("originalName"),
                            metadata.getProperty("contentType"),
                            storedSize)
                    : files.validateStored(
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

    private static void forceFile(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(
                path, StandardOpenOption.WRITE)) {
            channel.force(true);
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

    private record CopyResult(long size, byte[] prefix) {
    }

    record StoredMetadata(String originalName, String contentType, long size) {
    }

    private record Candidate(SortKey key, FileRepositoryEntry entry) {
    }

    private record EntryInspection(
            FileRepositoryEntry entry, String invalidManagedKey) {
    }

    private record DirectorySnapshot(
            List<Candidate> candidates,
            int directoryCount,
            int fileCount,
            long totalSize,
            int invalidEntryCount) {
    }

    private record CachedDirectorySnapshot(
            FileTime modified,
            long loadedAtNanos,
            DirectorySnapshot snapshot) {
    }

    private record LoadedDirectorySnapshot(
            FileTime modified,
            DirectorySnapshot snapshot,
            boolean cacheable) {
    }

    private static final class SnapshotLoad {
        private final CompletableFuture<DirectorySnapshot> future =
                new CompletableFuture<>();
        private long generation;

        private CompletableFuture<DirectorySnapshot> future() {
            return future;
        }

        private synchronized long generation() {
            return generation;
        }

        private synchronized void invalidate() {
            generation++;
        }
    }

    @FunctionalInterface
    interface SnapshotScanObserver {
        void beforeScan(Path path);
    }

    public record StoredFile(String relativePath, String id, String originalName, long size) {
    }

    public record DownloadFile(Path path, String originalName, String contentType, long size) {
    }

    public record ImportResult(
            String relativePath,
            int importedCount,
            int conflictCount,
            int rejectedCount,
            int deferredCount,
            int failedCount) {
    }
}
