#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
CLIENT_ROOT=$(cd "$BASEDIR/../../.." && pwd)

AEROSPIKE_DIR=$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike
CASSANDRA_DIR=$CLIENT_ROOT/appbase-client-cassandra/infra/docker/cassandra
CLAMAV_DIR=$CLIENT_ROOT/appbase-client-clamav/infra/docker/clamav
DRUID_DIR=$CLIENT_ROOT/appbase-client-druid/infra/docker/druid
ELASTICSEARCH_DIR=$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch
FIREBASE_DIR=$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase
KAFKA_DIR=$CLIENT_ROOT/appbase-client-kafka/infra/docker/kafka
MARIADB_DIR=$CLIENT_ROOT/appbase-client-jdbc/infra/docker/mariadb
POSTFIX_DIR=$CLIENT_ROOT/appbase-client-smtp/infra/docker/postfix
POSTGRESQL_DIR=$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql
# The S3 backend is the owner-selected pinned MinIO lifecycle under the current AWS S3 module. The
# obsolete MINIO_DIR pointed at a module deleted in 81c9f2389; S3_DIR is the canonical replacement
# wired by N09 (internal work item 1299), regardless of the backend's product name.
S3_DIR=$CLIENT_ROOT/appbase-client-cloud/appbase-client-cloud-aws/appbase-client-cloud-aws-s3/infra/docker/s3
SPARK_DIR=$CLIENT_ROOT/appbase-client-spark/infra/docker/spark
VITESS_DIR=$CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess
ZOOKEEPER_DIR=$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper
