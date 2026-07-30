package com.company.filerepo;

import com.company.config.ApplicationEnvironment;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class FileRepositoryConfig {
    public static final String ROOT_PROPERTY = "frog2.fileRepoRoot";
    public static final String DEVELOPMENT_DEFAULT = "/opt/frog2-dev/data/files";

    private FileRepositoryConfig() {
    }

    public static Path repositoryRoot() {
        Path root = resolveRoot(
                System.getProperty(ApplicationEnvironment.ENV_PROPERTY),
                System.getProperty(ROOT_PROPERTY));
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null && !catalinaBase.isBlank()) {
            Path webapps = Path.of(catalinaBase).toAbsolutePath().normalize().resolve("webapps");
            if (root.startsWith(webapps)) {
                throw new IllegalStateException(ROOT_PROPERTY + " must be outside the Tomcat webapps directory");
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
                throw new IllegalStateException("JVM property " + ROOT_PROPERTY + " is required outside development");
            }
        }

        try {
            Path root = Path.of(value).normalize();
            if (!root.isAbsolute()) {
                throw new IllegalStateException(ROOT_PROPERTY + " must be an absolute path");
            }
            return root;
        } catch (InvalidPathException e) {
            throw new IllegalStateException(ROOT_PROPERTY + " is not a valid path", e);
        }
    }
}
