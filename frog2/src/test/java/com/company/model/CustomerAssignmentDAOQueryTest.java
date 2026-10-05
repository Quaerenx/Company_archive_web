package com.company.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomerAssignmentDAOQueryTest {
    private static final Set<String> ASSIGNMENT_COLUMNS = Set.of(
            "vertica_customer_detail.main_manager_user_id",
            "vertica_customer_detail.sub_manager_user_id");

    @Test
    void assignedCustomerQueryUsesStableIdWithoutMaintenanceTypeRestriction() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ASSIGNMENT_COLUMNS;
        jdbc.enqueue(PaginationJdbcFixture.row(
                "customer_name", "Assigned project customer",
                "customer_type", "Project",
                "main_manager", "New display name"));
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        var customers = dao.getCustomersByAssignee(" stable-id ", "Old name");

        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
        assertEquals(1, jdbc.statements.size());
        var statement = jdbc.statements.getFirst();
        assertTrue(statement.sql.contains("d.is_deleted = 1"));
        assertTrue(statement.sql.contains(
                "d.main_manager_user_id = ? OR d.sub_manager_user_id = ?"));
        assertFalse(statement.sql.contains("d.customer_type = ?"));
        assertFalse(statement.sql.contains("LOWER(TRIM(d.main_manager))"));
        assertEquals("stable-id", statement.parameters.get(1));
        assertEquals("stable-id", statement.parameters.get(2));
        assertEquals("Project", customers.getFirst().getCustomerType());
    }

    @Test
    void legacyAssignedCustomerQueryPreservesNormalizedDisplayNameRules() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row("customer_name", "Assigned"));
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        dao.getCustomersByAssignee("ignored-id", " Manager ");

        var statement = jdbc.statements.getFirst();
        assertTrue(statement.sql.contains(
                "LOWER(TRIM(d.main_manager)) = LOWER(?) "
                        + "OR LOWER(TRIM(d.sub_manager)) = LOWER(?)"));
        assertEquals("Manager", statement.parameters.get(1));
        assertEquals("Manager", statement.parameters.get(2));
    }

    @Test
    void legacyDisplayNameNeverGrantsCustomerMutationPermission() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        assertTrue(dao.getCustomerNamesByAssignee("attacker-id", "Manager").isEmpty());
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void customerMutationPermissionUsesStableUserId() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ASSIGNMENT_COLUMNS;
        jdbc.enqueue(PaginationJdbcFixture.row("customer_name", "Assigned"));
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        assertEquals(Set.of("Assigned"),
                dao.getCustomerNamesByAssignee("stable-id", "Other Name"));
        var statement = jdbc.statements.getFirst();
        assertTrue(statement.sql.contains("d.main_manager_user_id = ?"));
        assertFalse(statement.sql.contains("LOWER(TRIM(d.main_manager))"));
        assertEquals("stable-id", statement.parameters.get(1));
    }

    @Test
    void combinedMaintenanceQueryMapsCustomerAndQuarterlyScheduleInOneQuery() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of(
                "vertica_customer_detail.main_manager_user_id",
                "vertica_customer_detail.sub_manager_user_id",
                "customer_maintenance_schedule.interval_months");
        jdbc.enqueue(PaginationJdbcFixture.row(
                "customer_name", "Assigned",
                "main_manager", "Renamed",
                "vertica_version", "24.4",
                "license_info", "1000",
                "interval_months", 3,
                "anchor_month", Date.valueOf("2025-02-01"),
                "enabled", true,
                "effective_from", Date.valueOf("2025-02-01"),
                "effective_to", null));
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        MaintenanceAssigneeData data = dao.getMaintenanceAssigneeData("stable-id", "Old name");

        assertEquals(1, jdbc.openCount);
        assertEquals(1, jdbc.closeCount);
        assertEquals(1, jdbc.statements.size());
        var statement = jdbc.statements.getFirst();
        assertTrue(statement.sql.contains("LEFT JOIN customer_maintenance_schedule s"));
        assertTrue(statement.sql.contains("d.customer_type = ?"));
        assertTrue(statement.sql.contains("d.is_deleted = 1"));
        assertEquals("정기점검 계약 고객사", statement.parameters.get(1));
        assertEquals("stable-id", statement.parameters.get(2));
        assertEquals("stable-id", statement.parameters.get(3));
        assertEquals("24.4", data.customers().getFirst().getVerticaVersion());
        assertEquals("1000", data.customers().getFirst().getLicenseSize());
        var assignment = data.assignments().getFirst();
        assertEquals("Assigned", assignment.customerName());
        assertEquals("Renamed", assignment.managerName());
        assertTrue(assignment.schedule().isDue(YearMonth.of(2026, 8)));
        assertFalse(assignment.schedule().isDue(YearMonth.of(2026, 9)));
    }

    @Test
    void combinedQueryPreservesDefaultScheduleForMissingScheduleRows() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of("customer_maintenance_schedule.interval_months");
        jdbc.enqueue(PaginationJdbcFixture.row(
                "customer_name", "No schedule", "main_manager", "Manager"));
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        MaintenanceAssigneeData data = dao.getMaintenanceAssigneeData("user-id", "Manager");

        assertEquals(MaintenanceSchedule.monthlyDefault(), data.assignments().getFirst().schedule());
        assertEquals("Manager", jdbc.statements.getFirst().parameters.get(2));
    }

    @Test
    void combinedQuerySupportsMissingScheduleTableWithoutJoiningIt() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.enqueue(PaginationJdbcFixture.row(
                "customer_name", "Assigned", "main_manager", "Manager"));
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        MaintenanceAssigneeData data = dao.getMaintenanceAssigneeData("user-id", "Manager");

        assertFalse(jdbc.statements.getFirst().sql.contains("JOIN customer_maintenance_schedule"));
        assertEquals(MaintenanceSchedule.monthlyDefault(), data.assignments().getFirst().schedule());
    }

    @Test
    void newQueriesFailClosedOnPartialAssignmentSchema() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = Set.of("vertica_customer_detail.main_manager_user_id");
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        assertThrows(DataAccessException.class, () -> dao.getCustomersByAssignee("user-id", "Manager"));
        assertThrows(DataAccessException.class, () -> dao.getMaintenanceAssigneeData("user-id", "Manager"));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void migratedQueriesDoNotFallBackToDisplayNameWhenUserIdIsMissing() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        jdbc.availableColumns = ASSIGNMENT_COLUMNS;
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        assertTrue(dao.getCustomersByAssignee(null, "Manager").isEmpty());
        assertTrue(dao.getMaintenanceAssigneeData(null, "Manager").customers().isEmpty());
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void blankAssigneeSkipsConnectionsForBothNewQueries() {
        PaginationJdbcFixture jdbc = new PaginationJdbcFixture();
        CustomerAssignmentDAO dao = new CustomerAssignmentDAO(jdbc::open);

        assertTrue(dao.getCustomersByAssignee(null, " ").isEmpty());
        assertTrue(dao.getMaintenanceAssigneeData(null, " ").customers().isEmpty());
        assertEquals(0, jdbc.openCount);
    }
}
