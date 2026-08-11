package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DatabaseSchemaReadinessTest {
    private static final Set<String> ALL_REQUIRED_COLUMNS = Set.of(
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

    @Test
    void allActiveMigrationCapabilitiesReportReady() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ALL_REQUIRED_COLUMNS;

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertTrue(report.ready());
        assertTrue(report.missingRequirements().isEmpty());
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
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
}
