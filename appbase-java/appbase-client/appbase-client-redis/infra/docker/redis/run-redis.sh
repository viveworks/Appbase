#!/bin/bash

BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-redis.sh

mkdir -p ${DATA_DIR}

docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --volume ${DATA_DIR}:${VOLUME_MOUNT} \
    --publish ${REDIS_PORT}:6379 \
    ${DOCKER_IMAGE}

## docker logs -f ${CONTAINER_NAME}
