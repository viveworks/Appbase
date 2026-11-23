#!/usr/bin/env bash
#
# check-signoff.sh — enforce the DCO sign-off trailer on every commit in a range (internal work item 1385 / N06).
#
#   scripts/ci/check-signoff.sh --base <ref> --head <ref>   # check base..head
#   scripts/ci/check-signoff.sh --base origin/develop       # --head defaults to HEAD
#
#   scripts/ci/check-signoff.sh --message-file <file> --author-email <email>
#   Validate a proposed squash message; the merger must bind these inputs to the actual commit.
#
# WHAT IT ENFORCES. Every commit in the range must carry a well-formed trailer
#
#     Signed-off-by: Full Name <email@example.com>
#
# on a line of its own, and — unless DCO_REQUIRE_AUTHOR_MATCH=0 — one such trailer must carry the
# commit AUTHOR's e-mail. `CONTRIBUTING.md` documents what a contributor has to type. The
# certification itself is the Developer Certificate of Origin 1.1.
#
# ⚠️ THE AUTHOR MATCH IS THE POINT, NOT A REFINEMENT. A presence-only check is satisfied by
# `Signed-off-by: x <x@x>`, which need not identify the author. Matching constrains the text; it
# cannot prove a person authorized certification. The range-mode override is local-only; CI pins 1.
# Proposed-message mode always requires author matching.
#
# Merge commits are checked too: a merge may introduce content absent from both parents.
# This does not change the repository's squash-leaf / rebase-integration merge policy.
# Message mode checks supplied text and an intended author, not personal authorization or forge state.
# It always enforces author matching. Range and message modes cannot be combined.
#
# EXIT CODES — three, because "could not check" is not "checked and passed":
#   0  every checked commit carries a valid sign-off (and at least one commit was checked)
#   1  at least one commit does not — each offender is named, with why
#   2  the check could not be performed: bad usage, not a git repo, unresolvable ref, or an EMPTY
#      range. ⚠️ AN EMPTY RANGE IS A REFUSAL, NOT A PASS. Zero commits checked is the most confident
#      possible way to report having checked nothing, and a CI step that exits 0 on a range it failed
#      to compute is a green tick over an unexamined PR.
#
#
# Repository tooling for the contributor sign-off policy.
# Certification remains the certifying person's responsibility.
set -uo pipefail

BANNER="check-signoff:"
BASE=""
HEAD_REF="HEAD"
HEAD_SET=0
MESSAGE_FILE=""
AUTHOR_EMAIL=""
# 🔴 ONLY A LITERAL `0` DISABLES THE AUTHOR MATCH. EVERYTHING ELSE ENABLES IT, INCLUDING NONSENSE.
# The first version tested `= "1"` to enable, which meant `DCO_REQUIRE_AUTHOR_MATCH=true` — or `yes`,
# or `on` — SILENTLY DISABLED the check. Measured: unset->enforced, `1`->enforced, and `0`, `true`,
# `yes`, `on` all -> not enforced. A person typing `true` is trying to turn the check ON and would
# have turned it off, with no error and no output saying so.
# The switch must fail toward the SAFE side, and for this control the safe side is ENABLED — which is
# the mirror of `APPBASE_GRAPEROOT=0` in CONTRIBUTING.md, where the safe side is OFF and so only `1` enables.
# Same principle, opposite literal: the value that is easy to typo must not be the dangerous one.
REQUIRE_AUTHOR_MATCH=1
[ "${DCO_REQUIRE_AUTHOR_MATCH:-1}" = "0" ] && REQUIRE_AUTHOR_MATCH=0

die()  { printf '%s REFUSING — %s\n' "$BANNER" "$1" >&2; exit 2; }

while [ $# -gt 0 ]; do
  case "$1" in
    --base) BASE="${2-}"; [ -n "$BASE" ] || die "--base needs a ref"; shift 2 ;;
    --head) HEAD_SET=1; HEAD_REF="${2-}"; [ -n "$HEAD_REF" ] || die "--head needs a ref"; shift 2 ;;
    --message-file) MESSAGE_FILE="${2-}"; [ -n "$MESSAGE_FILE" ] || die "--message-file needs a path"; shift 2 ;;
    --author-email) AUTHOR_EMAIL="${2-}"; [ -n "$AUTHOR_EMAIL" ] || die "--author-email needs an email"; shift 2 ;;
    -h|--help) sed -n '2,40p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) die "unknown argument '$1'" ;;
  esac
done

# One predicate for committed records and prospective squash messages.
check_message() {
  local message="$1" author_email="$2" short="$3" subject="$4"
  local signoffs emails want
  # ⚠️ ANCHORED HARD AT COLUMN ONE, AND THE E-MAIL IS REQUIRED. `grep "Signed-off-by"` alone matches
  # the phrase inside prose ("I was told to add Signed-off-by: ... but did not") and matches
  # `Signed-off-by: Some Name` with no address, which identifies nobody.
  # ⚠️ LEADING WHITESPACE IS *NOT* TOLERATED, DELIBERATELY. `git commit --signoff` never indents the
  # trailer, so nothing legitimate is lost — while allowing indentation would count an INDENTED
  # quotation of someone else's sign-off as this author's own certification.
  # ⚠️ AND THAT IS ALL THE COLUMN-ONE ANCHOR BUYS — it does NOT make quoting impossible. A sign-off
  # pasted at column one (inside a fenced block, or in a quoted patch) still counts. The bound on
  # that is the AUTHOR MATCH, not the anchor: under DCO_REQUIRE_AUTHOR_MATCH=1 the quoted line must
  # carry this commit author's own address. Acceptance is mechanical; it does not prove certification.
  # ⚠️ A NAME IS REQUIRED, NOT JUST AN ADDRESS. `.+` before the `<` is satisfied by the separating
  # space alone, so `Signed-off-by: <someone@example.com>` passed — which contradicts both the
  # `Signed-off-by: Name <email>` form this file's header states and the one CONTRIBUTING.md
  # documents. The first character after the colon-space must therefore be a real name character.
  # ⚠️ THE MATCH IS CASE-SENSITIVE, DELIBERATELY. `git commit --signoff` emits exactly
  # `Signed-off-by:`, so nothing a contributor can produce with the documented command is lost, and
  # a security control should not widen the set of spellings it accepts. The rejection message says
  # so, because a contributor whose `signed-off-by:` was rejected would otherwise hunt for a line
  # that is visibly present.
  signoffs="$(printf '%s\n' "$message" \
                | grep -E '^Signed-off-by:[[:space:]]+[^<>[:space:]][^<>]*<[^<>[:space:]]+@[^<>[:space:]]+>[[:space:]]*$' \
                || true)"

  if [ -z "$signoffs" ]; then
    BAD=$((BAD + 1))
    BAD_REPORT="$BAD_REPORT  $short  $subject
      no 'Signed-off-by: Name <email>' trailer (matched case-sensitively, at column 1, name required)
"
    return
  fi

  if [ "$REQUIRE_AUTHOR_MATCH" = "1" ]; then
    # Lower-case both sides: e-mail domains are case-insensitive and local parts are in practice.
    emails="$(printf '%s\n' "$signoffs" | sed -E 's/.*<([^<>]+)>.*/\1/' | tr '[:upper:]' '[:lower:]')"
    want="$(printf '%s' "$author_email" | tr '[:upper:]' '[:lower:]')"
    if ! printf '%s\n' "$emails" | grep -Fxq -- "$want"; then
      BAD=$((BAD + 1))
      BAD_REPORT="$BAD_REPORT  $short  $subject
      signed off by [$(printf '%s' "$emails" | tr '\n' ' ')] but authored by [$author_email]
"
      return
    fi
  fi
}

if [ -n "$MESSAGE_FILE" ] || [ -n "$AUTHOR_EMAIL" ]; then
  [ -z "$BASE" ] && [ "$HEAD_SET" -eq 0 ] || die "message and range options cannot be combined"
  [ -n "$MESSAGE_FILE" ] && [ -n "$AUTHOR_EMAIL" ] || die "message mode needs --message-file and --author-email"
  case "$AUTHOR_EMAIL" in *[[:space:]]*) die "author email must be one whitespace-free value" ;; esac
  printf '%s\n' "$AUTHOR_EMAIL" | grep -Eq '^[^<>[:space:]]+@[^<>[:space:]]+$' || die "invalid author email"
  [ -f "$MESSAGE_FILE" ] && [ -r "$MESSAGE_FILE" ] || die "cannot read message file"
  message="$(cat -- "$MESSAGE_FILE")" || die "cannot read message file"
  REQUIRE_AUTHOR_MATCH=1
  BAD=0; BAD_REPORT=""
  check_message "$message" "$AUTHOR_EMAIL" 'proposed-message' ''
  if [ "$BAD" -gt 0 ]; then
    printf '%s REJECT — %s\n' "$BANNER" "$BAD_REPORT" >&2
    exit 1
  fi
  printf '%s OK — supplied message matches supplied author; actual merge inputs are NOT verified.\n' "$BANNER"
  exit 0
fi

[ -n "$BASE" ] || die "--base is required; it is not guessed. A range this script invented is a range nobody can check it was right about"

git rev-parse --git-dir >/dev/null 2>&1 || die "not inside a git repository"

git rev-parse --verify --quiet "$BASE^{commit}" >/dev/null || die "cannot resolve --base '$BASE' to a commit"
git rev-parse --verify --quiet "$HEAD_REF^{commit}" >/dev/null || die "cannot resolve --head '$HEAD_REF' to a commit"

# ⚠️ TWO-DOT, AND IT IS ALREADY THE RIGHT ANSWER — DO NOT "IMPROVE" IT TO A MERGE BASE.
# `base..head` is "reachable from head, NOT reachable from base", so commits belonging to the base
# branch are excluded by construction: they are reachable from base. A review pass on this change
# proposed anchoring at `git merge-base` instead, reasoning that an advanced base branch would leak
# its own unsigned commits into the range. That reasoning is wrong, and it was MEASURED wrong rather
# than argued down — with the base advanced, and again with the advanced base merged back into the
# branch, both ranges yielded exactly the same commit. Whenever base is an ancestor of head the merge
# base IS base; whenever it is not, two-dot already excludes base's side. The merge-base version was
# therefore a no-op that added a second failure mode (no common ancestor), and it was reverted.
RANGE="$BASE..$HEAD_REF"

COMMITS="$(git rev-list "$RANGE" 2>/dev/null)" || die "cannot enumerate commit range"

CHECKED=0; BAD=0
BAD_REPORT=""

if [ -z "$COMMITS" ]; then
  printf '%s REFUSING — the range %s contains no commits.\n' "$BANNER" "$RANGE" >&2
  printf '%s   Zero commits checked is not a clean run; it is a run that measured nothing.\n' "$BANNER" >&2
  printf '%s   Check that the base ref is fetched and correct (CI: fetch-depth: 0).\n' "$BANNER" >&2
  exit 2
fi

while IFS= read -r sha; do
  [ -n "$sha" ] || continue
  CHECKED=$((CHECKED + 1))
  short="$(git rev-parse --short "$sha")"
  subject="$(git show -s --format=%s "$sha")"
  author_email="$(git show -s --format=%ae "$sha")"

  message="$(git show -s --format=%B "$sha")" || die "cannot read commit message"
  check_message "$message" "$author_email" "$short" "$subject"
done <<EOFC
$COMMITS
EOFC

printf '%s range %s — %s commit(s) checked' "$BANNER" "$RANGE" "$CHECKED"
[ "$REQUIRE_AUTHOR_MATCH" = "1" ] || printf ' (author match DISABLED via DCO_REQUIRE_AUTHOR_MATCH=0)'
printf '\n'

if [ "$BAD" -gt 0 ]; then
  printf '%s REJECT — %s of %s commit(s) are not signed off:\n' "$BANNER" "$BAD" "$CHECKED" >&2
  printf '%s' "$BAD_REPORT" >&2
  printf '\n%s How to fix, per CONTRIBUTING.md:\n' "$BANNER" >&2
  printf '%s Only the certifying person, with the same identity as the existing author, may use these repairs.\n' "$BANNER" >&2
  printf '%s If identities differ, obtain the author certification or explicitly authorized authorship correction; do not invent trailers.\n' "$BANNER" >&2
  printf '%s   the most recent commit only:   git commit --amend --signoff\n' "$BANNER" >&2
  printf '%s   every commit on this branch:   git rebase --signoff %s\n' "$BANNER" "$BASE" >&2
  printf '%s Sign-off certifies the Developer Certificate of Origin 1.1 (see CONTRIBUTING.md).\n' "$BANNER" >&2
  exit 1
fi

printf '%s OK — every checked commit is signed off.\n' "$BANNER"
exit 0
