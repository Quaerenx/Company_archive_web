package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import java.util.List;

/**
 * Shared database contract for stable customer-to-user assignments.
 */
final class CustomerAssignmentSupport {
    static final String TABLE = "vertica_customer_detail";
    static final String MAIN_USER_ID_COLUMN = "main_manager_user_id";
    static final String SUB_USER_ID_COLUMN = "sub_manager_user_id";

    private CustomerAssignmentSupport() {
    }

    static Capability capability(
            Connection connection,
            SchemaCapabilityCache schemaCapabilities) {
        boolean mainAvailable = schemaCapabilities.columnExists(
                connection, TABLE, MAIN_USER_ID_COLUMN);
        boolean subAvailable = schemaCapabilities.columnExists(
                connection, TABLE, SUB_USER_ID_COLUMN);
        if (mainAvailable && subAvailable) {
            return Capability.COMPLETE;
        }
        if (!mainAvailable && !subAvailable) {
            return Capability.NONE;
        }
        return Capability.PARTIAL;
    }

    static String assigneePredicate(Capability capability) throws SQLException {
        return switch (capability) {
            case COMPLETE -> "(d.main_manager_user_id = ? "
                    + "OR d.sub_manager_user_id = ?)";
            case NONE -> "(LOWER(TRIM(d.main_manager)) = LOWER(?) "
                    + "OR LOWER(TRIM(d.sub_manager)) = LOWER(?))";
            case PARTIAL -> throw new SQLException(
                    "Customer assignment user-ID columns are partially applied");
        };
    }

    static String assigneeValue(
            Capability capability,
            String userId,
            String displayName) {
        return capability == Capability.COMPLETE
                ? normalize(userId)
                : normalize(displayName);
    }

    static AssignmentUserIds resolveUserIds(
            Connection connection,
            String mainManagerName,
            String subManagerName) throws SQLException {
        String mainUserId = resolveUniqueUserId(connection, mainManagerName);
        String subUserId = normalizedEquals(mainManagerName, subManagerName)
                ? mainUserId
                : resolveUniqueUserId(connection, subManagerName);
        return new AssignmentUserIds(mainUserId, subUserId);
    }

    static AssignmentUserIds resolveUserIds(
            Connection connection, String mainName, String subName, AssignmentState original)
            throws SQLException {
        return new AssignmentUserIds(
                resolveChangedUserId(connection, mainName,
                        original == null ? null : original.mainName(),
                        original == null ? null : original.mainUserId(), original != null),
                resolveChangedUserId(connection, subName,
                        original == null ? null : original.subName(),
                        original == null ? null : original.subUserId(), original != null));
    }

    static AssignmentState loadAssignment(
            Connection connection, String customerName, boolean references) throws SQLException {
        String sql = "SELECT main_manager, sub_manager, main_manager_user_id, sub_manager_user_id "
                + "FROM " + TABLE + " WHERE " + CustomerReferenceSupport.predicate(references)
                + " AND is_deleted = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerName);
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? new AssignmentState(row.getString("main_manager"),
                        row.getString("sub_manager"), row.getString(MAIN_USER_ID_COLUMN),
                        row.getString(SUB_USER_ID_COLUMN)) : null;
            }
        }
    }

    private static String resolveChangedUserId(
            Connection connection, String name, String oldName, String oldId, boolean existing)
            throws SQLException {
        if (existing && (java.util.Objects.equals(normalize(name), normalize(oldName))
                || normalizedEquals(name, oldName))) return oldId;
        if (normalize(name) == null) return null;
        String id = resolveUniqueUserId(connection, name);
        if (id == null) throw new CustomerAssignmentException();
        return id;
    }

    static int bindUserIds(
            PreparedStatement statement,
            int startIndex,
            AssignmentUserIds userIds) throws SQLException {
        int parameter = startIndex;
        setNullableString(statement, parameter++, userIds.mainUserId());
        setNullableString(statement, parameter++, userIds.subUserId());
        return parameter;
    }

    static void synchronizeDisplayName(
            Connection connection,
            String userId,
            String displayName) throws SQLException {
        String normalizedUserId = normalize(userId);
        String normalizedDisplayName = normalize(displayName);
        if (normalizedUserId == null || normalizedDisplayName == null) {
            throw new IllegalArgumentException(
                    "User ID and display name are required");
        }
        updateDisplayName(
                connection,
                "main_manager",
                MAIN_USER_ID_COLUMN,
                normalizedUserId,
                normalizedDisplayName);
        updateDisplayName(
                connection,
                "sub_manager",
                SUB_USER_ID_COLUMN,
                normalizedUserId,
                normalizedDisplayName);
    }

    private static void updateDisplayName(
            Connection connection,
            String displayColumn,
            String userIdColumn,
            String userId,
            String displayName) throws SQLException {
        String sql = "UPDATE " + TABLE + " SET " + displayColumn
                + " = ? WHERE " + userIdColumn + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, displayName);
            statement.setString(2, userId);
            statement.executeUpdate();
        }
    }

    private static String resolveUniqueUserId(
            Connection connection,
            String displayName) throws SQLException {
        String normalizedName = normalize(displayName);
        if (normalizedName == null) {
            return null;
        }
        String sql = "SELECT MIN(userId) AS user_id, COUNT(*) AS user_count "
                + "FROM company_users "
                + "WHERE LOWER(TRIM(userName)) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt("user_count") != 1) {
                    return null;
                }
                return normalize(resultSet.getString("user_id"));
            }
        }
    }

    private static void setNullableString(
            PreparedStatement statement,
            int parameter,
            String value) throws SQLException {
        if (value == null) {
            statement.setNull(parameter, Types.VARCHAR);
        } else {
            statement.setString(parameter, value);
        }
    }

    private static boolean normalizedEquals(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return normalizedLeft != null
                && normalizedRight != null
                && normalizedLeft.toLowerCase(Locale.ROOT).equals(
                        normalizedRight.toLowerCase(Locale.ROOT));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    enum Capability {
        NONE,
        PARTIAL,
        COMPLETE
    }

    record AssignmentUserIds(String mainUserId, String subUserId) {
    }

    record AssignmentState(String mainName, String subName, String mainUserId, String subUserId) {
        private static final List<String> COLUMNS = List.of(
                "main_manager", "sub_manager", MAIN_USER_ID_COLUMN, SUB_USER_ID_COLUMN);

        private java.util.List<String> values() {
            return java.util.Arrays.asList(mainName, subName, mainUserId, subUserId);
        }

        String predicate() {
            java.util.List<String> values = values();
            java.util.List<String> clauses = new java.util.ArrayList<>();
            for (int index = 0; index < COLUMNS.size(); index++) {
                clauses.add(COLUMNS.get(index) + (values.get(index) == null ? " IS NULL" : " = ?"));
            }
            return String.join(" AND ", clauses);
        }

        void bind(PreparedStatement statement, int index) throws SQLException {
            for (String value : values()) {
                if (value != null) statement.setString(index++, value);
            }
        }
    }
}
