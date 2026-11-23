#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-vitess.sh


# Use the helper's own directory as the context so callers do not have to cd here first. The hosted
# campaign invokes this script by absolute path from the repository root.
BUILDKIT_PROGRESS=${BUILDKIT_PROGRESS:-plain} docker build --no-cache --force-rm -t "${TAG}" "${BASEDIR}"
