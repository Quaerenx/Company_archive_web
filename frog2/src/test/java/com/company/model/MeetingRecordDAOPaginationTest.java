package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class MeetingRecordDAOPaginationTest {
    @Test
    void aNormalPageUsesOneWindowCountQuery() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(meetingRow(17L, 41));
        MeetingRecordDAO dao = new MeetingRecordDAO(jdbc::open);

        PageResult<MeetingRecordDTO> page = dao.getMeetingPage(2);

        assertEquals(1, jdbc.statements.size());
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
        assertTrue(jdbc.statements.get(0).sql.contains(
                "COUNT(*) OVER () AS total_count"));
        assertTrue(jdbc.statements.get(0).sql.contains(
                "ORDER BY meeting_datetime DESC, meeting_id DESC"));
        assertEquals(20, jdbc.statements.get(0).parameters.get(1));
        assertEquals(20, jdbc.statements.get(0).parameters.get(2));
        assertEquals(2, page.page());
        assertEquals(3, page.totalPages());
        assertEquals(41, page.totalCount());
        assertEquals(17L, page.items().getFirst().getMeetingId());
    }

    @Test
    void anOutOfRangePageFallsBackToTheLastAvailablePage() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 41));
        jdbc.enqueue(meetingRow(17L, 41));
        MeetingRecordDAO dao = new MeetingRecordDAO(jdbc::open);

        PageResult<MeetingRecordDTO> page = dao.getMeetingPage(999);

        assertEquals(3, jdbc.statements.size());
        assertTrue(jdbc.statements.get(1).sql.startsWith(
                "SELECT COUNT(*) FROM meeting_records"));
        assertEquals(19_960, jdbc.statements.get(0).parameters.get(2));
        assertEquals(40, jdbc.statements.get(2).parameters.get(2));
        assertEquals(3, page.page());
        assertEquals(41, page.totalCount());
    }

    @Test
    void anEmptyFirstPageDoesNotRunASeparateCount() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        MeetingRecordDAO dao = new MeetingRecordDAO(jdbc::open);

        PageResult<MeetingRecordDTO> page = dao.getMeetingPage(1);

        assertEquals(1, jdbc.statements.size());
        assertEquals(0, page.totalCount());
        assertEquals(1, page.page());
        assertTrue(page.items().isEmpty());
    }

    @Test
    void concurrentDeletesDuringPageCorrectionReturnAnEmptyFirstPage() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 41));
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 0));
        MeetingRecordDAO dao = new MeetingRecordDAO(jdbc::open);

        PageResult<MeetingRecordDTO> page = dao.getMeetingPage(999);

        assertEquals(4, jdbc.statements.size());
        assertEquals(1, page.page());
        assertEquals(0, page.totalCount());
        assertTrue(page.items().isEmpty());
    }

    private static java.util.Map<String, Object> meetingRow(
            long meetingId, int totalCount) {
        return PaginationJdbcFixture.row(
                "meeting_id", meetingId,
                "title", "Weekly meeting",
                "meeting_type", "weekly",
                "author_name", "Alice",
                "meeting_datetime", Timestamp.valueOf(
                        "2026-08-22 09:30:00"),
                "total_count", totalCount);
    }
}
