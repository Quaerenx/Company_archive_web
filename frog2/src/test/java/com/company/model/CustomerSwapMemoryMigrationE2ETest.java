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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e-customer-swap-migration")
class CustomerSwapMemoryMigrationE2ETest {
    private static final String CONFIG_ENV =
            "FROG2_CUSTOMER_SWAP_DB_CONFIG";
    private static final String APPROVAL_ENV =
            "FROG2_CUSTOMER_SWAP_MIGRATION_APPROVED";
    private static final String APPROVAL_VALUE = "V20260901_10";
    private static final String COLUMN = "swap_memory";
    private static final List<String> TABLES = List.of(
            "vertica_customer_detail",
            "vertica_customer_detail_stg",
            "vertica_customer_detail_dev");
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/"
                    + "V20260901_10__add_customer_swap_memory.sql");

    @Test
    void appliesApprovedMigrationWithoutChangingCustomerRows()
            throws Exception {
        assertEquals(
                APPROVAL_VALUE,
                requireEnvironment(APPROVAL_ENV),
                "Explicit migration approval does not match");

        Properties database = loadDatabaseProperties();
        Class.forName(database.getProperty("db.driver"));
        List<String> statements = loadMigrationStatements();

        try (Connection connection = open(database)) {
            Map<String, Long> rowsBefore = rowCounts(connection);
            assertCompatibleExistingColumns(connection);
            for (String sql : statements) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
            assertExpectedColumns(connection);
            assertEquals(
                    rowsBefore,
                    rowCounts(connection),
                    "Schema migration must not change customer rows");
            System.out.printf(
                    Locale.ROOT,
                    "Customer SWAP-memory migration verified: tables=%d, rows=%s%n",
                    TABLES.size(),
                    rowsBefore.values());
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
        assertEquals(3, statements.size(), "Expected three SWAP-memory ALTER statements");
        for (int index = 0; index < TABLES.size(); index++) {
            String normalized = statements.get(index)
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("\\s+", " ");
            assertEquals(
                    "alter table " + TABLES.get(index)
                            + " add column if not exists swap_memory varchar(255)",
                    normalized,
                    "Migration contains an unexpected statement");
        }
        return statements;
    }

    private static void assertCompatibleExistingColumns(Connection connection)
            throws Exception {
        for (String table : TABLES) {
            ColumnType column = columnType(connection, table);
            assertTrue(
                    column == null || column.compatible(),
                    () -> "Incompatible existing column: " + table + "." + COLUMN);
        }
    }

    private static void assertExpectedColumns(Connection connection)
            throws Exception {
        for (String table : TABLES) {
            ColumnType column = columnType(connection, table);
            assertTrue(column != null, () -> "Missing column: " + table + "." + COLUMN);
            assertTrue(
                    column.compatible(),
                    () -> "Unexpected column type: " + table + "." + COLUMN);
        }
    }

    private static ColumnType columnType(Connection connection, String table)
            throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getColumns(
                null, null, table, COLUMN)) {
            if (!resultSet.next()) {
                return null;
            }
            ColumnType column = new ColumnType(
                    resultSet.getString("TYPE_NAME"),
                    resultSet.getInt("COLUMN_SIZE"));
            assertTrue(!resultSet.next(), () -> "Duplicate metadata column: " + table);
            return column;
        }
    }

    private static Map<String, Long> rowCounts(Connection connection)
            throws Exception {
        Map<String, Long> rows = new LinkedHashMap<>();
        for (String table : TABLES) {
            try (Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery(
                            "SELECT COUNT(*) FROM " + table)) {
                assertTrue(resultSet.next());
                rows.put(table, resultSet.getLong(1));
            }
        }
        return rows;
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

    private record ColumnType(String typeName, int size) {
        private boolean compatible() {
            return typeName != null
                    && typeName.toLowerCase(Locale.ROOT).contains("varchar")
                    && size == 255;
        }
    }
}
