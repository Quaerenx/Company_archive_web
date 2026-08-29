package com.company.filerepo;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class FileRepositoryFilePolicy {
    public static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    public static final int MAX_FILE_COUNT = 5;
    public static final long MAX_REQUEST_SIZE = (MAX_FILE_SIZE * MAX_FILE_COUNT) + (1024L * 1024L);

    private static final Set<String> ACTIVE_EXTENSIONS = Set.of(
            "jsp", "jspx", "jspf", "html", "htm", "xhtml", "svg", "svgz", "js", "mjs",
            "exe", "com", "bat", "cmd", "ps1", "sh", "php", "phtml", "cgi", "jar", "war");
    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("txt", Set.of("text/plain")),
            Map.entry("log", Set.of("text/plain")),
            Map.entry("csv", Set.of("text/csv", "application/csv", "application/vnd.ms-excel")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("doc", Set.of("application/msword")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry("xls", Set.of("application/vnd.ms-excel")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry("ppt", Set.of("application/vnd.ms-powerpoint")),
            Map.entry("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
            Map.entry("zip", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream")),
            Map.entry("7z", Set.of("application/x-7z-compressed", "application/octet-stream")),
            Map.entry("rar", Set.of("application/vnd.rar", "application/x-rar-compressed", "application/octet-stream")),
            Map.entry("gz", Set.of("application/gzip", "application/x-gzip", "application/octet-stream")),
            Map.entry("tar", Set.of("application/x-tar", "application/octet-stream")));
    private static final Map<String, String> IMPORT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("txt", "text/plain"),
            Map.entry("log", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("zip", "application/zip"),
            Map.entry("7z", "application/x-7z-compressed"),
            Map.entry("rar", "application/vnd.rar"),
            Map.entry("gz", "application/gzip"),
            Map.entry("tar", "application/x-tar"),
            Map.entry("rpm", "application/x-rpm"));

    public ValidatedFile validate(String submittedName, String contentType, long size)
            throws FileRepositoryException {
        if (size <= 0) {
            throw new FileRepositoryException(400, "empty_file", "Empty files are not allowed");
        }
        if (size > MAX_FILE_SIZE) {
            throw new FileRepositoryException(413, "file_too_large", "A file exceeds the 10 MB limit");
        }
        return validateStored(submittedName, contentType, size);
    }

    ValidatedFile validateStored(String submittedName, String contentType, long size)
            throws FileRepositoryException {
        if (size <= 0) {
            throw new FileRepositoryException(400, "empty_file", "Empty files are not allowed");
        }
        if (size > MAX_FILE_SIZE) {
            throw new FileRepositoryException(413, "file_too_large", "A file exceeds the 10 MB limit");
        }
        ValidatedName validatedName = validateName(
                submittedName, ALLOWED_TYPES.keySet());
        String normalizedType = normalizeContentType(contentType);
        if (!ALLOWED_TYPES.get(validatedName.extension()).contains(normalizedType)) {
            throw new FileRepositoryException(415, "unsupported_media_type", "File MIME type is not allowed");
        }
        return new ValidatedFile(
                validatedName.originalName(),
                validatedName.extension(),
                normalizedType);
    }

    /**
     * Validates a server-side file selected by the administrator import flow.
     * Unlike browser uploads, the file has already been copied to the repository,
     * so the multipart 10 MB limit does not apply. Name and active-content
     * controls remain in force, while explicitly approved import-only
     * extensions may differ from browser uploads.
     */
    ValidatedFile validateImported(String fileName, long size)
            throws FileRepositoryException {
        if (size <= 0) {
            throw new FileRepositoryException(400, "empty_file", "Empty files are not allowed");
        }
        ValidatedName validatedName = validateName(
                fileName, IMPORT_TYPES.keySet());
        return new ValidatedFile(
                validatedName.originalName(),
                validatedName.extension(),
                IMPORT_TYPES.get(validatedName.extension()));
    }

    ValidatedFile validateImportedStored(
            String submittedName, String contentType, long size)
            throws FileRepositoryException {
        if (size <= 0) {
            throw new FileRepositoryException(400, "empty_file", "Empty files are not allowed");
        }
        ValidatedName validatedName = validateName(
                submittedName, IMPORT_TYPES.keySet());
        String normalizedType = normalizeContentType(contentType);
        if (!IMPORT_TYPES.get(validatedName.extension()).equals(normalizedType)) {
            throw new FileRepositoryException(
                    415,
                    "unsupported_media_type",
                    "File MIME type is not allowed");
        }
        return new ValidatedFile(
                validatedName.originalName(),
                validatedName.extension(),
                normalizedType);
    }

    public void validateContent(byte[] prefix) throws FileRepositoryException {
        if (prefix.length >= 2 && prefix[0] == 'M' && prefix[1] == 'Z') {
            throw activeContent();
        }
        if (prefix.length >= 4 && prefix[0] == 0x7f && prefix[1] == 'E' && prefix[2] == 'L' && prefix[3] == 'F') {
            throw activeContent();
        }
        String sample = new String(prefix, StandardCharsets.ISO_8859_1)
                .replace("\u0000", "")
                .replace("\u00ef\u00bb\u00bf", "")
                .replace("\u00ff\u00fe", "")
                .replace("\u00fe\u00ff", "")
                .stripLeading()
                .toLowerCase(Locale.ROOT);
        if (sample.startsWith("#!") || sample.startsWith("<%") || sample.startsWith("<?php")
                || sample.contains("<!doctype html") || sample.contains("<html")
                || sample.contains("<svg") || sample.contains("<script")) {
            throw activeContent();
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parameters = contentType.indexOf(';');
        String value = parameters >= 0 ? contentType.substring(0, parameters) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static ValidatedName validateName(
            String submittedName, Set<String> allowedExtensions)
            throws FileRepositoryException {
        if (submittedName == null) {
            throw invalidName();
        }
        String originalName = submittedName.trim();
        if (originalName.isEmpty() || originalName.length() > 255 || originalName.contains("/")
                || originalName.contains("\\") || originalName.codePoints().anyMatch(Character::isISOControl)) {
            throw invalidName();
        }
        int dot = originalName.lastIndexOf('.');
        if (dot <= 0 || dot == originalName.length() - 1) {
            throw new FileRepositoryException(415, "unsupported_extension", "File extension is not allowed");
        }
        String extension = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (ACTIVE_EXTENSIONS.contains(extension)
                || !allowedExtensions.contains(extension)) {
            throw new FileRepositoryException(415, "unsupported_extension", "File extension is not allowed");
        }
        return new ValidatedName(originalName, extension);
    }

    private static FileRepositoryException invalidName() {
        return new FileRepositoryException(400, "invalid_filename", "Invalid original filename");
    }

    private static FileRepositoryException activeContent() {
        return new FileRepositoryException(415, "active_content", "Executable or active content is not allowed");
    }

    public record ValidatedFile(String originalName, String extension, String contentType) {
    }

    private record ValidatedName(String originalName, String extension) {
    }
}
