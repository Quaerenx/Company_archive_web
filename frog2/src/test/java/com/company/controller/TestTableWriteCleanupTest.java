package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestTableWriteCleanupTest {
    private static final List<String> TABLES = List.of(
            "maintenance_records", "customer_maintenance_schedule", "vertica_customer_detail", "company_users");

    @Test
    void failedDeleteStillCleansOtherTablesAndChecksAllTables() {
        CleanupFixture fixture = new CleanupFixture(true, false);

        SQLException failure = assertThrows(SQLException.class,
                () -> TestTableWriteE2ETest.cleanupAndVerify(fixture.connection()));

        assertEquals("Simulated delete failure", failure.getMessage());
        assertEquals(TABLES, fixture.deleteAttempts);
        assertEquals(TABLES, fixture.countAttempts);
        assertEquals(1, failure.getSuppressed().length);
        assertInstanceOf(AssertionError.class, failure.getSuppressed()[0]);
        assertEquals(1L, fixture.rows.get("maintenance_records"));
        for (String table : TABLES.subList(1, TABLES.size())) {
            assertEquals(0L, fixture.rows.get(table));
        }
    }

    @Test
    void failedFirstCountStillChecksRemainingTablesAndReportsFailure() {
        CleanupFixture fixture = new CleanupFixture(false, true);

        SQLException failure = assertThrows(SQLException.class,
                () -> TestTableWriteE2ETest.cleanupAndVerify(fixture.connection()));

        assertEquals("Simulated count failure", failure.getMessage());
        assertEquals(TABLES, fixture.deleteAttempts);
        assertEquals(TABLES, fixture.countAttempts);
        assertEquals(0, failure.getSuppressed().length);
        assertEquals(Map.of(TABLES.get(0), 0L, TABLES.get(1), 0L,
                TABLES.get(2), 0L, TABLES.get(3), 0L), fixture.rows);
    }

    private static final class CleanupFixture {
        private final boolean failFirstDelete;
        private final boolean failFirstCount;
        private final List<String> deleteAttempts = new ArrayList<>();
        private final List<String> countAttempts = new ArrayList<>();
        private final Map<String, Long> rows = new HashMap<>();

        private CleanupFixture(boolean failFirstDelete, boolean failFirstCount) {
            this.failFirstDelete = failFirstDelete;
            this.failFirstCount = failFirstCount;
            TABLES.forEach(table -> rows.put(table, 1L));
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            return statement((String) args[0]);
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private PreparedStatement statement(String sql) {
            String table = TABLES.stream().filter(sql::contains).findFirst().orElseThrow();
            return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                    new Class<?>[] {PreparedStatement.class}, (proxy, method, args) -> {
                        if ("executeUpdate".equals(method.getName())) {
                            deleteAttempts.add(table);
                            if (failFirstDelete && TABLES.getFirst().equals(table)) {
                                throw new SQLException("Simulated delete failure");
                            }
                            rows.put(table, 0L);
                            return 1;
                        }
                        if ("executeQuery".equals(method.getName())) {
                            countAttempts.add(table);
                            if (failFirstCount && TABLES.getFirst().equals(table)) {
                                throw new SQLException("Simulated count failure");
                            }
                            return countResult(rows.get(table));
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private ResultSet countResult(long count) {
            boolean[] first = {true};
            return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                    new Class<?>[] {ResultSet.class}, (proxy, method, args) -> {
                        if ("next".equals(method.getName())) {
                            boolean available = first[0];
                            first[0] = false;
                            return available;
                        }
                        if ("getLong".equals(method.getName())) {
                            return count;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }
    }
}
