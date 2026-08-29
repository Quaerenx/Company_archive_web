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
import java.util.HashMap;
import java.util.HashSet;
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
    private static final int MAX_IMPORT_CANDIDATES = 10_000;
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

    Result importUnmanaged(String rawPath) throws FileRepositoryException {
        synchronized (IMPORT_LOCK) {
            ResolvedDirectory rootDirectory =
                    paths.resolveExistingDirectory(rawPath);
            ImportDiscovery discovery = discover(rootDirectory);
            Map<Path, Set<String>> managedNames =
                    loadManagedOriginalNames(discovery.directories());
            int imported = 0;
            int conflicts = 0;
            int rejected = discovery.rejectedCount();
            int deferred = 0;
            int failed = 0;

            for (ImportCandidate candidate : discovery.candidates()) {
                try {
                    ImportDisposition disposition = importCandidate(
                            candidate,
                            managedNames.get(candidate.directory().path()));
                    switch (disposition) {
                        case IMPORTED -> {
                            imported++;
                            managedNames.get(candidate.directory().path()).add(
                                    candidate.source().getFileName().toString()
                                            .toLowerCase(Locale.ROOT));
                        }
                        case CONFLICT -> conflicts++;
                        case REJECTED -> rejected++;
                        case DEFERRED -> deferred++;
                    }
                } catch (IOException exception) {
                    failed++;
                    logger.error(
                            "Unable to import a server-side repository file");
                }
            }

            logger.info(
                    "File repository import completed: imported={}, conflicts={}, rejected={}, deferred={}, failed={}",
                    imported,
                    conflicts,
                    rejected,
                    deferred,
                    failed);
            return new Result(
                    rootDirectory.relativePath(),
                    imported,
                    conflicts,
                    rejected,
                    deferred,
                    failed);
        }
    }

    private ImportDiscovery discover(ResolvedDirectory rootDirectory)
            throws FileRepositoryException {
        List<ResolvedDirectory> directories = new ArrayList<>();
        Set<Path> visited = new HashSet<>();
        List<ImportCandidate> candidates = new ArrayList<>();
        int rejected = 0;
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
                        rejected++;
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
                            rejected++;
                        }
                        continue;
                    }
                    if (!Files.isRegularFile(
                            child, LinkOption.NOFOLLOW_LINKS)) {
                        rejected++;
                        continue;
                    }
                    candidates.add(new ImportCandidate(directory, child));
                    if (candidates.size() > MAX_IMPORT_CANDIDATES) {
                        throw new FileRepositoryException(
                                413,
                                "too_many_import_files",
                                "At most 10000 server-side files may be imported at once");
                    }
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
                rejected);
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

    record Result(
            String relativePath,
            int importedCount,
            int conflictCount,
            int rejectedCount,
            int deferredCount,
            int failedCount) {
    }

    private record ImportDiscovery(
            List<ResolvedDirectory> directories,
            List<ImportCandidate> candidates,
            int rejectedCount) {
    }

    private record ImportCandidate(
            ResolvedDirectory directory, Path source) {
    }

    private enum ImportDisposition {
        IMPORTED,
        CONFLICT,
        REJECTED,
        DEFERRED
    }
}
