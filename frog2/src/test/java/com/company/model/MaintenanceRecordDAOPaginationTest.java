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
}
