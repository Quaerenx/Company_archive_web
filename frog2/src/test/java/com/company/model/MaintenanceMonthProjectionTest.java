package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.util.LicenseRiskPolicy.Level;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MaintenanceMonthProjectionTest {
    private static final Date MONTH_START = Date.valueOf("2026-08-01");
    private static final Date NEXT_MONTH_START = Date.valueOf("2026-09-01");
    private static final Set<String> LEGACY_LICENSE_COLUMNS = Set.of(
            "maintenance_records.license_size_gb",
            "maintenance_records.license_usage_size",
            "maintenance_records.license_usage_pct");

    @Test
    void monthStatesReadOnlyStateFieldsWithoutOptionalLicenseColumns() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "customer_name", "Alpha", "inspector_name", "Tester",
                "inspection_date", Date.valueOf("2026-08-10")));

        List<MaintenanceMonthState> states = dao(jdbc).getMaintenanceMonthStates(
                MONTH_START, NEXT_MONTH_START);

        PaginationJdbcFixture.StatementRecord query = jdbc.statements.getFirst();
        assertEquals("customer_name, inspector_name, inspection_date", projection(query.sql));
        assertTrue(query.sql.contains("inspection_date >= ? AND inspection_date < ?"));
        assertTrue(query.sql.endsWith("ORDER BY inspection_date ASC, customer_name ASC"));
        assertEquals(MONTH_START, query.parameters.get(1));
        assertEquals(NEXT_MONTH_START, query.parameters.get(2));
        assertEquals(new MaintenanceMonthState("Alpha", "Tester", LocalDate.of(2026, 8, 10),
                Level.UNAVAILABLE), states.getFirst());
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void legacyMonthlyLicenseRiskKeepsCapacityUsageAndPercentageFallbacks() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = LEGACY_LICENSE_COLUMNS;
        jdbc.enqueue(
                PaginationJdbcFixture.row(
                        "customer_name", "Alpha", "inspection_date", MONTH_START,
                        "license_size_gb", "1 TB", "license_usage_size", "921.6 GB"),
                PaginationJdbcFixture.row(
                        "customer_name", "Alpha", "inspection_date", Date.valueOf("2026-08-02"),
                        "license_size_gb", "1 TB", "license_usage_size", "0.4 TB",
                        "license_usage_pct", "110%"));

        List<MaintenanceMonthState> states = dao(jdbc).getMaintenanceMonthStates(
                MONTH_START, NEXT_MONTH_START);

        assertEquals(List.of(Level.WARNING, Level.RISK),
                states.stream().map(MaintenanceMonthState::licenseRiskLevel).toList());
        assertEquals(List.of(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2)),
                states.stream().map(MaintenanceMonthState::inspectionDate).toList());
        assertNarrowMonthlyProjection(jdbc.statements.getFirst().sql);
    }

    @Test
    void numericLicenseValuesOverrideLegacyValuesAndNullNumericValuesKeepLegacyFallbacks() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "maintenance_records.license_size_gb", "maintenance_records.license_usage_size",
                "maintenance_records.license_usage_pct", "maintenance_records.license_capacity_tb",
                "maintenance_records.license_used_tb", "maintenance_records.license_usage_pct_value");
        jdbc.enqueue(
                PaginationJdbcFixture.row(
                        "customer_name", "Alpha", "inspection_date", MONTH_START,
                        "license_usage_pct", "20%", "license_usage_pct_value", "95"),
                PaginationJdbcFixture.row(
                        "customer_name", "Beta", "inspection_date", MONTH_START,
                        "license_usage_pct", "110%", "license_usage_pct_value", null),
                PaginationJdbcFixture.row(
                        "customer_name", "Gamma", "inspection_date", MONTH_START,
                        "license_size_gb", "10 TB", "license_usage_size", "1 TB",
                        "license_capacity_tb", "1", "license_used_tb", "0.95"));

        List<MaintenanceMonthState> states = dao(jdbc).getMaintenanceMonthStates(
                MONTH_START, NEXT_MONTH_START);

        assertEquals(List.of(Level.WARNING, Level.RISK, Level.WARNING),
                states.stream().map(MaintenanceMonthState::licenseRiskLevel).toList());
        assertNarrowMonthlyProjection(jdbc.statements.getFirst().sql);
    }

    @Test
    void partialNumericSchemaFailsBeforeReadingMonthlyStates() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of("maintenance_records.license_capacity_tb");

        assertThrows(DataAccessException.class,
                () -> dao(jdbc).getMaintenanceMonthStates(MONTH_START, NEXT_MONTH_START));

        assertEquals(0, jdbc.statements.size());
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void registeredNamesUseOneDistinctCustomerOnlyQueryWithNormalizedBoundNames() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row("customer_name", "Alpha"),
                PaginationJdbcFixture.row("customer_name", "Alpha"),
                PaginationJdbcFixture.row("customer_name", "Beta"));

        Set<String> registered = dao(jdbc).getRegisteredCustomerNamesByMonth(
                MONTH_START, NEXT_MONTH_START, List.of(" Alpha ", "Beta", "Alpha", " "));

        assertEquals(Set.of("Alpha", "Beta"), registered);
        assertEquals(1, jdbc.statements.size());
        PaginationJdbcFixture.StatementRecord query = jdbc.statements.getFirst();
        assertEquals("DISTINCT customer_name", projection(query.sql));
        assertTrue(query.sql.contains("customer_name IN (?, ?)"));
        assertEquals(MONTH_START, query.parameters.get(1));
        assertEquals(NEXT_MONTH_START, query.parameters.get(2));
        assertEquals("Alpha", query.parameters.get(3));
        assertEquals("Beta", query.parameters.get(4));
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void registeredNamesKeepUuidRelationshipPredicateWhenIdentitySchemaExists() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of("maintenance_records.customer_id",
                "customer_identity.customer_id", "customer_identity.customer_name");
        jdbc.enqueue(PaginationJdbcFixture.row("customer_name", "Alpha"));

        assertEquals(Set.of("Alpha"), dao(jdbc).getRegisteredCustomerNamesByMonth(
                MONTH_START, NEXT_MONTH_START, List.of("Alpha")));

        assertTrue(jdbc.statements.getFirst().sql.contains(
                "customer_id IN (SELECT customer_id FROM customer_identity WHERE customer_name IN (?))"));
        assertEquals("DISTINCT customer_name", projection(jdbc.statements.getFirst().sql));
    }

    @Test
    void registeredNamesRejectIncompleteIdentitySchema() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of("maintenance_records.customer_id",
                "customer_identity.customer_id");

        assertThrows(DataAccessException.class, () -> dao(jdbc).getRegisteredCustomerNamesByMonth(
                MONTH_START, NEXT_MONTH_START, List.of("Alpha")));

        assertEquals(0, jdbc.statements.size());
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void noAssignedCustomersSkipsRegistrationDatabaseRead() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();

        assertEquals(Set.of(), dao(jdbc).getRegisteredCustomerNamesByMonth(
                MONTH_START, NEXT_MONTH_START, List.of(" ")));
        assertEquals(0, jdbc.openCount);
    }

    private static MaintenanceRecordDAO dao(PaginationJdbcFixture jdbc) {
        return new MaintenanceRecordDAO(jdbc::open, new SchemaCapabilityCache());
    }

    private static String projection(String sql) {
        return sql.substring("SELECT ".length(), sql.indexOf(" FROM "));
    }

    private static void assertNarrowMonthlyProjection(String sql) {
        for (String unused : List.of("note", "vertica_version", "created_at", "updated_at",
                "created_by_user_id", "maintenance_id")) {
            assertFalse(projection(sql).contains(unused), unused);
        }
    }
}
