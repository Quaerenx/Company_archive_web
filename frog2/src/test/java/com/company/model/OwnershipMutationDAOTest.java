package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class OwnershipMutationDAOTest {
    @Test
    void maintenanceUpdateCombinesOwnershipAndMutationInOneStatement() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "maintenance_records.created_by_user_id");
        jdbc.enqueueUpdate(1);
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setMaintenanceId(17L);
        record.setCustomerName("Acme");
        record.setInspectorName("Owner");
        record.setNote("Updated");

        assertTrue(dao.updateMaintenanceRecordForOwner(
                record, "owner-1"));

        assertEquals(1, jdbc.statements.size());
        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.startsWith(
                "UPDATE maintenance_records"));
        assertTrue(statement.sql.contains(
                "WHERE maintenance_id = ? AND created_by_user_id = ?"));
        assertEquals(17L, statement.parameters.get(6));
        assertEquals("owner-1", statement.parameters.get(7));
    }

    @Test
    void maintenanceDeleteReturnsFalseWhenOwnerPredicateMatchesNothing() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "maintenance_records.created_by_user_id");
        jdbc.enqueueUpdate(0);
        MaintenanceRecordDAO dao = new MaintenanceRecordDAO(
                jdbc::open, new SchemaCapabilityCache());

        assertFalse(dao.deleteMaintenanceRecordForOwner(
                17L, "different-owner"));

        assertEquals(1, jdbc.statements.size());
        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.contains(
                "WHERE maintenance_id = ? AND created_by_user_id = ?"));
        assertEquals(17L, statement.parameters.get(1));
        assertEquals("different-owner", statement.parameters.get(2));
    }

    @Test
    void meetingUpdateCombinesOwnershipAndMutationInOneStatement() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(1);
        MeetingRecordDAO dao = new MeetingRecordDAO(jdbc::open);
        MeetingRecordDTO meeting = new MeetingRecordDTO();
        meeting.setMeetingId(7L);
        meeting.setTitle("Updated");

        assertTrue(dao.updateMeetingRecordForAuthor(
                meeting, "author-1"));

        assertEquals(1, jdbc.statements.size());
        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.startsWith(
                "UPDATE meeting_records"));
        assertTrue(statement.sql.contains(
                "WHERE meeting_id = ? AND author_id = ?"));
        assertEquals(7L, statement.parameters.get(5));
        assertEquals("author-1", statement.parameters.get(6));
    }

    @Test
    void meetingDeleteReturnsFalseWhenAtomicOwnerPredicateMatchesNothing() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(0);
        MeetingRecordDAO dao = new MeetingRecordDAO(jdbc::open);

        assertFalse(dao.deleteMeetingRecordForAuthor(
                7L, "different-author"));

        assertEquals(1, jdbc.statements.size());
        assertTrue(jdbc.statements.getFirst().sql.contains(
                "WHERE meeting_id = ? AND author_id = ?"));
    }

    @Test
    void commentMutationsCombineOwnershipAndMutation() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueueUpdate(1);
        jdbc.enqueueUpdate(1);
        MeetingCommentDAO dao = new MeetingCommentDAO(jdbc::open);
        MeetingCommentDTO comment = new MeetingCommentDTO();
        comment.setCommentId(11L);
        comment.setContent("Updated");

        assertTrue(dao.updateCommentForAuthor(
                comment, "author-1"));
        assertTrue(dao.deleteCommentForAuthor(
                11L, "author-1"));

        assertEquals(2, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains(
                "WHERE comment_id = ? AND author_id = ?"));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "WHERE comment_id = ? AND author_id = ?"));
    }
}
