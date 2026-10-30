#!/usr/bin/env bash
# Pin the hosted compile job's ripgrep prerequisite before build.sh's bounded scan.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKFLOW="$ROOT/.github/workflows/ci-verify.yml"

has_compile_ripgrep() {
  local section install_line compile_line
  section="$(awk '
    /^  maven-compile:$/ { inside = 1; next }
    inside && /^  [a-zA-Z0-9_-]+:$/ { exit }
    inside { print }
  ' "$1")"
  install_line="$(grep -nF 'run: sudo apt-get update && sudo apt-get install -y ripgrep && rg --version' \
    <<< "$section" | cut -d: -f1)"
  compile_line="$(grep -nF 'run: ./build.sh --mode compile' <<< "$section" | cut -d: -f1)"
  [[ -n "$install_line" && -n "$compile_line" && "$install_line" -lt "$compile_line" ]]
}

has_offline_full_history() {
  local section checkout_step
  section="$(awk '
    /^  offline-shell-suites:$/ { inside = 1; next }
    inside && /^  [a-zA-Z0-9_-]+:$/ { exit }
    inside { print }
  ' "$1")"
  checkout_step="$(awk '
    $0 == "      - name: Check out the exact head (§5.2)" { capture = 1 }
    capture && seen && /^      - name:/ { exit }
    capture { print; seen = 1 }
  ' <<< "$section")"
  [[ "$(grep -Fxc '          fetch-depth: 0' <<< "$checkout_step")" -eq 1 ]]
}

if ! has_compile_ripgrep "$WORKFLOW"; then
  echo 'FAIL maven-compile must install and verify ripgrep before build.sh' >&2
  exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf -- "$TMP"' EXIT
sed 's/install -y ripgrep/install -y vim/' "$WORKFLOW" > "$TMP/missing-ripgrep.yml"
if has_compile_ripgrep "$TMP/missing-ripgrep.yml"; then
  echo 'FAIL missing-ripgrep mutation was accepted' >&2
  exit 1
fi

if ! has_offline_full_history "$WORKFLOW"; then
  echo 'FAIL offline-shell-suites must fetch full history for pinned receipt comparisons' >&2
  exit 1
fi

sed '/^  offline-shell-suites:$/,/^  [a-zA-Z0-9_-]*:$/ s/^          fetch-depth: 0$/          fetch-depth: 1/' \
  "$WORKFLOW" > "$TMP/shallow-offline.yml"
if has_offline_full_history "$TMP/shallow-offline.yml"; then
  echo 'FAIL shallow offline-shell checkout mutation was accepted' >&2
  exit 1
fi

echo 'ci-verify-compile-prereqs-test: 4 passed, 0 failed'
