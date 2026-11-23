#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-vitess.sh


docker exec -it -u 0 ${CONTAINER_NAME} bash
