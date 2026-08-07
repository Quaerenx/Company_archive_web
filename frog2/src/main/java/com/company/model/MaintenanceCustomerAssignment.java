package com.company.model;

public record MaintenanceCustomerAssignment(
        String customerName,
        String managerName,
        MaintenanceSchedule schedule) {
    public MaintenanceCustomerAssignment(
            String customerName,
            String managerName) {
        this(customerName, managerName, MaintenanceSchedule.monthlyDefault());
    }

    public MaintenanceCustomerAssignment {
        if (schedule == null) {
            schedule = MaintenanceSchedule.monthlyDefault();
        }
    }
}
