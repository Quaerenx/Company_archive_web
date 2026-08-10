# Archive pagination and runtime hardening completion

Date: 2026-08-10

## Result

- Authenticated read-only visual verification covered seven routes at 360,
  768, 1024, and 1440 pixels. All 28 captures completed without browser
  console errors or viewport overflow.
- The two mobile regressions found during that run were corrected: the customer
  search field no longer consumes the page height and maintenance inspector
  cards stack correctly on narrow screens.
- Active database schema requirements and the reviewed quarterly schedule
  override passed a metadata/read-only audit. No migration SQL or mutation was
  executed.
- The existing authentication and authorization baseline is recorded in
  `docs/frog2-authorization-policy-20260810.md`.
- File repository listings now use a stable opaque cursor and retain at most 51
  visible candidates in memory for a 50-row page.
- Meeting details now load the newest 50 comments and use a stable
  `comment_id` cursor for older pages.
- CSP no longer allows inline scripts or inline styles. The pool monitor's
  former inline style and click handler were moved to static assets.
- Session-cookie and TLS requirements are recorded in
  `docs/frog2-runtime-security-policy-20260810.md`.
- HikariCP shutdown now clears the data-source reference and deregisters only
  JDBC drivers owned by the web-application classloader. A development restart
  produced no new Tomcat forced-driver-deregistration warning.

## Verification

- Clean build: two consecutive successes.
- Standard tests: 348 tests, 0 failures, 0 errors.
- Read-only schema audit: success.
- JspC: 44 JSP/JSPF/tag inputs, 36 generated Java files, 61 generated classes,
  0 errors.
- JavaScript syntax: visual runner, pool monitor, and meeting detail scripts
  passed `node --check`.
- Shell syntax: visual regression runner passed `bash -n`.
- WAR allowlist and forbidden-entry verification: success.
- `git diff --check`: success.
- Authenticated development smoke: login and eight protected GET routes passed.
- Development login, static asset, and anonymous redirect: 200, 200, and 302
  to login respectively.
- Runtime CSP contains no `unsafe-inline`.
- HTTP development cookie: `HttpOnly` and `SameSite=Strict`; `Secure` is
  intentionally absent because the connector is not HTTPS.
- Final source and deployed development WAR SHA-256:
  `17b7967f8ad02cd918f686c0267771588e2df84b0b7cf4ed1d91005f8f4639e0`.

## Database migration audit

The following active capabilities were present:

- `V20260720_01`: `user_vm_hosts.owner_user_id`
- `V20260720_04`: `maintenance_records.license_usage_pct`
- `V20260730_05`: `troubleshooting.creator_user_id`
- `V20260731_06`: `maintenance_records.created_by_user_id`
- `V20260731_06`: `monthly_customer_response.created_by_user_id`
- `V20260804_07`: `customer_maintenance_schedule.interval_months`
- `V20260804_08`: reviewed quarterly schedule override

The audit used JDBC metadata and one bounded parameterized `SELECT`. It did not
execute DDL, INSERT, UPDATE, or DELETE.

## Deployment and production invariant

- Only `tomcat-dev.service` was stopped and started.
- The final development service is active.
- Production `tomcat.service` PID remained `1012286`.
- Production WAR SHA-256 remained
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`.
- Production login GET remained HTTP 200.

Development backups:

- Pre-work deployment:
  `/opt/frog2-dev/backups/pagination-security-20260810-105116`
- Intermediate deployment before the final comment UX correction:
  `/opt/frog2-dev/backups/pagination-security-final-20260810-110307`

## Full rollback to the pre-work development deployment

This procedure affects development Tomcat only. Use a new, empty quarantine
directory and never overwrite an existing backup.

1. Stop `tomcat-dev.service`.
2. Create a new directory under `/opt/frog2-dev/backups` for the failed current
   deployment.
3. Move these current paths into that new quarantine directory:
   - `/opt/tomcat-dev/webapps/frog2.war`
   - `/opt/tomcat-dev/webapps/frog2`
   - `/opt/tomcat-dev/work/Catalina/localhost/frog2`
4. Restore the following paths from
   `/opt/frog2-dev/backups/pagination-security-20260810-105116`:
   - `frog2.war.before` to `/opt/tomcat-dev/webapps/frog2.war`
   - `frog2.exploded.before` to `/opt/tomcat-dev/webapps/frog2`
   - `frog2.work.before` to
     `/opt/tomcat-dev/work/Catalina/localhost/frog2`
5. Restore ownership `tomcat-dev:tomcat-dev`, WAR mode `0640`, and directory
   mode `0750`.
6. Start `tomcat-dev.service` and verify the restored WAR hash, login GET,
   static asset, anonymous redirect, and filtered startup errors.

## Remaining gates

### Isolated write E2E

No separate writable database or snapshot configuration is currently
available. The existing write E2E was therefore not run. Its new isolation
gate rejects the shared config path, a matching shared JDBC URL, and a config
without `frog2.e2e.isolated=true`. An isolated Tomcat target and isolated DB
must be provisioned before CRUD and upload lifecycle verification.

The 2026-08-10 environment preflight confirmed that ports 8080 and 18081 are
the production and development Frog2 Tomcats, while the service on port 18080
is an unrelated sales application. The only active Frog2 database config found
outside backups is `/opt/frog2-dev/config/db.properties`, which points to the
intentional shared database. It was not used for write E2E. Mock-backed
authorization/CSRF checks and the temporary-directory file repository lifecycle
tests pass, but they do not replace the isolated HTTP write E2E.

### Production HTTPS cookie

Production rollout is not approved until an HTTPS response is verified to
contain `JSESSIONID` with `HttpOnly; Secure; SameSite=Strict`. The connector or
reviewed `RemoteIpValve` must make `request.isSecure()` accurate.

The `httpsCookieAudit` Gradle task now performs this check without credentials
or state-changing requests and never prints the cookie value. Set
`FROG2_HTTPS_BASE_URL` to the exact trusted HTTPS context root before running
it. Neither the production nor development Tomcat currently exposes an active
HTTPS connector, so the real endpoint check is still pending.

An additional local TLS runtime check used a temporary loopback-only Tomcat,
the development WAR, a one-day temporary certificate, and
`frog2.readOnly=true`. The anonymous login GET returned HTTP 200 and its session
cookie contained `HttpOnly`, `Secure`, and `SameSite=Strict`; HSTS was also
present. The temporary server, WAR, and certificate were removed immediately
after the check. This proves the application/Tomcat behavior under TLS, but it
does not prove the future production TLS terminator or proxy configuration.

### File repository scan cost

Cursor pagination bounds response and in-memory entry accumulation, but exact
folder/file/size totals still require one directory scan. If a directory grows
to tens of thousands of managed files, a versioned repository index should
replace repeated filesystem scans.

## Operating rollout decision

Current decision: **NO-GO for production deployment**. Development read-only
behavior, schema readiness, build, JSP, visual, and runtime checks passed, but
the isolated write E2E environment and HTTPS Secure-cookie verification remain
mandatory external gates.
