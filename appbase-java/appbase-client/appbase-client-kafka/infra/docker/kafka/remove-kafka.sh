#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-kafka.sh


docker stop ${CONTAINER_NAME}
docker rm ${CONTAINER_NAME}
rm -rf ${DATA_DIR}
