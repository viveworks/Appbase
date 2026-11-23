#!/usr/bin/env bash
#
# Behavioral coverage for the fast API-inventory gate and each operational caller.
#
# @author Navaneeth Mysore G
# @author Claude
#
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TMP="$(mktemp -d "${TMPDIR:-/tmp}/api-inventory-callers.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT

passed=0
failed=0
real_suite_ran=0
pass() { passed=$((passed + 1)); printf 'PASS: %s\n' "$1"; }
fail() { failed=$((failed + 1)); printf 'FAIL: %s\n' "$1" >&2; }

assert_eq() {
  name="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then pass "$name"; else fail "$name (expected $expected, got $actual)"; fi
}
make_wrapper_fixture() {
  fixture="$1"
  mkdir -p "$fixture/scripts" "$fixture/bin"
  cp "$ROOT/scripts/api-inventory-fast-check.sh" "$fixture/scripts/"
  printf 'consumer\n' > "$fixture/README.md"
  cat > "$fixture/bin/python3" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$TRACE"
case "$*" in
  *check-consumer*) exit "${CONSUMER_RC:-0}" ;;
  *check) exit "${CHECK_RC:-0}" ;;
esac
exit 99
EOF
  chmod +x "$fixture/bin/python3"
}
test_wrapper_commands_and_failures() {
  fixture="$TMP/wrapper with spaces"
  make_wrapper_fixture "$fixture"
  : > "$TMP/trace"
  (cd "$TMP" && TRACE="$TMP/trace" PATH="$fixture/bin:$PATH" bash "$fixture/scripts/api-inventory-fast-check.sh") >"$TMP/out" 2>"$TMP/err"
  rc=$?
  assert_eq 'wrapper succeeds' 0 "$rc"
  assert_eq 'wrapper runs exactly the two commands in order' \
    $'scripts/api-inventory.py check\nscripts/api-inventory.py check-consumer README.md' "$(cat "$TMP/trace")"

  : > "$TMP/trace"
  (cd "$fixture" && TRACE="$TMP/trace" CHECK_RC=23 PATH="$fixture/bin:$PATH" bash scripts/api-inventory-fast-check.sh) >"$TMP/out" 2>"$TMP/err"
  rc=$?
  assert_eq 'check exit code is preserved' 23 "$rc"
  assert_eq 'check failure stops before consumer command' 'scripts/api-inventory.py check' "$(cat "$TMP/trace")"
  grep -Fq 'python3 scripts/api-inventory.py check' "$TMP/err" && pass 'check failure names command' || fail 'check failure names command'

  for wanted_rc in 1 2 127; do
    : > "$TMP/trace"
    (cd "$fixture" && TRACE="$TMP/trace" CHECK_RC="$wanted_rc" PATH="$fixture/bin:$PATH" bash scripts/api-inventory-fast-check.sh) >"$TMP/out" 2>"$TMP/err"
    assert_eq "check preserves exit $wanted_rc" "$wanted_rc" "$?"
  done

  : > "$TMP/trace"
  (cd "$fixture" && TRACE="$TMP/trace" CONSUMER_RC=37 PATH="$fixture/bin:$PATH" bash scripts/api-inventory-fast-check.sh) >"$TMP/out" 2>"$TMP/err"
  rc=$?
  assert_eq 'consumer exit code is preserved' 37 "$rc"
  grep -Fq 'python3 scripts/api-inventory.py check-consumer README.md' "$TMP/err" && pass 'consumer failure names command' || fail 'consumer failure names command'

  : > "$fixture/README.md"
  : > "$TMP/trace"
  (cd "$fixture" && TRACE="$TMP/trace" PATH="$fixture/bin:$PATH" bash scripts/api-inventory-fast-check.sh) >"$TMP/out" 2>"$TMP/err"
  rc=$?
  [ "$rc" -ne 0 ] && pass 'empty consumer fails closed' || fail 'empty consumer fails closed'
  assert_eq 'empty consumer runs no Python' '' "$(cat "$TMP/trace")"
  grep -Fq 'python3 scripts/api-inventory.py check-consumer README.md' "$TMP/err" && pass 'empty consumer error names command' || fail 'empty consumer error names command'

  rm "$fixture/README.md"
  (cd "$fixture" && TRACE="$TMP/trace" PATH="$fixture/bin:$PATH" bash scripts/api-inventory-fast-check.sh) >"$TMP/out" 2>"$TMP/err"
  rc=$?
  [ "$rc" -ne 0 ] && pass 'missing consumer fails closed' || fail 'missing consumer fails closed'

  mkdir "$fixture/README.md"
  (cd "$TMP" && TRACE="$TMP/trace" PATH="$fixture/bin:$PATH" bash "$fixture/scripts/api-inventory-fast-check.sh") >"$TMP/out" 2>"$TMP/err"
  assert_eq 'directory consumer fails closed' 2 "$?"
  rmdir "$fixture/README.md"
  printf 'consumer\n' > "$fixture/README.md"
  chmod 000 "$fixture/README.md"
  if [ ! -r "$fixture/README.md" ]; then
    (cd "$TMP" && TRACE="$TMP/trace" PATH="$fixture/bin:$PATH" bash "$fixture/scripts/api-inventory-fast-check.sh") >"$TMP/out" 2>"$TMP/err"
    assert_eq 'unreadable consumer fails closed' 2 "$?"
  else
    pass 'unreadable consumer check not representable for privileged user'
  fi
  chmod 600 "$fixture/README.md"
}

make_gate_stub() {
  path="$1" label="$2" rc="${3:-0}"
  mkdir -p "$(dirname "$path")"
  cat > "$path" <<EOF
#!/usr/bin/env bash
echo '$label' >> "\$CALLER_TRACE"
exit $rc
EOF
  chmod +x "$path"
}

assert_summary_semantics() {
  needs="$TMP/summary.needs"
  awk '
    /^  summary:/ { in_summary=1; next }
    in_summary && /^  [^ ]/ { exit }
    in_summary && /^    needs:/ {
      sub(/^    needs:[[:space:]]*/, "")
      gsub(/[\[\],]/, " ")
      for (i=1; i<=NF; i++) print $i
      exit
    }
  ' "$ROOT/.github/workflows/ci-verify.yml" | sort > "$needs"
  printf '%s\n' api-inventory changed-modules maven-compile | sort > "$TMP/expected.needs"
  if cmp -s "$TMP/expected.needs" "$needs"; then
    pass 'summary semantically depends on all three gates'
  else
    fail 'summary semantically depends on all three gates'
  fi

  awk '
    /^  summary:/ { in_summary=1; next }
    in_summary && /^  [^ ]/ { exit }
    in_summary && /^        run: \|/ { in_run=1; next }
    in_run {
      if ($0 ~ /^          /) { sub(/^          /, ""); print; next }
      exit
    }
  ' "$ROOT/.github/workflows/ci-verify.yml" > "$TMP/summary.sh"

  run_summary_case success PASS
  run_summary_case failure FAIL
  return 0
}

run_summary_case() {
  inventory_result="$1" expected_verdict="$2"
  summary_out="$TMP/summary-$inventory_result.out"
  : > "$summary_out"
  REPO=example/Appbase PR=1 EVENT=pull_request BASE_REF=develop BASE_SHA=base \
    HEAD_SHA=head TREE=tree WORKFLOW_BLOB=blob MODULES=ALL RUN_URL=local ATTEMPT=1 \
    CHANGED_RESULT=success COMPILE_RESULT=success INVENTORY_RESULT="$inventory_result" \
    GITHUB_STEP_SUMMARY="$summary_out" ImageVersion=test bash "$TMP/summary.sh"
  rc=$?
  if [ "$rc" -eq 0 ] && grep -Fq "verdict=$expected_verdict" "$summary_out"; then
    pass "summary computes $expected_verdict when inventory is $inventory_result"
  else
    fail "summary computes $expected_verdict when inventory is $inventory_result"
  fi
}

test_real_inventory_suite() {
  suite_log="$TMP/real-inventory-suite.log"
  /bin/bash "$ROOT/scripts/test/inventory_test.sh" >"$suite_log" 2>&1
  rc=$?
  if [ "$rc" -eq 0 ] && grep -Eq '^passed:[[:space:]]+90[[:space:]]+failed:[[:space:]]+0$' "$suite_log"; then
    real_suite_ran=1
    pass 'caller harness composes the real inventory suite (90/0)'
    return 0
  fi
  tail -n 10 "$suite_log" >&2
  fail "real inventory suite propagates failure (rc $rc)"
  return 1
}

test_build_caller() {
  fixture="$TMP/build"
  mkdir -p "$fixture/scripts"
  cp "$ROOT/build.sh" "$fixture/build.sh"
  make_gate_stub "$fixture/scripts/api-inventory-fast-check.sh" build-gate 41
  (cd "$fixture" && CALLER_TRACE="$TMP/build.trace" bash ./build.sh --mode quick) >"$TMP/out" 2>"$TMP/err"
  assert_eq 'build preserves blocking gate failure' 41 "$?"
  assert_eq 'build calls inventory gate once' build-gate "$(cat "$TMP/build.trace")"

  : > "$TMP/build.trace"
  (cd "$fixture" && CALLER_TRACE="$TMP/build.trace" bash ./build.sh --help) >"$TMP/out" 2>"$TMP/err"
  assert_eq 'build help retains documented exit status' 1 "$?"
  assert_eq 'build help has no gate side effect' '' "$(cat "$TMP/build.trace")"

  rm "$fixture/scripts/api-inventory-fast-check.sh"
  (cd "$fixture" && CALLER_TRACE="$TMP/build.trace" bash ./build.sh --mode quick) >"$TMP/out" 2>"$TMP/err"
  assert_eq 'build blocks when wrapper is missing' 127 "$?"
}

test_hook_caller() {
  fixture="$TMP/hook"
  mkdir -p "$fixture/.githooks" "$fixture/scripts" "$fixture/bin"
  cp "$ROOT/.githooks/pre-push" "$fixture/.githooks/pre-push"
  make_gate_stub "$fixture/scripts/api-inventory-fast-check.sh" hook-gate
  make_gate_stub "$fixture/scripts/lint-branch-name.sh" branch-lint
  head_sha=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
  cat > "$fixture/bin/git" <<EOF
#!/usr/bin/env bash
if [ "\$1 \$2" = 'rev-parse --show-toplevel' ]; then printf '%s\n' '$fixture'; exit 0; fi
exit 88
EOF
  chmod +x "$fixture/bin/git"
  ref_line="refs/heads/good $head_sha refs/heads/feature-good 0000000000000000000000000000000000000000"
  printf '%s\n' "$ref_line" | PATH="$fixture/bin:$PATH" CALLER_TRACE="$TMP/hook.trace" bash "$fixture/.githooks/pre-push" >"$TMP/out" 2>"$TMP/err"
  assert_eq 'pre-push invokes checkout gate once and preserves ref-list branch lint' $'hook-gate\nbranch-lint' "$(cat "$TMP/hook.trace")"

  make_gate_stub "$fixture/scripts/api-inventory-fast-check.sh" hook-gate 43
  : > "$TMP/hook.trace"
  printf '%s\n' "$ref_line" | PATH="$fixture/bin:$PATH" CALLER_TRACE="$TMP/hook.trace" bash "$fixture/.githooks/pre-push" >"$TMP/out" 2>"$TMP/err"
  assert_eq 'pre-push preserves blocking gate failure' 43 "$?"
  assert_eq 'blocking gate stops before branch lint' hook-gate "$(cat "$TMP/hook.trace")"

  rm "$fixture/scripts/api-inventory-fast-check.sh"
  printf '%s\n' "$ref_line" | PATH="$fixture/bin:$PATH" CALLER_TRACE="$TMP/hook.trace" bash "$fixture/.githooks/pre-push" >"$TMP/out" 2>"$TMP/err"
  assert_eq 'pre-push blocks when wrapper is missing' 127 "$?"
}

test_ci_caller() {
  fixture="$TMP/ci"
  mkdir -p "$fixture/scripts/test"
  make_gate_stub "$fixture/scripts/api-inventory-fast-check.sh" ci-fast
  make_gate_stub "$fixture/scripts/test/inventory_test.sh" ci-suite
  awk '
    /^  api-inventory:/ { in_job=1 }
    in_job && /^  [a-zA-Z0-9_-]+:/ && !/^  api-inventory:/ { exit }
    in_job && /^        run: \|/ { in_run=1; next }
    in_run {
      if ($0 ~ /^          /) { sub(/^          /, ""); print; next }
      exit
    }
  ' "$ROOT/.github/workflows/ci-verify.yml" > "$fixture/run.sh"
  (cd "$fixture" && CALLER_TRACE="$TMP/ci.trace" bash run.sh) >"$TMP/out" 2>"$TMP/err"
  rc=$?
  assert_eq 'CI inventory job succeeds' 0 "$rc"
  assert_eq 'CI runs fast gate then fixture suite' $'ci-fast\nci-suite' "$(cat "$TMP/ci.trace")"

  make_gate_stub "$fixture/scripts/api-inventory-fast-check.sh" ci-fast 47
  : > "$TMP/ci.trace"
  (cd "$fixture" && CALLER_TRACE="$TMP/ci.trace" bash run.sh) >"$TMP/out" 2>"$TMP/err"
  assert_eq 'CI propagates fast-gate failure' 47 "$?"
  assert_eq 'CI does not run suite after fast-gate failure' ci-fast "$(cat "$TMP/ci.trace")"

  assert_summary_semantics || fail 'summary semantic checks execute'
}

test_wrapper_commands_and_failures
test_build_caller
test_hook_caller
test_ci_caller
[ "$real_suite_ran" -eq 0 ] || fail 'real inventory suite ran before its caller'
test_real_inventory_suite || fail 'real inventory suite executes from caller harness'
[ "$real_suite_ran" -eq 1 ] && pass 'real inventory suite caller completed' || fail 'real inventory suite caller completed'

printf 'passed: %s  failed: %s\n' "$passed" "$failed"
[ "$failed" -eq 0 ]
