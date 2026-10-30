#!/usr/bin/env bash
# Runs the service-Javadoc check through the repository's normal *_test.sh discovery path.
#
# @author Navaneeth Mysore G
# @author Claude
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PASS=0
FAIL=0

run_check() {
  local name="$1"
  shift
  if output="$(cd "$ROOT" && python3 scripts/check-service-javadocs.py "$@" 2>&1)"; then
    printf '  ok   %s\n' "$name"
    PASS=$((PASS + 1))
  else
    printf '  FAIL %s\n%s\n' "$name" "$output"
    FAIL=$((FAIL + 1))
  fi
}

run_check "fixture rejects declaration, tag, placement, scope, and repetition defects" --fixture
run_check "inventory-selected service sources satisfy the contract"

printf 'service_javadocs_test: %d passed, %d failed\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
