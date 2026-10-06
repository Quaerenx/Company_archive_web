package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Copies structure only, with independent identity defaults, into a disposable test schema. */
@Tag("e2e-db-integrity")
class DatabaseIntegrityE2ETest {
    private static final List<String> TABLES = List.of("company_users", "customer_identity",
            "vertica_customer_detail", "vertica_customer_detail_stg", "vertica_customer_detail_dev",
            "customer_maintenance_schedule", "maintenance_records", "monthly_customer_response",
            "troubleshooting", "meeting_records", "meeting_comments");
    private static final Map<String, String> GENERATED_IDS = Map.of(
            "maintenance_records", "maintenance_id", "monthly_customer_response", "id",
            "troubleshooting", "id", "meeting_records", "meeting_id", "meeting_comments", "comment_id");
    private static final List<String> MIGRATIONS = List.of("V20261006_14__add_customer_references.sql",
            "V20261006_15__enforce_environment_customer_keys.sql", "V20261006_16__add_numeric_maintenance_license.sql",
            "V20261006_17__preserve_deleted_meetings.sql");

    @Test
    void migrationsAndRealDaosPreserveDataAndEnforceTheNewIntegrityRules() throws Exception {
        assertEquals("true", System.getenv("FROG2_INTEGRITY_E2E_ENABLED"));
        Properties config = config();
        Class.forName(config.getProperty("db.driver"));
        String schema = "frog2_test_integrity_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = open(config)) {
            Map<String, Long> sourceCounts = counts(admin, "public");
            execute(admin, "CREATE SCHEMA " + schema);
            try {
                createSyntheticTables(admin, schema);
                try (Connection isolated = isolated(config, schema)) {
                    seed(isolated);
                    Map<String, Long> before = counts(isolated, schema);
                    for (String migration : MIGRATIONS) apply(isolated, migration);
                    Map<String, Long> after = counts(isolated, schema);
                    before.remove("customer_identity");
                    after.remove("customer_identity");
                    assertEquals(before, after, "All business rows must be preserved; identities are additive");
                    verifyBackfillAndConstraints(isolated);
                    verifyRealWrites(config, schema, isolated);
                    verifyMeetingLifecycle(config, schema, isolated);
                }
                assertEquals(sourceCounts, counts(admin, "public"), "Business table row counts changed during the isolated test");
            } finally {
                // Only the schema created by this invocation is eligible for cleanup.
                if (!schema.matches("frog2_test_integrity_[0-9a-f]{32}")) throw new IllegalStateException("Invalid test schema");
                execute(admin, "DROP SCHEMA " + schema + " CASCADE");
            }
        } catch (SQLException exception) {
            String location = java.util.Arrays.stream(exception.getStackTrace())
                    .filter(frame -> frame.getClassName().equals(DatabaseIntegrityE2ETest.class.getName()))
                    .map(frame -> frame.getMethodName() + ":" + frame.getLineNumber())
                    .collect(java.util.stream.Collectors.joining(","));
            throw new AssertionError("Integrity E2E JDBC failure; SQLState=" + exception.getSQLState() + "; location=" + location);
        }
        System.out.println("INTEGRITY_E2E|migrations=4|sourceRows=unchanged|uuid=passed|numeric=passed|keys=passed|meetingRetention=passed|cleanup=passed");
    }

    private static void createSyntheticTables(Connection connection, String schema) throws Exception {
        for (String table : TABLES) {
            List<String> fields = new ArrayList<>();
            try (ResultSet columns = connection.getMetaData().getColumns(null, "public", table, "%")) {
                while (columns.next()) {
                    if (!table.equals(columns.getString("TABLE_NAME"))) continue;
                    String name = columns.getString("COLUMN_NAME");
                    // Test baseline always starts before these four migrations, even after production applies them.
                    if ((CustomerReferenceSupport.TABLES.contains(table) && "customer_id".equals(name))
                            || ("maintenance_records".equals(table) && MaintenanceLicenseValues.COLUMNS.contains(name))
                            || ("meeting_records".equals(table) && List.of("deleted_at", "deleted_by").contains(name))
                            || ("meeting_comments".equals(table) && "archived_at".equals(name))) continue;
                    String type = columns.getString("TYPE_NAME").toLowerCase(Locale.ROOT);
                    String sqlType = switch (type) {
                        case "integer" -> "INTEGER";
                        case "varchar" -> "VARCHAR(" + columns.getInt("COLUMN_SIZE") + ")";
                        case "long varchar", "longvarchar" -> "LONG VARCHAR(" + columns.getInt("COLUMN_SIZE") + ")";
                        case "uuid" -> "UUID";
                        case "boolean" -> "BOOLEAN";
                        case "date" -> "DATE";
                        case "timestamp" -> "TIMESTAMP";
                        case "timestamptz" -> "TIMESTAMPTZ";
                        default -> throw new IllegalStateException("Unsupported synthetic column type: " + type);
                    };
                    String definition = name + " " + sqlType;
                    if (name.equals(GENERATED_IDS.get(table))) definition = name + " IDENTITY(1,1)";
                    if ("customer_identity".equals(table) && "customer_id".equals(name)) definition += " DEFAULT UUID_GENERATE() PRIMARY KEY ENABLED";
                    if ("customer_identity".equals(table) && "customer_name".equals(name)) definition += " UNIQUE ENABLED";
                    if (("vertica_customer_detail".equals(table) || "customer_maintenance_schedule".equals(table))
                            && "customer_name".equals(name)) definition += " PRIMARY KEY ENABLED";
                    if (type.startsWith("timestamp") && (name.endsWith("_at") || name.endsWith("_date"))) definition += " DEFAULT CURRENT_TIMESTAMP";
                    if ("is_deleted".equals(name)) definition += " DEFAULT 1";
                    if (columns.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls) definition += " NOT NULL";
                    fields.add(definition);
                }
            }
            assertFalse(fields.isEmpty(), table);
            if (GENERATED_IDS.containsKey(table)) fields.add("PRIMARY KEY (" + GENERATED_IDS.get(table) + ") ENABLED");
            try {
                execute(connection, "CREATE TABLE " + schema + "." + table + " (" + String.join(", ", fields) + ")");
            } catch (SQLException exception) {
                var syntax = java.util.regex.Pattern.compile("[sS]yntax error at or near \"([A-Za-z_]+)\"").matcher(exception.getMessage());
                throw new AssertionError("Synthetic DDL failed for " + table + "; SQLState="
                        + exception.getSQLState() + "; token=" + (syntax.find() ? syntax.group(1) : "unknown")
                        + "; definitions=" + String.join(", ", fields));
            }
        }
    }

    private static void seed(Connection connection) throws Exception {
        execute(connection, "INSERT INTO customer_identity (customer_name) VALUES ('Acme')");
        for (String table : List.of("vertica_customer_detail", "vertica_customer_detail_stg", "vertica_customer_detail_dev")) {
            execute(connection, "INSERT INTO " + table + " (customer_name, customer_type) VALUES ('Acme', '정기점검 계약 고객사')");
        }
        execute(connection, "INSERT INTO customer_maintenance_schedule (customer_name, interval_months, anchor_month, enabled, effective_from, updated_by) "
                + "VALUES ('Acme', 1, DATE '2026-01-01', TRUE, DATE '2026-01-01', 'fixture')");
        for (String percentage : List.of("25 %", "120.125", "9999999999999999999999999999999999999999999", "invalid")) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO maintenance_records "
                    + "(customer_name, inspector_name, created_by_user_id, inspection_date, license_size_gb, license_usage_size, license_usage_pct) "
                    + "VALUES ('Acme', 'Fixture', 'owner', DATE '2026-10-01', '2048GB', '0.5TB', ?)")) {
                statement.setString(1, percentage);
                statement.executeUpdate();
            }
        }
        execute(connection, "INSERT INTO monthly_customer_response (created_by, created_by_user_id, response_date, customer_name, reason) "
                + "VALUES ('Fixture', 'owner', DATE '2026-10-01', 'Historical customer', 'Support')");
        execute(connection, "INSERT INTO troubleshooting (title, customer_name, creator, creator_user_id) "
                + "VALUES ('Fixture issue', 'Historical customer', 'Fixture', 'owner')");
        execute(connection, "INSERT INTO meeting_records (title, meeting_datetime, meeting_type, content, author_id, author_name) "
                + "VALUES ('Fixture meeting', CURRENT_TIMESTAMP, 'weekly', 'Preserved body', 'owner', 'Fixture')");
        execute(connection, "INSERT INTO meeting_comments (meeting_id, content, author_id, author_name) "
                + "SELECT meeting_id, 'Preserved comment', 'owner', 'Fixture' FROM meeting_records");
        execute(connection, "INSERT INTO meeting_comments (meeting_id, content, author_id, author_name) VALUES (99999999, 'Preserved orphan', 'owner', 'Fixture')");
    }

    private static void verifyBackfillAndConstraints(Connection connection) throws Exception {
        for (String table : CustomerReferenceSupport.TABLES) {
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM " + table
                    + " t WHERE customer_id IS NULL OR NOT EXISTS (SELECT 1 FROM customer_identity i WHERE i.customer_id = t.customer_id)"));
        }
        assertEquals(2, scalar(connection, "SELECT COUNT(*) FROM customer_identity"));
        assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM vertica_customer_detail"));
        assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM maintenance_records WHERE license_usage_pct_value = 25 AND license_capacity_tb = 2 AND license_used_tb = 0.5"));
        assertEquals(2, scalar(connection, "SELECT COUNT(*) FROM maintenance_records WHERE license_usage_pct_value IS NULL"));
        assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM meeting_comments WHERE archived_at IS NOT NULL AND content = 'Preserved orphan'"));
        for (String table : List.of("vertica_customer_detail_stg", "vertica_customer_detail_dev")) {
            assertThrows(SQLException.class, () -> execute(connection, "INSERT INTO " + table + " (customer_name) VALUES ('Acme')"));
            assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM " + table));
        }
    }

    private static void verifyRealWrites(Properties config, String schema, Connection connection) throws Exception {
        JdbcConnectionProvider provider = () -> isolated(config, schema);
        MaintenanceRecordDAO maintenance = new MaintenanceRecordDAO(provider, new SchemaCapabilityCache());
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setCustomerName("Acme");
        record.setCreatorUserId("owner");
        record.setInspectorName("Fixture");
        record.setInspectionDate(java.sql.Date.valueOf("2026-10-06"));
        record.setLicenseSizeGb("4096GB");
        record.setLicenseUsageSize("1TB");
        record.setLicenseUsagePct("25 %");
        assertTrue(maintenance.addMaintenanceRecord(record));
        assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM maintenance_records WHERE license_capacity_tb = 4 AND license_used_tb = 1 AND license_usage_pct_value = 25"));
        execute(connection, "UPDATE customer_identity SET customer_name = 'Renamed customer' WHERE customer_name = 'Acme'");
        assertEquals(5, maintenance.getMaintenanceRecordsByCustomer("Renamed customer", 1, 20).totalCount());
        assertNotNull(new CustomerDAO(provider).getCustomerByName("Renamed customer"));
        assertNotNull(new CustomerDetailDAO(provider).getCustomerDetails("Renamed customer").production());
        assertNotNull(new CustomerDetailDAO(provider).getCustomerDetailStg("Renamed customer"));
        execute(connection, "UPDATE customer_identity SET customer_name = 'Acme' WHERE customer_name = 'Renamed customer'");

        MonthlyCustomerResponseDTO response = new MonthlyCustomerResponseDTO();
        response.setUserId("owner");
        response.setUserName("Fixture");
        response.setCustomerName("New external customer");
        response.setResponseDate(java.sql.Date.valueOf("2026-10-06"));
        response.setReason("Support");
        MonthlyCustomerResponseDAO monthly = new MonthlyCustomerResponseDAO(provider);
        assertTrue(monthly.addResponse(response));
        response.setId((int) scalar(connection, "SELECT MAX(id) FROM monthly_customer_response"));
        response.setUserId("attacker");
        response.setCustomerName("Denied customer");
        assertFalse(monthly.updateResponse(response));
        assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM customer_identity WHERE customer_name = 'Denied customer'"));

        TroubleshootingDTO trouble = new TroubleshootingDTO();
        trouble.setCustomerName("Failed customer");
        trouble.setCreatorUserId("owner");
        trouble.setCreator("Fixture");
        assertThrows(DataAccessException.class, () -> new TroubleshootingDAO(provider, new SchemaCapabilityCache()).addTroubleshooting(trouble));
        assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM customer_identity WHERE customer_name = 'Failed customer'"));

        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName("New master customer");
        assertTrue(new CustomerDAO(provider).addCustomer(customer, "owner"));
        assertNotNull(new CustomerDAO(provider).getCustomerById(customer.getCustomerId()));
        CustomerDetailDTO detail = new CustomerDetailDTO();
        detail.setCustomerName("New master customer");
        CustomerDetailDAO details = new CustomerDetailDAO(provider);
        assertTrue(details.saveOrUpdateCustomerDetailStg(detail));
        assertTrue(details.saveOrUpdateCustomerDetailDev(detail));
        for (String table : List.of("vertica_customer_detail", "vertica_customer_detail_stg", "vertica_customer_detail_dev")) {
            assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM " + table + " t JOIN customer_identity i ON t.customer_id = i.customer_id WHERE i.customer_name = 'New master customer'"));
        }
    }

    private static void verifyMeetingLifecycle(Properties config, String schema, Connection connection) throws Exception {
        JdbcConnectionProvider provider = () -> isolated(config, schema);
        MeetingRecordDAO meetings = new MeetingRecordDAO(provider);
        MeetingCommentDAO comments = new MeetingCommentDAO(provider);
        long id = scalar(connection, "SELECT MIN(meeting_id) FROM meeting_records");
        assertFalse(meetings.deleteMeetingRecordForAuthor(id, "attacker"));
        MeetingCommentDTO comment = new MeetingCommentDTO();
        comment.setMeetingId(id);
        comment.setContent("Racing comment");
        comment.setAuthorId("owner");
        comment.setAuthorName("Fixture");
        try (var executor = Executors.newSingleThreadExecutor(); Connection blocker = isolated(config, schema)) {
            blocker.setAutoCommit(false);
            execute(blocker, "LOCK TABLE meeting_records IN SHARE MODE");
            var deletion = executor.submit(() -> meetings.deleteMeetingRecordForAuthor(id, "owner"));
            assertTrue(comments.addComment(comment));
            blocker.commit();
            assertTrue(deletion.get(10, TimeUnit.SECONDS));
        }
        assertFalse(comments.addComment(comment));
        assertNull(meetings.getMeetingRecord(id));
        assertEquals(0, meetings.getMeetingPage(1).totalCount());
        assertEquals(0, meetings.getTotalCount());
        assertTrue(meetings.searchMeetingRecords("Fixture", 5).isEmpty());
        assertTrue(comments.getCommentsByMeetingId(id).isEmpty());
        assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM meeting_records WHERE content = 'Preserved body' AND deleted_by = 'owner'"));
        assertEquals(3, scalar(connection, "SELECT COUNT(*) FROM meeting_comments"));
    }

    private static Properties config() throws Exception {
        String value = System.getenv("FROG2_INTEGRITY_DB_CONFIG");
        if (value == null) throw new IllegalArgumentException("External integrity DB config is required");
        Path path = Path.of(value).toRealPath();
        if (!path.startsWith(Path.of("/opt/frog2-dev")) || !Files.isRegularFile(path)) throw new IllegalArgumentException("Invalid integrity DB config path");
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) { properties.load(input); }
        return properties;
    }

    private static Connection open(Properties config) throws SQLException {
        return DriverManager.getConnection(config.getProperty("db.url"), config.getProperty("db.user"), config.getProperty("db.password"));
    }

    private static Connection isolated(Properties config, String schema) throws SQLException {
        Connection delegate = open(config);
        try {
            execute(delegate, "SET SEARCH_PATH TO " + schema);
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                try {
                    if ("getSchema".equals(method.getName())) return schema;
                    if ("getMetaData".equals(method.getName())) {
                        DatabaseMetaData metadata = delegate.getMetaData();
                        return Proxy.newProxyInstance(DatabaseMetaData.class.getClassLoader(), new Class<?>[] {DatabaseMetaData.class}, (ignored, call, values) -> {
                            if (!"getColumns".equals(call.getName())) throw new SQLException("Only isolated column metadata is allowed");
                            Object[] scoped = values.clone();
                            scoped[1] = schema;
                            try { return call.invoke(metadata, scoped); }
                            catch (InvocationTargetException exception) { throw exception.getCause(); }
                        });
                    }
                    return method.invoke(delegate, args);
                } catch (InvocationTargetException exception) { throw exception.getCause(); }
            });
        } catch (SQLException exception) {
            delegate.close();
            throw exception;
        }
    }

    private static void apply(Connection connection, String migration) throws Exception {
        String source = Files.readString(Path.of("src/main/resources/db/migration", migration));
        String sql = source.lines().filter(line -> !line.stripLeading().startsWith("--")).collect(java.util.stream.Collectors.joining("\n"));
        for (String statement : sql.split(";")) if (!statement.isBlank()) execute(connection, statement);
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.setQueryTimeout(15); statement.execute(sql); }
    }

    private static long scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getLong(1);
        }
    }

    private static Map<String, Long> counts(Connection connection, String schema) throws SQLException {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : TABLES) counts.put(table, scalar(connection, "SELECT COUNT(*) FROM " + schema + "." + table));
        return counts;
    }
}
