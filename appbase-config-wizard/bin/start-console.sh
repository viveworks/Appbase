#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$SCRIPT_DIR")"
# Pick the largest built jar rather than a hardcoded version string -- correct whether target/
# holds a SNAPSHOT or a real release build; the real executable artifact is far larger than the
# sources/shaded/javadoc jars sitting next to it.
jar_lookup() { ls -S "$MODULE_DIR"/target/appbase-config-wizard-*.jar 2>/dev/null \
  | grep -v -- '-sources\.jar$\|-shaded\.jar$\|-javadoc\.jar$' | head -1 || true; }
JAR="$(jar_lookup)"

if [ -z "$JAR" ]; then
  echo "JAR not found — building..."
  (cd "$MODULE_DIR/.." && mvn clean package -pl appbase-config-wizard -am \
    -DskipTests -P quick-profile)
  JAR="$(jar_lookup)"
fi

if [ -z "$JAR" ]; then
  echo "ERROR: no appbase-config-wizard-*.jar found under $MODULE_DIR/target/ after building." >&2
  exit 1
fi

exec java --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -jar "$JAR" "$@"
