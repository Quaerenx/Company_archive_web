package com.company.filerepo;

import com.company.filerepo.FileRepositoryService.StoredMetadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

final class FileRepositoryPresentation {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm", Locale.KOREA)
            .withZone(ZoneId.systemDefault());
    private static final Set<String> IMAGES = Set.of(
            "jpg", "jpeg", "png", "gif");
    private static final Set<String> DOCUMENTS = Set.of(
            "doc", "docx", "ppt", "pptx");
    private static final Set<String> SPREADSHEETS = Set.of(
            "xls", "xlsx", "csv");
    private static final Set<String> ARCHIVES = Set.of(
            "zip", "7z", "rar", "gz", "tar");
    private static final Set<String> TEXT = Set.of("txt", "log");

    private FileRepositoryPresentation() {
    }

    static String modifiedText(Path path) throws IOException {
        return DATE_FORMAT.format(
                Files.getLastModifiedTime(path).toInstant());
    }

    static FileRepositoryEntry fileEntry(
            String relativePath,
            String storageId,
            Path dataPath,
            StoredMetadata metadata) throws IOException {
        String extension = extension(metadata.originalName());
        String icon = "📄";
        String description = "파일";
        if (IMAGES.contains(extension)) {
            icon = "🖼️";
            description = "이미지 파일";
        } else if ("pdf".equals(extension)) {
            icon = "📋";
            description = "PDF 문서";
        } else if (DOCUMENTS.contains(extension)) {
            icon = "📝";
            description = "문서 파일";
        } else if (SPREADSHEETS.contains(extension)) {
            icon = "📊";
            description = "스프레드시트";
        } else if (ARCHIVES.contains(extension)) {
            icon = "📦";
            description = "압축 파일";
        } else if ("rpm".equals(extension)) {
            icon = "📦";
            description = "RPM 패키지";
        } else if (TEXT.contains(extension)) {
            icon = "📃";
            description = "텍스트 파일";
        }
        return new FileRepositoryEntry(
                false,
                storageId,
                metadata.originalName(),
                relativePath,
                modifiedText(dataPath),
                metadata.size(),
                formatSize(metadata.size()),
                icon,
                description);
    }

    static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unit = -1;
        do {
            value /= 1024.0;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0
                ? ""
                : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
