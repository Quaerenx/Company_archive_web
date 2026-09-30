package com.company.util;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JdbcReadinessValidationTest {
    @Test
    void verifiesActualSelectWithShortTimeoutAndClosesResources() {
        Fixture fixture = new Fixture();

        assertTrue(JdbcReadinessValidation.check(fixture::connection));
        assertEquals(1, fixture.timeoutSeconds);
        assertEquals("SELECT 1", fixture.sql);
        assertEquals(List.of("result", "statement", "connection"), fixture.closed);
    }

    @Test
    void failedQueryProducesDownAndReturnsConnection() {
        Fixture fixture = new Fixture();
        fixture.failQuery = true;

        assertFalse(JdbcReadinessValidation.check(fixture::connection));
        assertEquals(List.of("statement", "connection"), fixture.closed);
    }

    @Test
    void connectionAcquisitionFailureProducesDown() {
        assertFalse(JdbcReadinessValidation.check(() -> {
            throw new SQLException("unavailable", "08001");
        }));
    }

    @Test
    void unexpectedQueryResultIsNotConsideredReady() {
        Fixture fixture = new Fixture();
        fixture.value = 0;

        assertFalse(JdbcReadinessValidation.check(fixture::connection));
        assertEquals(List.of("result", "statement", "connection"), fixture.closed);
    }

    private static final class Fixture {
        private final List<String> closed = new ArrayList<>();
        private int timeoutSeconds;
        private String sql;
        private boolean failQuery;
        private int value = 1;

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                    (ignored, method, args) -> switch (method.getName()) {
                        case "createStatement" -> statement();
                        case "close" -> { closed.add("connection"); yield null; }
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private Statement statement() {
            return (Statement) Proxy.newProxyInstance(
                    Statement.class.getClassLoader(), new Class<?>[] {Statement.class},
                    (ignored, method, args) -> switch (method.getName()) {
                        case "setQueryTimeout" -> { timeoutSeconds = (Integer) args[0]; yield null; }
                        case "executeQuery" -> {
                            sql = (String) args[0];
                            if (failQuery) { throw new SQLException("query failed", "08006"); }
                            yield result();
                        }
                        case "close" -> { closed.add("statement"); yield null; }
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private ResultSet result() {
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(), new Class<?>[] {ResultSet.class},
                    (ignored, method, args) -> switch (method.getName()) {
                        case "next" -> true;
                        case "getInt" -> value;
                        case "close" -> { closed.add("result"); yield null; }
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }
}
