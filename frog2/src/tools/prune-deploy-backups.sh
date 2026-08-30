#!/usr/bin/env bash
set -euo pipefail
umask 077

TARGET=""
APPLY=false
KEEP_RECENT="${FROG2_BACKUP_KEEP_RECENT:-10}"

usage() {
    printf '%s\n' \
        "Usage: $0 --target development|production [--keep <count>] [--apply]" \
        "Default mode is dry-run. --apply requires FROG2_BACKUP_PRUNE_APPROVED=yes."
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --target) TARGET="${2:-}"; shift 2 ;;
        --keep) KEEP_RECENT="${2:-}"; shift 2 ;;
        --apply) APPLY=true; shift ;;
        --help|-h) usage; exit 0 ;;
        *) usage >&2; exit 2 ;;
    esac
done

if ! [[ "$KEEP_RECENT" =~ ^[1-9][0-9]*$ ]] || [ "$KEEP_RECENT" -gt 100 ]; then
    printf '%s\n' "--keep must be between 1 and 100." >&2
    exit 2
fi

case "$TARGET" in
    development) BACKUP_ROOT=/opt/tomcat-dev/backups ;;
    production) BACKUP_ROOT=/opt/tomcat-prod-base/backups ;;
    *) usage >&2; exit 2 ;;
esac

if [ "$APPLY" = true ] && [ "${FROG2_BACKUP_PRUNE_APPROVED:-}" != yes ]; then
    printf '%s\n' "Backup deletion requires FROG2_BACKUP_PRUNE_APPROVED=yes." >&2
    exit 2
fi
if [ ! -d "$BACKUP_ROOT" ] || [ "$(readlink -f -- "$BACKUP_ROOT")" != "$BACKUP_ROOT" ]; then
    printf 'Backup root is unavailable or aliased: %s\n' "$BACKUP_ROOT" >&2
    exit 2
fi

mapfile -t BACKUPS < <(
    find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d \
        -regextype posix-extended \
        -regex '.*/frog2-deploy-[0-9]{8}_[0-9]{6}' \
        -printf '%f\n' | sort -r
)

for index in "${!BACKUPS[@]}"; do
    name="${BACKUPS[$index]}"
    path="$BACKUP_ROOT/$name"
    if [ "$index" -lt "$KEEP_RECENT" ]; then
        printf 'KEEP  %s\n' "$path"
        continue
    fi
    printf '%s %s\n' "$([ "$APPLY" = true ] && printf DELETE || printf PRUNE)" "$path"
    if [ "$APPLY" = true ]; then
        canonical="$(readlink -f -- "$path")"
        case "$canonical" in
            "$BACKUP_ROOT"/frog2-deploy-????????_??????) ;;
            *) printf 'Refusing unexpected path: %s\n' "$canonical" >&2; exit 1 ;;
        esac
        find "$canonical" -xdev -depth -delete
    fi
done

printf 'Reviewed %d deployment backups; retained at least %d.\n' \
    "${#BACKUPS[@]}" "$KEEP_RECENT"
