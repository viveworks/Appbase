#!/bin/bash

BASEDIR=$(dirname "$0")
source $BASEDIR/common-all.sh

# Best-effort bring-up: run every child, restore the working directory, then return non-zero if
# any child failed (or the final restore failed) — the closing cd "$BASEDIR" must not mask a
# child failure (internal work item 1291).
rc=0

if cd "$AEROSPIKE_DIR"; then
    $AEROSPIKE_DIR/start-aerospike.sh || rc=1
else
    echo "docker-start-all: cannot enter $AEROSPIKE_DIR" >&2
    rc=1
fi

if cd "$CASSANDRA_DIR"; then
    $CASSANDRA_DIR/start-cassandra.sh || rc=1
else
    echo "docker-start-all: cannot enter $CASSANDRA_DIR" >&2
    rc=1
fi

if cd "$CLAMAV_DIR"; then
    $CLAMAV_DIR/start-clamav.sh || rc=1
else
    echo "docker-start-all: cannot enter $CLAMAV_DIR" >&2
    rc=1
fi

if cd "$DRUID_DIR"; then
    $DRUID_DIR/start-druid.sh || rc=1
else
    echo "docker-start-all: cannot enter $DRUID_DIR" >&2
    rc=1
fi

if cd "$ELASTICSEARCH_DIR"; then
    $ELASTICSEARCH_DIR/start-elasticsearch.sh || rc=1
else
    echo "docker-start-all: cannot enter $ELASTICSEARCH_DIR" >&2
    rc=1
fi

if cd "$FIREBASE_DIR"; then
    $FIREBASE_DIR/start-firebase.sh || rc=1
else
    echo "docker-start-all: cannot enter $FIREBASE_DIR" >&2
    rc=1
fi

if cd "$KAFKA_DIR"; then
    $KAFKA_DIR/start-kafka.sh || rc=1
else
    echo "docker-start-all: cannot enter $KAFKA_DIR" >&2
    rc=1
fi

if cd "$MARIADB_DIR"; then
    $MARIADB_DIR/start-mariadb.sh || rc=1
else
    echo "docker-start-all: cannot enter $MARIADB_DIR" >&2
    rc=1
fi

if cd "$POSTFIX_DIR"; then
    $POSTFIX_DIR/start-postfix.sh || rc=1
else
    echo "docker-start-all: cannot enter $POSTFIX_DIR" >&2
    rc=1
fi

if cd "$POSTGRESQL_DIR"; then
    $POSTGRESQL_DIR/start-postgresql.sh || rc=1
else
    echo "docker-start-all: cannot enter $POSTGRESQL_DIR" >&2
    rc=1
fi

if cd "$S3_DIR"; then
$S3_DIR/start-s3.sh || rc=1
else
    echo "docker-start-all: cannot enter $S3_DIR" >&2
    rc=1
fi

echo "NOT starting spark..."
#cd $SPARK_DIR
#$SPARK_DIR/start-spark.sh

if cd "$VITESS_DIR"; then
    $VITESS_DIR/start-vitess.sh || rc=1
else
    echo "docker-start-all: cannot enter $VITESS_DIR" >&2
    rc=1
fi

if cd "$ZOOKEEPER_DIR"; then
    $ZOOKEEPER_DIR/start-zookeeper.sh || rc=1
else
    echo "docker-start-all: cannot enter $ZOOKEEPER_DIR" >&2
    rc=1
fi


cd $BASEDIR || rc=1
exit $rc
