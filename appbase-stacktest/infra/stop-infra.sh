#!/bin/bash
# @author Navaneeth Mysore G
# @author Claude
set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "$0")" >/dev/null 2>&1 && pwd -P)
APPBASE_CLIENT_ROOT=$(cd "$SCRIPT_DIR/../../appbase-java/appbase-client" >/dev/null 2>&1 && pwd -P)
APPBASE_CHILD_BASH=${APPBASE_CHILD_BASH:-/bin/bash}


AEROSPIKE_DIR="$APPBASE_CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike"
cd "$AEROSPIKE_DIR"
"$APPBASE_CHILD_BASH" "$AEROSPIKE_DIR/stop-aerospike.sh"


DRUID_DIR="$APPBASE_CLIENT_ROOT/appbase-client-druid/infra/docker/druid"
cd "$DRUID_DIR"
"$APPBASE_CHILD_BASH" "$DRUID_DIR/stop-druid.sh"


ELASTICSEARCH_DIR="$APPBASE_CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch"
cd "$ELASTICSEARCH_DIR"
"$APPBASE_CHILD_BASH" "$ELASTICSEARCH_DIR/stop-elasticsearch.sh"


FIREBASE_DIR="$APPBASE_CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase"
cd "$FIREBASE_DIR"
"$APPBASE_CHILD_BASH" "$FIREBASE_DIR/stop-firebase.sh"

VITESS_DIR="$SCRIPT_DIR/vitess"
cd "$VITESS_DIR"
"$APPBASE_CHILD_BASH" "$VITESS_DIR/local-vitess-stop.sh"


# Used by Druid
POSTGRESQL_DIR="$APPBASE_CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql"
cd "$POSTGRESQL_DIR"
"$APPBASE_CHILD_BASH" "$POSTGRESQL_DIR/stop-postgresql.sh"


ZOOKEEPER_DIR="$APPBASE_CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper"
cd "$ZOOKEEPER_DIR"
"$APPBASE_CHILD_BASH" "$ZOOKEEPER_DIR/stop-zookeeper.sh"


cd "$APPBASE_CLIENT_ROOT"
