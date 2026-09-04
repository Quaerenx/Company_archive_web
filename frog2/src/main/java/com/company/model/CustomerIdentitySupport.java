package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Optional rolling-schema support for immutable customer identifiers. */
final class CustomerIdentitySupport {
    static final String TABLE = "customer_identity";
    static final String ID_COLUMN = "customer_id";
    static final String NAME_COLUMN = "customer_name";

    private CustomerIdentitySupport() {
    }

    static Capability capability(
            Connection connection,
            SchemaCapabilityCache schemaCapabilities) {
        boolean idAvailable = schemaCapabilities.columnExists(
                connection, TABLE, ID_COLUMN);
        boolean nameAvailable = schemaCapabilities.columnExists(
                connection, TABLE, NAME_COLUMN);
        if (idAvailable && nameAvailable) {
            return Capability.COMPLETE;
        }
        if (!idAvailable && !nameAvailable) {
            return Capability.NONE;
        }
        return Capability.PARTIAL;
    }

    static String findId(Connection connection, String customerName)
            throws SQLException {
        String sql = "SELECT CAST(customer_id AS VARCHAR(36)) AS customer_id "
                + "FROM customer_identity WHERE customer_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(30);
            statement.setString(1, normalizeName(customerName));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? resultSet.getString("customer_id")
                        : null;
            }
        }
    }

    static String ensureId(Connection connection, String customerName)
            throws SQLException {
        String normalizedName = normalizeName(customerName);
        String existing = findId(connection, normalizedName);
        if (existing != null) {
            return existing;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO customer_identity (customer_name) VALUES (?)")) {
            statement.setQueryTimeout(30);
            statement.setString(1, normalizedName);
            statement.executeUpdate();
        } catch (SQLException exception) {
            if (!isDuplicateKey(exception)) {
                throw exception;
            }
        }
        String created = findId(connection, normalizedName);
        if (created == null) {
            throw new SQLException("Customer identity was not created");
        }
        return created;
    }

    static void requireCompatible(Capability capability) throws SQLException {
        if (capability == Capability.PARTIAL) {
            throw new SQLException("Customer identity schema is partially applied");
        }
    }

    private static boolean isDuplicateKey(SQLException exception) {
        for (SQLException current = exception;
                current != null;
                current = current.getNextException()) {
            if ("23505".equals(current.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        return customerName.strip();
    }

    enum Capability {
        NONE,
        PARTIAL,
        COMPLETE
    }
}
