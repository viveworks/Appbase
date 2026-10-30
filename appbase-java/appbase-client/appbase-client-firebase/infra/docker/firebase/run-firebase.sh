#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-firebase.sh

docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --publish 4000:4000 \
    --publish 9099:9099 \
    --volume ${DATA_DIR}:${VOLUME_MOUNT} \
    ${TAG}


## docker logs -f ${CONTAINER_NAME}
