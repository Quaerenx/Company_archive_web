package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class DaoInjectedConnectionBehaviorTest {
    @Test
    void maintenancePointAndMonthLookupsUseInjectedConnections() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(maintenanceRow(17L, "2026-08-12"));
        jdbc.enqueue(maintenanceRow(18L, "2026-08-20"));
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        MaintenanceRecordDTO record = dao.getMaintenanceRecordById(17L);
        List<MaintenanceRecordDTO> month =
                dao.getMaintenanceRecordsByMonth(
                        Date.valueOf("2026-08-01"),
                        Date.valueOf("2026-09-01"));

        assertEquals(17L, record.getMaintenanceId());
        assertEquals(18L, month.getFirst().getMaintenanceId());
        assertEquals(2, jdbc.openCount);
        assertEquals(2, jdbc.closeCount);
        assertEquals(2, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains(
                "WHERE maintenance_id = ?"));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "inspection_date >= ? AND inspection_date < ?"));
    }

    @Test
    void customerListAndPointLookupUseInjectedConnections() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(customerRow("Acme"));
        jdbc.enqueue(customerRow("Acme"));
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        List<CustomerDTO> customers = dao.getMaintenanceCustomers(
                "manager_name", "DESC");
        CustomerDTO customer = dao.getCustomerByName("Acme");

        assertEquals("Acme", customers.getFirst().getCustomerName());
        assertEquals("23.4", customer.getVerticaVersion());
        assertEquals(2, jdbc.openCount);
        assertEquals(2, jdbc.closeCount);
        assertTrue(jdbc.statements.get(0).sql.contains(
                "d.customer_type = '정기점검 계약 고객사'"));
        assertTrue(jdbc.statements.get(0).sql.endsWith(
                "ORDER BY d.main_manager DESC"));
        assertEquals("Acme", jdbc.statements.get(1).parameters.get(1));
    }

    @Test
    void activeMaintenanceCustomerValidationUsesTheCustomerType() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(customerRow("Maintenance", "정기점검 계약 고객사"));
        jdbc.enqueue(customerRow("General", "일반 고객사"));
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        assertTrue(dao.isActiveMaintenanceCustomer("Maintenance"));
        assertFalse(dao.isActiveMaintenanceCustomer("General"));

        assertEquals(2, jdbc.openCount);
        assertEquals(2, jdbc.closeCount);
    }

    @Test
    void customerMutationsUseInjectedConnectionsAndCloseThem() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(1);
        jdbc.enqueueUpdate(1);
        jdbc.enqueueUpdate(1);
        CustomerDAO dao = new CustomerDAO(jdbc::open);
        CustomerDTO customer = customer("Acme");

        assertTrue(dao.updateCustomer(customer));
        assertTrue(dao.addCustomer(customer));
        assertTrue(dao.deleteCustomer("Acme"));

        assertEquals(3, jdbc.openCount);
        assertEquals(3, jdbc.closeCount);
        assertTrue(jdbc.statements.get(0).sql.startsWith(
                "UPDATE vertica_customer_detail SET"));
        assertTrue(jdbc.statements.get(1).sql.startsWith(
                "INSERT INTO vertica_customer_detail"));
        assertTrue(jdbc.statements.get(2).sql.contains(
                "SET is_deleted = 0"));
    }

    private static java.util.Map<String, Object> maintenanceRow(
            long id, String date) {
        return PaginationJdbcFixture.row(
                "maintenance_id", id,
                "customer_name", "Acme",
                "inspector_name", "Alice",
                "inspection_date", Date.valueOf(date),
                "vertica_version", "23.4",
                "note", null,
                "created_at", null,
                "updated_at", null);
    }

    private static java.util.Map<String, Object> customerRow(String name) {
        return customerRow(name, "정기점검 계약 고객사");
    }

    private static java.util.Map<String, Object> customerRow(
            String name, String customerType) {
        return PaginationJdbcFixture.row(
                "customer_name", name,
                "vertica_version", "23.4",
                "db_mode", "ENT",
                "os_info", "Linux",
                "node_count", "3",
                "license_info", "25TB",
                "said", "SAID",
                "main_manager", "Alice",
                "sub_manager", "Bob",
                "db_name", "archive",
                "customer_type", customerType);
    }

    private static CustomerDTO customer(String name) {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(name);
        customer.setDbName("archive");
        customer.setVerticaVersion("23.4");
        customer.setMode("ENT");
        customer.setOs("Linux");
        customer.setNodes("3");
        customer.setLicenseSize("25TB");
        customer.setManagerName("Alice");
        customer.setSubManagerName("Bob");
        customer.setSaid("SAID");
        customer.setCustomerType("정기점검 계약 고객사");
        return customer;
    }
}
