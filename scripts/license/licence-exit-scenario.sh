#!/usr/bin/env bash
# Individual executable scenarios for the P05-E02 epic-close matrix (internal work item 2539).
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
id="${1:-}"

case "$id" in
  LI-01) exec /bin/bash "$ROOT/scripts/test/licence_report_test.sh" ;;
  LI-02) exec /bin/bash "$ROOT/scripts/test/aerospike_digest_reachability_test.sh" ;;
  LI-03|LI-04) exec /bin/bash "$ROOT/scripts/test/license_header_gate_test.sh" ;;
  LI-05) exec /bin/bash "$ROOT/scripts/test/license_files_test.sh" ;;
  LI-06|LI-07) exec /bin/bash "$ROOT/scripts/test/notice_check_test.sh" ;;
  LI-08)
    doc="$ROOT/docs/spec/1.0.0/P05/decisions/E02-trademark-position.md"
    grep -q 'appbase\.io' "$doc" && grep -Eq 'pending|proceed|rename|blocked-on-legal' "$doc" \
      && grep -Eqi 'coordinate|package|Sonatype|brand' "$doc"
    ;;
  LI-09) exec /bin/bash "$ROOT/scripts/test/signoff_check_test.sh" ;;
  LI-10) exec /bin/bash "$ROOT/scripts/test/licence_change_gate_test.sh" ;;
  LI-11)
    revision="$(git -C "$ROOT" rev-parse --verify 'HEAD^{commit}')" || exit 1
    tree="$(git -C "$ROOT" rev-parse --verify 'HEAD^{tree}')" || exit 1
    matrix_sha="$(shasum -a 256 "$ROOT/scripts/licence-exit-matrix.tsv" | awk '{print $1}')" || exit 1
    ! grep -q $'\tPENDING\t' "$ROOT/scripts/licence-exit-matrix.tsv" || exit 1
    printf 'LI-11 RECEIPT revision=%s tree=%s matrix_sha256=%s\n' "$revision" "$tree" "$matrix_sha"
    ;;
  *) echo "licence-exit-scenario: expected LI-01 through LI-11" >&2; exit 2 ;;
esac
