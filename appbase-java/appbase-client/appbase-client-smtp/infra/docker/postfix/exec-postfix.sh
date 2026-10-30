#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-postfix.sh


docker exec -it -u 0 ${CONTAINER_NAME} bash

## apt update
## apt install -y vim ncurses-bin net-tools procps iputils-ping
