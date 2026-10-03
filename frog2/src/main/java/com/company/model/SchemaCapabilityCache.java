package com.company.model;

import com.company.performance.RequestPerformanceContext;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class SchemaCapabilityCache {
    private static final SchemaCapabilityCache APPLICATION = new SchemaCapabilityCache();
    private volatile ConcurrentMap<ColumnKey, Boolean> columns = new ConcurrentHashMap<>();

    static SchemaCapabilityCache application() {
        return APPLICATION;
    }

    void replaceWith(SchemaCapabilityCache verified) {
        // Publish only a complete inspection; failed refreshes preserve the previous snapshot.
        columns = new ConcurrentHashMap<>(verified.columns);
    }

    boolean columnExists(Connection connection, String tableName, String columnName) {
        ColumnKey key = new ColumnKey(normalize(tableName), normalize(columnName));
        return columns.computeIfAbsent(
                key, ignored -> inspectColumn(connection, tableName, columnName));
    }

    void inspectColumns(Connection connection, Map<String, List<String>> columnsByTable) {
        Map<ColumnKey, Boolean> inspected = new LinkedHashMap<>();
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            for (var table : columnsByTable.entrySet()) {
                Set<String> originalColumns = tableColumns(metadata, table.getKey());
                boolean needsUppercase = table.getValue().stream()
                        .anyMatch(column -> !originalColumns.contains(normalize(column)));
                Set<String> uppercaseColumns = needsUppercase
                        ? tableColumns(metadata, table.getKey().toUpperCase(Locale.ROOT))
                        : Set.of();
                for (String column : table.getValue()) {
                    inspected.put(new ColumnKey(normalize(table.getKey()), normalize(column)),
                            originalColumns.contains(normalize(column))
                                    || uppercaseColumns.contains(normalize(column)));
                }
            }
        } catch (SQLException exception) {
            throw DataAccessException.from("inspect schema capabilities", exception);
        }
        columns.putAll(inspected);
    }

    private static Set<String> tableColumns(DatabaseMetaData metadata, String tableName)
            throws SQLException {
        Set<String> found = new HashSet<>();
        long start = System.nanoTime();
        try (ResultSet result = metadata.getColumns(null, null, tableName, "%")) {
            while (result.next()) {
                // JDBC table patterns treat underscores as wildcards; exclude similarly named tables.
                if (tableName.equalsIgnoreCase(result.getString("TABLE_NAME"))) {
                    String columnName = result.getString("COLUMN_NAME");
                    if (columnName != null) {
                        found.add(normalize(columnName));
                    }
                }
            }
        } finally {
            RequestPerformanceContext.recordMetadata(System.nanoTime() - start);
        }
        return found;
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
        long start = System.nanoTime();
        try (ResultSet columns = metadata.getColumns(null, null, tableName, columnName)) {
            while (columns.next()) {
                // JDBC identifier patterns can include rows from similarly named tables or columns.
                if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
            return false;
        } finally {
            RequestPerformanceContext.recordMetadata(System.nanoTime() - start);
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private record ColumnKey(String tableName, String columnName) {
    }
}
