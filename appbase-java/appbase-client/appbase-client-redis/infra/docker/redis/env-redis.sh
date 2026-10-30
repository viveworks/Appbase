#!/bin/bash

export DOCKER_IMAGE="redis:7.4"
export HOSTNAME="redis.local"
export CONTAINER_NAME="${HOSTNAME}"
export DATA_DIR="$HOME/Temporary/Data/Redis"
export VOLUME_MOUNT="/data"
export REDIS_PORT=6379
