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

public class CustomerDAO {
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
                    + "OR CAST(d.node_count AS VARCHAR(65000)) ILIKE ? "
                    + "OR CAST(d.license_info AS VARCHAR(65000)) ILIKE ? "
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
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();

            if (sortField == null || sortField.isEmpty()) {
                sortField = "customer_name";
            }
            if (sortDirection == null || sortDirection.isEmpty()) {
                sortDirection = "ASC";
            }

            String direction = "ASC";
            if ("DESC".equalsIgnoreCase(sortDirection)) {
                direction = "DESC";
            }

            String orderByColumn;
            switch (sortField) {
                case "customer_name":
                    orderByColumn = "d.customer_name";
                    break;
                case "vertica_version":
                    orderByColumn = "d.vertica_version";
                    break;
                case "mode":
                    orderByColumn = "d.db_mode";
                    break;
                case "os":
                    orderByColumn = "d.os_info";
                    break;
                case "nodes":
                    orderByColumn = "d.node_count";
                    break;
                case "license_size":
                    orderByColumn = "d.license_info";
                    break;
                case "said":
                    orderByColumn = "d.said";
                    break;
                case "manager_name":
                    orderByColumn = "d.main_manager";
                    break;
                default:
                    orderByColumn = "d.customer_name";
                    break;
            }

            String sql =
                "SELECT d.customer_name, d.vertica_version, d.db_mode, d.os_info, d.node_count, d.license_info, d.said, d.main_manager, d.sub_manager, d.db_name, d.customer_type " +
                "FROM vertica_customer_detail d WHERE d.is_deleted = 1";
            if ("maintenance".equals(filter)) {
                sql += " AND d.customer_type = '정기점검 계약 고객사'";
            }
            sql += " ORDER BY " + orderByColumn + " " + direction;

            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                CustomerDTO customer = new CustomerDTO();
                customer.setCustomerName(rs.getString("customer_name"));
                customer.setDbName(rs.getString("db_name"));
                customer.setVerticaVersion(rs.getString("vertica_version"));
                customer.setMode(rs.getString("db_mode"));
                customer.setOs(rs.getString("os_info"));
                customer.setNodes(rs.getString("node_count"));
                customer.setLicenseSize(rs.getString("license_info"));
                customer.setSaid(rs.getString("said"));
                customer.setManagerName(rs.getString("main_manager"));
                customer.setSubManagerName(rs.getString("sub_manager"));
                customer.setCustomerType(rs.getString("customer_type"));

                customerList.add(customer);
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return customerList;
    }

    // 기존 호환성을 위한 오버로드 메소드 (기본값: 전체 보기)
    public List<CustomerDTO> getAllCustomers(String sortField, String sortDirection) {
        return getAllCustomers(sortField, sortDirection, "all");
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
                + "WHERE d.is_deleted = 1 AND d.customer_type = ? "
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
                            + "WHERE d.is_deleted = 1";
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
                    + "WHERE d.is_deleted = 1 AND "
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
                + "WHERE d.is_deleted = 1 AND " + selectionPredicate;
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
        CustomerDTO customer = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT customer_name, vertica_version, db_mode, os_info, node_count, license_info, said, main_manager, sub_manager, db_name, customer_type " +
                         "FROM vertica_customer_detail WHERE customer_name = ? AND is_deleted = 1";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, customerName);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                customer = new CustomerDTO();
                customer.setCustomerName(rs.getString("customer_name"));
                customer.setDbName(rs.getString("db_name"));
                customer.setVerticaVersion(rs.getString("vertica_version"));
                customer.setMode(rs.getString("db_mode"));
                customer.setOs(rs.getString("os_info"));
                customer.setNodes(rs.getString("node_count"));
                customer.setLicenseSize(rs.getString("license_info"));
                customer.setManagerName(rs.getString("main_manager"));
                customer.setSubManagerName(rs.getString("sub_manager"));
                customer.setSaid(rs.getString("said"));
                customer.setCustomerType(rs.getString("customer_type"));
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return customer;
    }

    // 고객사 정보 업데이트 (상세 테이블 기준)
    public boolean updateCustomer(CustomerDTO customer) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "UPDATE vertica_customer_detail SET db_name = ?, vertica_version = ?, db_mode = ?, os_info = ?, "
                    + "node_count = ?, license_info = ?, main_manager = ?, sub_manager = ?, said = ?, customer_type = ? "
                    + "WHERE customer_name = ? AND is_deleted = 1";

            pstmt = conn.prepareStatement(sql);
            setStringOrNull(pstmt, 1, customer.getDbName());
            setStringOrNull(pstmt, 2, customer.getVerticaVersion());
            setStringOrNull(pstmt, 3, customer.getMode());
            setStringOrNull(pstmt, 4, customer.getOs());
            setStringOrNull(pstmt, 5, customer.getNodes());
            setStringOrNull(pstmt, 6, customer.getLicenseSize());
            setStringOrNull(pstmt, 7, customer.getManagerName());
            setStringOrNull(pstmt, 8, customer.getSubManagerName());
            setStringOrNull(pstmt, 9, customer.getSaid());
            setStringOrNull(pstmt, 10, customer.getCustomerType());
            pstmt.setString(11, customer.getCustomerName());

            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);

        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }

        return success;
    }

    // 새 고객사 추가 (상세 테이블에 삽입)
    public boolean addCustomer(CustomerDTO customer) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO vertica_customer_detail (customer_name, db_name, vertica_version, db_mode, os_info, node_count, license_info, main_manager, sub_manager, said, customer_type, is_deleted) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, customer.getCustomerName());
            setStringOrNull(pstmt, 2, customer.getDbName());
            setStringOrNull(pstmt, 3, customer.getVerticaVersion());
            setStringOrNull(pstmt, 4, customer.getMode());
            setStringOrNull(pstmt, 5, customer.getOs());
            setStringOrNull(pstmt, 6, customer.getNodes());
            setStringOrNull(pstmt, 7, customer.getLicenseSize());
            setStringOrNull(pstmt, 8, customer.getManagerName());
            setStringOrNull(pstmt, 9, customer.getSubManagerName());
            setStringOrNull(pstmt, 10, customer.getSaid());
            setStringOrNull(pstmt, 11, customer.getCustomerType());

            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);

        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }

        return success;
    }

    // 고객사 삭제 (상세 테이블에서 비활성)
    public boolean deleteCustomer(String customerName) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "UPDATE vertica_customer_detail SET is_deleted = 0 "
                    + "WHERE customer_name = ? AND is_deleted = 1";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, customerName);

            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);

        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }

        return success;
    }

    // 빈 문자열을 NULL로 처리하는 헬퍼 메서드
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
        for (int field = 0; field < 8; field++) {
            statement.setString(parameterIndex++, like);
        }
        return parameterIndex;
    }

    private static String normalizeQuery(String query) {
        return query == null || query.trim().isEmpty()
                ? null
                : query.trim();
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
