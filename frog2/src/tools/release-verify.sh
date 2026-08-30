#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd -P)"
ALLOW_DIRTY=false

usage() {
    printf '%s\n' \
        "Usage: FROG2_SCHEMA_AUDIT_DB_CONFIG=/opt/frog2-dev/... $0 [--allow-dirty]" \
        "Runs two clean checks, compares reproducible WAR hashes, and performs the read-only schema audit."
}

for argument in "$@"; do
    case "$argument" in
        --allow-dirty) ALLOW_DIRTY=true ;;
        --help|-h) usage; exit 0 ;;
        *) usage >&2; exit 2 ;;
    esac
done

cd "$PROJECT_ROOT"

for command_name in git sha256sum mktemp; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        printf 'Required command is unavailable: %s\n' "$command_name" >&2
        exit 2
    fi
done

WORKING_TREE=clean
if [ -n "$(git status --porcelain)" ]; then
    WORKING_TREE=dirty
fi
if [ "$ALLOW_DIRTY" != true ] && [ "$WORKING_TREE" = dirty ]; then
    printf '%s\n' "Release verification requires a clean working tree." >&2
    exit 2
fi

SCHEMA_CONFIG="${FROG2_SCHEMA_AUDIT_DB_CONFIG:-}"
if [ -z "$SCHEMA_CONFIG" ] || [ ! -f "$SCHEMA_CONFIG" ]; then
    printf '%s\n' "FROG2_SCHEMA_AUDIT_DB_CONFIG must identify the reviewed read-only audit configuration." >&2
    exit 2
fi

if [ -z "${FROG2_JSPC_CATALINA_HOME:-}" ] \
        || [ -z "${FROG2_JSPC_JASPER_VERSION:-}" ]; then
    printf '%s\n' "FROG2_JSPC_CATALINA_HOME and FROG2_JSPC_JASPER_VERSION are required." >&2
    exit 2
fi

WORK_DIR="$(mktemp -d /tmp/frog2-release-verify.XXXXXX)"
cleanup() {
    case "$WORK_DIR" in
        /tmp/frog2-release-verify.*) rm -rf -- "$WORK_DIR" ;;
    esac
}
trap cleanup EXIT INT TERM

run_clean_check() {
    ./gradlew --offline --no-daemon clean check
}

run_clean_check
cp -p build/libs/frog2.war "$WORK_DIR/frog2-first.war"
FIRST_HASH="$(sha256sum "$WORK_DIR/frog2-first.war" | awk '{print $1}')"

run_clean_check
SECOND_HASH="$(sha256sum build/libs/frog2.war | awk '{print $1}')"
if [ "$FIRST_HASH" != "$SECOND_HASH" ]; then
    printf 'WAR reproducibility check failed: first=%s second=%s\n' \
        "$FIRST_HASH" "$SECOND_HASH" >&2
    exit 1
fi

FROG2_SCHEMA_AUDIT_DB_CONFIG="$SCHEMA_CONFIG" \
    ./gradlew --offline --no-daemon schemaAudit

mkdir -p build/release
MANIFEST="build/release/frog2-release-manifest.txt"
{
    printf 'commit=%s\n' "$(git rev-parse HEAD)"
    printf 'branch=%s\n' "$(git branch --show-current)"
    printf 'working_tree=%s\n' "$WORKING_TREE"
    printf 'war_sha256=%s\n' "$SECOND_HASH"
    printf 'war_size=%s\n' "$(stat -c '%s' build/libs/frog2.war)"
    printf 'verified_at_utc=%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    printf 'schema_audit=passed\n'
    printf 'clean_check_runs=2\n'
} > "$MANIFEST"
chmod 0600 "$MANIFEST"

printf 'Release verification passed: %s\n' "$SECOND_HASH"
printf 'Manifest: %s/%s\n' "$PROJECT_ROOT" "$MANIFEST"
