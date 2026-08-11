package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.company.util.DBConnection;

public class MeetingRecordDAO {
    private static final int PAGE_SIZE = 20;
    static final String LIST_SQL =
            "SELECT meeting_id, title, author_name, meeting_datetime "
                    + "FROM meeting_records "
                    + "ORDER BY meeting_datetime DESC LIMIT ? OFFSET ?";
    private final JdbcConnectionProvider connectionProvider;

    public MeetingRecordDAO() {
        this(DBConnection::getConnection);
    }

    MeetingRecordDAO(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(
                connectionProvider, "connectionProvider");
    }

    public List<MeetingRecordDTO> getMeetingRecords(int page) {
        List<MeetingRecordDTO> records = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(LIST_SQL);
            pstmt.setInt(1, PAGE_SIZE);
            pstmt.setInt(2, offsetForPage(page));
            rs = pstmt.executeQuery();

            while (rs.next()) {
                MeetingRecordDTO record = new MeetingRecordDTO();
                record.setMeetingId(rs.getLong("meeting_id"));
                record.setTitle(rs.getString("title"));
                record.setMeetingDatetime(rs.getTimestamp("meeting_datetime"));
                record.setAuthorName(rs.getString("author_name"));

                records.add(record);
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return records;
    }

    public int getTotalCount() {
        int count = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM meeting_records";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return count;
    }

    public MeetingRecordDTO getMeetingRecord(Long meetingId) {
        MeetingRecordDTO record = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();

            String selectSql = "SELECT meeting_id, title, meeting_datetime, meeting_type, "
                    + "content, author_id, author_name, view_count, created_at, updated_at "
                    + "FROM meeting_records WHERE meeting_id = ?";
            pstmt = conn.prepareStatement(selectSql);
            pstmt.setLong(1, meetingId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                record = new MeetingRecordDTO();
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
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return record;
    }

    public boolean addMeetingRecord(MeetingRecordDTO record) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO meeting_records (title, meeting_datetime, meeting_type, content, author_id, author_name) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, record.getTitle());
            pstmt.setTimestamp(2, record.getMeetingDatetime());
            pstmt.setString(3, record.getMeetingType());
            pstmt.setString(4, record.getContent());
            pstmt.setString(5, record.getAuthorId());
            pstmt.setString(6, record.getAuthorName());

            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);

        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }

        return success;
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
        if (page < 1) {
            throw new IllegalArgumentException("Page must be positive.");
        }
        return Math.multiplyExact(page - 1, PAGE_SIZE);
    }
}
