package com.company.customerhistory;

import com.company.config.ApplicationEnvironment;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class CustomerHistoryConfig {
    public static final String ROOT_PROPERTY = "frog2.customerHistoryRoot";
    public static final String DEVELOPMENT_DEFAULT =
            "/opt/frog2-dev/data/customer-history";

    private CustomerHistoryConfig() {
    }

    public static Path repositoryRoot() {
        Path root = resolveRoot(
                System.getProperty(ApplicationEnvironment.ENV_PROPERTY),
                System.getProperty(ROOT_PROPERTY));
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null && !catalinaBase.isBlank()) {
            Path webapps = Path.of(catalinaBase)
                    .toAbsolutePath()
                    .normalize()
                    .resolve("webapps");
            if (root.startsWith(webapps)) {
                throw new IllegalStateException(
                        ROOT_PROPERTY + " must be outside the Tomcat webapps directory");
            }
        }
        return root;
    }

    static Path resolveRoot(String environment, String configuredRoot) {
        String value = configuredRoot;
        if (value == null || value.isBlank()) {
            if ("dev".equalsIgnoreCase(environment == null ? "" : environment.trim())) {
                value = DEVELOPMENT_DEFAULT;
            } else {
                throw new IllegalStateException(
                        "JVM property " + ROOT_PROPERTY
                                + " is required outside development");
            }
        }
        try {
            Path configured = Path.of(value);
            if (!configured.isAbsolute()) {
                throw new IllegalStateException(ROOT_PROPERTY + " must be an absolute path");
            }
            return configured.toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalStateException(
                    ROOT_PROPERTY + " is not a valid path", exception);
        }
    }
}
