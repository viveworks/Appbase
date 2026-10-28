#!/usr/bin/env python3
"""Generate the exact Checkstyle suppression baseline from reactor XML reports.

Generation consumes the 58 per-project ``target/checkstyle-result.xml`` files emitted by the documented
direct Maven ``check`` goal. Git supplies the tracked generation set. Verify mode derives current module
ownership from cached plus untracked nonignored POM/Java paths and refuses report paths not owned by the
module, wrong roots, paths outside the repository, or duplicate file/event identities. Checkstyle XML
contains file nodes only for files with findings, so absence from the XML is not evidence that a clean
file was omitted from the scan.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True, order=True)
class Finding:
    path: str
    line: int
    column: int
    severity: str
    source: str
    message: str


def git_paths(root: Path, pattern: str) -> list[str]:
    result = subprocess.run(
        ["git", "-C", str(root), "ls-files", pattern],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
    )
    return sorted(line for line in result.stdout.splitlines() if line)


def current_git_paths(root: Path, patterns: list[str]) -> list[str]:
    result = subprocess.run(
        ["git", "-C", str(root), "ls-files", "--cached", "--others", "--exclude-standard",
         "--", *patterns],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
    )
    return sorted(line for line in result.stdout.splitlines() if line)


def module_java_ownership(root: Path) -> dict[str, set[str]]:
    pom_dirs = {
        "." if path == "pom.xml" else Path(path).parent.as_posix()
        for path in current_git_paths(root, ["*pom.xml"])
    }
    if not pom_dirs:
        raise ValueError("Git returned no cached or untracked nonignored POMs")
    ownership = {directory: set() for directory in pom_dirs}
    for java_path in current_git_paths(root, ["*.java"]):
        candidates = [directory for directory in pom_dirs
                      if directory == "." or java_path.startswith(directory + "/")]
        if not candidates:
            raise ValueError(f"Java file has no ancestor POM: {java_path}")
        owner = max(candidates, key=lambda value: len(Path(value).parts))
        relative = Path(java_path) if owner == "." else Path(java_path).relative_to(owner)
        parts = relative.parts
        if len(parts) < 4 or parts[:3] not in (("src", "main", "java"),
                                              ("src", "test", "java")):
            raise ValueError(f"Java file is outside an owning module's standard source trees: {java_path}")
        ownership[owner].add(java_path)
    return ownership


def load_findings(root: Path) -> tuple[list[Finding], int, int]:
    findings: list[Finding] = []
    report_files: set[str] = set()
    pom_paths = git_paths(root, "*pom.xml")
    if not pom_paths:
        raise ValueError("Git returned no tracked POMs; refusing a vacuous baseline")

    for pom_path in pom_paths:
        module = root if pom_path == "pom.xml" else root / Path(pom_path).parent
        report = module / "target" / "checkstyle-result.xml"
        if not report.is_file():
            raise ValueError(f"missing Checkstyle report for {pom_path}: {report}")
        document = ET.parse(report)
        if document.getroot().tag != "checkstyle":
            raise ValueError(f"unexpected root element in {report}")
        for file_element in document.getroot().findall("file"):
            absolute = Path(file_element.attrib["name"]).resolve()
            try:
                relative = absolute.relative_to(root).as_posix()
            except ValueError as error:
                raise ValueError(f"report path escapes repository: {absolute}") from error
            report_files.add(relative)
            for event in file_element.findall("error"):
                findings.append(
                    Finding(
                        path=relative,
                        line=int(event.attrib["line"]),
                        column=int(event.attrib.get("column", "0")),
                        severity=event.attrib["severity"],
                        source=event.attrib["source"],
                        message=event.attrib["message"],
                    )
                )

    tracked_java = set(git_paths(root, "*.java"))
    if report_files != tracked_java:
        missing = sorted(tracked_java - report_files)
        extra = sorted(report_files - tracked_java)
        raise ValueError(
            "Checkstyle file set differs from tracked Java set: "
            f"missing={len(missing)} extra={len(extra)} "
            f"first_missing={missing[:1]} first_extra={extra[:1]}"
        )
    if any(finding.severity != "error" for finding in findings):
        raise ValueError("baseline contains a non-error event; severity is part of the identity")
    if len(set(findings)) != len(findings):
        raise ValueError("duplicate Checkstyle finding identities; suppression would lose multiplicity")
    return sorted(findings), len(pom_paths), len(report_files)


def java_regex(value: str) -> str:
    """Return an anchored Java-compatible literal regex for a Checkstyle attribute."""
    return "^" + re.escape(value) + "$"


def file_regex(path: str) -> str:
    escaped = re.escape(path).replace("/", r"[/\\]")
    return r"^.*[/\\]" + escaped + "$"


def suppression_identity(finding: Finding) -> tuple[str, str, str, str, str]:
    return (
        file_regex(finding.path),
        java_regex(finding.source),
        java_regex(finding.message),
        str(finding.line),
        str(finding.column),
    )


def report_snapshot(root: Path, report: Path) -> tuple[list[Finding], set[str]]:
    findings: list[Finding] = []
    document = ET.parse(report)
    report_root = document.getroot()
    if report_root.tag != "checkstyle":
        raise ValueError(f"unexpected report root {report_root.tag!r}; expected 'checkstyle'")
    reported_paths: list[str] = []
    for file_element in report_root.findall("file"):
        absolute = Path(file_element.attrib["name"]).resolve()
        try:
            relative = absolute.relative_to(root).as_posix()
        except ValueError as error:
            raise ValueError(f"report path escapes repository: {absolute}") from error
        reported_paths.append(relative)
        for event in file_element.findall("error"):
            findings.append(Finding(
                path=relative,
                line=int(event.attrib["line"]),
                column=int(event.attrib.get("column", "0")),
                severity=event.attrib["severity"],
                source=event.attrib["source"],
                message=event.attrib["message"],
            ))
    if len(reported_paths) != len(set(reported_paths)):
        raise ValueError("report contains duplicate file nodes")
    if len(findings) != len(set(findings)):
        raise ValueError("report contains duplicate finding identities")
    return findings, set(reported_paths)


def render(findings: list[Finding], report_count: int, file_count: int) -> str:
    lines = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<!DOCTYPE suppressions PUBLIC "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN"',
        '        "https://checkstyle.org/dtds/suppressions_1_2.dtd">',
        '<!-- GENERATED by scripts/checkstyle-baseline.py; do not hand-edit.',
        f'     Measured reports={report_count}, tracked Java files={file_count}, findings={len(findings)}.',
        '     Identity = repo-relative path + line + column + severity(error) + full source + exact message.',
        '     Every regex is anchored; a same-count replacement finding remains unbaselined and fails. -->',
        '<suppressions>',
    ]
    for finding in findings:
        element = ET.Element(
            "suppress",
            {
                "files": file_regex(finding.path),
                "checks": java_regex(finding.source),
                "message": java_regex(finding.message),
                "lines": str(finding.line),
                "columns": str(finding.column),
            },
        )
        lines.append("    " + ET.tostring(element, encoding="unicode", short_empty_elements=True))
    lines.extend(["</suppressions>", ""])
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--output", type=Path)
    parser.add_argument("--verify", action="store_true", help="compare instead of overwriting")
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--check-report", type=Path,
                        help="fail if this raw report contains an identity absent from --baseline")
    parser.add_argument("--module-root", type=Path,
                        help="module whose direct src/main/java and src/test/java coverage is required")
    args = parser.parse_args()

    root = args.root.resolve()
    if args.check_report:
        if not args.baseline or not args.module_root:
            parser.error("--check-report requires --baseline and --module-root")
        module_root = args.module_root.resolve()
        try:
            module_relative = module_root.relative_to(root).as_posix()
        except ValueError as error:
            raise ValueError(f"module root escapes repository: {module_root}") from error
        ownership = module_java_ownership(root)
        if module_relative not in ownership:
            raise ValueError(f"module root has no cached or untracked nonignored POM: {module_relative}")
        expected_files = ownership[module_relative]
        baseline_path = args.baseline if args.baseline.is_absolute() else root / args.baseline
        baseline_root = ET.parse(baseline_path).getroot()
        if baseline_root.tag != "suppressions":
            raise ValueError(f"unexpected baseline root {baseline_root.tag!r}; expected 'suppressions'")
        required_attributes = ("files", "checks", "message", "lines", "columns")
        baseline_rows: list[tuple[str, str, str, str, str]] = []
        for row_number, node in enumerate(list(baseline_root), start=1):
            if node.tag != "suppress":
                raise ValueError(f"baseline row {row_number} has unexpected element {node.tag!r}")
            if set(node.attrib) != set(required_attributes):
                raise ValueError(
                    f"baseline row {row_number} attributes differ from required set "
                    f"{required_attributes}: {sorted(node.attrib)}"
                )
            values = tuple(node.attrib[key] for key in required_attributes)
            if any(not value for value in values):
                raise ValueError(f"baseline row {row_number} has an empty identity attribute")
            baseline_rows.append(values)
        if len(baseline_rows) != len(set(baseline_rows)):
            raise ValueError("baseline contains duplicate identity rows")
        known = set(baseline_rows)
        current, reported_files = report_snapshot(root, args.check_report.resolve())
        unexpected_files = reported_files - expected_files
        if unexpected_files:
            raise ValueError(
                f"report contains Java paths not owned by {module_relative}: "
                f"owned={len(expected_files)} reported_with_findings={len(reported_files)} "
                f"unexpected={len(unexpected_files)} first_unexpected={sorted(unexpected_files)[:1]}"
            )
        if any(finding.severity != "error" for finding in current):
            raise ValueError("raw report contains non-error severity")
        new = [finding for finding in current if suppression_identity(finding) not in known]
        if new:
            for finding in new[:20]:
                print(f"NEW {finding.path}:{finding.line}:{finding.column} "
                      f"{finding.source}: {finding.message}", file=sys.stderr)
            print(f"checkstyle-ratchet: REFUSING: {len(new)} new/unbaselined finding(s)",
                  file=sys.stderr)
            return 1
        print(f"checkstyle-ratchet: owned_files={len(expected_files)} "
              f"reported_with_findings={len(reported_files)} current={len(current)} new=0 "
              f"report={args.check_report}")
        return 0

    if not args.output:
        parser.error("generation requires --output")
    findings, report_count, file_count = load_findings(root)
    rendered = render(findings, report_count, file_count)
    output = args.output if args.output.is_absolute() else root / args.output
    if args.verify:
        if not output.is_file() or output.read_text(encoding="utf-8") != rendered:
            print(f"Checkstyle baseline differs from deterministic report projection: {output}", file=sys.stderr)
            return 1
    else:
        output.write_text(rendered, encoding="utf-8")
    print(
        f"checkstyle-baseline: reports={report_count} files={file_count} "
        f"findings={len(findings)} output={output}"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, ET.ParseError, subprocess.CalledProcessError) as error:
        print(f"checkstyle-baseline: REFUSING: {error}", file=sys.stderr)
        raise SystemExit(2)
