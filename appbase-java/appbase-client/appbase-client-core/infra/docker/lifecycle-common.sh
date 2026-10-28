#!/bin/bash
#
# lifecycle-common.sh — shared behaviour for the per-backend docker lifecycle scripts (internal work item 1291 / N01).
#
# WHY THIS EXISTS. The old `run-<backend>.sh` scripts did `docker run --detach … ${DOCKER_IMAGE}` and
# nothing else. Two failures hid behind that:
#   1. When the pinned image was deleted from Docker Hub (internal work item 502) the run failed with a raw
#      `manifest unknown`, naming neither which env file pinned it nor how to fix it.
#   2. Under the rootless devcontainer daemon (P-046 S07, internal work item 522) an image running as a non-root uid
#      cannot write a bind-mounted data dir and DIES on the next tick — yet `docker run --detach`
#      still exits 0. "Container started" was never proof the backend was up.
# This file gives the run scripts three composable, daemon-agnostic behaviours so those two failures
# become loud instead of silent. It is sourced, never executed; it defines functions and sets nothing.
#
# @author Navaneeth Mysore G

# appbase_require_image IMAGE
#   Fails, with an ACTIONABLE diagnostic, when IMAGE cannot be resolved either locally or from a
#   configured registry — before `docker run` turns it into a bare `manifest unknown`.
appbase_require_image() {
  local image="${1:?appbase_require_image: image argument required}"
  if docker image inspect "$image" >/dev/null 2>&1; then
    return 0
  fi
  if docker manifest inspect "$image" >/dev/null 2>&1; then
    return 0
  fi
  {
    echo "appbase lifecycle: image '$image' is not available locally and no registry manifest exists."
    echo "  → If the pin was replaced, run the sibling build-*.sh to pull it, or check network/registry access."
    echo "  → Deleted-image replacements and their rationale: docs/spec/1.0.0/P04/decisions/E01-image-versions.md"
  } >&2
  return 1
}

# appbase_verify_running CONTAINER [RETRIES] [INTERVAL] [STABLE]
#   After a detached run, confirm the container is actually RUNNING and did not die silently. This is a
#   TWO-PHASE check because the silent-death failure (S07/internal work item 522) is precisely a container that IS running
#   for one tick and dies on the next:
#     phase 1 — wait up to RETRIES times (default 10), INTERVAL apart (default 1), for `Running`==true;
#     phase 2 — once up, re-check STABLE more times (default 2), INTERVAL apart, and FAIL if it is no
#               longer running. A single first-tick `true` is NOT accepted as proof of a live container.
#   On failure it dumps the container log so the silent death is visible, and returns non-zero. THIS is
#   what makes a run script refuse to exit 0 on a container that started and immediately exited.
#   ⚠️ Returning on the first `true` (the original shape) could not catch a next-tick death — the exact
#   thing this function's WHY block says it exists to catch — so phase 2 is load-bearing, not belt-and-braces.
appbase_verify_running() {
  local container="${1:?appbase_verify_running: container argument required}"
  local retries="${2:-10}" interval="${3:-1}" stable="${4:-2}" i j state up=""
  for (( i = 0; i < retries; i++ )); do
    state="$(docker inspect -f '{{.State.Running}}' "$container" 2>/dev/null)"
    if [ "$state" = "true" ]; then up=1; break; fi
    sleep "$interval"
  done
  if [ -n "$up" ]; then
    for (( j = 0; j < stable; j++ )); do
      sleep "$interval"
      state="$(docker inspect -f '{{.State.Running}}' "$container" 2>/dev/null)"
      [ "$state" = "true" ] || { up=""; break; }
    done
  fi
  [ -n "$up" ] && return 0
  {
    echo "appbase lifecycle: container '$container' is not running / did not stay running (state='${state:-unknown}')."
    echo "  → A detached run exits 0 even when the container dies on the next tick (rootless non-root uid, internal work item 522)."
    echo "  → Last container log lines:"
    docker logs --tail 50 "$container" 2>&1 | sed 's/^/      /'
  } >&2
  return 1
}

# appbase_require_network NETWORK
#   Idempotently ensure a user-defined docker network exists. Container-name DNS resolution (one
#   container reaching another by name) is a USER-DEFINED-network feature — the default bridge does NOT
#   resolve names, so a master↔worker link that relies on `spark://<master-name>:7077` silently never
#   connects on the default bridge. Safe to call repeatedly; returns non-zero with a diagnostic if the
#   network can neither be found nor created.
appbase_require_network() {
  local net="${1:?appbase_require_network: network name required}"
  if docker network inspect "$net" >/dev/null 2>&1; then
    return 0
  fi
  if docker network create "$net" >/dev/null 2>&1; then
    return 0
  fi
  {
    echo "appbase lifecycle: docker network '$net' could not be found or created."
    echo "  → Containers that reach each other by name (e.g. Spark master↔worker) require a shared"
    echo "    user-defined network; the default bridge does not resolve container names."
  } >&2
  return 1
}

# appbase_spark_worker_registered MASTER_CONTAINER
#   Observable Spark worker-registration probe: succeeds iff the MASTER's log shows a worker has
#   registered. Container-running is NOT cluster-membership — the standalone worker process retries
#   master registration, so a running worker that can never resolve/reach the master (wrong or missing
#   shared network) stays "running" forever without ever joining. Used as the readiness probe that turns
#   "both containers are up" into "the worker actually joined the cluster".
appbase_spark_worker_registered() {
  local master="${1:?appbase_spark_worker_registered: master container required}"
  docker logs "$master" 2>&1 | grep -q "Registering worker"
}

# appbase_wait_ready RETRIES INTERVAL -- PROBE_CMD [ARGS...]
#   Poll PROBE_CMD until it exits 0 (backend is ready) or RETRIES is exhausted, INTERVAL seconds apart.
#   Readiness is a PROPERTY the caller supplies (a port probe, a client ping) — this owns only the
#   retry-and-fail-loudly loop, so container-up is never mistaken for backend-ready.
appbase_wait_ready() {
  local retries="${1:?appbase_wait_ready: retries required}" interval="${2:?appbase_wait_ready: interval required}"
  shift 2
  [ "${1:-}" = "--" ] && shift
  [ "$#" -ge 1 ] || { echo "appbase_wait_ready: a probe command is required after --" >&2; return 2; }
  local i
  for (( i = 0; i < retries; i++ )); do
    if "$@" >/dev/null 2>&1; then
      return 0
    fi
    sleep "$interval"
  done
  {
    echo "appbase lifecycle: backend not ready after ${retries} attempts — probe never succeeded:"
    echo "      $*"
  } >&2
  return 1
}

# appbase_tcp_probe HOST PORT
#   A dependency-free readiness probe: succeeds iff a TCP connection to HOST:PORT is accepted. Used by
#   the run scripts as the default readiness check against each backend's published host port. Deeper,
#   auth-level readiness is layered by the driver-IT tasks (N06–N09) against a live backend.
appbase_tcp_probe() {
  local host="${1:?appbase_tcp_probe: host required}" port="${2:?appbase_tcp_probe: port required}"
  ( exec 3<>"/dev/tcp/${host}/${port}" ) >/dev/null 2>&1
}

# appbase_remove_container CONTAINER
#   Idempotent, HONEST container teardown for the remove-*.sh scripts. The failure it exists to stop is
#   the one internal work item 1291's reviewer reproduced: a teardown that runs `docker stop`/`docker rm` unchecked and
#   then lets a later local command's exit status stand, so the script reports success even when NOTHING
#   was removed — which silently defeats the orchestrator's child-status aggregation.
#   Contract:
#     • already absent (docker reports "No such container") → SUCCESS: removal is idempotent;
#     • present and removed                                 → SUCCESS;
#     • present but `docker rm` fails for any OTHER reason  → NON-ZERO, with the daemon's message.
#   `docker stop` is best-effort (an already-stopped/absent container is fine); the removal decides the
#   status, distinguishing "already gone" from "could not remove" by the daemon's own error text rather
#   than swallowing every error the way `docker rm … 2>/dev/null || true` would.
appbase_remove_container() {
  local c="${1:?appbase_remove_container: container required}" out
  docker stop "$c" >/dev/null 2>&1
  out="$(docker rm "$c" 2>&1)" && return 0
  case "$out" in
    *[Nn]"o such container"*) return 0 ;;   # already absent — idempotent success
  esac
  echo "appbase lifecycle: failed to remove container '$c': $out" >&2
  return 1
}

# appbase_remove_network NETWORK
#   The network mirror of appbase_remove_container: absent → success; removed → success; any other
#   `docker network rm` failure → non-zero with the daemon's message. Must be called only after every
#   attached container is gone (a network cannot be removed while a container is still attached) — which
#   is why the remove scripts tear the containers down first.
#   ⚠️ The absent network is reported with DIFFERENT text across engines: the CLI carries
#   "No such network: <n>", while dockerd/libnetwork returns "network <n> not found". BOTH are accepted as
#   idempotent success — but the match is ANCHORED TO THIS NETWORK'S OWN NAME ($n), not a bare "not found"
#   or a driver-agnostic "network … not found" substring. That anchoring is load-bearing, because several
#   REAL failures that are NOT this network's absence also carry "not found" and MUST be surfaced:
#     • "docker: command not found"                          (the CLI missing from $PATH)
#     • `context "<n>" not found`                            (a misconfigured DOCKER_CONTEXT/DOCKER_HOST)
#     • "network driver <d> not found" / "remote network driver: endpoint <e> not found"
#                                                            (a plugin/remote-driver teardown failure —
#       real dockerd 29.6.2 format strings, reachable at `network rm` time for a plugin-backed network)
#   None of those embed "network $n not found" or "No such network: $n", so all are surfaced. (Verified
#   against docker 29.6.2; the bare / driver-agnostic over-match was caught by the internal work item 1291 adversarial
#   teardown review — two independent passes — and the anchoring is covered by Tier F6/F7.)
appbase_remove_network() {
  local n="${1:?appbase_remove_network: network required}" out
  out="$(docker network rm "$n" 2>&1)" && return 0
  case "$out" in
    *[Nn]"o such network: $n"*|*"network $n not found"*) return 0 ;;   # THIS network already absent (CLI or libnetwork phrasing) — idempotent
  esac
  echo "appbase lifecycle: failed to remove network '$n': $out" >&2
  return 1
}

# appbase_remove_volume VOLUME
#   Idempotent named-volume teardown. Accept only Docker's absence messages anchored to this exact
#   volume name; missing CLI/context, attached-volume, permission, and daemon failures remain errors.
appbase_remove_volume() {
  local v="${1:?appbase_remove_volume: volume required}" out
  out="$(docker volume rm "$v" 2>&1)" && return 0
  case "$out" in
    *[Nn]"o such volume: $v"*|*"volume $v not found"*|*"get $v: no such volume"*) return 0 ;;
  esac
  echo "appbase lifecycle: failed to remove volume '$v': $out" >&2
  return 1
}
