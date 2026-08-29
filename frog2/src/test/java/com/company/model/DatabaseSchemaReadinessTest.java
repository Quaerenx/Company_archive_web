package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DatabaseSchemaReadinessTest {
    private static final Set<String> OPTIONAL_COLUMNS = Set.of(
            "vertica_customer_detail.updated_at",
            "vertica_customer_detail.updated_by",
            "vertica_customer_detail.deleted_at",
            "vertica_customer_detail.deleted_by",
            "company_users.department");
    private static final Set<String> BASE_REQUIRED_COLUMNS = Set.of(
            "user_vm_hosts.ip",
            "user_vm_hosts.owner_user_id",
            "user_vm_hosts.owner_user_name",
            "user_vm_hosts.purpose",
            "user_vm_hosts.os_info",
            "user_vm_hosts.vertica_version",
            "user_vm_hosts.remote_host",
            "user_vm_hosts.note",
            "user_vm_hosts.status",
            "user_vm_hosts.created_at",
            "user_vm_hosts.updated_at",
            "maintenance_records.license_usage_pct",
            "maintenance_records.license_size_gb",
            "maintenance_records.license_usage_size",
            "troubleshooting.creator_user_id",
            "maintenance_records.created_by_user_id",
            "monthly_customer_response.created_by_user_id",
            "customer_maintenance_schedule.interval_months",
            "customer_maintenance_schedule.anchor_month",
            "customer_maintenance_schedule.enabled",
            "customer_maintenance_schedule.effective_from",
            "customer_maintenance_schedule.effective_to",
            "customer_maintenance_schedule.updated_by",
            "customer_maintenance_schedule.updated_at");
    private static final Set<String> ALL_REQUIRED_COLUMNS =
            allRequiredColumns();

    private static Set<String> allRequiredColumns() {
        Set<String> columns = new HashSet<>(BASE_REQUIRED_COLUMNS);
        for (CustomerDetailEnvironment environment
                : CustomerDetailEnvironment.values()) {
            for (String column : CustomerDetailDAO.requiredColumnNames()) {
                columns.add(environment.tableName() + "." + column);
            }
        }
        columns.add("vertica_customer_detail.is_deleted");
        return Set.copyOf(columns);
    }

    @Test
    void legacyRecordConstructorsRemainRequiredByDefault() {
        DatabaseSchemaReadiness.Requirement requirement =
                new DatabaseSchemaReadiness.Requirement(
                        "legacy", "sample_table", "sample_column");
        DatabaseSchemaReadiness.Report report =
                new DatabaseSchemaReadiness.Report(List.of(requirement));

        assertTrue(requirement.required());
        assertFalse(report.ready());
        assertEquals(List.of(requirement), report.missingRequirements());
        assertTrue(report.missingOptionalRequirements().isEmpty());
    }

    @Test
    void allActiveMigrationCapabilitiesReportReady() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ALL_REQUIRED_COLUMNS;

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertTrue(report.ready());
        assertTrue(report.missingRequirements().isEmpty());
        assertEquals(OPTIONAL_COLUMNS, report.missingOptionalRequirements().stream()
                .map(requirement -> requirement.tableName() + "."
                        + requirement.columnName())
                .collect(java.util.stream.Collectors.toUnmodifiableSet()));
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void missingMaintenanceLicenseDetailIsRequired() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ALL_REQUIRED_COLUMNS.stream()
                .filter(column -> !column.equals(
                        "maintenance_records.license_usage_size"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertFalse(report.ready());
        assertEquals(1, report.missingRequirements().size());
        assertEquals(
                "BASELINE_MAINTENANCE_LICENSE_DETAILS",
                report.missingRequirements().getFirst().migrationVersion());
        assertEquals(
                "license_usage_size",
                report.missingRequirements().getFirst().columnName());
    }

    @Test
    void missingOptionalDepartmentDoesNotBlockReadiness() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ALL_REQUIRED_COLUMNS;

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertTrue(report.ready());
        assertTrue(report.missingRequirements().isEmpty());
        assertEquals(OPTIONAL_COLUMNS.size(),
                report.missingOptionalRequirements().size());
        assertTrue(report.missingOptionalRequirements().stream()
                .noneMatch(DatabaseSchemaReadiness.Requirement::required));
    }

    @Test
    void legacyUpdatedAtAloneDoesNotBlockReadiness() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        java.util.Set<String> available = new java.util.HashSet<>(
                ALL_REQUIRED_COLUMNS);
        available.add("vertica_customer_detail.updated_at");
        jdbc.availableColumns = Set.copyOf(available);

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertTrue(report.ready());
        assertTrue(report.missingRequirements().isEmpty());
        assertEquals(
                Set.of("updated_by", "deleted_at", "deleted_by"),
                report.missingOptionalRequirements().stream()
                        .filter(requirement -> "V20260825_09".equals(
                                requirement.migrationVersion()))
                        .map(DatabaseSchemaReadiness.Requirement::columnName)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void partiallyAppliedCustomerAuditMigrationBlocksReadiness() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        java.util.Set<String> available = new java.util.HashSet<>(
                ALL_REQUIRED_COLUMNS);
        available.add("vertica_customer_detail.updated_at");
        available.add("vertica_customer_detail.updated_by");
        jdbc.availableColumns = Set.copyOf(available);

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertFalse(report.ready());
        assertEquals(Set.of("deleted_at", "deleted_by"),
                report.missingRequirements().stream()
                        .map(DatabaseSchemaReadiness.Requirement::columnName)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
        assertTrue(report.missingRequirements().stream()
                .allMatch(DatabaseSchemaReadiness.Requirement::required));
        assertTrue(report.missingOptionalRequirements().stream()
                .noneMatch(requirement -> "V20260825_09".equals(
                        requirement.migrationVersion())));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void missingCapabilityReportsItsMigrationWithoutExecutingSql() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ALL_REQUIRED_COLUMNS.stream()
                .filter(column -> !column.equals(
                        "monthly_customer_response.created_by_user_id"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertFalse(report.ready());
        assertEquals(1, report.missingRequirements().size());
        assertEquals(
                "V20260731_06",
                report.missingRequirements().getFirst().migrationVersion());
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void incompleteScheduleContractIsNotReportedReady() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ALL_REQUIRED_COLUMNS.stream()
                .filter(column -> !column.equals(
                        "customer_maintenance_schedule.anchor_month"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertFalse(report.ready());
        assertEquals("V20260804_07",
                report.missingRequirements().getFirst().migrationVersion());
        assertEquals("anchor_month",
                report.missingRequirements().getFirst().columnName());
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void missingCustomerDetailColumnBlocksReadinessWithoutExecutingSql() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ALL_REQUIRED_COLUMNS.stream()
                .filter(column -> !column.equals(
                        "vertica_customer_detail_stg.storage_network"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertFalse(report.ready());
        assertEquals(1, report.missingRequirements().size());
        DatabaseSchemaReadiness.Requirement missing =
                report.missingRequirements().getFirst();
        assertEquals("BASELINE_CUSTOMER_DETAIL",
                missing.migrationVersion());
        assertEquals("vertica_customer_detail_stg",
                missing.tableName());
        assertEquals("storage_network", missing.columnName());
        assertTrue(jdbc.statements.isEmpty());
    }
}
