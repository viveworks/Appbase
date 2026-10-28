#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-mariadb.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "run-mariadb: cannot source lifecycle-common.sh" >&2; exit 1; }

# Readiness budget is env-tunable (defaults are the production values); the lifecycle test overrides it
# so its behavioural readiness check does not sleep for minutes on the failure path.
: "${MARIADB_READY_RETRIES:=60}"; : "${MARIADB_READY_INTERVAL:=3}"

appbase_require_image "${DOCKER_IMAGE}" || exit 1

mkdir -p ${DATA_DIR}

# Upstream-official `mariadb` (not bitnami): same MARIADB_* env contract, data dir /var/lib/mysql.
docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --volume ${DATA_DIR}:${VOLUME_MOUNT} \
    --publish 3306:3306 \
    --env MARIADB_ROOT_PASSWORD=password \
    --env MARIADB_DATABASE=appbase-test-mariadb-db \
    ${DOCKER_IMAGE} || exit 1

appbase_verify_running "${CONTAINER_NAME}" || exit 1
# Probe 127.0.0.1 (not `localhost`): `--publish 3306:3306` binds the IPv4 loopback, but on a host where
# `localhost` resolves to ::1 first a bash /dev/tcp probe of `localhost` would miss the published port.
appbase_wait_ready "${MARIADB_READY_RETRIES}" "${MARIADB_READY_INTERVAL}" -- appbase_tcp_probe 127.0.0.1 3306 || exit 1


## docker logs -f ${CONTAINER_NAME}
