#!/bin/bash

if [[ $EUID -eq 0 ]]; then
  fail "This script cannot be run as root"
fi

export APPBASE_STACK_TEST_SCRIPTS_BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
export APPBASE_CLIENT_VITESS_BASEDIR="${APPBASE_STACK_TEST_SCRIPTS_BASEDIR}/../../../appbase-java/appbase-client/appbase-client-vitess/infra/local/vitess"
source "${APPBASE_CLIENT_VITESS_BASEDIR}/vitess-utils.sh"
source "${APPBASE_CLIENT_VITESS_BASEDIR}/appbase-vitess-start.sh"

export APPBASE_SERVICE_VITESS_BASEDIR="${APPBASE_STACK_TEST_SCRIPTS_BASEDIR}/../../../appbase-java/appbase-client/appbase-client-vitess/infra/local/service-schema"

export STACK_TEST_UNSHARDED_KEYSPACE="appbase-stacktest-unsharded-keyspace"
export STACK_TEST_SHARDED_KEYSPACE="appbase-stacktest-sharded-keyspace"



########################################
## Main
########################################
zookeeper_configure

vtctld_start

create_keyspace ${STACK_TEST_UNSHARDED_KEYSPACE}
create_keyspace ${STACK_TEST_SHARDED_KEYSPACE}

vttablet_start ${STACK_TEST_UNSHARDED_KEYSPACE} 100 rdonly
vttablet_start ${STACK_TEST_SHARDED_KEYSPACE} 110 rdonly
vttablet_start ${STACK_TEST_SHARDED_KEYSPACE} 111 rdonly
vttablet_start ${STACK_TEST_SHARDED_KEYSPACE} 112 rdonly

assign_shard_primary 100 ${STACK_TEST_UNSHARDED_KEYSPACE}
assign_shard_primary 110 ${STACK_TEST_SHARDED_KEYSPACE}

#vtorc_start

echo "Waiting for healthy shards for keyspace=${STACK_TEST_UNSHARDED_KEYSPACE}, count=1"
wait_for_healthy_shard ${STACK_TEST_UNSHARDED_KEYSPACE} ${VITESS_INIT_SHARD} 1 || exit 1

echo "Waiting for healthy shards for keyspace=${STACK_TEST_SHARDED_KEYSPACE}, count=3"
wait_for_healthy_shard ${STACK_TEST_SHARDED_KEYSPACE} ${VITESS_INIT_SHARD} 3 || exit 1

echo "Applying appbase-stacktest unsharded vitess schema ..."
create_schema "${APPBASE_STACK_TEST_SCRIPTS_BASEDIR}/001-unsharded.sql" \
    "${APPBASE_STACK_TEST_SCRIPTS_BASEDIR}/001-unsharded.json" \
    ${STACK_TEST_UNSHARDED_KEYSPACE}

echo "Applying appbase-stacktest sharded vitess schema ..."
apply_schema "${APPBASE_STACK_TEST_SCRIPTS_BASEDIR}/001-sharded.sql" ${STACK_TEST_SHARDED_KEYSPACE}

echo "Applying appbase-service sharded vitess schema ..."
apply_schema "${APPBASE_SERVICE_VITESS_BASEDIR}/001-sharded.sql" ${STACK_TEST_SHARDED_KEYSPACE}

echo "Applying appbase-stacktest and appbase-service sharded vitess vschema ..."
apply_vschema "${APPBASE_STACK_TEST_SCRIPTS_BASEDIR}/001-sharded.json" ${STACK_TEST_SHARDED_KEYSPACE}

vtgate_start
vtadmin_start
