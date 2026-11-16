#!/usr/bin/env bash
# Shared live StackTestMain launcher used by the internal work item 1137 F1 campaign and the internal work item 1138 close gate.
set -euo pipefail

STACKTEST_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACKTEST_LOG_ROOT="${RUNNER_TEMP:-/tmp}/stacktest-live"
mkdir -p "$STACKTEST_LOG_ROOT"

require_stacktest_java_runtime() {
  local java_properties java_spec_values java_spec_version value_count
  if ! java_properties="$(java -XshowSettings:properties -version 2>&1)"; then
    echo "::error::StackTestMain requires java.specification.version=21; Java runtime inspection failed" >&2
    return 1
  fi

  java_spec_values="$(awk 'NF == 3 && $1 == "java.specification.version" && $2 == "=" { print $3 }' <<<"$java_properties")"
  value_count="$(awk 'NF { count++ } END { print count + 0 }' <<<"$java_spec_values")"
  java_spec_version="$(awk 'NF { print; exit }' <<<"$java_spec_values")"
  if [[ "$value_count" != 1 || "$java_spec_version" != 21 ]]; then
    echo "::error::StackTestMain requires java.specification.version=21; found '${java_spec_values:-missing}'" >&2
    return 1
  fi
}

stacktest_command() {
  require_stacktest_java_runtime || return 1
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
  local -a release_args=()
  if [[ -n "${STACKTEST_JAVA_RELEASE:-}" ]]; then
    release_args+=("-Djdk.version=${STACKTEST_JAVA_RELEASE}")
  fi
  (cd "$STACKTEST_ROOT" && timeout --signal=TERM --kill-after=30s 30m mvn -q -B -ntp \
    -pl appbase-stacktest -am install -DskipTests -Dcheckstyle.skip=true -Dexec.skip=true \
    "${release_args[@]}")
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  case "${1:-}" in
    install) install_stacktest ;;
    green) run_green "${2:?green label required}" ;;
    *) echo "usage: $0 <install|green LABEL>" >&2; exit 2 ;;
  esac
fi
