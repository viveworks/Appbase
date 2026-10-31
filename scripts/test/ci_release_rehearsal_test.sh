#!/usr/bin/env bash
# Exercise the release rehearsal as a command-level safety contract.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUNNER="$ROOT/.github/scripts/ci-release-rehearsal.sh"
WORKFLOW="$ROOT/.github/workflows/ci-verify.yml"
CONTRACT_CHECKER="$ROOT/scripts/test/ci_release_rehearsal_contract.py"
PASSED=0
FAILED=0

ok() { echo "  ok   $1"; PASSED=$((PASSED + 1)); }
bad() { echo "  FAIL $1" >&2; FAILED=$((FAILED + 1)); }

if [[ ! -f "$RUNNER" ]]; then
  echo "FAIL missing release rehearsal runner: $RUNNER" >&2
  exit 1
fi
if [[ ! -x "$0" ]]; then
  echo "FAIL release rehearsal contract test is not executable: $0" >&2
  exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf -- "$TMP"' EXIT

new_fixture() {
  local fixture="$1"
  mkdir -p "$fixture/bin"
  printf '<project><version>1.0.1-SNAPSHOT</version></project>\n' > "$fixture/pom.xml"
  printf '%s\n' '#!/usr/bin/env bash' \
    'printf "build" >> "$REHEARSAL_LOG"' \
    'printf " <%s>" "$@" >> "$REHEARSAL_LOG"' \
    'printf "\n" >> "$REHEARSAL_LOG"' \
    '[[ "${REHEARSAL_MUTATION:-none}" != build-fail ]] || exit 43' > "$fixture/build.sh"
  printf '%s\n' '#!/usr/bin/env bash' \
    'printf "mvn" >> "$REHEARSAL_LOG"' \
    'printf " <%s>" "$@" >> "$REHEARSAL_LOG"' \
    'printf "\n" >> "$REHEARSAL_LOG"' \
    'case " $* " in' \
    '  *" release:prepare "*)' \
    '    case "${REHEARSAL_MUTATION:-none}" in' \
    '      pom) printf "<project><version>1.0.0</version></project>\n" > pom.xml ;;' \
    '      tag) git tag rehearsal-mutant ;;' \
    '      prepare-fail) exit 42 ;;' \
    '    esac' \
    '    ;;' \
    'esac' > "$fixture/bin/mvn"
  chmod +x "$fixture/bin/mvn"
  git -C "$fixture" init -q
  git -C "$fixture" config user.name 'Release Rehearsal Test'
  git -C "$fixture" config user.email 'release-rehearsal@example.invalid'
  git -C "$fixture" add pom.xml build.sh
  git -C "$fixture" commit -qm fixture
}

run_fixture() {
  local fixture="$1" mutation="$2"
  (
    cd "$fixture"
    PATH="$fixture/bin:$PATH" \
      REHEARSAL_LOG="$fixture/commands.log" \
      REHEARSAL_MUTATION="$mutation" \
      bash "$RUNNER"
  )
}

HAPPY="$TMP/happy"
new_fixture "$HAPPY"
if run_fixture "$HAPPY" none; then
  ok 'clean dry-run rehearsal succeeds'
else
  bad 'clean dry-run rehearsal succeeds'
fi

if [[ "$(grep -Fxc 'build <--mode> <compile>' "$HAPPY/commands.log")" -eq 1 ]]; then
  ok 'whole-repository compile runs first'
else
  bad 'whole-repository compile runs first'
fi

if [[ "$(wc -l < "$HAPPY/commands.log" | tr -d ' ')" -eq 3 ]] &&
   [[ "$(grep -Fxc 'mvn <-B> <-ntp> <-P> <release-profile> <release:prepare> <-DdryRun=true> <-DpushChanges=false> <-DreleaseVersion=1.0.0> <-DdevelopmentVersion=1.0.1-SNAPSHOT> <-Dgpg.skip=true> <-Darguments=-Dgpg.skip=true -DskipTests>' "$HAPPY/commands.log")" -eq 1 ]] &&
   [[ "$(grep -Fxc 'mvn <-B> <-ntp> <-P> <release-profile> <release:clean>' "$HAPPY/commands.log")" -eq 1 ]]; then
  ok 'only dry-run prepare and cleanup use the release plugin'
else
  bad 'only dry-run prepare and cleanup use the release plugin'
fi

if [[ "$(grep -Fc '<-Dgpg.skip=true>' "$HAPPY/commands.log")" -eq 1 ]] &&
   [[ "$(grep -Fc '<-Darguments=-Dgpg.skip=true -DskipTests>' "$HAPPY/commands.log")" -eq 1 ]] &&
   grep -F '<release:prepare>' "$HAPPY/commands.log" | grep -Fq '<-Darguments=-Dgpg.skip=true -DskipTests>'; then
  ok 'rehearsal alone forwards signing and duplicate-test skips to the preparation build'
else
  bad 'rehearsal alone forwards signing and duplicate-test skips to the preparation build'
fi

if ! grep -Eq 'release:perform|(^|[ <])deploy([ >]|$)|git push' "$HAPPY/commands.log"; then
  ok 'rehearsal invokes no perform, deploy, or push operation'
else
  bad 'rehearsal invokes no perform, deploy, or push operation'
fi

if ! grep -Fq 'appbase-config-wizard.skipFrontend' "$HAPPY/commands.log"; then
  ok 'rehearsal does not bypass the shipping Flutter frontend'
else
  bad 'rehearsal does not bypass the shipping Flutter frontend'
fi

POM_MUTANT="$TMP/pom-mutant"
new_fixture "$POM_MUTANT"
if run_fixture "$POM_MUTANT" pom; then
  bad 'tracked POM mutation fails closed'
else
  ok 'tracked POM mutation fails closed'
fi
if [[ "$(grep -Fc '<release:clean>' "$POM_MUTANT/commands.log")" -eq 1 ]]; then
  ok 'cleanup still runs after a POM integrity failure'
else
  bad 'cleanup still runs after a POM integrity failure'
fi

TAG_MUTANT="$TMP/tag-mutant"
new_fixture "$TAG_MUTANT"
if run_fixture "$TAG_MUTANT" tag; then
  bad 'local tag mutation fails closed'
else
  ok 'local tag mutation fails closed'
fi

PREPARE_FAIL="$TMP/prepare-fail"
new_fixture "$PREPARE_FAIL"
set +e
run_fixture "$PREPARE_FAIL" prepare-fail
prepare_rc=$?
set -e
if [[ "$prepare_rc" -eq 42 ]] && [[ "$(grep -Fc '<release:clean>' "$PREPARE_FAIL/commands.log")" -eq 1 ]]; then
  ok 'prepare failure is preserved and cleanup still runs'
else
  bad 'prepare failure is preserved and cleanup still runs'
fi

BUILD_FAIL="$TMP/build-fail"
new_fixture "$BUILD_FAIL"
set +e
run_fixture "$BUILD_FAIL" build-fail
build_rc=$?
set -e
if [[ "$build_rc" -eq 43 ]] && ! grep -Fq '<release:prepare>' "$BUILD_FAIL/commands.log"; then
  ok 'compile failure prevents every release-plugin invocation'
else
  bad 'compile failure prevents every release-plugin invocation'
fi

workflow_contract() {
  local file="$1" input job summary offline
  input="$(awk '
    /^      run_release_rehearsal:$/ { capture = 1 }
    capture && seen && /^      [a-zA-Z0-9_-]+:$/ { exit }
    capture { print; seen = 1 }
  ' "$file")"
  job="$(awk '
    /^  release-profile-rehearsal:$/ { capture = 1 }
    capture && seen && /^  [a-zA-Z0-9_-]+:$/ { exit }
    capture { print; seen = 1 }
  ' "$file")"
  summary="$(awk '
    /^  summary:$/ { capture = 1 }
    capture && seen && /^  [a-zA-Z0-9_-]+:$/ { exit }
    capture { print; seen = 1 }
  ' "$file")"
  offline="$(awk '
    /^  offline-shell-suites:$/ { capture = 1 }
    capture && seen && /^  [a-zA-Z0-9_-]+:$/ { exit }
    capture { print; seen = 1 }
  ' "$file")"
  [[ "$(grep -Fxc '        type: boolean' <<< "$input")" -eq 1 ]] &&
    [[ "$(grep -Fxc '        default: false' <<< "$input")" -eq 1 ]] &&
    grep -Fq "github.event_name == 'workflow_dispatch'" <<< "$job" &&
    grep -Fq "github.event.inputs.run_release_rehearsal == 'true'" <<< "$job" &&
    grep -Fq '    timeout-minutes: 45' <<< "$job" &&
    grep -Fq '          persist-credentials: false' <<< "$job" &&
    grep -Fq '          FLUTTER_VERSION=3.47.7' <<< "$job" &&
    grep -Fq '          FLUTTER_SHA256=af7455f540df951184c7953fbe3cf884766e7541dacf8571f9f582db03dd6230' <<< "$job" &&
    grep -Fq 'flutter_linux_${FLUTTER_VERSION}-stable.tar.xz' <<< "$job" &&
    grep -Fq 'sha256sum -c -' <<< "$job" &&
    grep -Fq 'echo "$RUNNER_TEMP/flutter/bin" >> "$GITHUB_PATH"' <<< "$job" &&
    grep -Fq '          flutter pub get' <<< "$job" &&
    grep -Fq '          git diff --exit-code -- pubspec.lock' <<< "$job" &&
    grep -Fq 'run: bash .github/scripts/ci-release-rehearsal.sh' <<< "$job" &&
    grep -Fq 'release-profile-rehearsal' <<< "$(grep 'needs:' <<< "$summary")" &&
    python3 "$CONTRACT_CHECKER" verify "$file"
}

# This file ships into both repos (classified PUBLIC for export). Two genuinely different,
# both-correct implementations exist for "the summary job accounts for the release-rehearsal
# job's result": a named-env-var receipt-table style and a generic selected/result JSON style.
# Accept either --
# this is not a weaker check, it is the same property proven by whichever real mechanism is
# actually present. The generic mechanism has no per-job tested_sha cross-check, so this helper
# does not require one when the generic form is what's present.
#
# APP review finding (receipt 6527519002, round 1 Medium): the prior version of this helper
# accepted EITHER form's marker text alone, which a comment or dead code containing that exact
# text could also satisfy.
#
# APP review finding (receipt 6528263562, round 2 Medium): round 1's fix still accepted the
# public form's two strings merely co-occurring anywhere in the section -- dead code reachable
# by neither a `#` nor isolated from the real path (e.g. nested under `if False:`, or split
# into an unreachable function) still satisfied two independent `grep -Fq` substring checks.
# Fixed by requiring each line's EXACT real indentation (14 and 20 spaces), not a substring
# match that ignores leading whitespace. This is sound, not just
# narrow: Python requires MORE indentation to nest code inside any guard or function body, so
# reindenting either line to make it dead necessarily breaks the exact-indentation match.
#
# Follow-up internal work item 2883 replaces those textual proofs. The verifier parses the workflow as YAML,
# parses the public summary program as a Python AST, and executes the exact parsed private or
# public summary scalar with sentinel results. A disconnected assignment, nested dead block,
# or triple-quoted marker is therefore not an active module-scope gate.
summary_accounts_for_rehearsal_result() {
  printf '%s\n' "$1" | python3 "$CONTRACT_CHECKER" summary-fragment
}

# APP review finding (receipt 6527519002, round 1 Medium): accepting the bare filename
# anywhere in the offline-shell-suites section let a comment-only mention pass.
#
# APP review finding (receipt 6528263562, round 2 Medium): round 1's fix still accepted the
# filename and the invocation marker merely co-occurring anywhere in the section, which a
# disconnected pair (filename in a comment or a dead step, invocation marker in a real but
# unrelated step) could also satisfy. For the public loop form, bound the filename
# specifically to the `for suite in ...; do` list itself, and the invocation marker
# specifically to that same loop's own body (between its `do` and `done`).
#
# Follow-up internal work item 2883 executes the exact YAML-parsed offline command in an isolated fixture and
# requires the named rehearsal suite to write a sentinel. A filename in a comment, unrelated
# step, argument-free runner, or non-executing loop cannot satisfy that proof.
offline_arm_invokes_test() {
  [[ "$2" == 'ci_release_rehearsal_test.sh' ]] || return 1
  printf '%s\n' "$1" | python3 "$CONTRACT_CHECKER" offline-fragment
}

if workflow_contract "$WORKFLOW"; then
  ok 'workflow exposes and accounts for a credential-free, false-by-default opt-in job'
else
  bad 'workflow exposes and accounts for a credential-free, false-by-default opt-in job'
fi

if python3 "$CONTRACT_CHECKER" control disconnected-summary; then
  ok 'test_AC2883_1_disconnected_summary_markers_fail_closed'
else
  bad 'test_AC2883_1_disconnected_summary_markers_fail_closed'
fi
if python3 "$CONTRACT_CHECKER" control runner-not-executed; then
  ok 'test_AC2883_2_runner_requires_execution_evidence'
else
  bad 'test_AC2883_2_runner_requires_execution_evidence'
fi
if python3 "$CONTRACT_CHECKER" control docstring-markers; then
  ok 'test_AC2883_3_docstring_markers_fail_real_ast_parse'
else
  bad 'test_AC2883_3_docstring_markers_fail_real_ast_parse'
fi
if python3 "$CONTRACT_CHECKER" control private-decoy-row; then
  ok 'test_AC2883_1_private_release_row_rejects_disconnected_decoy'
else
  bad 'test_AC2883_1_private_release_row_rejects_disconnected_decoy'
fi
if python3 "$CONTRACT_CHECKER" control runner-verifier-env; then
  ok 'test_AC2883_2_runner_rejects_verifier_only_execution_condition'
else
  bad 'test_AC2883_2_runner_rejects_verifier_only_execution_condition'
fi
if python3 "$CONTRACT_CHECKER" control missing-runner; then
  ok 'EXTRA_offline_binding: a nonexistent runner fails closed'
else
  bad 'EXTRA_offline_binding: a nonexistent runner was accepted'
fi
if python3 "$CONTRACT_CHECKER" control existing-non-runner; then
  ok 'EXTRA_offline_binding: an existing non-runner executable fails closed'
else
  bad 'EXTRA_offline_binding: an existing non-runner executable was accepted'
fi
if python3 - "$ROOT/pom.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET

ns = {"m": "http://maven.apache.org/POM/4.0.0"}
root = ET.parse(sys.argv[1]).getroot()
profile = next(
    item
    for item in root.findall("m:profiles/m:profile", ns)
    if item.findtext("m:id", default="", namespaces=ns) == "release-profile"
)
frontend_skip = profile.findtext(
    "m:properties/m:appbase-config-wizard.skipFrontend",
    default="",
    namespaces=ns,
)
plugins = profile.findall(".//m:plugin", ns)
gpg = next(
    item
    for item in plugins
    if item.findtext("m:artifactId", default="", namespaces=ns) == "maven-gpg-plugin"
    and item.find("m:executions", ns) is not None
)
sign = next(
    item
    for item in gpg.findall("m:executions/m:execution", ns)
    if item.findtext("m:id", default="", namespaces=ns) == "sign-artifacts"
)
goals = [item.text or "" for item in sign.findall("m:goals/m:goal", ns)]
skips = [
    (item.text or "").strip().lower()
    for item in gpg.findall(".//m:skip", ns)
]
release = next(
    item
    for item in plugins
    if item.findtext("m:artifactId", default="", namespaces=ns) == "maven-release-plugin"
    and item.find("m:configuration/m:preparationGoals", ns) is not None
)
preparation_goals = release.findtext(
    "m:configuration/m:preparationGoals",
    default="",
    namespaces=ns,
)
if sign.findtext("m:phase", default="", namespaces=ns) != "verify" or goals != ["sign"]:
    raise SystemExit("release profile no longer binds sign-artifacts:sign to verify")
if "true" in skips:
    raise SystemExit("real release signing is statically skipped")
if frontend_skip != "false":
    raise SystemExit("real release profile no longer builds the Config Wizard frontend")
if preparation_goals != "clean verify -Denforcer.skip=true":
    raise SystemExit("real release preparation goals were changed by the rehearsal-only test skip")
if release.find("m:configuration/m:arguments", ns) is not None:
    raise SystemExit("rehearsal-only forwarded arguments leaked into the real release POM")
PY
then
  ok 'real release profile still signs, builds Flutter, and runs its unchanged preparation goals'
else
  bad 'real release profile still signs, builds Flutter, and runs its unchanged preparation goals'
fi

# APP review receipt 6528263562 (round 2): planted negative controls for the two structural-
# binding helpers above, shipped as real regression coverage rather than only verified ad hoc
# before this push. Each decoy is REAL, non-commented Python/bash -- not merely text the prior
# weaker checks could already tell apart -- reachable by neither a `#` nor isolated from the
# real execution path, and each must still be rejected.

REAL_SUMMARY="$(cat <<'TEXT'
          selected = {
              'changed-modules': True,
              'release-profile-rehearsal': os.environ['RUN_RELEASE'] == 'true',
          }
          failed = {name: data['result'] for name, data in needs.items()
                    if data['result'] != ('success' if selected[name] else 'skipped')}
TEXT
)"
if summary_accounts_for_rehearsal_result "$REAL_SUMMARY"; then
  ok 'EXTRA_summary_binding: the real public summary template is accepted'
else
  bad 'EXTRA_summary_binding: the real public summary template was rejected'
fi

DEAD_UNDER_GUARD="$(cat <<'TEXT'
          if False:
              selected = {
                  'release-profile-rehearsal': os.environ['RUN_RELEASE'] == 'true',
              }
              failed = {name: data['result'] for name, data in needs.items()
                        if data['result'] != ('success' if selected[name] else 'skipped')}
TEXT
)"
if summary_accounts_for_rehearsal_result "$DEAD_UNDER_GUARD"; then
  bad 'EXTRA_summary_binding: real, non-commented dead code under "if False:" was wrongly accepted'
else
  ok 'EXTRA_summary_binding: real, non-commented dead code under "if False:" is rejected'
fi

DEAD_IN_FUNCTION="$(cat <<'TEXT'
          selected = {
              'release-profile-rehearsal': os.environ['RUN_RELEASE'] == 'true',
          }
          def unused_dead_function():
              failed = {name: data['result'] for name, data in needs.items()
                        if data['result'] != ('success' if selected[name] else 'skipped')}
TEXT
)"
if summary_accounts_for_rehearsal_result "$DEAD_IN_FUNCTION"; then
  bad 'EXTRA_summary_binding: an unreachable dead function containing the comparison was wrongly accepted'
else
  ok 'EXTRA_summary_binding: an unreachable dead function containing the comparison is rejected'
fi

REAL_OFFLINE="$(cat <<'TEXT'
      - name: Public offline product contracts
        run: |
          set -euo pipefail
          for suite in             aerospike_container_contract_test.sh             ci_release_rehearsal_test.sh; do
            timeout 900 bash "scripts/test/$suite"
          done
TEXT
)"
if offline_arm_invokes_test "$REAL_OFFLINE" 'ci_release_rehearsal_test.sh'; then
  ok 'EXTRA_offline_binding: the real public offline loop is accepted'
else
  bad 'EXTRA_offline_binding: the real public offline loop was rejected'
fi

NAME_IN_COMMENT_INVOCATION_ELSEWHERE="$(cat <<'TEXT'
      # ci_release_rehearsal_test.sh is one of the suites
      - name: Unrelated dead step
        run: |
          if false; then
            bash "scripts/test/$suite"
          fi
TEXT
)"
if offline_arm_invokes_test "$NAME_IN_COMMENT_INVOCATION_ELSEWHERE" 'ci_release_rehearsal_test.sh'; then
  bad 'EXTRA_offline_binding: a commented filename plus an unrelated dead invocation was wrongly accepted'
else
  ok 'EXTRA_offline_binding: a commented filename plus an unrelated dead invocation is rejected'
fi

NAME_IN_LOOP_INVOCATION_DEAD="$(cat <<'TEXT'
          for suite in ci_release_rehearsal_test.sh other.sh; do
            echo "pretending to run $suite"
          done
          if false; then
            bash "scripts/test/$suite"
          fi
TEXT
)"
if offline_arm_invokes_test "$NAME_IN_LOOP_INVOCATION_DEAD" 'ci_release_rehearsal_test.sh'; then
  bad 'EXTRA_offline_binding: the named suite in a real loop whose body never bash-invokes it was wrongly accepted'
else
  ok 'EXTRA_offline_binding: the named suite in a real loop whose body never bash-invokes it is rejected'
fi

# APP review receipt 6528849657 (round 3): planted controls for the three remaining gaps named
# in that receipt -- each is real, reachable text (no comment prefix, syntactically live), just
# disconnected from the real decision/invocation path in a way round 2's checks could not tell.

ENV_VAR_DECLARED_NEVER_CONSUMED="$(cat <<'TEXT'
          RELEASE_REHEARSAL_RESULT: ${{ needs.release-profile-rehearsal.result }}
          echo "declared but never assigned to release_rehearsal_receipt_result anywhere"
TEXT
)"
if summary_accounts_for_rehearsal_result "$ENV_VAR_DECLARED_NEVER_CONSUMED"; then
  bad 'EXTRA_summary_binding: a private env-var line declared but never consumed downstream was wrongly accepted'
else
  ok 'EXTRA_summary_binding: a private env-var line declared but never consumed downstream is rejected'
fi

INSIDE_DOCSTRING="$(cat <<'TEXT'
          """
          selected = {
              'release-profile-rehearsal': os.environ['RUN_RELEASE'] == 'true',
          }
          failed = {name: data['result'] for name, data in needs.items()
                    if data['result'] != ('success' if selected[name] else 'skipped')}
          """
TEXT
)"
if summary_accounts_for_rehearsal_result "$INSIDE_DOCSTRING"; then
  bad 'EXTRA_summary_binding: the exact lines inside an inactive triple-quoted docstring were wrongly accepted'
else
  ok 'EXTRA_summary_binding: the exact lines inside an inactive triple-quoted docstring are rejected'
fi

echo "ci-release-rehearsal-test: $PASSED passed, $FAILED failed"
[[ "$FAILED" -eq 0 ]]
