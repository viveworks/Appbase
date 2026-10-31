#!/usr/bin/env bash
#
# appbase_emulator_infra_test.sh — the two EMULATOR-backed integration surfaces of epic P04·E01
# (internal work item 1135): S3 on owner-selected MinIO and Firebase Auth on the Firebase Auth Emulator (internal work item 1299 / N09).
#
# 🔴 WHY THIS SUITE EXISTS IN THIS SHAPE. N09's acceptance ("both ITs run green against emulators")
# needs a live Docker daemon, which the authoring lane does not have. That is precisely the situation
# in which a gate quietly becomes decorative — so the durable regression signal is the part that is
# checkable WITHOUT a daemon, and it is deliberately NOT a set of string comparisons:
#
#   TIER S — the MinIO S3 lifecycle is honest: the pin is the evidence-backed immutable Chainguard
#   digest on MinIO's own data path, the run script goes through the shared lifecycle
#   contract (require-image → verify-running → wait-ready) rather than a bare detached run, and the
#   teardown surfaces failure instead of masking it behind a trailing `rm -rf`.
#
#   TIER W — the shared orchestrators actually reach the new backend, and name no directory that does
#   not exist. N01 removed MINIO_DIR and left the S3 slot empty for this task; an orchestrator that
#   silently skips S3 is exactly the "green build that ran nothing" this epic exists to end.
#
#   TIER C — the client config points at the endpoint the lifecycle PUBLISHES. This is a CROSS-FILE
#   behaviour, not a literal: the arm reads the published port out of run-s3.sh and the endpoint out
#   of the YAML and asserts they agree, so moving either one alone reddens it. The config pointed at
#   http://localhost:9000 — the port of the MinIO container deleted from the tree in 81c9f2389 — so
#   the IT could only ever have failed to connect.
#
#   TIER F — the Firebase Auth emulator host satisfies the Admin SDK's OWN URL contract. The suite
#   composes the URL with the exact template firebase-admin 9.3.0 uses
#   (FirebaseUserManager.ID_TOOLKIT_URL_EMULATOR = "http://%s/identitytoolkit.googleapis.com/%s/projects/%s",
#   fed by Utils.getEmulatorHost()) and asserts the RESULT is a well-formed URL whose authority port is
#   the auth port this repo's own firebase.json declares. The old value "http://localhost:4000/"
#   composed to "http://http://localhost:4000//identitytoolkit.googleapis.com/v1/projects/…" — a
#   doubled scheme AND the Emulator *UI* port — so the Failsafe env could never have reached the Auth
#   emulator. Asserting the composition rather than the literal is what makes the arm survive a
#   re-wording and still catch a re-breakage.
#
# ⚠️ ARMS *0 CHECK THE CHECK. Each tier's first arm feeds its own guard a KNOWN-BAD fixture and asserts
# the guard REJECTS it. Without them every green arm below could be the vacuous green a never-run suite
# hides best — the same reasoning as appbase_infra_lifecycle_test.sh, whose shape this follows.
#
# The live bring-up (MinIO up → bucket round trip → Auth emulator up → user round trip) is
# the Docker proof this lane cannot run and does not fake. It remains EPIC-VERIFICATION-PENDING,
# not asserted here.
#
# ⚠️ NOT MEASURED BY THIS SUITE, stated so a green run is not read as more than it is:
#   · The lifecycle HELPERS' behaviour for the S3 path. S5 asserts run-s3.sh GOES THROUGH
#     require-image / verify-running / wait-ready; it does not re-prove those helpers work — that is
#     appbase_infra_lifecycle_test.sh TIER B, which drives them against an enforcing fake `docker`.
#     This suite deliberately does not duplicate it, and therefore does not cover it.
#   · Anything needing a daemon: image pull, container start, silent death, readiness, teardown.
#   · Whether MinIO ACCEPTS every client operation. TIER C statically proves endpoint/region
#     agreement; the hosted live gate supplies the service-level create-bucket evidence.
#   · The Java ITs themselves. Neither was compiled or run: the lane has no JDK 25 (build.sh refuses).
#
# It needs `python3` to read firebase.json; absent, TIER A's arms fail loudly rather than pass.
#
# @author Navaneeth Mysore G
# @author Claude
#
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$HERE/../.." && pwd)"
CLIENT="$ROOT/appbase-java/appbase-client"
CORE_DOCKER="$CLIENT/appbase-client-core/infra/docker"
S3_MODULE="$CLIENT/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3"
S3_DOCKER="$S3_MODULE/infra/docker/s3"
S3_ENV="$S3_DOCKER/env-s3.sh"
S3_RUN="$S3_DOCKER/run-s3.sh"
S3_REMOVE="$S3_DOCKER/remove-s3.sh"
S3_YAML="$S3_MODULE/src/test/resources/appbase-client-aws-s3.yaml"
STACKTEST_YAML="$ROOT/appbase-stacktest/src/main/resources/appbase-stacktest-aws-s3.yaml"
FIREBASE_DOCKER="$CLIENT/appbase-client-firebase/infra/docker/firebase"
FIREBASE_JSON="$FIREBASE_DOCKER/firebase.json"
FIREBASE_RUN="$FIREBASE_DOCKER/run-firebase.sh"
FIREBASE_ENTRYPOINT="$FIREBASE_DOCKER/firebase-entrypoint.sh"
ROOT_POM="$ROOT/pom.xml"
COMMON_ALL="$CORE_DOCKER/common-all.sh"

# The EXACT, evidence-backed MinIO pin. Asserting the exact string is what makes a
# swap to a nonexistent/typo tag, or a drift back to a stale one, turn an arm red — a guard that only
# rejected "not minio" would let any other string through.
EXPECTED_S3_IMAGE="cgr.dev/chainguard/minio@sha256:2136d593beacf4461187035eca949fe49abd589d5079735025c711ca58fee0c8"
EXPECTED_S3_PORT="9000"
EXPECTED_S3_ENDPOINT="http://localhost:${EXPECTED_S3_PORT}"
EXPECTED_S3_MOUNT="/data"
EXPECTED_S3_CONTAINER="minio-s3.local"
EXPECTED_S3_REGION="us-east-1"
# The superseded LocalStack edge port must not return through a stale fixture.
SUPERSEDED_LOCALSTACK_PORT="4566"

PASSED=0; FAILED=0
ok()  { PASSED=$((PASSED+1)); echo "  ok   $1"; }
bad() { FAILED=$((FAILED+1)); echo "  FAIL $1"; }

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT

# ─────────────────────────────────────────────────────────────────────────────────────────────────
# Shared readers. `env_var FILE NAME` sources FILE in a subshell and prints NAME, so the arms read the
# EFFECTIVE value (after any expansion) instead of pattern-matching the line that sets it.
# ─────────────────────────────────────────────────────────────────────────────────────────────────
env_var() {
  local file="$1" name="$2"
  [ -r "$file" ] || return 1
  ( set +u; HOME="${HOME:-/tmp}"; . "$file" >/dev/null 2>&1; printf '%s' "${!name-}" )
}

# common_var NAME — the same reader for common-all.sh, which derives CLIENT_ROOT from `dirname "$0"`.
# Give the child shell the lifecycle script's own $0. Merely changing directory does not change $0:
# an absolute suite invocation otherwise resolves every *_DIR relative to scripts/test.
common_var() {
  local name="$1"
  [ -r "$COMMON_ALL" ] || return 1
  ( cd "$(dirname "$COMMON_ALL")" || exit 1
    bash -c 'set +u; . ./common-all.sh >/dev/null 2>&1; printf "%s" "${!1-}"' ./common-all.sh "$name" )
}

# published_port FILE CONTAINER_PORT -> prints the host port published onto CONTAINER_PORT, if any.
# Accepts both `--publish H:C` and `--publish ADDR:H:C`.
# 🔴 COMMENTS ARE STRIPPED FIRST, AND THAT IS LOAD-BEARING. run-s3.sh's rationale block QUOTES the wide
# `--publish 9000:9000` form it rejects, so a reader that did not strip comments found the port in the
# PROSE and reported the container as publishing it — a FALSE GREEN that survived a real change to the
# publish line. Measured on this suite before the PR: S4 and C1 stayed green after the binding moved to
# 127.0.0.1, because both were reading the comment.
published_port() {
  local file="$1" cport="$2"
  [ -r "$file" ] || return 1
  sed -E 's/(^|[[:space:]])#.*$//' "$file" \
    | grep -oE -- "--publish[[:space:]]+([0-9.]+:)?[0-9]+:${cport}\b" \
    | sed -E 's/.*[[:space:]]//; s/:'"${cport}"'$//; s/^.*:([0-9]+)$/\1/' | head -1
}

# Print every Docker publish mapping in normalized form, or an UNSAFE marker for publish-all/host networking.
recorded_publishes() {
  sed -E 's/(^|[[:space:]])#.*$//' "$1" \
    | awk '/^[[:space:]]*docker run/{in_run=1; print "DOCKER_RUN"} in_run{print} in_run && /\|\| exit/{in_run=0}' \
    | awk '
    pending_publish { print $0; pending_publish=0; next }
    pending_network { if ($0 == "host") print "UNSAFE_NETWORK"; pending_network=0; next }
    $0 == "DOCKER_RUN" { print; next }
    $0 == "-P" || $0 == "--publish-all" || $0 == "-P=true" || $0 == "--publish-all=true" {
      print "UNSAFE_PUBLISH_ALL"; next
    }
    $0 == "--network" || $0 == "--net" { pending_network=1; next }
    $0 == "--network=host" || $0 == "--net=host" { print "UNSAFE_NETWORK"; next }
    $0 == "-p" || $0 == "--publish" { pending_publish=1; next }
    /^-p./ { sub(/^-p/, ""); print; next }
    /^--publish=/ { sub(/^--publish=/, ""); print }
  ' RS='[[:space:]]+' | sed '/^$/d'
}

# yaml_endpoint FILE -> prints the endpointOverrideUri value.
yaml_endpoint() {
  local file="$1"
  [ -r "$file" ] || return 1
  grep -E '^[[:space:]]*endpointOverrideUri:' "$file" | head -1 | sed -E 's/.*endpointOverrideUri:[[:space:]]*"?([^"]*)"?[[:space:]]*$/\1/'
}

# uri_port URI -> prints the port of a scheme://host:port URI ("" when absent).
uri_port() { printf '%s' "$1" | sed -E 's#^[a-zA-Z][a-zA-Z0-9+.-]*://##' | sed -E 's#/.*$##' | awk -F: 'NF>1{print $NF}'; }

s3_endpoint_ok() { [ "$1" = "$EXPECTED_S3_ENDPOINT" ]; }

# Read only a direct child of the root Maven <properties>. XML comments and profile-local properties
# are deliberately outside that model and therefore cannot win by appearing first in the file.
maven_root_property() {
  python3 - "$1" "$2" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
namespace = root.tag.split('}', 1)[0] + '}' if root.tag.startswith('{') else ''
properties = root.find(f'{namespace}properties')
if properties is None:
    raise SystemExit(1)
values = [child.text.strip() for child in properties
          if child.tag == f'{namespace}{sys.argv[2]}' and child.text and child.text.strip()]
if len(values) != 1:
    raise SystemExit(1)
print(values[0], end='')
PY
}

sandbox_prepare() {
  SANDBOX_ROOT="$TMP/sandbox path with space"; local client="$SANDBOX_ROOT/appbase-client"
  SANDBOX_S3="$client/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3/infra/docker/s3"
  SANDBOX_HOME="$SANDBOX_ROOT/home with space"; SANDBOX_BIN="$SANDBOX_ROOT/bin"
  SANDBOX_LOG="$SANDBOX_ROOT/docker.argv"; SANDBOX_PROBE="$SANDBOX_ROOT/probe.log"
  mkdir -p "$SANDBOX_S3" "$client/appbase-client-core/infra/docker" "$SANDBOX_HOME" "$SANDBOX_BIN" || return 1
  cp "$S3_ENV" "$S3_RUN" "$S3_DOCKER/start-s3.sh" "$S3_REMOVE" "$SANDBOX_S3/" || return 1
  cp "$CORE_DOCKER/lifecycle-common.sh" "$client/appbase-client-core/infra/docker/" || return 1
  cat >> "$client/appbase-client-core/infra/docker/lifecycle-common.sh" <<'STUB'
appbase_tcp_probe() { printf '%s:%s\n' "$1" "$2" >> "$S3_PROBE_LOG"; [ "$S3_SANDBOX_MODE" = ready ]; }
STUB
  cat > "$SANDBOX_BIN/docker" <<'DOCKER'
#!/bin/bash
cmd="${1:-}"; [ "$#" -gt 0 ] && shift
case "$cmd" in
 run)
   : > "$S3_DOCKER_LOG"; printf '%s\n' "$@" >> "$S3_DOCKER_LOG"
   cleanup_mount=""; cleanup=false; previous=""
   for arg in "$@"; do
     [ "$previous" = "--volume" ] && cleanup_mount="${arg%%:*}"
     [ "$previous" = "--entrypoint" ] && [ "$arg" = "/bin/sh" ] && cleanup=true
     previous="$arg"
   done
   if $cleanup && [ -n "$cleanup_mount" ]; then
     canonical_mount="$(python3 - "$cleanup_mount" "$S3_SANDBOX_ROOT" <<'PY'
import pathlib
import sys

mount = pathlib.Path(sys.argv[1])
root = pathlib.Path(sys.argv[2]).resolve(strict=True)
if mount.is_symlink():
    raise SystemExit(1)
resolved = mount.resolve(strict=True)
try:
    relative = resolved.relative_to(root)
except ValueError:
    raise SystemExit(1)
if not relative.parts:
    raise SystemExit(1)
print(resolved)
PY
)" || exit 98
     rm -rf -- "$canonical_mount"/* "$canonical_mount"/.[!.]* "$canonical_mount"/..?*
   fi;;
 image) [ "${1:-}" = inspect ] || exit 97;;
 inspect) [ "$S3_SANDBOX_MODE" = stopped ] && echo false || echo true;;
 start) :;;
 rm) [ "$S3_SANDBOX_MODE" = rm_fail ] && exit 1 || :;;
 *) exit 97;;
esac
DOCKER
  cat > "$SANDBOX_BIN/curl" <<'CURL'
#!/bin/bash
[ "$S3_SANDBOX_MODE" = ready ]
CURL
  chmod +x "$SANDBOX_BIN/docker" "$SANDBOX_BIN/curl"
  export S3_DOCKER_LOG="$SANDBOX_LOG" S3_PROBE_LOG="$SANDBOX_PROBE" \
    S3_SANDBOX_ROOT="$SANDBOX_ROOT"
}
sandbox_reset() {
  S3_SANDBOX_MODE="$1"; export S3_SANDBOX_MODE
  cp "$S3_ENV" "$S3_RUN" "$S3_DOCKER/start-s3.sh" "$S3_REMOVE" "$SANDBOX_S3/" || exit 2
  rm -f "$SANDBOX_LOG" "$SANDBOX_PROBE" "$TMP/sandbox.out" "$TMP/sandbox.err"
  rm -rf "$SANDBOX_HOME/Temporary"
}
sandbox_run() {
  HOME="$SANDBOX_HOME" PATH="$SANDBOX_BIN:$PATH" S3_READY_RETRIES=1 S3_READY_INTERVAL=0 \
    bash "$SANDBOX_S3/$1" >"$TMP/sandbox.out" 2>"$TMP/sandbox.err"
}
sandbox_prepare || { echo 'sandbox setup failed' >&2; exit 2; }

echo "appbase_emulator_infra_test: N09 (internal work item 1299) — MinIO S3 + Firebase Auth Emulator surfaces"

# ═════════════════════════════════════════════════════════════════════════════════════════════════
# TIER S — the MinIO S3 lifecycle is honest
# ═════════════════════════════════════════════════════════════════════════════════════════════════
echo "TIER S — MinIO S3 lifecycle manifest"

# s3_env_ok FILE — the Tier-S guard, factored out so S0 can feed it a known-bad fixture.
s3_env_ok() {
  local f="$1" img mount name
  img="$(env_var "$f" DOCKER_IMAGE)" || return 1
  mount="$(env_var "$f" VOLUME_MOUNT)" || return 1
  name="$(env_var "$f" CONTAINER_NAME)" || return 1
  [ "$img" = "$EXPECTED_S3_IMAGE" ] || return 1
  [ "$mount" = "$EXPECTED_S3_MOUNT" ] || return 1
  [ "$name" = "$EXPECTED_S3_CONTAINER" ] || return 1
  return 0
}

# ── S0 — CHECK THE CHECK: the guard must REJECT the superseded LocalStack contract ────────────────
cat > "$TMP/env-bad.sh" <<'BAD'
#!/bin/bash
export DOCKER_IMAGE="localstack/localstack:2026.08.3"
export CONTAINER_NAME="localstack.local"
export VOLUME_MOUNT="/var/lib/localstack"
BAD
if s3_env_ok "$TMP/env-bad.sh"; then bad "S0 the Tier-S guard ACCEPTED the superseded LocalStack env file (guard is vacuous)"
else ok "S0 check-the-check: the Tier-S guard rejects the superseded LocalStack env file"; fi

# ── S1 — the S3 backend is pinned to the exact owner-selected MinIO digest ────────────────────────
if [ -r "$S3_ENV" ]; then
  s1_img="$(env_var "$S3_ENV" DOCKER_IMAGE)"
  if [ "$s1_img" = "$EXPECTED_S3_IMAGE" ]; then ok "S1 env-s3.sh pins $EXPECTED_S3_IMAGE (manifest digest recorded in the decisions log)"
  else bad "S1 env-s3.sh pins '$s1_img' (expected $EXPECTED_S3_IMAGE)"; fi
else bad "S1 $S3_ENV is absent — there is no S3 lifecycle"; fi

# ── S2 — nothing the S3 lifecycle EXECUTES resurrects the superseded LocalStack backend ───────────
# Comments are stripped so historical rationale can name LocalStack without becoming runtime state.
if [ -d "$S3_DOCKER" ]; then
  s2_hits=""
  for f in "$S3_DOCKER"/*.sh; do
    [ -f "$f" ] || continue
    sed -E 's/(^|[[:space:]])#.*$//' "$f" | grep -iq 'localstack' && s2_hits="$s2_hits $(basename "$f")"
  done
  # The effective values must not name the superseded backend either.
  for v in DOCKER_IMAGE CONTAINER_NAME VOLUME_MOUNT HOSTNAME; do
    printf '%s' "$(env_var "$S3_ENV" "$v" 2>/dev/null)" | grep -iq 'localstack' && s2_hits="$s2_hits \$$v"
  done
  [ -z "$s2_hits" ] && ok "S2 nothing the S3 lifecycle executes references LocalStack" \
    || bad "S2 executable LocalStack references remain in the S3 lifecycle:$s2_hits"
else bad "S2 $S3_DOCKER is absent"; fi

# ── S3 — the bind-mount lands on MinIO's data path ────────────────────────────────────────────────
s3_mount="$(env_var "$S3_ENV" VOLUME_MOUNT 2>/dev/null)"
if [ "$s3_mount" = "$EXPECTED_S3_MOUNT" ]; then ok "S3 VOLUME_MOUNT=$EXPECTED_S3_MOUNT (MinIO data path)"
else bad "S3 VOLUME_MOUNT='$s3_mount' (expected $EXPECTED_S3_MOUNT)"; fi

# ── S4 — run-s3.sh publishes MinIO's API port and carries no LocalStack service selector ──────────
s4_port="$(published_port "$S3_RUN" "$EXPECTED_S3_PORT" 2>/dev/null)"
s4_services="$(env_var "$S3_ENV" LOCALSTACK_SERVICES 2>/dev/null)"
if [ "$s4_port" = "$EXPECTED_S3_PORT" ] && [ -z "$s4_services" ]; then
  ok "S4 run-s3.sh publishes ${EXPECTED_S3_PORT}:${EXPECTED_S3_PORT} and carries no stale LOCALSTACK_SERVICES"
else bad "S4 published API port='$s4_port' (expected $EXPECTED_S3_PORT), stale LOCALSTACK_SERVICES='$s4_services'"; fi

# ── S5 — run-s3.sh goes through the shared lifecycle contract, not a bare detached run ────────────
# A `docker run --detach` exits 0 even when the container dies on the next tick (the S07/internal work item 522 lesson).
# All three helpers must be present or "started" is not proof of anything.
if [ -r "$S3_RUN" ] \
   && grep -q 'appbase_require_image' "$S3_RUN" \
   && grep -q 'appbase_verify_running' "$S3_RUN" \
   && grep -q 'appbase_wait_ready' "$S3_RUN"; then
  ok "S5 run-s3.sh uses require-image + verify-running + wait-ready (not a bare detached run)"
else bad "S5 run-s3.sh does not go through the shared lifecycle contract"; fi

# ── S6 — the readiness probe targets the IPv4 loopback LITERALLY ──────────────────────────────────
# `--publish` binds the IPv4 loopback; on a host where `localhost` resolves to ::1 first, a bash
# /dev/tcp probe of `localhost` misses the published port (the postgres lesson, N01).
if grep -qE 'appbase_tcp_probe[[:space:]]+127\.0\.0\.1[[:space:]]+'"$EXPECTED_S3_PORT" "$S3_RUN" 2>/dev/null; then
  ok "S6 readiness probes 127.0.0.1:$EXPECTED_S3_PORT (not 'localhost', which may resolve to ::1)"
else bad "S6 readiness does not probe 127.0.0.1:$EXPECTED_S3_PORT"; fi

# ── S7 — the readiness budget is env-tunable so a failure path is not a multi-minute sleep ────────
if grep -qE '^:[[:space:]]*"\$\{S3_READY_RETRIES:=' "$S3_RUN" 2>/dev/null \
   && grep -qE 'S3_READY_INTERVAL:=' "$S3_RUN" 2>/dev/null; then
  ok "S7 readiness budget is env-tunable (S3_READY_RETRIES / S3_READY_INTERVAL)"
else bad "S7 readiness budget is hardcoded — a failing probe cannot be bounded by a caller"; fi

# ── S8 — failed container removal preserves local data; successful removal cleans it ─────────────
sandbox_reset rm_fail; mkdir -p "$SANDBOX_HOME/Temporary/Data/MinIO"; touch "$SANDBOX_HOME/Temporary/Data/MinIO/sentinel"
if sandbox_run remove-s3.sh || [ ! -f "$SANDBOX_HOME/Temporary/Data/MinIO/sentinel" ]; then
  bad "S8 failed Docker removal was masked or DATA_DIR was not preserved"
else ok "S8 failed removal stays non-zero and preserves DATA_DIR"; fi
sandbox_reset ready; mkdir -p "$SANDBOX_HOME/Temporary/Data/MinIO"; touch "$SANDBOX_HOME/Temporary/Data/MinIO/sentinel"
if sandbox_run remove-s3.sh && [ ! -e "$SANDBOX_HOME/Temporary/Data/MinIO" ]; then
  ok "S8a successful/absent removal deletes DATA_DIR"
else bad "S8a successful/absent removal did not clean DATA_DIR"; fi
sandbox_reset ready; touch "$SANDBOX_HOME/sentinel"
if HOME='' PATH="$SANDBOX_BIN:$PATH" bash "$SANDBOX_S3/remove-s3.sh" >"$TMP/sandbox.out" 2>"$TMP/sandbox.err" \
   || [ ! -f "$SANDBOX_HOME/sentinel" ] || ! grep -q 'refusing unsafe DATA_DIR' "$TMP/sandbox.err"; then
  bad "S8b empty HOME was accepted, omitted its diagnostic, or removed a sentinel"
else ok "S8b empty HOME is rejected without deletion"; fi

# A lifecycle script under test is untrusted input to this fake. A malicious volume must not let the
# harness delete outside its own canonical sandbox root.
mkdir -p "$TMP/outside-sentinel"; touch "$TMP/outside-sentinel/keep"
ln -s "$TMP/outside-sentinel" "$SANDBOX_ROOT/escape-link"
s8c_bad=0
for unsafe_mount in \
    "$TMP/outside-sentinel" \
    "$SANDBOX_ROOT/../outside-sentinel" \
    "$SANDBOX_ROOT" \
    "$SANDBOX_ROOT/escape-link"; do
  if S3_SANDBOX_MODE=ready S3_DOCKER_LOG="$SANDBOX_LOG" S3_PROBE_LOG="$SANDBOX_PROBE" \
      "$SANDBOX_BIN/docker" run --volume "$unsafe_mount:/appbase-cleanup" \
        --entrypoint /bin/sh "$EXPECTED_S3_IMAGE" -c true >/dev/null 2>&1; then
    s8c_bad=1
  fi
done
if [ "$s8c_bad" -eq 0 ] && [ -f "$TMP/outside-sentinel/keep" ]; then
  ok "S8c fake Docker refuses cleanup outside the canonical sandbox root"
else bad "S8c fake Docker deleted or accepted cleanup outside the canonical sandbox root"; fi

# ── S9 — the lifecycle set is complete and executable ─────────────────────────────────────────────
s9_missing=""
for s in env-s3.sh run-s3.sh start-s3.sh stop-s3.sh remove-s3.sh exec-s3.sh; do
  [ -f "$S3_DOCKER/$s" ] || { s9_missing="$s9_missing $s(absent)"; continue; }
  [ -x "$S3_DOCKER/$s" ] || s9_missing="$s9_missing $s(not-executable)"
done
[ -z "$s9_missing" ] && ok "S9 the S3 lifecycle set is complete and executable" || bad "S9 lifecycle set:$s9_missing"

# ── S10 — every sibling script sources env-s3.sh, so the pin has exactly one home ─────────────────
s10_bad=""
for s in run-s3.sh start-s3.sh stop-s3.sh remove-s3.sh exec-s3.sh; do
  [ -r "$S3_DOCKER/$s" ] || { s10_bad="$s10_bad $s(absent)"; continue; }
  grep -q 'env-s3.sh' "$S3_DOCKER/$s" || s10_bad="$s10_bad $s"
done
[ -z "$s10_bad" ] && ok "S10 every S3 script sources env-s3.sh (one home for the pin and the container name)" \
  || bad "S10 these S3 scripts do not source env-s3.sh:$s10_bad"

# ── S11 — the MinIO API port is published to the LOOPBACK ONLY ────────────────────────────────────
# These are static emulator credentials and do not prove IAM. A default `--publish 9000:9000` would
# put a read/write S3-compatible API on every interface, so THE BINDING IS THE ACCESS CONTROL.
# Comments are stripped for the same reason published_port strips them: the rationale quotes the wide
# form it rejects, and a guard that read the quotation would reject the very fix it checks for.
s11_publishes="$(recorded_publishes "$S3_RUN")"
if [ "$s11_publishes" = "DOCKER_RUN
127.0.0.1:${EXPECTED_S3_PORT}:${EXPECTED_S3_PORT}" ]; then
  ok "S11 exactly one publish maps the MinIO API port to 127.0.0.1 only"
else bad "S11 unsafe or additional Docker publish option(s): $(printf '%s' "$s11_publishes" | tr '\n' ' ')"; fi

# ── S12/S13 — real behavior of start/run lifecycle and spaced argv ────────────────────────────────
sandbox_reset stopped
if sandbox_run start-s3.sh || ! grep -q 'did not stay running' "$TMP/sandbox.err" || [ -e "$SANDBOX_PROBE" ]; then
  bad "S12 start did not reject a stopped container before readiness"
else ok "S12 start rejects inspect=false before probing"; fi
sandbox_reset never
if sandbox_run start-s3.sh || ! grep -q 'readiness timed out' "$TMP/sandbox.err"; then
  bad "S12a start did not surface readiness timeout"
else ok "S12a start surfaces bounded readiness timeout"; fi
sandbox_reset ready
if sandbox_run start-s3.sh && grep -qx "127.0.0.1:$EXPECTED_S3_PORT" "$SANDBOX_PROBE"; then
  ok "S12b start verifies running and reaches readiness"
else bad "S12b start ready path failed"; fi
sandbox_reset ready
if sandbox_run run-s3.sh && grep -qx "$SANDBOX_HOME/Temporary/Data/MinIO:$EXPECTED_S3_MOUNT" "$SANDBOX_LOG"; then
  ok "S13 spaced HOME produces one exact volume argv element"
else bad "S13 spaced HOME split or changed Docker argv"; fi

# ═════════════════════════════════════════════════════════════════════════════════════════════════
# TIER W — the shared orchestrators actually reach the new backend
# ═════════════════════════════════════════════════════════════════════════════════════════════════
echo "TIER W — shared-lifecycle wiring"

# ── W0 — CHECK THE CHECK, BOTH WAYS: the wiring pattern must reject AND accept ────────────────────
# ⚠️ This arm was one-sided on its first draft — it proved only that the pattern does not match an
# orchestrator WITHOUT the S3 child, which a pattern matching NOTHING AT ALL also satisfies. A negative
# control alone cannot tell a working guard from a dead one; the positive control carries the weight.
cat > "$TMP/run-all-bad.sh" <<'BAD'
#!/bin/bash
cd $POSTGRESQL_DIR
$POSTGRESQL_DIR/run-postgresql.sh || rc=1
BAD
cat > "$TMP/run-all-good.sh" <<'GOOD'
#!/bin/bash
cd $S3_DIR
$S3_DIR/run-s3.sh || rc=1
GOOD
w0_neg=1; w0_pos=0
grep -qE '^\$S3_DIR/run-s3\.sh' "$TMP/run-all-bad.sh"  && w0_neg=0
grep -qE '^\$S3_DIR/run-s3\.sh' "$TMP/run-all-good.sh" && w0_pos=1
if [ "$w0_neg" = "1" ] && [ "$w0_pos" = "1" ]; then
  ok "W0 check-the-check: the wiring pattern rejects an S3-less orchestrator AND matches one that has it"
else bad "W0 wiring pattern is not two-sided (rejects-bad=$w0_neg matches-good=$w0_pos)"; fi

# ── W1 — common-all.sh defines S3_DIR and still defines no MINIO_DIR ──────────────────────────────
w1_s3dir="$(common_var S3_DIR 2>/dev/null)"
w1_minio="$(common_var MINIO_DIR 2>/dev/null)"
if [ -n "$w1_s3dir" ] && [ -z "$w1_minio" ]; then
  ok "W1 common-all.sh defines S3_DIR and defines no MINIO_DIR"
else bad "W1 S3_DIR='$w1_s3dir' (expected non-empty), MINIO_DIR='$w1_minio' (expected empty)"; fi

# ── W2 — S3_DIR resolves to a directory that EXISTS ───────────────────────────────────────────────
# This is the exact class of bug N01 fixed: an orchestrator naming a path that no longer exists.
if [ -n "$w1_s3dir" ] && [ -d "$w1_s3dir" ]; then ok "W2 S3_DIR resolves to an existing directory ($w1_s3dir)"
else bad "W2 S3_DIR='$w1_s3dir' does not resolve to an existing directory"; fi

# ── W3..W6 — each orchestrator invokes the S3 child through $S3_DIR ───────────────────────────────
w_n=3
for pair in "run-all:run-s3" "start-all:start-s3" "stop-all:stop-s3" "remove-all:remove-s3"; do
  orch="docker-${pair%%:*}.sh"; child="${pair##*:}.sh"
  if grep -qE '^\$S3_DIR/'"${child%.sh}"'\.sh' "$CORE_DOCKER/$orch" 2>/dev/null; then
    ok "W$w_n $orch invokes \$S3_DIR/$child"
  else bad "W$w_n $orch does not invoke \$S3_DIR/$child — S3 is silently skipped"; fi
  w_n=$((w_n+1))
done

# ── W7 — every directory variable the orchestrators name resolves to a real directory ─────────────
# Reads the *_DIR variables out of common-all.sh and asserts each one exists, so the manifest as a
# whole stays honest rather than only the row this task added.
w7_missing=""
while IFS= read -r v; do
  val="$(common_var "$v")"
  [ -n "$val" ] && [ -d "$val" ] || w7_missing="$w7_missing $v"
done < <(grep -oE '^[A-Z0-9_]+_DIR=' "$COMMON_ALL" 2>/dev/null | tr -d '=' )
[ -z "$w7_missing" ] && ok "W7 every *_DIR in common-all.sh resolves to an existing directory" \
  || bad "W7 common-all.sh names non-existent directories:$w7_missing"

# ── W8 — the S3 child is reached under the SAME best-effort contract as its siblings ──────────────
# The orchestrators accumulate `|| rc=1`; an S3 line without it would let an S3 failure read as success.
if grep -qE '^\$S3_DIR/run-s3\.sh \|\| rc=1' "$CORE_DOCKER/docker-run-all.sh" 2>/dev/null; then
  ok "W8 docker-run-all.sh accumulates the S3 child's failure (|| rc=1), like every sibling"
else bad "W8 docker-run-all.sh does not accumulate the S3 child's exit status"; fi

# ═════════════════════════════════════════════════════════════════════════════════════════════════
# TIER C — the client config points at the endpoint the lifecycle PUBLISHES
# ═════════════════════════════════════════════════════════════════════════════════════════════════
echo "TIER C — S3 client config agrees with the lifecycle"

# ── C0 — CHECK THE CHECK: the agreement guard must REJECT the superseded LocalStack endpoint ──────
c0_dead="http://localhost:${SUPERSEDED_LOCALSTACK_PORT}"
if [ "$(uri_port "$c0_dead")" = "$EXPECTED_S3_PORT" ]; then
  bad "C0 the agreement guard ACCEPTED the superseded LocalStack endpoint (guard is vacuous)"
else ok "C0 check-the-check: the agreement guard rejects the superseded LocalStack endpoint ($c0_dead)"; fi

c0_off_loopback="http://attacker.example:${EXPECTED_S3_PORT}"
if s3_endpoint_ok "$c0_off_loopback"; then
  bad "C0a the agreement guard ACCEPTED an off-loopback endpoint ($c0_off_loopback)"
else ok "C0a the agreement guard rejects an off-loopback endpoint"; fi

# ── C1 — the IT's endpoint port EQUALS the port run-s3.sh publishes (cross-file agreement) ────────
c1_ep="$(yaml_endpoint "$S3_YAML" 2>/dev/null)"; c1_port="$(uri_port "$c1_ep")"
c1_pub="$(published_port "$S3_RUN" "$EXPECTED_S3_PORT" 2>/dev/null)"
if [ "$c1_pub" = "$EXPECTED_S3_PORT" ] && s3_endpoint_ok "$c1_ep"; then
  ok "C1 appbase-client-aws-s3.yaml uses the owner-approved endpoint $EXPECTED_S3_ENDPOINT"
else bad "C1 endpoint='$c1_ep' must equal '$EXPECTED_S3_ENDPOINT' and published port is '$c1_pub'"; fi

# ── C2 — the IT's endpoint is not the superseded LocalStack edge port ─────────────────────────────
if [ "$c1_port" != "$SUPERSEDED_LOCALSTACK_PORT" ]; then
  ok "C2 the IT endpoint is not the superseded LocalStack port :$SUPERSEDED_LOCALSTACK_PORT"
else bad "C2 the IT endpoint still points at superseded LocalStack :$SUPERSEDED_LOCALSTACK_PORT"; fi

# ── C3 — path-style addressing stays on for the loopback MinIO endpoint ───────────────────────────
if grep -qE '^[[:space:]]*pathStyleAccessEnabled:[[:space:]]*true' "$S3_YAML" 2>/dev/null; then
  ok "C3 pathStyleAccessEnabled: true — buckets addressed by path, which is what the emulator serves"
else bad "C3 pathStyleAccessEnabled is not true — vhost-style addressing needs per-bucket DNS the emulator has none of"; fi

# ── C4 — the stacktest S3 config agrees with the same endpoint (no second, stale truth) ───────────
c4_ep="$(yaml_endpoint "$STACKTEST_YAML" 2>/dev/null)"; c4_port="$(uri_port "$c4_ep")"
if s3_endpoint_ok "$c4_ep" && [ "$c4_ep" = "$c1_ep" ]; then
  ok "C4 appbase-stacktest-aws-s3.yaml agrees on exact endpoint $EXPECTED_S3_ENDPOINT"
else bad "C4 stacktest endpoint='$c4_ep' disagrees with owner-approved endpoint '$EXPECTED_S3_ENDPOINT'"; fi

# ── C5 — both clients use S3's constraint-free default region ────────────────────────────────────
# The SDK maps any other region into CreateBucketConfiguration.LocationConstraint. The historical
# `dev-region` is not an AWS region and can produce InvalidLocationConstraint on S3-compatible servers.
if grep -qE "^[[:space:]]*id:[[:space:]]*\"$EXPECTED_S3_REGION\"" "$S3_YAML" \
    && grep -qE "^[[:space:]]*id:[[:space:]]*\"$EXPECTED_S3_REGION\"" "$STACKTEST_YAML"; then
  ok "C5 isolated and stacktest S3 clients use S3-valid region $EXPECTED_S3_REGION"
else bad "C5 S3 client region can produce an invalid create-bucket location constraint"; fi

# ═════════════════════════════════════════════════════════════════════════════════════════════════
# TIER A — the Firebase Auth emulator host satisfies the Admin SDK's URL contract
# ═════════════════════════════════════════════════════════════════════════════════════════════════
echo "TIER A — Firebase Auth emulator host composes to a reachable URL"

# The EXACT template firebase-admin 9.3.0 uses when FIREBASE_AUTH_EMULATOR_HOST is set:
#   FirebaseUserManager.ID_TOOLKIT_URL_EMULATOR = "http://%s/identitytoolkit.googleapis.com/%s/projects/%s"
#   fed by Utils.getEmulatorHost(), which returns the raw env value verbatim.
# compose_emulator_url HOSTVALUE -> the URL the SDK would actually build.
compose_emulator_url() { printf 'http://%s/identitytoolkit.googleapis.com/v1/projects/appbase-it' "$1"; }

# emulator_host_ok HOSTVALUE EXPECTED_PORT — the Tier-A guard. A value is acceptable only when the
# COMPOSED url has exactly one scheme, no empty path segment, and an authority on EXPECTED_PORT.
emulator_host_ok() {
  local value="$1" want_port="$2" url authority port
  case "$value" in localhost:"$want_port"|127.0.0.1:"$want_port") ;; *) return 1 ;; esac
  url="$(compose_emulator_url "$value")"
  # exactly one "://" in the composed URL — a value carrying its own scheme doubles it
  [ "$(printf '%s' "$url" | grep -o '://' | wc -l | tr -d ' ')" = "1" ] || return 1
  # no empty path segment — a value with a trailing slash produces "…:4000//identitytoolkit…"
  printf '%s' "${url#http://}" | grep -q '//' && return 1
  authority="$(printf '%s' "${url#http://}" | sed -E 's#/.*$##')"
  port="$(printf '%s' "$authority" | awk -F: 'NF>1{print $NF}')"
  [ "$port" = "$want_port" ] || return 1
  return 0
}

# The auth port is read from the repo's OWN firebase.json, not typed here — move the emulator and
# these arms move with it.
A_AUTH_PORT="$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['emulators']['auth']['port'])" "$FIREBASE_JSON" 2>/dev/null)"
A_UI_PORT="$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['emulators']['ui']['port'])" "$FIREBASE_JSON" 2>/dev/null)"
A_UI_ENABLED="$(python3 -c "import json,sys; print(str(json.load(open(sys.argv[1]))['emulators']['ui']['enabled']).lower())" "$FIREBASE_JSON" 2>/dev/null)"
A_AUTH_HOST="$(maven_root_property "$ROOT_POM" firebase.auth.emulator.host 2>/dev/null)"

cat > "$TMP/root-property-decoy.xml" <<'XML'
<project>
  <!-- <firebase.auth.emulator.host>attacker.example:9099</firebase.auth.emulator.host> -->
  <profiles><profile><properties>
    <firebase.auth.emulator.host>inactive.example:9099</firebase.auth.emulator.host>
  </properties></profile></profiles>
  <properties><firebase.auth.emulator.host>localhost:9099</firebase.auth.emulator.host></properties>
</project>
XML
a3_decoy="$(maven_root_property "$TMP/root-property-decoy.xml" firebase.auth.emulator.host 2>/dev/null)"
if [ "$a3_decoy" = "localhost:9099" ]; then
  ok "A9 root-property reader ignores comments and inactive profiles"
else bad "A9 root-property reader selected decoy '$a3_decoy'"; fi

# ── A0 — CHECK THE CHECK: the guard must REJECT the value that shipped ────────────────────────────
# "http://localhost:4000/" composes to
#   http://http://localhost:4000//identitytoolkit.googleapis.com/v1/projects/…
# — a doubled scheme, an empty path segment, and the Emulator UI port rather than the Auth port.
if emulator_host_ok "http://localhost:4000/" "$A_AUTH_PORT"; then
  bad "A0 the Tier-A guard ACCEPTED 'http://localhost:4000/' (guard is vacuous)"
else ok "A0 check-the-check: the guard rejects 'http://localhost:4000/' (doubled scheme + UI port)"; fi

if emulator_host_ok "attacker.example:$A_AUTH_PORT" "$A_AUTH_PORT"; then
  bad "A7 the guard accepted a non-loopback emulator host"
else ok "A7 the guard rejects a non-loopback emulator host"; fi

# ── A1 — CHECK THE CHECK, positive control: a correct value must be ACCEPTED ──────────────────────
if [ -n "$A_AUTH_PORT" ] && emulator_host_ok "localhost:$A_AUTH_PORT" "$A_AUTH_PORT"; then
  ok "A1 positive control: the guard accepts 'localhost:$A_AUTH_PORT'"
else bad "A1 the guard rejected a correct value 'localhost:$A_AUTH_PORT' (guard rejects everything)"; fi

# ── A2 — firebase.json declares the auth emulator, and the UI is on a DIFFERENT port ──────────────
if [ -n "$A_AUTH_PORT" ] && [ -n "$A_UI_PORT" ] && [ "$A_AUTH_PORT" != "$A_UI_PORT" ]; then
  ok "A2 firebase.json declares auth:$A_AUTH_PORT distinct from ui:$A_UI_PORT (the two are not interchangeable)"
else bad "A2 firebase.json auth port='$A_AUTH_PORT' ui port='$A_UI_PORT' — unreadable or indistinguishable"; fi

if [ "$A_UI_ENABLED" = "false" ]; then
  ok "A8 headless auth container disables the optional Emulator UI download"
else bad "A8 firebase.json enables the optional Emulator UI and its runtime download"; fi

# ── A3 — EVERY FIREBASE_AUTH_EMULATOR_HOST in the root pom composes to a reachable URL ────────────
a3_bad=""; a3_n=0
while IFS= read -r value; do
  a3_n=$((a3_n+1))
  resolved="$value"
  [ "$value" = '${firebase.auth.emulator.host}' ] && resolved="$A_AUTH_HOST"
  emulator_host_ok "$resolved" "$A_AUTH_PORT" || a3_bad="$a3_bad [$value -> $resolved]"
done < <(grep -oE '<FIREBASE_AUTH_EMULATOR_HOST>[^<]*</FIREBASE_AUTH_EMULATOR_HOST>' "$ROOT_POM" \
         | sed -E 's#</?FIREBASE_AUTH_EMULATOR_HOST>##g')
if [ "$a3_n" -eq 4 ] && [ -z "$a3_bad" ]; then
  ok "A3 all $a3_n FIREBASE_AUTH_EMULATOR_HOST values compose to a well-formed URL on the auth port $A_AUTH_PORT"
elif [ "$a3_n" -ne 4 ]; then bad "A3 expected 4 FIREBASE_AUTH_EMULATOR_HOST values, found $a3_n"
else bad "A3 these FIREBASE_AUTH_EMULATOR_HOST values do not compose to a reachable URL:$a3_bad"; fi

# ── A4 — the occurrences AGREE with each other (no per-profile drift) ─────────────────────────────
a4_distinct="$(grep -oE '<FIREBASE_AUTH_EMULATOR_HOST>[^<]*</FIREBASE_AUTH_EMULATOR_HOST>' "$ROOT_POM" \
               | sed -E 's#</?FIREBASE_AUTH_EMULATOR_HOST>##g' | sort -u | wc -l | tr -d ' ')"
if [ "$a4_distinct" = "1" ]; then ok "A4 every profile carries the SAME emulator host (no per-profile drift)"
else bad "A4 $a4_distinct distinct FIREBASE_AUTH_EMULATOR_HOST values across the profiles"; fi

# ── A5 — run-firebase.sh publishes the auth port firebase.json declares ───────────────────────────
a5_pub="$(published_port "$FIREBASE_RUN" "$A_AUTH_PORT" 2>/dev/null)"
if [ -n "$A_AUTH_PORT" ] && [ "$a5_pub" = "$A_AUTH_PORT" ]; then
  ok "A5 run-firebase.sh publishes the auth port $A_AUTH_PORT that firebase.json declares"
else bad "A5 run-firebase.sh does not publish the declared auth port $A_AUTH_PORT (published '$a5_pub')"; fi

# ── A6 — the container actually starts the AUTH emulator ──────────────────────────────────────────
# `firebase emulators:start --only auth` is what makes :9099 exist at all; without it the published
# port is an open socket to nothing.
if grep -qE 'emulators:start.*--only[[:space:]]+auth' "$FIREBASE_ENTRYPOINT" 2>/dev/null; then
  ok "A6 the container entrypoint starts the auth emulator (--only auth)"
else bad "A6 the container entrypoint does not start the auth emulator"; fi

echo "appbase_emulator_infra: $PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ] || exit 1
# anti-vacuity: a run that exercised nothing must not read as success. EVERY arm in this suite is
# daemon-free and unconditional, so the result must equal the exact arm count:
#   TIER S 19 + TIER W 9 (W0–W8) + TIER C 7 (C0–C5 plus C0a) + TIER A 10 = 45.
# ⚠️ ADD AN ARM → RAISE THIS NUMBER AND THE ARITHMETIC ABOVE IT. A floor left at the old number lets
# exactly as many arms vanish silently as were added.
[ "$PASSED" -eq 45 ] || { echo "REFUSING TO REPORT SUCCESS — expected 45 arms, saw $PASSED"; exit 3; }
