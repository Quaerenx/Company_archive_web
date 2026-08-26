package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import com.company.util.DBConnection;

public class UserVmHostDAO {
    public enum MutationResult {
        SAVED,
        HOST_NOT_FOUND,
        DUPLICATE_OWN_IP,
        DUPLICATE_OTHER_IP,
        HOST_LIMIT_REACHED,
        WRITE_FAILED
    }

    private static final int MAX_HOSTS_PER_USER = 20;
    private static final int MUTATION_LOCK_STRIPES = 64;
    private static final ReentrantLock[] MUTATION_LOCKS =
            createMutationLocks();
    private static final String HOST_COLUMNS =
            "ip, owner_user_id, owner_user_name, purpose, os_info, "
                    + "vertica_version, remote_host, note, status, "
                    + "created_at, updated_at";
    private final JdbcConnectionProvider connectionProvider;

    public UserVmHostDAO() {
        this(DBConnection::getConnection);
    }

    UserVmHostDAO(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(
                connectionProvider, "connectionProvider");
    }

    public int getMaxHostsPerUser() {
        return MAX_HOSTS_PER_USER;
    }

    public List<UserVmHostDTO> getActiveHostsByOwner(String ownerUserId) {
        List<UserVmHostDTO> hosts = new ArrayList<>();
        String sql = "SELECT " + HOST_COLUMNS
                + " FROM user_vm_hosts WHERE owner_user_id = ? "
                + "AND status = 'ACTIVE' ORDER BY ip";
        try (Connection conn = connectionProvider.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, safe(ownerUserId));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    hosts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
        return hosts;
    }

    public int getActiveHostCountByOwner(String ownerUserId) {
        try (Connection conn = connectionProvider.getConnection()) {
            return countActiveHostsByOwner(ownerUserId, conn);
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public UserVmHostDTO getHostByIpAndOwner(String ip, String ownerUserId) {
        try (Connection conn = connectionProvider.getConnection()) {
            return getHostByIpAndOwner(ip, ownerUserId, conn);
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public boolean deleteByIpAndOwner(String ip, String ownerUserId) {
        String normalizedIp = safe(ip);
        String normalizedOwnerUserId = safe(ownerUserId);
        List<ReentrantLock> mutationLocks = mutationLocks(
                normalizedOwnerUserId, normalizedIp, null);
        lockAll(mutationLocks);
        try {
            return deleteWhileLocked(normalizedIp, normalizedOwnerUserId);
        } finally {
            unlockAll(mutationLocks);
        }
    }

    private boolean deleteWhileLocked(String ip, String ownerUserId) {
        String sql = "DELETE FROM user_vm_hosts "
                + "WHERE ip = ? AND owner_user_id = ?";
        try (Connection conn = connectionProvider.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ip);
            pstmt.setString(2, ownerUserId);
            return pstmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    public MutationResult saveNormalized(
            UserVmHostDTO dto, String originalIp) {
        List<ReentrantLock> mutationLocks = mutationLocks(
                safe(dto.getOwnerUserId()),
                safe(dto.getIp()),
                safe(originalIp));
        lockAll(mutationLocks);
        try {
            try (Connection conn = connectionProvider.getConnection()) {
                MutationResult result = saveWhileLocked(
                        dto, originalIp, conn);
                return result;
            } catch (SQLException e) {
                throw DataAccessException.from(e);
            }
        } finally {
            unlockAll(mutationLocks);
        }
    }

    private MutationResult saveWhileLocked(
            UserVmHostDTO dto,
            String originalIp,
            Connection conn) throws SQLException {
        UserVmHostDTO existing = null;
        if (originalIp != null) {
            existing = getHostByIpAndOwner(
                    originalIp, dto.getOwnerUserId(), conn);
            if (existing == null) {
                return MutationResult.HOST_NOT_FOUND;
            }
        }

        UserVmHostDTO targetIpHost = getHostByIp(dto.getIp(), conn);
        if (targetIpHost != null
                && (existing == null
                        || !dto.getIp().equals(originalIp))) {
            return dto.getOwnerUserId().equals(
                    targetIpHost.getOwnerUserId())
                    ? MutationResult.DUPLICATE_OWN_IP
                    : MutationResult.DUPLICATE_OTHER_IP;
        }

        if (existing == null
                && countActiveHostsByOwner(
                        dto.getOwnerUserId(), conn)
                        >= MAX_HOSTS_PER_USER) {
            return MutationResult.HOST_LIMIT_REACHED;
        }

        boolean saved = existing == null
                ? insert(dto, conn)
                : update(dto, originalIp, conn);
        return saved
                ? MutationResult.SAVED
                : MutationResult.WRITE_FAILED;
    }

    /**
     * Compatibility adapter for callers compiled against the legacy API.
     * New request paths use the typed service result directly.
     */
    @Deprecated
    public String save(UserVmHostDTO dto, String originalIp) {
        normalize(dto);
        String normalizedOriginalIp = safe(originalIp);
        if (dto.getOwnerUserId() == null) {
            return "사용자 정보가 없습니다. 다시 로그인해 주세요.";
        }
        if (!isAllowedIp(dto.getIp())) {
            return "IP는 192.168.40.1 ~ 192.168.40.254 범위만 등록할 수 있습니다.";
        }
        if (dto.getPurpose() == null) {
            return "사용 목적은 필수입니다.";
        }

        MutationResult result = saveNormalized(dto, normalizedOriginalIp);
        return switch (result) {
            case SAVED -> null;
            case HOST_NOT_FOUND -> "수정 대상 호스트를 찾을 수 없습니다.";
            case DUPLICATE_OWN_IP -> "이미 등록한 IP입니다.";
            case DUPLICATE_OTHER_IP -> "이미 다른 사용자가 등록한 IP입니다.";
            case HOST_LIMIT_REACHED -> "사용자당 VM 호스트는 최대 20개까지만 등록할 수 있습니다.";
            case WRITE_FAILED -> "호스트 정보를 저장하지 못했습니다. 다시 시도해 주세요.";
        };
    }

    private boolean insert(UserVmHostDTO dto, Connection conn)
            throws SQLException {
        String sql = "INSERT INTO user_vm_hosts "
                + "(ip, owner_user_id, owner_user_name, purpose, "
                + "os_info, vertica_version, remote_host, note, "
                + "status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', "
                + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dto.getIp());
            pstmt.setString(2, dto.getOwnerUserId());
            pstmt.setString(3, dto.getOwnerUserName());
            pstmt.setString(4, dto.getPurpose());
            pstmt.setString(5, dto.getOsInfo());
            pstmt.setString(6, dto.getVerticaVersion());
            pstmt.setString(7, dto.getRemoteHost());
            pstmt.setString(8, dto.getNote());
            return pstmt.executeUpdate() == 1;
        }
    }

    private boolean update(
            UserVmHostDTO dto, String originalIp, Connection conn)
            throws SQLException {
        String sql = "UPDATE user_vm_hosts SET ip = ?, "
                + "owner_user_name = ?, purpose = ?, os_info = ?, "
                + "vertica_version = ?, remote_host = ?, note = ?, "
                + "status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP "
                + "WHERE ip = ? AND owner_user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dto.getIp());
            pstmt.setString(2, dto.getOwnerUserName());
            pstmt.setString(3, dto.getPurpose());
            pstmt.setString(4, dto.getOsInfo());
            pstmt.setString(5, dto.getVerticaVersion());
            pstmt.setString(6, dto.getRemoteHost());
            pstmt.setString(7, dto.getNote());
            pstmt.setString(8, originalIp);
            pstmt.setString(9, dto.getOwnerUserId());
            return pstmt.executeUpdate() == 1;
        }
    }

    private int countActiveHostsByOwner(
            String ownerUserId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_vm_hosts "
                + "WHERE owner_user_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, safe(ownerUserId));
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static List<ReentrantLock> mutationLocks(
            String ownerUserId,
            String targetIp,
            String originalIp) {
        Set<Integer> indexes = new TreeSet<>();
        indexes.add(mutationLockIndex("owner", ownerUserId));
        if (targetIp != null) {
            indexes.add(mutationLockIndex("ip", targetIp));
        }
        if (originalIp != null) {
            indexes.add(mutationLockIndex("ip", originalIp));
        }

        List<ReentrantLock> locks = new ArrayList<>(indexes.size());
        for (int index : indexes) {
            locks.add(MUTATION_LOCKS[index]);
        }
        return locks;
    }

    private static int mutationLockIndex(String namespace, String value) {
        return Math.floorMod(
                Objects.hash(namespace, value), MUTATION_LOCK_STRIPES);
    }

    private static void lockAll(List<ReentrantLock> locks) {
        for (ReentrantLock lock : locks) {
            lock.lock();
        }
    }

    private static void unlockAll(List<ReentrantLock> locks) {
        for (int index = locks.size() - 1; index >= 0; index--) {
            locks.get(index).unlock();
        }
    }

    private static ReentrantLock[] createMutationLocks() {
        ReentrantLock[] locks = new ReentrantLock[MUTATION_LOCK_STRIPES];
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
        return locks;
    }

    private UserVmHostDTO getHostByIp(String ip, Connection conn) throws SQLException {
        String sql = "SELECT " + HOST_COLUMNS
                + " FROM user_vm_hosts WHERE ip = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, safe(ip));
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private UserVmHostDTO getHostByIpAndOwner(
            String ip,
            String ownerUserId,
            Connection conn) throws SQLException {
        String sql = "SELECT " + HOST_COLUMNS
                + " FROM user_vm_hosts WHERE ip = ? AND owner_user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, safe(ip));
            pstmt.setString(2, safe(ownerUserId));
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
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

    private void normalize(UserVmHostDTO dto) {
        dto.setOwnerUserId(safe(dto.getOwnerUserId()));
        dto.setOwnerUserName(safe(dto.getOwnerUserName()));
        dto.setIp(safe(dto.getIp()));
        dto.setPurpose(safe(dto.getPurpose()));
        dto.setOsInfo(safe(dto.getOsInfo()));
        dto.setVerticaVersion(safe(dto.getVerticaVersion()));
        dto.setRemoteHost(safe(dto.getRemoteHost()));
        dto.setNote(safe(dto.getNote()));
        dto.setStatus("ACTIVE");
    }

    private boolean isAllowedIp(String ip) {
        if (ip == null || !ip.startsWith("192.168.40.")) {
            return false;
        }
        try {
            int lastOctet = Integer.parseInt(
                    ip.substring("192.168.40.".length()));
            return lastOctet >= 1 && lastOctet <= 254;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String safe(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
