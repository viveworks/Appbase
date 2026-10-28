#!/bin/bash

BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )

cd ${BASEDIR}
docker exec -it -u 0 druid.local rm -rf /opt/druid-volume/data/healthcheck
docker exec -it -u 0 druid.local mkdir -p /opt/druid-volume/data/healthcheck
docker cp $BASEDIR/stacktest-healthcheck.csv druid.local:/opt/druid-volume/data/healthcheck

echo "Ingest sample data..."
curl -vvv \
    -d "@druid-spec.json" \
    -H "Content-Type: application/json" \
    -X POST \
    http://localhost:8888/druid/indexer/v1/task

local seconds=5
echo "Waiting ${seconds}s for ingestion..."
sleep ${seconds}


echo "Querying sample data..."
curl -vvv \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{"query": "SELECT * FROM appbase_stacktest_healthcheck_data"}' \
  http://localhost:8888/druid/v2/sql