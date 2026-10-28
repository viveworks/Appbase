#!/bin/bash

export DOCKER_IMAGE="mariadb:10.11"
export HOSTNAME="mariadb.local"
export CONTAINER_NAME="${HOSTNAME}"
export DATA_DIR="${APPBASE_DATA_ROOT:-$HOME/Temporary/Data}/MariaDB"
export VOLUME_MOUNT="/var/lib/mysql"
