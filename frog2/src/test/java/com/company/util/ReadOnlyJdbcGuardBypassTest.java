package com.company.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ReadOnlyJdbcGuardBypassTest {
    @Test
    void metadataConnectionAndResultSetsStayGuarded() throws Exception {
        FakeDriver driver = new FakeDriver();
        Connection guarded = ReadOnlyJdbcGuard.wrap(driver.connection);

        DatabaseMetaData metadata = guarded.getMetaData();
        assertSame(guarded, metadata.getConnection());
        assertSame(metadata, metadata.unwrap(DatabaseMetaData.class));

        assertReadOnlyViolation(assertThrows(
                SQLException.class,
                () -> metadata.getConnection().prepareStatement(
                        "UPDATE meeting_records SET view_count = 1")));
        assertReadOnlyViolation(assertThrows(
                SQLException.class,
                () -> metadata.unwrap(DriverMarker.class)));

        ResultSet metadataResultSet = metadata.getColumns(null, null, "users", "department");
        assertNull(metadataResultSet.getStatement());
        assertReadOnlyViolation(assertThrows(
                SQLException.class,
                () -> metadataResultSet.unwrap(DriverMarker.class)));
        assertEquals(0, driver.driverMutationCalls.get());
    }

    @Test
    void statementResultSetCannotExposeRawDriverOrMutateRows() throws Exception {
        FakeDriver driver = new FakeDriver();
        Connection guarded = ReadOnlyJdbcGuard.wrap(driver.connection);
        Statement statement = guarded.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT 1");

        assertSame(statement, resultSet.getStatement());
        assertSame(guarded, resultSet.getStatement().getConnection());
        assertSame(resultSet, resultSet.unwrap(ResultSet.class));

        assertReadOnlyViolation(assertThrows(
                SQLException.class,
                () -> resultSet.updateString(1, "changed")));
        assertReadOnlyViolation(assertThrows(SQLException.class, resultSet::insertRow));
        assertReadOnlyViolation(assertThrows(SQLException.class, resultSet::updateRow));
        assertReadOnlyViolation(assertThrows(SQLException.class, resultSet::deleteRow));
        assertReadOnlyViolation(assertThrows(SQLException.class, resultSet::moveToInsertRow));
        assertReadOnlyViolation(assertThrows(
                SQLException.class,
                () -> resultSet.unwrap(DriverMarker.class)));
        assertReadOnlyViolation(assertThrows(
                SQLException.class,
                () -> resultSet.getStatement().executeUpdate(
                        "UPDATE meeting_records SET view_count = 1")));
        assertEquals(0, driver.driverMutationCalls.get());
    }

    @Test
    void updateableResultSetRequestsStopBeforeDriverCalls() throws Exception {
        FakeDriver driver = new FakeDriver();
        Connection guarded = ReadOnlyJdbcGuard.wrap(driver.connection);

        assertReadOnlyViolation(assertThrows(
                SQLException.class,
                () -> guarded.createStatement(
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)));
        assertReadOnlyViolation(assertThrows(
                SQLException.class,
                () -> guarded.prepareStatement(
                        "SELECT 1",
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_UPDATABLE)));

        assertEquals(0, driver.createStatementCalls.get());
        assertEquals(0, driver.prepareStatementCalls.get());
        assertEquals(0, driver.driverMutationCalls.get());
    }

    private static void assertReadOnlyViolation(SQLException exception) {
        assertEquals("25006", exception.getSQLState());
    }

    private interface DriverMarker {
    }

    private static final class FakeDriver {
        private final AtomicInteger driverMutationCalls = new AtomicInteger();
        private final AtomicInteger createStatementCalls = new AtomicInteger();
        private final AtomicInteger prepareStatementCalls = new AtomicInteger();
        private Connection connection;
        private DatabaseMetaData metadata;
        private Statement statement;
        private ResultSet resultSet;

        private FakeDriver() {
            resultSet = proxy(ResultSet.class, this::invokeResultSet);
            statement = proxy(Statement.class, this::invokeStatement);
            metadata = proxy(DatabaseMetaData.class, this::invokeMetadata);
            connection = proxy(Connection.class, this::invokeConnection);
        }

        private Object invokeConnection(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "getMetaData" -> metadata;
                case "createStatement" -> {
                    createStatementCalls.incrementAndGet();
                    yield statement;
                }
                case "prepareStatement" -> {
                    prepareStatementCalls.incrementAndGet();
                    if (!ReadOnlyJdbcGuard.isReadOnlySql((String) args[0])) {
                        driverMutationCalls.incrementAndGet();
                    }
                    yield null;
                }
                case "isReadOnly" -> true;
                case "unwrap" -> unwrap(proxy, args);
                case "isWrapperFor" -> isWrapperFor(proxy, args);
                default -> defaultValue(method.getReturnType());
            };
        }

        private Object invokeMetadata(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "getConnection" -> connection;
                case "getColumns" -> resultSet;
                case "unwrap" -> unwrap(proxy, args);
                case "isWrapperFor" -> isWrapperFor(proxy, args);
                default -> defaultValue(method.getReturnType());
            };
        }

        private Object invokeStatement(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (name.equals("getConnection")) {
                return connection;
            }
            if (name.equals("executeQuery") || name.equals("getResultSet")) {
                return resultSet;
            }
            if (name.startsWith("execute")) {
                driverMutationCalls.incrementAndGet();
            }
            if (name.equals("unwrap")) {
                return unwrap(proxy, args);
            }
            if (name.equals("isWrapperFor")) {
                return isWrapperFor(proxy, args);
            }
            return defaultValue(method.getReturnType());
        }

        private Object invokeResultSet(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (name.equals("getStatement")) {
                return statement;
            }
            if (name.startsWith("update")
                    || name.equals("insertRow")
                    || name.equals("deleteRow")
                    || name.equals("moveToInsertRow")) {
                driverMutationCalls.incrementAndGet();
                return null;
            }
            if (name.equals("unwrap")) {
                return unwrap(proxy, args);
            }
            if (name.equals("isWrapperFor")) {
                return isWrapperFor(proxy, args);
            }
            return defaultValue(method.getReturnType());
        }

        @SuppressWarnings("unchecked")
        private <T> T proxy(Class<T> jdbcType, java.lang.reflect.InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(
                    ReadOnlyJdbcGuardBypassTest.class.getClassLoader(),
                    new Class<?>[] { jdbcType, DriverMarker.class },
                    handler);
        }

        private static Object unwrap(Object proxy, Object[] args) {
            return ((Class<?>) args[0]).isInstance(proxy) ? proxy : null;
        }

        private static boolean isWrapperFor(Object proxy, Object[] args) {
            return ((Class<?>) args[0]).isInstance(proxy);
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return '\0';
    }
}
