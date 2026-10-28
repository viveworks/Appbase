#!/bin/bash

export APPBASE_VITESS_SCRIPTS_BASEDIR=${APPBASE_VITESS_SCRIPTS_BASEDIR:-$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )}
export VTDATAROOT=${VTDATAROOT:-"/Users/nav/Temporary/Data/Vitess"}
export CELL=${CELL:-'zone1'}
source "${APPBASE_VITESS_SCRIPTS_BASEDIR}/vitess-utils.sh"

for binary in vttablet vtgate vtctld mysqlctl vtorc vtctl; do
  majorVersion=$("${binary}" --version | sed -rn 's/^Version:[[:space:]]*([[:digit:]]+)\.[[:digit:]]+\.[[:digit:]]+.*/\1/p')
  if [[ $majorVersion -gt "16" ]]; then
    alias $binary="$binary --config-file-not-found-handling=ignore"
  fi
done;

alias mysql="command mysql --no-defaults -h 127.0.0.1 -P 15306"
alias vtctlclient="command vtctlclient --server localhost:15999 --log_dir ${VTDATAROOT}/tmp --alsologtostderr --config-file-not-found-handling=ignore"
alias vtctldclient="command vtctldclient --server localhost:15999"
shopt -s expand_aliases


function vtadmin_web_stop() {
  echo "vtadmin_web stopping..."
  stop_process "vtadmin-web" "${VTDATAROOT}/tmp/vtadmin-web.pid"
  echo "vtadmin_web stop done."
}


function vtadmin_api_stop() {
  echo "vtadmin_api stopping..."
  stop_process "vtadmin-api" "${VTDATAROOT}/tmp/vtadmin-api.pid"
  echo "vtadmin_api stop done."
}


function vtorc_stop() {
  echo "vtorc stopping..."
  stop_process "vtorc" "${VTDATAROOT}/tmp/vtorc.pid"
  echo "vtorc stop done."
}


function vtgate_stop() {
  echo "vtgate stopping..."
  stop_process "vtgate" "${VTDATAROOT}/tmp/vtgate.pid"
  echo "vtgate stop done."
}


function vttablet_stop() {
  echo "vttablet stopping..."

  if [[ -z ${1} ]]; then
    fail "Missing argument(s), requires: tablet_uid"
  fi
  local tablet_uid=${1}

  printf -v alias '%s-%010d' ${CELL} ${tablet_uid}
  printf -v tablet_dir 'vt_%010d' ${tablet_uid}

  echo "Shutting down tablet ${alias}"
  stop_process "vttablet" "${VTDATAROOT}/${tablet_dir}/vttablet.pid"

  echo "Shutting down MySQL for tablet ${alias}..."
  mysqlctl --config-file-not-found-handling=ignore --tablet_uid ${tablet_uid} shutdown

  echo "vttablet stop done."
}


function vtctld_stop() {
  echo "vtctld stopping..."
  stop_process "vtctld" "${VTDATAROOT}/tmp/vtctld.pid"
  echo "vtctld stop done."
}

function zookeeper_clear() {
  echo "zookeeper state clearing..."
  docker exec -t -u 0 zookeeper.local zkCli.sh deleteall /vitess
  echo "zookeeper state cleared."
}

function stop_vitess_processes() {
  if [ -n "${VTDATAROOT}" ]; then
    if pgrep -f -l "${VTDATAROOT}" >/dev/null; then
      echo "ERROR: Stale processes found, kill it manually:"
      pgrep -f -l "${VTDATAROOT}"
    fi
  fi

  if pgrep -f -l "vitess" >/dev/null; then
    echo "ERROR: Stale processes found, kill it manually:"
    pgrep -f -l "vitess"
  fi
}


function verify_stop() {
  echo "vitess stop verification starting..."
  stop_vitess_processes
  echo "vitess verification done."

  disown -a
}