#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
artifact="${1:-$repo_root/appbase_mcp/api-surface.json}"

if [[ ! -f "$artifact" ]]; then
    echo "generated API artifact is missing: $artifact" >&2
    exit 1
fi

classpath="$({
    jq -r '.symbols[].source_path' "$artifact" \
        | sed -n 's#\(/src/main/java/.*\)$##p' \
        | sort -u \
        | while IFS= read -r module; do
            classes="$repo_root/$module/target/classes"
            if [[ -d "$classes" ]]; then
                printf '%s\n' "$classes"
            fi
        done
} | paste -sd: -)"

if [[ -z "$classpath" ]]; then
    echo "no compiled Appbase target/classes directories found for generated catalog" >&2
    exit 1
fi

exec python3 "$repo_root/scripts/verify-mcp-api-surface.py" "$artifact" --classpath "$classpath"
