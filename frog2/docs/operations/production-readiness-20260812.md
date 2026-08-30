# Archive production runtime readiness — 2026-08-12

> Historical snapshot: this document records the 2026-08-12 migration decision
> and is no longer the current runtime runbook. Both environments now use Tomcat
> 10.1.59 with separate Catalina bases. Use
> [`current-runtime-baseline.md`](current-runtime-baseline.md) and
> [`release-automation.md`](release-automation.md) for current operations.

## Decision

**Conditional NO-GO for production change today.** Development is already
running with an isolated Apache Tomcat 10.1.57 `CATALINA_HOME`, while production
still runs Tomcat 10.1.41 from a combined `/opt/tomcat` home/base. The
application has passed the existing isolated 10.1.57 compatibility checks, but
production must not be upgraded in place.

No production file, service, connector, proxy, certificate, or database was
changed while producing this document.

## Read-only facts checked

| Item | Current state |
| --- | --- |
| Development home | `/opt/tomcat-dev-home/current` → Tomcat 10.1.57 |
| Development base | `/opt/tomcat-dev` |
| Development connector | HTTP 18081, `maxPartCount=10`, `maxPartHeaderSize=512` |
| Production home/base | `/opt/tomcat` → Tomcat 10.1.41 |
| Production connector | direct HTTP 8080; multipart parser limits are not explicit |
| Tomcat request threads | default `maxThreads=200` (not explicitly configured) |
| Application DB pool | Hikari maximum 20; acquisition timeout 30 seconds |
| Services | `tomcat-dev.service` and `tomcat.service` are active |
| Production HTTPS proof | BLOCKED: no reviewed Archive HTTPS endpoint is available |

The production unit also waits for its database endpoint before startup. That
dependency must be healthy during both upgrade and rollback drills; no database
query or write is part of this readiness check.

The 200-to-20 thread/pool ratio is not itself a defect, but it is an explicit
capacity risk: DB-bound traffic can leave many request threads waiting for a
connection. The `dbAcquireDurationMs` metric measures the complete
`DataSource.getConnection()` acquisition (queueing plus validation/acquisition
overhead), not pure pool queue time. Do not change these limits until an
isolated concurrency test establishes the expected request mix and latency.

Tomcat's official security page lists 10.1.41 in affected ranges for multipart
resource-exhaustion issues fixed in 10.1.42. Archive uses multipart file upload,
so production 10.1.41 is a time-bounded rollback runtime, not an acceptable
long-term target: https://tomcat.apache.org/security-10.html

## HTTPS cookie and HSTS gate

Production is ready for an HTTPS release only after all of the following are
true:

1. A reviewed HTTPS URL with a valid certificate and hostname is available.
2. If TLS terminates at a proxy, the proxy overwrites forwarding headers,
   Tomcat cannot be reached around it, and `RemoteIpValve` trusts only the exact
   proxy network. The application must not trust client-supplied
   `X-Forwarded-Proto` directly.
3. The external login GET returns a session cookie with `HttpOnly`, `Secure`,
   and `SameSite=Strict`.
4. The external response includes HSTS with `max-age` of at least 31,536,000.
5. The existing audit is run with a trusted URL supplied outside source control:

   ```text
   FROG2_HTTPS_BASE_URL=https://<reviewed-host>/<context>/ ./gradlew httpsCookieAudit
   ```

The audit may report attributes, but it must never print or store the cookie
value. Failure of any item is a release blocker, not a reason to weaken the
test. See `docs/security/https-cookie-deployment-checklist-20260810.md`.

## Safe Tomcat 10.1.57 production plan

The current `/opt/tomcat` directory contains both product binaries and runtime
state. Preserve it unchanged as the rollback runtime. Do not copy its complete
`lib` directory into 10.1.57 because that can mix Tomcat 10.1.41 core JARs with
10.1.57.

Before an approved maintenance window:

1. Stage the already checksum/signature-verified 10.1.57 release under a new,
   versioned production home such as
   `/opt/tomcat-prod-home/releases/apache-tomcat-10.1.57`.
2. Create a separate production base such as `/opt/tomcat-prod-base` containing
   only reviewed `conf`, `webapps`, `logs`, `temp`, and `work` paths.
   Confirm that `examples`, `docs`, `manager`, `host-manager`, and the default
   `ROOT` application are not deployed unless they have a documented need and
   separate access controls.
3. Inventory `/opt/tomcat/lib`; copy only application-specific shared JARs whose
   purpose and checksum are known. Never copy Tomcat core JARs.
4. Copy production configuration without printing secrets, preserve ownership
   and mode, and set explicit connector limits equivalent to the validated
   development values (`maxPartCount=10`, `maxPartHeaderSize=512`).
5. Back up the production WAR, exploded application, unit/drop-ins, connector
   configuration, and the original PID/WAR hashes to a new timestamped backup.
6. Run Java 22 clean build, tests, JspC 10.1.57, and WAR allowlist checks before
   touching the service.
7. Prepare a systemd drop-in that changes only `CATALINA_HOME`, `CATALINA_BASE`,
   `CATALINA_PID`, `ExecStart`, and `ExecStop` to the staged paths. Review it
   before installation; installation, daemon reload, and restart require a
   separate explicit production approval.

During the approved window, stop and start only `tomcat.service`. Do not restart
`tomcat-dev.service`. Accept the new runtime only when:

- exactly one process owns port 8080;
- login GET and versioned static assets return 200;
- unauthenticated dashboard access keeps its existing redirect;
- the deployed WAR hash is the approved hash;
- logs contain no JSP compile, `ClassNotFoundException`,
  `NoSuchMethodError`, linkage, pool-initialization, or connector errors;
- the HTTPS cookie/HSTS audit passes when HTTPS is in release scope.

## Exact rollback contract

Rollback immediately if startup exceeds the agreed window, any smoke check
fails, a linkage/JSP error appears, or the HTTPS gate fails.

1. Stop only `tomcat.service`.
2. Remove or disable only the newly installed production-home drop-in.
3. Restore the saved unit/drop-in files and run `systemctl daemon-reload`.
4. Start the untouched original `/opt/tomcat` 10.1.41 runtime.
5. Verify the saved PID/WAR expectations, port 8080, login GET, static asset GET,
   unauthenticated redirect, and recent logs.
6. Keep the failed 10.1.57 home/base and logs for diagnosis; do not overwrite the
   rollback copy or alter database data.

Because 10.1.41 is in known affected ranges, a rollback must open a dated
follow-up incident/change item before the maintenance window closes. The item
must name an owner, the failed acceptance check, and the next upgrade attempt;
rollback must not silently become the permanent runtime.

The exact backup directory and approved WAR hash must be written into the
maintenance ticket before execution. This repository document is a plan, not
authorization to run it.

## Remaining approvals

- Production filesystem and systemd changes
- Production service stop/start
- HTTPS proxy/connector/certificate configuration
- Actual HTTPS cookie/HSTS audit URL

Until those four items are approved and verified, the supported decision is:
continue using Tomcat 10.1.57 in development and leave production unchanged.
