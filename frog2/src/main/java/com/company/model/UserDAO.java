package com.company.model;

import com.company.util.DBConnection;
import com.company.util.PasswordUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class UserDAO {
    private static final SchemaCapabilityCache APPLICATION_SCHEMA_CAPABILITIES =
            new SchemaCapabilityCache();

    private final JdbcConnectionProvider connectionProvider;
    private final SchemaCapabilityCache schemaCapabilities;

    public UserDAO() {
        this(DBConnection::getConnection, APPLICATION_SCHEMA_CAPABILITIES);
    }

    UserDAO(JdbcConnectionProvider connectionProvider) {
        this(connectionProvider, new SchemaCapabilityCache());
    }

    UserDAO(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache schemaCapabilities) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.schemaCapabilities = Objects.requireNonNull(schemaCapabilities, "schemaCapabilities");
    }

    public UserDTO authenticateUser(String userId, String password) {
        String sql = "SELECT userId, password, userName FROM company_users WHERE userId = ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                String hashedPassword = resultSet.getString("password");
                if (!PasswordUtils.checkPassword(password, hashedPassword)) {
                    return null;
                }
                UserDTO user = new UserDTO();
                user.setUserId(resultSet.getString("userId"));
                user.setPassword("");
                user.setUserName(resultSet.getString("userName"));
                return user;
            }
        } catch (SQLException exception) {
            throw DataAccessException.from("authenticate user", exception);
        }
    }

    public boolean registerUser(UserDTO user) {
        String sql = "INSERT INTO company_users (userId, password, userName) VALUES (?, ?, ?)";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUserId());
            statement.setString(2, PasswordUtils.hashPassword(user.getPassword()));
            statement.setString(3, user.getUserName());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw DataAccessException.from("register user", exception);
        }
    }

    public boolean updatePassword(String userId, String newPassword) {
        String sql = "UPDATE company_users SET password = ? WHERE userId = ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PasswordUtils.hashPassword(newPassword));
            statement.setString(2, userId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw DataAccessException.from("update password", exception);
        }
    }

    public UserDTO getUserById(String userId) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean hasDepartment = columnExists(connection, "company_users", "department");
            String sql = hasDepartment
                    ? "SELECT userId, userName, department FROM company_users WHERE userId = ?"
                    : "SELECT userId, userName FROM company_users WHERE userId = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    UserDTO user = new UserDTO();
                    user.setUserId(resultSet.getString("userId"));
                    user.setUserName(resultSet.getString("userName"));
                    if (hasDepartment) {
                        user.setDepartment(resultSet.getString("department"));
                    }
                    return user;
                }
            }
        } catch (SQLException exception) {
            throw DataAccessException.from("load user", exception);
        }
    }

    public boolean updateUserName(String userId, String userName) {
        String sql = "UPDATE company_users SET userName = ? WHERE userId = ?";
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userName);
            statement.setString(2, userId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw DataAccessException.from("update user name", exception);
        }
    }

    public boolean updateUserProfile(String userId, String userName, String department) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean hasDepartment = columnExists(connection, "company_users", "department");
            String sql = hasDepartment
                    ? "UPDATE company_users SET userName = ?, department = ? WHERE userId = ?"
                    : "UPDATE company_users SET userName = ? WHERE userId = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, userName);
                if (hasDepartment) {
                    statement.setString(2, department);
                    statement.setString(3, userId);
                } else {
                    statement.setString(2, userId);
                }
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException exception) {
            throw DataAccessException.from("update user profile", exception);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName)
            throws SQLException {
        return schemaCapabilities.columnExists(connection, tableName, columnName);
    }
}
