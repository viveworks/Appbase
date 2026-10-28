#!/usr/bin/env python3
"""readme_storefront_scan.py — the parsing half of readme_storefront_test.sh (internal work item 2419).

The bash suite owns the arms and their verdicts. This helper does the Markdown and POM parsing, and
every mode exits non-zero on an internal error so the suite can tell "nothing found" from "crashed"
(RA-A1 round 4, L2-1):

  reactor ROOT                 every artifactId reachable from the root pom's <modules> graph
  links FILE                   LINE<TAB>TARGET for every relative link: inline and image links,
                               reference definitions, and HTML href=/src= (quoted or not)
  deferred FILE ARTIFACTS...   line numbers where a deferred capability is named without being
                               marked deferred NEAR the name; deferred artifactIds inside fenced
                               code; letters outside ASCII (homoglyphs)
  matrix FILE                  problems with the "## Release 1 support" matrix: exact capability
                               names, exact artifact cells, no coverage cell that denies support
  publication-state FILE       candidate only for the exact empty publication placeholder; otherwise
                               publication only for a unique populated marker pair; malformed fails
  notverified FILE             matrix rows that lack the "NOT VERIFIED vs internal work item 1399" marker
  verify README CATALOG INVENTORY
                               cross-check every matrix artifact and verified row marker
  tableshape FILE              rows whose cell count differs from their table's header
  swagger FILE                 problems with the "## Swagger UI" section (AC2419-7)
  forbidden-public FILE        internal devcontainer/topology/tooling references that public docs refuse
  tracking-public FILE         issue/PR/epic tracking references that public docs refuse

Tables follow GFM: a header row, a delimiter row, then every following non-blank line is a row whether
or not it starts with "|" (RA-A1 L5-2). HTML comments are not visible content anywhere (L1-N1(a),
L5-4). Standard library only; xml.etree resolves no external entities on python3 >= 3.7 / expat >= 2.4.

@author Navaneeth Mysore G
@author Claude
"""
import json
import os
import re
import sys
import unicodedata
import xml.etree.ElementTree as ET

NS = "{http://maven.apache.org/POM/4.0.0}"

# The Release-1 supported matrix: baseline section 4.1 (docs/Appbase-Release-1.0-Lean-Requirements-LATEST.md
# on develop), amended by the owner's internal work item 1141 Q1 ruling (comment 5855318230), which adds the reactive OAuth
# face to OAuth/OIDC and defers the R2DBC user-account store. Each capability maps to the EXACT artifact
# set its README row must list. This is the only copy.
MATRIX = {
    "Redis": {"appbase-client-redis", "appbase-service-web-oauth-tokencache-redis"},
    "Kafka": {"appbase-client-kafka"},
    "Cassandra": {"appbase-client-cassandra"},
    "Aerospike": {"appbase-client-aerospike", "appbase-service-user-session-aerospike",
                  "appbase-service-web-oauth-tokencache-aerospike"},
    "Elasticsearch": {"appbase-client-elasticsearch"},
    "ZooKeeper": {"appbase-client-zookeeper"},
    "Token cache": {"appbase-service-web-oauth"},
    "Firebase Auth": {"appbase-client-firebase", "appbase-service-user-account-firebaseauth"},
    "OAuth/OIDC": {"appbase-service-web-oauth-google", "appbase-service-web-oauth-okta",
                   "appbase-service-web-oauth-microsoft", "appbase-service-web-oauth-apple",
                   "appbase-service-web-oauth-github", "appbase-service-web-oauth-reactive"},
    "S3": {"appbase-client-cloud-aws-s3"},
    "JDBC": {"appbase-client-jdbc"},
    "JDBI": {"appbase-client-jdbi", "appbase-service-user-account-jdbi"},
    "JPA": {"appbase-client-jpa"},
    "R2DBC": {"appbase-client-r2dbc"},
    "Vitess": {"appbase-client-vitess"},
    "Druid": {"appbase-client-druid"},
}

# Deferred capabilities with NO artifact of their own (baseline section 4.2), as case-insensitive
# patterns. Meta and X are separate names (RA-A1 L1-N1(d)); X is matched only as a standalone capital,
# case-sensitively, so ordinary words and "x" in prose do not trip it.
DEFERRED_PATTERNS = [r"\bcognito\b", r"\bdynamodb\b", r"\bsqs\b", r"\bgrpc service bootstrap\b",
                     r"\bmeta\b(?=[\s,]+(and\s+)?\bX\b)",
                     # Meta on its own, as the OAuth/identity provider (RA-A4 A4-R3 on internal work item 2504).
                     r"\bmeta\b(?=\s+(oauth|oidc|login|log-in|sign[- ]?in|provider|identity|accounts?)\b)",
                     r"\bmeta\b(?=\s*\((facebook|instagram)\))", r"\bfacebook\b(?=\s+(oauth|login|sign[- ]?in|provider))"]
DEFERRED_CASED = [r"(?<![\w/.-])X(?=\s+and\s+AWS\s+Cognito|\s+OAuth|\s+provider)"]

# Prose names for every excluded artifact (RA-A1 L1-N1(c)). A single-segment id like
# appbase-client-spark also gets its short name ("spark"). An excluded id missing from this map fails
# the suite (see `prose_names`), so the list cannot silently grow without prose coverage.
PROSE_NAMES = {
    "appbase-client-clamav": [r"\bclamav\b"],
    "appbase-client-smtp": [r"\bsmtp\b"],
    "appbase-client-jasync": [r"\bjasync\b"],
    "appbase-client-spark": [r"\bspark\b"],
    "appbase-service-password": [r"\bpassword service\b"],
    "appbase-service-supportmessage-jdbi": [r"\bsupport[- ]?message[- ]jdbi\b", r"\bjdbi support[- ]?message\b"],
    "appbase-service-user-account-vitess": [r"\bvitess user[- ]account\b", r"\buser[- ]account[- ]vitess\b"],
    "appbase-service-user-account-r2dbc": [r"\br2dbc user[- ]account\b", r"\buser[- ]account[- ]r2dbc\b"],
}

# Removed L-13 coordinates are no longer reactor artifacts or BOM exclusions, but a public
# claim that they are supported must still fail the storefront gate.
REMOVED_ARTIFACTS = (
    "appbase-service-password",
    "appbase-service-supportmessage-jdbi",
    "appbase-service-user-account-vitess",
)

# A marker says the capability is deferred / unsupported. `defers` counts: "the owner defers X".
OK_RE = re.compile(r"\bdefer(s|red)?\b|not in release 1|\bnot supported\b|\bunsupported\b|not advertised"
                   r"|empty shells?\b|to release 2|release 2 work")
# A negated or past-tense marker marks nothing: "not deferred", "previously deferred" (L1-N2).
NEGATED_RE = re.compile(r"\b(not|no longer|never|isn't|is not|aren't|are not|previously|formerly|once|"
                        r"used to be|was|were|un-?)\s+(deferred|unsupported|an? empty shell)"
                        r"|\bno longer (not )?supported|\bnot (an? )?empty shell")
# A coverage cell that denies support in any of these ways fails the matrix (L1-N3).
DENY_RE = re.compile(OK_RE.pattern + r"|\bplanned\b|\bunavailable\b|not available|release 2|\bnot (yet )?"
                     r"(shipped|included|offered)\b")
# An affirmative support claim. When one sits CLOSER to a name than the nearest deferral marker, the
# name is being called supported: "Unlike SMTP, which is deferred, Spark is supported" (L1-N2, L3-N1).
AFFIRM_RE = re.compile(r"(?<!not )(?<!n't )\b(is|are|remains?|stays?|now|fully|also)\s+supported\b"
                       r"|(?<!not )(?<!un)\bsupported (in|for) release 1\b|\bships in release 1\b")
WINDOW = 8          # words: the marker must be within this distance of the name (L1-N1(b))
FENCE_OPEN = re.compile(r"^ {0,3}(`{3,}|~{3,})")
FENCE_CLOSE = re.compile(r"^ {0,3}(`{3,}|~{3,})\s*$")

# Owner decision D-I: these internal experiments and contributor tools are not public-release
# documentation. Keep the list narrow: README may still name public-facing scripts such as build.sh and
# the configuration-wizard launcher. Source-header @author lines are intentionally outside this rule.
FORBIDDEN_PUBLIC = (
    ("devcontainer", re.compile(r"\bdevcontainers?\b|\.[d]evcontainer/|devcontainer\.local_folder", re.I)),
    ("container guide", re.compile(r"Container-Guide\.md", re.I)),
    ("internal container tooling", re.compile(
        r"scripts/(?:appbase-container(?:-login)?|appbase-claude-launcher|container-keepalive(?:-install)?)\.sh", re.I)),
    # Public docs default-deny contributor automation. The storefront acceptance suite is the one
    # public evidence reference owner decision D-I retained; build.sh and the wizard launchers live
    # outside scripts/, so they remain unaffected by this boundary.
    ("internal scripts tooling", re.compile(
        r"\bscripts/(?!(?:test/readme_storefront_test|api-inventory-fast-check)\.sh"
        r"(?=$|[\s`)\]}>]|[.,;:](?=$|[\s`)\]}>])))"
        r"\S+"
        r"|\bscripts(?:\s*[/.-]\s*|\s+)tooling\b", re.I)),
    ("multi-container topology", re.compile(r"\bmulti[- ]container topology\b|scripts/appbase-topology\.sh", re.I)),
    ("internal container quickstart", re.compile(r"Fresh container off [`']?develop|Container quick start", re.I)),
)


def reactor(root):
    seen, stack, out = set(), [os.path.join(root, "pom.xml")], set()
    while stack:
        p = os.path.realpath(stack.pop())
        if p in seen or not os.path.isfile(p):
            continue
        seen.add(p)
        r = ET.parse(p).getroot()
        a = r.find(NS + "artifactId")
        if a is not None and a.text:
            out.add(a.text.strip())
        for m in r.iter(NS + "module"):
            if m.text:
                stack.append(os.path.join(os.path.dirname(p), m.text.strip(), "pom.xml"))
    if not out:
        raise SystemExit("reactor: no artifactId read from %s" % root)
    for a in sorted(out):
        print(a)
    return 0


def visible_lines(text):
    """Yield (lineno, line, in_fence). CommonMark fences: a fence closes only on the same character, at
    least as long as its opener. HTML comments are removed from non-fence lines, across lines too."""
    fence, in_comment = None, False
    for n, line in enumerate(text.split("\n"), 1):
        if fence is not None:
            c = FENCE_CLOSE.match(line)
            if c and c.group(1)[0] == fence[0] and len(c.group(1)) >= len(fence):
                fence = None
            yield n, line, True
            continue
        out, rest = "", line
        while rest:
            if in_comment:
                end = rest.find("-->")
                if end < 0:
                    rest = ""
                else:
                    rest, in_comment = rest[end + 3:], False
            else:
                start = rest.find("<!--")
                if start < 0:
                    out, rest = out + rest, ""
                else:
                    out, rest, in_comment = out + rest[:start], rest[start + 4:], True
        m = FENCE_OPEN.match(out)
        if m and not in_comment:
            fence = m.group(1)
            yield n, out, True
            continue
        yield n, out, False


def links(path):
    text = open(path, encoding="utf-8").read()
    pats = [
        re.compile(r"!?\[[^\]]*\]\(\s*<?([^)\s>]+)>?(?:\s+\"[^\"]*\")?\s*\)"),   # inline + image
        re.compile(r"^\s{0,3}\[[^\]]+\]:\s*<?(\S+?)>?(?:\s|$)"),                 # reference definition
        re.compile(r"\b(?:href|src)\s*=\s*(?:\"([^\"]+)\"|'([^']+)'|([^\s>]+))", re.I),
    ]
    for n, line, fenced in visible_lines(text):
        if fenced:
            continue
        for pat in pats:
            for m in pat.finditer(line):
                target = next(g for g in m.groups() if g)
                if re.match(r"^(https?:|mailto:|#)", target):
                    continue
                print(f"{n}\t{target}")
    return 0


def prose_names(artifacts):
    missing = [a for a in artifacts if a not in PROSE_NAMES]
    if missing:
        raise SystemExit("deferred: excluded artifacts with no prose name in PROSE_NAMES: %s" % missing)
    pats = list(DEFERRED_PATTERNS)
    for a in artifacts:
        pats.append(re.escape(a))
        pats.extend(PROSE_NAMES[a])
    return re.compile("|".join(pats), re.I), re.compile("|".join(DEFERRED_CASED))


def deferred(path, artifacts):
    guarded = tuple(dict.fromkeys((*artifacts, *REMOVED_ARTIFACTS)))
    bad_re, bad_cased = prose_names(guarded)
    art_re = re.compile("|".join(re.escape(a) for a in guarded))

    def names_in(text):
        return [m for m in bad_re.finditer(text)] + [m for m in bad_cased.finditer(text)]

    def marker_near(text, m):
        """A non-negated marker within WINDOW words of the name occurrence m."""
        words = [(w.start(), w.end()) for w in re.finditer(r"\S+", text)]
        idx = [i for i, (s, e) in enumerate(words) if s <= m.start() < e or m.start() <= s < m.end()]
        if not idx:
            return False
        lo, hi = max(0, idx[0] - WINDOW), min(len(words), idx[-1] + WINDOW + 1)
        s0 = words[lo][0]
        span = text[s0:words[hi - 1][1]].lower()
        if NEGATED_RE.search(span):
            return False
        oks = list(OK_RE.finditer(span))
        if not oks:
            return False
        pos = (m.start() - s0, m.end() - s0)

        def dist(x):
            return max(0, x.start() - pos[1], pos[0] - x.end())
        affs = list(AFFIRM_RE.finditer(span))
        return not affs or min(map(dist, oks)) < min(map(dist, affs))

    def judge_prose(block):
        # Sentences end at . ! ? ; — a clause ending in ':' is a lead-in whose (non-negated) marker carries
        # into the clause that follows it ("Deferred to Release 2: ClamAV, SMTP"). Elsewhere every name
        # needs its own marker within WINDOW words.
        for sent in re.split(r"(?<=[.!?;])\s+", block):
            carry = False
            for part in re.split(r":(?:\*\*|__)?\s+", sent):   # "**Deferred:** X" too
                found = names_in(part)
                low = part.lower()
                lead = OK_RE.search(NEGATED_RE.sub(" ", low)) is not None and NEGATED_RE.search(low) is None
                if found and not carry and not all(marker_near(part, m) for m in found):
                    return True
                if found and carry and NEGATED_RE.search(low):
                    return True
                carry = lead and not found
        return False

    def judge_row(row):
        # A row carries one status for everything in it, so its marker may sit in any cell. But an
        # affirmative support claim CLOSER to a name than every marker still calls that name supported,
        # exactly as in prose (RA-A1 A1-1 on internal work item 2504).
        low = row.lower()
        found = names_in(row)
        if not found:
            return False
        if not OK_RE.search(NEGATED_RE.sub(" ", low)) or NEGATED_RE.search(low):
            return True
        oks, affs = list(OK_RE.finditer(low)), list(AFFIRM_RE.finditer(low))
        if not affs:
            return False

        def dist(x, m):
            return max(0, x.start() - m.end(), m.start() - x.end())
        return any(min(dist(o, m) for o in oks) >= min(dist(a, m) for a in affs) for m in found)

    text = open(path, encoding="utf-8").read()
    hits, blk, kind, start = [], "", "", 0

    def flush():
        nonlocal blk, kind
        if blk and names_in(blk):
            if (judge_row(blk) if kind == "row" else judge_prose(blk)):
                hits.append(start)
        blk, kind = "", ""

    in_table = False
    for n, line, fenced in visible_lines(text):
        # Homoglyphs: a letter outside ASCII can spell a deferred name that no pattern matches (L1-N1(e)).
        norm = unicodedata.normalize("NFKC", line)
        if any(ch.isalpha() and ord(ch) > 127 for ch in norm):
            hits.append(n)
        if fenced:
            flush()
            in_table = False
            # Fenced code is not prose, but a deferred ARTIFACT in a snippet is a usage example of it.
            if art_re and art_re.search(line):
                hits.append(n)
            continue
        if not line.strip():
            flush()
            in_table = False
            continue
        if in_table or line.lstrip().startswith("|"):
            flush()
            in_table = True
            kind = "row"
            start, blk = n, line
            flush()
            continue
        if re.match(r"^(#|\s*[-*] |\s*[0-9]+\. )", line):
            flush()
        if not blk:
            start = n
        blk += " " + line
    flush()
    for h in sorted(set(hits)):
        print(h)
    return 0


def split_cells(line):
    t = re.sub(r"`[^`]*`", lambda m: m.group(0).replace("|", "\x00"), line.strip())
    t = t.replace("\\|", "\x00")
    if t.startswith("|"):
        t = t[1:]
    if t.endswith("|"):
        t = t[:-1]
    return [c.replace("\x00", "|").strip() for c in t.split("|")]


DELIM = re.compile(r"^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)*\|?\s*$")


def tables(lines):
    """GFM tables among (lineno, text) pairs: header, delimiter, then every non-blank line until a blank
    one, whether or not it starts with '|'. Yields (header_lineno, header_cells, [(lineno, cells)])."""
    i = 0
    while i < len(lines) - 1:
        n, line = lines[i]
        nxt = lines[i + 1][1]
        if "|" in line and DELIM.match(nxt):
            rows, j = [], i + 2
            while j < len(lines) and lines[j][1].strip():
                rows.append((lines[j][0], split_cells(lines[j][1])))
                j += 1
            yield n, split_cells(line), rows
            i = j
        else:
            i += 1


def section(path, heading_re):
    text = open(path, encoding="utf-8").read()
    out, inside = [], False
    for n, line, fenced in visible_lines(text):
        if not fenced and line.startswith("## "):
            if inside:
                break
            inside = bool(re.match(heading_re, line))
            continue
        if inside:
            out.append((n, "" if fenced else line))
    return out


def matrix_rows(path):
    body = section(path, r"^## Release 1 support")
    found = list(tables(body))
    return found[0][2] if found else []


def publication_state(path):
    """Classify the README's release section without treating a missing table as evidence."""
    text = open(path, encoding="utf-8").read()
    begin, end = "<!-- release-evidence:begin -->", "<!-- release-evidence:end -->"
    sections = list(re.finditer(r"(?ms)^## Release 1 support[ \t]*\n(.*?)(?=^## |\Z)", text))
    if (len(sections) != 1 or text.count(begin) != 1 or text.count(end) != 1
            or "docs/release/evidence-index/known-limitations.md" in text):
        raise SystemExit("publication-state: missing, duplicated or stale release section")
    body = sections[0].group(1).strip()
    if body == begin + "\n" + end:
        print("candidate")
    elif body.startswith(begin + "\n") and body.endswith("\n" + end):
        print("publication")
    else:
        raise SystemExit("publication-state: ambiguous release section")
    return 0


def matrix(path):
    rows = matrix_rows(path)
    problems = []
    if not rows:
        problems.append("no matrix rows under '## Release 1 support' — refusing to pass over nothing")
    seen = set()
    for _, cells in rows:
        if len(cells) < 3:
            problems.append(f"row {cells!r} has fewer than 3 cells")
            continue
        cap = cells[0].replace("**", "").strip()
        if cap not in MATRIX:
            problems.append(f"row '{cap}' is not a Release-1 supported capability (exact name)")
            continue
        if cap in seen:
            problems.append(f"row '{cap}' is listed twice")
        seen.add(cap)
        if NEGATED_RE.search(cells[1].lower()) or DENY_RE.search(cells[1].lower()):
            problems.append(f"row '{cap}': coverage cell says it is not supported: {cells[1]!r}")
        arts = set(re.findall(r"`(appbase-[a-z0-9-]+)`", cells[2]))
        if arts != MATRIX[cap]:
            problems.append(f"row '{cap}': artifacts {sorted(arts)} != expected {sorted(MATRIX[cap])}")
    for cap in sorted(set(MATRIX) - seen):
        problems.append(f"supported matrix is missing '{cap}'")
    for p in problems:
        print(p)
    return 0


def notverified(path):
    rows = matrix_rows(path)
    if not rows:
        print("0\tno matrix rows — refusing to pass over nothing")
    for n, cells in rows:
        if not any("NOT VERIFIED vs internal work item 1399" in c for c in cells):
            print(f"{n}\t{' | '.join(cells)[:60]}")
    return 0


def verify(readme, catalogue, inventory):
    """Print AC3 discrepancies between the matrix and both shipped-surface sources."""
    rows = matrix_rows(readme)
    if not rows:
        print("no matrix rows under '## Release 1 support' — refusing to pass over nothing")
        return 0
    catalogue_text = open(catalogue, encoding="utf-8").read()
    catalogue_ids = set(re.findall(r"com\.viveworks\.appbase:(appbase-[a-z0-9-]+)", catalogue_text))
    if not catalogue_ids:
        print("catalogue has no Appbase coordinates — refusing to pass over nothing")
    records = json.load(open(inventory, encoding="utf-8")).get("artifacts")
    if not isinstance(records, list) or not records:
        print("inventory has no artifact records — refusing to pass over nothing")
        return 0
    by_id = {}
    for record in records:
        artifact_id = record.get("artifact_id") if isinstance(record, dict) else None
        if not artifact_id:
            print("inventory contains an artifact record without artifact_id")
            continue
        if artifact_id in by_id:
            print(f"inventory contains duplicate artifact_id {artifact_id}")
        by_id[artifact_id] = record
    for n, cells in rows:
        if len(cells) < 4:
            print(f"README.md:{n}: matrix row has fewer than 4 cells")
            continue
        capability = cells[0].replace("**", "").strip()
        artifacts = set(re.findall(r"`(appbase-[a-z0-9-]+)`", cells[2]))
        if not artifacts:
            print(f"README.md:{n}: {capability} row has no artifacts — refusing to pass over nothing")
        for artifact_id in sorted(artifacts):
            if artifact_id not in catalogue_ids:
                print(f"README.md:{n}: {artifact_id} is absent from the catalogue")
            record = by_id.get(artifact_id)
            if record is None:
                print(f"README.md:{n}: {artifact_id} is absent from the internal work item 1399 inventory")
            elif record.get("support") != "supported-public":
                print(f"README.md:{n}: {artifact_id} is {record.get('support')!r}, not supported-public")
        if not any("VERIFIED vs internal work item 1399 inventory" in cell and "NOT VERIFIED" not in cell
                   for cell in cells):
            print(f"README.md:{n}: {capability} row lacks the VERIFIED vs internal work item 1399 inventory marker")
    return 0


def tableshape(path):
    text = open(path, encoding="utf-8").read()
    lines = [(n, "" if fenced else line) for n, line, fenced in visible_lines(text)]
    for hn, header, rows in tables(lines):
        for n, cells in rows:
            if len(cells) != len(header):
                print(f"{n}: {len(cells)} cells vs header {len(header)}")
    return 0


def forbidden_public(path):
    """Print every public-doc line that reintroduces an owner-rejected internal experiment/tool."""
    text = open(path, encoding="utf-8").read()
    for n, line in enumerate(text.splitlines(), 1):
        for label, pattern in FORBIDDEN_PUBLIC:
            if pattern.search(line):
                print(f"{n}\t{label}")
    return 0


def tracking_public(path):
    """Print internal delivery-tracking references found in visible public user documentation."""
    patterns = (
        ("issue number", re.compile(r"(?<![A-Za-z0-9])#[0-9]{4,}\b")),
        ("PR number", re.compile(r"\b(?:PR|pull\s+request)\s*#?\s*[0-9]+\b", re.I)),
        ("issue reference", re.compile(r"\bissues?\s*#?\s*[0-9]+\b", re.I)),
        ("tracker link", re.compile(
            r"https?://github\.com/navaneeth-mysore/Appbase/(?:issues|pull)/[0-9]+\b", re.I)),
        ("epic reference", re.compile(r"\bepic\s*#?\s*[0-9]+\b", re.I)),
    )
    text = open(path, encoding="utf-8").read()
    for n, line, fenced in visible_lines(text):
        if fenced:
            continue
        line = re.sub(r"(`+).*?\1", "", line)
        for label, pattern in patterns:
            if pattern.search(line):
                print(f"{n}\t{label}")
    return 0


def swagger(path):
    """AC2419-7. Screenshots count in every image form (inline, reference-style, <img>) outside HTML
    comments. With none, the section must say NOT MEASURED and name the stacktest run. With any, each must
    be a non-empty LOCAL file, the section must carry a capture date and revision, and the NOT MEASURED
    marker must be gone (RA-A1 L5-4)."""
    body = section(path, r"^## Swagger UI")
    if not body:
        print("README.md has no '## Swagger UI' section")
        return 0
    text = "\n".join(line for _, line in body)
    base = os.path.dirname(os.path.abspath(path))
    # Reference definitions may sit anywhere in the document, and labels match case-insensitively with
    # whitespace collapsed (CommonMark). Full `![a][ref]`, collapsed `![ref][]` and shortcut `![ref]` images
    # all resolve through them (RA-A4 A4-R2 on internal work item 2504).
    whole = "\n".join(line for _, line, fenced in visible_lines(open(path, encoding="utf-8").read()) if not fenced)

    def norm(label):
        return re.sub(r"\s+", " ", label.strip()).lower()
    refs = {norm(k): v for k, v in re.findall(r"^\s{0,3}\[([^\]]+)\]:\s*<?(\S+?)>?(?:\s|$)", whole, re.M)}
    targets = re.findall(r"!\[[^\]]*\]\(\s*<?([^)\s>]+)", text)
    for alt, ref in re.findall(r"!\[([^\]]*)\]\[([^\]]*)\]", text):            # full and collapsed
        targets.append(refs.get(norm(ref or alt), "unresolved-reference:" + (ref or alt)))
    for alt in re.findall(r"!\[([^\]]*)\](?![\[(:])", text):                    # shortcut
        targets.append(refs.get(norm(alt), "unresolved-reference:" + alt))
    targets += re.findall(r"<img\b[^>]*\bsrc\s*=\s*[\"']?([^\"'\s>]+)", text, re.I)
    if not targets:
        if not ("NOT MEASURED" in text and re.search(r"stacktest", text, re.I)):
            print("no screenshots, and the section does not say NOT MEASURED and name the stacktest run")
        return 0
    if "NOT MEASURED" in text:
        print("screenshots are embedded, but the section still says NOT MEASURED")
    for t in targets:
        if not t or re.match(r"^[a-z]+:", t, re.I):
            print(f"screenshot {t!r} is not a local file in the repository")
            continue
        p = os.path.normpath(os.path.join(base, t))
        if not os.path.isfile(p) or os.path.getsize(p) == 0:
            print(f"screenshot {t!r} is missing or empty")
    if not (re.search(r"20\d\d-\d\d-\d\d", text) and re.search(r"\b[0-9a-f]{9,40}\b", text)):
        print("screenshots are embedded without a capture date and revision")
    return 0


def main(argv):
    mode = argv[1] if len(argv) > 1 else ""
    modes = {"reactor": lambda: reactor(argv[2]), "links": lambda: links(argv[2]),
             "deferred": lambda: deferred(argv[2], argv[3:]), "matrix": lambda: matrix(argv[2]),
             "publication-state": lambda: publication_state(argv[2]),
             "notverified": lambda: notverified(argv[2]),
             "verify": lambda: verify(argv[2], argv[3], argv[4]),
             "tableshape": lambda: tableshape(argv[2]),
             "swagger": lambda: swagger(argv[2]), "forbidden-public": lambda: forbidden_public(argv[2]),
             "tracking-public": lambda: tracking_public(argv[2])}
    if mode not in modes:
        print(f"usage: {argv[0]} {'|'.join(modes)} ARGS", file=sys.stderr)
        return 2
    return modes[mode]()


if __name__ == "__main__":
    sys.exit(main(sys.argv))
