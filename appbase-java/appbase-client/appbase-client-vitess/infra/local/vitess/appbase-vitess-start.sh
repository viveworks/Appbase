#!/bin/bash

if [[ $EUID -eq 0 ]]; then
  fail "This script cannot be run as root"
fi

export APPBASE_VITESS_SCRIPTS_BASEDIR=${APPBASE_VITESS_SCRIPTS_BASEDIR:-$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )}
export VITESS_CLUSTER_NAME=${VITESS_CLUSTER_NAME:-'local'}
export VITESS_CELL=${VITESS_CELL:-'zone1'}
export VITESS_INIT_SHARD=${VITESS_INIT_SHARD:-'0'}
export VITESS_KEYSPACE=${VITESS_KEYSPACE:-'appbase-vitess-keyspace'}

export VITESS_HOSTNAME=${VITESS_HOSTNAME:-'localhost'}
export VTCTLD_WEB_PORT=${VTCTLD_WEB_PORT:-15000}
export VTCTLD_GRPC_PORT=${VTCTLD_GRPC_PORT:-15999}
export VTORC_PORT=${VTORC_PORT:-16000}
export VTGATE_WEB_PORT=${VTGATE_WEB_PORT:-15001}
export VTGATE_GRPC_PORT=${VTGATE_GRPC_PORT:-15991}
export VTGATE_MYSQL_PORT=${VTGATE_MYSQL_PORT:-15306}
export VTADMIN_API_PORT=${VTADMIN_API_PORT:-14200}
export VTADMIN_WEB_PORT=${VTADMIN_WEB_PORT:-14201}
export ETCD_SERVER=${ETCD_SERVER:-'localhost:2379'}
export ZK_SERVER=${ZK_SERVER:-'localhost:2181'}

export ETCDCTL_API=${ETCDCTL_API:-'2'}
export SIDECAR_DB_NAME=${SIDECAR_DB_NAME:-'_vt'}
export TOPOLOGY_FLAGS="--topo_implementation zk2 --topo_global_server_address ${ZK_SERVER} --topo_global_root /vitess/global"
export VTDATAROOT=${VTDATAROOT:-"/Users/nav/Temporary/Data/Vitess"}
export VT_LOGDIR_BASE=${VT_LOGDIR_BASE:-"${VTDATAROOT}/tmp"}
export VTORC_CONFIG=${VTORC_CONFIG:-"${APPBASE_VITESS_SCRIPTS_BASEDIR}/vtorc-config.json"}
export VTADMIN_WEB_BASEDIR=${VTADMIN_WEB_BASEDIR:-'/Users/nav/Temporary/vitess/web/vtadmin'}
export VTADMIN_RBAC_YAML=${VTADMIN_RBAC_YAML:-"${APPBASE_VITESS_SCRIPTS_BASEDIR}/vtadmin-rbac.yaml"}
export VTADMIN_DISCOVERY_JSON=${VTADMIN_DISCOVERY_JSON:-"${APPBASE_VITESS_SCRIPTS_BASEDIR}/vtadmin-discovery.json"}

mkdir -p "${VTDATAROOT}"
mkdir -p "${VTDATAROOT}/tmp"
mkdir -p "${VTDATAROOT}/backups"
source "${APPBASE_VITESS_SCRIPTS_BASEDIR}/vitess-utils.sh"

for binary in vttablet vtgate vtctld mysqlctl vtorc vtctl; do
  alias $binary="$binary --config-file-not-found-handling=ignore"
done;

alias mysql="command mysql --no-defaults -h 127.0.0.1 -P 15306"
alias vtctlclient="command vtctlclient --server localhost:15999 --log_dir ${VTDATAROOT}/tmp --alsologtostderr --config-file-not-found-handling=ignore"
alias vtctldclient="command vtctldclient --server localhost:15999"

shopt -s expand_aliases

if [ ! -d ${VTDATAROOT} ]; then
  echo "Cannot create ${VTDATAROOT}"
  exit 0
fi

if [ ! -d "${VTDATAROOT}/tmp" ]; then
  echo "Cannot create ${VTDATAROOT}/tmp"
  exit 0
fi

if [ ! -d "${VTDATAROOT}/backups" ]; then
  echo "Cannot create ${VTDATAROOT}/backups"
  exit 0
fi



####################
## zookeeper
####################
function zookeeper_configure() {
  echo "Configuring zookeeper..."

  vtctl ${TOPOLOGY_FLAGS} VtctldCommand AddCellInfo \
    --root "/vitess/${VITESS_CELL}" \
    --server-address ${ZK_SERVER} \
    ${VITESS_CELL}

  echo "Configuring zookeeper done: root=/vitess/${VITESS_CELL}"
}



####################
## vtctld
####################
function vtctld_start() {
  echo "vtctld starting..."

  vtctld \
    ${TOPOLOGY_FLAGS} \
    --cell ${VITESS_CELL} \
    --service_map 'grpc-vtctl,grpc-vtctld' \
    --backup_storage_implementation file \
    --file_backup_storage_root ${VTDATAROOT}/backups \
    --log_dir ${VTDATAROOT}/tmp \
    --port ${VTCTLD_WEB_PORT} \
    --grpc_port ${VTCTLD_GRPC_PORT} \
    --pid_file ${VTDATAROOT}/tmp/vtctld.pid \
    > ${VTDATAROOT}/tmp/vtctld.out 2>&1 &

  for _ in {0..300}; do
    curl -I "http://${VITESS_HOSTNAME}:${VTCTLD_WEB_PORT}/debug/status" &>/dev/null && break
    sleep 0.1
  done

  curl -I "http://${VITESS_HOSTNAME}:${VTCTLD_WEB_PORT}/debug/status" &>/dev/null || fail "vtctld start failed!"

  echo -e "vtctld start done."
}



####################
## Keyspace
####################
function create_keyspace() {
  if [[ -z ${1} ]]; then
    fail "Missing argument(s), required: keyspace"
  fi

  local keyspace=${1}

  if vtctldclient GetKeyspace ${keyspace} > /dev/null 2>&1 ; then
    echo "Configuring keyspace: ${keyspace}"
    vtctldclient SetKeyspaceDurabilityPolicy \
      --durability-policy=none \
      ${keyspace} \
      || fail "Failed to set keyspace durability policy on ${keyspace}"
  else
    echo "Creating and configuring keyspace: ${keyspace}"
    vtctldclient CreateKeyspace ${keyspace} \
      || fail "This looks like a config issue. Run this again and it should work."
  fi
}

function assign_shard_primary() {
  if [[ -z ${1} || -z ${2} ]]; then
    fail "Missing argument(s): cell_id and keyspace"
  fi

  local cell_id=${1}
  local keyspace=${2}

  ##vtctldclient --server localhost:15999 InitShardPrimary --force appbase-simple-keyspace/0 zone1-100
  vtctldclient InitShardPrimary --force ${keyspace}/${VITESS_INIT_SHARD} ${VITESS_CELL}-${cell_id}
}



####################
## vttablet
####################
function vttablet_start() {
  if [[ -z ${1} || -z ${2} || -z ${3} ]]; then
    fail "Missing argument(s), requires: keyspace, tablet_uid and tablet_type"
  fi

  local keyspace=${1}
  local tablet_uid=${2}
  local tablet_type=${3}

  local mysql_port=$[17000 + ${tablet_uid}]
  printf -v alias '%s-%010d' ${VITESS_CELL} ${tablet_uid}
  printf -v tablet_dir 'vt_%010d' ${tablet_uid}
  printf -v tablet_query_log 'vttablet_%010d_querylog.txt' ${tablet_uid}

  local mysql_action="init"
  if [ -d "${VTDATAROOT}/${tablet_dir}" ]; then
    echo "Resuming from existing vttablet dir: ${VTDATAROOT}/${tablet_dir}"
    mysql_action='start'
  fi

  echo "mysqlctl starting for tablet ${alias}..."
  mysqlctl \
    --log_dir ${VTDATAROOT}/tmp \
    --tablet_uid ${tablet_uid} \
    --mysql_port ${mysql_port} \
    ${mysql_action}
  echo "mysqlctl start done for tablet ${alias}."


  local vttablet_web_port=$[15000 + ${tablet_uid}]
  local vttablet_grpc_port=$[16000 + ${tablet_uid}]

  mkdir -p "${VTDATAROOT}/${tablet_dir}"

  echo "vttablet starting for ${alias}..."
  vttablet \
    ${TOPOLOGY_FLAGS} \
    --log_dir "${VTDATAROOT}/tmp" \
    --log_queries_to_file "${VTDATAROOT}/tmp/${tablet_query_log}" \
    --tablet-path "${alias}" \
    --tablet_hostname ${VITESS_HOSTNAME} \
    --init_keyspace ${keyspace} \
    --init_shard ${VITESS_INIT_SHARD} \
    --init_tablet_type ${tablet_type} \
    --health_check_interval 5s \
    --backup_storage_implementation file \
    --file_backup_storage_root "${VTDATAROOT}/backups" \
    --restore_from_backup \
    --port ${vttablet_web_port} \
    --grpc_port ${vttablet_grpc_port} \
    --service_map 'grpc-queryservice,grpc-tabletmanager,grpc-updatestream' \
    --pid_file "${VTDATAROOT}/$tablet_dir/vttablet.pid" \
    --heartbeat_enable \
    --heartbeat_interval=250ms \
    --heartbeat_on_demand_duration=5s \
    > ${VTDATAROOT}/$tablet_dir/vttablet.out 2>&1 &

  for i in $(seq 0 500); do
    curl -I "http://${VITESS_HOSTNAME}:${vttablet_web_port}/debug/status" >/dev/null 2>&1 && break
    sleep 0.2
  done
  curl -I "http://${VITESS_HOSTNAME}:${vttablet_web_port}/debug/status" \
    || fail "vttablet could not be started"

  echo "vttablet start done for ${alias}: http://${VITESS_HOSTNAME}:${vttablet_web_port}/debug/status"
}



####################
## vtorc
####################
function vtorc_start() {
  echo "vtorc starting..."

  vtorc \
    ${TOPOLOGY_FLAGS} \
    --logtostderr \
    --alsologtostderr \
    --config=${VTORC_CONFIG} \
    --port ${VTORC_PORT} \
    > "${VT_LOGDIR_BASE}/vtorc.out" 2>&1 &

  local vtorc_pid=$!
  echo ${vtorc_pid} > "${VT_LOGDIR_BASE}/vtorc.pid"

  echo "\
  vtorc start done.
    - UI: http://localhost:${VTORC_PORT}
    - Logs: ${VT_LOGDIR_BASE}/vtorc.out
    - PID: ${vtorc_pid}
  "
}



####################
## Schema
####################
function apply_schema() {
  if [[ -z ${1} || -z ${2} ]]; then
    fail "Missing argument(s), requires: sql_file and keyspace"
  fi

  local sql_file=${1}
  local keyspace=${2}

  echo "Applying schema sql_file=${sql_file}, keyspace=${keyspace}"
  vtctldclient ApplySchema \
    --action_timeout 1m \
    --sql-file ${sql_file} \
    ${keyspace} \
    || fail "Failed to apply schema for ${keyspace}"
}

function apply_vschema() {
  if [[ -z ${1} || -z ${2} ]]; then
    fail "Missing argument(s), requires: vschema_file and keyspace"
  fi

  local vschema_file=${1}
  local keyspace=${2}

  echo "Applying vschema vschema_file=${vschema_file}, keyspace=${keyspace}"
  vtctldclient ApplyVSchema \
    --action_timeout 1m \
    --vschema-file ${vschema_file} \
    ${keyspace} \
    || fail "Failed to apply vschema for ${keyspace}"
}

function create_schema() {
  apply_schema ${1} ${3}
  apply_vschema ${2} ${3}
}



####################
## vtgate_start
####################
function vtgate_start() {
  echo "vtgate starting..."

  vtgate \
    ${TOPOLOGY_FLAGS} \
    --log_dir ${VTDATAROOT}/tmp \
    --log_queries_to_file ${VTDATAROOT}/tmp/vtgate_querylog.txt \
    --port ${VTGATE_WEB_PORT} \
    --grpc_port ${VTGATE_GRPC_PORT} \
    --mysql_server_port ${VTGATE_MYSQL_PORT} \
    --cell ${VITESS_CELL} \
    --cells_to_watch ${VITESS_CELL} \
    --tablet_types_to_wait PRIMARY,REPLICA \
    --service_map 'grpc-vtgateservice' \
    --pid_file ${VTDATAROOT}/tmp/vtgate.pid \
    --mysql_auth_server_impl none \
    > ${VTDATAROOT}/tmp/vtgate.out 2>&1 &

  echo "Waiting for vtgate to start..."
  while true; do
    curl -I "http://${VITESS_HOSTNAME}:${VTGATE_WEB_PORT}/debug/status" >/dev/null 2>&1 && break
    sleep 0.1
  done;
  echo "vtgate start done."

  echo "vtgate: http://${VITESS_HOSTNAME}:${VTGATE_WEB_PORT}/debug/status"

  disown -a
}



####################
## vtadmin_start
####################
function vtadmin_api_start() {
  echo "vtadmin-api starting..."

  vtadmin \
    --addr "${VITESS_HOSTNAME}:${VTADMIN_API_PORT}" \
    --http-tablet-url-tmpl "http://{{ .Tablet.Hostname }}:15{{ .Tablet.Alias.Uid }}" \
    --tracer "opentracing-jaeger" \
    --grpc-tracing \
    --http-tracing \
    --logtostderr \
    --alsologtostderr \
    --rbac \
    --rbac-config="${VTADMIN_RBAC_YAML}" \
    --cluster "id=${VITESS_CLUSTER_NAME},name=${VITESS_CLUSTER_NAME},discovery=staticfile,discovery-staticfile-path=${VTADMIN_DISCOVERY_JSON},tablet-fqdn-tmpl=http://{{ .Tablet.Hostname }}:15{{ .Tablet.Alias.Uid }}" \
    > "${VT_LOGDIR_BASE}/vtadmin-api.out" 2>&1 &

  local vtadmin_api_pid=$!
  echo ${vtadmin_api_pid} > "${VT_LOGDIR_BASE}/vtadmin-api.pid"

  echo "\
    vtadmin-api start done.
      - API: http://${VITESS_HOSTNAME}:${VTADMIN_API_PORT}
      - Logs: ${VT_LOGDIR_BASE}/vtadmin-api.out
      - PID: ${vtadmin_api_pid}
  "

  local expected_cluster_result="{\"result\":{\"clusters\":[{\"id\":\"${VITESS_CLUSTER_NAME}\",\"name\":\"${VITESS_CLUSTER_NAME}\"}]},\"ok\":true}"
  for _ in {0..300}; do
    local result=$(curl -s "http://${VITESS_HOSTNAME}:${VTADMIN_API_PORT}/api/clusters")
    if [[ ${result} == "${expected_cluster_result}" ]]; then
      break
    fi
    sleep 0.1
  done

  [[ $(curl -s "http://${VITESS_HOSTNAME}:${VTADMIN_API_PORT}/api/clusters") == "${expected_cluster_result}" ]] \
    || fail "vtadmin failed to discover the running cluster ${VITESS_CLUSTER_NAME}."

  echo "vtadmin-api start done."
}


function vtadmin_web_build() {
  source "${HOME}/.nvm/nvm.sh"
  ## nvm install 16 || fail "nvm not found. Install using 'brew install nvm'"
  nvm use 16

  cd ${VTADMIN_WEB_BASEDIR}

  if [[ ! -d ${VTADMIN_WEB_BASEDIR}/node_modules ]]; then
    echo "vtadmin - install node_modules "
    npm install
  fi

  if [[ ! -d ${VTADMIN_WEB_BASEDIR}/build ]]; then
    echo "vtadmin - install node_modules "
    VITE_VTADMIN_API_ADDRESS="http://${VITESS_HOSTNAME}:${VTADMIN_API_PORT}" \
        VITE_ENABLE_EXPERIMENTAL_TABLET_DEBUG_VARS="true" \
    npm run --prefix build
  fi
}


function vtadmin_web_start() {
  echo "vtadmin-web starting..."

  vtadmin_web_build

  VITE_VTADMIN_API_ADDRESS="http://${VITESS_HOSTNAME}:${VTADMIN_API_PORT}" \
    VITE_ENABLE_EXPERIMENTAL_TABLET_DEBUG_VARS="true" \
    npm run --prefix "${VTADMIN_WEB_BASEDIR}" build

  "${VTADMIN_WEB_BASEDIR}/node_modules/.bin/serve" \
    --no-clipboard \
    -l ${VTADMIN_WEB_PORT} \
    -s "${VTADMIN_WEB_BASEDIR}/build" \
    > "${VT_LOGDIR_BASE}/vtadmin-web.out" 2>&1 &

  local vtadmin_web_pid=$!
  echo ${vtadmin_web_pid} > "${VT_LOGDIR_BASE}/vtadmin-web.pid"

  echo "\
    vtadmin-web start done.
      - Browser: http://${VITESS_HOSTNAME}:${VTADMIN_WEB_PORT}
      - Logs: ${VT_LOGDIR_BASE}/vtadmin-web.out
      - PID: ${vtadmin_web_pid}
  "
}


function vtadmin_start() {
  echo "vtadmin api and web starting..."

  vtadmin_api_start
  vtadmin_web_start

  echo "vtadmin api and web start done."
}