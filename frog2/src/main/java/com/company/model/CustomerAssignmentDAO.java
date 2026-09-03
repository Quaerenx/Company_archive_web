package com.company.model;

import com.company.util.BusinessDate;
import com.company.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads maintenance ownership and schedule data for dashboards and inboxes.
 */
public class CustomerAssignmentDAO {
    private static final int ACTIVE_FLAG = 1;
    private static final String MAINTENANCE_SCHEDULE_TABLE =
            "customer_maintenance_schedule";
    private static final String MAINTENANCE_SCHEDULE_CAPABILITY =
            "interval_months";
    private static final String MAINTENANCE_CUSTOMER_TYPE =
            "정기점검 계약 고객사";
    private static final String CUSTOMER_COLUMNS =
            CustomerFieldContract.selectColumns("d");
    private static final SchemaCapabilityCache APPLICATION_SCHEMA_CAPABILITIES =
            new SchemaCapabilityCache();

    private final JdbcConnectionProvider connectionProvider;
    private final SchemaCapabilityCache schemaCapabilities;
    private final Clock clock;

    public CustomerAssignmentDAO() {
        this(
                DBConnection::getConnection,
                APPLICATION_SCHEMA_CAPABILITIES,
                BusinessDate.systemClock());
    }

    CustomerAssignmentDAO(JdbcConnectionProvider connectionProvider) {
        this(connectionProvider, new SchemaCapabilityCache(),
                BusinessDate.systemClock());
    }

    CustomerAssignmentDAO(
            JdbcConnectionProvider connectionProvider,
            Clock clock) {
        this(connectionProvider, new SchemaCapabilityCache(), clock);
    }

    CustomerAssignmentDAO(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache schemaCapabilities,
            Clock clock) {
        this.connectionProvider = Objects.requireNonNull(
                connectionProvider, "connectionProvider");
        this.schemaCapabilities = Objects.requireNonNull(
                schemaCapabilities, "schemaCapabilities");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public List<CustomerDTO> getMaintenanceCustomersByAssignee(
            String userId,
            String displayName) {
        if (isBlank(userId) && isBlank(displayName)) {
            return List.of();
        }
        try (Connection connection = connectionProvider.getConnection()) {
            CustomerAssignmentSupport.Capability capability =
                    CustomerAssignmentSupport.capability(
                            connection, schemaCapabilities);
            String assignee = CustomerAssignmentSupport.assigneeValue(
                    capability, userId, displayName);
            if (assignee == null) {
                return List.of();
            }
            String sql = "SELECT " + CUSTOMER_COLUMNS
                    + " FROM vertica_customer_detail d "
                    + "WHERE d.is_deleted = " + ACTIVE_FLAG
                    + " AND d.customer_type = ? AND "
                    + CustomerAssignmentSupport.assigneePredicate(capability)
                    + " ORDER BY d.customer_name ASC";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, MAINTENANCE_CUSTOMER_TYPE);
                statement.setString(2, assignee);
                statement.setString(3, assignee);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<CustomerDTO> customers = new ArrayList<>();
                    while (resultSet.next()) {
                        customers.add(CustomerFieldContract.read(resultSet));
                    }
                    return List.copyOf(customers);
                }
            }
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "load maintenance customers by assignee", exception);
        }
    }

    public List<MaintenanceCustomerAssignment>
            getMaintenanceCustomerAssignments() {
        return getMaintenanceCustomerAssignments(
                BusinessDate.currentMonth(clock));
    }

    public List<MaintenanceCustomerAssignment>
            getMaintenanceCustomerAssignments(YearMonth targetMonth) {
        Objects.requireNonNull(targetMonth, "targetMonth");
        return getAllMaintenanceCustomerAssignments().stream()
                .filter(assignment -> assignment.schedule().isDue(targetMonth))
                .toList();
    }

    public List<MaintenanceCustomerAssignment>
            getAllMaintenanceCustomerAssignments() {
        return loadMaintenanceCustomerAssignments(null, null);
    }

    public List<MaintenanceCustomerAssignment>
            getMaintenanceCustomerAssignmentsByAssignee(
                    String userId,
                    String displayName) {
        if (isBlank(userId) && isBlank(displayName)) {
            return List.of();
        }
        return loadMaintenanceCustomerAssignments(userId, displayName);
    }

    private List<MaintenanceCustomerAssignment>
            loadMaintenanceCustomerAssignments(
                    String userId,
                    String displayName) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean scheduleAvailable = schemaCapabilities.columnExists(
                    connection,
                    MAINTENANCE_SCHEDULE_TABLE,
                    MAINTENANCE_SCHEDULE_CAPABILITY);
            CustomerAssignmentSupport.Capability assignmentCapability =
                    CustomerAssignmentSupport.capability(
                            connection, schemaCapabilities);
            if (assignmentCapability
                    == CustomerAssignmentSupport.Capability.PARTIAL) {
                throw new SQLException(
                        "Customer assignment user-ID columns are partially applied");
            }
            String assignee = userId == null && displayName == null
                    ? null
                    : CustomerAssignmentSupport.assigneeValue(
                            assignmentCapability, userId, displayName);
            if ((userId != null || displayName != null) && assignee == null) {
                return List.of();
            }
            return loadMaintenanceCustomerAssignments(
                    connection,
                    scheduleAvailable,
                    assignmentCapability,
                    assignee);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "load maintenance customer assignments", exception);
        }
    }

    private List<MaintenanceCustomerAssignment>
            loadMaintenanceCustomerAssignments(
                    Connection connection,
                    boolean scheduleAvailable,
                    CustomerAssignmentSupport.Capability assignmentCapability,
                    String assignee) throws SQLException {
        String scheduleColumns = scheduleAvailable
                ? ", s.interval_months, s.anchor_month, s.enabled, "
                        + "s.effective_from, s.effective_to "
                : "";
        String scheduleJoin = scheduleAvailable
                ? "LEFT JOIN customer_maintenance_schedule s "
                        + "ON s.customer_name = d.customer_name "
                : "";
        String assigneeClause = assignee == null
                ? ""
                : "AND " + CustomerAssignmentSupport.assigneePredicate(
                        assignmentCapability) + " ";
        String sql = "SELECT d.customer_name, d.main_manager "
                + scheduleColumns
                + "FROM vertica_customer_detail d "
                + scheduleJoin
                + "WHERE d.is_deleted = " + ACTIVE_FLAG
                + " AND d.customer_type = ? "
                + assigneeClause
                + "ORDER BY CASE WHEN d.main_manager IS NULL "
                + "OR TRIM(d.main_manager) = '' THEN 1 ELSE 0 END, "
                + "d.main_manager ASC, d.customer_name ASC";
        List<MaintenanceCustomerAssignment> assignments = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, MAINTENANCE_CUSTOMER_TYPE);
            if (assignee != null) {
                statement.setString(2, assignee);
                statement.setString(3, assignee);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    MaintenanceSchedule schedule = scheduleAvailable
                            ? readMaintenanceSchedule(resultSet)
                            : MaintenanceSchedule.monthlyDefault();
                    assignments.add(new MaintenanceCustomerAssignment(
                            resultSet.getString("customer_name"),
                            resultSet.getString("main_manager"),
                            schedule));
                }
            }
        }
        return List.copyOf(assignments);
    }

    private static MaintenanceSchedule readMaintenanceSchedule(
            ResultSet resultSet) throws SQLException {
        int intervalMonths = resultSet.getInt("interval_months");
        java.sql.Date anchorDate = resultSet.getDate("anchor_month");
        java.sql.Date effectiveFromDate = resultSet.getDate("effective_from");
        if (intervalMonths == 0
                || anchorDate == null
                || effectiveFromDate == null) {
            return MaintenanceSchedule.monthlyDefault();
        }
        java.sql.Date effectiveToDate = resultSet.getDate("effective_to");
        String enabledValue = resultSet.getString("enabled");
        boolean enabled = enabledValue == null
                || "1".equals(enabledValue)
                || "t".equalsIgnoreCase(enabledValue)
                || "true".equalsIgnoreCase(enabledValue);
        LocalDate effectiveTo = effectiveToDate == null
                ? null
                : effectiveToDate.toLocalDate();
        return new MaintenanceSchedule(
                intervalMonths,
                YearMonth.from(anchorDate.toLocalDate()),
                effectiveFromDate.toLocalDate(),
                effectiveTo,
                enabled);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
