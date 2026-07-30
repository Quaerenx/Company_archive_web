package com.company.filerepo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.config.ApplicationEnvironment;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileRepositoryConfigTest {
    private String originalEnvironment;
    private String originalRoot;
    private String originalCatalinaBase;

    @BeforeEach
    void rememberProperties() {
        originalEnvironment = System.getProperty(ApplicationEnvironment.ENV_PROPERTY);
        originalRoot = System.getProperty(FileRepositoryConfig.ROOT_PROPERTY);
        originalCatalinaBase = System.getProperty("catalina.base");
    }

    @AfterEach
    void restoreProperties() {
        restore(ApplicationEnvironment.ENV_PROPERTY, originalEnvironment);
        restore(FileRepositoryConfig.ROOT_PROPERTY, originalRoot);
        restore("catalina.base", originalCatalinaBase);
    }

    @Test
    void developmentUsesExternalDefaultWhenRootIsNotConfigured() {
        assertEquals(Path.of(FileRepositoryConfig.DEVELOPMENT_DEFAULT),
                FileRepositoryConfig.resolveRoot("dev", null));
    }

    @Test
    void nonDevelopmentRequiresExplicitAbsoluteRoot() {
        assertThrows(IllegalStateException.class,
                () -> FileRepositoryConfig.resolveRoot("prod", null));
        assertThrows(IllegalStateException.class,
                () -> FileRepositoryConfig.resolveRoot("prod", "relative/files"));
    }

    @Test
    void repositoryRootCannotBeInsideTomcatWebapps() {
        System.setProperty(ApplicationEnvironment.ENV_PROPERTY, "dev");
        System.setProperty(FileRepositoryConfig.ROOT_PROPERTY, "/tmp/frog2-tomcat/webapps/frog2/files");
        System.setProperty("catalina.base", "/tmp/frog2-tomcat");

        assertThrows(IllegalStateException.class, FileRepositoryConfig::repositoryRoot);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
