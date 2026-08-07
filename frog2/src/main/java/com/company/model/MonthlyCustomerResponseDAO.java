package com.company.model;

import com.company.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MonthlyCustomerResponseDAO {
    private static final String TABLE_NAME = "monthly_customer_response";
    private static final String CREATOR_USER_ID_COLUMN = "created_by_user_id";
    private static final SchemaCapabilityCache APPLICATION_SCHEMA_CAPABILITIES =
            new SchemaCapabilityCache();

    private final JdbcConnectionProvider connectionProvider;
    private final SchemaCapabilityCache schemaCapabilities;

    public MonthlyCustomerResponseDAO() {
        this(DBConnection::getConnection, APPLICATION_SCHEMA_CAPABILITIES);
    }

    MonthlyCustomerResponseDAO(JdbcConnectionProvider connectionProvider) {
        this(connectionProvider, new SchemaCapabilityCache());
    }

    MonthlyCustomerResponseDAO(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache schemaCapabilities) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.schemaCapabilities = Objects.requireNonNull(
                schemaCapabilities, "schemaCapabilities");
    }

    public List<MonthlyCustomerResponseDTO> getMonthlyResponses(
            String userId, String legacyUserName, int year, int month) {
        List<MonthlyCustomerResponseDTO> responses = new ArrayList<>();
        if (isBlank(userId) && isBlank(legacyUserName)) {
            return responses;
        }
        java.sql.Date startDate;
        java.sql.Date endDate;
        try {
            YearMonth selectedMonth = YearMonth.of(year, month);
            startDate = java.sql.Date.valueOf(selectedMonth.atDay(1));
            endDate = java.sql.Date.valueOf(
                    selectedMonth.plusMonths(1).atDay(1));
        } catch (DateTimeException exception) {
            return responses;
        }
        try (Connection connection = connectionProvider.getConnection()) {
            boolean hasCreatorUserId = hasCreatorUserId(connection);
            if (hasCreatorUserId && isBlank(userId)) {
                return responses;
            }
            if (!hasCreatorUserId && isBlank(legacyUserName)) {
                return responses;
            }
            String ownerColumn = hasCreatorUserId
                    ? CREATOR_USER_ID_COLUMN
                    : "created_by";
            String sql = "SELECT id, created_by"
                    + (hasCreatorUserId ? ", " + CREATOR_USER_ID_COLUMN : "")
                    + ", response_date, customer_name, reason, "
                    + "action_content, note, created_at, updated_at "
                    + "FROM " + TABLE_NAME + " "
                    + "WHERE " + ownerColumn
                    + " = ? AND response_date >= ? AND response_date < ? "
                    + "ORDER BY response_date DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(
                        1,
                        hasCreatorUserId ? userId.trim() : legacyUserName.trim());
                statement.setDate(2, startDate);
                statement.setDate(3, endDate);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        responses.add(mapRow(resultSet, hasCreatorUserId));
                    }
                }
            }
            return responses;
        } catch (SQLException exception) {
            throw DataAccessException.from("load monthly customer responses", exception);
        }
    }

    public boolean addResponse(MonthlyCustomerResponseDTO dto) {
        if (dto == null || isBlank(dto.getUserId())) {
            return false;
        }
        try (Connection connection = connectionProvider.getConnection()) {
            if (!hasCreatorUserId(connection)) {
                return false;
            }
            String sql = "INSERT INTO " + TABLE_NAME + " "
                    + "(" + CREATOR_USER_ID_COLUMN
                    + ", created_by, response_date, customer_name, reason, "
                    + "action_content, note, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, GETDATE())";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, dto.getUserId().trim());
                statement.setString(2, dto.getUserName());
                statement.setDate(3, new java.sql.Date(dto.getResponseDate().getTime()));
                statement.setString(4, dto.getCustomerName());
                statement.setString(5, dto.getReason());
                statement.setString(6, dto.getActionContent());
                statement.setString(7, dto.getNote());
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException exception) {
            throw DataAccessException.from("add monthly customer response", exception);
        }
    }

    public boolean updateResponse(MonthlyCustomerResponseDTO dto) {
        if (dto == null || isBlank(dto.getUserId())) {
            return false;
        }
        try (Connection connection = connectionProvider.getConnection()) {
            if (!hasCreatorUserId(connection)) {
                return false;
            }
            String sql = "UPDATE " + TABLE_NAME + " SET "
                    + "response_date = ?, customer_name = ?, reason = ?, "
                    + "action_content = ?, note = ?, updated_at = GETDATE() "
                    + "WHERE id = ? AND " + CREATOR_USER_ID_COLUMN + " = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDate(1, new java.sql.Date(dto.getResponseDate().getTime()));
                statement.setString(2, dto.getCustomerName());
                statement.setString(3, dto.getReason());
                statement.setString(4, dto.getActionContent());
                statement.setString(5, dto.getNote());
                statement.setInt(6, dto.getId());
                statement.setString(7, dto.getUserId().trim());
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException exception) {
            throw DataAccessException.from("update monthly customer response", exception);
        }
    }

    public boolean deleteResponse(int id, String userId) {
        if (isBlank(userId)) {
            return false;
        }
        try (Connection connection = connectionProvider.getConnection()) {
            if (!hasCreatorUserId(connection)) {
                return false;
            }
            String sql = "DELETE FROM " + TABLE_NAME
                    + " WHERE id = ? AND " + CREATOR_USER_ID_COLUMN + " = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.setString(2, userId.trim());
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException exception) {
            throw DataAccessException.from("delete monthly customer response", exception);
        }
    }


    private static MonthlyCustomerResponseDTO mapRow(
            ResultSet resultSet, boolean hasCreatorUserId) throws SQLException {
        MonthlyCustomerResponseDTO dto = new MonthlyCustomerResponseDTO();
        dto.setId(resultSet.getInt("id"));
        if (hasCreatorUserId) {
            dto.setUserId(resultSet.getString(CREATOR_USER_ID_COLUMN));
        }
        dto.setUserName(resultSet.getString("created_by"));
        dto.setResponseDate(resultSet.getDate("response_date"));
        dto.setCustomerName(resultSet.getString("customer_name"));
        dto.setReason(resultSet.getString("reason"));
        dto.setActionContent(resultSet.getString("action_content"));
        dto.setNote(resultSet.getString("note"));
        dto.setCreatedAt(resultSet.getTimestamp("created_at"));
        dto.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return dto;
    }

    private boolean hasCreatorUserId(Connection connection) {
        return schemaCapabilities.columnExists(
                connection, TABLE_NAME, CREATOR_USER_ID_COLUMN);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
