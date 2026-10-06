package com.company.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record MaintenanceLicenseValues(BigDecimal capacityTb, BigDecimal usedTb, BigDecimal percentage) {
    static final String MIGRATION = "V20261006_16";
    static final List<String> COLUMNS = List.of(
            "license_capacity_tb", "license_used_tb", "license_usage_pct_value");
    private static final BigDecimal MAX = new BigDecimal("1000000");
    private static final Pattern CAPACITY = Pattern.compile(
            "^([+-]?\\d+(?:\\.\\d{1,6})?)\\s*(TB|GB)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERCENTAGE = Pattern.compile(
            "^([+-]?\\d+(?:\\.\\d{1,6})?)\\s*%?$");

    static MaintenanceLicenseValues fromLegacy(MaintenanceRecordDTO record) {
        // Old strings are preserved separately, including unsupported capacity units.
        return new MaintenanceLicenseValues(
                terabytes(record.getLicenseSizeGb()),
                terabytes(record.getLicenseUsageSize()),
                percentage(record.getLicenseUsagePct()));
    }

    private static BigDecimal terabytes(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher match = CAPACITY.matcher(raw.strip());
        if (!match.matches()) {
            return null;
        }
        BigDecimal value = bounded(match.group(1));
        if (value == null) {
            return null;
        }
        return "GB".equalsIgnoreCase(match.group(2))
                ? value.divide(BigDecimal.valueOf(1024), 6, RoundingMode.HALF_UP)
                : value;
    }

    private static BigDecimal percentage(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher match = PERCENTAGE.matcher(raw.strip());
        return match.matches() ? bounded(match.group(1)) : null;
    }

    private static BigDecimal bounded(String number) {
        BigDecimal value = new BigDecimal(number);
        return value.signum() >= 0 && value.compareTo(MAX) <= 0 ? value : null;
    }
}
