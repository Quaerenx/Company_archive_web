#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd -P)"
TARGET="${1:-development}"

if [ -z "${FROG2_E2E_USER_ID:-}" ] || [ -z "${FROG2_E2E_PASSWORD:-}" ]; then
    printf '%s\n' "Set FROG2_E2E_USER_ID and FROG2_E2E_PASSWORD outside source control." >&2
    exit 2
fi
if [ -z "${FROG2_JSPC_CATALINA_HOME:-}" ] \
        || [ -z "${FROG2_JSPC_JASPER_VERSION:-}" ]; then
    printf '%s\n' "FROG2_JSPC_CATALINA_HOME and FROG2_JSPC_JASPER_VERSION are required." >&2
    exit 2
fi

case "$TARGET" in
    development)
        BASE_URL=http://127.0.0.1:18081/frog2/
        DEPLOYED_WAR=/opt/tomcat-dev/webapps/frog2.war
        ;;
    production)
        if [ "${FROG2_PRODUCTION_SMOKE_APPROVED:-}" != yes ]; then
            printf '%s\n' "Production smoke requires FROG2_PRODUCTION_SMOKE_APPROVED=yes." >&2
            exit 2
        fi
        BASE_URL=http://127.0.0.1:8080/frog2/
        DEPLOYED_WAR=/opt/tomcat-prod-base/webapps/frog2.war
        ;;
    *)
        printf 'Usage: %s development|production\n' "$0" >&2
        exit 2
        ;;
esac

cd "$PROJECT_ROOT"
./gradlew --offline --no-daemon \
    "-Dfrog2.e2e.baseUrl=$BASE_URL" \
    "-Dfrog2.e2e.deployedWar=$DEPLOYED_WAR" \
    e2eAuthenticatedSmoke
