package com.company.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DaoConnectionProviderContractTest {
    private static final Path MODEL_SOURCE =
            Path.of("src/main/java/com/company/model");

    @Test
    void injectableDaosNeverBypassTheirConnectionProvider()
            throws Exception {
        for (String file : new String[] {
                "MaintenanceRecordDAO.java",
                "CustomerDAO.java",
                "MeetingRecordDAO.java",
                "MeetingCommentDAO.java",
                "UserVmHostDAO.java"
        }) {
            String source = Files.readString(MODEL_SOURCE.resolve(file));

            assertTrue(source.contains("DBConnection::getConnection"), file);
            assertFalse(source.contains("DBConnection.getConnection()"), file);
            assertFalse(source.contains("DBConnection.close("), file);
        }
    }
}
