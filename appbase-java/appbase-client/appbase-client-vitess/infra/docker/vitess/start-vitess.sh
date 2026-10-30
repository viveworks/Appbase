#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-vitess.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "start-vitess: cannot source lifecycle-common.sh" >&2; exit 1; }

: "${VITESS_READY_RETRIES:=60}"; : "${VITESS_READY_INTERVAL:=3}"

docker start "${CONTAINER_NAME}" || exit 1
appbase_verify_running "${CONTAINER_NAME}" || exit 1

vitess_schema_ready() {
    docker exec "${CONTAINER_NAME}" mysql --no-defaults -h 127.0.0.1 -P "${VTGATE_MYSQL_PORT}" \
        -e 'SELECT 1 FROM `appbase-stacktest-unsharded-keyspace`.stacktest_product LIMIT 0;
            SELECT 1 FROM `appbase-stacktest-sharded-keyspace`.stacktest_customer LIMIT 0;
            SELECT 1 FROM `appbase-stacktest-sharded-keyspace`.appbase_UserAccount LIMIT 0;' \
        >/dev/null 2>&1
}

# A restarted vttestserver container accepts the published gRPC port before vtgate's tablets and
# schema are usable. Require the same real vtgate/schema round trip as initial startup; otherwise a
# restored-control IT can see UNAVAILABLE or an HTTP/2 end-of-stream while child processes recover.
appbase_wait_ready "${VITESS_READY_RETRIES}" "${VITESS_READY_INTERVAL}" -- vitess_schema_ready || exit 1
