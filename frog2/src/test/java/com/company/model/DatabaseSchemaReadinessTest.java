package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
            "vertica_customer_detail.main_manager_user_id",
            "vertica_customer_detail.sub_manager_user_id",
            "customer_identity.customer_id",
            "customer_identity.customer_name",
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
    void successfulInspectionWarmsRequiredAndAbsentOptionalCapabilities() throws Exception {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ALL_REQUIRED_COLUMNS;
        SchemaCapabilityCache warmed = new SchemaCapabilityCache();
        assertTrue(DatabaseSchemaReadiness.inspect(jdbc::open, warmed).ready());
        com.company.performance.RequestPerformanceContext.begin();
        com.company.performance.RequestPerformanceContext.Snapshot timing;
        try (java.sql.Connection connection = jdbc.open()) {
            assertTrue(warmed.columnExists(connection, "maintenance_records", "license_usage_pct"));
            assertTrue(warmed.columnExists(connection, "customer_maintenance_schedule", "interval_months"));
            assertFalse(warmed.columnExists(connection, "company_users", "department"));
        } finally {
            timing = com.company.performance.RequestPerformanceContext.finish();
        }
        assertEquals(0, timing.metadataCount());
    }

    @Test
    void failedRefreshKeepsPreviouslyVerifiedCapabilities() throws Exception {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ALL_REQUIRED_COLUMNS;
        SchemaCapabilityCache warmed = new SchemaCapabilityCache();
        DatabaseSchemaReadiness.inspect(jdbc::open, warmed);
        assertThrows(DataAccessException.class, () -> DatabaseSchemaReadiness.inspect(
                () -> { throw new java.sql.SQLException("Transient probe failure", "08006"); }, warmed));
        try (java.sql.Connection connection = jdbc.open()) {
            assertTrue(warmed.columnExists(connection, "maintenance_records", "license_usage_pct"));
        }
    }

    @Test
    void allColumnsUseOneMetadataQueryPerTable() {
        SchemaMetadataJdbcFixture jdbc = completeMetadata();
        com.company.performance.RequestPerformanceContext.begin();
        com.company.performance.RequestPerformanceContext.Snapshot timing;
        try {
            assertTrue(DatabaseSchemaReadiness.inspect(jdbc::open).ready());
        } finally {
            timing = com.company.performance.RequestPerformanceContext.finish();
        }

        assertEquals(184, jdbc.columns.size());
        assertEquals(10, jdbc.queries.size());
        assertEquals(10, timing.metadataCount());
        assertEquals(0, timing.sqlCount());
        assertTrue(jdbc.queries.stream().allMatch(query -> query.catalog() == null
                && query.schema() == null && "%".equals(query.column())));
        assertEquals(10, jdbc.resultCloses);
        assertEquals(1, jdbc.connectionCloses);
    }

    @Test
    void emptySchemaReportsMissingRequirementsWithTwoQueriesPerTable() {
        SchemaMetadataJdbcFixture jdbc = new SchemaMetadataJdbcFixture();

        DatabaseSchemaReadiness.Report report = DatabaseSchemaReadiness.inspect(jdbc::open);

        assertFalse(report.ready());
        assertEquals(ALL_REQUIRED_COLUMNS.size(), report.missingRequirements().size());
        assertEquals(OPTIONAL_COLUMNS.size(), report.missingOptionalRequirements().size());
        assertEquals(20, jdbc.queries.size());
        assertEquals(20, jdbc.resultCloses);
    }

    @Test
    void uppercaseSchemaRemainsReadyUsingOneFallbackPerTable() {
        SchemaMetadataJdbcFixture jdbc = completeMetadata();
        List<SchemaMetadataJdbcFixture.Column> lowercase = List.copyOf(jdbc.columns);
        jdbc.columns.clear();
        for (var column : lowercase) {
            jdbc.add(column.schema(), column.table().toUpperCase(java.util.Locale.ROOT),
                    column.name().toUpperCase(java.util.Locale.ROOT));
        }

        DatabaseSchemaReadiness.Report report = DatabaseSchemaReadiness.inspect(jdbc::open);

        assertTrue(report.ready());
        assertTrue(report.missingOptionalRequirements().isEmpty());
        assertEquals(20, jdbc.queries.size());
        assertEquals(20, jdbc.resultCloses);
    }

    @Test
    void failedConnectionCloseDoesNotPublishRefreshedCapabilities() {
        SchemaMetadataJdbcFixture jdbc = completeMetadata();
        SchemaCapabilityCache warmed = new SchemaCapabilityCache();
        DatabaseSchemaReadiness.inspect(jdbc::open, warmed);
        jdbc.columns.clear();
        jdbc.failConnectionClose = true;

        assertThrows(DataAccessException.class,
                () -> DatabaseSchemaReadiness.inspect(jdbc::open, warmed));

        jdbc.failConnectionClose = false;
        int queriesBeforeCacheRead = jdbc.queries.size();
        assertTrue(warmed.columnExists(jdbc.open(), "maintenance_records", "license_usage_pct"));
        assertEquals(queriesBeforeCacheRead, jdbc.queries.size());
    }

    @Test
    void failedMetadataRefreshKeepsSnapshotAndSuccessfulRetryReplacesIt() {
        SchemaMetadataJdbcFixture jdbc = completeMetadata();
        SchemaCapabilityCache warmed = new SchemaCapabilityCache();
        DatabaseSchemaReadiness.inspect(jdbc::open, warmed);
        jdbc.columns.removeIf(column -> "company_users".equals(column.table()));
        jdbc.failureQuery = jdbc.queries.size() + 2;

        assertThrows(DataAccessException.class,
                () -> DatabaseSchemaReadiness.inspect(jdbc::open, warmed));
        assertTrue(warmed.columnExists(jdbc.open(), "company_users", "department"));

        jdbc.failureQuery = -1;
        assertTrue(DatabaseSchemaReadiness.inspect(jdbc::open, warmed).ready());
        int queriesBeforeCacheRead = jdbc.queries.size();
        assertFalse(warmed.columnExists(jdbc.open(), "company_users", "department"));
        assertEquals(queriesBeforeCacheRead, jdbc.queries.size());
    }

    private static SchemaMetadataJdbcFixture completeMetadata() {
        SchemaMetadataJdbcFixture jdbc = new SchemaMetadataJdbcFixture();
        java.util.Set<String> columns = new java.util.HashSet<>(ALL_REQUIRED_COLUMNS);
        columns.addAll(OPTIONAL_COLUMNS);
        for (String column : columns) {
            int dot = column.indexOf('.');
            jdbc.add("application", column.substring(0, dot), column.substring(dot + 1));
        }
        return jdbc;
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
    void partiallyAppliedCustomerAssignmentMigrationBlocksReadiness() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        java.util.Set<String> available = new java.util.HashSet<>(
                ALL_REQUIRED_COLUMNS);
        available.add("vertica_customer_detail.main_manager_user_id");
        jdbc.availableColumns = Set.copyOf(available);

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertFalse(report.ready());
        assertEquals(List.of("sub_manager_user_id"),
                report.missingRequirements().stream()
                        .filter(requirement -> "V20260903_11".equals(
                                requirement.migrationVersion()))
                        .map(DatabaseSchemaReadiness.Requirement::columnName)
                        .toList());
        assertTrue(report.missingOptionalRequirements().stream()
                .noneMatch(requirement -> "V20260903_11".equals(
                        requirement.migrationVersion())));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void partiallyAppliedCustomerIdentityMigrationBlocksReadiness() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        java.util.Set<String> available = new java.util.HashSet<>(
                ALL_REQUIRED_COLUMNS);
        available.add("customer_identity.customer_id");
        jdbc.availableColumns = Set.copyOf(available);

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertFalse(report.ready());
        assertEquals(List.of("customer_name"),
                report.missingRequirements().stream()
                        .filter(requirement -> "V20260904_13".equals(
                                requirement.migrationVersion()))
                        .map(DatabaseSchemaReadiness.Requirement::columnName)
                        .toList());
        assertTrue(report.missingOptionalRequirements().stream()
                .noneMatch(requirement -> "V20260904_13".equals(
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

    @Test
    void missingSwapMemoryReportsItsAdditiveMigration() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ALL_REQUIRED_COLUMNS.stream()
                .filter(column -> !column.equals(
                        "vertica_customer_detail_dev.swap_memory"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        DatabaseSchemaReadiness.Report report =
                DatabaseSchemaReadiness.inspect(jdbc::open);

        assertFalse(report.ready());
        assertEquals(1, report.missingRequirements().size());
        DatabaseSchemaReadiness.Requirement missing =
                report.missingRequirements().getFirst();
        assertEquals("V20260901_10", missing.migrationVersion());
        assertEquals("vertica_customer_detail_dev", missing.tableName());
        assertEquals("swap_memory", missing.columnName());
        assertTrue(jdbc.statements.isEmpty());
    }
}
