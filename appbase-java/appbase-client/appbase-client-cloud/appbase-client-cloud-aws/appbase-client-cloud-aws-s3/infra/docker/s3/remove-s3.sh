#!/bin/bash


BASEDIR=$(cd -- "$(dirname "$0")" >/dev/null 2>&1 && pwd -P) || exit 1
source "$BASEDIR/env-s3.sh" || exit 1
source "$BASEDIR/../../../../../../appbase-client-core/infra/docker/lifecycle-common.sh" || { echo "remove-s3: cannot source lifecycle-common.sh" >&2; exit 1; }

# Teardown is best-effort but HONEST: if the container cannot be removed, preserve its bound data so a
# later successful removal can still clean up the same state. The pre-N01 shape let a trailing local
# cleanup hide Docker failure; cleanup must neither mask that status nor destroy data still owned by a
# possibly-running container (internal work item 1291).
rc=0
appbase_remove_container "${CONTAINER_NAME}" || rc=1

# Local state dir. Its removal is not part of the Docker teardown contract, but a failure to remove it
# is surfaced rather than allowed to mask (or be masked by) the teardown status.
case "${HOME:-}" in
  ""|/|//*|/*/../*|*/..) echo "remove-s3: refusing unsafe DATA_DIR '${DATA_DIR:-}'" >&2; exit 1 ;;
  /*) ;;
  *) echo "remove-s3: refusing unsafe DATA_DIR '${DATA_DIR:-}'" >&2; exit 1 ;;
esac
[ "${DATA_DIR:-}" = "$HOME/Temporary/Data/MinIO" ] \
  || { echo "remove-s3: refusing unsafe DATA_DIR '${DATA_DIR:-}'" >&2; exit 1; }

# MinIO creates `.minio.sys` as its container user, which an unprivileged hosted-runner user cannot
# reliably remove from the bind mount. Reuse the already-required pinned MinIO image as container root
# to empty that validated mount, then remove only the now-empty host directory. This avoids sudo and a
# second cleanup image while retaining an honest non-zero result for either teardown failure.
if [[ "$rc" -eq 0 && -d "${DATA_DIR}" ]]; then
  docker run --rm \
    --user 0:0 \
    --volume "${DATA_DIR}:/appbase-cleanup" \
    --entrypoint /bin/sh \
    "${DOCKER_IMAGE}" \
    -c 'rm -rf /appbase-cleanup/* /appbase-cleanup/.[!.]* /appbase-cleanup/..?*' || rc=1
  rmdir "${DATA_DIR}" || rc=1
fi

exit $rc
