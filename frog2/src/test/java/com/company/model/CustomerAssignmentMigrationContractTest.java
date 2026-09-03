package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerAssignmentMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/"
                    + "V20260903_11__add_customer_assignee_user_ids.sql");

    @Test
    void backfillUsesOnlyUniqueNormalizedDisplayNames() throws Exception {
        String migration = Files.readString(MIGRATION);

        assertTrue(migration.contains(
                "ADD COLUMN IF NOT EXISTS main_manager_user_id VARCHAR(100)"));
        assertTrue(migration.contains(
                "ADD COLUMN IF NOT EXISTS sub_manager_user_id VARCHAR(100)"));
        assertEquals(2, occurrences(migration, "HAVING COUNT(*) = 1"));
        assertEquals(6, occurrences(migration, "LOWER(TRIM("));
        assertTrue(migration.contains(
                "WHERE customer.main_manager_user_id IS NULL"));
        assertTrue(migration.contains(
                "WHERE customer.sub_manager_user_id IS NULL"));
        assertFalse(migration.matches(
                "(?is).*SET\\s+(?:main_manager|sub_manager)\\s*=.*"));
    }

    private static int occurrences(String source, String token) {
        return (source.length() - source.replace(token, "").length())
                / token.length();
    }
}
