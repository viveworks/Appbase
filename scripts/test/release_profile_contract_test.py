#!/usr/bin/env python3
"""Static source-POM checks for release metadata and plugin configuration."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
POM = ROOT / "pom.xml"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def text(node: ET.Element, path: str) -> str:
    found = node.find(path, NS)
    return "" if found is None or found.text is None else found.text.strip()


def main() -> int:
    project = ET.parse(POM).getroot()
    failures: list[str] = []

    developers = project.findall("m:developers/m:developer", NS)
    if len(developers) != 1:
        failures.append(f"expected exactly one published developer, got {len(developers)}")
    elif (text(developers[0], "m:name"), text(developers[0], "m:email")) != (
        "Navaneeth Mysore Govindarajan",
        "navaneeth.mysore@viveworks.com",
    ):
        failures.append("published developer metadata does not match the approved identity")

    if text(project, "m:properties/m:appbase.version") != "${project.version}":
        failures.append(
            "internal Appbase dependencies must follow project.version during release transitions"
        )

    managed_dependencies = {
        (text(dependency, "m:groupId"), text(dependency, "m:artifactId")): text(
            dependency, "m:version"
        )
        for dependency in project.findall("m:dependencyManagement/m:dependencies/m:dependency", NS)
    }
    release_bounds = {
        ("org.antlr", "antlr4-runtime"): "${antlr4-runtime.version}",
        ("io.dropwizard.metrics", "metrics-jmx"): "${dropwizard-metrics.version}",
        ("org.xerial.snappy", "snappy-java"): "${snappy-java.version}",
        ("org.apache.commons", "commons-lang3"): "${commons-lang3.version}",
        ("commons-codec", "commons-codec"): "${commons-codec.version}",
        ("com.thoughtworks.paranamer", "paranamer"): "${paranamer.version}",
        ("org.lz4", "lz4-java"): "${lz4-java.version}",
        ("org.scala-lang", "scala-library"): "${scala-library.version}",
        ("com.github.luben", "zstd-jni"): "${zstd-jni.version}",
        ("org.glassfish.jersey.core", "jersey-common"): "${jersey.version}",
        ("org.glassfish.jersey.core", "jersey-client"): "${jersey.version}",
        ("org.glassfish.jersey.core", "jersey-server"): "${jersey.version}",
        ("org.glassfish.jersey.containers", "jersey-container-servlet"): "${jersey.version}",
        ("jakarta.ws.rs", "jakarta.ws.rs-api"): "3.1.0",
        ("org.apache.curator", "curator-client"): "${curation.version}",
        ("io.opentracing", "opentracing-util"): "${opentracing.version}",
        ("org.checkerframework", "checker-qual"): "${checker-qual.version}",
        ("com.google.api.grpc", "proto-google-common-protos"): "${proto-google-common-protos.version}",
        ("io.perfmark", "perfmark-api"): "${perfmark-api.version}",
        ("org.jboss.logging", "jboss-logging"): "${jboss-logging.version}",
    }
    for coordinate, expected_version in release_bounds.items():
        if managed_dependencies.get(coordinate) != expected_version:
            failures.append(
                f"{coordinate[0]}:{coordinate[1]} must be centrally pinned to "
                f"{expected_version} for release upper-bound convergence"
            )

    repository = project.find("m:distributionManagement/m:repository", NS)
    if repository is None or text(repository, "m:id") != "ossrh":
        failures.append("release distribution repository must use the ossrh server id")
    elif not text(repository, "m:url").startswith("https://s01.oss.sonatype.org/"):
        failures.append("release distribution repository is not the approved Sonatype endpoint")

    release = next(
        (p for p in project.findall("m:profiles/m:profile", NS) if text(p, "m:id") == "release-profile"),
        None,
    )
    if release is None:
        failures.append("release-profile is missing")
    else:
        plugins = {
            text(p, "m:artifactId"): p
            for p in release.findall("m:build/m:pluginManagement/m:plugins/m:plugin", NS)
        }
        for artifact in ("maven-gpg-plugin", "nexus-staging-maven-plugin"):
            if artifact not in plugins:
                failures.append(f"release-profile is missing {artifact}")
        staging = plugins.get("nexus-staging-maven-plugin")
        if staging is not None and text(staging, "m:configuration/m:autoReleaseAfterClose") != "false":
            failures.append("Sonatype staging must not auto-release after close")
        active_plugins = {
            text(plugin, "m:artifactId"): plugin
            for plugin in release.findall("m:build/m:plugins/m:plugin", NS)
        }
        javadoc = active_plugins.get("maven-javadoc-plugin")
        execution_ids = (
            []
            if javadoc is None
            else [text(item, "m:id") for item in javadoc.findall("m:executions/m:execution", NS)]
        )
        if "javadoc-jar" not in execution_ids:
            failures.append("release-profile must attach per-module javadoc jars")
        if "aggregate-javadoc-jar" in execution_ids:
            failures.append(
                "release-profile must not fork an aggregate package lifecycle before reactor "
                "classifier artifacts exist"
            )

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1
    print("PASS: source POM contains approved release metadata and fail-closed plugin configuration")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
