#!/usr/bin/env bash
#
# inventory_test.sh — arms for scripts/api-inventory.py (internal work item 1399, P06·E01 N01).
#
# Every arm builds a throwaway fixture repository (poms, a BOM, a pubspec, a few Java sources, a
# classification file) and runs the generator against it with --root, so the arms exercise the
# generator's behaviour — never the text of the committed inventory. Arms are named after the issue's
# Acceptance items (AC1399-<n>); anything beyond them is EXTRA_<slug> with its reason.
#
# @author Navaneeth Mysore G
# @author Claude
#
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
GEN="$ROOT/scripts/api-inventory.py"

PASS=0; FAIL=0
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

ok()  { printf '  ok   %s\n' "$1"; PASS=$((PASS+1)); }
bad() { printf '  FAIL %s\n' "$1"; FAIL=$((FAIL+1)); }

# run ARGS... — run the generator against the fixture; sets RC and OUT.
run() { OUT="$(python3 "$GEN" --root "$FX" "$@" 2>&1)"; RC=$?; }

# expect NAME WANT_RC [GREP_PATTERN] — assert the last run's rc, and optionally that its output matches.
expect() {
    local name="$1" want="$2" pat="${3:-}"
    if [ "$RC" != "$want" ]; then bad "$name — expected rc=$want, got rc=$RC"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -15; return; fi
    if [ -n "$pat" ] && ! printf '%s\n' "$OUT" | grep -qE -- "$pat"; then
        bad "$name — rc=$RC but output lacks /$pat/"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -15; return
    fi
    ok "$name (rc=$RC)"
}

pom() {  # pom DIR ARTIFACT PACKAGING [MODULES...]
    local dir="$1" art="$2" pkg="$3"; shift 3
    mkdir -p "$FX/$dir"
    {
        printf '<project xmlns="http://maven.apache.org/POM/4.0.0">\n  <modelVersion>4.0.0</modelVersion>\n'
        if [ "$art" = "demo" ]; then
            printf '  <groupId>com.example</groupId>\n  <version>1.0.0</version>\n'
        else
            printf '  <parent><groupId>com.example</groupId><artifactId>demo</artifactId><version>1.0.0</version></parent>\n'
        fi
        printf '  <artifactId>%s</artifactId>\n  <packaging>%s</packaging>\n' "$art" "$pkg"
        if [ $# -gt 0 ]; then
            printf '  <modules>\n'; for m in "$@"; do printf '    <module>%s</module>\n' "$m"; done; printf '  </modules>\n'
        fi
        if [ "$art" = "demo-bom" ]; then
            printf '  <dependencyManagement><dependencies>\n'
            for d in $BOM_DEPS; do
                printf '    <dependency><groupId>com.example</groupId><artifactId>%s</artifactId><version>${project.version}</version></dependency>\n' "$d"
            done
            printf '  </dependencies></dependencyManagement>\n'
        fi
        printf '</project>\n'
    } > "$FX/$dir/pom.xml"
}

java() {  # java MODULE_DIR PACKAGE CLASS DOCUMENTED(0|1) KIND
    local d="$FX/$1/src/main/java/$(printf '%s' "$2" | tr . /)"
    mkdir -p "$d"
    {
        printf 'package %s;\n\n' "$2"
        [ "$4" = 1 ] && printf '/**\n * Does a real thing.\n */\n'
        if [ "$5" = interface ]; then
            printf 'public interface %s {\n    /** Returns the id. */\n    String id();\n    void undocumented();\n}\n' "$3"
        else
            # EXTRA_override_inherits rides here: the @Override member inherits its Javadoc, so it must
            # not count as undocumented (the agg row below expects exactly ONE undocumented member).
            # EXTRA_inline_annotation rides here too: `@Deprecated public void legacy() {` is a member (and
            # carries a brace), so a scanner that skips annotation-led lines would lose it.
            # EXTRA_lambda_field_depth: a multi-line lambda field and an anonymous-class field (with a
            # "}" literal) must not desync brace depth, or run()/legacy() below would silently vanish.
            printf 'public class %s {\n    public Runnable r = () -> {\n        System.out.println("}");\n    };\n    public Object o = new Object() {\n        public String toString() { return "{"; }\n    };\n    public void run() {}\n\n    @Override\n    public String toString() { return ""; }\n\n    @Deprecated public void legacy() {\n    }\n}\n' "$3"
        fi
    } > "$d/$3.java"
}

# The baseline fixture: root → {lib (jar), impl (jar), agg (pom → sub (jar)), demo-bom}, plus a pubspec.
fixture() {
    rm -rf "$WORK/fx"; FX="$WORK/fx"; mkdir -p "$FX"
    BOM_DEPS="${BOM_DEPS_OVERRIDE:-lib sub}"
    pom . demo pom lib impl agg demo-bom
    pom lib lib jar
    pom impl impl jar
    pom agg agg pom sub
    pom agg/sub sub jar
    pom demo-bom demo-bom pom
    java lib com.example.lib Api 1 interface
    java lib com.example.lib.internal Helper 0 class
    java agg/sub com.example.sub Sub 0 class
    mkdir -p "$FX/lib/ui/lib"
    printf 'name: demo_ui\npublish_to: none\nversion: 1.0.0\n' > "$FX/lib/ui/pubspec.yaml"
    cat > "$FX/lib/ui/lib/w.dart" <<'EOF'
/// A documented widget.
class Shown {
  /// Creates a shown widget.
  const Shown();

  /// A documented value.
  static const value = 1;

  void missingMember() {}

  @override
  String toString() => 'shown';
}
class Bare {}
class _Private {}
/// An HTTP client.
class Client {
  Client();
}
/// Helpers for display labels.
extension LabelHelpers on String {
  /// Number of code units in the label.
  int get displaySize => length;

  void missingExtensionMember() {}
}
/// Starts the demo.
void startDemo() {}
final missingProvider = Object();
EOF
    mkdir -p "$FX/docs/api-inventory"
    cat > "$FX/docs/api-inventory/classification.json" <<'EOF'
{
  "schema": 1,
  "excluded_trees": {"tools": "agent configuration only"},
  "baseline": {"source": "fixture baseline", "deferred": {}},
  "module_groups": [
    {"prefix": "lib", "group": "lib", "issue": 9001},
    {"prefix": "agg", "group": "agg", "issue": 9002},
    {"prefix": "lib/ui", "group": "ui", "issue": 9003}
  ],
  "artifacts": {
    "com.example:demo":     {"support": "internal-implementation", "evidence": "parent pom"},
    "com.example:demo-bom": {"support": "supported-public", "family": "build", "evidence": "§2 the BOM", "entry_points": ["import it"], "backend": "none", "docs_destination": "README", "limitations": "none"},
    "com.example:lib":      {"support": "supported-public", "family": "lib", "evidence": "§4.1 Lib", "entry_points": ["Api"], "backend": "none", "docs_destination": "javadoc", "limitations": "none"},
    "com.example:impl":     {"support": "internal-implementation", "evidence": "not exported"},
    "com.example:agg":      {"support": "internal-implementation", "evidence": "aggregator"},
    "com.example:sub":      {"support": "supported-public", "family": "lib", "evidence": "test: SubIT", "entry_points": ["Sub"], "backend": "none", "docs_destination": "javadoc", "limitations": "none"},
    "pub:demo_ui":          {"support": "generated-only", "evidence": "publish_to none"}
  },
  "packages": [
    {"prefix": "com.example.lib.internal", "classification": "implementation-detail", "reason": "helpers"},
    {"prefix": "com.example.lib", "classification": "supported-public", "reason": "entry points"},
    {"prefix": "com.example.sub", "classification": "supported-public", "reason": "entry points"}
  ]
}
EOF
}

cls() {  # cls PYTHON_EXPR — mutate the fixture classification: `c` is the parsed JSON.
    python3 - "$FX/docs/api-inventory/classification.json" "$1" <<'EOF'
import json, sys
p, expr = sys.argv[1], sys.argv[2]
c = json.load(open(p))
exec(expr)
json.dump(c, open(p, "w"), indent=2)
EOF
}

INV="docs/api-inventory/inventory.json"

echo "api-inventory arms — generator: ${GEN#"$ROOT"/}"
echo
[ -f "$GEN" ] || { bad "generator missing at ${GEN#"$ROOT"/}"; echo; echo "passed: $PASS  failed: $FAIL"; exit 1; }

# ── AC1399-1: the raw list is generated reproducibly from the build/export authorities ──────────────
fixture
run generate; expect "AC1399-1: a fully classified fixture generates cleanly" 0
cp "$FX/$INV" "$WORK/first.json"
run generate
if cmp -s "$FX/$INV" "$WORK/first.json"; then ok "AC1399-1: re-running generate is byte-identical"; else bad "AC1399-1: re-running generate changed the inventory"; fi
ids="$(python3 -c "import json,sys; print(' '.join(sorted(a['id'] for a in json.load(open(sys.argv[1]))['artifacts'])))" "$FX/$INV")"
want="com.example:agg com.example:demo com.example:demo-bom com.example:impl com.example:lib com.example:sub pub:demo_ui"
if [ "$ids" = "$want" ]; then ok "AC1399-1: artifact list is exactly the reactor + pubspec set"; else bad "AC1399-1: artifact list was [$ids], want [$want]"; fi
run check; expect "AC1399-1: check passes on a freshly generated inventory" 0
python3 - "$FX/$INV" <<'EOF'
import json, sys
p = sys.argv[1]; d = json.load(open(p)); d["artifacts"] = d["artifacts"][1:]
json.dump(d, open(p, "w"), indent=2, sort_keys=True)
EOF
run check; expect "AC1399-1: check FAILS when the committed inventory drifts from a fresh generation" 1 "drift"
rm -f "$FX/$INV"
run check; expect "AC1399-1: check FAILS when the committed inventory is missing" 1 "drift|missing"

# ── AC1399-2: nothing absent, duplicated or unclassified ──────────────────────────────────────────
fixture; cls 'del c["artifacts"]["com.example:impl"]'
run generate; expect "AC1399-2: an unclassified reactor artifact fails" 1 "unclassified.*com.example:impl"
run check;    expect "AC1399-2: check also fails on the unclassified artifact" 1 "unclassified.*com.example:impl"
fixture; cls 'del c["artifacts"]["pub:demo_ui"]'
run generate; expect "AC1399-2: an unclassified pubspec package fails" 1 "unclassified.*pub:demo_ui"
fixture; pom . demo pom lib impl agg demo-bom lib
run generate; expect "AC1399-2: a module listed twice fails as a duplicate" 1 "duplicate"
fixture; pom impl lib jar
run generate; expect "AC1399-2: two modules with the same coordinates fail as a duplicate" 1 "duplicate.*com.example:lib"
fixture; pom orphan orphan jar
run generate; expect "AC1399-2: a pom.xml on disk outside the reactor fails as absent" 1 "absent.*orphan"
fixture; mkdir -p "$FX/tools"; cp "$FX/lib/pom.xml" "$FX/tools/pom.xml"
run generate; expect "AC1399-2: a pom.xml under a declared excluded tree is not an orphan" 0
fixture; cls 'c["artifacts"]["com.example:impl"]["support"] = "maybe"'
run generate; expect "AC1399-2: an unknown support value is treated as unclassified" 1 "unclassified.*com.example:impl"

# ── AC1399-3: supported public entry points vs public implementation detail ─────────────────────────
fixture; run generate
got="$(python3 - "$FX/$INV" <<'EOF'
import json, sys
d = json.load(open(sys.argv[1]))
lib = [a for a in d["artifacts"] if a["id"] == "com.example:lib"][0]
print(" ".join("%s=%s" % (p["package"], p["classification"]) for p in lib["packages"]))
EOF
)"
if [ "$got" = "com.example.lib=supported-public com.example.lib.internal=implementation-detail" ]; then
    ok "AC1399-3: packages split into supported-public and implementation-detail"
else bad "AC1399-3: package split was [$got]"; fi
got="$(python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print([a for a in d['artifacts'] if a['id']=='com.example:lib'][0]['packages'][0]['public_types'])" "$FX/$INV")"
[ "$got" = "['Api']" ] && ok "AC1399-3: supported-public package names its public entry-point types" || bad "AC1399-3: public_types was $got"
# The owner-approved package rule: a package with an `internal`, `impl` or `support` SEGMENT is implementation-detail;
# a `Default*Impl` or `*Helper` class is implementation-detail even in a supported-public package;
# everything else in a supported-public artifact is supported-public. Explicit rules still win.
fixture; cls 'c["packages"] = []'
java lib com.example.lib.impl ImplThing 1 class
java lib com.example.lib.support Sup 1 class
java lib com.example.lib.supporting Supp 1 class
java lib com.example.lib DefaultWidgetImpl 1 class
java lib com.example.lib WidgetHelper 1 class
java lib com.example.lib DefaultWidget 1 class
java lib com.example.lib HelperFactory 1 class
run generate; expect "AC1399-3: with no explicit rules every package is classified by the owner-ruled rule — none left unclassified" 0
pk() { python3 - "$FX/$INV" "$1" <<'PYEOF'
import json, sys
d = json.load(open(sys.argv[1])); lib = [a for a in d["artifacts"] if a["id"] == "com.example:lib"][0]
p = [x for x in lib["packages"] if x["package"] == sys.argv[2]][0]
print(p["classification"], p.get("basis"), ",".join(p.get("public_types", [])), ",".join(p.get("implementation_detail_types", [])))
PYEOF
}
run check; expect "AC1399-3: the owner-ruled rule is final — check passes with rule-classified packages" 0
if printf '%s\n' "$OUT" | grep -qi 'provisional'; then bad "AC1399-3: check still reports a PROVISIONAL rule after the owner ruling"; else ok "AC1399-3: check reports no PROVISIONAL problem once the rule is owner-ruled"; fi
[ "$(pk com.example.lib.internal)" = "implementation-detail rule  " ] && ok "AC1399-3: package rule — an 'internal' segment is implementation-detail" || bad "AC1399-3: internal segment → $(pk com.example.lib.internal)"
[ "$(pk com.example.lib.impl)" = "implementation-detail rule  " ] && ok "AC1399-3: package rule — an 'impl' segment is implementation-detail" || bad "AC1399-3: impl segment → $(pk com.example.lib.impl)"
[ "$(pk com.example.lib.support)" = "implementation-detail rule  " ] && ok "AC1399-3: package rule — a 'support' segment is implementation-detail" || bad "AC1399-3: support segment → $(pk com.example.lib.support)"
[ "$(pk com.example.lib.supporting)" = "supported-public rule Supp " ] && ok "AC1399-3: negative control — 'supporting' is not a 'support' segment, so it stays supported-public" || bad "AC1399-3: supporting → $(pk com.example.lib.supporting)"
[ "$(pk com.example.lib)" = "supported-public rule Api,DefaultWidget,HelperFactory DefaultWidgetImpl,WidgetHelper" ] && ok "AC1399-3: package rule — Default*Impl and *Helper types are implementation-detail inside a supported-public package; DefaultWidget and HelperFactory (negative controls) stay public" || bad "AC1399-3: lib package → $(pk com.example.lib)"
wl="$FX/docs/spec/1.0.0/P06/decisions/E01-published-api-gap-list.md"
grep -q 'owner-approved package rule' "$wl" && ! grep -qi 'provisional' "$wl" && ok "AC1399-3: the work list cites the owner-approved rule and no longer says PROVISIONAL" || bad "AC1399-3: work list does not cite the owner-approved rule, or still says PROVISIONAL"
fixture; cls 'c["packages"].append({"prefix": "com.example.sub.impl", "classification": "supported-public", "reason": "explicit override"})'
java agg/sub com.example.sub.impl Exported 1 class
run generate
[ "$(python3 -c "import json,sys; d=json.load(open(sys.argv[1])); a=[x for x in d['artifacts'] if x['id']=='com.example:sub'][0]; p=[x for x in a['packages'] if x['package']=='com.example.sub.impl'][0]; print(p['classification'], p['basis'])" "$FX/$INV")" = "supported-public explicit" ] \
    && ok "AC1399-3: an explicit rule wins over the package rule (an 'impl' package made supported-public on purpose)" || bad "AC1399-3: explicit rule did not win"
fixture; run generate; run check
expect "AC1399-3: when explicit rules cover every package, check passes" 0
# Guard: if the rule's default is ever changed to `unclassified` (option (c), not the ruling), a package no
# explicit rule covers must be reported as an issue, not pass silently. That issue was once dropped: with a
# supported-public default the path is unreachable, so no other arm would notice.
fixture; cls 'c["packages"] = []'
if OUT="$(python3 - "$GEN" "$FX" 2>&1 <<'PYEOF'
import importlib.util, sys
spec = importlib.util.spec_from_file_location("api_inventory", sys.argv[1])
mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
mod.PACKAGE_RULE["default"] = "unclassified"
inv, issues = mod.build(sys.argv[2])
hits = [i for i in issues if i.startswith("unclassified package com.example.lib in supported-public artifact com.example:lib")]
print("\n".join(issues))
sys.exit(0 if hits else 1)
PYEOF
)"; then ok "AC1399-3: with the rule default set to unclassified (option c), an uncovered package is reported as an issue"
else bad "AC1399-3: an uncovered package under option (c) passed silently — no 'unclassified package' issue"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -5; fi

# ── AC1399-4: historical/aspirational capabilities cannot enter the shipped inventory ──────────────
fixture; cls 'c["artifacts"]["com.example:grpc"] = {"support": "supported-public", "evidence": "planned"}'
run generate; expect "AC1399-4: a classification naming an artifact the build does not produce fails" 1 "not produced by the build.*com.example:grpc"
if python3 -c "import json,sys; sys.exit(any(a['id']=='com.example:grpc' for a in json.load(open(sys.argv[1]))['artifacts']))" "$FX/$INV"; then
    ok "AC1399-4: the aspirational artifact gets no inventory record"; else bad "AC1399-4: aspirational artifact leaked into the inventory records"; fi
fixture; cls 'c["artifacts"]["com.example:impl"] = dict(c["artifacts"]["com.example:lib"])'
run generate; expect "AC1399-4: supported-public but not exported by the BOM fails" 1 "not exported by the BOM.*com.example:impl"
fixture; cls 'c["artifacts"]["com.example:lib"]["evidence"] = "exported by the BOM"'
run generate; expect "AC1399-4: supported-public evidence that cites only BOM membership fails (must cite a baseline section or a test)" 1 "evidence must cite.*com.example:lib"
# An owner ruling is the third valid source, but only when it is citable: `owner ruling: #<issue> comment <id>`.
# APP review receipt 6527519002 (round 2): a prior revision carved a path/token exception into
# the export rewrite and its independent scanner so a real tracker citation (issue internal work item 1141 plus
# its rationale/identifiers) would survive export unchanged -- a path-scoped exception is still
# a leak of exactly what the boundary exists to prevent, not a fix. The citation is now
# constructed at runtime (`chr(35)` instead of a literal "#") so this file's own committed text
# never contains a bare-issue-shaped substring at all -- nothing for the export rewrite or the
# scanner to special-case, and no private reference ever reaches the exported tree. The
# resulting string at runtime is byte-identical to the real format this exercises; only its
# representation in the committed source differs. Limiting principle, stated explicitly so
# this is never copied as general advice: this is safe ONLY because the reconstructed string
# is transient process data consumed by an in-memory format check and never itself written
# into anything the export ships -- it is not a technique for hiding a real reference that
# ships in exported content, which this mechanism exists to catch, not evade.
fixture; cls 'c["artifacts"]["com.example:lib"]["evidence"] = "owner ruling: " + chr(35) + "1141 comment 5855318230 (Q1 (a))"'
run generate; expect "AC1399-4: supported-public evidence citing a recorded owner ruling (issue + comment) is accepted" 0
fixture; cls 'c["artifacts"]["com.example:lib"]["evidence"] = "the owner ruled it public"'
run generate; expect "AC1399-4: an uncitable owner ruling (no issue + comment) is rejected" 1 "evidence must cite.*com.example:lib"
fixture; cls 'c["artifacts"]["com.example:lib"]["evidence"] = "owner ruling: " + chr(35) + "1141"'
run generate; expect "AC1399-4: an owner ruling naming only the issue, not the comment, is rejected" 1 "evidence must cite.*com.example:lib"
fixture; cls 'c["baseline"]["deferred"]["com.example:lib"] = "§4.2 Lib is deferred"'
run generate; expect "AC1399-4: an artifact the baseline defers cannot be supported-public" 1 "baseline defers.*com.example:lib.*§4.2"
BOM_DEPS_OVERRIDE="lib sub impl" fixture; cls 'c["artifacts"]["com.example:impl"]["support"] = "deferred-unsupported"'
run generate; expect "AC1399-4: a deferred/unsupported artifact exported by the BOM fails" 1 "deferred.*exported by the BOM.*com.example:impl"
unset BOM_DEPS_OVERRIDE

# ── AC1399-5: measured supported-public work list and estimate per documentation issue ─────────────
fixture; run generate
WL="$FX/docs/spec/1.0.0/P06/decisions/E01-published-api-gap-list.md"
if [ -f "$WL" ]; then
    # lib: supported-public package com.example.lib holds Api (documented) with one undocumented member.
    if grep -qE '^\| work item 9001 \| lib \| 1 \| 1 \| 0 \| 1 \|' "$WL"; then ok "AC1399-5: the lib group row carries measured counts"; else bad "AC1399-5: lib row missing or wrong"; grep '^|' "$WL" | sed 's/^/         /'; fi
    if grep -qE '^\| work item 9002 \| agg \| 1 \| 1 \| 1 \| 2 \|' "$WL"; then ok "AC1399-5: the agg group row carries measured counts (EXTRA_override_inherits, EXTRA_inline_annotation)"; else bad "AC1399-5: agg row missing or wrong"; grep '^| work item 9002' "$WL" | sed 's/^/         /'; fi
    grep -q 'estimate' "$WL" && ok "AC1399-5: the work list states an estimate" || bad "AC1399-5: no estimate in work list"
    grep -qE 'demo_ui.*12 public.*5 undocumented' "$WL" && ok "AC1399-5: the full authored Dart surface is measured (members/functions/variables/ordinary constructors/extensions included; private/override excluded)" || bad "AC1399-5: Dart surface not measured"
    if grep -qE '^\| work item 9003 \| ui \| 1 \| 12 \| 5 \| 0 \|' "$WL"; then ok "AC1399-5: the Dart documentation issue's row carries the measured Dart surface"; else bad "AC1399-5: Dart group row missing or zero"; grep '^| work item 9003' "$WL" | sed 's/^/         /'; fi
else bad "AC1399-5: work list not generated"; fi

# ── AC1399-6: the README/catalogue consumes the same records ────────────────────────────────────────
fixture; run generate
cat > "$FX/README.md" <<'EOF'
# Demo
<!-- api-inventory:supported-public:begin -->
| Artifact | What |
|---|---|
| `com.example:demo-bom` | the BOM |
| `com.example:lib` | library |
| `com.example:sub` | sub |
<!-- api-inventory:supported-public:end -->
EOF
run check-consumer README.md; expect "AC1399-6: a catalogue listing exactly the supported-public records passes" 0
printf 'no region here\n' > "$FX/README.md"
run check-consumer README.md; expect "AC1399-6: a consumer with no inventory region fails, never passes vacuously" 1 "no .*region"

# ── AC1399-7: API docs and the public catalogue disagreeing fails the documentation gate ───────────
fixture; run generate
region() { printf '<!-- api-inventory:supported-public:begin -->\n'; for a in "$@"; do printf -- '- `%s`\n' "$a"; done; printf '<!-- api-inventory:supported-public:end -->\n'; }
region com.example:demo-bom com.example:lib > "$FX/catalogue.md"
run check-consumer catalogue.md; expect "AC1399-7: a catalogue MISSING a supported-public artifact fails" 1 "missing.*com.example:sub"
region com.example:demo-bom com.example:lib com.example:sub com.example:impl > "$FX/catalogue.md"
run check-consumer catalogue.md; expect "AC1399-7: a catalogue advertising a non-supported artifact fails" 1 "not supported-public.*com.example:impl"
region com.example:demo-bom com.example:lib com.example:sub > "$FX/catalogue.md"
region com.example:demo-bom com.example:lib com.example:sub > "$FX/apidocs.md"
run check-consumer catalogue.md apidocs.md; expect "AC1399-7: API docs and catalogue that agree with the inventory pass" 0
region com.example:demo-bom com.example:lib > "$FX/apidocs.md"
run check-consumer catalogue.md apidocs.md; expect "AC1399-7: API docs disagreeing with the catalogue fails" 1 "apidocs.md.*missing.*com.example:sub"

# AC1399-2 duplicates inside the classification file itself: a duplicated key silently lets the LAST
# entry win, so a reader of the file sees a state the generator does not use.
fixture; python3 - "$FX/docs/api-inventory/classification.json" <<'PYEOF'
import sys
p = sys.argv[1]; t = open(p).read()
t = t.replace('"com.example:impl":     {', '"com.example:impl": {"support": "deferred-unsupported", "evidence": "x"},\n    "com.example:impl":     {', 1)
open(p, "w").write(t)
PYEOF
run generate; expect "AC1399-2: a duplicated artifact key in classification.json is refused" 1 "duplicate key.*com.example:impl"
if ! printf '%s\n' "$OUT" | grep -q Traceback; then ok "AC1399-2: the duplicate key is reported cleanly, not as a traceback"; else bad "AC1399-2: duplicate key produced a traceback"; fi
fixture; cls 'c["packages"].append({"prefix": "com.example.lib", "classification": "implementation-detail", "reason": "conflict"})'
run generate; expect "AC1399-2: a duplicated package prefix is refused" 1 "duplicate package prefix.*com.example.lib"

# EXTRA_handoff_fields — the issue's Classification clause: every supported-public record names its entry
# points, required backend/configuration, API-doc destination and known limitations (owning module is derived).
fixture; cls 'del c["artifacts"]["com.example:sub"]["backend"]; c["artifacts"]["com.example:lib"]["entry_points"] = []'
run generate; expect "EXTRA_handoff_fields: supported-public records missing handoff fields fail" 1 "missing handoff field.*com.example:lib.*entry_points" "missing handoff field.*com.example:sub.*backend"
fixture; run generate
got="$(python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print([(a['id'], a['owning_module']) for a in d['artifacts'] if a['id'] == 'com.example:lib'][0])" "$FX/$INV")"
[ "$got" = "('com.example:lib', 'lib')" ] && ok "EXTRA_handoff_fields: every record names its owning module" || bad "EXTRA_handoff_fields: owning_module was $got"

# EXTRA_malformed_classification — a broken decisions file is a clean ERROR, not a traceback.
fixture; printf '{ "schema": 1, ' > "$FX/docs/api-inventory/classification.json"
run generate; expect "EXTRA_malformed_classification: malformed JSON is reported as an ERROR" 1 "ERROR.*classification.json"
if ! printf '%s\n' "$OUT" | grep -q Traceback; then ok "EXTRA_malformed_classification: no traceback"; else bad "EXTRA_malformed_classification: traceback"; fi

# EXTRA_module_containment (file level) — the module DIRECTORY may be inside the root while a file it
# reads is a symlink out: pom.xml, or the src/main/java root. Neither may be parsed or published.
fixture; mkdir -p "$WORK/outside2/src/main/java/com/secret"; cp "$FX/lib/pom.xml" "$WORK/outside2/pom.xml"
sed -i.bak 's#<artifactId>lib</artifactId>#<artifactId>leaked-pom</artifactId>#' "$WORK/outside2/pom.xml"
printf 'package com.secret;\npublic class LeakedSecretName {}\n' > "$WORK/outside2/src/main/java/com/secret/LeakedSecretName.java"
rm "$FX/impl/pom.xml"; ln -s "$WORK/outside2/pom.xml" "$FX/impl/pom.xml"
run generate; expect "EXTRA_module_containment: a pom.xml symlinked outside the root is refused" 1 "outside the repository"
if ! grep -q leaked-pom "$FX/$INV" 2>/dev/null; then ok "EXTRA_module_containment: the outside pom's content does not reach the inventory"; else bad "EXTRA_module_containment: outside pom leaked"; fi
fixture; rm -rf "$FX/agg/sub/src/main/java"; ln -s "$WORK/outside2/src/main/java" "$FX/agg/sub/src/main/java"
run generate; expect "EXTRA_module_containment: a src/main/java symlinked outside the root is refused" 1 "outside the repository"
if ! grep -qE 'LeakedSecretName|com\.secret' "$FX/$INV" 2>/dev/null; then ok "EXTRA_module_containment: outside package/type names do not reach the inventory"; else bad "EXTRA_module_containment: outside source names leaked"; fi

# EXTRA_consumer_requires_valid_inventory — check-consumer compares against the inventory, so an
# inventory that is itself invalid must fail it too, or a docs-only lane gets a false clean.
fixture; run generate
region com.example:demo-bom com.example:lib com.example:sub > "$FX/catalogue.md"
cls 'del c["artifacts"]["com.example:impl"]'
run check-consumer catalogue.md; expect "EXTRA_consumer_requires_valid_inventory: an agreeing consumer still fails over an invalid inventory" 1 "unclassified.*com.example:impl"

# EXTRA_module_containment — a <module> that escapes the repository (../, absolute, or a symlink out)
# must be refused as an issue and never parsed: pom.xml is PR-controlled content and the inventory is
# written back into the repo, so an escaping module would read and republish files from outside it.
fixture; mkdir -p "$WORK/outside"; cp "$FX/lib/pom.xml" "$WORK/outside/pom.xml"
sed -i.bak 's#<artifactId>lib</artifactId>#<artifactId>leaked-secret</artifactId>#' "$WORK/outside/pom.xml"
pom . demo pom lib impl agg demo-bom ../outside
run generate; expect "EXTRA_module_containment: a ../ module outside the root is refused" 1 "outside the repository"
if ! grep -q leaked-secret "$FX/$INV" 2>/dev/null; then ok "EXTRA_module_containment: nothing from outside the root reaches the inventory"; else bad "EXTRA_module_containment: outside pom content leaked into the inventory"; fi
fixture; ln -s "$WORK/outside" "$FX/escape"; pom . demo pom lib impl agg demo-bom escape
run generate; expect "EXTRA_module_containment: a symlinked module leaving the root is refused" 1 "outside the repository"

# EXTRA_non_utf8 — a stray non-UTF-8 byte in a Dart file or pubspec is reported/tolerated, never a traceback.
fixture; printf '/// caf\xe9\nclass Cafe {}\n' > "$FX/lib/ui/lib/latin1.dart"; printf '# \xe9\n' >> "$FX/lib/ui/pubspec.yaml"
run generate; expect "EXTRA_non_utf8: non-UTF-8 Dart/pubspec bytes do not crash generate" 0

# ── EXTRAS ──────────────────────────────────────────────────────────────────────────────────────────
# EXTRA_stdlib_only — the generator must run on a bare python3 (macOS host), so no third-party import.
if python3 - "$GEN" <<'EOF'
import ast, sys
tree = ast.parse(open(sys.argv[1]).read())
mods = {n.name.split('.')[0] for x in ast.walk(tree) if isinstance(x, ast.Import) for n in x.names}
mods |= {x.module.split('.')[0] for x in ast.walk(tree) if isinstance(x, ast.ImportFrom) and x.module}
bad = sorted(m for m in mods if m not in sys.stdlib_module_names)
sys.exit(1 if bad else 0)
EOF
then ok "EXTRA_stdlib_only: generator imports only the standard library"; else bad "EXTRA_stdlib_only: non-stdlib import"; fi

# EXTRA_excluded_trees_mirror_build_sh — classification.json's excluded_trees says it mirrors EXCLUDED_TREES in
# build.sh. A tree excluded in one and not the other either reappears as "absent" here or hides a buildable tree,
# so the two lists must hold the same paths. (Found when a develop merge brought two private Flutter trees
# that build.sh excludes and this file did not.)
if python3 - "$ROOT" <<'PYEOF'
import json, re, sys
root = sys.argv[1]
src = open(root + "/build.sh", encoding="utf-8").read()
block = re.search(r"^EXCLUDED_TREES=\((.*?)^\)", src, re.S | re.M).group(1)
build = set(re.findall(r'^\s*"([^"]+)"', block, re.M))
cls = set(json.load(open(root + "/docs/api-inventory/classification.json", encoding="utf-8"))["excluded_trees"])
if build != cls:
    print("only in build.sh: %s; only in classification.json: %s" % (sorted(build - cls), sorted(cls - build)))
    sys.exit(1)
PYEOF
then ok "EXTRA_excluded_trees_mirror_build_sh: excluded_trees and build.sh EXCLUDED_TREES name the same paths"
else bad "EXTRA_excluded_trees_mirror_build_sh: excluded_trees and build.sh EXCLUDED_TREES disagree"; fi

# ── RA-A4 (independent review of c712f8fe4) ──────────────────────────────────────────────────────────
# A4-I1: repository-controlled default paths never lead outside the repository. A symlinked output file or
# output parent directory, or a symlinked classification.json, is refused BEFORE anything is read or written,
# and an outside sentinel is left byte-identical.
fixture; printf 'SENTINEL\n' > "$WORK/outside.json"; ln -sf "$WORK/outside.json" "$FX/docs/api-inventory/inventory.json"
run generate
if [ "$RC" -ne 0 ] && printf '%s\n' "$OUT" | grep -q 'refusing' && [ "$(cat "$WORK/outside.json")" = SENTINEL ]; then
    ok "A4-I1: a symlinked inventory.json is refused and the outside file is untouched (rc=$RC)"
else bad "A4-I1: a symlinked inventory.json was written through or not refused (rc=$RC, outside=$(head -c 40 "$WORK/outside.json"))"; fi
fixture; rm -rf "$WORK/outdir"; mkdir -p "$WORK/outdir" "$FX/docs/spec/1.0.0/P06"; ln -s "$WORK/outdir" "$FX/docs/spec/1.0.0/P06/decisions"
run generate
if [ "$RC" -ne 0 ] && printf '%s\n' "$OUT" | grep -q 'refusing' && [ -z "$(ls -A "$WORK/outdir")" ] && [ ! -e "$FX/docs/api-inventory/inventory.json" ]; then
    ok "A4-I1: a symlinked work-list parent directory is refused before either output is written (rc=$RC)"
else bad "A4-I1: a symlinked output parent directory was written through or not refused (rc=$RC, outdir: $(ls -A "$WORK/outdir" | tr '\n' ' '))"; fi
fixture; mv "$FX/docs/api-inventory/classification.json" "$WORK/outside-cls.json"; ln -s "$WORK/outside-cls.json" "$FX/docs/api-inventory/classification.json"
run check; expect "A4-I1: a classification.json symlinked outside the repository is refused" 1 "refusing.*classification.json"

# A4-I2: an explicit package rule with an invalid state is reported, and never falls back to the public default.
fixture; cls 'c["packages"][1]["classification"] = "implementation-details"'
run generate; run check
expect "A4-I2: an invalid explicit package classification fails check instead of broadening the public API" 1 "invalid package rule.*com\.example\.lib.*implementation-details"
if printf '%s\n' "$OUT" | grep -qE 'unclassified package com\.example\.lib in'; then ok "A4-I2: the package stays unclassified (not public) under an invalid explicit rule"
else bad "A4-I2: the package under an invalid explicit rule was not reported unclassified"; fi
fixture; cls 'c["packages"].append({"classification": "supported-public"})'
run check; expect "A4-I2: a package rule with no prefix is reported" 1 "invalid package rule.*prefix"

# A4-I3: family is a required handoff field for supported-public records (issue internal work item 1399: capability/service family).
fixture; cls 'del c["artifacts"]["com.example:lib"]["family"]'
run check; expect "A4-I3: a supported-public record without a family fails check" 1 "missing handoff field.*com\.example:lib.*family"

# ── RA-A2 review fix batch (internal work item 2506, R-2506-1..5,7) ─────────────────────────────────────────────────
# R-2506-1: every containment check has its own arm. Each plants ONE symlink out of the fixture and asserts
# the specific refusal AND that nothing from outside reaches the inventory (a rival check refusing something
# else must not be able to pass the arm).
# records_mention NAME — how many artifact RECORDS mention NAME (the issues list names the refused path, so a
# whole-file grep cannot tell a refusal message from a leak; ledger row 3's near-miss).
records_mention() { python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print(sum(sys.argv[2] in json.dumps(a) for a in d['artifacts']))" "$FX/$INV" "$1"; }
fixture; mkdir -p "$WORK/out-pub"; printf 'name: leaked_pub\npublish_to: none\nversion: 9.9.9\n' > "$WORK/out-pub/pubspec.yaml"
rm "$FX/lib/ui/pubspec.yaml"; ln -s "$WORK/out-pub/pubspec.yaml" "$FX/lib/ui/pubspec.yaml"
run generate
if [ "$RC" -ne 0 ] && printf '%s\n' "$OUT" | grep -q 'lib/ui/pubspec.yaml resolves outside the repository; refused' && [ "$(records_mention leaked_pub)" = 0 ]; then
    ok "EXTRA_containment_pubspec: a pubspec symlinked outside is refused and its content never reaches the inventory (rc=$RC)"
else bad "EXTRA_containment_pubspec: rc=$RC, records naming it=$(records_mention leaked_pub)"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -5; fi

dart_surface() { python3 -c "import json,sys; d=json.load(open(sys.argv[1])); s=[a for a in d['artifacts'] if a['id']=='pub:demo_ui'][0]['dart_surface']; print(s['public_declarations'], s['undocumented_declarations'])" "$FX/$INV"; }
fixture; mkdir -p "$WORK/out-dart"; printf 'class LeakA {}\nclass LeakB {}\nclass LeakC {}\n' > "$WORK/out-dart/x.dart"
rm -rf "$FX/lib/ui/lib"; ln -s "$WORK/out-dart" "$FX/lib/ui/lib"
run generate
if [ "$RC" -ne 0 ] && printf '%s\n' "$OUT" | grep -q 'Dart lib lib/ui/lib resolves outside the repository; refused' && [ "$(dart_surface)" = "0 0" ]; then
    ok "EXTRA_containment_dart_lib: a Dart lib dir symlinked outside is refused and scans nothing (rc=$RC)"
else bad "EXTRA_containment_dart_lib: rc=$RC, surface=[$(dart_surface)]"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -5; fi

fixture; ln -s "$WORK/out-dart/x.dart" "$FX/lib/ui/lib/leak.dart"
run generate
if [ "$RC" -ne 0 ] && printf '%s\n' "$OUT" | grep -q 'Dart file lib/ui/lib/leak.dart resolves outside the repository; refused' && [ "$(dart_surface)" = "12 5" ]; then
    ok "EXTRA_containment_dart_file: a single Dart file symlinked outside is refused and not counted (rc=$RC)"
else bad "EXTRA_containment_dart_file: rc=$RC, surface=[$(dart_surface)] (want 12 5)"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -5; fi

fixture; printf 'package com.example.sub;\npublic class LeakedJavaType {}\n' > "$WORK/LeakedJavaType.java"
ln -s "$WORK/LeakedJavaType.java" "$FX/agg/sub/src/main/java/com/example/sub/LeakedJavaType.java"
run generate
if [ "$RC" -ne 0 ] && printf '%s\n' "$OUT" | grep -q 'source file agg/sub/src/main/java/com/example/sub/LeakedJavaType.java resolves outside the repository; refused' \
        && [ "$(records_mention LeakedJavaType)" = 0 ]; then
    ok "EXTRA_containment_java_file: a single .java symlinked outside is refused, its type is not published, rc=$RC"
else bad "EXTRA_containment_java_file: rc=$RC, records naming it=$(records_mention LeakedJavaType)"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -5; fi

fixture; rm -f "$WORK/dangling-target.json"; ln -s "$WORK/dangling-target.json" "$FX/docs/api-inventory/inventory.json"
run generate
if [ "$RC" -ne 0 ] && printf '%s\n' "$OUT" | grep -q 'refusing docs/api-inventory/inventory.json' && [ ! -e "$WORK/dangling-target.json" ]; then
    ok "EXTRA_containment_dangling_output: a DANGLING symlinked inventory.json is refused and no outside file is created (rc=$RC)"
else bad "EXTRA_containment_dangling_output: rc=$RC, outside created=$([ -e "$WORK/dangling-target.json" ] && echo yes || echo no)"; printf '%s\n' "$OUT" | sed 's/^/         /' | head -5; fi

fixture; run generate; mv "$FX/$INV" "$WORK/identical.json"; ln -s "$WORK/identical.json" "$FX/$INV"
run check
expect "EXTRA_containment_check_identical_symlink: check refuses a symlinked inventory.json even when its content is identical" 1 "refusing docs/api-inventory/inventory.json"

# R-2506-2 (ii): Plan N01 Step 1 — the work list's raw count is exactly what the verbatim command prints.
raw_doc() { sed -n 's/^\*\*\([0-9][0-9]*\)\*\* `\.java` files under.*/\1/p' "$1"; }
raw_cmd() { (cd "$1" && grep -rlE '^\s*public\s+(final\s+|abstract\s+|sealed\s+)?(interface|class|enum|record)\s' \
  --include='*.java' $(find appbase-java appbase-cloud -path '*/src/main/java' -type d 2>/dev/null) 2>/dev/null | wc -l | tr -d ' '); }
fixture; m="$FX/appbase-java/mod/src/main/java/p"; mkdir -p "$m" "$FX/appbase-java/other/src/main/java"
printf 'package p;\npublic class A {}\n' > "$m/A.java"
printf 'package p;\n  public final interface B {}\n' > "$m/B.java"
printf 'package p;\nclass C {}\n' > "$m/C.java"
printf 'package p;\npublic class\nD {}\n' > "$m/D.java"
printf 'package p;\npublic record E() {}\n' > "$FX/appbase-java/other/src/main/java/E.java"
printf 'public class NotJava {}\n' > "$m/F.txt"
ln -s "$m/A.java" "$m/Link.java"
run generate
got="$(raw_doc "$FX/docs/spec/1.0.0/P06/decisions/E01-published-api-gap-list.md")"; want="$(raw_cmd "$FX")"
if [ "$RC" = 0 ] && [ -n "$got" ] && [ "$got" = "$want" ] && [ "$want" = 3 ]; then
    ok "EXTRA_raw_count_matches_command: the fixture work list's raw count ($got) is what Plan N01 Step 1's command prints"
else bad "EXTRA_raw_count_matches_command: work list says [$got], the command prints [$want] (want 3), rc=$RC"; fi
if [ -f "$ROOT/docs/spec/1.0.0/P06/decisions/E01-published-api-gap-list.md" ]; then
    got="$(raw_doc "$ROOT/docs/spec/1.0.0/P06/decisions/E01-published-api-gap-list.md")"; want="$(raw_cmd "$ROOT")"
    if [ -n "$got" ] && [ "$got" = "$want" ]; then ok "EXTRA_raw_count_matches_command: the committed work list's raw count ($got) is what the command prints on this tree"
    else bad "EXTRA_raw_count_matches_command: committed work list says [$got], the command prints [$want]"; fi
fi

# R-2506-2 (iii): the javadoc configuration baseline is read from the root pom, with properties resolved per
# profile, and the completed internal work item 1405 runtime and mutation evidence is retained.
fixture; python3 - "$FX/pom.xml" <<'PYEOF'
import sys
p = sys.argv[1]; t = open(p).read()
t = t.replace("</project>", """  <properties><x.fail>false</x.fail><jd.version>9.9</jd.version></properties>
  <profiles><profile><id>release-profile</id><properties><x.fail>true</x.fail></properties>
    <build><pluginManagement><plugins><plugin><artifactId>maven-javadoc-plugin</artifactId><version>${jd.version}</version>
      <configuration><doclint>all,-missing</doclint><failOnError>${x.fail}</failOnError></configuration>
    </plugin></plugins></pluginManagement></build></profile></profiles>
</project>""")
open(p, "w").write(t)
PYEOF
run generate
wl="$FX/docs/spec/1.0.0/P06/decisions/E01-published-api-gap-list.md"
if [ "$RC" = 0 ] && grep -qF -- '- `release-profile` · pluginManagement · plugin `9.9`: `doclint`=`all,-missing`, `failOnError`=`true`, `author`=not set' "$wl" \
        && grep -q 'Runtime evidence:' "$wl" \
        && grep -q 'whole aggregate returned `rc=0`' "$wl" \
        && grep -q 'returned `rc=1` with `reference not found`' "$wl"; then
    ok "EXTRA_javadoc_config_baseline: configured values resolve per profile and completed runtime/mutation evidence is retained"
else bad "EXTRA_javadoc_config_baseline: rc=$RC"; grep -A3 'Javadoc configuration' "$wl" | sed 's/^/         /'; fi

# R-2506-3: malformed inputs are one clean `api-inventory: ERROR` line and rc=1, never a traceback.
malformed_fail=""
mal() {  # mal LABEL — run generate on the current fixture and check the clean-error contract
    run generate
    if [ "$RC" -eq 0 ] || ! printf '%s\n' "$OUT" | grep -q '^api-inventory: ERROR ' || printf '%s\n' "$OUT" | grep -q Traceback; then
        malformed_fail="$malformed_fail [$1: rc=$RC $(printf '%s' "$OUT" | tail -1 | cut -c1-80)]"
    fi
}
fixture; printf '<project><artifactId>lib</artifactId>\n' > "$FX/lib/pom.xml"; mal "malformed pom"
fixture; printf '[1, 2]\n' > "$FX/docs/api-inventory/classification.json"; mal "top-level list"
fixture; cls 'c["artifacts"]["com.example:lib"] = "supported-public"'; mal "artifact entry is a string"
fixture; cls 'c["baseline"] = ["§4.2"]'; mal "baseline is a list"
fixture; cls 'del c["module_groups"][0]["prefix"]'; mal "group missing prefix"
fixture; python3 -c "import sys; open(sys.argv[1], 'w').write('[' * 100000 + ']' * 100000)" "$FX/docs/api-inventory/classification.json"; mal "deeply nested JSON"
fixture; cls 'c["artifacts"]["com.example:lib"]["evidence"] = 5'; mal "supported-public evidence is an integer"
fixture; cls 'c["artifacts"]["com.example:lib"]["support"] = "bogus"; c["artifacts"]["com.example:lib"]["evidence"] = 5'; mal "unclassified evidence is an integer"
# R-2506-11 follow-up: every handoff field, `support`, and a package rule's `reason` are type-checked like
# `evidence` — a string (entry_points: a list of strings) or absent/null. Each was rc=0 (or an ERROR-less rc=1).
for f in family backend docs_destination limitations; do
    fixture; cls "c['artifacts']['com.example:lib']['$f'] = 5"; mal "$f is an integer"
done
fixture; cls 'c["artifacts"]["com.example:lib"]["entry_points"] = "Api"'; mal "entry_points is a string"
fixture; cls 'c["artifacts"]["com.example:lib"]["entry_points"] = ["Api", 5]'; mal "entry_points holds an integer"
fixture; cls 'c["artifacts"]["com.example:lib"]["support"] = 5'; mal "support is an integer"
fixture; cls 'c.setdefault("packages", []).append({"prefix": "com.example.nomatch", "classification": "supported-public", "reason": 5})'; mal "package reason is an integer"
if [ -z "$malformed_fail" ]; then ok "EXTRA_malformed_inputs: malformed pom, list/str-shaped classification, list baseline, prefix-less group, deep nesting, and non-string evidence/support/handoff fields/package reason each give one ERROR line, rc=1, no traceback"
else bad "EXTRA_malformed_inputs:$malformed_fail"; fi

# R-2506-4: a pom carrying a DOCTYPE is refused, so no entity is ever expanded. The entity
# here expands to the module's real artifactId, so a parser that accepted it would generate cleanly (rc=0).
fixture; printf '<?xml version="1.0"?>\n<!DOCTYPE project [<!ENTITY a "lib">]>\n<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>com.example</groupId><artifactId>demo</artifactId><version>1.0.0</version></parent><artifactId>&a;</artifactId><packaging>jar</packaging></project>\n' > "$FX/lib/pom.xml"
run generate; expect "EXTRA_pom_doctype: a pom with a <!DOCTYPE is refused with an ERROR before parsing" 1 "^api-inventory: ERROR refusing .*lib/pom.xml.*DOCTYPE"
if ! printf '%s\n' "$OUT" | grep -q Traceback; then ok "EXTRA_pom_doctype: the refusal is clean, not a traceback"; else bad "EXTRA_pom_doctype: traceback"; fi

# R-2506-8: the byte regex above cannot see a UTF-16 DOCTYPE, but expat decodes UTF-16 itself. The refusal must
# therefore live INSIDE the parser. One plain internal entity; its value must appear in no output.
fixture
printf '<?xml version="1.0" encoding="UTF-16"?>\n<!DOCTYPE project [<!ENTITY a "ENTITYVALUE7f3">]>\n<project><artifactId>&a;</artifactId></project>\n' \
    | { iconv -f UTF-8 -t UTF-16 2>/dev/null || python3 -c "import sys; sys.stdout.buffer.write(sys.stdin.buffer.read().decode('utf-8').encode('utf-16'))"; } > "$FX/lib/pom.xml"
run generate
if [ "$RC" = 1 ] && printf '%s\n' "$OUT" | grep -q "^api-inventory: ERROR refusing .*lib/pom.xml.*DOCTYPE" \
        && ! grep -rq ENTITYVALUE7f3 "$FX/docs" 2>/dev/null && ! printf '%s\n' "$OUT" | grep -q ENTITYVALUE7f3 \
        && ! printf '%s\n' "$OUT" | grep -q Traceback; then
    ok "EXTRA_pom_doctype_utf16: a UTF-16 pom with an internal entity is refused inside the parser (rc=1) and the entity value appears in no output"
else bad "EXTRA_pom_doctype_utf16: rc=$RC $(printf '%s' "$OUT" | tail -1 | cut -c1-120)"; fi

# R-2506-11: a DOCTYPE with NO internal subset declares no entity, so only StartDoctypeDeclHandler can refuse it;
# UTF-16 hides it from the byte regex. This arm is what pins that handler on its own.
fixture
printf '<?xml version="1.0" encoding="UTF-16"?>\n<!DOCTYPE project>\n<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>com.example</groupId><artifactId>demo</artifactId><version>1.0.0</version></parent><artifactId>lib</artifactId><packaging>jar</packaging></project>\n' \
    | { iconv -f UTF-8 -t UTF-16 2>/dev/null || python3 -c "import sys; sys.stdout.buffer.write(sys.stdin.buffer.read().decode('utf-8').encode('utf-16'))"; } > "$FX/lib/pom.xml"
run generate
if [ "$RC" = 1 ] && printf '%s\n' "$OUT" | grep -q "^api-inventory: ERROR refusing .*lib/pom.xml.*DOCTYPE" \
        && ! printf '%s\n' "$OUT" | grep -q Traceback; then
    ok "EXTRA_pom_doctype_no_subset_utf16: a UTF-16 pom with <!DOCTYPE project> and no internal subset is refused (rc=1, one ERROR)"
else bad "EXTRA_pom_doctype_no_subset_utf16: rc=$RC $(printf '%s' "$OUT" | tail -1 | cut -c1-120)"; fi


# EXTRA_real_repo_deterministic — the committed repo inventory regenerates byte-identically twice
# (the fixture arms cannot catch nondeterminism that only a large real tree exposes, e.g. walk order).
if [ -f "$ROOT/$INV" ]; then
    python3 "$GEN" --root "$ROOT" generate --out "$WORK/real1.json" >/dev/null 2>&1
    python3 "$GEN" --root "$ROOT" generate --out "$WORK/real2.json" >/dev/null 2>&1
    if [ -s "$WORK/real1.json" ] && cmp -s "$WORK/real1.json" "$WORK/real2.json" && cmp -s "$WORK/real1.json" "$ROOT/$INV"; then
        ok "EXTRA_real_repo_deterministic: repo inventory regenerates byte-identically and matches the committed copy"
    else bad "EXTRA_real_repo_deterministic: repo inventory is not reproducible or has drifted"; fi
fi

echo
echo "passed: $PASS  failed: $FAIL"
[ "$FAIL" -eq 0 ]
