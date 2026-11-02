#!/usr/bin/env bash
#
# bom_release1_test.sh — appbase-bom pins exactly the Release-1 publishable surface (internal work item 2419, owner
# decision on internal work item 2504: "Need to fix BOM").
#
# The rule, from the Release 1.0 Lean Requirements baseline (reconciled 2026-08-28) §4.2/§4.3: deferred capability
# and empty artifact shells must not appear in Maven publication claims, and the BOM is the one
# publication claim a consumer imports. So the BOM must manage:
#
#   every packaging=jar module REACHABLE from the root pom's <modules> graph
#     MINUS the artifacts in scripts/test/release1-excluded-artifacts.txt (the single list)
#
# each at groupId com.viveworks.appbase and version ${project.version}, and nothing else. The expected
# set is DERIVED from the reactor on every run, so a new jar module that is not added to the BOM fails
# here instead of silently shipping unpinned.
#
# Parsing is XML-aware (python3 xml.etree, standard library): an earlier line-based version missed a
# one-line <parent>…</parent> and two artifactIds on one line (RA R3). Walking <modules> rather than
# every pom.xml on disk means a stray non-reactor pom cannot turn it falsely red. ElementTree on
# python3 >= 3.7 / expat >= 2.4 does not resolve external entities and caps entity amplification;
# defusedxml would be stricter but is not in the standard library the repo's scripts rely on.
#
#   scripts/test/bom_release1_test.sh
#   BOM_TEST_ROOT=/path/to/mutated/copy scripts/test/bom_release1_test.sh   # mutation runs
#
# @author Navaneeth Mysore G
# @author Claude
#
set -uo pipefail

ROOT="${BOM_TEST_ROOT:-$(cd "$(dirname "$0")/../.." && pwd)}"
BOM="$ROOT/appbase-bom/pom.xml"
EXCLUDED_FILE="$ROOT/scripts/test/release1-excluded-artifacts.txt"

PASSED=0
FAILED=0
ok()  { PASSED=$((PASSED + 1)); printf 'ok    %s\n' "$1"; }
bad() { FAILED=$((FAILED + 1)); printf 'FAIL  %s\n' "$1"; [ $# -gt 1 ] && printf '        %s\n' "${@:2}"; }

if ! command -v python3 >/dev/null 2>&1; then
  bad "EXTRA_bom_python3" "python3 not found — refusing to pass over nothing"
  printf 'bom-release1-test: %d passed, %d failed\n' "$PASSED" "$FAILED"; exit 1
fi

# One python pass emits tagged lines:  JAR <artifactId> · ENTRY <groupId> <artifactId> <version> ·
# PARENT <pom read for inherited entries> · ERR <msg>
# The python lives in a function, not inside "$(…)": bash 3.2 (macOS /bin/bash) misparses a heredoc
# inside command substitution when the body holds quotes or parentheses (RA-A4 A4-R4 on internal work item 2504).
bom_facts() {
python3 - "$1" "$2" <<'PY'
import os, sys, xml.etree.ElementTree as ET
root, bom = sys.argv[1], sys.argv[2]
NS = '{http://maven.apache.org/POM/4.0.0}'
def txt(el, tag):
    e = el.find(NS + tag)
    return e.text.strip() if e is not None and e.text else None
def load(path):
    return ET.parse(path).getroot()
# Walk the <modules> graph from the root pom (profile modules included — build.sh's gate does too).
seen, stack = set(), [os.path.join(root, 'pom.xml')]
while stack:
    p = os.path.realpath(stack.pop())
    if p in seen: continue
    seen.add(p)
    if not os.path.isfile(p):
        print('ERR module pom missing: ' + os.path.relpath(p, root)); continue
    r = load(p)
    art, pk = txt(r, 'artifactId'), txt(r, 'packaging') or 'jar'
    if art is None: print('ERR no artifactId in ' + os.path.relpath(p, root)); continue
    if pk == 'jar': print('JAR ' + art)
    # Every direct dependency edge between reactor modules: DEP <from> <to> <scope> <optional>. Profile
    # <dependencies> count too: any profile can be activated, so its edges reach a consumer just the same
    # (RA-A4 A4-R1 on internal work item 2504). <dependencyManagement> entries are not edges and are not read here.
    containers = [r.find(NS + 'dependencies')]
    for prof in r.findall(NS + 'profiles/' + NS + 'profile'):
        containers.append(prof.find(NS + 'dependencies'))
    for d in [d for c in containers if c is not None for d in c.findall(NS + 'dependency')]:
        g, a = txt(d, 'groupId'), txt(d, 'artifactId')
        if g in ('com.viveworks.appbase', '${project.groupId}') and a:
            print('DEP %s %s %s %s' % (art, a, txt(d, 'scope') or 'compile', txt(d, 'optional') or 'false'))
    mods = [m for m in r.iter(NS + 'module') if m.text]
    for m in mods:
        stack.append(os.path.join(os.path.dirname(p), m.text.strip(), 'pom.xml'))
def managed(path):
    # EVERY <dependencyManagement>, profiles included: an activeByDefault profile's entries are managed
    # just the same (RA-A1 B5-4 on internal work item 2504).
    for dm in load(path).iter(NS + 'dependencyManagement'):
        for d in dm.iter(NS + 'dependency'):
            yield txt(d, 'groupId'), txt(d, 'artifactId'), txt(d, 'version')
if os.path.isfile(bom):
    for g, a, v in managed(bom):
        print('ENTRY %s %s %s' % (g, a, v))
    # The PARENT CHAIN too: a consumer that imports the BOM imports its EFFECTIVE dependencyManagement,
    # which includes everything the BOM inherits (RA-A1 L5-3 on internal work item 2504). The root pom manages ~130
    # third-party coordinates that are no Appbase publication claim, so only Appbase-group entries
    # count from an ancestor. A <parent> whose pom is not on disk (a released parent) is an error, not
    # a pass: the chain could not be read.
    cur, hops = load(bom), 0
    while True:
        par = cur.find(NS + 'parent')
        if par is None:
            break
        hops += 1
        rel = txt(par, 'relativePath')
        base = os.path.dirname(os.path.realpath(bom if hops == 1 else ppath))
        ppath = os.path.normpath(os.path.join(base, rel if rel is not None else '../pom.xml'))
        if os.path.isdir(ppath):
            ppath = os.path.join(ppath, 'pom.xml')
        if not os.path.isfile(ppath) or hops > 20:
            print('ERR BOM parent pom not readable: %s' % os.path.relpath(ppath, root)); break
        cur = load(ppath)
        if txt(cur, 'artifactId') != txt(par, 'artifactId'):
            print('ERR BOM parent at %s is %s, not %s' % (os.path.relpath(ppath, root), txt(cur, 'artifactId'), txt(par, 'artifactId'))); break
        for g, a, v in managed(ppath):
            if g in ('com.viveworks.appbase', '${project.groupId}'):
                print('ENTRY %s %s %s' % (g, a, v))
        print('PARENT ' + os.path.relpath(ppath, root))
PY
}
facts="$(bom_facts "$ROOT" "$BOM")" || { bad "EXTRA_bom_parse" "python3 parse failed"; printf 'bom-release1-test: %d passed, %d failed\n' "$PASSED" "$FAILED"; exit 1; }

errs="$(printf '%s\n' "$facts" | sed -n 's/^ERR //p')"
jars="$(printf '%s\n' "$facts" | sed -n 's/^JAR //p' | sort -u)"
entries_full="$(printf '%s\n' "$facts" | sed -n 's/^ENTRY //p')"
entries="$(printf '%s\n' "$entries_full" | awk 'NF { print $2 }')"
excluded="$( [ -f "$EXCLUDED_FILE" ] && sed 's/#.*//; s/[[:space:]]//g; /^$/d' "$EXCLUDED_FILE")"

if [ ! -f "$BOM" ] || [ -z "$excluded" ] || [ -n "$errs" ]; then
  bad "EXTRA_bom_inputs" "BOM present: $([ -f "$BOM" ] && echo yes || echo no)" \
      "exclusion list entries: $(printf '%s\n' "$excluded" | grep -c .)" ${errs:+"$errs"}
elif [ "$(printf '%s\n' "$jars" | grep -c .)" -lt 10 ] || [ "$(printf '%s\n' "$entries" | grep -c .)" -lt 10 ]; then
  bad "EXTRA_bom_nonvacuous" "read too few jars or BOM entries — refusing to pass over nothing"
elif ! printf '%s\n' "$facts" | grep -q '^PARENT '; then
  bad "EXTRA_bom_nonvacuous" "the BOM's parent chain was not read — inherited dependencyManagement unchecked"
else
  ok "EXTRA_bom_nonvacuous (parent chain: $(printf '%s\n' "$facts" | sed -n 's/^PARENT //p' | paste -sd' '))"
fi

miss=()
while IFS= read -r a; do
  [ -n "$a" ] || continue
  printf '%s\n' "$excluded" | grep -qxF "$a" && continue
  printf '%s\n' "$entries" | grep -qxF "$a" || miss+=("$a is a Release-1 jar but is not in the BOM")
done <<< "$jars"
[ ${#miss[@]} -eq 0 ] && ok "EXTRA_bom_lists_every_release1_jar" || bad "EXTRA_bom_lists_every_release1_jar" "${miss[@]}"

miss=()
while IFS= read -r a; do
  [ -n "$a" ] || continue
  printf '%s\n' "$entries" | grep -qxF "$a" && miss+=("$a is deferred or an empty shell but is in the BOM")
done <<< "$excluded"
[ ${#miss[@]} -eq 0 ] && ok "EXTRA_bom_excludes_deferred_and_shells" || bad "EXTRA_bom_excludes_deferred_and_shells" "${miss[@]}"

miss=()
while IFS= read -r a; do
  [ -n "$a" ] || continue
  printf '%s\n' "$jars" | grep -qxF "$a" || miss+=("$a is in the BOM but is not a reactor jar")
done <<< "$entries"
dups="$(printf '%s\n' "$entries" | sort | uniq -d)"
[ -n "$dups" ] && miss+=("duplicated BOM entries: $(echo $dups)")
[ ${#miss[@]} -eq 0 ] && ok "EXTRA_bom_has_no_unknown_or_duplicate_entries" || bad "EXTRA_bom_has_no_unknown_or_duplicate_entries" "${miss[@]}"

# RA R3(1): a wrong groupId or a literal version pins nothing useful — assert every coordinate.
miss=()
while read -r g a v; do
  [ -n "${a:-}" ] || continue
  [ "$g" = "com.viveworks.appbase" ] || miss+=("$a has groupId '$g', expected com.viveworks.appbase")
  [ "$v" = '${project.version}' ] || miss+=("$a has version '$v', expected \${project.version}")
done <<< "$entries_full"
[ ${#miss[@]} -eq 0 ] && ok "EXTRA_bom_coordinates" || bad "EXTRA_bom_coordinates" "${miss[@]}"

# RA R3(2): the prose copies of the exclusion list must agree with the single list.
miss=()
while IFS= read -r a; do
  [ -n "$a" ] || continue
  sed -n '/<!--/,/-->/p' "$BOM" | grep -qwF "$a" \
    || miss+=("appbase-bom/pom.xml comment does not name $a")
  grep -qF "$a" "$ROOT/docs/Known-Limitations.md" 2>/dev/null \
    || miss+=("docs/Known-Limitations.md does not name $a")
done <<< "$excluded"
[ ${#miss[@]} -eq 0 ] && ok "EXTRA_exclusion_copies_agree" || bad "EXTRA_exclusion_copies_agree" "${miss[@]}"

# Owner ruling Q-A on internal work item 2504 (2026-09-27): a Release-1 module must not drag a deferred or empty-shell
# artifact onto a consumer's classpath. So no reactor module outside the exclusion list may depend on an
# excluded artifact at compile or runtime scope unless the edge is <optional>true</optional>. Before the
# ruling, appbase-bootstrap-core pulled eight (ledger N9). The three empty shells are now removed;
# no exception edge remains.
KNOWN_EDGES=''
# edge_violations FACTS -> one line per non-optional compile/runtime edge from a non-excluded module to an
# excluded one, except the named KNOWN_EDGES.
edge_violations() {
  local from to scope optional
  while read -r from to scope optional; do
    [ -n "${to:-}" ] || continue
    printf '%s\n' "$excluded" | grep -qxF "$to" || continue      # edge target is not excluded
    printf '%s\n' "$excluded" | grep -qxF "$from" && continue    # deferred modules may use each other
    case "$scope" in compile|runtime) ;; *) continue ;; esac     # test/provided never reach a consumer
    [ "$optional" = "true" ] && continue
    printf '%s\n' $KNOWN_EDGES | grep -qxF "$from>$to" && continue
    printf '%s depends on excluded %s at %s scope without <optional>true</optional>\n' "$from" "$to" "$scope"
  done <<< "$(printf '%s\n' "$1" | sed -n 's/^DEP //p')"
}
miss=()
while IFS= read -r line; do [ -n "$line" ] && miss+=("$line"); done <<< "$(edge_violations "$facts")"
[ ${#miss[@]} -eq 0 ] && ok "EXTRA_no_transitive_deferred: no Release-1 module pulls an excluded artifact onto a consumer classpath" \
                      || bad "EXTRA_no_transitive_deferred: no Release-1 module pulls an excluded artifact onto a consumer classpath" "${miss[@]}"

# EXTRA_profile_edge_fixture — reason: RA-A4 A4-R1. An excluded dependency declared inside a profile (here
# activeByDefault) reaches a consumer exactly like a top-level one, so the edge check must see it. A two-pom
# fixture reactor runs through the same bom_facts/edge_violations path as the real tree.
FX="$(mktemp -d)"
mkdir -p "$FX/sup"
cat > "$FX/pom.xml" <<'XML'
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
  <groupId>com.viveworks.appbase</groupId><artifactId>appbase-fixture-root</artifactId><version>1</version>
  <packaging>pom</packaging><modules><module>sup</module></modules></project>
XML
cat > "$FX/sup/pom.xml" <<'XML'
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
  <groupId>com.viveworks.appbase</groupId><artifactId>appbase-fixture-supported</artifactId><version>1</version>
  <profiles><profile><id>on</id><activation><activeByDefault>true</activeByDefault></activation>
    <dependencies><dependency><groupId>com.viveworks.appbase</groupId>
      <artifactId>appbase-client-spark</artifactId><version>1</version></dependency></dependencies>
  </profile></profiles></project>
XML
fx_out="$(edge_violations "$(bom_facts "$FX" "$FX/no-bom.xml")")"
rm -rf "$FX"
if printf '%s\n' "$fx_out" | grep -q '^appbase-fixture-supported depends on excluded appbase-client-spark at compile scope'; then
  ok "EXTRA_profile_edge_fixture: an excluded dependency inside an activeByDefault profile is caught"
else
  bad "EXTRA_profile_edge_fixture: an excluded dependency inside an activeByDefault profile is caught" "got: ${fx_out:-<nothing>}"
fi

printf 'bom-release1-test: %d passed, %d failed\n' "$PASSED" "$FAILED"
[ "$FAILED" -eq 0 ]
