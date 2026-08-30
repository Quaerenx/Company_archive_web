# Archive current runtime baseline

Verified locally on 2026-08-30. This is the current operational reference; it
contains no credentials and does not authorize a production change.

| Item | Development | Production |
| --- | --- | --- |
| Service | `tomcat-dev.service` | `tomcat.service` |
| Tomcat home | `/opt/tomcat-dev-home/current` → 10.1.59 | `/opt/tomcat-prod-home/current` → 10.1.59 |
| Catalina base | `/opt/tomcat-dev` | `/opt/tomcat-prod-base` |
| Loopback port | 18081 | 8080 |
| WAR | `/opt/tomcat-dev/webapps/frog2.war` | `/opt/tomcat-prod-base/webapps/frog2.war` |
| Deployment backup root | `/opt/tomcat-dev/backups` | `/opt/tomcat-prod-base/backups` |

Runtime database credentials remain outside the WAR and are selected through
`-Dfrog2.config`. File-repository and customer-history data also remain in
external, environment-specific storage. A WAR replacement must never overwrite
those stores.

## Operational endpoints

- `GET /frog2/health/live` is public and proves only that the web application can
  answer requests.
- `GET /frog2/health/ready` is public and returns 200 only when startup schema
  validation, the connection pool, and both external storage roots are ready.
  It exposes component up/down states but no paths, credentials, or record counts.
- `GET /frog2/admin/pool-status` and
  `GET /frog2/admin/performance-metrics` require an authenticated administrator
  ID from the fail-closed allowlist.

## Release invariants

1. Build and verify the WAR twice with `src/tools/release-verify.sh`.
2. Deploy only the SHA-256 recorded in `build/release/frog2-release-manifest.txt`.
3. Stop, back up, replace, and start one target service at a time.
4. Treat a failed readiness check as a deployment failure and restore the backup.
5. Never deploy a dirty working-tree build to production.
6. Keep credentials out of source, command history, logs, manifests, and reports.

HTTPS and domain registration are intentionally outside this baseline pending an
internal infrastructure decision.
