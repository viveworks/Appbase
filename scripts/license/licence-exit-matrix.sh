#!/usr/bin/env bash
# Validate and run the P05-E02 epic-close licence/header/NOTICE matrix (internal work item 2539).
# Bash 3.2 compatible; the tracked catalogue is intentionally PENDING until its leaf owners land.
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="${LICENCE_EXIT_MATRIX_ROOT:-$(cd "$HERE/.." && pwd)}"
MATRIX="$HERE/licence-exit-matrix.tsv"
MODE=""
die() { echo "licence-exit-matrix: $2" >&2; exit "$1"; }
runner_has_symlink() {
  runner_path="$ROOT"
  runner_remainder="$1"
  while [ -n "$runner_remainder" ]; do
    runner_part="${runner_remainder%%/*}"
    runner_path="$runner_path/$runner_part"
    [ -L "$runner_path" ] && return 0
    case "$runner_remainder" in
      */*) runner_remainder="${runner_remainder#*/}" ;;
      *) runner_remainder="" ;;
    esac
  done
  return 1
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --matrix) [ "$#" -ge 2 ] && [ -n "$2" ] || die 2 "--matrix needs a file"; MATRIX="$2"; shift 2 ;;
    --check|--list|--run) [ -z "$MODE" ] || die 2 "choose exactly one mode"; MODE="$1"; shift ;;
    *) die 2 "unknown argument '$1'" ;;
  esac
done
[ -n "$MODE" ] || die 2 "usage: $0 [--matrix file] --check|--list|--run"
[ -f "$MATRIX" ] && [ -r "$MATRIX" ] || die 2 "cannot read matrix '$MATRIX'"

WORK="$(mktemp -d "${TMPDIR:-/tmp}/licence-exit-matrix.XXXXXX")" || die 2 "cannot allocate workspace"
trap 'rm -rf "$WORK"' EXIT
: > "$WORK/seen"
: > "$WORK/rows"
count=0
pending=0

while IFS="$(printf '\t')" read -r id status runner description extra; do
  case "$id" in ''|'#'*) continue ;; esac
  [ -z "${extra:-}" ] || die 2 "$id has more than four tab-separated fields"
  printf '%s\n' "$id" | grep -Eq '^LI-(0[1-9]|1[01])$' || die 2 "invalid scenario id '$id'"
  if grep -qxF -- "$id" "$WORK/seen"; then die 2 "duplicate scenario '$id'"; fi
  printf '%s\n' "$id" >> "$WORK/seen"
  case "$status" in
    READY)
      case "$runner" in scripts/*) ;; *) die 2 "$id READY runner must be under scripts/" ;; esac
      case "/$runner/" in */../*) die 2 "$id READY runner escapes the scripts tree" ;; esac
      runner_has_symlink "$runner" && die 2 "$id READY runner path contains a symlink"
      [ -f "$ROOT/$runner" ] || die 2 "$id runner is missing: $runner"
      ;;
    PENDING)
      [ "$runner" = "-" ] || die 2 "$id PENDING runner must be '-'"
      pending=$((pending + 1))
      ;;
    *) die 2 "$id has invalid status '$status'" ;;
  esac
  [ -n "$description" ] || die 2 "$id needs a description"
  printf '%s\t%s\t%s\t%s\n' "$id" "$status" "$runner" "$description" >> "$WORK/rows"
  count=$((count + 1))
done < "$MATRIX"

[ "$count" -eq 11 ] || die 2 "expected 11 scenarios, found $count"
i=1
while [ "$i" -le 11 ]; do
  id="$(printf 'LI-%02d' "$i")"
  grep -qxF -- "$id" "$WORK/seen" || die 2 "missing scenario '$id'"
  i=$((i + 1))
done

if [ "$MODE" = "--list" ]; then
  cat "$WORK/rows"
  exit 0
fi
if [ "$MODE" = "--check" ]; then
  echo "licence-exit-matrix: catalogue valid — 11 scenarios ($pending PENDING)"
  exit 0
fi
if [ "$pending" -ne 0 ]; then
  echo "licence-exit-matrix: REFUSED — $pending scenario(s) are PENDING; the epic-close matrix has not run" >&2
  awk -F '\t' '$2 == "PENDING" { print "  " $1 " PENDING — " $4 }' "$WORK/rows" >&2
  exit 3
fi

while IFS="$(printf '\t')" read -r id status runner description; do
  echo "licence-exit-matrix: RUN $id — $description"
  /bin/bash "$ROOT/$runner" "$id"
  rc=$?
  if [ "$rc" -ne 0 ]; then
    echo "licence-exit-matrix: $id failed (exit $rc)" >&2
    exit "$rc"
  fi
done < "$WORK/rows"
echo "licence-exit-matrix: PASS — all 11 scenarios completed"
