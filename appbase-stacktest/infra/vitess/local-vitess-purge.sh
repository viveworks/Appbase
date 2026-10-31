#!/bin/bash

APPBASE_STACK_TEST_SCRIPTS_BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
# The purge script lives under the appbase-client-vitess module. (The former path omitted that segment
# and never sourced it, so this entry point silently did nothing — A10-08 / internal work item 1489.)
export APPBASE_VITESS_SCRIPTS_BASEDIR="${APPBASE_STACK_TEST_SCRIPTS_BASEDIR}/../../../appbase-java/appbase-client/appbase-client-vitess/infra/local/vitess"
source "${APPBASE_VITESS_SCRIPTS_BASEDIR}/appbase-vitess-purge.sh"

vitess_purge
