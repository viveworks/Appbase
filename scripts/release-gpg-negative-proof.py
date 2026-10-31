#!/usr/bin/env python3
"""Prove release signing rejects an empty keyring without using real credentials.

This command invokes Maven and must therefore be run under the repository's host build lease.
It never reaches a lifecycle phase or deploy goal: it invokes only the root POM's pinned GPG sign
goal against a fresh, empty GNUPGHOME and requires the observable ``No secret key`` rejection.
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import subprocess
import sys
import tempfile


ROOT = Path(__file__).resolve().parents[1]
MISSING_KEY = "0000000000000000000000000000000000000000"


def run(command: list[str], env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        command,
        cwd=ROOT,
        env=env,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=False,
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--maven", default="mvn", help="Maven executable (3.9.11 required)")
    parser.add_argument("--repo-local", type=Path, help="isolated Maven local repository")
    args = parser.parse_args()

    before = run(["git", "status", "--porcelain"])
    if before.returncode != 0:
        print("FAIL: cannot capture the pre-proof worktree state", file=sys.stderr)
        return 1

    with tempfile.TemporaryDirectory(prefix="appbase-gpg-negative-") as home:
        os.chmod(home, 0o700)
        env = os.environ.copy()
        env["GNUPGHOME"] = home
        env["MAVEN_GPG_PASSPHRASE"] = "deliberately-not-a-real-passphrase"
        command = [
            args.maven,
            "-N",
            "-B",
            "-ntp",
            "-P",
            "release-profile",
        ]
        if args.repo_local is not None:
            command.append(f"-Dmaven.repo.local={args.repo_local}")
        command.extend(
            [
                f"-Dgpg.homedir={home}",
                f"-Dgpg.keyname={MISSING_KEY}",
                "org.apache.maven.plugins:maven-gpg-plugin:3.2.8:sign",
            ]
        )
        result = run(command, env)

    after = run(["git", "status", "--porcelain"])
    failures: list[str] = []
    if result.returncode == 0:
        failures.append("GPG signing unexpectedly accepted an empty keyring")
    if "gpg:3.2.8:sign" not in result.stdout:
        failures.append("Maven output does not prove the pinned GPG signing goal ran")
    if "No secret key" not in result.stdout:
        failures.append("GPG output does not name the missing-secret-key rejection")
    if "BUILD FAILURE" not in result.stdout:
        failures.append("Maven output does not report BUILD FAILURE")
    if after.returncode != 0 or after.stdout != before.stdout:
        failures.append("the negative proof changed the worktree")

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        print(result.stdout, file=sys.stderr)
        return 1

    print(
        "PASS: maven-gpg-plugin:3.2.8:sign rejected the isolated empty keyring "
        "with 'No secret key'; worktree unchanged"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
