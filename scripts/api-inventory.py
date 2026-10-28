#!/usr/bin/env python3
"""api-inventory.py - the shipped-artifact and supported-public API inventory (internal work item 1399, P06-E01 N01).

The inventory is DERIVED, never typed. Its raw artifact list comes only from the build and export
authorities:

  * the Maven reactor - the root pom.xml's <modules>, followed recursively through every aggregator
    (profile <modules> included, flagged with the profile id);
  * the BOM - any reactor module whose artifactId ends in "-bom" and carries <dependencyManagement>;
    its entries are the published-artifact export list;
  * every pubspec.yaml that lives inside a reactor module (the Flutter/Dart packages).

Everything a human decides lives in ONE hand-edited file, docs/api-inventory/classification.json:
each artifact's support state, the package split inside supported-public artifacts, the module-group
-> documentation-issue map and the deliberately excluded trees. The generator joins the two and
REFUSES (exit 1) whenever they disagree:

  * a reactor/pubspec artifact with no valid classification        -> "unclassified"      (AC2)
  * a module listed twice, or two modules with one coordinate      -> "duplicate"         (AC2)
  * a pom.xml/pubspec.yaml on disk that the reactor never reaches  -> "absent"            (AC2)
  * a package of a supported-public artifact matching no rule      -> "unclassified package" (AC3)
  * a classification for an artifact the build does not produce    -> historical/aspirational (AC4)
  * supported-public but not in the BOM; deferred but in the BOM   -> export disagreement (AC4)

Subcommands:
  generate [--out FILE]     write docs/api-inventory/inventory.json and the internal work item 1400-internal work item 1406 work list
  check                     regenerate in memory; fail on drift from the committed files or any issue
  check-consumer FILE...    fail when a consumer doc's inventory region disagrees with the inventory

Standard library only, so it runs on a bare macOS python3. Output is byte-deterministic: no
timestamps, no commit ids, every collection sorted.

@author Navaneeth Mysore G
@author Claude
"""

import argparse
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from xml.parsers import expat

CLASSIFICATION = "docs/api-inventory/classification.json"
INVENTORY = "docs/api-inventory/inventory.json"
WORKLIST = "docs/spec/1.0.0/P06/decisions/E01-published-api-gap-list.md"

# `family` is the capability/service family internal work item 1399 requires every supported-public record to carry (RA-A4 A4-I3).
HANDOFF_FIELDS = ("family", "entry_points", "backend", "docs_destination", "limitations")
SUPPORT_STATES = ("supported-public", "internal-implementation", "deferred-unsupported", "generated-only")
PACKAGE_STATES = ("supported-public", "implementation-detail")
SKIP_DIRS = {"target", "build", "node_modules", ".dart_tool", "__pycache__"}
REGION_BEGIN = "<!-- api-inventory:supported-public:begin -->"
REGION_END = "<!-- api-inventory:supported-public:end -->"

# Estimate heuristic for the work list (lines of added Javadoc). Stated in the output, not hidden.
LINES_PER_TYPE = 6
LINES_PER_MEMBER = 4

# Plan N01 Step 1's raw-count command, verbatim. RAW_TOPS/RAW_PUBLIC reproduce it (find + grep -rl) so the work
# list can carry the number with the command that re-derives it; EXTRA_raw_count_matches_command pins the two.
RAW_COUNT_COMMAND = ("grep -rlE '^\\s*public\\s+(final\\s+|abstract\\s+|sealed\\s+)?(interface|class|enum|record)\\s' \\\n"
                     "  --include='*.java' $(find appbase-java appbase-cloud -path '*/src/main/java' -type d) | wc -l")
RAW_TOPS = ("appbase-java", "appbase-cloud")
_WS = rb"[ \t\v\f\r]"  # grep's \s in the C/UTF-8 locales: the newline is never part of a grep line
RAW_PUBLIC = re.compile(rb"^" + _WS + rb"*public" + _WS + rb"+(?:(?:final|abstract|sealed)" + _WS + rb"+)?"
                        rb"(?:interface|class|enum|record)" + _WS)


# ── Maven ──────────────────────────────────────────────────────────────────────────────────────────

def _local(tag):
    return tag.rsplit("}", 1)[-1]


def _child(el, name):
    if el is None:
        return None
    for c in el:
        if _local(c.tag) == name:
            return c
    return None


def _children(el, name):
    return [c for c in (el if el is not None else []) if _local(c.tag) == name]


def _text(el, name):
    c = _child(el, name)
    return c.text.strip() if c is not None and c.text else None


class _DoctypeRefused(Exception):
    """Raised from inside expat the moment it reports a DOCTYPE declaration."""


def _refuse_dtd(*_args):
    raise _DoctypeRefused()


def _parse_pom(path):
    """Parse one pom, refusing any DOCTYPE or entity declaration; malformed XML is a clean InputError.

    The byte regex is only a fast path: expat decodes UTF-16 (and other declared encodings) itself, so a
    DOCTYPE the regex cannot see still reaches the parser (R-2506-8). The authoritative refusal is therefore
    the expat handler, which fires on the declaration itself, before any entity it declares can be referenced.

    One handler is enough (R-2506-11): expat reports StartDoctypeDecl before anything in an internal subset, and
    an <!ENTITY outside a DOCTYPE is not well-formed, so an EntityDeclHandler could never fire first. It was
    removed rather than kept as an unpinnable second guard; EXTRA_pom_doctype_no_subset_utf16 pins this one.
    """
    refusal = "refusing %s: it contains a <!DOCTYPE or entity declaration, which is never processed" % path
    with open(path, "rb") as f:
        data = f.read()
    if re.search(rb"<!DOCTYPE", data, re.I):
        raise InputError(refusal)
    # ET's C XMLParser does not expose its expat object, so drive expat directly into a TreeBuilder. Tags are
    # spelled `{ns}local` as ElementTree spells them; comments and processing instructions are dropped, as
    # ET.fromstring drops them.
    builder = ET.TreeBuilder()
    parser = expat.ParserCreate(namespace_separator="}")
    parser.StartDoctypeDeclHandler = _refuse_dtd
    tag = lambda n: "{" + n if "}" in n else n
    parser.StartElementHandler = lambda n, attrs: builder.start(tag(n), {tag(k): v for k, v in attrs.items()})
    parser.EndElementHandler = lambda n: builder.end(tag(n))
    parser.CharacterDataHandler = builder.data
    try:
        parser.Parse(data, True)
        return builder.close()
    except _DoctypeRefused:
        raise InputError(refusal)
    except expat.ExpatError as e:
        raise InputError("malformed pom %s: %s" % (path, e))


def read_pom(path):
    root = _parse_pom(path)
    parent = _child(root, "parent")
    group = _text(root, "groupId") or _text(parent, "groupId")
    version = _text(root, "version") or _text(parent, "version")
    modules = [(m.text.strip(), None) for m in _children(_child(root, "modules"), "module") if m.text]
    for prof in _children(_child(root, "profiles"), "profile"):
        pid = _text(prof, "id")
        modules += [(m.text.strip(), pid) for m in _children(_child(prof, "modules"), "module") if m.text]
    bom = []
    dm = _child(root, "dependencyManagement")
    for dep in _children(_child(dm, "dependencies"), "dependency"):
        bom.append((_text(dep, "groupId"), _text(dep, "artifactId")))
    return {
        "group_id": group,
        "artifact_id": _text(root, "artifactId"),
        "version": version,
        "packaging": _text(root, "packaging") or "jar",
        "modules": modules,
        "bom": bom if dm is not None else None,
    }


def inside(real_root, path):
    """True when PATH, after resolving every symlink, is the root or lies under it."""
    real = os.path.realpath(path)
    return real == real_root or real.startswith(real_root + os.sep)


def display_rel(path, real_root):
    """PATH relative to the resolved root for messages. Only the parent is resolved, so the link itself keeps
    its name and a root under a symlinked temp dir (macOS /var -> /private/var) still prints repo-relative."""
    parent, name = os.path.split(os.path.abspath(path))
    return os.path.relpath(os.path.join(os.path.realpath(parent), name), real_root)


class ClassificationError(Exception):
    pass


class InputError(ClassificationError):
    """A build or decisions input the generator cannot use (malformed pom, wrong JSON shape): a clean ERROR, rc=1."""


def repo_path(root, rel):
    """ROOT/REL for a repository-controlled default input or output, refusing any path that leaves the repo.

    Every existing component from ROOT down (the file itself and each parent directory) must be a real
    directory or file, never a symlink, so a read cannot pull outside data in and a write cannot truncate
    an outside file through a planted link (RA-A4 A4-I1). Components that do not exist yet are fine:
    they are created as real directories. An explicit, user-chosen `--out` is a separate contract.
    """
    real_root = os.path.realpath(root)
    cur = real_root
    for seg in rel.split("/"):
        if seg in ("", ".", ".."):
            raise ClassificationError("refusing %s: not a plain repository-relative path" % rel)
        cur = os.path.join(cur, seg)
        if os.path.islink(cur):
            raise ClassificationError("refusing %s: %s is a symlink, which could lead outside the repository"
                                      % (rel, os.path.relpath(cur, real_root)))
        if not os.path.exists(cur):
            break
    return os.path.join(real_root, rel)


def _no_duplicate_keys(pairs):
    out = {}
    for k, v in pairs:
        if k in out:
            raise ClassificationError("duplicate key %r in %s — the last entry would silently win" % (k, CLASSIFICATION))
        out[k] = v
    return out


def load_classification(root):
    """Parse the decisions file, refusing duplicate keys and reporting malformed JSON cleanly."""
    path = repo_path(root, CLASSIFICATION)
    if not os.path.isfile(path):
        raise ClassificationError("missing classification file %s" % CLASSIFICATION)
    try:
        with open(path, encoding="utf-8") as f:
            cls = json.load(f, object_pairs_hook=_no_duplicate_keys)
    except ValueError as e:
        raise ClassificationError("%s is not valid JSON: %s" % (CLASSIFICATION, e))
    except RecursionError:
        raise InputError("%s is nested too deeply to parse" % CLASSIFICATION)
    validate_shape(cls)
    return cls


def validate_shape(cls):
    """Refuse a decisions file whose SHAPE the generator would otherwise trip over with a traceback (R-2506-3)."""
    def need(ok, what):
        if not ok:
            raise InputError("%s: %s" % (CLASSIFICATION, what))
    need(isinstance(cls, dict), "the top level must be an object, not %s" % type(cls).__name__)
    arts = cls.get("artifacts", {})
    need(isinstance(arts, dict), "'artifacts' must be an object")
    for k, v in arts.items():
        need(isinstance(v, dict), "artifact %s must be an object, not %s" % (k, type(v).__name__))
        # Optional fields are a string or absent/null; a missing value is reported as an ISSUE downstream, but a
        # value of the wrong TYPE is a malformed decisions file (R-2506-11 follow-up).
        for f in ("support", "evidence") + tuple(h for h in HANDOFF_FIELDS if h != "entry_points"):
            fv = v.get(f)
            need(fv is None or isinstance(fv, str),
                 "artifact %s '%s' must be a string, not %s" % (k, f, type(fv).__name__))
        ep = v.get("entry_points")
        need(ep is None or (isinstance(ep, list) and all(isinstance(e, str) for e in ep)),
             "artifact %s 'entry_points' must be a list of strings (%r)" % (k, ep))
    base = cls.get("baseline", {})
    need(isinstance(base, dict), "'baseline' must be an object, not %s" % type(base).__name__)
    need(isinstance(base.get("deferred", {}), dict), "'baseline.deferred' must be an object")
    pkgs = cls.get("packages", [])
    need(isinstance(pkgs, list), "'packages' must be a list")
    for i, r in enumerate(pkgs):
        # a non-object rule or a bad prefix/classification stays an ISSUE (validate_package_rules); a reason
        # of the wrong type would be copied verbatim into the inventory, so it is refused here.
        rr = r.get("reason") if isinstance(r, dict) else None
        need(rr is None or isinstance(rr, str),
             "package rule #%d 'reason' must be a string, not %s" % (i, type(rr).__name__))
    need(isinstance(cls.get("excluded_trees", {}), dict), "'excluded_trees' must be an object")
    groups = cls.get("module_groups", [])
    need(isinstance(groups, list), "'module_groups' must be a list")
    for i, g in enumerate(groups):
        need(isinstance(g, dict) and isinstance(g.get("prefix"), str) and g["prefix"].strip()
             and isinstance(g.get("group"), str) and g["group"].strip(),
             "module group #%d needs a non-empty 'prefix' and 'group' (%r)" % (i, g))


def walk_reactor(root, issues):
    """Follow <modules> from the root pom. Returns {module_path: pom-info} ('' is the root)."""
    found = {}
    listed = {}

    real_root = os.path.realpath(root)

    def visit(rel, profile):
        pom = os.path.join(root, rel, "pom.xml") if rel else os.path.join(root, "pom.xml")
        # pom.xml is PR-controlled content and the inventory is written back into the repository, so a
        # <module> escaping the root (../, absolute, or through a symlink) is refused and never parsed.
        real = os.path.realpath(os.path.join(root, rel))
        if rel and (os.path.isabs(rel) or rel.split("/")[0] == ".." or not real.startswith(real_root + os.sep)):
            issues.append("module '%s' resolves outside the repository; refused" % rel)
            return
        if os.path.lexists(pom) and not inside(real_root, pom):
            issues.append("module '%s' pom.xml resolves outside the repository; refused" % (rel or "."))
            return
        if not os.path.isfile(pom):
            issues.append("absent: reactor lists module '%s' but %s does not exist" % (rel, os.path.relpath(pom, root)))
            return
        info = read_pom(pom)
        info["profile"] = profile
        found[rel] = info
        for mod, prof in info["modules"]:
            child = os.path.normpath(os.path.join(rel, mod)).replace(os.sep, "/") if rel else os.path.normpath(mod).replace(os.sep, "/")
            if child in listed:
                issues.append("duplicate module entry: '%s' is listed by both '%s' and '%s'" % (child, listed[child] or ".", rel or "."))
                continue
            listed[child] = rel
            visit(child, prof or profile)

    visit("", None)
    return found


# ── Dart ───────────────────────────────────────────────────────────────────────────────────────────

def read_pubspec(path):
    out = {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            m = re.match(r"^(name|version|publish_to):\s*(\S+)", line)
            if m:
                out[m.group(1)] = m.group(2).strip("'\"")
    return out


DART_DECL = re.compile(r"^(?:(?:abstract|sealed|final|base|interface|mixin)\s+)*(class|enum|mixin|extension type|extension|typedef)\s+([A-Za-z_$][\w$]*)")
DART_FUNCTION = re.compile(r"^(?:(?:external|static)\s+)*(?:[\w$?.<>\[\],]+\s+)+([A-Za-z_$][\w$]*)\s*(?:<[^;{=]+>)?\s*\(")
DART_VARIABLE = re.compile(r"^(?:(?:external|static|late)\s+)*(?:final|const|var)\s+(?:(?:[\w$?.<>\[\],]+)\s+)?([A-Za-z_$][\w$]*)\s*(?:[=;])")


def scan_dart(lib_dir, real_root, issues):
    """Count authored public Dart declarations, including top-level functions/variables and members.

    Generated files are deliberately excluded: their public API is derived from the documented source models,
    and editing generated comments would be overwritten by the next code-generation run. Overrides inherit the
    contract they implement, matching Dart's public_member_api_docs lint.
    """
    public = undocumented = 0
    if os.path.isdir(lib_dir) and not inside(real_root, lib_dir):
        issues.append("Dart lib %s resolves outside the repository; refused" % display_rel(lib_dir, real_root))
        return {"public_declarations": 0, "undocumented_declarations": 0}
    for path in sorted_files(lib_dir, ".dart"):
        if path.endswith((".g.dart", ".freezed.dart")):
            continue
        if not inside(real_root, path):
            issues.append("Dart file %s resolves outside the repository; refused" % display_rel(path, real_root))
            continue
        doc = override = False
        depth = 0
        public_type = None
        type_depth = None
        state = {"block": False, "javadoc": False}
        with open(path, encoding="utf-8", errors="replace") as f:
            for raw in f:
                stripped = raw.strip()
                if stripped.startswith("///"):
                    doc = True
                    continue
                code, state, closed = _code(raw, state)
                if closed:
                    doc = True
                s = code.strip()
                if not s:
                    continue
                if s.startswith("@"):
                    override = override or s.startswith("@override")
                    continue

                declaration = None
                m = DART_DECL.match(s)
                top_level = code == code.lstrip()
                if depth == 0 and top_level and m and not m.group(2).startswith("_") and m.group(2) != "on":
                    declaration = m.group(2)
                    if m.group(1) != "typedef":
                        public_type = declaration
                        type_depth = depth + 1
                elif depth == 0 and top_level:
                    m = DART_FUNCTION.match(s) or DART_VARIABLE.match(s)
                    if m and not m.group(1).startswith("_"):
                        declaration = m.group(1)
                elif public_type is not None and depth == type_depth:
                    constructor = re.match(r"^(?:(?:const|factory)\s+)?%s(?:\.[A-Za-z_$][\w$]*)?\s*\(" % re.escape(public_type), s)
                    getter = re.match(r"^(?:(?:external|static|late|final|const)\s+)*(?:[\w$?.<>\[\],]+\s+)?(?:get|set)\s+([A-Za-z_$][\w$]*)\b", s)
                    m = constructor or getter or DART_FUNCTION.match(s) or DART_VARIABLE.match(s)
                    if m:
                        name = public_type if constructor else m.group(1)
                        if not name.startswith("_") and not override:
                            declaration = name

                if declaration is not None:
                    public += 1
                    if not doc:
                        undocumented += 1
                if declaration is not None or not s.startswith(("//", "/*")):
                    doc = override = False

                depth += code.count("{") - code.count("}")
                if public_type is not None and depth < type_depth:
                    public_type = None
                    type_depth = None
    return {"public_declarations": public, "undocumented_declarations": undocumented}


# ── Java ───────────────────────────────────────────────────────────────────────────────────────────

ANNOTATION = re.compile(r"^@([\w.]+)(?:\s*\([^()]*(?:\([^()]*\)[^()]*)*\))?")
TYPE_DECL = re.compile(r"^public\s+(?:(?:abstract|final|sealed|non-sealed|static|strictfp)\s+)*(class|interface|enum|record|@interface)\s+(\w+)")
PUBLIC_MEMBER = re.compile(r"^public\s+[^=;]*?\b\w+\s*\(")
IFACE_MEMBER = re.compile(r"^(?:(?:default|static|abstract|public)\s+)*(?:<[^>]*>\s*)?[\w.$]+(?:<.*>)?(?:\[\])*\s+\w+\s*\(")


def _code(line, state):
    """Strip comments and string/char literals; returns (code, state, closed_javadoc)."""
    out = []
    i, n = 0, len(line)
    closed_doc = False
    while i < n:
        if state["block"]:
            j = line.find("*/", i)
            if j < 0:
                return "".join(out), state, closed_doc
            if state["javadoc"]:
                closed_doc = True
            state["block"] = state["javadoc"] = False
            i = j + 2
            continue
        c = line[i]
        if line.startswith("//", i):
            break
        if line.startswith("/*", i):
            state["block"] = True
            state["javadoc"] = line.startswith("/**", i) and not line.startswith("/**/", i)
            i += 3 if state["javadoc"] else 2
            continue
        if c in "\"'":
            j = i + 1
            while j < n and line[j] != c:
                j += 2 if line[j] == "\\" else 1
            out.append(c + c)
            i = j + 1
            continue
        out.append(c)
        i += 1
    return "".join(out), state, closed_doc


def _after_balanced_parens(text):
    """For text starting at an annotation's '(' return (open_count, rest).

    open_count > 0 means the arguments continue on later lines. Otherwise rest is the text after the
    parenthesis that closed the arguments. String and char literals were already blanked by _code."""
    balance = 0
    for i, c in enumerate(text):
        if c == "(":
            balance += 1
        elif c == ")":
            balance -= 1
            if balance == 0:
                return 0, text[i + 1:].lstrip()
    return balance, ""


def scan_java_file(path):
    """Top-level public types in one file and, per type, whether it and its public members carry Javadoc.

    Scope accounting (documented exclusions, internal work item 1402 N5; no owner ruling accepts them, see the T1402 ledger): only the callable members declared directly in a
    top-level public type are counted. Nested types' members, fields and constants are not counted, so a
    clean result does not prove them documented; they are reviewed by hand, not by this scanner."""
    pkg = None
    types = []
    depth = 0
    doc = override = annotations = False
    ann_paren = 0  # parentheses still open in a multi-line annotation's arguments
    current = None
    state = {"block": False, "javadoc": False}
    with open(path, encoding="utf-8", errors="replace") as f:
        for raw in f:
            code, state, closed = _code(raw, state)
            s = code.strip()
            if closed:
                # Javadoc must precede the complete declaration, including its annotations.
                # A block inserted between an annotation and the declaration is ignored by javadoc.
                doc = not annotations
            if not s:
                continue
            if ann_paren > 0:
                # A multi-line annotation's argument lines are part of the declaration prefix and never
                # change the type depth, whatever braces or nested annotations they hold.
                ann_paren, rest = _after_balanced_parens("(" * ann_paren + s)
                if ann_paren > 0 or not rest:
                    continue
                s = rest
            # Leading annotations: a line holding ONLY annotations keeps the pending Javadoc; one that
            # continues into a declaration (`@Nullable public Foo bar() {`) is scanned as that declaration.
            # An @Override member inherits its Javadoc from the supertype, so it is not a gap.
            while s.startswith("@") and not s.startswith("@interface"):
                m = ANNOTATION.match(s)
                if m is None:
                    break
                override = override or m.group(1) in ("Override", "java.lang.Override")
                annotations = True
                s = s[m.end():].lstrip()
                if s.startswith("("):
                    # arguments the one-line ANNOTATION pattern could not consume: multi-line or deeper nesting
                    ann_paren, s = _after_balanced_parens(s)
                    if ann_paren > 0:
                        break
            if ann_paren > 0:
                continue
            if annotations and code.strip().startswith("@") and not s:
                continue
            if not s:
                continue
            if pkg is None:
                m = re.match(r"^package\s+([\w.]+)\s*;", s)
                if m:
                    pkg = m.group(1)
            if depth == 0:
                m = TYPE_DECL.match(s)
                if m:
                    current = {"name": m.group(2), "kind": m.group(1), "documented": doc, "members": 0, "undocumented_members": 0}
                    types.append(current)
            elif depth == 1 and current is not None and not s.startswith(("private ", "}", "{")):
                iface = current["kind"] in ("interface", "@interface")
                if PUBLIC_MEMBER.match(s) or (iface and IFACE_MEMBER.match(s) and not s.startswith(("class ", "interface ", "enum ", "record "))):
                    current["members"] += 1
                    if not doc and not override:
                        current["undocumented_members"] += 1
            doc = override = annotations = False
            depth += s.count("{") - s.count("}")
            if depth <= 0:
                depth = 0
    return pkg, types


def sorted_files(top, suffix):
    out = []
    for d, dirs, files in os.walk(top):
        dirs[:] = sorted(x for x in dirs if x not in SKIP_DIRS and not x.startswith("."))
        out += [os.path.join(d, f) for f in files if f.endswith(suffix)]
    return sorted(out)


def scan_java(module_dir, real_root, issues):
    src = os.path.join(module_dir, "src", "main", "java")
    pkgs = {}
    if not os.path.isdir(src):
        return pkgs
    if not inside(real_root, src):
        issues.append("source root %s resolves outside the repository; refused" % display_rel(src, real_root))
        return pkgs
    for path in sorted_files(src, ".java"):
        if not inside(real_root, path):
            issues.append("source file %s resolves outside the repository; refused" % display_rel(path, real_root))
            continue
        pkg, types = scan_java_file(path)
        if pkg is None or not types:
            continue
        pkgs.setdefault(pkg, []).extend(types)
    return pkgs


# ── Classification join ────────────────────────────────────────────────────────────────────────────

def classify_package(pkg, rules):
    best = None
    for r in rules:
        p = r.get("prefix", "")
        if (pkg == p or pkg.startswith(p + ".")) and (best is None or len(p) > len(best["prefix"])):
            best = r
    if best is None:
        return "unclassified", None
    if best.get("classification") not in PACKAGE_STATES:
        # A matching explicit rule with an invalid state is NOT "no rule": it must never fall through to the
        # public default (RA-A4 A4-I2). The rule itself is reported by validate_package_rules.
        return "invalid", best
    return best["classification"], best.get("reason")


def validate_package_rules(rules):
    """Issues for every malformed explicit package rule, whether or not any package matches it."""
    out = []
    for i, r in enumerate(rules):
        pfx = r.get("prefix") if isinstance(r, dict) else None
        if not isinstance(pfx, str) or not pfx.strip():
            out.append("invalid package rule #%d: missing or empty prefix (%r)" % (i, r))
        elif r.get("classification") not in PACKAGE_STATES:
            out.append("invalid package rule %s: classification %r is not one of %s"
                       % (pfx, r.get("classification"), ", ".join(PACKAGE_STATES)))
    return out


# ── The package rule — owner-approved rule-based default recorded on 2026-09-27
# The owner chose option (b), the rule-based default, as the FINAL public-API package split. It applies
# only to packages of supported-public artifacts that NO explicit rule in classification.json covers, and
# every record it produces is marked basis=rule. Its default is supported-public by the ruling, so a new
# package in a supported-public artifact becomes public unless it carries an implementation segment or an
# explicit rule says otherwise. A package the rule still leaves unclassified is reported as an issue.
PACKAGE_RULE = {
    "note": "owner-approved rule-based default recorded on 2026-09-27",
    # a package with any of these as a whole SEGMENT (`.internal.` / `.impl.` / `.support.`, or at the end)
    "implementation_segments": ("internal", "impl", "support"),
    # a class matching one of these is implementation-detail even inside a supported-public package
    "implementation_type_patterns": (r"^Default\w*Impl$", r"^\w*Helper$"),
    # everything else in a supported-public artifact
    "default": "supported-public",
}


def rule_package(pkg):
    rule = PACKAGE_RULE
    hit = [seg for seg in pkg.split(".") if seg in rule["implementation_segments"]]
    if hit:
        return "implementation-detail", "package rule: package segment '%s'" % hit[0]
    return rule["default"], "package rule: default for a supported-public artifact"


def rule_type_is_implementation(name):
    return any(re.match(p, name) for p in PACKAGE_RULE["implementation_type_patterns"])


def module_group(path, groups):
    best = None
    for g in groups:
        p = g["prefix"].rstrip("/")
        if (path == p or path.startswith(p + "/")) and (best is None or len(p) > len(best["prefix"].rstrip("/"))):
            best = g
    return (best["group"], best.get("issue")) if best else ("unassigned", None)


def disk_manifests(root, excluded):
    out = []
    for d, dirs, files in os.walk(root):
        rel = os.path.relpath(d, root).replace(os.sep, "/")
        rel = "" if rel == "." else rel
        dirs[:] = sorted(x for x in dirs if x not in SKIP_DIRS and not x.startswith(".")
                         and (rel + "/" + x if rel else x) not in excluded)
        for f in ("pom.xml", "pubspec.yaml"):
            if f in files:
                out.append((rel, f))
    return sorted(out)


def build(root):
    issues = []
    real_root = os.path.realpath(root)
    try:
        cls = load_classification(root)
    except ClassificationError as e:
        return None, [str(e)]
    decisions = cls.get("artifacts", {})
    rules = cls.get("packages", [])
    issues.extend(validate_package_rules(rules))
    rules = [r for r in rules if isinstance(r, dict) and isinstance(r.get("prefix"), str) and r["prefix"].strip()]
    deferred_by_baseline = cls.get("baseline", {}).get("deferred", {})
    prefixes = {}
    for rule in rules:
        pfx = rule.get("prefix", "")
        if pfx in prefixes:
            issues.append("duplicate package prefix %s in %s" % (pfx, CLASSIFICATION))
        prefixes[pfx] = rule
    groups = cls.get("module_groups", [])
    excluded = set(p.rstrip("/") for p in cls.get("excluded_trees", {}))

    reactor = walk_reactor(root, issues)

    exported = set()
    for info in reactor.values():
        if info["bom"] is not None and (info["artifact_id"] or "").endswith("-bom"):
            for g, a in info["bom"]:
                g = info["group_id"] if g in (None, "${project.groupId}") else g
                exported.add("%s:%s" % (g, a))

    records = []
    seen = {}
    for rel in sorted(reactor):
        info = reactor[rel]
        rid = "%s:%s" % (info["group_id"], info["artifact_id"])
        if rid in seen:
            issues.append("duplicate coordinates %s at '%s' and '%s'" % (rid, seen[rid] or ".", rel or "."))
            continue
        seen[rid] = rel
        records.append({
            "id": rid, "type": "maven-" + info["packaging"], "module_path": rel or ".",
            "group_id": info["group_id"], "artifact_id": info["artifact_id"], "version": info["version"],
            "reactor_profile": info["profile"], "exported_by_bom": rid in exported,
            "owning_module": rel or ".", "_dir": os.path.join(root, rel),
        })

    reactor_dirs = set(reactor)
    for rel, name in disk_manifests(root, excluded):
        if name == "pom.xml":
            if rel not in reactor_dirs:
                issues.append("absent: %s is on disk but not reached by the reactor (wire it in or list it in excluded_trees)" % ((rel + "/" if rel else "") + name))
            continue
        owner = next((d for d in sorted(reactor_dirs, key=len, reverse=True) if d == "" or rel == d or rel.startswith(d + "/")), None)
        if owner is None or owner == "":
            issues.append("absent: %s/pubspec.yaml is not inside any reactor module" % rel)
            continue
        if not inside(real_root, os.path.join(root, rel, name)):
            issues.append("%s/pubspec.yaml resolves outside the repository; refused" % rel)
            continue
        spec = read_pubspec(os.path.join(root, rel, name))
        rid = "pub:%s" % spec.get("name")
        if rid in seen:
            issues.append("duplicate coordinates %s at '%s' and '%s'" % (rid, seen[rid], rel))
            continue
        seen[rid] = rel
        rec = {
            "id": rid, "type": "dart-package", "module_path": rel, "owning_module": owner,
            "group_id": None, "artifact_id": spec.get("name"), "version": spec.get("version"),
            "publish_to": spec.get("publish_to"), "reactor_profile": None, "exported_by_bom": False,
        }
        rec["dart_surface"] = scan_dart(os.path.join(root, rel, "lib"), real_root, issues)
        records.append(rec)

    produced = set(r["id"] for r in records)
    for rid in sorted(decisions):
        if rid not in produced:
            issues.append("classified but not produced by the build (historical/aspirational): %s" % rid)

    for r in records:
        d = decisions.get(r["id"], {})
        support = d.get("support")
        if support not in SUPPORT_STATES:
            issues.append("unclassified artifact: %s (%s)%s" % (r["id"], r["module_path"],
                          "; note: " + d["evidence"] if d.get("evidence") else ""))
            support = "unclassified"
        r["support"] = support
        for k in ("evidence", "family", "entry_points", "backend", "docs_destination", "limitations"):
            r[k] = d.get(k)
        r["module_group"], r["doc_issue"] = module_group(r["module_path"], groups)
        if support == "supported-public" and r["type"].startswith("maven-") and not r["exported_by_bom"] \
                and not r["artifact_id"].endswith("-bom"):
            issues.append("supported-public but not exported by the BOM: %s" % r["id"])
        if support == "deferred-unsupported" and r["exported_by_bom"]:
            issues.append("deferred-unsupported but exported by the BOM (it would ship): %s" % r["id"])
        if r["id"] in deferred_by_baseline and support not in ("deferred-unsupported", "unclassified"):
            issues.append("the baseline defers %s (%s) but it is classified %s" % (r["id"], deferred_by_baseline[r["id"]], support))
        if support == "supported-public":
            # BOM membership is not evidence of support: the claim must cite the release baseline, a test, or a
            # recorded owner ruling that a reader can open (`owner ruling: #<issue> comment <id>`).
            if not re.search(r"§\d|\btest:|\bowner ruling: #\d+ comment \d+", r["evidence"] or ""):
                issues.append("supported-public evidence must cite a baseline section (§…), a test (test: …) or an owner "
                              "ruling (owner ruling: #<issue> comment <id>): %s" % r["id"])
            missing = [k for k in HANDOFF_FIELDS if not r.get(k) or (isinstance(r[k], str) and not r[k].strip())]
            if missing:
                issues.append("missing handoff field(s) for supported-public %s: %s" % (r["id"], ", ".join(missing)))

        if "_dir" in r:
            pkgs = []
            for pkg, types in sorted(scan_java(r.pop("_dir"), real_root, issues).items()):
                impl_types = []
                if support == "supported-public":
                    pc, reason = classify_package(pkg, rules)
                    basis = "explicit"
                    if pc == "invalid":
                        pc, reason = "unclassified", "invalid explicit rule %s" % reason.get("prefix")
                    elif pc == "unclassified":
                        pc, reason = rule_package(pkg)
                        basis = "rule"
                    if pc == "unclassified":
                        issues.append("unclassified package %s in supported-public artifact %s" % (pkg, r["id"]))
                    if pc == "supported-public":
                        impl_types = [t for t in types if rule_type_is_implementation(t["name"])]
                else:
                    pc, reason, basis = "implementation-detail", "artifact is %s" % support, "artifact"
                api = [t for t in types if t not in impl_types]
                entry = {
                    "package": pkg, "classification": pc, "reason": reason, "basis": basis,
                    "public_type_count": len(api),
                    "undocumented_types": sum(1 for t in api if not t["documented"]),
                    "public_members": sum(t["members"] for t in api),
                    "undocumented_members": sum(t["undocumented_members"] for t in api),
                }
                if pc == "supported-public":
                    entry["public_types"] = sorted(t["name"] for t in api)
                    if impl_types:
                        entry["implementation_detail_types"] = sorted(t["name"] for t in impl_types)
                pkgs.append(entry)
            r["packages"] = pkgs

    records.sort(key=lambda x: x["id"])
    summary = {}
    for r in records:
        summary[r["support"]] = summary.get(r["support"], 0) + 1
    inv = {
        "schema": 1,
        "generator": "scripts/api-inventory.py",
        "classification": CLASSIFICATION,
        "summary": summary,
        "package_classification": {
            "package_rule": PACKAGE_RULE["note"],
            "explicit": sum(1 for r in records for p in r.get("packages", []) if p["basis"] == "explicit"),
            "rule": sum(1 for r in records for p in r.get("packages", []) if p["basis"] == "rule"),
        },
        "issues": sorted(set(issues)),
        "artifacts": records,
    }
    return inv, sorted(set(issues))


def render_inventory(inv):
    """Pretty JSON, except that each package entry is kept to ONE line so a diff reads per package."""
    def compact(v):
        return json.dumps(v, sort_keys=True, ensure_ascii=False)

    out = ["{"]
    top = [k for k in sorted(inv) if k != "artifacts"]
    for k in top:
        out.append('  %s: %s,' % (compact(k), json.dumps(inv[k], indent=2, sort_keys=True, ensure_ascii=False).replace("\n", "\n  ")))
    out.append('  "artifacts": [')
    for i, r in enumerate(inv["artifacts"]):
        fields = []
        for k in sorted(r):
            if k == "packages":
                pk = ["      %s" % compact(p) for p in r["packages"]]
                fields.append('    "packages": [' + ("\n" + ",\n".join(pk) + "\n    ]" if pk else "]"))
            else:
                fields.append("    %s: %s" % (compact(k), compact(r[k])))
        out.append("   {\n" + ",\n".join(fields) + "\n   }" + ("," if i < len(inv["artifacts"]) - 1 else ""))
    out.append("  ]")
    out.append("}")
    return "\n".join(out) + "\n"


# ── Static measurements of the tree for the work list (Plan N01 Step 1, file map "T3 doclint baseline") ──

def raw_public_count(root, groups):
    """Plan N01 Step 1's count, reproduced: `find TOPS -path '*/src/main/java' -type d`, then `grep -rlE … | wc -l`.

    find neither follows symlinks nor prunes anything; grep -r does not follow a symlink it meets while recursing,
    and a file under two matched roots is listed (and counted) twice. Returns (total, {group: n}, absent tops).
    """
    total, per_group, absent = 0, {}, []
    for top in RAW_TOPS:
        path = os.path.join(root, top)
        if os.path.islink(path) or not os.path.isdir(path):
            absent.append(top)
            continue
        roots = []
        for d, dirs, _ in os.walk(path):
            dirs.sort()
            rel = os.path.relpath(d, root).replace(os.sep, "/")
            if rel.endswith("/src/main/java"):
                roots.append((d, rel))
        for d, rel in roots:
            n = 0
            for sd, sdirs, files in os.walk(d):
                sdirs[:] = [x for x in sdirs if not os.path.islink(os.path.join(sd, x))]
                for f in files:
                    fp = os.path.join(sd, f)
                    if not f.endswith(".java") or os.path.islink(fp):
                        continue
                    with open(fp, "rb") as fh:
                        if any(RAW_PUBLIC.match(line.rstrip(b"\n")) for line in fh):
                            n += 1
            total += n
            g = module_group(rel[: -len("/src/main/java")], groups)[0]
            per_group[g] = per_group.get(g, 0) + n
    return total, per_group, absent


JAVADOC_KEYS = ("doclint", "failOnError", "validateLinks", "show", "author", "quiet", "additionalOptions",
                "additionalJOption", "additionalparam")


def javadoc_config(root):
    """Every maven-javadoc-plugin block in the root pom (default build and each profile), read statically.

    `${prop}` values resolve against the root <properties> overlaid with the profile's own. Nothing is run, so
    this is the CONFIGURED doclint level, never the effective result of a javadoc run.
    """
    pom = os.path.join(root, "pom.xml")
    if not os.path.isfile(pom) or not inside(os.path.realpath(root), pom):
        return []
    top = _parse_pom(pom)

    def props(el):
        p = _child(el, "properties")
        return {_local(c.tag): (c.text or "").strip() for c in (p if p is not None else [])}

    base = props(top)
    scopes = [("(default build)", top, base)]
    for prof in _children(_child(top, "profiles"), "profile"):
        scopes.append((_text(prof, "id") or "?", prof, dict(base, **props(prof))))
    out = []
    for scope, el, pr in scopes:
        def res(v, pr=pr):
            return re.sub(r"\$\{([^}]+)\}", lambda m: pr.get(m.group(1), m.group(0)), v) if v is not None else None
        build_el = _child(el, "build")
        for section, plugins in (("plugins", _child(build_el, "plugins")),
                                 ("pluginManagement", _child(_child(build_el, "pluginManagement"), "plugins")),
                                 ("reporting", _child(_child(el, "reporting"), "plugins"))):
            for pl in _children(plugins, "plugin"):
                if _text(pl, "artifactId") != "maven-javadoc-plugin":
                    continue
                conf = _child(pl, "configuration")
                goals = sorted(set(g.text.strip() for ex in _children(_child(pl, "executions"), "execution")
                                   for g in _children(_child(ex, "goals"), "goal") if g.text))
                goals += sorted(set(r.text.strip() for rs in _children(_child(pl, "reportSets"), "reportSet")
                                    for r in _children(_child(rs, "reports"), "report") if r.text))
                entry = {"scope": scope, "section": section, "version": res(_text(pl, "version")), "goals": goals}
                for k in JAVADOC_KEYS:
                    entry[k] = res(_text(conf, k))
                out.append(entry)
    return out


def render_worklist(inv, root):
    groups = load_classification(root).get("module_groups", [])
    L = []
    L.append("# Measured supported-public API work list")
    L.append("")
    L.append("> **GENERATED by `scripts/api-inventory.py generate` — do not edit by hand.** It is a view of")
    L.append("> [`docs/api-inventory/inventory.json`](../../../../api-inventory/inventory.json), plus two static readings of")
    L.append("> the tree (the raw count and the javadoc configuration below); `check` fails if it drifts.")
    L.append("")
    raw, raw_groups, raw_absent = raw_public_count(root, groups)
    L.append("## Raw public surface")
    L.append("")
    L.append("**%d** `.java` files under a `src/main/java` root declare a top-level-looking `public` type. Reproduce it"
             " from the repository root with this command:" % raw)
    L.append("")
    L.append("```sh")
    L.append(RAW_COUNT_COMMAND)
    L.append("```")
    L.append("")
    if raw_absent:
        L.append("%s absent in this tree; `find` reports it on stderr and the count is unaffected."
                 % " and ".join("`%s` is" % t for t in raw_absent))
        L.append("")
    L.append("Per module group (each `src/main/java` root attributed by the longest `module_groups` prefix, so a"
             " nested group such as `cloud` is not also counted in its parent): "
             + ", ".join("%s %d" % (g, raw_groups[g]) for g in sorted(raw_groups)) + "." if raw_groups else
             "Per module group: none.")
    L.append("")
    L.append("This raw file count is an upper bound on the surface, not the work list: it counts every artifact,"
             " internal and deferred ones included, and a `public` nested type. The measured work below counts"
             " only supported-public packages.")
    L.append("")
    L.append("## Work list")
    L.append("")
    pc = inv.get("package_classification", {})
    L.append("**Package split:** the owner-approved package rule classified %d packages; explicit rules classified %d."
             % (pc.get("rule", 0), pc.get("explicit", 0)))
    L.append("")
    L.append("Counts cover **supported-public Java artifacts**, and within them only packages classified")
    L.append("`supported-public`. Packages still `unclassified` are counted separately — they are an upper")
    L.append("bound that the package split has not yet decided, never silently in or out of scope.")
    L.append("")
    L.append("Estimate heuristic (added Javadoc lines): `%d × undocumented types + %d × undocumented members`." % (LINES_PER_TYPE, LINES_PER_MEMBER))
    L.append("Members are public methods/constructors (interface methods included), found statically.")
    L.append("")
    L.append("| Work item | Group | Artifacts in scope | Public types | Undocumented types | Undocumented members | Public types in unclassified packages | Estimate (lines) | Upper-bound estimate incl. unclassified (lines) |")
    L.append("|---|---|---|---|---|---|---|---|---|")
    order = [(g["group"], g.get("issue")) for g in groups]
    seen = set(x[0] for x in order)
    for r in inv["artifacts"]:
        if r["module_group"] not in seen:
            order.append((r["module_group"], r["doc_issue"]))
            seen.add(r["module_group"])
    total_lines = total_upper = 0
    for group, issue in order:
        java = [r for r in inv["artifacts"] if r["module_group"] == group and r["support"] == "supported-public" and r.get("packages") is not None]
        # A Dart package is documented with dartdoc whether it is published or only bundled (generated-only),
        # so its public declarations are work for the group's issue; an unclassified or deferred one is not.
        dart = [r for r in inv["artifacts"] if r["module_group"] == group and r["type"] == "dart-package"
                and r["support"] in ("supported-public", "generated-only")]
        arts = java + dart
        pub = [p for r in java for p in r["packages"] if p["classification"] == "supported-public"]
        unc = [p for r in java for p in r["packages"] if p["classification"] == "unclassified"]
        types = sum(p["public_type_count"] for p in pub) + sum(r["dart_surface"]["public_declarations"] for r in dart)
        ut = sum(p["undocumented_types"] for p in pub) + sum(r["dart_surface"]["undocumented_declarations"] for r in dart)
        um = sum(p["undocumented_members"] for p in pub)
        est = ut * LINES_PER_TYPE + um * LINES_PER_MEMBER
        upper = est + sum(p["undocumented_types"] * LINES_PER_TYPE + p["undocumented_members"] * LINES_PER_MEMBER for p in unc)
        total_lines += est
        total_upper += upper
        L.append("| %s | %s | %d | %d | %d | %d | %d | %d | %d |" % ("work item %s" % issue if issue else "—", group, len(arts), types, ut, um,
                                                               sum(p["public_type_count"] for p in unc), est, upper))
    L.append("")
    L.append("**Total estimate: %d added lines over classified supported-public packages; upper bound %d once every"
             " still-unclassified package is counted as supported-public.**" % (total_lines, total_upper))
    jars = sum(1 for r in inv["artifacts"] if r["support"] == "supported-public" and r["type"] == "maven-jar")
    L.append("")
    L.append("**Javadoc generation:** `release-profile` must generate cleanly for **%d** supported-public jar artifacts." % jars)
    L.append("## Javadoc configuration baseline")
    L.append("")
    cfg = javadoc_config(root)
    if not cfg:
        L.append("- the root `pom.xml` configures no `maven-javadoc-plugin`")
    for c in cfg:
        keys = ", ".join("`%s`=%s" % (k, "`%s`" % c[k] if c[k] is not None else "not set") for k in JAVADOC_KEYS
                         if c[k] is not None or k in ("doclint", "failOnError", "author"))
        L.append("- `%s` · %s · plugin %s%s: %s" % (c["scope"], c["section"], "`%s`" % c["version"] if c["version"] else "(version not set)",
                                                  " · goals " + ", ".join("`%s`" % g for g in c["goals"]) if c["goals"] else "", keys))
    L.append("")
    L.append("This is the **configured** level, read statically from the root `pom.xml` with `${…}` properties resolved;"
             " `not set` means the plugin's own default applies. **Runtime evidence:** the effective aggregate"
             " invocation used `-Xdoclint:all`, `-protected`, and no `-author`. Its initial whole-reactor run reached"
             " Javadoc but failed with 47 unresolved Spark symbols because the aggregate classpath omitted Spark's"
             " core, SQL, and streaming jars. The release configuration now supplies those jars explicitly with the modern"
             " aggregate plugin. With Maven 3.9.16 and maven-javadoc-plugin 3.12.0, the whole aggregate returned `rc=0`"
             " after two strict-doclint corrections, and rendered `target/reports/apidocs` contains no `Author:` label."
             " Mutation proof: adding `{@link MissingStrictDoclintType}` to `Traceable`, then running `clean"
             " javadoc:javadoc`, returned `rc=1` with `reference not found`; reverting the mutation restored the clean"
             " source.")
    L.append("")
    L.append("## Dart surface")
    L.append("")
    darts = [r for r in inv["artifacts"] if r["type"] == "dart-package"]
    if not darts:
        L.append("- none")
    for r in darts:
        s = r["dart_surface"]
        L.append("- `%s` (`%s`, %s): %d public declarations, %d undocumented" % (r["id"], r["module_path"], r["support"],
                                                                              s["public_declarations"], s["undocumented_declarations"]))
    L.append("")
    L.append("## Open inventory issues")
    L.append("")
    if inv["issues"]:
        L.append("`check` fails until each of these is resolved in `%s` or the build."
                 " Unclassified packages are counted per artifact here; `inventory.json` lists each one." % CLASSIFICATION)
        L.append("")
        per_art = {}
        for i in inv["issues"]:
            m = re.match(r"unclassified package \S+ in supported-public artifact (\S+)$", i)
            if m:
                per_art[m.group(1)] = per_art.get(m.group(1), 0) + 1
            else:
                L.append("- %s" % i)
        for rid in sorted(per_art):
            L.append("- %d unclassified package(s) in supported-public artifact `%s`" % (per_art[rid], rid))
    else:
        L.append("- none")
    return "\n".join(L) + "\n"


# ── Consumers ──────────────────────────────────────────────────────────────────────────────────────

def check_consumer(path, inv, name=None):
    problems = []
    name = name or path
    try:
        text = open(path, encoding="utf-8").read()
    except OSError as e:
        return ["%s: cannot read (%s)" % (name, e)]
    regions = re.findall(re.escape(REGION_BEGIN) + r"(.*?)" + re.escape(REGION_END), text, re.S)
    if not regions:
        return ["%s: no api-inventory region (%s … %s) — a consumer with no region cannot be checked" % (name, REGION_BEGIN, REGION_END)]
    listed = set(re.findall(r"`([\w.-]+:[\w.-]+)`", "".join(regions)))
    support = {r["id"]: r["support"] for r in inv["artifacts"]}
    want = set(k for k, v in support.items() if v == "supported-public")
    for rid in sorted(want - listed):
        problems.append("%s: missing supported-public artifact %s" % (name, rid))
    for rid in sorted(listed - want):
        problems.append("%s: not supported-public but listed: %s (%s)" % (name, rid, support.get(rid, "not in the inventory")))
    return problems


# ── CLI ────────────────────────────────────────────────────────────────────────────────────────────

def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    ap.add_argument("--root", default=os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")))
    sub = ap.add_subparsers(dest="cmd", required=True)
    g = sub.add_parser("generate")
    g.add_argument("--out", help="write the inventory JSON here only (no work list)")
    sub.add_parser("check")
    c = sub.add_parser("check-consumer")
    c.add_argument("files", nargs="+")
    a = ap.parse_args(argv)
    root = os.path.abspath(a.root)

    try:
        inv, issues = build(root)
    except ClassificationError as e:
        inv, issues = None, [str(e)]
    if inv is None:
        for i in issues:
            print("api-inventory: ERROR %s" % i, file=sys.stderr)
        return 1

    if a.cmd == "generate":
        if a.out:
            with open(a.out, "w", encoding="utf-8", newline="\n") as f:
                f.write(render_inventory(inv))
        else:
            outs = []
            try:  # vet BOTH destinations before creating a directory or writing a byte (RA-A4 A4-I1)
                for rel, body in ((INVENTORY, render_inventory(inv)), (WORKLIST, render_worklist(inv, root))):
                    outs.append((repo_path(root, rel), body))
            except ClassificationError as e:
                print("api-inventory: ERROR %s" % e, file=sys.stderr)
                return 1
            for p, body in outs:
                os.makedirs(os.path.dirname(p), exist_ok=True)
                with open(p, "w", encoding="utf-8", newline="\n") as f:
                    f.write(body)
        for i in issues:
            print("api-inventory: ISSUE %s" % i)
        print("api-inventory: %d artifacts, %d issue(s)" % (len(inv["artifacts"]), len(issues)))
        return 1 if issues else 0

    if a.cmd == "check":
        problems = list(issues)
        for rel, body in ((INVENTORY, render_inventory(inv)), (WORKLIST, render_worklist(inv, root))):
            try:
                p = repo_path(root, rel)
            except ClassificationError as e:
                problems.append(str(e))
                continue
            if not os.path.isfile(p):
                problems.append("drift: %s is missing (run scripts/api-inventory.py generate)" % rel)
            elif open(p, encoding="utf-8").read() != body:
                problems.append("drift: %s differs from a fresh generation (run scripts/api-inventory.py generate)" % rel)
        for pr in problems:
            print("api-inventory: FAIL %s" % pr)
        print("api-inventory: check %s — %d artifacts, %d problem(s)" % ("FAILED" if problems else "passed", len(inv["artifacts"]), len(problems)))
        return 1 if problems else 0

    # The comparison is only as good as the inventory it compares against: an invalid inventory fails
    # the consumer check too, so a docs-only lane cannot pass over it.
    problems = list(issues)
    for f in a.files:
        problems += check_consumer(f if os.path.isabs(f) else os.path.join(root, f), inv, f)
    for pr in problems:
        print("api-inventory: FAIL %s" % pr)
    print("api-inventory: consumer check %s — %d problem(s)" % ("FAILED" if problems else "passed", len(problems)))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
