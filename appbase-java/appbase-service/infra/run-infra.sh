#!/bin/bash
# @author Navaneeth Mysore G
# @author Claude
set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "$0")" >/dev/null 2>&1 && pwd -P)
APPBASE_CLIENT_ROOT=$(cd "$SCRIPT_DIR/../../appbase-client" >/dev/null 2>&1 && pwd -P)
APPBASE_CHILD_BASH=${APPBASE_CHILD_BASH:-/bin/bash}


AEROSPIKE_DIR="$APPBASE_CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike"
cd "$AEROSPIKE_DIR"
"$APPBASE_CHILD_BASH" "$AEROSPIKE_DIR/run-aerospike.sh"


ZOOKEEPER_DIR="$APPBASE_CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper"
cd "$ZOOKEEPER_DIR"
"$APPBASE_CHILD_BASH" "$ZOOKEEPER_DIR/run-zookeeper.sh"


VITESS_DIR="$APPBASE_CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema"
cd "$VITESS_DIR"
"$APPBASE_CHILD_BASH" "$VITESS_DIR/local-vitess-purge.sh"
"$APPBASE_CHILD_BASH" "$VITESS_DIR/local-vitess-start.sh"


FIREBASE_DIR="$APPBASE_CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase"
cd "$FIREBASE_DIR"
"$APPBASE_CHILD_BASH" "$FIREBASE_DIR/run-firebase.sh"


cd "$APPBASE_CLIENT_ROOT"
