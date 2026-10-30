#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-elasticsearch.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "run-elasticsearch: cannot source lifecycle-common.sh" >&2; exit 1; }

: "${ELASTICSEARCH_READY_RETRIES:=90}"; : "${ELASTICSEARCH_READY_INTERVAL:=2}"


docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --volume ${DATA_VOLUME}:${VOLUME_MOUNT} \
    --publish 9200:9200 \
    --publish 9300:9300 \
    --env discovery.type=single-node \
    --env xpack.security.enabled=false \
    --env cluster.routing.allocation.disk.threshold_enabled=false \
    --env "ES_JAVA_OPTS=-Xms256m -Xmx256m" \
    ${DOCKER_IMAGE} || exit 1

appbase_verify_running "${CONTAINER_NAME}" || exit 1
# Port 9200 opens before the node has recovered its cluster state. Require the same HTTP surface the
# client uses and wait for the single-node cluster to reach yellow before admitting live tests.
appbase_wait_ready "${ELASTICSEARCH_READY_RETRIES}" "${ELASTICSEARCH_READY_INTERVAL}" -- \
    appbase_elasticsearch_ready || exit 1


## docker logs -f ${CONTAINER_NAME}
