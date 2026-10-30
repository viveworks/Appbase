#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-spark.sh

echo "Stopping Spark Worker"
docker stop ${WORKER_CONTAINER_NAME}

echo "Stopping Spark Master"
docker stop ${MASTER_CONTAINER_NAME}
