#!/bin/bash

APPBASE_SERVICE_TEST_SCRIPTS_BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
export APPBASE_VITESS_SCRIPTS_BASEDIR="${APPBASE_SERVICE_TEST_SCRIPTS_BASEDIR}/../vitess"
source "${APPBASE_VITESS_SCRIPTS_BASEDIR}/appbase-vitess-stop.sh"

vtadmin_web_stop
vtadmin_api_stop
vtgate_stop
vtorc_stop
vttablet_stop 212
vttablet_stop 211
vttablet_stop 210
vtctld_stop
verify_stop
