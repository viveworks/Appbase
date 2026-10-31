#!/bin/bash
# @author Navaneeth Mysore G
# @author Claude
set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "$0")" >/dev/null 2>&1 && pwd -P)
APPBASE_CLIENT_ROOT=$(cd "$SCRIPT_DIR/../../appbase-java/appbase-client" >/dev/null 2>&1 && pwd -P)
APPBASE_CHILD_BASH=${APPBASE_CHILD_BASH:-/bin/bash}

# Validate the Vitess backend selector FIRST, before any infrastructure side effect below. An
# invalid value must fail closed with ZERO child-process calls -- not after this script has already
# created/started six other backends. See the Vitess block further down for what "local"/"docker"
# each do.
case "${APPBASE_STACKTEST_VITESS_BACKEND:-docker}" in
  local|docker) ;;
  *)
    echo "run-infra.sh: unknown APPBASE_STACKTEST_VITESS_BACKEND='${APPBASE_STACKTEST_VITESS_BACKEND}' (expected 'docker' or 'local')" >&2
    exit 2
    ;;
esac


AEROSPIKE_DIR="$APPBASE_CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike"
cd "$AEROSPIKE_DIR"
"$APPBASE_CHILD_BASH" "$AEROSPIKE_DIR/run-aerospike.sh"


ZOOKEEPER_DIR="$APPBASE_CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper"
cd "$ZOOKEEPER_DIR"
"$APPBASE_CHILD_BASH" "$ZOOKEEPER_DIR/run-zookeeper.sh"


# Used by Druid
POSTGRESQL_DIR="$APPBASE_CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql"
cd "$POSTGRESQL_DIR"
"$APPBASE_CHILD_BASH" "$POSTGRESQL_DIR/run-postgresql.sh"


DRUID_DIR="$APPBASE_CLIENT_ROOT/appbase-client-druid/infra/docker/druid"
cd "$DRUID_DIR"
"$APPBASE_CHILD_BASH" "$DRUID_DIR/run-druid.sh"


ELASTICSEARCH_DIR="$APPBASE_CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch"
cd "$ELASTICSEARCH_DIR"
"$APPBASE_CHILD_BASH" "$ELASTICSEARCH_DIR/run-elasticsearch.sh"


FIREBASE_DIR="$APPBASE_CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase"
cd "$FIREBASE_DIR"
"$APPBASE_CHILD_BASH" "$FIREBASE_DIR/run-firebase.sh"

S3_DIR="$APPBASE_CLIENT_ROOT/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3/infra/docker/s3"
cd "$S3_DIR"
"$APPBASE_CHILD_BASH" "$S3_DIR/run-s3.sh"

# Vitess backend selector (default: docker). The local-binary path assumes vttablet/vtgate/etc. are
# installed on the machine running this script and a hard-coded local data root -- true on a
# maintainer's own workstation, never on a hosted CI runner. Unset, or set
# APPBASE_STACKTEST_VITESS_BACKEND=docker explicitly, to use the Docker vttestserver lifecycle that
# appbase-client-vitess already exercises for its own gRPC/JDBC integration tests. Set it to `local` to
# keep using the local-binary lifecycle on a machine that has Vitess installed. Any OTHER value
# already failed closed at the top of this script, before any backend above started -- the `*)`
# branch below is a defensive second check, not the primary gate. See the infra_client_paths_test.sh
# coverage for all three orchestrators, including the zero-calls assertion on an invalid value.
VITESS_LOCAL_DIR="$SCRIPT_DIR/vitess"
VITESS_DOCKER_DIR="$APPBASE_CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess"
case "${APPBASE_STACKTEST_VITESS_BACKEND:-docker}" in
  local)
    cd "$VITESS_LOCAL_DIR"
    "$APPBASE_CHILD_BASH" "$VITESS_LOCAL_DIR/local-vitess-purge.sh"
    "$APPBASE_CHILD_BASH" "$VITESS_LOCAL_DIR/local-vitess-start.sh"
    ;;
  docker)
    cd "$VITESS_DOCKER_DIR"
    "$APPBASE_CHILD_BASH" "$VITESS_DOCKER_DIR/remove-vitess.sh"
    "$APPBASE_CHILD_BASH" "$VITESS_DOCKER_DIR/run-vitess.sh"
    ;;
  *)
    echo "run-infra.sh: unknown APPBASE_STACKTEST_VITESS_BACKEND='${APPBASE_STACKTEST_VITESS_BACKEND}' (expected 'docker' or 'local')" >&2
    exit 2
    ;;
esac


cd "$APPBASE_CLIENT_ROOT"
