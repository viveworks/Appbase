#!/bin/bash

# Copyright 2019 The Vitess Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# this script brings up zookeeper and all the vitess components
# required for a single shard deployment.

source /vt/local/scripts/env.sh

# start topo server
if [ "${TOPO}" = "zk2" ]; then
	CELL=zone1 /vt/local/scripts/zk-up.sh
elif [ "${TOPO}" = "k8s" ]; then
	CELL=zone1 /vt/local/scripts/k3s-up.sh
else
	CELL=zone1 /vt/local/scripts/etcd-up.sh
fi


# start vtctld
CELL=zone1 /vt/local/scripts/vtctld-up.sh


# start vttablets for keyspace appbase-vitess-test-keyspace
for i in 100 101 102; do
	CELL=zone1 TABLET_UID=$i /vt/local/scripts/mysqlctl-up.sh
	CELL=zone1 KEYSPACE=appbase-vitess-test-sharded-keyspace TABLET_UID=$i /vt/local/scripts/vttablet-up.sh
done

CELL=zone1 TABLET_UID=200 /vt/local/scripts/mysqlctl-up.sh
CELL=zone1 KEYSPACE=appbase-vitess-test-unsharded-keyspace TABLET_UID=200 /vt/local/scripts/vttablet-up.sh
vtctldclient InitShardPrimary --force appbase-vitess-test-unsharded-keyspace/0 zone1-200


# set one of the replicas to primary
vtctldclient InitShardPrimary --force appbase-vitess-test-sharded-keyspace/0 zone1-100


# create the schema
vtctlclient ApplySchema -sql-file /vt/local/scripts/appbase-vitess-test-sharded-keyspace-schema.sql appbase-vitess-test-sharded-keyspace


# create the vschema
vtctlclient ApplyVSchema \
  -vschema_file /vt/local/scripts/appbase-vitess-test-sharded-keyspace-vschema.json \
  appbase-vitess-test-sharded-keyspace


# start vtgate
CELL=zone1 /vt/local/scripts/vtgate-up.sh
