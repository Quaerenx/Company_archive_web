package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DatabaseMigrationInventoryTest {
    private static final Path DATABASE_ROOT =
            Path.of("src/main/resources/db");

    @Test
    void activeMigrationsMatchCurrentSchemaContracts() throws IOException {
        assertEquals(
                Set.of(
                        "V20260720_01__create_user_vm_hosts.sql",
                        "V20260720_04__rename_license_usage_pct.sql",
                        "V20260730_05__add_troubleshooting_creator_user_id.sql",
                        "V20260731_06__add_activity_creator_user_ids.sql",
                        "V20260804_07__create_customer_maintenance_schedule.sql",
                        "V20260804_08__set_konkuk_hospital_quarterly_schedule.sql",
                        "V20260825_09__add_customer_audit_columns.sql",
                        "V20260901_10__add_customer_swap_memory.sql",
                        "V20260903_11__add_customer_assignee_user_ids.sql",
                        "V20260904_12__create_schema_migration_ledger.sql",
                        "V20260904_13__create_customer_identity.sql"),
                sqlFileNames(DATABASE_ROOT.resolve("migration")));
    }

    @Test
    void retiredHostMigrationsRemainHistoricalOnly() throws IOException {
        Path legacy = DATABASE_ROOT.resolve("legacy");
        assertTrue(Files.isRegularFile(
                legacy.resolve("V20260720_02__create_hosts.sql")));
        assertTrue(Files.isRegularFile(
                legacy.resolve("V20260720_03__add_hosts_row_color.sql")));

        String readme = Files.readString(legacy.resolve("README.md"));
        assertTrue(readme.contains("`HostDAO`/`HostDTO` implementation"));
        assertTrue(readme.contains("must not be executed as active migrations"));
    }

    @Test
    void activeAndLegacyMigrationChecksumsMatchTheirManifests()
            throws Exception {
        verifyManifest(DATABASE_ROOT.resolve("migration"));
        verifyManifest(DATABASE_ROOT.resolve("legacy"));
    }

    private static void verifyManifest(Path directory) throws Exception {
        Path manifest = directory.resolve("manifest.sha256");
        Map<String, String> expected = new HashMap<>();
        for (String line : Files.readAllLines(manifest)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.trim().split("\\s+", 2);
            assertEquals(2, parts.length, "invalid manifest line: " + line);
            assertTrue(
                    parts[0].matches("[0-9a-f]{64}"),
                    "invalid SHA-256: " + line);
            assertTrue(
                    expected.put(parts[1], parts[0]) == null,
                    "duplicate manifest entry: " + parts[1]);
        }

        Set<String> migrations = sqlFileNames(directory);
        assertEquals(migrations, expected.keySet());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String migration : migrations) {
            String actual = HexFormat.of().formatHex(
                    digest.digest(Files.readAllBytes(directory.resolve(migration))));
            assertEquals(expected.get(migration), actual, migration);
        }
    }

    private static Set<String> sqlFileNames(Path directory)
            throws IOException {
        try (var files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".sql"))
                    .collect(Collectors.toSet());
        }
    }
}
