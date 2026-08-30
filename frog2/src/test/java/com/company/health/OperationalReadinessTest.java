package com.company.health;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.listener.AppLifecycleListener;
import com.company.listener.AppLifecycleListener.SchemaStatus;
import com.company.util.DBConnection;
import jakarta.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OperationalReadinessTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void allRequiredComponentsProduceReadyReport() {
        OperationalReadiness readiness = new OperationalReadiness(
                () -> new DBConnection.PoolSnapshot(true, 0, 2, 2, 0),
                () -> temporaryDirectory,
                () -> temporaryDirectory,
                () -> true);

        OperationalReadiness.Report report = readiness.inspect(
                context(SchemaStatus.READY));

        assertTrue(report.ready());
        assertTrue(report.databaseReady());
        assertTrue(report.fileRepositoryReady());
        assertTrue(report.customerHistoryReady());
    }

    @Test
    void missingSchemaPoolOrStorageFailsClosed() {
        Path missing = temporaryDirectory.resolve("missing");
        OperationalReadiness readiness = new OperationalReadiness(
                DBConnection.PoolSnapshot::unavailable,
                () -> missing,
                () -> temporaryDirectory,
                () -> false);

        OperationalReadiness.Report report = readiness.inspect(
                context(SchemaStatus.INCOMPATIBLE));

        assertFalse(report.ready());
        assertFalse(report.schemaReady());
        assertFalse(report.databaseReady());
        assertFalse(report.fileRepositoryReady());
        assertTrue(report.customerHistoryReady());
    }

    @Test
    void failingSuppliersBecomeDownComponents() {
        OperationalReadiness readiness = new OperationalReadiness(
                () -> {
                    throw new IllegalStateException("pool unavailable");
                },
                () -> {
                    throw new IllegalStateException("path unavailable");
                },
                () -> temporaryDirectory,
                () -> true);

        OperationalReadiness.Report report = readiness.inspect(
                context(SchemaStatus.READY));

        assertFalse(report.ready());
        assertFalse(report.databaseReady());
        assertFalse(report.fileRepositoryReady());
    }

    private static ServletContext context(SchemaStatus status) {
        Map<String, Object> attributes = Map.of(
                AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE, status);
        return (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[] {ServletContext.class},
                (ignored, method, arguments) -> "getAttribute".equals(
                        method.getName())
                        ? attributes.get((String) arguments[0])
                        : null);
    }
}
