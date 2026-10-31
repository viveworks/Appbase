#!/usr/bin/env bash
#
# vitess_purge_safety_test.sh — A10-08 (internal work item 1489 / N10 internal work item 1486): Vitess purge deletion-path safety.
#
# The shipped defect: appbase-vitess-purge.sh ran `rm -rf ${VTDATAROOT}` — UNQUOTED, on a
# caller-controlled env var. A VTDATAROOT of "/", "/a/*", "/a /b", or "/data/../../etc" therefore
# word-split, glob-expanded, or escaped into an unbounded destructive delete. The two entry points
# (service-schema/local-vitess-purge.sh, appbase-stacktest/.../local-vitess-purge.sh) both source this
# core library and call vitess_purge(), so the fail-closed invariant is asserted once, at the choke point.
#
# Strategy: source the real script in an isolated subshell with (a) a stub for the sourced stop-script
# and (b) a fake `rm` on PATH that RECORDS its argv and working directory instead of deleting. We then
# assert what `rm` would have received. Nothing is ever deleted.
#
# @author Navaneeth Mysore G
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$HERE/../.." && pwd)"
PASSED=0; FAILED=0
ok(){ PASSED=$((PASSED+1)); echo "  ok   $1"; }
bad(){ FAILED=$((FAILED+1)); echo "  FAIL $1"; }
PURGE="$ROOT/appbase-java/appbase-client/appbase-client-vitess/infra/local/vitess/appbase-vitess-purge.sh"

echo "vitess purge deletion-path safety (A10-08 / internal work item 1489)"; echo

# run_purge <VTDATAROOT> [ALLOWED_ROOT]
# Sources the real purge script and runs vitess_purge with a recording `rm`. Sets globals:
#   RC       — exit status of vitess_purge
#   RM_ARGS  — newline-separated argv the fake rm received ("" if rm was never called)
#   RM_LINES — number of argv tokens rm received (0 if never called)
run_purge() {
  local vtdata="$1"; local allowed="${2:-}"
  local sandbox bindir
  sandbox="$(mktemp -d)"; bindir="$(mktemp -d)"
  # Stub the sourced stop-script: define exactly the two functions vitess_purge invokes.
  cat >"$sandbox/appbase-vitess-stop.sh" <<'STUB'
function stop_vitess_processes(){ :; }
function zookeeper_clear(){ :; }
STUB
  # Fake rm: record argv, delete nothing.
cat >"$bindir/rm" <<RECORDER
#!/usr/bin/env bash
printf '%s\n' "\$@" >>"$sandbox/rm.args"
pwd -P >"$sandbox/rm.cwd"
RECORDER
  chmod +x "$bindir/rm"
  : >"$sandbox/rm.args"
  : >"$sandbox/rm.cwd"
  RC=0
  (
    export PATH="$bindir:$PATH"
    export APPBASE_VITESS_SCRIPTS_BASEDIR="$sandbox"
    export VTDATAROOT="$vtdata"
    if [ -n "$allowed" ]; then export APPBASE_VITESS_ALLOWED_DATA_ROOT="$allowed"; fi
    # shellcheck disable=SC1090
    source "$PURGE"
    vitess_purge
  ) >/dev/null 2>&1 || RC=$?
  RM_ARGS="$(cat "$sandbox/rm.args")"
  RM_CWD="$(cat "$sandbox/rm.cwd")"
  RM_LINES="$(grep -c . "$sandbox/rm.args" 2>/dev/null || echo 0)"
  rm -rf "$sandbox" "$bindir"
}

# Same, but override VTDATAROOT at the vitess_purge CALL so the ${VTDATAROOT:-default} at source-time
# cannot mask a deliberately-empty value.
run_purge_empty() {
  local sandbox bindir
  sandbox="$(mktemp -d)"; bindir="$(mktemp -d)"
  cat >"$sandbox/appbase-vitess-stop.sh" <<'STUB'
function stop_vitess_processes(){ :; }
function zookeeper_clear(){ :; }
STUB
  cat >"$bindir/rm" <<RECORDER
#!/usr/bin/env bash
printf '%s\n' "\$@" >>"$sandbox/rm.args"
RECORDER
  chmod +x "$bindir/rm"
  : >"$sandbox/rm.args"
  RC=0
  (
    export PATH="$bindir:$PATH"
    export APPBASE_VITESS_SCRIPTS_BASEDIR="$sandbox"
    # shellcheck disable=SC1090
    source "$PURGE"
    VTDATAROOT="" vitess_purge
  ) >/dev/null 2>&1 || RC=$?
  RM_ARGS="$(cat "$sandbox/rm.args")"
  RM_LINES="$(grep -c . "$sandbox/rm.args" 2>/dev/null || echo 0)"
  rm -rf "$sandbox" "$bindir"
}

# Same, but override APPBASE_VITESS_ALLOWED_DATA_ROOT to empty at the vitess_purge CALL (a real system
# dir as the target), so the source-time ${...:-default} cannot mask an empty allowed root.
run_purge_empty_allowed() {
  local sandbox bindir
  sandbox="$(mktemp -d)"; bindir="$(mktemp -d)"
  cat >"$sandbox/appbase-vitess-stop.sh" <<'STUB'
function stop_vitess_processes(){ :; }
function zookeeper_clear(){ :; }
STUB
  cat >"$bindir/rm" <<RECORDER
#!/usr/bin/env bash
printf '%s\n' "\$@" >>"$sandbox/rm.args"
RECORDER
  chmod +x "$bindir/rm"
  : >"$sandbox/rm.args"
  RC=0
  (
    export PATH="$bindir:$PATH"
    export APPBASE_VITESS_SCRIPTS_BASEDIR="$sandbox"
    # shellcheck disable=SC1090
    source "$PURGE"
    VTDATAROOT="/etc" APPBASE_VITESS_ALLOWED_DATA_ROOT="" vitess_purge
  ) >/dev/null 2>&1 || RC=$?
  RM_ARGS="$(cat "$sandbox/rm.args")"
  RM_LINES="$(grep -c . "$sandbox/rm.args" 2>/dev/null || echo 0)"
  rm -rf "$sandbox" "$bindir"
}

# ── Happy path: default target, inside the (default) allowed root ────────────────────────────────────
# Expect: rm called once from the physical parent with a relative leaf. That relative operand is the
# capability boundary: ancestor renames after `cd -P` cannot redirect it.
HAPPY_PARENT="$ROOT/.vitess-purge-happy.$$"
DEFAULT_TARGET="$HAPPY_PARENT/Vitess"
mkdir -p "$DEFAULT_TARGET"
run_purge "$DEFAULT_TARGET" "$DEFAULT_TARGET"
[ "$RC" = "0" ] && ok "default target purges (exit 0)" || bad "default target rejected (rc=$RC)"
printf '%s\n' "$RM_ARGS" | grep -qx -- '--' \
  && ok "rm is invoked with a -- end-of-options guard" || bad "rm has no -- guard (argv: $RM_ARGS)"
printf '%s\n' "$RM_ARGS" | grep -qx -- './Vitess' \
  && ok "rm targets the resolved data-root leaf" || bad "rm did not target the data-root leaf (argv: $RM_ARGS)"
[ "$RM_CWD" = "$HAPPY_PARENT" ] \
  && ok "rm is anchored in the physical target parent" || bad "rm cwd is '$RM_CWD', expected target parent"
[ "$RM_LINES" = "3" ] \
  && ok "rm receives exactly one operand (rf, --, target)" || bad "rm received $RM_LINES tokens, expected 3 (argv: $RM_ARGS)"
[ "$RM_ARGS" = "-rf
--
./Vitess" ] \
  && ok "rm receives the exact anchored argv" || bad "rm argv differs from '-rf -- ./leaf' (argv: $RM_ARGS)"

# ── Unsafe values: rm must NEVER be invoked (fail closed BEFORE any deletion) ──────────────────────────
assert_blocked() { # <label> — asserts RC!=0 and rm untouched, using current RC/RM_ARGS
  local label="$1"
  if [ "$RC" != "0" ] && [ -z "$RM_ARGS" ]; then ok "$label — blocked, rm never called"
  else bad "$label — NOT blocked (rc=$RC, rm argv: ${RM_ARGS:-<none>})"; fi
}

run_purge "/tmp/aaa /tmp/bbb" "/tmp";        assert_blocked "multiword VTDATAROOT"
run_purge "/tmp/data/*" "/tmp/data";         assert_blocked "glob VTDATAROOT"
run_purge "/" "/";                           assert_blocked "root VTDATAROOT"
run_purge "/tmp/data/../../etc" "/tmp/data"; assert_blocked ".. traversal escaping the allowed root"
run_purge "/etc/passwd_dir" "/tmp/data";     assert_blocked "target outside the allowed data root"
run_purge_empty;                             assert_blocked "empty VTDATAROOT"

# An empty/unsafe ALLOWED root must fail closed too — the guard must not depend on line-17's `:-` default
# masking it. Set it empty at the vitess_purge call so the source-time default cannot substitute.
run_purge_empty_allowed;                     assert_blocked "empty APPBASE_VITESS_ALLOWED_DATA_ROOT"
run_purge "/tmp/data" "/tmp/*";             assert_blocked "glob-bearing allowed data root"
run_purge "/tmp/data" "/tmp/data/..";       assert_blocked "traversing allowed data root"

# Source-time explicit empties must remain empty rather than being replaced by defaults.
RC=0
SOURCE_SANDBOX="$(mktemp -d)"
(
  export APPBASE_VITESS_SCRIPTS_BASEDIR="$SOURCE_SANDBOX"
  printf 'function stop_vitess_processes(){ :; }\nfunction zookeeper_clear(){ :; }\n' \
    >"$APPBASE_VITESS_SCRIPTS_BASEDIR/appbase-vitess-stop.sh"
  export VTDATAROOT=""
  export APPBASE_VITESS_ALLOWED_DATA_ROOT=""
  # shellcheck disable=SC1090
  source "$PURGE"
  [ -z "$VTDATAROOT" ] && [ -z "$APPBASE_VITESS_ALLOWED_DATA_ROOT" ]
) >/dev/null 2>&1 || RC=$?
[ "$RC" = "0" ] && ok "source-time explicit empty values are not defaulted" \
  || bad "source-time explicit empty values were replaced"
rm -rf "$SOURCE_SANDBOX"

# An intermediate symlink can make a textually contained target escape the allowed root.
SYMLINK_SANDBOX="$(mktemp -d)"
mkdir -p "$SYMLINK_SANDBOX/allowed" "$SYMLINK_SANDBOX/outside/data"
ln -s "$SYMLINK_SANDBOX/outside" "$SYMLINK_SANDBOX/allowed/link"
run_purge "$SYMLINK_SANDBOX/allowed/link/data" "$SYMLINK_SANDBOX/allowed"
assert_blocked "intermediate symlink escaping the allowed root"
rm -rf "$SYMLINK_SANDBOX"

# Deterministic check/use race: validation sees a real contained directory, then the stop step swaps its
# parent pathname to a symlink before deletion. An absolute `rm "$target"` would reach OUTSIDE/target.
# The anchored delete must re-open the parent physically, detect the swap, and never invoke rm.
RACE_ROOT="$ROOT/.vitess-purge-race.$$"
mkdir -p "$RACE_ROOT/allowed/target" "$RACE_ROOT/outside/target"
RACE_BIN="$(mktemp -d)"; : >"$RACE_ROOT/rm.args"
cat >"$RACE_BIN/rm" <<RECORDER
#!/usr/bin/env bash
printf '%s\n' "\$@" >>"$RACE_ROOT/rm.args"
RECORDER
chmod +x "$RACE_BIN/rm"
RC=0
(
  export PATH="$RACE_BIN:$PATH"
  export APPBASE_VITESS_SCRIPTS_BASEDIR="$RACE_ROOT"
  cat >"$RACE_ROOT/appbase-vitess-stop.sh" <<STUB
function stop_vitess_processes(){ mv "$RACE_ROOT/allowed" "$RACE_ROOT/allowed-original"; ln -s "$RACE_ROOT/outside" "$RACE_ROOT/allowed"; }
function zookeeper_clear(){ :; }
STUB
  export VTDATAROOT="$RACE_ROOT/allowed/target"
  export APPBASE_VITESS_ALLOWED_DATA_ROOT="$RACE_ROOT/allowed"
  # shellcheck disable=SC1090
  source "$PURGE"
  vitess_purge
) >/dev/null 2>&1 || RC=$?
if [ "$RC" -ne 0 ] && [ ! -s "$RACE_ROOT/rm.args" ] && [ -d "$RACE_ROOT/outside/target" ]; then
  ok "parent swap between validation and deletion is refused before rm"
else
  bad "parent swap race was not contained (rc=$RC, rm=$(cat "$RACE_ROOT/rm.args"))"
fi
rm -rf "$RACE_ROOT" "$RACE_BIN"

# ── Both wrapper entry points reach the guarded core ─────────────────────────────────────────────────
# service-schema/local-vitess-purge.sh and appbase-stacktest/.../local-vitess-purge.sh only `source` the
# core script and call vitess_purge, so the invariant is shared ONLY if their source path resolves to the
# hardened core. We prove that end-to-end: run the real wrapper with a recording `rm` and externals
# shimmed to no-ops, and assert `rm` is invoked once as `rm -rf -- <target>`. A wrong source path leaves
# vitess_purge undefined, so `rm` is never called and the arm fails.
SS_WRAPPER="$ROOT/appbase-java/appbase-client/appbase-client-vitess/infra/local/service-schema/local-vitess-purge.sh"
ST_WRAPPER="$ROOT/appbase-stacktest/infra/vitess/local-vitess-purge.sh"

run_wrapper() { # <wrapper-path> — happy path; sets RC / RM_ARGS / RM_LINES
  local wrapper="$1" sandbox bindir
  sandbox="$(mktemp -d)"; bindir="$(mktemp -d)"
  # Recording rm; no-op shims for every external the real stop/purge chain may touch.
cat >"$bindir/rm" <<RECORDER
#!/usr/bin/env bash
printf '%s\n' "\$@" >>"$sandbox/rm.args"
pwd -P >"$sandbox/rm.cwd"
RECORDER
  for stub in docker mysqlctl vtctlclient vtctldclient; do printf '#!/usr/bin/env bash\nexit 0\n' >"$bindir/$stub"; done
  printf '#!/usr/bin/env bash\nexit 1\n' >"$bindir/pgrep"   # "no stale processes"
  for bin in vttablet vtgate vtctld vtorc vtctl; do printf '#!/usr/bin/env bash\necho "Version: 17.0.0"\n' >"$bindir/$bin"; done
  chmod +x "$bindir"/*
  : >"$sandbox/rm.args"
  : >"$sandbox/rm.cwd"
  RC=0
  (
    export PATH="$bindir:$PATH"
    export VTDATAROOT="$DEFAULT_TARGET"
    export APPBASE_VITESS_ALLOWED_DATA_ROOT="$DEFAULT_TARGET"
    bash "$wrapper"
  ) >/dev/null 2>&1 || RC=$?
  RM_ARGS="$(cat "$sandbox/rm.args")"
  RM_CWD="$(cat "$sandbox/rm.cwd")"
  RM_LINES="$(grep -c . "$sandbox/rm.args" 2>/dev/null || echo 0)"
  rm -rf "$sandbox" "$bindir"
}

run_wrapper "$SS_WRAPPER"
{ [ "$RC" = "0" ] && [ "$RM_ARGS" = "-rf
--
./Vitess" ] && [ "$RM_CWD" = "$HAPPY_PARENT" ]; } \
  && ok "service-schema wrapper reaches the anchored core (parent + ./leaf)" \
  || bad "service-schema wrapper did not reach the guard (rc=$RC, argv: ${RM_ARGS:-<none>})"

run_wrapper "$ST_WRAPPER"
{ [ "$RC" = "0" ] && [ "$RM_ARGS" = "-rf
--
./Vitess" ] && [ "$RM_CWD" = "$HAPPY_PARENT" ]; } \
  && ok "stacktest wrapper reaches the anchored core (parent + ./leaf)" \
  || bad "stacktest wrapper did not reach the guard (rc=$RC, argv: ${RM_ARGS:-<none>})"

echo; echo "  $PASSED passed, $FAILED failed"
rm -rf "$HAPPY_PARENT"
[ "$FAILED" -eq 0 ]
