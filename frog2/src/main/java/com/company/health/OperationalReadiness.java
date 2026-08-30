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

public final class OperationalReadiness {
    private final Supplier<DBConnection.PoolSnapshot> poolSnapshot;
    private final Supplier<Path> fileRepositoryRoot;
    private final Supplier<Path> customerHistoryRoot;
    private final BooleanSupplier readOnly;

    public OperationalReadiness() {
        this(
                DBConnection::getPoolSnapshot,
                FileRepositoryConfig::repositoryRoot,
                CustomerHistoryConfig::repositoryRoot,
                ApplicationEnvironment::isReadOnly);
    }

    OperationalReadiness(
            Supplier<DBConnection.PoolSnapshot> poolSnapshot,
            Supplier<Path> fileRepositoryRoot,
            Supplier<Path> customerHistoryRoot,
            BooleanSupplier readOnly) {
        this.poolSnapshot = Objects.requireNonNull(
                poolSnapshot, "poolSnapshot");
        this.fileRepositoryRoot = Objects.requireNonNull(
                fileRepositoryRoot, "fileRepositoryRoot");
        this.customerHistoryRoot = Objects.requireNonNull(
                customerHistoryRoot, "customerHistoryRoot");
        this.readOnly = Objects.requireNonNull(readOnly, "readOnly");
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
            return snapshot != null && snapshot.ready();
        } catch (RuntimeException exception) {
            return false;
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
