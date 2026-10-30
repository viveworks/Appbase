#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-cassandra.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "remove-cassandra: cannot source lifecycle-common.sh" >&2; exit 1; }

# Teardown is best-effort but HONEST: run every step even if an earlier one fails, accumulate a non-zero
# status, and exit non-zero if the container was not removed. Before this the docker commands ran
# unchecked and the trailing local `rm -rf` (which normally succeeds) became the exit status, so the
# script reported a clean stop even when the container was NOT removed — which the orchestrator
# (docker-remove-all.sh) then aggregated as success (internal work item 1291). `appbase_remove_container` treats an
# already-absent container as idempotent success and any other docker failure as a real, surfaced error.
rc=0

appbase_remove_container "${CONTAINER_NAME}" || rc=1

# Local test data dir. Its removal is not part of the Docker teardown contract, but a failure to remove
# it is surfaced rather than allowed to mask (or be masked by) the teardown status.
rm -rf "${DATA_DIR}" || rc=1

exit $rc
