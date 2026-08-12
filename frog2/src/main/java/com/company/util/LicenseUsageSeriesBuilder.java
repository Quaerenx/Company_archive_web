package com.company.util;

import com.company.model.MaintenanceRecordDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LicenseUsageSeriesBuilder {
    private LicenseUsageSeriesBuilder() {
    }

    public static List<Map<String, Object>> build(List<MaintenanceRecordDTO> records) {
        List<MaintenanceRecordDTO> datedRecords = new ArrayList<>();
        if (records != null) {
            for (MaintenanceRecordDTO record : records) {
                if (record != null
                        && record.getInspectionDate() != null
                        && hasLicenseValue(record)) {
                    datedRecords.add(record);
                }
            }
        }
        datedRecords.sort(Comparator.comparing(MaintenanceRecordDTO::getInspectionDate));

        List<Map<String, Object>> points = new ArrayList<>(datedRecords.size());
        for (MaintenanceRecordDTO record : datedRecords) {
            BigDecimal percentage = parsePercentage(
                    record.getLicenseUsagePct());
            Double sizeTb = parseTerabytes(record.getLicenseSizeGb());
            Double usedTb = parseTerabytes(record.getLicenseUsageSize());

            if (percentage == null && sizeTb != null && sizeTb > 0 && usedTb != null) {
                percentage = BigDecimal.valueOf(
                                (usedTb / sizeTb) * 100.0)
                        .setScale(1, RoundingMode.HALF_UP);
            }
            if (usedTb == null && sizeTb != null && percentage != null) {
                usedTb = (sizeTb * percentage.doubleValue()) / 100.0;
            }
            if (sizeTb == null
                    && usedTb != null
                    && percentage != null
                    && percentage.signum() > 0) {
                sizeTb = (usedTb * 100.0) / percentage.doubleValue();
            }

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", StrictDateParser.formatDate(record.getInspectionDate()));
            point.put("value", percentage);
            point.put("pct", percentage);
            point.put("usedTb", usedTb);
            point.put("sizeTb", sizeTb);
            points.add(point);
        }
        return points;
    }

    private static boolean hasLicenseValue(MaintenanceRecordDTO record) {
        return record.getLicenseUsagePct() != null
                || record.getLicenseSizeGb() != null
                || record.getLicenseUsageSize() != null;
    }

    private static BigDecimal parsePercentage(String value) {
        if (value == null) {
            return null;
        }
        try {
            String normalized = value.trim();
            if (normalized.endsWith("%")) {
                normalized = normalized.substring(0, normalized.length() - 1).trim();
            }
            if (!normalized.matches("[+-]?\\d+(?:\\.\\d+)?")) {
                return null;
            }
            return new BigDecimal(normalized)
                    .setScale(1, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Double parseTerabytes(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        Double number = LicenseSummaryFormatter.parseNumber(normalized);
        if (number == null) {
            return null;
        }
        return normalized.contains("gb") ? number / 1024.0 : number;
    }
}
