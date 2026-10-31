#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-kafka.sh
source $BASEDIR/wait-kafka-ready.sh


mkdir -p ${DATA_DIR}

# Single-node Apache Kafka in KRaft mode: broker + controller in one process, no ZooKeeper. The
# KRaft config comes from env-kafka.sh; the apache/kafka entrypoint formats the log dir on first run.
docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --volume ${DATA_DIR}:${VOLUME_MOUNT} \
    --publish 9092:9092 \
    --env KAFKA_HEAP_OPTS=-Xmx128M \
    --env KAFKA_NODE_ID=${KAFKA_NODE_ID} \
    --env KAFKA_PROCESS_ROLES=${KAFKA_PROCESS_ROLES} \
    --env KAFKA_LISTENERS=${KAFKA_LISTENERS} \
    --env KAFKA_ADVERTISED_LISTENERS=${KAFKA_ADVERTISED_LISTENERS} \
    --env KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=${KAFKA_LISTENER_SECURITY_PROTOCOL_MAP} \
    --env KAFKA_CONTROLLER_QUORUM_VOTERS=${KAFKA_CONTROLLER_QUORUM_VOTERS} \
    --env KAFKA_CONTROLLER_LISTENER_NAMES=${KAFKA_CONTROLLER_LISTENER_NAMES} \
    --env KAFKA_INTER_BROKER_LISTENER_NAME=${KAFKA_INTER_BROKER_LISTENER_NAME} \
    --env KAFKA_LOG_DIRS=${KAFKA_LOG_DIRS} \
    --env KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=${KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR} \
    --env KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=${KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR} \
    --env KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=${KAFKA_TRANSACTION_STATE_LOG_MIN_ISR} \
    --env KAFKA_LOG_CLEANER_DEDUPE_BUFFER_SIZE=${KAFKA_LOG_CLEANER_DEDUPE_BUFFER_SIZE} \
    ${DOCKER_IMAGE}
rc=$?
if [ "$rc" -ne 0 ]; then
  echo "run-kafka: docker run failed (rc=$rc); no broker started." >&2
  exit "$rc"
fi

# Readiness: the broker counts as up only once the published PLAINTEXT port accepts connections.
wait_kafka_ready localhost 9092 || exit 1


## docker logs -f ${CONTAINER_NAME}
