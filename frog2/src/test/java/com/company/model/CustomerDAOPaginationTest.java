package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CustomerDAOPaginationTest {
    @Test
    void globalSearchUsesOneBoundedLiteralQuery() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "customer_name", "Acme",
                "vertica_version", "12.0.4-29",
                "db_mode", "ENT",
                "os_info", "Linux",
                "node_count", "3",
                "license_info", "10TB",
                "said", "SAID",
                "main_manager", "Tester",
                "sub_manager", null,
                "db_name", "db",
                "customer_type", "정기점검 계약 고객사"));
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        var results = dao.searchCustomers(" need%_!le ", 5);

        assertEquals(1, jdbc.statements.size());
        assertTrue(jdbc.statements.getFirst().sql.contains(
                "customer_name AS VARCHAR(65000)) ILIKE ? ESCAPE '!'"));
        assertEquals(
                "%need!%!_!!le%",
                jdbc.statements.getFirst().parameters.get(1));
        assertEquals(
                "%need!%!_!!le%",
                jdbc.statements.getFirst().parameters.get(6));
        assertEquals(5, jdbc.statements.getFirst().parameters.get(7));
        assertEquals("Acme", results.getFirst().getCustomerName());
    }

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
                "REGEXP_SUBSTR(CAST(d.node_count AS VARCHAR(65000))"));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "AS NUMERIC) DESC, d.customer_name ASC LIMIT ? OFFSET ?"));
        assertEquals("%acme%",
                jdbc.statements.get(1).parameters.get(1));
        assertEquals("%acme%",
                jdbc.statements.get(1).parameters.get(6));
        assertEquals(50, jdbc.statements.get(1).parameters.get(7));
        assertEquals(0, jdbc.statements.get(1).parameters.get(8));
        assertTrue(!jdbc.statements.get(1).sql.contains(
                "ORDER BY d.node_count DESC"));
        assertEquals(1, customerPage.result().page());
        assertEquals(51, customerPage.result().totalCount());
        assertEquals(120, customerPage.counts().total());
        assertEquals(70, customerPage.counts().maintenance());
        assertEquals("Acme",
                customerPage.result().items().getFirst().getCustomerName());
    }

    @Test
    void licenseAndVersionSortsUseSemanticComponents() {
        PaginationJdbcFixture licenseJdbc = fixtureWithOneCustomer();
        CustomerDAO licenseDao = new CustomerDAO(licenseJdbc::open);

        licenseDao.getCustomerPage(
                "license_size", "ASC", "all", null, 1, 50);

        String licenseSql = licenseJdbc.statements.get(1).sql;
        assertTrue(licenseSql.contains(
                "REGEXP_SUBSTR(CAST(d.license_info AS VARCHAR(65000)), "
                        + "'[[:alpha:]]+')"));
        assertTrue(licenseSql.contains("AS VARCHAR(64)))"));
        assertTrue(licenseSql.contains(
                "REGEXP_SUBSTR(CAST(d.license_info AS VARCHAR(65000)), "
                        + "'[0-9]+([.][0-9]+)?')"));

        PaginationJdbcFixture versionJdbc = fixtureWithOneCustomer();
        CustomerDAO versionDao = new CustomerDAO(versionJdbc::open);

        versionDao.getCustomerPage(
                "vertica_version", "DESC", "all", null, 1, 50);

        String versionSql = versionJdbc.statements.get(1).sql;
        assertTrue(versionSql.contains(
                "SPLIT_PART(SPLIT_PART(CAST(d.vertica_version "
                        + "AS VARCHAR(65000)), '.', 1), '-', 1)"));
        assertTrue(versionSql.contains("AS INTEGER) DESC"));
        assertTrue(!versionSql.contains(
                "ORDER BY d.vertica_version DESC"));
    }

    private static PaginationJdbcFixture fixtureWithOneCustomer() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "total_count", 1,
                "maintenance_count", 1));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "customer_name", "Acme",
                "vertica_version", "12.0.1-0",
                "db_mode", "ENT",
                "os_info", "Linux",
                "node_count", "3",
                "license_info", "10TB",
                "said", "SAID",
                "main_manager", "Tester",
                "sub_manager", null,
                "db_name", "db",
                "customer_type", "정기점검 계약 고객사",
                "result_count", 1));
        return jdbc;
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
        assertEquals(100, jdbc.statements.get(1).parameters.get(8));
        assertEquals(50, jdbc.statements.get(3).parameters.get(8));
        assertEquals(2, customerPage.result().page());
        assertEquals(51, customerPage.result().totalCount());
    }

    @Test
    void deletedRowsDuringCorrectionReturnEmptyFirstPage() {
        PaginationJdbcFixture jdbc = customerDeletionFixture();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 0));

        CustomerPage result = new CustomerDAO(jdbc::open).getCustomerPage(
                "customer_name", "ASC", "maintenance", "needle", 999, 20);

        assertEquals(5, jdbc.statements.size());
        assertEquals(1, result.result().page());
        assertEquals(0, result.result().totalCount());
        assertTrue(result.result().items().isEmpty());
        assertEquals(100, result.counts().total());
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void furtherDeletesDuringCorrectionRecountAndUseRemainingPage() {
        PaginationJdbcFixture jdbc = customerDeletionFixture();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 21));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "customer_name", "Acme", "result_count", 21));

        CustomerPage result = new CustomerDAO(jdbc::open).getCustomerPage(
                "customer_name", "ASC", "maintenance", "needle", 999, 20);

        assertEquals(6, jdbc.statements.size());
        assertEquals(2, result.result().page());
        assertEquals(21, result.result().totalCount());
        assertEquals("Acme", result.result().items().getFirst().getCustomerName());
        assertEquals(40, jdbc.statements.get(3).parameters.get(8));
        assertEquals(20, jdbc.statements.get(5).parameters.get(8));
        assertEquals(jdbc.statements.get(1).sql, jdbc.statements.get(5).sql);
        assertEquals(jdbc.statements.get(2).sql, jdbc.statements.get(4).sql);
        for (int parameter = 1; parameter <= 6; parameter++) {
            assertEquals(jdbc.statements.get(1).parameters.get(parameter),
                    jdbc.statements.get(5).parameters.get(parameter));
            assertEquals(jdbc.statements.get(2).parameters.get(parameter),
                    jdbc.statements.get(4).parameters.get(parameter));
        }
    }

    @Test
    void repeatedDeletesStopAfterOneRecount() {
        PaginationJdbcFixture jdbc = customerDeletionFixture();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 21));
        jdbc.enqueue();

        CustomerPage result = new CustomerDAO(jdbc::open).getCustomerPage(
                "", "ASC", "maintenance", "needle", 999, 20);

        assertEquals(6, jdbc.statements.size());
        assertEquals(1, result.result().page());
        assertEquals(0, result.result().totalCount());
        assertTrue(result.result().items().isEmpty());
        assertTrue(jdbc.queryResults.isEmpty());
    }

    private static PaginationJdbcFixture customerDeletionFixture() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "total_count", 100, "maintenance_count", 100));
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 41));
        jdbc.enqueue();
        return jdbc;
    }
}
