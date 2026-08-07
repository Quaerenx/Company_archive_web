package com.company.model;

import com.company.util.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class DatabaseSchemaReadiness {
    private static final List<Requirement> REQUIREMENTS = List.of(
            new Requirement(
                    "V20260720_01",
                    "user_vm_hosts",
                    "owner_user_id"),
            new Requirement(
                    "V20260720_04",
                    "maintenance_records",
                    "license_usage_pct"),
            new Requirement(
                    "V20260730_05",
                    "troubleshooting",
                    "creator_user_id"),
            new Requirement(
                    "V20260731_06",
                    "maintenance_records",
                    "created_by_user_id"),
            new Requirement(
                    "V20260731_06",
                    "monthly_customer_response",
                    "created_by_user_id"),
            new Requirement(
                    "V20260804_07",
                    "customer_maintenance_schedule",
                    "interval_months"));

    private DatabaseSchemaReadiness() {
    }

    public static Report inspect() {
        return inspect(DBConnection::getConnection);
    }

    static Report inspect(JdbcConnectionProvider connectionProvider) {
        Objects.requireNonNull(connectionProvider, "connectionProvider");
        SchemaCapabilityCache capabilities = new SchemaCapabilityCache();
        try (Connection connection = connectionProvider.getConnection()) {
            List<Requirement> missing = REQUIREMENTS.stream()
                    .filter(requirement -> !capabilities.columnExists(
                            connection,
                            requirement.tableName(),
                            requirement.columnName()))
                    .toList();
            return new Report(missing);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "inspect database schema readiness", exception);
        }
    }

    public record Requirement(
            String migrationVersion,
            String tableName,
            String columnName) {
    }

    public record Report(List<Requirement> missingRequirements) {
        public Report {
            missingRequirements = List.copyOf(missingRequirements);
        }

        public boolean ready() {
            return missingRequirements.isEmpty();
        }
    }
}
