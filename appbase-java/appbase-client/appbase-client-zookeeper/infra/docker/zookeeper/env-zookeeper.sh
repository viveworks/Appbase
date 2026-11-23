#!/bin/bash

export DOCKER_IMAGE="zookeeper:3.9"
export HOSTNAME="zookeeper.local"
export CONTAINER_NAME="${HOSTNAME}"
export DATA_DIR="$HOME/Temporary/Data/ZooKeeper"
export VOLUME_MOUNT="/data"