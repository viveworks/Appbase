#!/usr/bin/env python3
"""Executable documentation contract for internal work item 1144/N02's release runbook."""

from pathlib import Path
import os
import subprocess
import sys
import tempfile


ROOT = Path(__file__).resolve().parents[2]
RUNBOOK = ROOT / "RELEASING.md"


def validate(document: str) -> list[str]:
    failures: list[str] = []

    local_heading = "## Procedure A: local, uncredentialed rehearsal"
    public_heading = "## Procedure B: owner-dispatched public release and staged drop"
    local_at = document.find(local_heading)
    public_at = document.find(public_heading)
    if local_at < 0 or public_at < 0 or local_at >= public_at:
        failures.append("local rehearsal must precede the owner-dispatched public procedure")

    local = document[local_at:public_at] if local_at >= 0 and public_at >= 0 else ""
    public = document[public_at:] if public_at >= 0 else ""
    if "release:perform" in local and "does not prepare a release for `release:perform`" not in local:
        failures.append("local rehearsal must not invoke or offer release:perform")
    for command in (
        "./build.sh --mode compile",
        "release:prepare -DdryRun=true",
        "release:clean",
        "test ! -e release.properties",
        "test ! -e pom.xml.tag",
        "test ! -e pom.xml.next",
        'test -z "$(git status --porcelain)"',
    ):
        if command not in local:
            failures.append(f"local rehearsal is missing {command!r}")

    required_fragments = (
        ': "${PRIVATE_SHA:',
        ': "${PRIVATE_REPO:',
        ': "${MANIFEST_SHA256:',
        ': "${EXPORT_RUN_URL:',
        ': "${RELEASE_SHA:',
        ': "${RELEASE_REF:',
        'case "$RELEASE_REF" in dispatch/*)',
        'test "$PUBLIC_REMOTE" = "https://github.com/$PUBLIC_REPO.git"',
        'gh run view "$EXPORT_RUN_ID" --repo "$PRIVATE_REPO" --json conclusion',
        'gh run view "$EXPORT_RUN_ID" --repo "$PRIVATE_REPO" --json headSha',
        'gh run view "$EXPORT_RUN_ID" --repo "$PRIVATE_REPO" --json url',
        'test "$(git rev-parse HEAD)" = "$PRIVATE_SHA"',
        'gh repo view "$PUBLIC_REPO" --json defaultBranchRef',
        'contents/.github/workflows/release.yml?ref=$PUBLIC_DEFAULT_BRANCH',
        'git ls-remote "$PUBLIC_REMOTE" "refs/heads/$PUBLIC_RELEASE_BRANCH"',
        "Creating this remote tag is a credentialed public-repository write",
        "STOP / DO NOT CREATE THE TAG",
        'test -z "$existing_dispatch_tag"',
        'git fetch --no-tags "$PUBLIC_REMOTE"',
        '"refs/heads/$PUBLIC_RELEASE_BRANCH:$PUBLIC_TAG_SOURCE_REF"',
        'git rev-parse --verify "$PUBLIC_TAG_SOURCE_REF^{commit}"',
        'git push "$PUBLIC_REMOTE" "$PUBLIC_TAG_SOURCE_REF:refs/tags/$RELEASE_REF"',
        "Never use `git tag -a`, `git push --force`, or reuse a dispatch tag",
        'test "$smoke_run_count" -le 1',
        'git ls-remote "$PUBLIC_REMOTE" "refs/tags/$RELEASE_REF"',
        'git ls-remote "$PUBLIC_REMOTE" "refs/tags/$RELEASE_REF^{}"',
        'test "$run_count" -le 1',
        'gh run view "$RUN_ID" --repo "$PUBLIC_REPO" --json headSha',
        'gh run watch "$RUN_ID" --repo "$PUBLIC_REPO" --exit-status',
        'gh run view "$RUN_ID" --repo "$PUBLIC_REPO" --json conclusion',
        '"https://github.com/$PUBLIC_REPO/actions/runs/$RUN_ID"',
        'gh workflow run release.yml --repo "$PUBLIC_REPO" --ref "$RELEASE_REF"',
        "-f smoke_test=true",
        "autoReleaseAfterClose=false",
        "mvn -B -P release-profile release:prepare",
        "mvn -B -P release-profile release:perform",
        "terminal `dropped`",
        "never promote",
        "develop` into protected `main",
    )
    for fragment in required_fragments:
        if fragment not in document:
            failures.append(f"runbook is missing required contract fragment {fragment!r}")

    if "Immediately before dispatch, obtain the owner-authored authorization" not in public:
        failures.append("public procedure lacks the immediate owner-authorization gate")
    if "Do not type `release:perform` locally" not in public:
        failures.append("public procedure does not forbid local release:perform")
    if "inside the protected `release` Environment" not in public:
        failures.append("public procedure does not name the protected release Environment")

    return failures


def verify_tag_creation_from_clean_private_clone(document: str) -> list[str]:
    """Exercise the documented tag block with a public commit absent from the private clone."""
    failures: list[str] = []
    heading = "### Create the protected lightweight dispatch tag"
    start = document.find("```sh\n", document.find(heading))
    if start < 0:
        return ["dispatch-tag creation block is missing"]
    start += len("```sh\n")
    end = document.find("\n```", start)
    if end < 0:
        return ["dispatch-tag creation block is unterminated"]
    script = document[start:end]
    fetch = ('git fetch --no-tags "$PUBLIC_REMOTE" \\\n'
             '  "refs/heads/$PUBLIC_RELEASE_BRANCH:$PUBLIC_TAG_SOURCE_REF" || exit 1\n')
    if fetch not in script:
        return ["dispatch-tag block lacks the exact public-branch fetch"]

    def git(*args: str, cwd: Path) -> subprocess.CompletedProcess[str]:
        return subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True)

    with tempfile.TemporaryDirectory(prefix="release-runbook-test-") as scratch:
        root = Path(scratch)
        public = root / "public.git"
        seed = root / "public-seed"
        private = root / "private"
        for args, cwd in (
            (("init", "--bare", str(public)), root),
            (("init", str(seed)), root),
            (("config", "core.hooksPath", "/dev/null"), seed),
            (("config", "user.name", "Runbook Test"), seed),
            (("config", "user.email", "runbook-test@example.invalid"), seed),
            (("commit", "--allow-empty", "-m", "public release"), seed),
            (("push", str(public), "HEAD:refs/heads/release"), seed),
            (("init", str(private)), root),
            (("config", "core.hooksPath", "/dev/null"), private),
            (("config", "user.name", "Runbook Test"), private),
            (("config", "user.email", "runbook-test@example.invalid"), private),
            (("commit", "--allow-empty", "-m", "private source"), private),
        ):
            result = git(*args, cwd=cwd)
            if result.returncode:
                return [f"git fixture failed: {' '.join(args)}: {result.stderr.strip()}"]
        release_sha = git("rev-parse", "HEAD", cwd=seed).stdout.strip()
        private_sha = git("rev-parse", "HEAD", cwd=private).stdout.strip()
        if git("cat-file", "-e", f"{release_sha}^{{commit}}", cwd=private).returncode == 0:
            failures.append("public commit unexpectedly exists in the clean private clone")

        env = os.environ.copy()
        env.update(PUBLIC_REMOTE=str(public), PUBLIC_RELEASE_BRANCH="release",
                   RELEASE_SHA=release_sha, RELEASE_REF="dispatch/runbook-test")

        def run(block: str, expected_sha: str) -> subprocess.CompletedProcess[str]:
            return subprocess.run(["bash", "-e", "-c", block], cwd=private,
                                  env={**env, "RELEASE_SHA": expected_sha},
                                  capture_output=True, text=True)

        if run(script.replace(fetch, ""), release_sha).returncode == 0:
            failures.append("missing-fetch negative control created the dispatch tag")
        if run(script, private_sha).returncode == 0:
            failures.append("wrong-SHA negative control created the dispatch tag")
        if git("--git-dir", str(public), "show-ref", "--verify",
               "refs/tags/dispatch/runbook-test", cwd=root).returncode == 0:
            failures.append("a negative control created the public dispatch tag")
        result = run(script, release_sha)
        if result.returncode:
            failures.append(f"clean-private-clone tag creation failed: {result.stderr.strip()}")
        actual = git("--git-dir", str(public), "rev-parse",
                     "refs/tags/dispatch/runbook-test", cwd=root)
        if actual.returncode or actual.stdout.strip() != release_sha:
            failures.append("dispatch tag does not name the fetched reviewed public commit")
    return failures


def main() -> int:
    document = RUNBOOK.read_text(encoding="utf-8")
    failures = validate(document)
    failures.extend(verify_tag_creation_from_clean_private_clone(document))

    local_perform_mutation = document.replace(
        "Do not type `release:perform` locally and do not substitute a direct `deploy`.",
        "Run `release:perform` locally.",
        1,
    )
    if not any("forbid local release:perform" in item for item in validate(local_perform_mutation)):
        failures.append("negative control: local release:perform mutation was accepted")

    tag_shape_mutation = document.replace(
        'git ls-remote "$PUBLIC_REMOTE" "refs/tags/$RELEASE_REF^{}"',
        'git ls-remote "$PUBLIC_REMOTE" "refs/tags/$RELEASE_REF"',
    )
    if not any("refs/tags/$RELEASE_REF^{}" in item for item in validate(tag_shape_mutation)):
        failures.append("negative control: missing peeled-tag query was accepted")

    private_head_mutation = document.replace(
        'test "$(git rev-parse HEAD)" = "$PRIVATE_SHA" || exit 1',
        'test -n "$PRIVATE_SHA" || exit 1',
        1,
    )
    if not any("git rev-parse HEAD" in item for item in validate(private_head_mutation)):
        failures.append("negative control: unbound private HEAD mutation was accepted")

    public_branch_mutation = document.replace(
        'git ls-remote "$PUBLIC_REMOTE" "refs/heads/$PUBLIC_RELEASE_BRANCH"',
        'git ls-remote "$PUBLIC_REMOTE"',
        1,
    )
    if not any(
        "refs/heads/$PUBLIC_RELEASE_BRANCH" in item
        for item in validate(public_branch_mutation)
    ):
        failures.append("negative control: unbound public release branch mutation was accepted")

    dispatch_tag_creation_mutation = document.replace(
        'git push "$PUBLIC_REMOTE" "$PUBLIC_TAG_SOURCE_REF:refs/tags/$RELEASE_REF"',
        'git push "$PUBLIC_REMOTE" "$RELEASE_REF"',
        1,
    )
    if not any(
        'git push "$PUBLIC_REMOTE" "$PUBLIC_TAG_SOURCE_REF:refs/tags/$RELEASE_REF"' in item
        for item in validate(dispatch_tag_creation_mutation)
    ):
        failures.append("negative control: unbound dispatch-tag creation was accepted")

    missing_fetch_mutation = document.replace('git fetch --no-tags "$PUBLIC_REMOTE"',
                                              'git status --short', 1)
    if not any('git fetch --no-tags "$PUBLIC_REMOTE"' in item
               for item in validate(missing_fetch_mutation)):
        failures.append("negative control: missing public-commit fetch was accepted")

    tag_authorization_mutation = document.replace(
        "Creating this remote tag is a credentialed public-repository write",
        "Creating this remote tag is harmless setup",
        1,
    )
    if not any(
        "credentialed public-repository write" in item
        for item in validate(tag_authorization_mutation)
    ):
        failures.append("negative control: unauthorized dispatch-tag creation was accepted")

    terminal_run_mutation = document.replace(
        'gh run watch "$RUN_ID" --repo "$PUBLIC_REPO" --exit-status',
        'gh run watch "$RUN_ID" --repo "$PUBLIC_REPO"',
        1,
    )
    if not any(
        'gh run watch "$RUN_ID" --repo "$PUBLIC_REPO" --exit-status' in item
        for item in validate(terminal_run_mutation)
    ):
        failures.append("negative control: non-failing release-run watch was accepted")

    export_run_identity_mutation = document.replace(
        'test "$(gh run view "$EXPORT_RUN_ID" --repo "$PRIVATE_REPO" --json headSha --jq .headSha)" = "$PRIVATE_SHA" || exit 1',
        'test -n "$PRIVATE_SHA" || exit 1',
        1,
    )
    if not any(
        'gh run view "$EXPORT_RUN_ID" --repo "$PRIVATE_REPO" --json headSha' in item
        for item in validate(export_run_identity_mutation)
    ):
        failures.append("negative control: export run unbound from private SHA was accepted")

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1
    print(
        "PASS: release runbook keeps local/public boundaries, exact identity checks, "
        "owner approval, staged drop, cleanup, and main marker explicit"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
