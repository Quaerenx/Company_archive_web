package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class VerticaEosDAOTest {
    @Test
    void resolvesSchemaOnceAndReusesTheCachedCapability() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        Timestamp first = Timestamp.valueOf("2028-12-31 00:00:00");
        Timestamp second = Timestamp.valueOf("2029-06-30 00:00:00");
        jdbc.enqueue(PaginationJdbcFixture.row(
                "table_schema", "public",
                "column_name", "vertica_version"));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "end_of_service_date", first,
                "priority", 1,
                "match_length", 9));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "end_of_service_date", second,
                "priority", 1,
                "match_length", 9));
        VerticaEosDAO dao = new VerticaEosDAO(
                jdbc::open, new VerticaEosCapabilityCache());

        Date firstResult = dao.findEosDateByVersion(" 23.4.0-15 ");
        Date secondResult = dao.findEosDateByVersion("24.1.0-0");

        assertEquals(first.getTime(), firstResult.getTime());
        assertEquals(second.getTime(), secondResult.getTime());
        assertEquals(2, jdbc.openCount);
        assertEquals(2, jdbc.closeCount);
        assertEquals(3, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.contains("v_catalog.columns"));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "\"public\".\"vertica_eos\""));
        assertTrue(jdbc.statements.get(1).sql.contains(
                "\"vertica_version\" = ?"));
        assertEquals("23.4.0-15",
                jdbc.statements.get(1).parameters.get(1));
        assertTrue(!jdbc.statements.get(2).sql.contains("v_catalog.columns"));
    }

    @Test
    void aMissingCapabilityIsNotCachedAndCanBeDiscoveredLater() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        Timestamp eos = Timestamp.valueOf("2030-01-31 00:00:00");
        jdbc.enqueue();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "table_schema", "legacy",
                "column_name", "version"));
        jdbc.enqueue(PaginationJdbcFixture.row(
                "end_of_service_date", eos,
                "priority", 1,
                "match_length", 4));
        VerticaEosDAO dao = new VerticaEosDAO(
                jdbc::open, new VerticaEosCapabilityCache());

        assertNull(dao.findEosDateByVersion("23.4"));
        Date result = dao.findEosDateByVersion("23.4");

        assertEquals(eos.getTime(), result.getTime());
        assertEquals(3, jdbc.statements.size());
        assertTrue(jdbc.statements.get(2).sql.contains(
                "\"legacy\".\"vertica_eos\""));
        assertTrue(jdbc.statements.get(2).sql.contains("\"version\" = ?"));
    }

    @Test
    void blankVersionsDoNotAcquireAConnection() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        VerticaEosDAO dao = new VerticaEosDAO(jdbc::open);

        assertNull(dao.findEosDateByVersion("  "));
        assertEquals(0, jdbc.openCount);
    }

    @Test
    void fallbackCandidatesPreserveNumericComponentBoundariesAndLeadingZeros() {
        assertEquals(List.of("112.0.1", "112.0", "112"),
                VerticaEosDAO.fallbackVersionCandidates("112.0.1"));
        assertEquals(List.of("12.01", "12"),
                VerticaEosDAO.fallbackVersionCandidates("12.01"));
        assertEquals(List.of("1.12.0", "1.12", "1"),
                VerticaEosDAO.fallbackVersionCandidates("1.12.0"));
        assertEquals(List.of("12.0.10", "12.0", "12"),
                VerticaEosDAO.fallbackVersionCandidates("12.0.10"));
        assertEquals(List.of("12.0.1-01", "12.0.1", "12.0", "12"),
                VerticaEosDAO.fallbackVersionCandidates("12.0.1-01"));
        assertEquals(List.of("12.00.1", "12.00", "12"),
                VerticaEosDAO.fallbackVersionCandidates("12.00.1"));
    }

    @Test
    void fallbackCandidatesSupportVersionPrefixesMultipleTokensAndDeduplication() {
        assertEquals(List.of("11.1.1", "11.1", "11", "12.0.1-0", "12.0.1", "12.0", "12"),
                VerticaEosDAO.fallbackVersionCandidates(
                        " v11.1.1 / Vertica v12.0.1-0 / v11.1.1 "));
        assertEquals(List.of("12.0", "12"),
                VerticaEosDAO.fallbackVersionCandidates("12.0%_[]()"));
        assertTrue(VerticaEosDAO.fallbackVersionCandidates("unknown %_ 12a0").isEmpty());
        assertTrue(VerticaEosDAO.fallbackVersionCandidates("12_0").isEmpty());
        assertTrue(VerticaEosDAO.fallbackVersionCandidates("12%0").isEmpty());
    }

    @Test
    void fallbackUsesBoundLiteralCandidatesAndPreservesExactThenLongestPriority() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        Timestamp eos = Timestamp.valueOf("2030-01-31 00:00:00");
        jdbc.enqueue(PaginationJdbcFixture.row(
                "table_schema", "public", "column_name", "vertica_version"));
        jdbc.enqueue(PaginationJdbcFixture.row("end_of_service_date", eos));
        VerticaEosDAO dao = new VerticaEosDAO(jdbc::open);

        Date result = dao.findEosDateByVersion(" Vertica v12.0.1-0 %_[]() ");

        assertEquals(eos.getTime(), result.getTime());
        assertEquals(2, jdbc.statements.size());
        var statement = jdbc.statements.get(1);
        assertTrue(statement.sql.contains("1 AS priority"));
        assertTrue(statement.sql.contains("2 AS priority"));
        assertTrue(statement.sql.contains("\"vertica_version\" IN (?, ?, ?, ?)"));
        assertTrue(statement.sql.endsWith("ORDER BY priority, match_length DESC LIMIT 1"));
        assertTrue(!statement.sql.contains("ILIKE"));
        assertTrue(!statement.sql.contains("%_[]()"));
        assertEquals("Vertica v12.0.1-0 %_[]()", statement.parameters.get(1));
        assertEquals("12.0.1-0", statement.parameters.get(2));
        assertEquals("12.0.1", statement.parameters.get(3));
        assertEquals("12.0", statement.parameters.get(4));
        assertEquals("12", statement.parameters.get(5));
        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
    }

    @Test
    void nonnumericCatalogVersionsStillUseTheExactLookupWithoutAnEmptyInClause() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        Timestamp eos = Timestamp.valueOf("2030-01-31 00:00:00");
        jdbc.enqueue(PaginationJdbcFixture.row(
                "table_schema", "legacy", "column_name", "version"));
        jdbc.enqueue(PaginationJdbcFixture.row("end_of_service_date", eos));
        VerticaEosDAO dao = new VerticaEosDAO(jdbc::open);

        Date result = dao.findEosDateByVersion(" Special catalog% ");

        assertEquals(eos.getTime(), result.getTime());
        var statement = jdbc.statements.get(1);
        assertTrue(statement.sql.contains("\"version\" = ?"));
        assertTrue(!statement.sql.contains("UNION ALL"));
        assertTrue(!statement.sql.contains(" IN ("));
        assertTrue(!statement.sql.contains("ILIKE"));
        assertEquals(1, statement.parameters.size());
        assertEquals("Special catalog%", statement.parameters.get(1));
        assertEquals(2, jdbc.statements.size());
    }
}
