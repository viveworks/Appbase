#!/usr/bin/env bash
#
# Lightweight contract for the Aerospike test container (internal work item 2650). The live proof remains a hosted
# Docker build/start/readiness run; this offline floor pins the reviewed upstream artifact and drives
# the lifecycle wrappers through an enforcing fake Docker CLI.
#
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
AEROSPIKE_DIR="$ROOT/appbase-java/appbase-client/appbase-client-aerospike/infra/docker/aerospike"
DOCKERFILE="$AEROSPIKE_DIR/Dockerfile"
EXPECTED_IMAGE='aerospike/aerospike-server:8.2.0.0@sha256:583e882d540f4ba813b67f2167452b4893f025760b7541d2d9a8b5107a2f0324'
PASS=0
FAIL=0

ok() { PASS=$((PASS + 1)); echo "ok   $*"; }
bad() { FAIL=$((FAIL + 1)); echo "FAIL $*"; }
refuse() { echo "aerospike-container-contract: REFUSING: $*" >&2; exit 2; }

allocate_work_dir() {
  local command template expected_parent expected_parent_physical candidate candidate_physical probe
  command="${AEROSPIKE_MKTEMP_COMMAND:-mktemp}"
  [ -n "$command" ] || refuse "empty mktemp command"
  template="/tmp/appbase-aerospike-contract.XXXXXX"
  expected_parent="${template%/*}"
  expected_parent_physical="$(cd "$expected_parent" 2>/dev/null && pwd -P)" \
    || refuse "temporary parent is unavailable: $expected_parent"
  candidate="$("$command" -d "$template")" \
    || refuse "mktemp command failed"
  [ -n "$candidate" ] || refuse "mktemp returned an empty path"
  [ -d "$candidate" ] || refuse "mktemp path is not a directory: $candidate"
  candidate_physical="$(cd "$candidate" 2>/dev/null && pwd -P)" \
    || refuse "mktemp path cannot be resolved: $candidate"
  case "$candidate_physical" in
    "$expected_parent_physical"/appbase-aerospike-contract.*) ;;
    *) refuse "mktemp returned a misdirected path: $candidate" ;;
  esac
  [ -w "$candidate_physical" ] && [ -x "$candidate_physical" ] \
    || refuse "mktemp path is unusable: $candidate"
  [ ! -e "$candidate_physical/bin" ] && [ ! -L "$candidate_physical/bin" ] \
    || refuse "mktemp path contains a pre-existing bin entry: $candidate"
  probe="$candidate_physical/.allocation-probe"
  mkdir "$probe" 2>/dev/null || refuse "mktemp path cannot create a probe directory: $candidate"
  rmdir "$probe" 2>/dev/null || refuse "mktemp path cannot remove its probe directory: $candidate"
  TMP="$candidate_physical"
  trap 'rm -rf "$TMP"' EXIT
  mkdir "$TMP/bin" || refuse "cannot create isolated shim directory: $TMP/bin"
}

if [ "${AEROSPIKE_ALLOCATION_ONLY:-0}" = 1 ]; then
  allocate_work_dir
  exit 0
fi

linkage_guard_is_fail_closed() {
  awk '
    /^[[:space:]]*#/ { next }
    /\|\|[[:space:]]*true/ { swallowing=1 }
    /if[[:space:]]+ldd .*grep -q .not found.;[[:space:]]*then/ { in_guard=1; saw_if=1 }
    in_guard && /exit[[:space:]]+1;/ { saw_exit=1 }
    in_guard && /^[[:space:]]*fi;/ { saw_fi=1; in_guard=0 }
    END { exit(saw_if && saw_exit && saw_fi && !swallowing ? 0 : 1) }
  ' "$1"
}

from_image="$(awk '$1 == "FROM" { print $2; exit }' "$DOCKERFILE")"
if [ "$from_image" = "$EXPECTED_IMAGE" ]; then
  ok "Dockerfile pins the reviewed Aerospike 8.2.0.0 OCI index digest"
else
  bad "Dockerfile FROM is '$from_image', expected '$EXPECTED_IMAGE'"
fi

if grep -qE 'debian10|aerospike-server-community-5\.6\.0\.8' "$DOCKERFILE"; then
  bad "Dockerfile still installs the incompatible Aerospike 5.6 Debian 10 package"
else
  ok "Dockerfile no longer composes Debian 10 packages onto a newer base"
fi

if linkage_guard_is_fail_closed "$DOCKERFILE" && grep -q 'asd --version' "$DOCKERFILE"; then
  ok "image build has a fail-closed if/then/exit-1 linkage guard and executable smoke check"
else
  bad "image build must retain a fail-closed linkage guard and asd --version smoke check"
fi

if awk '!/^[[:space:]]*#/' "$DOCKERFILE" | grep -qE '\|\|[[:space:]]*true'; then
  bad "linkage guard must not swallow its own failure with || true"
else
  ok "linkage guard contains no active || true failure swallowing"
fi

allocate_work_dir
CALLS="$TMP/docker-calls"

# Failure injection runs the allocation path in isolation. A failing allocator must exit nonzero
# before the Docker shim write, and must leave the system /bin/docker path byte-for-byte unchanged
# (or absent if it was absent before the probe).
FAILED_MKTEMP="$TMP/failed-mktemp"
cat > "$FAILED_MKTEMP" <<'FAILED'
#!/usr/bin/env bash
exit 71
FAILED
chmod +x "$FAILED_MKTEMP"
if [ -e /bin/docker ] || [ -L /bin/docker ]; then
  BIN_DOCKER_BEFORE="$(ls -ld /bin/docker 2>/dev/null; cksum /bin/docker 2>/dev/null)"
else
  BIN_DOCKER_BEFORE='ABSENT'
fi
if AEROSPIKE_ALLOCATION_ONLY=1 AEROSPIKE_MKTEMP_COMMAND="$FAILED_MKTEMP" \
    bash "$0" >/dev/null 2>&1; then
  FAILED_ALLOCATION_RC=0
else
  FAILED_ALLOCATION_RC=$?
fi
if [ -e /bin/docker ] || [ -L /bin/docker ]; then
  BIN_DOCKER_AFTER="$(ls -ld /bin/docker 2>/dev/null; cksum /bin/docker 2>/dev/null)"
else
  BIN_DOCKER_AFTER='ABSENT'
fi
if [ "$FAILED_ALLOCATION_RC" -ne 0 ] && [ "$BIN_DOCKER_AFTER" = "$BIN_DOCKER_BEFORE" ]; then
  ok "failed temp allocation exits nonzero before any write to /bin/docker"
else
  bad "failed temp allocation was accepted or changed /bin/docker (rc=$FAILED_ALLOCATION_RC)"
fi

BROKEN_GUARD="$TMP/broken-linkage-guard.Dockerfile"
cat > "$BROKEN_GUARD" <<'BROKEN'
FROM example.invalid/aerospike
RUN (ldd /usr/bin/asd 2>/dev/null | grep 'not found' && exit 1 || true) \
    && asd --version
BROKEN
if linkage_guard_is_fail_closed "$BROKEN_GUARD"; then
  bad "mutation: known failure-swallowing linkage guard was accepted"
else
  ok "mutation: known && exit 1 || true guard is rejected"
fi

cat > "$TMP/bin/docker" <<'FAKE'
#!/usr/bin/env bash
printf 'BUILDKIT_PROGRESS=%s ' "${BUILDKIT_PROGRESS:-}" >> "${AEROSPIKE_FAKE_CALLS:?}"
printf '%q ' "$@" >> "${AEROSPIKE_FAKE_CALLS:?}"
printf '\n' >> "$AEROSPIKE_FAKE_CALLS"
FAKE
chmod +x "$TMP/bin/docker"

if AEROSPIKE_FAKE_CALLS="$CALLS" PATH="$TMP/bin:/usr/bin:/bin" \
    bash "$AEROSPIKE_DIR/build-aerospike.sh"; then
  if grep -Fq "BUILDKIT_PROGRESS=plain build -t viveworks.com/infra/local/aerospike $AEROSPIKE_DIR" "$CALLS"; then
    ok "build wrapper emits portable plain progress and builds the stable local tag"
  else
    bad "build wrapper did not issue the expected diagnostic docker build command"
  fi
else
  bad "build wrapper failed under the enforcing Docker recorder"
fi

: > "$CALLS"
if AEROSPIKE_FAKE_CALLS="$CALLS" PATH="$TMP/bin:/usr/bin:/bin" \
    bash "$AEROSPIKE_DIR/run-aerospike.sh"; then
  call="$(cat "$CALLS")"
  if [[ "$call" == *'--publish 3000-3002:3000-3002'* ]]; then
    ok "run wrapper publishes the Aerospike service/fabric/heartbeat port contract"
  else
    bad "run wrapper did not publish ports 3000-3002"
  fi
  if [[ "$call" == *'--env NAMESPACE=dev'* && "$call" == *'--env DATA_IN_MEMORY=true'* ]]; then
    ok "run wrapper preserves the stacktest dev namespace with in-memory storage"
  else
    bad "run wrapper must set NAMESPACE=dev and DATA_IN_MEMORY=true"
  fi
  if [[ "$call" == *'--env DEFAULT_TTL=30d'* ]]; then
    ok "run wrapper preserves the historical 30-day default TTL"
  else
    bad "run wrapper must preserve DEFAULT_TTL=30d"
  fi
  if [[ "$call" == *'viveworks.com/infra/local/aerospike'* ]]; then
    ok "run wrapper starts the stable local Aerospike tag"
  else
    bad "run wrapper did not start the stable local Aerospike tag"
  fi
else
  bad "run wrapper failed under the enforcing Docker recorder"
fi

dead_assets=0
for asset in aerospike-entrypoint.sh aerospike.template.conf aerospike-telemetry.conf; do
  [ ! -e "$AEROSPIKE_DIR/$asset" ] || dead_assets=$((dead_assets + 1))
done
if [ "$dead_assets" -eq 0 ]; then
  ok "obsolete custom entrypoint/template/telemetry assets are removed"
else
  bad "$dead_assets obsolete custom Aerospike assets remain after adopting the upstream entrypoint"
fi

echo
echo "aerospike-container-contract: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
