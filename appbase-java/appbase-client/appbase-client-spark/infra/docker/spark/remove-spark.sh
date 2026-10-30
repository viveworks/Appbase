#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-spark.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "remove-spark: cannot source lifecycle-common.sh" >&2; exit 1; }

# Teardown is best-effort but HONEST: run every step even if an earlier one fails, accumulate a non-zero
# status, and exit non-zero if any REQUIRED teardown did not happen. Before this the docker commands ran
# unchecked and the trailing local `rm -rf` (which normally succeeds) became the exit status, so the
# script reported a clean stop even when neither container nor the network was removed — which the
# orchestrator (docker-remove-all.sh) then aggregated as success. `appbase_remove_container` /
# `appbase_remove_network` treat an already-absent object as idempotent success and any other docker
# failure as a real, surfaced error.
rc=0

echo "Removing Spark Worker"
appbase_remove_container "${WORKER_CONTAINER_NAME}" || rc=1

echo "Removing Spark Master"
appbase_remove_container "${MASTER_CONTAINER_NAME}" || rc=1

# Tear down the user-defined network created by run-spark.sh — only after both containers are gone, since
# a network cannot be removed while a container is still attached.
echo "Removing Spark network"
appbase_remove_network "${SPARK_NETWORK}" || rc=1

# Local test data dir. Its removal is not part of the Docker teardown contract, but a failure to remove
# it is surfaced rather than allowed to mask (or be masked by) the teardown status.
rm -rf "${DATA_DIR}" || rc=1

exit $rc
