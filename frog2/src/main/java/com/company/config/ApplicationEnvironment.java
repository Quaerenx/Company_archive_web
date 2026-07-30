package com.company.config;

import java.util.Locale;

/**
 * Resolves environment-wide safety settings from JVM system properties.
 */
public final class ApplicationEnvironment {
    public static final String ENV_PROPERTY = "frog2.env";
    public static final String READ_ONLY_PROPERTY = "frog2.readOnly";

    private ApplicationEnvironment() {
    }

    public static boolean isDevelopment() {
        return "dev".equals(normalize(System.getProperty(ENV_PROPERTY)));
    }

    public static boolean isReadOnly() {
        return resolveReadOnly(System.getProperty(ENV_PROPERTY), System.getProperty(READ_ONLY_PROPERTY));
    }

    public static boolean isDatabaseWriteAllowed() {
        return !isReadOnly();
    }

    static boolean resolveReadOnly(String environment, String readOnlySetting) {
        if ("dev".equals(normalize(environment))) {
            return true;
        }
        if (readOnlySetting == null || readOnlySetting.isBlank()) {
            return false;
        }

        String normalized = normalize(readOnlySetting);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw new IllegalArgumentException(
                READ_ONLY_PROPERTY + " must be either true or false, but was: " + readOnlySetting);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
