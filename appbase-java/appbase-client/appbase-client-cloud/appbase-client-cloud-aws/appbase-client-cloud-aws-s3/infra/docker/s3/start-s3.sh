#!/bin/bash


BASEDIR=$(cd -- "$(dirname "$0")" >/dev/null 2>&1 && pwd -P) || exit 1
source "$BASEDIR/env-s3.sh" || exit 1
source "$BASEDIR/../../../../../../appbase-client-core/infra/docker/lifecycle-common.sh" || { echo "start-s3: cannot source lifecycle-common.sh" >&2; exit 1; }

: "${S3_READY_RETRIES:=60}"; : "${S3_READY_INTERVAL:=3}"

docker start "${CONTAINER_NAME}" || exit 1
appbase_verify_running "${CONTAINER_NAME}" || exit 1
appbase_wait_ready "${S3_READY_RETRIES}" "${S3_READY_INTERVAL}" -- appbase_tcp_probe 127.0.0.1 9000 \
  || { echo "start-s3: MinIO S3 readiness timed out on 127.0.0.1:9000" >&2; exit 1; }
appbase_wait_ready "${S3_READY_RETRIES}" "${S3_READY_INTERVAL}" -- \
  curl --fail --silent --show-error --max-time 2 http://127.0.0.1:9000/minio/health/ready \
  || { echo "start-s3: MinIO application readiness timed out" >&2; exit 1; }
