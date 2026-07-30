package com.company.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class SchemaCapabilityCache {
    private final ConcurrentMap<ColumnKey, Boolean> columns = new ConcurrentHashMap<>();

    boolean columnExists(Connection connection, String tableName, String columnName) {
        ColumnKey key = new ColumnKey(normalize(tableName), normalize(columnName));
        return columns.computeIfAbsent(
                key, ignored -> inspectColumn(connection, tableName, columnName));
    }

    private static boolean inspectColumn(
            Connection connection, String tableName, String columnName) {
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            if (hasColumn(metadata, tableName, columnName)) {
                return true;
            }
            return hasColumn(
                    metadata,
                    tableName.toUpperCase(Locale.ROOT),
                    columnName.toUpperCase(Locale.ROOT));
        } catch (SQLException exception) {
            throw DataAccessException.from("inspect schema capability", exception);
        }
    }

    private static boolean hasColumn(
            DatabaseMetaData metadata, String tableName, String columnName)
            throws SQLException {
        try (ResultSet columns = metadata.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private record ColumnKey(String tableName, String columnName) {
    }
}
