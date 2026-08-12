package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MaintenanceRecordDAOPaginationTest {
    @Test
    void customerHistoryUsesOneBoundedQueryWithStableOrder() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "maintenance_id", 41L,
                "customer_name", "Acme",
                "inspector_name", "Tester",
                "inspection_date", Date.valueOf("2026-08-10"),
                "vertica_version", "12.0",
                "note", null,
                "created_at", null,
                "updated_at", null,
                "total_count", 41));
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<MaintenanceRecordDTO> result =
                dao.getMaintenanceRecordsByCustomer("Acme", 1, 20);

        assertEquals(1, jdbc.statements.size());
        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.contains(
                "COUNT(*) OVER () AS total_count"));
        assertTrue(statement.sql.contains(
                "ORDER BY CASE WHEN inspection_date IS NULL THEN 1 ELSE 0 END, "
                        + "inspection_date DESC, maintenance_id DESC LIMIT ? OFFSET ?"));
        assertEquals("Acme", statement.parameters.get(1));
        assertEquals(20, statement.parameters.get(2));
        assertEquals(0, statement.parameters.get(3));
        assertEquals(41, result.totalCount());
        assertEquals(1, result.page());
        assertEquals(41L, result.items().getFirst().getMaintenanceId());
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void emptyOutOfRangeCustomerHistoryFallsBackToTheLastPage() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 41));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "maintenance_id", 1L,
                "customer_name", "Acme",
                "inspector_name", "Tester",
                "inspection_date", Date.valueOf("2024-01-10"),
                "vertica_version", "11.0",
                "note", null,
                "created_at", null,
                "updated_at", null,
                "total_count", 41));
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<MaintenanceRecordDTO> result =
                dao.getMaintenanceRecordsByCustomer("Acme", 999, 20);

        assertEquals(3, jdbc.statements.size());
        assertTrue(jdbc.statements.get(1).sql.startsWith(
                "SELECT COUNT(*) FROM maintenance_records WHERE customer_name = ?"));
        assertEquals(3, result.page());
        assertEquals(41, result.totalCount());
        assertEquals(40, jdbc.statements.get(2).parameters.get(3));
    }

    @Test
    void filteredCustomerHistoryUsesTheSameBoundPredicateForPageAndCount() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 21));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "maintenance_id", 21L,
                "customer_name", "Acme",
                "inspector_name", "Tester",
                "inspection_date", Date.valueOf("2026-02-10"),
                "vertica_version", "12_1",
                "note", "disk% checked",
                "created_at", null,
                "updated_at", null,
                "total_count", 21));
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());
        MaintenanceHistoryFilter filter =
                MaintenanceHistoryFilter.parse(
                        "2026", "12_", "disk%");

        PageResult<MaintenanceRecordDTO> result =
                dao.getMaintenanceRecordsByCustomer(
                        "Acme", 999, 20, filter);

        assertEquals(3, jdbc.statements.size());
        String pageSql = jdbc.statements.get(0).sql;
        String countSql = jdbc.statements.get(1).sql;
        assertTrue(pageSql.contains("inspection_date >= ?"));
        assertTrue(pageSql.contains("inspection_date < ?"));
        assertTrue(pageSql.contains(
                "vertica_version ILIKE ? ESCAPE '!'"));
        assertTrue(pageSql.contains("inspector_name ILIKE ? ESCAPE '!'"));
        assertTrue(pageSql.contains("SUBSTR(note,1,65000)"));
        assertTrue(countSql.contains("inspection_date >= ?"));
        assertTrue(countSql.contains(
                "vertica_version ILIKE ? ESCAPE '!'"));
        assertTrue(countSql.contains("SUBSTR(note,1,65000)"));
        assertFalse(pageSql.contains("disk%"));
        assertFalse(countSql.contains("disk%"));

        PaginationJdbcFixture.StatementRecord first =
                jdbc.statements.get(0);
        assertEquals("Acme", first.parameters.get(1));
        assertEquals(Date.valueOf("2026-01-01"),
                first.parameters.get(2));
        assertEquals(Date.valueOf("2027-01-01"),
                first.parameters.get(3));
        assertEquals("%12!_%", first.parameters.get(4));
        assertEquals("%disk!%%", first.parameters.get(5));
        assertEquals("%disk!%%", first.parameters.get(6));
        assertEquals("%disk!%%", first.parameters.get(7));
        assertEquals(20, first.parameters.get(8));
        assertEquals(19_960, first.parameters.get(9));

        PaginationJdbcFixture.StatementRecord count =
                jdbc.statements.get(1);
        assertEquals(first.parameters.get(1), count.parameters.get(1));
        assertEquals(first.parameters.get(2), count.parameters.get(2));
        assertEquals(first.parameters.get(3), count.parameters.get(3));
        assertEquals(first.parameters.get(4), count.parameters.get(4));
        assertEquals(first.parameters.get(5), count.parameters.get(5));
        assertEquals(first.parameters.get(6), count.parameters.get(6));
        assertEquals(first.parameters.get(7), count.parameters.get(7));

        assertEquals(2, result.page());
        assertEquals(21, result.totalCount());
        assertEquals(20, jdbc.statements.get(2).parameters.get(9));
    }

    @Test
    void ownerHistoryFailsClosedWhenStableOwnershipColumnIsMissing() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<MaintenanceRecordDTO> result =
                dao.getMaintenanceRecordsByOwner(
                        "owner-1", 999, 10);

        assertTrue(result.items().isEmpty());
        assertEquals(0, result.totalCount());
        assertTrue(jdbc.statements.isEmpty());
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void ownerHistoryUsesStableUserIdWhenMigrationIsReady() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns =
                Set.of("maintenance_records.created_by_user_id");
        jdbc.enqueue(PaginationJdbcFixture.row("count", 1));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "maintenance_id", 8L,
                "created_by_user_id", "owner-1",
                "customer_name", "Beta",
                "inspector_name", "Renamed",
                "inspection_date", Date.valueOf("2026-07-31"),
                "vertica_version", "12",
                "note", null,
                "created_at", null,
                "updated_at", null));
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<MaintenanceRecordDTO> result =
                dao.getMaintenanceRecordsByOwner(
                        " owner-1 ", 1, 10);

        assertTrue(jdbc.statements.get(0).sql.contains(
                "WHERE created_by_user_id = ?"));
        assertEquals("owner-1",
                jdbc.statements.get(0).parameters.get(1));
        assertEquals("owner-1",
                result.items().getFirst().getCreatorUserId());
    }

    @Test
    void newRecordFailsClosedWhenOwnershipColumnIsMissing() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setCreatorUserId("owner-1");

        assertTrue(!dao.addMaintenanceRecord(record));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void newRecordPersistsStableOwnerWhenMigrationIsReady() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns =
                Set.of("maintenance_records.created_by_user_id");
        jdbc.enqueueUpdate(1);
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setCreatorUserId("owner-1");
        record.setCustomerName("Acme");
        record.setInspectorName("Renamed");
        record.setInspectionDate(Date.valueOf("2026-07-31"));

        assertTrue(dao.addMaintenanceRecord(record));

        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.contains("created_by_user_id"));
        assertEquals("owner-1", statement.parameters.get(3));
    }

    @Test
    void formContextLoadsPreviousMonthAndSameMonthDuplicateInOneConnection() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "maintenance_id", 7L,
                "customer_name", "Acme",
                "inspector_name", "Alice",
                "inspection_date", Date.valueOf("2026-07-20"),
                "vertica_version", "23.4",
                "note", null,
                "created_at", null,
                "updated_at", null));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "maintenance_id", 8L,
                "customer_name", "Acme",
                "inspector_name", "Alice",
                "inspection_date", Date.valueOf("2026-08-03"),
                "vertica_version", "23.4",
                "note", null,
                "created_at", null,
                "updated_at", null));
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        MaintenanceFormHistoryContext context =
                dao.getMaintenanceFormHistoryContext(
                        "Acme", Date.valueOf("2026-08-12"), 99L);

        assertEquals(7L, context.previousRecord().getMaintenanceId());
        assertEquals(8L, context.duplicateRecord().getMaintenanceId());
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
        assertEquals(2, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains(
                "inspection_date < ?"));
        assertEquals(Date.valueOf("2026-08-01"),
                jdbc.statements.get(0).parameters.get(2));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "inspection_date >= ? AND inspection_date < ?"));
        assertEquals(Date.valueOf("2026-08-01"),
                jdbc.statements.get(1).parameters.get(2));
        assertEquals(Date.valueOf("2026-09-01"),
                jdbc.statements.get(1).parameters.get(3));
        assertEquals(99L, jdbc.statements.get(1).parameters.get(4));
    }
}
