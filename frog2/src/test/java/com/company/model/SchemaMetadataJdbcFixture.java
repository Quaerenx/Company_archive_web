package com.company.model;

import static com.company.testsupport.ProxyDefaults.defaultValue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class SchemaMetadataJdbcFixture {
    final List<Column> columns = new ArrayList<>();
    final List<Query> queries = new ArrayList<>();
    int failureQuery = -1;
    int connectionCloses;
    int resultCloses;
    int schemaReads;
    int catalogReads;
    boolean failConnectionClose;
    boolean failResultClose;
    boolean caseInsensitive;

    void add(String schema, String table, String column) {
        columns.add(new Column(schema, table, column));
    }

    Connection open() {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "getMetaData" -> metadata();
                    case "getSchema" -> { schemaReads++; yield "application"; }
                    case "getCatalog" -> { catalogReads++; yield "catalog"; }
                    case "close" -> {
                        connectionCloses++;
                        if (failConnectionClose) throw new SQLException("Fixture close failed", "08006");
                        yield null;
                    }
                    default -> defaultValue(call.getReturnType());
                });
    }

    private DatabaseMetaData metadata() {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(), new Class<?>[] {DatabaseMetaData.class},
                (ignored, call, args) -> {
                    if ("getColumns".equals(call.getName())) {
                        Query query = new Query((String) args[0], (String) args[1],
                                (String) args[2], (String) args[3]);
                        queries.add(query);
                        if (queries.size() == failureQuery) {
                            throw new SQLException("Fixture metadata failed", "08006");
                        }
                        return resultSet(columns.stream()
                                .filter(column -> matches(query.schema(), column.schema()))
                                .filter(column -> matches(query.table(), column.table()))
                                .filter(column -> matches(query.column(), column.name()))
                                .toList());
                    }
                    return defaultValue(call.getReturnType());
                });
    }

    private ResultSet resultSet(List<Column> rows) {
        int[] cursor = {-1};
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(), new Class<?>[] {ResultSet.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "next" -> ++cursor[0] < rows.size();
                    case "getString" -> switch ((String) args[0]) {
                        case "TABLE_SCHEM" -> rows.get(cursor[0]).schema();
                        case "TABLE_NAME" -> rows.get(cursor[0]).table();
                        case "COLUMN_NAME" -> rows.get(cursor[0]).name();
                        default -> null;
                    };
                    case "close" -> {
                        resultCloses++;
                        if (failResultClose) throw new SQLException("Fixture result close failed", "08006");
                        yield null;
                    }
                    default -> defaultValue(call.getReturnType());
                });
    }

    private boolean matches(String pattern, String identifier) {
        if (pattern == null) return true;
        StringBuilder regex = new StringBuilder();
        for (char character : pattern.toCharArray()) {
            regex.append(switch (character) {
                case '_' -> ".";
                case '%' -> ".*";
                default -> Pattern.quote(String.valueOf(character));
            });
        }
        return Pattern.compile(regex.toString(),
                caseInsensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0)
                .matcher(identifier).matches();
    }

    record Column(String schema, String table, String name) { }
    record Query(String catalog, String schema, String table, String column) { }
}
