package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
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
}
