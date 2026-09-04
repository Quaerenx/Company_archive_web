package com.company.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MigrationLedgerRepository {
    static final String TABLE = "frog2_schema_migrations";
    private static final List<String> REQUIRED_COLUMNS = List.of(
            "database_identity",
            "migration_version",
            "filename",
            "checksum_sha256",
            "decision",
            "approved_by",
            "executed_by",
            "change_reference",
            "backup_reference",
            "applied_at");

    SchemaState schemaState(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        int available = 0;
        for (String column : REQUIRED_COLUMNS) {
            if (hasColumn(metadata, TABLE, column)
                    || hasColumn(
                            metadata,
                            TABLE.toUpperCase(Locale.ROOT),
                            column.toUpperCase(Locale.ROOT))) {
                available++;
            }
        }
        if (available == 0) {
            return SchemaState.ABSENT;
        }
        return available == REQUIRED_COLUMNS.size()
                ? SchemaState.COMPLETE
                : SchemaState.PARTIAL;
    }

    List<Record> load(Connection connection, String databaseIdentity)
            throws SQLException {
        String sql = "SELECT migration_version, filename, checksum_sha256, decision "
                + "FROM " + TABLE + " WHERE database_identity = ? "
                + "ORDER BY migration_version";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(30);
            statement.setString(1, databaseIdentity);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Record> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(new Record(
                            resultSet.getString("migration_version"),
                            resultSet.getString("filename"),
                            resultSet.getString("checksum_sha256"),
                            resultSet.getString("decision")));
                }
                return List.copyOf(records);
            }
        }
    }

    void insert(
            Connection connection,
            String databaseIdentity,
            MigrationManifest.Entry migration,
            String decision,
            String approvedBy,
            String executedBy,
            String changeReference,
            String backupReference) throws SQLException {
        String sql = "INSERT INTO " + TABLE + " ("
                + "database_identity, migration_version, filename, "
                + "checksum_sha256, decision, approved_by, executed_by, "
                + "change_reference, backup_reference, applied_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(30);
            statement.setString(1, databaseIdentity);
            statement.setString(2, migration.version());
            statement.setString(3, migration.filename());
            statement.setString(4, migration.checksum());
            statement.setString(5, decision);
            statement.setString(6, approvedBy);
            statement.setString(7, executedBy);
            statement.setString(8, changeReference);
            statement.setString(9, backupReference);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Migration ledger insert changed an unexpected row count");
            }
        }
    }

    private static boolean hasColumn(
            DatabaseMetaData metadata, String table, String column)
            throws SQLException {
        try (ResultSet columns = metadata.getColumns(null, null, table, column)) {
            return columns.next();
        }
    }

    enum SchemaState {
        ABSENT,
        PARTIAL,
        COMPLETE
    }

    record Record(
            String version,
            String filename,
            String checksum,
            String decision) {
    }
}
