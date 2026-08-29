package com.company.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Shared fail-closed validation for storage roots kept outside the web app.
 */
public final class ExternalStoragePathPolicy {
    private ExternalStoragePathPolicy() {
    }

    public static Path resolveRoot(
            String environment,
            String configuredRoot,
            String propertyName,
            String developmentDefault,
            String catalinaBase) {
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(developmentDefault, "developmentDefault");
        Path root = absoluteRoot(
                environment,
                configuredRoot,
                propertyName,
                developmentDefault);
        rejectSymbolicLinkRoot(root, propertyName);
        rejectDeploymentPath(root, propertyName, catalinaBase);
        return root;
    }

    public static boolean isSafeDirectory(Path path) {
        return path != null
                && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(path);
    }

    private static Path absoluteRoot(
            String environment,
            String configuredRoot,
            String propertyName,
            String developmentDefault) {
        String value = configuredRoot;
        if (value == null || value.isBlank()) {
            if ("dev".equalsIgnoreCase(
                    environment == null ? "" : environment.trim())) {
                value = developmentDefault;
            } else {
                throw new IllegalStateException(
                        "JVM property " + propertyName
                                + " is required outside development");
            }
        }

        try {
            Path root = Path.of(value);
            if (!root.isAbsolute()) {
                throw new IllegalStateException(
                        propertyName + " must be an absolute path");
            }
            return root.toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalStateException(
                    propertyName + " is not a valid path", exception);
        }
    }

    private static void rejectSymbolicLinkRoot(
            Path root, String propertyName) {
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)
                && Files.isSymbolicLink(root)) {
            throw new IllegalStateException(
                    propertyName + " must not be a symbolic link");
        }
    }

    private static void rejectDeploymentPath(
            Path root,
            String propertyName,
            String catalinaBase) {
        if (catalinaBase == null || catalinaBase.isBlank()) {
            return;
        }

        try {
            Path webapps = Path.of(catalinaBase)
                    .toAbsolutePath()
                    .normalize()
                    .resolve("webapps");
            if (root.startsWith(webapps)
                    || effectivePath(root).startsWith(effectivePath(webapps))) {
                throw new IllegalStateException(
                        propertyName
                                + " must be outside the Tomcat webapps directory");
            }
        } catch (InvalidPathException exception) {
            throw new IllegalStateException(
                    "catalina.base is not a valid path", exception);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    propertyName + " could not be validated", exception);
        }
    }

    private static Path effectivePath(Path path) throws IOException {
        Path existing = path;
        while (existing != null
                && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            throw new IOException("Storage path has no existing ancestor");
        }
        Path suffix = existing.relativize(path);
        return existing.toRealPath().resolve(suffix).normalize();
    }
}
