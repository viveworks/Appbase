#!/usr/bin/env bash
# Verify and publish the immutable commit tested by a CI tier.
#
# @author Navaneeth Mysore G
# @author Claude
set -euo pipefail
: "${EXPECTED_SHA:?EXPECTED_SHA is required}"
: "${GITHUB_OUTPUT:?GITHUB_OUTPUT is required}"
tested_sha="$(git rev-parse HEAD)"
test "$tested_sha" = "$EXPECTED_SHA"
echo "tested_sha=$tested_sha" >> "$GITHUB_OUTPUT"
