#!/usr/bin/env python3
"""Check every tracked Java source against the canonical Appbase header."""
import pathlib
import re
import subprocess
import sys


def fail(message: str) -> None:
    print(f"license-header-check: {message}", file=sys.stderr)
    raise SystemExit(1)


if len(sys.argv) != 3:
    print("usage: check-license-headers.py <repo-root> <header.txt>", file=sys.stderr)
    raise SystemExit(2)

root = pathlib.Path(sys.argv[1]).resolve()
header = pathlib.Path(sys.argv[2]).resolve()
if not header.is_file():
    fail(f"missing header template: {header}")

lines = header.read_text(encoding="utf-8").splitlines()
rendered = "/*\n" + "\n".join((" * " + line) if line else " *" for line in lines) + "\n */\n"
tracked = [
    item.decode()
    for item in subprocess.check_output(
        ["git", "-C", str(root), "ls-files", "-z", "--", "*.java"]
    ).split(b"\0")
    if item
]
selected = [path for path in tracked if re.search(r"(^|/)src/(main|test)/java/", path)]
outside = sorted(set(tracked) - set(selected))
if outside:
    fail(f"tracked Java source outside src/main|test/java: {outside[0]}")
if not selected:
    fail("no tracked Java sources")

missing = []
mismatched = []
for relative in selected:
    text = (root / relative).read_text(encoding="utf-8", errors="replace")
    match = re.match(r"/\*.*?\*/\n", text, re.S)
    if not match or "Licensed under the Apache License" not in match.group(0):
        missing.append(relative)
    elif match.group(0) != rendered:
        mismatched.append(relative)

if missing:
    fail(f"{len(missing)} source(s) missing the canonical header; first: {missing[0]}")
if mismatched:
    fail(f"{len(mismatched)} source header(s) differ from header.txt; first: {mismatched[0]}")
print(f"license-header-check: PASS — {len(selected)} tracked Java source(s), all canonical")
