#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-clamav.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "run-clamav: cannot source lifecycle-common.sh" >&2; exit 1; }

# Readiness budget is env-tunable (defaults are the production values); the lifecycle test overrides it
# so its behavioural readiness check does not sleep for minutes on the failure path.
: "${CLAMAV_READY_RETRIES:=60}"; : "${CLAMAV_READY_INTERVAL:=3}"

appbase_require_image "${DOCKER_IMAGE}" || exit 1

# clamav/clamav is already an upstream-official image; only the tag moved (0.104.0 → 1.4). The
# CLAMAV_NO_* env contract is unchanged. clamd exposes its scan socket on 3310 once signatures load.
docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --publish 3310:3310 \
    --env CLAMAV_NO_CLAMD=false \
    --env CLAMAV_NO_FRESHCLAMD=true \
    --env CLAMAV_NO_MILTERD=true \
    ${DOCKER_IMAGE} || exit 1

appbase_verify_running "${CONTAINER_NAME}" || exit 1
# Probe 127.0.0.1 (not `localhost`): `--publish 3310:3310` binds the IPv4 loopback, but on a host where
# `localhost` resolves to ::1 first a bash /dev/tcp probe of `localhost` would miss the published port.
appbase_wait_ready "${CLAMAV_READY_RETRIES}" "${CLAMAV_READY_INTERVAL}" -- appbase_tcp_probe 127.0.0.1 3310 || exit 1

## docker logs -f ${CONTAINER_NAME}
