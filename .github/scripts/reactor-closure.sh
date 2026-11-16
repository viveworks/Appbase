#!/usr/bin/env bash
#
# reactor-closure.sh — expand changed modules through two directed phases before Maven applies its
# prerequisite-only `-am` backstop (internal work item 2655 PR#3 maven-compile RED). Never reapply `-amd` to the result.
#
# Maven's own -am ("also make": build what the -pl list depends on) and -amd ("also make dependents":
# build what depends on the -pl list) each expand the ORIGINAL -pl list exactly ONCE, in one direction,
# against the whole reactor's dependency graph. They do not re-run the OTHER expansion for a module
# that one of them pulls in. A module -amd adds because it depends on something in the original list
# can itself have a second, parallel dependency on a THIRD module with no relationship at all to the
# original list -- -am never sees that third module (it only expanded from the original list, not from
# the -amd-added one), so it is never added, and the build fails resolving it as a never-published
# internal SNAPSHOT jar.
#
# Measured directly against the real failure (viveworks/Appbase PR#3, run 41148350374, job
# "maven-compile"): $MODULES was the single module appbase-java/appbase-core. -amd correctly pulled in
# appbase-service-core (depends on appbase-core) and, transitively, appbase-service-web-oauth-reactive
# (depends on appbase-service-core) -- but appbase-service-web-oauth-reactive ALSO directly depends on
# appbase-service-web-oauth, a sibling module with NO dependency on appbase-core at all, which neither
# -am nor -amd ever added. `mvn ... -pl appbase-java/appbase-core -am -amd` then failed:
#   Could not resolve dependencies for project appbase-service-web-oauth-reactive:
#   dependency: appbase-service-web-oauth:jar:1.0.0 (compile)
#   prefixes: Path .../appbase-service-web-oauth-1.0.0.jar NOT allowed from central
# Reproduced with a 4-module synthetic reactor shaped exactly like that (core / service-core / web-oauth
# / web-oauth-reactive -> {service-core, web-oauth}): `mvn -pl core -am -amd compile` fails identically
# ("Could not find artifact ...:web-oauth:jar:1.0.0"). This script first computes the
# downstream-consumer fixpoint from the ORIGINAL seeds, then computes the prerequisite fixpoint from
# that result. It never feeds newly added prerequisites back through the consumer phase: that would
# pull unrelated sibling consumers into the build. The result is prerequisite-closed, so Maven `-am`
# is normally idempotent; retaining it provides a native prerequisite backstop for parser omissions.
#
# Usage: .github/scripts/reactor-closure.sh <comma-separated-module-dirs> [repo-root]
# Prints the expanded comma-separated module-dir list on stdout, sorted, de-duplicated, exit 0.
# ALL/NONE pass through unchanged (the sentinels changed-modules.sh itself can emit).
# Exit 2, nothing on stdout: no module poms were discovered, or a seed directory has no pom.xml.
#
# Pure bash 3.2 + awk + coreutils: no bash-4-only associative arrays, "is variable set" tests, or
# lowercasing parameter expansions.
#
# @author Navaneeth Mysore G
set -euo pipefail

die() { echo "reactor-closure: ERROR — $*" >&2; exit 2; }

# `${1:?...}` alone would route an EMPTY string through bash's own "parameter null or unset" error
# (exit 1, bypassing die()) rather than this script's documented exit-2 contract -- an unset $1 and an
# empty $1 must fail the same documented way, so both are checked explicitly before any ${1:?...} use.
[[ $# -ge 1 ]] || die "usage: reactor-closure.sh <comma-separated-module-dirs> [repo-root]"
seed_csv="$1"
[[ -n "$seed_csv" ]] || die "usage: reactor-closure.sh <comma-separated-module-dirs> [repo-root] -- got an empty module list"
root="${2:-.}"

case "$seed_csv" in
  ALL | NONE) echo "$seed_csv"; exit 0 ;;
esac

tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT

# 1. Parse every pom.xml under $root (skipping build output) into SELF (a module's own artifactId)
#    and DEP (an internal com.viveworks.appbase <dependency> it declares) records, one dir at a time.
#    Each pom.xml here is written one element per line (no nested tags on one line), matching every
#    pom in this repo; a state machine over <parent>/<dependency> blocks is enough, no real XML parser.
: > "$tmp/all.tsv"
while IFS= read -r pom; do
  dir="$(dirname "$pom")"
  rel="${dir#"$root"/}"
  [[ "$dir" == "$root" ]] && rel="."
  awk -v dir="$rel" '
    BEGIN { in_parent = 0; in_dep_mgmt = 0; in_dep = 0; in_excl = 0; got_own = 0; dep_group = ""; dep_artifact = "" }
    /<parent>/      { in_parent = 1; next }
    /<\/parent>/    { in_parent = 0; next }
    # <dependencyManagement> pins versions for CONSUMERS -- it does not make the declaring module
    # itself need those artifacts built. appbase-bom/pom.xml is exactly this: a packaging=pom BOM
    # whose <dependencyManagement> lists ~40 of this repos modules as pinned versions. Treating
    # those as real DEP edges (as a dependency-block-only guard would) manufactures a false
    # "appbase-bom depends on X" edge for every one of them, which the fixpoint then uses to pull
    # appbase-bom itself into the set the moment ANY one of those ~40 modules is seeded -- and once
    # appbase-bom is in, pass (a) sweeps all ~40 back in, collapsing the closure to nearly the whole
    # reactor. Never enter the real <dependency> state machine while inside <dependencyManagement>.
    /<dependencyManagement>/     { in_dep_mgmt = 1; next }
    /<\/dependencyManagement>/   { in_dep_mgmt = 0; next }
    !in_dep_mgmt && /<dependency>/  { in_dep = 1; in_excl = 0; dep_group = ""; dep_artifact = ""; next }
    /<\/dependency>/ {
      if (in_dep && dep_group == "com.viveworks.appbase" && dep_artifact != "")
        print "DEP\t" dir "\t" dep_artifact
      in_dep = 0; in_excl = 0; next
    }
    # A <dependency> may carry its own <exclusions><exclusion> block, each with ITS OWN groupId/
    # artifactId. Those are a different artifact entirely -- the one being excluded, not the one
    # being depended on -- so they must never be allowed to overwrite dep_group/dep_artifact once
    # the real <groupId>/<artifactId> pair has already been read. Skipping lines inside <exclusion>
    # is enough: the enclosing <exclusions> tag itself never matches /<groupId>/ or /<artifactId>/.
    in_dep && /<exclusion>/  { in_excl = 1; next }
    in_dep && /<\/exclusion>/ { in_excl = 0; next }
    in_dep && in_excl { next }
    in_dep && /<groupId>/ {
      line = $0; gsub(/.*<groupId>/, "", line); gsub(/<\/groupId>.*/, "", line); dep_group = line; next
    }
    in_dep && /<artifactId>/ {
      line = $0; gsub(/.*<artifactId>/, "", line); gsub(/<\/artifactId>.*/, "", line); dep_artifact = line; next
    }
    !in_parent && !in_dep && !in_dep_mgmt && !got_own && /<artifactId>/ {
      line = $0; gsub(/.*<artifactId>/, "", line); gsub(/<\/artifactId>.*/, "", line)
      print "SELF\t" dir "\t" line
      got_own = 1; next
    }
  ' "$pom" >> "$tmp/all.tsv"
done < <(find "$root" -name pom.xml -not -path '*/target/*')

grep '^SELF' "$tmp/all.tsv" | cut -f2,3 > "$tmp/self.tsv" || true     # dir \t artifactId
grep '^DEP'  "$tmp/all.tsv" | cut -f2,3 > "$tmp/deps_raw.tsv" || true # dir \t depArtifactId

[[ -s "$tmp/self.tsv" ]] || die "no module poms discovered under '$root'"

# 2. Resolve each dependency's artifactId to its module directory. An artifactId that never matches
#    a SELF record is an external dependency (e.g. a third-party jar) and is simply dropped -- it is
#    not a reactor module, so it cannot be a `-pl` entry.
: > "$tmp/edges.tsv"
while IFS=$'\t' read -r depender_dir dep_artifact; do
  dep_dir="$(awk -F'\t' -v a="$dep_artifact" '$2==a{print $1; exit}' "$tmp/self.tsv")"
  [[ -n "$dep_dir" ]] && printf '%s\t%s\n' "$depender_dir" "$dep_dir" >> "$tmp/edges.tsv"
done < "$tmp/deps_raw.tsv"

# 3. Two SEPARATE directed fixpoints, run in sequence, never mixed into one symmetric pass.
#
#    A single pass that adds BOTH directions on every iteration (dependencies of anything in the
#    set, AND dependents of anything in the set) computes the whole UNDIRECTED connected
#    component, not the directed closure Maven's -am/-amd semantics actually need: the moment a
#    true downstream consumer (added via the dependents direction) pulls in a shared prerequisite
#    (added via the dependencies direction), the NEXT pass re-applies the dependents direction to
#    that shared prerequisite too, and pulls in every OTHER, unrelated consumer of it -- a module
#    with no path at all from the original seed. Reproduced directly: seed `a`; `b` depends on
#    `a` and on `shared`; `unrelated` depends only on `shared`. The one-pass-mixed algorithm
#    returns `a,b,shared,unrelated` -- `unrelated` must never be in this answer.
#
#    Phase (a) -- also-make-dependents, fixpoint, ONE direction only: starting from the seed,
#    repeatedly add whatever depends on something already in the set, until stable. This finds
#    every module that needs rebuilding because it transitively consumes a changed module --
#    exactly what -amd does, but as a real fixpoint instead of Maven's single non-recursive pass.
#
#    Phase (b) -- also-make, fixpoint, the OTHER direction only, seeded from phase (a)'s full
#    result: repeatedly add whatever anything already in the set depends on, until stable. This
#    gives every downstream consumer found in phase (a) its OWN prerequisites (e.g. `shared`),
#    exactly what -am does for the original seed, but reapplied to every module phase (a) added.
#
#    Phase (b) is NOT looped back into phase (a). A module added in phase (b) (a shared
#    prerequisite like `shared`) is a dependency the set NEEDS present to resolve -- it is not a
#    new root whose own unrelated consumers (`unrelated`) must also be rebuilt. Re-running phase
#    (a) after phase (b) is exactly the undirected-component bug above, so the fixpoint stops
#    after one phase (a) pass and one phase (b) pass.
: > "$tmp/set.txt"
oldifs="$IFS"; IFS=','
for s in $seed_csv; do
  [[ -d "$root/$s" ]] || die "seed module directory '$s' does not exist under '$root'"
  [[ -f "$root/$s/pom.xml" ]] || die "seed module directory '$s' has no pom.xml under '$root'"
  echo "$s" >> "$tmp/set.txt"
done
IFS="$oldifs"
sort -u -o "$tmp/set.txt" "$tmp/set.txt"

# Phase (a): also-make-dependents fixpoint (reverse edges only: col2 in set -> add col1).
while :; do
  cp "$tmp/set.txt" "$tmp/set.before.txt"
  if [[ -s "$tmp/edges.tsv" ]]; then
    awk -F'\t' 'NR==FNR{want[$1]=1;next} want[$2]{print $1}' "$tmp/set.txt" "$tmp/edges.tsv" >> "$tmp/set.txt"
  fi
  sort -u -o "$tmp/set.txt" "$tmp/set.txt"
  cmp -s "$tmp/set.before.txt" "$tmp/set.txt" && break
done

# Phase (b): also-make fixpoint (forward edges only: col1 in set -> add col2). Seeded from the
# full result of phase (a), run exactly once to a fixpoint, never re-entering phase (a).
while :; do
  cp "$tmp/set.txt" "$tmp/set.before.txt"
  if [[ -s "$tmp/edges.tsv" ]]; then
    awk -F'\t' 'NR==FNR{want[$1]=1;next} want[$1]{print $2}' "$tmp/set.txt" "$tmp/edges.tsv" >> "$tmp/set.txt"
  fi
  sort -u -o "$tmp/set.txt" "$tmp/set.txt"
  cmp -s "$tmp/set.before.txt" "$tmp/set.txt" && break
done

paste -sd, "$tmp/set.txt"
