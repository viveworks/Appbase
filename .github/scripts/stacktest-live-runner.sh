#!/usr/bin/env bash
# Shared live StackTestMain launcher used by the internal work item 1137 F1 campaign and the internal work item 1138 close gate.
set -euo pipefail

STACKTEST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACKTEST_LOG_ROOT="${RUNNER_TEMP:-/tmp}/stacktest-live"
mkdir -p "$STACKTEST_LOG_ROOT"

stacktest_command() {
  mvn -q -B -ntp -pl appbase-stacktest \
    org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
    -Dexec.mainClass=com.viveworks.appbase.stacktest.StackTestMain \
    -Dexec.classpathScope=test
}

wait_for_log() {
  local pid="$1" log_file="$2" pattern="$3" timeout_s="$4" start_s
  start_s=$(date +%s)
  while ! grep -q -- "$pattern" "$log_file" 2>/dev/null; do
    if ! kill -0 "$pid" 2>/dev/null; then
      wait "$pid" || true
      echo "::error::StackTestMain exited before '$pattern'; log follows" >&2
      tail -n 160 "$log_file" >&2 || true
      return 1
    fi
    if (( $(date +%s) - start_s >= timeout_s )); then
      echo "::error::StackTestMain did not emit '$pattern' within ${timeout_s}s" >&2
      tail -n 160 "$log_file" >&2 || true
      return 1
    fi
    sleep 2
  done
}

stop_stacktest_process() {
  local pid="$1"
  kill "$pid" 2>/dev/null || true
  for _ in $(seq 1 15); do
    kill -0 "$pid" 2>/dev/null || { wait "$pid" 2>/dev/null || true; return 0; }
    sleep 1
  done
  kill -9 "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
}

run_green() {
  local label="$1"
  local pid
  local log_file="$STACKTEST_LOG_ROOT/${label}.log"
  (cd "$STACKTEST_ROOT" && stacktest_command) >"$log_file" 2>&1 &
  pid=$!
  trap 'stop_stacktest_process "$pid"' RETURN
  wait_for_log "$pid" "$log_file" "Server ready" 1200
  for proof in "Druid record found" "Vitess check - DONE" "Testing done"; do
    if ! grep -q -- "$proof" "$log_file"; then
      echo "::error::$label lacked required live proof '$proof'" >&2
      tail -n 160 "$log_file" >&2 || true
      return 1
    fi
  done
  stop_stacktest_process "$pid"
  trap - RETURN
  echo "$label: PASS (Druid query, Vitess configure/query, complete stack arm set)"
}

install_stacktest() {
  (cd "$STACKTEST_ROOT" && timeout --signal=TERM --kill-after=30s 30m mvn -q -B -ntp \
    -pl appbase-stacktest -am install -DskipTests -Dcheckstyle.skip=true -Dexec.skip=true)
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  case "${1:-}" in
    install) install_stacktest ;;
    green) run_green "${2:?green label required}" ;;
    *) echo "usage: $0 <install|green LABEL>" >&2; exit 2 ;;
  esac
fi
