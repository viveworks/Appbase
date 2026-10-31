#!/usr/bin/env bash
# Fast, blocking API-inventory checks shared by local and CI callers.
#
# @author Navaneeth Mysore G
# @author Claude
#
set -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)" || exit 1
CONSUMER="README.md"
CONSUMER_COMMAND="python3 scripts/api-inventory.py check-consumer README.md"

cd "$ROOT" || exit 1

if [ ! -f "$CONSUMER" ] || [ ! -s "$CONSUMER" ] || [ ! -r "$CONSUMER" ]; then
  echo "api-inventory-fast-check: cannot run '$CONSUMER_COMMAND': consumer path must be a readable, non-empty file: $CONSUMER" >&2
  exit 2
fi

if python3 scripts/api-inventory.py check; then
  :
else
  rc=$?
  echo "api-inventory-fast-check: command failed (exit $rc): python3 scripts/api-inventory.py check" >&2
  exit "$rc"
fi

if python3 scripts/api-inventory.py check-consumer README.md; then
  :
else
  rc=$?
  echo "api-inventory-fast-check: command failed (exit $rc): $CONSUMER_COMMAND" >&2
  exit "$rc"
fi
