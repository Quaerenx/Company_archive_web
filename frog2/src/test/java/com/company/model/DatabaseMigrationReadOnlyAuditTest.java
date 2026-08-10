package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e-schema")
class DatabaseMigrationReadOnlyAuditTest {
    private static final String CONFIG_ENV = "FROG2_SCHEMA_AUDIT_DB_CONFIG";

    @Test
    void activeSchemaContractsAndReviewedScheduleOverrideAreApplied()
            throws Exception {
        Properties database = loadDatabaseProperties();
        Class.forName(database.getProperty("db.driver"));

        DatabaseSchemaReadiness.Report schema = DatabaseSchemaReadiness.inspect(
                () -> openReadOnly(database));

        assertTrue(
                schema.ready(),
                () -> "Missing migration capabilities: "
                        + schema.missingRequirements());

        try (Connection connection = openReadOnly(database);
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT interval_months, anchor_month, enabled "
                                + "FROM customer_maintenance_schedule "
                                + "WHERE customer_name = ?")) {
            statement.setString(1, "건국대병원");
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(
                        resultSet.next(),
                        "V20260804_08 reviewed schedule override is missing");
                assertEquals(3, resultSet.getInt("interval_months"));
                assertEquals(
                        LocalDate.of(2000, 3, 1),
                        resultSet.getDate("anchor_month").toLocalDate());
                assertTrue(resultSet.getBoolean("enabled"));
                assertFalse(
                        resultSet.next(),
                        "Reviewed schedule override must be unique");
            }
        }
    }

    private static Properties loadDatabaseProperties() throws Exception {
        String configured = System.getenv(CONFIG_ENV);
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException(CONFIG_ENV + " is required");
        }

        Path config = Path.of(configured).toRealPath().normalize();
        Path allowedRoot = Path.of("/opt/frog2-dev").toRealPath().normalize();
        if (!config.startsWith(allowedRoot) || !Files.isRegularFile(config)) {
            throw new IllegalArgumentException(
                    "Schema audit config must be under /opt/frog2-dev");
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(config)) {
            properties.load(input);
        }
        for (String key : List.of(
                "db.url", "db.user", "db.password", "db.driver")) {
            String value = properties.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "Missing database configuration key: " + key);
            }
        }
        return properties;
    }

    private static Connection openReadOnly(Properties database)
            throws SQLException {
        Connection connection = DriverManager.getConnection(
                database.getProperty("db.url"),
                database.getProperty("db.user"),
                database.getProperty("db.password"));
        connection.setReadOnly(true);
        return connection;
    }
}
