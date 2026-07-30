package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.company.util.DBConnection;

public class TroubleshootingDAO {

    // 모든 ?�러�??�팅 목록 조회
    public List<TroubleshootingDTO> getAllTroubleshooting() {
        List<TroubleshootingDTO> troubleshootingList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT id, title, customer_name, occurrence_date, creator, create_date " +
                        // ?�렬: 발생?�자 ?�림차순, NULL?� ?�로, ?�일 ???�성???�림차순
                        "FROM troubleshooting ORDER BY occurrence_date DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                TroubleshootingDTO ts = new TroubleshootingDTO();
                ts.setId(rs.getInt("id"));
                ts.setTitle(rs.getString("title"));
                ts.setCustomerName(rs.getString("customer_name"));

                Timestamp occurrenceTs = rs.getTimestamp("occurrence_date");
                if (occurrenceTs != null) {
                    ts.setOccurrenceDate(new java.util.Date(occurrenceTs.getTime()));
                }

                ts.setCreator(rs.getString("creator"));

                Timestamp createTs = rs.getTimestamp("create_date");
                if (createTs != null) {
                    ts.setCreateDate(new java.util.Date(createTs.getTime()));
                }

                troubleshootingList.add(ts);
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return troubleshootingList;
    }

    // 검색: 제목/고객명/작성자 및 본문 필드까지 ILIKE 검색
    public List<TroubleshootingDTO> searchTroubleshooting(String query) {
        List<TroubleshootingDTO> troubleshootingList = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return troubleshootingList;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            String sql =
                "SELECT id, title, customer_name, occurrence_date, creator, create_date " +
                "FROM troubleshooting " +
                "WHERE title ILIKE ? " +
                "   OR customer_name ILIKE ? " +
                "   OR creator ILIKE ? " +
                "   OR CAST(SUBSTR(overview,1,65000) AS VARCHAR(65000)) ILIKE CAST(? AS VARCHAR(65000)) " +
                "   OR CAST(SUBSTR(cause_analysis,1,65000) AS VARCHAR(65000)) ILIKE CAST(? AS VARCHAR(65000)) " +
                "   OR CAST(SUBSTR(error_content,1,65000) AS VARCHAR(65000)) ILIKE CAST(? AS VARCHAR(65000)) " +
                "   OR CAST(SUBSTR(action_taken,1,65000) AS VARCHAR(65000)) ILIKE CAST(? AS VARCHAR(65000)) " +
                "   OR CAST(SUBSTR(script_content,1,65000) AS VARCHAR(65000)) ILIKE CAST(? AS VARCHAR(65000)) " +
                "   OR CAST(SUBSTR(note,1,65000) AS VARCHAR(65000)) ILIKE CAST(? AS VARCHAR(65000)) " +
                // ?�렬: 발생?�자 ?�림차순, NULL?� ?�로, ?�일 ???�성???�림차순
                "ORDER BY occurrence_date DESC NULLS LAST, create_date DESC";

            pstmt = conn.prepareStatement(sql);
            String like = "%" + query.trim() + "%";
            for (int i = 1; i <= 9; i++) {
                pstmt.setString(i, like);
            }

            rs = pstmt.executeQuery();
            while (rs.next()) {
                TroubleshootingDTO ts = new TroubleshootingDTO();
                ts.setId(rs.getInt("id"));
                ts.setTitle(rs.getString("title"));
                ts.setCustomerName(rs.getString("customer_name"));

                Timestamp occurrenceTs = rs.getTimestamp("occurrence_date");
                if (occurrenceTs != null) {
                    ts.setOccurrenceDate(new java.util.Date(occurrenceTs.getTime()));
                }

                ts.setCreator(rs.getString("creator"));

                Timestamp createTs = rs.getTimestamp("create_date");
                if (createTs != null) {
                    ts.setCreateDate(new java.util.Date(createTs.getTime()));
                }

                troubleshootingList.add(ts);
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return troubleshootingList;
    }

    // ?�정 ?�러�??�팅 ?�세 조회
    public TroubleshootingDTO getTroubleshootingById(int id) {
        TroubleshootingDTO ts = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT id, title, customer_name, customer_manager, occurrence_date, "
                    + "work_personnel, work_period, creator, create_date, support_type, "
                    + "case_open_yn, overview, cause_analysis, error_content, action_taken, "
                    + "script_content, note, updated_date "
                    + "FROM troubleshooting WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                ts = new TroubleshootingDTO();
                ts.setId(rs.getInt("id"));
                ts.setTitle(rs.getString("title"));
                ts.setCustomerName(rs.getString("customer_name"));
                ts.setCustomerManager(rs.getString("customer_manager"));

                Timestamp occurrenceTs = rs.getTimestamp("occurrence_date");
                if (occurrenceTs != null) {
                    ts.setOccurrenceDate(new java.util.Date(occurrenceTs.getTime()));
                }

                ts.setWorkPersonnel(rs.getString("work_personnel"));
                ts.setWorkPeriod(rs.getString("work_period"));
                ts.setCreator(rs.getString("creator"));

                Timestamp createTs = rs.getTimestamp("create_date");
                if (createTs != null) {
                    ts.setCreateDate(new java.util.Date(createTs.getTime()));
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
                    ts.setUpdatedDate(new java.util.Date(updatedTs.getTime()));
                }
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return ts;
    }

    // ???�러�??�팅 추�?
    public boolean addTroubleshooting(TroubleshootingDTO ts) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "INSERT INTO troubleshooting (" +
                        "title, customer_name, customer_manager, occurrence_date, work_personnel, " +
                        "work_period, creator, support_type, case_open_yn, overview, " +
                        "cause_analysis, error_content, action_taken, script_content, note" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, ts.getTitle());
            pstmt.setString(2, ts.getCustomerName());
            setStringOrNull(pstmt, 3, ts.getCustomerManager());

            if (ts.getOccurrenceDate() != null) {
                pstmt.setTimestamp(4, new Timestamp(ts.getOccurrenceDate().getTime()));
            } else {
                pstmt.setNull(4, Types.TIMESTAMP);
            }

            setStringOrNull(pstmt, 5, ts.getWorkPersonnel());
            setStringOrNull(pstmt, 6, ts.getWorkPeriod());
            pstmt.setString(7, ts.getCreator());
            setStringOrNull(pstmt, 8, ts.getSupportType());
            setStringOrNull(pstmt, 9, ts.getCaseOpenYn());
            setStringOrNull(pstmt, 10, ts.getOverview());
            setStringOrNull(pstmt, 11, ts.getCauseAnalysis());
            setStringOrNull(pstmt, 12, ts.getErrorContent());
            setStringOrNull(pstmt, 13, ts.getActionTaken());
            setStringOrNull(pstmt, 14, ts.getScriptContent());
            setStringOrNull(pstmt, 15, ts.getNote());

            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);

        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }

        return success;
    }

    // ?�러�??�팅 ?�정
    public boolean updateTroubleshooting(TroubleshootingDTO ts) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "UPDATE troubleshooting SET " +
                        "title = ?, customer_name = ?, customer_manager = ?, occurrence_date = ?, " +
                        "work_personnel = ?, work_period = ?, support_type = ?, case_open_yn = ?, " +
                        "overview = ?, cause_analysis = ?, error_content = ?, action_taken = ?, " +
                        "script_content = ?, note = ?, updated_date = NOW() " +
                        "WHERE id = ?";

            pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, ts.getTitle());
            pstmt.setString(2, ts.getCustomerName());
            setStringOrNull(pstmt, 3, ts.getCustomerManager());

            if (ts.getOccurrenceDate() != null) {
                pstmt.setTimestamp(4, new Timestamp(ts.getOccurrenceDate().getTime()));
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

            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);

        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }

        return success;
    }

    // ?�러�??�팅 ??��
    public boolean deleteTroubleshooting(int id) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM troubleshooting WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);

            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);

        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }

        return success;
    }

    // 특정 작성자의 트러블슈팅 목록 조회
    public List<TroubleshootingDTO> getTroubleshootingByCreator(String creator) {
        List<TroubleshootingDTO> troubleshootingList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT id, title, customer_name, occurrence_date, creator, create_date " +
                        "FROM troubleshooting WHERE creator = ? " +
                        "ORDER BY occurrence_date DESC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, creator);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                TroubleshootingDTO ts = new TroubleshootingDTO();
                ts.setId(rs.getInt("id"));
                ts.setTitle(rs.getString("title"));
                ts.setCustomerName(rs.getString("customer_name"));

                Timestamp occurrenceTs = rs.getTimestamp("occurrence_date");
                if (occurrenceTs != null) {
                    ts.setOccurrenceDate(new java.util.Date(occurrenceTs.getTime()));
                }

                ts.setCreator(rs.getString("creator"));

                Timestamp createTs = rs.getTimestamp("create_date");
                if (createTs != null) {
                    ts.setCreateDate(new java.util.Date(createTs.getTime()));
                }

                troubleshootingList.add(ts);
            }
        } catch (SQLException  e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }

        return troubleshootingList;
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