#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-elasticsearch.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "start-elasticsearch: cannot source lifecycle-common.sh" >&2; exit 1; }

: "${ELASTICSEARCH_READY_RETRIES:=90}"; : "${ELASTICSEARCH_READY_INTERVAL:=2}"


docker start ${CONTAINER_NAME} || exit 1
appbase_verify_running "${CONTAINER_NAME}" || exit 1
appbase_wait_ready "${ELASTICSEARCH_READY_RETRIES}" "${ELASTICSEARCH_READY_INTERVAL}" -- \
    appbase_elasticsearch_ready || exit 1
