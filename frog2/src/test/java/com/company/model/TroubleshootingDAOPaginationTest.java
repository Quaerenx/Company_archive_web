package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import com.company.performance.RequestPerformanceContext;
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

        RequestPerformanceContext.begin();
        PageResult<TroubleshootingDTO> result =
                dao.getTroubleshootingPage("  need%_!le  ", 1, 20);
        RequestPerformanceContext.Snapshot performance =
                RequestPerformanceContext.finish();

        assertEquals(1, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains(
                "COUNT(*) OVER () AS total_count"));
        assertTrue(jdbc.statements.get(0).sql.contains(
                "ORDER BY CASE WHEN occurrence_date IS NULL THEN 1 ELSE 0 END, "
                        + "occurrence_date DESC, create_date DESC, "
                        + "id DESC LIMIT ? OFFSET ?"));
        assertFalse(jdbc.statements.get(0).sql.contains("NULLS LAST"));
        assertFalse(jdbc.statements.get(0).sql.contains("SUBSTR(overview"));
        assertTrue(jdbc.statements.get(0).sql.contains(
                "title ILIKE ? ESCAPE '!'"));
        assertEquals("%need!%!_!!le%",
                jdbc.statements.get(0).parameters.get(1));
        assertEquals("%need!%!_!!le%",
                jdbc.statements.get(0).parameters.get(3));
        assertEquals(20, jdbc.statements.get(0).parameters.get(4));
        assertEquals(0, jdbc.statements.get(0).parameters.get(5));
        assertEquals(1, result.page());
        assertEquals(41, result.totalCount());
        assertEquals(7, result.items().getFirst().getId());
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
        assertEquals(
                RequestPerformanceContext.Operation.TROUBLESHOOTING_SUMMARY_SEARCH,
                performance.operation());
    }

    @Test
    void contentSearchIsExplicitAndSearchesCompleteLongVarcharFields() {
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

        RequestPerformanceContext.begin();
        PageResult<TroubleshootingDTO> result =
                dao.getTroubleshootingPage(
                        "need [le].*#", true, 1, 20);
        RequestPerformanceContext.Snapshot performance =
                RequestPerformanceContext.finish();

        assertFalse(jdbc.statements.get(0).sql.contains("SUBSTR("));
        assertFalse(jdbc.statements.get(0).sql.contains("VARCHAR(65000)"));
        assertTrue(jdbc.statements.get(0).sql.contains(
                "REGEXP_ILIKE(overview, ?)"));
        assertTrue(jdbc.statements.get(0).sql.contains(
                "REGEXP_ILIKE(script_content, ?)"));
        assertTrue(jdbc.statements.get(0).sql.contains(
                "REGEXP_ILIKE(note, ?)"));
        assertEquals("%need [le].*#%",
                jdbc.statements.get(0).parameters.get(1));
        assertEquals("need\\x{20}\\[le\\]\\.\\*\\#",
                jdbc.statements.get(0).parameters.get(4));
        assertEquals("need\\x{20}\\[le\\]\\.\\*\\#",
                jdbc.statements.get(0).parameters.get(9));
        assertEquals(20, jdbc.statements.get(0).parameters.get(10));
        assertEquals(0, jdbc.statements.get(0).parameters.get(11));
        assertEquals(1, result.totalCount());
        assertEquals(
                RequestPerformanceContext.Operation.TROUBLESHOOTING_CONTENT_SEARCH,
                performance.operation());
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
                        "owner-1", 2, 10);

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
    void ownerPageFailsClosedWhenStableOwnershipColumnIsMissing() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        TroubleshootingDAO dao = new TroubleshootingDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<TroubleshootingDTO> result =
                dao.getTroubleshootingPageByOwner(
                        "owner-1", 1, 10);

        assertTrue(result.items().isEmpty());
        assertEquals(0, result.totalCount());
        assertTrue(jdbc.statements.isEmpty());
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

    @Test
    void contentSearchUsesTheSamePredicateAndBindingsForPageAndCount() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row("count", 0));
        TroubleshootingDAO dao = new TroubleshootingDAO(
                jdbc::open, new SchemaCapabilityCache());

        PageResult<TroubleshootingDTO> result =
                dao.getTroubleshootingPage(
                        "disk_100%", true, 999, 20);

        assertEquals(2, jdbc.statements.size());
        String pageSql = jdbc.statements.get(0).sql;
        String countSql = jdbc.statements.get(1).sql;
        String pagePredicate = pageSql.substring(
                pageSql.indexOf(" WHERE ") + " WHERE ".length(),
                pageSql.indexOf(" ORDER BY "));
        String countPredicate = countSql.substring(
                countSql.indexOf(" WHERE ") + " WHERE ".length());
        assertEquals(pagePredicate, countPredicate);
        for (int parameter = 1; parameter <= 9; parameter++) {
            assertEquals(
                    jdbc.statements.get(0).parameters.get(parameter),
                    jdbc.statements.get(1).parameters.get(parameter));
        }
        assertEquals("%disk!_100!%%",
                jdbc.statements.get(0).parameters.get(1));
        assertEquals("disk_100%",
                jdbc.statements.get(0).parameters.get(4));
        assertEquals(0, result.totalCount());
        assertEquals(1, result.page());
    }
}
