package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.company.util.DBConnection;
import com.company.util.Pagination;
import com.company.util.SearchQueryPolicy;
import com.company.performance.RequestPerformanceContext;
import com.company.performance.RequestPerformanceContext.Operation;

public class TroubleshootingDAO {
    private static final String TABLE_NAME = "troubleshooting";
    private static final String CREATOR_USER_ID_COLUMN = "creator_user_id";
    private static final String SUMMARY_COLUMNS =
            "id, title, customer_name, occurrence_date, creator, create_date";
    private static final String SUMMARY_SEARCH_PREDICATE =
            "(title ILIKE ? ESCAPE '!' "
                    + "OR customer_name ILIKE ? ESCAPE '!' "
                    + "OR creator ILIKE ? ESCAPE '!')";
    private static final String CONTENT_SEARCH_PREDICATE =
            "(" + SUMMARY_SEARCH_PREDICATE
                    + " OR REGEXP_ILIKE(overview, ?) "
                    + "OR REGEXP_ILIKE(cause_analysis, ?) "
                    + "OR REGEXP_ILIKE(error_content, ?) "
                    + "OR REGEXP_ILIKE(action_taken, ?) "
                    + "OR REGEXP_ILIKE(script_content, ?) "
                    + "OR REGEXP_ILIKE(note, ?))";
    private static final String STABLE_SUMMARY_ORDER =
            " ORDER BY CASE WHEN occurrence_date IS NULL THEN 1 ELSE 0 END, "
                    + "occurrence_date DESC, create_date DESC, id DESC";
    private static final SchemaCapabilityCache APPLICATION_SCHEMA_CAPABILITIES =
            new SchemaCapabilityCache();

    private final JdbcConnectionProvider connectionProvider;
    private final SchemaCapabilityCache schemaCapabilities;

    public TroubleshootingDAO() {
        this(DBConnection::getConnection, APPLICATION_SCHEMA_CAPABILITIES);
    }

    TroubleshootingDAO(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache schemaCapabilities) {
        this.connectionProvider = Objects.requireNonNull(
                connectionProvider, "connectionProvider");
        this.schemaCapabilities = Objects.requireNonNull(
                schemaCapabilities, "schemaCapabilities");
    }

    public PageResult<TroubleshootingDTO> getTroubleshootingPage(
            String query, int requestedPage, int pageSize) {
        return getTroubleshootingPage(
                query, false, requestedPage, pageSize);
    }

    public PageResult<TroubleshootingDTO> getTroubleshootingPage(
            String query,
            boolean includeContent,
            int requestedPage,
            int pageSize) {
        String normalizedQuery = normalizedQuery(query);
        if (normalizedQuery != null) {
            RequestPerformanceContext.markOperation(includeContent
                    ? Operation.TROUBLESHOOTING_CONTENT_SEARCH
                    : Operation.TROUBLESHOOTING_SUMMARY_SEARCH);
        }
        String searchPredicate = includeContent
                ? CONTENT_SEARCH_PREDICATE
                : SUMMARY_SEARCH_PREDICATE;
        String whereClause =
                normalizedQuery == null ? "" : " WHERE " + searchPredicate;
        StatementBinder binder = normalizedQuery == null
                ? (statement, startIndex) -> startIndex
                : (statement, startIndex) ->
                        bindSearch(
                                statement,
                                startIndex,
                                normalizedQuery,
                                includeContent);

        try (Connection connection = connectionProvider.getConnection()) {
            return loadSummaryPage(
                    connection,
                    whereClause,
                    binder,
                    false,
                    requestedPage,
                    pageSize);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "load troubleshooting page", exception);
        }
    }

    public PageResult<TroubleshootingDTO> getTroubleshootingPageByOwner(
            String creatorUserId,
            int requestedPage,
            int pageSize) {
        if (isBlank(creatorUserId)) {
            return new PageResult<>(List.of(), 0, 1, pageSize);
        }
        try (Connection connection = connectionProvider.getConnection()) {
            boolean hasCreatorUserId = hasCreatorUserId(connection);
            if (!hasCreatorUserId) {
                return new PageResult<>(List.of(), 0, 1, pageSize);
            }
            StatementBinder binder = (statement, startIndex) -> {
                statement.setString(startIndex, creatorUserId.trim());
                return startIndex + 1;
            };
            return loadSummaryPage(
                    connection,
                    " WHERE " + CREATOR_USER_ID_COLUMN + " = ?",
                    binder,
                    true,
                    requestedPage,
                    pageSize);
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "load owned troubleshooting page", exception);
        }
    }

    public TroubleshootingDTO getTroubleshootingById(int id) {
        try (Connection conn = connectionProvider.getConnection()) {
            boolean hasCreatorUserId = hasCreatorUserId(conn);
            String sql = "SELECT id, title, customer_name, customer_manager, occurrence_date, "
                    + "work_personnel, work_period, creator, create_date, support_type, "
                    + "case_open_yn, overview, cause_analysis, error_content, action_taken, "
                    + "script_content, note, updated_date"
                    + (hasCreatorUserId ? ", creator_user_id " : " ")
                    + "FROM troubleshooting WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    TroubleshootingDTO ts = new TroubleshootingDTO();
                    ts.setId(rs.getInt("id"));
                    ts.setTitle(rs.getString("title"));
                    ts.setCustomerName(rs.getString("customer_name"));
                    ts.setCustomerManager(rs.getString("customer_manager"));

                    Timestamp occurrenceTs =
                            rs.getTimestamp("occurrence_date");
                    if (occurrenceTs != null) {
                        ts.setOccurrenceDate(new java.util.Date(
                                occurrenceTs.getTime()));
                    }

                    ts.setWorkPersonnel(rs.getString("work_personnel"));
                    ts.setWorkPeriod(rs.getString("work_period"));
                    ts.setCreator(rs.getString("creator"));
                    if (hasCreatorUserId) {
                        ts.setCreatorUserId(
                                rs.getString(CREATOR_USER_ID_COLUMN));
                    }

                    Timestamp createTs = rs.getTimestamp("create_date");
                    if (createTs != null) {
                        ts.setCreateDate(new java.util.Date(
                                createTs.getTime()));
                    }

                    ts.setSupportType(rs.getString("support_type"));
                    ts.setCaseOpenYn(rs.getString("case_open_yn"));
                    ts.setOverview(rs.getString("overview"));
                    ts.setCauseAnalysis(rs.getString("cause_analysis"));
                    ts.setErrorContent(rs.getString("error_content"));
                    ts.setActionTaken(rs.getString("action_taken"));
                    ts.setScriptContent(rs.getString("script_content"));
                    ts.setNote(rs.getString("note"));

                    Timestamp updatedTs = rs.getTimestamp("updated_date");
                    if (updatedTs != null) {
                        ts.setUpdatedDate(new java.util.Date(
                                updatedTs.getTime()));
                    }
                    return ts;
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.from("load troubleshooting", e);
        }
    }

    public boolean addTroubleshooting(TroubleshootingDTO ts) {
        try (Connection conn = connectionProvider.getConnection()) {
            if (!hasCreatorUserId(conn) || isBlank(ts.getCreatorUserId())) {
                return false;
            }
            String sql = "INSERT INTO troubleshooting ("
                    + "title, customer_name, customer_manager, "
                    + "occurrence_date, work_personnel, work_period, "
                    + "creator_user_id, creator, support_type, case_open_yn, "
                    + "overview, cause_analysis, error_content, "
                    + "action_taken, script_content, note) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, ts.getTitle());
                pstmt.setString(2, ts.getCustomerName());
                setStringOrNull(pstmt, 3, ts.getCustomerManager());

                if (ts.getOccurrenceDate() != null) {
                    pstmt.setTimestamp(4, new Timestamp(
                            ts.getOccurrenceDate().getTime()));
                } else {
                    pstmt.setNull(4, Types.TIMESTAMP);
                }

                setStringOrNull(pstmt, 5, ts.getWorkPersonnel());
                setStringOrNull(pstmt, 6, ts.getWorkPeriod());
                pstmt.setString(7, ts.getCreatorUserId().trim());
                pstmt.setString(8, ts.getCreator());
                setStringOrNull(pstmt, 9, ts.getSupportType());
                setStringOrNull(pstmt, 10, ts.getCaseOpenYn());
                setStringOrNull(pstmt, 11, ts.getOverview());
                setStringOrNull(pstmt, 12, ts.getCauseAnalysis());
                setStringOrNull(pstmt, 13, ts.getErrorContent());
                setStringOrNull(pstmt, 14, ts.getActionTaken());
                setStringOrNull(pstmt, 15, ts.getScriptContent());
                setStringOrNull(pstmt, 16, ts.getNote());

                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from("add troubleshooting", e);
        }
    }

    public boolean updateTroubleshootingForOwner(
            TroubleshootingDTO ts, String creatorUserId) {
        if (isBlank(creatorUserId)) {
            return false;
        }
        try (Connection conn = connectionProvider.getConnection()) {
            if (!hasCreatorUserId(conn)) {
                return false;
            }
            String sql = "UPDATE troubleshooting SET "
                    + "title = ?, customer_name = ?, customer_manager = ?, "
                    + "occurrence_date = ?, work_personnel = ?, "
                    + "work_period = ?, support_type = ?, case_open_yn = ?, "
                    + "overview = ?, cause_analysis = ?, error_content = ?, "
                    + "action_taken = ?, script_content = ?, note = ?, "
                    + "updated_date = NOW() "
                    + "WHERE id = ? AND creator_user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, ts.getTitle());
                pstmt.setString(2, ts.getCustomerName());
                setStringOrNull(pstmt, 3, ts.getCustomerManager());

                if (ts.getOccurrenceDate() != null) {
                    pstmt.setTimestamp(4, new Timestamp(
                            ts.getOccurrenceDate().getTime()));
                } else {
                    pstmt.setNull(4, Types.TIMESTAMP);
                }

                setStringOrNull(pstmt, 5, ts.getWorkPersonnel());
                setStringOrNull(pstmt, 6, ts.getWorkPeriod());
                setStringOrNull(pstmt, 7, ts.getSupportType());
                setStringOrNull(pstmt, 8, ts.getCaseOpenYn());
                setStringOrNull(pstmt, 9, ts.getOverview());
                setStringOrNull(pstmt, 10, ts.getCauseAnalysis());
                setStringOrNull(pstmt, 11, ts.getErrorContent());
                setStringOrNull(pstmt, 12, ts.getActionTaken());
                setStringOrNull(pstmt, 13, ts.getScriptContent());
                setStringOrNull(pstmt, 14, ts.getNote());
                pstmt.setInt(15, ts.getId());
                pstmt.setString(16, creatorUserId.trim());

                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from("update troubleshooting", e);
        }
    }

    public boolean deleteTroubleshootingForOwner(int id, String creatorUserId) {
        if (isBlank(creatorUserId)) {
            return false;
        }
        try (Connection conn = connectionProvider.getConnection()) {
            if (!hasCreatorUserId(conn)) {
                return false;
            }
            String sql = "DELETE FROM troubleshooting "
                    + "WHERE id = ? AND creator_user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                pstmt.setString(2, creatorUserId.trim());

                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from("delete troubleshooting", e);
        }
    }

    private boolean hasCreatorUserId(Connection connection) {
        return schemaCapabilities.columnExists(
                connection, TABLE_NAME, CREATOR_USER_ID_COLUMN);
    }

    private PageResult<TroubleshootingDTO> loadSummaryPage(
            Connection connection,
            String whereClause,
            StatementBinder binder,
            boolean includeCreatorUserId,
            int requestedPage,
            int pageSize) throws SQLException {
        Pagination.totalPages(0, pageSize);
        int page = Math.max(1, requestedPage);
        SummaryRows rows;
        try {
            rows = loadSummaryRows(
                    connection,
                    whereClause,
                    binder,
                    includeCreatorUserId,
                    page,
                    pageSize);
        } catch (ArithmeticException exception) {
            rows = new SummaryRows(List.of(), 0);
        }

        if (!rows.items().isEmpty() || page == 1) {
            return new PageResult<>(
                    rows.items(),
                    rows.totalCount(),
                    page,
                    pageSize);
        }

        int totalCount = countSummaryRows(
                connection, whereClause, binder);
        int correctedPage = Pagination.clampPage(
                page,
                Pagination.totalPages(totalCount, pageSize));
        if (totalCount == 0) {
            return new PageResult<>(
                    List.of(), 0, correctedPage, pageSize);
        }
        SummaryRows correctedRows = loadSummaryRows(
                connection,
                whereClause,
                binder,
                includeCreatorUserId,
                correctedPage,
                pageSize);
        return new PageResult<>(
                correctedRows.items(),
                correctedRows.totalCount(),
                correctedPage,
                pageSize);
    }

    private SummaryRows loadSummaryRows(
            Connection connection,
            String whereClause,
            StatementBinder binder,
            boolean includeCreatorUserId,
            int page,
            int pageSize) throws SQLException {
        String itemSql = "SELECT " + SUMMARY_COLUMNS
                + (includeCreatorUserId ? ", " + CREATOR_USER_ID_COLUMN : "")
                + ", COUNT(*) OVER () AS total_count"
                + " FROM " + TABLE_NAME
                + whereClause
                + STABLE_SUMMARY_ORDER
                + " LIMIT ? OFFSET ?";
        List<TroubleshootingDTO> items = new ArrayList<>();
        int totalCount = 0;
        try (PreparedStatement statement =
                        connection.prepareStatement(itemSql)) {
            int parameterIndex = binder.bind(statement, 1);
            statement.setInt(parameterIndex++, pageSize);
            statement.setInt(
                    parameterIndex,
                    Pagination.offset(page, pageSize));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (items.isEmpty()) {
                        totalCount = resultSet.getInt("total_count");
                    }
                    items.add(mapSummary(
                            resultSet, includeCreatorUserId));
                }
            }
        }
        return new SummaryRows(items, totalCount);
    }

    private int countSummaryRows(
            Connection connection,
            String whereClause,
            StatementBinder binder) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM " + TABLE_NAME + whereClause;
        try (PreparedStatement statement =
                        connection.prepareStatement(countSql)) {
            binder.bind(statement, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private static TroubleshootingDTO mapSummary(
            ResultSet resultSet,
            boolean includeCreatorUserId) throws SQLException {
        TroubleshootingDTO troubleshooting = new TroubleshootingDTO();
        troubleshooting.setId(resultSet.getInt("id"));
        troubleshooting.setTitle(resultSet.getString("title"));
        troubleshooting.setCustomerName(
                resultSet.getString("customer_name"));

        Timestamp occurrenceTimestamp =
                resultSet.getTimestamp("occurrence_date");
        if (occurrenceTimestamp != null) {
            troubleshooting.setOccurrenceDate(
                    new java.util.Date(occurrenceTimestamp.getTime()));
        }

        troubleshooting.setCreator(resultSet.getString("creator"));
        if (includeCreatorUserId) {
            troubleshooting.setCreatorUserId(
                    resultSet.getString(CREATOR_USER_ID_COLUMN));
        }

        Timestamp createTimestamp =
                resultSet.getTimestamp("create_date");
        if (createTimestamp != null) {
            troubleshooting.setCreateDate(
                    new java.util.Date(createTimestamp.getTime()));
        }
        return troubleshooting;
    }

    private static int bindSearch(
            PreparedStatement statement,
            int startIndex,
            String query,
            boolean includeContent) throws SQLException {
        int parameterIndex = startIndex;
        String summaryPattern = SearchQueryPolicy.literalContainsLikePattern(
                query);
        for (int field = 0; field < 3; field++) {
            statement.setString(parameterIndex++, summaryPattern);
        }
        if (includeContent) {
            String contentPattern = SearchQueryPolicy.literalContainsRegex(
                    query);
            for (int field = 0; field < 6; field++) {
                statement.setString(parameterIndex++, contentPattern);
            }
        }
        return parameterIndex;
    }

    private static String normalizedQuery(String query) {
        return SearchQueryPolicy.normalize(query);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface StatementBinder {
        int bind(PreparedStatement statement, int startIndex)
                throws SQLException;
    }

    private record SummaryRows(
            List<TroubleshootingDTO> items, int totalCount) {
    }

    // 빈 문자열을 NULL로 처리하는 헬퍼 메서드
    private void setStringOrNull(PreparedStatement pstmt, int parameterIndex, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            pstmt.setNull(parameterIndex, Types.VARCHAR);
        } else {
            pstmt.setString(parameterIndex, value.trim());
        }
    }
}
