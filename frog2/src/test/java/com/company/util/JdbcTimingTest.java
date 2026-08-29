package com.company.util;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.performance.RequestPerformanceContext;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

class JdbcTimingTest {
    @Test
    void recordsPreparedStatementExecutionWithoutLoggingParameterValues() throws Exception {
        AtomicInteger queryTimeout = new AtomicInteger();
        PreparedStatement delegateStatement = (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] { PreparedStatement.class },
                (proxy, method, args) -> {
                    if ("setQueryTimeout".equals(method.getName())) {
                        queryTimeout.set((Integer) args[0]);
                    }
                    return defaultValue(method.getReturnType());
                });
        Connection delegateConnection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "prepareStatement" -> delegateStatement;
                    default -> defaultValue(method.getReturnType());
                });
        AtomicLong clock = new AtomicLong();
        LongSupplier nanoTime = () -> clock.getAndAdd(4_000_000);
        Connection timed = JdbcTiming.wrap(delegateConnection, 17, nanoTime);

        RequestPerformanceContext.begin();
        PreparedStatement statement = timed.prepareStatement(
                "SELECT * FROM company_users WHERE userId = ?");
        statement.executeQuery();
        RequestPerformanceContext.Snapshot snapshot =
                RequestPerformanceContext.finish();

        assertEquals(1, snapshot.sqlCount());
        assertEquals(17, queryTimeout.get());
        assertEquals(4_000_000, snapshot.sqlDurationNanos());
        assertSame(timed, statement.getConnection());
        assertTrue(timed.isWrapperFor(Connection.class));
    }

    @Test
    void sqlSummaryRemovesLiteralValuesAndComments() {
        String summary = JdbcTiming.summarizeSql(
                "SELECT * FROM audit_log -- private note\n"
                        + "WHERE actor = 'sample-user' AND event_id = 42");

        assertEquals(
                "SELECT * FROM audit_log WHERE actor = ? AND event_id = ?",
                summary);
    }

}
