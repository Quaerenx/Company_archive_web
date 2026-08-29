package com.company.model;

import com.company.util.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
                        CUSTOMER_DETAIL_BASELINE,
                        environment.tableName(),
                        column));
            }
        }
        requirements.add(required(
                CUSTOMER_DETAIL_BASELINE,
                CustomerDetailEnvironment.PROD.tableName(),
                "is_deleted"));
        return List.copyOf(requirements);
    }

    public static Report inspect() {
        return inspect(DBConnection::getConnection);
    }

    static Report inspect(JdbcConnectionProvider connectionProvider) {
        Objects.requireNonNull(connectionProvider, "connectionProvider");
        SchemaCapabilityCache capabilities = new SchemaCapabilityCache();
        try (Connection connection = connectionProvider.getConnection()) {
            CustomerAuditSupport.Capability customerAuditCapability =
                    CustomerAuditSupport.capability(connection, capabilities);
            List<Requirement> missing = REQUIREMENTS.stream()
                    .filter(requirement -> !capabilities.columnExists(
                            connection,
                            requirement.tableName(),
                            requirement.columnName()))
                    .toList();
            List<Requirement> missingRequired = missing.stream()
                    .filter(requirement -> requirement.required()
                            || isIncompleteCustomerAuditRequirement(
                                    requirement, customerAuditCapability))
                    .map(requirement -> isIncompleteCustomerAuditRequirement(
                                    requirement, customerAuditCapability)
                            ? new Requirement(
                                    requirement.migrationVersion(),
                                    requirement.tableName(),
                                    requirement.columnName(),
                                    true)
                            : requirement)
                    .toList();
            List<Requirement> missingOptional = missing.stream()
                    .filter(requirement -> !requirement.required()
                            && !isIncompleteCustomerAuditRequirement(
                                    requirement, customerAuditCapability))
                    .toList();
            return new Report(missingRequired, missingOptional);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "inspect database schema readiness", exception);
        }
    }

    private static boolean isIncompleteCustomerAuditRequirement(
            Requirement requirement,
            CustomerAuditSupport.Capability capability) {
        return capability == CustomerAuditSupport.Capability.PARTIAL
                && "V20260825_09".equals(requirement.migrationVersion());
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
