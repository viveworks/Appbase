#!/usr/bin/env bash
set -euo pipefail

BASEDIR=$(cd -- "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1; pwd -P)
# shellcheck disable=SC1091
source "$BASEDIR/env-druid.sh"

DRUID_VERSION=34.0.0
DIST_DIR="${DRUID_DIST_DIR:-$BASEDIR/.druid-dist}"
ARCHIVE="apache-druid-${DRUID_VERSION}-bin.tar.gz"
BASE_URL="${DRUID_BASE_URL:-https://dlcdn.apache.org/druid/${DRUID_VERSION}}"
DOWNLOAD_ATTEMPTS="${DRUID_DOWNLOAD_ATTEMPTS:-3}"
DOWNLOAD_TIMEOUT_STEP_SECONDS="${DRUID_DOWNLOAD_TIMEOUT_STEP_SECONDS:-60}"
DOWNLOAD_BACKOFF_STEP_SECONDS="${DRUID_DOWNLOAD_BACKOFF_STEP_SECONDS:-5}"
mkdir -p "$DIST_DIR"

download_with_backoff() {
  local file="$1" attempt timeout_seconds temp_file download_exit
  for ((attempt = 1; attempt <= DOWNLOAD_ATTEMPTS; attempt++)); do
    timeout_seconds=$((attempt * DOWNLOAD_TIMEOUT_STEP_SECONDS))
    temp_file="$DIST_DIR/${file}.part"
    rm -f "$temp_file"
    echo "Downloading $file (attempt $attempt/$DOWNLOAD_ATTEMPTS, timeout ${timeout_seconds}s)"
    if timeout "$timeout_seconds" wget --quiet --timeout="$timeout_seconds" --tries=1 \
        --output-document="$temp_file" "$BASE_URL/$file"; then
      mv "$temp_file" "$DIST_DIR/$file"
      return 0
    else
      download_exit=$?
      echo "Download attempt $attempt/$DOWNLOAD_ATTEMPTS failed or timed out (exit=$download_exit): $file" >&2
    fi
    rm -f "$temp_file"
    [[ "$attempt" -eq "$DOWNLOAD_ATTEMPTS" ]] \
      || sleep $((attempt * DOWNLOAD_BACKOFF_STEP_SECONDS))
  done
  echo "Failed to download $file after $DOWNLOAD_ATTEMPTS attempts" >&2
  return 1
}

main() {
  [[ -s "$DIST_DIR/$ARCHIVE" ]] || download_with_backoff "$ARCHIVE"
  [[ -s "$DIST_DIR/$ARCHIVE.sha512" ]] || download_with_backoff "$ARCHIVE.sha512"
  if ! (cd "$DIST_DIR" && echo "$(cat "$ARCHIVE.sha512") $ARCHIVE" | sha512sum --check --status); then
    echo "Cached Druid distribution failed checksum; downloading both files once more" >&2
    rm -f "$DIST_DIR/$ARCHIVE" "$DIST_DIR/$ARCHIVE.sha512"
    download_with_backoff "$ARCHIVE"
    download_with_backoff "$ARCHIVE.sha512"
    (cd "$DIST_DIR" && echo "$(cat "$ARCHIVE.sha512") $ARCHIVE" | sha512sum --check --status)
  fi

  BUILDKIT_PROGRESS=${BUILDKIT_PROGRESS:-plain} docker build -t "$TAG" "$BASEDIR"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
