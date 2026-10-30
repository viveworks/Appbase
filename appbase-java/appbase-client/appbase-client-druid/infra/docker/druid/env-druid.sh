#!/bin/bash

export TAG="viveworks.com/infra/local/druid"
export HOSTNAME="druid.local"
export CONTAINER_NAME="druid.local"
export DATA_DIR="${HOME}/Temporary/Data/Druid"
export VOLUME_MOUNT="/opt/druid-volume"

appbase_druid_sql_ready() {
    curl -fsS --connect-timeout 2 --max-time 5 \
        -H 'Content-Type: application/json' \
        --data '{"query":"SELECT 1"}' \
        http://127.0.0.1:8082/druid/v2/sql/ >/dev/null
}
