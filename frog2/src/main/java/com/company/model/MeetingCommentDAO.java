package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.company.util.DBConnection;

public class MeetingCommentDAO {
    public static final int PAGE_SIZE = 50;
    private final JdbcConnectionProvider connectionProvider;

    public MeetingCommentDAO() {
        this(DBConnection::getConnection);
    }

    MeetingCommentDAO(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(
                connectionProvider, "connectionProvider");
    }
    public List<MeetingCommentDTO> getCommentsByMeetingId(Long meetingId) {
        return getCommentPage(meetingId, null, PAGE_SIZE).getComments();
    }

    public MeetingCommentPage getCommentPage(
            Long meetingId, Long beforeCommentId, int pageSize) {
        if (meetingId == null || meetingId <= 0) {
            throw new IllegalArgumentException("Meeting ID must be positive");
        }
        if (beforeCommentId != null && beforeCommentId <= 0) {
            throw new IllegalArgumentException("Comment cursor must be positive");
        }
        if (pageSize <= 0 || pageSize > 100) {
            throw new IllegalArgumentException(
                    "Comment page size must be between 1 and 100");
        }
        List<MeetingCommentDTO> comments = new ArrayList<>();
        try (Connection conn = connectionProvider.getConnection()) {
            String sql = "SELECT comment_id, meeting_id, content, author_id, author_name, "
                    + "created_at, updated_at FROM meeting_comments "
                    + "WHERE meeting_id = ? "
                    + (beforeCommentId == null ? "" : "AND comment_id < ? ")
                    + "ORDER BY comment_id DESC LIMIT ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int parameter = 1;
                pstmt.setLong(parameter++, meetingId);
                if (beforeCommentId != null) {
                    pstmt.setLong(parameter++, beforeCommentId);
                }
                pstmt.setInt(parameter, pageSize + 1);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        comments.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.from(
                    "load meeting comment page", e);
        }

        boolean hasOlder = comments.size() > pageSize;
        if (hasOlder) {
            comments.removeLast();
        }
        Collections.reverse(comments);
        Long nextBeforeCommentId = hasOlder && !comments.isEmpty()
                ? comments.getFirst().getCommentId()
                : null;
        return new MeetingCommentPage(
                comments, nextBeforeCommentId, hasOlder, pageSize);
    }

    private static MeetingCommentDTO mapRow(ResultSet rs)
            throws SQLException {
        MeetingCommentDTO comment = new MeetingCommentDTO();
        comment.setCommentId(rs.getLong("comment_id"));
        comment.setMeetingId(rs.getLong("meeting_id"));
        comment.setContent(rs.getString("content"));
        comment.setAuthorId(rs.getString("author_id"));
        comment.setAuthorName(rs.getString("author_name"));
        comment.setCreatedAt(rs.getTimestamp("created_at"));
        comment.setUpdatedAt(rs.getTimestamp("updated_at"));
        return comment;
    }

    public boolean addComment(MeetingCommentDTO comment) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO meeting_comments (meeting_id, content, author_id, author_name) " +
                        "VALUES (?, ?, ?, ?)";

            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, comment.getMeetingId());
            pstmt.setString(2, comment.getContent());
            pstmt.setString(3, comment.getAuthorId());
            pstmt.setString(4, comment.getAuthorName());

            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);

        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }

        return success;
    }

    public boolean updateCommentForAuthor(
            MeetingCommentDTO comment, String authorUserId) {
        if (comment == null
                || authorUserId == null
                || authorUserId.isBlank()) {
            return false;
        }
        try (Connection conn = connectionProvider.getConnection()) {
            String sql = "UPDATE meeting_comments SET content = ?, updated_at = statement_timestamp() " +
                        "WHERE comment_id = ? AND author_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, comment.getContent());
                pstmt.setLong(2, comment.getCommentId());
                pstmt.setString(3, authorUserId.trim());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public boolean deleteCommentForAuthor(
            Long commentId, String authorUserId) {
        if (commentId == null
                || authorUserId == null
                || authorUserId.isBlank()) {
            return false;
        }
        try (Connection conn = connectionProvider.getConnection()) {
            String sql = "DELETE FROM meeting_comments "
                    + "WHERE comment_id = ? AND author_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, commentId);
                pstmt.setString(2, authorUserId.trim());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

}
