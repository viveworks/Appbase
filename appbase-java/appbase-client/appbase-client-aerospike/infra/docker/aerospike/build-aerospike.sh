#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-aerospike.sh


# BuildKit's default tty progress was absent from the hosted internal work item 2650 failure log. The environment form
# is portable to older Docker engines (which ignore it and retain their ordinary verbose output).
BUILDKIT_PROGRESS=${BUILDKIT_PROGRESS:-plain} docker build -t "${TAG}" "${BASEDIR}"
