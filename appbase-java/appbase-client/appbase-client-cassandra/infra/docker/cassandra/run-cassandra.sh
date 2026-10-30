#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-cassandra.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "run-cassandra: cannot source lifecycle-common.sh" >&2; exit 1; }

# Readiness budget is env-tunable (defaults are the production values); the lifecycle test overrides it
# so its behavioural readiness check does not sleep for minutes on the failure path.
: "${CASSANDRA_READY_RETRIES:=60}"; : "${CASSANDRA_READY_INTERVAL:=3}"

appbase_require_image "${DOCKER_IMAGE}" || exit 1

mkdir -p ${DATA_DIR}

# Upstream-official `cassandra` (not bitnami): no auth by default, DC/RACK honoured only under the
# GossipingPropertyFileSnitch, heap via MAX_HEAP_SIZE/HEAP_NEWSIZE, data dir /var/lib/cassandra.
docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --volume ${DATA_DIR}:${VOLUME_MOUNT} \
    --publish 9042:9042 \
    --publish 7197:7000 \
    --publish 7199:7199 \
    --env CASSANDRA_CLUSTER_NAME=cassandra-local \
    --env CASSANDRA_DC=datacenter1 \
    --env CASSANDRA_RACK=rack1 \
    --env CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch \
    --env MAX_HEAP_SIZE=2G \
    --env HEAP_NEWSIZE=512M \
    ${DOCKER_IMAGE} || exit 1

appbase_verify_running "${CONTAINER_NAME}" || exit 1
# Probe 127.0.0.1 (not `localhost`): `--publish 9042:9042` binds the IPv4 loopback, but on a host where
# `localhost` resolves to ::1 first a bash /dev/tcp probe of `localhost` would miss the published port.
appbase_wait_ready "${CASSANDRA_READY_RETRIES}" "${CASSANDRA_READY_INTERVAL}" -- appbase_tcp_probe 127.0.0.1 9042 || exit 1

## docker logs -f ${CONTAINER_NAME}
