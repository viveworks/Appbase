#!/bin/bash


export DOCKER_IMAGE="cassandra:4.1"
export HOSTNAME="cassandra.local"
export CONTAINER_NAME="${HOSTNAME}"
export DATA_DIR="${APPBASE_DATA_ROOT:-$HOME/Temporary/Data}/Cassandra"
export VOLUME_MOUNT="/var/lib/cassandra"
