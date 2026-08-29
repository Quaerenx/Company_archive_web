package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.company.model.CustomerDTO;
import com.company.model.MaintenanceHistoryFilter;
import com.company.model.MaintenanceRecordDTO;
import com.company.model.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaintenanceHistoryViewDataTest {
    @Test
    void mapsQueryResultToTheExistingJspAttributeContract() {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setMaintenanceId(17L);
        record.setInspectionDate(Date.valueOf("2026-08-10"));
        record.setLicenseSizeGb("4TB");
        record.setLicenseUsageSize("2TB");
        record.setLicenseUsagePct("50");
        PageResult<MaintenanceRecordDTO> page =
                new PageResult<>(List.of(record), 41, 2, 20);
        MaintenanceHistoryFilter filter = MaintenanceHistoryFilter.parse(
                "2026", "23.4", "Alice");
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName("Acme");
        Map<String, Object> attributes = new HashMap<>();

        MaintenanceHistoryViewData.from(
                        page, filter, customer, "Acme")
                .expose(request(attributes));

        assertEquals(page.items(), attributes.get("records"));
        assertEquals(1,
                ((List<?>) attributes.get("historyRows")).size());
        assertEquals(1,
                ((List<?>) attributes.get("usageSeries")).size());
        assertSame(customer, attributes.get("customer"));
        assertEquals("Acme", attributes.get("customerName"));
        assertEquals(2, attributes.get("currentPage"));
        assertEquals(20, attributes.get("pageSize"));
        assertEquals(3, attributes.get("totalPages"));
        assertEquals(41, attributes.get("totalCount"));
        assertEquals(2026, attributes.get("historyYear"));
        assertEquals("23.4", attributes.get("historyVersion"));
        assertEquals("Alice", attributes.get("historyQuery"));
        assertEquals(true, attributes.get("historyFiltersActive"));
        assertEquals("history", attributes.get("viewType"));
    }

    private static HttpServletRequest request(
            Map<String, Object> attributes) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, method, args) -> {
                    if ("setAttribute".equals(method.getName())) {
                        attributes.put((String) args[0], args[1]);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

}
