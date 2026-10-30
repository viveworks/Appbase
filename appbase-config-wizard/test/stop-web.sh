#!/usr/bin/env bash
# stop-web.sh — Stop the background appbase-config-wizard web server.
#
# Reads .config-wizard-test.pid to find the PID and sends SIGTERM, then removes
# the .config-wizard-test.port and .config-wizard-test.pid files.
#
# Usage (from the module root):
#   ./test/stop-web.sh
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
MODULE_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"

PID_FILE="${MODULE_DIR}/.config-wizard-test.pid"
PORT_FILE="${MODULE_DIR}/.config-wizard-test.port"

if [ ! -f "${PID_FILE}" ]; then
  echo "No .config-wizard-test.pid found — server may not be running." >&2
  exit 0
fi

SERVER_PID="$(cat "${PID_FILE}")"

if kill -0 "${SERVER_PID}" 2>/dev/null; then
  echo "Stopping Appbase Config Wizard server (PID ${SERVER_PID})..."
  kill "${SERVER_PID}" 2>/dev/null || true
  # Give it up to 5 s to terminate gracefully
  for _ in 1 2 3 4 5; do
    if ! kill -0 "${SERVER_PID}" 2>/dev/null; then
      break
    fi
    sleep 1
  done
  # Force kill if still alive
  if kill -0 "${SERVER_PID}" 2>/dev/null; then
    echo "Server did not stop gracefully; sending SIGKILL..."
    kill -9 "${SERVER_PID}" 2>/dev/null || true
  fi
  echo "Server stopped."
else
  echo "Server process ${SERVER_PID} was already gone."
fi

rm -f "${PID_FILE}" "${PORT_FILE}"
