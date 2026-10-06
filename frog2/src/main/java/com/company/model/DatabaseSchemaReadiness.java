package com.company.model;

import com.company.util.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DatabaseSchemaReadiness {
    private static final String CUSTOMER_DETAIL_BASELINE =
            "BASELINE_CUSTOMER_DETAIL";
    private static final List<Requirement> BASE_REQUIREMENTS = List.of(
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "ip"),
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "owner_user_id"),
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "owner_user_name"),
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "purpose"),
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "os_info"),
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "vertica_version"),
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "remote_host"),
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "note"),
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "status"),
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "created_at"),
            required(
                    "V20260720_01",
                    "user_vm_hosts",
                    "updated_at"),
            required(
                    "V20260720_04",
                    "maintenance_records",
                    "license_usage_pct"),
            required(
                    "BASELINE_MAINTENANCE_LICENSE_DETAILS",
                    "maintenance_records",
                    "license_size_gb"),
            required(
                    "BASELINE_MAINTENANCE_LICENSE_DETAILS",
                    "maintenance_records",
                    "license_usage_size"),
            required(
                    "V20260730_05",
                    "troubleshooting",
                    "creator_user_id"),
            required(
                    "V20260731_06",
                    "maintenance_records",
                    "created_by_user_id"),
            required(
                    "V20260731_06",
                    "monthly_customer_response",
                    "created_by_user_id"),
            required(
                    "V20260804_07",
                    "customer_maintenance_schedule",
                    "interval_months"),
            required(
                    "V20260804_07",
                    "customer_maintenance_schedule",
                    "anchor_month"),
            required(
                    "V20260804_07",
                    "customer_maintenance_schedule",
                    "enabled"),
            required(
                    "V20260804_07",
                    "customer_maintenance_schedule",
                    "effective_from"),
            required(
                    "V20260804_07",
                    "customer_maintenance_schedule",
                    "effective_to"),
            required(
                    "V20260804_07",
                    "customer_maintenance_schedule",
                    "updated_by"),
            required(
                    "V20260804_07",
                    "customer_maintenance_schedule",
                    "updated_at"),
            optional(
                    "V20260825_09",
                    "vertica_customer_detail",
                    "updated_at"),
            optional(
                    "V20260825_09",
                    "vertica_customer_detail",
                    "updated_by"),
            optional(
                    "V20260825_09",
                    "vertica_customer_detail",
                    "deleted_at"),
            optional(
                    "V20260825_09",
                    "vertica_customer_detail",
                    "deleted_by"),
            optional(
                    "V20260903_11",
                    "vertica_customer_detail",
                    "main_manager_user_id"),
            optional(
                    "V20260903_11",
                    "vertica_customer_detail",
                    "sub_manager_user_id"),
            optional(
                    "V20260904_13",
                    "customer_identity",
                    "customer_id"),
            optional(
                    "V20260904_13",
                    "customer_identity",
                    "customer_name"),
            optional(
                    "LEGACY_ADD_DEPARTMENT_COLUMN",
                    "company_users",
                    "department"));
    private static final List<Requirement> REQUIREMENTS = requirements();

    private DatabaseSchemaReadiness() {
    }

    private static List<Requirement> requirements() {
        List<Requirement> requirements = new ArrayList<>(BASE_REQUIREMENTS);
        for (CustomerDetailEnvironment environment
                : CustomerDetailEnvironment.values()) {
            for (String column : CustomerDetailDAO.requiredColumnNames()) {
                requirements.add(required(
                        customerDetailMigration(column),
                        environment.tableName(),
                        column));
            }
        }
        requirements.add(required(
                CUSTOMER_DETAIL_BASELINE,
                CustomerDetailEnvironment.PROD.tableName(),
                "is_deleted"));
        for (String table : CustomerReferenceSupport.TABLES) {
            requirements.add(optional(CustomerReferenceSupport.MIGRATION, table, "customer_id"));
        }
        for (String column : MaintenanceLicenseValues.COLUMNS) {
            requirements.add(optional(MaintenanceLicenseValues.MIGRATION, "maintenance_records", column));
        }
        requirements.add(optional(MeetingLifecycleSupport.MIGRATION, "meeting_records", "deleted_at"));
        requirements.add(optional(MeetingLifecycleSupport.MIGRATION, "meeting_records", "deleted_by"));
        requirements.add(optional(MeetingLifecycleSupport.MIGRATION, "meeting_comments", "archived_at"));
        return List.copyOf(requirements);
    }

    private static String customerDetailMigration(String column) {
        return "swap_memory".equals(column)
                ? "V20260901_10"
                : CUSTOMER_DETAIL_BASELINE;
    }

    public static Report inspect() {
        return inspect(DBConnection::getConnection, SchemaCapabilityCache.application());
    }

    static Report inspect(JdbcConnectionProvider connectionProvider) {
        return inspect(connectionProvider, new SchemaCapabilityCache());
    }

    static Report inspect(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache verifiedCapabilities) {
        Objects.requireNonNull(connectionProvider, "connectionProvider");
        Objects.requireNonNull(verifiedCapabilities, "verifiedCapabilities");
        SchemaCapabilityCache capabilities = new SchemaCapabilityCache();
        Report report;
        try (Connection connection = connectionProvider.getConnection()) {
            capabilities.inspectColumns(connection, REQUIREMENTS.stream().collect(
                    Collectors.groupingBy(Requirement::tableName, LinkedHashMap::new,
                            Collectors.mapping(Requirement::columnName, Collectors.toList()))));
            CustomerAuditSupport.Capability customerAuditCapability =
                    CustomerAuditSupport.capability(connection, capabilities);
            CustomerAssignmentSupport.Capability
                    customerAssignmentCapability =
                            CustomerAssignmentSupport.capability(
                                    connection, capabilities);
            CustomerIdentitySupport.Capability customerIdentityCapability =
                    CustomerIdentitySupport.capability(
                            connection, capabilities);
            boolean partialReferences = CustomerReferenceSupport.partiallyApplied(connection, capabilities);
            long numericColumns = MaintenanceLicenseValues.COLUMNS.stream().filter(column ->
                    capabilities.columnExists(connection, "maintenance_records", column)).count();
            long meetingColumns = REQUIREMENTS.stream().filter(requirement ->
                    MeetingLifecycleSupport.MIGRATION.equals(requirement.migrationVersion())
                            && capabilities.columnExists(connection, requirement.tableName(), requirement.columnName())).count();
            java.util.function.Predicate<Requirement> incomplete = requirement ->
                    isIncompleteCustomerAuditRequirement(requirement, customerAuditCapability)
                            || isIncompleteCustomerAssignmentRequirement(requirement, customerAssignmentCapability)
                            || isIncompleteCustomerIdentityRequirement(requirement, customerIdentityCapability)
                            || (partialReferences && (CustomerReferenceSupport.MIGRATION.equals(requirement.migrationVersion())
                                    || "V20260904_13".equals(requirement.migrationVersion())))
                            || (numericColumns > 0 && numericColumns < 3
                                    && MaintenanceLicenseValues.MIGRATION.equals(requirement.migrationVersion()))
                            || (meetingColumns > 0 && meetingColumns < 3
                                    && MeetingLifecycleSupport.MIGRATION.equals(requirement.migrationVersion()));
            List<Requirement> missing = REQUIREMENTS.stream()
                    .filter(requirement -> !capabilities.columnExists(
                            connection,
                            requirement.tableName(),
                            requirement.columnName()))
                    .toList();
            List<Requirement> missingRequired = missing.stream()
                    .filter(requirement -> requirement.required() || incomplete.test(requirement))
                    .map(requirement -> incomplete.test(requirement)
                            ? new Requirement(
                                    requirement.migrationVersion(),
                                    requirement.tableName(),
                                    requirement.columnName(),
                                    true)
                            : requirement)
                    .toList();
            List<Requirement> missingOptional = missing.stream()
                    .filter(requirement -> !requirement.required() && !incomplete.test(requirement))
                    .toList();
            report = new Report(missingRequired, missingOptional);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "inspect database schema readiness", exception);
        }
        verifiedCapabilities.replaceWith(capabilities);
        return report;
    }

    private static boolean isIncompleteCustomerAuditRequirement(
            Requirement requirement,
            CustomerAuditSupport.Capability capability) {
        return capability == CustomerAuditSupport.Capability.PARTIAL
                && "V20260825_09".equals(requirement.migrationVersion());
    }

    private static boolean isIncompleteCustomerAssignmentRequirement(
            Requirement requirement,
            CustomerAssignmentSupport.Capability capability) {
        return capability == CustomerAssignmentSupport.Capability.PARTIAL
                && "V20260903_11".equals(requirement.migrationVersion());
    }

    private static boolean isIncompleteCustomerIdentityRequirement(
            Requirement requirement,
            CustomerIdentitySupport.Capability capability) {
        return capability == CustomerIdentitySupport.Capability.PARTIAL
                && "V20260904_13".equals(requirement.migrationVersion());
    }

    private static Requirement required(
            String migrationVersion,
            String tableName,
            String columnName) {
        return new Requirement(
                migrationVersion, tableName, columnName, true);
    }

    private static Requirement optional(
            String migrationVersion,
            String tableName,
            String columnName) {
        return new Requirement(
                migrationVersion, tableName, columnName, false);
    }

    public record Requirement(
            String migrationVersion,
            String tableName,
            String columnName,
            boolean required) {
        public Requirement(
                String migrationVersion,
                String tableName,
                String columnName) {
            this(migrationVersion, tableName, columnName, true);
        }
    }

    public record Report(
            List<Requirement> missingRequirements,
            List<Requirement> missingOptionalRequirements) {
        public Report {
            missingRequirements = List.copyOf(missingRequirements);
            missingOptionalRequirements = List.copyOf(
                    missingOptionalRequirements);
        }

        public Report(List<Requirement> missingRequirements) {
            this(missingRequirements, List.of());
        }

        public boolean ready() {
            return missingRequirements.isEmpty();
        }
    }
}
