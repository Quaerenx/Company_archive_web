# Archive database integrity remediation

Date: 2026-08-10
Repository: `/opt/frog2-dev/repo/frog2`
Source audit: `02-database-integrity-audit.md`
Database access during remediation: **0 connections / 0 writes**

## Result

The safe, code-only portions of all six milestones were implemented. No migration, schema audit, authenticated POST, Tomcat deployment or restart was performed.

| Milestone | Result |
| --- | --- |
| SQL call map | HTTP → filter → controller → DAO → SQL map documented; runtime DDL absent |
| read-only defense | fail-closed matrix and JDBC bypass regression coverage expanded |
| connection/transaction | wrapper failure closes raw connection; multi-statement customer detail writes always commit/rollback |
| stable ownership | display-name fallback removed from three owner history queries; session userId remains the only owner source |
| migration management | active/legacy checksum manifests, expanded metadata readiness and external ledger procedure added |
| isolated write E2E | explicit database identity preflight added; no isolated database was supplied, so the tagged suite was not run |

## Main changes

### GET/HEAD purity

- Removed the meeting-detail view counter UPDATE from `MeetingServlet` and removed the unused `MeetingRecordDAO.incrementViewCount` mutation.
- Added `GetRequestDatabasePurityContractTest` to reject known mutation calls in servlet GET handlers and database DDL in runtime Java.
- Static call-map review found no remaining implicit counter, last-access or statistics DML in GET/HEAD.

### Read-only defense

- `ApplicationEnvironment` is unchanged because its implementation was already fail-closed.
- Tests now include null, empty, whitespace, mixed case, invalid environment, invalid boolean, dev/prod/staging combinations.
- Tests now explicitly cover comment-prefixed SQL, keyword strings, `WITH UPDATE`, `WITH DELETE`, callable statements, batch mutation, `setReadOnly(false)`, unwrap, metadata escape and updatable ResultSet paths.
- All blocked paths preserve SQLState `25006`; the existing exception filter contract preserves HTTP `409` and code `read_only`.

### Connection and transaction safety

- Added `JdbcConnectionDecorator`: if timing/read-only proxy setup fails after Hikari has supplied a connection, the raw connection is closed and close failures are suppressed onto the original exception.
- `CustomerDetailDAO` now commits or rolls back the transaction it owns even when a provider supplies `autoCommit=false`; it restores auto-commit only when it changed that state.
- Password update BCrypt hashing now finishes before a JDBC connection is acquired.
- Authentication BCrypt verification was already performed after the connection closed and remains unchanged.

### Stable userId ownership

Removed legacy display-name fallback and legacy name parameters from:

- `MaintenanceRecordDAO.getMaintenanceRecordsByOwner`
- `MonthlyCustomerResponseDAO.getMonthlyResponses`
- `TroubleshootingDAO.getTroubleshootingPageByOwner`

`MyPageServlet` passes only the session principal userId. When a required ownership column is absent, owner-scoped reads and writes fail closed instead of matching `inspector_name`, `created_by` or `creator`.

Owner-bound UPDATE/DELETE remains atomic (`object_id AND owner_user_id` in the same SQL). A zero-row result deliberately remains indistinguishable as “missing or forbidden”; no second existence query was introduced.

### Migration management

- Added immutable SHA-256 manifests for all 6 active and 4 legacy SQL files.
- `DatabaseMigrationInventoryTest` verifies exact inventory and content hashes.
- `DatabaseSchemaReadiness` now checks the full `user_vm_hosts` and maintenance-schedule column contracts rather than one representative column.
- Added an external deployment ledger procedure. No application-side SQL runner or ledger table was added.
- Startup metadata still intentionally does not query customer rows or verify backfill, PK, check constraints or the reviewed schedule override.

### Isolated write E2E

`IsolatedDatabaseConfig` now requires both configs to declare a different non-secret `frog2.databaseIdentity`, in addition to different config paths, different JDBC URLs and `frog2.e2e.isolated=true`.

Prepared scenario and remaining gates are documented in `docs/testing/isolated-write-e2e.md`. The write E2E task remains tagged and excluded from normal tests.

## Verification evidence

| Check | Result |
| --- | --- |
| RED tests before implementation | 8 expected failures plus 2 expected compile failures reproduced |
| clean build | success, 13 tasks |
| Java compilation | success, Java 22 |
| unit tests | 374 passed, 0 failed, 0 skipped |
| JspC | 38 generated sources, 63 classes, 0 errors |
| WAR allowlist | success |
| generated WAR | `6f887537ec15067580a733241338b2ba7ad656b8e608c0f12779e9cd5ed0e40a` |
| `git diff --check` | success |
| schemaAudit / e2eWrite | not run by design |
| actual DB DDL/DML | 0 |

The generated WAR was **not deployed**. Runtime state stayed unchanged:

- production PID `1012286`, WAR `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`, login GET 200
- development PID `3605261`, WAR `50837b9406a60b75ddd47fb54ec95b64333c797dacdb4a70b2465a4242a2bdb9`, login GET 200

## Migrations not executed

Active:

- `V20260720_01__create_user_vm_hosts.sql`
- `V20260720_04__rename_license_usage_pct.sql`
- `V20260730_05__add_troubleshooting_creator_user_id.sql`
- `V20260731_06__add_activity_creator_user_ids.sql`
- `V20260804_07__create_customer_maintenance_schedule.sql`
- `V20260804_08__set_konkuk_hospital_quarterly_schedule.sql`

All four files under `db/legacy` also remained historical and unexecuted.

## Remaining risks and explicit gaps

1. **Isolated write E2E not run:** an approved isolated database/snapshot and isolated app instance are still required.
2. **Actual schema state not rechecked:** `schemaAudit` was deliberately not rerun. Until separately approved, current column/backfill readiness is unknown.
3. **User lifecycle policy absent:** `UserDTO`/`company_users` has no inactive/deleted state contract, so an inactive/deleted-owner fixture cannot be implemented without a new policy and likely schema decision. Stable ownership itself remains userId-based.
4. **VM host limit race:** `countActiveHostsByOwner` followed by INSERT can exceed the per-user limit under concurrent requests. A durable fix needs an isolated Vertica concurrency test and possibly a schema/locking decision.
5. **Migration 05 gate:** its backfill and `NOT NULL` are bundled. It must not run unless aggregate preflight proves zero unresolved mappings.
6. **Metadata limits:** startup readiness proves column presence, not data reconciliation or constraints.
7. **External ledger:** the template exists, but actual approvals/application records must live in the deployment system.

## Files and supporting records

- SQL map: `docs/database/sql-call-map-20260810.md`
- migration ledger: `docs/database/migration-ledger.md`
- isolated E2E gate: `docs/testing/isolated-write-e2e.md`
- active and legacy manifests: `src/main/resources/db/*/manifest.sha256`

No commit, branch, push or PR was created. Confidence for the code-only and mock-tested scope is **93%**. The minimum check to raise end-to-end confidence is the tagged write suite against an approved isolated database, followed by a separately approved read-only schema readiness audit.
