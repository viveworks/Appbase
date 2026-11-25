#!/usr/bin/env python3
"""Verify the complete LGPL payload in a final Config Wizard shaded JAR."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path, PurePosixPath


PREFIX = PurePosixPath("META-INF/lgpl")
REQUIRED = {
    str(PREFIX / "COPYING.LESSER-2.1.txt"),
    str(PREFIX / "COPYING.LESSER-3.0.txt"),
    str(PREFIX / "COPYING.GPL-3.0.txt"),
    str(PREFIX / "README.md"),
    str(PREFIX / "source-manifest.json"),
}
LICENCE_SHA256 = {
    str(PREFIX / "COPYING.LESSER-2.1.txt"):
        "730aca838484e53c7c4838873de0cf2f77fc08f27b18f3f20ab775a52687042a",
    str(PREFIX / "COPYING.LESSER-3.0.txt"):
        "1a45b1d0a8603dfe2cfc644f9dab970b1762f92babe2aac6eb2f5d4572c4a680",
    str(PREFIX / "COPYING.GPL-3.0.txt"):
        "e57f1c320b8cf8798a7d2ff83a6f9e06a33a03585f6e065fea97f1d86db84052",
}
EXPECTED = {
    "com.google.code.findbugs:annotations:3.0.1": {
        "spdx": "LGPL-2.1-or-later", "source": "annotations-3.0.1-sources.jar",
        "sha256": "1a64119813ca80a6ebef047190a4f62ee4ad44afe786e92d698ba7aa730ffc0a",
        "pom": "annotations-3.0.1.pom",
        "pom_sha256": "649ecedb944702cd5ed3538d0bd651bbe3866cfc1008f6fb265de986d6760292",
        "class": "javax/annotation/CheckForNull.class",
    },
    "com.github.spotbugs:spotbugs-annotations:4.10.3": {
        "spdx": "LGPL-2.1-only", "source": "spotbugs-annotations-4.10.3-sources.jar",
        "sha256": "87974d23caffbc8c6e66c567747627267b5ed06573cee966d7af6d236b8d65bd",
        "pom": "spotbugs-annotations-4.10.3.pom",
        "pom_sha256": "9aca49f4a1c39a2df7a22989989b76886cd19e4e795e63e6b8f16bf00e840cf5",
        "class": "edu/umd/cs/findbugs/annotations/ExpectWarning.class",
    },
    "net.openhft:affinity:3.0.6": {
        "spdx": "LGPL-3.0-only", "source": "affinity-3.0.6-sources.jar",
        "sha256": "8e797604b4ad747b7f993c185400dbfc6966572c30d7b2715915682d3a206be9",
        "pom": "affinity-3.0.6.pom",
        "pom_sha256": "f0686b5f96193247f32ad04e4bd4fadaeca54260fd9c60564c077499d62df56b",
        "class": "net/openhft/affinity/Affinity.class",
    },
    "org.hibernate.common:hibernate-commons-annotations:6.0.6.Final": {
        "spdx": "LGPL-2.1-or-later", "source": "hibernate-commons-annotations-6.0.6.Final-sources.jar",
        "sha256": "5f9ef9fcd3a6942102f4857a4c7fb2a85e203d9a6ece7d700ec7d1f789411991",
        "pom": "hibernate-commons-annotations-6.0.6.Final.pom",
        "pom_sha256": "87387244eda18c181d0ef1301f8cf2790f074ced8b5eb165d6a5bd4ed207e4c4",
        "class": "org/hibernate/annotations/common/annotationfactory/AnnotationFactory.class",
    },
    "org.hibernate.orm:hibernate-core:6.5.3.Final": {
        "spdx": "LGPL-2.1-or-later", "source": "hibernate-core-6.5.3.Final-sources.jar",
        "sha256": "dc63ac15239aad5666e15988bdbf28ddd98b2fbe558b3cc79bfd6a7c19df52bf",
        "pom": "hibernate-core-6.5.3.Final.pom",
        "pom_sha256": "1c914c1a777714072246f51003195ff9ff3efc36054bdddb4eca301a048585c9",
        "class": "org/hibernate/Session.class",
    },
    "org.mariadb.jdbc:mariadb-java-client:2.7.4": {
        "spdx": "LGPL-2.1-only", "source": "mariadb-java-client-2.7.4-sources.jar",
        "sha256": "84bf45e993d4e144a1efec6df943e00a1f454c5f5a3eb565cf820de2fb089406",
        "pom": "mariadb-java-client-2.7.4.pom",
        "pom_sha256": "0232c207f2e5249270fc7bcd5c0e94813c36becb0902afb682aba729787b8254",
        "class": "org/mariadb/jdbc/Driver.class",
    },
}


def pom_gav(content: bytes) -> str:
    root = ET.fromstring(content)
    namespace = root.tag.partition("}")[0] + "}" if root.tag.startswith("{") else ""
    own = lambda name: root.findtext(namespace + name)
    parent = root.find(namespace + "parent")
    inherited = lambda name: parent.findtext(namespace + name) if parent is not None else None
    group = own("groupId") or inherited("groupId")
    artifact = own("artifactId")
    version = own("version") or inherited("version")
    if not all((group, artifact, version)):
        raise ValueError("POM does not declare an effective GAV")
    return f"{group}:{artifact}:{version}"


def validate(path: Path) -> list[str]:
    failures: list[str] = []
    try:
        archive = zipfile.ZipFile(path)
    except (OSError, zipfile.BadZipFile) as exc:
        return [f"cannot read shaded JAR: {exc}"]
    with archive:
        names = set(archive.namelist())
        for required in sorted(REQUIRED):
            if required not in names:
                failures.append(f"missing {required}")
        for licence, digest in LICENCE_SHA256.items():
            if licence in names:
                actual = hashlib.sha256(archive.read(licence)).hexdigest()
                if actual != digest:
                    failures.append(f"licence digest mismatch for {licence}: {actual} != {digest}")
        manifest_name = str(PREFIX / "source-manifest.json")
        if manifest_name not in names:
            return failures
        try:
            manifest = json.loads(archive.read(manifest_name))
        except (json.JSONDecodeError, UnicodeDecodeError) as exc:
            failures.append(f"invalid source manifest: {exc}")
            return failures
        dependencies = manifest.get("dependencies")
        if manifest.get("format") != "appbase-config-wizard-lgpl-source-v1":
            failures.append("unexpected source manifest format")
        if not isinstance(dependencies, list) or len(dependencies) != len(EXPECTED):
            failures.append("source manifest must contain exactly six dependencies")
            return failures

        expected_sources: set[str] = set()
        expected_poms: set[str] = set()
        seen_gavs: set[str] = set()
        for dependency in dependencies:
            if not isinstance(dependency, dict):
                failures.append("source manifest dependency is not an object")
                continue
            gav = dependency.get("gav")
            source = dependency.get("source")
            digest = dependency.get("sha256")
            spdx = dependency.get("spdx")
            pom = dependency.get("pom")
            pom_digest = dependency.get("pom_sha256")
            if not all(isinstance(value, str) and value
                       for value in (gav, source, digest, spdx, pom, pom_digest)):
                failures.append("source manifest dependency has incomplete metadata")
                continue
            if gav in seen_gavs:
                failures.append(f"duplicate manifest coordinate: {gav}")
            seen_gavs.add(gav)
            approved = EXPECTED.get(gav)
            if approved is None:
                failures.append(f"unapproved manifest coordinate: {gav}")
                continue
            for field in ("spdx", "source", "sha256", "pom", "pom_sha256"):
                if dependency.get(field) != approved[field]:
                    failures.append(
                        f"approved matrix mismatch for {gav} {field}: "
                        f"{dependency.get(field)} != {approved[field]}"
                    )
            source_name = str(PREFIX / "sources" / source)
            pom_name = str(PREFIX / "sources" / pom)
            expected_sources.add(source_name)
            expected_poms.add(pom_name)
            if source_name not in names:
                failures.append(f"missing corresponding source: {source_name}")
            else:
                actual = hashlib.sha256(archive.read(source_name)).hexdigest()
                if actual != digest:
                    failures.append(f"source digest mismatch for {gav}: {actual} != {digest}")
                try:
                    with zipfile.ZipFile(io.BytesIO(archive.read(source_name))) as source_archive:
                        if not any(name.endswith(".java") for name in source_archive.namelist()):
                            failures.append(f"source archive contains no Java source for {gav}")
                except zipfile.BadZipFile:
                    failures.append(f"corresponding source is not a ZIP/JAR for {gav}")
            if pom_name not in names:
                failures.append(f"missing published build POM: {pom_name}")
            else:
                pom_content = archive.read(pom_name)
                actual_pom_digest = hashlib.sha256(pom_content).hexdigest()
                if actual_pom_digest != pom_digest:
                    failures.append(
                        f"POM digest mismatch for {gav}: {actual_pom_digest} != {pom_digest}"
                    )
                try:
                    actual_gav = pom_gav(pom_content)
                    if actual_gav != gav:
                        failures.append(f"POM GAV mismatch for {gav}: {actual_gav}")
                except (ET.ParseError, ValueError) as exc:
                    failures.append(f"invalid published POM for {gav}: {exc}")
            if approved["class"] not in names:
                failures.append(f"missing shaded representative class for {gav}: {approved['class']}")

        missing_gavs = set(EXPECTED) - seen_gavs
        if missing_gavs:
            failures.append(f"approved coordinates absent from manifest: {sorted(missing_gavs)}")

        actual_sources = {
            name for name in names
            if name.startswith(str(PREFIX / "sources") + "/") and name.endswith("-sources.jar")
        }
        actual_poms = {
            name for name in names
            if name.startswith(str(PREFIX / "sources") + "/") and name.endswith(".pom")
        }
        stale_sources = actual_sources - expected_sources
        stale_poms = actual_poms - expected_poms
        if stale_sources or expected_sources - actual_sources:
            failures.append(
                "source archive set differs from manifest: "
                f"extra={sorted(stale_sources)} missing={sorted(expected_sources - actual_sources)}"
            )
        if stale_poms or expected_poms - actual_poms:
            failures.append(
                "published POM set differs from manifest: "
                f"extra={sorted(stale_poms)} missing={sorted(expected_poms - actual_poms)}"
            )
    return failures


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path, help="final shaded Config Wizard JAR")
    args = parser.parse_args()
    failures = validate(args.jar)
    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1
    print(f"config-wizard LGPL payload PASS: {args.jar}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
