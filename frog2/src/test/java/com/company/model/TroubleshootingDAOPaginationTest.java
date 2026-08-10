package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TroubleshootingDAOPaginationTest {
    @Test
    void searchUsesOneBoundedQueryWithWindowCount() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "id", 7,
                "title", "Connection issue",
                "customer_name", "Acme",
                "occurrence_date", null,
                "creator", "Tester",
                "create_date", null,
                "total_count", 41));
        TroubleshootingDAO dao = new TroubleshootingDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<TroubleshootingDTO> result =
                dao.getTroubleshootingPage("  needle  ", 1, 20);

        assertEquals(1, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains(
                "COUNT(*) OVER () AS total_count"));
        assertTrue(jdbc.statements.get(0).sql.contains(
                "ORDER BY CASE WHEN occurrence_date IS NULL THEN 1 ELSE 0 END, "
                        + "occurrence_date DESC, create_date DESC, "
                        + "id DESC LIMIT ? OFFSET ?"));
        assertFalse(jdbc.statements.get(0).sql.contains("NULLS LAST"));
        assertFalse(jdbc.statements.get(0).sql.contains("SUBSTR(overview"));
        assertEquals("%needle%",
                jdbc.statements.get(0).parameters.get(1));
        assertEquals("%needle%",
                jdbc.statements.get(0).parameters.get(3));
        assertEquals(20, jdbc.statements.get(0).parameters.get(4));
        assertEquals(0, jdbc.statements.get(0).parameters.get(5));
        assertEquals(1, result.page());
        assertEquals(41, result.totalCount());
        assertEquals(7, result.items().getFirst().getId());
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void contentSearchIsExplicitAndKeepsTheLegacyBodyFields() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "id", 9,
                "title", "Deep issue",
                "customer_name", "Acme",
                "occurrence_date", null,
                "creator", "Tester",
                "create_date", null,
                "total_count", 1));
        TroubleshootingDAO dao = new TroubleshootingDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<TroubleshootingDTO> result =
                dao.getTroubleshootingPage(
                        "needle", true, 1, 20);

        assertTrue(jdbc.statements.get(0).sql.contains("SUBSTR(overview"));
        assertTrue(jdbc.statements.get(0).sql.contains("SUBSTR(script_content"));
        assertTrue(jdbc.statements.get(0).sql.contains("SUBSTR(note"));
        assertEquals("%needle%",
                jdbc.statements.get(0).parameters.get(9));
        assertEquals(20, jdbc.statements.get(0).parameters.get(10));
        assertEquals(0, jdbc.statements.get(0).parameters.get(11));
        assertEquals(1, result.totalCount());
    }

    @Test
    void ownerPageUsesStableUserIdWhenTheColumnExists() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns =
                Set.of("troubleshooting.creator_user_id");
        jdbc.enqueue(PaginationJdbcFixture.row(
                "id", 8,
                "title", "Owned issue",
                "customer_name", "Beta",
                "occurrence_date", null,
                "creator", "Renamed",
                "create_date", null,
                "creator_user_id", "owner-1",
                "total_count", 11));
        TroubleshootingDAO dao = new TroubleshootingDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<TroubleshootingDTO> result =
                dao.getTroubleshootingPageByOwner(
                        "owner-1", "Old Name", 2, 10);

        assertEquals(1, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains(
                "WHERE creator_user_id = ?"));
        assertEquals("owner-1",
                jdbc.statements.get(0).parameters.get(1));
        assertEquals(10, jdbc.statements.get(0).parameters.get(2));
        assertEquals(10, jdbc.statements.get(0).parameters.get(3));
        assertEquals("owner-1",
                result.items().getFirst().getCreatorUserId());
    }

    @Test
    void emptyOutOfRangePageFallsBackToCountAndLastPage() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 41));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "id", 7,
                "title", "Connection issue",
                "customer_name", "Acme",
                "occurrence_date", null,
                "creator", "Tester",
                "create_date", null,
                "total_count", 41));
        TroubleshootingDAO dao = new TroubleshootingDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<TroubleshootingDTO> result =
                dao.getTroubleshootingPage("needle", 999, 20);

        assertEquals(3, jdbc.statements.size());
        assertTrue(jdbc.statements.get(1).sql.startsWith(
                "SELECT COUNT(*) FROM troubleshooting WHERE"));
        assertEquals(3, result.page());
        assertEquals(41, result.totalCount());
    }
}
