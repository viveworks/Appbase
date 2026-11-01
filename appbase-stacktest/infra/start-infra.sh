#!/bin/bash
# @author Navaneeth Mysore G
# @author Claude
set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "$0")" >/dev/null 2>&1 && pwd -P)
APPBASE_CLIENT_ROOT=$(cd "$SCRIPT_DIR/../../appbase-java/appbase-client" >/dev/null 2>&1 && pwd -P)
APPBASE_CHILD_BASH=${APPBASE_CHILD_BASH:-/bin/bash}

# Validate the Vitess backend selector FIRST, before any infrastructure side effect below. An
# invalid value must fail closed with ZERO child-process calls -- not after this script has already
# started six other backends. See the Vitess block further down for what "local"/"docker" each do.
case "${APPBASE_STACKTEST_VITESS_BACKEND:-docker}" in
  local|docker) ;;
  *)
    echo "start-infra.sh: unknown APPBASE_STACKTEST_VITESS_BACKEND='${APPBASE_STACKTEST_VITESS_BACKEND}' (expected 'docker' or 'local')" >&2
    exit 2
    ;;
esac


# Used by Druid
POSTGRESQL_DIR="$APPBASE_CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql"
cd "$POSTGRESQL_DIR"
"$APPBASE_CHILD_BASH" "$POSTGRESQL_DIR/start-postgresql.sh"
sleep 3


ZOOKEEPER_DIR="$APPBASE_CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper"
cd "$ZOOKEEPER_DIR"
"$APPBASE_CHILD_BASH" "$ZOOKEEPER_DIR/start-zookeeper.sh"
sleep 5


AEROSPIKE_DIR="$APPBASE_CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike"
cd "$AEROSPIKE_DIR"
"$APPBASE_CHILD_BASH" "$AEROSPIKE_DIR/start-aerospike.sh"

DRUID_DIR="$APPBASE_CLIENT_ROOT/appbase-client-druid/infra/docker/druid"
cd "$DRUID_DIR"
"$APPBASE_CHILD_BASH" "$DRUID_DIR/start-druid.sh"

DRUID_DATASOURCE_DIR="$SCRIPT_DIR/druid"
cd "$DRUID_DATASOURCE_DIR"
"$APPBASE_CHILD_BASH" "$DRUID_DATASOURCE_DIR/create-druid-datasource.sh"


ELASTICSEARCH_DIR="$APPBASE_CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch"
cd "$ELASTICSEARCH_DIR"
"$APPBASE_CHILD_BASH" "$ELASTICSEARCH_DIR/start-elasticsearch.sh"


FIREBASE_DIR="$APPBASE_CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase"
cd "$FIREBASE_DIR"
"$APPBASE_CHILD_BASH" "$FIREBASE_DIR/start-firebase.sh"

S3_DIR="$APPBASE_CLIENT_ROOT/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3/infra/docker/s3"
cd "$S3_DIR"
"$APPBASE_CHILD_BASH" "$S3_DIR/start-s3.sh"

REDIS_DIR="$APPBASE_CLIENT_ROOT/appbase-client-redis/infra/docker/redis"
cd "$REDIS_DIR"
"$APPBASE_CHILD_BASH" "$REDIS_DIR/start-redis.sh"

# See run-infra.sh for why this defaults to the Docker vttestserver lifecycle instead of the
# local-binary scripts, how to opt back into the local path on a machine that has Vitess installed
# (APPBASE_STACKTEST_VITESS_BACKEND=local), and why any OTHER value already failed closed at the top
# of this script (the `*)` branch below is a defensive second check, not the primary gate).
VITESS_LOCAL_DIR="$SCRIPT_DIR/vitess"
VITESS_DOCKER_DIR="$APPBASE_CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess"
case "${APPBASE_STACKTEST_VITESS_BACKEND:-docker}" in
  local)
    cd "$VITESS_LOCAL_DIR"
    "$APPBASE_CHILD_BASH" "$VITESS_LOCAL_DIR/local-vitess-start.sh"
    ;;
  docker)
    cd "$VITESS_DOCKER_DIR"
    "$APPBASE_CHILD_BASH" "$VITESS_DOCKER_DIR/start-vitess.sh"
    ;;
  *)
    echo "start-infra.sh: unknown APPBASE_STACKTEST_VITESS_BACKEND='${APPBASE_STACKTEST_VITESS_BACKEND}' (expected 'docker' or 'local')" >&2
    exit 2
    ;;
esac


cd "$APPBASE_CLIENT_ROOT"
