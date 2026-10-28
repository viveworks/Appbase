#!/bin/bash

BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-redis.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "remove-redis: cannot source lifecycle-common.sh" >&2; exit 1; }

appbase_remove_container "${CONTAINER_NAME}"
