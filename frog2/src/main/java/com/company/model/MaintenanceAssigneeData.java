package com.company.model;

import java.util.List;

public record MaintenanceAssigneeData(
        List<CustomerDTO> customers,
        List<MaintenanceCustomerAssignment> assignments) {
    public MaintenanceAssigneeData {
        customers = List.copyOf(customers);
        assignments = List.copyOf(assignments);
    }
}
