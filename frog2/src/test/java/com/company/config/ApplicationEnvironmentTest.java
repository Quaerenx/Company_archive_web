package com.company.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApplicationEnvironmentTest {
    @Test
    void developmentIsAlwaysReadOnly() {
        assertTrue(ApplicationEnvironment.resolveReadOnly("dev", null));
        assertTrue(ApplicationEnvironment.resolveReadOnly(" DEV ", "false"));
    }

    @Test
    void explicitReadOnlySettingAppliesOutsideDevelopment() {
        assertTrue(ApplicationEnvironment.resolveReadOnly("prod", "true"));
        assertFalse(ApplicationEnvironment.resolveReadOnly("prod", "false"));
        assertFalse(ApplicationEnvironment.resolveReadOnly(null, null));
    }

    @Test
    void invalidReadOnlySettingFailsLoudly() {
        assertThrows(IllegalArgumentException.class,
                () -> ApplicationEnvironment.resolveReadOnly("prod", "yes"));
    }
}
