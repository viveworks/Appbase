#!/usr/bin/env bash
# Light AD-09 Javadoc visibility contract; compatible with macOS /bin/bash 3.2.
#
# @author Navaneeth Mysore G
# @author Claude
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
exec python3 "$ROOT/scripts/test/javadoc_visibility_test.py"
