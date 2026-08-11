package com.company.controller;

import com.company.model.MaintenanceRecordDTO;
import com.company.util.LicenseSummaryFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MaintenanceHistoryRowView {
    private static final String DELTA_UNAVAILABLE = "—";

    private final MaintenanceRecordDTO record;
    private final String usedTerabytes;
    private final String capacityTerabytes;
    private final Integer usagePercentage;
    private final Integer usageProgressPercentage;
    private final String deltaLabel;
    private final String deltaTone;

    private MaintenanceHistoryRowView(
            MaintenanceRecordDTO record,
            MaintenanceRecordDTO olderRecord) {
        this.record = Objects.requireNonNull(record, "record");
        usedTerabytes = LicenseSummaryFormatter
                .formatUsageTerabytes(record);
        capacityTerabytes = LicenseSummaryFormatter
                .formatCapacityTerabytes(record);
        usagePercentage = LicenseSummaryFormatter
                .resolveRoundedUsagePercentage(record);
        usageProgressPercentage = LicenseSummaryFormatter
                .resolveUsageProgressPercentage(record);

        Integer olderPercentage = LicenseSummaryFormatter
                .resolveRoundedUsagePercentage(olderRecord);
        if (usagePercentage == null || olderPercentage == null) {
            deltaLabel = DELTA_UNAVAILABLE;
            deltaTone = "unavailable";
            return;
        }

        long difference = (long) usagePercentage - olderPercentage;
        if (difference > 0) {
            deltaLabel = "↑ " + difference + "%p";
            deltaTone = "up";
        } else if (difference < 0) {
            deltaLabel = "↓ " + Math.abs(difference) + "%p";
            deltaTone = "down";
        } else {
            deltaLabel = "— 0%p";
            deltaTone = "flat";
        }
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
                    records.get(index), olderRecord));
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

    public Integer getUsagePercentage() {
        return usagePercentage;
    }

    public Integer getUsageProgressPercentage() {
        return usageProgressPercentage;
    }

    public String getDeltaLabel() {
        return deltaLabel;
    }

    public String getDeltaTone() {
        return deltaTone;
    }
}
