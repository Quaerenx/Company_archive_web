package com.company.controller;

import com.company.model.MaintenanceRecordDTO;
import com.company.util.LicenseRiskPolicy;
import com.company.util.LicenseSummaryFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MaintenanceHistoryRowView {
    private static final String DELTA_UNAVAILABLE = "-";

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
    private final boolean versionVisible;
    private final boolean modifiedAfterCreation;

    private MaintenanceHistoryRowView(
            MaintenanceRecordDTO record,
            MaintenanceRecordDTO olderRecord,
            MaintenanceRecordDTO newerRecord,
            int rowIndex) {
        this.record = Objects.requireNonNull(record, "record");
        usedTerabytes = LicenseSummaryFormatter
                .formatUsageTerabytes(record);
        capacityTerabytes = LicenseSummaryFormatter
                .formatCapacityTerabytes(record);
        usagePercentage = LicenseSummaryFormatter
                .resolveUsagePercentageForDisplay(record);
        usageProgressPercentage = LicenseSummaryFormatter
                .resolveUsageProgressPercentageOneDecimal(record);

        previousUsagePercentage = LicenseSummaryFormatter
                .resolveUsagePercentageForDisplay(olderRecord);
        LicenseRiskPolicy.Level usageLevel =
                LicenseSummaryFormatter.resolveUsageRiskLevel(record);
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
        versionVisible = rowIndex == 0 || newerRecord == null
                || !sameText(record.getVerticaVersion(),
                        newerRecord.getVerticaVersion());
        modifiedAfterCreation = record.getUpdatedAt() != null
                && (record.getCreatedAt() == null
                        || record.getUpdatedAt().after(record.getCreatedAt()));

        if (usagePercentage == null || previousUsagePercentage == null) {
            deltaLabel = DELTA_UNAVAILABLE;
            deltaTone = "unavailable";
            return;
        }

        BigDecimal difference = usagePercentage
                .subtract(previousUsagePercentage)
                .setScale(2, RoundingMode.HALF_UP);
        if (difference.signum() > 0) {
            deltaLabel = "↑ " + formatPercentagePoint(difference) + "%p";
        } else if (difference.signum() < 0) {
            deltaLabel = "↓ " + formatPercentagePoint(difference.abs()) + "%p";
        } else {
            deltaLabel = "0.0%p";
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
            MaintenanceRecordDTO newerRecord = index > 0
                    ? records.get(index - 1)
                    : null;
            rows.add(new MaintenanceHistoryRowView(
                    records.get(index), olderRecord, newerRecord, index));
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

    public boolean isVersionVisible() {
        return versionVisible;
    }

    public boolean isModifiedAfterCreation() {
        return modifiedAfterCreation;
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
        return firstMeaningfulLine.replaceFirst("^\\d+\\s*[.)]\\s*", "");
    }

    private static String formatPercentagePoint(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 1
                ? normalized.setScale(1).toPlainString()
                : normalized.toPlainString();
    }

    private static boolean sameText(String left, String right) {
        return Objects.equals(normalize(left), normalize(right));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
