package com.company.config;

import java.util.Locale;

/**
 * Resolves environment-wide safety settings from JVM system properties.
 */
public final class ApplicationEnvironment {
    public static final String ENV_PROPERTY = "frog2.env";
    public static final String READ_ONLY_PROPERTY = "frog2.readOnly";
    private static final String PRODUCTION_ENVIRONMENT = "prod";
    private static final String STAGING_ENVIRONMENT = "staging";

    private ApplicationEnvironment() {
    }

    public static boolean isReadOnly() {
        return resolveReadOnly(System.getProperty(ENV_PROPERTY), System.getProperty(READ_ONLY_PROPERTY));
    }

    public static boolean isDatabaseWriteAllowed() {
        return !isReadOnly();
    }

    static boolean resolveReadOnly(String environment, String readOnlySetting) {
        String normalizedEnvironment = normalize(environment);
        boolean writeCapableEnvironment =
                PRODUCTION_ENVIRONMENT.equals(normalizedEnvironment)
                        || STAGING_ENVIRONMENT.equals(normalizedEnvironment);
        return !(writeCapableEnvironment
                && "false".equals(normalize(readOnlySetting)));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
