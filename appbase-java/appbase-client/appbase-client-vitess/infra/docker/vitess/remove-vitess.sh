#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-vitess.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "remove-vitess: cannot source lifecycle-common.sh" >&2; exit 1; }

rc=0
appbase_remove_container "${CONTAINER_NAME}" || rc=1
appbase_remove_volume "${DATA_VOLUME}" || rc=1
exit "$rc"
