package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.company.model.MaintenanceRecordDTO;
import java.sql.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaintenanceHistoryRowViewTest {
    @Test
    void preservesDuplicateMonthsAndComparesEachRecordWithTheNextOlderRecord() {
        MaintenanceRecordDTO newest = record(
                30L, "2026-08-20", "12TB", "3TB", "25%");
        MaintenanceRecordDTO olderInSameMonth = record(
                29L, "2026-08-02", "2048GB", "512GB", "20%");

        List<MaintenanceHistoryRowView> rows =
                MaintenanceHistoryRowView.fromRecords(
                        List.of(newest, olderInSameMonth));

        assertEquals(2, rows.size());
        assertSame(newest, rows.get(0).getRecord());
        assertSame(olderInSameMonth, rows.get(1).getRecord());
        assertEquals("3", rows.get(0).getUsedTerabytes());
        assertEquals("12", rows.get(0).getCapacityTerabytes());
        assertEquals(25, rows.get(0).getUsagePercentage());
        assertEquals(25, rows.get(0).getUsageProgressPercentage());
        assertEquals("↑ 5%p", rows.get(0).getDeltaLabel());
        assertEquals("up", rows.get(0).getDeltaTone());
        assertEquals("—", rows.get(1).getDeltaLabel());
        assertEquals("unavailable", rows.get(1).getDeltaTone());
    }

    @Test
    void exposesDownAndUnchangedComparisonsWithoutRelyingOnColor() {
        List<MaintenanceHistoryRowView> rows =
                MaintenanceHistoryRowView.fromRecords(List.of(
                        record(3L, "2026-08-03", "10", "5", "50"),
                        record(2L, "2026-07-03", "10", "6", "60"),
                        record(1L, "2026-06-03", "10", "6", "60")));

        assertEquals("↓ 10%p", rows.get(0).getDeltaLabel());
        assertEquals("down", rows.get(0).getDeltaTone());
        assertEquals("— 0%p", rows.get(1).getDeltaLabel());
        assertEquals("flat", rows.get(1).getDeltaTone());
        assertEquals("—", rows.get(2).getDeltaLabel());
    }

    @Test
    void calculatesPercentageButKeepsUnavailableValuesExplicit() {
        MaintenanceRecordDTO calculated = record(
                2L, "2026-08-03", "2TB", "512GB", null);
        MaintenanceRecordDTO missing = record(
                1L, "2026-07-03", null, null, null);

        List<MaintenanceHistoryRowView> rows =
                MaintenanceHistoryRowView.fromRecords(
                        List.of(calculated, missing));

        assertEquals("0.5", rows.get(0).getUsedTerabytes());
        assertEquals("2", rows.get(0).getCapacityTerabytes());
        assertEquals(25, rows.get(0).getUsagePercentage());
        assertEquals("—", rows.get(0).getDeltaLabel());
        assertNull(rows.get(1).getUsedTerabytes());
        assertNull(rows.get(1).getCapacityTerabytes());
        assertNull(rows.get(1).getUsagePercentage());
        assertNull(rows.get(1).getUsageProgressPercentage());
    }

    @Test
    void boundsOnlyTheDecorativeRingPercentage() {
        MaintenanceHistoryRowView row =
                MaintenanceHistoryRowView.fromRecords(List.of(
                        record(1L, "2026-08-03", "10", "12", "108")))
                        .get(0);

        assertEquals(108, row.getUsagePercentage());
        assertEquals(100, row.getUsageProgressPercentage());
    }

    private static MaintenanceRecordDTO record(
            long id,
            String date,
            String capacity,
            String used,
            String percentage) {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setMaintenanceId(id);
        record.setInspectionDate(Date.valueOf(date));
        record.setLicenseSizeGb(capacity);
        record.setLicenseUsageSize(used);
        record.setLicenseUsagePct(percentage);
        return record;
    }
}
