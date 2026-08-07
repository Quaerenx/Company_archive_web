package com.company.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TroubleshootingOwnershipContractTest {
    private static final Path PROJECT = Path.of(".");

    @Test
    void mutationsAndMigrationUseStableCreatorUserIdOwnership() throws Exception {
        String dao = Files.readString(PROJECT.resolve(
                "src/main/java/com/company/model/TroubleshootingDAO.java"));
        String servlet = Files.readString(PROJECT.resolve(
                "src/main/java/com/company/controller/TroubleshootingServlet.java"));
        String dto = Files.readString(PROJECT.resolve(
                "src/main/java/com/company/model/TroubleshootingDTO.java"));
        Path migrationPath = PROJECT.resolve(
                "src/main/resources/db/migration/"
                        + "V20260730_05__add_troubleshooting_creator_user_id.sql");

        assertTrue(dto.contains("creatorUserId"));
        assertTrue(dao.contains("creator_user_id"));
        assertTrue(dao.contains("WHERE id = ? AND creator_user_id = ?"));
        assertFalse(dao.contains(
                "\"DELETE FROM troubleshooting WHERE id = ?\""));
        assertTrue(servlet.contains("user.getUserId()"));
        assertTrue(servlet.contains("canManageTroubleshooting"));
        assertTrue(Files.isRegularFile(migrationPath));

        String migration = Files.readString(migrationPath);
        assertTrue(migration.contains(
                "ADD COLUMN IF NOT EXISTS creator_user_id VARCHAR(100)"));
        assertTrue(migration.contains("FROM company_users"));
        assertTrue(migration.contains(
                "ALTER COLUMN creator_user_id SET NOT NULL"));
    }
}
