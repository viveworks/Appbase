#!/usr/bin/env bash
# Install Druid's top-level Debian package set from one repository epoch.
set -euo pipefail

if [[ "$#" -eq 0 ]]; then
  echo "usage: install-apt-packages.sh PACKAGE [...]" >&2
  exit 2
fi
BASE_PACKAGES=()
JAVA_PACKAGES=()
for package in "$@"; do
  if [[ ! "$package" =~ ^[a-z0-9][a-z0-9+.-]*$ ]]; then
    echo "Invalid Druid APT package name: $package" >&2
    exit 2
  fi
  if [[ "$package" == openjdk-11-jdk ]]; then
    JAVA_PACKAGES+=("$package")
  else
    BASE_PACKAGES+=("$package")
  fi
done

APT_SOURCES_FILE="${APT_SOURCES_FILE:-/etc/apt/sources.list}"
APT_LISTS_DIR="${APT_LISTS_DIR:-/var/lib/apt/lists}"
APT_RETRY_ATTEMPTS="${APT_RETRY_ATTEMPTS:-6}"
APT_RETRY_SLEEP_SECONDS="${APT_RETRY_SLEEP_SECONDS:-30}"
APT_SOURCE_TIMEOUT_SECONDS="${APT_SOURCE_TIMEOUT_SECONDS:-240}"
APT_TOTAL_TIMEOUT_SECONDS="${APT_TOTAL_TIMEOUT_SECONDS:-900}"
case "$APT_RETRY_ATTEMPTS:$APT_SOURCE_TIMEOUT_SECONDS:$APT_TOTAL_TIMEOUT_SECONDS" in
  *[!0-9:]*|0:*|*:0:*|*:0) echo "Druid APT retry counts/timeouts must be positive integers" >&2; exit 2 ;;
esac
case "$APT_RETRY_SLEEP_SECONDS" in
  ''|*[!0-9]*) echo "Druid APT retry sleep must be a non-negative integer" >&2; exit 2 ;;
esac
command -v timeout >/dev/null 2>&1 || { echo "Druid APT retry ceiling requires coreutils timeout" >&2; exit 2; }
SNAPSHOT_TIMESTAMP=20240311T000000Z
# HTTP avoids requiring a CA bundle before ca-certificates can itself be installed. APT still
# verifies Debian's signed Release/InRelease metadata; no repository is marked trusted.
SNAPSHOT_SOURCES="deb [check-valid-until=no] http://snapshot.debian.org/archive/debian/${SNAPSHOT_TIMESTAMP} buster main
deb [check-valid-until=no] http://snapshot.debian.org/archive/debian-security/${SNAPSHOT_TIMESTAMP} buster/updates main"
SNAPSHOT_CLOUDFLARE_SOURCES="deb [check-valid-until=no] http://snapshot-cloudflare.debian.org/archive/debian/${SNAPSHOT_TIMESTAMP} buster main
deb [check-valid-until=no] http://snapshot-cloudflare.debian.org/archive/debian-security/${SNAPSHOT_TIMESTAMP} buster/updates main"
ARCHIVE_SOURCES='deb [check-valid-until=no] http://archive.debian.org/debian buster main
deb [check-valid-until=no] http://archive.debian.org/debian-security buster/updates main'
APT_OPTIONS=(-o Acquire::Check-Valid-Until=false -o Acquire::Retries=5)
UPDATE_LOG="$(mktemp)"
trap 'rm -f -- "$UPDATE_LOG"' EXIT
APT_TOTAL_DEADLINE_EPOCH=$(( $(date +%s) + APT_TOTAL_TIMEOUT_SECONDS ))

write_sources() {
  printf '%s\n' "$1" > "$APT_SOURCES_FILE"
}

clear_lists() {
  local entries=()
  case "$APT_LISTS_DIR" in
    /*) ;;
    *) echo "Druid APT lists directory must be absolute: $APT_LISTS_DIR" >&2; return 20 ;;
  esac
  if [[ "$APT_LISTS_DIR" == / || ! -d "$APT_LISTS_DIR" ]]; then
    echo "Druid APT lists directory is unsafe or missing: $APT_LISTS_DIR" >&2
    return 20
  fi
  shopt -s nullglob dotglob
  entries=("$APT_LISTS_DIR"/*)
  shopt -u nullglob dotglob
  if [[ "${#entries[@]}" -gt 0 ]]; then
    rm -rf -- "${entries[@]}"
  fi
}

has_metadata_failure() {
  grep -Eqi 'signatures? (were )?invalid|NO_PUBKEY|not signed|does not have a Release file|Clearsigned file.*isn.t valid|Hash Sum mismatch|Malformed' "$UPDATE_LOG"
}

has_http_client_failure() {
  grep -Eqi '(Failed to fetch|Err:|HTTP/[0-9.]+).*([^0-9]|^)4[0-9][0-9]([^0-9]|$)|^[[:space:]]*4[0-9][0-9][[:space:]]' "$UPDATE_LOG"
}

has_transport_failure() {
  grep -Eqi 'Failed to fetch|Could not connect|Connection failed|Connection timed out|Temporary failure resolving|TLS connection| 504 |504 Gateway' "$UPDATE_LOG"
}

# Return 0 for a complete signed index refresh, 10 for an exhausted transport outage (the only
# condition that may choose another mirror), and 20 for metadata/signature/ordinary APT failures.
update_with_bounded_retries() {
  local label="$1" attempt rc now remaining sleep_seconds source_deadline
  source_deadline=$(( $(date +%s) + APT_SOURCE_TIMEOUT_SECONDS ))
  if [[ "$source_deadline" -gt "$APT_TOTAL_DEADLINE_EPOCH" ]]; then
    source_deadline="$APT_TOTAL_DEADLINE_EPOCH"
  fi
  for ((attempt = 1; attempt <= APT_RETRY_ATTEMPTS; attempt++)); do
    now="$(date +%s)"
    remaining=$(( source_deadline - now ))
    if [[ "$remaining" -le 0 ]]; then
      echo "Druid $label APT transport retry window exhausted" >&2
      return 10
    fi
    clear_lists || return $?
    : > "$UPDATE_LOG"
    if timeout --signal=TERM --kill-after=5s "${remaining}s" \
        apt-get "${APT_OPTIONS[@]}" update -y >"$UPDATE_LOG" 2>&1; then
      rc=0
    else
      rc=$?
    fi
    if [[ "$rc" -eq 124 || "$rc" -eq 137 ]]; then
      echo "Connection timed out: Druid $label APT retry window exhausted" >> "$UPDATE_LOG"
    fi
    cat "$UPDATE_LOG"
    if has_metadata_failure; then
      echo "Druid $label APT metadata/signature verification failed; refusing fallback" >&2
      return 20
    fi
    if has_http_client_failure; then
      echo "Druid $label APT update returned an HTTP client error; refusing fallback" >&2
      return 20
    fi
    # APT can return zero after ignoring an unavailable index. Treat any transport diagnostic as
    # incomplete so packages never install from a partial mixture of old and new index state.
    if [[ "$rc" -eq 0 ]] && ! has_transport_failure; then
      return 0
    fi
    if ! has_transport_failure; then
      echo "Druid $label APT update failed for a non-transport reason (rc=$rc)" >&2
      return 20
    fi
    if [[ "$attempt" -lt "$APT_RETRY_ATTEMPTS" ]]; then
      echo "Druid $label APT transport failure (attempt $attempt/$APT_RETRY_ATTEMPTS); retrying" >&2
      now="$(date +%s)"
      remaining=$(( source_deadline - now ))
      [[ "$remaining" -gt 0 ]] || return 10
      sleep_seconds="$APT_RETRY_SLEEP_SECONDS"
      if [[ "$sleep_seconds" -gt "$remaining" ]]; then
        sleep_seconds="$remaining"
      fi
      if [[ "$sleep_seconds" -gt 0 ]]; then
        sleep "$sleep_seconds"
      fi
    fi
  done
  return 10
}

install_with_bounded_retries() {
  local label="$1" attempt rc now remaining sleep_seconds source_deadline
  shift
  source_deadline=$(( $(date +%s) + APT_SOURCE_TIMEOUT_SECONDS ))
  if [[ "$source_deadline" -gt "$APT_TOTAL_DEADLINE_EPOCH" ]]; then
    source_deadline="$APT_TOTAL_DEADLINE_EPOCH"
  fi
  for ((attempt = 1; attempt <= APT_RETRY_ATTEMPTS; attempt++)); do
    now="$(date +%s)"
    remaining=$(( source_deadline - now ))
    if [[ "$remaining" -le 0 ]]; then
      echo "Druid $label APT package transport retry window exhausted" >&2
      return 10
    fi
    : > "$UPDATE_LOG"
    if [[ "$attempt" -gt 1 ]]; then
      if timeout --signal=TERM --kill-after=5s "${remaining}s" \
          apt-get "${APT_OPTIONS[@]}" --fix-missing install -y --no-install-recommends "$@" \
          >"$UPDATE_LOG" 2>&1; then
        rc=0
      else
        rc=$?
      fi
    else
      if timeout --signal=TERM --kill-after=5s "${remaining}s" \
          apt-get "${APT_OPTIONS[@]}" install -y --no-install-recommends "$@" \
          >"$UPDATE_LOG" 2>&1; then
        rc=0
      else
        rc=$?
      fi
    fi
    if [[ "$rc" -eq 124 || "$rc" -eq 137 ]]; then
      echo "Connection timed out: Druid $label APT package retry ceiling exhausted" >> "$UPDATE_LOG"
    fi
    cat "$UPDATE_LOG"
    if has_metadata_failure || has_http_client_failure; then
      echo "Druid $label APT package verification/client failure; refusing retry" >&2
      return 20
    fi
    if [[ "$rc" -eq 0 ]] && ! has_transport_failure; then
      return 0
    fi
    if ! has_transport_failure; then
      echo "Druid $label APT package install failed for a non-transport reason (rc=$rc)" >&2
      return 20
    fi
    if [[ "$attempt" -lt "$APT_RETRY_ATTEMPTS" ]]; then
      now="$(date +%s)"
      remaining=$(( source_deadline - now ))
      [[ "$remaining" -gt 0 ]] || return 10
      echo "Druid $label APT package transport failure (attempt $attempt/$APT_RETRY_ATTEMPTS); retrying with --fix-missing" >&2
      sleep_seconds="$APT_RETRY_SLEEP_SECONDS"
      if [[ "$sleep_seconds" -gt "$remaining" ]]; then
        sleep_seconds="$remaining"
      fi
      if [[ "$sleep_seconds" -gt 0 ]]; then
        sleep "$sleep_seconds"
      fi
    fi
  done
  return 10
}

# A signed index refresh can succeed while that same host later times out serving package objects.
# Give each ordered source its own bounded package window; only exhausted transport failures may
# advance to the next source, which is always independently refreshed and signature-verified first.
advance_source_after_transport() {
  local next_source next_sources rc
  while true; do
    case "$selected_source" in
      snapshot)
        next_source=snapshot-cloudflare
        next_sources="$SNAPSHOT_CLOUDFLARE_SOURCES"
        echo "Druid primary immutable snapshot package transport is exhausted; trying the pinned Cloudflare mirror" >&2
        ;;
      snapshot-cloudflare)
        next_source=archive
        next_sources="$ARCHIVE_SOURCES"
        echo "Druid immutable snapshot package transports are exhausted; trying signed final Buster archive indexes" >&2
        ;;
      archive) return 10 ;;
      *) echo "Unknown selected Druid APT source: $selected_source" >&2; return 20 ;;
    esac
    selected_source="$next_source"
    write_sources "$next_sources"
    if update_with_bounded_retries "$selected_source"; then
      echo "Druid APT source selected: $selected_source" >&2
      return 0
    else
      rc=$?
    fi
    [[ "$rc" -eq 10 ]] || return "$rc"
  done
}

install_with_source_rotation() {
  local label="$1" rc
  shift
  while true; do
    if install_with_bounded_retries "$label" "$@"; then
      return 0
    else
      rc=$?
    fi
    [[ "$rc" -eq 10 ]] || return "$rc"
    advance_source_after_transport || return $?
  done
}

install_java_with_source_rotation() {
  local rc bootstrapped_source=''
  while true; do
    if [[ "$bootstrapped_source" != "$selected_source" ]]; then
      if install_with_bounded_retries java-bootstrap openjdk-11-jre-headless; then
        bootstrapped_source="$selected_source"
      else
        rc=$?
        [[ "$rc" -eq 10 ]] || return "$rc"
        advance_source_after_transport || return $?
        continue
      fi
    fi
    if install_with_bounded_retries java-packages "${JAVA_PACKAGES[@]}"; then
      return 0
    else
      rc=$?
    fi
    [[ "$rc" -eq 10 ]] || return "$rc"
    advance_source_after_transport || return $?
  done
}

write_sources "$SNAPSHOT_SOURCES"
if update_with_bounded_retries snapshot; then
  snapshot_rc=0
else
  snapshot_rc=$?
fi
case "$snapshot_rc" in
  0) selected_source=snapshot ;;
  10)
    echo "Druid primary immutable snapshot is unavailable; trying the pinned Cloudflare mirror" >&2
    write_sources "$SNAPSHOT_CLOUDFLARE_SOURCES"
    if update_with_bounded_retries snapshot-cloudflare; then
      cloudflare_rc=0
    else
      cloudflare_rc=$?
    fi
    case "$cloudflare_rc" in
      0) selected_source=snapshot-cloudflare ;;
      10)
        echo "Druid immutable snapshot mirrors are unavailable; using signed final Buster archive indexes" >&2
        write_sources "$ARCHIVE_SOURCES"
        if update_with_bounded_retries archive; then
          archive_rc=0
        else
          archive_rc=$?
        fi
        [[ "$archive_rc" -eq 0 ]] || exit "$archive_rc"
        selected_source=archive
        ;;
      *) exit "$cloudflare_rc" ;;
    esac
    ;;
  *) exit "$snapshot_rc" ;;
esac

echo "Druid APT source selected: $selected_source" >&2
# Debian bug #1030129: Buster can configure ca-certificates-java before its unpacked OpenJDK has a
# usable java.security file. Keep unrelated packages separate and then bootstrap Java below, without
# refreshing or changing the one selected signed index state.
if [[ "${#BASE_PACKAGES[@]}" -gt 0 ]]; then
  install_with_source_rotation base-packages "${BASE_PACKAGES[@]}" || exit $?
fi
if [[ "${#JAVA_PACKAGES[@]}" -gt 0 ]]; then
  # Buster can configure ca-certificates-java before the full JDK has installed a usable
  # java.security file on any of the signed sources. Bootstrap the headless runtime before the
  # full JDK, and repeat that bootstrap whenever package transport rotates to a new source.
  install_java_with_source_rotation || exit $?
fi
