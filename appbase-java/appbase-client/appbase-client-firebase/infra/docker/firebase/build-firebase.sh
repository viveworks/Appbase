#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-firebase.sh


# Callers invoke this script from several working directories (including the repository root in the
# epic campaign), so the Docker context must be anchored to the directory containing the Dockerfile.
# Do not let the following mkdir mask a failed build and leave `docker run` trying the local-looking
# viveworks.com tag as a real remote registry reference.
BUILDKIT_PROGRESS=${BUILDKIT_PROGRESS:-plain} docker build -t "${TAG}" "${BASEDIR}" || exit 1
mkdir -p "${DATA_DIR}"
