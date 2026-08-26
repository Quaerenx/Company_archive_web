package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import com.company.util.DBConnection;

public class VerticaEosDAO {
    private static final VerticaEosCapabilityCache APPLICATION_CAPABILITIES =
            new VerticaEosCapabilityCache();
    private final JdbcConnectionProvider connectionProvider;
    private final VerticaEosCapabilityCache capabilities;

    public VerticaEosDAO() {
        this(DBConnection::getConnection, APPLICATION_CAPABILITIES);
    }

    VerticaEosDAO(JdbcConnectionProvider connectionProvider) {
        this(connectionProvider, new VerticaEosCapabilityCache());
    }

    VerticaEosDAO(
            JdbcConnectionProvider connectionProvider,
            VerticaEosCapabilityCache capabilities) {
        this.connectionProvider = Objects.requireNonNull(
                connectionProvider, "connectionProvider");
        this.capabilities = Objects.requireNonNull(
                capabilities, "capabilities");
    }

    public java.util.Date findEosDateByVersion(String versionText) {
        if (versionText == null || versionText.trim().isEmpty()) {
            return null;
        }

        try (Connection connection = connectionProvider.getConnection()) {
            VerticaEosCapabilityCache.Capability capability =
                    capabilities.resolve(connection);
            if (capability == null) {
                return null;
            }

            String column = capability.versionColumn();
            String sql = "SELECT end_of_service_date, 1 AS priority, "
                    + "LENGTH(" + column + ") AS match_length FROM "
                    + capability.qualifiedTable() + " WHERE " + column + " = ? "
                    + "UNION ALL "
                    + "SELECT end_of_service_date, 2 AS priority, "
                    + "LENGTH(" + column + ") AS match_length FROM "
                    + capability.qualifiedTable()
                    + " WHERE ? ILIKE ('%' || " + column + " || '%') "
                    + "ORDER BY priority, match_length DESC LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                String normalizedVersion = versionText.trim();
                statement.setString(1, normalizedVersion);
                statement.setString(2, normalizedVersion);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    Timestamp eosTimestamp = resultSet.getTimestamp(1);
                    return eosTimestamp == null
                            ? null
                            : new java.util.Date(eosTimestamp.getTime());
                }
            }
        } catch (SQLException exception) {
            throw DataAccessException.from(exception);
        }
    }
}
