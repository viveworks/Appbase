#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
pidfile="${APPBASE_MCP_PID_FILE:-$repo_root/target/appbase-mcp/server.pid}"

if [ -L "$pidfile" ]; then
    echo "appbase-developer-mcp: refusing symlink pid file: $pidfile" >&2
    exit 2
fi
if [ ! -f "$pidfile" ]; then
    echo "appbase-developer-mcp: not running (no pid file)"
    exit 0
fi
pid="$(sed -n '1p' "$pidfile" 2>/dev/null || true)"
if [[ ! "$pid" =~ ^[0-9]+$ ]]; then
    echo "appbase-developer-mcp: invalid pid file: $pidfile" >&2
    exit 2
fi
if ! kill -0 "$pid" 2>/dev/null; then
    rm -f "$pidfile"
    echo "appbase-developer-mcp: removed stale pid file"
    exit 0
fi
command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
case "$command_line" in
    *"-m appbase_mcp"*"--repository-root $repo_root"*) ;;
    *)
        echo "appbase-developer-mcp: refusing to signal unrelated PID $pid" >&2
        exit 2
        ;;
esac

kill -TERM "$pid"
attempt=0
while kill -0 "$pid" 2>/dev/null && [ "$attempt" -lt 50 ]; do
    sleep 0.1
    attempt=$((attempt + 1))
done
if kill -0 "$pid" 2>/dev/null; then
    echo "appbase-developer-mcp: PID $pid did not stop" >&2
    exit 1
fi
rm -f "$pidfile"
echo "appbase-developer-mcp: stopped PID $pid"
