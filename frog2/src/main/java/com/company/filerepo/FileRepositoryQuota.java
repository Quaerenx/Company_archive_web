package com.company.filerepo;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ConcurrentHashMap;

/** Limits browser uploads against the entire repository, including orphaned files. */
final class FileRepositoryQuota {
    static final String LOCK_FILE_NAME = ".frog2-upload-quota.lock";
    private static final long METADATA_ALLOWANCE = 8L * 1024L;
    private static final ConcurrentHashMap<Path, Object> ROOT_LOCKS =
            new ConcurrentHashMap<>();

    private final Path root;
    private final long maxBytes;
    private final long maxFiles;

    FileRepositoryQuota(Path root, long maxBytes, long maxFiles) {
        this.root = root;
        this.maxBytes = maxBytes;
        this.maxFiles = maxFiles;
    }

    <T> T store(long declaredSize, Upload<T> upload)
            throws FileRepositoryException {
        if (declaredSize <= 0 || declaredSize > FileRepositoryFilePolicy.MAX_FILE_SIZE) {
            throw new FileRepositoryException(
                    413, "file_too_large", "Uploaded file size is invalid");
        }
        synchronized (ROOT_LOCKS.computeIfAbsent(root, ignored -> new Object())) {
            Path lockPath = root.resolve(LOCK_FILE_NAME);
            try (FileChannel channel = FileChannel.open(
                    lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
                    FileLock ignored = channel.lock()) {
                Usage usage = usage(lockPath);
                if (usage.bytes > maxBytes - METADATA_ALLOWANCE - declaredSize
                        || usage.files > maxFiles - 2) {
                    throw new FileRepositoryException(
                            507, "repository_quota_exceeded",
                            "File repository upload limit has been reached");
                }
                return upload.run();
            } catch (IOException exception) {
                throw new FileRepositoryException(
                        500, "repository_quota_unavailable",
                        "Unable to check file repository upload limit", exception);
            }
        }
    }

    private Usage usage(Path lockPath) throws IOException {
        long[] totals = new long[2];
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(
                    Path file, BasicFileAttributes attributes) {
                if (!file.equals(lockPath)) {
                    totals[0] = saturatedAdd(totals[0], attributes.size());
                    totals[1] = saturatedAdd(totals[1], 1);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return new Usage(totals[0], totals[1]);
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    @FunctionalInterface
    interface Upload<T> {
        T run() throws FileRepositoryException;
    }

    private record Usage(long bytes, long files) {
    }
}
