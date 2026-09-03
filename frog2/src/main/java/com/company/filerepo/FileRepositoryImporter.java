package com.company.filerepo;

import com.company.filerepo.FileRepositoryFilePolicy.ValidatedFile;
import com.company.filerepo.FileRepositoryPathPolicy.ResolvedDirectory;
import com.company.filerepo.FileRepositoryService.StoredMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FileRepositoryImporter {
    private static final Logger logger =
            LoggerFactory.getLogger(FileRepositoryImporter.class);
    private static final String DATA_SUFFIX = ".data";
    private static final String META_SUFFIX = ".meta";
    private static final String QUARANTINE_DIRECTORY = ".frog2-quarantine";
    private static final int MAX_IMPORT_ENTRIES = 10_000;
    private static final int MAX_IMPORT_DIRECTORIES = 1_000;
    private static final Duration MINIMUM_STABLE_AGE =
            Duration.ofSeconds(30);
    private static final Pattern METADATA_FILE =
            Pattern.compile("^\\.frog2-([0-9a-f]{32})\\.meta$");
    private static final Pattern MANAGED_FILE = Pattern.compile(
            "^\\.frog2-([0-9a-f]{32})\\.(?:data|meta)$");
    private static final Pattern UPLOAD_TEMP_FILE = Pattern.compile(
            "^\\.frog2-(?:upload|meta)-.+\\.tmp$");
    private static final Object IMPORT_LOCK = new Object();

    private final FileRepositoryPathPolicy paths;
    private final FileRepositoryFilePolicy files;
    private final ManagedMetadataReader metadataReader;
    private final SnapshotInvalidator snapshotInvalidator;
    private final PublishObserver publishObserver;

    FileRepositoryImporter(
            FileRepositoryPathPolicy paths,
            FileRepositoryFilePolicy files,
            ManagedMetadataReader metadataReader,
            SnapshotInvalidator snapshotInvalidator) {
        this(
                paths,
                files,
                metadataReader,
                snapshotInvalidator,
                (source, data) -> { });
    }

    FileRepositoryImporter(
            FileRepositoryPathPolicy paths,
            FileRepositoryFilePolicy files,
            ManagedMetadataReader metadataReader,
            SnapshotInvalidator snapshotInvalidator,
            PublishObserver publishObserver) {
        this.paths = paths;
        this.files = files;
        this.metadataReader = metadataReader;
        this.snapshotInvalidator = snapshotInvalidator;
        this.publishObserver = publishObserver;
    }

    Preview previewUnmanaged(String rawPath) throws FileRepositoryException {
        synchronized (IMPORT_LOCK) {
            ResolvedDirectory rootDirectory =
                    paths.resolveExistingDirectory(rawPath);
            ImportDiscovery discovery = discover(rootDirectory);
            Map<Path, Set<String>> managedNames =
                    loadManagedOriginalNames(discovery.directories());
            List<Item> items = new ArrayList<>(discovery.rejectedItems());
            for (ImportCandidate candidate : discovery.candidates()) {
                items.add(inspectCandidate(
                        candidate,
                        managedNames.get(candidate.directory().path())));
            }
            items.sort(Item.BY_PATH);
            return new Preview(
                    rootDirectory.relativePath(), List.copyOf(items));
        }
    }

    Result importUnmanaged(String rawPath) throws FileRepositoryException {
        return importUnmanaged(rawPath, null);
    }

    Result importUnmanaged(String rawPath, List<String> selectedPaths)
            throws FileRepositoryException {
        synchronized (IMPORT_LOCK) {
            ResolvedDirectory rootDirectory =
                    paths.resolveExistingDirectory(rawPath);
            ImportDiscovery discovery = discover(rootDirectory);
            Map<Path, Set<String>> managedNames =
                    loadManagedOriginalNames(discovery.directories());
            Set<String> selection = normalizeSelection(
                    selectedPaths, discovery.candidates());
            boolean importAll = selectedPaths == null;
            List<Item> items = new ArrayList<>();
            if (importAll) {
                items.addAll(discovery.rejectedItems());
            }

            for (ImportCandidate candidate : discovery.candidates()) {
                if (!importAll && !selection.contains(candidate.relativePath())) {
                    continue;
                }
                Item inspected = inspectCandidate(
                        candidate,
                        managedNames.get(candidate.directory().path()));
                if (inspected.disposition() != ImportDisposition.READY) {
                    items.add(inspected);
                    continue;
                }
                try {
                    ImportDisposition disposition = importCandidate(
                            candidate,
                            managedNames.get(candidate.directory().path()));
                    Item item = item(candidate, disposition);
                    items.add(item);
                    if (disposition == ImportDisposition.IMPORTED) {
                        managedNames.get(candidate.directory().path()).add(
                                candidate.source().getFileName().toString()
                                        .toLowerCase(Locale.ROOT));
                    }
                } catch (IOException exception) {
                    items.add(item(candidate, ImportDisposition.FAILED));
                    logger.error(
                            "Unable to import a server-side repository file");
                }
            }

            items.sort(Item.BY_PATH);
            Result result = new Result(
                    rootDirectory.relativePath(), List.copyOf(items));

            logger.info(
                    "File repository import completed: imported={}, conflicts={}, rejected={}, deferred={}, failed={}",
                    result.importedCount(),
                    result.conflictCount(),
                    result.rejectedCount(),
                    result.deferredCount(),
                    result.failedCount());
            return result;
        }
    }

    private static Set<String> normalizeSelection(
            List<String> selectedPaths,
            List<ImportCandidate> candidates)
            throws FileRepositoryException {
        if (selectedPaths == null) {
            return Set.of();
        }
        Set<String> available = candidates.stream()
                .map(ImportCandidate::relativePath)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> selection = new LinkedHashSet<>();
        for (String selectedPath : selectedPaths) {
            String normalized = selectedPath == null
                    ? "" : selectedPath.strip();
            if (normalized.isEmpty() || !available.contains(normalized)) {
                throw new FileRepositoryException(
                        400,
                        "invalid_import_selection",
                        "Selected server-side file is unavailable");
            }
            selection.add(normalized);
        }
        return Set.copyOf(selection);
    }

    private ImportDiscovery discover(ResolvedDirectory rootDirectory)
            throws FileRepositoryException {
        List<ResolvedDirectory> directories = new ArrayList<>();
        Set<Path> visited = new HashSet<>();
        List<ImportCandidate> candidates = new ArrayList<>();
        List<Item> rejectedItems = new ArrayList<>();
        directories.add(rootDirectory);
        visited.add(rootDirectory.path());

        for (int directoryIndex = 0;
                directoryIndex < directories.size();
                directoryIndex++) {
            ResolvedDirectory directory = directories.get(directoryIndex);
            try (var children = Files.newDirectoryStream(directory.path())) {
                for (Path child : children) {
                    String name = child.getFileName().toString();
                    if (isExpectedManagedEntry(name)
                            || QUARANTINE_DIRECTORY.equals(name)) {
                        continue;
                    }
                    if (name.startsWith(".") || Files.isSymbolicLink(child)) {
                        rejectedItems.add(rejectedItem(
                                directory,
                                name,
                                "숨김 파일 또는 심볼릭 링크는 반입할 수 없습니다."));
                        ensureImportEntryLimit(
                                candidates.size() + rejectedItems.size());
                        continue;
                    }
                    if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                        String childPath = directory.relativePath().isEmpty()
                                ? name
                                : directory.relativePath() + "/" + name;
                        try {
                            ResolvedDirectory resolved =
                                    paths.resolveExistingDirectory(childPath);
                            if (visited.add(resolved.path())) {
                                directories.add(resolved);
                                if (directories.size()
                                        > MAX_IMPORT_DIRECTORIES) {
                                    throw new FileRepositoryException(
                                            413,
                                            "too_many_import_directories",
                                            "At most 1000 repository directories may be imported at once");
                                }
                            }
                        } catch (FileRepositoryException exception) {
                            if (exception.getHttpStatus() == 413
                                    || exception.getHttpStatus() >= 500) {
                                throw exception;
                            }
                            rejectedItems.add(rejectedItem(
                                    directory,
                                    name,
                                    "안전한 자료실 경로로 확인되지 않아 제외했습니다."));
                            ensureImportEntryLimit(
                                    candidates.size() + rejectedItems.size());
                        }
                        continue;
                    }
                    if (!Files.isRegularFile(
                            child, LinkOption.NOFOLLOW_LINKS)) {
                        rejectedItems.add(rejectedItem(
                                directory,
                                name,
                                "일반 파일이 아니어서 제외했습니다."));
                        ensureImportEntryLimit(
                                candidates.size() + rejectedItems.size());
                        continue;
                    }
                    candidates.add(new ImportCandidate(
                            directory,
                            child,
                            relativePath(directory, name)));
                    ensureImportEntryLimit(
                            candidates.size() + rejectedItems.size());
                }
            } catch (DirectoryIteratorException exception) {
                throw importFailure(
                        "Unable to inspect server-side repository files",
                        exception.getCause());
            } catch (IOException exception) {
                throw importFailure(
                        "Unable to inspect server-side repository files",
                        exception);
            }
        }
        return new ImportDiscovery(
                List.copyOf(directories),
                List.copyOf(candidates),
                List.copyOf(rejectedItems));
    }

    private static void ensureImportEntryLimit(int entryCount)
            throws FileRepositoryException {
        if (entryCount > MAX_IMPORT_ENTRIES) {
            throw new FileRepositoryException(
                    413,
                    "too_many_import_files",
                    "At most 10000 server-side entries may be inspected at once");
        }
    }

    private Item inspectCandidate(
            ImportCandidate candidate, Set<String> managedNames) {
        Path source = candidate.source();
        try {
            BasicFileAttributes before = Files.readAttributes(
                    source,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (!before.isRegularFile() || Files.isSymbolicLink(source)) {
                return item(candidate, ImportDisposition.REJECTED);
            }
            if (before.lastModifiedTime().toInstant().isAfter(
                    Instant.now().minus(MINIMUM_STABLE_AGE))) {
                return item(candidate, ImportDisposition.DEFERRED);
            }

            String fileName = source.getFileName().toString();
            ValidatedFile validated;
            try {
                validated = files.validateImported(fileName, before.size());
                if (!fileName.equals(validated.originalName())) {
                    return item(candidate, ImportDisposition.REJECTED);
                }
                validateContent(source);
            } catch (FileRepositoryException exception) {
                return new Item(
                        candidate.relativePath(),
                        fileName,
                        ImportDisposition.REJECTED,
                        rejectionReason(exception),
                        false);
            }

            BasicFileAttributes inspected = Files.readAttributes(
                    source,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (!sameFile(before, inspected) || Files.isSymbolicLink(source)) {
                return new Item(
                        candidate.relativePath(),
                        fileName,
                        ImportDisposition.DEFERRED,
                        "파일이 검사 중 변경되어 다음 미리보기에서 다시 확인합니다.",
                        false);
            }
            if (managedNames.contains(
                    validated.originalName().toLowerCase(Locale.ROOT))) {
                return item(candidate, ImportDisposition.CONFLICT);
            }
            return item(candidate, ImportDisposition.READY);
        } catch (IOException exception) {
            return item(candidate, ImportDisposition.FAILED);
        }
    }

    private static Item rejectedItem(
            ResolvedDirectory directory, String name, String reason) {
        return new Item(
                relativePath(directory, name),
                name,
                ImportDisposition.REJECTED,
                reason,
                false);
    }

    private static Item item(
            ImportCandidate candidate, ImportDisposition disposition) {
        return new Item(
                candidate.relativePath(),
                candidate.source().getFileName().toString(),
                disposition,
                dispositionReason(disposition),
                disposition == ImportDisposition.READY);
    }

    private static String relativePath(
            ResolvedDirectory directory, String name) {
        return directory.relativePath().isEmpty()
                ? name
                : directory.relativePath() + "/" + name;
    }

    private static String rejectionReason(FileRepositoryException exception) {
        return switch (exception.getCode()) {
            case "empty_file" -> "빈 파일은 반입할 수 없습니다.";
            case "unsupported_extension" -> "허용되지 않은 확장자입니다.";
            case "invalid_filename" -> "파일명이 안전하지 않아 제외했습니다.";
            case "active_content" -> "실행 가능한 콘텐츠가 감지되어 제외했습니다.";
            default -> "자료실 파일 정책을 통과하지 못했습니다.";
        };
    }

    private static String dispositionReason(
            ImportDisposition disposition) {
        return switch (disposition) {
            case READY -> "반입할 수 있습니다.";
            case IMPORTED -> "자료실에 등록했습니다.";
            case CONFLICT -> "같은 이름의 관리 파일이 이미 있습니다.";
            case REJECTED -> "파일 정책 또는 경로 정책에 따라 제외했습니다.";
            case DEFERRED -> "복사가 끝난 지 30초가 지나지 않아 대기합니다.";
            case FAILED -> "반입 중 오류가 발생했습니다. 다시 시도할 수 있습니다.";
        };
    }

    private static boolean isExpectedManagedEntry(String name) {
        return MANAGED_FILE.matcher(name).matches()
                || UPLOAD_TEMP_FILE.matcher(name).matches();
    }

    private ImportDisposition importCandidate(
            ImportCandidate candidate, Set<String> managedNames)
            throws IOException {
        Path source = candidate.source();
        BasicFileAttributes before = Files.readAttributes(
                source,
                BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || Files.isSymbolicLink(source)) {
            return ImportDisposition.REJECTED;
        }
        if (before.lastModifiedTime().toInstant().isAfter(
                Instant.now().minus(MINIMUM_STABLE_AGE))) {
            return ImportDisposition.DEFERRED;
        }

        String fileName = source.getFileName().toString();
        ValidatedFile validated;
        try {
            validated = files.validateImported(fileName, before.size());
            if (!fileName.equals(validated.originalName())) {
                return ImportDisposition.REJECTED;
            }
            validateContent(source);
        } catch (FileRepositoryException exception) {
            return ImportDisposition.REJECTED;
        }

        BasicFileAttributes inspected = Files.readAttributes(
                source,
                BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (!sameFile(before, inspected) || Files.isSymbolicLink(source)) {
            return ImportDisposition.REJECTED;
        }
        if (managedNames.contains(
                validated.originalName().toLowerCase(Locale.ROOT))) {
            return ImportDisposition.CONFLICT;
        }

        String storageId = UUID.randomUUID().toString().replace("-", "");
        Path dataPath;
        Path metadataPath;
        try {
            dataPath = paths.managedPathForWrite(
                    candidate.directory(), storageId, DATA_SUFFIX);
            metadataPath = paths.managedPathForWrite(
                    candidate.directory(), storageId, META_SUFFIX);
        } catch (FileRepositoryException exception) {
            throw new IOException(
                    "Unable to allocate repository storage", exception);
        }

        Path metadataTemp = null;
        boolean dataMoved = false;
        boolean metadataMoved = false;
        try {
            metadataTemp = writeMetadata(
                    candidate.directory(), storageId, validated, before.size());
            atomicMove(source, dataPath);
            dataMoved = true;
            publishObserver.afterDataMove(source, dataPath);
            BasicFileAttributes moved = Files.readAttributes(
                    dataPath,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (Files.isSymbolicLink(dataPath) || !sameFile(before, moved)) {
                throw new IOException(
                        "Imported repository file changed during indexing");
            }
            try {
                validateContent(dataPath);
            } catch (FileRepositoryException exception) {
                throw new IOException(
                        "Imported repository content changed during indexing",
                        exception);
            }

            atomicMove(metadataTemp, metadataPath);
            metadataTemp = null;
            metadataMoved = true;
            snapshotInvalidator.invalidate(candidate.directory().path());
            return ImportDisposition.IMPORTED;
        } finally {
            cleanup(metadataTemp);
            if (dataMoved && !metadataMoved) {
                rollbackDataMove(source, dataPath);
            }
        }
    }

    private Path writeMetadata(
            ResolvedDirectory directory,
            String storageId,
            ValidatedFile validated,
            long size) throws IOException {
        Properties metadata = new Properties();
        metadata.setProperty("id", storageId);
        metadata.setProperty("originalName", validated.originalName());
        metadata.setProperty("contentType", validated.contentType());
        metadata.setProperty("size", Long.toString(size));
        metadata.setProperty("uploadedAt", Instant.now().toString());
        metadata.setProperty("source", "server-import");
        Path metadataTemp = Files.createTempFile(
                directory.path(), ".frog2-meta-", ".tmp");
        boolean complete = false;
        try {
            try (OutputStream output = Files.newOutputStream(metadataTemp)) {
                metadata.store(output, "frog2 file metadata");
            }
            forceFile(metadataTemp);
            complete = true;
            return metadataTemp;
        } finally {
            if (!complete) {
                cleanup(metadataTemp);
            }
        }
    }

    private Map<Path, Set<String>> loadManagedOriginalNames(
            List<ResolvedDirectory> directories)
            throws FileRepositoryException {
        Map<Path, Set<String>> managedNames = new HashMap<>();
        for (ResolvedDirectory directory : directories) {
            Set<String> names = new HashSet<>();
            try (var children = Files.newDirectoryStream(directory.path())) {
                for (Path metadataPath : children) {
                    Matcher matcher = METADATA_FILE.matcher(
                            metadataPath.getFileName().toString());
                    if (!matcher.matches()
                            || Files.isSymbolicLink(metadataPath)
                            || !Files.isRegularFile(
                                    metadataPath,
                                    LinkOption.NOFOLLOW_LINKS)) {
                        continue;
                    }
                    String storageId = matcher.group(1);
                    try {
                        Path dataPath = paths.resolveManagedFile(
                                directory, storageId, DATA_SUFFIX);
                        StoredMetadata metadata = metadataReader.read(
                                metadataPath, dataPath, storageId);
                        names.add(metadata.originalName()
                                .toLowerCase(Locale.ROOT));
                    } catch (FileRepositoryException ignored) {
                        // Invalid pairs are counted by the normal directory scan.
                    }
                }
            } catch (DirectoryIteratorException exception) {
                throw importFailure(
                        "Unable to inspect existing repository metadata",
                        exception.getCause());
            } catch (IOException exception) {
                throw importFailure(
                        "Unable to inspect existing repository metadata",
                        exception);
            }
            managedNames.put(directory.path(), names);
        }
        return managedNames;
    }

    private void validateContent(Path path)
            throws IOException, FileRepositoryException {
        try (InputStream input = Files.newInputStream(path)) {
            files.validateContent(input.readNBytes(4096));
        }
    }

    private static boolean sameFile(
            BasicFileAttributes expected,
            BasicFileAttributes actual) {
        if (!actual.isRegularFile()
                || expected.size() != actual.size()
                || !expected.lastModifiedTime().equals(
                        actual.lastModifiedTime())) {
            return false;
        }
        Object expectedKey = expected.fileKey();
        Object actualKey = actual.fileKey();
        return expectedKey == null
                || actualKey == null
                || expectedKey.equals(actualKey);
    }

    private static void rollbackDataMove(Path source, Path dataPath) {
        try {
            if (Files.notExists(source, LinkOption.NOFOLLOW_LINKS)) {
                atomicMove(dataPath, source);
            }
        } catch (IOException rollbackFailure) {
            logger.error(
                    "Unable to roll back a failed server-side repository import");
        }
    }

    private static void atomicMove(Path source, Path target)
            throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private static void forceFile(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(
                path, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private static void cleanup(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup of a temporary metadata file.
        }
    }

    private static FileRepositoryException importFailure(
            String message, IOException cause) {
        return new FileRepositoryException(
                500, "repository_import_failed", message, cause);
    }

    @FunctionalInterface
    interface ManagedMetadataReader {
        StoredMetadata read(Path metadata, Path data, String storageId)
                throws FileRepositoryException;
    }

    @FunctionalInterface
    interface SnapshotInvalidator {
        void invalidate(Path directory);
    }

    @FunctionalInterface
    interface PublishObserver {
        void afterDataMove(Path source, Path data) throws IOException;
    }

    record Preview(String relativePath, List<Item> items) {
        int readyCount() {
            return count(ImportDisposition.READY);
        }

        private int count(ImportDisposition disposition) {
            return (int) items.stream()
                    .filter(item -> item.disposition() == disposition)
                    .count();
        }
    }

    record Result(String relativePath, List<Item> items) {
        int importedCount() {
            return count(ImportDisposition.IMPORTED);
        }

        int conflictCount() {
            return count(ImportDisposition.CONFLICT);
        }

        int rejectedCount() {
            return count(ImportDisposition.REJECTED);
        }

        int deferredCount() {
            return count(ImportDisposition.DEFERRED);
        }

        int failedCount() {
            return count(ImportDisposition.FAILED);
        }

        private int count(ImportDisposition disposition) {
            return (int) items.stream()
                    .filter(item -> item.disposition() == disposition)
                    .count();
        }
    }

    record Item(
            String relativePath,
            String name,
            ImportDisposition disposition,
            String reason,
            boolean selectable) {
        private static final Comparator<Item> BY_PATH = Comparator.comparing(
                Item::relativePath, String.CASE_INSENSITIVE_ORDER);
    }

    private record ImportDiscovery(
            List<ResolvedDirectory> directories,
            List<ImportCandidate> candidates,
            List<Item> rejectedItems) {
    }

    private record ImportCandidate(
            ResolvedDirectory directory, Path source, String relativePath) {
    }

    enum ImportDisposition {
        READY,
        IMPORTED,
        CONFLICT,
        REJECTED,
        DEFERRED,
        FAILED
    }
}
