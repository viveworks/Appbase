#!/usr/bin/env python3
"""Validate the Maven-generated effective release POM for internal work item 1144/N01."""

import argparse
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


NS = {"m": "http://maven.apache.org/POM/4.0.0"}
APPROVED_DEVELOPER = (
    "Navaneeth Mysore Govindarajan",
    "navaneeth.mysore@viveworks.com",
)
RELEASE_URL = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
SNAPSHOT_URL = "https://s01.oss.sonatype.org/content/repositories/snapshots"


def text(node: ET.Element, path: str) -> str:
    found = node.find(path, NS)
    return "" if found is None or found.text is None else found.text.strip()


def plugin(project: ET.Element, artifact_id: str) -> ET.Element | None:
    for candidate in project.findall("m:build/m:plugins/m:plugin", NS):
        if text(candidate, "m:artifactId") == artifact_id:
            return candidate
    return None


def execution(plugin_node: ET.Element, execution_id: str) -> ET.Element | None:
    for candidate in plugin_node.findall("m:executions/m:execution", NS):
        if text(candidate, "m:id") == execution_id:
            return candidate
    return None


def goals(execution_node: ET.Element) -> list[str]:
    return [text(goal, ".") for goal in execution_node.findall("m:goals/m:goal", NS)]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pom", required=True, type=Path)
    parser.add_argument(
        "--expected-scm-url",
        help="when set, require all generated SCM fields to name this public repository",
    )
    parser.add_argument("--expected-scm-tag", default="HEAD")
    parser.add_argument(
        "--resolved-scm-tag",
        help="project.scm.tag from Maven help:evaluate when effective-pom omits default HEAD",
    )
    args = parser.parse_args()
    failures: list[str] = []

    try:
        project = ET.parse(args.pom).getroot()
    except (OSError, ET.ParseError) as error:
        print(f"FAIL: cannot read effective POM: {error}", file=sys.stderr)
        return 1

    developers = project.findall("m:developers/m:developer", NS)
    identities = [(text(item, "m:name"), text(item, "m:email")) for item in developers]
    if identities != [APPROVED_DEVELOPER]:
        failures.append(
            "generated POM must contain exactly the approved developer identity; "
            f"got {identities!r}"
        )

    for path, expected_url, label in (
        ("m:distributionManagement/m:repository", RELEASE_URL, "release"),
        ("m:distributionManagement/m:snapshotRepository", SNAPSHOT_URL, "snapshot"),
    ):
        repository = project.find(path, NS)
        if repository is None:
            failures.append(f"generated POM is missing the {label} repository")
        elif (text(repository, "m:id"), text(repository, "m:url")) != (
            "ossrh",
            expected_url,
        ):
            failures.append(
                f"generated POM has unexpected {label} repository: "
                f"{text(repository, 'm:id')!r} {text(repository, 'm:url')!r}"
            )

    if args.expected_scm_url is not None:
        public_url = args.expected_scm_url.rstrip("/")
        serialized_tag = text(project, "m:scm/m:tag")
        resolved_tag = serialized_tag or (args.resolved_scm_tag or "")
        actual_scm = (
            text(project, "m:scm/m:connection"),
            text(project, "m:scm/m:developerConnection"),
            text(project, "m:scm/m:url"),
            resolved_tag,
        )
        expected_scm = (
            f"scm:git:{public_url}.git",
            f"scm:git:{public_url}.git",
            public_url,
            args.expected_scm_tag,
        )
        if actual_scm != expected_scm:
            failures.append(
                "generated SCM connection/developerConnection/url/tag do not exactly name the "
                f"public repository: got {actual_scm!r}"
            )
        if not serialized_tag and args.resolved_scm_tag is None:
            failures.append(
                "effective-pom omitted the default SCM tag; supply Maven's independently resolved "
                "project.scm.tag value"
            )

    gpg = plugin(project, "maven-gpg-plugin")
    sign = None if gpg is None else execution(gpg, "sign-artifacts")
    if sign is None or text(sign, "m:phase") != "verify" or goals(sign) != ["sign"]:
        failures.append("generated POM does not bind sign-artifacts:sign to verify")

    staging = plugin(project, "nexus-staging-maven-plugin")
    if staging is None:
        failures.append("release-profile did not activate nexus-staging-maven-plugin")
    elif (
        text(staging, "m:configuration/m:serverId"),
        text(staging, "m:configuration/m:nexusUrl"),
        text(staging, "m:configuration/m:autoReleaseAfterClose"),
    ) != ("ossrh", "https://s01.oss.sonatype.org/", "false"):
        failures.append("effective staging configuration is not ossrh/fail-closed")

    sources = plugin(project, "maven-source-plugin")
    attach_sources = None if sources is None else execution(sources, "attach-sources")
    if (
        attach_sources is None
        or text(attach_sources, "m:phase") != "package"
        or goals(attach_sources) != ["jar-no-fork"]
    ):
        failures.append("generated POM does not attach source jars at package")

    javadoc = plugin(project, "maven-javadoc-plugin")
    javadoc_jar = None if javadoc is None else execution(javadoc, "javadoc-jar")
    if (
        javadoc_jar is None
        or text(javadoc_jar, "m:phase") != "package"
        or goals(javadoc_jar) != ["jar"]
    ):
        failures.append("generated POM does not attach javadoc jars at package")

    release = plugin(project, "maven-release-plugin")
    if release is None:
        failures.append("release-profile did not activate maven-release-plugin")
    else:
        tag_name_format = text(release, "m:configuration/m:tagNameFormat")
        if tag_name_format != "@{project.version}":
            failures.append(
                "effective release plugin must explicitly use the bare-version "
                "tagNameFormat @{project.version}; absence would use Maven's "
                "artifactId-version default and a v prefix is forbidden"
            )
    if release is not None and (
        text(release, "m:configuration/m:autoVersionSubmodules"),
        text(release, "m:configuration/m:preparationGoals"),
        text(release, "m:configuration/m:releaseProfiles"),
    ) != ("true", "clean verify -Denforcer.skip=true", "release-profile"):
        failures.append(
            "effective release plugin must retain version submodules, prepare-only "
            "enforcer skip, and release-profile during perform"
        )

    enforcer = plugin(project, "maven-enforcer-plugin")
    release_enforcement = (
        None if enforcer is None else execution(enforcer, "enforce-release-environment")
    )
    if release_enforcement is None:
        failures.append("release-profile did not activate enforce-release-environment")
    else:
        rules = release_enforcement.find("m:configuration/m:rules", NS)
        rule_names = [] if rules is None else [item.tag.rsplit("}", 1)[-1] for item in rules]
        if (
            text(release_enforcement, "m:phase") != "initialize"
            or goals(release_enforcement) != ["enforce"]
            or text(release_enforcement, "m:configuration/m:fail") != "true"
            or text(release_enforcement, "m:configuration/m:failFast") != "true"
            or rule_names.count("requireReleaseDeps") != 1
            or text(
                release_enforcement,
                "m:configuration/m:rules/m:requireReleaseDeps/m:onlyWhenRelease",
            )
            != "true"
            or rule_names.count("requireReleaseVersion") != 1
        ):
            failures.append(
                "effective release environment enforcer must bind the release-only dependency "
                "and project-version rules to a fail-fast initialize execution"
            )

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1

    print(
        "PASS: Maven-generated effective POM has the sole approved developer, "
        "OSSRH endpoints, signing, source/javadoc attachment, canonical release-plugin "
        "settings, fail-fast release enforcement, and non-promoting staging"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
