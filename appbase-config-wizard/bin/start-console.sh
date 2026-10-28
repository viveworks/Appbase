#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$SCRIPT_DIR")"
JAR="$MODULE_DIR/target/appbase-config-wizard-1.0.0-SNAPSHOT.jar"

if [ ! -f "$JAR" ]; then
  echo "JAR not found — building..."
  (cd "$MODULE_DIR/.." && mvn clean package -pl appbase-config-wizard -am \
    -DskipTests -P quick-profile)
fi

exec java --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -jar "$JAR" "$@"
