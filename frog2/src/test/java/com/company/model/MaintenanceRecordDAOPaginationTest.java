package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MaintenanceRecordDAOPaginationTest {
    @Test
    void inspectorHistoryUsesOneCountAndOneBoundedStableItemQuery() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 21));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "maintenance_id", 7L,
                "customer_name", "Acme",
                "inspector_name", "Tester",
                "inspection_date", Date.valueOf("2026-07-30"),
                "vertica_version", "12",
                "note", null,
                "created_at", null,
                "updated_at", null));
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<MaintenanceRecordDTO> result =
                dao.getMaintenanceRecordsByOwner(
                        "owner-1", " Tester ", 999, 10);

        assertEquals(2, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains(
                "WHERE inspector_name = ?"));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "ORDER BY CASE WHEN inspection_date IS NULL THEN 1 ELSE 0 END, "
                        + "inspection_date DESC, maintenance_id DESC "
                        + "LIMIT ? OFFSET ?"));
        assertFalse(jdbc.statements.get(1).sql.contains("NULLS LAST"));
        assertEquals("Tester",
                jdbc.statements.get(0).parameters.get(1));
        assertEquals(10, jdbc.statements.get(1).parameters.get(2));
        assertEquals(20, jdbc.statements.get(1).parameters.get(3));
        assertEquals(3, result.page());
        assertEquals(21, result.totalCount());
        assertEquals(7L,
                result.items().getFirst().getMaintenanceId());
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
                        " owner-1 ", "Old Name", 1, 10);

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
