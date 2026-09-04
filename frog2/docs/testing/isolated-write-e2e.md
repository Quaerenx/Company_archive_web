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

No real write E2E ran during this work because no approved isolated database/snapshot was supplied.

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
