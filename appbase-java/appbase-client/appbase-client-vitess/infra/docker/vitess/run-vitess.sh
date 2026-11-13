#!/bin/bash

set -euo pipefail

BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source "$BASEDIR/env-vitess.sh"
source "$BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh" || { echo "run-vitess: cannot source lifecycle-common.sh" >&2; exit 1; }

ROOT=$(cd "$BASEDIR/../../../../../.." && pwd)
STACKTEST_VITESS_DIR="$ROOT/appbase-stacktest/infra/vitess"
SERVICE_VITESS_DIR="$ROOT/appbase-java/appbase-service/infra/vitess"
GRPC_SCHEMA_DIR="$BASEDIR/scripts"
GRPC_KEYSPACE="appbase-vitess-test-keyspace"
STACKTEST_UNSHARDED_KEYSPACE="appbase-stacktest-unsharded-keyspace"
STACKTEST_KEYSPACE="appbase-stacktest-sharded-keyspace"
SCHEMA_STAGE=$(mktemp -d "${TMPDIR:-/tmp}/appbase-vitess-schema.XXXXXX")
trap 'rm -rf "$SCHEMA_STAGE"' EXIT
mkdir -p "$SCHEMA_STAGE/$GRPC_KEYSPACE" \
    "$SCHEMA_STAGE/$STACKTEST_UNSHARDED_KEYSPACE" "$SCHEMA_STAGE/$STACKTEST_KEYSPACE"
cp "$GRPC_SCHEMA_DIR/appbase-vitess-test-sharded-keyspace-schema.sql" \
    "$SCHEMA_STAGE/$GRPC_KEYSPACE/schema.sql"
cp "$GRPC_SCHEMA_DIR/appbase-vitess-test-sharded-keyspace-vschema.json" \
    "$SCHEMA_STAGE/$GRPC_KEYSPACE/vschema.json"
cp "$STACKTEST_VITESS_DIR/001-unsharded.sql" \
    "$SCHEMA_STAGE/$STACKTEST_UNSHARDED_KEYSPACE/001-stacktest.sql"
cp "$STACKTEST_VITESS_DIR/001-unsharded.json" \
    "$SCHEMA_STAGE/$STACKTEST_UNSHARDED_KEYSPACE/vschema.json"
cp "$STACKTEST_VITESS_DIR/001-sharded.sql" \
    "$SCHEMA_STAGE/$STACKTEST_KEYSPACE/001-stacktest.sql"
cp "$SERVICE_VITESS_DIR/001-sharded.sql" \
    "$SCHEMA_STAGE/$STACKTEST_KEYSPACE/002-service.sql"
cp "$STACKTEST_VITESS_DIR/001-sharded.json" \
    "$SCHEMA_STAGE/$STACKTEST_KEYSPACE/vschema.json"

# mktemp creates the staging root as 0700. That works through Docker Desktop's virtualized bind
# mounts, but on a native Linux hosted runner the image's non-root `vitess` user cannot traverse it,
# and the pre-start copy fails with `cp: cannot stat '/vt/schema-stage/.': Permission denied`.
# The mount is read-only in the container; expose only directory traversal/read and regular-file read.
chmod 0755 "$SCHEMA_STAGE" \
    "$SCHEMA_STAGE/$GRPC_KEYSPACE" \
    "$SCHEMA_STAGE/$STACKTEST_UNSHARDED_KEYSPACE" \
    "$SCHEMA_STAGE/$STACKTEST_KEYSPACE"
chmod 0644 \
    "$SCHEMA_STAGE/$GRPC_KEYSPACE/schema.sql" \
    "$SCHEMA_STAGE/$GRPC_KEYSPACE/vschema.json" \
    "$SCHEMA_STAGE/$STACKTEST_UNSHARDED_KEYSPACE/001-stacktest.sql" \
    "$SCHEMA_STAGE/$STACKTEST_UNSHARDED_KEYSPACE/vschema.json" \
    "$SCHEMA_STAGE/$STACKTEST_KEYSPACE/001-stacktest.sql" \
    "$SCHEMA_STAGE/$STACKTEST_KEYSPACE/002-service.sql" \
    "$SCHEMA_STAGE/$STACKTEST_KEYSPACE/vschema.json"

# Readiness budget is env-tunable (defaults are the production values); the lifecycle test overrides it
# so its behavioural IPv4-loopback mutation does not sleep for minutes on the failure path.
: "${VITESS_READY_RETRIES:=60}"; : "${VITESS_READY_INTERVAL:=3}"
VITESS_LAST_PROBE_ERROR="$SCHEMA_STAGE/last-probe-error.log"

# TAG is the local wrapper built by build-vitess.sh; V2b enforces that its independently pinned
# Dockerfile ARG matches DOCKER_IMAGE. Require TAG so a missing build fails with an actionable error.
appbase_require_image "${TAG}" || exit 1

############
## Ports — vttestserver derives every port from VITESS_BASE_PORT (PORT):
## ====================================================================
## host VITESS_WEB_PORT   -> container VITESS_BASE_PORT       vtcombo HTTP / status dashboard
## host VTGATE_GRPC_PORT  -> container VITESS_BASE_PORT + 1   vtgate gRPC (vitess-grpc-client)
## host VTGATE_MYSQL_PORT -> container VITESS_BASE_PORT + 3   vtgate MySQL wire = 15306
##
## 15306 is vtgate speaking the MySQL protocol — NOT MariaDB (a separate backend on 3306).
## See env-vitess.sh and README.adoc for the distinction.
############
docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --volume ${DATA_VOLUME}:${VOLUME_MOUNT} \
    --publish ${VITESS_WEB_PORT}:${VITESS_BASE_PORT} \
    --publish ${VTGATE_GRPC_PORT}:$((VITESS_BASE_PORT + 1)) \
    --publish ${VTGATE_MYSQL_PORT}:$((VITESS_BASE_PORT + 3)) \
    --volume "${SCHEMA_STAGE}:/vt/schema-stage:ro" \
    --env PORT=${VITESS_BASE_PORT} \
    --env MYSQL_BIND_HOST=0.0.0.0 \
    --env KEYSPACES=${VITESS_KEYSPACES} \
    --env NUM_SHARDS=${VITESS_NUM_SHARDS} \
    ${TAG} || exit 1

appbase_verify_running "${CONTAINER_NAME}" || exit 1

# A listening vtgate socket is not sufficient: it opens before vttestserver has installed the target
# keyspaces' SQL and VSchema. StackTest opens both the sharded customer/order keyspace and the
# unsharded product keyspace, while the service schema shares the sharded keyspace. Require a
# routed query through every one of those surfaces plus the separate gRPC TestTable keyspace so
# neither caller races initialization or receives VT05003 (unknown database in vschema) after
# bring-up reports success.
vitess_keyspace_ready() {
  local probe_exit
  if timeout 5 docker exec "${CONTAINER_NAME}" mysql \
    --host 127.0.0.1 \
    --port "$((VITESS_BASE_PORT + 3))" \
    --user root \
    --database "${STACKTEST_KEYSPACE}" \
    --batch --skip-column-names \
    --execute 'SELECT 1 FROM stacktest_customer LIMIT 1' \
    >/dev/null 2>"$VITESS_LAST_PROBE_ERROR" \
    && timeout 5 docker exec "${CONTAINER_NAME}" mysql \
      --host 127.0.0.1 \
      --port "$((VITESS_BASE_PORT + 3))" \
      --user root \
      --database "${STACKTEST_UNSHARDED_KEYSPACE}" \
      --batch --skip-column-names \
      --execute 'SELECT 1 FROM stacktest_product LIMIT 0' \
      >/dev/null 2>"$VITESS_LAST_PROBE_ERROR" \
    && timeout 5 docker exec "${CONTAINER_NAME}" mysql \
      --host 127.0.0.1 \
      --port "$((VITESS_BASE_PORT + 3))" \
      --user root \
      --database "${STACKTEST_KEYSPACE}" \
      --batch --skip-column-names \
      --execute 'SELECT 1 FROM appbase_UserAccount LIMIT 0' \
      >/dev/null 2>"$VITESS_LAST_PROBE_ERROR" \
    && timeout 5 docker exec "${CONTAINER_NAME}" mysql \
      --host 127.0.0.1 \
      --port "$((VITESS_BASE_PORT + 3))" \
      --user root \
      --database "${GRPC_KEYSPACE}" \
      --batch --skip-column-names \
      --execute 'SELECT 1 FROM TestTable LIMIT 0' \
      >/dev/null 2>"$VITESS_LAST_PROBE_ERROR"; then
    return 0
  else
    probe_exit=$?
  fi
  printf 'probe exit=%s\n' "$probe_exit" >>"$VITESS_LAST_PROBE_ERROR"
  return "$probe_exit"
}

dump_vitess_readiness_diagnostics() {
  local fixture
  {
    echo "Vitess readiness diagnostics after query-probe exhaustion:"
    echo "  Last raw MySQL probe error:"
    if [ -s "$VITESS_LAST_PROBE_ERROR" ]; then
      sed 's/^/    /' "$VITESS_LAST_PROBE_ERROR"
    else
      echo "    <empty>"
    fi
    echo "  Container state:"
    timeout 10 docker inspect --format \
      'status={{.State.Status}} running={{.State.Running}} exit={{.State.ExitCode}} oom={{.State.OOMKilled}} error={{.State.Error}}' \
      "${CONTAINER_NAME}" 2>&1 | sed 's/^/    /' || true
    echo "  Staged host schema fixtures:"
    for fixture in \
      "$SCHEMA_STAGE/$GRPC_KEYSPACE/schema.sql" \
      "$SCHEMA_STAGE/$GRPC_KEYSPACE/vschema.json" \
      "$SCHEMA_STAGE/$STACKTEST_UNSHARDED_KEYSPACE/001-stacktest.sql" \
      "$SCHEMA_STAGE/$STACKTEST_UNSHARDED_KEYSPACE/vschema.json" \
      "$SCHEMA_STAGE/$STACKTEST_KEYSPACE/001-stacktest.sql" \
      "$SCHEMA_STAGE/$STACKTEST_KEYSPACE/002-service.sql" \
      "$SCHEMA_STAGE/$STACKTEST_KEYSPACE/vschema.json"; do
      if [ -f "$fixture" ]; then
        wc -c "$fixture" | sed 's/^/    /'
      else
        echo "    MISSING $fixture"
      fi
    done
    echo "  Container-mounted schema visibility:"
    timeout 10 docker exec "${CONTAINER_NAME}" /bin/sh -c \
      'for path in /vt/schema/appbase-vitess-test-keyspace/schema.sql /vt/schema/appbase-vitess-test-keyspace/vschema.json /vt/schema/appbase-stacktest-unsharded-keyspace/001-stacktest.sql /vt/schema/appbase-stacktest-unsharded-keyspace/vschema.json /vt/schema/appbase-stacktest-sharded-keyspace/001-stacktest.sql /vt/schema/appbase-stacktest-sharded-keyspace/002-service.sql /vt/schema/appbase-stacktest-sharded-keyspace/vschema.json; do if [ -f "$path" ]; then wc -c "$path"; else echo "MISSING $path"; fi; done' \
      2>&1 | sed 's/^/    /' || true
    echo "  Vtgate-visible databases/keyspaces:"
    timeout 10 docker exec "${CONTAINER_NAME}" mysql \
      --host 127.0.0.1 --port "$((VITESS_BASE_PORT + 3))" --user root \
      --batch --skip-column-names --execute 'SHOW DATABASES' \
      2>&1 | sed 's/^/    /' || true
    echo "  Last Vitess container log lines:"
    timeout 10 docker logs --tail 200 "${CONTAINER_NAME}" 2>&1 | sed 's/^/    /' || true
  } >&2
}

if ! appbase_wait_ready "${VITESS_READY_RETRIES}" "${VITESS_READY_INTERVAL}" -- vitess_keyspace_ready; then
  dump_vitess_readiness_diagnostics
  exit 1
fi


## docker logs -f ${CONTAINER_NAME}
