#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-clamav.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "remove-clamav: cannot source lifecycle-common.sh" >&2; exit 1; }

# Teardown is best-effort but HONEST: run the container removal and exit non-zero if it did not happen.
# Before this the docker commands ran unchecked and a trailing local command's exit status stood, so the
# script reported a clean stop even when the container was NOT removed — which the orchestrator
# (docker-remove-all.sh) then aggregated as success (internal work item 1291). clamav pins no DATA_DIR (see env-clamav.sh),
# so the container removal alone decides the status. `appbase_remove_container` treats an already-absent
# container as idempotent success and any other docker failure as a real, surfaced error.
rc=0

appbase_remove_container "${CONTAINER_NAME}" || rc=1

exit $rc
