package com.company.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Test-only routing; SQL outside the four approved synthetic tables is rejected. */
public final class TestTableJdbcRouter {
    private static final Set<String> TABLES = Set.of(
            "company_users", "vertica_customer_detail", "customer_maintenance_schedule", "maintenance_records");
    private static final Pattern REFERENCE = Pattern.compile(
            "\\b(FROM|JOIN|INTO|UPDATE)\\s+([a-z_][a-z0-9_]*)", Pattern.CASE_INSENSITIVE);
    private final String prefix;

    public TestTableJdbcRouter(String prefix) {
        if (prefix == null || !prefix.matches("frog2_test_[a-z0-9_]+_") || prefix.length() > 64) {
            throw new IllegalArgumentException("A dedicated frog2_test_ prefix ending with underscore is required");
        }
        this.prefix = prefix;
    }

    public Map<String, String> tableNames() {
        return TABLES.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                table -> table, table -> prefix + table));
    }

    @FunctionalInterface
    public interface ConnectionOpener {
        Connection open() throws SQLException;
    }

    public record DaoSet(UserDAO users, CustomerAssignmentDAO assignments,
                         MaintenanceRecordDAO records, CustomerDAO customers) {
    }

    public DaoSet daos(ConnectionOpener connections) {
        JdbcConnectionProvider provider = () -> wrap(connections.open());
        return new DaoSet(new UserDAO(provider), new CustomerAssignmentDAO(provider),
                new MaintenanceRecordDAO(provider, new SchemaCapabilityCache()), new CustomerDAO(provider));
    }

    String routeSql(String sql) throws SQLException {
        String masked = maskLiterals(sql);
        String leading = masked.stripLeading().toUpperCase(Locale.ROOT);
        if (!leading.matches("(?s)(SELECT|INSERT|UPDATE|DELETE)\\s+.*")
                || masked.matches("(?is).*\\b(USING|UNION|INTERSECT|EXCEPT|RETURNING|COPY|MERGE|CALL|NEXTVAL|SETVAL|CURRVAL)\\b.*")
                || masked.matches("(?is).*\\bFROM\\s*\\(.*")
                || masked.matches("(?is).*\\bFROM\\s+\\w+(?:\\s+(?:AS\\s+)?\\w+)?\\s*,.*")) {
            throw new SQLException("Only reviewed single-table or explicit-join DML is allowed");
        }
        if ((leading.startsWith("DELETE ") || leading.startsWith("UPDATE "))
                && !masked.matches("(?is).*\\bWHERE\\b.*")) {
            throw new SQLException("Test-table mutations require a row predicate");
        }
        Matcher references = REFERENCE.matcher(masked);
        StringBuilder routed = new StringBuilder();
        int end = 0;
        int count = 0;
        while (references.find()) {
            String logical = references.group(2).toLowerCase(Locale.ROOT);
            if (!TABLES.contains(logical)
                    || masked.substring(references.end()).stripLeading().startsWith(".")) {
                throw new SQLException("SQL references an unapproved table");
            }
            routed.append(sql, end, references.start(2)).append("public.").append(prefix).append(logical);
            end = references.end(2);
            count++;
        }
        if (count == 0) {
            throw new SQLException("SQL must reference an approved test table");
        }
        return routed.append(sql, end, sql.length()).toString();
    }

    private static String maskLiterals(String sql) throws SQLException {
        if (sql == null || sql.isBlank() || sql.contains(";") || sql.contains("--")
                || sql.contains("/*") || sql.contains("\"") || sql.contains("`") || sql.contains("$")) {
            throw new SQLException("Unsupported SQL syntax in test-table routing");
        }
        StringBuilder masked = new StringBuilder(sql);
        boolean literal = false;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (current == '\'') {
                masked.setCharAt(index, ' ');
                if (literal && index + 1 < sql.length() && sql.charAt(index + 1) == '\'') {
                    masked.setCharAt(++index, ' ');
                } else {
                    literal = !literal;
                }
            } else if (literal) {
                masked.setCharAt(index, ' ');
            }
        }
        if (literal) {
            throw new SQLException("Unterminated SQL literal");
        }
        return masked.toString();
    }

    public Connection wrap(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (proxy, call, args) -> switch (call.getName()) {
                    case "prepareStatement" -> {
                        Object[] routed = args.clone();
                        routed[0] = routeSql((String) args[0]);
                        yield statement((PreparedStatement) invoke(delegate, call, routed), (Connection) proxy);
                    }
                    case "getMetaData" -> metadata(delegate.getMetaData());
                    case "createStatement", "prepareCall", "unwrap" -> throw new SQLException("Unrouted JDBC access is forbidden");
                    case "isWrapperFor" -> false;
                    case "close", "isClosed", "getAutoCommit", "setAutoCommit", "commit", "rollback",
                            "setReadOnly", "isReadOnly", "getTransactionIsolation", "setTransactionIsolation" ->
                            invoke(delegate, call, args);
                    default -> throw new SQLException("Unsupported connection operation in test-table routing");
                });
    }

    private PreparedStatement statement(PreparedStatement delegate, Connection connection) throws SQLException {
        delegate.setQueryTimeout(5);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class}, (proxy, call, args) -> {
                    if ("unwrap".equals(call.getName())
                            || ((call.getName().startsWith("execute") || "addBatch".equals(call.getName()))
                                    && args != null && args.length > 0 && args[0] instanceof String)) {
                        throw new SQLException("Unrouted statement SQL is forbidden");
                    }
                    if ("getConnection".equals(call.getName())) {
                        return connection;
                    }
                    if ("isWrapperFor".equals(call.getName())) {
                        return false;
                    }
                    Object result = invoke(delegate, call, args);
                    return result instanceof ResultSet rows ? rows(rows, (PreparedStatement) proxy) : result;
                });
    }

    private DatabaseMetaData metadata(DatabaseMetaData delegate) {
        return (DatabaseMetaData) Proxy.newProxyInstance(DatabaseMetaData.class.getClassLoader(),
                new Class<?>[] {DatabaseMetaData.class}, (proxy, call, args) -> {
                    if (!"getColumns".equals(call.getName())) {
                        throw new SQLException("Only approved test-table column metadata is allowed");
                    }
                    String logical = String.valueOf(args[2]).toLowerCase(Locale.ROOT);
                    if (!TABLES.contains(logical)) {
                        throw new SQLException("Metadata references an unapproved table");
                    }
                    Object[] routed = args.clone();
                    String actual = prefix + logical;
                    routed[0] = null;
                    routed[1] = "public";
                    routed[2] = actual;
                    ResultSet columns = (ResultSet) invoke(delegate, call, routed);
                    return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                            new Class<?>[] {ResultSet.class}, (ignored, resultCall, resultArgs) -> {
                                if ("next".equals(resultCall.getName())) {
                                    while (columns.next()) {
                                        if (actual.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                                                && "public".equalsIgnoreCase(columns.getString("TABLE_SCHEM"))) {
                                            return true;
                                        }
                                    }
                                    return false;
                                }
                                if ("getString".equals(resultCall.getName())
                                        && "TABLE_NAME".equalsIgnoreCase(String.valueOf(resultArgs[0]))) {
                                    return logical;
                                }
                                if ("getStatement".equals(resultCall.getName()) || "unwrap".equals(resultCall.getName())) {
                                    throw new SQLException("Unrouted metadata access is forbidden");
                                }
                                return invoke(columns, resultCall, resultArgs);
                            });
                });
    }

    private ResultSet rows(ResultSet delegate, PreparedStatement statement) {
        return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class}, (ignored, call, args) -> switch (call.getName()) {
                    case "getStatement" -> statement;
                    case "unwrap" -> throw new SQLException("Unrouted result-set access is forbidden");
                    case "isWrapperFor" -> false;
                    default -> invoke(delegate, call, args);
                });
    }

    private static Object invoke(Object delegate, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
