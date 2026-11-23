#!/usr/bin/env bash
# start-web.sh — Start appbase-config-wizard in web mode on an ephemeral port.
#
# Writes:
#   .config-wizard-test.port  — the port the server is listening on
#   .config-wizard-test.pid   — the PID of the background server process
#
# Usage (from the module root):
#   ./test/start-web.sh [JAR_PATH]
#
# JAR_PATH is how Maven invokes this (${project.build.directory}/${project.build.finalName}.jar,
# correct for any project version). Without it -- a manual, non-Maven run -- this falls back to the
# largest appbase-config-wizard-*.jar under target/, excluding sources/shaded/javadoc; if none
# exists, it fails with a clear error rather than guessing a version string.
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
MODULE_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"

DEFAULT_JAR="$(ls -S "${MODULE_DIR}"/target/appbase-config-wizard-*.jar 2>/dev/null | grep -v -- '-sources\.jar$\|-shaded\.jar$\|-javadoc\.jar$' | head -1 || true)"
JAR="${1:-${DEFAULT_JAR}}"
PORT_FILE="${MODULE_DIR}/.config-wizard-test.port"
PID_FILE="${MODULE_DIR}/.config-wizard-test.pid"
LOG_FILE="${MODULE_DIR}/.config-wizard-test.log"

if [ -n "${1:-}" ] && [ ! -f "${JAR}" ]; then
  echo "ERROR: JAR not found at the given path: ${JAR}" >&2
  exit 1
fi
if [ -z "${JAR}" ]; then
  echo "ERROR: no appbase-config-wizard-*.jar found under ${MODULE_DIR}/target/." >&2
  echo "       Build it first (mvn package), or pass the exact path: ./test/start-web.sh JAR_PATH" >&2
  exit 1
fi

# Remove stale port/pid files
rm -f "${PORT_FILE}" "${PID_FILE}" "${LOG_FILE}"

echo "Starting appbase-config-wizard web server (port=0 → OS picks free port)..."

java \
  --enable-native-access=ALL-UNNAMED \
  --sun-misc-unsafe-memory-access=allow \
  -jar "${JAR}" \
  web \
  --port 0 \
  > "${LOG_FILE}" 2>&1 &

SERVER_PID=$!
echo "${SERVER_PID}" > "${PID_FILE}"
echo "Server PID: ${SERVER_PID}  (log: ${LOG_FILE})"

# Poll for "Open: http://localhost:<PORT>" in the log (up to 60 s)
MAX_WAIT=60
ELAPSED=0
PORT=""

while [ "${ELAPSED}" -lt "${MAX_WAIT}" ]; do
  if ! kill -0 "${SERVER_PID}" 2>/dev/null; then
    echo "ERROR: Server process ${SERVER_PID} died. Log:" >&2
    cat "${LOG_FILE}" >&2
    exit 1
  fi

  if [ -f "${LOG_FILE}" ]; then
    # The server prints a line like: "Open: http://localhost:PORT"
    PORT_LINE="$(grep -E 'Open: http://localhost:[0-9]+' "${LOG_FILE}" | head -1 || true)"
    if [ -n "${PORT_LINE}" ]; then
      PORT="$(echo "${PORT_LINE}" | grep -oE ':[0-9]+' | head -1 | tr -d ':')"
      break
    fi
  fi

  sleep 1
  ELAPSED=$(( ELAPSED + 1 ))
done

if [ -z "${PORT}" ]; then
  echo "ERROR: Server did not print 'Open: http://localhost:<PORT>' within ${MAX_WAIT} s." >&2
  echo "Last 20 lines of log:" >&2
  tail -20 "${LOG_FILE}" >&2
  kill "${SERVER_PID}" 2>/dev/null || true
  exit 1
fi

echo "${PORT}" > "${PORT_FILE}"
echo "Server is ready on port ${PORT} (PID ${SERVER_PID})"
echo "  CONFIG_WIZARD_URL=http://localhost:${PORT}"
