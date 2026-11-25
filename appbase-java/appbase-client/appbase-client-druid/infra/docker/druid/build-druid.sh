#!/usr/bin/env bash
set -euo pipefail

BASEDIR=$(cd -- "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1; pwd -P)
# shellcheck disable=SC1091
source "$BASEDIR/env-druid.sh"

DRUID_VERSION=34.0.0
DIST_DIR="${DRUID_DIST_DIR:-$BASEDIR/.druid-dist}"
ARCHIVE="apache-druid-${DRUID_VERSION}-bin.tar.gz"
CANONICAL_BASE_URL="${DRUID_BASE_URL:-https://downloads.apache.org/druid/${DRUID_VERSION}}"
ARCHIVE_BASE_URL="${DRUID_ARCHIVE_BASE_URL:-https://archive.apache.org/dist/druid/${DRUID_VERSION}}"
CLOSER_BASE_URL="${DRUID_CLOSER_BASE_URL:-https://www.apache.org/dyn/closer.lua/druid/${DRUID_VERSION}}"
# The Druid binary distribution is ~630MB. Once a version drops off Apache's "current release"
# mirror network (downloads.apache.org/dlcdn.apache.org both 404 for it), archive.apache.org is
# the only place it still exists, and that host throttles bulk downloads by design (it serves a
# `Vary: Slow,Glacial` header) -- closer.lua's redirect resolves to that exact same host for a
# version in this state, so it is not a faster alternative, just the same throttled transfer
# reached a different way. 60s-stepped timeouts (60/120/180s) were sized for a fast mirror; against
# the throttled archive host they only ever pull a few MB per attempt before the per-source budget
# is exhausted and the partial progress is discarded. Longer, fewer attempts let the resumed
# download actually make it to completion within one source's budget.
DOWNLOAD_ATTEMPTS="${DRUID_DOWNLOAD_ATTEMPTS:-4}"
DOWNLOAD_TIMEOUT_STEP_SECONDS="${DRUID_DOWNLOAD_TIMEOUT_STEP_SECONDS:-600}"
DOWNLOAD_BACKOFF_STEP_SECONDS="${DRUID_DOWNLOAD_BACKOFF_STEP_SECONDS:-5}"
mkdir -p "$DIST_DIR"

download_with_backoff() {
  local file="$1" url="${2:-$CANONICAL_BASE_URL/$1}" attempt timeout_seconds temp_file download_exit
  temp_file="$DIST_DIR/${file}.part"
  for ((attempt = 1; attempt <= DOWNLOAD_ATTEMPTS; attempt++)); do
    timeout_seconds=$((attempt * DOWNLOAD_TIMEOUT_STEP_SECONDS))
    if [[ -s "$temp_file" ]]; then
      echo "Resuming $file from $(wc -c < "$temp_file") bytes"
    else
      rm -f "$temp_file"
    fi
    echo "Downloading $file (attempt $attempt/$DOWNLOAD_ATTEMPTS, timeout ${timeout_seconds}s)"
    if timeout "$timeout_seconds" wget --quiet --continue --timeout="$timeout_seconds" --tries=1 \
        --output-document="$temp_file" "$url"; then
      mv "$temp_file" "$DIST_DIR/$file"
      return 0
    else
      download_exit=$?
      echo "Download attempt $attempt/$DOWNLOAD_ATTEMPTS failed or timed out (exit=$download_exit): $file" >&2
    fi
    [[ "$attempt" -eq "$DOWNLOAD_ATTEMPTS" ]] \
      || sleep $((attempt * DOWNLOAD_BACKOFF_STEP_SECONDS))
  done
  rm -f "$temp_file"
  echo "Failed to download $file after $DOWNLOAD_ATTEMPTS attempts" >&2
  return 1
}

archive_checksum_is_valid() {
  (cd "$DIST_DIR" && echo "$(cat "$ARCHIVE.sha512") $ARCHIVE" | sha512sum --check --status)
}

download_verified_archive() {
  local source source_name source_url
  local -a sources=(
    "canonical|$CANONICAL_BASE_URL/$ARCHIVE"
    "archive|$ARCHIVE_BASE_URL/$ARCHIVE"
    "closer|$CLOSER_BASE_URL/$ARCHIVE?action=download"
  )

  if [[ -s "$DIST_DIR/$ARCHIVE" ]] && archive_checksum_is_valid; then
    return 0
  fi
  rm -f "$DIST_DIR/$ARCHIVE" "$DIST_DIR/$ARCHIVE.part"

  for source in "${sources[@]}"; do
    source_name="${source%%|*}"
    source_url="${source#*|}"
    echo "Trying Druid distribution source: $source_name"
    if download_with_backoff "$ARCHIVE" "$source_url"; then
      if archive_checksum_is_valid; then
        echo "Verified Druid distribution from source: $source_name"
        return 0
      fi
      echo "Druid distribution from source $source_name failed canonical SHA-512 verification" >&2
      rm -f "$DIST_DIR/$ARCHIVE" "$DIST_DIR/$ARCHIVE.part"
    fi
  done

  echo "Failed to download a canonically verified Druid distribution from every source" >&2
  return 1
}

main() {
  # The checksum is intentionally fetched only from Apache's canonical current-release host.
  # Payload mirrors are never trusted to supply their own integrity value.
  rm -f "$DIST_DIR/$ARCHIVE.sha512" "$DIST_DIR/$ARCHIVE.sha512.part"
  download_with_backoff "$ARCHIVE.sha512" "$CANONICAL_BASE_URL/$ARCHIVE.sha512"
  download_verified_archive

  BUILDKIT_PROGRESS=${BUILDKIT_PROGRESS:-plain} docker build -t "$TAG" "$BASEDIR"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
