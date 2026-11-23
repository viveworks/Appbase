#!/usr/bin/env bash
#
# Light fixture suite for the internal work item 2541 API-documentation acceptance groundwork.
# Runs on macOS /bin/bash 3.2 and delegates assertions to the stdlib-only Python suite.
#
# @author Navaneeth Mysore G
# @author Claude
#
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
exec python3 "$ROOT/scripts/test/api_docs_acceptance_test.py"
