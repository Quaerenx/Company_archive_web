# Frog2 performance observability and JDBC timeout

Date: 2026-08-03

## Runtime behavior

- Dynamic HTTP requests emit one completion log with method, normalized path, status, total duration, SQL count, total SQL duration, and maximum single-SQL duration.
- Public static resources are excluded from application request timing to avoid duplicate high-volume access logging.
- Requests at or above the slow-request threshold are logged at `WARN`; other dynamic requests are logged at `INFO`.
- SQL executions at or above the slow-SQL threshold are logged at `WARN`.
- SQL summaries remove comments, string literals, and numeric literals and are limited to 240 characters.
- Query strings, form fields, user IDs, passwords, customer names, and bound JDBC parameter values are never written by the performance logger.
- Troubleshooting searches identify summary-only and explicit full-content operations separately. Full-content search covers complete `LONG VARCHAR` fields; it remains a bounded-time contains scan unless a separately approved Vertica text index is introduced.
- File-repository requests report snapshot cache hits/misses and scan count/duration.
- Customer-history requests report snapshot cache hits/misses, scanned record-file count, and scan duration without logging record content or customer names.

## Settings

| Setting | Location | Default | Purpose |
| --- | --- | ---: | --- |
| `frog2.performance.slowRequestMs` | JVM system property | 500 ms | Slow HTTP warning threshold |
| `frog2.performance.slowSqlMs` | JVM system property | 250 ms | Slow SQL warning threshold |
| `jdbc.queryTimeoutSeconds` | External `db.properties` | 30 s | `Statement.setQueryTimeout` value |

The query timeout must be a positive integer. Invalid values fail application startup with a configuration error instead of silently disabling the safeguard.

## Timeout decision

The bundled driver implements the standard JDBC `Statement.setQueryTimeout` API. A 20-second timeout was also set successfully during the read-only development baseline against Vertica 24.03.0003. Frog2 therefore applies a configurable 30-second timeout to every `Statement`, `PreparedStatement`, and `CallableStatement` created through `DBConnection`.

Vertica also provides connection-level `LoginTimeout`, `LoginNodeTimeout`, `LoginNetworkTimeout`, and `NetworkTimeout` properties. They solve connection establishment or network response stalls rather than query execution duration. They remain unchanged until network topology and failover requirements are reviewed with the DBA.

Official references:

- https://docs.vertica.com/24.3.x/en/connecting-to/client-libraries/accessing/java/creating-and-configuring-connection/jdbc-connection-properties/
- https://docs.vertica.com/24.3.x/sdkdocs/JDBC/com/vertica/jdbc/VerticaStatement.html

## E2E smoke execution

The default local target is `http://127.0.0.1:18081/frog2/`.

```bash
./gradlew e2eSmoke
```

The default smoke task performs only public GET and authentication-boundary checks and never submits the login form. Authenticated read-only navigation is deliberately separated:

```bash
./gradlew e2eAuthenticatedSmoke
```

The authenticated task requires both `FROG2_E2E_USER_ID` and `FROG2_E2E_PASSWORD` in the execution environment. Do not place those values in source control, shell scripts, Gradle files, or command-line arguments.

Remote targets are rejected unless the run explicitly sets `-Dfrog2.e2e.allowRemote=true` together with `-Dfrog2.e2e.baseUrl=...`.

## Development deployment verification

- Service: `tomcat-dev.service` only; active PID `803852`.
- Safety flags: `frog2.env=dev`, `frog2.readOnly=true`.
- Deployed WAR SHA-256: `d1ca434eb2091f69bf19430897afb0626ae2990ae2f54253f4aa50532a57cfdf`.
- Rollback backup: `/opt/frog2-dev/backups/stabilization-performance-20260803_140842`.
- Unit and contract tests: 298 passed, 0 failed, 0 skipped.
- E2E smoke: 2 passed, 0 failed, 1 authenticated check skipped because no dedicated credentials were configured.
- Post-deployment Tomcat fatal errors: 0; application `ERROR` logs: 0.
- The existing schema-readiness warning remains expected until the separately approved migrations are applied.
