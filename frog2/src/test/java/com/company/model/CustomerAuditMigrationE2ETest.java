package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e-customer-audit-migration")
class CustomerAuditMigrationE2ETest {
    private static final String CONFIG_ENV =
            "FROG2_CUSTOMER_AUDIT_DB_CONFIG";
    private static final String APPROVAL_ENV =
            "FROG2_CUSTOMER_AUDIT_MIGRATION_APPROVED";
    private static final String BACKUP_ENV =
            "FROG2_CUSTOMER_AUDIT_BACKUP";
    private static final String APPROVAL_VALUE = "V20260825_09";
    private static final Pattern BACKUP_PATTERN = Pattern.compile(
            "backup_snapshot_[0-9]{8}_[0-9]{6}");
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/"
                    + "V20260825_09__add_customer_audit_columns.sql");
    private static final String TABLE = "vertica_customer_detail";
    private static final Map<String, String> EXPECTED_COLUMNS = Map.of(
            "updated_at", "timestamp",
            "updated_by", "varchar",
            "deleted_at", "timestamp",
            "deleted_by", "varchar");

    @Test
    void appliesApprovedMigrationWithoutChangingCustomerRowCount()
            throws Exception {
        String approval = requireEnvironment(APPROVAL_ENV);
        assertEquals(
                APPROVAL_VALUE,
                approval,
                "Explicit migration approval does not match");
        String backup = requireEnvironment(BACKUP_ENV);
        assertTrue(
                BACKUP_PATTERN.matcher(backup).matches(),
                "A verified VBR recovery-point name is required");

        Properties database = loadDatabaseProperties();
        Class.forName(database.getProperty("db.driver"));
        List<String> statements = loadMigrationStatements();

        try (Connection connection = open(database)) {
            long before = customerRowCount(connection);
            assertCompatibleExistingColumns(connection);
            for (String sql : statements) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
            assertExpectedColumns(connection);
            assertEquals(
                    before,
                    customerRowCount(connection),
                    "Schema migration must not change customer rows");
            System.out.printf(
                    Locale.ROOT,
                    "Customer audit migration verified: rows=%d, columns=%d, backup=%s%n",
                    before,
                    EXPECTED_COLUMNS.size(),
                    backup);
        }
    }

    private static List<String> loadMigrationStatements() throws Exception {
        String source = Files.readString(MIGRATION, StandardCharsets.UTF_8);
        String withoutComments = source.lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .reduce("", (left, right) -> left + "\n" + right);
        List<String> statements = Arrays.stream(withoutComments.split(";"))
                .map(String::trim)
                .filter(statement -> !statement.isEmpty())
                .toList();
        assertEquals(4, statements.size(), "Expected four audit ALTER statements");
        for (String statement : statements) {
            String normalized = statement.toLowerCase(Locale.ROOT)
                    .replaceAll("\\s+", " ");
            assertTrue(
                    normalized.startsWith(
                            "alter table vertica_customer_detail add column if not exists "),
                    "Migration contains an unexpected statement");
        }
        return statements;
    }

    private static void assertCompatibleExistingColumns(Connection connection)
            throws Exception {
        Map<String, String> columns = auditColumns(connection);
        for (Map.Entry<String, String> column : columns.entrySet()) {
            assertTrue(
                    compatibleType(column.getKey(), column.getValue()),
                    () -> "Incompatible existing audit column: "
                            + column.getKey() + " " + column.getValue());
        }

        long newColumnCount = columns.keySet().stream()
                .filter(column -> !"updated_at".equals(column))
                .count();
        assertTrue(
                newColumnCount == 0 || columns.size() == EXPECTED_COLUMNS.size(),
                "Customer audit schema is already partially applied");
    }

    private static void assertExpectedColumns(Connection connection)
            throws Exception {
        Map<String, String> columns = auditColumns(connection);
        assertEquals(EXPECTED_COLUMNS.keySet(), columns.keySet());
        for (Map.Entry<String, String> column : columns.entrySet()) {
            assertTrue(
                    compatibleType(column.getKey(), column.getValue()),
                    () -> "Unexpected audit column type: "
                            + column.getKey() + " " + column.getValue());
        }
    }

    private static Map<String, String> auditColumns(Connection connection)
            throws Exception {
        Map<String, String> columns = new LinkedHashMap<>();
        DatabaseMetaData metadata = connection.getMetaData();
        for (String column : EXPECTED_COLUMNS.keySet()) {
            try (ResultSet resultSet = metadata.getColumns(
                    null, null, TABLE, column)) {
                if (resultSet.next()) {
                    columns.put(column, resultSet.getString("TYPE_NAME"));
                }
            }
        }
        return columns;
    }

    private static boolean compatibleType(String column, String type) {
        if (type == null) {
            return false;
        }
        return type.toLowerCase(Locale.ROOT)
                .contains(EXPECTED_COLUMNS.get(column));
    }

    private static long customerRowCount(Connection connection)
            throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT COUNT(*) FROM " + TABLE)) {
            assertTrue(resultSet.next());
            return resultSet.getLong(1);
        }
    }

    private static Properties loadDatabaseProperties() throws Exception {
        Path configured = Path.of(requireEnvironment(CONFIG_ENV))
                .toRealPath().normalize();
        Path allowedRoot = Path.of("/opt/frog2-dev")
                .toRealPath().normalize();
        if (!configured.startsWith(allowedRoot)
                || !Files.isRegularFile(configured)) {
            throw new IllegalArgumentException(
                    "Migration config must be under /opt/frog2-dev");
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configured)) {
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

    private static Connection open(Properties database) throws Exception {
        return DriverManager.getConnection(
                database.getProperty("db.url"),
                database.getProperty("db.user"),
                database.getProperty("db.password"));
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
