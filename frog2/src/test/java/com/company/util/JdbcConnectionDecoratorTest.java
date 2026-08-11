package com.company.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class JdbcConnectionDecoratorTest {
    @Test
    void closesRawConnectionWhenReadOnlyDecorationFails() {
        AtomicInteger closeCalls = new AtomicInteger();
        Connection raw = connection(closeCalls, true);

        SQLException exception = assertThrows(
                SQLException.class,
                () -> JdbcConnectionDecorator.decorate(raw, 30, true));

        assertEquals("cannot enable read-only", exception.getMessage());
        assertEquals(1, closeCalls.get());
    }

    @Test
    void successfulDecorationLeavesClosingToTheCaller() throws Exception {
        AtomicInteger closeCalls = new AtomicInteger();
        Connection raw = connection(closeCalls, false);

        Connection decorated = JdbcConnectionDecorator.decorate(raw, 30, true);

        assertEquals(0, closeCalls.get());
        decorated.close();
        assertEquals(1, closeCalls.get());
    }

    private static Connection connection(
            AtomicInteger closeCalls, boolean failReadOnly) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (ignored, method, args) -> switch (method.getName()) {
                    case "setReadOnly" -> {
                        if (failReadOnly) {
                            throw new SQLNonTransientException(
                                    "cannot enable read-only", "08006");
                        }
                        yield null;
                    }
                    case "close" -> {
                        closeCalls.incrementAndGet();
                        yield null;
                    }
                    case "isClosed" -> false;
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
