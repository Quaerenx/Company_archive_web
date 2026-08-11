package com.company.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApplicationEnvironmentTest {
    @Test
    void developmentIsAlwaysReadOnly() {
        assertTrue(ApplicationEnvironment.resolveReadOnly("dev", null));
        assertTrue(ApplicationEnvironment.resolveReadOnly(" DEV ", "false"));
    }

    @Test
    void databaseWritesRequireExplicitProductionOptIn() {
        assertTrue(ApplicationEnvironment.resolveReadOnly("prod", "true"));
        assertFalse(ApplicationEnvironment.resolveReadOnly("prod", "false"));
        assertFalse(ApplicationEnvironment.resolveReadOnly(" PROD ", " FALSE "));
    }

    @Test
    void isolatedStagingCanExplicitlyOptInToWrites() {
        assertTrue(ApplicationEnvironment.resolveReadOnly("staging", null));
        assertTrue(ApplicationEnvironment.resolveReadOnly("staging", "true"));
        assertFalse(ApplicationEnvironment.resolveReadOnly("staging", "false"));
        assertFalse(ApplicationEnvironment.resolveReadOnly(" STAGING ", " FALSE "));
    }

    @Test
    void missingOrIncompleteSettingsFailClosed() {
        assertTrue(ApplicationEnvironment.resolveReadOnly(null, null));
        assertTrue(ApplicationEnvironment.resolveReadOnly("", ""));
        assertTrue(ApplicationEnvironment.resolveReadOnly("   ", "   "));
        assertTrue(ApplicationEnvironment.resolveReadOnly("prod", null));
        assertTrue(ApplicationEnvironment.resolveReadOnly("prod", ""));
        assertTrue(ApplicationEnvironment.resolveReadOnly("prod", "   "));
        assertTrue(ApplicationEnvironment.resolveReadOnly(null, "false"));
        assertTrue(ApplicationEnvironment.resolveReadOnly("test", "false"));
        assertTrue(ApplicationEnvironment.resolveReadOnly("development", "false"));
    }

    @Test
    void invalidReadOnlySettingFailsClosed() {
        assertTrue(ApplicationEnvironment.resolveReadOnly("prod", "yes"));
        assertTrue(ApplicationEnvironment.resolveReadOnly("staging", "0"));
    }

    @Test
    void publicAccessorsFailClosedWithoutJvmSettings() {
        String originalEnvironment = System.getProperty(ApplicationEnvironment.ENV_PROPERTY);
        String originalReadOnly = System.getProperty(ApplicationEnvironment.READ_ONLY_PROPERTY);
        try {
            System.clearProperty(ApplicationEnvironment.ENV_PROPERTY);
            System.clearProperty(ApplicationEnvironment.READ_ONLY_PROPERTY);

            assertTrue(ApplicationEnvironment.isReadOnly());
            assertFalse(ApplicationEnvironment.isDatabaseWriteAllowed());

            System.setProperty(ApplicationEnvironment.ENV_PROPERTY, "prod");
            System.setProperty(ApplicationEnvironment.READ_ONLY_PROPERTY, "false");

            assertFalse(ApplicationEnvironment.isReadOnly());
            assertTrue(ApplicationEnvironment.isDatabaseWriteAllowed());
        } finally {
            restoreProperty(ApplicationEnvironment.ENV_PROPERTY, originalEnvironment);
            restoreProperty(ApplicationEnvironment.READ_ONLY_PROPERTY, originalReadOnly);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
