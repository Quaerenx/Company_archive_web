package com.company.util;

import java.math.BigDecimal;

public final class LicenseRiskPolicy {
    public static final BigDecimal WARNING_THRESHOLD_PERCENT =
            new BigDecimal("90.0");
    public static final BigDecimal RISK_THRESHOLD_PERCENT =
            new BigDecimal("105.1");

    public enum Level {
        UNAVAILABLE,
        NORMAL,
        WARNING,
        RISK
    }

    private LicenseRiskPolicy() {
    }

    public static Level classify(Number usagePercentage) {
        if (usagePercentage == null) {
            return Level.UNAVAILABLE;
        }
        double numericValue = usagePercentage.doubleValue();
        if (!Double.isFinite(numericValue)) {
            return Level.UNAVAILABLE;
        }
        BigDecimal percentage = usagePercentage instanceof BigDecimal decimal
                ? decimal
                : BigDecimal.valueOf(numericValue);
        if (percentage.compareTo(RISK_THRESHOLD_PERCENT) >= 0) {
            return Level.RISK;
        }
        if (percentage.compareTo(WARNING_THRESHOLD_PERCENT) >= 0) {
            return Level.WARNING;
        }
        return Level.NORMAL;
    }

    public static boolean requiresAttention(Number usagePercentage) {
        Level level = classify(usagePercentage);
        return level == Level.WARNING || level == Level.RISK;
    }

    public static boolean isAtRisk(Number usagePercentage) {
        return classify(usagePercentage) == Level.RISK;
    }
}
