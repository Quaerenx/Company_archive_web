# Frog2 P1 remediation report

Date: 2026-07-31
Scope: development source and `tomcat-dev.service` only

## Implemented

- Stable `userId` ownership for maintenance and monthly customer-response
  activity, with legacy name fallback for reads only when the new columns are
  absent.
- Version-controlled migration
  `V20260731_06__add_activity_creator_user_ids.sql`. It was not executed.
- Read-only startup schema readiness inspection for all active migration
  capabilities. The application never runs migration SQL.
- Login credential loading now closes JDBC resources before BCrypt
  verification.
- In-memory account and client login failure limits, with HTTP 429 and
  `Retry-After`.
- Customer search evaluates the expensive wildcard predicate once on the
  normal page path. Troubleshooting summary search uses one window-count query
  on the normal path.
- Meeting and comment update/delete authorization is included in the mutation
  SQL (`... AND author_id = ?`), removing the separate ownership query.
- `/admin/pool-status` is fail-closed and requires an ID from
  `frog2.adminUserIds` or `FROG2_ADMIN_USER_IDS`.

Customer and maintenance business permissions were not changed because no
role matrix has been approved.

## Verification

- 274 tests, 0 failures.
- Java 22 clean build, test, WAR allowlist, and WAR creation: two consecutive
  successes.
- Reproducible WAR SHA-256:
  `ac97857b1fdc42d65148510219d2870b1c82d53a62330863c87925e2808d4e64`.
- JspC: 43 JSP/JSPF/tag inputs, 36 generated Java files, 55 classes, 0 errors.
- Startup JSP/class/linkage error patterns: 0.
- Development login GET 200, CSS GET 200, unauthenticated dashboard and admin
  requests redirect to `/frog2/login`.
- No authenticated write request, DDL, DML, password migration, or migration
  utility was run.

## Schema readiness

The development startup metadata check reports these migrations as not ready:

- `V20260730_05`: `troubleshooting.creator_user_id`
- `V20260731_06`: `maintenance_records.created_by_user_id`
- `V20260731_06`: `monthly_customer_response.created_by_user_id`

Reads retain a legacy fallback where safe. Ownership-sensitive writes fail
closed until the reviewed migrations are applied separately. Migration 06
only proposes backfilling names that map to exactly one user; ambiguous rows
remain for manual review.

## Development deployment

- Development PID after deployment: `3958497`
- Development WAR: `/opt/tomcat-dev/webapps/frog2.war`
- Runtime backup:
  `/opt/frog2-dev/backups/p1-remediation-deploy-20260731_111044`
- Pre-change source backup:
  `/root/frog2-p1-source-before-20260731_105144.tar.gz`

## Production invariants

- Production PID remained `1012286`.
- Production WAR SHA-256 remained
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`.
- Production `server.xml` SHA-256 remained
  `34afc0a0f9d78660c5ded03b1654b9a24204378495928baf909b6238ac3ec47a`.
- Port 8080 login GET remained 200 and the response body was byte-identical.

## Rollback

1. Record the current production PID, WAR/config hashes, and 8080 login
   response without changing the production service.
2. Stop `tomcat-dev.service` only.
3. Move the current development WAR, exploded app, and
   `/opt/tomcat-dev/work/Catalina/localhost/frog2` to a new timestamped
   failed-runtime directory; do not delete them.
4. Restore these paths:
   - `frog2.war.before` to `/opt/tomcat-dev/webapps/frog2.war`
   - `frog2.exploded.before` to `/opt/tomcat-dev/webapps/frog2`
   - `frog2-work.before` to
     `/opt/tomcat-dev/work/Catalina/localhost/frog2`
5. Restore ownership `tomcat-dev:tomcat-dev`, WAR mode `0640`, and directory
   mode `0750`.
6. Start `tomcat-dev.service` only and verify port 18081, logs, and the
   restored WAR hash
   `c6a4a116328dcbc5f82600749d951f0e0c902d3369064c81c9f5b97b25bb4dc7`.
7. Reconfirm the unchanged production PID, hashes, and 8080 response.

Do not move or modify the external file repository or database configuration
during rollback.
