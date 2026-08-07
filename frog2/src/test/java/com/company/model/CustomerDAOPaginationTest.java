package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CustomerDAOPaginationTest {
    @Test
    void filterSearchAndSortUseTwoQueriesWithStableOrdering() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "total_count", 120,
                "maintenance_count", 70));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "customer_name", "Acme",
                "vertica_version", "12",
                "db_mode", "ENT",
                "os_info", "Linux",
                "node_count", "3",
                "license_info", "10TB",
                "said", "SAID",
                "main_manager", "Tester",
                "sub_manager", null,
                "db_name", "db",
                "customer_type", "정기점검 계약 고객사",
                "result_count", 51));
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        CustomerPage customerPage = dao.getCustomerPage(
                "nodes",
                "DESC",
                "maintenance",
                "  acme  ",
                1,
                50);

        assertEquals(2, jdbc.statements.size());
        assertTrue(!jdbc.statements.get(0).sql.contains(
                "result_count"));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "COUNT(*) OVER () AS result_count"));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "ORDER BY d.node_count DESC, d.customer_name ASC "
                        + "LIMIT ? OFFSET ?"));
        assertEquals("%acme%",
                jdbc.statements.get(1).parameters.get(1));
        assertEquals("%acme%",
                jdbc.statements.get(1).parameters.get(8));
        assertEquals(50, jdbc.statements.get(1).parameters.get(9));
        assertEquals(0, jdbc.statements.get(1).parameters.get(10));
        assertEquals(1, customerPage.result().page());
        assertEquals(51, customerPage.result().totalCount());
        assertEquals(120, customerPage.counts().total());
        assertEquals(70, customerPage.counts().maintenance());
        assertEquals("Acme",
                customerPage.result().items().getFirst().getCustomerName());
    }

    @Test
    void outOfRangeFilteredPageIsCountedOnlyAfterTheEmptyAttempt() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "total_count", 120,
                "maintenance_count", 70));
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 51));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "customer_name", "Acme",
                "vertica_version", "12",
                "db_mode", "ENT",
                "os_info", "Linux",
                "node_count", "3",
                "license_info", "10TB",
                "said", "SAID",
                "main_manager", "Tester",
                "sub_manager", null,
                "db_name", "db",
                "customer_type", "정기점검 계약 고객사",
                "result_count", 51));
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        CustomerPage customerPage = dao.getCustomerPage(
                "customer_name",
                "ASC",
                "maintenance",
                "acme",
                999,
                50);

        assertEquals(4, jdbc.statements.size());
        assertTrue(jdbc.statements.get(2).sql.startsWith(
                "SELECT COUNT(*) FROM vertica_customer_detail"));
        assertEquals(100, jdbc.statements.get(1).parameters.get(10));
        assertEquals(50, jdbc.statements.get(3).parameters.get(10));
        assertEquals(2, customerPage.result().page());
        assertEquals(51, customerPage.result().totalCount());
    }
}
