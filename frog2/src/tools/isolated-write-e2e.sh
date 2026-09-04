#!/usr/bin/env bash
set -euo pipefail
umask 077

ROOT=/opt/frog2-dev/e2e
RUNS_ROOT="$ROOT/runs"
CATALINA_HOME="${FROG2_E2E_CATALINA_HOME:-}"
BASE_URL="${FROG2_E2E_BASE_URL:-}"
DB_CONFIG="${FROG2_E2E_DB_CONFIG:-}"
SHARED_DB_CONFIG="${FROG2_E2E_SHARED_DB_CONFIG:-}"
PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd -P)"

require_regular_file() {
    local path="$1"
    local root="$2"
    if [ ! -f "$path" ] || [ "$(readlink -f -- "$path")" != "$path" ]; then
        printf 'Required file must be canonical: %s\n' "$path" >&2
        exit 2
    fi
    case "$path" in
        "$root"/*) ;;
        *) printf 'Required file is outside the approved root: %s\n' "$path" >&2; exit 2 ;;
    esac
}

if [ "${FROG2_E2E_WRITE_ENABLED:-}" != true ]; then
    printf '%s\n' 'FROG2_E2E_WRITE_ENABLED=true is required.' >&2
    exit 2
fi
if [ "$BASE_URL" != 'http://127.0.0.1:19081/frog2/' ]; then
    printf '%s\n' 'The isolated E2E URL must use the dedicated loopback port 19081.' >&2
    exit 2
fi
if [ ! -d "$CATALINA_HOME" ] \
        || [ "$(readlink -f -- "$CATALINA_HOME")" != "$CATALINA_HOME" ] \
        || [ ! -x "$CATALINA_HOME/bin/catalina.sh" ]; then
    printf '%s\n' 'FROG2_E2E_CATALINA_HOME must identify the reviewed Tomcat toolchain.' >&2
    exit 2
fi
require_regular_file "$DB_CONFIG" "$ROOT"
require_regular_file "$SHARED_DB_CONFIG" /opt/frog2-dev
if [ "$DB_CONFIG" = "$SHARED_DB_CONFIG" ]; then
    printf '%s\n' 'The isolated and shared database configs must differ.' >&2
    exit 2
fi

install -d -m 0700 "$RUNS_ROOT"
CATALINA_BASE="$(mktemp -d "$RUNS_ROOT/frog2-ci.XXXXXX")"
export CATALINA_BASE
export CATALINA_PID="$CATALINA_BASE/tomcat.pid"

cleanup() {
    set +e
    if [ -f "$CATALINA_PID" ]; then
        "$CATALINA_HOME/bin/catalina.sh" stop 10 -force >/dev/null 2>&1
    fi
    case "$CATALINA_BASE" in
        "$RUNS_ROOT"/frog2-ci.*) rm -rf -- "$CATALINA_BASE" ;;
    esac
}
trap cleanup EXIT INT TERM

install -d -m 0700 \
    "$CATALINA_BASE/conf" "$CATALINA_BASE/data/files" \
    "$CATALINA_BASE/data/customer-history" "$CATALINA_BASE/logs" \
    "$CATALINA_BASE/temp" "$CATALINA_BASE/webapps" "$CATALINA_BASE/work"
cp -a "$CATALINA_HOME/conf/." "$CATALINA_BASE/conf/"
sed -i \
    -e 's/port="8005"/port="19005"/' \
    -e 's/port="8080"/port="19081"/' \
    "$CATALINA_BASE/conf/server.xml"

cd "$PROJECT_ROOT"
./gradlew --no-daemon clean war
install -m 0600 build/libs/frog2.war "$CATALINA_BASE/webapps/frog2.war"
export FROG2_E2E_DEPLOYED_WAR="$CATALINA_BASE/webapps/frog2.war"
export CATALINA_OPTS="-Dfrog2.config=$DB_CONFIG -Dfrog2.env=staging -Dfrog2.readOnly=false -Dfrog2.fileRepoRoot=$CATALINA_BASE/data/files -Dfrog2.customerHistoryRoot=$CATALINA_BASE/data/customer-history"

"$CATALINA_HOME/bin/catalina.sh" start
healthy=false
for attempt in $(seq 1 60); do
    if curl -fsS -o /dev/null "${BASE_URL}health/ready"; then
        healthy=true
        break
    fi
    sleep 1
done
if [ "$healthy" != true ]; then
    printf '%s\n' 'The isolated application did not become ready.' >&2
    exit 1
fi

./gradlew --no-daemon e2eWrite
