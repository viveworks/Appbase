#!/bin/bash
#
# env-s3.sh — owner-selected, no-account MinIO backend for the gated S3 IT (internal work item 2584).
# The tag+digest pin is anonymously pullable and immutable; publication stays loopback-only.
export DOCKER_IMAGE="cgr.dev/chainguard/minio@sha256:2136d593beacf4461187035eca949fe49abd589d5079735025c711ca58fee0c8"
export HOSTNAME="minio-s3.local"
export CONTAINER_NAME="${HOSTNAME}"
export DATA_DIR="$HOME/Temporary/Data/MinIO"
export VOLUME_MOUNT="/data"
export MINIO_ROOT_USER="root"
export MINIO_ROOT_PASSWORD="password"
