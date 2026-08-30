#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd -P)"
TARGET=""
EXPECTED_HASH=""
WAR="$PROJECT_ROOT/build/libs/frog2.war"

usage() {
    printf '%s\n' \
        "Usage: $0 --target development|production --sha256 <approved-hash> [--war <absolute-path>]" \
        "Development dirty builds require FROG2_DEVELOPMENT_DIRTY_DEPLOY_APPROVED=yes." \
        "Production requires a clean manifest and FROG2_PRODUCTION_DEPLOY_APPROVED=yes."
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --target) TARGET="${2:-}"; shift 2 ;;
        --sha256) EXPECTED_HASH="${2:-}"; shift 2 ;;
        --war) WAR="${2:-}"; shift 2 ;;
        --help|-h) usage; exit 0 ;;
        *) usage >&2; exit 2 ;;
    esac
done

if [ "$(id -u)" -ne 0 ]; then
    printf '%s\n' "Deployment must run as root." >&2
    exit 2
fi
if [ -z "$EXPECTED_HASH" ] || ! [[ "$EXPECTED_HASH" =~ ^[0-9a-f]{64}$ ]]; then
    printf '%s\n' "A lowercase SHA-256 is required." >&2
    exit 2
fi
if [ ! -f "$WAR" ] || [ "$(readlink -f -- "$WAR")" != "$WAR" ]; then
    printf '%s\n' "WAR must be an existing canonical absolute path." >&2
    exit 2
fi

case "$TARGET" in
    development)
        SERVICE=tomcat-dev.service
        BASE=/opt/tomcat-dev
        OWNER=tomcat-dev
        GROUP=tomcat-dev
        PORT=18081
        ;;
    production)
        if [ "${FROG2_PRODUCTION_DEPLOY_APPROVED:-}" != yes ]; then
            printf '%s\n' "Production deployment requires FROG2_PRODUCTION_DEPLOY_APPROVED=yes." >&2
            exit 2
        fi
        SERVICE=tomcat.service
        BASE=/opt/tomcat-prod-base
        OWNER=tomcat
        GROUP=tomcat
        PORT=8080
        ;;
    *) usage >&2; exit 2 ;;
esac

LIVE_WAR="$BASE/webapps/frog2.war"
LIVE_EXPLODED="$BASE/webapps/frog2"
LIVE_WORK="$BASE/work/Catalina/localhost/frog2"
MANIFEST="$PROJECT_ROOT/build/release/frog2-release-manifest.txt"
ACTUAL_HASH="$(sha256sum "$WAR" | awk '{print $1}')"

if [ "$ACTUAL_HASH" != "$EXPECTED_HASH" ]; then
    printf 'WAR hash mismatch: expected=%s actual=%s\n' \
        "$EXPECTED_HASH" "$ACTUAL_HASH" >&2
    exit 1
fi
if [ ! -f "$MANIFEST" ] \
        || ! grep -Fxq "war_sha256=$EXPECTED_HASH" "$MANIFEST" \
        || ! grep -Fxq "commit=$(git -C "$PROJECT_ROOT" rev-parse HEAD)" "$MANIFEST"; then
    printf '%s\n' "The current commit and WAR are not covered by the release verification manifest." >&2
    exit 1
fi
MANIFEST_WORKING_TREE="$(sed -n 's/^working_tree=//p' "$MANIFEST")"
case "$MANIFEST_WORKING_TREE" in
    clean) ;;
    dirty)
        if [ "$TARGET" = production ]; then
            printf '%s\n' "Production deployment refuses a dirty working-tree build." >&2
            exit 1
        fi
        if [ "${FROG2_DEVELOPMENT_DIRTY_DEPLOY_APPROVED:-}" != yes ]; then
            printf '%s\n' "Dirty development deployment requires FROG2_DEVELOPMENT_DIRTY_DEPLOY_APPROVED=yes." >&2
            exit 2
        fi
        ;;
    *)
        printf '%s\n' "Release manifest has an invalid working_tree value." >&2
        exit 1
        ;;
esac

TIMESTAMP="$(date '+%Y%m%d_%H%M%S')"
BACKUP_DIR="$BASE/backups/frog2-deploy-$TIMESTAMP"
if [ -e "$BACKUP_DIR" ]; then
    printf 'Backup destination already exists: %s\n' "$BACKUP_DIR" >&2
    exit 1
fi
if [ ! -f "$LIVE_WAR" ]; then
    printf 'Current deployment WAR is unavailable: %s\n' "$LIVE_WAR" >&2
    exit 1
fi
BACKUP_READY=false

rollback() {
    trap - ERR
    set +e
    systemctl stop "$SERVICE"
    install -d -m 0700 -o root -g root "$BACKUP_DIR/failed"
    [ ! -e "$LIVE_WAR" ] || mv "$LIVE_WAR" "$BACKUP_DIR/failed/frog2.war.failed"
    [ ! -e "$LIVE_EXPLODED" ] || mv "$LIVE_EXPLODED" "$BACKUP_DIR/failed/frog2.exploded.failed"
    [ ! -e "$LIVE_WORK" ] || mv "$LIVE_WORK" "$BACKUP_DIR/failed/frog2.work.failed"
    if [ -f "$BACKUP_DIR/frog2.war.before" ]; then
        install -m 0640 -o "$OWNER" -g "$GROUP" \
            "$BACKUP_DIR/frog2.war.before" "$LIVE_WAR"
    fi
    [ ! -e "$BACKUP_DIR/frog2.exploded.before" ] \
        || mv "$BACKUP_DIR/frog2.exploded.before" "$LIVE_EXPLODED"
    [ ! -e "$BACKUP_DIR/frog2.work.before" ] \
        || mv "$BACKUP_DIR/frog2.work.before" "$LIVE_WORK"
    systemctl start "$SERVICE"
    printf 'Deployment failed and rollback was attempted: %s\n' "$BACKUP_DIR" >&2
}

on_error() {
    if [ "$BACKUP_READY" = true ]; then
        rollback
    fi
    exit 1
}
trap on_error ERR

install -d -m 0700 -o root -g root "$BACKUP_DIR"
systemctl stop "$SERVICE"
if systemctl is-active --quiet "$SERVICE"; then
    printf 'Service did not stop: %s\n' "$SERVICE" >&2
    exit 1
fi

cp -a "$LIVE_WAR" "$BACKUP_DIR/frog2.war.before"
[ ! -e "$LIVE_EXPLODED" ] \
    || mv "$LIVE_EXPLODED" "$BACKUP_DIR/frog2.exploded.before"
[ ! -e "$LIVE_WORK" ] \
    || mv "$LIVE_WORK" "$BACKUP_DIR/frog2.work.before"
BACKUP_READY=true

install -m 0640 -o "$OWNER" -g "$GROUP" "$WAR" "$LIVE_WAR"
systemctl start "$SERVICE"

healthy=false
for attempt in $(seq 1 60); do
    if systemctl is-active --quiet "$SERVICE" \
            && curl -fsS -o /dev/null "http://127.0.0.1:$PORT/frog2/login" \
            && curl -fsS -o /dev/null "http://127.0.0.1:$PORT/frog2/health/ready" \
            && curl -fsS -o /dev/null "http://127.0.0.1:$PORT/frog2/resources/css/tokens.css"; then
        healthy=true
        break
    fi
    sleep 1
done
if [ "$healthy" != true ]; then
    printf 'Health checks failed for %s.\n' "$TARGET" >&2
    exit 1
fi

DEPLOYED_HASH="$(sha256sum "$LIVE_WAR" | awk '{print $1}')"
if [ "$DEPLOYED_HASH" != "$EXPECTED_HASH" ]; then
    printf 'Deployed hash mismatch: %s\n' "$DEPLOYED_HASH" >&2
    exit 1
fi

{
    printf 'target=%s\n' "$TARGET"
    printf 'service=%s\n' "$SERVICE"
    printf 'commit=%s\n' "$(git -C "$PROJECT_ROOT" rev-parse HEAD)"
    printf 'war_sha256=%s\n' "$EXPECTED_HASH"
    printf 'deployed_at=%s\n' "$(date '+%Y-%m-%dT%H:%M:%S%z')"
} > "$BACKUP_DIR/deployment-result.txt"
chmod 0600 "$BACKUP_DIR/deployment-result.txt"

trap - ERR
printf 'Deployment passed: target=%s hash=%s backup=%s\n' \
    "$TARGET" "$EXPECTED_HASH" "$BACKUP_DIR"
