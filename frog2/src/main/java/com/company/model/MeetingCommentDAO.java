package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.company.util.DBConnection;

public class MeetingCommentDAO {

    // Legacy comment normalized during UTF-8 migration.
    public List<MeetingCommentDTO> getCommentsByMeetingId(Long meetingId) {
        List<MeetingCommentDTO> comments = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT comment_id, meeting_id, content, author_id, author_name, "
                    + "created_at, updated_at FROM meeting_comments "
                    + "WHERE meeting_id = ? ORDER BY created_at ASC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, meetingId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                MeetingCommentDTO comment = new MeetingCommentDTO();
                comment.setCommentId(rs.getLong("comment_id"));
                comment.setMeetingId(rs.getLong("meeting_id"));
                comment.setContent(rs.getString("content"));
                comment.setAuthorId(rs.getString("author_id"));
                comment.setAuthorName(rs.getString("author_name"));
                comment.setCreatedAt(rs.getTimestamp("created_at"));
                comment.setUpdatedAt(rs.getTimestamp("updated_at"));

                comments.add(comment);
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return comments;
    }

    // Legacy comment normalized during UTF-8 migration.
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

    // Legacy comment normalized during UTF-8 migration.
    public boolean updateComment(MeetingCommentDTO comment) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "UPDATE meeting_comments SET content = ?, updated_at = statement_timestamp() " +
                        "WHERE comment_id = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, comment.getContent());
            pstmt.setLong(2, comment.getCommentId());

            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);

        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }

        return success;
    }

    // Legacy comment normalized during UTF-8 migration.
    public boolean deleteComment(Long commentId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM meeting_comments WHERE comment_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, commentId);

            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);

        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }

        return success;
    }

    // Legacy comment normalized during UTF-8 migration.
    public boolean isCommentAuthor(Long commentId, String userId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean isAuthor = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT author_id FROM meeting_comments WHERE comment_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, commentId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String authorId = rs.getString("author_id");
                isAuthor = userId.equals(authorId);
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return isAuthor;
    }

    // Legacy comment normalized during UTF-8 migration.
    public MeetingCommentDTO getComment(Long commentId) {
        MeetingCommentDTO comment = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT comment_id, meeting_id, content, author_id, author_name, "
                    + "created_at, updated_at FROM meeting_comments "
                    + "WHERE comment_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, commentId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                comment = new MeetingCommentDTO();
                comment.setCommentId(rs.getLong("comment_id"));
                comment.setMeetingId(rs.getLong("meeting_id"));
                comment.setContent(rs.getString("content"));
                comment.setAuthorId(rs.getString("author_id"));
                comment.setAuthorName(rs.getString("author_name"));
                comment.setCreatedAt(rs.getTimestamp("created_at"));
                comment.setUpdatedAt(rs.getTimestamp("updated_at"));
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return comment;
    }
}
