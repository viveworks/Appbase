#!/bin/bash


export HOSTNAME="firebase.local"
export CONTAINER_NAME=${HOSTNAME}
export TAG="viveworks.com/infra/local/firebase"
export DATA_DIR="$HOME/Temporary/Data/Firebase/cache"
export VOLUME_MOUNT="/home/node/.cache"
