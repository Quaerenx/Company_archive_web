package com.company.controller;

import com.company.customerhistory.CustomerHistoryRecord;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.TroubleshootingDTO;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CustomerActivityViewData {
    private static final CustomerActivityViewData EMPTY = new CustomerActivityViewData(
            List.of(), List.of(), List.of());

    private final List<MaintenanceRecordDTO> maintenanceRecords;
    private final List<CustomerHistoryRecord> historyRecords;
    private final List<TroubleshootingDTO> troubleshootingRecords;
    private final Set<String> unavailableSources;

    public CustomerActivityViewData(
            List<MaintenanceRecordDTO> maintenanceRecords,
            List<CustomerHistoryRecord> historyRecords,
            List<TroubleshootingDTO> troubleshootingRecords) {
        this(maintenanceRecords, historyRecords, troubleshootingRecords, Set.of());
    }

    public CustomerActivityViewData(
            List<MaintenanceRecordDTO> maintenanceRecords,
            List<CustomerHistoryRecord> historyRecords,
            List<TroubleshootingDTO> troubleshootingRecords,
            Set<String> unavailableSources) {
        this.unavailableSources = Set.copyOf(Objects.requireNonNull(unavailableSources, "unavailableSources"));
        this.maintenanceRecords = List.copyOf(Objects.requireNonNull(
                maintenanceRecords, "maintenanceRecords"));
        this.historyRecords = List.copyOf(Objects.requireNonNull(
                historyRecords, "historyRecords"));
        this.troubleshootingRecords = List.copyOf(Objects.requireNonNull(
                troubleshootingRecords, "troubleshootingRecords"));
    }

    public boolean isMaintenanceUnavailable() {
        return unavailableSources.contains("maintenance");
    }

    public boolean isHistoryUnavailable() {
        return unavailableSources.contains("history");
    }

    public boolean isTroubleshootingUnavailable() {
        return unavailableSources.contains("troubleshooting");
    }

    public static CustomerActivityViewData empty() {
        return EMPTY;
    }

    public List<MaintenanceRecordDTO> getMaintenanceRecords() {
        return maintenanceRecords;
    }

    public List<CustomerHistoryRecord> getHistoryRecords() {
        return historyRecords;
    }

    public List<TroubleshootingDTO> getTroubleshootingRecords() {
        return troubleshootingRecords;
    }
}
