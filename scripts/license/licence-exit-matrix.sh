#!/usr/bin/env bash
# Validate and run the P05-E02 epic-close licence/header/NOTICE matrix (internal work item 2539).
# Bash 3.2 compatible; the tracked catalogue is intentionally PENDING until its leaf owners land.
#
# --run --receipt <path> additionally runs EVERY scenario (a failure no longer stops the run) and
# writes a JSON receipt for the licensing.exact-candidate release slot (internal work item 1139; owner ruling on internal work item 1387,
# 2026-11-08): the checkout's HEAD revision and tree, whether the working tree was clean before and
# after the run, the sha256 of the matrix catalogue that ran, and each scenario's status and exit
# code. HEAD moving during the run refuses the receipt. Every --receipt run first removes a receipt
# already at that path, so a refused run never leaves a stale one in its place. Write it outside the
# checkout, then commit it as a follow-up to the revision it names. Without --receipt behaviour and
# output are unchanged.
#
# @author Navaneeth Mysore G
# @author Claude
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="${LICENCE_EXIT_MATRIX_ROOT:-$(cd "$HERE/.." && pwd)}"
MATRIX="$HERE/licence-exit-matrix.tsv"
MODE=""
RECEIPT=""
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
    --receipt) [ "$#" -ge 2 ] && [ -n "$2" ] || die 2 "--receipt needs a file"; RECEIPT="$2"; shift 2 ;;
    --check|--list|--run) [ -z "$MODE" ] || die 2 "choose exactly one mode"; MODE="$1"; shift ;;
    *) die 2 "unknown argument '$1'" ;;
  esac
done
[ -n "$MODE" ] || die 2 "usage: $0 [--matrix file] --check|--list|--run [--receipt file]"
[ -z "$RECEIPT" ] || [ "$MODE" = "--run" ] || die 2 "--receipt requires --run"
[ -f "$MATRIX" ] && [ -r "$MATRIX" ] || die 2 "cannot read matrix '$MATRIX'"
# A refused run must not leave an older receipt where the caller expects this run's.
if [ -n "$RECEIPT" ]; then
  rm -f -- "$RECEIPT" 2>/dev/null && [ ! -e "$RECEIPT" ] && [ ! -L "$RECEIPT" ] \
    || die 2 "cannot remove the existing receipt $RECEIPT"
fi

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

if [ -n "$RECEIPT" ]; then
  receipt_dir="$(dirname "$RECEIPT")"
  [ -d "$receipt_dir" ] && [ -w "$receipt_dir" ] || die 2 "receipt directory is not writable: $receipt_dir"
  sha256_of() {
    if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | awk '{print $1}'
    else shasum -a 256 "$1" | awk '{print $1}'; fi
  }
  worktree_state() { git -C "$ROOT" status --porcelain --untracked-files=all 2>/dev/null || echo unreadable; }
  toplevel="$(git -C "$ROOT" rev-parse --show-toplevel 2>/dev/null)" || toplevel=""
  [ -n "$toplevel" ] && [ "$(cd "$toplevel" && pwd -P)" = "$(cd "$ROOT" && pwd -P)" ] \
    || die 2 "receipt needs $ROOT to be the top of a git checkout"
  revision="$(git -C "$ROOT" rev-parse --verify -q 'HEAD^{commit}')" || die 2 "receipt needs a git checkout at $ROOT"
  tree="$(git -C "$ROOT" rev-parse --verify -q 'HEAD^{tree}')" || die 2 "cannot resolve the HEAD tree"
  matrix_sha="$(sha256_of "$MATRIX")" || die 2 "cannot hash the matrix"
  printf '%s\n' "$matrix_sha" | grep -Eq '^[0-9a-f]{64}$' || die 2 "cannot hash the matrix"
  before="$(worktree_state)"
  first_rc=0
  : > "$WORK/results"
  while IFS="$(printf '\t')" read -r id status runner description; do
    echo "licence-exit-matrix: RUN $id — $description"
    /bin/bash "$ROOT/$runner" "$id" < /dev/null
    rc=$?
    if [ "$rc" -ne 0 ]; then
      echo "licence-exit-matrix: $id failed (exit $rc)" >&2
      [ "$first_rc" -ne 0 ] || first_rc="$rc"
      printf '%s\tFAIL\t%s\n' "$id" "$rc" >> "$WORK/results"
    else
      printf '%s\tPASS\t0\n' "$id" >> "$WORK/results"
    fi
  done < "$WORK/rows"
  after="$(worktree_state)"
  [ "$(git -C "$ROOT" rev-parse --verify -q 'HEAD^{commit}')" = "$revision" ] \
    || die 2 "HEAD moved during the run; no receipt written"
  clean=false
  [ -z "$before" ] && [ -z "$after" ] && clean=true
  {
    printf '{\n  "schema_version": 1,\n  "producer": "scripts/license/licence-exit-matrix.sh",\n'
    printf '  "revision": "%s",\n  "tree": "%s",\n  "clean": %s,\n' "$revision" "$tree" "$clean"
    printf '  "matrix_sha256": "%s",\n  "scenarios": [\n' "$matrix_sha"
    awk -F '\t' '{ printf "%s    {\"id\": \"%s\", \"status\": \"%s\", \"exit_code\": %d}", (NR > 1 ? ",\n" : ""), $1, $2, $3 }
      END { print "" }' "$WORK/results"
    printf '  ]\n}\n'
  } > "$RECEIPT.tmp.$$" || { rm -f "$RECEIPT.tmp.$$"; die 2 "cannot write the receipt"; }
  mv -f "$RECEIPT.tmp.$$" "$RECEIPT" || { rm -f "$RECEIPT.tmp.$$"; die 2 "cannot write the receipt to $RECEIPT"; }
  echo "licence-exit-matrix: receipt written to $RECEIPT (clean=$clean)"
  if [ "$first_rc" -ne 0 ]; then
    echo "licence-exit-matrix: FAIL — at least one scenario failed; the receipt records it" >&2
    exit "$first_rc"
  fi
  echo "licence-exit-matrix: PASS — all 11 scenarios completed"
  exit 0
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
