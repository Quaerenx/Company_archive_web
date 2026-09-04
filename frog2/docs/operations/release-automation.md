# Release verification and deployment

The scripts in `src/tools` make the existing release process repeatable. They do
not create users, change the database, install timers, or grant production
approval.

## 1. Verify a release artifact

Use a reviewed database configuration for the read-only schema audit. The build
runs two offline `clean check` passes and rejects non-reproducible WAR output.

```sh
export FROG2_JSPC_CATALINA_HOME=/opt/tomcat-dev-home/current
export FROG2_JSPC_JASPER_VERSION=10.1.59
export FROG2_SCHEMA_AUDIT_DB_CONFIG=/opt/frog2-dev/config/db.properties
export JAVA_HOME=/opt/jdk-25
export PATH="$JAVA_HOME/bin:$PATH"
src/tools/release-verify.sh
```

`--allow-dirty` is available only for an explicitly reviewed development
deployment. The generated manifest records that state, and production deployment
always rejects it.

After `V20260904_12` is installed and all active migrations are baselined or
recorded, set `FROG2_MIGRATION_LEDGER_REQUIRED=yes`. Release verification then
fails if the database ledger is missing, pending, or differs from the pinned
checksums.

## 2. Deploy to development

Read the approved hash from the manifest and pass it explicitly:

```sh
WAR_SHA256=$(sed -n 's/^war_sha256=//p' build/release/frog2-release-manifest.txt)
FROG2_DEVELOPMENT_DIRTY_DEPLOY_APPROVED=yes \
  src/tools/deploy-war.sh --target development --sha256 "$WAR_SHA256"
```

The script creates a timestamped backup, stops only `tomcat-dev.service`, replaces
the WAR, starts the service, and checks login, readiness, and a static asset. A
failure after backup triggers rollback.

## 3. Run authenticated read-only smoke coverage

Use a dedicated existing account supplied only through the process environment.
Do not store credentials in a file or create an account as part of this script.

```sh
export FROG2_E2E_USER_ID='<existing-test-user>'
export FROG2_E2E_PASSWORD='<secret-from-approved-store>'
src/tools/authenticated-smoke.sh development
unset FROG2_E2E_PASSWORD
```

The verifier accepts only the approved loopback application URLs and checks that
the deployed WAR exactly matches the current build before sending credentials.

## Production gate

Production requires all of the following: a clean manifest, the exact approved
hash, a separately reviewed maintenance window, and
`FROG2_PRODUCTION_DEPLOY_APPROVED=yes`. HTTPS/domain work remains separate. Never
copy the development database configuration or E2E credentials into production.
