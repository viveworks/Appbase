#!/usr/bin/env bash
#
# licence-report.sh — turn `license-maven-plugin`'s THIRD-PARTY.txt into the committed
# per-dependency SPDX + Apache-2.0-compatibility table (internal work item 1380, epic internal work item 1139, P-025 clause 1).
#
# WHY A SCRIPT AND NOT A ONE-OFF READING. The report is a release-gate artifact that has to be
# REGENERATED after every dependency change (clause 7's per-change gate, and immediately after
# internal work item 1133/internal work item 2430 lands). A table derived by hand once is stale the day after it is written and nobody
# can tell, because a stale row and a current row look identical. Regeneration has to be a command.
#
# WHAT IT IS NOT. It resolves each dependency's DECLARED licence string — what the POM says — to an
# SPDX identifier and a compatibility class, through the committed map. That is machine output about
# metadata, not a legal opinion and not a check against the upstream project's actual licence. Rows
# whose upstream licence was verified by hand are called out in the report's prose, separately and
# deliberately, so the two kinds of claim are never mixed.
#
# TWO INPUT SETS, BECAUSE SCOPE IS NOT IN THE FILE. THIRD-PARTY.txt carries no scope column, so the
# distributed set is derived by DIFFERENCE: one run with `-Dlicense.excludedScopes=test,provided,
# system` (compile+runtime — what actually ships) and one with everything. Membership of the first
# is the scope answer; the second exists so nothing is omitted.
#
# Usage:
#   licence-report.sh --parse <third-party.txt>
#   licence-report.sh --report --all <f> --distributed <f> --map <f> [--provenance <text>]
#                     [--in-place <report.md>]
#
# Exit codes — each distinct, because "could not tell" must never render as "fine":
#   0 ok · 2 usage · 3 unreadable input · 4 a declared licence is absent from the map
#   5 the two input sets disagree (one run is stale)
#
# @author Navaneeth Mysore G
#
set -uo pipefail

BEGIN_MARK='<!-- GENERATED-BEGIN — do not edit by hand; see the regeneration command above -->'
END_MARK='<!-- GENERATED-END -->'

die() { echo "licence-report.sh: $2" >&2; exit "$1"; }

require_value() {
  [[ $# -ge 2 && -n "$2" ]] || die 2 "$1 needs a value"
}

# ── The parser ────────────────────────────────────────────────────────────────────────────────────
# A THIRD-PARTY.txt line is:  (LIC) (LIC) Artifact Name (groupId:artifactId:version - url)
# Licence names contain parentheses of their own, so the leading licence groups are consumed by
# BALANCED scanning, not by a regex. The trailing coordinate group is taken from the END of the
# line, which is the only position it can occupy.
parse_file() {
  [[ -r "$1" ]] || die 3 "cannot read '$1'"
  awk '
    BEGIN { licence_sep = sprintf("%c", 28) }
    function emit(gav, name, lics) { printf "%s\t%s\t%s\n", gav, name, lics }
    {
      line = $0
      sub(/^[ \t]+/, "", line); sub(/[ \t\r]+$/, "", line)
      if (line == "") next
      if (line !~ /^\(/) next                      # preamble ("Lists of N third-party dependencies.")

      # --- trailing coordinate group: the last "(" that opens a group running to end-of-line ---
      if (substr(line, length(line), 1) != ")") next
      depth = 0; open = 0
      for (i = length(line); i >= 1; i--) {
        c = substr(line, i, 1)
        if (c == ")") depth++
        else if (c == "(") { depth--; if (depth == 0) { open = i; break } }
      }
      if (open == 0) next
      coord = substr(line, open + 1, length(line) - open - 1)
      sub(/ - .*$/, "", coord)                      # drop " - <url>" / " - no url defined"
      if (coord !~ /^[^ :]+:[^ :]+:[^ ]+$/) next    # not a g:a:v group -> not a dependency line
      head = substr(line, 1, open - 1)
      sub(/[ \t]+$/, "", head)

      # --- leading licence groups, balanced ---
      lics = ""; p = 1; n = length(head)
      while (p <= n && substr(head, p, 1) == "(") {
        depth = 0; close_at = 0
        for (i = p; i <= n; i++) {
          c = substr(head, i, 1)
          if (c == "(") depth++
          else if (c == ")") { depth--; if (depth == 0) { close_at = i; break } }
        }
        if (close_at == 0) break                    # unbalanced -> stop, keep the rest as the name
        one = substr(head, p + 1, close_at - p - 1)
        lics = (lics == "" ? one : lics licence_sep one)
        p = close_at + 1
        while (p <= n && substr(head, p, 1) == " ") p++
      }
      name = substr(head, p)
      sub(/[ \t]+$/, "", name)
      emit(coord, name, lics)
    }
  ' "$1"
}

# A report input is not an arbitrary collection of parseable lines. The plugin declares its record
# count; require that header and reconcile it with the parser so truncation cannot become a clean
# zero-row report. Any dependency-looking line that did not parse is also fatal.
validate_input() {
  local source="$1" parsed="$2" label="$3" declared candidates actual
  declared="$(awk 'match($0,/Lists of [0-9]+ third-party dependencies\./) { s=substr($0,RSTART,RLENGTH); sub(/^Lists of /,"",s); sub(/ third-party dependencies\.$/,"",s); print s }' "$source")"
  [[ -n "$declared" && "$declared" != *$'\n'* ]] || die 3 "$label input has no single dependency-count header"
  candidates="$(awk '{ x=$0; sub(/^[ \t]+/,"",x); if (x ~ /^\(/) n++ } END{print n+0}' "$source")"
  actual="$(wc -l < "$parsed" | tr -d ' ')"
  [[ "$declared" -eq "$candidates" && "$declared" -eq "$actual" ]] \
    || die 3 "$label input is incomplete or malformed: header=$declared candidate-lines=$candidates parsed=$actual"
}

# ── Class ranking. A dependency offering several licences offers a CHOICE, so it is verdicted on its
#    BEST option. Reporting the worst option of a genuine choice manufactures a finding the licence
#    text itself refutes. UNRESOLVED ranks last so it can never mask a real option.
class_rank() {
  case "$1" in
    COMPATIBLE)   echo 1 ;;
    CONDITIONS)   echo 2 ;;
    CONTESTED)    echo 3 ;;
    INCOMPATIBLE) echo 4 ;;
    UNRESOLVED)   echo 5 ;;
    *)            echo 9 ;;
  esac
}

report() {
  local all="$1" dist="$2" map="$3" provenance="$4"
  for f in "$all" "$dist" "$map"; do [[ -r "$f" ]] || die 3 "cannot read '$f'"; done

  local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' RETURN
  parse_file "$all"  > "$tmp/all.tsv"  || return $?
  parse_file "$dist" > "$tmp/dist.tsv" || return $?
  validate_input "$all" "$tmp/all.tsv" "full"
  validate_input "$dist" "$tmp/dist.tsv" "distributed"
  cut -f1 "$tmp/dist.tsv" | LC_ALL=C sort -u > "$tmp/dist.gav"
  cut -f1 "$tmp/all.tsv"  | LC_ALL=C sort -u > "$tmp/all.gav"

  # A distributed entry missing from the full set means one of the two runs is stale. Guessing which
  # would report a dependency set that never existed on any single commit.
  local orphans; orphans="$(comm -23 "$tmp/dist.gav" "$tmp/all.gav")"
  [[ -z "$orphans" ]] || die 5 "distributed set has entries absent from the full set (one run is stale):
$orphans"

  # Scope inputs must describe the same dependency identically. Membership alone cannot detect a
  # stale run whose POM licence metadata changed between generations.
  local conflicts
  conflicts="$(awk -F'\t' 'NR==FNR { meta[$1]=$2 FS $3; next } ($1 in meta) && meta[$1] != ($2 FS $3) { print "  " $1 }' "$tmp/all.tsv" "$tmp/dist.tsv")"
  [[ -z "$conflicts" ]] || die 5 "distributed and full inputs disagree on dependency metadata:
$conflicts"

  # Map lookup. An unmapped declared string takes the WHOLE run down: a blank verdict cell in a
  # several-hundred-row table reads as "fine" to every reader who skims it.
  grep -v '^#' "$map" | grep -v '^[[:space:]]*$' > "$tmp/map.tsv"

  # THE MAP IS HAND-MAINTAINED, SO IT IS VALIDATED. A row missing its class column, or carrying a
  # class this script does not know, resolves to an empty verdict — the same blank cell the unmapped
  # -string refusal exists to prevent, reached through a different door, and the likelier door.
  local badmap
  badmap="$(awk -F'\t' '
      NF < 3 { printf "  line %d (\"%s\"): only %d tab-separated fields (need declared/spdx/class)\n", NR, $1, NF; next }
      $1 == "" || $2 == "" { printf "  line %d: declared name and SPDX must both be nonempty\n", NR; next }
      seen[$1]++ { printf "  line %d: duplicate declared-name key \"%s\"\n", NR, $1; next }
      $3 != "COMPATIBLE" && $3 != "CONDITIONS" && $3 != "CONTESTED" &&
      $3 != "INCOMPATIBLE" && $3 != "UNRESOLVED" {
        printf "  line %d: unrecognised class \"%s\" for \"%s\"\n", NR, $3, $1
      }' "$tmp/map.tsv")"
  [[ -z "$badmap" ]] || die 4 "map is malformed — every row needs declared/spdx/class and a known class:
$badmap"

  local missing=""
  local nolicence=""
  while IFS=$'\t' read -r gav name lics; do
    # An entry that declares NOTHING cannot be ruled on, and must not render as a blank verdict.
    # `useMissingFile=false` makes the plugin write "(Unknown license)" instead, which the map DOES
    # rule on as UNRESOLVED — so this is the defence for the case that should not happen.
    [[ -n "$lics" ]] || { nolicence+="$gav"$'\n'; continue; }
    # `read -a` and NOT an unquoted $( ) word-split: a licence name is arbitrary publisher text
    # and may contain a glob metacharacter, which word-splitting would expand against the CWD.
    local -a arr; IFS=$'\034' read -r -a arr <<< "$lics"
    local l
    for l in "${arr[@]}"; do
      # ENVIRON, not -v: awk expands escape sequences in a -v value, so a licence name containing
      # a backslash-t would silently become a TAB and never match its own map row — a refusal the
      # user cannot clear by editing the map, because the lookup is what is broken.
      k="$l" awk -F'\t' '$1==ENVIRON["k"]{found=1} END{exit found?0:1}' "$tmp/map.tsv" \
        || missing+="$l"$'\n'
    done
  done < "$tmp/all.tsv"
  [[ -z "$nolicence" ]] || die 4 "dependencies parsed with NO declared licence at all — the input is
malformed, or the plugin ran with useMissingFile=true:
$nolicence"
  missing="$(printf '%s' "$missing" | LC_ALL=C sort -u)"
  [[ -z "$missing" ]] || die 4 "declared licence string(s) absent from the map — add a ruling for each, do not leave a blank verdict:
$missing"

  # ── build the rows ──
  : > "$tmp/rows.tsv"
  while IFS=$'\t' read -r gav name lics; do
    # `read -a` and NOT an unquoted $( ) word-split: a licence name is arbitrary publisher text
    # and may contain a glob metacharacter, which word-splitting would expand against the CWD.
    local -a arr; IFS=$'\034' read -r -a arr <<< "$lics"
    local best_rank=9 best_class="" best_spdx="" spdx_all="" l option_count=0
    for l in "${arr[@]}"; do
      local row spdx cls
      row="$(k="$l" awk -F'\t' '$1==ENVIRON["k"]{print $2"\t"$3; exit}' "$tmp/map.tsv")"
      spdx="${row%%$'\t'*}"; cls="${row##*$'\t'}"
      spdx_all="${spdx_all:+$spdx_all OR }$spdx"
      option_count=$((option_count + 1))
      local r; r="$(class_rank "$cls")"
      if (( r < best_rank )); then best_rank=$r; best_class="$cls"; best_spdx="$spdx"; fi
    done
    # THIRD-PARTY.txt lists names but does not encode whether multiple licences are alternatives or
    # cumulative obligations. Without artifact-specific relationship evidence, do not elect the
    # most permissive option.
    if (( option_count > 1 )); then
      best_class="UNRESOLVED"
      best_spdx="NOASSERTION"
      spdx_all="NOASSERTION (multiple declarations; relationship unverified)"
    fi
    local scope="test/provided only"
    # `--` because a groupId may start with "-": without it grep parses the GAV as an OPTION,
    # exits 2, the && fails, and the row silently keeps its default scope — a DISTRIBUTED
    # dependency reported as test-only, which is the licence report lying about the one column
    # that decides whether a licence matters at all.
    grep -qxF -- "$gav" "$tmp/dist.gav" && scope="compile/runtime"
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$gav" "$name" "${lics//$'\034'/ · }" "$spdx_all" "$best_spdx" "$scope" "$best_class" >> "$tmp/rows.tsv"
  done < "$tmp/all.tsv"
  # LC_ALL=C: byte order, so the committed report does not change with the ambient locale.
  LC_ALL=C sort -t$'\t' -k1,1 -o "$tmp/rows.tsv" "$tmp/rows.tsv"

  # ── derived totals — never typed ──
  local total dist_n test_n
  # awk emits canonical decimal counts. `wc -l` pads on BSD/macOS but not GNU/Linux, which made a
  # report regenerated locally differ from the same report generated by CI despite identical rows.
  total=$(awk 'END { print NR + 0 }' "$tmp/rows.tsv")
  dist_n=$(awk -F'\t' '$6=="compile/runtime" { n++ } END { print n + 0 }' "$tmp/rows.tsv")
  test_n=$(( total - dist_n ))

  echo "$BEGIN_MARK"
  echo
  echo "### Resolved set"
  echo
  [[ -n "$provenance" ]] && { echo "$provenance"; echo; }
  echo "- **Total dependencies enumerated**: $total"
  echo "- **On a distributed (compile/runtime) path**: $dist_n"
  echo "- **Test/provided scope only — not distributed**: $test_n"
  echo
  echo "| Verdict | All | On a distributed path |"
  echo "|---|---:|---:|"
  local c
  for c in COMPATIBLE CONDITIONS CONTESTED INCOMPATIBLE UNRESOLVED; do
    printf '| `%s` | %s | %s |\n' "$c" \
      "$(awk -F'\t' -v c="$c" '$7==c { n++ } END { print n + 0 }' "$tmp/rows.tsv")" \
      "$(awk -F'\t' -v c="$c" '$7==c && $6=="compile/runtime" { n++ } END { print n + 0 }' "$tmp/rows.tsv")"
  done
  echo

  emit_bucket() {   # <class> <heading> <marker-or-empty>
    local cls="$1" heading="$2" marker="$3"
    echo "### $heading"
    echo
    [[ -n "$marker" ]] && echo "<!-- ${marker}-BEGIN -->"
    local n; n=$(awk -F'\t' -v c="$cls" '$7==c { n++ } END { print n + 0 }' "$tmp/rows.tsv")
    if [[ $n -eq 0 ]]; then
      echo "_None._"
    else
      echo "| Dependency | Declared licence(s) | SPDX (elected) | Scope |"
      echo "|---|---|---|---|"
      awk -F'\t' -v c="$cls" '
      # A pipe in third-party metadata opens a new markdown cell and shifts every column after
      # it, so the Verdict cell renders whatever lands there — on a table that still LOOKS fine.
      function md(s) { gsub(/&/, "\\&amp;", s); gsub(/</, "\\&lt;", s); gsub(/>/, "\\&gt;", s); gsub(/\|/, "\\|", s); return s }
      $7==c { printf "| `%s` | %s | `%s` | %s |\n", md($1), md($3), md($5), md($6) }' "$tmp/rows.tsv"
    fi
    [[ -n "$marker" ]] && echo "<!-- ${marker}-END -->"
    echo
  }
  emit_bucket INCOMPATIBLE "Incompatible shortlist — for the owner to rule on" "INCOMPATIBLE-SHORTLIST"
  emit_bucket CONTESTED    "Contested — compatibility depends on linking and distribution" "CONTESTED-LIST"
  emit_bucket UNRESOLVED   "Unresolved — POM metadata declares no usable licence" "UNRESOLVED-LIST"

  echo "### Every dependency"
  echo
  echo "| Dependency | Name | Declared licence(s) | SPDX (elected) | Scope | Verdict |"
  echo "|---|---|---|---|---|---|"
  awk -F'\t' '
      # A pipe in third-party metadata opens a new markdown cell and shifts every column after
      # it, so the Verdict cell renders whatever lands there — on a table that still LOOKS fine.
      function md(s) { gsub(/&/, "\\&amp;", s); gsub(/</, "\\&lt;", s); gsub(/>/, "\\&gt;", s); gsub(/\|/, "\\|", s); return s }
    { printf "| `%s` | %s | %s | `%s` | %s | `%s` |\n", md($1), md($2), md($3), md($5), md($6), md($7) }' "$tmp/rows.tsv"
  echo
  echo "$END_MARK"
}

# ── CLI ───────────────────────────────────────────────────────────────────────────────────────────
MODE=""; ALL=""; DIST=""; MAP=""; PARSE=""; PROV=""; INPLACE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --parse)       require_value "$@"; MODE=parse; PARSE="$2"; shift 2 ;;
    --report)      MODE=report; shift ;;
    --all)         require_value "$@"; ALL="$2"; shift 2 ;;
    --distributed) require_value "$@"; DIST="$2"; shift 2 ;;
    --map)         require_value "$@"; MAP="$2"; shift 2 ;;
    --provenance)  require_value "$@"; PROV="$2"; shift 2 ;;
    --in-place)    require_value "$@"; INPLACE="$2"; shift 2 ;;
    *) die 2 "unknown argument '$1'" ;;
  esac
done

case "$MODE" in
  parse)  [[ -n "$PARSE" ]] || die 2 "--parse needs a file"; parse_file "$PARSE" ;;
  report)
    [[ -n "$ALL" && -n "$DIST" && -n "$MAP" ]] || die 2 "--report needs --all, --distributed and --map"
    if [[ -n "$INPLACE" ]]; then
      [[ -r "$INPLACE" ]] || die 3 "cannot read '$INPLACE'"
      marker_state="$(awk -v b="$BEGIN_MARK" -v e="$END_MARK" '
        $0==b { bc++; if (!bp) bp=NR }
        $0==e { ec++; if (!ep) ep=NR }
        END { print bc+0, ec+0, bp+0, ep+0 }' "$INPLACE")"
      [[ "$marker_state" == "1 1 "* ]] || die 3 "'$INPLACE' needs exactly one line-anchored GENERATED marker pair"
      set -- $marker_state
      [[ "$3" -lt "$4" ]] || die 3 "'$INPLACE' has reversed GENERATED markers"
      out="$(mktemp)" || die 3 "cannot create replacement temporary file"
      genf="$(mktemp)" || { rm -f "$out"; die 3 "cannot create generated temporary file"; }
      report "$ALL" "$DIST" "$MAP" "$PROV" > "$genf" || { rc=$?; rm -f "$out" "$genf"; exit "$rc"; }
      # 🔴 THE GENERATED BLOCK GOES IN THROUGH A FILE, NEVER THROUGH awk -v. awk processes escape
      # sequences in a -v value, so a backslash anywhere in an artifact name or licence string would
      # be silently rewritten on the way in — and the committed report would then differ from what
      # the generator produces, making `git diff --exit-code` report drift that no edit can clear.
      awk -v b="$BEGIN_MARK" -v e="$END_MARK" -v gf="$genf" '
        $0 == b { while ((getline line < gf) > 0) print line; close(gf); skip = 1; replaced++; next }
        skip && $0 == e { skip = 0; next }
        !skip { print }
        END { if (skip || replaced != 1) exit 7 }
      ' "$INPLACE" > "$out" || { rc=$?; rm -f "$out" "$genf"; die 3 "failed to construct replacement (awk rc=$rc)"; }
      "${LICENCE_REPORT_MV:-mv}" "$out" "$INPLACE" || { rc=$?; rm -f "$out" "$genf"; die 3 "failed to replace '$INPLACE' (move rc=$rc)"; }
      rm -f "$genf" || die 3 "failed to remove generated temporary file"
      echo "licence-report.sh: regenerated $INPLACE" >&2
    else
      report "$ALL" "$DIST" "$MAP" "$PROV"
    fi
    ;;
  *) die 2 "usage: licence-report.sh --parse <f> | --report --all <f> --distributed <f> --map <f>" ;;
esac
