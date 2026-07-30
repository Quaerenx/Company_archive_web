package com.company.filerepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.regex.Pattern;

public final class FileRepositoryPathPolicy {
    private static final Pattern SEGMENT = Pattern.compile("[\\p{L}\\p{N} _.-]{1,100}");
    private static final Pattern STORAGE_ID = Pattern.compile("[0-9a-f]{32}");

    private final Path root;

    public FileRepositoryPathPolicy(Path configuredRoot) throws IOException {
        Path absoluteRoot = configuredRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(absoluteRoot, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(absoluteRoot)) {
            throw new IOException("File repository root must be an existing non-symlink directory");
        }
        root = absoluteRoot.toRealPath();
    }

    public ResolvedDirectory resolveExistingDirectory(String rawPath) throws FileRepositoryException {
        String relativePath = normalizeRelativePath(rawPath);
        Path candidate = relativePath.isEmpty() ? root : root.resolve(relativePath).normalize();
        requireContained(candidate);
        if (!Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(candidate)) {
            throw new FileRepositoryException(404, "directory_not_found", "Requested repository directory was not found");
        }
        try {
            Path realDirectory = candidate.toRealPath();
            requireContained(realDirectory);
            return new ResolvedDirectory(relativePath, realDirectory);
        } catch (IOException e) {
            throw new FileRepositoryException(500, "repository_io_error", "Unable to resolve repository directory", e);
        }
    }

    public Path resolveManagedFile(ResolvedDirectory directory, String storageId, String suffix)
            throws FileRepositoryException {
        requireStorageId(storageId);
        Path candidate = directory.path().resolve(".frog2-" + storageId + suffix).normalize();
        requireContained(candidate);
        if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(candidate)) {
            throw new FileRepositoryException(404, "file_not_found", "Requested file was not found");
        }
        try {
            Path realFile = candidate.toRealPath();
            requireContained(realFile);
            return realFile;
        } catch (IOException e) {
            throw new FileRepositoryException(500, "repository_io_error", "Unable to resolve stored file", e);
        }
    }

    public Path managedPathForWrite(ResolvedDirectory directory, String storageId, String suffix)
            throws FileRepositoryException {
        requireStorageId(storageId);
        Path candidate = directory.path().resolve(".frog2-" + storageId + suffix).normalize();
        requireContained(candidate);
        return candidate;
    }

    static String normalizeRelativePath(String rawPath) throws FileRepositoryException {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }
        String value = rawPath.trim();
        if (value.length() > 500 || value.startsWith("/") || value.contains("\\") || value.indexOf('\0') >= 0) {
            throw invalidPath();
        }
        String[] segments = value.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)
                    || segment.startsWith(".") || !SEGMENT.matcher(segment).matches()) {
                throw invalidPath();
            }
        }
        return String.join("/", segments);
    }

    private static void requireStorageId(String storageId) throws FileRepositoryException {
        if (storageId == null || !STORAGE_ID.matcher(storageId).matches()) {
            throw new FileRepositoryException(400, "invalid_file_id", "Invalid file identifier");
        }
    }

    private void requireContained(Path candidate) throws FileRepositoryException {
        if (!candidate.toAbsolutePath().normalize().startsWith(root)) {
            throw invalidPath();
        }
    }

    private static FileRepositoryException invalidPath() {
        return new FileRepositoryException(400, "invalid_path", "Invalid repository path");
    }

    public record ResolvedDirectory(String relativePath, Path path) {
    }
}
