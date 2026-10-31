#!/usr/bin/env python3
"""Fail closed when a generated MCP API catalog disagrees with compiled Appbase classes."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import subprocess
import sys


METHOD_ID = re.compile(r"^(?P<owner>.+)#(?P<name>[^#(]+)\(")


def parameter_count(declaration: str) -> int:
    start = declaration.find("(")
    if start < 0:
        raise ValueError(f"method declaration has no parameter list: {declaration}")
    depth = 0
    commas = 0
    content = []
    for character in declaration[start + 1 :]:
        if character == ")" and depth == 0:
            break
        content.append(character)
        if character in "<([":
            depth += 1
        elif character in ">]" and depth:
            depth -= 1
        elif character == "," and depth == 0:
            commas += 1
    return 0 if not "".join(content).strip() else commas + 1


def compiled_methods(javap: str, classpath: str, qualified_name: str) -> set[tuple[str, int]]:
    result = subprocess.run(
        [javap, "-public", "-classpath", classpath, qualified_name],
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode:
        raise RuntimeError(f"javap could not inspect {qualified_name}: {result.stderr.strip()}")
    methods: set[tuple[str, int]] = set()
    for raw_line in result.stdout.splitlines():
        line = raw_line.strip()
        if not line.endswith(";") or "(" not in line:
            continue
        prefix = line[: line.find("(")].strip()
        name = prefix.rsplit(" ", 1)[-1].rsplit(".", 1)[-1]
        methods.add((name, parameter_count(line)))
    return methods


def verify(artifact: Path, classpath: str, javap: str) -> int:
    value = json.loads(artifact.read_text(encoding="utf-8"))
    symbols = value.get("symbols")
    if value.get("schema_version") != 1 or not isinstance(symbols, list) or not symbols:
        raise ValueError("expected a non-empty schema-v1 API artifact")
    by_type: dict[str, list[dict[str, str]]] = {}
    for symbol in symbols:
        if symbol.get("kind") in {"interface", "annotation"}:
            by_type.setdefault(symbol["qualified_name"], [])
        elif symbol.get("kind") == "method":
            by_type.setdefault(symbol["qualified_name"], []).append(symbol)
    if not by_type:
        raise ValueError("artifact contains no compiled API types")

    failures: list[str] = []
    checked_methods = 0
    for qualified_name, methods in sorted(by_type.items()):
        try:
            actual = compiled_methods(javap, classpath, qualified_name)
        except RuntimeError as error:
            failures.append(str(error))
            continue
        for method in methods:
            match = METHOD_ID.match(method["id"])
            if match is None:
                failures.append(f"invalid generated method id: {method['id']}")
                continue
            key = (match.group("name"), parameter_count(method["signature"]))
            checked_methods += 1
            if key not in actual:
                failures.append(
                    f"compiled method missing: {qualified_name}#{key[0]}/{key[1]} "
                    f"(source: {method['signature']})"
                )
    if failures:
        preview = "\n".join(failures[:50])
        suffix = "" if len(failures) <= 50 else f"\n... {len(failures) - 50} more"
        raise RuntimeError(f"compiled API drift ({len(failures)} mismatch(es)):\n{preview}{suffix}")
    print(f"verified {len(by_type)} compiled API types and {checked_methods} methods")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("artifact", type=Path)
    parser.add_argument("--classpath", required=True)
    parser.add_argument("--javap", default="javap")
    args = parser.parse_args()
    try:
        return verify(args.artifact, args.classpath, args.javap)
    except (OSError, ValueError, RuntimeError, json.JSONDecodeError) as error:
        print(f"API surface verification failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
