package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.sql.SQLNonTransientException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MonthlyCustomerResponseDAOContractTest {
    @Test
    void monthlyLookupUsesHalfOpenDateRange() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns =
                Set.of("monthly_customer_response.created_by_user_id");
        jdbc.enqueue();
        MonthlyCustomerResponseDAO dao =
                new MonthlyCustomerResponseDAO(jdbc::open);

        assertTrue(dao.getMonthlyResponses(
                "user-1", 2026, 7).isEmpty());

        assertEquals(1, jdbc.statements.size());
        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.contains(
                "response_date >= ? AND response_date < ?"));
        assertFalse(statement.sql.contains("YEAR("));
        assertFalse(statement.sql.contains("MONTH("));
        assertEquals("user-1", statement.parameters.get(1));
        assertEquals(Date.valueOf("2026-07-01"),
                statement.parameters.get(2));
        assertEquals(Date.valueOf("2026-08-01"),
                statement.parameters.get(3));
    }

    @Test
    void monthlyLookupHandlesLeapYearAndYearBoundary() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns =
                Set.of("monthly_customer_response.created_by_user_id");
        jdbc.enqueue();
        jdbc.enqueue();
        MonthlyCustomerResponseDAO dao =
                new MonthlyCustomerResponseDAO(jdbc::open);

        dao.getMonthlyResponses("user-1", 2024, 2);
        dao.getMonthlyResponses("user-1", 2026, 12);

        assertEquals(Date.valueOf("2024-02-01"),
                jdbc.statements.get(0).parameters.get(2));
        assertEquals(Date.valueOf("2024-03-01"),
                jdbc.statements.get(0).parameters.get(3));
        assertEquals(Date.valueOf("2026-12-01"),
                jdbc.statements.get(1).parameters.get(2));
        assertEquals(Date.valueOf("2027-01-01"),
                jdbc.statements.get(1).parameters.get(3));
    }

    @Test
    void invalidMonthKeepsEmptyResultWithoutOpeningDatabase() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        MonthlyCustomerResponseDAO dao =
                new MonthlyCustomerResponseDAO(jdbc::open);

        assertTrue(dao.getMonthlyResponses(
                "user-1", 2026, 13).isEmpty());
        assertTrue(dao.getMonthlyResponses(
                "user-1", 999_999_999, 12).isEmpty());
        assertEquals(0, jdbc.openCount);
    }

    @Test
    void ownerLookupFailsClosedWhenStableOwnershipColumnIsMissing() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO(
                jdbc::open, new SchemaCapabilityCache());

        assertTrue(dao.getMonthlyResponses(
                "user-1", 2026, 7).isEmpty());
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void migratedLookupUsesStableUserId() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns =
                Set.of("monthly_customer_response.created_by_user_id");
        jdbc.enqueue();
        MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO(
                jdbc::open, new SchemaCapabilityCache());

        assertTrue(dao.getMonthlyResponses(
                " user-1 ", 2026, 7).isEmpty());

        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.contains(
                "WHERE created_by_user_id = ?"));
        assertEquals("user-1", statement.parameters.get(1));
    }

    @Test
    void equalDisplayNamesCannotMergeDifferentStableOwnerQueries() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns =
                Set.of("monthly_customer_response.created_by_user_id");
        jdbc.enqueue();
        jdbc.enqueue();
        MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO(
                jdbc::open, new SchemaCapabilityCache());

        dao.getMonthlyResponses("user-1", 2026, 8);
        dao.getMonthlyResponses("user-2", 2026, 8);

        assertEquals("user-1", jdbc.statements.get(0).parameters.get(1));
        assertEquals("user-2", jdbc.statements.get(1).parameters.get(1));
        assertTrue(jdbc.statements.stream().allMatch(statement ->
                statement.sql.contains("WHERE created_by_user_id = ?")));
    }

    @Test
    void writeFailsClosedWhenOwnershipColumnIsMissing() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO(
                jdbc::open, new SchemaCapabilityCache());
        MonthlyCustomerResponseDTO dto = new MonthlyCustomerResponseDTO();
        dto.setUserId("user-1");
        dto.setResponseDate(Date.valueOf("2026-07-31"));
        dto.setCustomerName("Acme");
        dto.setReason("Support");

        assertFalse(dao.addResponse(dto));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void writePersistsStableOwnerWhenMigrationIsReady() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns =
                Set.of("monthly_customer_response.created_by_user_id");
        jdbc.enqueueUpdate(1);
        MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO(
                jdbc::open, new SchemaCapabilityCache());
        MonthlyCustomerResponseDTO dto = new MonthlyCustomerResponseDTO();
        dto.setUserId("user-1");
        dto.setUserName("Renamed");
        dto.setResponseDate(Date.valueOf("2026-07-31"));
        dto.setCustomerName("Acme");
        dto.setReason("Support");

        assertTrue(dao.addResponse(dto));

        PaginationJdbcFixture.StatementRecord statement =
                jdbc.statements.getFirst();
        assertTrue(statement.sql.contains("created_by_user_id"));
        assertEquals("user-1", statement.parameters.get(1));
        assertEquals("Renamed", statement.parameters.get(2));
    }

    @Test
    void readOnlyWriteFailureRemainsExplicit() {
        MonthlyCustomerResponseDAO dao = new MonthlyCustomerResponseDAO(() -> {
            throw new SQLNonTransientException("blocked", "25006");
        });
        MonthlyCustomerResponseDTO dto = new MonthlyCustomerResponseDTO();
        dto.setUserId("user-1");
        dto.setResponseDate(Date.valueOf("2026-07-31"));
        dto.setCustomerName("Acme");
        dto.setReason("Support");

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> dao.addResponse(dto));

        assertTrue(exception.isReadOnlyViolation());
        assertEquals(DataAccessException.Kind.READ_ONLY, exception.getKind());
    }

    @Test
    void writeRejectsMissingRequiredValuesBeforeOpeningDatabase() {
        for (String missingField : Set.of("date", "customer", "reason")) {
            PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
            MonthlyCustomerResponseDAO dao =
                    new MonthlyCustomerResponseDAO(jdbc::open);
            MonthlyCustomerResponseDTO dto =
                    new MonthlyCustomerResponseDTO();
            dto.setId(1);
            dto.setUserId("user-1");
            dto.setResponseDate(Date.valueOf("2026-08-30"));
            dto.setCustomerName("Acme");
            dto.setReason("Support");
            switch (missingField) {
                case "date" -> dto.setResponseDate(null);
                case "customer" -> dto.setCustomerName("  ");
                case "reason" -> dto.setReason(null);
                default -> throw new AssertionError(missingField);
            }

            assertFalse(dao.addResponse(dto));
            assertFalse(dao.updateResponse(dto));
            assertEquals(0, jdbc.openCount);
        }
    }
}
