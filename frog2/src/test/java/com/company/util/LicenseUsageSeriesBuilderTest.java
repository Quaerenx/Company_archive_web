package com.company.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.company.model.MaintenanceRecordDTO;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LicenseUsageSeriesBuilderTest {
    @Test
    void buildsTheLegacySeriesShapeWithoutAnotherDatabaseQuery() {
        MaintenanceRecordDTO newest = record("2026-03-03", "4TB", null, "75%");
        MaintenanceRecordDTO oldest = record("2026-01-01", "4TB", "1024 GB", null);
        MaintenanceRecordDTO middle = record("2026-02-02", null, "1TB", "50%");
        MaintenanceRecordDTO noLicenseData = record("2026-04-04", null, null, null);
        MaintenanceRecordDTO noDate = record(null, "1TB", "1TB", "100%");

        List<Map<String, Object>> points = LicenseUsageSeriesBuilder.build(
                List.of(noLicenseData, newest, middle, oldest, noDate));

        assertEquals(3, points.size());
        assertPoint(points.get(0), "2026-01-01", 25, 1.0, 4.0);
        assertPoint(points.get(1), "2026-02-02", 50, 1.0, 2.0);
        assertPoint(points.get(2), "2026-03-03", 75, 3.0, 4.0);
    }

    @Test
    void preservesRowsWithPresentButUnparseableLicenseValues() {
        MaintenanceRecordDTO record = record("2026-01-01", "", null, null);

        Map<String, Object> point = LicenseUsageSeriesBuilder.build(List.of(record)).get(0);

        assertEquals("2026-01-01", point.get("date"));
        assertNull(point.get("value"));
        assertNull(point.get("pct"));
        assertNull(point.get("usedTb"));
        assertNull(point.get("sizeTb"));
    }

    private static MaintenanceRecordDTO record(
            String date, String size, String used, String percentage) {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setInspectionDate(date == null ? null : Date.valueOf(date));
        record.setLicenseSizeGb(size);
        record.setLicenseUsageSize(used);
        record.setLicenseUsagePct(percentage);
        return record;
    }

    private static void assertPoint(
            Map<String, Object> point,
            String date,
            Integer percentage,
            Double usedTb,
            Double sizeTb) {
        assertEquals(date, point.get("date"));
        assertEquals(percentage, point.get("value"));
        assertEquals(percentage, point.get("pct"));
        assertEquals(usedTb, point.get("usedTb"));
        assertEquals(sizeTb, point.get("sizeTb"));
    }
}
