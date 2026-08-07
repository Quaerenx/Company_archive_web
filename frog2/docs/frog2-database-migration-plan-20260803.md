# Frog2 development database ownership migration plan

Date: 2026-08-03
Scope: development database only
Status: baseline complete; no migration executed

## Safety constraints

- Keep the application and migration session write-disabled until the change window begins.
- Do not run the migration against a shared database without DBA approval, a current backup or snapshot, and an agreed rollback owner.
- Re-run the aggregate preflight immediately before the change window because ownership data can change after this baseline.
- Do not include customer names, user names, database endpoints, or credentials in logs or review artifacts.

## Read-only baseline

The baseline was collected through JDBC using only aggregate `SELECT` statements. The connection was marked read-only and rolled back before close.

| Item | Result |
| --- | ---: |
| Vertica version | 24.03.0003 |
| `company_users` rows | 12 |
| Duplicate `userName` groups | 0 |

| Table | Stable owner column | Rows | Unique name match | Ambiguous | Unmatched |
| --- | --- | ---: | ---: | ---: | ---: |
| `troubleshooting` | `creator_user_id` (missing) | 19 | 19 | 0 | 0 |
| `maintenance_records` | `created_by_user_id` (missing) | 575 | 530 | 0 | 45 |
| `monthly_customer_response` | `created_by_user_id` (missing) | 15 | 15 | 0 | 0 |

## Execution sequence

### Gate 1: preflight and recovery readiness

1. Pause ownership-sensitive writes for the full maintenance window.
2. Confirm a DBA-approved backup or snapshot and record the recovery owner.
3. Re-run the aggregate baseline.
4. Stop if duplicate `company_users.userName` groups exist.
5. Stop migration 05 if any troubleshooting row is ambiguous or unmatched.

### Gate 2: migration 05

1. Review `V20260730_05__add_troubleshooting_creator_user_id.sql`.
2. Add `troubleshooting.creator_user_id`.
3. Backfill only names that map to exactly one user.
4. Verify all 19 rows have a non-null stable owner ID.
5. Apply the `NOT NULL` constraint only after the zero-null check passes.
6. Verify troubleshooting create, update, delete, history, and ownership denial in a write-enabled staging session.

### Gate 3: migration 06

1. Review `V20260731_06__add_activity_creator_user_ids.sql`.
2. Add both `created_by_user_id` columns.
3. Backfill the currently matchable maintenance and monthly-response rows.
4. Verify all 15 monthly-response rows are mapped.
5. Export only the identifiers of the 45 unresolved maintenance rows to an access-controlled DBA review, resolve each row to a stable user ID or an explicitly approved service/legacy owner, and record the decision separately.
6. Do not introduce a `NOT NULL` constraint until the unresolved count is zero and application reads no longer require the legacy display-name fallback.
7. Verify maintenance and monthly-response create, update, delete, history, and ownership denial in a write-enabled staging session.

### Gate 4: release and post-check

1. Start the application and require `Database schema readiness check passed`.
2. Confirm the three columns through metadata and repeat null/unmatched aggregate checks.
3. Run the authenticated E2E smoke suite.
4. Monitor 4xx/5xx rates, slow requests, slow SQL, and connection-pool pressure during the observation window.
5. Re-enable writes only after the smoke suite and post-checks pass.

## Rollback strategy

- Prefer forward repair when only backfilled values are wrong: keep the new nullable columns, correct mappings, and repeat verification.
- If migration 05 fails before `NOT NULL`, stop and repair unresolved mappings before continuing.
- If the application fails after schema changes, disable writes and roll back the application release first; do not drop populated columns ad hoc.
- Restore the DBA-approved snapshot only when forward repair is unsafe and the recovery owner approves it.
- Any column drop or data restoration is a separate destructive operation and requires explicit approval.

## Current go/no-go decision

- Migration 05 data gate: **GO**, subject to a repeated preflight and approved maintenance window.
- Migration 06 column/backfill gate: **GO WITH HOLD**; 45 maintenance rows require controlled reconciliation.
- Write-enabled release gate: **NO-GO** until migrations, reconciliation, and authenticated E2E verification are complete.
