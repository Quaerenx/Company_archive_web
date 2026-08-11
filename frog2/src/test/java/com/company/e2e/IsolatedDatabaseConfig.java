package com.company.e2e;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

final class IsolatedDatabaseConfig {
    private static final String ISOLATED_MARKER = "frog2.e2e.isolated";
    private static final String DATABASE_IDENTITY = "frog2.databaseIdentity";
    private static final List<String> REQUIRED_KEYS = List.of(
            "db.url", "db.user", "db.password", "db.driver");

    private IsolatedDatabaseConfig() {
    }

    static Properties load(Path isolatedConfig, Path sharedConfig)
            throws Exception {
        Path isolated = isolatedConfig.toRealPath().normalize();
        Path shared = sharedConfig.toRealPath().normalize();
        if (isolated.equals(shared)) {
            throw new IllegalArgumentException(
                    "Write E2E database must not use the shared DB config");
        }

        Properties isolatedProperties = read(isolated);
        Properties sharedProperties = read(shared);
        if (!"true".equalsIgnoreCase(
                isolatedProperties.getProperty(ISOLATED_MARKER, "").trim())) {
            throw new IllegalArgumentException(
                    ISOLATED_MARKER + "=true is required");
        }
        if (normalizedUrl(isolatedProperties).equals(
                normalizedUrl(sharedProperties))) {
            throw new IllegalArgumentException(
                    "Write E2E database URL must differ from the shared DB URL");
        }
        String isolatedIdentity = requiredIdentity(isolatedProperties);
        String sharedIdentity = requiredIdentity(sharedProperties);
        if (isolatedIdentity.equalsIgnoreCase(sharedIdentity)) {
            throw new IllegalArgumentException(
                    "Write E2E database identity must differ from the shared DB identity");
        }
        return isolatedProperties;
    }

    private static Properties read(Path path) throws Exception {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    "Database configuration file does not exist");
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        for (String key : REQUIRED_KEYS) {
            String value = properties.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "Missing database configuration key: " + key);
            }
        }
        return properties;
    }

    private static String normalizedUrl(Properties properties) {
        return properties.getProperty("db.url").trim();
    }

    private static String requiredIdentity(Properties properties) {
        String identity = properties.getProperty(DATABASE_IDENTITY);
        if (identity == null || identity.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing database configuration key: " + DATABASE_IDENTITY);
        }
        return identity.trim();
    }
}
