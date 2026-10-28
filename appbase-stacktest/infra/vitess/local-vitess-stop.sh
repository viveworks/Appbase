#!/bin/bash

APPBASE_STACK_TEST_SCRIPTS_BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
export APPBASE_VITESS_SCRIPTS_BASEDIR="${APPBASE_STACK_TEST_SCRIPTS_BASEDIR}/../../../appbase-java/appbase-client/appbase-client-vitess/infra/local/vitess"
source "${APPBASE_VITESS_SCRIPTS_BASEDIR}/appbase-vitess-stop.sh"

vtadmin_web_stop
vtadmin_api_stop
vtgate_stop
vtorc_stop
vttablet_stop 112
vttablet_stop 111
vttablet_stop 110
vttablet_stop 100
vtctld_stop
verify_stop
