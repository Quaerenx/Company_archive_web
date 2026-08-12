package com.company.controller;

import com.company.model.MaintenanceRecordDTO;
import com.company.util.LicenseRiskPolicy;
import com.company.util.LicenseSummaryFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MaintenanceHistoryRowView {
    private static final String DELTA_UNAVAILABLE = "—";

    private final MaintenanceRecordDTO record;
    private final String usedTerabytes;
    private final String capacityTerabytes;
    private final BigDecimal usagePercentage;
    private final BigDecimal usageProgressPercentage;
    private final BigDecimal previousUsagePercentage;
    private final String usageTone;
    private final String usageStatusLabel;
    private final String deltaLabel;
    private final String deltaTone;
    private final String noteSummary;
    private final String detailId;

    private MaintenanceHistoryRowView(
            MaintenanceRecordDTO record,
            MaintenanceRecordDTO olderRecord,
            int rowIndex) {
        this.record = Objects.requireNonNull(record, "record");
        usedTerabytes = LicenseSummaryFormatter
                .formatUsageTerabytes(record);
        capacityTerabytes = LicenseSummaryFormatter
                .formatCapacityTerabytes(record);
        usagePercentage = LicenseSummaryFormatter
                .resolveUsagePercentageOneDecimal(record);
        usageProgressPercentage = LicenseSummaryFormatter
                .resolveUsageProgressPercentageOneDecimal(record);

        previousUsagePercentage = LicenseSummaryFormatter
                .resolveUsagePercentageOneDecimal(olderRecord);
        LicenseRiskPolicy.Level usageLevel =
                LicenseRiskPolicy.classify(usagePercentage);
        usageTone = switch (usageLevel) {
            case NORMAL -> "normal";
            case WARNING -> "warning";
            case RISK -> "risk";
            case UNAVAILABLE -> "unavailable";
        };
        usageStatusLabel = switch (usageLevel) {
            case NORMAL -> "정상";
            case WARNING -> "경고";
            case RISK -> "위험";
            case UNAVAILABLE -> "확인 불가";
        };
        noteSummary = summarizeNote(record.getNote());
        detailId = "maintenance-history-detail-"
                + (record.getMaintenanceId() == null
                        ? "row-" + (rowIndex + 1)
                        : record.getMaintenanceId());

        if (usagePercentage == null || previousUsagePercentage == null) {
            deltaLabel = DELTA_UNAVAILABLE;
            deltaTone = "unavailable";
            return;
        }

        BigDecimal difference = usagePercentage
                .subtract(previousUsagePercentage)
                .setScale(1);
        if (difference.signum() > 0) {
            deltaLabel = "↑ " + difference.toPlainString() + "%p";
        } else if (difference.signum() < 0) {
            deltaLabel = "↓ " + difference.abs().toPlainString() + "%p";
        } else {
            deltaLabel = "— 0.0%p";
        }
        deltaTone = "neutral";
    }

    public static List<MaintenanceHistoryRowView> fromRecords(
            List<MaintenanceRecordDTO> records) {
        Objects.requireNonNull(records, "records");
        List<MaintenanceHistoryRowView> rows =
                new ArrayList<>(records.size());
        for (int index = 0; index < records.size(); index++) {
            MaintenanceRecordDTO olderRecord = index + 1 < records.size()
                    ? records.get(index + 1)
                    : null;
            rows.add(new MaintenanceHistoryRowView(
                    records.get(index), olderRecord, index));
        }
        return List.copyOf(rows);
    }

    public MaintenanceRecordDTO getRecord() {
        return record;
    }

    public String getUsedTerabytes() {
        return usedTerabytes;
    }

    public String getCapacityTerabytes() {
        return capacityTerabytes;
    }

    public BigDecimal getUsagePercentage() {
        return usagePercentage;
    }

    public BigDecimal getUsageProgressPercentage() {
        return usageProgressPercentage;
    }

    public BigDecimal getPreviousUsagePercentage() {
        return previousUsagePercentage;
    }

    public String getUsageTone() {
        return usageTone;
    }

    public String getUsageStatusLabel() {
        return usageStatusLabel;
    }

    public String getDeltaLabel() {
        return deltaLabel;
    }

    public String getDeltaTone() {
        return deltaTone;
    }

    public String getNoteSummary() {
        return noteSummary;
    }

    public String getDetailId() {
        return detailId;
    }

    private static String summarizeNote(String note) {
        if (note == null) {
            return "특이사항 없음";
        }
        String firstMeaningfulLine = note.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .orElse(null);
        if (firstMeaningfulLine == null) {
            return "특이사항 없음";
        }
        int codePointCount = firstMeaningfulLine.codePointCount(
                0, firstMeaningfulLine.length());
        if (codePointCount <= 64) {
            return firstMeaningfulLine;
        }
        int end = firstMeaningfulLine.offsetByCodePoints(0, 63);
        return firstMeaningfulLine.substring(0, end) + "…";
    }
}
