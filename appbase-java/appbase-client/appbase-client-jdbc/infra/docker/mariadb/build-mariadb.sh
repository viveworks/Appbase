#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-mariadb.sh


docker pull ${DOCKER_IMAGE}
