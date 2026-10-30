#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-druid.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "start-druid: cannot source lifecycle-common.sh" >&2; exit 1; }

: "${DRUID_READY_RETRIES:=90}"; : "${DRUID_READY_INTERVAL:=2}"


docker start ${CONTAINER_NAME} || exit 1
appbase_verify_running "${CONTAINER_NAME}" || exit 1
appbase_wait_ready "${DRUID_READY_RETRIES}" "${DRUID_READY_INTERVAL}" -- appbase_druid_sql_ready || exit 1
