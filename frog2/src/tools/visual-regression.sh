#!/usr/bin/env bash
set -eu
umask 077

MODE="${1:-compare}"
BASE_URL="${FROG2_VISUAL_BASE_URL:-http://127.0.0.1:18081/frog2}"
FIREFOX_PROFILE="${FROG2_VISUAL_FIREFOX_PROFILE:-}"
E2E_USER_ID="${FROG2_E2E_USER_ID:-}"
E2E_PASSWORD="${FROG2_E2E_PASSWORD:-}"
ROUTE_MANIFEST="${FROG2_VISUAL_ROUTE_MANIFEST:-src/tools/visual-regression-routes.tsv}"
THRESHOLD_PERCENT="${FROG2_VISUAL_THRESHOLD_PERCENT:-1.0}"
ASSET_VERSION="$(sed -n '/<param-name>frog2AssetVersion<\/param-name>/{n;s:.*<param-value>\([^<]*\)</param-value>.*:\1:p;}' src/main/webapp/WEB-INF/web.xml)"
BASELINE_ROOT="${FROG2_VISUAL_BASELINE_ROOT:-/opt/frog2-dev/visual-baselines/${ASSET_VERSION}}"

case "$MODE" in
    baseline|compare) ;;
    *)
        echo "Usage: $0 baseline|compare" >&2
        exit 2
        ;;
esac

case "$BASE_URL" in
    http://127.0.0.1:18081/*|http://localhost:18081/*|http://\[::1\]:18081/*|http://192.168.40.70:18081/*) ;;
    *)
        echo "Only the approved development server on port 18081 is allowed: $BASE_URL" >&2
        exit 2
        ;;
esac

if { [ -n "$E2E_USER_ID" ] && [ -z "$E2E_PASSWORD" ]; } \
        || { [ -z "$E2E_USER_ID" ] && [ -n "$E2E_PASSWORD" ]; }; then
    echo "Set both FROG2_E2E_USER_ID and FROG2_E2E_PASSWORD." >&2
    exit 2
fi
if [ -z "$E2E_USER_ID" ]; then
    if [ -z "$FIREFOX_PROFILE" ] || [ ! -d "$FIREFOX_PROFILE" ]; then
        echo "Set development E2E credentials or an authenticated Firefox profile." >&2
        exit 2
    fi
fi

if [ ! -f "$ROUTE_MANIFEST" ]; then
    echo "Route manifest not found: $ROUTE_MANIFEST" >&2
    exit 2
fi

for command_name in geckodriver node compare identify sha256sum diff; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Required command is unavailable: $command_name" >&2
        exit 2
    fi
done

WORK_ROOT="$(mktemp -d /tmp/frog2-visual-regression.XXXXXX)"
WORK_PROFILE="$(mktemp -d /root/snap/firefox/common/frog2-visual-profile.XXXXXX)"
PUBLIC_PROFILE="$(mktemp -d /root/snap/firefox/common/frog2-visual-public.XXXXXX)"
CURRENT_ROOT="$WORK_ROOT/current"
DIFF_ROOT="$WORK_ROOT/diff"

cleanup() {
    case "$WORK_ROOT" in
        /tmp/frog2-visual-regression.*) rm -rf -- "$WORK_ROOT" ;;
    esac
    case "$WORK_PROFILE" in
        /root/snap/firefox/common/frog2-visual-profile.*) rm -rf -- "$WORK_PROFILE" ;;
    esac
    case "$PUBLIC_PROFILE" in
        /root/snap/firefox/common/frog2-visual-public.*) rm -rf -- "$PUBLIC_PROFILE" ;;
    esac
}
trap cleanup EXIT INT TERM

mkdir -p "$CURRENT_ROOT" "$DIFF_ROOT"
if [ -n "$FIREFOX_PROFILE" ]; then
    for cookie_file in cookies.sqlite cookies.sqlite-wal cookies.sqlite-shm; do
        if [ -f "$FIREFOX_PROFILE/$cookie_file" ]; then
            cp -p "$FIREFOX_PROFILE/$cookie_file" "$WORK_PROFILE/$cookie_file"
        fi
    done
    if [ -z "$E2E_USER_ID" ] && [ ! -f "$WORK_PROFILE/cookies.sqlite" ]; then
        echo "Firefox cookie database is unavailable in the selected profile." >&2
        exit 2
    fi
fi

# Deterministic captures: no animation and no disk cache mutation in the source profile.
printf '%s\n' \
    'user_pref("ui.prefersReducedMotion", 1);' \
    'user_pref("browser.cache.disk.enable", false);' \
    'user_pref("browser.sessionstore.resume_from_crash", false);' \
    >> "$WORK_PROFILE/user.js"
printf '%s\n' \
    'user_pref("ui.prefersReducedMotion", 1);' \
    'user_pref("browser.cache.disk.enable", false);' \
    'user_pref("browser.sessionstore.resume_from_crash", false);' \
    >> "$PUBLIC_PROFILE/user.js"

capture_all() {
    destination="$1"
    mkdir -p "$destination"
    rm -f "$destination/capture-metrics.json"
    node src/tools/capture-visual-regression.mjs \
        "$BASE_URL" \
        "$PUBLIC_PROFILE" \
        "$ROUTE_MANIFEST" \
        "$destination" \
        public
    node src/tools/capture-visual-regression.mjs \
        "$BASE_URL" \
        "$WORK_PROFILE" \
        "$ROUTE_MANIFEST" \
        "$destination" \
        authenticated
    identify "$destination"/*.png >/dev/null
}

expected_capture_names() {
    while IFS=$'\t' read -r route_name route_path route_access; do
        if [ -z "$route_name" ]; then
            continue
        fi
        for viewport in \
                360x900 \
                390x900 \
                768x1024 \
                1024x900 \
                1440x1000; do
            printf '%s-%s.png\n' "$route_name" "$viewport"
        done
    done < "$ROUTE_MANIFEST"
}

verify_capture_set() {
    capture_root="$1"
    capture_label="$2"
    expected_list="$WORK_ROOT/${capture_label}-expected.txt"
    actual_list="$WORK_ROOT/${capture_label}-actual.txt"

    expected_capture_names | sort > "$expected_list"
    find "$capture_root" -maxdepth 1 -type f -name '*.png' \
        -printf '%f\n' | sort > "$actual_list"
    if ! diff -u "$expected_list" "$actual_list"; then
        echo "Unexpected ${capture_label} capture set: $capture_root" >&2
        exit 2
    fi
}

if [ "$MODE" = "baseline" ]; then
    mkdir -p "$BASELINE_ROOT"
    capture_all "$BASELINE_ROOT"
    verify_capture_set "$BASELINE_ROOT" baseline
    (
        cd "$BASELINE_ROOT"
        sha256sum ./*.png > manifest.sha256
    )
    echo "Visual baseline created: $BASELINE_ROOT"
    exit 0
fi

if [ ! -f "$BASELINE_ROOT/manifest.sha256" ]; then
    echo "Baseline is missing. Run baseline mode first: $BASELINE_ROOT" >&2
    exit 2
fi

verify_capture_set "$BASELINE_ROOT" baseline
(
    cd "$BASELINE_ROOT"
    sha256sum -c manifest.sha256 >/dev/null
)

capture_all "$CURRENT_ROOT"
verify_capture_set "$CURRENT_ROOT" current
failed=0
for baseline in "$BASELINE_ROOT"/*.png; do
    name="$(basename "$baseline")"
    current="$CURRENT_ROOT/$name"
    diff="$DIFF_ROOT/$name"
    if [ ! -f "$current" ]; then
        echo "Missing current capture: $name" >&2
        failed=1
        continue
    fi

    baseline_size="$(identify -format '%wx%h' "$baseline")"
    current_size="$(identify -format '%wx%h' "$current")"
    if [ "$baseline_size" != "$current_size" ]; then
        echo "FAIL $name: size $baseline_size -> $current_size" >&2
        failed=1
        continue
    fi

    changed_pixels="$(compare -metric AE -fuzz 2% "$baseline" "$current" null: 2>&1 || true)"
    changed_pixels="${changed_pixels%% *}"
    total_pixels="$(identify -format '%[fx:w*h]' "$baseline")"
    changed_percent="$(awk -v changed="$changed_pixels" -v total="$total_pixels" 'BEGIN { printf "%.4f", (changed / total) * 100 }')"

    if awk -v changed="$changed_percent" -v limit="$THRESHOLD_PERCENT" 'BEGIN { exit !(changed > limit) }'; then
        compare -fuzz 2% "$baseline" "$current" "$diff" 2>/dev/null || true
        echo "FAIL $name: ${changed_percent}% changed (limit ${THRESHOLD_PERCENT}%)" >&2
        failed=1
    else
        echo "PASS $name: ${changed_percent}% changed"
    fi
done

if [ "$failed" -ne 0 ]; then
    echo "Visual differences: $DIFF_ROOT" >&2
    exit 1
fi

echo "All visual baselines passed."
