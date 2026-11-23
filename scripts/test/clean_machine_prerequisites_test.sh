#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
failed=0

check() {
  local description="$1" file="$2" text="$3"
  if grep -qF "$text" "$ROOT/$file"; then
    printf 'ok   %s\n' "$description"
  else
    printf 'FAIL %s: %s does not contain %s\n' "$description" "$file" "$text"
    failed=$((failed + 1))
  fi
}

check 'POM enforces the Maven 3.6.3 floor' pom.xml '<version>[3.6.3,]</version>'
check 'README matches the Maven floor' README.md 'Maven 3.6.3 or later'
check 'User Guide prerequisites match the Maven floor' docs/user-guide/README.md '**3.6.3 or later**'
check 'User Guide verifier matches the Maven floor' docs/user-guide/README.md 'Maven 3.6.3+'
check 'Install command matches the Maven floor' docs/user-guide/01-install-and-first-service.md 'must report 3.6.3 or later'
check 'Configuration Reference matches the Maven floor' docs/Configuration-Reference.md '**3.6.3 or later**'
check 'Configuration Reference cites the Enforcer floor' docs/Configuration-Reference.md 'requireMavenVersion [3.6.3,]'
check 'Wizard guide documents the real fallback build' docs/user-guide/05-config-wizard.md \
  'mvn clean package -pl appbase-config-wizard -am -DskipTests -P quick-profile'

if rg -n 'Maven 3\.6 or later|Maven 3\.6\+' \
  "$ROOT/README.md" "$ROOT/docs/user-guide" "$ROOT/docs/Configuration-Reference.md"; then
  printf 'FAIL stale public Maven 3.6 minimum remains\n'
  failed=$((failed + 1))
else
  printf 'ok   no stale public Maven 3.6 minimum remains\n'
fi

if rg -n 'script.*fallback.*maven\.test\.skip|fallback build.*maven\.test\.skip' \
  "$ROOT/docs/user-guide/05-config-wizard.md"; then
  printf 'FAIL stale wizard fallback warning remains\n'
  failed=$((failed + 1))
else
  printf 'ok   no stale wizard fallback warning remains\n'
fi

printf 'clean-machine-prerequisites-test: %d failed\n' "$failed"
exit "$failed"
