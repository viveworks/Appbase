#!/usr/bin/env python3
"""Record and guard the release source/javadoc attachment inventory for internal work item 1144."""

import argparse
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import zipfile


ROOT = Path(__file__).resolve().parents[2]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
EXPECTED_PROJECTS = 55
EXPECTED_AGGREGATORS = 7
EXPECTED_JAR_PROJECTS = 47
EXPECTED_ATTACHMENTS = 47
# internal work item 2726 removed the three empty compatibility shells from the reactor.
EXPECTED_SHELLS: set[str] = set()


def text(node: ET.Element, path: str) -> str:
    found = node.find(path, NS)
    return "" if found is None or found.text is None else found.text.strip()


def reactor(root: Path) -> list[tuple[Path, ET.Element]]:
    projects: list[tuple[Path, ET.Element]] = []
    visited: set[Path] = set()

    def visit(pom: Path) -> None:
        pom = pom.resolve()
        if pom in visited:
            raise ValueError(f"duplicate/cyclic reactor project: {pom.relative_to(root)}")
        if not pom.is_relative_to(root) or not pom.is_file():
            raise ValueError(f"declared module POM is missing or outside the repository: {pom}")
        visited.add(pom)
        project = ET.parse(pom).getroot()
        projects.append((pom, project))
        for module in project.findall("m:modules/m:module", NS):
            module_path = text(module, ".")
            if not module_path:
                raise ValueError(f"empty module declaration in {pom.relative_to(root)}")
            visit(pom.parent / module_path / "pom.xml")

    visit(root / "pom.xml")
    return projects


def release_profile(root_project: ET.Element) -> ET.Element | None:
    return next(
        (
            profile
            for profile in root_project.findall("m:profiles/m:profile", NS)
            if text(profile, "m:id") == "release-profile"
        ),
        None,
    )


def attachment_execution(profile: ET.Element, artifact: str, execution_id: str, goal: str) -> bool:
    plugins = profile.findall("m:build/m:plugins/m:plugin", NS)
    plugin = next((item for item in plugins if text(item, "m:artifactId") == artifact), None)
    if plugin is None:
        return False
    managed_plugins = profile.findall("m:build/m:pluginManagement/m:plugins/m:plugin", NS)
    managed = next(
        (item for item in managed_plugins if text(item, "m:artifactId") == artifact),
        None,
    )
    execution = next(
        (
            item
            for item in plugin.findall("m:executions/m:execution", NS)
            if text(item, "m:id") == execution_id
        ),
        None,
    )
    if execution is None and managed is not None:
        execution = next(
            (
                item
                for item in managed.findall("m:executions/m:execution", NS)
                if text(item, "m:id") == execution_id
            ),
            None,
        )
    if execution is None:
        return False
    goals = [text(item, ".") for item in execution.findall("m:goals/m:goal", NS)]
    inherited = text(execution, "m:inherited")
    return text(execution, "m:phase") == "package" and goals == [goal] and inherited != "false"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--verify-outputs",
        metavar="VERSION",
        help="also require one nonempty source and javadoc JAR per attachment target",
    )
    args = parser.parse_args()
    failures: list[str] = []
    try:
        projects = reactor(ROOT)
    except (OSError, ET.ParseError, ValueError) as error:
        print(f"FAIL: cannot enumerate declared Maven reactor: {error}", file=sys.stderr)
        return 1

    aggregators = [item for item in projects if item[1].find("m:modules", NS) is not None]
    jar_projects = [
        item for item in projects if (text(item[1], "m:packaging") or "jar") == "jar"
    ]
    shells = [
        item
        for item in jar_projects
        if text(item[1], "m:properties/m:maven.deploy.skip") == "true"
    ]
    attachments = [item for item in jar_projects if item not in shells]
    pom_leaves = [
        item
        for item in projects
        if (text(item[1], "m:packaging") or "jar") == "pom"
        and item[1].find("m:modules", NS) is None
    ]

    if len(projects) != EXPECTED_PROJECTS:
        failures.append(f"expected {EXPECTED_PROJECTS} reactor projects, got {len(projects)}")
    if len(aggregators) != EXPECTED_AGGREGATORS:
        failures.append(f"expected {EXPECTED_AGGREGATORS} aggregators, got {len(aggregators)}")
    if len(jar_projects) != EXPECTED_JAR_PROJECTS:
        failures.append(
            f"expected {EXPECTED_JAR_PROJECTS} JAR projects, got {len(jar_projects)}"
        )
    if len(attachments) != EXPECTED_ATTACHMENTS:
        failures.append(
            f"expected {EXPECTED_ATTACHMENTS} JAR attachment targets, got {len(attachments)}"
        )
    shell_paths = {pom.relative_to(ROOT).as_posix() for pom, _ in shells}
    if shell_paths != EXPECTED_SHELLS:
        failures.append(
            "non-published compatibility shells differ from the exact approved allowlist: "
            f"expected {sorted(EXPECTED_SHELLS)}, got {sorted(shell_paths)}"
        )
    for pom, project in shells:
        for property_name in ("maven.deploy.skip", "maven.source.skip", "maven.javadoc.skip"):
            if text(project, f"m:properties/m:{property_name}") != "true":
                failures.append(
                    f"compatibility shell {pom.relative_to(ROOT)} must set {property_name}=true"
                )
    if [pom.relative_to(ROOT).as_posix() for pom, _ in pom_leaves] != ["appbase-bom/pom.xml"]:
        failures.append("the sole non-attachment POM leaf must be appbase-bom/pom.xml")

    profile = release_profile(projects[0][1])
    if profile is None:
        failures.append("release-profile is missing")
    else:
        if not attachment_execution(profile, "maven-source-plugin", "attach-sources", "jar-no-fork"):
            failures.append("release-profile source attachment is not inherited at package")
        if not attachment_execution(profile, "maven-javadoc-plugin", "javadoc-jar", "jar"):
            failures.append("release-profile javadoc attachment is not inherited at package")

    if args.verify_outputs:
        for pom, project in attachments:
            artifact_id = text(project, "m:artifactId")
            for classifier in ("sources", "javadoc"):
                artifact = (
                    pom.parent
                    / "target"
                    / f"{artifact_id}-{args.verify_outputs}-{classifier}.jar"
                )
                relative = artifact.relative_to(ROOT)
                if not artifact.is_file():
                    failures.append(f"missing {classifier} attachment: {relative}")
                    continue
                try:
                    with zipfile.ZipFile(artifact) as archive:
                        corrupt = archive.testzip()
                        if corrupt is not None:
                            failures.append(f"corrupt {classifier} attachment {relative}: {corrupt}")
                        elif not archive.namelist():
                            failures.append(f"empty {classifier} attachment: {relative}")
                except zipfile.BadZipFile:
                    failures.append(f"invalid ZIP attachment: {relative}")

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1

    for pom, project in attachments:
        print(f"ATTACH {text(project, 'm:artifactId')} {pom.relative_to(ROOT).as_posix()}")
    print(
        f"PASS: {len(projects)} reactor projects, {len(aggregators)} aggregators, "
        f"{len(jar_projects)} JAR projects ({len(attachments)} publishable source/javadoc "
        f"attachment targets plus {len(shells)} non-published compatibility shells), and one "
        "BOM POM leaf"
    )
    if args.verify_outputs:
        print(
            f"PASS: all {len(attachments) * 2} exactly named, nonempty source/javadoc ZIPs "
            f"exist for {len(attachments)} publishable targets at version {args.verify_outputs}"
        )
    else:
        print("NOTE: this is the expected inventory; artifact production still requires package proof")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
