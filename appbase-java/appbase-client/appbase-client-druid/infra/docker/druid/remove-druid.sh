#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-druid.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "remove-druid: cannot source lifecycle-common.sh" >&2; exit 1; }

rc=0
appbase_remove_container "${CONTAINER_NAME}" || rc=1

if [[ -d "${DATA_DIR}" ]]; then
    docker run --rm \
        --user 0:0 \
        --volume "${DATA_DIR}:/appbase-cleanup" \
        --entrypoint /bin/sh \
        "${TAG}" \
        -c 'rm -rf /appbase-cleanup/* /appbase-cleanup/.[!.]* /appbase-cleanup/..?*' || rc=1
    rmdir "${DATA_DIR}" || rc=1
fi

exit $rc
