package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.company.util.DBConnection;
import com.company.util.Pagination;
import com.company.util.SearchQueryPolicy;

public class MeetingRecordDAO {
    private static final int PAGE_SIZE = 20;
    static final String LIST_SQL =
            "SELECT meeting_id, title, meeting_type, author_name, meeting_datetime, "
                    + "COUNT(*) OVER () AS total_count "
                    + "FROM meeting_records "
                    + "ORDER BY meeting_datetime DESC, meeting_id DESC LIMIT ? OFFSET ?";
    private final JdbcConnectionProvider connectionProvider;

    public MeetingRecordDAO() {
        this(DBConnection::getConnection);
    }

    MeetingRecordDAO(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(
                connectionProvider, "connectionProvider");
    }

    public List<MeetingRecordDTO> getMeetingRecords(int page) {
        try (Connection conn = connectionProvider.getConnection()) {
            return loadMeetingRows(conn, page).items();
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public PageResult<MeetingRecordDTO> getMeetingPage(int requestedPage) {
        return getMeetingPage(MeetingListFilter.empty(), requestedPage);
    }

    public PageResult<MeetingRecordDTO> getMeetingPage(
            MeetingListFilter filter, int requestedPage) {
        Objects.requireNonNull(filter, "filter");
        int page = Math.max(1, requestedPage);
        try (Connection connection = connectionProvider.getConnection()) {
            FilterSql filterSql = buildFilterSql(filter);
            MeetingRows rows;
            try {
                rows = loadMeetingRows(connection, filterSql, page);
            } catch (ArithmeticException exception) {
                rows = new MeetingRows(List.of(), 0);
            }

            if (!rows.items().isEmpty() || page == 1) {
                return new PageResult<>(
                        rows.items(), rows.totalCount(), page, PAGE_SIZE);
            }

            int totalCount = countMeetingRecords(connection, filterSql);
            int correctedPage = Pagination.clampPage(
                    page, Pagination.totalPages(totalCount, PAGE_SIZE));
            if (totalCount == 0) {
                return new PageResult<>(List.of(), 0, correctedPage, PAGE_SIZE);
            }
            MeetingRows correctedRows = loadMeetingRows(
                    connection, filterSql, correctedPage);
            if (correctedRows.items().isEmpty()) {
                int refreshedCount = countMeetingRecords(
                        connection, filterSql);
                if (refreshedCount == 0) {
                    return new PageResult<>(List.of(), 0, 1, PAGE_SIZE);
                }
                int refreshedPage = Pagination.clampPage(
                        correctedPage,
                        Pagination.totalPages(refreshedCount, PAGE_SIZE));
                correctedRows = loadMeetingRows(
                        connection, filterSql, refreshedPage);
                if (correctedRows.items().isEmpty()) {
                    return new PageResult<>(List.of(), 0, 1, PAGE_SIZE);
                }
                correctedPage = refreshedPage;
            }
            return new PageResult<>(
                    correctedRows.items(), correctedRows.totalCount(),
                    correctedPage, PAGE_SIZE);
        } catch (SQLException exception) {
            throw DataAccessException.from("load meeting page", exception);
        }
    }

    public int getTotalCount() {
        try (Connection connection = connectionProvider.getConnection()) {
            return countMeetingRecords(connection);
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public List<MeetingRecordDTO> searchMeetingRecords(
            String query, int limit) {
        if (limit <= 0 || limit > 20) {
            throw new IllegalArgumentException(
                    "Search limit must be between 1 and 20");
        }
        String normalizedQuery = SearchQueryPolicy.normalize(query);
        if (normalizedQuery == null) {
            return List.of();
        }
        String sql = "SELECT meeting_id, title, meeting_type, "
                + "author_name, meeting_datetime FROM meeting_records "
                + "WHERE title ILIKE ? ESCAPE '!' "
                + "OR meeting_type ILIKE ? ESCAPE '!' "
                + "OR author_name ILIKE ? ESCAPE '!' "
                + "OR REGEXP_ILIKE(content, ?) "
                + "ORDER BY meeting_datetime DESC, meeting_id DESC LIMIT ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            String likePattern =
                    SearchQueryPolicy.literalContainsLikePattern(
                            normalizedQuery);
            for (int parameter = 1; parameter <= 3; parameter++) {
                statement.setString(parameter, likePattern);
            }
            statement.setString(
                    4,
                    SearchQueryPolicy.literalContainsRegex(
                            normalizedQuery));
            statement.setInt(5, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MeetingRecordDTO> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(mapListRow(resultSet));
                }
                return List.copyOf(records);
            }
        } catch (SQLException exception) {
            throw DataAccessException.from(
                    "search meeting records", exception);
        }
    }

    public MeetingRecordDTO getMeetingRecord(Long meetingId) {
        String selectSql = "SELECT meeting_id, title, meeting_datetime, meeting_type, "
                + "content, author_id, author_name, view_count, created_at, updated_at "
                + "FROM meeting_records WHERE meeting_id = ?";
        try (Connection conn = connectionProvider.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setLong(1, meetingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                MeetingRecordDTO record = new MeetingRecordDTO();
                record.setMeetingId(rs.getLong("meeting_id"));
                record.setTitle(rs.getString("title"));
                record.setMeetingDatetime(rs.getTimestamp("meeting_datetime"));
                record.setMeetingType(rs.getString("meeting_type"));
                record.setContent(rs.getString("content"));
                record.setAuthorId(rs.getString("author_id"));
                record.setAuthorName(rs.getString("author_name"));
                record.setViewCount(rs.getInt("view_count"));
                record.setCreatedAt(rs.getTimestamp("created_at"));
                record.setUpdatedAt(rs.getTimestamp("updated_at"));
                return record;
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public boolean addMeetingRecord(MeetingRecordDTO record) {
        String sql = "INSERT INTO meeting_records "
                + "(title, meeting_datetime, meeting_type, content, author_id, author_name) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionProvider.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, record.getTitle());
            pstmt.setTimestamp(2, record.getMeetingDatetime());
            pstmt.setString(3, record.getMeetingType());
            pstmt.setString(4, record.getContent());
            pstmt.setString(5, record.getAuthorId());
            pstmt.setString(6, record.getAuthorName());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public boolean updateMeetingRecordForAuthor(
            MeetingRecordDTO record, String authorUserId) {
        if (record == null || authorUserId == null || authorUserId.isBlank()) {
            return false;
        }
        try (Connection conn = connectionProvider.getConnection()) {
            String sql = "UPDATE meeting_records SET title = ?, meeting_datetime = ?, meeting_type = ?, " +
                        "content = ?, updated_at = statement_timestamp() "
                        + "WHERE meeting_id = ? AND author_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, record.getTitle());
                pstmt.setTimestamp(2, record.getMeetingDatetime());
                pstmt.setString(3, record.getMeetingType());
                pstmt.setString(4, record.getContent());
                pstmt.setLong(5, record.getMeetingId());
                pstmt.setString(6, authorUserId.trim());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public boolean deleteMeetingRecordForAuthor(
            Long meetingId, String authorUserId) {
        if (meetingId == null
                || authorUserId == null
                || authorUserId.isBlank()) {
            return false;
        }
        try (Connection conn = connectionProvider.getConnection()) {
            String sql = "DELETE FROM meeting_records "
                    + "WHERE meeting_id = ? AND author_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, meetingId);
                pstmt.setString(2, authorUserId.trim());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public static int getPageSize() {
        return PAGE_SIZE;
    }

    static int offsetForPage(int page) {
        return Pagination.offset(page, PAGE_SIZE);
    }

    private static MeetingRows loadMeetingRows(
            Connection connection, int page) throws SQLException {
        return loadMeetingRows(
                connection, buildFilterSql(MeetingListFilter.empty()), page);
    }

    private static MeetingRows loadMeetingRows(
            Connection connection,
            FilterSql filter,
            int page) throws SQLException {
        List<MeetingRecordDTO> records = new ArrayList<>();
        int totalCount = 0;
        String sql = "SELECT meeting_id, title, meeting_type, author_name, "
                + "meeting_datetime, COUNT(*) OVER () AS total_count "
                + "FROM meeting_records"
                + filter.whereClause()
                + " ORDER BY meeting_datetime DESC, meeting_id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {
            int parameter = filter.bind(statement, 1);
            statement.setInt(parameter++, PAGE_SIZE);
            statement.setInt(parameter, offsetForPage(page));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (records.isEmpty()) {
                        totalCount = resultSet.getInt("total_count");
                    }
                    records.add(mapListRow(resultSet));
                }
            }
        }
        return new MeetingRows(records, totalCount);
    }

    private static int countMeetingRecords(Connection connection)
            throws SQLException {
        return countMeetingRecords(
                connection, buildFilterSql(MeetingListFilter.empty()));
    }

    private static int countMeetingRecords(
            Connection connection,
            FilterSql filter) throws SQLException {
        String sql = "SELECT COUNT(*) FROM meeting_records"
                + filter.whereClause();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            filter.bind(statement, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private static FilterSql buildFilterSql(MeetingListFilter filter) {
        List<String> predicates = new ArrayList<>();
        List<ParameterBinder> binders = new ArrayList<>();
        if (filter.query() != null) {
            predicates.add("(title ILIKE ? ESCAPE '!' OR REGEXP_ILIKE(content, ?))");
            String like = SearchQueryPolicy.literalContainsLikePattern(
                    filter.query());
            String regex = SearchQueryPolicy.literalContainsRegex(
                    filter.query());
            binders.add((statement, index) -> {
                statement.setString(index++, like);
                statement.setString(index++, regex);
                return index;
            });
        }
        if (filter.meetingType() != null) {
            predicates.add("meeting_type = ?");
            binders.add((statement, index) -> {
                statement.setString(index++, filter.meetingType());
                return index;
            });
        }
        if (filter.author() != null) {
            predicates.add("author_name ILIKE ? ESCAPE '!'");
            String like = SearchQueryPolicy.literalContainsLikePattern(
                    filter.author());
            binders.add((statement, index) -> {
                statement.setString(index++, like);
                return index;
            });
        }
        if (filter.startDate() != null) {
            predicates.add("meeting_datetime >= ?");
            Timestamp start = Timestamp.valueOf(
                    filter.startDate().toLocalDate().atStartOfDay());
            binders.add((statement, index) -> {
                statement.setTimestamp(index++, start);
                return index;
            });
        }
        if (filter.endDate() != null) {
            predicates.add("meeting_datetime < ?");
            Timestamp endExclusive = Timestamp.valueOf(
                    filter.endDate().toLocalDate().plusDays(1).atStartOfDay());
            binders.add((statement, index) -> {
                statement.setTimestamp(index++, endExclusive);
                return index;
            });
        }
        String where = predicates.isEmpty()
                ? ""
                : " WHERE " + String.join(" AND ", predicates);
        return new FilterSql(where, List.copyOf(binders));
    }

    @FunctionalInterface
    private interface ParameterBinder {
        int bind(PreparedStatement statement, int index) throws SQLException;
    }

    private record FilterSql(
            String whereClause,
            List<ParameterBinder> binders) {
        int bind(PreparedStatement statement, int index) throws SQLException {
            int next = index;
            for (ParameterBinder binder : binders) {
                next = binder.bind(statement, next);
            }
            return next;
        }
    }

    private static MeetingRecordDTO mapListRow(ResultSet resultSet)
            throws SQLException {
        MeetingRecordDTO record = new MeetingRecordDTO();
        record.setMeetingId(resultSet.getLong("meeting_id"));
        record.setTitle(resultSet.getString("title"));
        record.setMeetingType(resultSet.getString("meeting_type"));
        record.setMeetingDatetime(resultSet.getTimestamp("meeting_datetime"));
        record.setAuthorName(resultSet.getString("author_name"));
        return record;
    }

    private record MeetingRows(
            List<MeetingRecordDTO> items, int totalCount) {
    }
}
