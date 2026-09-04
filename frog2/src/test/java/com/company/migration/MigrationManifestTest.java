package com.company.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationManifestTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsOnlyChecksumPinnedVersionedSql() throws Exception {
        String filename = "V20260904_99__test.sql";
        Path migration = temporaryDirectory.resolve(filename);
        Files.writeString(migration, "SELECT 1;\n");
        Files.writeString(
                temporaryDirectory.resolve("manifest.sha256"),
                sha256(migration) + "  " + filename + "\n");

        MigrationManifest.Entry entry =
                MigrationManifest.load(temporaryDirectory).getFirst();

        assertEquals("V20260904_99", entry.version());
        assertEquals(filename, entry.filename());
    }

    @Test
    void rejectsSqlChangedAfterManifestApproval() throws Exception {
        String filename = "V20260904_99__test.sql";
        Path migration = temporaryDirectory.resolve(filename);
        Files.writeString(migration, "SELECT 1;\n");
        Files.writeString(
                temporaryDirectory.resolve("manifest.sha256"),
                sha256(migration) + "  " + filename + "\n");
        Files.writeString(migration, "SELECT 2;\n");

        assertThrows(
                java.io.IOException.class,
                () -> MigrationManifest.load(temporaryDirectory));
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }
}
