package com.company.health;

import com.company.config.ApplicationEnvironment;
import com.company.customerhistory.CustomerHistoryConfig;
import com.company.filerepo.FileRepositoryConfig;
import com.company.listener.AppLifecycleListener;
import com.company.listener.AppLifecycleListener.SchemaStatus;
import com.company.storage.ExternalStoragePathPolicy;
import com.company.util.DBConnection;
import jakarta.servlet.ServletContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class OperationalReadiness implements AutoCloseable {
    private final Supplier<DBConnection.PoolSnapshot> poolSnapshot;
    private final Supplier<Path> fileRepositoryRoot;
    private final Supplier<Path> customerHistoryRoot;
    private final BooleanSupplier readOnly;
    private final BooleanSupplier databaseProbe;
    private final CachedDatabaseProbe managedProbe;

    public OperationalReadiness() {
        this(
                DBConnection::getPoolSnapshot,
                FileRepositoryConfig::repositoryRoot,
                CustomerHistoryConfig::repositoryRoot,
                ApplicationEnvironment::isReadOnly,
                new CachedDatabaseProbe());
    }

    OperationalReadiness(
            Supplier<DBConnection.PoolSnapshot> poolSnapshot,
            Supplier<Path> fileRepositoryRoot,
            Supplier<Path> customerHistoryRoot,
            BooleanSupplier readOnly) {
        this(poolSnapshot, fileRepositoryRoot, customerHistoryRoot, readOnly, () -> true);
    }

    OperationalReadiness(
            Supplier<DBConnection.PoolSnapshot> poolSnapshot,
            Supplier<Path> fileRepositoryRoot,
            Supplier<Path> customerHistoryRoot,
            BooleanSupplier readOnly,
            BooleanSupplier databaseProbe) {
        this.poolSnapshot = Objects.requireNonNull(
                poolSnapshot, "poolSnapshot");
        this.fileRepositoryRoot = Objects.requireNonNull(
                fileRepositoryRoot, "fileRepositoryRoot");
        this.customerHistoryRoot = Objects.requireNonNull(
                customerHistoryRoot, "customerHistoryRoot");
        this.readOnly = Objects.requireNonNull(readOnly, "readOnly");
        this.databaseProbe = Objects.requireNonNull(databaseProbe, "databaseProbe");
        managedProbe = databaseProbe instanceof CachedDatabaseProbe probe ? probe : null;
    }

    public Report inspect(ServletContext context) {
        boolean schemaReady = context != null
                && context.getAttribute(
                        AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE)
                        == SchemaStatus.READY;
        boolean databaseReady = safePoolReady();
        boolean requireWritable = !readOnly.getAsBoolean();
        boolean fileRepositoryReady = safeStorageReady(
                fileRepositoryRoot, requireWritable);
        boolean customerHistoryReady = safeStorageReady(
                customerHistoryRoot, requireWritable);
        return new Report(
                schemaReady,
                databaseReady,
                fileRepositoryReady,
                customerHistoryReady);
    }

    private boolean safePoolReady() {
        try {
            DBConnection.PoolSnapshot snapshot = poolSnapshot.get();
            return snapshot != null && snapshot.ready() && databaseProbe.getAsBoolean();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public void close() {
        if (managedProbe != null) {
            managedProbe.close();
        }
    }

    private static boolean safeStorageReady(
            Supplier<Path> rootSupplier, boolean requireWritable) {
        try {
            Path root = rootSupplier.get();
            return ExternalStoragePathPolicy.isSafeDirectory(root)
                    && Files.isReadable(root)
                    && Files.isExecutable(root)
                    && (!requireWritable || Files.isWritable(root));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public record Report(
            boolean schemaReady,
            boolean databaseReady,
            boolean fileRepositoryReady,
            boolean customerHistoryReady) {
        public boolean ready() {
            return schemaReady
                    && databaseReady
                    && fileRepositoryReady
                    && customerHistoryReady;
        }
    }
}
