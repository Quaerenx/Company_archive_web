-- Preconditions:
-- - Confirm frog2.databaseIdentity is a stable, non-secret database identifier.
-- - Take an approved database backup or snapshot.
-- - Apply this artifact separately; the application never executes DDL.

CREATE TABLE frog2_schema_migrations (
    database_identity VARCHAR(128) NOT NULL,
    migration_version VARCHAR(32) NOT NULL,
    filename VARCHAR(256) NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    decision VARCHAR(16) NOT NULL,
    approved_by VARCHAR(128) NOT NULL,
    executed_by VARCHAR(128) NOT NULL,
    change_reference VARCHAR(256) NOT NULL,
    backup_reference VARCHAR(256) NOT NULL,
    applied_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_frog2_schema_migrations
        PRIMARY KEY (database_identity, migration_version) ENABLED,
    CONSTRAINT ck_frog2_schema_migration_decision
        CHECK (decision IN ('applied', 'baselined')) ENABLED
);

-- After creation, use migrationLedgerRecord to baseline previously applied
-- artifacts and record this version. Never infer execution from metadata alone.
