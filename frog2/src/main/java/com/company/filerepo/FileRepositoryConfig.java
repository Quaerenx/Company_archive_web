package com.company.filerepo;

import com.company.config.ApplicationEnvironment;
import com.company.storage.ExternalStoragePathPolicy;
import java.nio.file.Path;

public final class FileRepositoryConfig {
    public static final String ROOT_PROPERTY = "frog2.fileRepoRoot";
    public static final String MAX_BYTES_PROPERTY = "frog2.fileRepoMaxBytes";
    public static final String MAX_FILES_PROPERTY = "frog2.fileRepoMaxFiles";
    public static final String DEVELOPMENT_DEFAULT = "/opt/frog2-dev/data/files";
    static final long DEFAULT_MAX_BYTES = 10L * 1024L * 1024L * 1024L;
    static final long DEFAULT_MAX_FILES = 20_000L;

    private FileRepositoryConfig() {
    }

    public static Path repositoryRoot() {
        return ExternalStoragePathPolicy.resolveRoot(
                System.getProperty(ApplicationEnvironment.ENV_PROPERTY),
                System.getProperty(ROOT_PROPERTY),
                ROOT_PROPERTY,
                DEVELOPMENT_DEFAULT,
                System.getProperty("catalina.base"));
    }

    static Path resolveRoot(String environment, String configuredRoot) {
        return ExternalStoragePathPolicy.resolveRoot(
                environment,
                configuredRoot,
                ROOT_PROPERTY,
                DEVELOPMENT_DEFAULT,
                null);
    }

    static long maxBytes() {
        return positiveLong(MAX_BYTES_PROPERTY, DEFAULT_MAX_BYTES);
    }

    static long maxFiles() {
        return positiveLong(MAX_FILES_PROPERTY, DEFAULT_MAX_FILES);
    }

    private static long positiveLong(String property, long defaultValue) {
        String configured = System.getProperty(property);
        if (configured == null || configured.isBlank()) {
            return defaultValue;
        }
        try {
            long value = Long.parseLong(configured.trim());
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // Report the property name without leaking its configured value.
        }
        throw new IllegalStateException(property + " must be a positive integer");
    }
}
