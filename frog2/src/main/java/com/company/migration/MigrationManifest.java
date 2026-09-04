package com.company.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class MigrationManifest {
    private static final Pattern FILE_PATTERN = Pattern.compile(
            "^(V[0-9]{8}_[0-9]{2})__[a-z0-9_]+[.]sql$");
    private static final Pattern CHECKSUM_PATTERN = Pattern.compile(
            "^[0-9a-f]{64}$");

    private MigrationManifest() {
    }

    public static List<Entry> load(Path configuredDirectory) throws IOException {
        Path directory = configuredDirectory.toRealPath().normalize();
        if (!Files.isDirectory(directory)) {
            throw new IOException("Migration directory does not exist");
        }

        Map<String, String> expectedChecksums = readChecksums(
                directory.resolve("manifest.sha256"));
        List<Path> sqlFiles;
        try (Stream<Path> entries = Files.list(directory)) {
            sqlFiles = entries
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        Map<String, Entry> byVersion = new LinkedHashMap<>();
        for (Path sqlFile : sqlFiles) {
            String filename = sqlFile.getFileName().toString();
            Matcher matcher = FILE_PATTERN.matcher(filename);
            if (!matcher.matches()) {
                throw new IOException("Invalid active migration filename: " + filename);
            }
            String expected = expectedChecksums.remove(filename);
            if (expected == null) {
                throw new IOException("Migration is missing from manifest: " + filename);
            }
            String actual = sha256(sqlFile);
            if (!expected.equals(actual)) {
                throw new IOException("Migration checksum mismatch: " + filename);
            }
            Entry previous = byVersion.put(
                    matcher.group(1),
                    new Entry(matcher.group(1), filename, actual, sqlFile));
            if (previous != null) {
                throw new IOException("Duplicate migration version: " + matcher.group(1));
            }
        }
        if (!expectedChecksums.isEmpty()) {
            throw new IOException(
                    "Manifest references missing migrations: "
                            + String.join(", ", expectedChecksums.keySet()));
        }
        return List.copyOf(byVersion.values());
    }

    private static Map<String, String> readChecksums(Path manifest)
            throws IOException {
        if (!Files.isRegularFile(manifest)) {
            throw new IOException("Migration manifest does not exist");
        }
        Map<String, String> checksums = new HashMap<>();
        for (String line : Files.readAllLines(manifest)) {
            String normalized = line.trim();
            if (normalized.isEmpty() || normalized.startsWith("#")) {
                continue;
            }
            String[] parts = normalized.split("\\s+", 2);
            if (parts.length != 2 || !CHECKSUM_PATTERN.matcher(parts[0]).matches()) {
                throw new IOException("Invalid migration manifest line");
            }
            if (checksums.put(parts[1], parts[0]) != null) {
                throw new IOException("Duplicate migration manifest entry: " + parts[1]);
            }
        }
        return checksums;
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record Entry(
            String version,
            String filename,
            String checksum,
            Path path) {
    }
}
