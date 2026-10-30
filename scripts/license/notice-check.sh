#!/usr/bin/env bash
#
# notice-check.sh — the NOTICE self-regeneration gate (internal work item 1384, epic internal work item 1139, P-025 clause 4).
#
# Apache-2.0 section 4(d) makes NOTICE an obligation, not a courtesy: it must carry the attribution
# of what we actually redistribute. So it has to be regenerated whenever the dependency set moves,
# and the only way that happens reliably is if something FAILS when it has not been.
#
# 🔴 THE FAILURE MODE THIS SCRIPT IS SHAPED AROUND. "Regenerate, then diff" has a hole: if the
# generation step produces nothing — an empty file, or a header with no dependency entries — then
# comparing nothing against nothing SUCCEEDS. The gate reports green having attributed nothing. That
# is the same shape as the security review that harvested the wrong tree and reported a clean pass,
# and as run-suites' VACUOUS state. So this script REFUSES on an unusable generated file instead of
# comparing it, and says how many entries it verified when it passes. **Cannot-verify is not verified.**
#
# 🔴 AND THE GENERATOR ITSELF CAN SILENTLY NOT REGENERATE. internal work item 1380 measured `license-maven-plugin`
# skipping regeneration when its output file already existed — exiting 0 with a stale file. The root
# pom pins <force>true</force> for exactly that reason, and because Maven's POM <configuration> takes
# precedence over a -D user property (measured: -Dlicense.excludedGroups is ignored while the pom's
# value still applies), that force CANNOT be switched off from the command line. Verified here by
# corrupting the output file and confirming regeneration restored it byte-for-byte.
#
# Usage:
#   notice-check.sh --generated <file> --committed <file>   # compare only (no Maven; what the tests drive)
#   notice-check.sh --generate [--committed <file>]         # generate separately, then compare
#   notice-check.sh --update [--committed <file>]           # explicitly generate and install; not verification
#
# Exit codes — each a DIFFERENT fact, because a gate that collapses them cannot be acted on:
#   0  in sync (check) / installed (update)         2  usage error
#   3  committed NOTICE missing/unreadable
#   4  DRIFT — regenerate and commit                5  generation/update failure
#   6  comparison could not be performed
# Entry counts prove a nonempty metadata list, not complete attribution or legal acceptance.
#
#
set -uo pipefail
HERE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$( cd "$HERE/.." && pwd )"
CALLER_DIR="$PWD"
ENTRY_MARKER='^  \* '
die() { echo "notice-check: $2" >&2; exit "$1"; }
entries_in() { grep -c -- "$ENTRY_MARKER" "$1" 2>/dev/null || true; }
operand() { [[ $# -ge 2 && -n "$2" && "$2" != --* ]] || die 2 "$1 needs a nonempty path"; }
# Absolute spelling before any cd; existing parent resolution collapses symlink/../ aliases.
absolute_path() {
  case "$1" in /*) printf '%s\n' "$1";; *) printf '%s/%s\n' "$CALLER_DIR" "$1";; esac
}
canonical_parent() {
  local parent
  parent="$(cd "$(dirname "$1")" && pwd -P)" || return 1
  printf '%s/%s\n' "$parent" "$(basename "$1")"
}
GENERATED=""; COMMITTED=""; DO_GENERATE=0; DO_UPDATE=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --generated) operand "$@"; GENERATED="$2"; shift 2;;
    --committed) operand "$@"; COMMITTED="$2"; shift 2;;
    --generate) DO_GENERATE=1; shift;;
    --update) DO_UPDATE=1; shift;;
    *) die 2 "unknown argument '$1'";;
  esac
done
if [[ $DO_UPDATE -eq 1 ]]; then
  [[ $DO_GENERATE -eq 0 && -z "$GENERATED" ]] || die 2 "--update cannot be combined with --generate or --generated"
  DO_GENERATE=1
fi
[[ -n "$COMMITTED" ]] || COMMITTED="$ROOT/NOTICE"
COMMITTED="$(absolute_path "$COMMITTED")"
# Comparison of missing inputs retains its dedicated status; update requires an existing parent.
committed_parent="$(dirname "$COMMITTED")"
if [[ -d "$committed_parent" ]]; then
  COMMITTED="$(canonical_parent "$COMMITTED")" || die 3 "cannot resolve committed NOTICE path"
elif [[ $DO_UPDATE -eq 1 ]]; then
  die 5 "update destination directory does not exist"
fi
if [[ $DO_UPDATE -eq 1 && ( -L "$COMMITTED" || -d "$COMMITTED" ) ]]; then
  die 2 "update destination must be a regular file path, not a symlink or directory"
fi

# Check mode requires an independent baseline before generation. In particular a dangling
# committed symlink must not become the generated file and then verify itself.
if [[ $DO_GENERATE -eq 1 && $DO_UPDATE -eq 0 ]]; then
  [[ -f "$COMMITTED" && -r "$COMMITTED" ]] || die 3 "no readable committed NOTICE at '$COMMITTED' — nothing was generated"
fi

_NC_TMPDIR=""
cleanup() { [[ -z "$_NC_TMPDIR" ]] || rm -rf -- "$_NC_TMPDIR"; }
trap cleanup EXIT
if [[ -n "$GENERATED" ]]; then
  GENERATED="$(absolute_path "$GENERATED")"
  # Resolve only existing parents here; alias detection must precede generation or output writes.
  if [[ -d "$(dirname "$GENERATED")" ]]; then
    GENERATED="$(canonical_parent "$GENERATED")" || die 2 "cannot resolve generated path"
  fi
  if [[ "$GENERATED" == "$COMMITTED" || "$GENERATED" -ef "$COMMITTED" ]]; then
    die 2 "generated and committed NOTICE must be distinct files"
  fi
elif [[ $DO_GENERATE -eq 1 ]]; then
  if [[ $DO_UPDATE -eq 1 ]]; then
    # Same filesystem as destination: install by atomic rename only after generation/validation.
    _NC_TMPDIR="$(mktemp -d "$committed_parent/.notice-update.XXXXXX")" || die 5 "cannot allocate generation directory"
  else
    _NC_TMPDIR="$(mktemp -d "${TMPDIR:-/tmp}/notice-check.XXXXXX")" || die 5 "cannot allocate generation directory"
  fi
  [[ -n "$_NC_TMPDIR" && -d "$_NC_TMPDIR" ]] || die 5 "generation directory is unusable"
  GENERATED="$_NC_TMPDIR/NOTICE"
else
  die 2 "need --generated <file> (or --generate to run the generator)"
fi

if [[ $DO_GENERATE -eq 1 ]]; then
  outdir="$(dirname "$GENERATED")"
  mkdir -p -- "$outdir" || die 5 "cannot create generation directory"
  GENERATED="$(canonical_parent "$GENERATED")" || die 5 "cannot resolve generation directory"
  # Recheck after creating/canonicalizing the output parent, before touching the file.
  if [[ "$GENERATED" == "$COMMITTED" || "$GENERATED" -ef "$COMMITTED" ]]; then
    die 2 "generated and committed NOTICE must be distinct files"
  fi
  outdir="$(dirname "$GENERATED")"
  # Never allow a successful no-output generator to vouch with a caller's stale artifact.
  rm -f -- "$GENERATED" || die 5 "cannot clear previous generated output"
  ( cd "$ROOT" && mvn -B -q ${MAVEN_REPO_LOCAL:+-Dmaven.repo.local="$MAVEN_REPO_LOCAL"} \
      org.codehaus.mojo:license-maven-plugin:aggregate-add-third-party \
      -Dlicense.excludedScopes=test,provided,system \
      -Dlicense.fileTemplate=appbase-java/etc/notice.ftl \
      -Dlicense.thirdPartyFilename="$(basename "$GENERATED")" \
      -Dlicense.outputDirectory="$outdir" ) \
    || die 5 "the generator failed — NOTICE could not be regenerated, so nothing was verified"
fi
[[ -f "$GENERATED" && -r "$GENERATED" ]] || die 5 "the generator produced no readable file at '$GENERATED' — nothing was verified"
[[ -s "$GENERATED" ]] || die 5 "the generated NOTICE at '$GENERATED' is EMPTY — nothing was verified"
gen_n="$(entries_in "$GENERATED")"
[[ "${gen_n:-0}" -gt 0 ]] || die 5 "the generated NOTICE has ZERO dependency entries — nothing was verified"

if [[ $DO_UPDATE -eq 1 ]]; then
  # Stage before replacing: failed copy/rename never truncates the existing baseline.
  install_file="$_NC_TMPDIR/install"
  if [[ -e "$COMMITTED" ]]; then
    [[ -f "$COMMITTED" ]] || die 5 "update destination is not a regular file"
    cp -p -- "$COMMITTED" "$install_file" || die 5 "cannot stage NOTICE update"
  fi
  cp -- "$GENERATED" "$install_file" || die 5 "cannot stage generated NOTICE"
  if [[ ! -e "$COMMITTED" ]]; then
    chmod 644 "$install_file" || die 5 "cannot set NOTICE permissions"
  fi
  mv -f -- "$install_file" "$COMMITTED" || die 5 "cannot install NOTICE update"
  echo "notice-check: updated '$COMMITTED' from $gen_n generated dependency entries; review and commit the diff"
  exit 0
fi

[[ -f "$COMMITTED" ]] || die 3 "no committed NOTICE at '$COMMITTED' — it has not drifted, it is absent"
[[ -r "$COMMITTED" ]] || die 3 "cannot read the committed NOTICE at '$COMMITTED'"
diff -q -- "$COMMITTED" "$GENERATED" >/dev/null 2>&1; diff_rc=$?
case "$diff_rc" in
  0) echo "notice-check: in sync — $gen_n dependency entries verified against '$COMMITTED'"; exit 0;;
  1) ;; # Actual differences, not comparison failure.
  *) die 6 "comparison failed (diff exit $diff_rc) — NOTICE could not be verified";;
esac
{
  echo "notice-check: DRIFT — the committed NOTICE does not match the generated file."
  echo "  committed: $COMMITTED ($(entries_in "$COMMITTED") entries)"
  echo "  generated: $GENERATED ($gen_n entries)"
  echo "  --- committed / +++ regenerated ---"
  diff -u -- "$COMMITTED" "$GENERATED" | tail -n +3 | head -60
  printf '  Regenerate and commit NOTICE:  /bin/bash %q --update --committed %q\n' "$HERE/notice-check.sh" "$COMMITTED"
} >&2
exit 4
