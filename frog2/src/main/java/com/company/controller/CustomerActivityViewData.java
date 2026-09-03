package com.company.controller;

import com.company.customerhistory.CustomerHistoryRecord;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.TroubleshootingDTO;
import java.util.List;
import java.util.Objects;

public final class CustomerActivityViewData {
    private static final CustomerActivityViewData EMPTY = new CustomerActivityViewData(
            List.of(), List.of(), List.of());

    private final List<MaintenanceRecordDTO> maintenanceRecords;
    private final List<CustomerHistoryRecord> historyRecords;
    private final List<TroubleshootingDTO> troubleshootingRecords;

    public CustomerActivityViewData(
            List<MaintenanceRecordDTO> maintenanceRecords,
            List<CustomerHistoryRecord> historyRecords,
            List<TroubleshootingDTO> troubleshootingRecords) {
        this.maintenanceRecords = List.copyOf(Objects.requireNonNull(
                maintenanceRecords, "maintenanceRecords"));
        this.historyRecords = List.copyOf(Objects.requireNonNull(
                historyRecords, "historyRecords"));
        this.troubleshootingRecords = List.copyOf(Objects.requireNonNull(
                troubleshootingRecords, "troubleshootingRecords"));
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
