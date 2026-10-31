#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-elasticsearch.sh


docker stop ${CONTAINER_NAME}
docker rm ${CONTAINER_NAME}

## Data now lives in a named volume, not ${HOME}/Temporary/Data (internal work item 522 -- see env-elasticsearch.sh).
## `docker rm` does not remove named volumes, so this must remove it explicitly or "remove permanently"
## would silently leave the cluster's data behind and the next run would resurrect the old indices.
##
## Guarded so this stays idempotent, matching the `rm -rf` it replaced and every sibling remove-*.sh:
## a bare `docker volume rm` errors on a second run, or on a machine that never created the volume.
if docker volume inspect ${DATA_VOLUME} >/dev/null 2>&1; then
    docker volume rm ${DATA_VOLUME}
else
    echo "volume ${DATA_VOLUME} not present -- nothing to remove"
fi
