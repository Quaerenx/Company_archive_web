package com.company.model;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SchemaCapabilityCacheTest {
    @Test
    void cachesExistingColumnAcrossCaseVariants() {
        AtomicInteger metadataQueries = new AtomicInteger();
        Connection connection = connection(metadataQueries, true);
        SchemaCapabilityCache cache = new SchemaCapabilityCache();

        assertTrue(cache.columnExists(connection, "company_users", "department"));
        assertTrue(cache.columnExists(connection, "COMPANY_USERS", "DEPARTMENT"));

        assertEquals(1, metadataQueries.get());
    }

    @Test
    void cachesMissingColumnAfterLowerAndUpperCaseLookup() {
        AtomicInteger metadataQueries = new AtomicInteger();
        Connection connection = connection(metadataQueries, false, false);
        SchemaCapabilityCache cache = new SchemaCapabilityCache();

        assertFalse(cache.columnExists(connection, "maintenance_records", "license_size_gb"));
        assertFalse(cache.columnExists(connection, "maintenance_records", "license_size_gb"));

        assertEquals(2, metadataQueries.get());
    }

    @Test
    void publishedSnapshotCachesPresentAndAbsentColumnsWithoutMetadata() {
        AtomicInteger queries = new AtomicInteger();
        SchemaCapabilityCache inspected = new SchemaCapabilityCache();
        Connection connection = connection(queries, true, false, false);
        assertTrue(inspected.columnExists(connection, "sample", "present"));
        assertFalse(inspected.columnExists(connection, "sample", "absent"));
        SchemaCapabilityCache warmed = new SchemaCapabilityCache();
        warmed.replaceWith(inspected);

        assertTrue(warmed.columnExists(connection, "SAMPLE", "PRESENT"));
        assertFalse(warmed.columnExists(connection, "sample", "absent"));
        assertEquals(3, queries.get());
    }

    @Test
    void refreshedSnapshotReplacesCachedAbsenceAndIsIndependentOfSource() {
        AtomicInteger queries = new AtomicInteger();
        SchemaCapabilityCache warmed = new SchemaCapabilityCache();
        assertFalse(warmed.columnExists(connection(queries, false, false), "sample", "new_column"));
        SchemaCapabilityCache refreshed = new SchemaCapabilityCache();
        assertTrue(refreshed.columnExists(connection(queries, true), "sample", "new_column"));

        warmed.replaceWith(refreshed);
        refreshed.replaceWith(new SchemaCapabilityCache());

        assertTrue(warmed.columnExists(connection(queries, false, false), "sample", "new_column"));
        assertEquals(3, queries.get());
    }

    @Test
    void metadataTimingCountsCacheMissesOnly() {
        AtomicInteger queries = new AtomicInteger();
        SchemaCapabilityCache cache = new SchemaCapabilityCache();
        Connection connection = connection(queries, true);
        com.company.performance.RequestPerformanceContext.begin();
        com.company.performance.RequestPerformanceContext.Snapshot timing;
        try {
            assertTrue(cache.columnExists(connection, "sample", "present"));
            assertTrue(cache.columnExists(connection, "sample", "present"));
        } finally {
            timing = com.company.performance.RequestPerformanceContext.finish();
        }
        assertEquals(1, timing.metadataCount());
        assertEquals(0, timing.sqlCount());
    }

    @Test
    void tableInspectionCachesAllRequestedColumnsWithOneMetadataCall() {
        SchemaMetadataJdbcFixture jdbc = new SchemaMetadataJdbcFixture();
        jdbc.add("application", "sample_table", "first_column");
        jdbc.add("application", "sample_table", "second_column");
        SchemaCapabilityCache cache = new SchemaCapabilityCache();
        com.company.performance.RequestPerformanceContext.begin();
        com.company.performance.RequestPerformanceContext.Snapshot timing;
        try {
            cache.inspectColumns(jdbc.open(), Map.of("sample_table", List.of("first_column", "second_column")));
            assertTrue(cache.columnExists(jdbc.open(), "sample_table", "first_column"));
            assertTrue(cache.columnExists(jdbc.open(), "SAMPLE_TABLE", "SECOND_COLUMN"));
        } finally {
            timing = com.company.performance.RequestPerformanceContext.finish();
        }
        assertEquals(1, jdbc.queries.size());
        assertEquals(1, jdbc.resultCloses);
        assertEquals(1, timing.metadataCount());
        assertEquals(0, timing.sqlCount());
    }

    @Test
    void emptyTableResultsCacheAbsenceAfterUppercaseFallback() {
        SchemaMetadataJdbcFixture jdbc = new SchemaMetadataJdbcFixture();
        SchemaCapabilityCache cache = new SchemaCapabilityCache();
        cache.inspectColumns(jdbc.open(), Map.of("sample_table", List.of("first_column", "second_column")));

        assertFalse(cache.columnExists(jdbc.open(), "sample_table", "first_column"));
        assertFalse(cache.columnExists(jdbc.open(), "sample_table", "second_column"));
        assertEquals(List.of("sample_table", "SAMPLE_TABLE"),
                jdbc.queries.stream().map(SchemaMetadataJdbcFixture.Query::table).toList());
        assertEquals(2, jdbc.resultCloses);
    }

    @Test
    void uppercaseTableAndColumnsUseTheExistingUppercaseFallback() {
        SchemaMetadataJdbcFixture jdbc = new SchemaMetadataJdbcFixture();
        jdbc.add("application", "SAMPLE_TABLE", "FIRST_COLUMN");
        jdbc.add("application", "SAMPLE_TABLE", "SECOND_COLUMN");
        SchemaCapabilityCache cache = new SchemaCapabilityCache();
        cache.inspectColumns(jdbc.open(), Map.of("sample_table", List.of("first_column", "second_column")));

        assertTrue(cache.columnExists(jdbc.open(), "sample_table", "first_column"));
        assertTrue(cache.columnExists(jdbc.open(), "sample_table", "second_column"));
        assertEquals(2, jdbc.queries.size());
    }

    @Test
    void mixedCaseIdentifiersMatchVerticaCaseInsensitiveMetadata() {
        SchemaMetadataJdbcFixture jdbc = new SchemaMetadataJdbcFixture();
        jdbc.caseInsensitive = true;
        jdbc.add("application", "Sample_Table", "Mixed_Column");
        SchemaCapabilityCache cache = new SchemaCapabilityCache();
        cache.inspectColumns(jdbc.open(), Map.of("Sample_Table", List.of("Mixed_Column")));

        assertTrue(cache.columnExists(jdbc.open(), "Sample_Table", "Mixed_Column"));
        assertEquals(1, jdbc.queries.size());

        jdbc.add("application", "lower_table", "UPPER_COLUMN");
        cache.inspectColumns(jdbc.open(), Map.of("lower_table", List.of("upper_column")));
        assertTrue(cache.columnExists(jdbc.open(), "lower_table", "upper_column"));
        assertTrue(new SchemaCapabilityCache().columnExists(jdbc.open(), "lower_table", "upper_column"));
    }

    @Test
    void caseInsensitiveMetadataPreservesTheOriginalDriverResultAndBindings() {
        SchemaMetadataJdbcFixture jdbc = new SchemaMetadataJdbcFixture();
        jdbc.caseInsensitive = true;
        jdbc.add("application", "MiXeD_TaBlE", "FiRsT_CoLuMn");
        SchemaCapabilityCache cache = new SchemaCapabilityCache();
        cache.inspectColumns(jdbc.open(), Map.of("mixed_table", List.of("first_column")));

        assertTrue(cache.columnExists(jdbc.open(), "mixed_table", "first_column"));
        assertEquals(1, jdbc.queries.size());
        assertEquals("mixed_table", jdbc.queries.getFirst().table());
        assertNull(jdbc.queries.getFirst().catalog());
        assertNull(jdbc.queries.getFirst().schema());
        assertTrue(new SchemaCapabilityCache().columnExists(jdbc.open(), "mixed_table", "first_column"));
    }

    @Test
    void wildcardLookalikeTablesAndColumnsDoNotSatisfyExactRequirements() {
        SchemaMetadataJdbcFixture jdbc = new SchemaMetadataJdbcFixture();
        jdbc.add("application", "sampleXtable", "present_column");
        jdbc.add("application", "sample_table", "otherXcolumn");
        SchemaCapabilityCache cache = new SchemaCapabilityCache();
        cache.inspectColumns(jdbc.open(), Map.of("sample_table", List.of("present_column", "other_column")));

        assertFalse(cache.columnExists(jdbc.open(), "sample_table", "present_column"));
        assertFalse(cache.columnExists(jdbc.open(), "sample_table", "other_column"));
        assertEquals(2, jdbc.queries.size());
        SchemaCapabilityCache legacyLookup = new SchemaCapabilityCache();
        assertTrue(legacyLookup.columnExists(jdbc.open(), "sample_table", "present_column"));
        assertTrue(legacyLookup.columnExists(jdbc.open(), "sample_table", "other_column"));
    }

    @Test
    void matchingTablesAcrossSchemasRetainTheUnrestrictedMetadataScope() {
        SchemaMetadataJdbcFixture jdbc = new SchemaMetadataJdbcFixture();
        jdbc.add("application", "sample_table", "first_column");
        jdbc.add("archive", "sample_table", "second_column");
        SchemaCapabilityCache cache = new SchemaCapabilityCache();
        cache.inspectColumns(jdbc.open(), Map.of("sample_table", List.of("first_column", "second_column")));

        assertTrue(cache.columnExists(jdbc.open(), "sample_table", "first_column"));
        assertTrue(cache.columnExists(jdbc.open(), "sample_table", "second_column"));
        assertNull(jdbc.queries.getFirst().catalog());
        assertNull(jdbc.queries.getFirst().schema());
        assertEquals(0, jdbc.schemaReads);
        assertEquals(0, jdbc.catalogReads);
        assertEquals(1, jdbc.queries.size());
    }

    @Test
    void failedBatchAndFailedResultClosePreserveExistingCacheEntries() {
        SchemaMetadataJdbcFixture jdbc = new SchemaMetadataJdbcFixture();
        jdbc.add("application", "sample", "present");
        SchemaCapabilityCache cache = new SchemaCapabilityCache();
        cache.inspectColumns(jdbc.open(), Map.of("sample", List.of("present")));
        jdbc.columns.clear();
        jdbc.failureQuery = jdbc.queries.size() + 2;

        assertThrows(DataAccessException.class,
                () -> cache.inspectColumns(jdbc.open(), Map.of("sample", List.of("present"))));
        assertTrue(cache.columnExists(jdbc.open(), "sample", "present"));
        assertEquals(3, jdbc.queries.size());

        jdbc.failureQuery = -1;
        jdbc.failResultClose = true;
        assertThrows(DataAccessException.class,
                () -> cache.inspectColumns(jdbc.open(), Map.of("sample", List.of("present"))));
        assertTrue(cache.columnExists(jdbc.open(), "sample", "present"));
        assertEquals(4, jdbc.queries.size());
    }

    private static Connection connection(AtomicInteger queries, Boolean... results) {
        Queue<Boolean> rows = new ArrayDeque<>();
        for (Boolean result : results) {
            rows.add(result);
        }
        DatabaseMetaData metadata = (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[] {DatabaseMetaData.class},
                (ignored, call, args) -> {
                    if ("getColumns".equals(call.getName())) {
                        queries.incrementAndGet();
                        return resultSet(Boolean.TRUE.equals(rows.poll()));
                    }
                    return defaultValue(call.getReturnType());
                });
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (ignored, call, args) -> "getMetaData".equals(call.getName())
                        ? metadata
                        : defaultValue(call.getReturnType()));
    }

    private static ResultSet resultSet(boolean hasRow) {
        boolean[] first = {hasRow};
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "next" -> {
                        boolean result = first[0];
                        first[0] = false;
                        yield result;
                    }
                    default -> defaultValue(call.getReturnType());
                });
    }

}
