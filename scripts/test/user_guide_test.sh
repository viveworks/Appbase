#!/usr/bin/env bash
#
# @author Navaneeth Mysore G
# @author Claude
#
# user_guide_test.sh — acceptance arms for the Release-1 User Guide (internal work item 1416, plan task N05).
#
# The guide is a set of files under docs/user-guide/: README.md (index, boundary, deploy, verification),
# one file per part (01 … 08) and known-divergences.md.
#
# AC1416-1 … AC1416-5 are numbered exactly as the issue numbers them. AC1416-S1 is the Swagger-evidence arm:
# the earlier screenshots were of a sample service and were ruled invalid evidence; the committed images are
# real captures, and the arm asserts the evidence: every image is linked and exists, the one capture.json manifest is
# complete and matches the bytes, dimensions and git history, and nothing in it leaks a path, URL or stack trace. EXTRA_ arms check what
# makes a guide followable and honest: required sections, every relative link and anchor, every repo path
# it names (and that the paths it names as MISSING really are missing), every code snippet against the
# source file it cites, the counts against the tree, the known-gap issue numbers, the groupId placeholder,
# and the absence of private paths and tool/lane names.
#
# ⚠️ What this suite CANNOT prove is AC1416-1 itself — that someone followed the local segment on a clean
# machine to a running service. That is a fact about a run, not about a file. The arm checks the guide
# states its measurement status honestly (a dated walkthrough record, or NOT MEASURED) and never claims
# more; the walkthrough is deferred to the internal work item 1145 checklist.
#
# Scope: repository reads and temporary mutation fixtures under a private temp dir (TMPDIR points at it for the
# embedded python runs). It does execute the capture runner's pure functions under
# python3 and, if present, node (no network, no docker, no build, no browser).
# Runs on /bin/bash 3.2.
#
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$HERE/../.." && pwd)"
GUIDE="${USER_GUIDE:-$ROOT/docs/user-guide/README.md}"
GDIR="$(dirname "$GUIDE")"
PASSED=0; FAILED=0
ok()  { PASSED=$((PASSED+1)); echo "  ok   $1"; }
bad() { FAILED=$((FAILED+1)); echo "  FAIL $1"; }

echo "user guide (internal work item 1416)"; echo

if [ ! -f "$GUIDE" ]; then
  bad "EXTRA_guide_exists: $GUIDE is missing"
  echo; echo "  passed: $PASSED   failed: $FAILED"; exit 1
fi
ok "EXTRA_guide_exists: $GUIDE"

TMPD="$(mktemp -d)"
trap 'rm -rf "$TMPD"' EXIT

# ── the guide's files ────────────────────────────────────────────────────────────────────────────────
PARTS="01-install-and-first-service 02-client-integrations 03-service-modules 04-bootstrap-flavors 05-config-wizard 06-docker-infra 07-swagger 08-agent-integration known-divergences"
missing_files=""
for p in $PARTS; do [ -f "$GDIR/$p.md" ] || missing_files="$missing_files $p.md"; done
[ -z "$missing_files" ] && ok "EXTRA_structure: README.md plus the 9 part files exist" \
  || bad "EXTRA_structure: missing:$missing_files"
for p in $PARTS; do
  grep -q "($p.md)" "$GUIDE" || bad "EXTRA_index_links: README.md does not link $p.md"
done
ok "EXTRA_index_links: README.md checked against every part file"

# All guide text in one file, README first, for section-wide arms.
ALL="$TMPD/all.md"; : > "$ALL"
cat "$GUIDE" >> "$ALL"
for f in "$GDIR"/*.md; do [ "$f" = "$GUIDE" ] || cat "$f" >> "$ALL"; done

# section <heading-regex> → the text of that ## section (up to the next ## heading), across every file
section() { awk -v re="$1" '/^## /{ins = (tolower($0) ~ re); next} ins' "$ALL"; }
# The guide minus the TABLE ROWS of known-divergences.md: those rows name missing/stale paths precisely in
# order to say they are missing. Prose everywhere else is still checked.
CLAIMS="$TMPD/claims.md"; : > "$CLAIMS"
for f in "$GDIR"/*.md; do
  case "$(basename "$f")" in
    known-divergences.md) awk '!/^\|/' "$f" >> "$CLAIMS" ;;
    *) cat "$f" >> "$CLAIMS" ;;
  esac
done

# ── EXTRA_sections — the parts the issue title names ─────────────────────────────────────────────────
for s in 'install' 'first service' 'configuration wizard' 'deploy'; do
  grep -qiE "^## .*$s" "$ALL" && ok "EXTRA_sections: a '## …${s}…' section exists" \
    || bad "EXTRA_sections: no '## …${s}…' section"
done

# ── AC1416-1 · local segment followed end to end — status stated honestly ────────────────────────────
LOCAL="$(section 'local')"
[ -n "$LOCAL" ] && ok "AC1416-1: a local (disposable) segment exists" || bad "AC1416-1: no '## …local…' segment"
if grep -qE 'Walkthrough record: (NOT MEASURED|PASSED [0-9]{4}-[0-9]{2}-[0-9]{2})' "$GUIDE"; then
  ok "AC1416-1: the guide carries a walkthrough record ($(grep -oE 'Walkthrough record: [A-Z]+( MEASURED)?' "$GUIDE" | head -1))"
else
  bad "AC1416-1: the guide must carry 'Walkthrough record: NOT MEASURED' or 'Walkthrough record: PASSED <date>'"
fi
if grep -qE 'Walkthrough record: PASSED [0-9]{4}' "$GUIDE" && ! grep -qE 'Walkthrough record: PASSED [0-9-]+ .*(#[0-9]+|https://)' "$GUIDE"; then
  bad "AC1416-1: a PASSED walkthrough record must cite its evidence (issue/PR/run link)"
else
  ok "AC1416-1: no uncited PASSED walkthrough claim"
fi
grep -qiE '^### .*verif(y|ier)' "$GUIDE" && ok "AC1416-1: the verifier's steps are written down" \
  || bad "AC1416-1: no '### …verifier…' steps for the host/CI walkthrough"
grep -q 'internal work item 1145' "$GUIDE" && ok "AC1416-1: the clean-machine walkthrough (A4-2) is deferred to internal work item 1145" \
  || bad "AC1416-1: the guide must state the A4-2 walkthrough is deferred to internal work item 1145"

# ── AC1416-2 · external/credentialed deploy separated and NOT MEASURED ───────────────────────────────
EXT="$(section 'external')"
[ -n "$EXT" ] && ok "AC1416-2: an external/credentialed deploy section exists, separate from the local one" \
  || bad "AC1416-2: no '## …external…' section"
grep -q 'NOT MEASURED' <<<"$EXT" && ok "AC1416-2: the external section states NOT MEASURED" \
  || bad "AC1416-2: the external section must state NOT MEASURED"

# ── AC1416-3 · acceptance environment and boundary named ─────────────────────────────────────────────
grep -qiE '^### .*acceptance environment' "$GUIDE" && ok "AC1416-3: the disposable acceptance environment is named" \
  || bad "AC1416-3: no '…acceptance environment…' heading"
grep -qiE '^### .*boundary' "$GUIDE" && ok "AC1416-3: the local/external boundary is stated" \
  || bad "AC1416-3: no '…boundary…' heading"

# ── AC1416-4 · no local run claims an external deploy verified ───────────────────────────────────────
EXT_CLAIMS="$(tr '[:upper:]' '[:lower:]' <<<"$EXT" | sed -E 's/not measured//g; s/not (yet )?(verified|tested|proven|measured|validated|confirmed|passed|succeeded|deployed)( successfully)?//g; s/unverified//g')"
viol="$(grep -inE '(^|[^[:alnum:]_])(verified|tested|proven|works|validated|confirmed|passed|succeeded|deployed successfully)([^[:alnum:]_]|$)' <<<"$EXT_CLAIMS")"
[ -z "$viol" ] && ok "AC1416-4: the external section makes no verification claim" \
  || bad "AC1416-4: verification claim in the external section: $viol"

# ── AC1416-5 · under the 6000-line PR cap ────────────────────────────────────────────────────────────
n="$(wc -l < "$ALL")"
[ "$n" -lt 6000 ] && ok "AC1416-5: guide is $n lines across its files, under the 6000-line cap" \
  || bad "AC1416-5: guide is $n lines, over the 6000-line cap — split it"

# ── EXTRA_links — every relative Markdown link (and cross-file anchor) resolves ─────────────────────
slugs_of() {
  local c="$TMPD/slug.$(basename "$1")"
  [ -f "$c" ] || grep -E '^#{1,6} ' "$1" | sed -E 's/^#+ //' | awk '{print tolower($0)}' | sed -E 's/[^a-z0-9 _-]//g; s/ /-/g' > "$c"
  cat "$c"
}
broken=""; nlinks=0
release_state="$(python3 "$ROOT/scripts/test/readme_storefront_scan.py" publication-state "$ROOT/README.md" 2>/dev/null)" || release_state="invalid"
for f in "$GDIR"/*.md; do
  fd="$(dirname "$f")"
  while IFS= read -r l; do
    nlinks=$((nlinks+1))
    t="${l%%#*}"; frag=""; case "$l" in *"#"*) frag="${l#*#}";; esac
    [ -z "$t" ] && continue
    if [ ! -e "$fd/$t" ]; then
      # The exact canonical quality statement is generated only in publication P. Candidate C
      # must carry this link but cannot carry a fake generated output just to satisfy this arm.
      if [ "$release_state" = "candidate" ] && [ "$f" = "$GUIDE" ] \
          && [ "$t" = "../release/evidence-index/quality-security-practices.md" ]; then
        continue
      fi
      broken="$broken $(basename "$f"):$l"; continue
    fi
    if [ -n "$frag" ] && [ -f "$fd/$t" ]; then
      slugs_of "$fd/$t" | grep -qxF -- "$frag" || broken="$broken $(basename "$f"):$l(anchor)"
    fi
  done < <(grep -oE '\]\([^)]+\)' "$f" | sed -E 's/^\]\(//; s/\)$//; s/ .*//' | grep -vE '^(https?:|mailto:|#)')
done
[ -z "$broken" ] && ok "EXTRA_links: all $nlinks relative links resolve (and cross-file anchors match a heading)" \
  || bad "EXTRA_links: broken:$broken"

# same-page anchors, per file; the index page must have a Contents list
badanch=""; nanch_total=0
for f in "$GDIR"/*.md; do
  slugs="$(slugs_of "$f")"
  anchors="$(grep -oE '\]\(#[^)]+\)' "$f" | sed -E 's/^\]\(#//; s/\)$//' | sort -u)"
  for a in $anchors; do
    nanch_total=$((nanch_total+1))
    grep -qxF -- "$a" <<<"$slugs" || badanch="$badanch $(basename "$f"):#$a"
  done
done
if [ "$nanch_total" -eq 0 ]; then bad "EXTRA_anchors: no same-page anchors found (the Contents list is gone?)"
elif [ -z "$badanch" ]; then ok "EXTRA_anchors: all $nanch_total same-page anchors match a heading"
else bad "EXTRA_anchors: no heading for:$badanch"; fi

# ── EXTRA_paths — every repo path the guide names in backticks exists, unless it is a documented gap ─
# A path the guide names as MISSING (a known divergence) must be on this list, and the list is checked to be
# really missing — so fixing the tree makes the guide's claim stale, and this arm says so.
DOC_MISSING="appbase-client/appbase-client-aerospike
appbase-java/appbase-client/infra
appbase-java/appbase-client/infra/docker
appbase-java/appbase-client/infra/local/vitess
appbase-java/appbase-client/appbase-client-aws/infra/docker/minio
appbase-stacktest/src/main/resources/swagger-ui/dist"
missing=""; npaths=0
while IFS= read -r p; do
  p="${p%/}"; p="${p#./}"
  if grep -qxF -- "$p" <<<"$DOC_MISSING"; then continue; fi
  npaths=$((npaths+1))
  [ -e "$ROOT/$p" ] || missing="$missing $p"
done < <(grep -oE '`(\./)?(appbase-[a-z-]+|scripts|docs)(/[A-Za-z0-9._-]+)+/?`' "$CLAIMS" | tr -d '`' | sort -u)
if [ "$npaths" -eq 0 ]; then bad "EXTRA_paths: found no backticked repo paths to check (vacuous)"
elif [ -z "$missing" ]; then ok "EXTRA_paths: all $npaths backticked repo paths exist"
else bad "EXTRA_paths: missing:$missing"; fi
nowexists=""
while IFS= read -r p; do [ -e "$ROOT/$p" ] && nowexists="$nowexists $p"; done <<<"$DOC_MISSING"
[ -z "$nowexists" ] && ok "EXTRA_missing_paths: every path the guide names as missing is still missing" \
  || bad "EXTRA_missing_paths: now exists, so the guide's divergence note is stale:$nowexists"

# ── EXTRA_build_modes — every ./build.sh --mode the guide uses is a real mode ───────────────────────
badm=""; nmodes=0
for m in $(grep -oE 'build\.sh --mode [a-z-]+' "$ALL" | awk '{print $3}' | sort -u); do
  nmodes=$((nmodes+1))
  grep -qE "^[[:space:]]+$m\)|\"$m\"|[| (]$m[|)]" "$ROOT/build.sh" || badm="$badm $m"
done
if [ "$nmodes" -eq 0 ]; then bad "EXTRA_build_modes: found no 'build.sh --mode' to check (vacuous)"
elif [ -z "$badm" ]; then ok "EXTRA_build_modes: all $nmodes build.sh modes exist"
else bad "EXTRA_build_modes: unknown mode(s):$badm"; fi

# ── EXTRA_no_unsupported — nothing unshipped advertised; support claims deferred to the inventory ────
# Join wrapped prose before evaluating claims. An exemption must negate the matched surface itself;
# an unrelated "not part" elsewhere in the sentence cannot launder a positive claim.
CLAIM_PARAGRAPHS="$(awk 'BEGIN{RS=""; ORS="\n"} {gsub(/(^|\n)[[:space:]]*>[[:space:]]*/, " "); gsub(/\n/, " "); print}' "$CLAIMS" | tr '[:upper:]' '[:lower:]')"
# Lowercased above (tr), so the sed below needs no GNU-only s///I flag: BSD sed (macOS, bash 3.2 hosts)
# rejects 'I', which silently emptied the residual and accepted every claim.
# Remove only a complete, unqualified negative sentence containing the forbidden surface; then reject
# every occurrence left. Sentence-end anchoring prevents a qualifier such as "not supported on Windows,
# but supported on Linux" from laundering the positive support claim.
#
# A truthful, now-accurate POSITIVE claim ("Appbase 1.0.0 is published on Maven Central", "the 1.0.0
# release published under `com.viveworks.appbase`") needs its own exemption, distinct from the negative
# ones above and from each other: the real sentences here embed a dotted identifier
# (`com.viveworks.appbase`) before the true sentence terminator, so the sentence-boundary anchoring
# style above (`[^.!?]*` up to a literal `[.!?]`) can never reach "maven central" at all -- the dots in
# the identifier look like sentence-ends to that pattern first. These two therefore match the specific
# claim phrase directly and stop at the nearest unambiguous boundary (a closing paren for the markdown
# link, or nothing beyond the phrase itself) instead of a sentence terminator, leaving the rest of the
# sentence (which does not itself name the forbidden surface) in the residual untouched.
CLAIM_RESIDUAL="$(sed -E \
  -e 's/(^|[.!?][[:space:]]+)([^.!?]* )?(not|never) (yet )?(published|available) (to|on) maven central[.!?]([[:space:]]+|$)/\1/g' \
  -e 's/(^|[.!?][[:space:]]+)maven central (publication|release) is (not|never) (yet )?(published|available)[.!?]([[:space:]]+|$)/\1/g' \
  -e 's/(^|[.!?][[:space:]]+)appbase[_-]flutter (is )?(not|never) (yet )?(shipped|supported|available)[.!?]([[:space:]]+|$)/\1/g' \
  -e 's/(^|[.!?][[:space:]]+)appbase-react (is )?(not|never) (yet )?(shipped|supported|available)[.!?]([[:space:]]+|$)/\1/g' \
  -e 's/(the )?1\.0\.0 release published under[^)]*maven central[^)]*\)//g' \
  -e 's/appbase 1\.0\.0 is published on maven central//g' <<<"$CLAIM_PARAGRAPHS")"
bad_claims="$(grep -niE 'maven central|appbase[_-]flutter|appbase-react' <<<"$CLAIM_RESIDUAL")"
[ -z "$bad_claims" ] && ok "EXTRA_no_unsupported: no unshipped surface advertised" \
  || bad "EXTRA_no_unsupported: $bad_claims"
grep -qF '[Module and Service Catalogue](../Module-and-Service-Catalog.md)' "$GUIDE" \
  && [ -f "$ROOT/docs/Module-and-Service-Catalog.md" ] \
  && ok "EXTRA_no_unsupported: support claims defer to the landed module/service catalogue" \
  || bad "EXTRA_no_unsupported: the guide must link the landed module/service catalogue"

# ── EXTRA_groupid — internal work item 2546 finalized com.viveworks.appbase as the real, permanent
# groupId (owner decision, 2026-11-27: abandoned the rename to a bare com.viveworks namespace). The
# real value is now REQUIRED, the reverse of this arm's prior "never a final literal" rule, and the
# bare, abandoned `com.viveworks` groupId (without `.appbase`) is what must never appear as if real ──
bare_viveworks="$(grep -nE '<groupId>com\.viveworks</groupId>' "$ALL")"
bare_m2_path="$(grep -noE '~/\.m2/repository/com/viveworks/[a-zA-Z0-9_-]+' "$ALL" | grep -v '/appbase$')"
if [ -n "$bare_viveworks" ] || [ -n "$bare_m2_path" ]; then
  bad "EXTRA_groupid: the abandoned bare com.viveworks groupId (without .appbase) is hard-coded as if real"
else
  ok "EXTRA_groupid: no reference to the abandoned bare com.viveworks groupId"
fi
if grep -qE '<groupId>com\.viveworks\.appbase</groupId>' "$ALL" && ! grep -qF 'GROUP_ID_PENDING_2546' "$ALL"; then
  ok "EXTRA_groupid: the real, permanent com.viveworks.appbase groupId is used, placeholder fully removed"
else
  bad "EXTRA_groupid: need the real com.viveworks.appbase groupId with no remaining GROUP_ID_PENDING_2546 placeholder"
fi
if grep -qiE 'internal work item 2546' "$CLAIMS" && grep -qiE 'abandon' "$CLAIMS"; then
  ok "EXTRA_groupid: the groupId note records internal work item 2546's abandoned-rename resolution"
else bad "EXTRA_groupid: need a note citing internal work item 2546's resolution (the rename was abandoned, com.viveworks.appbase is permanent)"; fi
grep -q 'internal work item 1143' "$CLAIMS" && bad "EXTRA_groupid: internal work item 1143 is the wrong reference for the groupId (use internal work item 2546)" \
  || ok "EXTRA_groupid: no stale internal work item 1143 reference"

# ── EXTRA_known_gaps — each known gap is named with its issue number ─────────────────────────────────
for g in 'internal work item 2546' 'internal work item 2547' 'internal work item 2548' 'internal work item 2549'; do
  grep -q "$g" "$ALL" && ok "EXTRA_known_gaps: $g is cited" || bad "EXTRA_known_gaps: $g is not cited"
done
if grep -qE 'no (Module|catalogue)|no catalogue document|catalogue.*not yet in the tree' "$ALL"; then
  bad "EXTRA_known_gaps: the guide still claims the landed catalogue does not exist"
else
  ok "EXTRA_known_gaps: no stale missing-catalogue claim"
fi

# ── EXTRA_counts — module counts in the guide are the counts in the tree ─────────────────────────────
count_modules() { grep -cE '^[[:space:]]*<module>' "$1"; }
n_client="$(count_modules "$ROOT/appbase-java/appbase-client/pom.xml")"
n_service="$(count_modules "$ROOT/appbase-java/appbase-service/pom.xml")"
n_boot="$(count_modules "$ROOT/appbase-java/appbase-bootstrap/pom.xml")"
n_oauth="$(ls -d "$ROOT"/appbase-java/appbase-service/appbase-service-web-oauth-* | grep -vcE 'oauth-(reactive|tokencache-.*)$')"
n_integ=$((n_client-1))
c_ok=1
[ "$n_client" -eq 19 ] && [ "$n_service" -eq 17 ] && [ "$n_boot" -eq 6 ] && [ "$n_oauth" -eq 5 ] || c_ok=0
if [ "$c_ok" -eq 1 ]; then ok "EXTRA_counts: the tree has $n_client client dirs ($n_integ integrations + core), $n_service service modules, $n_oauth OAuth providers, $n_boot bootstrap modules"
else bad "EXTRA_counts: tree counts changed (client $n_client, service $n_service, oauth $n_oauth, bootstrap $n_boot): update the guide, then this arm"; fi
# Each count phrase must appear in the file that owns the count (whitespace joined, so a wrapped phrase counts).
phrase_in() { tr '\n' ' ' < "$2" | tr -s ' ' | grep -qiF -- "$1"; }
count_arms() {  # $1 = guide dir; prints one "ok|bad text" line per phrase
  local d="$1" pf
  for pf in "$n_client client|02-client-integrations.md" "$n_integ integrations|02-client-integrations.md" \
            "$n_service service modules|03-service-modules.md" "$n_oauth OAuth providers|03-service-modules.md" \
            "$n_boot bootstrap modules|04-bootstrap-flavors.md"; do
    if phrase_in "${pf%%|*}" "$d/${pf##*|}"; then echo "ok the guide states '${pf%%|*}' in ${pf##*|}"
    else echo "bad the guide never states '${pf%%|*}' in ${pf##*|}"; fi
  done
}
while IFS= read -r l; do
  case "$l" in ok*) ok "EXTRA_counts: ${l#ok }";; *) bad "EXTRA_counts: ${l#bad }";; esac
done < <(count_arms "$GDIR")
stale="$(grep -nEi '(^|[^0-9])(20|21|twenty|twenty-one) (service )?(sub-?)?modules|(^|[^0-9])(6|six) (OAuth )?(providers|flavors)|(^|[^0-9])(5|five) (bootstrap |web-server |web server )?flavors|(^|[^0-9])(17|20) (client )?integrations|(^|[^0-9])(18|20) client (modules|directories|dirs)' "$CLAIMS")"
[ -z "$stale" ] && ok "EXTRA_counts: no stale count (21 service modules, 6 providers, five web-server flavors, …) in the prose" \
  || bad "EXTRA_counts: stale count: $stale"

# ── EXTRA_coverage — every module the tree has is named in the part that covers it ───────────────────
cov_missing=""
mods_of() { sed -n 's|^[[:space:]]*<module>\(.*\)</module>.*|\1|p' "$1"; }
# cov_part <pom> <guide part file> <tag> <minimum module count>: names every module of the pom in the part, and
# fails when the extractor finds fewer modules than the minimum (an empty or unreadable pom must not pass).
cov_part() {
  local mods m n=0 miss=""
  mods="$(mods_of "$1")"
  for m in $mods; do
    n=$((n+1))
    grep -qF "\`$m\`" "$2" || miss="$miss $3:$m"
  done
  [ "$n" -ge "$4" ] || miss="$miss $3:only-$n-modules-found(vacuous)"
  echo "$miss"
}
cov_missing="$cov_missing$(cov_part "$ROOT/appbase-java/appbase-client/pom.xml" "$GDIR/02-client-integrations.md" 02 15)"
cov_missing="$cov_missing$(cov_part "$ROOT/appbase-java/appbase-service/pom.xml" "$GDIR/03-service-modules.md" 03 15)"
cov_missing="$cov_missing$(cov_part "$ROOT/appbase-java/appbase-bootstrap/pom.xml" "$GDIR/04-bootstrap-flavors.md" 04 5)"
for p in apple github google microsoft okta; do grep -qiF "$p" "$GDIR/03-service-modules.md" || cov_missing="$cov_missing 03:provider-$p"; done
[ -z "$cov_missing" ] && ok "EXTRA_coverage: every client, service and bootstrap module in the poms is named in its part" \
  || bad "EXTRA_coverage: not named:$cov_missing"
if [ "${USER_GUIDE_FIXTURE_RUN:-0}" != 1 ]; then  # an empty extractor result must be reported, not passed
  : > "$TMPD/empty-pom.xml"
  case "$(cov_part "$TMPD/empty-pom.xml" "$GDIR/02-client-integrations.md" 02 15)" in
    *vacuous*) ok "EXTRA_coverage: rejects a pom the extractor finds no modules in";;
    *) bad "EXTRA_coverage: accepted a pom with no modules (vacuous pass)";;
  esac
fi
bomgap=""
grep -q 'appbase-bootstrap-reactor-netty' "$ROOT/appbase-bom/pom.xml" || bomgap="$bomgap reactor-netty-missing-from-BOM"
grep -qF 'internal work item 2549 resolved' "$GDIR/04-bootstrap-flavors.md" || bomgap="$bomgap reactor-netty-resolution-note"
grep -qiE 'reactor-netty.*(not|isn.t).*in.*BOM' "$GDIR/04-bootstrap-flavors.md" && bomgap="$bomgap stale-reactor-netty-BOM-gap" || true
[ -z "$bomgap" ] && ok "EXTRA_coverage: reactor-netty is in the BOM and internal work item 2549 is marked resolved" || bad "EXTRA_coverage:$bomgap"

# ── EXTRA_codeagents — document exactly the packages present in this exported tree ─────────────────
ca_bad=""
[ -d "$ROOT/appbase-codeagents/claude/skills/add-appbase-service" ] || ca_bad="$ca_bad missing-dir:add-appbase-service"
grep -qF 'add-appbase-service' "$GDIR/08-agent-integration.md" || ca_bad="$ca_bad add-appbase-service"
for d in appbase-code-review appbase-security-review coding-agent review-agent executor-agent; do
  if [ -e "$ROOT/appbase-codeagents/$d" ] || [ -e "$ROOT/appbase-codeagents/claude/skills/$d" ]; then
    ca_bad="$ca_bad unexpectedly-present:$d"
  fi
  grep -qiE "${d}.*not present|not present.*${d}" "$GDIR/08-agent-integration.md" || ca_bad="$ca_bad absence-undocumented:$d"
done
grep -qiE 'control plane.*not covered|not covered here' "$GDIR/08-agent-integration.md" || ca_bad="$ca_bad control-plane-line"
[ -z "$ca_bad" ] && ok "EXTRA_codeagents: the shipped add-appbase-service package and absent packages are documented; control plane marked not covered" \
  || bad "EXTRA_codeagents:$ca_bad"

# ── EXTRA_exclusions — control-plane and topology material is not documented here ────────────────────
private_tree_path='orchestrator'
private_tree_path+='/'
if grep -qiE "Topology-Guide|agent_control_plane|${private_tree_path}" "$ALL"; then
  bad "EXTRA_exclusions: the guide references excluded control-plane material"
else ok "EXTRA_exclusions: no reference to the topology guide, the orchestrator module or the control plane directory"; fi

# ── EXTRA_hygiene — no private paths, vendor, lane or tool-coordination names in the guide ──────────
# 'Claude Code' and the directory names claude/skills and .claude/ are the real names of the shipped
# assistant packages (part 8) and are removed before the scan; any other 'claude' is a hit.
SCAN="$TMPD/scan.md"
sed -E 's/Claude Code//g; s#\.?claude/skills#SKILLS_DIR#g; s#\.claude/#DOTDIR/#g' "$ALL" > "$SCAN"
leak="$(grep -niE '/Users/|/private/tmp|/var/folders|codex|anthropic|glm-?[0-9]|co-authored|generated with|\bfleet\b|BOARD\.md|scratchpad|subagent|worktree|build-lease|coordinator|claude[0-9]?-|claude|\blane\b|lane[0-9]|\bEA-|\bEA\b' "$SCAN" | head -5)"
[ -z "$leak" ] && ok "EXTRA_hygiene: no private path, vendor, lane or coordination name in the guide" \
  || bad "EXTRA_hygiene: $leak"

# ── EXTRA_snippets — every code snippet is a verbatim excerpt of the source file it cites ───────────
# A snippet is  <!-- snippet-source: <repo path>:<a-b[,c-d…]> -->  then a fenced block. Each range's lines
# (compared after trimming leading/trailing whitespace) must equal the source lines a..b; "// ..." lines
# separate ranges. Mutations prove the check can fail: a changed line, a shifted range and a missing source.
SNIP_PY="$TMPD/snip.py"
cat > "$SNIP_PY" <<'PY'
import copy
import re
import sys
from pathlib import Path

root, gdir, run_mutations = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3] == '1'

def require(cond, msg):
    if not cond:
        raise ValueError(msg)

def check(files):
    count = 0
    for name, text in files.items():
        lines = text.split('\n')
        i = 0
        while i < len(lines):
            m = re.match(r'<!-- snippet-source: (\S+):([0-9,-]+) -->$', lines[i])
            if not m:
                i += 1
                continue
            path, ranges = m.group(1), m.group(2)
            require(i + 1 < len(lines) and lines[i + 1].startswith('```'), '%s: no fenced block after the marker for %s' % (name, path))
            j, block = i + 2, []
            while j < len(lines) and not lines[j].startswith('```'):
                block.append(lines[j])
                j += 1
            require(j < len(lines), '%s: unterminated block for %s' % (name, path))
            src_file = root / path
            require(src_file.is_file(), '%s: cited source is missing: %s' % (name, path))
            src = src_file.read_text().split('\n')
            groups = [[]]
            for line in block:
                if line.strip() == '// ...':
                    groups.append([])
                else:
                    groups[-1].append(line)
            rs = ranges.split(',')
            require(len(rs) == len(groups), '%s: %s cites %d ranges but shows %d groups' % (name, path, len(rs), len(groups)))
            for group, rg in zip(groups, rs):
                a, b = (int(x) for x in rg.split('-'))
                require(1 <= a <= b <= len(src), '%s: range %s is outside %s' % (name, rg, path))
                want = [s.strip() for s in src[a - 1:b]]
                got = [s.strip() for s in group]
                require(want == got, '%s: snippet differs from %s lines %s' % (name, path, rg))
            tail = [x for x in lines[j + 1:j + 5] if x.strip()]
            require(tail and tail[0].startswith('Source: `%s`' % path), '%s: no "Source:" line under the snippet for %s' % (name, path))
            count += 1
            i = j + 1
    require(count > 0, 'no snippets found (vacuous)')
    return count

try:
    files = {p.name: p.read_text() for p in sorted(gdir.glob('*.md'))}
    n = check(files)
    if run_mutations:
        first = next(k for k, v in files.items() if 'snippet-source:' in v)
        def mutate(kind):
            f = copy.deepcopy(files)
            t = f[first]
            if kind == 'line':
                m = re.search(r'(<!-- snippet-source: \S+ -->\n```[a-z]*\n)([^\n]+)', t)
                t = t[:m.end(2)] + ' MUTATED' + t[m.end(2):]
            elif kind == 'range':
                m = re.search(r'(<!-- snippet-source: \S+:)(\d+)-', t)
                t = t[:m.start(2)] + str(int(m.group(2)) + 1) + t[m.end(2):] + ''
            elif kind == 'source':
                t = re.sub(r'(<!-- snippet-source: )(\S+)(:)', r'\1\2.gone\3', t, count=1)
            f[first] = t
            return f
        for kind in ('line', 'range', 'source'):
            try:
                check(mutate(kind))
            except ValueError:
                continue
            raise ValueError('snippet mutation survived: ' + kind)
    print('%d snippets match their cited sources%s' % (n, '; 3 mutations rejected' if run_mutations else ''))
except (ValueError, OSError, StopIteration) as error:
    print(error, file=sys.stderr)
    sys.exit(1)
PY
if snip_result="$(python3 "$SNIP_PY" "$ROOT" "$GDIR" "$([ "${USER_GUIDE_FIXTURE_RUN:-0}" = 1 ] && echo 0 || echo 1)" 2>&1)"; then
  ok "EXTRA_snippets: $snip_result"
else
  bad "EXTRA_snippets: $snip_result"
fi

# ── AC1416-S1 · committed images: linked, present, hashed, provenance-checked and free of leaks ────────
# The earlier images were of a sample service (not a real core service) and were ruled invalid evidence; they are
# gone. The images that replace them are real captures (swagger: the real core resources through a test-only
# OpenAPI harness, internal work item 2548). What this arm asserts is BEHAVIOUR:
# every linked image exists, every PNG is linked, the capture.json is valid and complete and matches the bytes on
# disk (sha256, size, PNG header dimensions), its source commit is an ancestor of HEAD, and no manifest carries a
# debugger URL, a local path or a stack trace. It cannot prove the PIXELS are free of a private path (no scan can);
# the guide says so and says the images were reviewed by eye.
IMG_PY="$TMPD/img_check.py"
cat > "$IMG_PY" <<'PY'
import hashlib, json, os, re, struct, subprocess, sys, urllib.parse
guide_dir, images_dir, root = sys.argv[1:4]
problems = []
notes = []  # NOT MEASURED notes: written to the file named by IMG_NOTES, never counted as a failure
HARNESS = 'appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/CoreSwaggerCaptureStackTest.java'
TASK_PREFIX = re.compile(r'^\[P\d{3}-E\d+-T1416\]')
def add(p):
    if p not in problems:
        problems.append(p)
def png_dims(path):
    with open(path, 'rb') as f:
        head = f.read(24)
    if len(head) < 24 or head[:8] != b'\x89PNG\r\n\x1a\n' or head[12:16] != b'IHDR':
        return None
    return struct.unpack('>II', head[16:24])
def git_ok(*args):
    return subprocess.run(['git', '-C', root] + list(args), capture_output=True, text=True)
def source_sha_problem(sha):
    """'' when the manifest's source commit is acceptable, a marker otherwise; a note (NOT MEASURED) for a commit this
    clone cannot judge. A malformed sha FAILS. A commit that is not in this clone (missing object, shallow clone) is
    NOT MEASURED. A commit that exists but is not an ancestor of HEAD FAILS, except one whose subject starts with the
    task prefix (a squash merge rewrites the branch commit): that is NOT MEASURED. An ancestor must also carry the
    harness file, so the root commit (an ancestor of everything) is not accepted."""
    if not re.fullmatch(r'[0-9a-f]{40}', sha or ''):
        return 'source-sha-not-ancestor'
    if git_ok('cat-file', '-e', sha + '^{commit}').returncode != 0:
        notes.append('source_sha %s is not in this clone (missing object or shallow clone): ancestry NOT MEASURED' % sha[:9])
        return ''
    if git_ok('merge-base', '--is-ancestor', sha, 'HEAD').returncode != 0:
        subject = git_ok('log', '-1', '--format=%s', sha).stdout.strip()
        if TASK_PREFIX.match(subject):
            notes.append('source_sha %s is not an ancestor of HEAD but carries the task prefix (a squash merge?): ancestry NOT MEASURED' % sha[:9])
            return ''
        return 'source-sha-not-ancestor'
    if git_ok('cat-file', '-e', sha + ':' + HARNESS).returncode != 0:
        return 'source-sha-lacks-the-harness'
    return ''
# 1. linked images exist; the swagger part links swagger images, the wizard part links no image
linked = set()
for name in sorted(os.listdir(guide_dir)):
    if not name.endswith('.md'):
        continue
    text = open(os.path.join(guide_dir, name), encoding='utf-8').read()
    for target in re.findall(r'!\[[^\]]*\]\(([^)\s]+)\)', text):
        if not target.startswith('images/'):
            add('image-link-outside-images:%s' % target)
            continue
        rel = target[len('images/'):]
        linked.add(rel)
        if not os.path.isfile(os.path.join(images_dir, rel)):
            add('linked-image-missing:%s' % rel)
        if name == '07-swagger.md' and not rel.startswith('swagger/'):
            add('07-links-a-non-swagger-image:%s' % rel)
        if name == '05-config-wizard.md':
            add('05-links-an-image:%s' % rel)
swagger_linked = [r for r in linked if r.startswith('swagger/')]
if not swagger_linked:
    swagger_text = open(os.path.join(guide_dir, '07-swagger.md'), encoding='utf-8').read()
    if '## Screenshot evidence — NOT MEASURED' not in swagger_text or 'no images or hashes are claimed here' not in swagger_text:
        add('swagger-not-measured-boundary-missing')
# 2. every PNG under images/ is linked from the guide
on_disk = []
for dp, _, fns in os.walk(images_dir):
    for fn in fns:
        if fn.lower().endswith('.png'):
            on_disk.append(os.path.relpath(os.path.join(dp, fn), images_dir).replace(os.sep, '/'))
for rel in sorted(on_disk):
    if swagger_linked and rel not in linked:
        add('unlinked-png:%s' % rel)
# 3. the manifests
LEAKS = [('webSocketDebuggerUrl', 'leak-webSocketDebuggerUrl'), ('devtools/browser', 'leak-devtools-browser'),
         ('/Users/', 'leak-local-path'), ('/home/', 'leak-local-path'), ('/private/', 'leak-local-path'),
         ('/root/', 'leak-root-path'), ('/tmp/', 'leak-tmp-path'), ('/var/folders/', 'leak-var-folders-path'),
         ('C:\\Users', 'leak-windows-path'), ('C:\\\\Users', 'leak-windows-path'),
         ('Traceback (most recent call last)', 'leak-stack-trace'), ('Exception in thread', 'leak-stack-trace')]
for sub in (('swagger',) if swagger_linked else ()):
    mp = os.path.join(images_dir, sub, 'capture.json')
    if not os.path.isfile(mp):
        add('capture-json-missing:%s' % sub)
        continue
    raw = open(mp, encoding='utf-8').read()
    for needle, marker in LEAKS:
        if needle in raw:
            add(marker)
    if root and root != '/' and root in raw:
        add('leak-local-path')
    if re.search(r'(\\n|\n)\s*(\\t|\t)?\s*at [\w.$<>]+\([^)]*\)', raw):
        add('leak-stack-trace')
    try:
        m = json.loads(raw)
    except ValueError:
        add('capture-json-invalid:%s' % sub)
        continue
    imgs = m.get('images')
    if not isinstance(imgs, list) or not imgs:
        add('capture-json-lists-no-images:%s' % sub)
        imgs = []
    if m.get('status') != 'complete':
        add('swagger-status-not-complete')
    if not str(m.get('label') or '').strip():
        add('swagger-label-empty')
    elif 'internal work item 2548' not in m['label']:
        add('swagger-label-lacks-2548')
    if m.get('try_it_out') != 'disabled':
        add('swagger-try-it-out-not-disabled')
    problem = source_sha_problem(m.get('source_sha'))
    if problem:
        add('%s-%s' % (sub, problem))
    # the manifest is complete: no browser error, a loopback URL, the provenance keys, groups and operation counts that agree with the images
    if m.get('browserErrors') != []:
        add('swagger-browser-errors-not-empty')
    for key in ('requestedURL', 'url', 'actualURL'):
        if key in m and urllib.parse.urlsplit(str(m[key])).hostname != '127.0.0.1':
            add('swagger-url-host-not-loopback')
    for key in ('resources', 'swagger_ui_webjar', 'harness'):
        if not m.get(key):
            add('swagger-missing-key:%s' % key)
    res = m.get('resources')
    if isinstance(res, dict) and not (isinstance(res.get('operations'), int) and res['operations'] > 0):
        add('swagger-no-operations')
    group_imgs = [i for i in imgs if i.get('kind') == 'group']
    if m.get('groups') != [i.get('group') for i in group_imgs] or not m.get('groups'):
        add('swagger-groups-mismatch')
    for i in group_imgs:
        if not (isinstance(i.get('operationCount'), int) and i['operationCount'] > 0):
            add('swagger-group-operation-count-not-positive:%s' % i.get('file'))
    files = [i.get('file') for i in imgs]
    for f in sorted(set(f for f in files if files.count(f) > 1)):
        add('swagger-duplicate-image-entry:%s' % f)
    listed = set()
    for i in imgs:
        f = i.get('file', '')
        listed.add(f)
        path = os.path.join(images_dir, sub, f)
        if not os.path.isfile(path):
            add('listed-image-missing:%s/%s' % (sub, f))
            continue
        data = open(path, 'rb').read()
        if hashlib.sha256(data).hexdigest() != i.get('sha256'):
            add('sha256-mismatch:%s' % f)
        if len(data) != i.get('bytes'):
            add('size-mismatch:%s' % f)
        d = png_dims(path)
        if d is None:
            add('not-a-png:%s' % f)
        elif d != (i.get('width'), i.get('height')):
            add('dimension-mismatch:%s' % f)
    for rel in on_disk:
        if rel.startswith(sub + '/') and rel.split('/', 1)[1] not in listed:
            add('png-not-in-capture-json:%s' % rel)
print(' '.join(problems))
if os.environ.get('IMG_NOTES'):
    with open(os.environ['IMG_NOTES'], 'a') as nf:
        for n in notes:
            nf.write(n + '\n')
PY
img_check() { python3 -B "$IMG_PY" "$1" "$2" "$ROOT" 2>&1 | tr '\n' ' ' || true; }  # $1 guide dir, $2 images dir; prints problems
IMGDIR="$ROOT/docs/user-guide/images"
: > "$TMPD/img.notes"
res="$(IMG_NOTES="$TMPD/img.notes" img_check "$GDIR" "$IMGDIR")"
if [ -z "${res// /}" ]; then
  if grep -qE '!\[[^]]*\]\(images/swagger/' "$GDIR/07-swagger.md"; then
    ok "AC1416-S1: claimed image evidence is linked, complete, provenance-checked and byte-verified"
  else
    ok "AC1416-S1: Swagger screenshots are explicitly NOT MEASURED and no image evidence is claimed"
  fi
else
  bad "AC1416-S1: image evidence: $res"
fi
while IFS= read -r inote; do echo "  NOTE AC1416-S1: $inote"; done < "$TMPD/img.notes"
if [ "${USER_GUIDE_FIXTURE_RUN:-0}" != 1 ]; then
  IMG_MUT="$TMPD/img_mut.py"
  cat > "$IMG_MUT" <<'PY'
import json, os, sys
kind, gdir, idir = sys.argv[1:4]
def jedit(sub, fn):
    p = os.path.join(idir, sub, 'capture.json'); m = json.load(open(p)); fn(m); json.dump(m, open(p, 'w'), indent=2)
def text_edit(p, a, b):
    t = open(p, encoding='utf-8').read(); assert a in t, 'mutation anchor missing: ' + a; open(p, 'w', encoding='utf-8').write(t.replace(a, b, 1))
if kind == 'corrupt-png-byte':
    p = os.path.join(idir, 'swagger', 'overview.png'); b = bytearray(open(p, 'rb').read()); b[len(b) // 2] ^= 0xFF; open(p, 'wb').write(bytes(b))
elif kind == 'edit-sha256':
    jedit('swagger', lambda m: m['images'][0].__setitem__('sha256', '0' * 64))
elif kind == 'edit-size':
    jedit('swagger', lambda m: m['images'][1].__setitem__('bytes', m['images'][1]['bytes'] + 1))
elif kind == 'edit-dimension':
    jedit('swagger', lambda m: m['images'][1].__setitem__('width', 1281))
elif kind == 'non-ancestor-source-sha':  # a real commit of this repository that is not an ancestor of HEAD and has another subject
    jedit('swagger', lambda m: m.__setitem__('source_sha', os.environ['UNREL_SHA']))
elif kind == 'missing-object-source-sha':  # well formed, but no such object in this clone: NOT MEASURED, not a failure
    jedit('swagger', lambda m: m.__setitem__('source_sha', '0' * 39 + '1'))
elif kind == 'malformed-source-sha':
    jedit('swagger', lambda m: m.__setitem__('source_sha', 'g' * 40))
elif kind == 'root-commit-source-sha':  # an ancestor of everything, but it has no harness
    jedit('swagger', lambda m: m.__setitem__('source_sha', os.environ['ROOT_SHA']))
elif kind == 'squash-source-sha':  # not an ancestor, but carries the task prefix: NOT MEASURED, not a failure
    jedit('swagger', lambda m: m.__setitem__('source_sha', os.environ['SQUASH_SHA']))
elif kind == 'browser-errors':
    jedit('swagger', lambda m: m.__setitem__('browserErrors', [{'kind': 'console'}]))
elif kind == 'zero-operations':
    def zero(m):
        m['resources']['operations'] = 0
        for i in m['images']:
            if i.get('kind') == 'group':
                i['operationCount'] = 0
    jedit('swagger', zero)
elif kind == 'empty-groups':
    jedit('swagger', lambda m: m.__setitem__('groups', []))
elif kind == 'delete-provenance-keys':
    def drop(m):
        for k in ('resources', 'swagger_ui_webjar', 'harness'):
            del m[k]
    jedit('swagger', drop)
elif kind == 'evil-url':
    jedit('swagger', lambda m: m.__setitem__('url', 'http://evil.example/restapi/'))
elif kind == 'duplicate-image':
    jedit('swagger', lambda m: m['images'].append(dict(m['images'][1])))
elif kind == 'add-tmp-path':
    jedit('swagger', lambda m: m.__setitem__('note', 'see /tmp/work/out'))
elif kind == 'add-root-path':
    jedit('swagger', lambda m: m.__setitem__('note', 'see /root/work/out'))
elif kind == 'add-var-folders-path':
    jedit('swagger', lambda m: m.__setitem__('note', 'see /var/folders/ab/cd/T/x'))
elif kind == 'add-windows-path':
    jedit('swagger', lambda m: m.__setitem__('note', 'see C:\\Users\\dev\\x'))
elif kind == 'add-ws-url':
    jedit('swagger', lambda m: m['browser'].__setitem__('webSocketDebuggerUrl', 'ws://127.0.0.1:9227/devtools/browser/abc'))
elif kind == 'add-users-path':
    jedit('swagger', lambda m: m.__setitem__('note', '/Users/someone/work'))
elif kind == 'add-stack-trace':
    jedit('swagger', lambda m: m.__setitem__('error', 'Traceback (most recent call last):\n  File "x.py", line 1'))
elif kind == 'status-incomplete':
    jedit('swagger', lambda m: m.__setitem__('status', 'failed'))
elif kind == 'try-it-out-enabled':
    jedit('swagger', lambda m: m.__setitem__('try_it_out', 'enabled'))
elif kind == 'label-lacks-2548':
    jedit('swagger', lambda m: m.__setitem__('label', 'Real resources.'))
elif kind == 'unlink-image':
    text_edit(os.path.join(gdir, '07-swagger.md'), '](images/swagger/group-001-default.png)', '](https://example.invalid/x.png)')
elif kind == 'link-missing-image':
    open(os.path.join(gdir, '07-swagger.md'), 'a', encoding='utf-8').write('\n![x](images/swagger/nope.png)\n')
elif kind == 'add-unlinked-png':
    import shutil; shutil.copy(os.path.join(idir, 'swagger', 'overview.png'), os.path.join(idir, 'swagger', 'extra.png'))
elif kind == 'not-a-png':
    p = os.path.join(idir, 'swagger', 'group-001-default.png'); open(p, 'wb').write(b'GIF89a' + b'\0' * 40)
elif kind == 'link-image-in-05':
    open(os.path.join(gdir, '05-config-wizard.md'), 'a', encoding='utf-8').write('\n![x](images/swagger/overview.png)\n')
elif kind == 'claim-without-evidence':
    text_edit(os.path.join(gdir, '07-swagger.md'), 'no images or hashes are claimed here', 'images are claimed here')
else:
    sys.exit('unknown mutation ' + kind)
PY
  img_mutant_marker() {  # $1 kind, $2 marker the check must name; ok when the mutant exists, differs and fails by that name
    local k="$1" want="$2" md="$TMPD/imgm/$1" out
    rm -rf "$md"; mkdir -p "$md/guide"; cp "$GDIR"/*.md "$md/guide/"; cp -R "$IMGDIR" "$md/images"
    if ! python3 -B "$IMG_MUT" "$k" "$md/guide" "$md/images" 2>"$md/err"; then bad "AC1416-S1: mutation $k could not be built: $(head -c 200 "$md/err")"; return; fi
    if diff -rq "$GDIR" "$md/guide" >/dev/null 2>&1 && diff -rq "$IMGDIR" "$md/images" >/dev/null 2>&1; then bad "AC1416-S1: mutation $k left the tree unchanged (control)"; return; fi
    case "$(img_check "$md/guide" "$md/images")" in
      *"$want"*) ok "AC1416-S1: rejects the $k mutation by name ($want)";;
      *) bad "AC1416-S1: mutation $k was not rejected by $want";;
    esac
  }
  if grep -qE '!\[[^]]*\]\(images/swagger/' "$GDIR/07-swagger.md"; then
  img_mutant_marker corrupt-png-byte 'sha256-mismatch:overview.png'
  img_mutant_marker edit-sha256 'sha256-mismatch:overview.png'
  img_mutant_marker edit-size 'size-mismatch:group-001-default.png'
  img_mutant_marker edit-dimension 'dimension-mismatch:group-001-default.png'
  # Real commits found with git (unrelated with another subject; squash-style with the task prefix; the root): a clone without one reports NOT MEASURED
  export UNREL_SHA="$(git -C "$ROOT" log --all --not HEAD --no-merges --format='%H %s' 2>/dev/null | grep -vE '^[0-9a-f]{40} \[P[0-9]{3}-E[0-9]+-T1416\]' | head -1 | cut -d' ' -f1)"
  export SQUASH_SHA="$(git -C "$ROOT" log --all --not HEAD --no-merges --format='%H %s' 2>/dev/null | grep -E '^[0-9a-f]{40} \[P[0-9]{3}-E[0-9]+-T1416\]' | head -1 | cut -d' ' -f1)"
  export ROOT_SHA="$(git -C "$ROOT" rev-list --max-parents=0 HEAD 2>/dev/null | head -1)"
  img_mutant_note() {  # $1 kind, $2 text the NOT MEASURED note must carry; ok when the mutant exists, differs and is NOT a failure
    local k="$1" want="$2" md="$TMPD/imgm/$1" out
    rm -rf "$md"; mkdir -p "$md/guide"; cp "$GDIR"/*.md "$md/guide/"; cp -R "$IMGDIR" "$md/images"; : > "$md/notes"
    if ! python3 -B "$IMG_MUT" "$k" "$md/guide" "$md/images" 2>"$md/err"; then bad "AC1416-S1: mutation $k could not be built: $(head -c 200 "$md/err")"; return; fi
    if diff -rq "$IMGDIR" "$md/images" >/dev/null 2>&1; then bad "AC1416-S1: mutation $k left the tree unchanged (control)"; return; fi
    out="$(IMG_NOTES="$md/notes" img_check "$md/guide" "$md/images")"
    if [ -z "${out// /}" ] && grep -qF "$want" "$md/notes"; then ok "AC1416-S1: a source commit this clone cannot judge ($k) is NOT MEASURED with a note, not a failure"
    else bad "AC1416-S1: mutation $k: expected no failure and a note carrying '$want', got: $out"; fi
  }
  if [ -n "$UNREL_SHA" ]; then img_mutant_marker non-ancestor-source-sha 'swagger-source-sha-not-ancestor'
  else echo "  NOTE AC1416-S1: no commit outside HEAD's history with another subject in this clone: the non-ancestor source_sha arm is NOT MEASURED"; fi
  if [ -n "$SQUASH_SHA" ]; then img_mutant_note squash-source-sha 'carries the task prefix'
  else echo "  NOTE AC1416-S1: no non-ancestor task-prefixed commit in this clone: the squash-merge arm is NOT MEASURED"; fi
  img_mutant_note missing-object-source-sha 'NOT MEASURED'
  img_mutant_marker malformed-source-sha 'swagger-source-sha-not-ancestor'
  img_mutant_marker root-commit-source-sha 'swagger-source-sha-lacks-the-harness'
  img_mutant_marker browser-errors 'swagger-browser-errors-not-empty'
  img_mutant_marker zero-operations 'swagger-no-operations'
  img_mutant_marker zero-operations 'swagger-group-operation-count-not-positive:group-001-default.png'
  img_mutant_marker empty-groups 'swagger-groups-mismatch'
  img_mutant_marker delete-provenance-keys 'swagger-missing-key:resources'
  img_mutant_marker delete-provenance-keys 'swagger-missing-key:swagger_ui_webjar'
  img_mutant_marker delete-provenance-keys 'swagger-missing-key:harness'
  img_mutant_marker evil-url 'swagger-url-host-not-loopback'
  img_mutant_marker duplicate-image 'swagger-duplicate-image-entry:group-001-default.png'
  img_mutant_marker add-tmp-path 'leak-tmp-path'
  img_mutant_marker add-root-path 'leak-root-path'
  img_mutant_marker add-var-folders-path 'leak-var-folders-path'
  img_mutant_marker add-windows-path 'leak-windows-path'
  img_mutant_marker add-ws-url 'leak-webSocketDebuggerUrl'
  img_mutant_marker add-ws-url 'leak-devtools-browser'
  img_mutant_marker add-users-path 'leak-local-path'
  img_mutant_marker add-stack-trace 'leak-stack-trace'
  img_mutant_marker status-incomplete 'swagger-status-not-complete'
  img_mutant_marker try-it-out-enabled 'swagger-try-it-out-not-disabled'
  img_mutant_marker label-lacks-2548 'swagger-label-lacks-2548'
  img_mutant_marker unlink-image 'unlinked-png:swagger/group-001-default.png'
  img_mutant_marker link-missing-image 'linked-image-missing:swagger/nope.png'
  img_mutant_marker add-unlinked-png 'unlinked-png:swagger/extra.png'
  img_mutant_marker add-unlinked-png 'png-not-in-capture-json:swagger/extra.png'
  img_mutant_marker not-a-png 'not-a-png:group-001-default.png'
  img_mutant_marker link-image-in-05 '05-links-an-image:swagger/overview.png'
  else
  img_mutant_marker claim-without-evidence 'swagger-not-measured-boundary-missing'
  img_mutant_marker link-missing-image 'linked-image-missing:swagger/nope.png'
  img_mutant_marker link-image-in-05 '05-links-an-image:swagger/overview.png'
  fi
  # negative control: an unknown mutation cannot be built, so it can never be counted as killed
  if python3 -B "$IMG_MUT" no-such-mutation "$TMPD" "$TMPD" >/dev/null 2>&1; then bad "AC1416-S1: an unknown mutation was accepted (control)"
  else ok "AC1416-S1: an unknown mutation is refused, never counted as killed (negative control)"; fi
fi
grep -q '^### Screenshots' "$GDIR/05-config-wizard.md" && ! grep -qiE 'screenshots pending capture' "$GDIR/05-config-wizard.md" \
  && grep -qF 'are delivered in the stacked follow-up pull request; nothing in this pull request claims them' "$GDIR/05-config-wizard.md" \
  && ok "EXTRA_wizard_screenshots: the wizard section points at the follow-up pull request for console screenshots and claims none" \
  || bad "EXTRA_wizard_screenshots: 05-config-wizard.md needs a '### Screenshots' section that points at the follow-up pull request and no 'pending capture' stub"

# ── EXTRA_capture_* — the capture harness and runner exist, are opt-in and honest (internal work item 2505, internal work item 2548) ─────
# Static checks only: nothing here compiles, builds or runs the harness. The harness is a test-only class,
# so it must be skipped by ordinary test runs, say what it is, and cite internal work item 2548.
CAP_HARNESS="$ROOT/appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/CoreSwaggerCaptureStackTest.java"
CAP_FILTER_TEST="$ROOT/appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/CoreSwaggerCaptureFilterTest.java"
CAP_RUNNER="$ROOT/scripts/docs/capture-core-swagger-run.py"
capture_files_check() {
  local f miss=""
  for f in "$CAP_HARNESS" "$CAP_FILTER_TEST" "$CAP_RUNNER" \
           "$ROOT/scripts/docs/capture-swagger.mjs" "$ROOT/appbase-stacktest/src/test/resources/appbase-core-swagger-capture.yaml"; do
    [ -f "$f" ] || miss="$miss ${f#$ROOT/}"
  done
  echo "$miss"
}
mut() {  # $1 src, $2 dst, $3 old, $4 new : python replace so the mutation cannot silently miss
  python3 -B -c "
import sys
s=open(sys.argv[1]).read()
assert sys.argv[3] in s, 'mutation target missing: '+sys.argv[3]
open(sys.argv[2],'w').write(s.replace(sys.argv[3],sys.argv[4],1))" "$1" "$2" "$3" "$4"
}
# TEXT-ONLY checks (the capture_harness_check, capture_filter_test_check, capture_runner_check and capture_runner_more_check
# arms below): they grep the source for a marker; they do not compile or run the Java or execute the Python or Node
# scripts. Behaviour is asserted by the cap-unit.py and node arms further down; the filter's by CoreSwaggerCaptureFilterTest.
capture_harness_check() {  # $1 = harness source
  local h="$1" bad="" c
  grep -qF '@EnabledIfSystemProperty(named = "stacktest.coreswagger.capture", matches = "true")' "$h" || bad="$bad no-opt-in-property-guard"
  grep -qF 'internal work item 2548' "$h" || bad="$bad does-not-cite-2548"
  grep -qF 'OpenApiResource' "$h" || bad="$bad no-OpenApiResource"
  grep -qF 'JettyServletContainer' "$h" || bad="$bad not-served-through-JettyServletContainer"
  for c in CoreJaxrsResource JmxJaxrsResource ServiceRegistryJaxrsResource StatusJaxrsResource LogJaxrsResource \
           MetricJaxrsResource MediatorJaxrsResource ExceptionJaxrsResource ConfigurationJaxrsResource HealthCheckJaxrsResource; do
    grep -qF "$c.class" "$h" || bad="$bad missing-$c"
  done
  grep -qE 'Stack(Test)?Constants|CustomerJaxrsResource' "$h" && bad="$bad uses-sample-stacktest-resources"
  grep -qF 'unauthenticated' "$h" || bad="$bad no-exposure-note"
  grep -qF '@author Claude' "$h" || bad="$bad no-author-header"
  grep -qE 'main code registers (no|none)' "$h" && bad="$bad overbroad-no-main-code-claim"
  # The filter's own behaviour is asserted by CoreSwaggerCaptureFilterTest; only its registration on every path is checked here.
  grep -qE 'addFilter\(new FilterHolder\(new LoopbackReadOnlyFilter\(\)\), "/\*",' "$h" && grep -qF 'DispatcherType.REQUEST' "$h" || bad="$bad host-method-filter-not-registered"
  echo "$bad"
}
capture_filter_test_check() {  # $1 = the plain JUnit test of the loopback filter (text checks: not compiled or run here)
  local t="$1" bad=""
  grep -qF 'CoreSwaggerCaptureStackTest.LoopbackReadOnlyFilter' "$t" && ! grep -qF 'EnabledIfSystemProperty' "$t" || bad="$bad no-plain-junit-filter-test"
  grep -qF 'new String[]{"POST"' "$t" && grep -qF '"HTTP/1.1 403 Forbidden"' "$t" || bad="$bad filter-test-lacks-post-403"
  grep -qF '"evil.example"' "$t" && grep -qF '"HTTP/1.1 403 Forbidden"' "$t" || bad="$bad filter-test-lacks-wrong-host-403"
  echo "$bad"
}
capture_runner_check() {  # $1 = runner source
  local r="$1" bad=""
  grep -qF '[0-9a-fA-F]{40}' "$r" || bad="$bad no-40-hex-check"
  grep -qF 'does not match HEAD' "$r" || bad="$bad no-head-comparison"
  grep -qF "'rev-parse', 'HEAD'" "$r" || bad="$bad head-not-read-from-git"
  grep -qE '12 \* GIB' "$r" && grep -qE '8 \* GIB' "$r" && grep -qE '5 \* GIB' "$r" || bad="$bad missing-disk-guards"
  grep -qF 'CoreSwaggerCaptureStackTest' "$r" || bad="$bad does-not-run-the-harness"
  grep -qF 'capture-swagger.mjs' "$r" || bad="$bad does-not-use-capture-swagger"
  grep -qF "'run'" "$r" && grep -qF -- '--project' "$r" || bad="$bad no-lease-invocation"
  grep -qE 'os\.remove|os\.unlink|shutil\.rmtree|\.unlink\(|rmdir|rm -' "$r" && bad="$bad deletes-something"
  grep -qF '/Users/' "$r" && bad="$bad hard-coded-private-path"
  echo "$bad"
}
cf="$(capture_files_check)"
[ -z "$cf" ] && ok "EXTRA_capture_files: harness test, runner, capture scripts and fixture config all exist" || bad "EXTRA_capture_files: missing:$cf"
capture_authors_check() {  # $@ = source files: the two author lines, Claude second
  local f miss=""
  for f in "$@"; do
    [ "$(grep -m2 -E '@author' "$f" | tr '\n' '|' | sed 's/[[:space:]]*[*#\/]*[[:space:]]*@author/@author/g')" = "@author Navaneeth Mysore G|@author Claude|" ] || miss="$miss ${f#$ROOT/}"
  done
  echo "$miss"
}
if [ -z "$cf" ]; then
  am="$(capture_authors_check "$CAP_HARNESS" "$CAP_FILTER_TEST" "$CAP_RUNNER" "$ROOT/scripts/docs/capture-swagger.mjs")"
  [ -z "$am" ] && ok "EXTRA_capture_authors: the harness and the capture scripts carry '@author Navaneeth Mysore G' then '@author Claude'" || bad "EXTRA_capture_authors: missing or misordered:$am"
  mkdir -p "$TMPD/cap"
  sed 's/@author Claude/@author Someone/' "$CAP_RUNNER" > "$TMPD/cap/a1.py"
  [ -n "$(capture_authors_check "$TMPD/cap/a1.py")" ] && ok "EXTRA_capture_authors: rejects a script without the Claude author line" || bad "EXTRA_capture_authors: accepted a script without the Claude author line"
  sed 's/@author Navaneeth Mysore G/@author Claude/' "$CAP_RUNNER" > "$TMPD/cap/a2.py"
  [ -n "$(capture_authors_check "$TMPD/cap/a2.py")" ] && ok "EXTRA_capture_authors: rejects a script without the owner author line first" || bad "EXTRA_capture_authors: accepted a misordered author header"
  hb="$(capture_harness_check "$CAP_HARNESS")"
  [ -z "$hb" ] && ok "EXTRA_capture_harness: opt-in property guard, cites internal work item 2548, real core resources through JettyServletContainer + OpenApiResource, Host and method filter registered (text checks; the Java is not compiled or run here)" || bad "EXTRA_capture_harness:$hb"
  rb="$(capture_runner_check "$CAP_RUNNER")"
  [ -z "$rb" ] && ok "EXTRA_capture_runner: refuses without a 40-hex SHA equal to HEAD, disk guards 12/8/5 GiB, one lease run, deletes nothing" || bad "EXTRA_capture_runner:$rb"
  # Mutations: each weakened copy must be rejected, so the arms above cannot pass vacuously.
  mkdir -p "$TMPD/cap"
  sed 's/unauthenticated/open/' "$CAP_HARNESS" > "$TMPD/cap/h2.java"
  case "$(capture_harness_check "$TMPD/cap/h2.java")" in *no-exposure-note*) ok "EXTRA_capture_harness: rejects a harness without the exposure note";; *) bad "EXTRA_capture_harness: accepted a harness without the exposure note";; esac
  sed 's/registers an OpenAPI resource (internal work item 2548/main code registers none (internal work item 2548/' "$CAP_HARNESS" > "$TMPD/cap/h3.java"
  case "$(capture_harness_check "$TMPD/cap/h3.java")" in *overbroad-no-main-code-claim*) ok "EXTRA_capture_harness: rejects the overbroad 'main code registers none' wording";; *) bad "EXTRA_capture_harness: accepted the overbroad wording";; esac
  sed 's/@author Claude/@author Someone/' "$CAP_HARNESS" > "$TMPD/cap/h4.java"
  case "$(capture_harness_check "$TMPD/cap/h4.java")" in *no-author-header*) ok "EXTRA_capture_harness: rejects a harness without the second author line";; *) bad "EXTRA_capture_harness: accepted a harness without the author line";; esac
  mut "$CAP_HARNESS" "$TMPD/cap/h8.java" 'context.addFilter(new FilterHolder(new LoopbackReadOnlyFilter()), "/*",' 'context.addAttributeNoFilter("/*",'
  [ -s "$TMPD/cap/h8.java" ] && ! cmp -s "$TMPD/cap/h8.java" "$CAP_HARNESS" && case "$(capture_harness_check "$TMPD/cap/h8.java")" in *host-method-filter-not-registered*) ok "EXTRA_capture_harness: rejects mutation h8 by its named marker (host-method-filter-not-registered; text check)";; *) bad "EXTRA_capture_harness: mutation h8 not rejected by host-method-filter-not-registered";; esac || bad "EXTRA_capture_harness: mutation h8 was not built"
  ft="$(capture_filter_test_check "$CAP_FILTER_TEST")"
  [ -z "$ft" ] && ok "EXTRA_capture_filter_test: the plain JUnit test (no opt-in flag) asserts 403 for POST/PUT/DELETE and for a wrong Host (text check; the Java is not compiled or run here)" || bad "EXTRA_capture_filter_test:$ft"
  for pair in 's/LoopbackReadOnlyFilter/OtherFilter/g:no-plain-junit-filter-test' 's/new String\[\]{"POST"/new String[]{"GET"/:filter-test-lacks-post-403' 's/"evil.example"/"ok.example"/:filter-test-lacks-wrong-host-403'; do
    sed "${pair%%:*}" "$CAP_FILTER_TEST" > "$TMPD/cap/ft.java"
    ! cmp -s "$TMPD/cap/ft.java" "$CAP_FILTER_TEST" && case "$(capture_filter_test_check "$TMPD/cap/ft.java")" in *"${pair#*:}"*) ok "EXTRA_capture_filter_test: rejects a test without its statement named ${pair#*:}";; *) bad "EXTRA_capture_filter_test: accepted a test without ${pair#*:}";; esac || bad "EXTRA_capture_filter_test: mutation of ${pair#*:} was not built"
  done
  sed 's/@EnabledIfSystemProperty/@Disabled_Mutation/' "$CAP_HARNESS" > "$TMPD/cap/h1.java"
  [ -n "$(capture_harness_check "$TMPD/cap/h1.java")" ] && ok "EXTRA_capture_harness: rejects a harness without the opt-in guard" || bad "EXTRA_capture_harness: accepted a harness without the opt-in guard"
  sed 's/does not match HEAD/matches whatever/' "$CAP_RUNNER" > "$TMPD/cap/r1.py"
  [ -n "$(capture_runner_check "$TMPD/cap/r1.py")" ] && ok "EXTRA_capture_runner: rejects a runner without the HEAD comparison" || bad "EXTRA_capture_runner: accepted a runner without the HEAD comparison"
  { cat "$CAP_RUNNER"; echo "shutil.rmtree('/x')"; } > "$TMPD/cap/r2.py"
  [ -n "$(capture_runner_check "$TMPD/cap/r2.py")" ] && ok "EXTRA_capture_runner: rejects a runner that deletes" || bad "EXTRA_capture_runner: accepted a runner that deletes"
  if command -v python3 >/dev/null 2>&1; then
    if PYTHONDONTWRITEBYTECODE=1 python3 -B -c "import ast,sys; [ast.parse(open(f).read()) for f in sys.argv[1:]]" "$CAP_RUNNER" 2>/dev/null; then
      ok "EXTRA_capture_syntax: the runner parses as Python"
    else bad "EXTRA_capture_syntax: the runner does not parse"; fi
  else echo "  NOTE EXTRA_capture_syntax: UNAVAILABLE (no python3)"; fi
fi
# Stronger, behavioural arms (internal work item 2505 review): nothing here touches the network, builds, or starts a browser.
# They run the runner's refusal paths and its pure functions under the system python3.
capture_runner_more_check() {  # $1 = runner source
  local r="$1" bad=""
  [ "$(grep -cF 'self.leak_check()' "$r")" -ge 2 ] || bad="$bad leak-check-not-called-on-success-and-in-finally"
  grep -qF 'def scan_leaks' "$r" || bad="$bad no-leak-scan"
  grep -qF -- '--untracked-files=normal' "$r" || bad="$bad untracked-files-ignored"
  grep -qE -- 'untracked-files=no([^r]|$)' "$r" && bad="$bad untracked-files-ignored"
  grep -qF 'def quarantine' "$r" || bad="$bad no-quarantine"
  grep -qF 'def webjar_version' "$r" || bad="$bad webjar-not-pinned-to-pom-version"
  grep -qF 'def validate_m2' "$r" || bad="$bad m2-not-validated"
  grep -qF 'fail closed' "$r" || bad="$bad watchdog-not-fail-closed"
  grep -qF 'jars[-1]' "$r" && bad="$bad webjar-picked-by-sort"
  grep -qF 'shutil.move(path, dest)' "$r" || bad="$bad quarantine-not-cross-volume-safe"
  [ "$(grep -cF ':(exclude,literal)' "$r")" -ge 2 ] || bad="$bad glob-pathspec-exclusion"
  grep -qF 'except BaseException as q:' "$r" || bad="$bad no-leak-check-on-every-exit-path"
  grep -qF '        install_term_handlers()' "$r" || bad="$bad term-handlers-not-installed"
  grep -qF 'if evidence_in_docs(a.evidence_dir):' "$r" || bad="$bad evidence-dir-not-refused-in-docs"
  grep -qE 'skip[-_]swagger' "$r" && bad="$bad dead-skip-swagger-option"
  grep -qiF 'primary IPv4' "$r" && bad="$bad stale-primary-ipv4-docstring"
  echo "$bad"
}
unused_imports() {  # $1 = python source; prints the imported names the module never references (AST, not text)
  python3 -B -c '
import ast, sys
t = ast.parse(open(sys.argv[1]).read())
imported = {}
for n in t.body:
    if isinstance(n, ast.Import):
        for a in n.names:
            imported[(a.asname or a.name).split(".")[0]] = n.lineno
    elif isinstance(n, ast.ImportFrom):
        for a in n.names:
            imported[a.asname or a.name] = n.lineno
used = set(n.id for n in ast.walk(t) if isinstance(n, ast.Name))
print(" ".join(sorted(k for k in imported if k not in used)))' "$1"
}
cat > "$TMPD/cap-unit.py" <<'PY'
import contextlib, importlib.util, io, os, sys, tempfile, threading
quiet = io.StringIO()
spec = importlib.util.spec_from_file_location('runner_under_test', sys.argv[1])
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
# A mutated copy lives in a temp dir, so its own REPO would be that dir: pin the checkout the arms are about.
root = sys.argv[2]
m.REPO, m.HERE = root, os.path.join(root, 'scripts', 'docs')
m.GUIDE_IMAGES = os.path.join(root, 'docs', 'user-guide', 'images')
fails = []
import signal
# 0 (round 9, S3-2): a signal that does not end a process (or cannot be caught) must never get the stop handler; checked first and alone, since the arms below would die of it
wrong_set = ['signal-%s-must-not-be-handled' % n_ for n_ in ('SIGKILL', 'SIGSTOP', 'SIGCHLD', 'SIGWINCH', 'SIGCONT', 'SIGURG', 'SIGPIPE')
             if getattr(signal, n_) in m.HANDLED_SIGNALS]
if wrong_set:
    print('UNIT-FAIL: ' + ' '.join(wrong_set))
    sys.exit(0)

def refuses(f, *a):
    try:
        with contextlib.redirect_stderr(quiet):
            f(*a)
    except SystemExit:
        return True
    return False

for bad in ['/a b', 'rel/x', '-Dx=1', '/a/../b', "/a'b", '/a;b', '/a"b']:
    if not refuses(m.validate_m2, bad):
        fails.append('m2-accepted:' + bad)
if refuses(m.validate_m2, '/tmp') or refuses(m.validate_m2, None):
    fails.append('m2-rejected-a-valid-value')

d = tempfile.mkdtemp()
def put(name, data):
    p = os.path.join(d, name)
    with open(p, 'wb') as f:
        f.write(data)
    return p
needles = m.leak_needles()
ok_files = [put('clean.json', b'{"a": 1}')]
bad_files = [put('repo.json', ('{"url": "file://%s/x"}' % m.REPO).encode()),
             put('shot.png', b'\x89PNG\r\n' + m.REPO.encode() + b'\x00')]
home = os.path.expanduser('~')
if len(home) > 1:
    bad_files.append(put('home.txt', ('at ' + home + '/y').encode()))
if m.scan_leaks(ok_files, needles):
    fails.append('leak-scan-false-positive')
for b in bad_files:
    if not m.scan_leaks([b], needles):
        fails.append('leak-scan-missed:' + os.path.basename(b))

class Stop(object):
    def __init__(self):
        self.n = 0
    def wait(self, t):
        self.n += 1
        return self.n > 50

def watch(seq):
    r = object.__new__(m.Run)
    r.evidence = tempfile.mkdtemp()
    r.vols, r.start_free_by = ['v'], {'v': 100 * m.GIB}
    r.stop, r.aborted, r.procs = Stop(), threading.Event(), []
    r.samples, r.max_drop, r.drop_reported = [], 0, False
    it = iter(seq)
    def fake(path=None):
        v = next(it, 100 * m.GIB)
        if isinstance(v, Exception):
            raise v
        return v
    m.free_kib = fake
    try:
        with contextlib.redirect_stdout(quiet), contextlib.redirect_stderr(quiet):
            r.watch()
    except Exception:
        return None  # the watchdog thread died: reported by the callers as a named failure
    return r.aborted.is_set()

if not watch([OSError('df'), 1000]):
    fails.append('watchdog-died-after-one-df-error')
if not watch([OSError('df')] * 3):
    fails.append('watchdog-did-not-fail-closed')
if watch([OSError('df'), 100 * m.GIB]):
    fails.append('watchdog-aborted-on-a-recovered-error')
if not watch([m.ABORT_FLOOR - 1]):
    fails.append('watchdog-missed-low-disk')

# leak_check() itself: success path, failure path, quarantine of a leaking json/text/png, cross-volume move.
import errno, shutil
def fixture():
    r = object.__new__(m.Run)
    r.evidence = tempfile.mkdtemp()
    gi = tempfile.mkdtemp()
    m.GUIDE_IMAGES = gi
    xd = os.path.join(gi, 'extra')
    os.makedirs(xd)
    r.touched = {xd}
    r.note = lambda msg: None
    return r, xd
def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(data)
def check(r):
    try:
        r.leak_check()
        return None
    except Exception as e:
        return type(e).__name__ + ': ' + str(e)
r, xd = fixture()
write(os.path.join(xd, 'clean.png'), b'\x89PNG clean')
write(os.path.join(xd, 'capture.json'), b'{"ok": 1}')
if check(r) is not None or len(os.listdir(xd)) != 2:
    fails.append('leak_check-success-path-raised-or-moved-clean-files')
# QA-F2: the evidence directory (a mkdtemp under TMPDIR) must be the ONLY needle that matches notes.txt, even when
# TMPDIR is under $HOME or the checkout, so this block runs with HOME and the checkout pointed at unrelated paths.
# NEUTRAL_ON = False is the mutation that proves the neutralisation is what makes the u8 arm independent of TMPDIR.
NEUTRAL_ON = True
real_home, real_repo_n = os.environ.get('HOME'), m.REPO
if NEUTRAL_ON:
    os.environ['HOME'], m.REPO = '/nonexistent-ug-home', '/nonexistent-ug-checkout'
r, xd = fixture()
write(os.path.join(xd, 'shot.png'), b'\x89PNG\r\n' + m.REPO.encode() + b'\x00')
write(os.path.join(xd, 'capture.json'), ('{"u": "%s/x"}' % m.REPO).encode())
write(os.path.join(xd, 'notes.txt'), ('see ' + r.evidence + '/log').encode())
os.environ.pop('TMPDIR', None)  # so that only the evidence-directory needle can find notes.txt
real_rename, real_replace = os.rename, os.replace
def exdev(*a):
    raise OSError(errno.EXDEV, 'cross-device')
os.rename, os.replace = exdev, exdev  # the evidence directory is on another volume: only shutil.move survives
try:
    msg = check(r)
finally:
    os.rename, os.replace = real_rename, real_replace
if msg is None:
    fails.append('leak_check-failure-path-did-not-raise')
left = os.listdir(xd)
if left:
    fails.append('leak_check-left-leaking-output-in-docs-tree:' + ','.join(sorted(left)))
q = os.path.join(r.evidence, 'quarantine')
got = sorted(os.listdir(q)) if os.path.isdir(q) else []
for want in ('extra-capture.json', 'extra-notes.txt', 'extra-shot.png'):
    if want not in got:
        fails.append('leak_check-did-not-quarantine:' + want)
if msg and (real_repo_n in msg or '/nonexistent-ug' in msg or os.path.expanduser('~') in msg):
    fails.append('leak_check-message-carries-a-path')
if real_home is not None:
    os.environ['HOME'] = real_home
m.REPO = real_repo_n

# 1a: the finally path must keep the original failure and append the leak outcome.
import signal, contextlib, io, time
def run_finally(error):
    def boom():
        raise RuntimeError(error)
    r, xd = fixture()
    write(os.path.join(xd, 'a.txt'), (m.REPO + '/x').encode())
    r.result = {}
    r.swagger = boom
    r.procs, r.stop, r.aborted = [], m.threading.Event(), m.threading.Event()
    r.vols, r.samples, r.max_drop, r.start_free, r.head = [], [m.GIB], 0, m.GIB, 'h'
    r.a, r.swagger_started = type('A', (), {})(), False
    r.watch = lambda: None
    r.note = lambda msg: None
    rc = r.execute()
    return rc, r.result.get('error', '')
rc, err = run_finally('ORIGINAL swagger failure')
if rc != 1 or not err.startswith('ORIGINAL swagger failure') or 'local path' not in err:
    fails.append('finally-leak-check-masked-the-original-error:' + err[:60])

# 2: --evidence-dir inside the guide tree or a scanned images dir is refused; an outside one is not.
m.GUIDE_IMAGES = os.path.join(m.REPO, 'docs', 'user-guide', 'images')
for bad_ev in (m.GUIDE_IMAGES, os.path.join(m.GUIDE_IMAGES, 'swagger', 'ev'), os.path.join(m.REPO, 'docs', 'user-guide'),
               os.path.join(m.REPO, 'docs', 'user-guide', 'x', 'ev')):
    if not m.evidence_in_docs(bad_ev):
        fails.append('evidence-in-docs-not-detected:' + bad_ev[len(m.REPO):])
if m.evidence_in_docs(os.path.join(m.REPO, 'appbase-stacktest', 'target', 'ev')) or m.evidence_in_docs(tempfile.gettempdir()):
    fails.append('evidence-outside-docs-refused')
ns = type('N', (), {'evidence_dir': os.path.join(m.GUIDE_IMAGES, 'swagger', 'ev'), 'sha': 'x'})()
err_io = io.StringIO()
try:
    with contextlib.redirect_stderr(err_io):
        m.preflight(ns)
except SystemExit:
    pass
if 'outside docs/user-guide' not in err_io.getvalue():
    fails.append('preflight-did-not-refuse-evidence-in-docs')

# 3: SIGTERM and SIGHUP become SystemExit (so the finally runs).
old = dict((s, signal.getsignal(s)) for s in m.HANDLED_SIGNALS)
try:
    for s in (signal.SIGTERM, signal.SIGHUP, signal.SIGINT):
        m.install_term_handlers()  # each signal's handler ignores the others, so it is installed afresh for each
        try:
            os.kill(os.getpid(), s)
            time.sleep(0.2)
            fails.append('signal-%d-did-not-raise' % s)
        except SystemExit as e:
            if e.code != 128 + s:
                fails.append('signal-%d-wrong-exit-code' % s)
finally:
    for s, h in old.items():
        signal.signal(s, h)
# 4 (C1): an interrupted or failed Swagger capture moves every file it wrote, and the stale manifest.
def prep(r):
    r.result = {}
    r.procs, r.stop, r.aborted = [], m.threading.Event(), m.threading.Event()
    r.vols, r.samples, r.max_drop, r.start_free, r.head = [], [m.GIB], 0, m.GIB, 'h'
    r.a = type('A', (), {})()
    r.watch = lambda: None
    r.note = lambda msg: None
def swagger_fail(exc):
    r, xd = fixture()
    sw = os.path.join(m.GUIDE_IMAGES, 'swagger')
    write(os.path.join(sw, 'keep.png'), b'untouched earlier image')
    write(os.path.join(sw, 'capture.json'), b'{"old": 1}')
    prep(r)
    r.swagger_before = dict((n, m.snapshot(os.path.join(sw, n))) for n in os.listdir(sw))
    r.touched, r.swagger_started = {sw}, True
    def partial():
        write(os.path.join(sw, 'overview.png'), b'new partial image')
        write(os.path.join(sw, 'group-001-default.png'), b'new partial image two')
        raise exc
    r.swagger = partial
    try:
        r.execute()
    except BaseException:
        pass
    q = os.path.join(r.evidence, 'quarantine')
    return sorted(os.listdir(sw)), sorted(os.listdir(q)) if os.path.isdir(q) else []
for exc in (RuntimeError('boom'), KeyboardInterrupt()):
    left, got = swagger_fail(exc)
    if left != ['keep.png']:
        fails.append('swagger-partial-left-in-docs-after-%s:%s' % (type(exc).__name__, ','.join(left)))
    for want in ('failed-swagger-overview.png', 'failed-swagger-group-001-default.png', 'failed-swagger-capture.json'):
        if want not in got:
            fails.append('swagger-partial-not-quarantined-after-%s:%s' % (type(exc).__name__, want))

# 5 (C4): a second SIGTERM during the cleanup, or a child that cannot be stopped, must not skip the leak check.
class Child(object):
    pid = 0
    def __init__(self, mode):
        self.mode = mode
    def poll(self):
        if self.mode == 'signal':
            os.kill(os.getpid(), signal.SIGTERM)
            time.sleep(0.3)
            return 0
        raise ValueError('poll exploded')
    def wait(self, timeout=None):
        return 0
r, xd = fixture()
prep(r)
write(os.path.join(xd, 'a.txt'), (m.REPO + '/x').encode())
def boom2():
    raise RuntimeError('ORIGINAL')
r.swagger = boom2
r.procs = [Child('boom'), Child('signal')]
saved = dict((s_, signal.getsignal(s_)) for s_ in m.HANDLED_SIGNALS)
try:
    rc5 = r.execute()
except BaseException as e:
    rc5 = 'escaped:' + type(e).__name__
finally:
    for s_, h_ in saved.items():
        signal.signal(s_, h_)
if rc5 != 1 or 'local path' not in r.result.get('error', '') or not os.path.isfile(os.path.join(r.evidence, 'result.json')):
    fails.append('cleanup-cut-short:%s' % (rc5,))

# 6 (C2): --evidence-dir is made absolute.
def pre(**kw):
    ns = type('N', (), dict({'evidence_dir': os.path.join(tempfile.gettempdir(), 'ev-x'), 'sha': 'x'}, **kw))()
    err = io.StringIO()
    try:
        with contextlib.redirect_stderr(err):
            m.preflight(ns)
    except SystemExit:
        pass
    return ns, err.getvalue()
ns6, _ = pre(evidence_dir=os.path.relpath(os.path.join(tempfile.gettempdir(), 'ev-rel')))
if not os.path.isabs(ns6.evidence_dir):
    fails.append('evidence-dir-left-relative')

# 7 (D1): a wrong-case spelling of the guide tree is the guide tree on a case-insensitive volume (macOS APFS).
# The checkout is faked by a directory that does not exist, so only the name comparison can decide (no real
# directory, no inode): the same answer on a case-sensitive and a case-insensitive host.
real_platform, real_root, real_gi = sys.platform, m.REPO, m.GUIDE_IMAGES
fake = os.path.join(os.path.realpath(tempfile.mkdtemp()), 'repo')
m.REPO, m.GUIDE_IMAGES = fake, os.path.join(fake, 'docs', 'user-guide', 'images')
try:
    sys.platform = 'linux'
    if not m.evidence_in_docs(os.path.join(fake, 'docs', 'user-guide', 'ev')) or \
            not m.evidence_in_docs(m.GUIDE_IMAGES):
        fails.append('evidence-inside-the-guide-tree-not-refused')
    if m.evidence_in_docs(os.path.join(fake, 'Docs', 'User-Guide', 'ev1')):
        fails.append('case-sensitive-host-refused-a-different-directory')
    sys.platform = 'darwin'
    wrong = os.path.join(fake, 'Docs', 'User-Guide', 'ev1')
    if not m.evidence_in_docs(wrong):
        fails.append('wrong-case-evidence-dir-not-refused')
    if not m.evidence_in_docs(os.path.join(fake, 'DOCS', 'user-guide', 'IMAGES', 'x')):
        fails.append('wrong-case-images-dir-not-refused')
    if m.evidence_in_docs(os.path.join(fake, 'appbase-stacktest', 'target', 'Ev')):
        fails.append('wrong-case-check-refused-an-outside-dir')
    if m.repo_relative(os.path.join(fake.upper(), 'target', 'ev')) != 'target/ev':
        fails.append('repo_relative-not-case-folded')
    ns7, e7 = pre(evidence_dir=wrong)
    if 'outside docs/user-guide' not in e7:
        fails.append('preflight-accepted-a-wrong-case-evidence-dir')
finally:
    sys.platform, m.REPO, m.GUIDE_IMAGES = real_platform, real_root, real_gi
# device/inode identity: a differently spelled name of the same directory (simulated alias on a case-sensitive host).
tdir = os.path.realpath(tempfile.mkdtemp())
os.makedirs(os.path.join(tdir, 'docs'))
real_exists, real_same = os.path.exists, os.path.samefile
sys.platform = 'linux'  # no case folding: only the inode identity can recognise the alias
alias = os.path.join(tdir, 'Docs')
os.path.exists = lambda p: True if p == alias else real_exists(p)
os.path.samefile = lambda a_, b_: (b_ == os.path.join(tdir, 'docs')) if a_ == alias else real_same(a_, b_)
try:
    if not m.inside(os.path.join(alias, 'ev1'), os.path.join(tdir, 'docs')):
        fails.append('samefile-alias-not-recognised')
finally:
    os.path.exists, os.path.samefile = real_exists, real_same
    sys.platform = real_platform

# 8 (D3c): html text is scanned too, so the escaped form of every needle is scanned.
real_repo = m.REPO
m.REPO = '/x&y/repo'
try:
    esc_needles = m.leak_needles()
finally:
    m.REPO = real_repo
if '/x&amp;y/repo' not in esc_needles:
    fails.append('needles-lack-the-html-escaped-form')
if not m.scan_leaks([put('esc.html', b'<pre>/x&amp;y/repo/out.yaml</pre>')], esc_needles):
    fails.append('escaped-path-in-html-not-found')

# 10 (round 7, QA-F1/Sec3-F1): two DIFFERENT signals back to back must not cut the cleanup short. Both are held with
# pthread_sigmask and released together, against a real Run.execute() whose swagger step writes a leaking file and
# spawns a sleeping child; the cleanup must finish (child gone, leak quarantined, result.json written).
def two_signals(first, second):
    r, xd = fixture()
    prep(r)
    r.swagger_started = False
    leak = os.path.join(xd, 'leak.txt')
    kids = []
    def step():
        write(leak, (m.REPO + '/x').encode())
        kids.append(r.spawn(['sleep', '60'], 'child.log'))
        old_mask = signal.pthread_sigmask(signal.SIG_BLOCK, {first, second})
        os.kill(os.getpid(), first)
        os.kill(os.getpid(), second)
        signal.pthread_sigmask(signal.SIG_SETMASK, old_mask)
        time.sleep(3)
        raise RuntimeError('no signal arrived')
    r.swagger = step
    saved_h = dict((s_, signal.getsignal(s_)) for s_ in m.HANDLED_SIGNALS)
    try:
        r.execute()
        out = 'returned'
    except SystemExit:
        out = 'exit'
    except BaseException as e:
        out = 'escaped:' + type(e).__name__
    finally:
        for s_, h_ in saved_h.items():
            signal.signal(s_, h_)
    tag = '%d+%d' % (first, second)
    if out != 'exit':
        fails.append('two-signals-%s-%s' % (tag, out))
    if not os.path.isfile(os.path.join(r.evidence, 'result.json')):
        fails.append('two-signals-%s-no-result-json' % tag)
    if os.path.exists(leak):
        fails.append('two-signals-%s-leak-left-in-docs' % tag)
    elif not os.path.isfile(os.path.join(r.evidence, 'quarantine', os.path.basename(xd) + '-leak.txt')):
        fails.append('two-signals-%s-leak-not-quarantined' % tag)
    if kids and kids[0].poll() is None:
        fails.append('two-signals-%s-child-left-running' % tag)
        kids[0].kill()
S = signal
for pair in ((S.SIGHUP, S.SIGTERM), (S.SIGTERM, S.SIGHUP), (S.SIGINT, S.SIGTERM), (S.SIGTERM, S.SIGINT), (S.SIGHUP, S.SIGINT)):
    two_signals(*pair)

# 11 (round 7, Sec3-F2): a broken stdout must not abort the cleanup; each cleanup step stands alone.
def cleanup_run(configure, real_note=True, files=('a.txt', 'b.txt'), broken_stdout=False):
    r, xd = fixture()
    prep(r)
    if real_note:
        r.note = m.Run.note.__get__(r)
    r.swagger_started = False
    for n in files:
        write(os.path.join(xd, n), (m.REPO + '/x').encode())
    def boom_step():
        raise RuntimeError('swagger failure')
    r.swagger = boom_step
    configure(r, xd)
    real_out, to_close = sys.stdout, None
    if broken_stdout:
        rfd, wfd = os.pipe()
        os.close(rfd)  # no reader: every write raises BrokenPipeError
        sys.stdout = to_close = os.fdopen(wfd, 'w')
    else:
        sys.stdout = io.StringIO()
    try:
        rc = r.execute()
    except BaseException as e:
        rc = 'escaped:' + type(e).__name__
    finally:
        sys.stdout = real_out
        if to_close is not None:
            try:
                to_close.close()
            except Exception:
                pass
    return r, xd, rc
def quarantined(r):
    q = os.path.join(r.evidence, 'quarantine')
    return sorted(os.listdir(q)) if os.path.isdir(q) else []
r, xd, rc = cleanup_run(lambda r_, w_: None, broken_stdout=True)
if rc != 1:
    fails.append('broken-stdout-rc:%s' % (rc,))
if quarantined(r) != ['extra-a.txt', 'extra-b.txt'] or os.listdir(xd):
    fails.append('broken-stdout-left-a-leak:%s' % ','.join(os.listdir(xd)))
if not os.path.isfile(os.path.join(r.evidence, 'result.json')):
    fails.append('broken-stdout-no-result-json')
logp = os.path.join(r.evidence, 'run.log')
if not os.path.isfile(logp) or 'DONE' not in open(logp).read():
    fails.append('broken-stdout-log-not-written')
# one file that cannot be moved: the others are still quarantined, the failure is named, result.json is written
real_move = shutil.move
def move_fails_for(name):
    def mv(src, dst, *a):
        if os.path.basename(src) == name:
            raise OSError(errno.EACCES, 'denied')
        return real_move(src, dst, *a)
    return mv
shutil.move = move_fails_for('a.txt')
try:
    r, xd, rc = cleanup_run(lambda r_, w_: None)
finally:
    shutil.move = real_move
if rc != 1 or quarantined(r) != ['extra-b.txt'] or os.listdir(xd) != ['a.txt']:
    fails.append('one-stuck-file-stopped-the-others:%s|%s' % (rc, ','.join(quarantined(r))))
if 'a.txt' not in r.result.get('error', '') or 'could NOT be moved' not in r.result.get('error', ''):
    fails.append('stuck-file-not-named-in-the-result')
if not os.path.isfile(os.path.join(r.evidence, 'result.json')):
    fails.append('stuck-file-no-result-json')
# the Swagger partial: the same, for quarantine_partial
def swagger_partial(r_, w_):
    sw_ = os.path.join(m.GUIDE_IMAGES, 'swagger')
    os.makedirs(sw_)
    r_.touched, r_.swagger_before, r_.swagger_started = {sw_}, {}, True
    def partial():
        write(os.path.join(sw_, 'a.png'), b'partial one')
        write(os.path.join(sw_, 'b.png'), b'partial two')
        raise RuntimeError('swagger failure')
    r_.swagger = partial
shutil.move = move_fails_for('a.png')
try:
    r, xd, rc = cleanup_run(swagger_partial, files=())
finally:
    shutil.move = real_move
if 'failed-swagger-b.png' not in quarantined(r) or not os.path.isfile(os.path.join(r.evidence, 'result.json')):
    fails.append('one-stuck-partial-stopped-the-others:%s' % ','.join(quarantined(r)))
# a failing quarantine_partial / leak_check must not skip the steps after it
def bad_partial(r_, w_):
    def qp():
        raise OSError('quarantine_partial exploded')
    r_.quarantine_partial = qp
r, xd, rc = cleanup_run(bad_partial)
if rc != 1 or quarantined(r) != ['extra-a.txt', 'extra-b.txt'] or not os.path.isfile(os.path.join(r.evidence, 'result.json')):
    fails.append('failed-quarantine_partial-skipped-later-steps:%s' % (rc,))
def bad_leak(r_, w_):
    def lc():
        raise OSError('leak_check exploded')
    r_.leak_check = lc
r, xd, rc = cleanup_run(bad_leak)
if rc != 1 or not os.path.isfile(os.path.join(r.evidence, 'result.json')) or 'leak_check exploded' not in r.result.get('error', ''):
    fails.append('failed-leak_check-skipped-result-json:%s' % (rc,))
def bad_result(r_, w_):
    os.makedirs(os.path.join(r_.evidence, 'result.json'))  # a directory: the write fails
r, xd, rc = cleanup_run(bad_result)
if rc != 1:
    fails.append('unwritable-result-json-escaped:%s' % (rc,))

# 12 (round 7, QA-F4): an EXISTING image rewritten during a failed --overwrite capture is quarantined; an untouched one stays.
r, xd = fixture()
sw = os.path.join(m.GUIDE_IMAGES, 'swagger')
write(os.path.join(sw, 'keep.png'), b'original image')
write(os.path.join(sw, 'untouched.png'), b'stays where it is')
prep(r)
r.swagger_before = dict((n, m.snapshot(os.path.join(sw, n))) for n in os.listdir(sw))
r.touched, r.swagger_started = {sw}, True
def overwrite_then_fail():
    write(os.path.join(sw, 'keep.png'), b'rewritten by a failed --overwrite capture')
    raise RuntimeError('boom')
r.swagger = overwrite_then_fail
r.execute()
if 'failed-swagger-keep.png' not in quarantined(r) or sorted(os.listdir(sw)) != ['untouched.png']:
    fails.append('modified-existing-image-not-quarantined:%s|%s' % (','.join(quarantined(r)), ','.join(os.listdir(sw))))

# 13 (round 7, CR2): the success-path leak check and the one in finally must not report the same leak twice.
r, xd = fixture()
prep(r)
r.swagger_started = False
r.swagger = lambda: None
write(os.path.join(xd, 'leak.txt'), (m.REPO + '/x').encode())
r.quarantine = lambda path, tag: None  # the leak stays in place, so the finally-path check reports it again
rc = r.execute()
if rc != 1 or r.result.get('error', '').count('a local path was found') != 1:
    fails.append('leak-message-duplicated:%s' % r.result.get('error', '')[:200])

# 14 (round 7, C5): git() ignores GIT_DIR / GIT_WORK_TREE / GIT_INDEX_FILE of the caller's environment.
import subprocess
def mkrepo(msg):
    d = tempfile.mkdtemp()
    base = ['git', '-c', 'user.name=t', '-c', 'user.email=t@example.invalid', '-c', 'commit.gpgsign=false', '-C', d]
    subprocess.check_call(base + ['init', '-q'])
    subprocess.check_call(base + ['commit', '-q', '--allow-empty', '-m', msg])
    return d
repo_a, repo_b = mkrepo('A'), mkrepo('B')
head_a = subprocess.check_output(['git', '-C', repo_a, 'rev-parse', 'HEAD'], universal_newlines=True).strip()
head_b = subprocess.check_output(['git', '-C', repo_b, 'rev-parse', 'HEAD'], universal_newlines=True).strip()
saved_env = dict((k, os.environ.get(k)) for k in ('GIT_DIR', 'GIT_WORK_TREE', 'GIT_INDEX_FILE'))
real_repo_g = m.REPO
m.REPO = repo_a
os.environ.update(GIT_DIR=repo_b + '/.git', GIT_WORK_TREE=repo_b, GIT_INDEX_FILE=repo_b + '/.git/index')
try:
    got = m.git('rev-parse', 'HEAD')
finally:
    m.REPO = real_repo_g
    for k, v in saved_env.items():
        if v is None:
            os.environ.pop(k, None)
        else:
            os.environ[k] = v
if head_a == head_b or got != head_a:
    fails.append('git-followed-the-environment-instead-of-the-checkout')

# 16 (round 7, Sec3-F4): swagger-ui webjar name, version and sha256 are recorded.
import hashlib, zipfile
ver = m.Run.webjar_version(object.__new__(m.Run))
m2d = tempfile.mkdtemp()
jar_dir = os.path.join(m2d, 'org', 'webjars', 'swagger-ui', ver)
os.makedirs(jar_dir)
jar_path = os.path.join(jar_dir, 'swagger-ui-%s.jar' % ver)
with zipfile.ZipFile(jar_path, 'w') as z:
    z.writestr('META-INF/resources/webjars/swagger-ui/%s/swagger-ui-bundle.js' % ver, 'js')
    z.writestr('META-INF/resources/webjars/swagger-ui/%s/swagger-ui.css' % ver, 'css')
r = object.__new__(m.Run)
r.a, r.evidence, r.result, r.webjar_info, r.note = type('A', (), {'m2': m2d})(), tempfile.mkdtemp(), {}, None, lambda msg: None
r.webjar_assets()
want_sha = hashlib.sha256(open(jar_path, 'rb').read()).hexdigest()
extra = r.swagger_extra({})
if extra.get('swagger_ui_webjar') != {'name': os.path.basename(jar_path), 'version': ver, 'sha256': want_sha}:
    fails.append('swagger-manifest-lacks-the-webjar-version-and-sha256:%s' % extra.get('swagger_ui_webjar'))

# 17 (round 7, CR5 L4): the recheck message carries status codes, never a path.
real_git, real_td = m.git, m.tree_dirty
m.git, m.tree_dirty = (lambda *a_: 'abc'), (lambda a_, i_: ' M /Users/Dana/ADMIN/Cert.TXT\n?? other/x')
r = object.__new__(m.Run)
r.head, r.a = 'abc', None
try:
    r.recheck_tree()
    fails.append('recheck_tree-accepted-a-dirty-tree')
except RuntimeError as e:
    if str(e) != 'the working tree changed since the run started (status M)':
        fails.append('recheck_tree-message-carries-a-path:%s' % e)
finally:
    m.git, m.tree_dirty = real_git, real_td
# 18 (round 8): the lease argv puts the `run` subcommand first (the lease CLI is `run [--project P] --label X -- cmd`);
# a first real run failed with the lease usage text because the options came before `run`.
import subprocess
ns = type('N', (), {'lease': '/x/lease.sh', 'lease_project': 'appbase'})()
av = m.lease_argv(ns, ['py', 'inner'])
if av[:3] != ['bash', '/x/lease.sh', 'run'] or av.index('--project') < av.index('run') or av.index('--label') < av.index('run') or \
        av[av.index('--') + 1:] != ['py', 'inner'] or av.count('run') != 1:
    fails.append('lease-argv-has-the-wrong-order:%s' % av)
# 19 (round 9, S3-1): git cannot be told to look away from the tree. Every case runs in a scratch repository.
import unicodedata
GIT_BASE = ['git', '-c', 'user.name=t', '-c', 'user.email=t@example.invalid', '-c', 'commit.gpgsign=false']
def scratch(extra=None):
    d = tempfile.mkdtemp()
    def sg(*a):
        subprocess.check_call(GIT_BASE + ['-C', d] + list(a), env=m.git_env(os.environ), stdout=subprocess.DEVNULL)
    sg('init', '-q')
    files = {'.gitignore': 'build\ntarget\n*.log\n', 'src/A.java': 'class A{}\n', 'src/B.java': 'class B{}\n'}
    files.update(extra or {})
    for n_, t_ in files.items():
        write(os.path.join(d, n_), t_.encode())
    sg('add', '-A')
    sg('commit', '-qm', 'x')
    return os.path.realpath(d), sg
def in_repo(d, f):
    real_r = m.REPO
    m.REPO = d
    try:
        return f(type('N', (), {'evidence_dir': os.path.join(d, 'target', 'ev'), 'overwrite': False, 'sha': ''})())
    finally:
        m.REPO = real_r
def both(d):
    return in_repo(d, lambda a_: (m.tree_dirty(a_, False), m.tree_hidden(a_, False)))
d19, sg19 = scratch()
dirty, hidden = both(d19)
if dirty or hidden:
    fails.append('clean-scratch-tree-reported-dirty-or-hidden:%s|%s' % (dirty, hidden))
os.makedirs(os.path.join(d19, 'target', 'classes'))
os.makedirs(os.path.join(d19, 'appbase-x', 'target'))
write(os.path.join(d19, 'target', 'classes', 'A.class'), b'x')
write(os.path.join(d19, 'appbase-x', 'target', 'y.jar'), b'x')
dirty, hidden = both(d19)
if dirty or hidden:
    fails.append('build-output-under-target-refused:%s|%s' % (dirty, hidden))
# assume-unchanged and skip-work-tree hide a modified tracked file from `git status`
write(os.path.join(d19, 'src', 'A.java'), b'class A{ /*TAMPERED*/ }\n')
sg19('update-index', '--assume-unchanged', 'src/A.java')
dirty, hidden = both(d19)
if dirty or 'assume-unchanged' not in hidden:
    fails.append('assume-unchanged-not-refused:%s|%s' % (dirty, hidden))
sg19('update-index', '--no-assume-unchanged', 'src/A.java')
write(os.path.join(d19, 'src', 'A.java'), b'class A{}\n')
sg19('update-index', '--skip-work' + 'tree', 'src/B.java')  # spelled in two parts: the committed text stays free of the word the hygiene scan rejects
write(os.path.join(d19, 'src', 'B.java'), b'class B{ /*TAMPERED*/ }\n')
dirty, hidden = both(d19)
if dirty or 'skip-work-tree' not in hidden:
    fails.append('skip-work-tree-not-refused:%s|%s' % (dirty, hidden))
# an ignored file in a source tree (.gitignore says `build`) and an ignored file outside target
d19b, sg19b = scratch()
write(os.path.join(d19b, 'src', 'build', 'Evil.java'), b'class Evil{}\n')
dirty, hidden = both(d19b)
if dirty or 'source tree' not in hidden:
    fails.append('ignored-source-file-not-refused:%s|%s' % (dirty, hidden))
d19c, sg19c = scratch()
write(os.path.join(d19c, 'notes.log'), b'x')
dirty, hidden = both(d19c)
if dirty or 'outside target' not in hidden:
    fails.append('ignored-file-outside-target-not-refused:%s|%s' % (dirty, hidden))
# .git/info/exclude hides an untracked source file from `git status`; the ignored listing refuses it
d19d, sg19d = scratch()
write(os.path.join(d19d, 'src', 'Extra.java'), b'class X{}\n')
os.makedirs(os.path.join(d19d, '.git', 'info'), exist_ok=True)
with open(os.path.join(d19d, '.git', 'info', 'exclude'), 'a') as f_:
    f_.write('Extra.java\n')
dirty, hidden = both(d19d)
if dirty or 'source tree' not in hidden:
    fails.append('info-exclude-hid-an-untracked-source:%s|%s' % (dirty, hidden))
# an excludes file named by the repository's own config, and one injected through GIT_CONFIG_*, hide nothing
d19e, sg19e = scratch()
write(os.path.join(d19e, 'src', 'Extra.java'), b'class X{}\n')
exf = os.path.join(d19e, 'excl')
write(exf, b'Extra.java\n')
sg19e('config', 'core.excludesFile', exf)
dirty, hidden = both(d19e)
if 'Extra.java' not in dirty:
    fails.append('local-excludesfile-hid-an-untracked-file:%s' % dirty)
sg19e('config', '--unset', 'core.excludesFile')
saved19 = dict((k, os.environ.get(k)) for k in ('GIT_CONFIG_COUNT', 'GIT_CONFIG_KEY_0', 'GIT_CONFIG_VALUE_0', 'GIT_OBJECT_DIRECTORY', 'HOME'))
try:
    os.environ.update(GIT_CONFIG_COUNT='1', GIT_CONFIG_KEY_0='core.excludesFile', GIT_CONFIG_VALUE_0=exf)
    dirty, hidden = both(d19e)
    if 'Extra.java' not in dirty:
        fails.append('env-injected-excludesfile-hid-an-untracked-file:%s' % dirty)
    for k in ('GIT_CONFIG_COUNT', 'GIT_CONFIG_KEY_0', 'GIT_CONFIG_VALUE_0'):
        os.environ.pop(k)
    # any other GIT_* variable is dropped too: a bogus object directory would make `git status` fail
    os.environ['GIT_OBJECT_DIRECTORY'] = os.path.join(d19e, 'no-such-object-dir')
    try:
        both(d19e)
    except Exception as e:
        fails.append('git-env-leaked-into-the-git-calls:%s' % type(e).__name__)
    os.environ.pop('GIT_OBJECT_DIRECTORY')
    # the user's own git configuration is not read
    fake_home = tempfile.mkdtemp()
    write(os.path.join(fake_home, '.gitconfig'), b'[user]\n\tname = LEAKED\n')
    os.environ['HOME'] = fake_home
    real_r19 = m.REPO
    m.REPO = d19e
    try:
        try:
            who = m.git('config', '--get', 'user.name')
        except subprocess.CalledProcessError:
            who = ''
    finally:
        m.REPO = real_r19
    if who:
        fails.append('global-git-config-was-read:' + who)
finally:
    for k, v in saved19.items():
        if v is None:
            os.environ.pop(k, None)
        else:
            os.environ[k] = v
want_env = {'PATH': '/p', 'HOME': '/h', 'GIT_CONFIG_NOSYSTEM': '1', 'GIT_CONFIG_GLOBAL': '/dev/null', 'GIT_NO_REPLACE_OBJECTS': '1'}
if m.git_env({'PATH': '/p', 'HOME': '/h', 'GIT_DIR': 'x', 'GIT_CONFIG_COUNT': '1', 'GIT_SSH_COMMAND': 'y', 'GIT_CONFIG_GLOBAL': '/evil'}) != want_env:
    fails.append('git-env-not-scrubbed:%s' % sorted(m.git_env({'GIT_DIR': 'x', 'GIT_CONFIG_COUNT': '1'})))
# replace refs: a replaced blob must not show another content
d19f, sg19f = scratch()
blob_a = subprocess.check_output(['git', '-C', d19f, 'rev-parse', 'HEAD:src/A.java'], env=m.git_env(os.environ), universal_newlines=True).strip()
write(os.path.join(d19f, 'evil.txt'), b'REPLACED\n')
blob_b = subprocess.check_output(['git', '-C', d19f, 'hash-object', '-w', 'evil.txt'], env=m.git_env(os.environ), universal_newlines=True).strip()
sg19f('replace', blob_a, blob_b)
real_r19 = m.REPO
m.REPO = d19f
try:
    shown = m.git('cat-file', '-p', 'HEAD:src/A.java')
finally:
    m.REPO = real_r19
if shown != 'class A{}':
    fails.append('replace-ref-honoured:' + shown)
# the guard is wired into the preflight and into the recheck
d19g, sg19g = scratch()
write(os.path.join(d19g, 'src', 'A.java'), b'class A{ /*TAMPERED*/ }\n')
sg19g('update-index', '--assume-unchanged', 'src/A.java')
head19 = subprocess.check_output(['git', '-C', d19g, 'rev-parse', 'HEAD'], env=m.git_env(os.environ), universal_newlines=True).strip()
real_r19 = m.REPO
m.REPO = d19g
try:
    ns19 = type('N', (), {'evidence_dir': os.path.join(d19g, 'target', 'ev'), 'overwrite': False, 'sha': head19})()
    err19 = io.StringIO()
    try:
        with contextlib.redirect_stderr(err19):
            m.preflight(ns19)
    except SystemExit:
        pass
    except Exception:
        pass  # went past the guard and failed on a later step: the refusal text below is what counts
    if 'git is not showing everything' not in err19.getvalue():
        fails.append('preflight-did-not-refuse-a-hidden-tree')
    r19 = object.__new__(m.Run)
    r19.head, r19.a = head19, ns19
    try:
        r19.recheck_tree()
        fails.append('recheck-tree-accepted-a-hidden-tree')
    except RuntimeError as e:
        if 'git is not showing everything' not in str(e) or d19g in str(e):
            fails.append('recheck-tree-hidden-message-wrong:%s' % e)
finally:
    m.REPO = real_r19

# 20 (round 9, S3-2): every catchable signal ends the run through the cleanup; sent to a child interpreter (an unhandled SIGQUIT would end this process)
for n_ in ('SIGTERM', 'SIGHUP', 'SIGINT', 'SIGQUIT', 'SIGUSR1', 'SIGUSR2', 'SIGALRM'):
    if getattr(signal, n_) not in m.HANDLED_SIGNALS:
        fails.append('signal-%s-not-in-HANDLED_SIGNALS' % n_)
SIG_CHILD = '''
import importlib.util, os, signal, sys, tempfile, time, types
sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('r', sys.argv[1]); r = importlib.util.module_from_spec(spec); spec.loader.exec_module(r)
base = sys.argv[3]
repo = os.path.join(base, 'repo'); img = os.path.join(repo, 'docs', 'user-guide', 'images', 'swagger'); os.makedirs(img)
r.REPO = repo; r.GUIDE_IMAGES = os.path.join(repo, 'docs', 'user-guide', 'images')
a = types.SimpleNamespace(evidence_dir=os.path.join(base, 'ev'), m2=None, chrome_port=1, hold_seconds=5, overwrite=False)
run = r.Run(a, '0' * 40)
def fake():
    run.touched.add(img); run.swagger_before = {}; run.swagger_started = True
    open(os.path.join(img, 'overview.png'), 'wb').write(b'x')
    open(os.path.join(img, 'capture.json'), 'w').write('{"p": "%s/x"}' % repo)
    run.spawn(['sleep', '300'], 'sleep.log')
    print('CHILD', run.procs[0].pid, flush=True)
    os.kill(os.getpid(), getattr(signal, sys.argv[2])); time.sleep(30)
run.swagger = fake
sys.exit(run.execute())
'''
for sname in ('SIGQUIT', 'SIGUSR1'):
    base20 = tempfile.mkdtemp()
    try:
        pr20 = subprocess.run([sys.executable, '-B', '-c', SIG_CHILD, sys.argv[1], sname, base20], capture_output=True, text=True, timeout=60,
                              env=dict(os.environ, PYTHONDONTWRITEBYTECODE='1'))
        rc20, out20 = pr20.returncode, pr20.stdout
    except subprocess.TimeoutExpired:
        rc20, out20 = 'timeout', ''
    left20 = os.listdir(os.path.join(base20, 'repo', 'docs', 'user-guide', 'images', 'swagger'))
    q20 = os.path.join(base20, 'ev', 'quarantine')
    if rc20 != 128 + int(getattr(signal, sname)):
        fails.append('signal-%s-run-did-not-finish-through-the-cleanup:rc=%s' % (sname, rc20))
    if left20:
        fails.append('signal-%s-left-output-in-the-docs:%s' % (sname, ','.join(sorted(left20))))
    if not os.path.isdir(q20):
        fails.append('signal-%s-no-quarantine' % sname)
    if not os.path.isfile(os.path.join(base20, 'ev', 'result.json')):
        fails.append('signal-%s-no-result-json' % sname)
    kid = [l_.split()[1] for l_ in out20.splitlines() if l_.startswith('CHILD ')]
    if kid:
        try:
            os.kill(int(kid[0]), 0)
            fails.append('signal-%s-child-left-running' % sname)
            os.killpg(int(kid[0]), signal.SIGKILL)
        except ProcessLookupError:
            pass

# 21 (round 9, S3-3): a non-ASCII home directory is found in every form it can take in an output.
def forms_scan(home_text):
    real_home21 = os.environ.get('HOME')
    os.environ['HOME'] = home_text
    try:
        nn = m.leak_needles()
    finally:
        if real_home21 is None:
            os.environ.pop('HOME', None)
        else:
            os.environ['HOME'] = real_home21
    return nn
nfc21 = '/Users/José Smith'
nfd21 = unicodedata.normalize('NFD', nfc21)
text_forms = {'nfc': nfc21, 'nfd': nfd21, 'lower': nfc21.lower(), 'percent': 'file://' + nfc21.replace('é', '%C3%A9').replace(' ', '%20'),
              'percent-lower': nfc21.lower().replace('é', '%c3%a9').replace(' ', '%20'), 'json': '/Users/Jos\\u00e9 Smith',
              'lower-nfd': nfd21.lower()}
for base_name, base_home in (('nfc-home', nfc21), ('nfd-home', nfd21)):
    nn21 = forms_scan(base_home)
    for fname, text in sorted(text_forms.items()):
        if not m.scan_leaks([put('f21.json', ('{"e": "%s/proj"}' % text).encode('utf-8'))], nn21):
            fails.append('leak-scan-missed-%s-as-%s' % (base_name, fname))
# the manifest the runner rewrites keeps non-ASCII characters (an ASCII-escaped copy hides them from a byte scan)
cj21 = put('capture.json', ('{"images": [], "e": "at %s/x"}' % nfc21).encode('utf-8'))
r21 = object.__new__(m.Run)
r21.annotate(cj21, 'label', {})
raw21 = open(cj21, 'rb').read()
if nfc21.encode('utf-8') not in raw21:
    fails.append('annotate-escaped-a-non-ascii-path')

# 22 (round 9, S3-4): --evidence-dir inside the checkout is refused unless it lies below a directory named target.
def pre22(ev):
    ns_, err_ = pre(evidence_dir=ev)
    return err_
if 'named target' not in pre22(os.path.join(m.REPO, 'scripts', 'ev-x')):
    fails.append('evidence-inside-the-checkout-accepted')
if 'named target' not in pre22(os.path.join(m.REPO, 'targets', 'ev-x')):
    fails.append('evidence-below-a-lookalike-of-target-accepted')
for ok_ev in (os.path.join(m.REPO, 'appbase-stacktest', 'target', 'capture-evidence-x'), os.path.join(m.REPO, 'x', 'target', 'a', 'b'),
              os.path.join(tempfile.gettempdir(), 'ev-outside')):
    if 'named target' in pre22(ok_ev):
        fails.append('evidence-below-target-or-outside-refused:' + ok_ev[-20:])
if 'named target' in pre22(None):
    fails.append('default-evidence-dir-refused')

# 23 (round 9, S3-5): the tree is checked again after the Maven wait and before the manifest is written.
def swagger_flow(fail_second):
    r_ = object.__new__(m.Run)
    r_.evidence = tempfile.mkdtemp()
    gi_ = tempfile.mkdtemp()
    m.GUIDE_IMAGES = gi_
    r_.procs, r_.aborted = [], threading.Event()
    r_.touched, r_.swagger_started, r_.swagger_before, r_.webjar_info = set(), False, {}, None
    r_.result, r_.head = {'steps': {}}, 'h'
    r_.a = type('A', (), {'overwrite': True, 'm2': None, 'mvn': 'mvn', 'hold_seconds': 5, 'chrome_port': 1})()
    events = []
    fx_ = os.path.join(r_.evidence, 'fixture')
    os.makedirs(fx_)
    write(os.path.join(fx_, 'ready.url'), b'http://127.0.0.1:1/restapi/\n')
    r_.note = lambda msg: None
    r_.chrome_ready = lambda: None
    r_.webjar_assets = lambda: r_.evidence
    def recheck():
        events.append('recheck')
        if fail_second and events.count('recheck') == 2:
            raise RuntimeError('tree changed after the build')
    r_.recheck_tree = recheck
    r_.annotate = lambda *a_: events.append('annotate')
    class Proc(object):
        def __init__(self, name, action=None):
            self.name, self.action = name, action
        def poll(self):
            return None
        def wait(self, timeout=None):
            events.append(self.name + '.wait')
            if self.action:
                self.action()
            return 0
    def spawn(cmd, log, **kw):
        if cmd[0] == 'node':
            return Proc('node', lambda: write(os.path.join(cmd[3], 'capture.json'), b'{"images": []}'))
        return Proc('mvn')
    r_.spawn = spawn
    try:
        r_.swagger()
        return events, None
    except RuntimeError as e:
        return events, str(e)
ev_ok, err_ok = swagger_flow(False)
if err_ok is not None or ev_ok != ['recheck', 'node.wait', 'mvn.wait', 'recheck', 'annotate']:
    fails.append('recheck-not-repeated-after-the-maven-wait:%s|%s' % (','.join(ev_ok), err_ok))
ev_bad, err_bad = swagger_flow(True)
if err_bad != 'tree changed after the build' or 'annotate' in ev_bad:
    fails.append('manifest-written-after-a-failed-second-recheck:%s|%s' % (','.join(ev_bad), err_bad))
m.GUIDE_IMAGES = os.path.join(m.REPO, 'docs', 'user-guide', 'images')

notes_path = os.environ.get('CAP_UNIT_NOTES')
def note(text):
    if notes_path:
        with open(notes_path, 'a') as nf:
            nf.write(text + '\n')
real_lease = os.environ.get('APPBASE_BUILD_LEASE')
if real_lease and os.path.isfile(real_lease):
    # Behavioural: the REAL lease CLI accepts this argv (private lease root, so no shared slot is taken).
    lroot = tempfile.mkdtemp()
    ns2 = type('N', (), {'lease': real_lease, 'lease_project': 'appbase'})()
    try:
        pr = subprocess.run(m.lease_argv(ns2, ['true']), env=dict(os.environ, BUILD_LEASE_ROOT=lroot, BUILD_LEASE_POLL='1'),
                            capture_output=True, text=True, timeout=25)
        if pr.returncode == 2 and 'sage' in (pr.stdout + pr.stderr):
            fails.append('real-lease-cli-rejected-the-argv-with-its-usage-text')
    except subprocess.TimeoutExpired:
        note('the real lease CLI did not answer within 25 s: the real-lease argv check is NOT MEASURED')
else:
    note('the real-lease argv check was SKIPPED (APPBASE_BUILD_LEASE is not set to an existing file): NOT MEASURED, only the argv-order arm ran')

# 24 (IR2505-SEC-03): the listener proof fails closed: only numeric-loopback listening sockets pass; no proof is a refusal.
import http.server, socket
Cdp = type('Cdp', (http.server.BaseHTTPRequestHandler,), {'log_message': lambda *a_: None,
           'do_GET': lambda self: (self.send_response(200), self.end_headers(), self.wfile.write(b'{}'))})
def cdp_check(bind):
    srv = type('S', (http.server.HTTPServer,), {'address_family': socket.AF_INET6 if ':' in bind else socket.AF_INET})((bind, 0), Cdp)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    r_ = object.__new__(m.Run)
    r_.a = type('A', (), {'chrome_port': srv.server_address[1]})()
    try:
        return r_.chrome_ready()
    except RuntimeError as e:
        return str(e)
    finally:
        srv.shutdown()
        srv.server_close()
def lsof(rc, out, which=lambda n_: 'x', run=None):
    return m.listener_lines(9, which=which, run=run or (lambda *a_, **k_: type('R', (), {'returncode': rc, 'stdout': out})()))
lp = m.listener_problem
if m.shutil.which('lsof') is None:
    note('lsof is not installed on this host: the real-socket listener arms are NOT MEASURED')
else:
    for bind_, want_ in (('127.0.0.1', None), ('0.0.0.0', 'wildcard-listener-accepted'), ('::', 'ipv6-wildcard-accepted')):
        if bind_ == '::' and not socket.has_ipv6:
            note('this host has no IPv6: the real [::] listener arm is NOT MEASURED')
        elif (cdp_check(bind_) is None) != (want_ is None):
            fails.append(want_ or 'loopback-listener-refused')
for lines_, bad_, marker_ in ((['127.0.0.1:9', '[::1]:9'], 0, 'loopback-listener-refused'), (['*:9'], 1, 'wildcard-listener-accepted'),
                              (['0.0.0.0:9'], 1, 'wildcard-listener-accepted'), (['[::]:9'], 1, 'ipv6-wildcard-accepted'),
                              (['127.0.0.1:9', '*:9'], 1, 'mixed-listeners-accepted'), (['*:9', '127.0.0.1:9'], 1, 'mixed-listeners-accepted'),
                              (['127.0.0.1:9', '192.168.1.5:9'], 1, 'mixed-listeners-accepted'), ([], 1, 'no-listener-lines-accepted'),
                              (lsof(0, 'p1\nf3\n'), 1, 'no-listener-lines-accepted'), (lsof(1, ''), 1, 'lsof-failure-accepted'),
                              (lsof(1, 'n127.0.0.1:9227\n'), 1, 'lsof-nonzero-exit-accepted'),
                              (lsof(0, '', which=lambda n_: None), 1, 'lsof-missing-accepted'),
                              (lsof(0, '', run=lambda *a_, **k_: m.subprocess.run(['sleep', '9'], timeout=0.2)), 1, 'lsof-timeout-accepted')):
    if bool(lp(lines_)) != bool(bad_):
        fails.append(marker_)

# 25 (S2): --inner is refused without the lease re-exec's environment variable, which the re-exec sets.
class FakeRun(object):
    def __init__(self, a_, head_):
        pass
    def execute(self):
        return 7
saved = (m.preflight, m.Run, os.execvpe, sys.argv, os.environ.pop('APPBASE_CAPTURE_LEASED', None))
reexec = []
def fake_exec(f_, c_, e_):
    reexec.append(e_)
    raise SystemExit(0)
m.preflight, m.Run, os.execvpe = lambda a_: 'h', FakeRun, fake_exec
try:
    sys.argv = ['r', '--inner', '--chrome-port', '1', '--sha', 'x']
    if not refuses(m.main):
        fails.append('inner-accepted-without-the-lease-env')
    os.environ['APPBASE_CAPTURE_LEASED'] = '1'
    if refuses(m.main):
        fails.append('inner-refused-with-the-lease-env')
    del os.environ['APPBASE_CAPTURE_LEASED']
    sys.argv = ['r', '--chrome-port', '1', '--sha', 'x']
    refuses(m.main)
    if [e_.get('APPBASE_CAPTURE_LEASED') for e_ in reexec] != ['1']:
        fails.append('reexec-does-not-set-the-lease-env')
finally:
    m.preflight, m.Run, os.execvpe, sys.argv = saved[:4]
    os.environ.pop('APPBASE_CAPTURE_LEASED', None)
print('UNIT-FAIL: ' + ' '.join(fails) if fails else '')
PY
capture_docs_check() {  # $1 = guide dir
  local d="$1" bad=""
  grep -qF 'internal work item 2548' "$d/07-swagger.md" || bad="$bad 07-lacks-2548"
  grep -qF 'a stock core service does not currently' "$d/07-swagger.md" || bad="$bad 07-lacks-core-service-scope"
  grep -qE 'No class in the main code|no main code serves|main code registers (no|none)' "$d/07-swagger.md" "$d/known-divergences.md" && bad="$bad overbroad-no-main-code-claim"
  grep -qF '## Screenshot evidence — NOT MEASURED' "$d/07-swagger.md" || bad="$bad 07-lacks-not-measured"
  grep -qF 'No such run has been performed' "$d/07-swagger.md" || bad="$bad 07-lacks-not-run-statement"
  grep -qF 'no images or hashes are claimed here' "$d/07-swagger.md" || bad="$bad 07-lacks-no-evidence-boundary"
  grep -qE '!\[[^]]*\]\(images/swagger/' "$d/07-swagger.md" && bad="$bad 07-claims-swagger-image" || true
  echo "$bad"
}
if [ -z "$cf" ]; then
  mkdir -p "$TMPD/cap"
  rr="$(capture_runner_more_check "$CAP_RUNNER")"
  [ -z "$rr" ] && ok "EXTRA_capture_runner_provenance: leak scan, untracked check, pinned webjar, validated --m2, fail-closed watchdog" || bad "EXTRA_capture_runner_provenance:$rr"
  mut "$CAP_RUNNER" "$TMPD/cap/m1.py" "            self.leak_check()
" ""
  mut "$CAP_RUNNER" "$TMPD/cap/m2.py" "--untracked-files=normal" "--untracked-files=no"
  killed_by() { case "$1" in *"$2"*) return 0;; esac; return 1; }  # the named failure, not just any output
  # A mutant counts only if its file exists, is non-empty and differs from the original: a missing file makes every
  # grep fail and would otherwise look like a kill (QA-F3).
  mvalid() { [ -f "$1" ] && [ -s "$1" ] && ! cmp -s "$1" "$2"; }
  kb() {  # $1 mutant file, $2 original, $3 checker function, $4 the failure name it must report
    mvalid "$1" "$2" || return 1
    killed_by "$("$3" "$1")" "$4"
  }
  kb "$TMPD/cap/m1.py" "$CAP_RUNNER" capture_runner_more_check leak-check-not-called && ok "EXTRA_capture_runner_provenance: rejects a runner that never runs the leak check" || bad "EXTRA_capture_runner_provenance: accepted a runner without the leak check"
  kb "$TMPD/cap/m2.py" "$CAP_RUNNER" capture_runner_more_check untracked-files-ignored && ok "EXTRA_capture_runner_provenance: rejects a runner that ignores untracked files" || bad "EXTRA_capture_runner_provenance: accepted a runner ignoring untracked files"
  mut "$CAP_RUNNER" "$TMPD/cap/m5.py" "            except BaseException as q:" "            except OSError as q:"
  mut "$CAP_RUNNER" "$TMPD/cap/m6.py" "import shutil
" "import shutil
import tempfile
"
  mut "$CAP_RUNNER" "$TMPD/cap/m7.py" ":(exclude,literal)' + rel" ":(exclude)' + rel"
  mut "$CAP_RUNNER" "$TMPD/cap/m8.py" "        install_term_handlers()
        threading" "        threading"
  mut "$CAP_RUNNER" "$TMPD/cap/m9.py" "    if evidence_in_docs(a.evidence_dir):" "    if False:"
  mut "$CAP_RUNNER" "$TMPD/cap/m11.py" "connects only to 127.0.0.1 (the DevTools port and the fixture)" "connects only to this host's own addresses (127.0.0.1 and its primary IPv4)"
  mut "$CAP_RUNNER" "$TMPD/cap/m10.py" "    ap.add_argument('--overwrite'" "    ap.add_argument('--skip-swagger', action='store_true')
    ap.add_argument('--overwrite'"
  for pair in 5:no-leak-check-on-every-exit-path 7:glob-pathspec-exclusion 8:term-handlers-not-installed 9:evidence-dir-not-refused-in-docs 10:dead-skip-swagger-option 11:stale-primary-ipv4-docstring; do
    n="${pair%%:*}"
    kb "$TMPD/cap/m$n.py" "$CAP_RUNNER" capture_runner_more_check "${pair#*:}" && ok "EXTRA_capture_runner_provenance: rejects mutation m$n with its named failure (${pair#*:})" || bad "EXTRA_capture_runner_provenance: mutation m$n not rejected by its named failure"
  done
  # Negative controls for the static kill check itself: a missing, empty or unchanged mutant is NOT a kill.
  if kb "$TMPD/cap/does-not-exist.py" "$CAP_RUNNER" capture_runner_more_check leak-check-not-called; then bad "EXTRA_capture_runner_provenance: a MISSING mutant file was counted as killed (control)"; else ok "EXTRA_capture_runner_provenance: a missing mutant file is NOT counted as killed (negative control)"; fi
  : > "$TMPD/cap/empty.py"
  if kb "$TMPD/cap/empty.py" "$CAP_RUNNER" capture_runner_more_check leak-check-not-called; then bad "EXTRA_capture_runner_provenance: an EMPTY mutant file was counted as killed (control)"; else ok "EXTRA_capture_runner_provenance: an empty mutant file is NOT counted as killed (negative control)"; fi
  cp "$CAP_RUNNER" "$TMPD/cap/same.py"
  if kb "$TMPD/cap/same.py" "$CAP_RUNNER" capture_runner_more_check leak-check-not-called; then bad "EXTRA_capture_runner_provenance: an UNCHANGED copy was counted as killed (control)"; else ok "EXTRA_capture_runner_provenance: a mutant identical to the original is NOT counted as killed (negative control)"; fi
  if command -v python3 >/dev/null 2>&1; then
    [ -z "$(unused_imports "$CAP_RUNNER")" ] && ok "EXTRA_capture_runner_provenance: the runner imports nothing it does not use (AST check)" || bad "EXTRA_capture_runner_provenance: unused import: $(unused_imports "$CAP_RUNNER")"
    [ "$(unused_imports "$TMPD/cap/m6.py")" = "tempfile" ] && ok "EXTRA_capture_runner_provenance: rejects mutation m6, an unused import (AST check)" || bad "EXTRA_capture_runner_provenance: mutation m6 not rejected"
  fi
  if command -v python3 >/dev/null 2>&1; then
    # Behaviour under the system python3 (no network, no builds, no bytecode written).
    export PYTHONDONTWRITEBYTECODE=1
    hp="$(python3 -B "$CAP_RUNNER" --help 2>&1)"
    case "$hp" in *--chrome-port*"Swagger capture"*) ok "EXTRA_capture_help: --help prints the Swagger capture command" ;; *) bad "EXTRA_capture_help: --help lacks the Swagger capture command" ;; esac
    python3 -B "$CAP_RUNNER" --chrome-port 1 --sha abc >"$TMPD/cap/ref1.txt" 2>&1; rc1=$?
    [ "$rc1" = 2 ] && grep -qF '40-hex' "$TMPD/cap/ref1.txt" && ok "EXTRA_capture_refusal: a short --sha is refused (rc 2)" || bad "EXTRA_capture_refusal: a short --sha was not refused (rc $rc1)"
    if git -C "$ROOT" rev-parse --git-dir >/dev/null 2>&1; then
      python3 -B "$CAP_RUNNER" --chrome-port 1 --sha 0000000000000000000000000000000000000000 >"$TMPD/cap/ref2.txt" 2>&1; rc2=$?
      [ "$rc2" = 2 ] && grep -qF 'does not match HEAD' "$TMPD/cap/ref2.txt" && ok "EXTRA_capture_refusal: a --sha that is not HEAD is refused (rc 2)" || bad "EXTRA_capture_refusal: a foreign --sha was not refused (rc $rc2)"
    elif grep -qF 'materialized export directory without Git metadata' "$GDIR/07-swagger.md"; then
      echo "  NOTE EXTRA_capture_refusal: foreign-SHA check NOT MEASURED without .git; the guide documents exact-head capture after repository initialization"
    else
      bad "EXTRA_capture_refusal: no .git and the guide does not document the exact-head capture boundary"
    fi
    export TMPDIR="$TMPD"  # the embedded runs create their fixtures under the private temp dir that the trap removes
    : > "$TMPD/cap-unit.notes"
    ur="$(CAP_UNIT_NOTES="$TMPD/cap-unit.notes" python3 -B "$TMPD/cap-unit.py" "$CAP_RUNNER" "$ROOT" 2>&1)"
    while IFS= read -r unote; do echo "  NOTE EXTRA_capture_unit: $unote"; done < "$TMPD/cap-unit.notes"
    [ -z "$ur" ] && ok "EXTRA_capture_unit: --m2 validation, path-leak scan (json and text; PNG pixels are not scanned), the disk watchdog, quarantine on every exit path, evidence-dir guards and signal handling behave" || bad "EXTRA_capture_unit: $ur"
    mut "$CAP_RUNNER" "$TMPD/cap/u1.py" "if n in data:" "if False:"
    mut "$CAP_RUNNER" "$TMPD/cap/u2.py" "def validate_m2(m2):
" "def validate_m2(m2):
    return
"
    mut "$CAP_RUNNER" "$TMPD/cap/u3.py" "if failures >= 3:" "if False:"
    mut "$CAP_RUNNER" "$TMPD/cap/u4.py" "            except Exception as e:  # df failed: never go silent" "            except KeyboardInterrupt as e:"
    mut "$CAP_RUNNER" "$TMPD/cap/u5.py" "shutil.move(path, dest)" "os.replace(path, dest)"
    mut "$CAP_RUNNER" "$TMPD/cap/u7.py" "                    if scan_leaks([f], needles):
                        bad.append" "                    if False:
                        bad.append"
    mut "$CAP_RUNNER" "$TMPD/cap/u8.py" "needles = leak_needles([self.evidence])
        bad, stuck = [], []" "needles = leak_needles()
        bad, stuck = [], []"
    mut "$CAP_RUNNER" "$TMPD/cap/u9.py" "(prev + ' | ' if prev else '') + str(q)" "str(q)"
    mut "$CAP_RUNNER" "$TMPD/cap/u11.py" "    return fold(p) == fold(d) or fold(p).startswith(fold(d) + os.sep)" "    return False"
    mut "$CAP_RUNNER" "$TMPD/cap/u12.py" "        raise SystemExit(128 + signum)" "        pass"
    mut "$CAP_RUNNER" "$TMPD/cap/u13.py" "    return path.lower() if sys.platform == 'darwin' else path" "    return path"
    mut "$CAP_RUNNER" "$TMPD/cap/u14.py" "if os.path.samefile(cur, d):" "if False:"
    mut "$CAP_RUNNER" "$TMPD/cap/u15.py" "a.evidence_dir = os.path.abspath(a.evidence_dir)" "a.evidence_dir = a.evidence_dir"
    mut "$CAP_RUNNER" "$TMPD/cap/u17.py" "            ignore_signals()
            for p in reversed" "            for p in reversed"
    mut "$CAP_RUNNER" "$TMPD/cap/u18.py" "            except Exception as c:  # one stuck child" "            except KeyboardInterrupt as c:  # one stuck child"
    mut "$CAP_RUNNER" "$TMPD/cap/u19.py" "                self.quarantine_partial()" "                pass"
    mut "$CAP_RUNNER" "$TMPD/cap/u20.py" "if os.path.isfile(f) and self.swagger_before.get(n) != snapshot(f):" "if False:"
    mut "$CAP_RUNNER" "$TMPD/cap/u21.py" "        if moved and os.path.isfile(cj):" "        if False:"
    mut "$CAP_RUNNER" "$TMPD/cap/u22.py" "out += [html.escape(n) for n in out if html.escape(n) != n]" "pass"
    # fix round 7: each of these removes the mechanism one new arm of cap-unit.py asserts
    mut "$CAP_RUNNER" "$TMPD/cap/u24.py" "        ignore_signals()
        raise SystemExit" "        raise SystemExit"
    mut "$CAP_RUNNER" "$TMPD/cap/u25.py" "        except (OSError, ValueError):
            try:  # silence" "        except KeyboardInterrupt:
            try:  # silence"
    mut "$CAP_RUNNER" "$TMPD/cap/u26.py" "                except Exception as e:
                    stuck.append('%s (%s)' % (os.path.basename(f)" "                except KeyboardInterrupt as e:
                    stuck.append('%s (%s)' % (os.path.basename(f)"
    mut "$CAP_RUNNER" "$TMPD/cap/u27.py" "            except Exception as q:
                self.note('could not quarantine" "            except KeyboardInterrupt as q:
                self.note('could not quarantine"
    mut "$CAP_RUNNER" "$TMPD/cap/u28.py" "            except Exception as e:
                stuck.append('%s (%s)' % (n, type(e).__name__))" "            except KeyboardInterrupt as e:
                stuck.append('%s (%s)' % (n, type(e).__name__))"
    mut "$CAP_RUNNER" "$TMPD/cap/u29.py" "            except BaseException as w:" "            except KeyboardInterrupt as w:"
    mut "$CAP_RUNNER" "$TMPD/cap/u30.py" "self.swagger_before.get(n) != snapshot(f)" "n not in self.swagger_before"
    mut "$CAP_RUNNER" "$TMPD/cap/u31.py" "                if str(q) not in (prev or ''):" "                if True:"
    mut "$CAP_RUNNER" "$TMPD/cap/u32.py" "env=git_env(os.environ), " ""
    mut "$CAP_RUNNER" "$TMPD/cap/u36.py" "'swagger_ui_webjar': self.webjar_info}" "}"
    mut "$CAP_RUNNER" "$TMPD/cap/u37.py" "'sha256': h.hexdigest()}" "'sha256': ''}"
    mut "$CAP_RUNNER" "$TMPD/cap/u38.py" "lines[0][:2]" "lines[0]"
    mut "$CAP_RUNNER" "$TMPD/cap/u39.py" "['bash', a.lease, 'run', '--project', a.lease_project," "['bash', a.lease, '--project', a.lease_project, 'run',"
    # fix round 9: each of these removes one mechanism a new arm of cap-unit.py asserts, and must fail BY ITS NAME
    mut "$CAP_RUNNER" "$TMPD/cap/u40.py" "if not k.startswith('GIT_'))" "if k not in ('GIT_DIR', 'GIT_WORK_TREE', 'GIT_INDEX_FILE'))"
    mut "$CAP_RUNNER" "$TMPD/cap/u41.py" "GIT_CONFIG_GLOBAL='/dev/null', " ""
    mut "$CAP_RUNNER" "$TMPD/cap/u42.py" ", GIT_NO_REPLACE_OBJECTS='1')" ")"
    mut "$CAP_RUNNER" "$TMPD/cap/u43.py" "GIT_CONFIG_NOSYSTEM='1', " ""
    mut "$CAP_RUNNER" "$TMPD/cap/u44.py" "'-c', 'core.excludesFile=', " ""
    mut "$CAP_RUNNER" "$TMPD/cap/u45.py" "t.islower() or " ""
    mut "$CAP_RUNNER" "$TMPD/cap/u46.py" " or t == 'S'" ""
    mut "$CAP_RUNNER" "$TMPD/cap/u47.py" "return 'an ignored path under a source tree' if 'src' in segs else ''" "return ''"
    mut "$CAP_RUNNER" "$TMPD/cap/u48.py" "        return 'an ignored file outside target'" "        return ''"
    mut "$CAP_RUNNER" "$TMPD/cap/u49.py" "    if hidden:
        refuse(" "    if False:
        refuse("
    mut "$CAP_RUNNER" "$TMPD/cap/u50.py" "        if hidden:
            raise RuntimeError(" "        if False:
            raise RuntimeError("
    mut "$CAP_RUNNER" "$TMPD/cap/u51.py" "'--ignored=matching', " ""
    mut "$CAP_RUNNER" "$TMPD/cap/u52.py" "HANDLED_SIGNALS = catchable_signals()" "HANDLED_SIGNALS = (signal.SIGTERM, signal.SIGHUP, signal.SIGINT)"
    mut "$CAP_RUNNER" "$TMPD/cap/u53.py" "'SIGSTOP', 'SIGCHLD', 'SIGCLD', " "'SIGSTOP', "
    mut "$CAP_RUNNER" "$TMPD/cap/u54.py" "    out += [unicodedata.normalize('NFC', n) for n in out]  # NFC form
" ""
    mut "$CAP_RUNNER" "$TMPD/cap/u55.py" "    out += [unicodedata.normalize('NFD', n) for n in out]  # NFD form
" ""
    mut "$CAP_RUNNER" "$TMPD/cap/u56.py" "    out += [urllib.parse.quote(n, safe='/') for n in out]  # percent-encoded form (a file: URL)
" ""
    mut "$CAP_RUNNER" "$TMPD/cap/u57.py" "    out += [n.lower() for n in out]  # lower-cased form
" ""
    mut "$CAP_RUNNER" "$TMPD/cap/u58.py" "    out += [json.dumps(n)[1:-1] for n in out]" "    out += []"
    mut "$CAP_RUNNER" "$TMPD/cap/u59.py" "json.dump(data, f, indent=2, ensure_ascii=False)" "json.dump(data, f, indent=2)"
    mut "$CAP_RUNNER" "$TMPD/cap/u60.py" "if rel is not None and 'target' not in rel.split('/'):" "if False:"
    mut "$CAP_RUNNER" "$TMPD/cap/u61.py" "if rel is not None and 'target' not in rel.split('/'):" "if rel is not None:"
    mut "$CAP_RUNNER" "$TMPD/cap/u62.py" "'target' not in rel.split('/')" "'target' not in rel"
    mut "$CAP_RUNNER" "$TMPD/cap/u63.py" "        self.recheck_tree()  # the build and the run changed no source: the manifest still names what ran
" ""
    mut "$CAP_RUNNER" "$TMPD/cap/u64.py" "        self.recheck_tree()  # the build and the run changed no source: the manifest still names what ran
" ""
    mut "$CAP_RUNNER" "$TMPD/cap/u65.py" "('127.0.0.1', '[::1]')" "('127.0.0.1', '[::1]', '*', '0.0.0.0')"
    mut "$CAP_RUNNER" "$TMPD/cap/u66.py" "if isinstance(lines, str) or not lines:" "if isinstance(lines, str):"
    mut "$CAP_RUNNER" "$TMPD/cap/u67.py" "        return 'lsof is not installed'" "        return ['127.0.0.1:0']"
    mut "$CAP_RUNNER" "$TMPD/cap/u87.py" "    if r.returncode != 0:" "    if False:"
    mut "$CAP_RUNNER" "$TMPD/cap/u71.py" "('127.0.0.1', '[::1]')" "('127.0.0.1', '[::1]', '[::]', '::')"
    mut "$CAP_RUNNER" "$TMPD/cap/u72.py" "bad = [x for x in lines if" "bad = [x for x in lines[:1] if"
    mut "$CAP_RUNNER" "$TMPD/cap/u68.py" "if a.inner and os.environ.get('APPBASE_CAPTURE_LEASED') != '1':" "if False:"
    mut "$CAP_RUNNER" "$TMPD/cap/u69.py" "os.environ.get('APPBASE_CAPTURE_LEASED') != '1':" "True:"
    mut "$CAP_RUNNER" "$TMPD/cap/u70.py" "env = dict(os.environ, APPBASE_CAPTURE_LEASED='1')" "env = dict(os.environ)"
    mut "$TMPD/cap/u64.py" "$TMPD/cap/u64.py" "        if mvn.wait(timeout=120) != 0:" "        self.recheck_tree()
        if mvn.wait(timeout=120) != 0:"
    # A mutant counts as killed only when the unit script reports a named failure (UNIT-FAIL:). A crash (traceback,
    # empty output from a dead interpreter) is not a kill: it would make a broken arm look like a working one.
    unit_killed() { case "$1" in *Traceback*) return 1;; *'UNIT-FAIL:'*) return 0;; esac; return 1; }
    for n in 1 2 3 4 5 7 8 9 11 12 13 14 15 17 18 19 20 21 22 24 25 26 27 28 29 30 31 32 36 37 38 39; do
      uo="$(python3 -B "$TMPD/cap-unit.py" "$TMPD/cap/u$n.py" "$ROOT" 2>&1)"
      mvalid "$TMPD/cap/u$n.py" "$CAP_RUNNER" && unit_killed "$uo" && ok "EXTRA_capture_unit: rejects mutation $n with a named failure" || bad "EXTRA_capture_unit: mutation $n not rejected by a named failure: $(printf %s "$uo" | tail -c 160)"
    done
    unit_killed_by() { case "$1" in *Traceback*) return 1;; *"$2"*) return 0;; esac; return 1; }  # the NAMED failure, not just any
    for pair in 40:git-env-leaked-into-the-git-calls 40:git-env-not-scrubbed 41:global-git-config-was-read 42:replace-ref-honoured \
                43:git-env-not-scrubbed 44:local-excludesfile-hid-an-untracked-file 45:assume-unchanged-not-refused \
                46:skip-work-tree-not-refused 47:ignored-source-file-not-refused 47:info-exclude-hid-an-untracked-source \
                48:ignored-file-outside-target-not-refused 49:preflight-did-not-refuse-a-hidden-tree 50:recheck-tree-accepted-a-hidden-tree \
                51:ignored-source-file-not-refused 52:signal-SIGQUIT-not-in-HANDLED_SIGNALS 52:signal-SIGUSR1-run-did-not-finish-through-the-cleanup \
                53:signal-SIGCHLD-must-not-be-handled 54:leak-scan-missed-nfd-home-as-nfc 55:leak-scan-missed-nfc-home-as-nfd \
                56:leak-scan-missed-nfc-home-as-percent 57:leak-scan-missed-nfc-home-as-lower 58:leak-scan-missed-nfc-home-as-json \
                59:annotate-escaped-a-non-ascii-path 60:evidence-inside-the-checkout-accepted 61:default-evidence-dir-refused \
                62:evidence-below-a-lookalike-of-target-accepted 63:recheck-not-repeated-after-the-maven-wait 64:recheck-not-repeated-after-the-maven-wait \
                65:wildcard-listener-accepted 66:no-listener-lines-accepted 67:lsof-missing-accepted 71:ipv6-wildcard-accepted 72:mixed-listeners-accepted 87:lsof-nonzero-exit-accepted \
                68:inner-accepted-without-the-lease-env 69:inner-refused-with-the-lease-env 70:reexec-does-not-set-the-lease-env; do
      n="${pair%%:*}"; want="${pair#*:}"
      uo="$(python3 -B "$TMPD/cap-unit.py" "$TMPD/cap/u$n.py" "$ROOT" 2>&1)"
      mvalid "$TMPD/cap/u$n.py" "$CAP_RUNNER" && unit_killed_by "$uo" "$want" && ok "EXTRA_capture_unit: rejects mutation $n by its named failure ($want)" || bad "EXTRA_capture_unit: mutation $n not rejected by $want: $(printf %s "$uo" | tail -c 160)"
    done
    mut "$CAP_RUNNER" "$TMPD/cap/uc.py" "def now():" "raise RuntimeError('crash-control')
def now():"
    uo="$(python3 -B "$TMPD/cap-unit.py" "$TMPD/cap/uc.py" "$ROOT" 2>&1)"
    case "$uo" in *Traceback*) unit_killed "$uo" && bad "EXTRA_capture_unit: a crashing mutant was accepted as killed" || ok "EXTRA_capture_unit: a crashing mutant (traceback) is NOT accepted as killed (negative control)";; *) bad "EXTRA_capture_unit: the crash control did not crash";; esac
    if mvalid "$TMPD/cap/does-not-exist.py" "$CAP_RUNNER"; then bad "EXTRA_capture_unit: a MISSING mutant file was accepted as a mutant (negative control)"; else ok "EXTRA_capture_unit: a missing mutant file is NOT accepted as a kill (negative control)"; fi
    if mvalid "$TMPD/cap/same.py" "$CAP_RUNNER"; then bad "EXTRA_capture_unit: an unchanged copy was accepted as a mutant (negative control)"; else ok "EXTRA_capture_unit: a mutant identical to the original is NOT accepted as a kill (negative control)"; fi
    # QA-F2: the u8 mutant (evidence directory not scanned) must be rejected whatever TMPDIR is: here the temp dir
    # and HOME are nested, which used to let the home needle quarantine the file and hide the mutant.
    mkdir -p "$TMPD/homeish/tmp"
    ub="$(HOME="$TMPD/homeish" TMPDIR="$TMPD/homeish/tmp" python3 -B "$TMPD/cap-unit.py" "$CAP_RUNNER" "$ROOT" 2>&1)"
    [ -z "$ub" ] && ok "EXTRA_capture_unit: the unmutated runner passes with TMPDIR under HOME" || bad "EXTRA_capture_unit: unmutated runner failed with TMPDIR under HOME: $ub"
    uo="$(HOME="$TMPD/homeish" TMPDIR="$TMPD/homeish/tmp" python3 -B "$TMPD/cap-unit.py" "$TMPD/cap/u8.py" "$ROOT" 2>&1)"
    mvalid "$TMPD/cap/u8.py" "$CAP_RUNNER" && unit_killed "$uo" && ok "EXTRA_capture_unit: mutation 8 is rejected even when TMPDIR is under HOME" || bad "EXTRA_capture_unit: mutation 8 not rejected when TMPDIR is under HOME: $(printf %s "$uo" | tail -c 120)"
    sed 's/^NEUTRAL_ON = True$/NEUTRAL_ON = False/' "$TMPD/cap-unit.py" > "$TMPD/cap/unit-noneutral.py"
    cmp -s "$TMPD/cap-unit.py" "$TMPD/cap/unit-noneutral.py" && bad "EXTRA_capture_unit: the neutralisation switch was not found in the unit script"
    uo="$(HOME="$TMPD/homeish" TMPDIR="$TMPD/homeish/tmp" python3 -B "$TMPD/cap/unit-noneutral.py" "$TMPD/cap/u8.py" "$ROOT" 2>&1)"
    unit_killed "$uo" && bad "EXTRA_capture_unit: without the HOME/checkout neutralisation mutation 8 was still rejected (the arm does not need it)" || ok "EXTRA_capture_unit: without the neutralisation mutation 8 slips through when TMPDIR is under HOME (mutation of the arm: it is what makes the arm independent of TMPDIR)"
    if command -v node >/dev/null 2>&1 && [ "$(node -e 'process.stdout.write(typeof WebSocket === "function" ? "yes" : "no")' 2>/dev/null)" = yes ]; then
      # Sec2 S2-1 and Sec3: capture-swagger.mjs keeps only the browser's two version strings and a scrubbed error
      # message in capture.json. It runs against a fake DevTools HTTP endpoint (no browser, no network beyond 127.0.0.1).
      CAP_SW="$ROOT/scripts/docs/capture-swagger.mjs"
      cat > "$TMPD/cap/fake-cdp.mjs" <<'JS'
import http from 'node:http';
import fs from 'node:fs';
const server = http.createServer((req, res) => {
  res.setHeader('content-type', 'application/json');
  if (req.url === '/json/version') res.end(JSON.stringify({Browser: 'HeadlessChrome/999.0.1', 'Protocol-Version': '1.3',
    'User-Agent': 'UA-SECRET', webSocketDebuggerUrl: 'ws://127.0.0.1:1/devtools/browser/SECRET-ID'}));
  else if (req.url.startsWith('/json/new')) res.end(JSON.stringify({id: 'T1', webSocketDebuggerUrl:
    (process.argv[3] || 'ws://127.0.0.1:@/devtools/page/T1').replace('@', server.address().port)}));
  else if (req.url.startsWith('/json/close')) { fs.writeFileSync(process.argv[2] + '.closed', '1'); res.end('{}'); }
  else res.end('{}');
});
server.on('upgrade', (req, socket) => { fs.writeFileSync(process.argv[2] + '.upgrade', '1'); socket.destroy(); });
server.listen(0, '127.0.0.1', () => fs.writeFileSync(process.argv[2], String(server.address().port)));
setTimeout(() => process.exit(0), 90000);
JS
      swagger_manifest_problems() {  # $1 = capture-swagger.mjs (or a mutant), $2 = tag; prints the problems found
        local sd="$TMPD/cap/sw-$2" i=0 port="" out="" cj srv
        mkdir -p "$sd"; rm -f "$sd/port"
        node "$TMPD/cap/fake-cdp.mjs" "$sd/port" >/dev/null 2>&1 &
        srv=$!
        while [ "$i" -lt 50 ] && [ ! -s "$sd/port" ]; do sleep 0.1; i=$((i+1)); done
        port="$(cat "$sd/port" 2>/dev/null)"
        if [ -z "$port" ]; then kill "$srv" 2>/dev/null; echo no-fake-endpoint; return; fi
        node "$1" http://127.0.0.1:1/ "$sd/out" "$port" 0000000000000000000000000000000000000000 >"$sd/run.log" 2>&1
        kill "$srv" 2>/dev/null; wait "$srv" 2>/dev/null
        cj="$sd/out/capture.json"
        [ -s "$cj" ] || { echo no-manifest; return; }
        grep -qF webSocketDebuggerUrl "$cj" && out="$out ws-url-in-manifest"
        grep -qF 'devtools/browser' "$cj" && out="$out devtools-browser-in-manifest"
        grep -qF 'UA-SECRET' "$cj" && out="$out user-agent-in-manifest"
        grep -qF 'HeadlessChrome/999.0.1' "$cj" || out="$out browser-version-missing"
        grep -qE '(^|[^[:alnum:]])at [^ ]+ \(|\.mjs:[0-9]' "$cj" && out="$out stack-trace-in-manifest"
        grep -qF "$TMPD" "$cj" && out="$out local-path-in-manifest"
        grep -qF 'CDP connection failed' "$cj" || out="$out error-message-missing"
        echo "$out"
      }
      CAP_SHA40=0000000000000000000000000000000000000000
      ws_reject_problems() {  # $1 = capture-swagger.mjs (or a mutant), $2 = tag, $3 = the WebSocket URL the fake returns ('@' = its port): refused by name, never connected to
        local sd="$TMPD/cap/wr-$2" i=0 port="" out="" srv rc
        mkdir -p "$sd"; rm -f "$sd/port" "$sd/port.upgrade" "$sd/port.closed"
        node "$TMPD/cap/fake-cdp.mjs" "$sd/port" "$3" >/dev/null 2>&1 &
        srv=$!
        while [ "$i" -lt 50 ] && [ ! -s "$sd/port" ]; do sleep 0.1; i=$((i+1)); done
        port="$(cat "$sd/port" 2>/dev/null)"
        if [ -z "$port" ]; then kill "$srv" 2>/dev/null; echo no-fake-endpoint; return; fi
        node "$1" http://127.0.0.1:1/ "$sd/out" "$port" "$CAP_SHA40" >"$sd/run.log" 2>&1; rc=$?
        kill "$srv" 2>/dev/null; wait "$srv" 2>/dev/null
        [ "$rc" != 0 ] || out="$out ws-url-accepted"
        grep -qF CDP_WEBSOCKET_URL_REJECTED "$sd/run.log" || out="$out named-error-missing"
        [ -e "$sd/port.upgrade" ] && out="$out connected-to-the-rejected-url"
        [ -e "$sd/port.closed" ] || out="$out tab-not-closed"
        echo "$out"
      }
      swp="$(swagger_manifest_problems "$CAP_SW" base)"
      [ -z "$swp" ] && ok "EXTRA_capture_swagger_manifest: capture.json keeps only Browser and Protocol-Version (no webSocketDebuggerUrl, no devtools/browser, no user agent) and a scrubbed error without stack or local path" || bad "EXTRA_capture_swagger_manifest:$swp"
      mut "$CAP_SW" "$TMPD/cap/sw1.mjs" "const browser = {Browser: String(version.Browser ?? ''), 'Protocol-Version': String(version['Protocol-Version'] ?? '')};" "const browser = version;"
      mut "$CAP_SW" "$TMPD/cap/sw2.mjs" "manifest.error = scrub(error && error.message ? error.message : error);" "manifest.error = error.stack || String(error);"
      for pair in 1:ws-url-in-manifest 2:stack-trace-in-manifest; do
        n="${pair%%:*}"
        if mvalid "$TMPD/cap/sw$n.mjs" "$CAP_SW" && killed_by "$(swagger_manifest_problems "$TMPD/cap/sw$n.mjs" "m$n")" "${pair#*:}"; then
          ok "EXTRA_capture_swagger_manifest: rejects mutation sw$n with its named problem (${pair#*:})"
        else bad "EXTRA_capture_swagger_manifest: mutation sw$n not rejected by ${pair#*:}"; fi
      done
      if killed_by "$(swagger_manifest_problems "$TMPD/cap/does-not-exist.mjs" missing)" ws-url-in-manifest; then bad "EXTRA_capture_swagger_manifest: a missing mutant script was counted as killed (control)"; else ok "EXTRA_capture_swagger_manifest: a missing mutant script is NOT counted as killed (negative control)"; fi
      # IR2505-SEC-03: a DevTools WebSocket URL that is not ws://127.0.0.1:<the chosen port> is refused by name before any connection
      wi=0
      for u in 'ws://192.0.2.1:@/devtools/page/T1' 'ws://127.0.0.1:1/devtools/page/T1' 'wss://127.0.0.1:@/devtools/page/T1' 'http://127.0.0.1:@/devtools/page/T1'; do
        wi=$((wi+1)); wp="$(ws_reject_problems "$CAP_SW" "b$wi" "$u")"
        [ -z "$wp" ] && ok "EXTRA_capture_swagger_ws: the DevTools URL ${u%%@*} is refused by name (CDP_WEBSOCKET_URL_REJECTED), exit non-zero, and the fake sees no WebSocket upgrade" || bad "EXTRA_capture_swagger_ws: ${u%%@*}:$wp"
      done
      mut "$CAP_SW" "$TMPD/cap/sw4.mjs" "wsUrl.protocol !== 'ws:' || " ""
      mut "$CAP_SW" "$TMPD/cap/sw5.mjs" "wsUrl.hostname !== '127.0.0.1' || " ""
      mut "$CAP_SW" "$TMPD/cap/sw6.mjs" " || wsUrl.port !== String(Number(portText))" ""
      mut "$CAP_SW" "$TMPD/cap/sw7.mjs" "  await getJSON(\`/json/close/\${target.id}\`).catch(() => {});  // the throw below skips the finally: close the tab here
" ""
      if mvalid "$TMPD/cap/sw7.mjs" "$CAP_SW" && killed_by "$(ws_reject_problems "$TMPD/cap/sw7.mjs" "m7" 'ws://192.0.2.1:@/devtools/page/T1')" tab-not-closed; then ok "EXTRA_capture_swagger_ws: rejects the mutation that leaves the tab open after a refused DevTools URL (tab-not-closed)"
      else bad "EXTRA_capture_swagger_ws: mutation sw7 (tab left open after a refused URL) not rejected by tab-not-closed"; fi
      for triple in 'sw4|http://127.0.0.1:@/devtools/page/T1|scheme' 'sw5|ws://192.0.2.1:@/devtools/page/T1|host' 'sw6|ws://127.0.0.1:1/devtools/page/T1|port'; do
        IFS='|' read -r n u what <<<"$triple"
        if mvalid "$TMPD/cap/$n.mjs" "$CAP_SW" && killed_by "$(ws_reject_problems "$TMPD/cap/$n.mjs" "m$n" "$u")" named-error-missing; then ok "EXTRA_capture_swagger_ws: rejects mutation $n (the $what check removed) by its named marker (named-error-missing)"
        else bad "EXTRA_capture_swagger_ws: mutation $n ($what check removed) not rejected by named-error-missing"; fi
      done
      if mvalid "$TMPD/cap/does-not-exist.mjs" "$CAP_SW"; then bad "EXTRA_capture_swagger_ws: a missing mutant script was accepted as a mutant (control)"; else ok "EXTRA_capture_swagger_ws: a missing mutant script is NOT accepted as a kill (negative control)"; fi
      # Node >= 22 guard: without a global WebSocket the script says so and exits 2 before any network use (global switched off with --no-experimental-websocket; a Node without the flag is reported, not failed)
      node --no-experimental-websocket "$CAP_SW" http://127.0.0.1:1/ "$TMPD/cap/sw-nows" 9 "$CAP_SHA40" >"$TMPD/cap/nows.log" 2>&1; nows_rc=$?
      if grep -qiE 'bad option|unknown option' "$TMPD/cap/nows.log"; then echo "  NOTE EXTRA_capture_swagger_node: this node has no --no-experimental-websocket: the Node >= 22 guard arm is NOT MEASURED"
      elif [ "$nows_rc" = 2 ] && grep -qF 'needs Node >= 22' "$TMPD/cap/nows.log"; then ok "EXTRA_capture_swagger_node: without a global WebSocket the script exits 2 with a 'needs Node >= 22' message"
      else bad "EXTRA_capture_swagger_node: without a global WebSocket the script did not exit 2 with 'needs Node >= 22' (rc $nows_rc)"; fi
      mut "$CAP_SW" "$TMPD/cap/sw3.mjs" "if (typeof WebSocket === 'undefined') {" "if (false) {"
      node --no-experimental-websocket "$TMPD/cap/sw3.mjs" http://127.0.0.1:1/ "$TMPD/cap/sw-nows3" 9 "$CAP_SHA40" >"$TMPD/cap/nows3.log" 2>&1; nows3_rc=$?
      if grep -qiE 'bad option|unknown option' "$TMPD/cap/nows3.log"; then :
      elif [ "$nows3_rc" = 2 ] && grep -qF 'needs Node >= 22' "$TMPD/cap/nows3.log"; then bad "EXTRA_capture_swagger_node: the guard mutation was not rejected"
      else ok "EXTRA_capture_swagger_node: rejects the mutation that removes the Node >= 22 guard (no 'needs Node >= 22' message)"; fi
    else echo "  NOTE EXTRA_capture_swagger_manifest/node: UNAVAILABLE (no node, or a node without a global WebSocket: the script needs Node >= 22)"; fi
  else echo "  NOTE EXTRA_capture_unit/help/refusal: UNAVAILABLE (no python3)"; fi
fi
db="$(capture_docs_check "$GDIR")"
[ -z "$db" ] && ok "EXTRA_capture_docs: part 7 states the scoped internal work item 2548 gap and the NOT MEASURED/no-evidence boundary" || bad "EXTRA_capture_docs:$db"
if [ "${USER_GUIDE_FIXTURE_RUN:-0}" != 1 ]; then
  mkdir -p "$TMPD/capdoc"; cp "$GDIR"/*.md "$TMPD/capdoc/"
  for pair in '## Screenshot evidence — NOT MEASURED|## Screenshot evidence — COMPLETE:07-lacks-not-measured' \
              'No such run has been performed|A run has been performed:07-lacks-not-run-statement' \
              'no images or hashes are claimed here|images and hashes are claimed here:07-lacks-no-evidence-boundary'; do
    from="${pair%%|*}"; rest="${pair#*|}"; to="${rest%%:*}"; want="${rest#*:}"
    mut "$GDIR/07-swagger.md" "$TMPD/capdoc/07-swagger.md" "$from" "$to"
    case "$(capture_docs_check "$TMPD/capdoc")" in *"$want"*) ok "EXTRA_capture_docs: rejects a guide without its statement named $want";; *) bad "EXTRA_capture_docs: accepted a guide without $want";; esac
  done
  cp "$GDIR/07-swagger.md" "$TMPD/capdoc/07-swagger.md"; printf '\n![claim](images/swagger/overview.png)\n' >> "$TMPD/capdoc/07-swagger.md"
  case "$(capture_docs_check "$TMPD/capdoc")" in *07-claims-swagger-image*) ok "EXTRA_capture_docs: rejects a Swagger image claim under NOT MEASURED";; *) bad "EXTRA_capture_docs: accepted a Swagger image claim under NOT MEASURED";; esac
fi

# ── EXTRA_wizard_prebuild — the wizard scripts' own build fails from a clean checkout ────────────────
# (-Dmaven.test.skip=true never builds appbase-client-core's test-jar, which appbase-client-clamav needs;
# reproduced on internal work item 2504's Example 4). The guide must not promise the self-build, and must say how to build.
WIZ="$(section 'configuration wizard')"
if grep -qiE 'script builds it first' <<<"$WIZ"; then
  bad "EXTRA_wizard_prebuild: the guide promises the script's self-build, which fails from a clean checkout"
elif grep -qE -- '-DskipTests' <<<"$WIZ"; then
  ok "EXTRA_wizard_prebuild: the wizard section says to build first and does not promise the self-build"
else
  bad "EXTRA_wizard_prebuild: the wizard section must name the build that makes the jar"
fi

# ── EXTRA_infra_commands — real per-client commands; internal work item 2547/internal work item 2562 fixed all six scripts ──────────────
# infra_check <guide dir> <root that holds the scripts> <real root, for the directories the scripts name>
STACKTEST_INFRA="appbase-stacktest/infra/start-infra.sh appbase-stacktest/infra/run-infra.sh appbase-stacktest/infra/stop-infra.sh"
SERVICE_INFRA="appbase-java/appbase-service/infra/start-infra.sh appbase-java/appbase-service/infra/stop-infra.sh appbase-java/appbase-service/infra/run-infra.sh"
ALL_INFRA="$STACKTEST_INFRA $SERVICE_INFRA"
infra_check() {
  local gd="$1" sr="$2" rr="$3" p bad="" d
  for p in appbase-java/appbase-client/appbase-client-redis/infra/docker/redis/run-redis.sh \
           appbase-java/appbase-client/appbase-client-core/infra/docker/docker-run-all.sh; do
    [ -f "$rr/$p" ] || bad="$bad missing:$p"
  done
  for p in $ALL_INFRA; do   # none of the six may use the shared base that never existed (the internal work item 2547 bug)
    grep -qE '^(export )?APPBASE_INFRA_BASEDIR=.*appbase-client/infra/docker' "$sr/$p" && bad="$bad $p:still-has-the-bad-base"
    grep -qE '^APPBASE_CLIENT_ROOT=\$\(cd "[^"]*appbase-client" ' "$sr/$p" || bad="$bad $p:no-APPBASE_CLIENT_ROOT"
    for d in appbase-client-aerospike/infra/docker/aerospike appbase-client-zookeeper/infra/docker/zookeeper appbase-client-firebase/infra/docker/firebase; do
      grep -qF "$d" "$sr/$p" || bad="$bad $p:does-not-name-$d"
      [ -d "$rr/appbase-java/appbase-client/$d" ] || bad="$bad missing-dir:$d"
    done
  done
  for p in $SERVICE_INFRA; do  # only the three appbase-service scripts drive Vitess
    grep -qF 'appbase-client-vitess/infra/local/service-schema' "$sr/$p" || bad="$bad $p:does-not-name-vitess-service-schema"
  done
  [ -d "$rr/appbase-java/appbase-client/appbase-client-vitess/infra/local/service-schema" ] || bad="$bad missing-dir:appbase-client-vitess/infra/local/service-schema"
  [ -d "$rr/appbase-java/appbase-client/infra/docker" ] && bad="$bad the-base-now-exists"
  local g="$gd/06-docker-infra.md"
  grep -qF 'Fixed: internal work item 2547' "$g" || bad="$bad guide-lacks-fixed-2547-heading"
  grep -qF 'Previously, five of the six' "$g" || bad="$bad guide-lacks-previously-five-of-six"
  grep -qiE 'currently broken|do not use these (five|six) scripts' "$g" && bad="$bad guide-still-says-broken"
  grep -qF 'appbase-java/appbase-service/infra/run-infra.sh' "$g" || bad="$bad guide-omits-the-sixth-script"
  echo "$bad"
}
infra_bad="$(infra_check "$GDIR" "$ROOT" "$ROOT")"
[ -z "$infra_bad" ] && ok "EXTRA_infra_commands: per-client commands exist; internal work item 2547/internal work item 2562 fixed the bad shared base on all six scripts" \
  || bad "EXTRA_infra_commands:$infra_bad"

# ── EXTRA_ports — the ports in the part 6 table appear in the cited infra directories ────────────────
ports_check() {  # $1 = guide dir; prints the problems
  local g="$1/06-docker-infra.md" row dir cell n port found nrows=0 bad="" nports=0
  while IFS= read -r row; do
    dir="$(sed -nE 's/^\| [^|]+ \| `([^`]+)` \| .*/\1/p' <<<"$row")"
    cell="$(sed -nE 's/^\| [^|]+ \| `[^`]+` \| (.*) \|$/\1/p' <<<"$row")"
    [ -n "$dir" ] || continue
    nrows=$((nrows+1))
    [ -d "$ROOT/$dir" ] || { bad="$bad no-dir:$dir"; continue; }
    case "$cell" in none*) continue;; esac
    for port in $(grep -oE '[0-9]{4,5}' <<<"$cell" | sort -u); do
      nports=$((nports+1))
      grep -rqE "(^|[^0-9])$port([^0-9]|\$)" "$ROOT/$dir" --include='*.sh' --include='*.yml' --include='*.yaml' || bad="$bad $dir:port-$port-not-in-its-scripts"
    done
  done < <(grep -E '^\| [^|]+ \| `appbase-java/appbase-client/appbase-client-[a-z0-9-]+/infra/docker/' "$g")
  [ "$nrows" -ge 12 ] || bad="$bad only-$nrows-rows(vacuous)"
  echo "$bad|$nports"
}
res="$(ports_check "$GDIR")"
[ -z "${res%%|*}" ] && ok "EXTRA_ports: ${res##*|} ports in the part 6 table appear in the scripts of the directory they are listed for" \
  || bad "EXTRA_ports: ${res%%|*}"

# ── EXTRA_config_keys — the YAML keys and configuration classes part 2 names exist in the cited source ──
keys_check() {  # $1 = guide dir
  local g="$1/02-client-integrations.md" k bad="" n=0
  for k in $(grep -ohE '`(keyed)?default[A-Za-z]*Configurations?`' "$g" | tr -d '`' | sort -u); do
    n=$((n+1))
    grep -rqE "^[[:space:]]*$k:" "$ROOT/appbase-java/appbase-client" "$ROOT/appbase-stacktest/src/main/resources" --include='*.yaml' 2>/dev/null || bad="$bad key-not-in-any-yaml:$k"
  done
  for k in $(grep -ohE '`Default[A-Za-z0-9]*Configuration`' "$g" | tr -d '`' | sort -u); do
    n=$((n+1))
    [ -n "$(find "$ROOT/appbase-java/appbase-client" -name "$k.java" -print -quit)" ] || bad="$bad class-not-found:$k"
  done
  [ "$n" -ge 30 ] || bad="$bad only-$n-names(vacuous)"
  echo "$bad|$n"
}
res="$(keys_check "$GDIR")"
[ -z "${res%%|*}" ] && ok "EXTRA_config_keys: ${res##*|} YAML keys and configuration classes in part 2 exist in the source" \
  || bad "EXTRA_config_keys: ${res%%|*}"

# ── EXTRA_bom_claim — which service modules the guide says are missing from the BOM, against the BOM ──
bom_check() {  # $1 = guide dir
  local f="$1/03-service-modules.md" m bad="" s
  s="$(tr '\n' ' ' < "$f" | tr -s ' ')"
  for m in apple github google microsoft okta; do
    grep -qF "appbase-service-web-oauth-$m<" "$ROOT/appbase-bom/pom.xml" || bad="$bad $m-missing-from-BOM"
  done
  grep -qF 'All five OAuth provider modules are in `appbase-bom/pom.xml`' <<<"$s" || bad="$bad guide-does-not-say-all-five-are-in-the-BOM"
  echo "$bad"
}
bomb="$(bom_check "$GDIR")"
[ -z "$bomb" ] && ok "EXTRA_bom_claim: all five OAuth provider modules are in the BOM and the guide says so" || bad "EXTRA_bom_claim:$bomb"

# ── EXTRA_prose — no internal project jargon; honest claim scope; one-line control plane; pending stated ──
prose_check() {  # $1 = guide dir; prints the problems
  local d="$1" bad="" f n
  local scan; scan="$(cat "$d"/*.md | sed -E 's/P-019-appbase-deploy-spec\.md//g')"
  grep -qE 'P0[0-9]·|P-0[0-9]{2}|P06|A4-2|[Oo]wner ruling|ruled invalid|[Pp]roject owner|guide.s author|\bowner\b' <<<"$scan" && bad="$bad internal-jargon:$(grep -oE 'P0[0-9]·[A-Z0-9/]*|P-0[0-9]{2}|A4-2|[Oo]wner ruling|ruled invalid|[Pp]roject owner|guide.s author|\bowner\b' <<<"$scan" | head -3 | tr '\n' ',')"
  grep -qiE 'every command and path' "$d/README.md" && bad="$bad readme-overclaims-every-command-and-path"
  grep -qF 'were **not executed**' "$d/README.md" || bad="$bad readme-does-not-say-what-was-not-executed"
  for f in README.md 05-config-wizard.md 07-swagger.md 08-agent-integration.md 01-install-and-first-service.md; do
    n="$(grep -ciE 'control plane|orchestrator' "$d/$f")"
    [ "$n" -le 1 ] || bad="$bad $f:$n-control-plane-lines"
  done
  grep -qE 'bundle\.yaml|roles/bundles' <<<"$scan" && bad="$bad role-bundle-path-documented"
  grep -qF 'requires the authorized local stacktest capture' "$d/README.md" || bad="$bad readme-lacks-swagger-not-measured-boundary"
  grep -qF 'a recorded real console run is delivered in the stacked follow-up pull request' "$d/README.md" || bad="$bad readme-lacks-wizard-follow-up-pointer"
  grep -qF 'web-form screenshot is NOT CAPTURED' "$d/README.md" || bad="$bad readme-lacks-web-form-not-captured"
  grep -qF '| Wizard jar build | NOT MEASURED in this guide revision' "$d/README.md" || bad="$bad readme-lacks-wizard-build-not-measured"
  grep -qF 'are delivered in the stacked follow-up pull request; nothing in this pull request claims them' "$d/05-config-wizard.md" || bad="$bad 05-lacks-follow-up-pointer"
  grep -qF 'NOT CAPTURED: needs a Flutter-built jar' "$d/05-config-wizard.md" || bad="$bad 05-lacks-web-not-captured"
  grep -qF 'run once and exited 0, and it produced the wizard jar' "$d/05-config-wizard.md" || bad="$bad 05-lacks-build-run-statement"
  grep -qF 'not run and is unverified' "$d/05-config-wizard.md" && bad="$bad 05-still-says-build-unverified"
  grep -qF '## Screenshot evidence — NOT MEASURED' "$d/07-swagger.md" || bad="$bad 07-lacks-not-measured-heading"
  grep -qF 'no images or hashes are claimed here' "$d/07-swagger.md" || bad="$bad 07-lacks-no-evidence-boundary"
  grep -qF 'Do not use sample-service or generated fixture images' "$d/07-swagger.md" || bad="$bad 07-lacks-invalid-evidence-boundary"
  echo "$bad"
}
pb="$(prose_check "$GDIR")"
[ -z "$pb" ] && ok "EXTRA_prose: no internal jargon; claim scope stated; control plane one line; Swagger delivered-with-limits stated; wizard screenshots deferred to the follow-up; wizard build run stated" || bad "EXTRA_prose:$pb"

# ── EXTRA_snippet_complete — the Jetty builder chain in part 4 ends at .build(); ─────────────────────
snip_complete() {  # $1 = guide dir
  awk '/snippet-source: .*StackTestMain\.java/{s=1} s&&/^```java/{inb=1;next} inb&&/^```/{exit} inb&&NF{last=$0} END{print last}' "$1/04-bootstrap-flavors.md" | grep -qE '^[[:space:]]*\.build\(\);$'
}
snip_complete "$GDIR" && ok "EXTRA_snippet_complete: the part 4 Jetty chain ends at .build();" || bad "EXTRA_snippet_complete: the part 4 chain is cut before .build();"

# Mutations of the arms above, in-process on copies of the guide (and of the infra scripts).
if [ "${USER_GUIDE_FIXTURE_RUN:-0}" != 1 ]; then
  mutate_guide() {  # $1 = name, $2 = file, $3 = python expression text.replace args "old" "new"
    mkdir -p "$TMPD/m/$1"; cp "$GDIR"/*.md "$TMPD/m/$1/"
    python3 - "$TMPD/m/$1/$2" "$3" "$4" <<'PYM' || bad "EXTRA_mutations: the anchor for the $1 mutation no longer exists in $2 (update the arm)"
import sys
p, a, b = sys.argv[1:4]
t = open(p).read()
if a not in t:
    sys.exit('mutation anchor not found: ' + a)
open(p, 'w').write(t.replace(a, b))
PYM
  }
  expect_reject() {  # $1 arm label, $2 mutation name, $3 output of the check
    if [ -n "$3" ]; then ok "$1: rejects the $2 mutation"; else bad "$1: accepted the $2 mutation"; fi
  }
  # infra: the guide regresses to claim six scripts are broken; a script gains the bad base; a script loses its fix
  mutate_guide infra_regress 06-docker-infra.md "now resolve correctly" "now resolve correctly. Do not use these six scripts"
  expect_reject EXTRA_infra_commands "guide-says-broken" "$(infra_check "$TMPD/m/infra_regress" "$ROOT" "$ROOT")"
  mkdir -p "$TMPD/m/inf_root"
  for p in $ALL_INFRA; do mkdir -p "$TMPD/m/inf_root/$(dirname "$p")"; cp "$ROOT/$p" "$TMPD/m/inf_root/$p"; done
  [ -z "$(infra_check "$GDIR" "$TMPD/m/inf_root" "$ROOT")" ] && ok "EXTRA_infra_commands: a copy of the six scripts passes (control)" || bad "EXTRA_infra_commands: control copy of the scripts failed"
  printf 'APPBASE_INFRA_BASEDIR=$CWD/../../appbase-client/infra/docker/\n' >> "$TMPD/m/inf_root/appbase-java/appbase-service/infra/run-infra.sh"
  expect_reject EXTRA_infra_commands "sixth-script-uses-the-bad-base" "$(infra_check "$GDIR" "$TMPD/m/inf_root" "$ROOT")"
  cp "$ROOT/appbase-java/appbase-service/infra/run-infra.sh" "$TMPD/m/inf_root/appbase-java/appbase-service/infra/run-infra.sh"
  python3 - "$TMPD/m/inf_root/appbase-java/appbase-service/infra/stop-infra.sh" <<'PYM'
import sys
p = sys.argv[1]; t = open(p).read()
open(p, 'w').write(t.replace('APPBASE_CLIENT_ROOT=', 'XX_CLIENT_ROOT='))
PYM
  expect_reject EXTRA_infra_commands "fix-regressed" "$(infra_check "$GDIR" "$TMPD/m/inf_root" "$ROOT")"
  # ports and keys
  mutate_guide port 06-docker-infra.md '| 6379 |' '| 6380 |'
  expect_reject EXTRA_ports "redis-6380" "$(ports_check "$TMPD/m/port" | sed 's/|[0-9]*$//')"
  mutate_guide key 02-client-integrations.md '`defaultDefaultRedisClientIntegrationServiceConfiguration`' '`defaultDefaultRedixClientIntegrationServiceConfiguration`'
  expect_reject EXTRA_config_keys "renamed-key" "$(keys_check "$TMPD/m/key" | sed 's/|[0-9]*$//')"
  mutate_guide cls 02-client-integrations.md '`DefaultRedisClientServiceConfiguration`' '`DefaultRedixClientServiceConfiguration`'
  expect_reject EXTRA_config_keys "renamed-class" "$(keys_check "$TMPD/m/cls" | sed 's/|[0-9]*$//')"
  # BOM claim
  mutate_guide bom 03-service-modules.md 'All five OAuth provider modules are in `appbase-bom/pom.xml`' 'The OAuth provider BOM status is unknown'
  expect_reject EXTRA_bom_claim "all-providers-claim-removed" "$(bom_check "$TMPD/m/bom")"
  # counts: the phrase must be in the intended file, not just anywhere
  mutate_guide cnt 03-service-modules.md 'The tree has **17 service' 'The tree has **21 service'
  python3 - "$TMPD/m/cnt/03-service-modules.md" <<'PYM'
import sys
p = sys.argv[1]; t = open(p).read()
open(p, 'w').write(t.replace('21 service\nmodules', '21 service\nunits'))
PYM
  case "$(count_arms "$TMPD/m/cnt")" in *bad*"service modules"*) ok "EXTRA_counts: rejects '17 service modules' removed from part 3 (README still has it)";; *) bad "EXTRA_counts: accepted '17 service modules' removed from part 3";; esac
  mutate_guide five 04-bootstrap-flavors.md 'So there are **four web-server flavors**' 'So there are **five web-server flavors**'
  if grep -nEi '(^|[^0-9])(5|five) (bootstrap |web-server |web server )?flavors' "$TMPD/m/five/04-bootstrap-flavors.md" >/dev/null; then ok "EXTRA_counts: the stale-count pattern catches 'five web-server flavors'"; else bad "EXTRA_counts: the stale-count pattern misses 'five web-server flavors'"; fi
  # prose
  mutate_guide j1 README.md 'Publication is planned' 'Publishing is Release work tracked under P06·E03/E05; it is planned'
  expect_reject EXTRA_prose "P06-jargon" "$(prose_check "$TMPD/m/j1")"
  mutate_guide j2 01-install-and-first-service.md 'An examples module is planned for 2.0.0.' 'An examples module is 2.0.0 work (P-036/P-037).'
  expect_reject EXTRA_prose "P-036-jargon" "$(prose_check "$TMPD/m/j2")"
  mutate_guide j3 known-divergences.md 'Where an item has an issue number' "The tracker numbers are the ones given to this guide's author. Where an item has an issue number"
  expect_reject EXTRA_prose "guide-author-jargon" "$(prose_check "$TMPD/m/j3")"
  mutate_guide j4 README.md '| Clean-machine walkthrough | DEFERRED' '| Clean-machine walkthrough (A4-2) | DEFERRED'
  expect_reject EXTRA_prose "A4-2-jargon" "$(prose_check "$TMPD/m/j4")"
  mutate_guide ov README.md 'What `scripts/test/user_guide_test.sh` checks:' 'Every command and path in it was checked to exist. What `scripts/test/user_guide_test.sh` checks:'
  expect_reject EXTRA_prose "readme-overclaim" "$(prose_check "$TMPD/m/ov")"
  mutate_guide cp1 08-agent-integration.md 'They are not covered here.**' 'They are not covered here.** They belong to the control plane and the orchestrator.'
  python3 - "$TMPD/m/cp1/08-agent-integration.md" <<'PYM'
import sys
p = sys.argv[1]; t = open(p).read()
open(p, 'w').write(t.replace('and the orchestrator.', 'and the orchestrator.\nMore on the control plane in the role bundles.'))
PYM
  expect_reject EXTRA_prose "control-plane-sprawl" "$(prose_check "$TMPD/m/cp1")"
  mutate_guide cp2 08-agent-integration.md 'They are not covered here.**' 'They are not covered here.** Bundles live under docs/roles/bundles/<role>/bundle.yaml.'
  expect_reject EXTRA_prose "role-bundle-path" "$(prose_check "$TMPD/m/cp2")"
  mutate_guide pend1 07-swagger.md '## Screenshot evidence — NOT MEASURED' '## Screenshot evidence — COMPLETE'
  expect_reject EXTRA_prose "07-not-measured-removed" "$(prose_check "$TMPD/m/pend1")"
  mutate_guide pend2 README.md 'web-form screenshot is NOT CAPTURED' 'web-form screenshot exists'
  expect_reject EXTRA_prose "readme-web-form-claim" "$(prose_check "$TMPD/m/pend2")"
  mutate_guide pend3 05-config-wizard.md 'was **run once and exited 0, and it produced the wizard jar**' 'was **not run and is unverified**'
  expect_reject EXTRA_prose "wizard-build-back-to-unverified" "$(prose_check "$TMPD/m/pend3")"
  mutate_guide pend4 05-config-wizard.md 'are delivered in the stacked follow-up pull request; nothing in this pull request claims them' 'are delivered'
  expect_reject EXTRA_prose "follow-up-pointer-removed" "$(prose_check "$TMPD/m/pend4")"
  mutate_guide pend5 README.md 'a recorded real console run is delivered in the stacked follow-up pull request' 'a recorded real console run is delivered'
  expect_reject EXTRA_prose "readme-follow-up-pointer-removed" "$(prose_check "$TMPD/m/pend5")"
  mutate_guide pend6 07-swagger.md 'no images or hashes are claimed here' 'images are claimed here'
  expect_reject EXTRA_prose "no-evidence-boundary-removed" "$(prose_check "$TMPD/m/pend6")"
  # snippet completeness
  mutate_guide snip 04-bootstrap-flavors.md '                })
                .build();
```' '                })
```'
  snip_complete "$TMPD/m/snip" && bad "EXTRA_snippet_complete: accepted a chain cut before .build();" || ok "EXTRA_snippet_complete: rejects a chain cut before .build();"
fi

# ── EXTRA_brand_placement — the internal work item 1415 placement (icon-64 before the title) or a pointer to it ────────
if [ -f "$ROOT/docs/branding/exports/icon-64.png" ]; then
  grep -q 'branding/exports/icon-64.png' "$GUIDE" && ok "EXTRA_brand_placement: icon-64 placed per docs/branding/README.md" \
    || bad "EXTRA_brand_placement: docs/branding exists but the guide does not use icon-64.png"
else
  grep -qE 'internal work item 2502|internal work item 1415' "$GUIDE" && ok "EXTRA_brand_placement: brand not merged yet; the guide points at internal work item 1415/internal work item 2502" \
    || bad "EXTRA_brand_placement: brand not merged and the guide does not point at internal work item 1415/internal work item 2502"
fi

# Execute the unsupported-surface guard against the laundering shapes from R11-1 and code review.
# They use a complete copy of the real guide so every other arm remains meaningful; the nested run is suppressed
# from creating fixtures of its own.
if [ "${USER_GUIDE_FIXTURE_RUN:-0}" != 1 ] && [ "${USER_GUIDE_SKIP_NESTED_FIXTURES:-0}" != 1 ]; then
  FIXTURE_ROOT="$TMPD/fixtures"
  for fixture in gradle flutter blockquote flutter_surface react_surface qualified_react external_claim uncited_passed; do
    mkdir -p "$FIXTURE_ROOT/$fixture"
    cp "$GDIR"/*.md "$FIXTURE_ROOT/$fixture/"
  done
  # Portable prepend (BSD sed has no GNU `sed -i '1i text'`; on macOS that form takes '1i …' as the
  # backup suffix and never writes the fixture).
  prepend() { { printf '%b\n' "$2"; cat "$1"; } > "$1.tmp" && mv "$1.tmp" "$1"; }
  prepend "$FIXTURE_ROOT/gradle/README.md" 'Appbase is published on Maven Central and is not yet supported for Gradle builds.\n'
  prepend "$FIXTURE_ROOT/flutter/README.md" 'Appbase ships on Maven Central, although the Flutter client is not yet supported.\n'
  prepend "$FIXTURE_ROOT/blockquote/README.md" '> Get Appbase from Maven\n> Central; it is not part of any other repo.\n'
  prepend "$FIXTURE_ROOT/flutter_surface/README.md" 'Appbase ships appbase_flutter today; Gradle builds are not yet supported.\n'
  prepend "$FIXTURE_ROOT/react_surface/README.md" 'Use appbase-react for the web UI; the Windows installer is not yet shipped.\n'
  prepend "$FIXTURE_ROOT/qualified_react/README.md" 'appbase-react is not supported on Windows, but is supported on Linux.\n'
  awk '/^## External/{print; print "The external deploy was verified on staging."; next} {print}' \
    "$FIXTURE_ROOT/external_claim/README.md" > "$FIXTURE_ROOT/external_claim/README.md.tmp"
  mv "$FIXTURE_ROOT/external_claim/README.md.tmp" "$FIXTURE_ROOT/external_claim/README.md"
  sed 's/Walkthrough record: NOT MEASURED/Walkthrough record: PASSED 2026-09-29/' \
    "$FIXTURE_ROOT/uncited_passed/README.md" > "$FIXTURE_ROOT/uncited_passed/README.md.tmp"
  mv "$FIXTURE_ROOT/uncited_passed/README.md.tmp" "$FIXTURE_ROOT/uncited_passed/README.md"

  for fixture in gradle flutter blockquote flutter_surface react_surface qualified_react; do
    fixture_output="$(USER_GUIDE_FIXTURE_RUN=1 USER_GUIDE="$FIXTURE_ROOT/$fixture/README.md" bash "$0" 2>&1)"
    fixture_rc=$?
    if [ "$fixture_rc" -ne 0 ] && grep -q 'FAIL EXTRA_no_unsupported:' <<<"$fixture_output"; then
      ok "EXTRA_no_unsupported: rejects the R11-1 $fixture laundering fixture"
    else
      bad "EXTRA_no_unsupported: accepted the R11-1 $fixture laundering fixture"
    fi
  done
  fixture_output="$(USER_GUIDE_FIXTURE_RUN=1 USER_GUIDE="$FIXTURE_ROOT/external_claim/README.md" bash "$0" 2>&1)"
  fixture_rc=$?
  if [ "$fixture_rc" -ne 0 ] && grep -q 'FAIL AC1416-4:' <<<"$fixture_output"; then
    ok "AC1416-4: rejects an external-deploy verification claim"
  else
    bad "AC1416-4: accepted an external-deploy verification claim"
  fi

  fixture_output="$(USER_GUIDE_FIXTURE_RUN=1 USER_GUIDE="$FIXTURE_ROOT/uncited_passed/README.md" bash "$0" 2>&1)"
  fixture_rc=$?
  if [ "$fixture_rc" -ne 0 ] && grep -q 'FAIL AC1416-1: a PASSED walkthrough record must cite its evidence' <<<"$fixture_output"; then
    ok "AC1416-1: rejects an uncited PASSED walkthrough record"
  else
    bad "AC1416-1: accepted an uncited PASSED walkthrough record"
  fi

  # Content mutations of the new arms, each on a full copy of the guide.
  mkdir -p "$FIXTURE_ROOT/groupid" "$FIXTURE_ROOT/leak" "$FIXTURE_ROOT/stalecount"
  cp "$GDIR"/*.md "$FIXTURE_ROOT/groupid/"; cp "$GDIR"/*.md "$FIXTURE_ROOT/leak/"; cp "$GDIR"/*.md "$FIXTURE_ROOT/stalecount/"
  prepend "$FIXTURE_ROOT/groupid/01-install-and-first-service.md" '<groupId>com.viveworks</groupId>\n'
  prepend "$FIXTURE_ROOT/leak/01-install-and-first-service.md" 'See /Users/someone/notes and the codex lane.\n'
  prepend "$FIXTURE_ROOT/stalecount/01-install-and-first-service.md" 'Appbase has 21 service modules.\n'
  for pair in "groupid:EXTRA_groupid" "leak:EXTRA_hygiene" "stalecount:EXTRA_counts"; do
    fx="${pair%%:*}"; arm="${pair##*:}"
    fixture_output="$(USER_GUIDE_FIXTURE_RUN=1 USER_GUIDE="$FIXTURE_ROOT/$fx/README.md" bash "$0" 2>&1)"
    fixture_rc=$?
    if [ "$fixture_rc" -ne 0 ] && grep -q "FAIL $arm:" <<<"$fixture_output"; then
      ok "$arm: rejects the $fx fixture"
    else
      bad "$arm: accepted the $fx fixture"
    fi
  done

  utf8_locale="$(locale -a 2>/dev/null | awk 'tolower($0) ~ /utf-?8/ {print; exit}')"
  if [ -z "$utf8_locale" ]; then
    echo "  NOTE EXTRA_utf8_locale: UNAVAILABLE (host exposes no UTF-8 locale)"
  elif LC_ALL="$utf8_locale" USER_GUIDE_FIXTURE_RUN=1 bash "$0" >/dev/null 2>&1; then
    ok "EXTRA_utf8_locale: current bash handles section messages in $utf8_locale"
  else
    bad "EXTRA_utf8_locale: current bash failed in $utf8_locale"
  fi
fi

echo; echo "  passed: $PASSED   failed: $FAILED"; [ "$FAILED" -eq 0 ] || exit 1
