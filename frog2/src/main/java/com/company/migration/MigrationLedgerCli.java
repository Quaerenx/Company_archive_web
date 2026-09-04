package com.company.migration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class MigrationLedgerCli {
    private static final System.Logger LOGGER =
            System.getLogger(MigrationLedgerCli.class.getName());
    private static final String CONFIG_ENV = "FROG2_MIGRATION_DB_CONFIG";
    private static final String DIRECTORY_ENV = "FROG2_MIGRATION_DIR";
    private static final String IDENTITY_KEY = "frog2.databaseIdentity";
    private static final Set<String> RECORD_DECISIONS =
            Set.of("applied", "baselined");

    private MigrationLedgerCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !("status".equals(args[0])
                || "record".equals(args[0]))) {
            throw new IllegalArgumentException("Expected status or record command");
        }
        Path migrationDirectory = migrationDirectory();
        List<MigrationManifest.Entry> migrations =
                MigrationManifest.load(migrationDirectory);
        Properties database = loadDatabaseConfiguration();
        Class.forName(required(database, "db.driver"));

        try (Connection connection = DriverManager.getConnection(
                required(database, "db.url"),
                required(database, "db.user"),
                required(database, "db.password"))) {
            String databaseIdentity = required(database, IDENTITY_KEY);
            if ("status".equals(args[0])) {
                verifyStatus(connection, databaseIdentity, migrations);
            } else {
                record(connection, databaseIdentity, migrations);
            }
        }
    }

    private static void verifyStatus(
            Connection connection,
            String databaseIdentity,
            List<MigrationManifest.Entry> migrations) throws Exception {
        connection.setReadOnly(true);
        MigrationLedgerRepository repository = new MigrationLedgerRepository();
        requireCompleteSchema(repository.schemaState(connection));
        MigrationLedgerVerifier.Result result = MigrationLedgerVerifier.reconcile(
                migrations, repository.load(connection, databaseIdentity));
        if (!result.complete()) {
            throw new IllegalStateException(
                    "Migration ledger has pending versions: "
                            + String.join(", ", result.pendingVersions()));
        }
        LOGGER.log(
                System.Logger.Level.INFO,
                "Migration ledger verified: recorded={0} pending=0",
                result.recordedCount());
    }

    private static void record(
            Connection connection,
            String databaseIdentity,
            List<MigrationManifest.Entry> migrations) throws Exception {
        if (!"yes".equals(requiredEnvironment(
                "FROG2_MIGRATION_LEDGER_RECORD_APPROVED"))) {
            throw new IllegalStateException(
                    "FROG2_MIGRATION_LEDGER_RECORD_APPROVED=yes is required");
        }
        String version = requiredEnvironment("FROG2_MIGRATION_VERSION");
        String decision = requiredEnvironment("FROG2_MIGRATION_DECISION")
                .toLowerCase(Locale.ROOT);
        if (!RECORD_DECISIONS.contains(decision)) {
            throw new IllegalArgumentException(
                    "Migration decision must be applied or baselined");
        }
        MigrationManifest.Entry migration = migrations.stream()
                .filter(candidate -> candidate.version().equals(version))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown active migration version: " + version));

        MigrationLedgerRepository repository = new MigrationLedgerRepository();
        requireCompleteSchema(repository.schemaState(connection));
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            repository.insert(
                    connection,
                    databaseIdentity,
                    migration,
                    decision,
                    requiredEnvironment("FROG2_MIGRATION_APPROVED_BY"),
                    requiredEnvironment("FROG2_MIGRATION_EXECUTED_BY"),
                    requiredEnvironment("FROG2_MIGRATION_CHANGE_REFERENCE"),
                    requiredEnvironment("FROG2_MIGRATION_BACKUP_REFERENCE"));
            connection.commit();
        } catch (Exception exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
        LOGGER.log(
                System.Logger.Level.INFO,
                "Migration ledger recorded: version={0} decision={1}",
                migration.version(),
                decision);
    }

    private static void requireCompleteSchema(
            MigrationLedgerRepository.SchemaState state) {
        if (state == MigrationLedgerRepository.SchemaState.ABSENT) {
            throw new IllegalStateException(
                    "Migration ledger is not installed; apply V20260904_12 separately");
        }
        if (state == MigrationLedgerRepository.SchemaState.PARTIAL) {
            throw new IllegalStateException("Migration ledger schema is partially applied");
        }
    }

    private static Path migrationDirectory() throws Exception {
        String configured = System.getenv(DIRECTORY_ENV);
        Path path = configured == null || configured.isBlank()
                ? Path.of("src/main/resources/db/migration")
                : Path.of(configured.trim());
        return path.toRealPath().normalize();
    }

    private static Properties loadDatabaseConfiguration() throws Exception {
        Path configured = Path.of(requiredEnvironment(CONFIG_ENV));
        if (!configured.isAbsolute()) {
            throw new IllegalArgumentException(
                    CONFIG_ENV + " must be an absolute path");
        }
        Path realPath = configured.toRealPath().normalize();
        if (!realPath.equals(configured.normalize())
                || !Files.isRegularFile(realPath)) {
            throw new IllegalArgumentException(
                    CONFIG_ENV + " must identify a canonical regular file");
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(realPath)) {
            properties.load(input);
        }
        for (String key : List.of(
                "db.url", "db.user", "db.password", "db.driver", IDENTITY_KEY)) {
            required(properties, key);
        }
        return properties;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing database configuration key: " + key);
        }
        return value.trim();
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
