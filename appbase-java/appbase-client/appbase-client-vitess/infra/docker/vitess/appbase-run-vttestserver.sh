#!/bin/bash

set -euo pipefail

STAGED_SCHEMA=/vt/schema-stage
OWNED_SCHEMA=/vt/schema
COPY_MARKER="$OWNED_SCHEMA/.appbase-schema-copy-complete"

# The host-side staging directory is removed when run-vitess.sh finishes. Docker can recreate its
# now-missing bind source as an empty directory when this same container is restarted. Preserve the
# verified copy in the container's writable layer for that restart instead of deleting it.
if [ -d "$STAGED_SCHEMA" ] && [ -n "$(ls -A "$STAGED_SCHEMA")" ]; then
  rm -rf "$OWNED_SCHEMA"
  mkdir -p "$OWNED_SCHEMA"
  cp -R "$STAGED_SCHEMA"/. "$OWNED_SCHEMA"/
elif [ ! -f "$COPY_MARKER" ]; then
  echo "appbase-vttestserver: staged schema is unavailable and no verified container copy exists" >&2
  exit 2
fi

for keyspace in ${KEYSPACES//,/ }; do
  if [ ! -s "$OWNED_SCHEMA/$keyspace/vschema.json" ] \
      || ! compgen -G "$OWNED_SCHEMA/$keyspace/*.sql" >/dev/null; then
    echo "appbase-vttestserver: copied schema fixtures are missing for keyspace: $keyspace" >&2
    exit 2
  fi
done

touch "$COPY_MARKER"
exec /vt/run.sh "8.0.21-vitess"
