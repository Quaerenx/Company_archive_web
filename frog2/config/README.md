# External runtime configuration

Frog2 does not package database credentials in the WAR. Start Tomcat with the JVM system property below, pointing to a permission-restricted properties file outside the webroot:

```text
-Dfrog2.config=/absolute/path/to/db.properties
```

Use `db.properties.sample` as the key-only template. Never commit a populated `db.properties` file.

Safety and authorization settings are supplied as JVM system properties:

```text
-Dfrog2.env=dev
-Dfrog2.readOnly=true
-Dfrog2.adminUserIds=user-id-1,user-id-2
```

Database writes are allowed only when `frog2.env=prod` or
`frog2.env=staging` and `frog2.readOnly=false` are both explicit. The
`staging` option is reserved for isolated, non-production instances during a
bounded test window; development and test environments remain read-only.
`/admin/pool-status` is denied to
everyone unless the authenticated stable user ID is listed in
`frog2.adminUserIds` (or `FROG2_ADMIN_USER_IDS`). Do not put passwords or other
secrets in that list.
The same administrator policy protects `/admin/performance-metrics`, which
reports aggregate troubleshooting search latency without query text or customer
data. Aggregates cover the current application process and reset on restart. Its
slow-search threshold defaults to 500 ms and may be changed with:

```text
-Dfrog2.performance.slowSearchMs=500
```

`/health/live` and `/health/ready` are public operational probes. Readiness
reports only component up/down state and must not be used as a source of secrets
or detailed diagnostics.

## Reproducible verification

The Java 22 JSP compiler uses an explicit, read-only Tomcat toolchain. Point
the build at an extracted Apache Tomcat 10.1.59 directory; do not reuse
`CATALINA_HOME`, which may identify a running production instance.

```sh
export FROG2_JSPC_CATALINA_HOME=/absolute/path/to/apache-tomcat-10.1.59
export FROG2_JSPC_JASPER_VERSION=10.1.59
./gradlew --no-daemon clean check
```
