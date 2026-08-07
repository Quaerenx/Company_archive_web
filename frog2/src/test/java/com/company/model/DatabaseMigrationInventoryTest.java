package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
                        "V20260804_08__set_konkuk_hospital_quarterly_schedule.sql"),
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
