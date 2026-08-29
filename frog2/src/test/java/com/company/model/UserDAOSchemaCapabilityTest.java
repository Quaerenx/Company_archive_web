package com.company.model;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class UserDAOSchemaCapabilityTest {
    @Test
    void reusesDepartmentCapabilityAcrossDaoInstances() {
        AtomicInteger metadataQueries = new AtomicInteger();
        AtomicInteger selectQueries = new AtomicInteger();
        DatabaseMetaData metadata = metadata(metadataQueries);
        JdbcConnectionProvider provider = () -> connection(metadata, selectQueries);
        SchemaCapabilityCache sharedCache = new SchemaCapabilityCache();
        UserDAO first = new UserDAO(provider, sharedCache);
        UserDAO second = new UserDAO(provider, sharedCache);

        assertNull(first.getUserById("first"));
        assertNull(second.getUserById("second"));

        assertEquals(1, metadataQueries.get());
        assertEquals(2, selectQueries.get());
    }

    private static DatabaseMetaData metadata(AtomicInteger queries) {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[] {DatabaseMetaData.class},
                (ignored, call, args) -> {
                    if ("getColumns".equals(call.getName())) {
                        queries.incrementAndGet();
                        return resultSet(true);
                    }
                    return defaultValue(call.getReturnType());
                });
    }

    private static Connection connection(
            DatabaseMetaData metadata, AtomicInteger selectQueries) {
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "executeQuery" -> {
                        selectQueries.incrementAndGet();
                        yield resultSet(false);
                    }
                    default -> defaultValue(call.getReturnType());
                });
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "getMetaData" -> metadata;
                    case "prepareStatement" -> statement;
                    default -> defaultValue(call.getReturnType());
                });
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

}
