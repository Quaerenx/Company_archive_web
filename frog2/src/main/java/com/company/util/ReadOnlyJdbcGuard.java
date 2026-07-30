package com.company.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Prevents mutating SQL from reaching the JDBC driver in read-only environments.
 */
public final class ReadOnlyJdbcGuard {
    private static final Pattern SQL_COMMENTS = Pattern.compile("(?s)/\\*.*?\\*/|--[^\\r\\n]*");
    private static final Pattern SQL_STRING_LITERALS = Pattern.compile("'(?:''|[^'])*'");
    private static final Pattern SQL_WORDS = Pattern.compile("[^A-Z_]+");
    private static final Set<String> MUTATING_KEYWORDS = Set.of(
            "ALTER", "CALL", "COPY", "CREATE", "DELETE", "DROP", "EXECUTE", "GRANT",
            "INSERT", "INTO", "MERGE", "REVOKE", "SET", "TRUNCATE", "UPDATE", "UPSERT");
    private static final Set<String> RESULT_SET_MUTATORS = Set.of(
            "insertRow", "updateRow", "deleteRow", "moveToInsertRow");

    private ReadOnlyJdbcGuard() {
    }

    public static Connection wrap(Connection delegate) throws SQLException {
        if (delegate == null) {
            throw new SQLException("Cannot wrap a null JDBC connection");
        }
        delegate.setReadOnly(true);
        ConnectionHandler handler = new ConnectionHandler(delegate);
        Connection proxy = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, handler);
        handler.setProxy(proxy);
        return proxy;
    }

    static boolean isReadOnlySql(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        String normalized = SQL_COMMENTS.matcher(sql).replaceAll(" ");
        normalized = SQL_STRING_LITERALS.matcher(normalized).replaceAll(" ");
        String[] words = SQL_WORDS.split(normalized.toUpperCase(Locale.ROOT).trim());
        if (words.length == 0 || !("SELECT".equals(words[0]) || "WITH".equals(words[0]))) {
            return false;
        }
        for (String word : words) {
            if (MUTATING_KEYWORDS.contains(word)) {
                return false;
            }
        }
        return true;
    }

    private static void requireReadOnlySql(String sql) throws SQLException {
        if (!isReadOnlySql(sql)) {
            throw readOnlyViolation("Mutating SQL is disabled by frog2 read-only mode");
        }
    }

    private static void requireReadOnlyResultSetConcurrency(
            String methodName, Object[] args) throws SQLException {
        int concurrencyArgument = -1;
        if ("createStatement".equals(methodName) && args != null && args.length >= 2) {
            concurrencyArgument = 1;
        } else if ("prepareStatement".equals(methodName)
                && args != null
                && args.length >= 3) {
            concurrencyArgument = 2;
        }

        if (concurrencyArgument >= 0
                && args[concurrencyArgument] instanceof Number concurrency
                && concurrency.intValue() == ResultSet.CONCUR_UPDATABLE) {
            throw readOnlyViolation(
                    "Updateable result sets are disabled by frog2 read-only mode");
        }
    }

    private static SQLNonTransientException readOnlyViolation(String message) {
        return new SQLNonTransientException(message, "25006");
    }

    private static Object invoke(Method method, Object target, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static final class ConnectionHandler implements InvocationHandler {
        private final Connection delegate;
        private Connection proxy;

        private ConnectionHandler(Connection delegate) {
            this.delegate = delegate;
        }

        private void setProxy(Connection proxy) {
            this.proxy = proxy;
        }

        @Override
        public Object invoke(Object ignoredProxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("prepareCall".equals(name)) {
                throw readOnlyViolation(
                        "Callable statements are disabled by frog2 read-only mode");
            }
            if ("prepareStatement".equals(name) && args != null && args.length > 0 && args[0] instanceof String sql) {
                requireReadOnlySql(sql);
                requireReadOnlyResultSetConcurrency(name, args);
                PreparedStatement statement = (PreparedStatement) ReadOnlyJdbcGuard.invoke(method, delegate, args);
                return wrapPreparedStatement(statement, proxy);
            }
            if ("createStatement".equals(name)) {
                requireReadOnlyResultSetConcurrency(name, args);
                Statement statement = (Statement) ReadOnlyJdbcGuard.invoke(method, delegate, args);
                return wrapStatement(statement, proxy);
            }
            if ("getMetaData".equals(name)) {
                DatabaseMetaData metadata =
                        (DatabaseMetaData) ReadOnlyJdbcGuard.invoke(method, delegate, args);
                return wrapDatabaseMetaData(metadata, proxy);
            }
            if ("setReadOnly".equals(name) && args != null && Boolean.FALSE.equals(args[0])) {
                throw readOnlyViolation("Read-only mode cannot be disabled");
            }
            if ("unwrap".equals(name)) {
                Class<?> requestedType = (Class<?>) args[0];
                if (requestedType.isInstance(proxy)) {
                    return proxy;
                }
                throw readOnlyViolation("Unwrapping a read-only connection is disabled");
            }
            if ("isWrapperFor".equals(name)) {
                return ((Class<?>) args[0]).isInstance(proxy);
            }
            return ReadOnlyJdbcGuard.invoke(method, delegate, args);
        }
    }

    private static DatabaseMetaData wrapDatabaseMetaData(
            DatabaseMetaData delegate, Connection connectionProxy) {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[] { DatabaseMetaData.class },
                new DatabaseMetaDataHandler(delegate, connectionProxy));
    }

    private static final class DatabaseMetaDataHandler implements InvocationHandler {
        private final DatabaseMetaData delegate;
        private final Connection connectionProxy;

        private DatabaseMetaDataHandler(
                DatabaseMetaData delegate, Connection connectionProxy) {
            this.delegate = delegate;
            this.connectionProxy = connectionProxy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("getConnection".equals(name)) {
                return connectionProxy;
            }
            if ("unwrap".equals(name)) {
                Class<?> requestedType = (Class<?>) args[0];
                if (requestedType.isInstance(proxy)) {
                    return proxy;
                }
                throw readOnlyViolation("Unwrapping read-only database metadata is disabled");
            }
            if ("isWrapperFor".equals(name)) {
                return ((Class<?>) args[0]).isInstance(proxy);
            }

            Object result = ReadOnlyJdbcGuard.invoke(method, delegate, args);
            if (result instanceof ResultSet resultSet) {
                return wrapResultSet(resultSet, null);
            }
            return result;
        }
    }

    private static Statement wrapStatement(Statement delegate, Connection connectionProxy) {
        Class<?> statementType = delegate instanceof CallableStatement ? CallableStatement.class : Statement.class;
        return (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(), new Class<?>[] { statementType },
                new StatementHandler(delegate, connectionProxy));
    }

    private static PreparedStatement wrapPreparedStatement(
            PreparedStatement delegate, Connection connectionProxy) {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(), new Class<?>[] { PreparedStatement.class },
                new StatementHandler(delegate, connectionProxy));
    }

    private static final class StatementHandler implements InvocationHandler {
        private final Statement delegate;
        private final Connection connectionProxy;

        private StatementHandler(Statement delegate, Connection connectionProxy) {
            this.delegate = delegate;
            this.connectionProxy = connectionProxy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (args != null && args.length > 0 && args[0] instanceof String sql
                    && (name.startsWith("execute") || "addBatch".equals(name))) {
                requireReadOnlySql(sql);
            }
            if ("getConnection".equals(name)) {
                return connectionProxy;
            }
            if ("unwrap".equals(name)) {
                Class<?> requestedType = (Class<?>) args[0];
                if (requestedType.isInstance(proxy)) {
                    return proxy;
                }
                throw readOnlyViolation("Unwrapping a read-only statement is disabled");
            }
            if ("isWrapperFor".equals(name)) {
                return ((Class<?>) args[0]).isInstance(proxy);
            }

            Object result = ReadOnlyJdbcGuard.invoke(method, delegate, args);
            if (result instanceof ResultSet resultSet) {
                return wrapResultSet(resultSet, (Statement) proxy);
            }
            return result;
        }
    }

    private static ResultSet wrapResultSet(
            ResultSet delegate, Statement statementProxy) {
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] { ResultSet.class },
                new ResultSetHandler(delegate, statementProxy));
    }

    private static final class ResultSetHandler implements InvocationHandler {
        private final ResultSet delegate;
        private final Statement statementProxy;

        private ResultSetHandler(ResultSet delegate, Statement statementProxy) {
            this.delegate = delegate;
            this.statementProxy = statementProxy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (name.startsWith("update") || RESULT_SET_MUTATORS.contains(name)) {
                throw readOnlyViolation(
                        "Result-set mutation is disabled by frog2 read-only mode");
            }
            if ("getStatement".equals(name)) {
                return statementProxy;
            }
            if ("unwrap".equals(name)) {
                Class<?> requestedType = (Class<?>) args[0];
                if (requestedType.isInstance(proxy)) {
                    return proxy;
                }
                throw readOnlyViolation("Unwrapping a read-only result set is disabled");
            }
            if ("isWrapperFor".equals(name)) {
                return ((Class<?>) args[0]).isInstance(proxy);
            }
            return ReadOnlyJdbcGuard.invoke(method, delegate, args);
        }
    }
}
