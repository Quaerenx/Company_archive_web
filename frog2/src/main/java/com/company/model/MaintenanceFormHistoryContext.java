package com.company.model;

public record MaintenanceFormHistoryContext(
        MaintenanceRecordDTO previousRecord,
        MaintenanceRecordDTO duplicateRecord) {
    public static MaintenanceFormHistoryContext empty() {
        return new MaintenanceFormHistoryContext(null, null);
    }
}
