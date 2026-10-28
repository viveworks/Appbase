#!/usr/bin/env bash
# Proves the Druid archive retry loop has a real wall-clock bound even when a server trickles data.
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

python3 - "$WORK/port" <<'PY' >/dev/null 2>&1 &
import http.server
import pathlib
import sys
import time

port_file = pathlib.Path(sys.argv[1])

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Length", "100")
        self.end_headers()
        for _ in range(100):
            self.wfile.write(b"x")
            self.wfile.flush()
            time.sleep(0.25)

    def log_message(self, *_):
        pass

server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
port_file.write_text(str(server.server_port))
server.serve_forever()
PY
SERVER_PID=$!

for _ in 1 2 3 4 5; do
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

[[ "$download_exit" -ne 0 ]] || { echo "FAIL trickle download unexpectedly succeeded" >&2; exit 1; }
[[ "$output" == *"attempt 1/2, timeout 1s"* ]] \
  && [[ "$output" == *"attempt 2/2, timeout 2s"* ]] \
  || { echo "FAIL retry progression was not logged" >&2; echo "$output" >&2; exit 1; }
[[ "$(grep -c 'failed or timed out (exit=124)' <<<"$output")" -eq 2 ]] \
  || { echo "FAIL both trickle attempts were not wall-clock terminated" >&2; echo "$output" >&2; exit 1; }
(( elapsed_seconds >= 3 && elapsed_seconds <= 6 )) \
  || { echo "FAIL retry series took ${elapsed_seconds}s; expected bounded 3-6s" >&2; exit 1; }
[[ ! -e "$DRUID_DIST_DIR/trickle.bin.part" ]] \
  || { echo "FAIL partial download survived failed attempts" >&2; exit 1; }

echo "druid-download-retry: passed (attempts=2 elapsed=${elapsed_seconds}s)"
