#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-druid.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "run-druid: cannot source lifecycle-common.sh" >&2; exit 1; }

: "${DRUID_READY_RETRIES:=90}"; : "${DRUID_READY_INTERVAL:=2}"


mkdir -p ${DATA_DIR}


docker run \
    --detach \
    --add-host docker.for.mac.localhost:host-gateway \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --add-host docker.for.mac.localhost:host-gateway \
    --volume ${DATA_DIR}:${VOLUME_MOUNT} \
    --publish 8081:8081 \
    --publish 8082:8082 \
    --publish 8083:8083 \
    --publish 8090:8090 \
    --publish 8091:8091 \
    --publish 8100:8100 \
    --publish 8101:8101 \
    --publish 8102:8102 \
    --publish 8103:8103 \
    --publish 8104:8104 \
    --publish 8105:8105 \
    --publish 8888:8888 \
    ${TAG} || exit 1

appbase_verify_running "${CONTAINER_NAME}" || exit 1
# Router 8888 can listen while Broker SQL still resets connections. The live client uses Broker
# Avatica on 8082, so require an actual SQL round trip before declaring Druid ready.
appbase_wait_ready "${DRUID_READY_RETRIES}" "${DRUID_READY_INTERVAL}" -- appbase_druid_sql_ready || exit 1


## docker logs -f ${CONTAINER_NAME}
