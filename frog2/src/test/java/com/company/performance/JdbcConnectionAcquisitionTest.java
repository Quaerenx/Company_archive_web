package com.company.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class JdbcConnectionAcquisitionTest {
    @Test
    void recordsSuccessfulAcquisitionWithoutInspectingTheConnection() throws Exception {
        Connection connection = connection();
        AtomicLong clock = new AtomicLong();
        RequestPerformanceContext.begin();

        Connection acquired = JdbcConnectionAcquisition.acquire(
                () -> connection,
                () -> clock.getAndAdd(4_000_000));
        RequestPerformanceContext.Snapshot snapshot =
                RequestPerformanceContext.finish();

        assertSame(connection, acquired);
        assertEquals(1, snapshot.dbAcquisitionCount());
        assertEquals(4_000_000, snapshot.dbAcquisitionDurationNanos());
        assertEquals(4_000_000, snapshot.maxDbAcquisitionNanos());
    }

    @Test
    void recordsFailedAcquisitionAndPreservesTheSqlException() {
        SQLException expected = new SQLException("pool exhausted");
        AtomicLong clock = new AtomicLong();
        RequestPerformanceContext.begin();

        SQLException actual = assertThrows(
                SQLException.class,
                () -> JdbcConnectionAcquisition.acquire(
                        () -> { throw expected; },
                        () -> clock.getAndAdd(7_000_000)));
        RequestPerformanceContext.Snapshot snapshot =
                RequestPerformanceContext.finish();

        assertSame(expected, actual);
        assertEquals(1, snapshot.dbAcquisitionCount());
        assertEquals(7_000_000, snapshot.dbAcquisitionDurationNanos());
    }

    private static Connection connection() {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> null);
    }
}
