#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-spark.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "run-spark: cannot source lifecycle-common.sh" >&2; exit 1; }

# Readiness budgets are env-tunable (defaults are the production values); the lifecycle test overrides
# them so its behavioural bring-up does not sleep for minutes on the failure path.
: "${SPARK_VERIFY_RETRIES:=10}"; : "${SPARK_VERIFY_INTERVAL:=1}"
: "${SPARK_READY_RETRIES:=60}"; : "${SPARK_READY_INTERVAL:=3}"

appbase_require_image "${DOCKER_IMAGE}" || exit 1

# Master↔worker link: the worker reaches the master at spark://${MASTER_HOSTNAME}:7077, which only
# resolves when both containers share a user-defined network (the default bridge does not resolve
# container names). Create/attach that network as part of the lifecycle.
appbase_require_network "${SPARK_NETWORK}" || exit 1

mkdir -p ${DATA_DIR}
# apache/spark runs as non-root uid 185 with no chown-on-start, so the host-owned bind-mount created by
# `mkdir -p` above is not writable inside the container and the worker dies on first write to its
# work-dir (the S07/internal work item 522 silent-death class). Make the mounted work dir writable for that uid. This is
# test-only local infrastructure, not a shipped artifact.
chmod 0777 ${DATA_DIR}

echo "Running Spark Master"
docker run \
    --detach \
    --network ${SPARK_NETWORK} \
    --hostname ${MASTER_HOSTNAME} \
    --name ${MASTER_CONTAINER_NAME} \
    --volume ${DATA_DIR}:${VOLUME_MOUNT} \
    --publish 7077:7077 \
    --publish 8077:8080 \
    ${DOCKER_IMAGE} \
    /opt/spark/bin/spark-class org.apache.spark.deploy.master.Master --host 0.0.0.0 --port 7077 --webui-port 8080 || exit 1

appbase_verify_running "${MASTER_CONTAINER_NAME}" "${SPARK_VERIFY_RETRIES}" "${SPARK_VERIFY_INTERVAL}" || exit 1
# Probe 127.0.0.1 (not `localhost`): `--publish 7077:7077` binds the IPv4 loopback, but on a host where
# `localhost` resolves to ::1 first a bash /dev/tcp probe of `localhost` would miss the published port.
appbase_wait_ready "${SPARK_READY_RETRIES}" "${SPARK_READY_INTERVAL}" -- appbase_tcp_probe 127.0.0.1 7077 || exit 1

echo "Running Spark Worker"
# The worker mounts the same host dir and is told to USE it as its scratch dir (--work-dir): the image
# WORKDIR is /opt/spark/work-dir while the standalone worker's default spark.worker.dir is `work`
# relative to cwd, so without --work-dir the effective scratch path would not be the mounted volume.
docker run \
    --detach \
    --network ${SPARK_NETWORK} \
    --hostname ${WORKER_HOSTNAME} \
    --name ${WORKER_CONTAINER_NAME} \
    --volume ${DATA_DIR}:${VOLUME_MOUNT} \
    --publish 7078:7078 \
    --publish 8078:8081 \
    ${DOCKER_IMAGE} \
    /opt/spark/bin/spark-class org.apache.spark.deploy.worker.Worker spark://${MASTER_HOSTNAME}:7077 --host 0.0.0.0 --port 7078 --webui-port 8081 --work-dir ${VOLUME_MOUNT} --cores 2 --memory 128M || exit 1

appbase_verify_running "${WORKER_CONTAINER_NAME}" "${SPARK_VERIFY_RETRIES}" "${SPARK_VERIFY_INTERVAL}" || exit 1

# Container-up is not cluster-join: the worker process retries master registration, so wait until the
# MASTER observes the worker registering before declaring the Spark lifecycle up. A worker that is
# "running" but never resolved/reached the master (wrong or missing shared network) fails HERE.
appbase_wait_ready "${SPARK_READY_RETRIES}" "${SPARK_READY_INTERVAL}" -- appbase_spark_worker_registered "${MASTER_CONTAINER_NAME}" || exit 1

## docker logs -f ${MASTER_CONTAINER_NAME}
