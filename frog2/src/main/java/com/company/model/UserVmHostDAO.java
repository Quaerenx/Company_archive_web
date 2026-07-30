package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.company.util.DBConnection;

public class UserVmHostDAO {
    private static final int MAX_HOSTS_PER_USER = 20;

    public int getMaxHostsPerUser() {
        return MAX_HOSTS_PER_USER;
    }

    public int countActiveHostsByOwner(String ownerUserId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            return countActiveHostsByOwner(ownerUserId, conn);
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(conn);
        }
    }

    public List<UserVmHostDTO> getActiveHostsByOwner(String ownerUserId) {
        List<UserVmHostDTO> hosts = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT ip, owner_user_id, owner_user_name, purpose, os_info, vertica_version, remote_host, note, status, created_at, updated_at "
                    + "FROM user_vm_hosts WHERE owner_user_id = ? AND status = 'ACTIVE' ORDER BY ip";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, safe(ownerUserId));
            rs = pstmt.executeQuery();
            while (rs.next()) {
                hosts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(rs, pstmt, conn);
        }
        return hosts;
    }

    public UserVmHostDTO getHostByIpAndOwner(String ip, String ownerUserId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            return getHostByIpAndOwner(ip, ownerUserId, conn);
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(conn);
        }
    }

    public boolean deleteByIpAndOwner(String ip, String ownerUserId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM user_vm_hosts WHERE ip = ? AND owner_user_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, safe(ip));
            pstmt.setString(2, safe(ownerUserId));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }
    }

    public String save(UserVmHostDTO dto, String originalIp) {
        String ownerUserId = safe(dto.getOwnerUserId());
        String ip = safe(dto.getIp());
        String purpose = safe(dto.getPurpose());

        dto.setOwnerUserId(ownerUserId);
        dto.setOwnerUserName(safe(dto.getOwnerUserName()));
        dto.setIp(ip);
        dto.setPurpose(purpose);
        dto.setOsInfo(safe(dto.getOsInfo()));
        dto.setVerticaVersion(safe(dto.getVerticaVersion()));
        dto.setRemoteHost(safe(dto.getRemoteHost()));
        dto.setNote(safe(dto.getNote()));
        dto.setStatus("ACTIVE");

        if (isBlank(ownerUserId)) {
            return "사용자 정보가 없습니다. 다시 로그인해 주세요.";
        }
        if (!isAllowedIp(ip)) {
            return "IP는 192.168.40.1 ~ 192.168.40.254 범위만 등록할 수 있습니다.";
        }
        if (isBlank(purpose)) {
            return "사용 목적은 필수입니다.";
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBConnection.getConnection();

            UserVmHostDTO existing = null;
            if (!isBlank(originalIp)) {
                existing = getHostByIpAndOwner(originalIp, ownerUserId, conn);
                if (existing == null) {
                    return "수정 대상 호스트를 찾을 수 없습니다.";
                }
            }

            UserVmHostDTO targetIpHost = getHostByIp(ip, conn);
            if (targetIpHost != null && (existing == null || !ip.equals(safe(originalIp)))) {
                if (ownerUserId.equals(targetIpHost.getOwnerUserId())) {
                    return "이미 등록한 IP입니다.";
                }
                return "이미 다른 사용자가 등록한 IP입니다.";
            }

            if (existing == null && countActiveHostsByOwner(ownerUserId, conn) >= MAX_HOSTS_PER_USER) {
                return "사용자당 VM 호스트는 최대 20개까지만 등록할 수 있습니다.";
            }

            if (existing == null) {
                String insert = "INSERT INTO user_vm_hosts (ip, owner_user_id, owner_user_name, purpose, os_info, vertica_version, remote_host, note, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                pstmt = conn.prepareStatement(insert);
                pstmt.setString(1, dto.getIp());
                pstmt.setString(2, dto.getOwnerUserId());
                pstmt.setString(3, dto.getOwnerUserName());
                pstmt.setString(4, dto.getPurpose());
                pstmt.setString(5, dto.getOsInfo());
                pstmt.setString(6, dto.getVerticaVersion());
                pstmt.setString(7, dto.getRemoteHost());
                pstmt.setString(8, dto.getNote());
                pstmt.executeUpdate();
            } else {
                String update = "UPDATE user_vm_hosts SET ip = ?, owner_user_name = ?, purpose = ?, os_info = ?, vertica_version = ?, remote_host = ?, note = ?, status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP "
                        + "WHERE ip = ? AND owner_user_id = ?";
                pstmt = conn.prepareStatement(update);
                pstmt.setString(1, dto.getIp());
                pstmt.setString(2, dto.getOwnerUserName());
                pstmt.setString(3, dto.getPurpose());
                pstmt.setString(4, dto.getOsInfo());
                pstmt.setString(5, dto.getVerticaVersion());
                pstmt.setString(6, dto.getRemoteHost());
                pstmt.setString(7, dto.getNote());
                pstmt.setString(8, safe(originalIp));
                pstmt.setString(9, dto.getOwnerUserId());
                pstmt.executeUpdate();
            }
            return null;
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        } finally {
            DBConnection.close(pstmt, conn);
        }
    }

    private int countActiveHostsByOwner(String ownerUserId, Connection conn) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT COUNT(*) FROM user_vm_hosts WHERE owner_user_id = ? AND status = 'ACTIVE'";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, safe(ownerUserId));
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            DBConnection.close(rs, pstmt);
        }
    }

    private UserVmHostDTO getHostByIp(String ip, Connection conn) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT ip, owner_user_id, owner_user_name, purpose, os_info, vertica_version, remote_host, note, status, created_at, updated_at "
                    + "FROM user_vm_hosts WHERE ip = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, safe(ip));
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } finally {
            DBConnection.close(rs, pstmt);
        }
    }

    private UserVmHostDTO getHostByIpAndOwner(String ip, String ownerUserId, Connection conn) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT ip, owner_user_id, owner_user_name, purpose, os_info, vertica_version, remote_host, note, status, created_at, updated_at "
                    + "FROM user_vm_hosts WHERE ip = ? AND owner_user_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, safe(ip));
            pstmt.setString(2, safe(ownerUserId));
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        } finally {
            DBConnection.close(rs, pstmt);
        }
    }

    private UserVmHostDTO mapRow(ResultSet rs) throws SQLException {
        UserVmHostDTO dto = new UserVmHostDTO();
        dto.setIp(rs.getString("ip"));
        dto.setOwnerUserId(rs.getString("owner_user_id"));
        dto.setOwnerUserName(rs.getString("owner_user_name"));
        dto.setPurpose(rs.getString("purpose"));
        dto.setOsInfo(rs.getString("os_info"));
        dto.setVerticaVersion(rs.getString("vertica_version"));
        dto.setRemoteHost(rs.getString("remote_host"));
        dto.setNote(rs.getString("note"));
        dto.setStatus(rs.getString("status"));
        dto.setCreatedAt(rs.getTimestamp("created_at"));
        dto.setUpdatedAt(rs.getTimestamp("updated_at"));
        return dto;
    }

    private boolean isAllowedIp(String ip) {
        if (isBlank(ip)) {
            return false;
        }
        String prefix = "192.168.40.";
        if (!ip.startsWith(prefix)) {
            return false;
        }
        try {
            int lastOctet = Integer.parseInt(ip.substring(prefix.length()));
            return lastOctet >= 1 && lastOctet <= 254;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
