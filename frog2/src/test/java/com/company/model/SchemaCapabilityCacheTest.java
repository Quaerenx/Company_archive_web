package com.company.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SchemaCapabilityCacheTest {
    @Test
    void cachesExistingColumnAcrossCaseVariants() {
        AtomicInteger metadataQueries = new AtomicInteger();
        Connection connection = connection(metadataQueries, true);
        SchemaCapabilityCache cache = new SchemaCapabilityCache();

        assertTrue(cache.columnExists(connection, "company_users", "department"));
        assertTrue(cache.columnExists(connection, "COMPANY_USERS", "DEPARTMENT"));

        assertEquals(1, metadataQueries.get());
    }

    @Test
    void cachesMissingColumnAfterLowerAndUpperCaseLookup() {
        AtomicInteger metadataQueries = new AtomicInteger();
        Connection connection = connection(metadataQueries, false, false);
        SchemaCapabilityCache cache = new SchemaCapabilityCache();

        assertFalse(cache.columnExists(connection, "maintenance_records", "license_size_gb"));
        assertFalse(cache.columnExists(connection, "maintenance_records", "license_size_gb"));

        assertEquals(2, metadataQueries.get());
    }

    private static Connection connection(AtomicInteger queries, Boolean... results) {
        Queue<Boolean> rows = new ArrayDeque<>();
        for (Boolean result : results) {
            rows.add(result);
        }
        DatabaseMetaData metadata = (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[] {DatabaseMetaData.class},
                (ignored, call, args) -> {
                    if ("getColumns".equals(call.getName())) {
                        queries.incrementAndGet();
                        return resultSet(Boolean.TRUE.equals(rows.poll()));
                    }
                    return defaultValue(call.getReturnType());
                });
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (ignored, call, args) -> "getMetaData".equals(call.getName())
                        ? metadata
                        : defaultValue(call.getReturnType()));
    }

    private static ResultSet resultSet(boolean hasRow) {
        boolean[] first = {hasRow};
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "next" -> {
                        boolean result = first[0];
                        first[0] = false;
                        yield result;
                    }
                    default -> defaultValue(call.getReturnType());
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
