#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-aerospike.sh


docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --env NAMESPACE=dev \
    --env DATA_IN_MEMORY=true \
    --env DEFAULT_TTL=30d \
    --publish 3000-3002:3000-3002 \
    ${TAG}


## docker logs -f ${CONTAINER_NAME}
