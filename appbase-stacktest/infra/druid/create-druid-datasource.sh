#!/bin/bash

set -euo pipefail

BASEDIR=$(cd -- "$(dirname -- "$0")" >/dev/null 2>&1 && pwd -P)
: "${DRUID_DATASOURCE_READY_RETRIES:=90}"
: "${DRUID_DATASOURCE_READY_INTERVAL:=2}"

cd "$BASEDIR"
docker exec -u 0 druid.local rm -rf /opt/druid-volume/data/healthcheck
docker exec -u 0 druid.local mkdir -p /opt/druid-volume/data/healthcheck
docker cp "$BASEDIR/stacktest-healthcheck.csv" druid.local:/opt/druid-volume/data/healthcheck

echo "Submitting StackTest Druid datasource ingestion task..."
curl --fail --silent --show-error \
    --data-binary "@druid-spec.json" \
    --header "Content-Type: application/json" \
    --request POST \
    http://localhost:8888/druid/indexer/v1/task >/dev/null

query='{"query":"SELECT * FROM appbase_stacktest_healthcheck_data"}'
attempt=1
while [ "$attempt" -le "$DRUID_DATASOURCE_READY_RETRIES" ]; do
    if response=$(curl --fail --silent --show-error \
        --header 'Content-Type: application/json' \
        --data "$query" \
        http://localhost:8888/druid/v2/sql 2>/dev/null) &&
        case "$response" in *'"userId"'*) true ;; *) false ;; esac
    then
        echo "StackTest Druid datasource is queryable."
        exit 0
    fi

    if [ "$attempt" -lt "$DRUID_DATASOURCE_READY_RETRIES" ]; then
        sleep "$DRUID_DATASOURCE_READY_INTERVAL"
    fi
    attempt=$((attempt + 1))
done

echo "StackTest Druid datasource did not become queryable after ${DRUID_DATASOURCE_READY_RETRIES} attempts" >&2
exit 1
