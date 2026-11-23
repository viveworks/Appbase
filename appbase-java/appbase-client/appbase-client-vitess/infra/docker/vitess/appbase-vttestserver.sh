#!/bin/bash
#
# Copyright 2019 The original author or authors of Appbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -euo pipefail

: "${KEYSPACES:?KEYSPACES is required}"
: "${NUM_SHARDS:?NUM_SHARDS is required}"
: "${PORT:?PORT is required}"
: "${SCHEMA_DIR:?SCHEMA_DIR is required}"

readonly unsharded_keyspace="appbase-stacktest-unsharded-keyspace"
readonly sharded_keyspace="appbase-stacktest-sharded-keyspace"

for schema_file in \
  stacktest-unsharded.sql stacktest-unsharded.json \
  stacktest-sharded.sql stacktest-sharded.json service-sharded.sql
do
  if [[ ! -r "${SCHEMA_DIR}/${schema_file}" ]]; then
    echo "appbase-vttestserver: required schema input is missing: ${SCHEMA_DIR}/${schema_file}" >&2
    exit 1
  fi
done

# v17's official vttestserver image creates one /vt/schema/<keyspace> directory per KEYSPACES
# entry, then passes /vt/schema to vttestserver. Populate those exact directories with the real
# repository schemas before starting; vttestserver creates the tablets and shard primaries.
/vt/setup_vschema_folder.sh "${KEYSPACES}" "${NUM_SHARDS}"

cp "${SCHEMA_DIR}/stacktest-unsharded.sql" "/vt/schema/${unsharded_keyspace}/schema.sql"
cp "${SCHEMA_DIR}/stacktest-unsharded.json" "/vt/schema/${unsharded_keyspace}/vschema.json"
cat "${SCHEMA_DIR}/stacktest-sharded.sql" "${SCHEMA_DIR}/service-sharded.sql" \
  > "/vt/schema/${sharded_keyspace}/schema.sql"
cp "${SCHEMA_DIR}/stacktest-sharded.json" "/vt/schema/${sharded_keyspace}/vschema.json"

readonly mysql_max_connections="${MYSQL_MAX_CONNECTIONS:-1000}"
echo "max_connections = ${mysql_max_connections}" >> /vt/config/mycnf/test-suite.cnf
rm -f "${VTDATAROOT}"/"${tablet_dir:-vt_0000000001}"/{mysql.sock,mysql.sock.lock}

exec /vt/bin/vttestserver \
  --port "${PORT}" \
  --keyspaces "${KEYSPACES}" \
  --num_shards "${NUM_SHARDS}" \
  --mysql_bind_host "${MYSQL_BIND_HOST:-127.0.0.1}" \
  --mysql_server_version "${MYSQL_SERVER_VERSION:-${1:-8.0.21-vitess}}" \
  --charset "${CHARSET:-utf8mb4}" \
  --foreign_key_mode "${FOREIGN_KEY_MODE:-allow}" \
  --enable_online_ddl="${ENABLE_ONLINE_DDL:-true}" \
  --enable_direct_ddl="${ENABLE_DIRECT_DDL:-true}" \
  --planner-version="${PLANNER_VERSION:-gen4}" \
  --vschema_ddl_authorized_users=% \
  --tablet_refresh_interval "${TABLET_REFRESH_INTERVAL:-10s}" \
  --schema_dir=/vt/schema
