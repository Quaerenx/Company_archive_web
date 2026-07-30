package com.company.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ReadOnlyJdbcGuardTest {
    @Test
    void classifiesOnlyNonMutatingQueriesAsReadOnly() {
        assertTrue(ReadOnlyJdbcGuard.isReadOnlySql("SELECT * FROM meeting_records"));
        assertTrue(ReadOnlyJdbcGuard.isReadOnlySql("-- comment\n SELECT 'UPDATE' AS value"));
        assertTrue(ReadOnlyJdbcGuard.isReadOnlySql("WITH records AS (SELECT 1) SELECT * FROM records"));

        assertFalse(ReadOnlyJdbcGuard.isReadOnlySql("UPDATE meeting_records SET view_count = 1"));
        assertFalse(ReadOnlyJdbcGuard.isReadOnlySql("SELECT * FROM meeting_records FOR UPDATE"));
        assertFalse(ReadOnlyJdbcGuard.isReadOnlySql("SELECT 1; DELETE FROM meeting_records"));
        assertFalse(ReadOnlyJdbcGuard.isReadOnlySql("SELECT * INTO archive FROM meeting_records"));
        assertFalse(ReadOnlyJdbcGuard.isReadOnlySql("CREATE TABLE unexpected(id INT)"));
    }

    @Test
    void blocksPreparedMutationBeforeItReachesTheDriver() throws Exception {
        AtomicInteger prepareCalls = new AtomicInteger();
        Connection guarded = ReadOnlyJdbcGuard.wrap(fakeConnection(prepareCalls, new AtomicInteger()));

        SQLException error = assertThrows(SQLException.class,
                () -> guarded.prepareStatement("DELETE FROM meeting_records"));

        assertEquals("25006", error.getSQLState());
        assertEquals(0, prepareCalls.get());
    }

    @Test
    void allowsPreparedSelectAndBlocksStatementMutation() throws Exception {
        AtomicInteger prepareCalls = new AtomicInteger();
        AtomicInteger statementCalls = new AtomicInteger();
        Connection guarded = ReadOnlyJdbcGuard.wrap(fakeConnection(prepareCalls, statementCalls));

        guarded.prepareStatement("SELECT * FROM meeting_records WHERE meeting_id = ?");
        Statement statement = guarded.createStatement();
        assertThrows(SQLException.class, () -> statement.executeUpdate("ALTER TABLE hosts ADD COLUMN bad INT"));

        assertEquals(1, prepareCalls.get());
        assertEquals(0, statementCalls.get());
        assertTrue(guarded.isReadOnly());
    }

    private static Connection fakeConnection(AtomicInteger prepareCalls, AtomicInteger statementCalls) {
        PreparedStatement preparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(), new Class<?>[] { PreparedStatement.class },
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        Statement statement = (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(), new Class<?>[] { Statement.class },
                (proxy, method, args) -> {
                    if (method.getName().startsWith("execute")) {
                        statementCalls.incrementAndGet();
                    }
                    return defaultValue(method.getReturnType());
                });

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[] { Connection.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "prepareStatement" -> {
                        prepareCalls.incrementAndGet();
                        yield preparedStatement;
                    }
                    case "createStatement" -> statement;
                    case "isReadOnly" -> true;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
