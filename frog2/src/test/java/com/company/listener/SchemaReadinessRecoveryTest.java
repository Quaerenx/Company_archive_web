package com.company.listener;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.listener.AppLifecycleListener.SchemaStatus;
import com.company.model.DataAccessException;
import com.company.model.DatabaseSchemaReadiness;
import jakarta.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SchemaReadinessRecoveryTest {
    @Test
    void unavailableSchemaRetriesThenRecoversWithoutRestart() throws Exception {
        ContextFixture context = new ContextFixture(SchemaStatus.UNAVAILABLE);
        AtomicInteger checks = new AtomicInteger();
        try (SchemaReadinessRecovery recovery = recovery(context, () -> {
            if (checks.incrementAndGet() == 1) {
                throw unavailable();
            }
            return new DatabaseSchemaReadiness.Report(List.of(), List.of());
        })) {
            recovery.start();
            assertTrue(context.resolved.await(1, TimeUnit.SECONDS));
            assertEquals(SchemaStatus.READY, context.status());
            assertEquals(2, checks.get());
        }
    }

    @Test
    void incompatibleSchemaIsPublishedAndStopsRetrying() throws Exception {
        ContextFixture context = new ContextFixture(SchemaStatus.UNAVAILABLE);
        AtomicInteger checks = new AtomicInteger();
        var requirement = new DatabaseSchemaReadiness.Requirement(
                "required", "maintenance_records", "license_usage_size", true);
        try (SchemaReadinessRecovery recovery = recovery(context, () -> {
            checks.incrementAndGet();
            return new DatabaseSchemaReadiness.Report(List.of(requirement), List.of());
        })) {
            recovery.start();
            assertTrue(context.resolved.await(1, TimeUnit.SECONDS));
            assertEquals(SchemaStatus.INCOMPATIBLE, context.status());
            recovery.start();
            assertEquals(1, checks.get());
        }
    }

    @Test
    void alreadyReadyContextDoesNotScheduleInspection() {
        ContextFixture context = new ContextFixture(SchemaStatus.READY);
        AtomicInteger checks = new AtomicInteger();
        try (SchemaReadinessRecovery recovery = recovery(context, () -> {
            checks.incrementAndGet();
            return new DatabaseSchemaReadiness.Report(List.of(), List.of());
        })) {
            recovery.start();
            assertEquals(0, checks.get());
        }
    }

    @Test
    void closingPreventsLateInspectionFromPublishingReady() throws Exception {
        ContextFixture context = new ContextFixture(SchemaStatus.UNAVAILABLE);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        SchemaReadinessRecovery recovery = recovery(context, () -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException exception) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
            return new DatabaseSchemaReadiness.Report(List.of(), List.of());
        });
        try {
            recovery.start();
            assertTrue(started.await(1, TimeUnit.SECONDS));
            recovery.close();
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
            assertEquals(SchemaStatus.UNAVAILABLE, context.status());
        } finally {
            recovery.close();
        }
    }

    private static SchemaReadinessRecovery recovery(
            ContextFixture context,
            java.util.function.Supplier<DatabaseSchemaReadiness.Report> inspection) {
        return new SchemaReadinessRecovery(
                context.proxy(), inspection, Duration.ofMillis(10), Duration.ofMillis(40));
    }

    private static DataAccessException unavailable() {
        return DataAccessException.from("readiness test", new SQLException("offline"));
    }

    private static final class ContextFixture {
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();
        private final CountDownLatch resolved = new CountDownLatch(1);

        private ContextFixture(SchemaStatus status) {
            attributes.put(AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE, status);
        }

        private SchemaStatus status() {
            return (SchemaStatus) attributes.get(AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE);
        }

        private ServletContext proxy() {
            return (ServletContext) Proxy.newProxyInstance(
                    ServletContext.class.getClassLoader(), new Class<?>[] {ServletContext.class},
                    (ignored, method, args) -> switch (method.getName()) {
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            if (AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE.equals(args[0])
                                    && args[1] != SchemaStatus.UNAVAILABLE) {
                                resolved.countDown();
                            }
                            yield null;
                        }
                        case "getAttribute" -> attributes.get((String) args[0]);
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }
}
