#!/bin/bash

APPBASE_SERVICE_SCRIPTS_BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
# vitess/ is a sibling of service-schema/ under infra/local/. (The former path pointed at a
# non-existent appbase-client/infra/local/vitess and never sourced the purge script — A10-08 / internal work item 1489.)
export APPBASE_VITESS_SCRIPTS_BASEDIR="${APPBASE_SERVICE_SCRIPTS_BASEDIR}/../vitess"
source "${APPBASE_VITESS_SCRIPTS_BASEDIR}/appbase-vitess-purge.sh"

vitess_purge
