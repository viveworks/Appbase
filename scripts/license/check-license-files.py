#!/usr/bin/env python3
"""Check Appbase-owned LICENSE files against the repository's canonical LICENSE (internal work item 1383 leaf A).

@author Navaneeth Mysore G
@author Claude
"""

import argparse
import hashlib
import subprocess
from pathlib import Path
import sys


# SHA-256 of the official Apache License 2.0 text (apache.org LICENSE-2.0.txt). Pinning it stops
# the canonical root itself from drifting, which a same-as-root comparison alone cannot catch.
APACHE_2_0_SHA256 = "cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30"

EXCLUDED_PREFIXES = (Path("appbase-stacktest/src/main/resources/swagger-ui"),)


def is_excluded(relative_path: Path) -> bool:
    return any(
        relative_path == prefix or prefix in relative_path.parents
        for prefix in EXCLUDED_PREFIXES
    )


def license_paths(root: Path) -> list:
    """Tracked LICENSE/LICENSE.txt files (git ls-files); a plain walk only when root is not a git work tree."""
    names = {"LICENSE", "LICENSE.txt"}
    try:
        listed = subprocess.run(
            ["git", "-C", str(root), "ls-files", "-z"], check=True, capture_output=True
        ).stdout.decode("utf-8").split("\0")
        inside = subprocess.run(
            ["git", "-C", str(root), "rev-parse", "--show-toplevel"], check=True, capture_output=True, text=True
        ).stdout.strip()
        if Path(inside).resolve() != root:
            raise subprocess.CalledProcessError(1, "git")
        return sorted(root / rel for rel in listed if rel and Path(rel).name in names)
    except (OSError, subprocess.CalledProcessError):
        return sorted(path for path in root.rglob("*") if path.is_file() and path.name in names)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--canonical", type=Path)
    args = parser.parse_args()

    root = args.root.resolve()
    canonical = (args.canonical or root / "LICENSE").resolve()
    expected = canonical.read_bytes()
    actual_sha256 = hashlib.sha256(expected).hexdigest()
    if actual_sha256 != APACHE_2_0_SHA256:
        print(
            f"Canonical {canonical.name} is not the official Apache-2.0 text: "
            f"SHA-256 {actual_sha256} != {APACHE_2_0_SHA256}",
            file=sys.stderr,
        )
        return 1
    all_paths = license_paths(root)
    excluded = [path.relative_to(root) for path in all_paths if is_excluded(path.relative_to(root))]
    candidates = [path for path in all_paths if not is_excluded(path.relative_to(root))]
    failures = [path.relative_to(root) for path in candidates if path.read_bytes() != expected]

    if failures:
        print("Non-canonical LICENSE files:", file=sys.stderr)
        for path in failures:
            print(f"  {path}", file=sys.stderr)
        return 1

    print(f"All {len(candidates)} Appbase-owned LICENSE files are canonical.")
    for path in excluded:
        print(f"Excluded (vendored): {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
