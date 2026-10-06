# Database integrity improvements

Date: 2026-10-06
Status: business-schema migrations applied and verified; compatible application release approved

## Scope and preservation policy

- `V20261006_14` adds UUID relationships to maintenance, monthly responses,
  troubleshooting, maintenance schedules and all three customer environments.
  Exact trimmed historical names receive identity-only UUIDs when no master
  exists. This does not create active customers or infer aliases. Names and all
  historical business rows remain intact.
- `V20261006_15` enables staging/development customer-name primary keys and
  unique customer UUIDs in each environment and the maintenance schedule.
  Duplicate data stops migration; it is never automatically discarded.
- `V20261006_16` adds `NUMERIC(18,6)` TB capacity/usage and percentage columns.
  The existing `license_size_gb` field actually stores TB. Plain values mean TB;
  explicit GB is divided by 1024. Percentage suffixes are removed numerically.
  Unsupported legacy units/values retain their original strings and numeric
  NULL; values over 100% remain valid up to the existing 1,000,000 limit.
- `V20261006_17` implements the user-approved retention policy: meeting deletion
  marks `deleted_at`/`deleted_by`, retaining parent content and comments. Existing
  orphan comments receive `archived_at` without deleting or inventing a parent.
  Lists, detail, search, counts and updates exclude deleted meetings. Comment
  creation verifies an active parent while holding a shared parent-table lock
  through commit; the parent update takes an incompatible exclusive lock.

No UI changes or projection tuning are included. Name-based public URLs remain
compatible. Filesystem customer-history migration and public route replacement
remain separate work because this release changes database relationships only.

Vertica [does not enforce foreign keys](https://docs.vertica.com/24.1.x/en/admin/constraints/constraint-enforcement/constraint-enforcement-limitations/).
Integrity therefore depends on controlled
application writes, enabled primary/unique constraints and aggregate audits.
Child records and any newly created identity commit together; failed or
unauthorized writes roll back both. UUID/name snapshots are dual-written. Reads
by customer and customer-scoped mutations use the UUID after migration.

Schema inspection is restricted to the connection schema, with the current
Frog2 `public` baseline when the bundled Vertica driver reports no schema.
Another schema's matching table must not enable application capabilities.
Partial UUID, numeric or meeting migrations fail readiness. Entirely absent
optional migrations retain legacy reads. Meeting deletion requires the retention
columns and never falls back to permanent deletion.

## Forward migration

1. Confirm the target database identity and take a verified VBR backup. Preserve
   the existing migration ledger and approved checksum manifests.
2. Quiesce application and import writes for the full four-migration window.
   Capture aggregate row counts for each business table and retain a restricted
   backup of the rows being backfilled. Run the checks below before execution.
3. Apply `_14`, `_15`, `_16`, `_17` in order with the existing externally managed
   migration process. DDL can commit independently in Vertica: do not assume
   that a transaction will undo all four files.
4. Verify business row counts, references, constraints, units and retained
   orphan comments. Record each actually applied checksum in the migration
   ledger with approval/executor/change/backup references.
5. Deploy the compatible application, restart to refresh schema capabilities,
   and verify login, customer details, maintenance, monthly responses and
   meeting/comment behavior before reopening writes.

DDL and backfill acquire table locks; enabled key constraints validate existing
rows and may need supporting projections internally. The current tables are
small, but perform migration in a maintenance window. No optional physical
projection tuning is performed.

## Aggregate-only preflight and postflight

For each of the seven UUID target tables, require zero blank names and zero
trimmed duplicate-name groups in master/environment/schedule tables:

```sql
SELECT COUNT(*) AS blank_names FROM maintenance_records
WHERE NULLIF(TRIM(customer_name), '') IS NULL;

SELECT COUNT(*) AS duplicate_groups FROM (
    SELECT TRIM(customer_name) FROM vertica_customer_detail_stg
    GROUP BY TRIM(customer_name) HAVING COUNT(*) > 1
) duplicates;
```

Run the equivalent checks against the remaining tables; duplicates in history
tables are expected and must not be deduplicated. Also review distinct historical
names absent from the master using restricted access. Backfill maps exact names
and preserves these as identity-only records rather than guessing a customer.

After `_14`, each target table must return zero missing/dangling UUIDs:

```sql
SELECT COUNT(*) AS missing_or_dangling FROM maintenance_records r
WHERE r.customer_id IS NULL OR NOT EXISTS (
    SELECT 1 FROM customer_identity i WHERE i.customer_id = r.customer_id
);
```

After `_15`, verify enabled constraints with `v_catalog.table_constraints` and
`ANALYZE_CONSTRAINTS` on the three environment tables and the schedule. Require
zero violation rows. After `_16`, verify raw strings are unchanged, supported
numeric values map exactly, and review aggregate unsupported-value counts.
After `_17`, preserve the original total meeting/comment counts and require zero
unclassified orphan comments:

```sql
SELECT COUNT(*) AS unclassified_orphans FROM meeting_comments c
WHERE c.archived_at IS NULL AND NOT EXISTS (
    SELECT 1 FROM meeting_records m WHERE m.meeting_id = c.meeting_id
);
```

The raw orphan count may remain nonzero by design: archived comments are retained
for recovery and are unavailable through active meeting routes.

## Rollback and partial failure

Retain additive columns, raw strings, UUIDs and deletion markers. Do not drop
columns, remove identities or clear deletion flags to roll back this release.
An older binary that does not honor meeting deletion markers would expose hidden
meetings and could permanently delete parents. Use a compatible rollback build
that retains soft-delete filtering, or keep writes closed until the current
release is repaired. Legacy writers can leave nullable UUID/numeric columns
unpopulated, so keep importers stopped until they dual-write or are reconciled.

On partial DDL failure, keep writes closed, inspect actual columns/constraints
and reconcile the migration ledger. Apply only the remaining approved statements;
do not rerun an entire file blindly, particularly key/check constraints.
Readiness blocks partial column groups. Backup recovery is a separate explicitly
approved operation, never an automatic application fallback.

## Verification commands

```bash
JAVA_HOME=/opt/jdk-25.0.4.1-temurin ./gradlew check --offline --no-daemon

FROG2_INTEGRITY_E2E_ENABLED=true \
FROG2_INTEGRITY_DB_CONFIG=/opt/frog2-dev/config/db.properties \
JAVA_HOME=/opt/jdk-25.0.4.1-temurin \
./gradlew databaseIntegrityE2E --offline --no-daemon
```

The opt-in E2E copies structure only into a freshly generated
`frog2_test_integrity_<random>` schema, creates independent identity defaults,
seeds synthetic data, applies all four real SQL files and uses real DAOs. It
checks customer-name changes, numeric conversion including extreme malformed
values, duplicate rejection, rollback, retained orphan content, concurrent
comment creation/deletion marking, unchanged source row counts and cleanup.
It never runs these migration files against the shared business schema.

Verified on 2026-10-06: `check` passed 999 Java tests, 157 JavaScript tests,
JavaScript syntax checks, Tomcat 10.1.59 JSP precompilation and WAR checks.
The real Vertica isolated E2E passed all four migrations. The production release
source, excluding the pre-existing development UI changes, additionally passed
990 Java tests and 154 JavaScript tests, JSP precompilation and WAR checks.

The user authorized execution, deployment, commit and push on 2026-10-06.
Development and production use the same business database. Both application
services were stopped for the four migrations. A dedicated VBR object backup
contains all 14 application tables and passed `full-check`; restricted row/DDL
snapshots for 11 migration-relevant tables were also written and verified.
Original business content across nine tables is unchanged. All seven UUID
relationships reconcile; nine historical identities were added without active
customers; nine new constraints are enabled; all 601 numeric license records
reconcile and unsupported raw values remain intact. The existing orphan comment
was retained and classified. The migration ledger contains all 15 pinned versions
with no pending entries.

The durable operator record at
`/root/FROG2_OUTPUT/reports/frog2-db-integrity-20261006/rollout.json` records the
recovery point, release commit, deployed hashes and authenticated smoke results.
The dedicated backup leaves existing scheduled-backup configuration unchanged;
its stale database settings require a separate operational correction.
