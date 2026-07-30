package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.company.util.DBConnection;

public class CustomerDAO {

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

    // 고객사 개수 조회 (필터별) - 상세 테이블 기준
    public int getCustomerCount(String filter) {
        int count = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT COUNT(*) FROM vertica_customer_detail d WHERE d.is_deleted = 1");
            if ("maintenance".equals(filter)) {
                sb.append(" AND d.customer_type = '정기점검 계약 고객사'");
            }

            pstmt = conn.prepareStatement(sb.toString());
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

    public CustomerCounts getCustomerCounts() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT COUNT(*) AS total_count, "
                    + "COALESCE(SUM(CASE WHEN d.customer_type = '정기점검 계약 고객사' "
                    + "THEN 1 ELSE 0 END), 0) AS maintenance_count "
                    + "FROM vertica_customer_detail d WHERE d.is_deleted = 1";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return new CustomerCounts(
                        rs.getInt("total_count"), rs.getInt("maintenance_count"));
            }
            return new CustomerCounts(0, 0);
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }
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
    private void setStringOrNull(PreparedStatement pstmt, int parameterIndex, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            pstmt.setNull(parameterIndex, Types.VARCHAR);
        } else {
            pstmt.setString(parameterIndex, value.trim());
        }
    }
}
