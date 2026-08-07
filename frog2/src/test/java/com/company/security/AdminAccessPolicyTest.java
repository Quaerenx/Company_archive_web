package com.company.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdminAccessPolicyTest {
    @Test
    void missingConfigurationFailsClosed() {
        assertFalse(AdminAccessPolicy.containsUserId(null, "admin"));
        assertFalse(AdminAccessPolicy.containsUserId("", "admin"));
    }

    @Test
    void configuredIdsUseExactTrimmedMatches() {
        assertTrue(AdminAccessPolicy.containsUserId(
                "alice, admin ,bob", "admin"));
        assertFalse(AdminAccessPolicy.containsUserId(
                "alice, administrator", "admin"));
        assertFalse(AdminAccessPolicy.containsUserId(
                "alice, ADMIN", "admin"));
    }
}
