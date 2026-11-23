#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-spark.sh

echo "Staring Spark Master"
docker start ${MASTER_CONTAINER_NAME}

echo "Staring Spark Worker"
docker start ${WORKER_CONTAINER_NAME}
