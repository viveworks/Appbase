#!/usr/bin/env bash
# Recurring dependency-change gate for P05-E02 N07 (internal work item 1386).
set -uo pipefail

ROOT="${LICENCE_CHANGE_GATE_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
BASE=""
HEAD="HEAD"
BODY_FILE=""
ALL=""
DIST=""
REPORT="docs/spec/1.0.0/P05/decisions/E02-dependency-licences.md"

die() { echo "licence-change-gate: $2" >&2; exit "$1"; }
need() { [ "$#" -ge 2 ] && [ -n "$2" ] || die 2 "$1 needs a value"; }

while [ "$#" -gt 0 ]; do
  case "$1" in
    --base) need "$@"; BASE="$2"; shift 2 ;;
    --head) need "$@"; HEAD="$2"; shift 2 ;;
    --body-file) need "$@"; BODY_FILE="$2"; shift 2 ;;
    --all) need "$@"; ALL="$2"; shift 2 ;;
    --distributed) need "$@"; DIST="$2"; shift 2 ;;
    *) die 2 "unknown argument '$1'" ;;
  esac
done

[ -n "$BASE" ] || die 2 "--base is required"
[ -n "$BODY_FILE" ] && [ -r "$BODY_FILE" ] || die 2 "--body-file must name a readable PR-body file"
git -C "$ROOT" rev-parse --verify "$BASE^{commit}" >/dev/null 2>&1 || die 2 "base is not a commit: $BASE"
git -C "$ROOT" rev-parse --verify "$HEAD^{commit}" >/dev/null 2>&1 || die 2 "head is not a commit: $HEAD"

changed_poms="$(git -C "$ROOT" diff --name-only "$BASE" "$HEAD" -- '**/pom.xml' 'pom.xml')"
if [ -z "$changed_poms" ]; then
  echo "licence-change-gate: SKIP — no pom.xml changed"
  exit 0
fi

# A dependency-affecting PR must state what changed and the licence evidence. The generated report
# below is also compared with the base revision so EVERY actually added coordinate (direct or
# transitive) can be reconciled against this block. `None`/`N/A` deliberately do not satisfy it.
body_block="$(awk '
  BEGIN { IGNORECASE=1 }
  /^Licence impact:[[:space:]]*/ { active=1; sub(/^[^:]*:[[:space:]]*/, ""); print; next }
  active && /^#{1,6}[[:space:]]/ { exit }
  active { print }
' "$BODY_FILE")"
[ -n "$(printf '%s' "$body_block" | tr -d '[:space:]')" ] || die 6 "PR body needs a non-empty 'Licence impact:' section"
printf '%s\n' "$body_block" | grep -Eqi '(Apache-2\.0|MIT|BSD-[23]-Clause|EPL-[12]\.0|MPL-2\.0|CDDL-[01]\.0|LGPL-[0-9]|GPL-[0-9]|AGPL-[0-9]|NOASSERTION)' \
  || die 6 "Licence impact must name an SPDX identifier or NOASSERTION"

[ -n "$ALL" ] && [ -r "$ALL" ] || die 2 "--all must name the full generated THIRD-PARTY file"
[ -n "$DIST" ] && [ -r "$DIST" ] || die 2 "--distributed must name the generated compile/runtime THIRD-PARTY file"

work="$(mktemp -d "${TMPDIR:-/tmp}/licence-change-gate.XXXXXX")" || die 3 "cannot allocate workspace"
trap 'rm -rf "$work"' EXIT
cp "$ROOT/$REPORT" "$work/report.md" || die 3 "cannot stage the committed report"
"$ROOT/scripts/license/licence-report.sh" --report --all "$ALL" --distributed "$DIST" \
  --map "$ROOT/scripts/license/licence-spdx-map.tsv" --in-place "$work/report.md" >/dev/null \
  || die 4 "dependency metadata could not be classified"

if grep -Eq '\| compile/runtime \| `INCOMPATIBLE` \|' "$work/report.md"; then
  die 4 "an Apache-2.0-incompatible dependency is present on a compile/runtime path"
fi

if git -C "$ROOT" show "$BASE:$REPORT" > "$work/base-report.md" 2>/dev/null; then
  awk -F'`' '/^\| `[^`]+:[^`]+:[^`]+` \|/ { print $2 }' "$work/base-report.md" | LC_ALL=C sort -u > "$work/base.gavs"
  awk -F'`' '/^\| `[^`]+:[^`]+:[^`]+` \|/ { print $2 }' "$work/report.md" | LC_ALL=C sort -u > "$work/head.gavs"
  comm -13 "$work/base.gavs" "$work/head.gavs" > "$work/added.gavs"

  if [ -s "$work/added.gavs" ]; then
    missing=""
    while IFS= read -r gav; do
      printf '%s\n' "$body_block" | grep -Fq -- "$gav" || missing="${missing}${gav}\n"
    done < "$work/added.gavs"
    [ -z "$missing" ] || die 6 "Licence impact omits added/changed direct or transitive coordinates:
$(printf '%b' "$missing")"
  else
    printf '%s\n' "$body_block" | grep -Eqi 'no dependency coordinate changes|[[:alnum:]_.-]+:[[:alnum:]_.-]+:[[:alnum:]_.+-]+' \
      || die 6 "Licence impact must name changed coordinates or state 'No dependency coordinate changes'"
  fi
else
  printf '%s\n' "$body_block" | grep -Fxiq 'Initial dependency licence baseline' \
    || die 6 "Licence impact must state 'Initial dependency licence baseline' when the base has no report"
fi

if ! cmp -s "$ROOT/$REPORT" "$work/report.md"; then
  diff -u "$ROOT/$REPORT" "$work/report.md" >&2 || true
  die 5 "dependency report drifted; commit the regenerated report and record every changed licence"
fi

echo "licence-change-gate: PASS — changed POMs, dependency universe, verdict report and PR declaration agree"
