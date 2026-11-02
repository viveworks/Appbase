#!/usr/bin/env bash
# Compute the public nightly's fail-closed 24-hour develop scope.
set -euo pipefail

output="${1:?usage: nightly-scope.sh <github-output> <head> <cutoff-epoch>}"
head="${2:?usage: nightly-scope.sh <github-output> <head> <cutoff-epoch>}"
cutoff="${3:?usage: nightly-scope.sh <github-output> <head> <cutoff-epoch>}"
[[ "$cutoff" =~ ^[0-9]+$ ]] || { echo "nightly-scope: cutoff must be an epoch" >&2; exit 2; }
git cat-file -e "$head^{commit}" 2>/dev/null \
  || { echo "nightly-scope: head is not a commit: $head" >&2; exit 2; }

# A nightly window is about commits that LANDED on develop, not every side-branch commit reachable
# from it. First-parent history gives the integration timeline and makes the oldest landing's first
# parent the deterministic diff base even when that landing is a multi-parent merge.
recent="$(git rev-list --first-parent --since="@$cutoff" --reverse "$head")"
if [[ -z "$recent" ]]; then
  {
    echo "has_changes=false"
    echo "base_sha=$(git rev-parse "$head")"
    echo "head_sha=$(git rev-parse "$head")"
    echo "modules=NONE"
    printf '%s\n' run_shell_suites=false run_console_suite=false run_emulator=false \
      run_stacktest=false run_hosted_tiers=false run_unit_tests=false
  } >> "$output"
  echo "nightly-scope: no commits since @$cutoff; no product jobs selected" >&2
  exit 0
fi

oldest="$(printf '%s\n' "$recent" | sed -n '1p')"
recent_count="$(printf '%s\n' "$recent" | wc -l | tr -d ' ')"
if base="$(git rev-parse "$oldest^1" 2>/dev/null)"; then
  selectors="$(mktemp "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/nightly-selectors.XXXXXX")"
  trap 'rm -f "$selectors"' EXIT
  modules="$(.github/scripts/changed-modules.sh --github-output "$selectors" "$base" "$head")"
  cat "$selectors" >> "$output"
else
  # A repository whose root commit is inside the window has no diff base. Fail toward the full
  # product scope rather than misreporting the root tree as no change.
  base="$oldest"
  modules=ALL
  printf '%s\n' run_shell_suites=true run_console_suite=true run_emulator=true \
    run_stacktest=true >> "$output"
fi

{
  echo "has_changes=true"
  echo "base_sha=$base"
  echo "head_sha=$(git rev-parse "$head")"
  echo "modules=$modules"
  if [[ "$modules" == NONE ]]; then
    printf '%s\n' run_hosted_tiers=false run_unit_tests=false
  else
    printf '%s\n' run_hosted_tiers=true run_unit_tests=true
  fi
} >> "$output"
echo "nightly-scope: $recent_count commit(s), $base..$(git rev-parse "$head"), modules=$modules" >&2
