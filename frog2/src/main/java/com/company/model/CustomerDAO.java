package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.company.util.DBConnection;
import com.company.util.Pagination;
import com.company.util.SearchQueryPolicy;

public class CustomerDAO {
    private static final int ACTIVE_FLAG = 1;
    private static final int DELETED_FLAG = 0;
    private static final String MAINTENANCE_SCHEDULE_TABLE =
            "customer_maintenance_schedule";
    private static final String MAINTENANCE_SCHEDULE_CAPABILITY =
            "interval_months";
    private static final SchemaCapabilityCache APPLICATION_SCHEMA_CAPABILITIES =
            new SchemaCapabilityCache();
    private static final String MAINTENANCE_FILTER = "maintenance";
    private static final String MAINTENANCE_CUSTOMER_TYPE =
            "정기점검 계약 고객사";
    private static final String CUSTOMER_COLUMNS =
            "d.customer_name, d.vertica_version, d.db_mode, d.os_info, "
                    + "d.node_count, d.license_info, d.said, d.main_manager, "
                    + "d.sub_manager, d.db_name, d.customer_type";
    private static final String SEARCH_PREDICATE =
            "(CAST(d.customer_name AS VARCHAR(65000)) ILIKE ? "
                    + "OR CAST(d.vertica_version AS VARCHAR(65000)) ILIKE ? "
                    + "OR CAST(d.db_mode AS VARCHAR(65000)) ILIKE ? "
                    + "OR CAST(d.os_info AS VARCHAR(65000)) ILIKE ? "
                    + "OR CAST(d.said AS VARCHAR(65000)) ILIKE ? "
                    + "OR CAST(d.main_manager AS VARCHAR(65000)) ILIKE ?)";

    private final JdbcConnectionProvider connectionProvider;
    private final SchemaCapabilityCache schemaCapabilities;

    public CustomerDAO() {
        this(DBConnection::getConnection, APPLICATION_SCHEMA_CAPABILITIES);
    }

    CustomerDAO(JdbcConnectionProvider connectionProvider) {
        this(connectionProvider, new SchemaCapabilityCache());
    }

    CustomerDAO(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache schemaCapabilities) {
        this.connectionProvider = Objects.requireNonNull(
                connectionProvider, "connectionProvider");
        this.schemaCapabilities = Objects.requireNonNull(
                schemaCapabilities, "schemaCapabilities");
    }

    // 모든 고객사 정보 조회 (활성 상태만, 필터 옵션 추가)
    public List<CustomerDTO> getAllCustomers(String sortField, String sortDirection, String filter) {
        List<CustomerDTO> customerList = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection()) {
            String direction = "DESC".equalsIgnoreCase(sortDirection)
                    ? "DESC"
                    : "ASC";
            String sql = "SELECT " + CUSTOMER_COLUMNS
                    + " FROM vertica_customer_detail d "
                    + "WHERE d.is_deleted = " + ACTIVE_FLAG;
            if (MAINTENANCE_FILTER.equals(filter)) {
                sql += " AND d.customer_type = '정기점검 계약 고객사'";
            }
            sql += " ORDER BY " + sortColumn(sortField) + " " + direction;

            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    customerList.add(mapCustomer(resultSet));
                }
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        }

        return customerList;
    }

    // 기존 호환성을 위한 오버로드 메소드 (기본값: 전체 보기)
    public List<CustomerDTO> getAllCustomers(String sortField, String sortDirection) {
        return getAllCustomers(sortField, sortDirection, "all");
    }

    public List<CustomerDTO> getMaintenanceCustomers(
            String sortField, String sortDirection) {
        return getAllCustomers(
                sortField, sortDirection, MAINTENANCE_FILTER);
    }

    public boolean isActiveMaintenanceCustomer(String customerName) {
        CustomerDTO customer = getCustomerByName(customerName);
        return customer != null
                && MAINTENANCE_CUSTOMER_TYPE.equals(customer.getCustomerType());
    }

    public List<MaintenanceCustomerAssignment> getMaintenanceCustomerAssignments() {
        return getMaintenanceCustomerAssignments(YearMonth.now());
    }

    public List<MaintenanceCustomerAssignment> getMaintenanceCustomerAssignments(
            YearMonth targetMonth) {
        Objects.requireNonNull(targetMonth, "targetMonth");
        return getAllMaintenanceCustomerAssignments().stream()
                .filter(assignment -> assignment.schedule().isDue(targetMonth))
                .toList();
    }

    public List<MaintenanceCustomerAssignment> getAllMaintenanceCustomerAssignments() {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean scheduleAvailable = schemaCapabilities.columnExists(
                    connection,
                    MAINTENANCE_SCHEDULE_TABLE,
                    MAINTENANCE_SCHEDULE_CAPABILITY);
            return loadMaintenanceCustomerAssignments(
                    connection, scheduleAvailable);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "load maintenance customer assignments", exception);
        }
    }

    private List<MaintenanceCustomerAssignment> loadMaintenanceCustomerAssignments(
            Connection connection,
            boolean scheduleAvailable) throws SQLException {
        String scheduleColumns = scheduleAvailable
                ? ", s.interval_months, s.anchor_month, s.enabled, "
                        + "s.effective_from, s.effective_to "
                : "";
        String scheduleJoin = scheduleAvailable
                ? "LEFT JOIN customer_maintenance_schedule s "
                        + "ON s.customer_name = d.customer_name "
                : "";
        String sql = "SELECT d.customer_name, d.main_manager "
                + scheduleColumns
                + "FROM vertica_customer_detail d "
                + scheduleJoin
                + "WHERE d.is_deleted = " + ACTIVE_FLAG
                + " AND d.customer_type = ? "
                + "ORDER BY CASE WHEN d.main_manager IS NULL "
                + "OR TRIM(d.main_manager) = '' THEN 1 ELSE 0 END, "
                + "d.main_manager ASC, d.customer_name ASC";
        List<MaintenanceCustomerAssignment> assignments = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, MAINTENANCE_CUSTOMER_TYPE);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    MaintenanceSchedule schedule = scheduleAvailable
                            ? readMaintenanceSchedule(resultSet)
                            : MaintenanceSchedule.monthlyDefault();
                    MaintenanceCustomerAssignment assignment =
                            new MaintenanceCustomerAssignment(
                                    resultSet.getString("customer_name"),
                                    resultSet.getString("main_manager"),
                                    schedule);
                    assignments.add(assignment);
                }
            }
            return assignments;
        }
    }

    private MaintenanceSchedule readMaintenanceSchedule(ResultSet resultSet)
            throws SQLException {
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

    public CustomerPage getCustomerPage(
            String sortField,
            String sortDirection,
            String filter,
            String query,
            int requestedPage,
            int pageSize) {
        String normalizedFilter = MAINTENANCE_FILTER.equals(filter)
                ? MAINTENANCE_FILTER
                : "all";
        String normalizedQuery = normalizeQuery(query);
        String selectionPredicate =
                selectionPredicate(normalizedFilter, normalizedQuery);

        try (Connection connection = connectionProvider.getConnection()) {
            CustomerCounts counts;
            String countSql =
                    "SELECT COUNT(*) AS total_count, "
                            + "COALESCE(SUM(CASE WHEN d.customer_type = '"
                            + MAINTENANCE_CUSTOMER_TYPE
                            + "' THEN 1 ELSE 0 END), 0) AS maintenance_count "
                            + "FROM vertica_customer_detail d "
                            + "WHERE d.is_deleted = " + ACTIVE_FLAG;
            try (PreparedStatement statement =
                            connection.prepareStatement(countSql)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        counts = new CustomerCounts(
                                resultSet.getInt("total_count"),
                                resultSet.getInt("maintenance_count"));
                    } else {
                        counts = new CustomerCounts(0, 0);
                    }
                }
            }

            String direction =
                    "DESC".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
            String itemSql = "SELECT " + CUSTOMER_COLUMNS
                    + ", COUNT(*) OVER () AS result_count"
                    + " FROM vertica_customer_detail d "
                    + "WHERE d.is_deleted = " + ACTIVE_FLAG + " AND "
                    + selectionPredicate
                    + " ORDER BY "
                    + sortColumn(sortField)
                    + " "
                    + direction
                    + ", d.customer_name ASC LIMIT ? OFFSET ?";

            int maximumPage = Pagination.totalPages(
                    counts.total(), pageSize);
            int page = Pagination.clampPage(requestedPage, maximumPage);
            CustomerRows rows = loadCustomerRows(
                    connection,
                    itemSql,
                    normalizedQuery,
                    page,
                    pageSize);
            if (!rows.items().isEmpty()) {
                return customerPage(rows, page, pageSize, counts);
            }

            if (page == 1) {
                return customerPage(rows, page, pageSize, counts);
            }

            int resultCount = countCustomerMatches(
                    connection, selectionPredicate, normalizedQuery);
            int correctedPage = Pagination.clampPage(
                    page,
                    Pagination.totalPages(resultCount, pageSize));
            if (resultCount == 0) {
                return new CustomerPage(
                        new PageResult<>(
                                List.of(), 0, correctedPage, pageSize),
                        counts);
            }
            CustomerRows correctedRows = loadCustomerRows(
                    connection,
                    itemSql,
                    normalizedQuery,
                    correctedPage,
                    pageSize);
            return customerPage(
                    correctedRows, correctedPage, pageSize, counts);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "load customer page", exception);
        }
    }

    private static CustomerPage customerPage(
            CustomerRows rows,
            int page,
            int pageSize,
            CustomerCounts counts) {
        return new CustomerPage(
                new PageResult<>(
                        rows.items(),
                        rows.totalCount(),
                        page,
                        pageSize),
                counts);
    }

    private static CustomerRows loadCustomerRows(
            Connection connection,
            String sql,
            String query,
            int page,
            int pageSize) throws SQLException {
        List<CustomerDTO> customers = new ArrayList<>();
        int totalCount = 0;
        try (PreparedStatement statement =
                        connection.prepareStatement(sql)) {
            int parameterIndex = bindSearch(statement, 1, query);
            statement.setInt(parameterIndex++, pageSize);
            statement.setInt(
                    parameterIndex,
                    Pagination.offset(page, pageSize));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (customers.isEmpty()) {
                        totalCount = resultSet.getInt("result_count");
                    }
                    customers.add(mapCustomer(resultSet));
                }
            }
        }
        return new CustomerRows(customers, totalCount);
    }

    private static int countCustomerMatches(
            Connection connection,
            String selectionPredicate,
            String query) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vertica_customer_detail d "
                + "WHERE d.is_deleted = " + ACTIVE_FLAG + " AND "
                + selectionPredicate;
        try (PreparedStatement statement =
                        connection.prepareStatement(sql)) {
            bindSearch(statement, 1, query);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private record CustomerRows(
            List<CustomerDTO> items, int totalCount) {
    }

    // 고객사 상세 정보 조회 (상세 테이블 기준)
    public CustomerDTO getCustomerByName(String customerName) {
        try (Connection connection = connectionProvider.getConnection()) {
            String sql = "SELECT " + CUSTOMER_COLUMNS
                    + " FROM vertica_customer_detail d "
                    + "WHERE d.customer_name = ? AND d.is_deleted = "
                    + ACTIVE_FLAG;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, customerName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? mapCustomer(resultSet) : null;
                }
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        }
    }

    // 고객사 정보 업데이트 (상세 테이블 기준)
    public boolean updateCustomer(CustomerDTO customer) {
        return updateCustomer(customer, null);
    }

    public boolean updateCustomer(CustomerDTO customer, String actorUserId) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean auditAvailable = CustomerAuditSupport.shouldAuditWrite(
                    connection, schemaCapabilities, actorUserId);
            String sql = "UPDATE vertica_customer_detail SET db_name = ?, vertica_version = ?, db_mode = ?, os_info = ?, "
                    + "node_count = ?, license_info = ?, main_manager = ?, sub_manager = ?, said = ?, customer_type = ? "
                    + (auditAvailable
                            ? ", updated_at = CURRENT_TIMESTAMP, updated_by = ? "
                            : "")
                    + "WHERE customer_name = ? AND is_deleted = "
                    + ACTIVE_FLAG;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int nextParameter = bindMutableCustomerFields(
                        statement, 1, customer);
                if (auditAvailable) {
                    statement.setString(nextParameter++, actorUserId.trim());
                }
                statement.setString(nextParameter, customer.getCustomerName());
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        }
    }

    // 새 고객사 추가 (상세 테이블에 삽입)
    public boolean addCustomer(CustomerDTO customer) {
        return addCustomer(customer, null);
    }

    public boolean addCustomer(CustomerDTO customer, String actorUserId) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean auditAvailable = CustomerAuditSupport.shouldAuditWrite(
                    connection, schemaCapabilities, actorUserId);
            String columns = "customer_name, db_name, vertica_version, db_mode, os_info, node_count, license_info, main_manager, sub_manager, said, customer_type, is_deleted";
            String values = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                    + ACTIVE_FLAG;
            if (auditAvailable) {
                columns += ", updated_at, updated_by";
                values += ", CURRENT_TIMESTAMP, ?";
            }
            String sql = "INSERT INTO vertica_customer_detail (" + columns
                    + ") VALUES (" + values + ")";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, customer.getCustomerName());
                int nextParameter = bindMutableCustomerFields(
                        statement, 2, customer);
                if (auditAvailable) {
                    statement.setString(nextParameter, actorUserId.trim());
                }
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        }
    }

    // 고객사 삭제 (상세 테이블에서 비활성)
    public boolean deleteCustomer(String customerName) {
        return deleteCustomer(customerName, null);
    }

    public boolean deleteCustomer(String customerName, String actorUserId) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean auditAvailable = CustomerAuditSupport.shouldAuditWrite(
                    connection, schemaCapabilities, actorUserId);
            String sql = "UPDATE vertica_customer_detail SET is_deleted = "
                    + DELETED_FLAG
                    + (auditAvailable
                            ? ", deleted_at = CURRENT_TIMESTAMP, deleted_by = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ? "
                            : " ")
                    + "WHERE customer_name = ? "
                    + "AND is_deleted = " + ACTIVE_FLAG;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int nextParameter = 1;
                if (auditAvailable) {
                    statement.setString(nextParameter++, actorUserId.trim());
                    statement.setString(nextParameter++, actorUserId.trim());
                }
                statement.setString(nextParameter, customerName);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        }
    }

    private int bindMutableCustomerFields(
            PreparedStatement statement,
            int startIndex,
            CustomerDTO customer) throws SQLException {
        int parameter = startIndex;
        setStringOrNull(statement, parameter++, customer.getDbName());
        setStringOrNull(statement, parameter++, customer.getVerticaVersion());
        setStringOrNull(statement, parameter++, customer.getMode());
        setStringOrNull(statement, parameter++, customer.getOs());
        setStringOrNull(statement, parameter++, customer.getNodes());
        setStringOrNull(statement, parameter++, customer.getLicenseSize());
        setStringOrNull(statement, parameter++, customer.getManagerName());
        setStringOrNull(statement, parameter++, customer.getSubManagerName());
        setStringOrNull(statement, parameter++, customer.getSaid());
        setStringOrNull(statement, parameter++, customer.getCustomerType());
        return parameter;
    }

    private static CustomerDTO mapCustomer(ResultSet resultSet)
            throws SQLException {
        CustomerDTO customer = new CustomerDTO();
        customer.setCustomerName(
                resultSet.getString("customer_name"));
        customer.setDbName(resultSet.getString("db_name"));
        customer.setVerticaVersion(
                resultSet.getString("vertica_version"));
        customer.setMode(resultSet.getString("db_mode"));
        customer.setOs(resultSet.getString("os_info"));
        customer.setNodes(resultSet.getString("node_count"));
        customer.setLicenseSize(
                resultSet.getString("license_info"));
        customer.setSaid(resultSet.getString("said"));
        customer.setManagerName(
                resultSet.getString("main_manager"));
        customer.setSubManagerName(
                resultSet.getString("sub_manager"));
        customer.setCustomerType(
                resultSet.getString("customer_type"));
        return customer;
    }

    private static String selectionPredicate(
            String filter, String query) {
        String filterPredicate = MAINTENANCE_FILTER.equals(filter)
                ? "d.customer_type = '" + MAINTENANCE_CUSTOMER_TYPE + "'"
                : "1 = 1";
        return query == null
                ? filterPredicate
                : filterPredicate + " AND " + SEARCH_PREDICATE;
    }

    private static int bindSearch(
            PreparedStatement statement,
            int startIndex,
            String query) throws SQLException {
        if (query == null) {
            return startIndex;
        }
        String like = "%" + query + "%";
        int parameterIndex = startIndex;
        for (int field = 0; field < 6; field++) {
            statement.setString(parameterIndex++, like);
        }
        return parameterIndex;
    }

    private static String normalizeQuery(String query) {
        return SearchQueryPolicy.normalize(query);
    }

    private static String sortColumn(String sortField) {
        if (sortField == null) {
            return "d.customer_name";
        }
        return switch (sortField) {
            case "vertica_version" -> "d.vertica_version";
            case "mode" -> "d.db_mode";
            case "os" -> "d.os_info";
            case "nodes" -> "d.node_count";
            case "license_size" -> "d.license_info";
            case "said" -> "d.said";
            case "manager_name" -> "d.main_manager";
            default -> "d.customer_name";
        };
    }

    private void setStringOrNull(PreparedStatement pstmt, int parameterIndex, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            pstmt.setNull(parameterIndex, Types.VARCHAR);
        } else {
            pstmt.setString(parameterIndex, value.trim());
        }
    }
}
