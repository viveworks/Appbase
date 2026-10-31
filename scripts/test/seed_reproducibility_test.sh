#!/usr/bin/env bash
#
# @author Navaneeth Mysore G
# @author Claude
#
# seed_reproducibility_test.sh — proves the REAL public-repo export pipeline
# (export.sh -> seed-commit.sh) is byte-identical across two independent runs
# from the same frozen source commit. (internal work item 2659, follow-up from internal work item 1430/AC11.)
#
# seed_commit_test.sh already proves seed-commit.sh is deterministic against
# a tiny synthetic fixture tree. That is not the same claim as internal work item 2659 asks
# for: that the real export pipeline, run twice end to end against the real
# repository, produces byte-identical output -- not "looks the same," but
# actually diffed. This test runs the real export.sh against two separate,
# freshly created detached worktrees pinned to the same frozen commit (never
# the live in-progress worktree, so nothing mid-edit here can leak in),
# diffs the two exported trees, then runs the real seed-commit.sh on each and
# compares the resulting commit and tree object ids.
#
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$HERE/../.." && pwd)"
EXPORT_SCRIPT="$ROOT/scripts/export/export.sh"
SEED_SCRIPT="$ROOT/scripts/export/seed-commit.sh"
PASSED=0; FAILED=0
ok(){  PASSED=$((PASSED+1)); echo "  ok   $1"; }
bad(){ FAILED=$((FAILED+1)); echo "  FAIL $1"; }
WORK="$(mktemp -d)"
cleanup() {
  git -C "$ROOT" worktree remove --force "$WORK/src1" >/dev/null 2>&1 || true
  git -C "$ROOT" worktree remove --force "$WORK/src2" >/dev/null 2>&1 || true
  rm -rf "$WORK"
}
trap cleanup EXIT

echo "seed reproducibility, real pipeline (internal work item 2659)"; echo

# Frozen at the current develop commit used to revive this draft. This is after internal work item 2706 fixed the
# manifest-completeness gap discovered by the original attempt, so the proof now exercises the
# current real export surface rather than the manifest's creation-era tree.
FROZEN_SHA="9239b6bcccac9633ffc69a6ee2800744e1118e33"
if ! git -C "$ROOT" cat-file -e "$FROZEN_SHA" 2>/dev/null; then
  bad "the pinned frozen commit $FROZEN_SHA exists in this checkout"
  echo; echo "seed_reproducibility_test: $PASSED passed, $FAILED failed"; exit 1
fi

if git -C "$ROOT" worktree add --detach "$WORK/src1" "$FROZEN_SHA" >"$WORK/wt1.out" 2>&1 \
   && git -C "$ROOT" worktree add --detach "$WORK/src2" "$FROZEN_SHA" >"$WORK/wt2.out" 2>&1; then
  ok "two independent detached worktrees created at the same frozen commit ($FROZEN_SHA)"
else
  bad "two independent detached worktrees created at the same frozen commit"
  echo; echo "seed_reproducibility_test: $PASSED passed, $FAILED failed"; exit 1
fi

PUBLIC_URL="https://example.invalid/appbase-public"
if "$EXPORT_SCRIPT" --source "$WORK/src1" --output "$WORK/exp1" --public-repo-url "$PUBLIC_URL" >"$WORK/exp1.out" 2>&1 \
   && "$EXPORT_SCRIPT" --source "$WORK/src2" --output "$WORK/exp2" --public-repo-url "$PUBLIC_URL" >"$WORK/exp2.out" 2>&1; then
  ok "export.sh succeeds independently against each worktree"
else
  bad "export.sh succeeds independently against each worktree"
  cat "$WORK/exp1.out" "$WORK/exp2.out" >&2
  echo; echo "seed_reproducibility_test: $PASSED passed, $FAILED failed"; exit 1
fi

# diff -rq compares file CONTENT and the presence/type of entries, never mtime -- exactly the
# "byte-identical," not "looks the same," claim internal work item 2659 asks for, at the export-tree level.
if diff_out="$(diff -rq "$WORK/exp1" "$WORK/exp2" 2>&1)" && [ -z "$diff_out" ]; then
  ok "the two independently exported trees are byte-identical (diff -rq empty)"
else
  bad "the two independently exported trees are byte-identical"
  echo "$diff_out" >&2
fi

if "$SEED_SCRIPT" --tree "$WORK/exp1" --output "$WORK/seed1" \
    --author "Appbase Release <release@example.invalid>" \
    --committer "Appbase Release <release@example.invalid>" \
    --date 1700000000 --message "Initial public release" \
    --skip-leak-scan-FOR-TEST-ONLY >"$WORK/seed1.out" 2>&1 \
   && "$SEED_SCRIPT" --tree "$WORK/exp2" --output "$WORK/seed2" \
    --author "Appbase Release <release@example.invalid>" \
    --committer "Appbase Release <release@example.invalid>" \
    --date 1700000000 --message "Initial public release" \
    --skip-leak-scan-FOR-TEST-ONLY >"$WORK/seed2.out" 2>&1; then
  ok "seed-commit.sh succeeds independently against each exported tree"
else
  bad "seed-commit.sh succeeds independently against each exported tree"
  cat "$WORK/seed1.out" "$WORK/seed2.out" >&2
  echo; echo "seed_reproducibility_test: $PASSED passed, $FAILED failed"; exit 1
fi

commit1="$(git -C "$WORK/seed1" rev-parse HEAD)"
commit2="$(git -C "$WORK/seed2" rev-parse HEAD)"
tree1="$(git -C "$WORK/seed1" rev-parse HEAD^{tree})"
tree2="$(git -C "$WORK/seed2" rev-parse HEAD^{tree})"

[ "$commit1" = "$commit2" ] && ok "the two independent end-to-end pipeline runs produce an identical commit id ($commit1)" \
                             || bad "the two independent end-to-end pipeline runs produce an identical commit id ($commit1 != $commit2)"
[ "$tree1" = "$tree2" ] && ok "the two independent end-to-end pipeline runs produce an identical tree id ($tree1)" \
                         || bad "the two independent end-to-end pipeline runs produce an identical tree id ($tree1 != $tree2)"

# Negative control: this harness can actually detect nondeterminism, not just rubber-stamp a
# match -- perturb one exported tree's content after the fact and confirm the diff/tree-id checks
# above would have caught it.
mkdir -p "$WORK/exp2-mutated"
cp -a "$WORK/exp2"/. "$WORK/exp2-mutated"/
printf 'perturbed\n' > "$WORK/exp2-mutated/.repro-control-marker"
# diff -rq exits 1 (not 0) the moment it finds a difference, so the exit status and the captured
# text are read separately here -- folding them into one `if cmd && [ -n "$out" ]` would short-
# circuit on that nonzero exit before the string is ever checked, misreading "found a difference"
# (the outcome this control exists to prove) as a shell failure instead of a result.
diff_out="$(diff -rq "$WORK/exp1" "$WORK/exp2-mutated" 2>&1)"
diff_rc=$?
if [ "$diff_rc" -ne 0 ] && [ -n "$diff_out" ]; then
  ok "negative control: a perturbed tree IS caught by the diff (not a vacuous check)"
else
  bad "negative control: a perturbed tree IS caught by the diff (not a vacuous check)"
fi

echo
echo "seed_reproducibility_test: $PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ]
