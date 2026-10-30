#!/usr/bin/env bash
#
# changed-modules.sh — which Maven modules does a diff touch? (internal work item 2424, CI changed-module compile)
#
# Usage: .github/scripts/changed-modules.sh [--github-output <file>] <base-sha> <head-sha>
#
# Prints ONE line on stdout, exit 0:
#   ALL                     the whole reactor must be compiled (build tooling, a parent/root pom, or a
#                           module added/removed changed; the coverage self-check in build.sh must run)
#   NONE                    the diff was computed and has no Java source and no pom.xml change
#   dir/a,dir/b             repo-relative module directories for `mvn -pl <list> -am`
# Exit 2, nothing on stdout: the base or head is unknown/unreachable, or `git diff` failed. NEVER NONE
# on an error — NONE skips the compile, so an error that printed NONE would be a false green.
# Reasons go to stderr, so the CI log says WHY a run chose ALL.
#
# Pure git: it reads blobs at <head-sha> (and <base-sha> for deletions), never the working tree, so it
# can be run against any two commits from any checkout of this repo.
# Focused selector regression coverage pins this behavior.
#
# @author Navaneeth Mysore G
# @author Claude
set -euo pipefail

github_output=""
if [[ "${1:-}" == --github-output ]]; then
  github_output="${2:?--github-output requires a file}"
  shift 2
fi
base="${1:?usage: changed-modules.sh [--github-output file] <base-sha> <head-sha>}"
head="${2:?usage: changed-modules.sh [--github-output file] <base-sha> <head-sha>}"

die() { echo "changed-modules: ERROR — $*" >&2; exit 2; }

for rev in "$base" "$head"; do
  git cat-file -e "$rev^{commit}" 2>/dev/null \
    || die "'$rev' is not a commit in this clone (unknown SHA, or history not fetched: fetch-depth 0?)"
done

# Captured into a variable, NOT read through `< <(git diff …)`: `set -e` cannot see a failure inside a
# process substitution, and that is exactly how an unreachable base used to print NONE (internal work item 2500 F3).
# core.quotePath=false keeps non-ASCII paths literal instead of C-quoting them (internal work item 2500 F7).
diff_out="$(git -c core.quotePath=false diff --name-status --no-renames "$base...$head")" \
  || die "git diff $base...$head failed (no merge base in this clone?)"

# Build tooling whose change can alter what `./build.sh --mode compile` means for EVERY module.
is_build_tooling() {
  case "$1" in
    pom.xml | build.sh | .mvn/* | .github/scripts/changed-modules.sh) return 0 ;;
  esac
  return 1
}

has_blob() { git cat-file -e "$1:$2" 2>/dev/null; }

is_parent_pom() { # <rev> <path>: an aggregator/parent pom changes the build of every child
  # Whitespace-insensitive, so `<packaging>\n  pom\n</packaging>` counts too (internal work item 2500 F11).
  git show "$1:$2" 2>/dev/null | tr -d '[:space:]' | grep -q '<packaging>pom</packaging>'
}

# Live StackTestMain ARM surfaces from the accepted internal work item 2537 matrix. REUSE and OMIT-R2 modules keep
# their own lower-level gates and must not make an ordinary public PR pay for the full live stack.
is_stacktest_covered() {
  case "$1" in
    appbase-stacktest/* | appbase-java/appbase-bootstrap/* | appbase-java/appbase-core/* | \
    appbase-java/appbase-client/appbase-client-aerospike/* | \
    appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws-common/* | \
    appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws-httpclient/* | \
    appbase-java/appbase-client/appbase-client-cloud/appbase-client-cloud-aws-s3/* | \
    appbase-java/appbase-client/appbase-client-druid/* | \
    appbase-java/appbase-client/appbase-client-elasticsearch/* | \
    appbase-java/appbase-client/appbase-client-firebase/* | \
    appbase-java/appbase-client/appbase-client-jdbc/* | \
    appbase-java/appbase-client/appbase-client-jdbi/* | \
    appbase-java/appbase-client/appbase-client-redis/* | \
    appbase-java/appbase-client/appbase-client-vitess/* | \
    appbase-java/appbase-client/appbase-client-zookeeper/* | \
    appbase-java/appbase-service/appbase-service-core/* | \
    appbase-java/appbase-service/appbase-service-supportmessage/* | \
    appbase-java/appbase-service/appbase-service-user-account-firebaseauth/* | \
    appbase-java/appbase-service/appbase-service-user-account-jdbi/* | \
    appbase-java/appbase-service/appbase-service-user-session-aerospike/* | \
    appbase-java/appbase-service/appbase-service-user/* | \
    appbase-java/appbase-service/appbase-service-web/* | \
    appbase-java/appbase-service/appbase-service-web-oauth/* | \
    appbase-java/appbase-service/appbase-service-web-oauth-apple/* | \
    appbase-java/appbase-service/appbase-service-web-oauth-github/* | \
    appbase-java/appbase-service/appbase-service-web-oauth-google/* | \
    appbase-java/appbase-service/appbase-service-web-oauth-microsoft/* | \
    appbase-java/appbase-service/appbase-service-web-oauth-okta/* | \
    appbase-java/appbase-service/appbase-service-web-oauth-reactive/*) return 0 ;;
  esac
  return 1
}

run_shell_suites=false
run_console_suite=false
run_emulator=false
run_stacktest=false
write_selectors() {
  [[ -z "$github_output" ]] && return
  {
    echo "run_shell_suites=$run_shell_suites"
    echo "run_console_suite=$run_console_suite"
    echo "run_emulator=$run_emulator"
    echo "run_stacktest=$run_stacktest"
  } >> "$github_output"
}
all() {
  run_shell_suites=true
  run_console_suite=true
  run_emulator=true
  run_stacktest=true
  write_selectors
  echo "changed-modules: ALL — $*" >&2
  echo ALL
  exit 0
}

modules=()
while IFS=$'\t' read -r status path; do
  [[ -z "$path" ]] && continue
  # A path git still had to quote (a tab, newline or quote in its name) cannot be mapped reliably.
  [[ "$path" == \"* ]] && all "unmappable quoted path: $path"
  case "$path" in
    scripts/* | appbase_mcp/* | */infra/docker/* | development-environment/* | .github/workflows/ci-verify.yml) run_shell_suites=true ;;
  esac
  case "$path" in
    appbase_mcp/generator/* | scripts/generate-mcp-api-surface.sh | scripts/verify-mcp-api-surface.*)
      all "MCP API generator/oracle changed: $path"
      ;;
  esac
  case "$path" in
    appbase-config-wizard/* | appbase-java/*) run_console_suite=true ;;
  esac
  case "$path" in
    config/* | */src/main/resources/* | */configuration/*) run_emulator=true ;;
  esac
  is_stacktest_covered "$path" && run_stacktest=true
  is_build_tooling "$path" && all "build tooling changed: $path"

  case "$path" in
    pom.xml | */pom.xml)
      # Adding or removing a module is exactly what build.sh's coverage self-check exists to catch
      # (internal work item 401/internal work item 402/internal work item 404), and only the whole-repo gate runs that check.
      [[ "$status" == A || "$status" == D ]] && all "module added/removed: $path ($status)"
      rev="$head"; [[ "$status" == D ]] && rev="$base"
      is_parent_pom "$rev" "$path" && all "parent/aggregator pom changed: $path"
      ;;
    */pubspec.yaml)
      [[ "$status" == A || "$status" == D ]] && all "Flutter package added/removed: $path ($status)"
      continue
      ;;
    *.java)
      # Public interfaces and annotations feed the generated MCP API artifact. Force a complete
      # reactor compile so the independent javap oracle has every class and cannot pass partially.
      if { has_blob "$head" "$path" && git show "$head:$path" | grep -Eq 'public[[:space:]]+(@interface|interface)[[:space:]]'; } \
        || { has_blob "$base" "$path" && git show "$base:$path" | grep -Eq 'public[[:space:]]+(@interface|interface)[[:space:]]'; }; then
        all "generated MCP API source changed: $path"
      fi
      ;;
    *) continue ;;
  esac

  # Nearest ancestor directory that has a pom.xml at <head>.
  dir="$(dirname "$path")"
  while [[ "$dir" != "." ]] && ! has_blob "$head" "$dir/pom.xml"; do
    dir="$(dirname "$dir")"
  done
  [[ "$dir" == "." ]] && all "file outside every module: $path"
  modules+=("$dir")
done <<<"$diff_out"

if [[ ${#modules[@]} -eq 0 ]]; then
  write_selectors
  echo "changed-modules: NONE — no Java or pom.xml change" >&2
  echo NONE
  exit 0
fi

list="$(printf '%s\n' "${modules[@]}" | sort -u | paste -sd, -)"
echo "changed-modules: $list" >&2
write_selectors
echo "$list"
