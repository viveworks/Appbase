#!/usr/bin/env python3
"""catalogue-check.py - fail-closed comparison of the Module and Service Catalog (internal work item 2418) against the
shipped-artifact inventory (internal work item 1399).

The catalogue, docs/Module-and-Service-Catalog.md, is prose written for people. Each artifact entry has
a fixed, machine-checkable shape:

    ### `groupId:artifactId`            (or `pub:<name>` for a Dart package)
    - **Coordinates:** `groupId:artifactId:version`
    - **Module:** `path/from/repo/root`
    - **Support:** `supported-public`
    - **Purpose:** ...
    - **Capabilities:** ...
    - **Public entry points:** ...
    - **Configuration / backend:** ...
    - **Examples / API docs:** ...
    - **Limitations:** ...

The comparison target is a FRESH in-memory generation of the inventory by scripts/api-inventory.py. It
never reads the committed inventory.json, so a stale committed file cannot make it pass. It exits 1 on:

  missing           an inventory artifact with no catalogue entry
  duplicate         two catalogue entries for one artifact, or a repeated required field in one entry
  inconsistent      Coordinates / Module / Support disagree with the inventory record
  incomplete        a required field absent, empty, or a placeholder: TODO/TBD/FIXME/XXX anywhere in
                    the value, a bare `<...>`/`[...]` slot, a bare dash, `n/a`, or `not applicable`
                    (emphasis and backticks are stripped first)
  placement         an entry under a `## ` section that does not admit its support state, or under none
  not in inventory  an entry for something the build does not produce (historical/aspirational)
  inventory         the inventory itself has an artifact-level issue (unclassified, absent, ...):
                    an open decision is never guessed into the catalogue
  region            the supported-public summary region disagrees (internal work item 1399's check-consumer contract)

Only entries a reader can see count: a heading or field inside a fenced code block or an HTML comment
is ignored, so an entry hidden there is reported missing.

Sections are matched by the start of their `## ` heading:

  ## Deferred ...        deferred-unsupported only
  ## Open decisions ...  unclassified only
  ## Internal ...        internal-implementation only
  any other ## section   supported-public or generated-only (shipped capability)

--catalogue-only gates only the catalogue side: eligible downstream inventory-side comparison problems
(open owner decisions) are still printed, but do not set the exit code. Failure to construct the fresh
inventory is always fatal. That makes catalogue drift gateable while the constructed inventory is red.

Unclassified PACKAGES are the API-documentation work list's concern (internal work item 1400-internal work item 1406), not the artifact
catalogue's. They are printed as a note and do not fail this check.

Standard library only.

@author Navaneeth Mysore G
@author Claude
"""

import argparse
import importlib.util
import os
import re
import sys

CATALOGUE = "docs/Module-and-Service-Catalog.md"
REQUIRED = (
    "Coordinates", "Module", "Support", "Purpose", "Capabilities", "Public entry points",
    "Configuration / backend", "Examples / API docs", "Limitations",
)
PLACEHOLDER_WORD = re.compile(r"\b(?:TODO|TBD|FIXME|XXX)\b", re.I)
PLACEHOLDER_WHOLE = re.compile(r"^(?:\?+|[-\u2013\u2014]+|n/?a|not applicable|…|\.\.\.|<[^>]*>|\[[^\]]*\])$", re.I)
EMPHASIS = re.compile(r"[*_`]+")
FENCE = re.compile(r"^ {0,3}(`{3,}|~{3,})(.*)$")
SECTION = re.compile(r"^##\s+(.*?)\s*$")
SECTION_RULES = (
    ("Deferred", ("deferred-unsupported",)),
    ("Open decisions", ("unclassified",)),
    ("Internal", ("internal-implementation",)),
)
SHIPPED = ("supported-public", "generated-only")
HEADING = re.compile(r"^###\s+`([^`]+)`\s*$")
FIELD = re.compile(r"^-\s+\*\*([^*]+?):\*\*\s*(.*)$")
PACKAGE_ISSUE = re.compile(r"^unclassified package \S+ in supported-public artifact \S+$")


def load_inventory_module():
    here = os.path.dirname(os.path.abspath(__file__))
    spec = importlib.util.spec_from_file_location("api_inventory", os.path.join(here, "api-inventory.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def is_placeholder(value):
    v = EMPHASIS.sub("", value).strip()
    return not v or bool(PLACEHOLDER_WORD.search(v) or PLACEHOLDER_WHOLE.match(v))


def allowed_support(section):
    """The support states a `## ` section admits; None when the entry is under no section."""
    if section is None:
        return None
    for prefix, allowed in SECTION_RULES:
        if section.startswith(prefix):
            return allowed
    return SHIPPED


def visible_lines(text):
    """Yield (line_no, line) for the lines a reader sees: fenced blocks and HTML comments are dropped."""
    fence, in_comment = None, False
    for n, line in enumerate(text.splitlines(), 1):
        if fence:
            close = re.match(r"^ {0,3}(%s{%d,})[ \t]*$" % (re.escape(fence[0]), fence[1]), line)
            if close:
                fence = None
            continue
        out = ""
        while line:
            if in_comment:
                end = line.find("-->")
                if end < 0:
                    line = ""
                else:
                    line, in_comment = line[end + 3:], False
            else:
                start = line.find("<!--")
                if start < 0:
                    out, line = out + line, ""
                else:
                    out, line, in_comment = out + line[:start], line[start + 4:], True
        f = FENCE.match(out)
        if f and not (f.group(1)[0] == "`" and "`" in f.group(2)):
            fence = (f.group(1)[0], len(f.group(1)))
            continue
        yield n, out


def parse_catalogue(text):
    """Return entries with fields and duplicate-required-field occurrence lines in document order."""
    entries = []
    current, section = None, None
    for n, line in visible_lines(text):
        m = HEADING.match(line)
        if m:
            current = (m.group(1).strip(), {}, n, section, [], {})
            entries.append(current)
            continue
        s = SECTION.match(line)
        if s:
            section, current = s.group(1), None
            continue
        if line.startswith("#"):
            current = None
            continue
        if current is None:
            continue
        f = FIELD.match(line.strip())
        if f:
            name = f.group(1).strip()
            if name in REQUIRED and name in current[1]:
                current[4].append((name, current[5][name], n))
            else:
                current[1][name] = f.group(2).strip()
                current[5][name] = n
    return entries


def _unquote(v):
    v = v.strip()
    return v[1:-1] if len(v) >= 2 and v[0] == v[-1] == "`" else v


def expected_coordinates(rec):
    """`groupId:artifactId:version` for Maven, `pub:<name>:<version>` for Dart."""
    return "%s:%s" % (rec["id"], rec["version"])


def compare(root, inv_mod, cat_path):
    """Return (problems, notes, entries_parsed)."""
    problems, notes = [], []
    inv, issues = inv_mod.build(root)
    if inv is None:
        return ["inventory construction: " + i for i in issues], notes, 0

    pkg_issues = [i for i in issues if PACKAGE_ISSUE.match(i)]
    problems += ["inventory: " + i for i in issues if not PACKAGE_ISSUE.match(i)]
    if pkg_issues:
        notes.append("note: %d unclassified package(s) remain; that is the API-docs work list's concern (internal work item 1400-internal work item 1406), "
                     "not the catalogue's" % len(pkg_issues))

    if not os.path.isfile(cat_path):
        problems.append("catalogue %s not found" % os.path.relpath(cat_path, root))
        return problems, notes, 0
    text = open(cat_path, encoding="utf-8", errors="replace").read()

    records = {r["id"]: r for r in inv["artifacts"]}
    seen = {}
    entries = parse_catalogue(text)
    for rid, fields, line, section, duplicate_fields, _field_lines in entries:
        if rid in seen:
            problems.append("duplicate: %s has entries at lines %d and %d" % (rid, seen[rid], line))
            continue
        seen[rid] = line
        for name, first_line, repeated_line in duplicate_fields:
            problems.append("duplicate required field: %s repeats '%s' at lines %d and %d"
                            % (rid, name, first_line, repeated_line))
        rec = records.get(rid)
        if rec is None:
            problems.append("not in the inventory (historical/aspirational, or a typo): %s (line %d)" % (rid, line))
            continue
        for name in REQUIRED:
            v = fields.get(name, "")
            if is_placeholder(v):
                problems.append("incomplete: %s lacks a real '%s' (line %d)" % (rid, name, line))
        checks = (
            ("Coordinates", expected_coordinates(rec)),
            ("Module", rec["module_path"]),
            ("Support", rec["support"]),
        )
        for name, want in checks:
            got = fields.get(name)
            if got is not None and got != "" and _unquote(got) != want:
                problems.append("inconsistent: %s %s is '%s' but the inventory says '%s'" % (rid, name, _unquote(got), want))
        allowed = allowed_support(section)
        if allowed is None:
            problems.append("placement: %s (line %d) is under no '## ' section" % (rid, line))
        elif rec["support"] not in allowed:
            problems.append("placement: %s is %s but sits under '## %s', which admits only %s (line %d)"
                            % (rid, rec["support"], section, "/".join(allowed), line))

    for rid in sorted(set(records) - set(seen)):
        problems.append("missing: %s (%s) has no catalogue entry" % (rid, records[rid]["module_path"]))

    problems += ["region: " + p.split(": ", 1)[1] if ": " in p else "region: " + p
                 for p in inv_mod.check_consumer(cat_path, inv, os.path.relpath(cat_path, root))]
    return problems, notes, len(entries)


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    ap.add_argument("--root", default=os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")))
    ap.add_argument("--catalogue", default=CATALOGUE, help="path relative to --root; must resolve inside it")
    ap.add_argument("--catalogue-only", action="store_true",
                    help="exit non-zero only on catalogue-side problems; inventory-side ones are still printed")
    a = ap.parse_args(argv)
    root = os.path.realpath(a.root)
    cat = os.path.join(root, a.catalogue)
    if os.path.commonpath([root, os.path.realpath(cat)]) != root:
        print("catalogue-check: FAIL catalogue %s resolves outside --root; refused" % a.catalogue)
        return 1
    problems, notes, parsed = compare(root, load_inventory_module(), cat)
    gated = [p for p in problems if not (a.catalogue_only and p.startswith("inventory: "))]
    for p in problems:
        print("catalogue-check: %s %s" % ("FAIL" if p in gated else "inventory-side (not gated by --catalogue-only):", p))
    for n in notes:
        print("catalogue-check: %s" % n)
    print("catalogue-check: %s — %d entries parsed, %d problem(s)%s"
          % ("FAILED" if gated else "passed", parsed, len(gated),
             "" if len(gated) == len(problems) else ", %d inventory-side not gated" % (len(problems) - len(gated))))
    return 1 if gated else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
