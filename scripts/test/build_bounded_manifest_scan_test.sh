#!/usr/bin/env bash
# Host-safe contract and fixture checks for build.sh's bounded manifest coverage gate.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BUILD="$ROOT/build.sh"
PASSED=0
FAILED=0
ok() { PASSED=$((PASSED + 1)); echo "  ok   $1"; }
bad() { FAILED=$((FAILED + 1)); echo "  FAIL $1"; }

# Never execute a fixture copy of the old script: that copy would still run an
# unbounded recursive manifest search, which this host explicitly forbids.
if ! grep -Fq -- 'rg --files --hidden --no-ignore --max-depth 8' "$BUILD" ||
   grep -Fq -- 'find . \(' "$BUILD"; then
  bad "build.sh still lacks the bounded rg scan or contains the old recursive search"
  echo "build-bounded-manifest-scan-test: $PASSED passed, $FAILED failed"
  exit 1
fi
ok "build.sh uses a depth-8 rg scan instead of the old recursive search"

TMP="$(mktemp -d)"
trap 'rm -rf -- "$TMP"' EXIT
mkdir -p "$TMP/base/scripts" "$TMP/bin"
cp "$BUILD" "$TMP/base/build.sh"
printf '<project/>\n' > "$TMP/base/pom.xml"
printf '#!/usr/bin/env bash\nexit 0\n' > "$TMP/base/scripts/api-inventory-fast-check.sh"
printf '#!/usr/bin/env bash\nprintf called > "$MAVEN_MARKER"\n' > "$TMP/bin/mvn"
chmod +x "$TMP/bin/mvn"

run_case() {
  local name="$1" expected="$2" marker="$3" fixture="$TMP/$1" output rc
  mkdir -p "$fixture/bin"
  cp -R "$TMP/base/." "$fixture/"
  case "$name" in
    shallow-orphan) mkdir -p "$fixture/orphan"; printf '<project/>\n' > "$fixture/orphan/pom.xml" ;;
    nested-reactor-orphan)
      mkdir -p "$fixture/appbase-java/rogue"
      printf '<project><modules><module>appbase-java</module></modules></project>\n' > "$fixture/pom.xml"
      printf '<project/>\n' > "$fixture/appbase-java/pom.xml"
      printf '<project/>\n' > "$fixture/appbase-java/rogue/pom.xml" ;;
    wizard-frontend)
      mkdir -p "$fixture/appbase-config-wizard/frontend"
      printf '<project><modules><module>appbase-config-wizard</module></modules></project>\n' > "$fixture/pom.xml"
      printf '<project/>\n' > "$fixture/appbase-config-wizard/pom.xml"
      printf 'name: frontend\n' > "$fixture/appbase-config-wizard/frontend/pubspec.yaml" ;;
    wizard-frontend-pom-orphan)
      mkdir -p "$fixture/appbase-config-wizard/frontend"
      printf '<project><modules><module>appbase-config-wizard</module></modules></project>\n' > "$fixture/pom.xml"
      printf '<project/>\n' > "$fixture/appbase-config-wizard/pom.xml"
      printf '<project/>\n' > "$fixture/appbase-config-wizard/frontend/pom.xml" ;;
    current-inventory)
      local manifests manifest
      manifests="$(cd "$ROOT" && rg --files --hidden --no-ignore --max-depth 8 \
        -g 'pom.xml' -g 'pubspec.yaml' \
        -g '!**/target/**' -g '!**/build/**' \
        -g '!**/.dart_tool/**' -g '!**/node_modules/**' \
        -g '!**/.claude/**' -g '!**/.git/**' .)"
      while IFS= read -r manifest; do
        manifest="${manifest#./}"
        [[ -z "$manifest" ]] && continue
        mkdir -p "$fixture/$(dirname "$manifest")"
        cp "$ROOT/$manifest" "$fixture/$manifest"
      done <<< "$manifests" ;;
    depth-eight-orphan)
      mkdir -p "$fixture/a/b/c/d/e/f/g"
      printf '<project/>\n' > "$fixture/a/b/c/d/e/f/g/pom.xml" ;;
    depth-nine-outside-bound)
      mkdir -p "$fixture/a/b/c/d/e/f/g/h"
      printf '<project/>\n' > "$fixture/a/b/c/d/e/f/g/h/pom.xml" ;;
    excluded-tree)
      mkdir -p "$fixture/appbase-codeagents/other"
      printf 'name: ignored\n' > "$fixture/appbase-codeagents/other/pubspec.yaml" ;;
    generated-tree)
      mkdir -p "$fixture/orphan/target"
      printf '<project/>\n' > "$fixture/orphan/target/pom.xml" ;;
    scan-error)
      printf '#!/usr/bin/env bash\nexit 2\n' > "$fixture/bin/rg"
      chmod +x "$fixture/bin/rg" ;;
  esac
  if output="$(cd "$fixture" && PATH="$fixture/bin:$TMP/bin:$PATH" MAVEN_MARKER="$fixture/maven-called" \
      APPBASE_SKIP_JDK_CHECK=1 APPBASE_ALLOW_CONCURRENT_JAVA=1 \
      bash ./build.sh --mode compile 2>&1)"; then rc=0; else rc=$?; fi
  if [[ "$rc" -eq "$expected" && ( -z "$marker" || "$output" == *"$marker"* ) ]] &&
     { [[ "$expected" -eq 0 && -f "$fixture/maven-called" ]] ||
       [[ "$expected" -ne 0 && ! -e "$fixture/maven-called" ]]; }; then
    ok "$name exits $expected with expected diagnostic"
  else
    bad "$name exits $rc (expected $expected), Maven called=$([[ -e "$fixture/maven-called" ]] && echo yes || echo no): $output"
  fi
}

run_case clean 0 'compile gate:'
run_case shallow-orphan 1 'orphan'
run_case nested-reactor-orphan 1 'appbase-java/rogue'
run_case wizard-frontend 0 'compile gate:'
run_case wizard-frontend-pom-orphan 1 'appbase-config-wizard/frontend'
run_case current-inventory 0 'compile gate:'
run_case depth-eight-orphan 1 'a/b/c/d/e/f/g'
run_case depth-nine-outside-bound 0 'compile gate:'
run_case excluded-tree 0 'compile gate:'
run_case generated-tree 0 'compile gate:'
run_case scan-error 1 'bounded build-manifest scan failed'

echo "build-bounded-manifest-scan-test: $PASSED passed, $FAILED failed"
[[ "$FAILED" -eq 0 ]]
