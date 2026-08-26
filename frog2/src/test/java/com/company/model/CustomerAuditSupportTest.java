package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomerAuditSupportTest {
    private static final List<String> AUDIT_COLUMNS = List.of(
            CustomerAuditSupport.UPDATED_AT,
            CustomerAuditSupport.UPDATED_BY,
            CustomerAuditSupport.DELETED_AT,
            CustomerAuditSupport.DELETED_BY);

    @Test
    void noAuditColumnsAreUnavailable() throws SQLException {
        assertEquals(
                CustomerAuditSupport.Capability.NONE,
                capability(Set.of()));
    }

    @Test
    void legacyUpdatedAtAloneIsUnavailable() throws SQLException {
        assertEquals(
                CustomerAuditSupport.Capability.NONE,
                capability(Set.of(CustomerAuditSupport.UPDATED_AT)));
    }

    @Test
    void allAuditColumnsAreComplete() throws SQLException {
        assertEquals(
                CustomerAuditSupport.Capability.COMPLETE,
                capability(Set.copyOf(AUDIT_COLUMNS)));
    }

    @Test
    void everyOtherIncompleteCombinationIsPartial() throws SQLException {
        int completeMask = (1 << AUDIT_COLUMNS.size()) - 1;
        for (int mask = 1; mask < completeMask; mask++) {
            Set<String> available = columnsFor(mask);
            if (available.equals(Set.of(CustomerAuditSupport.UPDATED_AT))) {
                continue;
            }

            assertEquals(
                    CustomerAuditSupport.Capability.PARTIAL,
                    capability(available),
                    available.toString());
        }
    }

    private static CustomerAuditSupport.Capability capability(
            Set<String> availableColumns) throws SQLException {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = availableColumns.stream()
                .map(column -> CustomerAuditSupport.TABLE + "." + column)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        try (Connection connection = jdbc.open()) {
            return CustomerAuditSupport.capability(
                    connection, new SchemaCapabilityCache());
        }
    }

    private static Set<String> columnsFor(int mask) {
        Set<String> columns = new HashSet<>();
        for (int index = 0; index < AUDIT_COLUMNS.size(); index++) {
            if ((mask & (1 << index)) != 0) {
                columns.add(AUDIT_COLUMNS.get(index));
            }
        }
        return Set.copyOf(columns);
    }
}
