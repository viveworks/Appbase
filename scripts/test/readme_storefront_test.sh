#!/usr/bin/env bash
#
# readme_storefront_test.sh — the acceptance checks for internal work item 2419 (P06·E02 N04): the root README.md
# storefront and the three pages it owns (configuration reference, supported examples, known
# limitations).
#
# WHAT THIS PROVES, AND WHAT IT DOES NOT.
#
#   It checks STRUCTURE and TRUTHFULNESS AGAINST THE TREE: the required sections exist, relative
#   links resolve, every named
#   `appbase-*` artifact is a real reactor artifact, and no deferred capability is advertised as
#   supported. A populated Release-1 section must pass the independent publication verifier.
#
#   An exact empty publication placeholder is a prepublication state, not a Release-1 pass. The
#   historical matrix parser and its adversarial fixture remain for the old storefront contract.
#
#   SCOPE (RA-A1 L5-5): the deferred-name and table-shape arms read the FOUR OWNED pages only —
#   README.md, docs/Configuration-Reference.md, docs/Examples.md and docs/Known-Limitations.md. A deferred
#   capability advertised on any other page (a user-guide chapter, a module README) is out of this
#   suite's reach; those pages belong to their own internal work item 1142 children and their own tests. The link arm also
#   checks the user-guide index because it is a public entry point linked directly from the storefront.
#
#   KNOWN LIMIT of the deferred-name arm: it is a proximity heuristic, not a parser of English. A marker
#   must sit within 8 words of the name, a negated or past-tense marker counts for nothing, and an
#   affirmative support claim closer to the name than any marker fails it ("Unlike SMTP, which is
#   deferred, Spark is supported"). A sentence built to defeat all three can still pass; the reviewer is
#   the backstop for prose, and this arm is the backstop for the reviewer. Inside a TABLE ROW the marker
#   may sit in any cell (a row carries one status), but an affirmative claim at least as close to the
#   name as every marker still fails the row (RA-A1 A1-1, A1-9).
#
# Every arm is a FAIL-CAPABLE check; `EXTRA_*` arms carry a one-line reason. Mutation evidence for each
# AC arm is recorded in the PR and the quality ledger.
#
#   scripts/test/readme_storefront_test.sh
#   STOREFRONT_ROOT=/path/to/mutated/copy scripts/test/readme_storefront_test.sh   # mutation runs
#
# @author Navaneeth Mysore G
# @author Claude
#
set -uo pipefail

ROOT="${STOREFRONT_ROOT:-$(cd "$(dirname "$0")/../.." && pwd)}"
README="$ROOT/README.md"
CONFIG="$ROOT/docs/Configuration-Reference.md"
EXAMPLES="$ROOT/docs/Examples.md"
LIMITS="$ROOT/docs/Known-Limitations.md"
OWNED=("$README" "$CONFIG" "$EXAMPLES" "$LIMITS")
LINK_CHECKED=("${OWNED[@]}" "$ROOT/docs/user-guide/README.md")

PASSED=0
FAILED=0
ok()  { PASSED=$((PASSED + 1)); printf 'ok    %s\n' "$1"; }
bad() { FAILED=$((FAILED + 1)); printf 'FAIL  %s\n' "$1"; [ $# -gt 1 ] && printf '        %s\n' "${@:2}"; }

# Planned pages owned by sibling internal work item 1142 children. A link to one may dangle ONLY while that page does
# not exist AND the linking line says so (`planned`); once the page lands the link must simply resolve.
PLANNED=(
  "docs/Module-and-Service-Catalog.md"   # internal work item 2418 (N03)
  "docs/user-guide/README.md"            # internal work item 1416 (N05) — the plan's docs/user-guide/ path, as internal work item 2505 adds it
  "docs/branding/README.md"              # internal work item 1415 (N02)
)

# The Markdown and POM parsing lives in readme_storefront_scan.py (RA-A1 round 3 on internal work item 2504: the awk and
# sed versions split sentences only on .!?, lost CommonMark fence lengths, checked only the first matrix
# cell, and read POMs line by line). That file also holds the ONE copy of the Release-1 supported matrix
# (Release 1.0 Lean Requirements baseline, reconciled 2026-08-28, section 4.1) and the deferred capabilities that have no artifact. The deferred
# ARTIFACTS come from the single list scripts/test/release1-excluded-artifacts.txt.
SCAN="$ROOT/scripts/test/readme_storefront_scan.py"
[ -f "$SCAN" ] || SCAN="$(cd "$(dirname "$0")" && pwd)/readme_storefront_scan.py"
# The scanner is the whole parser: without python3 every content arm would be judging empty output.
if ! command -v python3 >/dev/null 2>&1; then
  printf 'FAIL  python3 is required (scripts/test/readme_storefront_scan.py) — refusing to run over nothing\n'
  printf 'readme-storefront-test: 0 passed, 1 failed\n'
  exit 1
fi
EXCLUDED_FILE="$ROOT/scripts/test/release1-excluded-artifacts.txt"
EXCLUDED=()
# A read loop, not mapfile: mapfile is bash 4+, and macOS ships bash 3.2 (RA-A4 A4-R4).
if [ -f "$EXCLUDED_FILE" ]; then
  while IFS= read -r _x; do [ -n "$_x" ] && EXCLUDED+=("$_x"); done \
    < <(sed 's/#.*//; s/[[:space:]]//g; /^$/d' "$EXCLUDED_FILE")
fi

# -- helpers ---------------------------------------------------------------------------------------

require_file() {  # require_file FILE — a content arm means nothing until its file exists
  [ -f "$1" ]
}

# norm_path BASE REL → BASE/REL, lexically normalised. Portable: `realpath -m` is GNU-only, and BSD/macOS
# realpath has no -m (RA-A1 A1-7 on internal work item 2504).
norm_path() {
  python3 -c 'import os,sys; print(os.path.normpath(os.path.join(sys.argv[1], sys.argv[2])))' "$1" "$2"
}

# section_body FILE HEADING_REGEX  → the lines under the first matching `## ` heading
section_body() {
  awk -v re="$2" '
    /^## / { if (inside) exit; if ($0 ~ re) { inside = 1; next } }
    inside { print }' "$1"
}

# Every scanner call is CAPTURED first and its exit status checked, never fed straight into a loop:
# a process substitution discards the exit status, so a crashed scanner read as "found nothing" and the
# arm passed (RA-A1 L2-1). Usage:  out="$(scan MODE ARGS...)" || miss+=("scanner failed: MODE")
scan() { python3 "$SCAN" "$@"; }

publication_state() { scan publication-state "$README"; }

# -- arms ------------------------------------------------------------------------------------------

# AC2419-1 — purpose, capability families, release-evidence section, shortest quick start.
ac1() {
  local miss=()
  require_file "$README" || { bad "AC2419-1: README purpose, families, evidence section, quick start" "README.md missing"; return; }
  grep -qiE '^## What (is )?Appbase' "$README"            || miss+=("## What is Appbase (purpose)")
  grep -qiE '^## Capability families' "$README"            || miss+=("## Capability families")
  grep -qiE '^## Release 1 support' "$README"              || miss+=("## Release 1 support section")
  grep -qiE '^## Quick start' "$README"                    || miss+=("## Quick start")
  # A quick start with no command in it is a heading, not a quick start.
  section_body "$README" '^## Quick start' | grep -q '^```' || miss+=("a fenced command block under ## Quick start")
  section_body "$README" '^## Quick start' | grep -q 'python3' || miss+=("the python3 build-check prerequisite under ## Quick start")
  # The purpose must come before the contributor material, i.e. the README opens as a storefront.
  local purpose contrib
  purpose="$(grep -niE '^## What (is )?Appbase' "$README" | head -1 | cut -d: -f1)"
  contrib="$(grep -niE '^## (Developing|Contributing)' "$README" | head -1 | cut -d: -f1)"
  if [ -n "$purpose" ] && [ -n "$contrib" ] && [ "$purpose" -gt "$contrib" ]; then
    miss+=("purpose must precede the contributor section")
  fi
  [ ${#miss[@]} -eq 0 ] && ok "AC2419-1: README purpose, families, evidence section, quick start" \
                        || bad "AC2419-1: README purpose, families, evidence section, quick start" "${miss[@]}"
}

# AC2419-2 — README links every required destination, and every relative link in the owned pages
# resolves (or is a labelled planned sibling page). Future publication outputs must not be linked
# until their files exist.
ac2() {
  local miss=() want
  require_file "$README" || { bad "AC2419-2: README links required destinations; links resolve" "README.md missing"; return; }
  for want in \
      'docs/Module-and-Service-Catalog.md' 'docs/user-guide/README.md' 'docs/Examples.md' \
      'docs/Configuration-Reference.md' 'docs/Known-Limitations.md'; do
    # A real Markdown link target, not the path merely mentioned in prose (RA R4d on internal work item 2504).
    grep -qE "\]\(([^)]*/)?${want//./\\.}([#)])" "$README" || miss+=("README does not link $want")
  done
  # The private source links its API-doc owner issue; the public export keeps
  # the historical identifier as text, never as a link to a different tracker.
  grep -qF 'issues/1141' "$README" || grep -qF 'internal work item 1141' "$README" \
    || miss+=("README does not identify API-doc work item 1141")
  local f line target path base clean planned p text out
  publication_state >/dev/null || miss+=("README release publication state is malformed")
  for f in "${LINK_CHECKED[@]}"; do
    [ -f "$f" ] || { miss+=("${f#"$ROOT"/} missing"); continue; }
    base="$(dirname "$f")"
    # links: LINE<TAB>TARGET for inline/image links, reference definitions and HTML href=/src=.
    out="$(scan links "$f")" || { miss+=("scanner failed: links ${f#"$ROOT"/}"); continue; }
    while IFS=$'\t' read -r line target; do
      [ -n "$target" ] || continue
      clean="${target%%#*}"; clean="${clean%% *}"
      [ -n "$clean" ] || continue
      path="$(norm_path "$base" "$clean")"
      planned=""
      for p in "${PLANNED[@]}"; do
        [ "$path" = "$(norm_path "$ROOT" "$p")" ] && planned="$p"
      done
      text="$(sed -n "${line}p" "$f")"
      if [ -e "$path" ]; then
        if [ "$planned" = "docs/Module-and-Service-Catalog.md" ] && printf '%s' "$text" | grep -qiE 'planned'; then
          miss+=("${f#"$ROOT"/}:$line → $target exists but is still labelled planned")
        fi
        continue
      fi
      if [ -n "$planned" ] && printf '%s' "$text" | grep -qiE 'planned'; then
        continue
      fi
      miss+=("${f#"$ROOT"/}:$line → $target does not resolve${planned:+ (planned page, but the line does not say 'planned')}")
    done <<< "$out"
  done
  [ ${#miss[@]} -eq 0 ] && ok "AC2419-2: README links required destinations; links resolve" \
                        || bad "AC2419-2: README links required destinations; links resolve" "${miss[@]}"
}

# AC2419-3 — no hand-authored support claims; populated publication is independently verified.
ac3() {
  local miss=() arts f tok row name out state candidate publication
  require_file "$README" || { bad "AC2419-3: matrix + claims agree with the shipped surface" "README.md missing"; return; }
  # Every artifactId reachable from the root pom's <modules> graph; the scanner exits non-zero on none.
  arts="$(scan reactor "$ROOT")" || miss+=("scanner failed: reactor — could not read any reactor artifactId")

  # (a) every appbase-* artifact named in an owned page is a real reactor artifact.
  for f in "${OWNED[@]}"; do
    [ -f "$f" ] || continue
    while read -r tok; do
      [ -n "$tok" ] || continue
      printf '%s\n' "$arts" | grep -qxF "$tok" || miss+=("${f#"$ROOT"/} names $tok, which is not a reactor artifact")
    done < <(grep -oE '\bappbase-[a-z0-9-]*[a-z0-9]\b(\.[a-z]+)?' "$f" | grep -v '\.' | sort -u \
               | grep -vxE 'appbase-(codeagents|devcontainer-.*|container.*|topology.*|claude-launcher)')
  done

  state="$(publication_state)" || miss+=("release publication markers are malformed")
  if [ "$state" = "candidate" ]; then
    # Structure may be valid, but the generated support matrix is absent until P. This acceptance
    # arm must stay nonzero so CI cannot call the prepublication tree release-ready.
    miss+=("PREPUBLICATION: empty release-evidence placeholder is not a release support claim")
  elif [ "$state" = "publication" ]; then
    publication="$(git -C "$ROOT" rev-parse HEAD 2>/dev/null)" || miss+=("publication HEAD unavailable")
    candidate="$(git -C "$ROOT" rev-parse HEAD^ 2>/dev/null)" || miss+=("publication parent unavailable")
    if [ -n "$publication" ] && [ -n "$candidate" ]; then
      out="$(python3 "$ROOT/docs/release/evidence-index/verify-evidence-index.py" \
        --repo "$ROOT" --manifest "$ROOT/docs/release/evidence-index/source-manifest.json" \
        --candidate "$candidate" --publication "$publication" 2>&1)" \
        || miss+=("independent publication verification failed: $out")
    fi
  else
    miss+=("README is neither an exact prepublication candidate nor a verified publication")
  fi

  # (c) no deferred capability is named without a deferral marker NEAR it (8 words; a marked lead-in
  # carries across ':'), a negated or past-tense marker marks nothing, a closer support claim overrides a
  # marker, HTML comments are not content, a deferred artifactId in fenced code is a usage example of it,
  # and a non-ASCII letter is flagged as a possible homoglyph (RA-A1 B5-1, B5-2, L1-N1, L1-N2).
  [ ${#EXCLUDED[@]} -gt 0 ] || miss+=("no excluded artifacts read from ${EXCLUDED_FILE#"$ROOT"/} — refusing to pass over nothing")
  for f in "${OWNED[@]}"; do
    [ -f "$f" ] || continue
    out="$(scan deferred "$f" ${EXCLUDED[@]+"${EXCLUDED[@]}"})" || { miss+=("scanner failed: deferred ${f#"$ROOT"/}"); continue; }
    while IFS= read -r row; do
      [ -n "$row" ] || continue
      miss+=("${f#"$ROOT"/}:$row names a deferred capability without marking it deferred (or has a non-ASCII letter)")
    done <<< "$out"
  done

  [ ${#miss[@]} -eq 0 ] && ok "AC2419-3: $state release-evidence structure (candidate is prepublication only)" \
                        || bad "AC2419-3: release-evidence structure/publication verification" "${miss[@]}"
}

# AC2419-4 — every example carries a status: EXECUTED (with evidence) or NOT MEASURED naming EXTERNAL
# infrastructure. A missing local toolchain is not an external dependency (RA R4e on internal work item 2504): a local,
# non-destructive example must be EXECUTED.
ac4() {
  local miss=() n=0 heading status
  require_file "$EXAMPLES" || { bad "AC2419-4: every example is EXECUTED or labelled NOT MEASURED" "docs/Examples.md missing"; return; }
  while IFS= read -r heading; do
    n=$((n + 1))
    status="$(awk -v h="$heading" '$0 == h { f = 1; next } f && /^### / { exit } f && /^\*\*Status:\*\*/ { print; exit }' "$EXAMPLES")"
    # Anchored on the status word itself: "NOT EXECUTED" must not read as EXECUTED (RA-A1 B5-6).
    if printf '%s' "$status" | grep -qE '^\*\*Status:\*\* EXECUTED\b'; then
      printf '%s' "$status" | grep -qE '[0-9a-f]{9}|20[0-9]{2}-[0-9]{2}-[0-9]{2}' \
        || miss+=("'$heading' claims EXECUTED without a date/revision")
    elif printf '%s' "$status" | grep -qE '^\*\*Status:\*\* NOT MEASURED\b'; then
      printf '%s' "$status" | grep -qiE 'external|credential|docker|backend' \
        || miss+=("'$heading' is NOT MEASURED without naming the external dependency")
    else
      miss+=("'$heading' has no **Status:** EXECUTED / NOT MEASURED line")
    fi
  done < <(grep -E '^### ' "$EXAMPLES")
  [ "$n" -gt 0 ] || miss+=("docs/Examples.md has no ### example entries — refusing to pass over nothing")
  [ ${#miss[@]} -eq 0 ] && ok "AC2419-4: every example is EXECUTED or labelled NOT MEASURED ($n examples)" \
                        || bad "AC2419-4: every example is EXECUTED or labelled NOT MEASURED" "${miss[@]}"
}

# AC2419-5 — Release-2 work is LINKED (registry internal work item 1957 and adapter registry internal work item 1925), not duplicated.
ac5() {
  local miss=() f
  require_file "$README" || { bad "AC2419-5: Release-2 work linked, not duplicated" "README.md missing"; return; }
  grep -qF 'issues/1957' "$README" || grep -qF 'internal work item 1957' "$README" \
    || miss+=("README does not identify the Release 2 Plan of Record work item 1957")
  [ -f "$LIMITS" ] && {
    grep -qF 'issues/1925' "$LIMITS" || grep -qF 'internal work item 1925' "$LIMITS" \
      || miss+=("Known-Limitations does not identify work item 1925")
  }
  for f in "${OWNED[@]}"; do
    [ -f "$f" ] || continue
    # The Next Release Items table is internal work item 1420's to preserve; a copy here would be a second store.
    grep -qiE 'Next Release Items' "$f" && ! grep -qiE 'Next Release Items.*(Releases\.md|internal work item 1420|issues/1420)' "$f" \
      && miss+=("${f#"$ROOT"/} mentions Next Release Items without pointing at its internal work item 1420 owner")
    # A copied Release-2 backlog shows up as a table with many Release-2 issue rows.
    [ "$(grep -cE '^\|.*issues/(1136|1332|1388|1417|1418|1419|1887|1901)' "$f")" -le 3 ] \
      || miss+=("${f#"$ROOT"/} reproduces the Release 2 backlog table instead of linking internal work item 1957")
  done
  [ ${#miss[@]} -eq 0 ] && ok "AC2419-5: Release-2 work linked, not duplicated" \
                        || bad "AC2419-5: Release-2 work linked, not duplicated" "${miss[@]}"
}

# AC2419-7 — the Swagger UI screenshots, appended to internal work item 2419 on 2026-09-27 by the owner's
# ruling. The README has a "## Swagger UI" section. Post-release this section carries no screenshot-status
# commentary by default (no screenshots, no NOT MEASURED marker) -- that is the expected, non-problem state.
# Once it embeds an image, every image must exist (AC2419-2 resolves image links) and the NOT MEASURED
# marker must be gone.
# Every image form counts (inline, reference-style, <img>) outside HTML comments; each must be a non-empty
# local file, with a capture date and revision in the section (RA-A1 L5-4).
ac7() {
  local out rows=()
  require_file "$README" || { bad "AC2419-7: Swagger UI screenshots section" "README.md missing"; return; }
  out="$(scan swagger "$README")" || { bad "AC2419-7: Swagger UI screenshots section" "scanner failed: swagger"; return; }
  if [ -n "$out" ]; then while IFS= read -r _r; do rows+=("$_r"); done <<< "$out"; fi
  [ ${#rows[@]} -eq 0 ] && ok "AC2419-7: Swagger UI screenshots section" \
                        || bad "AC2419-7: Swagger UI screenshots section" "${rows[@]}"
}

# EXTRA_owned_pages_exist — reason: every content arm above short-circuits on a missing file, so a
# single arm must say plainly which owned page is absent.
extra_exist() {
  local miss=() f
  for f in "${OWNED[@]}"; do [ -f "$f" ] || miss+=("${f#"$ROOT"/}"); done
  [ ${#miss[@]} -eq 0 ] && ok "EXTRA_owned_pages_exist" || bad "EXTRA_owned_pages_exist" "${miss[@]}"
}

# EXTRA_no_adoc_link — reason: internal work item 1421 deletes every .adoc, so a live link to one is a dangle-in-waiting.
extra_adoc() {
  local hits=() f
  for f in "${OWNED[@]}"; do
    [ -f "$f" ] || continue
    while IFS= read -r l; do hits+=("${f#"$ROOT"/}:$l"); done \
      < <(grep -nE '\]\([^)]*\.adoc[)#]' "$f" | grep -vE '\]\(https?://')
  done
  [ ${#hits[@]} -eq 0 ] && ok "EXTRA_no_adoc_link" || bad "EXTRA_no_adoc_link" "${hits[@]}"
}

# EXTRA_brand_embed — reason (RA C3 on internal work item 2504): linking the planned brand set is right while internal work item 1415 is
# unmerged, but once the lockups exist the link simply resolves and nothing would require the placement
# docs/branding/README.md prescribes. When both lockups exist, the README must embed them in a <picture>.
extra_brand_embed() {
  local b="$ROOT/docs/branding"
  if [ -f "$b/lockup-light.svg" ] && [ -f "$b/lockup-dark.svg" ]; then
    # The placement contract (docs/branding/README.md, internal work item 2502): the README's FIRST element is the
    # <picture> block, both lockups inside it, width="520", alt="Appbase" (RA R5 on internal work item 2504).
    local first pic
    first="$(grep -m1 -v '^[[:space:]]*$' "$README")"
    pic="$(awk '/<picture/ { f = 1 } f { print } /<\/picture>/ { exit }' "$README")"
    if printf '%s' "$first" | grep -q '^<picture' \
       && printf '%s' "$pic" | grep -qF 'docs/branding/lockup-light.svg' \
       && printf '%s' "$pic" | grep -qF 'docs/branding/lockup-dark.svg' \
       && printf '%s' "$pic" | grep -qF 'width="520"' && printf '%s' "$pic" | grep -qF 'alt="Appbase"'; then
      ok "EXTRA_brand_embed (lockups present and embedded first)"
    else
      bad "EXTRA_brand_embed" "docs/branding lockups exist, but README.md does not open with the <picture> block (both lockups, width=\"520\", alt=\"Appbase\")"
    fi
  else
    ok "EXTRA_brand_embed (lockups not on this base yet; link-only is correct)"
  fi
}

# EXTRA_table_shape — reason: a scripted per-row edit once gave every capability-family row a 4th cell
# its 3-column header lacked (found after push on internal work item 2504). GitHub silently drops the extra cell, so
# nothing else would notice.
extra_table_shape() {
  local bad_rows=() f
  local out l
  for f in "${OWNED[@]}"; do
    [ -f "$f" ] || continue
    # GFM tables via the scanner, the one table reader in this suite (RA-A1 L5-2, L6-2).
    out="$(scan tableshape "$f")" || { bad_rows+=("scanner failed: tableshape ${f#"$ROOT"/}"); continue; }
    while IFS= read -r l; do [ -n "$l" ] && bad_rows+=("${f#"$ROOT"/}:$l"); done <<< "$out"
  done
  [ ${#bad_rows[@]} -eq 0 ] && ok "EXTRA_table_shape" || bad "EXTRA_table_shape" "${bad_rows[@]}"
}

# EXTRA_no_internal_experiments — owner decision D-I removes devcontainer, multi-container-topology and
# contributor scripts tooling from the PUBLIC root README. This is a negative arm so later edits cannot
# quietly restore the removed internal experiments while every positive storefront assertion still passes.
extra_no_internal_experiments() {
  local out rows=()
  out="$(scan forbidden-public "$README")" \
    || { bad "EXTRA_no_internal_experiments" "scanner failed: forbidden-public"; return; }
  if [ -n "$out" ]; then while IFS= read -r _r; do rows+=("README.md:$_r"); done <<< "$out"; fi
  [ ${#rows[@]} -eq 0 ] && ok "EXTRA_no_internal_experiments" \
                        || bad "EXTRA_no_internal_experiments" "${rows[@]}"
}

# EXTRA_no_internal_tracking — the public Configuration Reference is user documentation, not a
# delivery ledger. Issue/PR numbers, tracker links and epic terminology belong in internal records.
extra_no_internal_tracking() {
  local out rows=()
  out="$(scan tracking-public "$CONFIG")" \
    || { bad "EXTRA_no_internal_tracking" "scanner failed: tracking-public"; return; }
  if [ -n "$out" ]; then while IFS= read -r _r; do rows+=("docs/Configuration-Reference.md:$_r"); done <<< "$out"; fi
  [ ${#rows[@]} -eq 0 ] && ok "EXTRA_no_internal_tracking" \
                        || bad "EXTRA_no_internal_tracking" "${rows[@]}"
}

# EXTRA_scanner_fixtures — reason: the README arms pass on today's README whether or not the scanner
# can see the forms RA-A4 found it blind to. Each fixture below is one of those forms, with the verdict
# the scanner must reach (RA-A4 A4-R2 reference-style Swagger images, A4-R3 standalone Meta).
extra_fixtures() {
  local d miss=() out
  d="$(mktemp -d)" || { bad "EXTRA_scanner_fixtures" "mktemp failed"; return; }
  mkdir -p "$d/docs" && printf 'x' > "$d/docs/s.png"
  local hdr='# T

## Swagger UI

Captured 2026-09-27 at revision 0123456789abcdef.
'
  printf '%s\n![swagger ui]\n\n[Swagger UI]: https://example.com/remote.png\n' "$hdr" > "$d/shortcut.md"
  printf '%s\n![shot][]\n\n[shot]: docs/missing.png\n' "$hdr" > "$d/collapsed.md"
  # The definition sits under a LATER heading: CommonMark resolves it document-wide.
  printf '%s\n![Shot][]\n\n## Later\n\n[shot]: docs/s.png\n' "$hdr" > "$d/good.md"
  printf '# T\n\nMeta OAuth is fully supported in Release 1.\n' > "$d/meta.md"
  printf '# T\n\nMeta OAuth is deferred to Release 2. Build metadata is recorded.\n' > "$d/meta-ok.md"
  printf '# T\n\nRun scripts/appbase-''topology.sh for multiple containers.\n' > "$d/internal.md"
  printf '# T\n\nUse the devcontainer for local work.\n' > "$d/devcontainer.md"
  printf '# T\n\nDevcontainers are contributor environments.\n' > "$d/devcontainers.md"
  printf '# T\n\nThe multi container topology is contributor-only.\n' > "$d/topology-spaced.md"
  printf '# T\n\nThe multi-container topology is contributor-only.\n' > "$d/topology-hyphenated.md"
  printf '# T\n\nThe scripts/tooling area contains contributor automation.\n' > "$d/scripts-tooling.md"
  printf '# T\n\nRun scripts/new-internal-''tool.sh.\n' > "$d/internal-script-path.md"
  printf '# T\n\nRun scripts/new+internal-tool.sh.\n' > "$d/internal-script-plus-path.md"
  printf '# T\n\nRun scripts/@internal.sh.\n' > "$d/internal-script-at-path.md"
  printf '# T\n\nSee scripts/test/readme_storefront_test.sh for the executable evidence.\n' > "$d/public-evidence-script.md"
  printf '# T\n\nRun scripts/test/readme_storefront_test.sh.bak.\n' > "$d/evidence-script-suffix.md"
  printf '# T\n\nRun scripts/test/readme_storefront_test.sh/evil.\n' > "$d/evidence-script-descendant.md"
  printf '# T\n\nRun scripts/test/readme_storefront_test.sh:evil.\n' > "$d/evidence-script-colon-suffix.md"
  printf '# T\n\nSee scripts/test/readme_storefront_test.sh.\n' > "$d/public-evidence-script-period.md"
  printf '# T\n\nRun scripts/api-inventory-fast-check.sh.\n' > "$d/public-inventory-script.md"
  printf '# T\n\nRun scripts/api-inventory-fast-check.sh.bak.\n' > "$d/inventory-script-suffix.md"
  # Construct the negative fixtures at runtime so publication-time reference
  # rewriting does not erase the very syntax this scanner must still reject.
  local mark='#' tracker_repo
  tracker_repo="$(printf 'https://github.com/%s-%s/Appbase' navaneeth mysore)"
  printf '# T\n\nSee %s2563.\nPR %s7\nPR 7\npull request 7\npull request %s7\nissue 9\nissue %s9\n[tracker](%s/issues/8)\nepic 4\nepic %s5\n' \
    "$mark" "$mark" "$mark" "$mark" "$tracker_repo" "$mark" > "$d/tracking.md"
  printf '# T\n\nRelease 1.0.0 needs JDK 25 and Maven 3.6; this is an epic capability.\nSee [Example 4](Examples.md#4-generate-a-configuration-file-with-the-wizard). Literal `internal work item 1234` is sample code.\n\n```text\n#2563 is sample data here\n```\n' > "$d/tracking-ok.md"
  printf '# T\n\nRun ./build.sh --mode compile.\n' > "$d/public-script.md"
  printf '# T\n\nRun appbase-config-wizard/bin/start-console.sh.\n' > "$d/public-wizard.md"

  # Publication-state mutation controls: a hand-typed support row, duplicate marker or
  # premature generated-output link must not be mistaken for the exact prepublication placeholder.
  printf '# Fixture\n\n## Release 1 support\n\n<!-- release-evidence:begin -->\n<!-- release-evidence:end -->\n' > "$d/candidate.md"
  out="$(scan publication-state "$d/candidate.md")" || miss+=("scanner failed: exact candidate placeholder")
  [ "$out" = "candidate" ] || miss+=("exact candidate placeholder was not classified prepublication")
  printf '# Fixture\n\n## Release 1 support\n\n<!-- release-evidence:begin -->\n| Redis | Supported |\n<!-- release-evidence:end -->\n' > "$d/manual.md"
  out="$(scan publication-state "$d/manual.md")" || miss+=("scanner failed: populated publication state")
  [ "$out" = "publication" ] || miss+=("manually populated section escaped publication verification")
  printf '# Fixture\n\n## Release 1 support\n\n<!-- release-evidence:begin -->\n<!-- release-evidence:end -->\n<!-- release-evidence:end -->\n' > "$d/duplicate.md"
  scan publication-state "$d/duplicate.md" >/dev/null 2>&1 \
    && miss+=("duplicate publication end marker was accepted")
  printf '# Fixture\n\n## Release 1 support\n\n<!-- release-evidence:begin -->\n<!-- release-evidence:end -->\n\n[unpublished](docs/release/evidence-index/known-''limitations.md)\n' > "$d/stale.md"
  scan publication-state "$d/stale.md" >/dev/null 2>&1 \
    && miss+=("unpublished generated limitations link was accepted")

  # AC3 mutation control: support-classification drift must make real verify mode complain.
  printf '# Fixture\n\n## Release 1 support\n\n| Capability | Coverage | Artifacts | Inventory check |\n|---|---|---|---|\n| **Redis** | Client and lifecycle | `appbase-client-redis` | VERIFIED vs internal work item 1399 inventory |\n' > "$d/readme.md"
  cp "$ROOT/docs/Module-and-Service-Catalog.md" "$d/catalogue.md"
  python3 - "$ROOT/docs/api-inventory/inventory.json" "$d/inventory.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
for artifact in data["artifacts"]:
    if artifact.get("artifact_id") == "appbase-client-redis":
        artifact["support"] = "internal-implementation"
        break
with open(sys.argv[2], "w", encoding="utf-8") as stream:
    json.dump(data, stream)
PY
  out="$(scan verify "$d/readme.md" "$d/catalogue.md" "$d/inventory.json")" \
    || miss+=("scanner failed: verify mutation")
  printf '%s\n' "$out" | grep -q 'appbase-client-redis.*not supported-public' \
    || miss+=("AC3 inventory-support mutation was not detected by verify mode")
  python3 - "$d/catalogue.md" <<'PY'
import sys
path = sys.argv[1]
text = open(path, encoding="utf-8").read()
text = text.replace("com.viveworks.appbase:appbase-client-redis",
                    "com.viveworks.appbase:appbase-client-redis-mutant")
with open(path, "w", encoding="utf-8") as stream:
    stream.write(text)
PY
  out="$(scan verify "$d/readme.md" "$d/catalogue.md" "$ROOT/docs/api-inventory/inventory.json")" \
    || miss+=("scanner failed: verify catalogue mutation")
  printf '%s\n' "$out" | grep -q 'appbase-client-redis.*absent from the catalogue' \
    || miss+=("AC3 catalogue-removal mutation was not detected by verify mode")

  out="$(scan swagger "$d/shortcut.md")" || miss+=("scanner failed: swagger shortcut")
  printf '%s\n' "$out" | grep -q 'is not a local file' || miss+=("shortcut image with a remote definition was not flagged")
  out="$(scan swagger "$d/collapsed.md")" || miss+=("scanner failed: swagger collapsed")
  printf '%s\n' "$out" | grep -q 'missing or empty' || miss+=("collapsed reference to a missing file was not flagged")
  out="$(scan swagger "$d/good.md")" || miss+=("scanner failed: swagger good")
  [ -z "$out" ] || miss+=("case-insensitive collapsed reference, defined under a later heading, to a real file was flagged: $out")
  out="$(scan deferred "$d/meta.md" appbase-client-spark)" || miss+=("scanner failed: deferred meta")
  [ -n "$out" ] || miss+=("standalone 'Meta OAuth … supported' claim was not flagged")
  out="$(scan deferred "$d/meta-ok.md" appbase-client-spark)" || miss+=("scanner failed: deferred meta-ok")
  [ -z "$out" ] || miss+=("deferred Meta, or the word 'metadata', was flagged: $out")
  local removed_shell
  for removed_shell in appbase-service-password appbase-service-supportmessage-jdbi appbase-service-user-account-vitess; do
    printf '# T\n\n%s is fully supported in Release 1.\n' "$removed_shell" > "$d/removed-shell.md"
    out="$(scan deferred "$d/removed-shell.md" appbase-client-spark)" || miss+=("scanner failed: deferred $removed_shell")
    [ -n "$out" ] || miss+=("removed L-13 shell $removed_shell advertised as supported was not flagged")
  done
  out="$(scan forbidden-public "$d/internal.md")" || miss+=("scanner failed: forbidden-public internal")
  [ -n "$out" ] || miss+=("internal topology tooling was not flagged")
  out="$(scan forbidden-public "$d/devcontainer.md")" || miss+=("scanner failed: forbidden-public devcontainer")
  printf '%s\n' "$out" | grep -q 'devcontainer' \
    || miss+=("singular devcontainer prose was not flagged")
  out="$(scan forbidden-public "$d/devcontainers.md")" || miss+=("scanner failed: forbidden-public devcontainers")
  printf '%s\n' "$out" | grep -q 'devcontainer' \
    || miss+=("plural devcontainers prose was not flagged")
  out="$(scan forbidden-public "$d/topology-spaced.md")" || miss+=("scanner failed: forbidden-public topology-spaced")
  printf '%s\n' "$out" | grep -q 'multi-container topology' \
    || miss+=("spaced multi container topology prose was not flagged")
  out="$(scan forbidden-public "$d/topology-hyphenated.md")" || miss+=("scanner failed: forbidden-public topology-hyphenated")
  printf '%s\n' "$out" | grep -q 'multi-container topology' \
    || miss+=("hyphenated multi-container topology prose was not flagged")
  out="$(scan forbidden-public "$d/scripts-tooling.md")" || miss+=("scanner failed: forbidden-public scripts-tooling")
  printf '%s\n' "$out" | grep -q 'internal scripts tooling' \
    || miss+=("internal scripts/tooling boundary prose was not flagged")
  out="$(scan forbidden-public "$d/internal-script-path.md")" || miss+=("scanner failed: forbidden-public internal-script-path")
  printf '%s\n' "$out" | grep -q 'internal scripts tooling' \
    || miss+=("arbitrary internal scripts path was not flagged")
  out="$(scan forbidden-public "$d/internal-script-plus-path.md")" || miss+=("scanner failed: forbidden-public internal-script-plus-path")
  printf '%s\n' "$out" | grep -q 'internal scripts tooling' \
    || miss+=("internal scripts path containing plus was not flagged")
  out="$(scan forbidden-public "$d/internal-script-at-path.md")" || miss+=("scanner failed: forbidden-public internal-script-at-path")
  printf '%s\n' "$out" | grep -q 'internal scripts tooling' \
    || miss+=("internal scripts path containing at-sign was not flagged")
  out="$(scan forbidden-public "$d/public-evidence-script.md")" || miss+=("scanner failed: forbidden-public public-evidence-script")
  [ -z "$out" ] || miss+=("allowlisted storefront evidence script was wrongly flagged: $out")
  out="$(scan forbidden-public "$d/evidence-script-suffix.md")" || miss+=("scanner failed: forbidden-public evidence-script-suffix")
  printf '%s\n' "$out" | grep -q 'internal scripts tooling' \
    || miss+=("allowlisted evidence path with a .bak suffix was not flagged")
  out="$(scan forbidden-public "$d/evidence-script-descendant.md")" || miss+=("scanner failed: forbidden-public evidence-script-descendant")
  printf '%s\n' "$out" | grep -q 'internal scripts tooling' \
    || miss+=("allowlisted evidence path with a descendant was not flagged")
  out="$(scan forbidden-public "$d/evidence-script-colon-suffix.md")" || miss+=("scanner failed: forbidden-public evidence-script-colon-suffix")
  printf '%s\n' "$out" | grep -q 'internal scripts tooling' \
    || miss+=("allowlisted evidence path with a colon suffix was not flagged")
  out="$(scan forbidden-public "$d/public-evidence-script-period.md")" || miss+=("scanner failed: forbidden-public public-evidence-script-period")
  [ -z "$out" ] || miss+=("allowlisted evidence path with a terminal period was wrongly flagged: $out")
  out="$(scan forbidden-public "$d/public-inventory-script.md")" || miss+=("scanner failed: forbidden-public public-inventory-script")
  [ -z "$out" ] || miss+=("public inventory gate was wrongly flagged: $out")
  out="$(scan forbidden-public "$d/inventory-script-suffix.md")" || miss+=("scanner failed: forbidden-public inventory-script-suffix")
  printf '%s\n' "$out" | grep -q 'internal scripts tooling' \
    || miss+=("allowlisted inventory path with a .bak suffix was not flagged")
  out="$(scan forbidden-public "$d/public-script.md")" || miss+=("scanner failed: forbidden-public public-script")
  [ -z "$out" ] || miss+=("public build.sh command was wrongly flagged: $out")
  out="$(scan forbidden-public "$d/public-wizard.md")" || miss+=("scanner failed: forbidden-public public-wizard")
  [ -z "$out" ] || miss+=("public configuration-wizard launcher was wrongly flagged: $out")
  out="$(scan tracking-public "$d/tracking.md")" || miss+=("scanner failed: tracking-public internal references")
  printf '%s\n' "$out" | grep -q 'issue number' || miss+=("bare internal issue number was not flagged")
  [ "$(printf '%s\n' "$out" | grep -c 'PR number')" -eq 4 ] \
    || miss+=("PR/pull-request variants were not all flagged")
  [ "$(printf '%s\n' "$out" | grep -c 'issue reference')" -eq 2 ] \
    || miss+=("issue variants were not all flagged")
  printf '%s\n' "$out" | grep -q 'tracker link' || miss+=("issue tracker link was not flagged")
  [ "$(printf '%s\n' "$out" | grep -c 'epic reference')" -eq 2 ] \
    || miss+=("numbered epic variants were not all flagged")
  out="$(scan tracking-public "$d/tracking-ok.md")" || miss+=("scanner failed: tracking-public public anchor")
  [ -z "$out" ] || miss+=("ordinary documentation anchor was wrongly flagged: $out")
  # A table row whose supported-claim sits nearer the name than its marker (RA-A1 A1-1).
  printf '# T\n\n| Family | Coverage | Notes |\n|---|---|---|\n| **Batch analytics** | Spark jobs, fully supported in Release 1 | not the deferred adapters |\n' > "$d/row.md"
  printf '# T\n\n| Family | Coverage | Notes |\n|---|---|---|\n| **Batch analytics** | Spark | Deferred to Release 2 |\n' > "$d/row-ok.md"
  out="$(scan deferred "$d/row.md" appbase-client-spark)" || miss+=("scanner failed: deferred row")
  [ -n "$out" ] || miss+=("table row calling Spark 'fully supported' beside a distant marker was not flagged")
  # A tie — claim and marker equally close — fails, as it does in prose (RA-A1 A1-10).
  printf '# T\n\n| Family | Notes |\n|---|---|\n| Batch | deferred Spark fully supported |\n' > "$d/row-tie.md"
  out="$(scan deferred "$d/row-tie.md" appbase-client-spark)" || miss+=("scanner failed: deferred row-tie")
  [ -n "$out" ] || miss+=("table row with a claim exactly as close as its marker was not flagged")
  out="$(scan deferred "$d/row-ok.md" appbase-client-spark)" || miss+=("scanner failed: deferred row-ok")
  [ -z "$out" ] || miss+=("table row marking Spark deferred was flagged: $out")
  rm -rf "$d"
  [ ${#miss[@]} -eq 0 ] && ok "EXTRA_scanner_fixtures" || bad "EXTRA_scanner_fixtures" "${miss[@]}"
}

extra_exist
extra_table_shape
extra_brand_embed
extra_no_internal_experiments
extra_no_internal_tracking
ac1
ac2
ac3
ac4
ac5
ac7
extra_adoc
extra_fixtures
printf 'readme-storefront-test: %d passed, %d failed\n' "$PASSED" "$FAILED"
[ "$FAILED" -eq 0 ]
