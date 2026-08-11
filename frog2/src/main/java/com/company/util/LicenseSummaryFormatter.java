package com.company.util;

import com.company.model.MaintenanceRecordDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class LicenseSummaryFormatter {
    private LicenseSummaryFormatter() {
    }

    public static String format(MaintenanceRecordDTO record) {
        if (record == null) {
            return null;
        }
        Double sizeGb = toGigabytes(record.getLicenseSizeGb());
        Double usageGb = toGigabytes(record.getLicenseUsageSize());
        Double percentage = resolveUsagePercentage(record);

        Double sizeTb = sizeGb == null ? null : sizeGb / 1024.0;
        Double usageTb = usageGb == null ? null : usageGb / 1024.0;
        if (sizeTb == null && usageTb == null && percentage == null) {
            return null;
        }

        String size = sizeTb == null ? "-" : formatLegacyTerabytes(sizeTb);
        String usage = usageTb == null ? "-" : formatLegacyTerabytes(usageTb);
        String pct = percentage == null ? "-" : Math.round(percentage) + "%";
        return size + " 중 " + usage + " 총 " + pct + " 사용 중";
    }

    public static String formatUsageTerabytes(
            MaintenanceRecordDTO record) {
        return record == null
                ? null
                : formatGigabytesAsTerabytes(
                        toGigabytes(record.getLicenseUsageSize()));
    }

    public static String formatCapacityTerabytes(
            MaintenanceRecordDTO record) {
        return record == null
                ? null
                : formatGigabytesAsTerabytes(
                        toGigabytes(record.getLicenseSizeGb()));
    }

    public static Integer resolveRoundedUsagePercentage(
            MaintenanceRecordDTO record) {
        Double percentage = resolveUsagePercentage(record);
        if (percentage == null) {
            return null;
        }
        long rounded = Math.round(percentage);
        if (rounded > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (rounded < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) rounded;
    }

    public static Integer resolveUsageProgressPercentage(
            MaintenanceRecordDTO record) {
        Integer percentage = resolveRoundedUsagePercentage(record);
        if (percentage == null) {
            return null;
        }
        return Math.max(0, Math.min(100, percentage));
    }

    public static Double resolveUsagePercentage(MaintenanceRecordDTO record) {
        if (record == null) {
            return null;
        }

        Double percentage = parseNumber(record.getLicenseUsagePct());
        if (percentage != null) {
            return percentage;
        }

        Double sizeGb = toGigabytes(record.getLicenseSizeGb());
        Double usageGb = toGigabytes(record.getLicenseUsageSize());
        if (sizeGb == null || usageGb == null || sizeGb <= 0) {
            return null;
        }
        return (usageGb / sizeGb) * 100.0;
    }

    public static Double parseNumber(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replace(",", "")
                .replaceAll("[^0-9.\\-]", "");
        if (normalized.isEmpty() || "-".equals(normalized) || ".".equals(normalized)) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Double toGigabytes(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        Double number = parseNumber(normalized);
        if (number == null) {
            return null;
        }
        boolean gigabytes = normalized.contains("gb");
        boolean terabytes = normalized.contains("tb");
        return terabytes || !gigabytes ? number * 1024.0 : number;
    }

    private static String formatGigabytesAsTerabytes(Double value) {
        return value == null ? null : formatTerabytes(value / 1024.0);
    }

    private static String formatLegacyTerabytes(double value) {
        return String.format(Locale.US, "%.2fTB", value);
    }

    private static String formatTerabytes(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
