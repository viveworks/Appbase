#!/usr/bin/env bash
# Proves bounded Druid archive retries resume a partial transfer instead of restarting at byte zero.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BUILD_SCRIPT="$ROOT/appbase-java/appbase-client/appbase-client-druid/infra/docker/druid/build-druid.sh"
WORK="$(mktemp -d)"
SERVER_PID=''
cleanup() {
  if [[ -n "$SERVER_PID" ]]; then
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
  rm -rf "$WORK"
}
trap cleanup EXIT

python3 - "$WORK/port" "$WORK/requests" <<'PY' >/dev/null 2>&1 &
import hashlib
import http.server
import pathlib
import sys
import time
import urllib.parse

port_file = pathlib.Path(sys.argv[1])
requests_file = pathlib.Path(sys.argv[2])

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        path = urllib.parse.urlsplit(self.path).path
        with requests_file.open("a") as requests:
            requests.write(path + "\n")
        fast = False
        if path == "/canonical/fallback.bin.sha512":
            content = hashlib.sha512(b"verified-payload").hexdigest().encode()
            fast = True
        elif path == "/canonical/fallback.bin":
            self.send_error(503)
            return
        elif path == "/archive/fallback.bin":
            content = b"corrupt-payload"
            fast = True
        elif path == "/mirror/fallback.bin":
            content = b"verified-payload"
            fast = True
        else:
            content = b"x" * (100 if path.endswith("never.bin") else 12)
        range_header = self.headers.get("Range")
        start = 0
        if range_header:
            start = int(range_header.removeprefix("bytes=").removesuffix("-"))
            self.send_response(206)
            self.send_header("Content-Range", f"bytes {start}-{len(content) - 1}/{len(content)}")
        else:
            self.send_response(200)
        body = content[start:]
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        try:
            if fast:
                self.wfile.write(body)
            else:
                for byte in body:
                    self.wfile.write(bytes([byte]))
                    self.wfile.flush()
                    time.sleep(0.15)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def log_message(self, *_):
        pass

server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
port_file.write_text(str(server.server_port))
server.serve_forever()
PY
SERVER_PID=$!

for _ in {1..50}; do
  [[ -s "$WORK/port" ]] && break
  sleep 0.1
done
[[ -s "$WORK/port" ]] || { echo "FAIL trickle server did not start" >&2; exit 1; }

export DRUID_BASE_URL="http://127.0.0.1:$(cat "$WORK/port")"
export DRUID_DIST_DIR="$WORK/dist"
export DRUID_DOWNLOAD_ATTEMPTS=2
export DRUID_DOWNLOAD_TIMEOUT_STEP_SECONDS=1
export DRUID_DOWNLOAD_BACKOFF_STEP_SECONDS=0
# shellcheck disable=SC1090
source "$BUILD_SCRIPT"

start_seconds=$(date +%s)
set +e
output="$(download_with_backoff trickle.bin 2>&1)"
download_exit=$?
set -e
elapsed_seconds=$(( $(date +%s) - start_seconds ))

[[ "$download_exit" -eq 0 ]] || { echo "FAIL resumed trickle download did not succeed" >&2; echo "$output" >&2; exit 1; }
[[ "$output" == *"attempt 1/2, timeout 1s"* ]] \
  && [[ "$output" == *"attempt 2/2, timeout 2s"* ]] \
  || { echo "FAIL retry progression was not logged" >&2; echo "$output" >&2; exit 1; }
[[ "$(grep -c 'failed or timed out (exit=124)' <<<"$output")" -eq 1 ]] \
  || { echo "FAIL first trickle attempt was not wall-clock terminated" >&2; echo "$output" >&2; exit 1; }
[[ "$output" == *"Resuming trickle.bin from "* ]] \
  || { echo "FAIL second attempt did not report partial-transfer resume" >&2; echo "$output" >&2; exit 1; }
(( elapsed_seconds >= 1 && elapsed_seconds <= 5 )) \
  || { echo "FAIL resumed retry series took ${elapsed_seconds}s; expected bounded 1-5s" >&2; exit 1; }
[[ "$(wc -c < "$DRUID_DIST_DIR/trickle.bin")" -eq 12 ]] \
  || { echo "FAIL resumed download is incomplete" >&2; exit 1; }
[[ ! -e "$DRUID_DIST_DIR/trickle.bin.part" ]] \
  || { echo "FAIL partial download survived successful completion" >&2; exit 1; }

set +e
failure_output="$(download_with_backoff never.bin 2>&1)"
failure_exit=$?
set -e
[[ "$failure_exit" -ne 0 ]] || { echo "FAIL permanently slow download unexpectedly succeeded" >&2; exit 1; }
[[ "$failure_output" == *"Resuming never.bin from "* ]] \
  || { echo "FAIL permanently slow download did not resume between attempts" >&2; echo "$failure_output" >&2; exit 1; }
[[ ! -e "$DRUID_DIST_DIR/never.bin.part" ]] \
  || { echo "FAIL partial download survived final failed attempt" >&2; exit 1; }

rm -rf "$DRUID_DIST_DIR"
mkdir -p "$DRUID_DIST_DIR"
: > "$WORK/requests"
ARCHIVE=fallback.bin
CANONICAL_BASE_URL="http://127.0.0.1:$(cat "$WORK/port")/canonical"
ARCHIVE_BASE_URL="http://127.0.0.1:$(cat "$WORK/port")/archive"
CLOSER_BASE_URL="http://127.0.0.1:$(cat "$WORK/port")/mirror"
DOWNLOAD_ATTEMPTS=1
download_with_backoff "$ARCHIVE.sha512" "$CANONICAL_BASE_URL/$ARCHIVE.sha512"
set +e
fallback_output="$(download_verified_archive 2>&1)"
fallback_exit=$?
set -e
[[ "$fallback_exit" -eq 0 ]] \
  || { echo "FAIL ordered fallback did not find a verified payload" >&2; echo "$fallback_output" >&2; exit 1; }
[[ "$fallback_output" == *"Trying Druid distribution source: canonical"* ]] \
  && [[ "$fallback_output" == *"Trying Druid distribution source: archive"* ]] \
  && [[ "$fallback_output" == *"source archive failed canonical SHA-512 verification"* ]] \
  && [[ "$fallback_output" == *"Trying Druid distribution source: closer"* ]] \
  && [[ "$fallback_output" == *"Verified Druid distribution from source: closer"* ]] \
  || { echo "FAIL fallback progression or corruption rejection was not logged" >&2; echo "$fallback_output" >&2; exit 1; }
expected_requests=$'/canonical/fallback.bin.sha512\n/canonical/fallback.bin\n/archive/fallback.bin\n/mirror/fallback.bin'
[[ "$(cat "$WORK/requests")" == "$expected_requests" ]] \
  || { echo "FAIL fallback request order differed" >&2; cat "$WORK/requests" >&2; exit 1; }
[[ "$(cat "$DRUID_DIST_DIR/$ARCHIVE")" == "verified-payload" ]] \
  || { echo "FAIL fallback retained an unverified payload" >&2; exit 1; }

echo "druid-download-retry: passed (attempts=2 resumed=yes final-failure-clean=yes fallback=canonical/archive/closer corrupt-rejected=yes elapsed=${elapsed_seconds}s)"
