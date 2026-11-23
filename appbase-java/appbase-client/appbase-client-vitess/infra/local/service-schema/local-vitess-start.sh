#!/bin/bash

if [[ $EUID -eq 0 ]]; then
  fail "This script cannot be run as root"
fi

APPBASE_SERVICE_TEST_SCRIPTS_BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
export APPBASE_VITESS_SCRIPTS_BASEDIR="${APPBASE_SERVICE_TEST_SCRIPTS_BASEDIR}/../vitess"
source "${APPBASE_VITESS_SCRIPTS_BASEDIR}/appbase-vitess-start.sh"

export SERVICE_SHARDED_KEYSPACE="appbase-service-sharded-keyspace"



########################################
## Main
########################################
zookeeper_configure

vtctld_start

create_keyspace ${SERVICE_SHARDED_KEYSPACE}

vttablet_start ${SERVICE_SHARDED_KEYSPACE} 210 rdonly
vttablet_start ${SERVICE_SHARDED_KEYSPACE} 211 rdonly
vttablet_start ${SERVICE_SHARDED_KEYSPACE} 212 rdonly

assign_shard_primary 210 ${SERVICE_SHARDED_KEYSPACE}

#vtorc_start

echo "Waiting for healthy shards for keyspace=${SERVICE_SHARDED_KEYSPACE}, count=3"
wait_for_healthy_shard ${SERVICE_SHARDED_KEYSPACE} ${VITESS_INIT_SHARD} 3 || exit 1

create_schema "${APPBASE_SERVICE_TEST_SCRIPTS_BASEDIR}/001-sharded.sql" \
    "${APPBASE_SERVICE_TEST_SCRIPTS_BASEDIR}/001-sharded.json" \
    ${SERVICE_SHARDED_KEYSPACE}

vtgate_start
vtadmin_start
