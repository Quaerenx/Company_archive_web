package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.model.CustomerDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaintenanceInspectorOrderingTest {
    @Test
    void preferredInspectorIsFirstWithoutDuplicatingGroups() {
        Map<String, List<CustomerDTO>> groups = new LinkedHashMap<>();
        groups.put("Alice", List.of(customer("A")));
        groups.put("Bob", List.of(customer("B")));
        groups.put("Carol", List.of(customer("C")));

        Map<String, List<CustomerDTO>> ordered =
                MaintenanceServlet.prioritizeInspector(groups, " Bob ");

        assertEquals(List.of("Bob", "Alice", "Carol"),
                List.copyOf(ordered.keySet()));
        assertEquals(3, ordered.size());
        assertEquals(List.of("Alice", "Bob", "Carol"),
                List.copyOf(groups.keySet()));
    }

    @Test
    void unknownPreferredInspectorKeepsExistingOrder() {
        Map<String, List<CustomerDTO>> groups = new LinkedHashMap<>();
        groups.put("Alice", List.of(customer("A")));
        groups.put("Bob", List.of(customer("B")));

        Map<String, List<CustomerDTO>> ordered =
                MaintenanceServlet.prioritizeInspector(groups, "Unknown");

        assertEquals(List.of("Alice", "Bob"),
                List.copyOf(ordered.keySet()));
    }

    private static CustomerDTO customer(String name) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(name);
        return customer;
    }
}
