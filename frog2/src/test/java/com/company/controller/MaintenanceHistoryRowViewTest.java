package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.company.model.MaintenanceRecordDTO;
import java.math.BigDecimal;
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
        assertEquals(new BigDecimal("25.0"),
                rows.get(0).getUsagePercentage());
        assertEquals(new BigDecimal("25.0"),
                rows.get(0).getUsageProgressPercentage());
        assertEquals("↑ 5.0%p", rows.get(0).getDeltaLabel());
        assertEquals("neutral", rows.get(0).getDeltaTone());
        assertEquals(new BigDecimal("20.0"),
                rows.get(0).getPreviousUsagePercentage());
        assertEquals("—", rows.get(1).getDeltaLabel());
        assertEquals("unavailable", rows.get(1).getDeltaTone());
        assertNull(rows.get(1).getPreviousUsagePercentage());
    }

    @Test
    void exposesDownAndUnchangedComparisonsWithoutRelyingOnColor() {
        List<MaintenanceHistoryRowView> rows =
                MaintenanceHistoryRowView.fromRecords(List.of(
                        record(3L, "2026-08-03", "10", "5", "50"),
                        record(2L, "2026-07-03", "10", "6", "60"),
                        record(1L, "2026-06-03", "10", "6", "60")));

        assertEquals("↓ 10.0%p", rows.get(0).getDeltaLabel());
        assertEquals("neutral", rows.get(0).getDeltaTone());
        assertEquals("— 0.0%p", rows.get(1).getDeltaLabel());
        assertEquals("neutral", rows.get(1).getDeltaTone());
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
        assertEquals(new BigDecimal("25.0"),
                rows.get(0).getUsagePercentage());
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

        assertEquals(new BigDecimal("108.0"),
                row.getUsagePercentage());
        assertEquals(new BigDecimal("100.0"),
                row.getUsageProgressPercentage());
    }

    @Test
    void summarizesTheFirstMeaningfulNoteAndKeepsStableDetailIds() {
        MaintenanceRecordDTO first = record(
                31L, "2026-08-03", "10", "5", "50");
        first.setNote("\n  첫 번째 점검 내용  \n두 번째 줄");
        MaintenanceRecordDTO second = record(
                30L, "2026-07-03", "10", "5", "50");
        second.setNote("  \n\t");
        MaintenanceRecordDTO third = record(
                29L, "2026-06-03", "10", "5", "50");
        third.setNote("가".repeat(70));

        List<MaintenanceHistoryRowView> rows =
                MaintenanceHistoryRowView.fromRecords(
                        List.of(first, second, third));

        assertEquals("첫 번째 점검 내용", rows.get(0).getNoteSummary());
        assertEquals("maintenance-history-detail-31",
                rows.get(0).getDetailId());
        assertEquals("특이사항 없음", rows.get(1).getNoteSummary());
        assertEquals("가".repeat(63) + "…",
                rows.get(2).getNoteSummary());
        assertEquals(64,
                rows.get(2).getNoteSummary().codePointCount(
                        0, rows.get(2).getNoteSummary().length()));
    }

    @Test
    void mapsLicenseUsageBoundariesToNormalWarningAndRiskTones() {
        List<MaintenanceHistoryRowView> rows =
                MaintenanceHistoryRowView.fromRecords(List.of(
                        record(5L, "2026-08-03", "100", "89.9", "89.9"),
                        record(4L, "2026-07-03", "100", "90", "90"),
                        record(3L, "2026-06-03", "100", "105", "105"),
                        record(2L, "2026-05-03", "100", "105.1", "105.1"),
                        record(1L, "2026-04-03", null, null, null)));

        assertEquals("normal", rows.get(0).getUsageTone());
        assertEquals("정상", rows.get(0).getUsageStatusLabel());
        assertEquals("warning", rows.get(1).getUsageTone());
        assertEquals("경고", rows.get(1).getUsageStatusLabel());
        assertEquals("warning", rows.get(2).getUsageTone());
        assertEquals("risk", rows.get(3).getUsageTone());
        assertEquals("위험", rows.get(3).getUsageStatusLabel());
        assertEquals("unavailable", rows.get(4).getUsageTone());
    }

    @Test
    void appliesRiskThresholdsToTheDisplayedOneDecimalValue() {
        List<MaintenanceHistoryRowView> rows =
                MaintenanceHistoryRowView.fromRecords(List.of(
                        record(2L, "2026-08-03", "100", "89.95", "89.95"),
                        record(1L, "2026-07-03", "100", "105.05", "105.05")));

        assertEquals(new BigDecimal("90.0"),
                rows.get(0).getUsagePercentage());
        assertEquals("warning", rows.get(0).getUsageTone());
        assertEquals(new BigDecimal("105.1"),
                rows.get(1).getUsagePercentage());
        assertEquals("risk", rows.get(1).getUsageTone());
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
