package com.company.filerepo;

import com.company.config.ApplicationEnvironment;
import com.company.storage.ExternalStoragePathPolicy;
import java.nio.file.Path;

public final class FileRepositoryConfig {
    public static final String ROOT_PROPERTY = "frog2.fileRepoRoot";
    public static final String DEVELOPMENT_DEFAULT = "/opt/frog2-dev/data/files";

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
}
