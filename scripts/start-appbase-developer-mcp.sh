#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
artifact="${APPBASE_MCP_API_ARTIFACT:-$repo_root/target/appbase-mcp/api-surface.json}"
pidfile="${APPBASE_MCP_PID_FILE:-$repo_root/target/appbase-mcp/server.pid}"
python="${APPBASE_MCP_PYTHON:-python3}"
generator="${APPBASE_MCP_GENERATOR:-$repo_root/scripts/generate-mcp-api-surface.sh}"
revision="$(git -C "$repo_root" rev-parse HEAD)"

mkdir -p "$(dirname "$artifact")" "$(dirname "$pidfile")"
if [ -L "$pidfile" ]; then
    echo "appbase-developer-mcp: refusing symlink pid file: $pidfile" >&2
    exit 2
fi
if [ -f "$pidfile" ]; then
    old_pid="$(sed -n '1p' "$pidfile" 2>/dev/null || true)"
    if [[ "$old_pid" =~ ^[0-9]+$ ]] && kill -0 "$old_pid" 2>/dev/null; then
        echo "appbase-developer-mcp: already running as PID $old_pid" >&2
        exit 2
    fi
    rm -f "$pidfile"
fi

MCP_API_SOURCE_REVISION="$revision" bash "$generator" "$artifact"

child=""
cleanup() {
    if [ -f "$pidfile" ] && [ "$(sed -n '1p' "$pidfile" 2>/dev/null || true)" = "$child" ]; then
        rm -f "$pidfile"
    fi
}
forward() {
    [ -z "$child" ] || kill -TERM "$child" 2>/dev/null || true
}
trap cleanup EXIT
trap forward HUP INT TERM

# An asynchronous command in a non-interactive shell otherwise inherits /dev/null as stdin. Preserve
# the MCP host's input explicitly before backgrounding so the wrapper remains a transparent stdio
# transport while still being able to record and supervise the child PID.
exec 3<&0
"$python" -m appbase_mcp \
    --api-artifact "$artifact" \
    --repository-root "$repo_root" \
    --expected-revision "$revision" <&3 &
child="$!"
exec 3<&-
printf '%s\n' "$child" > "$pidfile"
wait "$child"
