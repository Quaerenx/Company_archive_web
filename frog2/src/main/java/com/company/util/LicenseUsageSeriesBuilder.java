package com.company.util;

import com.company.model.MaintenanceRecordDTO;
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
            Integer percentage = parsePercentage(record.getLicenseUsagePct());
            Double sizeTb = parseTerabytes(record.getLicenseSizeGb());
            Double usedTb = parseTerabytes(record.getLicenseUsageSize());

            if (percentage == null && sizeTb != null && sizeTb > 0 && usedTb != null) {
                percentage = (int) Math.round((usedTb / sizeTb) * 100.0);
            }
            if (usedTb == null && sizeTb != null && percentage != null) {
                usedTb = (sizeTb * percentage) / 100.0;
            }
            if (sizeTb == null && usedTb != null && percentage != null && percentage > 0) {
                sizeTb = (usedTb * 100.0) / percentage;
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

    private static Integer parsePercentage(String value) {
        if (value == null) {
            return null;
        }
        try {
            String normalized = value.trim().replaceAll("[^0-9-]", "");
            return normalized.isEmpty() ? null : Integer.valueOf(normalized);
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
