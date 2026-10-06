package com.company.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Compatibility boundary for UUID relationships and retained name snapshots. */
final class CustomerReferenceSupport {
    static final String MIGRATION = "V20261006_14";
    static final List<String> TABLES = List.of(
            "maintenance_records", "monthly_customer_response", "troubleshooting",
            "customer_maintenance_schedule", "vertica_customer_detail",
            "vertica_customer_detail_stg", "vertica_customer_detail_dev");

    private CustomerReferenceSupport() {
    }

    static boolean enabled(
            Connection connection, SchemaCapabilityCache capabilities, String table)
            throws SQLException {
        if (!TABLES.contains(table)) {
            throw new IllegalArgumentException("Unsupported customer reference table");
        }
        if (!capabilities.columnExists(connection, table, "customer_id")) {
            return false;
        }
        if (CustomerIdentitySupport.capability(connection, capabilities)
                != CustomerIdentitySupport.Capability.COMPLETE) {
            throw new SQLException("Customer references require a complete identity schema");
        }
        return true;
    }

    static boolean partiallyApplied(
            Connection connection, SchemaCapabilityCache capabilities) {
        long count = TABLES.stream().filter(table -> capabilities.columnExists(
                connection, table, "customer_id")).count();
        return count != 0 && (count != TABLES.size()
                || CustomerIdentitySupport.capability(connection, capabilities)
                        != CustomerIdentitySupport.Capability.COMPLETE);
    }

    static String predicate(boolean enabled) {
        return enabled
                ? "customer_id = (SELECT customer_id FROM customer_identity WHERE customer_name = ?)"
                : "customer_name = ?";
    }

    static String namesPredicate(boolean enabled, String placeholders) {
        return enabled
                ? "customer_id IN (SELECT customer_id FROM customer_identity WHERE customer_name IN ("
                        + placeholders + "))"
                : "customer_name IN (" + placeholders + ")";
    }
}
