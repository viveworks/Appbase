#!/bin/bash


BASEDIR=$(cd -- "$(dirname "$0")" >/dev/null 2>&1 && pwd -P) || exit 1
source "$BASEDIR/env-s3.sh" || exit 1
source "$BASEDIR/../../../../../../appbase-client-core/infra/docker/lifecycle-common.sh" || { echo "stop-s3: cannot source lifecycle-common.sh" >&2; exit 1; }

appbase_stop_container "${CONTAINER_NAME}"
