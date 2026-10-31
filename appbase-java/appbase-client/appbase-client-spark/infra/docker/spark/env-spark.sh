#!/bin/bash

export DOCKER_IMAGE="apache/spark:3.5.9"
export MASTER_HOSTNAME="spark-master.local"
export MASTER_CONTAINER_NAME="${MASTER_HOSTNAME}"
export WORKER_HOSTNAME="spark-worker.local"
export WORKER_CONTAINER_NAME="${WORKER_HOSTNAME}"
export DATA_DIR="$HOME/Temporary/Data/Spark"
export VOLUME_MOUNT="/opt/spark/work"
# Shared user-defined network so the worker resolves the master by name (the default bridge does not
# resolve container names — see appbase_require_network / E01-image-versions.md Spark caveats).
export SPARK_NETWORK="appbase-spark.local"
