#!/bin/bash

# Single-node Apache Kafka in KRaft mode — one process is both broker and controller, with NO
# ZooKeeper (owner-ratified move off the retired bitnami catalog; see
# docs/spec/1.0.0/P04/decisions/E01-image-versions.md, epic P04·E01 internal work item 1135, task N02/internal work item 1292).
export DOCKER_IMAGE="apache/kafka:3.7.1"
export HOSTNAME="kafka.local"
export CONTAINER_NAME="${HOSTNAME}"
export DATA_DIR="$HOME/Temporary/Data/Kafka"
# apache/kafka stores its KRaft log segments here (bitnami mounted /bitnami/kafka). VOLUME_MOUNT
# and KAFKA_LOG_DIRS below are kept equal so the bind-mount is where Kafka actually writes.
export VOLUME_MOUNT="/var/lib/kafka/data"

# KRaft single-node configuration. The apache/kafka image maps a KAFKA_<A>_<B> env var onto the
# <a>.<b> server property (bitnami used the KAFKA_CFG_* prefix instead — do not reintroduce it).
# CONTAINER_NAME / HOSTNAME / the published 9092 port are unchanged, so the Kafka client's default
# bootstrap of kafka.local:9092 needs no edit.
export KAFKA_NODE_ID=1
export KAFKA_PROCESS_ROLES=broker,controller
export KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
export KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
export KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
export KAFKA_CONTROLLER_QUORUM_VOTERS=1@${HOSTNAME}:9093
export KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER
export KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT
export KAFKA_LOG_DIRS=/var/lib/kafka/data
# single-node: no replicas to spread the internal topics across
export KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
export KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
export KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1
# log.cleaner.dedupe.buffer.size is a standard Kafka property (bitnami just prefixed it KAFKA_CFG_); its
# default is ~128 MiB, which alone is the whole -Xmx128M heap run-kafka.sh runs this broker under. The
# on-heap log cleaner compacts __consumer_offsets/__transaction_state, so the default would OOM the broker
# under load. Pin it to 2 MiB (the value carried since 2022) so it fits the heap. run-kafka.sh forwards it.
export KAFKA_LOG_CLEANER_DEDUPE_BUFFER_SIZE=2097152
