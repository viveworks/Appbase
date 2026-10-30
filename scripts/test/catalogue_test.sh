#!/usr/bin/env bash
#
# catalogue_test.sh — arms for scripts/catalogue-check.py (internal work item 2418, P06·E02 N03).
#
# Each arm builds a throwaway fixture repository (poms, a BOM, a pubspec, the internal work item 1399 classification file
# and a catalogue) and runs the comparison against it with --root, so the arms exercise the command's
# behaviour — never the text of the committed catalogue. Arms are named after internal work item 2418's Acceptance items
# (AC2418-<n>); anything beyond them is EXTRA_<slug> with its reason.
#
# @author Navaneeth Mysore G
# @author Claude
#
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CHK="$ROOT/scripts/catalogue-check.py"
INV_GEN="$ROOT/scripts/api-inventory.py"

PASS=0; FAIL=0
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

ok()  { printf '  ok   %s\n' "$1"; PASS=$((PASS+1)); }
bad() { printf '  FAIL %s\n' "$1"; FAIL=$((FAIL+1)); }

run() { OUT="$(python3 "$CHK" --root "$FX" "$@" 2>&1)"; RC=$?; }

expect() {  # expect NAME WANT_RC [PATTERN...] — every pattern must appear in the output
    local name="$1" want="$2"; shift 2
    if [ "$RC" != "$want" ]; then bad "$name — expected rc=$want, got rc=$RC"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -15; return; fi
    for pat in "$@"; do
        if ! printf '%s\n' "$OUT" | grep -qE -- "$pat"; then
            bad "$name — rc=$RC but output lacks /$pat/"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -15; return
        fi
    done
    ok "$name (rc=$RC)"
}

pom() {  # pom DIR ARTIFACT PACKAGING [MODULES...]
    local dir="$1" art="$2" pkg="$3"; shift 3
    mkdir -p "$FX/$dir"
    {
        printf '<project xmlns="http://maven.apache.org/POM/4.0.0">\n  <modelVersion>4.0.0</modelVersion>\n'
        if [ "$art" = demo ]; then printf '  <groupId>com.example</groupId>\n  <version>1.0.0</version>\n'
        else printf '  <parent><groupId>com.example</groupId><artifactId>demo</artifactId><version>1.0.0</version></parent>\n'; fi
        printf '  <artifactId>%s</artifactId>\n  <packaging>%s</packaging>\n' "$art" "$pkg"
        if [ $# -gt 0 ]; then printf '  <modules>\n'; for m in "$@"; do printf '    <module>%s</module>\n' "$m"; done; printf '  </modules>\n'; fi
        [ "$art" = demo-bom ] && printf '  <dependencyManagement><dependencies>\n    <dependency><groupId>com.example</groupId><artifactId>lib</artifactId><version>${project.version}</version></dependency>\n  </dependencies></dependencyManagement>\n'
        printf '</project>\n'
    } > "$FX/$dir/pom.xml"
}

# entry ID SUPPORT MODULE [VERSION] — one complete catalogue entry.
entry() {
    printf '### `%s`\n\n' "$1"
    printf -- '- **Coordinates:** `%s:%s`\n' "$1" "${4:-1.0.0}"
    printf -- '- **Module:** `%s`\n' "$3"
    printf -- '- **Support:** `%s`\n' "$2"
    printf -- '- **Purpose:** does a thing\n'
    printf -- '- **Capabilities:** thing one; thing two\n'
    printf -- '- **Public entry points:** `Api`\n'
    printf -- '- **Configuration / backend:** none\n'
    printf -- '- **Examples / API docs:** see the guide\n'
    printf -- '- **Limitations:** none known\n\n'
}

region() { printf '<!-- api-inventory:supported-public:begin -->\n'; for a in "$@"; do printf -- '- `%s`\n' "$a"; done; printf '<!-- api-inventory:supported-public:end -->\n\n'; }

CAT="docs/Module-and-Service-Catalog.md"

# The complete, agreeing catalogue for the baseline fixture.
catalogue() {
    {
        printf '# Catalogue\n\n'
        region com.example:demo-bom com.example:lib
        printf '## Shipped\n\n'
        entry com.example:demo-bom supported-public demo-bom
        entry com.example:lib supported-public lib
        entry pub:demo_ui generated-only lib/ui
        printf '## Internal build modules\n\n'
        entry com.example:demo internal-implementation .
        entry com.example:impl internal-implementation impl
    } > "$FX/$CAT"
}

fixture() {
    rm -rf "$WORK/fx"; FX="$WORK/fx"; mkdir -p "$FX/docs/api-inventory"
    pom . demo pom lib impl demo-bom
    pom lib lib jar
    pom impl impl jar
    pom demo-bom demo-bom pom
    mkdir -p "$FX/lib/src/main/java/com/example/lib" "$FX/lib/ui/lib"
    printf 'package com.example.lib;\n/** Api. */\npublic interface Api {}\n' > "$FX/lib/src/main/java/com/example/lib/Api.java"
    printf 'name: demo_ui\npublish_to: none\nversion: 1.0.0\n' > "$FX/lib/ui/pubspec.yaml"
    cat > "$FX/docs/api-inventory/classification.json" <<'EOF'
{
  "schema": 1,
  "module_groups": [],
  "artifacts": {
    "com.example:demo":     {"support": "internal-implementation", "evidence": "parent"},
    "com.example:demo-bom": {"support": "supported-public", "evidence": "§2 bom", "family": "platform", "entry_points": ["Api"], "backend": "none", "docs_destination": "javadoc", "limitations": "none"},
    "com.example:lib":      {"support": "supported-public", "evidence": "§4.1 lib", "family": "library", "entry_points": ["Api"], "backend": "none", "docs_destination": "javadoc", "limitations": "none"},
    "com.example:impl":     {"support": "internal-implementation", "evidence": "not exported"},
    "pub:demo_ui":          {"support": "generated-only", "evidence": "publish_to none"}
  },
  "packages": [{"prefix": "com.example.lib", "classification": "supported-public", "reason": "api"}]
}
EOF
    catalogue
}

# edit PYTHON — rewrite the fixture catalogue text `t` in place.
edit() {
    python3 - "$FX/$CAT" "$1" <<'EOF'
import sys
p, expr = sys.argv[1], sys.argv[2]
t = open(p).read()
exec(expr)
open(p, "w").write(t)
EOF
}

echo "catalogue arms — command: ${CHK#"$ROOT"/}"
echo
[ -f "$CHK" ] || { bad "comparison command missing at ${CHK#"$ROOT"/}"; echo; echo "passed: $PASS  failed: $FAIL"; exit 1; }

# ── AC2418-1: every shipped artifact records all nine required fields ─────────────────────────────
fixture; run
expect "AC2418-1: a complete catalogue that agrees with the inventory passes" 0
fixture; edit 't = t.replace("- **Limitations:** none known\n\n### `pub:demo_ui`", "\n### `pub:demo_ui`")'
run; expect "AC2418-1: an entry missing a required field fails" 1 "incomplete.*com.example:lib.*Limitations"
fixture; edit 't = t.replace("- **Purpose:** does a thing\n- **Capabilities:** thing one; thing two\n- **Public entry points:** `Api`", "- **Purpose:** TODO\n- **Capabilities:** thing one; thing two\n- **Public entry points:** `Api`", 1)'
run; expect "AC2418-1: a placeholder (TODO) field counts as missing" 1 "incomplete.*Purpose"
fixture; edit 't = t.replace("- **Configuration / backend:** none\n", "- **Configuration / backend:**\n", 1)'
run; expect "AC2418-1: an empty field counts as missing" 1 "incomplete.*Configuration / backend"
# Placeholder forms beyond a bare TODO (RA-A2 C1): each counts as missing, with emphasis stripped first.
for form in 'TODO: fill in' '**TBD**' '<fill me>' '—' 'Not applicable' '[describe]' 'see FIXME later' '*n/a*'; do
    fixture; edit "t = t.replace('- **Capabilities:** thing one; thing two', '- **Capabilities:** $form', 1)"
    run; expect "AC2418-1: the placeholder form '$form' counts as missing" 1 "incomplete.*Capabilities"
done
fixture; edit "t = t.replace('- **Capabilities:** thing one; thing two', '- **Capabilities:** reads — and writes — records; not applicable to Dart', 1)"
run; expect "AC2418-1: negative control — real prose containing a dash and 'not applicable' passes" 0
# Entries a reader cannot see (RA-A2 C2): inside a fenced block or an HTML comment they do not count.
fixture; edit 'import re; m = re.search(r"### `com.example:impl`.*?(?=### |\Z)", t, flags=re.S); t = t[:m.start()] + "```\n" + m.group(0) + "```\n" + t[m.end():]'
run; expect "AC2418-1: an entry inside a fenced code block counts as missing" 1 "missing.*com.example:impl"
fixture; edit 'import re; m = re.search(r"### `com.example:impl`.*?(?=### |\Z)", t, flags=re.S); t = t[:m.start()] + "````\nignored\n```\n" + m.group(0) + "````\n" + t[m.end():]'
run; expect "AC2418-1: three backticks cannot close a four-backtick fence" 1 "missing.*com.example:impl"
fixture; edit 'import re; m = re.search(r"### `com.example:impl`.*?(?=### |\Z)", t, flags=re.S); t = t[:m.start()] + "````\nignored\n```` trailing text\n" + m.group(0) + "````\n" + t[m.end():]'
run; expect "AC2418-1: trailing text prevents a fence delimiter from closing" 1 "missing.*com.example:impl"
fixture; edit 'import re; m = re.search(r"### `com.example:impl`.*?(?=### |\Z)", t, flags=re.S); t = t[:m.start()] + "    ````\n" + m.group(0) + "````\n" + t[m.end():]'
run; expect "AC2418-1: a four-space-indented delimiter is not a fence opener" 0
fixture; edit 'import re; m = re.search(r"### `com.example:impl`.*?(?=### |\Z)", t, flags=re.S); t = t[:m.start()] + "``` bad`info\n" + m.group(0) + "```\n" + t[m.end():]'
run; expect "AC2418-1: a backtick opener rejects info containing a backtick" 0
fixture; edit 'import re; m = re.search(r"### `com.example:impl`.*?(?=### |\Z)", t, flags=re.S); t = t[:m.start()] + "<!--\n" + m.group(0) + "-->\n" + t[m.end():]'
run; expect "AC2418-1: an entry inside a multi-line HTML comment counts as missing" 1 "missing.*com.example:impl"

# ── AC2418-2: a checked-in command compares against the generated inventory and exits non-zero ─────
fixture; rm -f "$FX/$CAT"
run; expect "AC2418-2: a missing catalogue file fails rather than passing vacuously" 1 "catalogue.*not found|no such"
fixture; printf '# Catalogue\n\nno entries\n' > "$FX/$CAT"
run; expect "AC2418-2: a catalogue with zero entries fails" 1 "missing"
cls_add() { python3 - "$FX/docs/api-inventory/classification.json" <<'EOF'
import json, sys
p = sys.argv[1]; c = json.load(open(p))
c["artifacts"]["com.example:extra"] = {"support": "internal-implementation", "evidence": "new"}
json.dump(c, open(p, "w"), indent=2)
EOF
}
# Plant a STALE committed inventory.json first (generated before `extra` existed), so a checker that
# preferred the committed file would miss `extra` and this arm would go red (RA-A2 C4).
fixture; python3 "$INV_GEN" --root "$FX" generate >/dev/null 2>&1
[ -s "$FX/docs/api-inventory/inventory.json" ] || bad "AC2418-2: fixture setup — the stale inventory.json was not planted"
pom . demo pom lib impl demo-bom extra; pom extra extra jar
cls_add
if grep -q 'com.example:extra' "$FX/docs/api-inventory/inventory.json"; then bad "AC2418-2: fixture setup — the planted inventory is not stale"; fi
run; expect "AC2418-2: the comparison reads the FRESH inventory — a stale committed inventory.json cannot hide a new artifact" 1 "missing.*com.example:extra"

# ── AC2418-3: mutation — one missing, one duplicate, one inconsistent entry, each reported ─────────
fixture; edit 'import re; t = re.sub(r"### `com.example:impl`.*?(?=### |\Z)", "", t, flags=re.S)'
run; expect "AC2418-3: a missing entry is reported" 1 "missing.*com.example:impl"
fixture; edit 't = t + t[t.index("### `com.example:lib`"):t.index("### `pub:demo_ui`")]'
run; expect "AC2418-3: a duplicate entry is reported" 1 "duplicate.*com.example:lib"
fixture; edit 't = t.replace("- **Support:** `supported-public`\n- **Purpose:** does a thing", "- **Support:** `supported-public`\n- **Support:** `internal-implementation`\n- **Purpose:** does a thing", 1)'
run; expect "AC2418-3: a contradictory duplicate required field is reported" 1 "duplicate required field.*com.example:demo-bom.*Support"
if printf '%s\n' "$OUT" | grep -q "inconsistent.*com.example:demo-bom.*Support"; then
    bad "AC2418-3: duplicate handling preserves the first Support value"
else ok "AC2418-3: duplicate handling preserves the first Support value"; fi
fixture; edit 't = t.replace("- **Support:** `supported-public`\n- **Purpose:** does a thing", "- **Support:** `supported-public`\n- **Support:** `supported-public`\n- **Purpose:** does a thing", 1)'
run; expect "AC2418-3: an identical duplicate required field is still reported" 1 "duplicate required field.*com.example:demo-bom.*Support.*lines [0-9]+ and [0-9]+"
fixture; edit 't = t.replace("- **Support:** `internal-implementation`\n- **Purpose:** does a thing", "- **Support:** `supported-public`\n- **Purpose:** does a thing", 1)'
run; expect "AC2418-3: an inconsistent support state is reported" 1 "inconsistent.*com.example:demo.*Support"
fixture; edit 't = t.replace("`com.example:lib:1.0.0`", "`com.example:lib:9.9.9`")'
run; expect "AC2418-3: inconsistent coordinates are reported" 1 "inconsistent.*com.example:lib.*Coordinates"
fixture; edit 't = t.replace("- **Module:** `impl`", "- **Module:** `elsewhere`")'
run; expect "AC2418-3: an inconsistent module path is reported" 1 "inconsistent.*com.example:impl.*Module"
fixture; edit '
import re
t = re.sub(r"### `com.example:impl`.*?(?=### |\Z)", "", t, flags=re.S)
t = t + t[t.index("### `com.example:lib`"):t.index("### `pub:demo_ui`")]
t = t.replace("- **Support:** `generated-only`", "- **Support:** `supported-public`")'
run; expect "AC2418-3: all three mutations at once are each reported" 1 "missing.*com.example:impl" "duplicate.*com.example:lib" "inconsistent.*pub:demo_ui"

# ── AC2418-4: only shipped Release-1 capability; nothing aspirational presented as shipped ─────────
fixture; { entry com.example:grpc supported-public grpc; } >> "$FX/$CAT"
run; expect "AC2418-4: an entry for an artifact the build does not produce fails" 1 "not in the inventory.*com.example:grpc"
fixture; python3 - "$FX/docs/api-inventory/classification.json" <<'EOF'
import json, sys
p = sys.argv[1]; c = json.load(open(p)); del c["artifacts"]["com.example:impl"]; json.dump(c, open(p, "w"), indent=2)
EOF
edit 'import re; t = re.sub(r"(### `com.example:impl`.*?- \*\*Support:\*\* )`internal-implementation`", r"\1`unclassified`", t, flags=re.S)'
run; expect "AC2418-4: an unclassified inventory artifact fails even when the catalogue mirrors it" 1 "inventory.*unclassified.*com.example:impl"
fixture; edit 't = t.replace("- `com.example:lib`\n<!--", "- `com.example:lib`\n- `com.example:impl`\n<!--")'
run; expect "AC2418-4: the supported-public summary region advertising a non-supported artifact fails" 1 "not supported-public.*com.example:impl"
# Section placement (RA-A2 C3): a section admits only the support states its heading names.
fixture; edit 't = t.replace("## Internal build modules", "## Deferred — NOT part of Release 1")'
run; expect "AC2418-4: an internal artifact under a Deferred section fails placement" 1 "placement.*com.example:demo.*Deferred"
fixture; edit 'import re; m = re.search(r"### `com.example:lib`.*?(?=### )", t, flags=re.S); t = t[:m.start()] + t[m.end():] + m.group(0)'
run; expect "AC2418-4: a supported-public artifact moved under the Internal section fails placement" 1 "placement.*com.example:lib.*Internal"
fixture; edit 't = t.replace("## Shipped\n\n", "")'
run; expect "AC2418-4: an entry under no section fails placement" 1 "placement.*com.example:demo-bom.*no '## ' section"
fixture; edit 't = t.replace("## Internal build modules", "## Open decisions — NOT shipped")'
run; expect "AC2418-4: an internal artifact under Open decisions fails placement" 1 "placement.*com.example:impl.*Open decisions"

# ── EXTRAS ──────────────────────────────────────────────────────────────────────────────────────────
# EXTRA_stdlib_only — must run on a bare python3 (macOS host).
if python3 - "$CHK" <<'EOF'
import ast, sys
tree = ast.parse(open(sys.argv[1]).read())
mods = {n.name.split('.')[0] for x in ast.walk(tree) if isinstance(x, ast.Import) for n in x.names}
mods |= {x.module.split('.')[0] for x in ast.walk(tree) if isinstance(x, ast.ImportFrom) and x.module}
sys.exit(1 if sorted(m for m in mods if m not in sys.stdlib_module_names) else 0)
EOF
then ok "EXTRA_stdlib_only: comparison imports only the standard library"; else bad "EXTRA_stdlib_only: non-stdlib import"; fi

# EXTRA_package_issues_not_catalogue_scope — the PACKAGE split is an API-doc (internal work item 1400–internal work item 1406) concern, not an
# artifact-catalogue one. With no explicit package rules the internal work item 1399 package rule (owner ruling (b)) classifies every package;
# either way an otherwise agreeing catalogue must pass. (If a package is ever left unclassified again, it is
# printed as a note and still does not fail the catalogue.)
fixture; python3 - "$FX/docs/api-inventory/classification.json" <<'EOF'
import json, sys
p = sys.argv[1]; c = json.load(open(p)); c["packages"] = []; json.dump(c, open(p, "w"), indent=2)
EOF
run; expect "EXTRA_package_issues_not_catalogue_scope: the package split (explicit or rule) never fails the artifact catalogue" 0

# EXTRA_catalogue_only_mode — catalogue drift must be gateable while the inventory is still red (RA-A2 C5):
# --catalogue-only ignores inventory-side problems for the exit code, and still fails on catalogue-side ones.
fixture; python3 - "$FX/docs/api-inventory/classification.json" <<'PYEOF'
import json, sys
p = sys.argv[1]; c = json.load(open(p)); del c["artifacts"]["com.example:impl"]; json.dump(c, open(p, "w"), indent=2)
PYEOF
edit 'import re; t = re.sub(r"(### `com.example:impl`.*?- \*\*Support:\*\* )`internal-implementation`", r"\1`unclassified`", t, flags=re.S); t = t.replace("### `com.example:impl`", "## Open decisions\n\n### `com.example:impl`")'
run --catalogue-only; expect "EXTRA_catalogue_only_mode: an inventory-side problem alone does not fail --catalogue-only" 0 "inventory-side.*unclassified.*com.example:impl"
edit 't = t.replace("`com.example:lib:1.0.0`", "`com.example:lib:9.9.9`")'
run --catalogue-only; expect "EXTRA_catalogue_only_mode: a catalogue-side problem still fails --catalogue-only" 1 "FAIL inconsistent.*com.example:lib"
fixture; rm -f "$FX/docs/api-inventory/classification.json"
run --catalogue-only; expect "EXTRA_catalogue_only_mode: missing inventory classification is never suppressed" 1 "FAIL inventory construction:.*classification"
fixture; printf '{ malformed\n' > "$FX/docs/api-inventory/classification.json"
run --catalogue-only; expect "EXTRA_catalogue_only_mode: malformed inventory classification is never suppressed" 1 "FAIL inventory construction:.*classification"

# EXTRA_package_issue_coupling — the note filter keys on internal work item 1399's "unclassified package" wording. Drive
# api-inventory into producing that issue for real (the package rule's default set to unclassified, i.e.
# option (c), not the ruling) and require it to stay a note. If internal work item 1399 rewords the message this arm goes red,
# instead of the catalogue silently turning red later (RA-A2 C10).
fixture; python3 - "$FX/docs/api-inventory/classification.json" <<'PYEOF'
import json, sys
p = sys.argv[1]; c = json.load(open(p)); c["packages"] = []; json.dump(c, open(p, "w"), indent=2)
PYEOF
if OUT="$(python3 - "$CHK" "$FX" 2>&1 <<'PYEOF'
import importlib.util, os, sys
chk_path, fx = sys.argv[1], sys.argv[2]
spec = importlib.util.spec_from_file_location("catalogue_check", chk_path)
chk = importlib.util.module_from_spec(spec); spec.loader.exec_module(chk)
inv = chk.load_inventory_module()
inv.PACKAGE_RULE["default"] = "unclassified"
problems, notes, parsed = chk.compare(fx, inv, os.path.join(fx, "docs/Module-and-Service-Catalog.md"))
print("problems=%r notes=%r parsed=%d" % (problems, notes, parsed))
sys.exit(0 if not problems and any("unclassified package" in n for n in notes) else 1)
PYEOF
)"; then ok "EXTRA_package_issue_coupling: a real unclassified-package issue from internal work item 1399 stays a note"
else bad "EXTRA_package_issue_coupling: internal work item 1399's unclassified-package issue is no longer recognised"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -5; fi

# EXTRA_catalogue_containment — --catalogue must resolve inside --root; a path or symlink out is refused
# before anything is read (RA-A2 C11).
fixture; printf '# outside\n' > "$WORK/outside.md"
run --catalogue ../outside.md; expect "EXTRA_catalogue_containment: a --catalogue path outside --root is refused" 1 "resolves outside --root"
ln -s "$WORK/outside.md" "$FX/docs"/link.md
run --catalogue "docs"/link.md; expect "EXTRA_catalogue_containment: a --catalogue symlink pointing outside --root is refused" 1 "resolves outside --root"

# EXTRA_real_repo_catalogue_side_clean — on the real repo, every remaining failure must be an
# inventory-side open decision (owner classification), never a catalogue-side mismatch. A missing
# catalogue or zero parsed entries is a FAIL, never a skip (RA-A2 C9).
if [ ! -f "$ROOT/$CAT" ]; then
    bad "EXTRA_real_repo_catalogue_side_clean: $CAT is missing"
else
    out="$(python3 "$CHK" --root "$ROOT" --catalogue-only 2>&1)"; rc=$?
    parsed="$(printf '%s\n' "$out" | sed -nE 's/.* ([0-9]+) entries parsed.*/\1/p' | tail -1)"
    if [ -z "$parsed" ] || [ "$parsed" -lt 1 ]; then
        bad "EXTRA_real_repo_catalogue_side_clean: no catalogue entries were parsed (${parsed:-none})"
        printf '%s\n' "$out" | tail -3 | sed 's/^/         /'
    elif [ "$rc" -ne 0 ]; then
        bad "EXTRA_real_repo_catalogue_side_clean: the committed catalogue disagrees with the inventory"
        printf '%s\n' "$out" | grep -E '^catalogue-check: FAIL' | sed 's/^/         /' | head -10
    else ok "EXTRA_real_repo_catalogue_side_clean: $parsed entries parsed; every real-repo failure is an inventory-side open decision"; fi
fi

echo
echo "passed: $PASS  failed: $FAIL"
[ "$FAIL" -eq 0 ]
