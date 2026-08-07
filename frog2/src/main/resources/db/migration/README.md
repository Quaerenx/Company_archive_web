# Frog2 database migrations

These SQL files are version-controlled migration artifacts only. The application does not discover or execute them at startup or during HTTP requests.

Do not run these migrations against the shared database without a separate approval, a schema baseline review, and a maintenance-window rollback plan. Existing installations must be baselined so migrations for already-present tables or columns are marked as applied rather than executed again.

Active schema contracts:

- `V20260720_01__create_user_vm_hosts.sql`: personal VM host repository.
- `V20260720_04__rename_license_usage_pct.sql`: maintenance license column spelling.
- `V20260730_05__add_troubleshooting_creator_user_id.sql`: troubleshooting ownership.
- `V20260731_06__add_activity_creator_user_ids.sql`: stable user ID ownership
  for maintenance and monthly customer-response activity.
- `V20260804_07__create_customer_maintenance_schedule.sql`: per-customer
  monthly or quarterly maintenance schedule. It classifies a customer as
  quarterly only when at least three distinct inspection months span six
  months and at least 80% share the same three-month residue. All other
  customers remain monthly.
- `V20260804_08__set_konkuk_hospital_quarterly_schedule.sql`: reviewed
  business override for 건국대병원. It sets a three-month interval anchored
  to March, so the due months are March, June, September, and December.

At startup the application performs a read-only metadata readiness check for
the active schema contracts. It never executes migration SQL. Missing
capabilities are logged and ownership-sensitive writes fail closed until the
corresponding migration has been reviewed and applied separately.

Until migration 07 is applied, the dashboard intentionally treats every
maintenance-contract customer as monthly. Review the inferred quarterly rows
and each `anchor_month` before executing the migration against a shared
database. The application derives due months from `anchor_month` and never
stores a drifting `next_due_month` value.

Migration 06 only backfills rows whose historical display name maps to exactly
one user. Ambiguous or unmatched legacy rows remain unchanged and must be
reviewed before a later `NOT NULL` constraint is considered.

Versions 02 and 03 belonged to the retired `HostDAO`/`HostDTO` feature. Their SQL
is preserved unchanged under `db/legacy` for historical review and must not be
treated as an active migration sequence.
