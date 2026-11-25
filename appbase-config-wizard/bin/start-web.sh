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
PID_FILE="$MODULE_DIR/.config-wizard-web.pid"
LOG_FILE="$MODULE_DIR/.config-wizard-web.log"

FORCE_REBUILD=false
PORT=8080
for arg in "$@"; do
  case "$arg" in
    --force-rebuild) FORCE_REBUILD=true ;;
    [0-9]*) PORT="$arg" ;;
  esac
done

if [ "$FORCE_REBUILD" = true ] || [ -z "$JAR" ]; then
  [ "$FORCE_REBUILD" = true ] && echo "Force-rebuilding..." || echo "JAR not found — building..."
  (cd "$MODULE_DIR/.." && mvn clean package -pl appbase-config-wizard -am \
    -DskipTests -P quick-profile)
  JAR="$(jar_lookup)"
fi

if [ -z "$JAR" ]; then
  echo "ERROR: no appbase-config-wizard-*.jar found under $MODULE_DIR/target/ after building." >&2
  exit 1
fi

if [ -f "$PID_FILE" ]; then
  OLD_PID=$(cat "$PID_FILE")
  if kill -0 "$OLD_PID" 2>/dev/null; then
    echo "Appbase Config Wizard already running (PID $OLD_PID). Use stop-web.sh first."
    exit 1
  fi
fi

echo "Starting Appbase Config Wizard web on port $PORT..."
nohup java --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -jar "$JAR" web --port "$PORT" > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"

# Poll for "Open:" line in log (up to 15s)
for i in $(seq 1 30); do
  sleep 0.5
  if grep -q "Open:" "$LOG_FILE" 2>/dev/null; then
    grep "Open:" "$LOG_FILE"
    echo "Appbase Config Wizard started (PID $(cat "$PID_FILE"))"
    exit 0
  fi
done
echo "Warning: server did not signal ready within 15s. Check $LOG_FILE"
exit 1
