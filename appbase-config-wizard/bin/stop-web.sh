#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$SCRIPT_DIR")"
PID_FILE="$MODULE_DIR/.config-wizard-web.pid"

if [ ! -f "$PID_FILE" ]; then
  echo "No PID file found — is Appbase Config Wizard running?"
  exit 1
fi

PID=$(cat "$PID_FILE")
if kill -0 "$PID" 2>/dev/null; then
  kill "$PID"
  rm -f "$PID_FILE"
  echo "Appbase Config Wizard stopped (PID $PID)"
else
  echo "Process $PID not running — cleaning up PID file"
  rm -f "$PID_FILE"
fi
