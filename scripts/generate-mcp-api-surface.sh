#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
revision="${MCP_API_SOURCE_REVISION:-$(git -C "$repo_root" rev-parse HEAD)}"
output="${1:-$repo_root/appbase_mcp/api-surface.json}"
classes="$(mktemp -d "${TMPDIR:-/tmp}/appbase-mcp-generator.XXXXXX")"
trap 'rm -rf "$classes"' EXIT

javac -d "$classes" \
    "$repo_root/appbase-java/appbase-core/src/main/java/appbase_mcp/generator/ApiSurfaceGenerator.java"
java -cp "$classes" appbase_mcp.generator.ApiSurfaceGenerator "$repo_root" "$revision" "$output"
