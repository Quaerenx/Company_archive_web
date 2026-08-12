package com.company.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LicenseRiskPolicyTest {
    @Test
    void classifiesExactUsageBoundaries() {
        assertEquals(
                LicenseRiskPolicy.Level.UNAVAILABLE,
                LicenseRiskPolicy.classify(null));
        assertEquals(
                LicenseRiskPolicy.Level.NORMAL,
                LicenseRiskPolicy.classify(new BigDecimal("89.9")));
        assertEquals(
                LicenseRiskPolicy.Level.WARNING,
                LicenseRiskPolicy.classify(new BigDecimal("90.0")));
        assertEquals(
                LicenseRiskPolicy.Level.WARNING,
                LicenseRiskPolicy.classify(new BigDecimal("105.0")));
        assertEquals(
                LicenseRiskPolicy.Level.RISK,
                LicenseRiskPolicy.classify(new BigDecimal("105.1")));
    }

    @Test
    void dashboardAttentionStillIncludesWarnings() {
        assertFalse(LicenseRiskPolicy.requiresAttention(
                new BigDecimal("89.9")));
        assertTrue(LicenseRiskPolicy.requiresAttention(
                new BigDecimal("90.0")));
        assertTrue(LicenseRiskPolicy.requiresAttention(
                new BigDecimal("105.1")));
        assertFalse(LicenseRiskPolicy.isAtRisk(
                new BigDecimal("105.0")));
        assertTrue(LicenseRiskPolicy.isAtRisk(
                new BigDecimal("105.1")));
    }
}
