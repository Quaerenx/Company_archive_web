package com.company.model;

import com.company.util.Pagination;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.company.util.DBConnection;

public class MaintenanceRecordDAO {
    private static final String TABLE_NAME = "maintenance_records";
    private static final String CREATOR_USER_ID_COLUMN = "created_by_user_id";
    private static final SchemaCapabilityCache APPLICATION_SCHEMA_CAPABILITIES =
            new SchemaCapabilityCache();
    private static final String BASE_SELECT_COLUMNS =
            "maintenance_id, customer_name, inspector_name, inspection_date, "
                    + "vertica_version, note, created_at, updated_at";

    private final JdbcConnectionProvider connectionProvider;
    private final SchemaCapabilityCache schemaCapabilities;

    public MaintenanceRecordDAO() {
        this(DBConnection::getConnection, APPLICATION_SCHEMA_CAPABILITIES);
    }

    MaintenanceRecordDAO(SchemaCapabilityCache schemaCapabilities) {
        this(DBConnection::getConnection, schemaCapabilities);
    }

    MaintenanceRecordDAO(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache schemaCapabilities) {
        this.connectionProvider = Objects.requireNonNull(
                connectionProvider, "connectionProvider");
        this.schemaCapabilities = Objects.requireNonNull(
                schemaCapabilities, "schemaCapabilities");
    }

    public boolean addMaintenanceRecord(MaintenanceRecordDTO record) {
        if (record == null || isBlank(record.getCreatorUserId())) {
            return false;
        }
        try (Connection conn = connectionProvider.getConnection()) {
            if (!columnExists(conn, TABLE_NAME, CREATOR_USER_ID_COLUMN)) {
                return false;
            }
            boolean hasSize = columnExists(conn, "maintenance_records", "license_size_gb");
            boolean hasUsagePct = columnExists(conn, "maintenance_records", "license_usage_pct");
            boolean hasUsageSize = columnExists(conn, "maintenance_records", "license_usage_size");

            List<String> cols = new ArrayList<>();
            cols.add("customer_name");
            cols.add("inspector_name");
            cols.add(CREATOR_USER_ID_COLUMN);
            cols.add("inspection_date");
            cols.add("vertica_version");
            cols.add("note");

            if (hasSize) cols.add("license_size_gb");
            if (hasUsageSize) cols.add("license_usage_size");
            if (hasUsagePct) {
                cols.add("license_usage_pct");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO maintenance_records (");
            sb.append(String.join(", ", cols));
            sb.append(") VALUES (");
            for (int i = 0; i < cols.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("?");
            }
            sb.append(")");

            try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
                int idx = 1;
                pstmt.setString(idx++, record.getCustomerName());
                pstmt.setString(idx++, record.getInspectorName());
                pstmt.setString(idx++, record.getCreatorUserId().trim());
                pstmt.setDate(idx++, record.getInspectionDate());
                setStringOrNull(pstmt, idx++, record.getVerticaVersion());
                setStringOrNull(pstmt, idx++, record.getNote());
                if (hasSize) setStringOrNull(pstmt, idx++, record.getLicenseSizeGb());
                if (hasUsageSize) setStringOrNull(pstmt, idx++, record.getLicenseUsageSize());
                if (hasUsagePct) setStringOrNull(pstmt, idx++, record.getLicenseUsagePct());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public boolean updateMaintenanceRecordForOwner(
            MaintenanceRecordDTO record, String creatorUserId) {
        if (record == null
                || record.getMaintenanceId() == null
                || isBlank(creatorUserId)) {
            return false;
        }
        try (Connection conn = connectionProvider.getConnection()) {
            if (!columnExists(conn, TABLE_NAME, CREATOR_USER_ID_COLUMN)) {
                return false;
            }
            boolean hasSize = columnExists(conn, "maintenance_records", "license_size_gb");
            boolean hasUsagePct = columnExists(conn, "maintenance_records", "license_usage_pct");
            boolean hasUsageSize = columnExists(conn, "maintenance_records", "license_usage_size");

            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE maintenance_records SET ");
            sb.append("customer_name = ?, inspector_name = ?, inspection_date = ?, vertica_version = ?, note = ?");
            if (hasSize) sb.append(", license_size_gb = ?");
            if (hasUsageSize) sb.append(", license_usage_size = ?");
            if (hasUsagePct) sb.append(", license_usage_pct = ?");
            sb.append(", updated_at = statement_timestamp() ");
            sb.append("WHERE maintenance_id = ? AND created_by_user_id = ?");

            try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
                int idx = 1;
                pstmt.setString(idx++, record.getCustomerName());
                pstmt.setString(idx++, record.getInspectorName());
                pstmt.setDate(idx++, record.getInspectionDate());
                setStringOrNull(pstmt, idx++, record.getVerticaVersion());
                setStringOrNull(pstmt, idx++, record.getNote());
                if (hasSize) setStringOrNull(pstmt, idx++, record.getLicenseSizeGb());
                if (hasUsageSize) setStringOrNull(pstmt, idx++, record.getLicenseUsageSize());
                if (hasUsagePct) setStringOrNull(pstmt, idx++, record.getLicenseUsagePct());
                pstmt.setLong(idx++, record.getMaintenanceId());
                pstmt.setString(idx, creatorUserId.trim());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public boolean deleteMaintenanceRecordForOwner(
            Long maintenanceId, String creatorUserId) {
        if (maintenanceId == null || isBlank(creatorUserId)) {
            return false;
        }
        try (Connection conn = connectionProvider.getConnection()) {
            if (!columnExists(conn, TABLE_NAME, CREATOR_USER_ID_COLUMN)) {
                return false;
            }
            String sql = "DELETE FROM maintenance_records "
                    + "WHERE maintenance_id = ? AND created_by_user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, maintenanceId);
                pstmt.setString(2, creatorUserId.trim());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public MaintenanceRecordDTO getMaintenanceRecordById(Long maintenanceId) {
        try (Connection conn = connectionProvider.getConnection()) {
            boolean hasSize = columnExists(conn, "maintenance_records", "license_size_gb");
            boolean hasUsagePct = columnExists(conn, "maintenance_records", "license_usage_pct");
            boolean hasUsageSize = columnExists(conn, "maintenance_records", "license_usage_size");
            boolean hasCreatorUserId = columnExists(
                    conn, TABLE_NAME, CREATOR_USER_ID_COLUMN);

            String sql = "SELECT " + selectColumns(
                    hasSize, hasUsagePct, hasUsageSize, hasCreatorUserId)
                    + " FROM maintenance_records WHERE maintenance_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, maintenanceId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToDto(
                                rs,
                                hasSize,
                                hasUsagePct,
                                hasUsageSize,
                                hasCreatorUserId);
                    }
                }
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        }
        return null;
    }

    public PageResult<MaintenanceRecordDTO> getMaintenanceRecordsByCustomer(
            String customerName,
            int requestedPage,
            int pageSize) {
        return getMaintenanceRecordsByCustomer(
                customerName,
                requestedPage,
                pageSize,
                MaintenanceHistoryFilter.empty());
    }

    public MaintenanceFormHistoryContext getMaintenanceFormHistoryContext(
            String customerName,
            Date inspectionDate,
            Long excludedMaintenanceId) {
        if (isBlank(customerName)) {
            return MaintenanceFormHistoryContext.empty();
        }
        try (Connection connection = connectionProvider.getConnection()) {
            boolean hasSize = columnExists(
                    connection, TABLE_NAME, "license_size_gb");
            boolean hasUsagePct = columnExists(
                    connection, TABLE_NAME, "license_usage_pct");
            boolean hasUsageSize = columnExists(
                    connection, TABLE_NAME, "license_usage_size");
            boolean hasCreatorUserId = columnExists(
                    connection, TABLE_NAME, CREATOR_USER_ID_COLUMN);
            Date monthStart = inspectionDate == null
                    ? null
                    : Date.valueOf(inspectionDate.toLocalDate()
                            .withDayOfMonth(1));
            Date nextMonthStart = monthStart == null
                    ? null
                    : Date.valueOf(monthStart.toLocalDate().plusMonths(1));

            MaintenanceRecordDTO previous = loadFormContextRecord(
                    connection,
                    customerName,
                    monthStart,
                    null,
                    excludedMaintenanceId,
                    hasSize,
                    hasUsagePct,
                    hasUsageSize,
                    hasCreatorUserId);
            MaintenanceRecordDTO duplicate = monthStart == null
                    ? null
                    : loadFormContextRecord(
                            connection,
                            customerName,
                            monthStart,
                            nextMonthStart,
                            excludedMaintenanceId,
                            hasSize,
                            hasUsagePct,
                            hasUsageSize,
                            hasCreatorUserId);
            return new MaintenanceFormHistoryContext(previous, duplicate);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "load maintenance form history context", exception);
        }
    }

    private MaintenanceRecordDTO loadFormContextRecord(
            Connection connection,
            String customerName,
            Date rangeStart,
            Date rangeEnd,
            Long excludedMaintenanceId,
            boolean hasSize,
            boolean hasUsagePct,
            boolean hasUsageSize,
            boolean hasCreatorUserId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(selectColumns(
                        hasSize,
                        hasUsagePct,
                        hasUsageSize,
                        hasCreatorUserId))
                .append(" FROM maintenance_records WHERE customer_name = ?");
        if (rangeStart != null && rangeEnd == null) {
            sql.append(" AND inspection_date < ?");
        } else if (rangeStart != null) {
            sql.append(" AND inspection_date >= ? AND inspection_date < ?");
        }
        if (excludedMaintenanceId != null) {
            sql.append(" AND maintenance_id <> ?");
        }
        sql.append(" ORDER BY CASE WHEN inspection_date IS NULL THEN 1 ELSE 0 END, ")
                .append("inspection_date DESC, maintenance_id DESC LIMIT 1");

        try (PreparedStatement statement =
                     connection.prepareStatement(sql.toString())) {
            int parameter = 1;
            statement.setString(parameter++, customerName.trim());
            if (rangeStart != null) {
                statement.setDate(parameter++, rangeStart);
            }
            if (rangeEnd != null) {
                statement.setDate(parameter++, rangeEnd);
            }
            if (excludedMaintenanceId != null) {
                statement.setLong(parameter, excludedMaintenanceId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapRowToDto(
                        resultSet,
                        hasSize,
                        hasUsagePct,
                        hasUsageSize,
                        hasCreatorUserId);
            }
        }
    }

    public PageResult<MaintenanceRecordDTO> getMaintenanceRecordsByCustomer(
            String customerName,
            int requestedPage,
            int pageSize,
            MaintenanceHistoryFilter filter) {
        Pagination.totalPages(0, pageSize);
        Objects.requireNonNull(filter, "filter");
        if (isBlank(customerName)) {
            return new PageResult<>(List.of(), 0, 1, pageSize);
        }

        try (Connection connection = connectionProvider.getConnection()) {
            boolean hasSize = columnExists(
                    connection, TABLE_NAME, "license_size_gb");
            boolean hasUsagePct = columnExists(
                    connection, TABLE_NAME, "license_usage_pct");
            boolean hasUsageSize = columnExists(
                    connection, TABLE_NAME, "license_usage_size");
            boolean hasCreatorUserId = columnExists(
                    connection, TABLE_NAME, CREATOR_USER_ID_COLUMN);
            int page = Math.max(1, requestedPage);
            CustomerHistoryRows rows;
            try {
                rows = loadCustomerHistoryRows(
                        connection,
                        customerName,
                        page,
                        pageSize,
                        filter,
                        hasSize,
                        hasUsagePct,
                        hasUsageSize,
                        hasCreatorUserId);
            } catch (ArithmeticException exception) {
                rows = new CustomerHistoryRows(List.of(), 0);
            }

            if (!rows.items().isEmpty() || page == 1) {
                return new PageResult<>(
                        rows.items(), rows.totalCount(), page, pageSize);
            }

            int totalCount = countCustomerHistory(
                    connection, customerName, filter);
            int correctedPage = Pagination.clampPage(
                    page, Pagination.totalPages(totalCount, pageSize));
            if (totalCount == 0) {
                return new PageResult<>(
                        List.of(), 0, correctedPage, pageSize);
            }
            CustomerHistoryRows correctedRows = loadCustomerHistoryRows(
                    connection,
                    customerName,
                    correctedPage,
                    pageSize,
                    filter,
                    hasSize,
                    hasUsagePct,
                    hasUsageSize,
                    hasCreatorUserId);
            return new PageResult<>(
                    correctedRows.items(),
                    correctedRows.totalCount(),
                    correctedPage,
                    pageSize);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "load maintenance page by customer", exception);
        }
    }

    private CustomerHistoryRows loadCustomerHistoryRows(
            Connection connection,
            String customerName,
            int page,
            int pageSize,
            MaintenanceHistoryFilter filter,
            boolean hasSize,
            boolean hasUsagePct,
            boolean hasUsageSize,
            boolean hasCreatorUserId) throws SQLException {
        String sql = "SELECT "
                + selectColumns(
                        hasSize,
                        hasUsagePct,
                        hasUsageSize,
                        hasCreatorUserId)
                + ", COUNT(*) OVER () AS total_count "
                + "FROM maintenance_records WHERE "
                + customerHistoryPredicate(filter) + " "
                + "ORDER BY CASE WHEN inspection_date IS NULL THEN 1 ELSE 0 END, "
                + "inspection_date DESC, maintenance_id DESC LIMIT ? OFFSET ?";
        List<MaintenanceRecordDTO> records = new ArrayList<>();
        int totalCount = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = bindCustomerHistoryPredicate(
                    statement, customerName, filter);
            statement.setInt(parameter++, pageSize);
            statement.setInt(parameter, Pagination.offset(page, pageSize));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (records.isEmpty()) {
                        totalCount = resultSet.getInt("total_count");
                    }
                    records.add(mapRowToDto(
                            resultSet,
                            hasSize,
                            hasUsagePct,
                            hasUsageSize,
                            hasCreatorUserId));
                }
            }
        }
        return new CustomerHistoryRows(records, totalCount);
    }

    private static int countCustomerHistory(
            Connection connection,
            String customerName,
            MaintenanceHistoryFilter filter) throws SQLException {
        String sql = "SELECT COUNT(*) FROM maintenance_records "
                + "WHERE " + customerHistoryPredicate(filter);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCustomerHistoryPredicate(statement, customerName, filter);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private static String customerHistoryPredicate(
            MaintenanceHistoryFilter filter) {
        StringBuilder predicate = new StringBuilder("customer_name = ?");
        if (filter.year() != null) {
            predicate.append(
                    " AND inspection_date >= ? AND inspection_date < ?");
        }
        if (filter.version() != null) {
            predicate.append(
                    " AND vertica_version ILIKE ? ESCAPE '!'");
        }
        if (filter.query() != null) {
            predicate.append(
                    " AND (inspector_name ILIKE ? ESCAPE '!'"
                            + " OR vertica_version ILIKE ? ESCAPE '!'"
                            + " OR CAST(SUBSTR(note,1,65000) "
                            + "AS VARCHAR(65000)) ILIKE "
                            + "CAST(? AS VARCHAR(65000)) ESCAPE '!')");
        }
        return predicate.toString();
    }

    private static int bindCustomerHistoryPredicate(
            PreparedStatement statement,
            String customerName,
            MaintenanceHistoryFilter filter) throws SQLException {
        int parameter = 1;
        statement.setString(parameter++, customerName);
        if (filter.year() != null) {
            statement.setDate(parameter++, filter.yearStart());
            statement.setDate(parameter++, filter.yearEndExclusive());
        }
        if (filter.version() != null) {
            statement.setString(
                    parameter++, filter.versionLikePattern());
        }
        if (filter.query() != null) {
            String queryPattern = filter.queryLikePattern();
            statement.setString(parameter++, queryPattern);
            statement.setString(parameter++, queryPattern);
            statement.setString(parameter++, queryPattern);
        }
        return parameter;
    }

    public List<MaintenanceRecordDTO> getMaintenanceRecordsByMonth(Date startDate, Date endDate) {
        List<MaintenanceRecordDTO> records = new ArrayList<>();
        try (Connection conn = connectionProvider.getConnection()) {
            boolean hasSize = columnExists(conn, "maintenance_records", "license_size_gb");
            boolean hasUsagePct = columnExists(conn, "maintenance_records", "license_usage_pct");
            boolean hasUsageSize = columnExists(conn, "maintenance_records", "license_usage_size");

            String sql = "SELECT " + selectColumns(hasSize, hasUsagePct, hasUsageSize)
                    + " FROM maintenance_records " +
                    "WHERE inspection_date >= ? AND inspection_date < ? " +
                    "ORDER BY inspection_date ASC, customer_name ASC";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDate(1, startDate);
                pstmt.setDate(2, endDate);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        records.add(mapRowToDto(
                                rs, hasSize, hasUsagePct, hasUsageSize));
                    }
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }

        return records;
    }

    private void setStringOrNull(PreparedStatement pstmt, int parameterIndex, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            pstmt.setNull(parameterIndex, Types.VARCHAR);
        } else {
            pstmt.setString(parameterIndex, value.trim());
        }
    }

    private MaintenanceRecordDTO mapRowToDto(ResultSet rs, boolean hasSize, boolean hasUsagePct, boolean hasUsageSize) throws SQLException {
        return mapRowToDto(rs, hasSize, hasUsagePct, hasUsageSize, false);
    }

    private MaintenanceRecordDTO mapRowToDto(
            ResultSet rs,
            boolean hasSize,
            boolean hasUsagePct,
            boolean hasUsageSize,
            boolean hasCreatorUserId) throws SQLException {
        MaintenanceRecordDTO record = new MaintenanceRecordDTO();
        record.setMaintenanceId(rs.getLong("maintenance_id"));
        if (hasCreatorUserId) {
            record.setCreatorUserId(rs.getString(CREATOR_USER_ID_COLUMN));
        }
        record.setCustomerName(rs.getString("customer_name"));
        record.setInspectorName(rs.getString("inspector_name"));
        record.setInspectionDate(rs.getDate("inspection_date"));
        record.setVerticaVersion(rs.getString("vertica_version"));
        record.setNote(rs.getString("note"));
        record.setCreatedAt(rs.getTimestamp("created_at"));
        record.setUpdatedAt(rs.getTimestamp("updated_at"));

        if (hasSize) {
            record.setLicenseSizeGb(rs.getString("license_size_gb"));
        }
        if (hasUsageSize) {
            record.setLicenseUsageSize(rs.getString("license_usage_size"));
        }
        if (hasUsagePct) {
            record.setLicenseUsagePct(rs.getString("license_usage_pct"));
        }

        return record;
    }

    boolean columnExists(Connection conn, String tableName, String columnName) {
        return schemaCapabilities.columnExists(conn, tableName, columnName);
    }

    private static String selectColumns(
            boolean hasSize, boolean hasUsagePct, boolean hasUsageSize) {
        return selectColumns(hasSize, hasUsagePct, hasUsageSize, false);
    }

    private static String selectColumns(
            boolean hasSize,
            boolean hasUsagePct,
            boolean hasUsageSize,
            boolean hasCreatorUserId) {
        StringBuilder columns = new StringBuilder(BASE_SELECT_COLUMNS);
        if (hasCreatorUserId) {
            columns.append(", ").append(CREATOR_USER_ID_COLUMN);
        }
        if (hasSize) {
            columns.append(", license_size_gb");
        }
        if (hasUsageSize) {
            columns.append(", license_usage_size");
        }
        if (hasUsagePct) {
            columns.append(", license_usage_pct");
        }
        return columns.toString();
    }

    public PageResult<MaintenanceRecordDTO> getMaintenanceRecordsByOwner(
            String creatorUserId,
            int requestedPage,
            int pageSize) {
        Pagination.totalPages(0, pageSize);
        if (isBlank(creatorUserId)) {
            return new PageResult<>(List.of(), 0, 1, pageSize);
        }

        try (Connection connection = connectionProvider.getConnection()) {
            boolean hasCreatorUserId = columnExists(
                    connection, TABLE_NAME, CREATOR_USER_ID_COLUMN);
            if (!hasCreatorUserId) {
                return new PageResult<>(List.of(), 0, 1, pageSize);
            }
            boolean hasSize = columnExists(
                    connection, "maintenance_records", "license_size_gb");
            boolean hasUsagePct = columnExists(
                    connection, "maintenance_records", "license_usage_pct");
            boolean hasUsageSize = columnExists(
                    connection, "maintenance_records", "license_usage_size");

            String ownerColumn = CREATOR_USER_ID_COLUMN;
            String ownerValue = creatorUserId.trim();
            int totalCount;
            String countSql = "SELECT COUNT(*) FROM maintenance_records "
                    + "WHERE " + ownerColumn + " = ?";
            try (PreparedStatement statement =
                            connection.prepareStatement(countSql)) {
                statement.setString(1, ownerValue);
                try (ResultSet resultSet = statement.executeQuery()) {
                    totalCount = resultSet.next() ? resultSet.getInt(1) : 0;
                }
            }

            int totalPages = Pagination.totalPages(totalCount, pageSize);
            int page = Pagination.clampPage(requestedPage, totalPages);
            if (totalCount == 0) {
                return new PageResult<>(List.of(), 0, page, pageSize);
            }

            String itemSql = "SELECT "
                    + selectColumns(
                            hasSize,
                            hasUsagePct,
                            hasUsageSize,
                            hasCreatorUserId)
                    + " FROM maintenance_records WHERE "
                    + ownerColumn + " = ? "
                    + "ORDER BY CASE WHEN inspection_date IS NULL "
                    + "THEN 1 ELSE 0 END, inspection_date DESC, "
                    + "maintenance_id DESC LIMIT ? OFFSET ?";
            List<MaintenanceRecordDTO> records = new ArrayList<>();
            try (PreparedStatement statement =
                            connection.prepareStatement(itemSql)) {
                statement.setString(1, ownerValue);
                statement.setInt(2, pageSize);
                statement.setInt(
                        3, Pagination.offset(page, pageSize));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        records.add(mapRowToDto(
                                resultSet,
                                hasSize,
                                hasUsagePct,
                                hasUsageSize,
                                hasCreatorUserId));
                    }
                }
            }
            return new PageResult<>(
                    records, totalCount, page, pageSize);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "load maintenance page by owner", exception);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record CustomerHistoryRows(
            List<MaintenanceRecordDTO> items, int totalCount) {
    }

}
