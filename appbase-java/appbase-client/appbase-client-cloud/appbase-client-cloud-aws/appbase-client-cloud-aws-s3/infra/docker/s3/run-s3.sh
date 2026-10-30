#!/bin/bash


BASEDIR=$(cd -- "$(dirname "$0")" >/dev/null 2>&1 && pwd -P) || exit 1
source "$BASEDIR/env-s3.sh" || exit 1
source "$BASEDIR/../../../../../../appbase-client-core/infra/docker/lifecycle-common.sh" || { echo "run-s3: cannot source lifecycle-common.sh" >&2; exit 1; }

# Readiness budget is env-tunable (defaults are the production values); a caller that only wants to
# know whether the container came up must be able to bound the failure path instead of sleeping for
# minutes. MinIO's cold start can initialise its filesystem, so the default budget is generous.
: "${S3_READY_RETRIES:=60}"; : "${S3_READY_INTERVAL:=3}"

appbase_require_image "${DOCKER_IMAGE}" || exit 1

# Chainguard MinIO deliberately runs as uid 65532. A freshly-created hosted-runner bind
# directory is normally 0755 and owned by the runner uid, so the container can traverse it but
# cannot initialise .minio.sys. This directory contains disposable emulator data only; make the
# bind target writable before starting the non-root image. Do not solve this by running MinIO as
# root or by changing the image's intended uid.
mkdir -p "${DATA_DIR}" || exit 1
chmod 0777 "${DATA_DIR}" || exit 1

# Exact-pinned MinIO with static, non-secret emulator credentials.
docker run \
    --detach \
    --hostname "${HOSTNAME}" \
    --name "${CONTAINER_NAME}" \
    --volume "${DATA_DIR}:${VOLUME_MOUNT}" \
    --publish 127.0.0.1:9000:9000 \
    --env "MINIO_ROOT_USER=${MINIO_ROOT_USER}" \
    --env "MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD}" \
    "${DOCKER_IMAGE}" server "${VOLUME_MOUNT}" || exit 1

# ⚠️ BOUND TO LOOPBACK DELIBERATELY. These are public test credentials and MinIO is not an IAM
# proof, so a default `--publish 9000:9000` would expose a read/write S3 API to the network.
# postgres and mariadb publish wide but at least carry credentials; aerospike/cassandra/druid carry
# neither, which is a pre-existing exposure this task does not widen and does not fix. Everything that
# consumes this backend addresses it as localhost (the endpoint override, the readiness probe), so the
# narrower binding costs nothing here.
#
# `docker run --detach` exits 0 even when the container dies on the next tick (S07/internal work item 522), so "started"
# is not proof of anything until the container is verified still running.
appbase_verify_running "${CONTAINER_NAME}" || exit 1
# Probe 127.0.0.1 (not `localhost`): the publish above binds the IPv4 loopback, but on a host where
# `localhost` resolves to ::1 first a bash /dev/tcp probe of `localhost` would miss the published port.
appbase_wait_ready "${S3_READY_RETRIES}" "${S3_READY_INTERVAL}" -- appbase_tcp_probe 127.0.0.1 9000 \
  || { echo "run-s3: MinIO S3 readiness timed out on 127.0.0.1:9000" >&2; exit 1; }
appbase_wait_ready "${S3_READY_RETRIES}" "${S3_READY_INTERVAL}" -- \
  curl --fail --silent --show-error --max-time 2 http://127.0.0.1:9000/minio/health/ready \
  || { echo "run-s3: MinIO application readiness timed out" >&2; exit 1; }


## docker logs -f ${CONTAINER_NAME}
