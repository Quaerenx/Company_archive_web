package com.company.model;

import com.company.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MonthlyCustomerResponseDAO {
    private final JdbcConnectionProvider connectionProvider;

    public MonthlyCustomerResponseDAO() {
        this(DBConnection::getConnection);
    }

    MonthlyCustomerResponseDAO(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
    }

    public List<MonthlyCustomerResponseDTO> getMonthlyResponses(
            String userName, int year, int month) {
        List<MonthlyCustomerResponseDTO> responses = new ArrayList<>();
        String sql = "SELECT id, created_by, response_date, customer_name, reason, "
                + "action_content, note, created_at, updated_at "
                + "FROM monthly_customer_response "
                + "WHERE created_by = ? AND YEAR(response_date) = ? AND MONTH(response_date) = ? "
                + "ORDER BY response_date DESC";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userName);
            statement.setInt(2, year);
            statement.setInt(3, month);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    responses.add(mapRow(resultSet));
                }
            }
            return responses;
        } catch (SQLException exception) {
            throw DataAccessException.from("load monthly customer responses", exception);
        }
    }

    public boolean addResponse(MonthlyCustomerResponseDTO dto) {
        String sql = "INSERT INTO monthly_customer_response "
                + "(created_by, response_date, customer_name, reason, action_content, note, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, GETDATE())";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dto.getUserName());
            statement.setDate(2, new java.sql.Date(dto.getResponseDate().getTime()));
            statement.setString(3, dto.getCustomerName());
            statement.setString(4, dto.getReason());
            statement.setString(5, dto.getActionContent());
            statement.setString(6, dto.getNote());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw DataAccessException.from("add monthly customer response", exception);
        }
    }

    public boolean updateResponse(MonthlyCustomerResponseDTO dto) {
        String sql = "UPDATE monthly_customer_response SET "
                + "response_date = ?, customer_name = ?, reason = ?, "
                + "action_content = ?, note = ?, updated_at = GETDATE() "
                + "WHERE id = ? AND created_by = ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, new java.sql.Date(dto.getResponseDate().getTime()));
            statement.setString(2, dto.getCustomerName());
            statement.setString(3, dto.getReason());
            statement.setString(4, dto.getActionContent());
            statement.setString(5, dto.getNote());
            statement.setInt(6, dto.getId());
            statement.setString(7, dto.getUserName());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw DataAccessException.from("update monthly customer response", exception);
        }
    }

    public boolean deleteResponse(int id, String userName) {
        String sql = "DELETE FROM monthly_customer_response WHERE id = ? AND created_by = ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, userName);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw DataAccessException.from("delete monthly customer response", exception);
        }
    }


    private static MonthlyCustomerResponseDTO mapRow(ResultSet resultSet) throws SQLException {
        MonthlyCustomerResponseDTO dto = new MonthlyCustomerResponseDTO();
        dto.setId(resultSet.getInt("id"));
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
}
