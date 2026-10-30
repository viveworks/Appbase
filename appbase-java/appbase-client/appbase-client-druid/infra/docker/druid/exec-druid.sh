#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-druid.sh


docker exec -it -u 0 ${CONTAINER_NAME} sh


## apt update
## apt install vim ncurses-bin net-tools
