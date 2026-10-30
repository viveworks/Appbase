#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-spark.sh


docker exec -it -u 0 ${MASTER_CONTAINER_NAME} bash


## apt update
## apt install vim ncurses-bin net-tools
