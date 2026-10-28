#!/usr/bin/env python3
"""Offline acceptance check for the supported-public bootstrap Javadoc (internal work item 1400)."""

import argparse
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True


ROOT = Path(__file__).resolve().parents[2]
INVENTORY = ROOT / "docs/api-inventory/inventory.json"
INVENTORY_TOOL = ROOT / "scripts/api-inventory.py"


def load_scanner():
    spec = importlib.util.spec_from_file_location("api_inventory", INVENTORY_TOOL)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def fail(messages):
    for message in messages:
        print(f"FAIL: {message}", file=sys.stderr)
    return 1


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inventory", type=Path, default=INVENTORY)
    args = parser.parse_args(argv)
    inventory = json.loads(args.inventory.read_text(encoding="utf-8"))
    artifacts = [
        artifact
        for artifact in inventory["artifacts"]
        if str(artifact.get("doc_issue")) == "1400"
        and artifact["support"] == "supported-public"
    ]
    scanner = load_scanner()
    errors = []
    checked_types = checked_members = 0

    # The committed JSON is a cache, not an authority.  Rebuild the records before
    # selecting scope so a newly added artifact, package or type cannot disappear
    # merely because the cache was not refreshed.
    fresh_inventory, build_issues = scanner.build(str(ROOT))
    if fresh_inventory is None:
        return fail([f"authoritative inventory scan failed: {issue}" for issue in build_issues])
    fresh_artifacts = {
        artifact["id"]: artifact
        for artifact in fresh_inventory["artifacts"]
        if str(artifact.get("doc_issue")) == "1400"
        and artifact["support"] == "supported-public"
    }
    committed_artifacts = {artifact["id"]: artifact for artifact in artifacts}
    if committed_artifacts != fresh_artifacts:
        errors.append(
            "bootstrap scope differs from a fresh authoritative scan; "
            "run scripts/api-inventory.py generate"
        )

    for artifact in artifacts:
        expected = {
            (package["package"], type_name)
            for package in artifact["packages"]
            if package["classification"] == "supported-public"
            for type_name in package["public_types"]
        }
        found = {}
        source_root = ROOT / artifact["module_path"] / "src/main/java"
        for source in sorted(source_root.rglob("*.java")):
            package_name, types = scanner.scan_java_file(source)
            for public_type in types:
                found[(package_name, public_type["name"])] = (source, public_type)

        if not expected:
            errors.append(f"{artifact['id']}: expected artifact contributes zero public types")

        for key in sorted(expected):
            checked_types += 1
            if key not in found:
                errors.append(f"{artifact['id']}: inventory type {key[0]}.{key[1]} was not found")
                continue
            source, public_type = found[key]
            checked_members += public_type["members"]
            relative = source.relative_to(ROOT)
            if not public_type["documented"]:
                errors.append(f"{relative}: public type {key[1]} has no class-level Javadoc")
            if public_type["undocumented_members"]:
                errors.append(
                    f"{relative}: public type {key[1]} has "
                    f"{public_type['undocumented_members']} undocumented public member(s)"
                )

    if not artifacts:
        return fail(["inventory contains no supported-public artifacts assigned to internal work item 1400"])
    if checked_types == 0:
        errors.append("supported-public bootstrap scope contains zero public types")
    if errors:
        return fail(errors)

    print(
        "AC1400-1: PASS — every inventory-listed supported-public bootstrap type "
        f"and public member has Javadoc ({checked_types} types, {checked_members} members)"
    )
    print(
        "AC1400-2: RUN SEPARATELY — strict generation command: "
        "mvn -Prelease-profile -pl appbase-java/appbase-bootstrap -am "
        "-Dmaven.gitcommitid.skip=true -Denforcer.skip=true "
        "-Dsurefire.failIfNoSpecifiedTests=false javadoc:javadoc"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
