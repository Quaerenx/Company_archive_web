package com.company.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.listener.AppLifecycleListener.SchemaStatus;
import com.company.model.DataAccessException;
import com.company.model.DatabaseSchemaReadiness;
import jakarta.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AppLifecycleListenerReadinessTest {
    @Test
    void publishesReadyStateAndOptionalWarningsSeparately() {
        ContextFixture context = new ContextFixture();
        DatabaseSchemaReadiness.Requirement optional =
                new DatabaseSchemaReadiness.Requirement(
                        "legacy", "company_users", "department", false);

        AppLifecycleListener.publishSchemaReadiness(
                context.proxy(),
                () -> new DatabaseSchemaReadiness.Report(
                        List.of(), List.of(optional)));

        assertEquals(
                SchemaStatus.READY,
                context.attributes.get(
                        AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE));
        assertEquals(
                true,
                context.attributes.get(
                        AppLifecycleListener.SCHEMA_READY_ATTRIBUTE));
        assertEquals(
                List.of(optional),
                context.attributes.get(
                        AppLifecycleListener.SCHEMA_OPTIONAL_MISSING_ATTRIBUTE));
    }

    @Test
    void distinguishesIncompatibleSchemaFromUnavailableDatabase() {
        DatabaseSchemaReadiness.Requirement required =
                new DatabaseSchemaReadiness.Requirement(
                        "required", "maintenance_records",
                        "license_usage_size", true);
        ContextFixture incompatible = new ContextFixture();

        AppLifecycleListener.publishSchemaReadiness(
                incompatible.proxy(),
                () -> new DatabaseSchemaReadiness.Report(
                        List.of(required), List.of()));

        assertEquals(
                SchemaStatus.INCOMPATIBLE,
                incompatible.attributes.get(
                        AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE));
        assertFalse((Boolean) incompatible.attributes.get(
                AppLifecycleListener.SCHEMA_READY_ATTRIBUTE));

        ContextFixture unavailable = new ContextFixture();
        AppLifecycleListener.publishSchemaReadiness(
                unavailable.proxy(),
                () -> {
                    throw DataAccessException.from(
                            "readiness test", new SQLException("offline"));
                });

        assertEquals(
                SchemaStatus.UNAVAILABLE,
                unavailable.attributes.get(
                        AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE));
        assertFalse((Boolean) unavailable.attributes.get(
                AppLifecycleListener.SCHEMA_READY_ATTRIBUTE));
        assertTrue(((List<?>) unavailable.attributes.get(
                AppLifecycleListener.SCHEMA_MISSING_ATTRIBUTE)).isEmpty());
    }

    private static final class ContextFixture {
        private final Map<String, Object> attributes = new HashMap<>();

        private ServletContext proxy() {
            return (ServletContext) Proxy.newProxyInstance(
                    ServletContext.class.getClassLoader(),
                    new Class<?>[] {ServletContext.class},
                    (ignored, method, arguments) -> switch (method.getName()) {
                        case "setAttribute" -> {
                            attributes.put(
                                    (String) arguments[0], arguments[1]);
                            yield null;
                        }
                        case "getAttribute" ->
                                attributes.get((String) arguments[0]);
                        default -> defaultValue(method.getReturnType());
                    });
        }
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
