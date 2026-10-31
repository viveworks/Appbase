#!/bin/bash

export DOCKER_IMAGE="postgres:16"
export HOSTNAME="postgresql.local"
export CONTAINER_NAME="${HOSTNAME}"
export DATA_DIR="$HOME/Temporary/Data/PostgreSQL"
export VOLUME_MOUNT="/var/lib/postgresql/data"
