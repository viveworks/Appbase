#!/bin/bash


BASEDIR=$(cd -- "$(dirname "$0")" >/dev/null 2>&1 && pwd -P) || exit 1
source "$BASEDIR/env-s3.sh" || exit 1


# MinIO ships as an upstream-official image; there is nothing to build locally. Pulling the exact
# pin here is what makes `run-s3.sh` fast and makes a bad pin fail HERE, with the tag named, rather
# than inside a detached run.
docker pull "${DOCKER_IMAGE}"
