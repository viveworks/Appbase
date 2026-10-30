#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-zookeeper.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "run-zookeeper: cannot source lifecycle-common.sh" >&2; exit 1; }

# Readiness budget is env-tunable (defaults are the production values); the lifecycle test overrides it
# so its behavioural readiness check does not sleep for minutes on the failure path.
: "${ZOOKEEPER_READY_RETRIES:=60}"; : "${ZOOKEEPER_READY_INTERVAL:=3}"

appbase_require_image "${DOCKER_IMAGE}" || exit 1

mkdir -p ${DATA_DIR}

# Upstream-official `zookeeper` (not bitnami): anonymous by default, standalone single node,
# data dir /data, JVM sizing via JVMFLAGS.
docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --volume ${DATA_DIR}:${VOLUME_MOUNT} \
    --publish 2181:2181 \
    --publish 2888:2888 \
    --publish 3888:3888 \
    --env ZOO_STANDALONE_ENABLED=true \
    --env JVMFLAGS="-Xmx128M" \
    ${DOCKER_IMAGE} || exit 1

appbase_verify_running "${CONTAINER_NAME}" || exit 1
# Probe 127.0.0.1 (not `localhost`): `--publish 2181:2181` binds the IPv4 loopback, but on a host where
# `localhost` resolves to ::1 first a bash /dev/tcp probe of `localhost` would miss the published port.
appbase_wait_ready "${ZOOKEEPER_READY_RETRIES}" "${ZOOKEEPER_READY_INTERVAL}" -- appbase_tcp_probe 127.0.0.1 2181 || exit 1

## docker logs -f ${CONTAINER_NAME}
