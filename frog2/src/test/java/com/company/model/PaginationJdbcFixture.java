package com.company.model;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class PaginationJdbcFixture {
    final List<StatementRecord> statements = new ArrayList<>();
    final Deque<List<Map<String, Object>>> queryResults =
            new ArrayDeque<>();
    final Deque<Integer> updateResults = new ArrayDeque<>();
    Set<String> availableColumns = Set.of();
    int openCount;
    int closeCount;
    int commitCount;
    int rollbackCount;
    boolean autoCommit = true;
    final List<Boolean> autoCommitValues = new ArrayList<>();

    @SafeVarargs
    final void enqueue(Map<String, Object>... rows) {
        queryResults.addLast(List.of(rows));
    }

    void enqueueUpdate(int affectedRows) {
        updateResults.addLast(affectedRows);
    }

    Connection open() {
        openCount++;
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "prepareStatement" ->
                            statement((String) args[0]);
                    case "getMetaData" -> metadata();
                    case "getAutoCommit" -> autoCommit;
                    case "setAutoCommit" -> {
                        autoCommit = (Boolean) args[0];
                        autoCommitValues.add(autoCommit);
                        yield null;
                    }
                    case "commit" -> {
                        commitCount++;
                        yield null;
                    }
                    case "rollback" -> {
                        rollbackCount++;
                        yield null;
                    }
                    case "close" -> {
                        closeCount++;
                        yield null;
                    }
                    case "isClosed" -> false;
                    default -> defaultValue(call.getReturnType());
                });
    }

    static Map<String, Object> row(Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            row.put((String) values[index], values[index + 1]);
        }
        return row;
    }

    private PreparedStatement statement(String sql) {
        StatementRecord record = new StatementRecord(sql);
        statements.add(record);
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "setString", "setInt", "setLong",
                            "setDate", "setTimestamp" -> {
                        record.parameters.put(
                                (Integer) args[0], args[1]);
                        yield null;
                    }
                    case "setNull" -> {
                        record.parameters.put((Integer) args[0], null);
                        yield null;
                    }
                    case "executeQuery" -> {
                        if (queryResults.isEmpty()) {
                            throw new SQLException(
                                    "No queued query result for " + sql);
                        }
                        yield resultSet(queryResults.removeFirst());
                    }
                    case "executeUpdate" -> {
                        if (updateResults.isEmpty()) {
                            throw new SQLException(
                                    "No queued update result for " + sql);
                        }
                        yield updateResults.removeFirst();
                    }
                    case "close" -> null;
                    default -> defaultValue(call.getReturnType());
                });
    }

    private DatabaseMetaData metadata() {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[] {DatabaseMetaData.class},
                (ignored, call, args) -> {
                    if ("getColumns".equals(call.getName())) {
                        String table = String.valueOf(args[2])
                                .toLowerCase(Locale.ROOT);
                        String column = String.valueOf(args[3])
                                .toLowerCase(Locale.ROOT);
                        boolean available = availableColumns.contains(
                                table + "." + column);
                        return resultSet(available
                                ? List.of(row("column_name", column))
                                : List.of());
                    }
                    return defaultValue(call.getReturnType());
                });
    }

    private static ResultSet resultSet(
            List<Map<String, Object>> rows) {
        int[] cursor = {-1};
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "next" -> ++cursor[0] < rows.size();
                    case "getString" -> {
                        Object value = value(rows, cursor[0], args[0]);
                        yield value == null ? null : value.toString();
                    }
                    case "getInt" -> number(
                            value(rows, cursor[0], args[0])).intValue();
                    case "getLong" -> number(
                            value(rows, cursor[0], args[0])).longValue();
                    case "getDate", "getTimestamp" ->
                            value(rows, cursor[0], args[0]);
                    case "close" -> null;
                    default -> defaultValue(call.getReturnType());
                });
    }

    private static Object value(
            List<Map<String, Object>> rows,
            int cursor,
            Object key) {
        Map<String, Object> row = rows.get(cursor);
        if (key instanceof Integer index) {
            return row.values().stream()
                    .skip(index - 1L)
                    .findFirst()
                    .orElse(null);
        }
        return row.get(String.valueOf(key));
    }

    private static Number number(Object value) {
        return value instanceof Number number ? number : 0;
    }

    static final class StatementRecord {
        final String sql;
        final Map<Integer, Object> parameters =
                new LinkedHashMap<>();

        StatementRecord(String sql) {
            this.sql = sql;
        }
    }

}
