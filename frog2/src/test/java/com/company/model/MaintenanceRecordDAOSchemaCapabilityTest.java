package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MaintenanceRecordDAOSchemaCapabilityTest {
    @Test
    void reusesOptionalColumnCapabilityAcrossDaoInstances() {
        AtomicInteger metadataQueries = new AtomicInteger();
        Connection connection = connection(metadataQueries);
        SchemaCapabilityCache sharedCache = new SchemaCapabilityCache();
        MaintenanceRecordDAO first = new MaintenanceRecordDAO(sharedCache);
        MaintenanceRecordDAO second = new MaintenanceRecordDAO(sharedCache);

        assertTrue(first.columnExists(
                connection, "maintenance_records", "license_size_gb"));
        assertTrue(second.columnExists(
                connection, "maintenance_records", "license_size_gb"));

        assertEquals(1, metadataQueries.get());
    }

    @Test
    void partialSchemaKeepsTheLegacyBaseProjection() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(baseRecord(17L));
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        MaintenanceRecordDTO record = dao.getMaintenanceRecordById(17L);

        String sql = jdbc.statements.getFirst().sql;
        assertFalse(sql.contains("created_by_user_id"));
        assertFalse(sql.contains("license_size_gb"));
        assertFalse(sql.contains("license_usage_size"));
        assertFalse(sql.contains("license_usage_pct"));
        assertNull(record.getCreatorUserId());
        assertNull(record.getLicenseSizeGb());
        assertNull(record.getLicenseUsageSize());
        assertNull(record.getLicenseUsagePct());
    }

    @Test
    void fullSchemaKeepsTheExtendedProjectionAndMapping() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "maintenance_records.created_by_user_id",
                "maintenance_records.license_size_gb",
                "maintenance_records.license_usage_size",
                "maintenance_records.license_usage_pct");
        jdbc.enqueue(PaginationJdbcFixture.row(
                "maintenance_id", 17L,
                "customer_name", "Acme",
                "inspector_name", "Alice",
                "inspection_date", Date.valueOf("2026-08-12"),
                "vertica_version", "23.4",
                "note", "Checked",
                "created_at", null,
                "updated_at", null,
                "created_by_user_id", "user-17",
                "license_size_gb", "80",
                "license_usage_size", "50",
                "license_usage_pct", "62.5"));
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        MaintenanceRecordDTO record = dao.getMaintenanceRecordById(17L);

        String sql = jdbc.statements.getFirst().sql;
        assertTrue(sql.contains(
                "created_by_user_id, license_size_gb, "
                        + "license_usage_size, license_usage_pct"));
        assertEquals("user-17", record.getCreatorUserId());
        assertEquals("80", record.getLicenseSizeGb());
        assertEquals("50", record.getLicenseUsageSize());
        assertEquals("62.5", record.getLicenseUsagePct());
    }

    @Test
    void fullSchemaKeepsTheOptionalInsertColumnsAndParameterOrder() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "maintenance_records.created_by_user_id",
                "maintenance_records.license_size_gb",
                "maintenance_records.license_usage_size",
                "maintenance_records.license_usage_pct");
        jdbc.enqueueUpdate(1);
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setCustomerName("Acme");
        record.setInspectorName("Alice");
        record.setCreatorUserId("user-17");
        record.setInspectionDate(Date.valueOf("2026-08-12"));
        record.setVerticaVersion("23.4");
        record.setNote("Checked");
        record.setLicenseSizeGb("80");
        record.setLicenseUsageSize("50");
        record.setLicenseUsagePct("62.5");

        assertTrue(dao.addMaintenanceRecord(record));

        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertEquals(
                "INSERT INTO maintenance_records (customer_name, "
                        + "inspector_name, created_by_user_id, "
                        + "inspection_date, vertica_version, note, "
                        + "license_size_gb, license_usage_size, "
                        + "license_usage_pct) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                statement.sql);
        assertEquals("80", statement.parameters.get(7));
        assertEquals("50", statement.parameters.get(8));
        assertEquals("62.5", statement.parameters.get(9));
    }

    @Test
    void mixedLicenseSchemasKeepProjectionAndMappingAligned() {
        for (LicenseSchemaCase schema : licenseSchemaCases()) {
            PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
            jdbc.availableColumns = schema.availableColumns(true);
            jdbc.enqueue(extendedRecord(17L));
            MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                    jdbc::open, new SchemaCapabilityCache());

            MaintenanceRecordDTO record = dao.getMaintenanceRecordById(17L);

            assertEquals(
                    schema.expectedSelectColumns(true),
                    selectList(jdbc.statements.getFirst().sql),
                    schema.name());
            assertEquals("user-17", record.getCreatorUserId(), schema.name());
            assertEquals(
                    schema.hasLicenseSize() ? "80" : null,
                    record.getLicenseSizeGb(),
                    schema.name());
            assertEquals(
                    schema.hasLicenseUsageSize() ? "50" : null,
                    record.getLicenseUsageSize(),
                    schema.name());
            assertEquals(
                    schema.hasLicenseUsagePct() ? "62.5" : null,
                    record.getLicenseUsagePct(),
                    schema.name());
        }
    }

    @Test
    void mixedLicenseSchemasKeepInsertColumnsAndParameterOrderAligned() {
        for (LicenseSchemaCase schema : licenseSchemaCases()) {
            PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
            jdbc.availableColumns = schema.availableColumns(true);
            jdbc.enqueueUpdate(1);
            MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                    jdbc::open, new SchemaCapabilityCache());
            MaintenanceRecordDTO record = populatedRecord();

            assertTrue(dao.addMaintenanceRecord(record), schema.name());

            PaginationJdbcFixture.StatementRecord statement =
                    jdbc.statements.getFirst();
            assertEquals(
                    schema.expectedInsertSql(),
                    statement.sql,
                    schema.name());
            assertEquals(
                    schema.expectedInsertParameters(),
                    new ArrayList<>(statement.parameters.values()),
                    schema.name());
        }
    }

    @Test
    void mixedLicenseSchemasKeepUpdateColumnsAndParameterOrderAligned() {
        for (LicenseSchemaCase schema : licenseSchemaCases()) {
            PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
            jdbc.availableColumns = schema.availableColumns(true);
            jdbc.enqueueUpdate(1);
            MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                    jdbc::open, new SchemaCapabilityCache());
            MaintenanceRecordDTO record = populatedRecord();

            assertTrue(
                    dao.updateMaintenanceRecordForOwner(record, "user-17"),
                    schema.name());

            PaginationJdbcFixture.StatementRecord statement =
                    jdbc.statements.getFirst();
            assertEquals(
                    schema.expectedUpdateSql(),
                    statement.sql,
                    schema.name());
            assertEquals(
                    schema.expectedUpdateParameters(),
                    new ArrayList<>(statement.parameters.values()),
                    schema.name());
        }
    }

    private static Map<String, Object> baseRecord(long id) {
        return PaginationJdbcFixture.row(
                "maintenance_id", id,
                "customer_name", "Acme",
                "inspector_name", "Alice",
                "inspection_date", Date.valueOf("2026-08-12"),
                "vertica_version", "23.4",
                "note", "Checked",
                "created_at", null,
                "updated_at", null);
    }

    private static Map<String, Object> extendedRecord(long id) {
        Map<String, Object> row = baseRecord(id);
        row.put("created_by_user_id", "user-17");
        row.put("license_size_gb", "80");
        row.put("license_usage_size", "50");
        row.put("license_usage_pct", "62.5");
        return row;
    }

    private static MaintenanceRecordDTO populatedRecord() {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setMaintenanceId(17L);
        record.setCustomerName("Acme");
        record.setInspectorName("Alice");
        record.setCreatorUserId("user-17");
        record.setInspectionDate(Date.valueOf("2026-08-12"));
        record.setVerticaVersion("23.4");
        record.setNote("Checked");
        record.setLicenseSizeGb("80");
        record.setLicenseUsageSize("50");
        record.setLicenseUsagePct("62.5");
        return record;
    }

    private static String selectList(String sql) {
        return sql.substring("SELECT ".length(), sql.indexOf(" FROM "));
    }

    private static List<LicenseSchemaCase> licenseSchemaCases() {
        return List.of(
                new LicenseSchemaCase("none", false, false, false),
                new LicenseSchemaCase("size-only", true, false, false),
                new LicenseSchemaCase("usage-only", false, true, false),
                new LicenseSchemaCase("percentage-only", false, false, true),
                new LicenseSchemaCase("size-and-usage", true, true, false),
                new LicenseSchemaCase(
                        "size-and-percentage", true, false, true),
                new LicenseSchemaCase(
                        "usage-and-percentage", false, true, true),
                new LicenseSchemaCase("all", true, true, true));
    }

    private record LicenseSchemaCase(
            String name,
            boolean hasLicenseSize,
            boolean hasLicenseUsageSize,
            boolean hasLicenseUsagePct) {
        Set<String> availableColumns(boolean includeCreator) {
            Set<String> columns = new java.util.LinkedHashSet<>();
            if (includeCreator) {
                columns.add("maintenance_records.created_by_user_id");
            }
            if (hasLicenseSize) {
                columns.add("maintenance_records.license_size_gb");
            }
            if (hasLicenseUsageSize) {
                columns.add("maintenance_records.license_usage_size");
            }
            if (hasLicenseUsagePct) {
                columns.add("maintenance_records.license_usage_pct");
            }
            return Set.copyOf(columns);
        }

        String expectedSelectColumns(boolean includeCreator) {
            List<String> columns = new ArrayList<>(List.of(
                    "maintenance_id",
                    "customer_name",
                    "inspector_name",
                    "inspection_date",
                    "vertica_version",
                    "note",
                    "created_at",
                    "updated_at"));
            if (includeCreator) columns.add("created_by_user_id");
            addLicenseColumns(columns);
            return String.join(", ", columns);
        }

        String expectedInsertSql() {
            List<String> columns = new ArrayList<>(List.of(
                    "customer_name",
                    "inspector_name",
                    "created_by_user_id",
                    "inspection_date",
                    "vertica_version",
                    "note"));
            addLicenseColumns(columns);
            return "INSERT INTO maintenance_records ("
                    + String.join(", ", columns)
                    + ") VALUES ("
                    + String.join(", ", java.util.Collections.nCopies(
                            columns.size(), "?"))
                    + ")";
        }

        List<Object> expectedInsertParameters() {
            List<Object> parameters = new ArrayList<>(List.of(
                    "Acme",
                    "Alice",
                    "user-17",
                    Date.valueOf("2026-08-12"),
                    "23.4",
                    "Checked"));
            addLicenseValues(parameters);
            return parameters;
        }

        String expectedUpdateSql() {
            List<String> assignments = new ArrayList<>(List.of(
                    "customer_name = ?",
                    "inspector_name = ?",
                    "inspection_date = ?",
                    "vertica_version = ?",
                    "note = ?"));
            if (hasLicenseSize) assignments.add("license_size_gb = ?");
            if (hasLicenseUsageSize) {
                assignments.add("license_usage_size = ?");
            }
            if (hasLicenseUsagePct) assignments.add("license_usage_pct = ?");
            return "UPDATE maintenance_records SET "
                    + String.join(", ", assignments)
                    + ", updated_at = statement_timestamp() "
                    + "WHERE maintenance_id = ? AND created_by_user_id = ?";
        }

        List<Object> expectedUpdateParameters() {
            List<Object> parameters = new ArrayList<>(List.of(
                    "Acme",
                    "Alice",
                    Date.valueOf("2026-08-12"),
                    "23.4",
                    "Checked"));
            addLicenseValues(parameters);
            parameters.add(17L);
            parameters.add("user-17");
            return parameters;
        }

        private void addLicenseColumns(List<String> columns) {
            if (hasLicenseSize) columns.add("license_size_gb");
            if (hasLicenseUsageSize) columns.add("license_usage_size");
            if (hasLicenseUsagePct) columns.add("license_usage_pct");
        }

        private void addLicenseValues(List<Object> values) {
            if (hasLicenseSize) values.add("80");
            if (hasLicenseUsageSize) values.add("50");
            if (hasLicenseUsagePct) values.add("62.5");
        }
    }

    private static Connection connection(AtomicInteger metadataQueries) {
        DatabaseMetaData metadata = (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[] {DatabaseMetaData.class},
                (ignored, call, args) -> {
                    if ("getColumns".equals(call.getName())) {
                        metadataQueries.incrementAndGet();
                        return resultSet(true);
                    }
                    return defaultValue(call.getReturnType());
                });
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (ignored, call, args) -> "getMetaData".equals(call.getName())
                        ? metadata
                        : defaultValue(call.getReturnType()));
    }

    private static ResultSet resultSet(boolean hasRow) {
        AtomicBoolean first = new AtomicBoolean(hasRow);
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "next" -> first.getAndSet(false);
                    default -> defaultValue(call.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
