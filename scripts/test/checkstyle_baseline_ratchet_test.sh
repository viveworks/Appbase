#!/usr/bin/env bash
#
# Lightweight behavioural contract for internal work item 2650's exact Checkstyle baseline. This invokes only the
# direct checkstyle:check mojo for one seven-finding module; it never enters a Maven lifecycle.
#
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
BASELINE="$ROOT/appbase-java/etc/AppbaseCheckstyleBaseline.xml"
GENERATOR="$ROOT/scripts/ci/checkstyle-baseline.py"
CLAMAV_ROOT="$ROOT/appbase-java/appbase-client/appbase-client-clamav"
REPORT="$CLAMAV_ROOT/target/checkstyle-result.xml"
PASS=0
FAIL=0

ok() { PASS=$((PASS + 1)); echo "ok   $*"; }
bad() { FAIL=$((FAIL + 1)); echo "FAIL $*"; }

for tool in python3 java mvn git xmllint; do
  command -v "$tool" >/dev/null 2>&1 || { echo "checkstyle-baseline-ratchet: REFUSING: $tool is required" >&2; exit 2; }
done

WORK="$(mktemp -d)" || { echo "checkstyle-baseline-ratchet: REFUSING: mktemp failed" >&2; exit 2; }
[ -n "$WORK" ] && [ -d "$WORK" ] || { echo "checkstyle-baseline-ratchet: REFUSING: unusable temp directory" >&2; exit 2; }
trap 'rm -rf "$WORK"' EXIT

# Maven discovers the multi-module root by walking up to a tracked .mvn directory. Exercise that
# contract from a real nested module, then resolve every repository-relative input used by the raw
# verify scan and exact comparator. Presence alone is insufficient: the marker must be tracked, Maven
# must report the repository root, and all referenced gate inputs must exist beneath that root.
MAVEN_ROOT_OUTPUT="$WORK/maven-root.log"
if git -C "$ROOT" ls-files --error-unmatch .mvn/README.md >/dev/null 2>&1 \
    && [ -f "$ROOT/.mvn/README.md" ] \
    && (cd "$CLAMAV_ROOT" && mvn -q -ntp -N help:evaluate \
      -Dexpression=maven.multiModuleProjectDirectory -DforceStdout) \
      >"$MAVEN_ROOT_OUTPUT" 2>&1; then
  if python3 - "$MAVEN_ROOT_OUTPUT" "$ROOT" <<'PY'
import re, sys
log, expected = sys.argv[1:]
ansi = re.compile(r'\x1b\[[0-9;]*m')
with open(log, encoding='utf-8', errors='replace') as stream:
    values = [ansi.sub('', line).strip() for line in stream]
raise SystemExit(0 if expected in values else 1)
PY
  then
    MAVEN_ROOT="$ROOT"
  else
    MAVEN_ROOT=''
  fi
else
  MAVEN_ROOT=''
fi
if [ "$MAVEN_ROOT" = "$ROOT" ] \
    && [ -f "$MAVEN_ROOT/appbase-java/etc/AppbaseCheckstyle.xml" ] \
    && [ -f "$MAVEN_ROOT/appbase-java/etc/AppbaseCheckstyleNoSuppressions.xml" ] \
    && [ -f "$MAVEN_ROOT/appbase-java/etc/AppbaseCheckstyleBaseline.xml" ] \
    && [ -f "$MAVEN_ROOT/scripts/ci/checkstyle-baseline.py" ]; then
  ok "tracked .mvn marker anchors nested-module ratchet inputs at the repository root"
else
  bad "nested Maven root or root-relative ratchet inputs did not resolve (root='$MAVEN_ROOT')"
fi

# The production artifact is exact, anchored, unique, and complete. Count-only validation is
# deliberately insufficient: every identity attribute participates in this assertion.
if python3 - "$BASELINE" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
rows = [tuple(node.attrib.get(key, '') for key in ('files', 'checks', 'message', 'lines', 'columns'))
        for node in root.findall('suppress')]
assert root.tag == 'suppressions'
assert len(rows) == 8687, len(rows)
assert len(set(rows)) == len(rows)
assert all(all(row) for row in rows)
assert all(row[0].startswith('^') and row[0].endswith('$') and
           row[1].startswith('^') and row[1].endswith('$') and
           row[2].startswith('^') and row[2].endswith('$') for row in rows)
# internal work item 2700 originally reconciled two PackageNameCheck findings added by internal work item 2701 after its raw scan.
# PR internal work item 2748 regenerated the committed baseline to 8,381 measured identities; PR internal work item 2789
# added three exact appbase-core identities; follow-ups add 38 exact appbase-service-web
# identities, 181 exact appbase-service-user identities, and five exact reactor-netty
# identities, followed by 75 exact appbase-stacktest identities and four exact Firebase test
# identities, per the same Release 2 policy.
# Pin both MCP identities so a future refresh cannot silently lose or replace the reconciliation.
reconciled = [row for row in rows if r'appbase_mcp[/\\]generator' in row[0]]
assert len(reconciled) == 2, reconciled
assert {row[0].split(r'[/\\]')[-1] for row in reconciled} == {
    r'ApiSurfaceGenerator\.java$', r'package\-info\.java$'}
assert all(row[1] == r'^com\.puppycrawl\.tools\.checkstyle\.checks\.naming\.PackageNameCheck$'
           for row in reconciled)
assert all("appbase_mcp\\.generator" in row[2] for row in reconciled)
PY
then
  ok "production baseline has 8,687 identities including 2 exact MCP reconciliation rows"
else
  bad "production baseline structure/count/identity invariant failed"
fi

if python3 - "$ROOT/pom.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
for element in root.iter():
    if isinstance(element.tag, str) and '}' in element.tag:
        element.tag = element.tag.split('}', 1)[1]
plugins = root.findall('./build/plugins/plugin')
artifacts = [plugin.findtext('artifactId') for plugin in plugins]
checkstyle_index = artifacts.index('maven-checkstyle-plugin')
exec_index = artifacts.index('exec-maven-plugin')
assert checkstyle_index < exec_index
scan = plugins[checkstyle_index].find("executions/execution[id='checkstyle-no-new-scan']")
ratchet = plugins[exec_index].find("executions/execution[id='checkstyle-no-new-ratchet']")
assert scan is not None and ratchet is not None
assert scan.findtext('phase') == 'verify'
assert scan.findtext('goals/goal') == 'check'
assert scan.findtext('configuration/failOnViolation') == 'false'
assert scan.findtext('configuration/failsOnError') == 'false'
assert scan.findtext('configuration/suppressionsLocation').endswith('AppbaseCheckstyleNoSuppressions.xml')
assert ratchet.findtext('phase') == 'verify'
assert ratchet.findtext('goals/goal') == 'exec'
arguments = [node.text for node in ratchet.findall('configuration/arguments/argument')]
assert '--baseline' in arguments and '--check-report' in arguments and '--module-root' in arguments
assert '${project.basedir}' in arguments
PY
then
  ok "build/plugins order runs the unsuppressed scan before the exact identity comparator"
else
  bad "Maven verify-phase ratchet wiring is absent or weakened"
fi

if python3 - "$ROOT/pom.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
for element in root.iter():
    if isinstance(element.tag, str) and '}' in element.tag:
        element.tag = element.tag.split('}', 1)[1]
plugins = root.findall('./build/plugins/plugin')
def ordered(nodes):
    artifacts = [plugin.findtext('artifactId') for plugin in nodes]
    return artifacts.index('maven-checkstyle-plugin') < artifacts.index('exec-maven-plugin')
assert ordered(plugins)
left = next(i for i, plugin in enumerate(plugins)
            if plugin.findtext('artifactId') == 'maven-checkstyle-plugin')
right = next(i for i, plugin in enumerate(plugins)
             if plugin.findtext('artifactId') == 'exec-maven-plugin')
plugins[left], plugins[right] = plugins[right], plugins[left]
assert not ordered(plugins)
PY
then
  ok "mutation: reversing the build plugin order is detected"
else
  bad "build plugin reorder mutation was not detected"
fi

# Adversarial generator fixture: regex metacharacters appear in the tracked path, source, and message;
# XML metacharacters and quotes appear in the message. Java itself compiles and matches the generated
# regex attributes, while a one-character replacement message must not match.
ESCAPE_ROOT="$WORK/escape-repo"
ESCAPE_REL='appbase-java/weird+[dir]/src/test/java/demo/Dollar$Plus+[Amp&Probe.java'
mkdir -p "$ESCAPE_ROOT/$(dirname "$ESCAPE_REL")" "$ESCAPE_ROOT/target"
git -C "$ESCAPE_ROOT" init -q
: > "$ESCAPE_ROOT/pom.xml"
: > "$ESCAPE_ROOT/$ESCAPE_REL"
git -C "$ESCAPE_ROOT" add pom.xml "$ESCAPE_REL"
python3 - "$ESCAPE_ROOT" "$ESCAPE_REL" <<'PY'
import sys, xml.etree.ElementTree as ET
root, rel = sys.argv[1:]
doc = ET.Element('checkstyle', {'version': '13.8.0'})
file_node = ET.SubElement(doc, 'file', {'name': root + '/' + rel})
ET.SubElement(file_node, 'error', {
    'line': '7', 'column': '11', 'severity': 'error',
    'source': r'demo.Source+$[Probe]\Check',
    'message': r'xml & regex + [x] (y)? ^$ \ "quoted" <tag>',
})
ET.ElementTree(doc).write(root + '/target/checkstyle-result.xml', encoding='utf-8', xml_declaration=True)
PY
if "$GENERATOR" --root "$ESCAPE_ROOT" --output Baseline.xml >/dev/null \
    && xmllint --nonet --noout "$ESCAPE_ROOT/Baseline.xml" \
    && cat > "$WORK/RegexProbe.java" <<'JAVA'
import java.nio.file.Path;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

final class RegexProbe {
    private RegexProbe() { }
    public static void main(final String[] args) throws Exception {
        final Element item = (Element) DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(Path.of(args[0]).toFile())
                .getElementsByTagName("suppress").item(0);
        if (!Pattern.compile(item.getAttribute("files")).matcher(args[1]).find()
                || !Pattern.compile(item.getAttribute("checks")).matcher(args[2]).find()
                || !Pattern.compile(item.getAttribute("message")).matcher(args[3]).find()
                || Pattern.compile(item.getAttribute("message")).matcher(args[3] + "!").find()) {
            throw new AssertionError("generated regex did not match exactly");
        }
    }
}
JAVA
then
  ESCAPE_SOURCE='demo.Source+$[Probe]\Check'
  ESCAPE_MESSAGE='xml & regex + [x] (y)? ^$ \ "quoted" <tag>'
  if java "$WORK/RegexProbe.java" "$ESCAPE_ROOT/Baseline.xml" \
      "$ESCAPE_ROOT/$ESCAPE_REL" "$ESCAPE_SOURCE" "$ESCAPE_MESSAGE"; then
    ok "generator XML-escapes attributes and emits exact Java regexes for adversarial identities"
  else
    bad "generated adversarial identity did not compile/match exactly under Java Pattern"
  fi
else
  bad "adversarial baseline generation or XML parsing failed"
fi

# Produce a mutation with the SAME committed exact-head count (8,687, including the 2 pinned MCP
# rows): replace one ClamAV identity with a unique, nonmatching identity. A count ratchet would
# accept it; the exact event ratchet must expose the removed real event and fail.
MUTATED="$WORK/same-count-replacement.xml"
REMOVED="$WORK/removed.json"
if python3 - "$BASELINE" "$MUTATED" "$REMOVED" <<'PY'
import json, sys, xml.etree.ElementTree as ET
source, output, removed = sys.argv[1:]
text = open(source, encoding='utf-8').read()
lines = text.splitlines(keepends=True)
indexes = [i for i, line in enumerate(lines)
           if '<suppress ' in line and r'appbase\-client\-clamav' in line]
assert len(indexes) >= 2
first = ET.fromstring(lines[indexes[0]].strip())
with open(removed, 'w', encoding='utf-8') as stream:
    json.dump(first.attrib, stream)
first.set('message', first.attrib['message'][:-1] + r'MUTATED$')
lines[indexes[0]] = '    ' + ET.tostring(first, encoding='unicode', short_empty_elements=True) + '\n'
with open(output, 'w', encoding='utf-8') as stream:
    stream.writelines(lines)
assert sum('<suppress ' in line for line in lines) == 8687
PY
then
  ok "mutation preserves 8,687 rows while replacing one exact identity"
else
  bad "could not construct same-count replacement mutation"
fi

CHECK=(mvn -B -ntp -T 1 -pl :appbase-client-clamav
  org.apache.maven.plugins:maven-checkstyle-plugin:3.1.2:check
  -Dcheckstyle.console=false -Dcheckstyle.logViolationCount=true)

if "${CHECK[@]}" -Dbuild.failOnError=false -Dcheckstyle.failOnViolation=false \
    -Dcheckstyle.cache.file="$WORK/raw-cache" \
    -Dcheckstyle.suppressions.location="$ROOT/appbase-java/etc/AppbaseCheckstyleNoSuppressions.xml" \
    >"$WORK/raw.log" 2>&1 \
    && "$GENERATOR" --root "$ROOT" --baseline "$BASELINE" --check-report "$REPORT" \
      --module-root "$CLAMAV_ROOT" \
      >"$WORK/control.log" 2>&1; then
  ok "exact verifier accepts the seven measured raw ClamAV backlog events"
else
  bad "raw focused scan or exact baseline verification failed"
  tail -30 "$WORK/raw.log" | sed 's/^/    | /'
  tail -30 "$WORK/control.log" | sed 's/^/    | /'
fi

"$GENERATOR" --root "$ROOT" --baseline "$MUTATED" --check-report "$REPORT" \
  --module-root "$CLAMAV_ROOT" \
  >"$WORK/mutation.log" 2>&1
MUTATION_RC=$?
if [ "$MUTATION_RC" -eq 1 ] \
    && grep -q 'REFUSING: 1 new/unbaselined finding(s)' "$WORK/mutation.log"; then
  ok "same-count replacement fails and exposes exactly the removed unbaselined event"
else
  bad "same-count replacement did not fail with exactly the removed event (rc=$MUTATION_RC)"
  tail -30 "$WORK/mutation.log" | sed 's/^/    | /'
fi

EMPTY_REPORT="$WORK/empty-report.xml"
WRONG_ROOT_REPORT="$WORK/wrong-root-report.xml"
OMITTED_REPORT="$WORK/omitted-report.xml"
FOREIGN_REPORT="$WORK/foreign-report.xml"
DUPLICATE_EVENT_REPORT="$WORK/duplicate-event-report.xml"
WRONG_BASELINE_ROOT="$WORK/wrong-baseline-root.xml"
EMPTY_BASELINE_FIELD="$WORK/empty-baseline-field.xml"
DUPLICATE_BASELINE_ROW="$WORK/duplicate-baseline-row.xml"
printf '%s\n' '<?xml version="1.0" encoding="UTF-8"?><checkstyle version="13.8.0"/>' > "$EMPTY_REPORT"
printf '%s\n' '<?xml version="1.0" encoding="UTF-8"?><not-checkstyle/>' > "$WRONG_ROOT_REPORT"
python3 - "$REPORT" "$OMITTED_REPORT" <<'PY'
import sys, xml.etree.ElementTree as ET
tree = ET.parse(sys.argv[1])
root = tree.getroot()
first = root.find('file')
assert first is not None
root.remove(first)
tree.write(sys.argv[2], encoding='utf-8', xml_declaration=True)
PY
python3 - "$REPORT" "$FOREIGN_REPORT" "$ROOT/pom.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
source, output, foreign = sys.argv[1:]
tree = ET.parse(source)
root = tree.getroot()
ET.SubElement(root, 'file', {'name': foreign})
tree.write(output, encoding='utf-8', xml_declaration=True)
PY
python3 - "$REPORT" "$DUPLICATE_EVENT_REPORT" <<'PY'
import copy, sys, xml.etree.ElementTree as ET
tree = ET.parse(sys.argv[1])
for file_node in tree.getroot().findall('file'):
    event = file_node.find('error')
    if event is not None:
        file_node.append(copy.deepcopy(event))
        break
else:
    raise AssertionError('raw report has no event to duplicate')
tree.write(sys.argv[2], encoding='utf-8', xml_declaration=True)
PY
python3 - "$BASELINE" "$WRONG_BASELINE_ROOT" "$EMPTY_BASELINE_FIELD" \
  "$DUPLICATE_BASELINE_ROW" <<'PY'
import copy, sys, xml.etree.ElementTree as ET
source, wrong_root, empty_field, duplicate_row = sys.argv[1:]
tree = ET.parse(source)
tree.getroot().tag = 'not-suppressions'
tree.write(wrong_root, encoding='utf-8', xml_declaration=True)
tree = ET.parse(source)
first = tree.getroot().find('suppress')
assert first is not None
first.set('columns', '')
tree.write(empty_field, encoding='utf-8', xml_declaration=True)
tree = ET.parse(source)
first = tree.getroot().find('suppress')
assert first is not None
tree.getroot().append(copy.deepcopy(first))
tree.write(duplicate_row, encoding='utf-8', xml_declaration=True)
PY

if "$GENERATOR" --root "$ROOT" --baseline "$BASELINE" --check-report "$EMPTY_REPORT" \
  --module-root "$CLAMAV_ROOT" >"$WORK/empty-owning.log" 2>&1; then
  ok "source-owning module accepts a well-formed empty findings report"
else
  bad "source-owning module rejected a valid empty findings report"
fi

if "$GENERATOR" --root "$ROOT" --baseline "$BASELINE" --check-report "$EMPTY_REPORT" \
    --module-root "$ROOT/appbase-bom" >"$WORK/empty-aggregator.log" 2>&1; then
  ok "genuinely source-free module accepts a well-formed empty report"
else
  bad "source-free module rejected a well-formed empty report"
fi

"$GENERATOR" --root "$ROOT" --baseline "$BASELINE" --check-report "$WRONG_ROOT_REPORT" \
  --module-root "$CLAMAV_ROOT" >"$WORK/wrong-root.log" 2>&1
WRONG_ROOT_RC=$?
if [ "$WRONG_ROOT_RC" -eq 2 ] && grep -q "expected 'checkstyle'" "$WORK/wrong-root.log"; then
  ok "wrong report root fails closed"
else
  bad "wrong report root did not fail closed (rc=$WRONG_ROOT_RC)"
fi

if "$GENERATOR" --root "$ROOT" --baseline "$BASELINE" --check-report "$OMITTED_REPORT" \
  --module-root "$CLAMAV_ROOT" >"$WORK/omitted.log" 2>&1; then
  ok "report with one fewer finding-bearing file remains valid"
else
  bad "report incorrectly required every owned Java file to have a findings node"
fi

"$GENERATOR" --root "$ROOT" --baseline "$BASELINE" --check-report "$FOREIGN_REPORT" \
  --module-root "$CLAMAV_ROOT" >"$WORK/foreign.log" 2>&1
FOREIGN_RC=$?
if [ "$FOREIGN_RC" -eq 2 ] && grep -q 'not owned by' "$WORK/foreign.log"; then
  ok "report containing a path outside the owning module fails closed"
else
  bad "foreign report path did not fail closed (rc=$FOREIGN_RC)"
fi

"$GENERATOR" --root "$ROOT" --baseline "$BASELINE" --check-report "$DUPLICATE_EVENT_REPORT" \
  --module-root "$CLAMAV_ROOT" >"$WORK/duplicate-event.log" 2>&1
DUPLICATE_EVENT_RC=$?
if [ "$DUPLICATE_EVENT_RC" -eq 2 ] \
    && grep -q 'duplicate finding identities' "$WORK/duplicate-event.log"; then
  ok "report with a duplicated finding identity fails closed"
else
  bad "duplicate report event did not fail closed (rc=$DUPLICATE_EVENT_RC)"
fi

"$GENERATOR" --root "$ROOT" --baseline "$WRONG_BASELINE_ROOT" --check-report "$REPORT" \
  --module-root "$CLAMAV_ROOT" >"$WORK/wrong-baseline-root.log" 2>&1
WRONG_BASELINE_ROOT_RC=$?
if [ "$WRONG_BASELINE_ROOT_RC" -eq 2 ] \
    && grep -q "expected 'suppressions'" "$WORK/wrong-baseline-root.log"; then
  ok "baseline with a wrong root fails closed"
else
  bad "wrong baseline root did not fail closed (rc=$WRONG_BASELINE_ROOT_RC)"
fi

"$GENERATOR" --root "$ROOT" --baseline "$EMPTY_BASELINE_FIELD" --check-report "$REPORT" \
  --module-root "$CLAMAV_ROOT" >"$WORK/empty-baseline-field.log" 2>&1
EMPTY_BASELINE_FIELD_RC=$?
if [ "$EMPTY_BASELINE_FIELD_RC" -eq 2 ] \
    && grep -q 'empty identity attribute' "$WORK/empty-baseline-field.log"; then
  ok "baseline row with an empty required identity field fails closed"
else
  bad "empty baseline identity field did not fail closed (rc=$EMPTY_BASELINE_FIELD_RC)"
fi

"$GENERATOR" --root "$ROOT" --baseline "$DUPLICATE_BASELINE_ROW" --check-report "$REPORT" \
  --module-root "$CLAMAV_ROOT" >"$WORK/duplicate-baseline-row.log" 2>&1
DUPLICATE_BASELINE_ROW_RC=$?
if [ "$DUPLICATE_BASELINE_ROW_RC" -eq 2 ] \
    && grep -q 'duplicate identity rows' "$WORK/duplicate-baseline-row.log"; then
  ok "baseline with a duplicated identity row fails closed"
else
  bad "duplicate baseline identity did not fail closed (rc=$DUPLICATE_BASELINE_ROW_RC)"
fi

echo
echo "checkstyle-baseline-ratchet: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
