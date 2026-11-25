#!/usr/bin/env bash
#
# jersey_resource_config_compatibility_wiring_test.sh — structural regression test for internal work item 3015.
# Proves every servlet builder validates ResourceConfig compatibility before constructing Jersey,
# and proves deleting any one validator call makes the check fail.
#
# @author Navaneeth Mysore G
# @author Claude
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"
BOOTSTRAP="$ROOT/appbase-java/appbase-bootstrap"
JAVA_PATH="src/main/java/com/viveworks/appbase/bootstrap"
JETTY="$BOOTSTRAP/appbase-bootstrap-servlet-jetty"
TOMCAT="$BOOTSTRAP/appbase-bootstrap-servlet-tomcat"
UNDERTOW="$BOOTSTRAP/appbase-bootstrap-servlet-undertow"

BUILDERS=(
  "$JETTY/$JAVA_PATH/jetty/server/JettyServletContainer.java"
  "$TOMCAT/$JAVA_PATH/tomcat/server/TomcatServletContainer.java"
  "$UNDERTOW/$JAVA_PATH/undertow/server/UndertowServletContainer.java"
)

check_wiring() {
  local source="$1"
  local validator_count container_count validator_line container_line
  validator_count="$(
    awk '/JerseyResourceConfigCompatibility\.validate\(rc\);/{count++} END{print count+0}' "$source"
  )"
  container_count="$(awk '/new ServletContainer\(rc\)/{count++} END{print count+0}' "$source")"
  validator_line="$(awk '/JerseyResourceConfigCompatibility\.validate\(rc\);/{print NR}' "$source")"
  container_line="$(awk '/new ServletContainer\(rc\)/{print NR}' "$source")"

  [[ "$validator_count" -eq 1 && "$container_count" -eq 1
      && "$validator_line" -lt "$container_line" ]]
}

for builder in "${BUILDERS[@]}"; do
  check_wiring "$builder"
  echo "ok: $(basename "$builder") validates before ServletContainer construction"
done

MUTANT_DIR="$(mktemp -d)"
trap 'rm -rf "$MUTANT_DIR"' EXIT
for builder in "${BUILDERS[@]}"; do
  mutant="$MUTANT_DIR/$(basename "$builder")"
  sed '/JerseyResourceConfigCompatibility\.validate(rc);/d' "$builder" > "$mutant"
  if check_wiring "$mutant"; then
    echo "FAIL: deleting validator call was not detected in $(basename "$builder")" >&2
    exit 1
  fi
  echo "ok: deletion mutant rejected for $(basename "$builder")"
done

echo "jersey-resource-config-compatibility-wiring-test: PASS"
