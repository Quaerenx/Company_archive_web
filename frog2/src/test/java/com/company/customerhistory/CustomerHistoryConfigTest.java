package com.company.customerhistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.config.ApplicationEnvironment;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerHistoryConfigTest {
    private String originalEnvironment;
    private String originalRoot;
    private String originalCatalinaBase;

    @BeforeEach
    void rememberProperties() {
        originalEnvironment = System.getProperty(
                ApplicationEnvironment.ENV_PROPERTY);
        originalRoot = System.getProperty(CustomerHistoryConfig.ROOT_PROPERTY);
        originalCatalinaBase = System.getProperty("catalina.base");
    }

    @AfterEach
    void restoreProperties() {
        restore(ApplicationEnvironment.ENV_PROPERTY, originalEnvironment);
        restore(CustomerHistoryConfig.ROOT_PROPERTY, originalRoot);
        restore("catalina.base", originalCatalinaBase);
    }

    @Test
    void developmentUsesExternalDefault() {
        assertEquals(
                Path.of(CustomerHistoryConfig.DEVELOPMENT_DEFAULT),
                CustomerHistoryConfig.resolveRoot("dev", null));
    }

    @Test
    void nonDevelopmentRequiresExplicitAbsoluteRoot() {
        assertThrows(
                IllegalStateException.class,
                () -> CustomerHistoryConfig.resolveRoot("prod", null));
        assertThrows(
                IllegalStateException.class,
                () -> CustomerHistoryConfig.resolveRoot("prod", "relative/history"));
    }

    @Test
    void configuredAbsoluteRootIsNormalized() {
        assertEquals(
                Path.of("/srv/archive/customer-history"),
                CustomerHistoryConfig.resolveRoot(
                        "prod", "/srv/archive/data/../customer-history"));
    }

    @Test
    void repositoryRootCannotBeInsideTomcatWebapps() {
        System.setProperty(ApplicationEnvironment.ENV_PROPERTY, "dev");
        System.setProperty(
                CustomerHistoryConfig.ROOT_PROPERTY,
                "/tmp/frog2-tomcat/webapps/frog2/customer-history");
        System.setProperty("catalina.base", "/tmp/frog2-tomcat");

        assertThrows(
                IllegalStateException.class,
                CustomerHistoryConfig::repositoryRoot);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
