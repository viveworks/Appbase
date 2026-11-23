#!/usr/bin/env bash
# Deterministic contract tests for the shared StackTestMain runner.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TEST_TEMP="$(mktemp -d "${TMPDIR:-/tmp}/stacktest-live-runner.XXXXXX")"
trap 'rm -rf "$TEST_TEMP"' EXIT

FAKE_BIN="$TEST_TEMP/bin"
MVN_CALLS="$TEST_TEMP/mvn-calls"
JAVA_CALLS="$TEST_TEMP/java-calls"
mkdir -p "$FAKE_BIN"

cat >"$FAKE_BIN/java" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >>"$JAVA_CALLS"
if [[ "${FAKE_JAVA_FAIL:-0}" == 1 ]]; then
  exit 1
fi
if [[ -n "${FAKE_JAVA_SPEC_VERSION:-}" ]]; then
  printf '    java.specification.version = %s\n' "$FAKE_JAVA_SPEC_VERSION" >&2
fi
if [[ "${FAKE_JAVA_DUPLICATE:-0}" == 1 ]]; then
  printf '    java.specification.version = %s\n' "$FAKE_JAVA_SPEC_VERSION" >&2
fi
if [[ "${FAKE_JAVA_FAIL_AFTER_OUTPUT:-0}" == 1 ]]; then
  exit 1
fi
EOF

cat >"$FAKE_BIN/mvn" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >>"$MVN_CALLS"
printf '%s\n' \
  "Server ready" \
  "Druid record found" \
  "Vitess check - DONE" \
  "Testing done"
if [[ "${FAKE_MVN_HOLD:-0}" == 1 ]]; then
  sleep 30
fi
EOF

cat >"$FAKE_BIN/timeout" <<'EOF'
#!/usr/bin/env bash
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --signal=*|--kill-after=*|[0-9]*[smh]) shift ;;
    *) break ;;
  esac
done
exec "$@"
EOF

chmod +x "$FAKE_BIN/java" "$FAKE_BIN/mvn" "$FAKE_BIN/timeout"
export PATH="$FAKE_BIN:$PATH"
export MVN_CALLS
export JAVA_CALLS
export RUNNER_TEMP="$TEST_TEMP"

source "${STACKTEST_RUNNER:-$ROOT/.github/scripts/stacktest-live-runner.sh}"

fail() {
  echo "stacktest-live-runner-test: FAIL $*" >&2
  exit 1
}

assert_runtime_rejected() {
  local version="$1" label="$2" output_file
  output_file="$TEST_TEMP/${label}.output"
  : >"$MVN_CALLS"
  if FAKE_JAVA_SPEC_VERSION="$version" stacktest_command >"$output_file" 2>&1; then
    fail "runtime version '$version' was accepted"
  fi
  grep -q -- "requires java.specification.version=21" "$output_file" ||
    fail "runtime version '$version' lacked the guard diagnostic"
  test ! -s "$MVN_CALLS" || fail "Maven ran for rejected runtime version '$version'"
}

test_AC3011_1_rejects_non_21_before_maven() {
  assert_runtime_rejected "25" jdk25
  assert_runtime_rejected "21.0" malformed
  assert_runtime_rejected "" empty
  assert_runtime_rejected "21 trailing" trailing-token
  FAKE_JAVA_FAIL_AFTER_OUTPUT=1 assert_runtime_rejected "21" nonzero-after-valid
  FAKE_JAVA_DUPLICATE=1 assert_runtime_rejected "21" duplicate-property
}

test_AC3011_2_accepts_21_at_shared_runtime_chokepoint() {
  : >"$MVN_CALLS"
  : >"$JAVA_CALLS"
  FAKE_JAVA_SPEC_VERSION=21 stacktest_command >"$TEST_TEMP/jdk21.output" 2>&1
  grep -q -- "StackTestMain" "$MVN_CALLS" || fail "JDK 21 did not reach StackTestMain Maven invocation"
  grep -q -- "-XshowSettings:properties -version" "$JAVA_CALLS" || fail "runtime guard did not inspect Java properties"
}

test_AC3011_3_preserves_install_route() {
  : >"$MVN_CALLS"
  : >"$JAVA_CALLS"
  FAKE_JAVA_SPEC_VERSION=25 STACKTEST_JAVA_RELEASE=21 install_stacktest >"$TEST_TEMP/install.output" 2>&1
  grep -q -- "appbase-stacktest -am install" "$MVN_CALLS" || fail "install route did not invoke Maven"
  grep -q -- "-Djdk.version=21" "$MVN_CALLS" || fail "install route lost Java release override"
  test ! -s "$JAVA_CALLS" || fail "install route incorrectly invoked the runtime guard"
}

# Preserve the original nounset regression while exercising the real guarded chokepoint.
test_EXTRA_run_green_under_nounset() {
  export FAKE_JAVA_SPEC_VERSION=21 FAKE_MVN_HOLD=1
  run_green nounset-regression
  unset FAKE_MVN_HOLD FAKE_JAVA_SPEC_VERSION
  test -f "$TEST_TEMP/stacktest-live/nounset-regression.log"
  grep -q -- "Server ready" "$TEST_TEMP/stacktest-live/nounset-regression.log"
}

test_AC3011_1_rejects_non_21_before_maven
test_AC3011_2_accepts_21_at_shared_runtime_chokepoint
test_AC3011_3_preserves_install_route
test_EXTRA_run_green_under_nounset

echo "stacktest-live-runner-test: PASS Java 21 runtime guard and install preservation"
