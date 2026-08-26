package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class VerticaEosCapabilityCache {
    private static final String DISCOVERY_SQL =
            "SELECT table_schema, column_name FROM v_catalog.columns "
                    + "WHERE lower(table_name) = 'vertica_eos' "
                    + "AND lower(column_name) IN ('vertica_version', 'version') "
                    + "ORDER BY CASE lower(table_schema) WHEN 'public' THEN 0 ELSE 1 END, "
                    + "CASE lower(column_name) WHEN 'vertica_version' THEN 0 ELSE 1 END "
                    + "LIMIT 1";

    private volatile Capability capability;

    Capability resolve(Connection connection) {
        Capability resolved = capability;
        if (resolved != null) {
            return resolved;
        }
        synchronized (this) {
            resolved = capability;
            if (resolved == null) {
                resolved = inspect(connection);
                if (resolved != null) {
                    capability = resolved;
                }
            }
            return resolved;
        }
    }

    private static Capability inspect(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(DISCOVERY_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return null;
            }
            String schemaName = resultSet.getString("table_schema");
            String versionColumn = resultSet.getString("column_name");
            if (schemaName == null || schemaName.isBlank()
                    || versionColumn == null || versionColumn.isBlank()) {
                return null;
            }
            return new Capability(
                    quoteIdentifier(schemaName.trim())
                            + "."
                            + quoteIdentifier("vertica_eos"),
                    quoteIdentifier(versionColumn.trim()));
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "inspect Vertica EOS schema capability", exception);
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    record Capability(String qualifiedTable, String versionColumn) {
    }
}
