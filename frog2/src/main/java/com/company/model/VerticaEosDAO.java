package com.company.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.company.util.DBConnection;

public class VerticaEosDAO {
    private static final Pattern VERSION_TOKEN = Pattern.compile(
            "(?<![0-9.])[0-9]+(?:\\.[0-9]+)+(?:-[0-9]+)?(?![0-9])");
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

            String normalizedVersion = versionText.trim();
            List<String> candidates = fallbackVersionCandidates(normalizedVersion);
            String column = capability.versionColumn();
            String sql = "SELECT end_of_service_date, 1 AS priority, "
                    + "LENGTH(" + column + ") AS match_length FROM "
                    + capability.qualifiedTable() + " WHERE " + column + " = ? ";
            if (!candidates.isEmpty()) {
                String placeholders = String.join(
                        ", ", java.util.Collections.nCopies(candidates.size(), "?"));
                sql += "UNION ALL SELECT end_of_service_date, 2 AS priority, "
                        + "LENGTH(" + column + ") AS match_length FROM "
                        + capability.qualifiedTable()
                        + " WHERE " + column + " IN (" + placeholders + ") ";
            }
            sql += "ORDER BY priority, match_length DESC LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, normalizedVersion);
                for (int index = 0; index < candidates.size(); index++) {
                    statement.setString(index + 2, candidates.get(index));
                }
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

    static List<String> fallbackVersionCandidates(String versionText) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        Matcher matcher = VERSION_TOKEN.matcher(versionText);
        while (matcher.find()) {
            String token = matcher.group();
            candidates.add(token);
            int buildSeparator = token.indexOf('-');
            if (buildSeparator >= 0) {
                token = token.substring(0, buildSeparator);
                candidates.add(token);
            }
            int componentSeparator;
            while ((componentSeparator = token.lastIndexOf('.')) >= 0) {
                token = token.substring(0, componentSeparator);
                candidates.add(token);
            }
        }
        return List.copyOf(candidates);
    }
}
