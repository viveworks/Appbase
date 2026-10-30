#!/bin/bash

BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-redis.sh

docker exec -it ${CONTAINER_NAME} redis-cli
