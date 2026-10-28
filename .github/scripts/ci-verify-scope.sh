#!/usr/bin/env bash
# Decide whether hosted verification tiers are in the owner's opt-in scope.
# Usage: ci-verify-scope.sh EVENT BASE_SHA HEAD_SHA SECURITY_SENSITIVE DISPATCH_REQUESTED
set -euo pipefail

[ "$#" -eq 5 ] || { echo "usage: $0 EVENT BASE_SHA HEAD_SHA SECURITY_SENSITIVE DISPATCH_REQUESTED" >&2; exit 2; }
event=$1
base=$2
head=$3
security_sensitive=$4
dispatch_requested=$5

case "$security_sensitive:$dispatch_requested" in
  true:true|true:false|false:true|false:false) ;;
  *) echo "invalid boolean input" >&2; exit 2 ;;
esac

if [ "$event" = workflow_dispatch ]; then
  printf '%s\n' "$dispatch_requested"
  exit 0
fi
if [ "$event" != pull_request ]; then
  printf '%s\n' false
  exit 0
fi
if [ "$security_sensitive" = true ]; then
  printf '%s\n' true
  exit 0
fi

git rev-parse --verify "$base^{commit}" >/dev/null 2>&1 || exit 2
git rev-parse --verify "$head^{commit}" >/dev/null 2>&1 || exit 2
if git diff --name-only -z "$base...$head" -- \
  .github build.sh scripts/run-suites.sh 'scripts/test/ci_*' \
  | tr '\0' '\n' | grep -q .; then
  printf '%s\n' true
else
  printf '%s\n' false
fi
