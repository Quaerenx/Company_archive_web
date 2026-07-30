package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MaintenanceRecordDAOSchemaCapabilityTest {
    @Test
    void reusesOptionalColumnCapabilityAcrossDaoInstances() {
        AtomicInteger metadataQueries = new AtomicInteger();
        Connection connection = connection(metadataQueries);
        SchemaCapabilityCache sharedCache = new SchemaCapabilityCache();
        MaintenanceRecordDAO first = new MaintenanceRecordDAO(sharedCache);
        MaintenanceRecordDAO second = new MaintenanceRecordDAO(sharedCache);

        assertTrue(first.columnExists(
                connection, "maintenance_records", "license_size_gb"));
        assertTrue(second.columnExists(
                connection, "maintenance_records", "license_size_gb"));

        assertEquals(1, metadataQueries.get());
    }

    private static Connection connection(AtomicInteger metadataQueries) {
        DatabaseMetaData metadata = (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[] {DatabaseMetaData.class},
                (ignored, call, args) -> {
                    if ("getColumns".equals(call.getName())) {
                        metadataQueries.incrementAndGet();
                        return resultSet(true);
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
        AtomicBoolean first = new AtomicBoolean(hasRow);
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "next" -> first.getAndSet(false);
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
