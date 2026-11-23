#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-vitess.sh
source $BASEDIR/../../../../appbase-client-core/infra/docker/lifecycle-common.sh || { echo "stop-vitess: cannot source lifecycle-common.sh" >&2; exit 1; }

# appbase_stop_container is idempotent on an absent container -- this is the Vitess step
# appbase-stacktest/infra/stop-infra.sh now defaults to, and that orchestrator runs under `set -e`
# with PostgreSQL/ZooKeeper stopped AFTER Vitess; a bare `docker stop` that fails outright when
# Vitess was never started (e.g. its own run-vitess.sh failed) would abort the orchestrator before
# it ever reaches them.
appbase_stop_container "${CONTAINER_NAME}"
