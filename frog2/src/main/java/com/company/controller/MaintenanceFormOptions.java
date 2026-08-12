package com.company.controller;

import com.company.model.CustomerDTO;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record MaintenanceFormOptions(
        List<CustomerDTO> customers, List<String> inspectors) {
    public MaintenanceFormOptions {
        customers = List.copyOf(customers);
        inspectors = List.copyOf(inspectors);
    }

    public List<CustomerDTO> getCustomers() {
        return customers;
    }

    public List<String> getInspectors() {
        return inspectors;
    }

    static MaintenanceFormOptions from(
            List<CustomerDTO> source, String retainedInspector) {
        List<CustomerDTO> customers = new ArrayList<>();
        Set<String> inspectors = new LinkedHashSet<>();
        if (source != null) {
            for (CustomerDTO customer : source) {
                if (customer == null || isBlank(customer.getCustomerName())) {
                    continue;
                }
                customers.add(customer);
                add(inspectors, customer.getManagerName());
                add(inspectors, customer.getSubManagerName());
            }
        }
        add(inspectors, retainedInspector);
        return new MaintenanceFormOptions(
                customers, new ArrayList<>(inspectors));
    }

    CustomerDTO customer(String customerName) {
        String normalized = normalize(customerName);
        if (normalized == null) {
            return null;
        }
        for (CustomerDTO customer : customers) {
            if (normalized.equals(normalize(customer.getCustomerName()))) {
                return customer;
            }
        }
        return null;
    }

    boolean hasCustomer(String customerName) {
        return customer(customerName) != null;
    }

    boolean hasInspector(String inspectorName) {
        String normalized = normalize(inspectorName);
        return normalized != null && inspectors.contains(normalized);
    }

    private static void add(Set<String> values, String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            values.add(normalized);
        }
    }

    private static String normalize(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
