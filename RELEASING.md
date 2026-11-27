# Releasing Appbase

This runbook separates the harmless private-repository rehearsal from the credentialed public-repository release. The private repository is export input only. Never deploy from it, and never run a credentialed Maven release goal locally -- except under a fresh, dated, explicit owner authorization recorded in Procedure C below, which this runbook treats as a one-time, named exception, not a standing alternative to Procedure B.

## Roles and repositories

- `PRIVATE_REPO` is this repository. It owns the reviewed source, `release-profile`, the local dry run, and the eventual `develop` to `main` marker merge.
- `PUBLIC_REPO` is the curated public export. It owns the protected release branch, dispatch tag, release workflow, release commit and release tag.
- The owner dispatches the public workflow and approves its protected `release` Environment. Environment credentials are not available to a local shell.

Record this identity chain before proceeding:

1. reviewed private commit: `PRIVATE_SHA`;
2. frozen export manifest and its hash: `MANIFEST_SHA256`;
3. successful export run URL: `EXPORT_RUN_URL`;
4. exported public commit: `RELEASE_SHA`;
5. protected lightweight dispatch tag on that commit: `RELEASE_REF`;
6. Maven release tag created by `release:prepare`: `1.0.0` (the root POM explicitly configures
   `<tagNameFormat>@{project.version}</tagNameFormat>`; Maven's plugin default would include the artifact ID).

Any missing link is a stop condition.

Set and validate the operator inputs before running either procedure:

```sh
: "${PRIVATE_SHA:?set the reviewed private commit}"
: "${PRIVATE_REPO:?set owner/name for this private repository}"
: "${MANIFEST_FILE:?set the frozen export manifest path}"
: "${MANIFEST_SHA256:?set the recorded lowercase SHA-256 of that manifest}"
: "${EXPORT_RUN_URL:?set the successful export workflow URL}"
: "${PRIVATE_RELEASE_BRANCH:?set the reviewed private branch name}"
: "${PUBLIC_REPO:?set owner/name for the public repository}"
: "${PUBLIC_REMOTE:?set the public repository Git remote URL}"
: "${PUBLIC_RELEASE_BRANCH:?set the public release branch name}"
: "${RELEASE_SHA:?set the 40-hex exported public commit}"
: "${RELEASE_REF:?set the protected lightweight dispatch tag name}"

git check-ref-format --branch "$PRIVATE_RELEASE_BRANCH" >/dev/null || exit 1
git check-ref-format --branch "$PUBLIC_RELEASE_BRANCH" >/dev/null || exit 1
git check-ref-format "refs/tags/$RELEASE_REF" >/dev/null || exit 1
case "$RELEASE_REF" in dispatch/*) ;; *) exit 1 ;; esac
printf '%s\n' "$PRIVATE_SHA" | grep -Eq '^[0-9a-f]{40}$' || exit 1
printf '%s\n' "$RELEASE_SHA" | grep -Eq '^[0-9a-f]{40}$' || exit 1
printf '%s\n' "$MANIFEST_SHA256" | grep -Eq '^[0-9a-f]{64}$' || exit 1
test -f "$MANIFEST_FILE" || exit 1
test "$(shasum -a 256 "$MANIFEST_FILE" | awk '{ print $1 }')" = "$MANIFEST_SHA256" || exit 1
test "$(git rev-parse "$PRIVATE_SHA^{commit}")" = "$PRIVATE_SHA" || exit 1
case "$PRIVATE_REPO" in */*) ;; *) exit 1 ;; esac
case "$PUBLIC_REPO" in */*) ;; *) exit 1 ;; esac
test "$PUBLIC_REMOTE" = "https://github.com/$PUBLIC_REPO.git" || exit 1
test "$(gh repo view "$PRIVATE_REPO" --json nameWithOwner --jq .nameWithOwner)" = "$PRIVATE_REPO" || exit 1
test "$(gh repo view "$PUBLIC_REPO" --json nameWithOwner --jq .nameWithOwner)" = "$PUBLIC_REPO" || exit 1
case "$EXPORT_RUN_URL" in "https://github.com/$PRIVATE_REPO/actions/runs/"*) ;; *) exit 1 ;; esac
EXPORT_RUN_ID="${EXPORT_RUN_URL##*/}"
printf '%s\n' "$EXPORT_RUN_ID" | grep -Eq '^[0-9]+$' || exit 1
test "$(gh run view "$EXPORT_RUN_ID" --repo "$PRIVATE_REPO" --json conclusion --jq .conclusion)" = success || exit 1
test "$(gh run view "$EXPORT_RUN_ID" --repo "$PRIVATE_REPO" --json headSha --jq .headSha)" = "$PRIVATE_SHA" || exit 1
test "$(gh run view "$EXPORT_RUN_ID" --repo "$PRIVATE_REPO" --json url --jq .url)" = "$EXPORT_RUN_URL" || exit 1
```

Success means every command exits zero and the manifest bytes hash to the recorded value. Any missing file, malformed identifier, hash mismatch, or unresolved private commit is `STOP`; do not reconstruct or guess a link in the identity chain.
The successful export run must also resolve to the exact recorded URL and reviewed private commit;
an unrelated successful run is not evidence for this export.

## Procedure A: local, uncredentialed rehearsal

Run this procedure only in a clean clone of `PRIVATE_REPO`. It performs no deployment and creates no durable release tag or commit.

```sh
test -z "$(git status --porcelain)" || exit 1
test "$(git branch --show-current)" = "$PRIVATE_RELEASE_BRANCH" || exit 1
test "$(git rev-parse HEAD)" = "$PRIVATE_SHA" || exit 1
./build.sh --mode compile
mvn -B -P release-profile release:prepare -DdryRun=true \
  -DreleaseVersion=1.0.0 \
  -DdevelopmentVersion=1.0.1-SNAPSHOT \
  -Dgpg.skip=true \
  -Darguments=-Dgpg.skip=true
mvn -B -P release-profile release:clean
test ! -e release.properties || exit 1
test ! -e pom.xml.tag || exit 1
test ! -e pom.xml.next || exit 1
test -z "$(git status --porcelain)" || exit 1
```

Success means the compile and dry-run commands succeed, all three transient release files are absent, and `release:clean` restores a clean tree. A dry run does not prepare a release for `release:perform`; its transient files are never input to the public workflow.
The release plugin's preparation build skips Maven Enforcer because a dry run verifies the unchanged SNAPSHOT working POM. The credential-free rehearsal also forwards `gpg.skip` through the release plugin's `arguments` parameter so that internal build does not require a private signing key. Both skips are scoped to this rehearsal command; `release:perform` reactivates `release-profile`, enforces `requireReleaseVersion`, and signs artifacts against the real tagged POM.

## Credentialed release preconditions

Stop unless every item below has a timestamped, redacted evidence record:

- An owner-authored comment explicitly authorizes this specific attempt.
- The heavy-work lease is released.
- The Sonatype namespace and published signing key are verified.
- The public export run ties `PRIVATE_SHA` and `MANIFEST_SHA256` to `RELEASE_SHA`.
- `.github/workflows/release.yml` is present on the public repository default branch.
- The public release branch tip is exactly `RELEASE_SHA`.
- The effective public POM names only `PUBLIC_REPO` in both SCM URLs and has SCM tag `HEAD`.
- The effective staging configuration has `autoReleaseAfterClose=false`.
- The protected `dispatch/*` tag ruleset is effective.

The public workflow must have an unprivileged, secret-free preflight job. Its release job must depend on preflight, reference the protected `release` Environment, and request only the permissions needed to create and clean up the public release commit and tag. It must run both `release:prepare` and `release:perform` with `-P release-profile`. This repository does not author that workflow.

Before using that workflow, verify its full contract in `PUBLIC_REPO`:

- the only trigger is `workflow_dispatch`;
- required string inputs are `correlation_id`, `release_sha`, and `release_branch`, and the run name is exactly `correlation_id`;
- `smoke_test` is a typed boolean defaulting to `false`;
- both jobs bind the reviewed SHA and release branch through `env`, and no `${{ inputs.* }}` value is interpolated directly into shell source;
- preflight has no Environment or secrets; the write-capable job has `needs: preflight` and `environment: release`;
- default permissions are `contents: read`, with `contents: write` only on the write-capable job;
- every `uses:` reference is a full commit SHA;
- every secret-consuming or repository-mutating step, including `release:prepare`, tag creation and deploy, is guarded by `if: ${{ ! inputs.smoke_test }}`;
- a separate smoke job still declares `environment: release`, so the approval mechanism itself is exercised; and
- failure cleanup deletes only the generated public release tag and reverts only the generated public release commit or commits. It never resets, force-pushes, promotes, or targets `PRIVATE_REPO`.

An absent or differently shaped property is `STOP / BLOCKED`, not permission to patch a second workflow here.

Perform the public repository existence, default-branch workflow and release-branch identity reads
before creating or accepting a dispatch tag:

```sh
PUBLIC_DEFAULT_BRANCH="$(gh repo view "$PUBLIC_REPO" --json defaultBranchRef --jq .defaultBranchRef.name)" || exit 1
: "${PUBLIC_DEFAULT_BRANCH:?STOP: public repository has no default branch}"
gh api "repos/$PUBLIC_REPO/contents/.github/workflows/release.yml?ref=$PUBLIC_DEFAULT_BRANCH" \
  --jq '.type == "file" and (.sha | test("^[0-9a-f]{40}$"))' | grep -qx true || exit 1

if ! public_branch_record="$(git ls-remote "$PUBLIC_REMOTE" "refs/heads/$PUBLIC_RELEASE_BRANCH")"; then
  echo "STOP: public release branch read failed" >&2
  exit 1
fi
test "$(printf '%s\n' "$public_branch_record" | awk 'NF { n++ } END { print n+0 }')" -eq 1 || exit 1
test "$(printf '%s\n' "$public_branch_record" | awk 'NF { print $1 }')" = "$RELEASE_SHA" || exit 1
```

Success means the public repository has a default branch, that exact branch contains the owned
workflow, and the named public release branch has exactly one remote record equal to `RELEASE_SHA`.
An API/read failure, missing workflow, missing/multiple branch record, or SHA mismatch is `STOP`.

### Create the protected lightweight dispatch tag

Creating this remote tag is a credentialed public-repository write, not harmless setup. Before it,
obtain a timestamped owner-authored authorization naming this exact `RELEASE_SHA`, `RELEASE_REF`,
`PUBLIC_RELEASE_BRANCH`, and smoke-test attempt. An absent authorization or any mismatched value is
`STOP / DO NOT CREATE THE TAG`.

The repository owner creates the dispatch tag only after that authorization is recorded, the reads
above succeed, and the `dispatch/*` ruleset is confirmed effective. First prove that the exact tag
does not already exist; a read failure is not proof of absence. This procedure runs in a private
clone, where the exported public commit is not necessarily present. Fetch the verified public
release branch into an isolated local ref and require its commit to equal `RELEASE_SHA` before
creating a lightweight tag from that local object:

```sh
if ! existing_dispatch_tag="$(git ls-remote "$PUBLIC_REMOTE" "refs/tags/$RELEASE_REF")"; then
  echo "STOP: pre-create dispatch tag read failed" >&2
  exit 1
fi
test -z "$existing_dispatch_tag" || {
  echo "STOP: dispatch tag already exists; do not move or replace it" >&2
  exit 1
}

PUBLIC_TAG_SOURCE_REF="refs/appbase-release/public/$RELEASE_SHA"
git check-ref-format "$PUBLIC_TAG_SOURCE_REF" || exit 1
git fetch --no-tags "$PUBLIC_REMOTE" \
  "refs/heads/$PUBLIC_RELEASE_BRANCH:$PUBLIC_TAG_SOURCE_REF" || exit 1
test "$(git rev-parse --verify "$PUBLIC_TAG_SOURCE_REF^{commit}")" = "$RELEASE_SHA" || {
  echo "STOP: fetched public release branch is not the reviewed commit" >&2
  exit 1
}
git push "$PUBLIC_REMOTE" "$PUBLIC_TAG_SOURCE_REF:refs/tags/$RELEASE_REF" || exit 1
```

Never use `git tag -a`, `git push --force`, or reuse a dispatch tag from an earlier attempt. The
exact and peeled reads in the next section are mandatory after creation; they prove that the remote
tag is lightweight and still names the reviewed exported commit. If creation or either verification
fails, `STOP`; an owner removes a newly created unusable tag through the public repository's
protected process before starting a new attempt with a new `RELEASE_REF`.

Retain the tag-creation authorization and successful exact/peeled read output with the smoke-run
evidence. Authorization to create the tag and run the credential-disabled smoke test is not
authorization for the later credentialed release; Procedure B requires a fresh owner decision.

### Credential-disabled workflow smoke test

This is a public-repository workflow dispatch, but it must be incapable of reading credentials or mutating either repository. It can run only after the public release workflow is merged on `PUBLIC_REPO`'s default branch and the protected lightweight dispatch tag has passed the exact and peeled reads below.

```sh
SMOKE_CORRELATION="release-smoke-${RELEASE_SHA}-$(date -u +%Y%m%dT%H%M%SZ)"
SMOKE_AFTER="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
export SMOKE_CORRELATION SMOKE_AFTER RELEASE_SHA

if ! smoke_tag_record="$(git ls-remote "$PUBLIC_REMOTE" "refs/tags/$RELEASE_REF")"; then
  echo "STOP: exact smoke-dispatch tag read failed" >&2
  exit 1
fi
test "$(printf '%s\n' "$smoke_tag_record" | awk 'NF { n++ } END { print n+0 }')" -eq 1 || exit 1
test "$(printf '%s\n' "$smoke_tag_record" | awk 'NF { print $1 }')" = "$RELEASE_SHA" || exit 1
if ! smoke_peeled_record="$(git ls-remote "$PUBLIC_REMOTE" "refs/tags/$RELEASE_REF^{}")"; then
  echo "STOP: peeled smoke-dispatch tag read failed" >&2
  exit 1
fi
test -z "$smoke_peeled_record" || exit 1

gh workflow run release.yml --repo "$PUBLIC_REPO" --ref "$RELEASE_REF" \
  -f "correlation_id=$SMOKE_CORRELATION" \
  -f "release_sha=$RELEASE_SHA" \
  -f "release_branch=$PUBLIC_RELEASE_BRANCH" \
  -f smoke_test=true

SMOKE_RUN_ID=
attempt=0
while test "$attempt" -lt 12; do
  smoke_run_rows="$(gh run list --repo "$PUBLIC_REPO" --workflow release.yml \
    --event workflow_dispatch --json databaseId,displayTitle,headSha,createdAt \
    --jq '.[] | select(.displayTitle == env.SMOKE_CORRELATION and .headSha == env.RELEASE_SHA and .createdAt >= env.SMOKE_AFTER) | .databaseId')" || exit 1
  smoke_run_count="$(printf '%s\n' "$smoke_run_rows" | awk 'NF { n++ } END { print n+0 }')"
  test "$smoke_run_count" -le 1 || exit 1
  if test "$smoke_run_count" -eq 1; then SMOKE_RUN_ID="$smoke_run_rows"; break; fi
  attempt=$((attempt + 1))
  sleep 5
done
: "${SMOKE_RUN_ID:?STOP: no unique matching smoke run}"
gh run watch "$SMOKE_RUN_ID" --repo "$PUBLIC_REPO" --exit-status || exit 1
test "$(gh run view "$SMOKE_RUN_ID" --repo "$PUBLIC_REPO" --json headSha --jq .headSha)" = "$RELEASE_SHA" || exit 1
gh run view "$SMOKE_RUN_ID" --repo "$PUBLIC_REPO" --log >"${TMPDIR:-/tmp}/release-smoke-$SMOKE_RUN_ID.log" || exit 1
```

Success requires one matching `workflow_dispatch` run, `headSha == RELEASE_SHA`, a recorded owner approval for the smoke job's protected `release` Environment, and successful preflight/smoke jobs. The test evidence must also prove that smoke mode skipped every secret-consuming step, `release:prepare`, release-tag creation and deploy. A green run without that negative-path evidence is not acceptance. Retain the run URL and approver; do not retain secret-bearing logs.

## Verify the public dispatch identity

Run these reads against `PUBLIC_REPO` immediately before dispatch. A failed read is a failure, not evidence of absence.

```sh
git check-ref-format "refs/tags/$RELEASE_REF" >/dev/null || exit 1

if ! tag_record="$(git ls-remote "$PUBLIC_REMOTE" "refs/tags/$RELEASE_REF")"; then
  echo "STOP: exact tag read failed" >&2
  exit 1
fi
test "$(printf '%s\n' "$tag_record" | awk 'NF { n++ } END { print n+0 }')" -eq 1 || exit 1
test "$(printf '%s\n' "$tag_record" | awk 'NF { print $1 }')" = "$RELEASE_SHA" || exit 1

if ! peeled_record="$(git ls-remote "$PUBLIC_REMOTE" "refs/tags/$RELEASE_REF^{}")"; then
  echo "STOP: peeled tag read failed" >&2
  exit 1
fi
test -z "$peeled_record" || {
  echo "STOP: dispatch tag is annotated" >&2
  exit 1
}
```

Exactly one unpeeled record must equal `RELEASE_SHA`. The separately successful peeled query must return no record, proving the tag is lightweight.

## Procedure B: owner-dispatched public release and staged drop

This is a credentialed external write. It runs only in `PUBLIC_REPO`, inside the protected `release` Environment, after the owner approves this attempt. Do not type `release:perform` locally and do not substitute a direct `deploy`.

Immediately before dispatch, obtain the owner-authored authorization that names this exact `RELEASE_SHA`, `RELEASE_REF`, `PUBLIC_RELEASE_BRANCH`, and attempt correlation. If it is absent or names a different value, `STOP / DO NOT DISPATCH`. The later Environment approval is a second gate, not a substitute for this authorization.

```sh
RUN_CORRELATION="release-${RELEASE_SHA}-$(date -u +%Y%m%dT%H%M%SZ)"
DISPATCHED_AFTER="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
export RUN_CORRELATION DISPATCHED_AFTER RELEASE_SHA

gh workflow run release.yml --repo "$PUBLIC_REPO" --ref "$RELEASE_REF" \
  -f "correlation_id=$RUN_CORRELATION" \
  -f "release_sha=$RELEASE_SHA" \
  -f "release_branch=$PUBLIC_RELEASE_BRANCH"

RUN_ID=
attempt=0
while test "$attempt" -lt 12; do
  run_rows="$(gh run list --repo "$PUBLIC_REPO" --workflow release.yml \
    --event workflow_dispatch --json databaseId,displayTitle,headSha,createdAt \
    --jq '.[] | select(.displayTitle == env.RUN_CORRELATION and .headSha == env.RELEASE_SHA and .createdAt >= env.DISPATCHED_AFTER) | .databaseId')" || exit 1
  run_count="$(printf '%s\n' "$run_rows" | awk 'NF { n++ } END { print n+0 }')"
  test "$run_count" -le 1 || exit 1
  if test "$run_count" -eq 1; then RUN_ID="$run_rows"; break; fi
  attempt=$((attempt + 1))
  sleep 5
done
: "${RUN_ID:?STOP: no unique matching workflow run found}"
```

Locate exactly one `workflow_dispatch` run whose display title is `RUN_CORRELATION`, whose creation time is not earlier than `DISPATCHED_AFTER`, and whose `headSha` is `RELEASE_SHA`. Before approving the Environment gate, read `headSha` again:

```sh
test "$(gh run view "$RUN_ID" --repo "$PUBLIC_REPO" --json headSha --jq .headSha)" = "$RELEASE_SHA" || exit 1
```

A missing, duplicate or mismatched run is `STOP / DO NOT APPROVE`.

After approval, the workflow re-verifies the release branch tip and clean tree before materializing credentials, then runs:

```sh
mvn -B -P release-profile release:prepare \
  -DreleaseVersion=1.0.0 \
  -DdevelopmentVersion=1.0.1-SNAPSHOT
mvn -B -P release-profile release:perform
```

Capture the Sonatype staging repository identifier from the workflow or authenticated API. Validate signatures, POM metadata, sources jars and javadoc jars against that exact identifier. Then drop, never promote, the staging repository. Re-read the same identifier until it reports terminal `dropped`, or an authenticated unambiguous not-found after its prior existence was recorded. Authentication, endpoint or timeout errors are failures, not proof of a drop.

Keep watching the uniquely selected run while those workflow-owned operations execute. After the
staging repository is verifiably dropped, require that same run to finish successfully and re-read
its immutable identity:

```sh
gh run watch "$RUN_ID" --repo "$PUBLIC_REPO" --exit-status || exit 1
test "$(gh run view "$RUN_ID" --repo "$PUBLIC_REPO" --json conclusion --jq .conclusion)" = success || exit 1
test "$(gh run view "$RUN_ID" --repo "$PUBLIC_REPO" --json headSha --jq .headSha)" = "$RELEASE_SHA" || exit 1
RUN_URL="$(gh run view "$RUN_ID" --repo "$PUBLIC_REPO" --json url --jq .url)" || exit 1
case "$RUN_URL" in "https://github.com/$PUBLIC_REPO/actions/runs/$RUN_ID") ;; *) exit 1 ;; esac
```

A queued, in-progress, cancelled, skipped or failed run is not release evidence. A successful run is
also insufficient without the separately recorded staging validation and terminal-drop proof.

Retain the run URL, approver, staging identifier and redacted validation/drop output. On workflow failure, clean up only the public repository's generated release tag and revert its generated release commit; leave no promoted artifact. Never aim cleanup at `PRIVATE_REPO`.

## Mark the released source

After the staged repository is verified dropped and the release evidence is accepted, a human merges `develop` into protected `main` in `PRIVATE_REPO` through a reviewed pull request. This merge records which private source state produced the export; it does not publish artifacts.

## Procedure C: owner-authorized local credentialed release (dated exception, 1.0.0 only)

**This procedure exists for exactly one release.** It is not a standing alternative to Procedure B.
Procedure B (the protected `release` Environment dispatch) remains the default; wiring it for real
is tracked separately so every release after 1.0.0 uses it. Using Procedure C again for a later
release needs its own fresh, dated owner authorization recorded here the same way -- an old
authorization below does not carry forward.

**Authorization on record:** owner, 2026-11-26/27, verbal in conversation with EA-Super: run the
real 1.0.0 Maven Central publish directly from this workstation instead of through the Actions
dispatch path, because the release profile is already proven to build clean on this exact host and
this is the very first real publish -- minimizing new, never-exercised surface (an Actions publish
step that has never actually run for real) was judged worth more than using the originally-planned
mechanism this one time.

Preconditions, all owner-driven, none committed to this repository:

- `com.viveworks` namespace verified on Sonatype's Central Portal (`central.sonatype.com`) --
  already true, independent of this procedure.
- A Central Portal **User Token** (not the owner's regular login) in `~/.m2/settings.xml` under
  `<server><id>central</id>...</server>`, matching `publishingServerId=central` in the POM's
  `central-publishing-maven-plugin` configuration.
- The release-signing GPG key present and its passphrase cached in the local `gpg-agent` before
  this procedure starts (`gpg --batch --yes --pinentry-mode error --local-user <key> --detach-sign
  --output /dev/null -` must succeed non-interactively; if it does not, stop and get the owner to
  re-enter the passphrase rather than let a release step block on an interactive prompt nobody is
  watching).
- A clean, fresh checkout of `PUBLIC_REPO` at the exact commit being released -- not a working
  branch, not a clone with uncommitted changes.
- The identity-chain checks earlier in this runbook (reviewed commit, exact head, no SNAPSHOT
  dependencies) still apply in full; this procedure changes *where* `release:prepare`/
  `release:perform` run, not what they verify first.

Sequence:

1. Run the real full build once, against this exact commit, before touching the release plugin at
   all: `./build.sh --mode release` (`mvn clean install site site:stage -P release-profile,
   appbase-config-wizard-it-profile`). This is the authoritative proof for *this* commit -- do not
   skip it on the theory that an earlier build of a different commit already proved enough.
2. Only after that build is confirmed green, run `release:prepare` and `release:perform` with
   tests running in full both times (owner decision, 2026-11-27: no `-DskipTests` anywhere, even
   though the standalone build in step 1 already proved this exact commit -- the redundancy is
   accepted deliberately for the first real release).
3. `release:perform` stages the signed artifacts at Central Portal and stops (`autoPublish=false`,
   `waitUntil=validated`) -- the same stop-before-promote posture Procedure B uses. Validate the
   staged deployment the same way Procedure B's own validation step does.
4. The actual promotion to a publicly resolvable `1.0.0` is a separate, fresh owner decision after
   validation -- this procedure does not make that decision for the owner, it only gets the build
   signed and staged.
5. Record the same evidence Procedure B would have recorded (run output, staging identifier,
   redacted validation output) on the release ticket, noting plainly that this ran via Procedure C,
   not Procedure B, and why.

Everything else in this runbook -- the identity chain, the stop conditions, never promoting without
a fresh owner decision, the `develop` into `main` marker merge -- is unchanged by this procedure.

The real Maven Central promotion belongs to the later release gate. This epic must not promote or publish anything.
