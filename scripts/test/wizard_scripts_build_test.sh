#!/usr/bin/env bash
#
# wizard_scripts_build_test.sh — the config wizard's launch scripts must build a JAR that can be built
# from a clean checkout (internal work item 2419 ledger N8; fixed in the PR under review per the owner's fix-in-PR rule).
#
# THE DEFECT. When the wizard JAR is missing, appbase-config-wizard/bin/start-console.sh and start-web.sh
# ran `mvn clean package -pl appbase-config-wizard -am -Dmaven.test.skip=true`. That flag skips COMPILING
# test sources, so appbase-client-core's test-jar is never produced — and appbase-client-clamav (pulled in
# via appbase-bootstrap-core under -am) has a test-scoped dependency on it. Measured 2026-09-26 on
# OpenJDK 25.0.2 from a clean Maven repo: "Could not find artifact
# com.viveworks.appbase:appbase-client-core:jar:tests:1.0.0". `-DskipTests` runs no tests but
# still compiles and packages test-jars, and the same build then succeeds (1m46s, clean repo).
#
# WHAT THIS ASSERTS — behaviour, not script text. Each script runs from a scratch copy of the module with
# NO jar present and with `mvn` and `java` replaced by stubs on PATH. The stub `mvn` records its argv; the
# test asserts the script's own build ran, with -DskipTests and without -Dmaven.test.skip. Nothing real
# is built and no port is opened, so it runs in well under a second and needs no JDK.
#
#   scripts/test/wizard_scripts_build_test.sh
#   WIZARD_BIN=/path/to/mutated/bin scripts/test/wizard_scripts_build_test.sh   # mutation runs (scripts)
#   WIZARD_REPO=/path/to/mutated/copy scripts/test/wizard_scripts_build_test.sh # mutation runs (config)
#
# @author Navaneeth Mysore G
# @author Claude
#
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BIN="${WIZARD_BIN:-$ROOT/appbase-config-wizard/bin}"

PASSED=0
FAILED=0
ok()  { PASSED=$((PASSED + 1)); printf 'ok    %s\n' "$1"; }
bad() { FAILED=$((FAILED + 1)); printf 'FAIL  %s\n' "$1"; [ $# -gt 1 ] && printf '        %s\n' "${@:2}"; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Stubs: mvn records its arguments and succeeds; java prints the web server's ready line and exits.
mkdir -p "$WORK/stubs"
cat > "$WORK/stubs/mvn" <<'STUB'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$MVN_LOG"
exit 0
STUB
cat > "$WORK/stubs/java" <<'STUB'
#!/usr/bin/env bash
echo "Open: http://127.0.0.1:0/"
exit 0
STUB
chmod +x "$WORK/stubs/mvn" "$WORK/stubs/java"

# check_script NAME ARGS… — run the script from a jar-less scratch module; judge the recorded mvn argv.
check_script() {
  local name="$1"; shift
  local mod="$WORK/$name/appbase-config-wizard" log="$WORK/$name/mvn.log"
  if [ ! -f "$BIN/$name" ]; then
    bad "AC-N8 $name: build step compiles test-jars (-DskipTests, not -Dmaven.test.skip)" "$BIN/$name missing"
    return
  fi
  mkdir -p "$mod/bin"
  cp "$BIN/$name" "$mod/bin/$name"
  : > "$log"
  ( cd "$mod" && PATH="$WORK/stubs:$PATH" MVN_LOG="$log" timeout 30 bash "bin/$name" "$@" ) >/dev/null 2>&1
  local argv; argv="$(cat "$log")"
  local miss=()
  [ -n "$argv" ] || miss+=("the script never invoked mvn with the jar missing — refusing to pass over nothing")
  printf '%s' "$argv" | grep -q -- '-Dmaven.test.skip' && miss+=("mvn was called with -Dmaven.test.skip: $argv")
  # The whole token: -DskipTests or -DskipTests=true. "-DskipTests=false" would run every test (RA-A1 B-F4).
  [ -z "$argv" ] || printf ' %s ' "$argv" | grep -qE -- ' -DskipTests(=true)? ' \
    || miss+=("mvn was called without -DskipTests (or with it disabled): $argv")
  [ ${#miss[@]} -eq 0 ] && ok "AC-N8 $name: build step compiles test-jars (-DskipTests, not -Dmaven.test.skip)" \
                        || bad "AC-N8 $name: build step compiles test-jars (-DskipTests, not -Dmaven.test.skip)" "${miss[@]}"
}

check_script start-console.sh
check_script start-web.sh 18080

# AC-N8 (effective configuration) — the argv is not the whole build. Maven also reads .mvn/maven.config
# and .mvn/jvm.config, the always-on quick-profile the scripts select, and the reactor POMs themselves;
# `maven.test.skip` set in any of those brings the clean-checkout failure back while the argv check above
# stays green (RA-A1 B5-7 on internal work item 2504). Static, because an empty-repository build needs JDK 25 and minutes;
# the clean-checkout BUILD SUCCESS itself is Example 4's recorded run.
# reactor_poms REPO → every reactor pom path, walked from the root pom's <modules>. A function because a
# heredoc inside $() is mis-parsed by bash 3.2, which macOS ships (RA-A4 A4-R4).
reactor_poms() {
  python3 - "$1" <<'PY'
import os, sys, xml.etree.ElementTree as ET
NS = '{http://maven.apache.org/POM/4.0.0}'
seen, stack = set(), [os.path.join(sys.argv[1], 'pom.xml')]
while stack:
    p = os.path.realpath(stack.pop())
    if p in seen or not os.path.isfile(p):
        continue
    seen.add(p)
    print(p)
    for m in ET.parse(p).getroot().iter(NS + 'module'):
        if m.text:
            stack.append(os.path.join(os.path.dirname(p), m.text.strip(), 'pom.xml'))
if not seen:
    sys.exit('no reactor pom read under ' + sys.argv[1])
PY
}
REPO="${WIZARD_REPO:-$ROOT}"
hits=()
for f in "$REPO/.mvn/maven.config" "$REPO/.mvn/jvm.config"; do
  [ -f "$f" ] && grep -q 'maven.test.skip' "$f" && hits+=("${f#"$REPO"/} sets maven.test.skip")
done
# REACTOR poms only, walked from the root pom's <modules> (profile modules included): a stray pom that no
# build reads cannot change the effective configuration, so it must not turn this arm red (RA-A1 L6-4).
# The walk is captured before use, so a parse failure is a failure and not "no poms found".
if poms="$(reactor_poms "$REPO")"; then
  while IFS= read -r f; do
    [ -n "$f" ] && grep -q 'maven.test.skip' "$f" && hits+=("${f#"$(realpath "$REPO")"/} mentions maven.test.skip")
  done <<< "$poms"
else
  hits+=("could not walk the reactor poms under $REPO — refusing to pass over nothing")
fi
for f in "$BIN/start-console.sh" "$BIN/start-web.sh"; do
  [ -f "$f" ] && grep -qE 'MAVEN_(OPTS|ARGS).*maven\.test\.skip' "$f" && hits+=("${f##*/} exports maven.test.skip through MAVEN_OPTS/MAVEN_ARGS")
done
[ ${#hits[@]} -eq 0 ] && ok "AC-N8 effective configuration: nothing re-enables maven.test.skip (.mvn, POMs, MAVEN_OPTS)" \
                      || bad "AC-N8 effective configuration: nothing re-enables maven.test.skip (.mvn, POMs, MAVEN_OPTS)" "${hits[@]}"

printf 'wizard-scripts-build-test: %d passed, %d failed\n' "$PASSED" "$FAILED"
[ "$FAILED" -eq 0 ]
