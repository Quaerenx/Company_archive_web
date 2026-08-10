package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;

class MeetingCommentDAOPaginationTest {
    @Test
    void loadsNewestBoundedPageAndReturnsStableOlderCursor() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(comment(9), comment(8), comment(7));
        MeetingCommentDAO dao = new MeetingCommentDAO(jdbc::open);

        MeetingCommentPage page = dao.getCommentPage(3L, 10L, 2);

        assertEquals(List.of(8L, 9L), page.getComments().stream()
                .map(MeetingCommentDTO::getCommentId)
                .toList());
        assertTrue(page.isHasOlder());
        assertEquals(8L, page.getNextBeforeCommentId());
        PaginationJdbcFixture.StatementRecord query = jdbc.statements.getFirst();
        assertTrue(query.sql.contains("WHERE meeting_id = ? AND comment_id < ?"));
        assertTrue(query.sql.contains("ORDER BY comment_id DESC LIMIT ?"));
        assertEquals(3L, query.parameters.get(1));
        assertEquals(10L, query.parameters.get(2));
        assertEquals(3, query.parameters.get(3));
    }

    @Test
    void finalPageHasNoOlderCursor() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(comment(2), comment(1));
        MeetingCommentDAO dao = new MeetingCommentDAO(jdbc::open);

        MeetingCommentPage page = dao.getCommentPage(3L, null, 2);

        assertEquals(List.of(1L, 2L), page.getComments().stream()
                .map(MeetingCommentDTO::getCommentId)
                .toList());
        assertFalse(page.isHasOlder());
        assertEquals(null, page.getNextBeforeCommentId());
        assertFalse(jdbc.statements.getFirst().sql.contains("comment_id < ?"));
    }

    private static java.util.Map<String, Object> comment(long id) {
        Timestamp timestamp = Timestamp.valueOf("2026-08-10 10:00:00");
        return PaginationJdbcFixture.row(
                "comment_id", id,
                "meeting_id", 3L,
                "content", "Comment " + id,
                "author_id", "user-1",
                "author_name", "Tester",
                "created_at", timestamp,
                "updated_at", timestamp);
    }
}
