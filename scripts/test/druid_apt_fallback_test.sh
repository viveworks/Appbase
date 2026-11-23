#!/usr/bin/env bash
# Behavioral contract for Druid's snapshot-first, signed APT fallback.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
INSTALLER="$ROOT/appbase-java/appbase-client/appbase-client-druid/infra/docker/druid/install-apt-packages.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
PASSED=0
FAILED=0
EXPECTED_PACKAGES=(bash curl wget perl gnupg apt-transport-https vim coreutils lsb-release openjdk-11-jdk)
ok() { PASSED=$((PASSED + 1)); echo "  ok   $1"; }
bad() { FAILED=$((FAILED + 1)); echo "  FAIL $1"; }

if [[ ! -x "$INSTALLER" ]]; then
  bad "Druid APT installer with fallback contract exists"
  echo "druid-apt-fallback-test: $PASSED passed, $FAILED failed"
  exit 1
fi

mkdir -p "$TMP/bin"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'printf "%s|%s\\n" "$(tr "\\n" ";" < "$APT_SOURCES_FILE")" "$*" >> "$APT_CALL_LOG"' \
  'if [[ "$*" == *update* ]]; then' \
  '  if compgen -G "$APT_LISTS_DIR/*" >/dev/null; then echo "stale APT list survived reset" >&2; exit 88; fi' \
  '  : > "$APT_LISTS_DIR/update-marker"' \
  '  case "$APT_MODE" in' \
  '    primary-down) if grep -q snapshot.debian.org "$APT_SOURCES_FILE"; then echo "Failed to fetch primary snapshot index: 504 Gateway Time-out" >&2; exit 100; fi ;;' \
  '    both-snapshots-down|archive-java-order) if grep -Eq "snapshot(-cloudflare)?[.]debian[.]org" "$APT_SOURCES_FILE"; then echo "Failed to fetch snapshot index: 504 Gateway Time-out" >&2; exit 100; fi ;;' \
  '    partial) if grep -q snapshot.debian.org "$APT_SOURCES_FILE"; then echo "Failed to fetch snapshot index: Connection timed out" >&2; exit 0; fi ;;' \
  '    http404) if grep -q snapshot.debian.org "$APT_SOURCES_FILE"; then echo "Failed to fetch snapshot index: 404 Not Found" >&2; exit 100; fi ;;' \
  '    signature) echo "The following signatures were invalid: NO_PUBKEY fixture" >&2; exit 100 ;;' \
  '    deadline-global) if [[ "$*" == *update* ]]; then echo "Failed to fetch index: Connection timed out" >&2; exit 100; fi ;;' \
  '  esac' \
  'fi' \
  'if [[ "$APT_MODE" == deadline-source && "$*" == *install* && "$*" == *openjdk-11-jdk* ]] && grep -q snapshot.debian.org "$APT_SOURCES_FILE"; then' \
  '  echo "Failed to fetch primary package: Connection timed out" >&2; exit 100' \
  'fi' \
  'if [[ "$*" == *install* && ( "$APT_MODE" == archive-java-order || "$APT_MODE" == snapshot-java-order ) ]]; then' \
  '  if [[ "$*" == *openjdk-11-jre-headless* ]]; then : > "$APT_LISTS_DIR/java-security-ready"; fi' \
  '  if [[ "$*" == *openjdk-11-jdk* && ! -f "$APT_LISTS_DIR/java-security-ready" ]]; then' \
  '    echo "Setting up ca-certificates-java ... java.lang.InternalError: Error loading java.security file" >&2; exit 100' \
  '  fi' \
  'fi' \
  'if [[ "$*" == *install* && "$APT_MODE" == package-transport && "$*" != *openjdk-11-jdk* ]]; then' \
  '  count=0; [[ ! -f "$APT_LISTS_DIR/install-attempt" ]] || count="$(cat "$APT_LISTS_DIR/install-attempt")"' \
  '  count=$((count + 1)); printf "%s\\n" "$count" > "$APT_LISTS_DIR/install-attempt"' \
  '  if [[ "$count" -lt 3 ]]; then echo "Failed to fetch package: 504 Gateway Time-out" >&2; exit 100; fi' \
  'fi' \
  'if [[ "$*" == *install* && "$*" == *openjdk-11-jdk* && "$APT_MODE" == package-transport-rotate && $(grep -c snapshot.debian.org "$APT_SOURCES_FILE") -gt 0 ]]; then' \
  '  echo "E: Failed to fetch pool/main/j/java-common/java-common_0.71_all.deb 503 No healthy backends" >&2; exit 100' \
  'fi' \
  'if [[ "$*" == *install* && "$*" == *openjdk-11-jdk* && "$APT_MODE" == package-transport-archive ]] && grep -Eq "snapshot(-cloudflare)?[.]debian[.]org" "$APT_SOURCES_FILE"; then' \
  '  echo "E: Failed to fetch pool/main/j/java-common/java-common_0.71_all.deb 503 No healthy backends" >&2; exit 100' \
  'fi' \
  'if [[ "$*" == *install* && "$APT_MODE" == install-error ]]; then echo "E: Unable to locate package fixture" >&2; exit 100; fi' \
  'exit 0' > "$TMP/bin/apt-get"
chmod +x "$TMP/bin/apt-get"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'original_args="$*"' \
  'printf "%s|%s\\n" "$(tr "\\n" ";" < "$APT_SOURCES_FILE")" "$original_args" >> "$APT_TIMEOUT_LOG"' \
  'while [[ "$1" == --* ]]; do shift; done' \
  'duration="$1"; shift' \
  'if [[ "$APT_MODE" == deadline-source ]]; then' \
  '  if grep -q snapshot.debian.org "$APT_SOURCES_FILE"; then' \
  '    case "$original_args" in *" update "*) advance=2 ;; *"openjdk-11-jdk"*) advance="${duration%s}" ;; *" install "*) advance=2 ;; *) advance=0 ;; esac' \
  '  else advance=0; fi' \
  'elif [[ "$APT_MODE" == deadline-global ]]; then' \
  '  advance="${duration%s}"' \
  'else advance=0; fi' \
  'if [[ "$advance" -gt 0 ]]; then now="$(cat "$APT_CLOCK_FILE")"; printf "%s\\n" "$((now + advance))" > "$APT_CLOCK_FILE"; fi' \
  'exec "$@"' > "$TMP/bin/timeout"
chmod +x "$TMP/bin/timeout"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'if [[ "$1" == +%s ]]; then cat "$APT_CLOCK_FILE"; else exec /bin/date "$@"; fi' > "$TMP/bin/date"
chmod +x "$TMP/bin/date"

run_case() {
  local mode="$1" case_dir="$TMP/$1"
  local source_timeout=240 total_timeout=900
  case "$mode" in
    deadline-source) source_timeout=10; total_timeout=100 ;;
    deadline-global) source_timeout=8; total_timeout=10 ;;
  esac
  mkdir -p "$case_dir/lists"
  : > "$case_dir/lists/stale-before-first-update"
  : > "$case_dir/calls"
  : > "$case_dir/timeouts"
  printf '%s\n' 1000 > "$case_dir/clock"
  env PATH="$TMP/bin:$PATH" APT_MODE="$mode" APT_CALL_LOG="$case_dir/calls" \
    APT_TIMEOUT_LOG="$case_dir/timeouts" APT_CLOCK_FILE="$case_dir/clock" \
    APT_SOURCES_FILE="$case_dir/sources.list" APT_LISTS_DIR="$case_dir/lists" APT_RETRY_SLEEP_SECONDS=0 \
    APT_SOURCE_TIMEOUT_SECONDS="$source_timeout" APT_TOTAL_TIMEOUT_SECONDS="$total_timeout" \
    "$INSTALLER" "${EXPECTED_PACKAGES[@]}"
}

if run_case success >/dev/null 2>&1 \
    && [[ "$(grep -c snapshot.debian.org "$TMP/success/calls")" -eq 4 ]] \
    && [[ "$(grep -c ' update ' "$TMP/success/calls")" -eq 1 ]] \
    && ! grep -q archive.debian.org "$TMP/success/calls"; then
  ok "healthy snapshot uses one signed index state for all install phases"
else
  bad "healthy snapshot did not remain snapshot-only"
fi

grep ' install ' "$TMP/success/calls" > "$TMP/success/install-calls"
base_install="$(sed -n '1p' "$TMP/success/install-calls")"
bootstrap_install="$(sed -n '2p' "$TMP/success/install-calls")"
java_install="$(sed -n '3p' "$TMP/success/install-calls")"
if [[ "$(wc -l < "$TMP/success/install-calls" | tr -d ' ')" -eq 3 ]] \
    && [[ "$base_install" == *'bash curl wget perl gnupg apt-transport-https vim coreutils lsb-release'* ]] \
    && [[ "$base_install" != *openjdk-11-jdk* ]] \
    && [[ "$bootstrap_install" == *'openjdk-11-jre-headless'* ]] \
    && [[ "$java_install" == *'openjdk-11-jdk'* ]] \
    && [[ "$java_install" != *' bash '* ]] \
    && ! grep ' install ' "$TMP/success/calls" | grep -vq snapshot.debian.org; then
  ok "Buster base packages and headless runtime configure before the full JDK"
else
  bad "Buster package ordering can reproduce Debian bug #1030129 or mix index epochs"
fi

if run_case primary-down >/dev/null 2>&1 \
    && grep -q snapshot.debian.org "$TMP/primary-down/calls" \
    && grep -q snapshot-cloudflare.debian.org "$TMP/primary-down/calls" \
    && ! grep -q archive.debian.org "$TMP/primary-down/calls" \
    && [[ "$(grep -c ' install ' "$TMP/primary-down/calls")" -eq 3 ]] \
    && ! grep ' install ' "$TMP/primary-down/calls" | grep -q snapshot.debian.org \
    && ! grep ' install ' "$TMP/primary-down/calls" | grep -vq snapshot-cloudflare.debian.org; then
  ok "primary snapshot outage selects the pinned Cloudflare snapshot mirror"
else
  bad "primary snapshot outage did not select only the Cloudflare mirror"
fi

if run_case both-snapshots-down >/dev/null 2>&1 \
    && grep -q snapshot.debian.org "$TMP/both-snapshots-down/calls" \
    && grep -q snapshot-cloudflare.debian.org "$TMP/both-snapshots-down/calls" \
    && grep -q archive.debian.org "$TMP/both-snapshots-down/calls" \
    && [[ "$(grep -c ' install ' "$TMP/both-snapshots-down/calls")" -eq 3 ]] \
    && grep ' install ' "$TMP/both-snapshots-down/calls" | grep -q openjdk-11-jre-headless \
    && ! grep ' install ' "$TMP/both-snapshots-down/calls" | grep -Eq 'snapshot(-cloudflare)?[.]debian[.]org'; then
  ok "both snapshot outages select one archive index epoch without source mixing"
else
  bad "two-host snapshot outage did not select only the archive"
fi

if run_case archive-java-order >/dev/null 2>&1 \
    && bootstrap_line="$(grep -n ' install .*openjdk-11-jre-headless' "$TMP/archive-java-order/calls" | cut -d: -f1)" \
    && jdk_line="$(grep -n ' install .*openjdk-11-jdk' "$TMP/archive-java-order/calls" | cut -d: -f1)" \
    && [[ -n "$bootstrap_line" && -n "$jdk_line" && "$bootstrap_line" -lt "$jdk_line" ]]; then
  ok "archive path bootstraps Java before ca-certificates-java can hit missing java.security"
else
  bad "archive path reproduced the ca-certificates-java/java.security ordering failure"
fi

if run_case snapshot-java-order >/dev/null 2>&1 \
    && bootstrap_line="$(grep -n 'snapshot.debian.org.* install .*openjdk-11-jre-headless' "$TMP/snapshot-java-order/calls" | cut -d: -f1)" \
    && jdk_line="$(grep -n 'snapshot.debian.org.* install .*openjdk-11-jdk' "$TMP/snapshot-java-order/calls" | cut -d: -f1)" \
    && [[ -n "$bootstrap_line" && -n "$jdk_line" && "$bootstrap_line" -lt "$jdk_line" ]] \
    && ! grep ' install ' "$TMP/snapshot-java-order/calls" | grep -vq snapshot.debian.org; then
  ok "healthy snapshot bootstraps Java before ca-certificates-java can hit missing java.security"
else
  bad "healthy snapshot reproduced run 42257041432's ca-certificates-java/java.security failure"
fi

if run_case partial >/dev/null 2>&1 \
    && grep -q snapshot-cloudflare.debian.org "$TMP/partial/calls" \
    && ! grep -q archive.debian.org "$TMP/partial/calls" \
    && ! grep ' install ' "$TMP/partial/calls" | grep -q snapshot.debian.org; then
  ok "zero-status partial primary update selects the complete mirror before install"
else
  bad "partial primary indexes were accepted or mixed into install"
fi

if run_case signature >/dev/null 2>&1; then
  bad "signature failure was accepted or fell back"
elif grep -Eq 'snapshot-cloudflare[.]debian[.]org|archive[.]debian[.]org' "$TMP/signature/calls"; then
  bad "signature failure incorrectly triggered mirror or archive fallback"
else
  ok "signature failure fails closed before fallback"
fi

if run_case http404 >/dev/null 2>&1; then
  bad "HTTP 404 was accepted or triggered fallback"
elif grep -Eq 'snapshot-cloudflare[.]debian[.]org|archive[.]debian[.]org' "$TMP/http404/calls"; then
  bad "HTTP 404 incorrectly triggered mirror or archive fallback"
else
  ok "HTTP 404 fails closed before archive fallback"
fi

if run_case install-error >/dev/null 2>&1; then
  bad "ordinary package install failure was accepted"
elif grep -q archive.debian.org "$TMP/install-error/calls"; then
  bad "ordinary install failure incorrectly triggered archive fallback"
else
  ok "ordinary install failure fails closed without fallback"
fi

if run_case package-transport >/dev/null 2>&1 \
    && [[ "$(grep -c ' install ' "$TMP/package-transport/calls")" -eq 5 ]] \
    && [[ "$(grep -c -- '--fix-missing.* install ' "$TMP/package-transport/calls")" -eq 2 ]] \
    && ! grep -q archive.debian.org "$TMP/package-transport/calls"; then
  ok "package transport 504s retry with fix-missing on the selected signed index state"
else
  bad "package transport 504s were not recovered without rotating repository state"
fi

if run_case package-transport-rotate >/dev/null 2>&1 \
    && grep ' install ' "$TMP/package-transport-rotate/calls" | grep -q snapshot.debian.org \
    && grep -q 'snapshot-cloudflare.debian.org.* update ' "$TMP/package-transport-rotate/calls" \
    && [[ "$(grep -c 'snapshot-cloudflare.debian.org.* install ' "$TMP/package-transport-rotate/calls")" -eq 2 ]] \
    && ! grep -q archive.debian.org "$TMP/package-transport-rotate/calls"; then
  ok "exhausted package transport rotates to the next signed source within the total deadline"
else
  bad "package transport exhaustion consumed the total deadline or did not rotate sources"
fi

if run_case package-transport-archive >/dev/null 2>&1 \
    && archive_bootstrap_line="$(grep -n 'archive.debian.org.* install .*openjdk-11-jre-headless' "$TMP/package-transport-archive/calls" | cut -d: -f1)" \
    && archive_jdk_line="$(grep -n 'archive.debian.org.* install .*openjdk-11-jdk' "$TMP/package-transport-archive/calls" | cut -d: -f1)" \
    && [[ -n "$archive_bootstrap_line" && -n "$archive_jdk_line" && "$archive_bootstrap_line" -lt "$archive_jdk_line" ]]; then
  ok "Java package transport exhaustion reaches archive and preserves the headless bootstrap"
else
  bad "Java package rotation into archive skipped the required headless bootstrap"
fi

# test_AC2929_1_source_deadline_spans_all_phases
if run_case deadline-source >/dev/null 2>&1 \
    && primary_timeout_limits="$(grep snapshot.debian.org "$TMP/deadline-source/timeouts" \
      | sed -E 's/.*\|[^|]* ([0-9]+)s .*/\1/' | paste -sd, -)" \
    && grep -q 'snapshot.debian.org.* update ' "$TMP/deadline-source/calls" \
    && [[ "$(grep -c 'snapshot.debian.org.* install ' "$TMP/deadline-source/calls")" -eq 3 ]] \
    && grep -q 'snapshot-cloudflare.debian.org.* update ' "$TMP/deadline-source/calls" \
    && [[ "$(grep -c 'snapshot-cloudflare.debian.org.* install ' "$TMP/deadline-source/calls")" -eq 2 ]] \
    && ! grep -q archive.debian.org "$TMP/deadline-source/calls" \
    && [[ "$primary_timeout_limits" == 10,8,6,4 ]] \
    && grep -q 'snapshot-cloudflare.debian.org.*|.*10s .* update ' "$TMP/deadline-source/timeouts" \
    && cloudflare_bootstrap_line="$(grep -n 'snapshot-cloudflare.debian.org.* install .*openjdk-11-jre-headless' "$TMP/deadline-source/calls" | cut -d: -f1)" \
    && cloudflare_jdk_line="$(grep -n 'snapshot-cloudflare.debian.org.* install .*openjdk-11-jdk' "$TMP/deadline-source/calls" | cut -d: -f1)" \
    && [[ -n "$cloudflare_bootstrap_line" && -n "$cloudflare_jdk_line" \
      && "$cloudflare_bootstrap_line" -lt "$cloudflare_jdk_line" ]]; then
  ok "one source deadline spans update and every install phase before rotating"
else
  bad "source deadline reset between update, base install, Java bootstrap, or JDK install"
fi

# test_AC2929_2_global_deadline_terminates_rotation
if run_case deadline-global >/dev/null 2>&1; then
  bad "global APT deadline expiration was accepted"
elif [[ "$(grep -c ' update ' "$TMP/deadline-global/calls")" -eq 2 ]] \
    && grep -q '8s .* update ' "$TMP/deadline-global/timeouts" \
    && grep -q '2s .* update ' "$TMP/deadline-global/timeouts" \
    && ! grep -q archive.debian.org "$TMP/deadline-global/calls"; then
  ok "global APT deadline terminates source rotation"
else
  bad "global APT deadline did not cap the ordered source attempts"
fi

if run_case archive-java-order >/dev/null 2>&1 \
    && [[ "$(grep -c ' update ' "$TMP/archive-java-order/calls")" -eq 13 ]] \
    && [[ "$(grep -c 'snapshot.debian.org.* update ' "$TMP/archive-java-order/calls")" -eq 6 ]] \
    && [[ "$(grep -c 'snapshot-cloudflare.debian.org.* update ' "$TMP/archive-java-order/calls")" -eq 6 ]] \
    && [[ "$(grep -c 'archive.debian.org.* update ' "$TMP/archive-java-order/calls")" -eq 1 ]] \
    && [[ "$(grep -c ' update ' "$TMP/archive-java-order/timeouts")" -eq 13 ]] \
    && [[ "$(grep -c ' install ' "$TMP/archive-java-order/calls")" -eq 3 ]] \
    && [[ "$(grep -c 'openjdk-11-jre-headless' "$TMP/archive-java-order/calls")" -eq 1 ]] \
    && [[ "$(grep -c 'openjdk-11-jdk' "$TMP/archive-java-order/calls")" -eq 1 ]] \
    && ! grep ' install ' "$TMP/archive-java-order/calls" | grep -vq archive.debian.org \
    && ! grep -Eq -- '--allow-unauthenticated|trusted=yes' "$INSTALLER"; then
  ok "archive fallback rides the extended retry window and bootstraps Java from one signed index state"
else
  bad "archive fallback did not retry, bootstrap Java, or preserve signature verification"
fi

DOCKERFILE="$(dirname "$INSTALLER")/Dockerfile"
docker_packages="$(awk '
  /&& install-druid-apt-packages \\/ { capture=1; next }
  capture {
    line=$0
    sub(/^[[:space:]]+/, "", line)
    sub(/[[:space:]]*\\$/, "", line)
    print line
    if ($0 !~ /\\$/) exit
  }
' "$DOCKERFILE" | tr '\n' ' ' | sed 's/ $//')"
expected_packages="${EXPECTED_PACKAGES[*]}"
if grep -Fq 'http://snapshot.debian.org/archive/debian/${SNAPSHOT_TIMESTAMP} buster main' "$INSTALLER" \
    && grep -Fq 'http://snapshot.debian.org/archive/debian-security/${SNAPSHOT_TIMESTAMP} buster/updates main' "$INSTALLER" \
    && grep -Fq 'http://snapshot-cloudflare.debian.org/archive/debian/${SNAPSHOT_TIMESTAMP} buster main' "$INSTALLER" \
    && grep -Fq 'http://snapshot-cloudflare.debian.org/archive/debian-security/${SNAPSHOT_TIMESTAMP} buster/updates main' "$INSTALLER" \
    && grep -Fq 'http://archive.debian.org/debian buster main' "$INSTALLER" \
    && grep -Fq 'http://archive.debian.org/debian-security buster/updates main' "$INSTALLER" \
    && [[ "$(grep -Fc 'check-valid-until=no' "$INSTALLER")" -eq 6 ]] \
    && grep -Fq 'APT_RETRY_ATTEMPTS="${APT_RETRY_ATTEMPTS:-6}"' "$INSTALLER" \
    && grep -Fq 'APT_RETRY_SLEEP_SECONDS="${APT_RETRY_SLEEP_SECONDS:-30}"' "$INSTALLER" \
    && grep -Fq 'APT_SOURCE_TIMEOUT_SECONDS="${APT_SOURCE_TIMEOUT_SECONDS:-240}"' "$INSTALLER" \
    && grep -Fq 'APT_TOTAL_TIMEOUT_SECONDS="${APT_TOTAL_TIMEOUT_SECONDS:-900}"' "$INSTALLER" \
    && [[ "$(grep -Fc '    && install-druid-apt-packages \' "$DOCKERFILE")" -eq 1 ]] \
    && [[ "$docker_packages" == "$expected_packages" ]]; then
  ok "official suites and original package-name set select one repository epoch"
else
  bad "repository URLs/suites or package-name set drifted, or Docker can mix epochs"
fi

DIAGNOSTIC="$ROOT/appbase-java/appbase-client/appbase-client-druid/infra/docker/druid/diagnose-druid-build-stages.sh"
diagnostic_bootstrap_line="$(grep -n 'apt-get -qq install -y openjdk-11-jre-headless' "$DIAGNOSTIC" | cut -d: -f1)"
diagnostic_jdk_line="$(grep -n 'apt-get -qq install -y openjdk-11-jdk' "$DIAGNOSTIC" | cut -d: -f1)"
if [[ -n "$diagnostic_bootstrap_line" && -n "$diagnostic_jdk_line" \
    && "$diagnostic_bootstrap_line" -lt "$diagnostic_jdk_line" ]]; then
  ok "archive-only Druid diagnostic bootstraps Java before the full JDK"
else
  bad "archive-only Druid diagnostic retains the ca-certificates-java ordering exposure"
fi

echo "druid-apt-fallback-test: $PASSED passed, $FAILED failed"
[[ "$FAILED" -eq 0 && "$PASSED" -eq 18 ]]
