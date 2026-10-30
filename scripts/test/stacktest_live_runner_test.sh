#!/usr/bin/env bash
# Runtime regression for the shared StackTestMain runner. This exercises run_green under nounset
# without Maven or Docker by replacing only the command that produces the live proof markers.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TEST_TEMP="$(mktemp -d "${TMPDIR:-/tmp}/stacktest-live-runner.XXXXXX")"
trap 'rm -rf "$TEST_TEMP"' EXIT

RUNNER_TEMP="$TEST_TEMP"
source "$ROOT/.github/scripts/stacktest-live-runner.sh"

stacktest_command() {
  printf '%s\n' \
    "Server ready" \
    "Druid record found" \
    "Vitess check - DONE" \
    "Testing done"
  sleep 30
}

run_green nounset-regression
test -f "$TEST_TEMP/stacktest-live/nounset-regression.log"
grep -q -- "Server ready" "$TEST_TEMP/stacktest-live/nounset-regression.log"

echo "stacktest-live-runner-test: PASS run_green executes under set -u"
