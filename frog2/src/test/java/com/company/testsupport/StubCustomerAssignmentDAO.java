package com.company.testsupport;

import com.company.model.CustomerAssignmentDAO;
import com.company.model.CustomerDTO;
import com.company.model.MaintenanceCustomerAssignment;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class StubCustomerAssignmentDAO extends CustomerAssignmentDAO {
    private final List<CustomerDTO> maintenanceCustomers;
    private final Set<String> customerNames;

    public StubCustomerAssignmentDAO(CustomerDTO... customers) {
        this(Arrays.asList(customers));
    }

    public StubCustomerAssignmentDAO(List<CustomerDTO> customers) {
        maintenanceCustomers = List.copyOf(customers);
        customerNames = maintenanceCustomers.stream()
                .map(CustomerDTO::getCustomerName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static StubCustomerAssignmentDAO assignedTo(String... customerNames) {
        List<CustomerDTO> customers = Arrays.stream(customerNames)
                .map(StubCustomerAssignmentDAO::maintenanceCustomer)
                .toList();
        return new StubCustomerAssignmentDAO(customers);
    }

    @Override
    public List<CustomerDTO> getMaintenanceCustomersByAssignee(
            String userId,
            String displayName) {
        return maintenanceCustomers;
    }

    @Override
    public Set<String> getCustomerNamesByAssignee(
            String userId,
            String displayName) {
        return customerNames;
    }

    @Override
    public List<MaintenanceCustomerAssignment>
            getAllMaintenanceCustomerAssignments() {
        return maintenanceCustomers.stream()
                .map(customer -> new MaintenanceCustomerAssignment(
                        customer.getCustomerName(),
                        customer.getManagerName()))
                .toList();
    }

    private static CustomerDTO maintenanceCustomer(String customerName) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(customerName);
        customer.setCustomerType("정기점검 계약 고객사");
        return customer;
    }
}
