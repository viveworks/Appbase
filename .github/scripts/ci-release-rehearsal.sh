#!/usr/bin/env bash
# Credential-free release:prepare dry-run. This must never perform or publish a release.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

TMP="$(mktemp -d)"
trap 'rm -rf -- "$TMP"' EXIT
POMS_BEFORE="$TMP/poms-before.sha256"
POMS_AFTER="$TMP/poms-after.sha256"
TAGS_BEFORE="$TMP/tags-before"
TAGS_AFTER="$TMP/tags-after"
HEAD_BEFORE="$(git rev-parse HEAD)"

snapshot_poms() {
  local output="$1" file count=0
  : > "$output"
  while IFS= read -r -d '' file; do
    sha256sum "$file" >> "$output"
    count=$((count + 1))
  done < <(git ls-files -z -- '*pom.xml')
  [[ "$count" -gt 0 ]] || {
    echo 'release rehearsal: no tracked pom.xml files found' >&2
    return 1
  }
}

verify_repository_integrity() {
  local phase="$1" failed=0 leftovers
  snapshot_poms "$POMS_AFTER" || failed=1
  if ! cmp -s "$POMS_BEFORE" "$POMS_AFTER"; then
    echo "release rehearsal: tracked pom.xml content changed during $phase" >&2
    diff -u "$POMS_BEFORE" "$POMS_AFTER" >&2 || true
    failed=1
  fi
  if [[ "$(git rev-parse HEAD)" != "$HEAD_BEFORE" ]]; then
    echo "release rehearsal: HEAD changed during $phase" >&2
    failed=1
  fi
  git tag --list | LC_ALL=C sort > "$TAGS_AFTER"
  if ! cmp -s "$TAGS_BEFORE" "$TAGS_AFTER"; then
    echo "release rehearsal: local tags changed during $phase" >&2
    diff -u "$TAGS_BEFORE" "$TAGS_AFTER" >&2 || true
    failed=1
  fi
  leftovers="$(git status --porcelain --untracked-files=all | sed -E 's/^.. //' | grep -E '(^|/)(release\.properties|pom\.xml\.(releaseBackup|tag|next))$' || true)"
  if [[ "$phase" == 'release:clean' && -n "$leftovers" ]]; then
    echo 'release rehearsal: release:clean left generated release files behind' >&2
    printf '%s\n' "$leftovers" >&2
    failed=1
  fi
  return "$failed"
}

snapshot_poms "$POMS_BEFORE"
git tag --list | LC_ALL=C sort > "$TAGS_BEFORE"

bash ./build.sh --mode compile
verify_repository_integrity 'whole-repository compile'

set +e
mvn -B -ntp -P release-profile release:prepare \
  -DdryRun=true \
  -DpushChanges=false \
  -DreleaseVersion=1.0.0 \
  -DdevelopmentVersion=1.0.1-SNAPSHOT \
  -Dgpg.skip=true \
  -Darguments="-Dgpg.skip=true -DskipTests"
prepare_rc=$?
verify_repository_integrity 'release:prepare -DdryRun=true'
prepare_integrity_rc=$?

mvn -B -ntp -P release-profile release:clean
clean_rc=$?
verify_repository_integrity 'release:clean'
clean_integrity_rc=$?
set -e

if [[ "$prepare_integrity_rc" -ne 0 || "$clean_integrity_rc" -ne 0 ]]; then
  exit 1
fi
if [[ "$prepare_rc" -ne 0 ]]; then
  exit "$prepare_rc"
fi
exit "$clean_rc"
