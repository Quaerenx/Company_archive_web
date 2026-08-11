package com.company.model;

import com.company.util.DBConnection;
import com.company.util.PasswordUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class UserDAO {
    @FunctionalInterface
    interface PasswordVerifier {
        boolean matches(String plainTextPassword, String hashedPassword);
    }

    @FunctionalInterface
    interface PasswordHasher {
        String hash(String plainTextPassword);
    }

    private static final SchemaCapabilityCache APPLICATION_SCHEMA_CAPABILITIES =
            new SchemaCapabilityCache();

    private final JdbcConnectionProvider connectionProvider;
    private final SchemaCapabilityCache schemaCapabilities;
    private final PasswordVerifier passwordVerifier;
    private final PasswordHasher passwordHasher;

    public UserDAO() {
        this(
                DBConnection::getConnection,
                APPLICATION_SCHEMA_CAPABILITIES,
                PasswordUtils::checkPassword,
                PasswordUtils::hashPassword);
    }

    UserDAO(JdbcConnectionProvider connectionProvider) {
        this(
                connectionProvider,
                new SchemaCapabilityCache(),
                PasswordUtils::checkPassword,
                PasswordUtils::hashPassword);
    }

    UserDAO(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache schemaCapabilities) {
        this(
                connectionProvider,
                schemaCapabilities,
                PasswordUtils::checkPassword,
                PasswordUtils::hashPassword);
    }

    UserDAO(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache schemaCapabilities,
            PasswordVerifier passwordVerifier) {
        this(
                connectionProvider,
                schemaCapabilities,
                passwordVerifier,
                PasswordUtils::hashPassword);
    }

    UserDAO(
            JdbcConnectionProvider connectionProvider,
            SchemaCapabilityCache schemaCapabilities,
            PasswordVerifier passwordVerifier,
            PasswordHasher passwordHasher) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.schemaCapabilities = Objects.requireNonNull(schemaCapabilities, "schemaCapabilities");
        this.passwordVerifier = Objects.requireNonNull(
                passwordVerifier, "passwordVerifier");
        this.passwordHasher = Objects.requireNonNull(
                passwordHasher, "passwordHasher");
    }

    public UserDTO authenticateUser(String userId, String password) {
        String sql = "SELECT userId, password, userName FROM company_users WHERE userId = ?";
        UserCredentials credentials;
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                credentials = new UserCredentials(
                        resultSet.getString("userId"),
                        resultSet.getString("password"),
                        resultSet.getString("userName"));
            }
        } catch (SQLException exception) {
            throw DataAccessException.from("authenticate user", exception);
        }

        if (!passwordVerifier.matches(password, credentials.passwordHash())) {
            return null;
        }
        UserDTO user = new UserDTO();
        user.setUserId(credentials.userId());
        user.setPassword("");
        user.setUserName(credentials.userName());
        return user;
    }

    public boolean updatePassword(String userId, String newPassword) {
        String sql = "UPDATE company_users SET password = ? WHERE userId = ?";
        String passwordHash = passwordHasher.hash(newPassword);
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
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

    private boolean columnExists(Connection connection, String tableName, String columnName)
            throws SQLException {
        return schemaCapabilities.columnExists(connection, tableName, columnName);
    }

    private record UserCredentials(
            String userId, String passwordHash, String userName) {
    }
}
