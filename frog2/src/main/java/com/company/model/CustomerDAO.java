package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.company.util.DBConnection;
import com.company.util.Pagination;
import com.company.util.SearchQueryPolicy;

public class CustomerDAO {
    private static final int ACTIVE_FLAG = 1;
    private static final int DELETED_FLAG = 0;
    private static final SchemaCapabilityCache APPLICATION_SCHEMA_CAPABILITIES =
            new SchemaCapabilityCache();
    private static final String MAINTENANCE_FILTER = "maintenance";
    private static final String MAINTENANCE_CUSTOMER_TYPE =
            "정기점검 계약 고객사";
    private static final String CUSTOMER_COLUMNS =
            CustomerFieldContract.selectColumns("d");
    private static final String SEARCH_PREDICATE =
            "(CAST(d.customer_name AS VARCHAR(65000)) ILIKE ? ESCAPE '!' "
                    + "OR CAST(d.vertica_version AS VARCHAR(65000)) ILIKE ? ESCAPE '!' "
                    + "OR CAST(d.db_mode AS VARCHAR(65000)) ILIKE ? ESCAPE '!' "
                    + "OR CAST(d.os_info AS VARCHAR(65000)) ILIKE ? ESCAPE '!' "
                    + "OR CAST(d.said AS VARCHAR(65000)) ILIKE ? ESCAPE '!' "
                    + "OR CAST(d.main_manager AS VARCHAR(65000)) ILIKE ? ESCAPE '!')";

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
            sql += " ORDER BY " + sortOrder(sortField, direction)
                    + ", d.customer_name ASC";

            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    customerList.add(CustomerFieldContract.read(resultSet));
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

    public List<CustomerDTO> searchCustomers(String query, int limit) {
        if (limit <= 0 || limit > 20) {
            throw new IllegalArgumentException(
                    "Search limit must be between 1 and 20");
        }
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery == null) {
            return List.of();
        }
        String sql = "SELECT " + CUSTOMER_COLUMNS
                + " FROM vertica_customer_detail d "
                + "WHERE d.is_deleted = " + ACTIVE_FLAG
                + " AND " + SEARCH_PREDICATE
                + " ORDER BY d.customer_name ASC LIMIT ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = bindSearch(
                    statement, 1, normalizedQuery);
            statement.setInt(parameterIndex, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CustomerDTO> customers = new ArrayList<>();
                while (resultSet.next()) {
                    customers.add(CustomerFieldContract.read(resultSet));
                }
                return List.copyOf(customers);
            }
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "search active customers", exception);
        }
    }

    public boolean isActiveMaintenanceCustomer(String customerName) {
        CustomerDTO customer = getCustomerByName(customerName);
        return customer != null
                && MAINTENANCE_CUSTOMER_TYPE.equals(customer.getCustomerType());
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
                    + sortOrder(sortField, direction)
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
                    customers.add(CustomerFieldContract.read(resultSet));
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
                    return resultSet.next()
                            ? CustomerFieldContract.read(resultSet)
                            : null;
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
            CustomerAssignmentSupport.Capability assignmentCapability =
                    customerAssignmentCapability(connection);
            CustomerAssignmentSupport.AssignmentUserIds assignmentUserIds =
                    assignmentCapability
                            == CustomerAssignmentSupport.Capability.COMPLETE
                    ? CustomerAssignmentSupport.resolveUserIds(
                            connection,
                            customer.getManagerName(),
                            customer.getSubManagerName())
                    : null;
            String sql = "UPDATE vertica_customer_detail SET "
                    + CustomerFieldContract.mutableAssignments() + " "
                    + (assignmentUserIds == null
                            ? ""
                            : ", main_manager_user_id = ?, "
                                    + "sub_manager_user_id = ? ")
                    + (auditAvailable
                            ? ", updated_at = CURRENT_TIMESTAMP, updated_by = ? "
                            : "")
                    + "WHERE customer_name = ? AND is_deleted = "
                    + ACTIVE_FLAG;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int nextParameter = CustomerFieldContract.bindMutableFields(
                        statement, 1, customer);
                if (assignmentUserIds != null) {
                    nextParameter = CustomerAssignmentSupport.bindUserIds(
                            statement, nextParameter, assignmentUserIds);
                }
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
            CustomerAssignmentSupport.Capability assignmentCapability =
                    customerAssignmentCapability(connection);
            CustomerAssignmentSupport.AssignmentUserIds assignmentUserIds =
                    assignmentCapability
                            == CustomerAssignmentSupport.Capability.COMPLETE
                    ? CustomerAssignmentSupport.resolveUserIds(
                            connection,
                            customer.getManagerName(),
                            customer.getSubManagerName())
                    : null;
            String columns = CustomerFieldContract.insertColumns()
                    + ", is_deleted";
            String values = CustomerFieldContract.insertPlaceholders() + ", "
                    + ACTIVE_FLAG;
            if (assignmentUserIds != null) {
                columns += ", main_manager_user_id, sub_manager_user_id";
                values += ", ?, ?";
            }
            if (auditAvailable) {
                columns += ", updated_at, updated_by";
                values += ", CURRENT_TIMESTAMP, ?";
            }
            String sql = "INSERT INTO vertica_customer_detail (" + columns
                    + ") VALUES (" + values + ")";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int nextParameter = CustomerFieldContract.bindInsertFields(
                        statement, 1, customer);
                if (assignmentUserIds != null) {
                    nextParameter = CustomerAssignmentSupport.bindUserIds(
                            statement, nextParameter, assignmentUserIds);
                }
                if (auditAvailable) {
                    statement.setString(nextParameter, actorUserId.trim());
                }
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        }
    }

    private CustomerAssignmentSupport.Capability
            customerAssignmentCapability(Connection connection)
            throws SQLException {
        CustomerAssignmentSupport.Capability capability =
                CustomerAssignmentSupport.capability(
                        connection, schemaCapabilities);
        if (capability == CustomerAssignmentSupport.Capability.PARTIAL) {
            throw new SQLException(
                    "Customer assignment user-ID columns are partially applied");
        }
        return capability;
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
        String like = SearchQueryPolicy.literalContainsLikePattern(query);
        int parameterIndex = startIndex;
        for (int field = 0; field < 6; field++) {
            statement.setString(parameterIndex++, like);
        }
        return parameterIndex;
    }

    private static String normalizeQuery(String query) {
        return SearchQueryPolicy.normalize(query);
    }

    private static String sortOrder(String sortField, String direction) {
        String safeDirection = "DESC".equalsIgnoreCase(direction)
                ? "DESC"
                : "ASC";
        if (sortField == null) {
            return "d.customer_name " + safeDirection;
        }
        return switch (sortField) {
            case "vertica_version" -> versionSortOrder(safeDirection);
            case "mode" -> nullableTextSortOrder("d.db_mode", safeDirection);
            case "os" -> nullableTextSortOrder("d.os_info", safeDirection);
            case "nodes" -> numericTextSortOrder("d.node_count", safeDirection);
            case "license_size" -> licenseSortOrder(safeDirection);
            case "said" -> nullableTextSortOrder("d.said", safeDirection);
            case "manager_name" -> nullableTextSortOrder(
                    "d.main_manager", safeDirection);
            default -> "d.customer_name " + safeDirection;
        };
    }

    private static String nullableTextSortOrder(
            String column, String direction) {
        return "CASE WHEN NULLIF(TRIM(" + column + "), '') IS NULL "
                + "THEN 1 ELSE 0 END ASC, " + column + " " + direction;
    }

    private static String numericTextSortOrder(
            String column, String direction) {
        String numericValue = numericPart(column);
        return "CASE WHEN " + numericValue + " IS NULL THEN 1 ELSE 0 END ASC, "
                + numericValue + " " + direction;
    }

    private static String licenseSortOrder(String direction) {
        String column = "d.license_info";
        String numericValue = numericPart(column);
        String unit = "LOWER(CAST(NULLIF(REGEXP_SUBSTR(CAST(" + column
                + " AS VARCHAR(65000)), '[[:alpha:]]+'), '') AS VARCHAR(64)))";
        return "CASE WHEN " + numericValue + " IS NULL THEN 1 ELSE 0 END ASC, "
                + unit + " " + direction + ", "
                + numericValue + " " + direction;
    }

    private static String versionSortOrder(String direction) {
        String column = "CAST(d.vertica_version AS VARCHAR(65000))";
        String major = versionPart(column, 1, 1);
        String minor = versionPart(column, 2, 1);
        String patch = versionPart(column, 3, 1);
        String build = versionPart(column, 3, 2);
        return "CASE WHEN NULLIF(TRIM(d.vertica_version), '') IS NULL "
                + "THEN 1 ELSE 0 END ASC, "
                + major + " " + direction + ", "
                + minor + " " + direction + ", "
                + patch + " " + direction + ", "
                + build + " " + direction;
    }

    private static String versionPart(
            String column, int dotPart, int hyphenPart) {
        String component = "SPLIT_PART(SPLIT_PART(" + column + ", '.', "
                + dotPart + "), '-', " + hyphenPart + ")";
        return "CAST(NULLIF(REGEXP_SUBSTR(" + component
                + ", '[0-9]+'), '') AS INTEGER)";
    }

    private static String numericPart(String column) {
        return "CAST(NULLIF(REGEXP_SUBSTR(CAST(" + column
                + " AS VARCHAR(65000)), '[0-9]+([.][0-9]+)?'), '') "
                + "AS NUMERIC)";
    }

}
