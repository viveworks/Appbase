#!/usr/bin/env python3
"""Coordinate the light, deterministic groundwork for API-doc acceptance (internal work item 2541).

This command deliberately does not generate Javadoc or Dartdoc. It composes the
existing inventory and full catalogue gates, then checks the declared reader-path
links. Pending generated outputs remain NOT MEASURED and produce exit code 2.

@author Navaneeth Mysore G
@author Claude
"""

import argparse
from html.parser import HTMLParser
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import unquote, urlsplit


INLINE_LINK = re.compile(r"(?<!!)(?<!\\)\[[^]]+\]\(([^)]+)\)")
REFERENCE_USE = re.compile(r"(?<!!)(?<!\\)\[[^]]+\]\[([^]]+)\]")
REFERENCE_DEF = re.compile(r"^\s*\[([^]]+)\]:\s*(\S+)", re.M)
FENCE = re.compile(r"^\s*(`{3,}|~{3,})")
HEADING = re.compile(r"^\s{0,3}(#{1,6})\s+(.+?)\s*#*\s*$")
EXTERNAL_SCHEMES = {"http", "https", "mailto"}
EXPECTED_ROLES = {"catalogue", "supported-modules", "supported-adapters", "known-limitations"}


class AnchorParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = set()
        self.links = []

    def handle_starttag(self, _tag, attrs):
        for key, value in attrs:
            if key in ("id", "name") and value:
                self.ids.add(value)
            if _tag == "a" and key == "href" and value:
                self.links.append(value)


def visible_markdown(text, preserve_inline_code_text=False):
    """Drop fenced blocks and HTML comments before extracting links/headings."""
    out, fence, in_comment = [], None, False
    for raw in text.splitlines():
        line = raw
        if fence:
            if re.match(r"^\s*%s{%d,}\s*$" % (re.escape(fence[0]), fence[1]), line):
                fence = None
            continue
        marker = FENCE.match(line)
        if marker:
            fence = (marker.group(1)[0], len(marker.group(1)))
            continue
        if line.startswith("    ") or line.startswith("\t"):
            continue
        visible = ""
        while line:
            if in_comment:
                end = line.find("-->")
                if end < 0:
                    line = ""
                else:
                    line, in_comment = line[end + 3 :], False
            else:
                start = line.find("<!--")
                if start < 0:
                    visible, line = visible + line, ""
                else:
                    visible, line, in_comment = visible + line[:start], line[start + 4 :], True
        out.append(visible)
    return re.sub(
        r"(`+)(.*?)\1",
        (lambda match: match.group(2)) if preserve_inline_code_text else "",
        "\n".join(out),
        flags=re.S,
    )


def markdown_links(text):
    visible = visible_markdown(text)
    refs = {}
    for name, target in REFERENCE_DEF.findall(visible):
        normalized = name.casefold()
        if normalized in refs:
            raise ValueError("duplicate reference definition: %s" % name)
        refs[normalized] = target
    links = INLINE_LINK.findall(visible)
    links += [refs[name.casefold()] for name in REFERENCE_USE.findall(visible) if name.casefold() in refs]
    return links


def markdown_anchors(text):
    """The bounded heading form used by this matrix; duplicate-slug rendering is left pending."""
    anchors = set()
    for line in visible_markdown(text, preserve_inline_code_text=True).splitlines():
        match = HEADING.match(line)
        if not match:
            continue
        label = re.sub(r"<[^>]+>", "", match.group(2)).strip().lower()
        label = re.sub(r"[^\w\s-]", "", label, flags=re.UNICODE)
        label = re.sub(r"[\s-]+", "-", label).strip("-")
        if label:
            anchors.add(label)
    return anchors


def safe_path(root, base, encoded):
    parsed = urlsplit(encoded)
    if parsed.scheme.casefold() in EXTERNAL_SCHEMES or parsed.netloc:
        return None, parsed.fragment, "external"
    if parsed.scheme:
        return None, parsed.fragment, "unsupported scheme %s" % parsed.scheme
    raw = unquote(parsed.path)
    candidate = (base / raw).resolve()
    try:
        candidate.relative_to(root)
    except ValueError:
        return None, parsed.fragment, "outside --root"
    return candidate, unquote(parsed.fragment), None


def run_gate(root, label, args):
    result = subprocess.run(
        [sys.executable, *args], cwd=root, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False,
    )
    return {
        "name": label,
        "status": "pass" if result.returncode == 0 else "fail",
        "returncode": result.returncode,
        "output": result.stdout.strip().splitlines(),
    }


def check_link(root, row):
    role = row.get("role")
    if row.get("status", "active") == "pending":
        reason = row.get("reason")
        if not isinstance(reason, str) or not reason.strip():
            return {"role": role, "status": "fail", "problem": "pending row needs a reason"}
        return {"role": role, "status": "not-measured", "reason": reason.strip()}
    source_name, target_name = row.get("source"), row.get("target")
    if not isinstance(source_name, str) or not isinstance(target_name, str):
        return {"role": role, "status": "fail", "problem": "source and target must be strings"}
    source, _, source_problem = safe_path(root, root, source_name)
    if source_problem or source is None:
        return {"role": role, "status": "fail", "problem": "%s: %s" % (source_name, source_problem)}
    if not source.is_file():
        return {"role": role, "status": "fail", "problem": "%s is missing" % source_name}
    text = source.read_text(encoding="utf-8", errors="replace")
    try:
        if source.suffix.lower() in (".md", ".markdown"):
            links = markdown_links(text)
        elif source.suffix.lower() in (".html", ".htm"):
            parser = AnchorParser()
            parser.feed(text)
            links = parser.links
        else:
            links = []
    except ValueError as exc:
        return {"role": role, "status": "fail", "problem": "%s: %s" % (source_name, exc)}
    if target_name not in links:
        return {"role": role, "status": "fail", "problem": "%s has no visible link to %s" % (source_name, target_name)}
    target, fragment, target_problem = safe_path(root, source.parent, target_name)
    if target_problem == "external":
        return {"role": role, "status": "not-measured", "reason": "%s external link %s" % (source_name, target_name)}
    if target_problem or target is None:
        return {"role": role, "status": "fail", "problem": "%s -> %s: %s" % (source_name, target_name, target_problem)}
    if not target.is_file():
        return {"role": role, "status": "fail", "problem": "%s -> %s is missing" % (source_name, target_name)}
    if fragment:
        target_text = target.read_text(encoding="utf-8", errors="replace")
        if target.suffix.lower() in (".md", ".markdown"):
            anchors = markdown_anchors(target_text)
        elif target.suffix.lower() in (".html", ".htm"):
            parser = AnchorParser()
            parser.feed(target_text)
            anchors = parser.ids
        else:
            return {"role": role, "status": "not-measured", "reason": "unsupported fragment format: %s" % target.name}
        if fragment not in anchors:
            return {"role": role, "status": "fail", "problem": "%s lacks fragment #%s" % (target_name, fragment)}
    return {"role": role, "status": "pass", "source": source_name, "target": target_name}


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--root", default=os.path.normpath(os.path.join(os.path.dirname(__file__), "..")))
    parser.add_argument("--matrix", default="docs/api-inventory/api-docs-link-matrix.json")
    parser.add_argument("--report", required=True)
    args = parser.parse_args(argv)
    root = Path(args.root).resolve()
    matrix_path = Path(args.matrix)
    if not matrix_path.is_absolute():
        matrix_path = root / matrix_path
    report_path = Path(args.report)
    if not report_path.is_absolute():
        report_path = root / report_path

    problems = []
    try:
        matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        matrix = {}
        problems.append("matrix unreadable: %s" % exc)
    if not isinstance(matrix, dict):
        problems.append("matrix must be a JSON object")
        matrix = {}
    required = matrix.get("required_roles", [])
    rows = matrix.get("links", [])
    if matrix.get("schema") != 1 or not isinstance(required, list) or not isinstance(rows, list):
        problems.append("matrix needs schema=1 and list required_roles/links")
        required, rows = [], []
    elif any(not isinstance(role, str) for role in required):
        problems.append("required_roles must contain only strings")
        required = []
    if set(required) != EXPECTED_ROLES or len(required) != len(EXPECTED_ROLES):
        problems.append("required_roles must declare exactly: %s" % ", ".join(sorted(EXPECTED_ROLES)))
    if any(not isinstance(row, dict) for row in rows):
        problems.append("every links entry must be an object")
        rows = [row for row in rows if isinstance(row, dict)]
    malformed_roles = [row.get("role") for row in rows if not isinstance(row.get("role"), str)]
    if malformed_roles:
        problems.append("every link role must be a string")
    roles = [row.get("role") for row in rows if isinstance(row.get("role"), str)]
    for role in sorted(set(roles)):
        if roles.count(role) > 1:
            problems.append("duplicate role: %s" % role)
    for role in required:
        if role not in roles:
            problems.append("missing required role: %s" % role)

    gates = [
        run_gate(root, "api-inventory", [str(root / "scripts/api-inventory.py"), "--root", str(root), "check"]),
        run_gate(root, "catalogue-full", [str(root / "scripts/catalogue-check.py"), "--root", str(root)]),
    ]
    links = [check_link(root, row) for row in rows if isinstance(row, dict)]
    problems += ["%s: %s" % (row.get("role"), row.get("problem")) for row in links if row["status"] == "fail"]
    problems += ["%s gate rc=%s" % (gate["name"], gate["returncode"]) for gate in gates if gate["status"] == "fail"]
    pending = [row for row in links if row["status"] == "not-measured"]
    status = "fail" if problems else "incomplete" if pending else "pass"
    report = {"schema": 1, "status": status, "gates": gates, "links": links, "problems": problems}
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    for problem in problems:
        print("api-docs-acceptance: FAIL %s" % problem)
    for row in pending:
        print("api-docs-acceptance: NOT MEASURED %s: %s" % (row["role"], row["reason"]))
    print("api-docs-acceptance: %s — %d gate(s), %d link role(s)" % (status, len(gates), len(links)))
    return 1 if status == "fail" else 2 if status == "incomplete" else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
