# Archive isolated write E2E gate

Date: 2026-09-03
Status: normal CI is automated; the isolated write scenario is connected to a
manual, approval-gated workflow and requires a dedicated isolated database and
runner

## Mandatory isolation inputs

The `e2eWrite` task remains disabled unless all of these are explicit:

- `FROG2_E2E_WRITE_ENABLED=true`
- loopback-only `FROG2_E2E_BASE_URL`
- a dedicated Tomcat port other than shared development `18081` and production
  `8080`
- `FROG2_E2E_DEPLOYED_WAR` below `/opt/frog2-dev/e2e`
- a separate `FROG2_E2E_DB_CONFIG`
- the shared reference `FROG2_E2E_SHARED_DB_CONFIG`
- `frog2.e2e.isolated=true` in the isolated config
- different `db.url` values
- different non-secret `frog2.databaseIdentity` values

Both config files must be distinct regular files below `/opt/frog2-dev`. The identity check prevents query-string or role-option changes from disguising the same database as a different target.

Before any write begins, Gradle builds the current WAR and compares its
SHA-256 with `FROG2_E2E_DEPLOYED_WAR`. A stale deployment, a shared Tomcat
port, or a WAR outside the isolated runtime root fails closed.

## Prepared executable scenario

`AuthenticatedMaintenanceE2ETest` is tagged `e2e-write` and excluded from normal builds. Once an isolated database and isolated Tomcat are supplied, it performs:

1. temporary owner and attacker creation;
2. login and CSRF acquisition;
3. owner create and read;
4. attacker edit/update/delete rejection;
5. owner update and delete;
6. bounded read checks;
7. cleanup in `finally`.

The runner command is `./gradlew e2eWrite`. It must never be pointed at the shared development/production database or Tomcat.

## Remaining scenario gates

| Scenario | State | Minimum prerequisite |
| --- | --- | --- |
| CRUD | prepared | isolated DB and isolated app |
| unauthorized update/delete | prepared | two isolated test users |
| transaction rollback | DAO mock contract passes; real E2E pending | isolated DB failure fixture |
| duplicate submission | pending policy | decide idempotency key or allowed duplicate rule |
| file metadata + DB recovery | not applicable today | repository is filesystem-only; reassess if DB metadata is introduced |

The isolated-database real-HTTP runner had not run during the original
2026-09-03 investigation because no approved isolated database/snapshot was
supplied. The separate test-table JDBC/servlet probe below has a different
execution boundary.

## Dedicated test-table write probe (2026-09-30)

`tableWriteE2e` is a separate opt-in task for explicitly approved, empty test
tables in the existing database. It does not bypass or change the isolated
database and deployed-WAR gates of `e2eWrite`.

Required inputs are `FROG2_TABLE_WRITE_ENABLED=true`,
`FROG2_TABLE_WRITE_PREFIX=frog2_test_<unique_run>_`, and an explicit regular
external config file in `FROG2_TABLE_WRITE_DB_CONFIG`. This tag is excluded from
normal `test` and `check` runs.

Before running, provision four empty `public.<prefix><original_table>` tables
for `company_users`, `vertica_customer_detail`,
`customer_maintenance_schedule`, and `maintenance_records`. Copy only their
structure; preserve required nullability and provide test-only timestamp
defaults. The maintenance ID must use a separate prefixed sequence. Inspect
defaults and foreign keys before any test write to ensure they cannot change
shared objects. The test fails before seeding if the tables are nonempty or a
sequence default lacks the test prefix.

The test-only JDBC router changes reviewed DAO and fixture DML to these four
fully qualified names. It rejects unknown tables, schema-qualified input,
unreviewed statement syntax, direct sequence calls, unrestricted mutation,
raw statements, callable statements, and JDBC unwrapping. Column metadata is
restricted to exact names in `public` to avoid wildcard or cross-schema matches.

The probe uses production `UserDAO` authentication and production
`AuthFilter`/`CsrfFilter`/`MaintenanceServlet` with original assignment and
maintenance DAOs. It checks create, update, delete, same-name non-assignee
rejection, assigned non-creator access, lost-assignment rejection, invalid CSRF,
and the atomic customer predicate. Servlet requests and sessions are test
doubles; this verifies real JDBC and servlet logic without claiming real HTTP,
multipart parsing, or Tomcat session-container coverage. Synthetic rows are
removed in `finally`, all four tables are checked empty, and the tables and
test sequence remain for inspection. Cleanup attempts every table even after
a failure, then attempts every empty-table check; failures are accumulated and
reported instead of claiming successful cleanup. No `DROP` is performed.

`IsolatedAuthenticatedWriteFlowTest` independently exercises authenticated and
CSRF-protected upload/import against JUnit temporary directories. It verifies
successful storage/download, rollback of a partially failed upload batch,
multipart cleanup, administrator-only selected import, and preservation of
unselected files. Its maintenance scenario uses a stateful DAO double; it
does not access any database.

The older real-HTTP maintenance scenario still needs an assigned-customer seed
fixture before it can run against the current customer-assignment policy.
Neither task should be reported as a real HTTP write test until an isolated
Tomcat/database has been provisioned and that scenario has actually run.

## CI boundary

`.github/workflows/ci.yml` executes `clean check` in a GitHub-hosted disposable
runner for pull requests and pushes to `develop`. It downloads the reviewed
Tomcat/Jasper 10.1.59 toolchain and verifies the published SHA-512 before JspC.

The normal workflow deliberately does not send pull-request code to an internal
self-hosted runner or expose a writable staging database. The separate
`isolated-write-e2e.yml` workflow runs only by manual dispatch from `develop`,
behind the `frog2-isolated-e2e` GitHub environment and runner label. Its runner
creates an ephemeral Tomcat on loopback port 19081 and removes that runtime at
the end of the job. The isolated Vertica database and its config remain
pre-provisioned infrastructure. Reusing the development or production database
is not an acceptable substitute.
