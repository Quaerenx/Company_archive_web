package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomerDAOMaintenanceAssignmentTest {
    @Test
    void loadsOnlyNamesNeededForTheDashboardInStableAssigneeOrder() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(
                PaginationJdbcFixture.row(
                        "customer_name", "Alpha",
                        "main_manager", "Manager A"),
                PaginationJdbcFixture.row(
                        "customer_name", "Beta",
                        "main_manager", null));
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        List<MaintenanceCustomerAssignment> assignments =
                dao.getMaintenanceCustomerAssignments();

        assertEquals(1, jdbc.statements.size());
        String sql = jdbc.statements.getFirst().sql;
        assertTrue(sql.startsWith(
                "SELECT d.customer_name, d.main_manager "));
        assertTrue(sql.contains("d.customer_type = ?"));
        assertTrue(sql.contains("d.is_deleted = 1"));
        assertTrue(sql.contains("d.main_manager ASC, d.customer_name ASC"));
        assertFalse(sql.contains("vertica_version"));
        assertFalse(sql.contains("license_info"));
        assertEquals(
                "정기점검 계약 고객사",
                jdbc.statements.getFirst().parameters.get(1));
        assertEquals(
                new MaintenanceCustomerAssignment("Alpha", "Manager A"),
                assignments.getFirst());
        assertEquals(
                new MaintenanceCustomerAssignment("Beta", null),
                assignments.get(1));
        assertTrue(assignments.stream()
                .allMatch(assignment -> !assignment.schedule().isQuarterly()));
    }

    @Test
    void filtersConfiguredQuarterlyCustomersByTheirOwnAnchorMonth() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "customer_maintenance_schedule.interval_months");
        jdbc.enqueue(
                scheduleRow("Monthly", "Manager A", 1, "2000-01-01", true),
                scheduleRow("Quarterly due", "Manager B", 3, "2025-02-01", true),
                scheduleRow("Quarterly later", "Manager C", 3, "2025-01-01", true),
                scheduleRow("Disabled", "Manager D", 1, "2000-01-01", false));
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        List<MaintenanceCustomerAssignment> assignments =
                dao.getMaintenanceCustomerAssignments(YearMonth.of(2026, 8));

        assertEquals(1, jdbc.statements.size());
        assertTrue(jdbc.statements.getFirst().sql.contains(
                "LEFT JOIN customer_maintenance_schedule s"));
        assertEquals(
                List.of("Monthly", "Quarterly due"),
                assignments.stream()
                        .map(MaintenanceCustomerAssignment::customerName)
                        .toList());
        assertFalse(assignments.getFirst().schedule().isQuarterly());
        assertTrue(assignments.get(1).schedule().isQuarterly());
    }

    @Test
    void loadsEveryMaintenanceScheduleForFrequencyLabels() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "customer_maintenance_schedule.interval_months");
        jdbc.enqueue(
                scheduleRow("Monthly", "Manager A", 1, "2000-01-01", true),
                scheduleRow("Quarterly not due", "Manager B", 3, "2025-01-01", true));
        CustomerDAO dao = new CustomerDAO(jdbc::open);

        List<MaintenanceCustomerAssignment> assignments =
                dao.getAllMaintenanceCustomerAssignments();

        assertEquals(1, jdbc.statements.size());
        assertEquals(List.of("Monthly", "Quarterly not due"),
                assignments.stream()
                        .map(MaintenanceCustomerAssignment::customerName)
                        .toList());
        assertFalse(assignments.getFirst().schedule().isQuarterly());
        assertTrue(assignments.get(1).schedule().isQuarterly());
    }

    @Test
    void defaultScheduleFilterUsesSeoulMonthAtUtcBoundary() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "customer_maintenance_schedule.interval_months");
        jdbc.enqueue(
                scheduleRow(
                        "September quarterly",
                        "Manager A",
                        3,
                        "2025-03-01",
                        true),
                scheduleRow(
                        "August quarterly",
                        "Manager B",
                        3,
                        "2025-02-01",
                        true));
        Clock utcClockAtSeoulMidnight = Clock.fixed(
                Instant.parse("2026-08-31T15:00:00Z"),
                ZoneOffset.UTC);
        CustomerDAO dao = new CustomerDAO(
                jdbc::open, utcClockAtSeoulMidnight);

        List<MaintenanceCustomerAssignment> assignments =
                dao.getMaintenanceCustomerAssignments();

        assertEquals(List.of("September quarterly"),
                assignments.stream()
                        .map(MaintenanceCustomerAssignment::customerName)
                        .toList());
    }

    private static java.util.Map<String, Object> scheduleRow(
            String customerName,
            String managerName,
            int intervalMonths,
            String anchorMonth,
            boolean enabled) {
        return PaginationJdbcFixture.row(
                "customer_name", customerName,
                "main_manager", managerName,
                "interval_months", intervalMonths,
                "anchor_month", Date.valueOf(anchorMonth),
                "enabled", enabled,
                "effective_from", Date.valueOf("2000-01-01"),
                "effective_to", null);
    }
}
