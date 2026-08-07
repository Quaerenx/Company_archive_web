package com.company.util;

import com.company.performance.RequestPerformanceContext;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JdbcTiming {
    private static final Logger logger = LoggerFactory.getLogger(JdbcTiming.class);
    private static final Set<String> EXECUTION_METHODS = Set.of(
            "execute",
            "executeBatch",
            "executeLargeBatch",
            "executeLargeUpdate",
            "executeQuery",
            "executeUpdate");
    private static final Pattern SQL_COMMENTS =
            Pattern.compile("(?s)/\\*.*?\\*/|--[^\\r\\n]*");
    private static final Pattern SQL_STRING_LITERALS =
            Pattern.compile("'(?:''|[^'])*'");
    private static final Pattern SQL_NUMERIC_LITERALS =
            Pattern.compile("(?<![\\p{Alnum}_])[-+]?\\d+(?:\\.\\d+)?(?![\\p{Alnum}_])");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int MAX_SQL_SUMMARY_LENGTH = 240;
    private static final long SLOW_SQL_NANOS = TimeUnit.MILLISECONDS.toNanos(
            positiveLongProperty("frog2.performance.slowSqlMs", 250));

    private JdbcTiming() {
    }

    public static Connection wrap(Connection delegate) throws SQLException {
        return wrap(delegate, 30);
    }

    public static Connection wrap(
            Connection delegate,
            int queryTimeoutSeconds) throws SQLException {
        return wrap(delegate, queryTimeoutSeconds, System::nanoTime);
    }

    static Connection wrap(
            Connection delegate,
            LongSupplier nanoTime) throws SQLException {
        return wrap(delegate, 30, nanoTime);
    }

    static Connection wrap(
            Connection delegate,
            int queryTimeoutSeconds,
            LongSupplier nanoTime) throws SQLException {
        if (delegate == null) {
            throw new SQLException("Cannot time a null JDBC connection");
        }
        if (queryTimeoutSeconds <= 0) {
            throw new SQLException("JDBC query timeout must be greater than zero");
        }
        ConnectionHandler handler = new ConnectionHandler(
                delegate, queryTimeoutSeconds, nanoTime);
        Connection proxy = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                handler);
        handler.setProxy(proxy);
        return proxy;
    }

    static String summarizeSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return "unknown";
        }
        String summary = SQL_COMMENTS.matcher(sql).replaceAll(" ");
        summary = SQL_STRING_LITERALS.matcher(summary).replaceAll("?");
        summary = SQL_NUMERIC_LITERALS.matcher(summary).replaceAll("?");
        summary = WHITESPACE.matcher(summary).replaceAll(" ").trim();
        if (summary.length() > MAX_SQL_SUMMARY_LENGTH) {
            return summary.substring(0, MAX_SQL_SUMMARY_LENGTH) + "...";
        }
        return summary;
    }

    private static Object invoke(Method method, Object target, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static Statement wrapStatement(
            Statement delegate,
            Connection connectionProxy,
            String preparedSql,
            LongSupplier nanoTime) {
        Class<?> statementType;
        if (delegate instanceof CallableStatement) {
            statementType = CallableStatement.class;
        } else if (delegate instanceof PreparedStatement) {
            statementType = PreparedStatement.class;
        } else {
            statementType = Statement.class;
        }
        return (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class<?>[] { statementType },
                new StatementHandler(
                        delegate,
                        connectionProxy,
                        preparedSql,
                        nanoTime));
    }

    private static void applyQueryTimeout(
            Statement statement,
            int queryTimeoutSeconds) throws SQLException {
        try {
            statement.setQueryTimeout(queryTimeoutSeconds);
        } catch (SQLException exception) {
            try {
                statement.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    private static void recordExecution(
            String methodName,
            String sql,
            long elapsedNanos,
            boolean succeeded) {
        RequestPerformanceContext.recordSql(elapsedNanos);
        if (elapsedNanos < SLOW_SQL_NANOS) {
            return;
        }
        logger.warn(
                "Slow SQL operation={} durationMs={} succeeded={} sql={}",
                methodName,
                millis(elapsedNanos),
                succeeded,
                summarizeSql(sql));
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static long positiveLongProperty(String name, long defaultValue) {
        Long configured = Long.getLong(name);
        return configured == null || configured <= 0 ? defaultValue : configured;
    }

    private static final class ConnectionHandler implements InvocationHandler {
        private final Connection delegate;
        private final LongSupplier nanoTime;
        private final int queryTimeoutSeconds;
        private Connection proxy;

        private ConnectionHandler(
                Connection delegate,
                int queryTimeoutSeconds,
                LongSupplier nanoTime) {
            this.delegate = delegate;
            this.queryTimeoutSeconds = queryTimeoutSeconds;
            this.nanoTime = nanoTime;
        }

        private void setProxy(Connection proxy) {
            this.proxy = proxy;
        }

        @Override
        public Object invoke(Object ignoredProxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("unwrap".equals(name)
                    && ((Class<?>) args[0]).isInstance(proxy)) {
                return proxy;
            }
            if ("isWrapperFor".equals(name)
                    && ((Class<?>) args[0]).isInstance(proxy)) {
                return true;
            }

            Object result = JdbcTiming.invoke(method, delegate, args);
            if (result instanceof Statement statement
                    && ("createStatement".equals(name)
                            || "prepareStatement".equals(name)
                            || "prepareCall".equals(name))) {
                String preparedSql = args != null
                        && args.length > 0
                        && args[0] instanceof String sql
                                ? sql
                                : null;
                applyQueryTimeout(statement, queryTimeoutSeconds);
                return wrapStatement(statement, proxy, preparedSql, nanoTime);
            }
            return result;
        }
    }

    private static final class StatementHandler implements InvocationHandler {
        private final Statement delegate;
        private final Connection connectionProxy;
        private final String preparedSql;
        private final LongSupplier nanoTime;

        private StatementHandler(
                Statement delegate,
                Connection connectionProxy,
                String preparedSql,
                LongSupplier nanoTime) {
            this.delegate = delegate;
            this.connectionProxy = connectionProxy;
            this.preparedSql = preparedSql;
            this.nanoTime = nanoTime;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("getConnection".equals(name)) {
                return connectionProxy;
            }
            if ("unwrap".equals(name)
                    && ((Class<?>) args[0]).isInstance(proxy)) {
                return proxy;
            }
            if ("isWrapperFor".equals(name)
                    && ((Class<?>) args[0]).isInstance(proxy)) {
                return true;
            }
            if (!EXECUTION_METHODS.contains(name)) {
                return JdbcTiming.invoke(method, delegate, args);
            }

            String sql = preparedSql;
            if (args != null && args.length > 0 && args[0] instanceof String directSql) {
                sql = directSql;
            }
            long start = nanoTime.getAsLong();
            boolean succeeded = false;
            try {
                Object result = JdbcTiming.invoke(method, delegate, args);
                succeeded = true;
                return result;
            } finally {
                long elapsed = Math.max(0, nanoTime.getAsLong() - start);
                recordExecution(name.toUpperCase(Locale.ROOT), sql, elapsed, succeeded);
            }
        }
    }
}
