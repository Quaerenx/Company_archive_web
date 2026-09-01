# Archive migration manifest and deployment ledger

Last updated: 2026-09-01
Status: repository procedure only; execution records remain external and no ledger table exists

## Immutable migration rule

Approved SQL files are immutable. Their SHA-256 values live beside them in:

- `src/main/resources/db/migration/manifest.sha256`
- `src/main/resources/db/legacy/manifest.sha256`

`DatabaseMigrationInventoryTest` checks that every SQL file has exactly one manifest entry and that its content still matches. A correction to an approved migration must be a new, ordered migration; do not edit the old file and silently replace its checksum.

The `legacy` directory is historical only. Neither application startup nor an external migration runner may include it as an active location.

## External deployment ledger template

Keep the actual ledger in the approved deployment record system, not in application source and not in the shared database unless a DBA separately approves a ledger table.

| Field | Required value |
| --- | --- |
| environment/database identity | non-secret stable identifier, never a JDBC URL |
| migration version | for example `V20260731_06` |
| filename | exact versioned SQL filename |
| SHA-256 | value from the active manifest |
| decision | applied, baselined, skipped, failed, rolled-forward |
| approved by | named change approver |
| executed by | named DBA/operator |
| applied at | timestamp with timezone |
| ticket/change record | durable external reference |
| preflight result | aggregate-only result; no customer data |
| backup/snapshot | recovery reference and owner |
| post-check result | readiness, reconciliation and smoke outcome |
| rollback/forward repair | chosen recovery action |

## Required gates

1. Compare the SQL to the manifest before approval.
2. Re-run aggregate-only ownership preflight in the approved maintenance window.
3. Stop when ambiguous or unmatched owner rows violate the migration precondition.
4. Apply only the explicitly approved version.
5. Record its manifest checksum and outcome in the external ledger.
6. Run metadata readiness and reconciliation checks.
7. Run authenticated write E2E only against the isolated database.

## Current migration cautions

- `V20260730_05` places `NOT NULL` in the same artifact as its unique-name backfill. It must not run unless aggregate preflight proves zero ambiguous and zero unmatched troubleshooting rows.
- `V20260731_06` leaves ownership columns nullable. The application now fails owner-scoped reads and writes closed when those columns are absent; legacy display names are not authorization keys.
- `V20260804_07` can be considered ready only when all schedule columns required by the code exist, not merely `interval_months`.
- `V20260720_01` is ready only when the complete `user_vm_hosts` column contract exists, not merely its ownership column.
- `V20260825_09` adds nullable audit evidence without backfilling existing
  rows. Validate all four column types before application and export audit
  values before any rollback that drops the columns. Quiesce all application
  nodes for the four statements: a partial schema is intentionally treated as
  incompatible, and a complete schema rejects writes without a stable actor.
- `V20260901_10` adds one nullable `swap_memory` column to each of the three
  customer-detail tables. Verify compatible VARCHAR types and aggregate row
  counts before and after; do not backfill values inferred from other fields.
- Metadata readiness intentionally does not inspect customer rows, backfill completeness, primary keys or check constraints. Those remain approved preflight/post-check items; startup does not perform data queries.
- Repository contents never prove whether a migration was executed. Consult
  the external ledger and its post-check evidence before deploying code that
  requires a versioned schema capability.
