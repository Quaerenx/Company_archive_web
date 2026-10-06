package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DatabaseIntegrityDAOTest {
    private static final String ID = "2d90df87-baca-4ae5-a0c0-d2d135696eb2";
    private static final Set<String> IDENTITY = Set.of(
            "customer_identity.customer_id", "customer_identity.customer_name");
    private static final Set<String> MEETING = Set.of("meeting_records.deleted_at", "meeting_records.deleted_by");

    @Test
    void maintenanceStoresTheStableRelationshipAndNumericUnitsWithoutChangingSnapshots() {
        PaginationJdbcFixture jdbc = linked("maintenance_records");
        Set<String> columns = new HashSet<>(jdbc.availableColumns);
        columns.addAll(Set.of("maintenance_records.created_by_user_id", "maintenance_records.license_size_gb",
                "maintenance_records.license_usage_size", "maintenance_records.license_usage_pct"));
        MaintenanceLicenseValues.COLUMNS.forEach(column -> columns.add("maintenance_records." + column));
        jdbc.availableColumns = columns;
        jdbc.enqueue(PaginationJdbcFixture.row("customer_id", ID));
        jdbc.enqueueUpdate(1);
        MaintenanceRecordDTO record = maintenance();
        record.setLicenseSizeGb("2048GB");
        record.setLicenseUsageSize("0.5 TB");
        record.setLicenseUsagePct("25 %");

        assertTrue(new MaintenanceRecordDAO(jdbc::open, new SchemaCapabilityCache()).addMaintenanceRecord(record));

        var insert = jdbc.statements.get(1);
        assertEquals(ID, insert.parameters.get(10));
        assertEquals("2048GB", insert.parameters.get(7));
        assertEquals(new BigDecimal("2.000000"), insert.parameters.get(11));
        assertEquals(new BigDecimal("0.5"), insert.parameters.get(12));
        assertEquals(new BigDecimal("25"), insert.parameters.get(13));
        assertEquals(1, jdbc.commitCount);
        assertEquals(0, jdbc.rollbackCount);
    }

    @Test
    void monthlyResponsesAndTroubleshootingAcceptHistoricalIdentityOnlyCustomers() {
        PaginationJdbcFixture monthly = linked("monthly_customer_response");
        monthly.availableColumns = with(monthly.availableColumns, "monthly_customer_response.created_by_user_id");
        monthly.enqueue(PaginationJdbcFixture.row("customer_id", ID));
        monthly.enqueueUpdate(1);
        MonthlyCustomerResponseDTO response = new MonthlyCustomerResponseDTO();
        response.setUserId("owner");
        response.setCustomerName("Historical customer");
        response.setResponseDate(Date.valueOf("2026-10-06"));
        response.setReason("Support");
        assertTrue(new MonthlyCustomerResponseDAO(monthly::open).addResponse(response));
        assertEquals(ID, monthly.statements.get(1).parameters.get(8));

        PaginationJdbcFixture trouble = linked("troubleshooting");
        trouble.availableColumns = with(trouble.availableColumns, "troubleshooting.creator_user_id");
        trouble.enqueue(PaginationJdbcFixture.row("customer_id", ID));
        trouble.enqueueUpdate(1);
        TroubleshootingDTO record = new TroubleshootingDTO();
        record.setCustomerName("Historical customer");
        record.setCreatorUserId("owner");
        assertTrue(new TroubleshootingDAO(trouble::open, new SchemaCapabilityCache()).addTroubleshooting(record));
        assertEquals(ID, trouble.statements.get(1).parameters.get(17));
    }

    @Test
    void failedChildWriteRollsBackTheNewIdentityAsWell() {
        PaginationJdbcFixture jdbc = linked("maintenance_records");
        jdbc.availableColumns = with(jdbc.availableColumns, "maintenance_records.created_by_user_id");
        jdbc.enqueue();
        jdbc.enqueueUpdate(1);
        jdbc.enqueue(PaginationJdbcFixture.row("customer_id", ID));
        jdbc.enqueueUpdate(0);

        assertFalse(new MaintenanceRecordDAO(jdbc::open, new SchemaCapabilityCache()).addMaintenanceRecord(maintenance()));
        assertEquals(0, jdbc.commitCount);
        assertEquals(1, jdbc.rollbackCount);
        assertEquals(java.util.List.of(false, true), jdbc.autoCommitValues);
    }

    @Test
    void customerScopedLookupsAndMutationsResolveTheIdInsteadOfMatchingNameSnapshots() {
        PaginationJdbcFixture jdbc = linked("maintenance_records");
        jdbc.availableColumns = with(jdbc.availableColumns, "maintenance_records.created_by_user_id");
        jdbc.enqueue();
        jdbc.enqueueUpdate(0);
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(jdbc::open, new SchemaCapabilityCache());
        dao.getMaintenanceRecordsByCustomer("Renamed customer", 1, 20);
        assertFalse(dao.deleteMaintenanceRecordForCustomer(4L, "Renamed customer"));
        for (var statement : jdbc.statements) {
            assertTrue(statement.sql.contains("customer_id = (SELECT customer_id FROM customer_identity"));
        }
        PaginationJdbcFixture trouble = linked("troubleshooting");
        trouble.availableColumns = with(trouble.availableColumns, "troubleshooting.creator_user_id");
        trouble.enqueueUpdate(0);
        assertFalse(new TroubleshootingDAO(trouble::open, new SchemaCapabilityCache())
                .deleteTroubleshootingForCustomer(4, "Renamed customer"));
        assertTrue(trouble.statements.getFirst().sql.contains(
                "WHERE id = ? AND customer_id = (SELECT customer_id FROM customer_identity"));
    }

    @Test
    void aReferenceColumnWithoutItsIdentityTableFailsBeforeMutation() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of("maintenance_records.customer_id", "maintenance_records.created_by_user_id");
        assertThrows(DataAccessException.class, () -> new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache()).addMaintenanceRecord(maintenance()));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void partialNumericSchemaFailsBeforeMutation() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of("maintenance_records.license_used_tb", "maintenance_records.created_by_user_id");
        assertThrows(DataAccessException.class, () -> new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache()).addMaintenanceRecord(maintenance()));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void licenseConversionPreservesUnsupportedRawValuesAndAcceptsOverOneHundredPercent() {
        MaintenanceRecordDTO record = maintenance();
        record.setLicenseSizeGb("Unlimited");
        record.setLicenseUsageSize("-1GB");
        record.setLicenseUsagePct("120.125 %");
        MaintenanceLicenseValues values = MaintenanceLicenseValues.fromLegacy(record);
        assertNull(values.capacityTb());
        assertNull(values.usedTb());
        assertEquals(new BigDecimal("120.125"), values.percentage());
        assertEquals("Unlimited", record.getLicenseSizeGb());
        for (String invalid : java.util.List.of("NaN", "Infinity", "1000001", "1e2", "1.1234567", "-1", "999999999999999999999999999")) {
            record.setLicenseUsagePct(invalid);
            assertNull(MaintenanceLicenseValues.fromLegacy(record).percentage(), invalid);
        }
    }

    @Test
    void meetingDeletionOnlyMarksTheAuthorizedParentAndPreservesAllComments() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = MEETING;
        jdbc.enqueueUpdate(1);
        assertTrue(new MeetingRecordDAO(jdbc::open).deleteMeetingRecordForAuthor(7L, " owner "));
        var mutation = jdbc.statements.getFirst();
        assertTrue(mutation.sql.startsWith("UPDATE meeting_records SET deleted_at"));
        assertTrue(mutation.sql.contains("author_id = ? AND deleted_at IS NULL"));
        assertEquals("owner", mutation.parameters.get(1));
        assertEquals(7L, mutation.parameters.get(2));
        assertTrue(jdbc.statements.stream().noneMatch(statement -> statement.sql.startsWith("DELETE")));
    }

    @Test
    void legacyMeetingSchemaNeverFallsBackToPermanentDeletion() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        assertThrows(DataAccessException.class, () -> new MeetingRecordDAO(jdbc::open).deleteMeetingRecordForAuthor(7L, "owner"));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void allMeetingReadsAndUpdatesExcludeDeletedParents() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = MEETING;
        jdbc.enqueue();
        jdbc.enqueue();
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 0));
        jdbc.enqueueUpdate(0);
        MeetingRecordDAO dao = new MeetingRecordDAO(jdbc::open);
        assertNull(dao.getMeetingRecord(7L));
        assertTrue(dao.getMeetingPage(1).items().isEmpty());
        assertTrue(dao.searchMeetingRecords("deleted", 5).isEmpty());
        assertEquals(0, dao.getTotalCount());
        MeetingRecordDTO record = new MeetingRecordDTO();
        record.setMeetingId(7L);
        assertFalse(dao.updateMeetingRecordForAuthor(record, "owner"));
        assertTrue(jdbc.statements.stream().allMatch(statement -> statement.sql.contains("deleted_at IS NULL")));
    }

    @Test
    void commentInsertRejectsMissingOrDeletedParentsWhileHoldingTheSharedLock() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = MEETING;
        jdbc.enqueueUpdate(0);
        MeetingCommentDTO comment = new MeetingCommentDTO();
        comment.setMeetingId(7L);
        comment.setAuthorId("owner");
        comment.setContent("Preserved");
        assertFalse(new MeetingCommentDAO(jdbc::open).addComment(comment));
        assertEquals("LOCK TABLE meeting_records IN SHARE MODE", jdbc.statements.getFirst().sql);
        assertTrue(jdbc.statements.get(1).sql.contains("FROM meeting_records WHERE meeting_id = ? AND deleted_at IS NULL"));
        assertEquals(0, jdbc.commitCount);
        assertEquals(1, jdbc.rollbackCount);
    }

    private static PaginationJdbcFixture linked(String table) {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = with(IDENTITY, table + ".customer_id");
        return jdbc;
    }

    private static Set<String> with(Set<String> columns, String column) {
        Set<String> result = new HashSet<>(columns);
        result.add(column);
        return result;
    }

    private static MaintenanceRecordDTO maintenance() {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setCustomerName("Acme");
        record.setCreatorUserId("owner");
        record.setInspectorName("Owner");
        record.setInspectionDate(Date.valueOf("2026-10-06"));
        return record;
    }
}
