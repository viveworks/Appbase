#!/usr/bin/env bash
set -euo pipefail

# The repo-root build entrypoint.
#
# This script living at the REPO ROOT is load-bearing, not cosmetic. It used to live in appbase-java/
# and `cd` there, so its reactor was only core/client/service/bootstrap — the five root siblings
# (devcontainers, config-wizard, cloud, stacktest, bom) were compiled by NO mode at all. A rename in
# P-027 R1 broke appbase-stacktest and every gate stayed green (internal work item 401, internal work item 402, internal work item 404).

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Buildable trees DELIBERATELY outside every gate.
#
# This list exists so that "not built" is a STATEMENT rather than an accident. An orphaned tree and a
# deliberately-excluded one look identical from inside the build, and that ambiguity is precisely how
# internal work item 404 survived. `compile` cross-checks this list against what is actually on disk and fails if it
# finds a buildable tree in neither the reactor nor this list.
EXCLUDED_TREES=(
  "appbase-codeagents"  # agent configuration only — no build surface
  "examples"  # standalone external-consumer examples, each resolves 1.0.0 from Central, not the reactor
)

MODE="quick"
MODULES=""
FRONTEND_ON=0
WANT_FRONTEND=0   # did the USER ask for the frontend? (distinct from staging/release implying it)

usage() {
  cat >&2 <<'EOF'
Usage: ./build.sh [--mode MODE] [--modules PATHS] [--wizard-frontend]
       ./build.sh MODE [--modules PATHS] [--wizard-frontend]    # positional mode also accepted

  --mode              compile | quick | develop | staging | release      (default: quick)
  --modules           comma-separated module PATHS; default = everything (Maven -pl … -am)
  --wizard-frontend   also build appbase-config-wizard's Flutter web UI  (implied by staging/release)

Modes:
  compile  WHOLE-REPO test-compile, no tests run. REQUIRED before every PR.
  quick    Fast feedback: unit tests only, no coverage threshold (default)
  develop  Full build with coverage + checkstyle/spotbugs gate — use before pushing
  staging  Everything develop does, plus appbase-stacktest and the config-wizard IT suite
  release  Everything staging does, plus javadoc/DOAP/versions-check, strict rules, no SNAPSHOTs

--modules takes PATHS, not artifactIds — paths tab-complete, so there is nothing to guess:
  ./build.sh --mode quick --modules appbase-java/appbase-core
  ./build.sh --mode quick --modules appbase-java/appbase-client/appbase-client-redis,appbase-java/appbase-client/appbase-client-cloud
  ./build.sh --mode quick --modules appbase-config-wizard/frontend   # the wizard + its web UI
Dependencies are resolved from the POMs (-am); you never hand-list them.

--modules is for the INNER LOOP only. `compile` rejects it: compile is the pre-PR gate that exists
because a build silently covered less than it claimed, so it must never be narrowable.

Flutter is ALWAYS OPT-IN. appbase-config-wizard.skipFrontend defaults to true (internal work item 406) so that
`flutter` is not a hard prerequisite for compiling any Java here — a bare CI runner, or the P-046
devcontainer (JDK + Maven only), must be able to build. Only staging/release turn it on implicitly,
because there a UI-less jar is a SHIPPING defect rather than a slow build.

NOT BUILT BY ANY MODE (deliberate — stated so it cannot be mistaken for a gap):
  appbase-codeagents/  agent configuration only — no build surface

Integration tests (all disabled by default):
  AEROSPIKE_CLIENT_SERVICE_TESTS_ENABLED=true ./build.sh develop
  ELASTICSEARCH_CLIENT_SERVICE_TESTS_ENABLED=true ./build.sh develop
  KAFKA_CLIENT_SERVICE_TESTS_ENABLED=true ./build.sh develop
  (see INT_FLAGS in this script for the full list)
EOF
}

# `shift 2` on a flag with no value would fail under `set -e` and kill the script with NO output at
# all — so demand the value explicitly and say what is missing. (usage() must already be defined:
# these run at parse position, and a function called before its definition is a 127, not an error.)
need_value() {
  [[ $# -ge 2 && "$2" != -* ]] && return 0
  echo "ERROR: $1 requires a value." >&2
  echo "" >&2
  usage
  exit 1
}

# `./build.sh develop` (positional) still works alongside `--mode develop`. The positional form is in
# the docs, in the P-023 CI plan and in muscle memory; breaking it would buy nothing.
if [[ $# -gt 0 && "$1" != -* ]]; then
  MODE="$1"
  shift
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)             need_value "$@"; MODE="$2";    shift 2 ;;
    --modules)          need_value "$@"; MODULES="$2"; shift 2 ;;
    --wizard-frontend)  FRONTEND_ON=1; WANT_FRONTEND=1; shift ;;
    -h|--help)          MODE="help";      shift   ;;
    *) echo "ERROR: unknown option '$1'" >&2; echo "" >&2; MODE="help"; break ;;
  esac
done

if [[ "$MODE" == "help" ]]; then usage; exit 1; fi

# The bounded manifest coverage gate below is implemented with rg. Name the missing tool before any
# inventory scan or build work so a clean machine gets the documented remedy, not a late exit 127.
if ! command -v rg >/dev/null 2>&1; then
  echo "ERROR: ripgrep is a minimum prerequisite for ./build.sh." >&2
  echo "       See README.md prerequisite and installation instructions." >&2
  echo '       After installing it, verify with `rg --version`, then re-run.' >&2
  exit 1
fi

# Fast documentation-contract preflight. This is deliberately before the JDK and Maven work: a stale
# public inventory must block every build mode without spending build resources first.
bash "$SCRIPT_DIR/scripts/api-inventory-fast-check.sh" </dev/null

# Fail FAST on the wrong toolchain (AUD-005). Without this, a wrong JDK surfaces only as Maven's
# cryptic "release version 25 not supported" deep into the build — avoidable onboarding friction. This
# is a read-only preflight: no behavior change on a correct JDK. Escape hatch: APPBASE_SKIP_JDK_CHECK=1
# (for a JDK whose `-version` output this cannot parse). Defined and called HERE, before any build
# work, so the error arrives first and actionable rather than last and cryptic.
assert_jdk_version() {
  [[ "${APPBASE_SKIP_JDK_CHECK:-0}" == "1" ]] && return 0
  local want=25 have
  if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: no 'java' on PATH. This build needs JDK ${want}+ (see pom.xml <jdk.version>)." >&2
    echo "       Install a Temurin ${want} JDK and set JAVA_HOME, then re-run." >&2
    exit 1
  fi
  # Parses "25.0.3" -> 25 and legacy "1.8.0" -> 1 (which is < want, so still fails correctly).
  have="$(java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -1)"
  if [[ -z "$have" ]]; then
    echo "WARNING: could not parse the Java version from 'java -version'; skipping the JDK ${want}" >&2
    echo "         preflight. Override with APPBASE_SKIP_JDK_CHECK=1 to silence this." >&2
    return 0
  fi
  if (( have < want )); then
    echo "ERROR: JDK ${want}+ required, found ${have}. Maven would otherwise fail later with" >&2
    echo "       'release version ${want} not supported'. Set JAVA_HOME to a Temurin ${want} JDK." >&2
    exit 1
  fi
}
assert_jdk_version

# Integration test flags — set env vars before calling to enable specific client tests.
INT_FLAGS=(
  "-DAerospikeClientServiceTests.enabled=${AEROSPIKE_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DCassandraClientServiceTests.enabled=${CASSANDRA_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DClamAVClientServiceTests.enabled=${CLAMAV_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DDruidClientServiceTests.enabled=${DRUID_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DElasticsearchClientServiceTests.enabled=${ELASTICSEARCH_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DJasyncClientServiceTests.enabled=${JASYNC_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DJdbcClientServiceTests.enabled=${JDBC_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DJdbiClientServiceTests.enabled=${JDBI_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DJpaClientServiceTests.enabled=${JPA_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DKafkaClientServiceTests.enabled=${KAFKA_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DS3ClientServiceTests.enabled=${S3_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DSmtpClientServiceTests.enabled=${SMTP_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DSparkClientServiceTests.enabled=${SPARK_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DVitessClientServiceTests.enabled=${VITESS_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DZooKeeperClientServiceTests.enabled=${ZOOKEEPER_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  # ⚠️ THESE THREE WERE MISSING, AND ONE OF THEM MATTERS MOST OF ALL (internal work item 929 F2).
  # Eighteen properties are gated by `@EnabledIfSystemProperty` in the test tree; this array carried
  # fifteen. **Redis was one of the three missing** — and Redis is the one backend that both starts
  # today and has integration tests, so `./build.sh` could never run them in any mode, by any
  # developer, while `REDIS_CLIENT_SERVICE_TESTS_ENABLED` is advertised as the way to switch them on
  # in two design documents. Found by the pre-PR adversarial pass on the change that plumbed the other
  # fifteen; the arm that plumbed them could not have caught it, because it derived its list from THIS
  # ARRAY — a check sharing its input with the defect.
  "-DRedisClientServiceTests.enabled=${REDIS_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DFirebaseAuthClientServiceTests.enabled=${FIREBASE_AUTH_CLIENT_SERVICE_TESTS_ENABLED:-false}"
  "-DR2dbcClientServiceTests.enabled=${R2DBC_CLIENT_SERVICE_TESTS_ENABLED:-false}"
)

# Walks <module> entries from the root pom, depth-first, emitting repo-relative paths with no "./"
# prefix — so they compare equal (grep -qxF) to the bounded manifest paths in
# assert_gate_covers_repo, and so they can be pasted straight back in as --modules arguments.
#
# Deliberately does not shell out to maven-help-plugin: this has to work offline and on the cold
# cache inside the P-046 devcontainer, where a plugin download is exactly what cannot be assumed.
#
# XML comments are stripped FIRST (awk, comment-state machine, handles multi-line). Without that, a
# commented-out `<!-- <module>foo</module> -->` — the usual way to temporarily disable a module —
# would be read as being IN the reactor, and assert_gate_covers_repo would then report foo/ as
# covered while no Maven mode builds it. That is the internal work item 404 defect class reintroduced inside the very
# gate meant to prevent it.
strip_xml_comments() {
  awk '
    BEGIN { inc = 0 }
    {
      s = $0; out = ""
      while (length(s)) {
        if (inc) {
          i = index(s, "-->"); if (i == 0) { s = ""; break }
          s = substr(s, i + 3); inc = 0
        } else {
          i = index(s, "<!--"); if (i == 0) { out = out s; s = ""; break }
          out = out substr(s, 1, i - 1); s = substr(s, i + 4); inc = 1
        }
      }
      print out
    }' "$1" 2>/dev/null
}

reactor_modules() {
  local base="${1:-.}" child path
  for child in $(strip_xml_comments "$base/pom.xml" | sed -n 's|.*<module>\(.*\)</module>.*|\1|p'); do
    if [[ "$base" == "." ]]; then path="$child"; else path="$base/$child"; fi
    echo "$path"
    reactor_modules "$path"
  done
}

# Refuse to build while ANOTHER Java process is compiling this same tree.
#
# This is not hypothetical paranoia. Serena's Eclipse JDT language server is started EAGERLY at every
# session (whether or not a Java tool is ever called) with `autobuild.enabled = true` hardcoded, and
# it compiles into the very target/ directories Maven is writing. The result is a build that fails
# roughly HALF the time, at a DIFFERENT module each run, with errors that look exactly like real code
# defects: "package ... does not exist", "cannot access X", "class file for X not found", bnd's
# "default package '.' is not permitted", and maven-clean-plugin's "Failed to delete .../target"
# (which a serial Maven cannot do to itself — that one is the tell).
#
# It cost a full day to find, because every symptom is indistinguishable from a genuine compile
# error. Never again: fail fast, and NAME the cause.
#
# Escape hatch: APPBASE_ALLOW_CONCURRENT_JAVA=1 ./build.sh ...
assert_no_concurrent_java_builder() {
  [[ "${APPBASE_ALLOW_CONCURRENT_JAVA:-0}" == "1" ]] && return 0
  command -v lsof >/dev/null 2>&1 || return 0   # cannot check -> do not block the build

  # No self-exclusion needed: this script is bash, not java, and it runs BEFORE any mvn JVM starts.
  #
  # The match must be the repo root itself or a path BENEATH it — a bare "$SCRIPT_DIR"* prefix glob
  # would also match a sibling directory like <repo>-experiments and refuse a perfectly good build.
  # A guard that blocks legitimate builds is a guard people switch off.
  local pids=() pid cwd
  while read -r pid; do
    [[ -z "$pid" ]] && continue
    cwd="$(lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' | head -1)"
    [[ "$cwd" == "$SCRIPT_DIR" || "$cwd" == "$SCRIPT_DIR"/* ]] && pids+=("$pid")
  done < <(pgrep -x java 2>/dev/null)

  [[ ${#pids[@]} -eq 0 ]] && return 0

  echo "" >&2
  echo "REFUSING TO BUILD — another Java process is working in this tree:" >&2
  for pid in "${pids[@]}"; do
    echo "  pid $pid  $(ps -o command= -p "$pid" 2>/dev/null | cut -c1-90)" >&2
  done
  echo "" >&2
  echo "A second compiler writing into target/ makes this build FAIL ~50% OF THE TIME, at a random" >&2
  echo "module, with errors that look exactly like real code defects. The build is not broken; it is" >&2
  echo "being raced." >&2
  echo "" >&2
  echo "Most likely: a Java language server autobuilding (Eclipse JDT, whether launched by an IDE, an" >&2
  echo "MCP server or jdtls directly). Stop it, or point its output out of target/, then re-run." >&2
  echo "" >&2
  echo "To override (you will get flaky results): APPBASE_ALLOW_CONCURRENT_JAVA=1 ./build.sh ..." >&2
  exit 1
}

is_excluded_tree() {
  # Match either the FULL directory for a precise nested exclusion or its
  # top-level tree (the original behaviour, kept
  # so existing single-segment entries such as appbase-codeagents still match).
  local dir="$1" tree="${1%%/*}" e
  for e in "${EXCLUDED_TREES[@]}"; do
    [[ "$dir" == "$e" || "$tree" == "$e" ]] && return 0
  done
  return 1
}

# The anti-internal work item 404 gate: PROVE the build's coverage rather than assert it.
#
# Every pom.xml / pubspec.yaml through depth 8 must be either IN the reactor or EXPLICITLY excluded
# above. This bounded scan avoids a host-wide recursive walk; it covers the current manifest layout
# (58 files, deepest at level 6), but does not claim to detect a new manifest below the depth limit.
# A module nobody wired into the reactor within that bound fails loudly instead of sitting invisible
# for a release cycle. This repo has already been bitten twice — the five root siblings (internal work item 404), and
# then an experimental Flutter package that no gate built.
assert_gate_covers_repo() {
  local reactor manifests orphans=() p dir
  reactor="$(reactor_modules . | sort -u)"

  if ! manifests="$(rg --files --hidden --no-ignore --max-depth 8 \
      -g 'pom.xml' -g 'pubspec.yaml' \
      -g '!**/target/**' -g '!**/build/**' \
      -g '!**/.dart_tool/**' -g '!**/node_modules/**' \
      -g '!**/.claude/**' -g '!**/.git/**' .)"; then
    echo "ERROR: bounded build-manifest scan failed; refusing to claim gate coverage." >&2
    exit 1
  fi

  while IFS= read -r p; do
    [[ -z "$p" ]] && continue
    dir="$(dirname "${p#./}")"
    [[ "$dir" == "." ]] && continue

    # Exact reactor membership only: a new nested pom.xml under a reactor module is not
    # automatically built, so a top-level prefix match would hide an orphan.
    grep -qxF "$dir" <<<"$reactor" && continue

    # The sole known lifecycle-built non-reactor manifest. Staging/release invoke the
    # wizard's exec-maven-plugin for this Flutter frontend; pin its manifest path exactly.
    [[ "${p#./}" == "appbase-config-wizard/frontend/pubspec.yaml" ]] && continue

    is_excluded_tree "$dir" && continue

    orphans+=("$dir")
  done <<< "$manifests"

  if [[ ${#orphans[@]} -gt 0 ]]; then
    echo "" >&2
    echo "GATE COVERAGE FAILURE — buildable trees that NO mode builds:" >&2
    printf '  %s\n' "${orphans[@]}" | sort -u >&2
    echo "" >&2
    echo "Each is neither in the Maven reactor, the known wizard frontend lifecycle, nor EXCLUDED_TREES in build.sh." >&2
    echo "This is the internal work item 404 defect class: a gate covering less than it claims." >&2
    echo "Fix by EITHER wiring it into the reactor, OR listing it in EXCLUDED_TREES with a reason." >&2
    exit 1
  fi
}

# NOTICE CURRENCY (internal work item 1384 / P-025 clause 4) — regenerate the NOTICE and refuse if the committed file
# no longer matches the dependency set that actually resolves.
#
# Apache-2.0 section 4(d) makes NOTICE an obligation, so it has to track the dependencies, and the
# only thing that makes that happen reliably is a build step that FAILS when it has not. The check
# itself lives in scripts/license/notice-check.sh and is pinned by focused regression coverage.
#
# ⚠️ IT RUNS IN develop/staging/release ONLY, AND NOT IN compile OR quick — DELIBERATELY. Those two
# modes are documented above as needing no database, no Docker and NO NETWORK, and that property is
# what makes `--mode compile` a gate no environmental failure can mask. Regenerating NOTICE resolves
# the whole dependency graph, so wiring it into compile would take that property away from the one
# gate that most depends on it. Run it by hand there if you want it: scripts/license/notice-check.sh --generate
#
# It runs BEFORE the main build: the failure is about the dependency set, not about anything the
# build produces, and discovering it after a full install+site run is discovering it late. For
# release it has to be before, or artifacts are produced against a NOTICE nobody checked.
assert_notice_current() {
  # $SCRIPT_DIR, not a second derivation from $BASH_SOURCE. This script already resolved its own
  # absolute root at the top and cd'd there; re-deriving it relatively RE-RESOLVES IT AFTER that cd,
  # so `bash Appbase/build.sh --mode develop` from the parent directory looked for the gate under
  # <root>/Appbase/scripts/ and did not find it. Two spellings of one predicate, and the weaker one
  # was wrong.
  local check="$SCRIPT_DIR/scripts/license/notice-check.sh"
  # A gate that cannot run has not found a stale NOTICE — it has found nothing. Saying otherwise
  # sends the reader off to regenerate a file that was never the problem (measured: the relative-path
  # failure above reported "NOTICE is not current" when the script simply was not there).
  [[ -x "$check" ]] || { echo "ERROR: the NOTICE gate is missing or not executable: $check" >&2; exit 5; }
  echo "notice gate: checking the committed NOTICE against the resolved dependency set"
  local gate_status
  if "$check" --generate; then
    return 0
  else
    gate_status=$?
  fi
  if [[ $gate_status -eq 4 ]]; then
    echo "ERROR: NOTICE is not current. Follow the gate's update instructions, then re-run." >&2
  else
    echo "ERROR: could not verify NOTICE (gate exit $gate_status). See the gate diagnostic above." >&2
  fi
  exit "$gate_status"
}


SELECT_FLAGS=()
if [[ -n "$MODULES" ]]; then
  if [[ "$MODE" == "compile" ]]; then
    echo "ERROR: --modules cannot narrow 'compile'." >&2
    echo "       compile is the whole-repo pre-PR gate; narrowing it re-opens the very hole it" >&2
    echo "       exists to close (internal work item 404). Use --modules with quick/develop for the inner loop." >&2
    exit 1
  fi

  SELECTED=()
  IFS=',' read -r -a REQUESTED <<< "$MODULES"
  for m in "${REQUESTED[@]}"; do
    m="${m//[[:space:]]/}"; m="${m%/}"
    [[ -z "$m" ]] && continue

    if [[ -f "$m/pom.xml" ]]; then
      SELECTED+=("$m")

    elif [[ -f "$m/pubspec.yaml" ]]; then
      # A Dart/Flutter tree. It is only buildable through Maven if it sits INSIDE a Maven module —
      # which is the case for appbase-config-wizard/frontend, whose four exec-maven-plugin
      # executions are bound to the wizard module's own lifecycle. So select the PARENT and switch
      # the frontend on. Building it any other way (calling flutter directly) would leave the bundle
      # out of the jar, which is worse than not building it: the jar looks fine and has no UI.
      if is_excluded_tree "$m"; then
        echo "ERROR: '$m' is deliberately excluded from every build (see EXCLUDED_TREES)." >&2
        exit 1
      fi
      parent="$(dirname "$m")"
      if [[ ! -f "$parent/pom.xml" ]]; then
        echo "ERROR: '$m' is a Dart/Flutter package with no Maven parent — nothing can build it." >&2
        exit 1
      fi
      SELECTED+=("$parent")
      FRONTEND_ON=1

    else
      echo "ERROR: '$m' is not a Maven module (no $m/pom.xml)." >&2
      echo "" >&2
      echo "Valid modules (paths — they tab-complete):" >&2
      reactor_modules . | sort | sed 's/^/  /' >&2
      exit 1
    fi
  done

  # Guard the empty case explicitly. `--modules ,` reduces to nothing, and expanding an EMPTY array
  # as "${SELECTED[*]}" aborts under `set -u` on bash 3.2 (what macOS ships) — the same trap that
  # MVN_ARGS is seeded against below.
  if [[ ${#SELECTED[@]} -eq 0 ]]; then
    echo "ERROR: --modules '$MODULES' selected no modules." >&2
    exit 1
  fi

  # -am also-makes the dependencies, resolved from the POMs — nothing to hand-list.
  SELECT_FLAGS=(-pl "$(IFS=','; echo "${SELECTED[*]}")" -am)
fi

# `compile` does not consume MVN_ARGS, so --wizard-frontend would be silently discarded. Say so
# rather than ignoring it: the gate must run on JDK + Maven ALONE (a bare CI runner, or the P-046
# devcontainer, has no flutter), so building the UI here is not something to quietly half-honour.
if [[ "$MODE" == "compile" && "$WANT_FRONTEND" -eq 1 ]]; then
  echo "ERROR: --wizard-frontend cannot be used with 'compile'." >&2
  echo "       The pre-PR gate must build on JDK + Maven alone — a bare CI runner and the P-046" >&2
  echo "       devcontainer ship no flutter. Use quick/develop/staging/release to build the UI." >&2
  exit 1
fi

# The config-wizard's Flutter UI is NOT a module — it is four exec-maven-plugin executions inside
# appbase-config-wizard's own lifecycle, gated by the appbase-config-wizard.skipFrontend property.
# That is why it is a flag rather than a plain --modules entry: it is not independently addressable.
case "$MODE" in staging|release) FRONTEND_ON=1 ;; esac

FRONTEND_FLAG=()
if [[ "$FRONTEND_ON" -eq 1 ]]; then
  FRONTEND_FLAG=(-Dappbase-config-wizard.skipFrontend=false)
elif [[ "$MODE" != "compile" ]]; then
  # WARN LOUDLY. The Flutter bundle is packaged INTO the wizard jar (frontend/build/web ->
  # static/web/), so a jar built without it HAS NO WEB UI. Running that jar gives a blank page and no
  # clue why. A silent UI-less artifact is worse than a slow build.
  echo "WARNING: appbase-config-wizard is being built WITHOUT its web UI (no Flutter)." >&2
  echo "         The wizard jar will have no frontend. Use '--wizard-frontend', or staging/release." >&2
  echo "" >&2
fi

# Accumulate every extra Maven arg into ONE array.
#
# Not cosmetic: macOS ships bash 3.2, where `"${arr[@]}"` on an EMPTY array under `set -u` aborts
# with "unbound variable". Expanding SELECT_FLAGS/FRONTEND_FLAG directly therefore worked in the
# devcontainer (bash 5) and failed on the developer's Mac. INT_FLAGS is never empty, so seeding from
# it keeps MVN_ARGS non-empty and the expansion safe on both.
MVN_ARGS=("${INT_FLAGS[@]}")
[[ ${#SELECT_FLAGS[@]} -gt 0 ]] && MVN_ARGS+=("${SELECT_FLAGS[@]}")
[[ ${#FRONTEND_FLAG[@]} -gt 0 ]] && MVN_ARGS+=("${FRONTEND_FLAG[@]}")

# EVERY mode, not just the gate: a concurrent Java builder corrupts any Maven run over this tree.
assert_no_concurrent_java_builder

case "$MODE" in
  compile)
    # WHOLE-REPO compile of main AND test sources. Run before EVERY PR.
    #
    # Why it is its own gate, even though every mode is now whole-repo: `test-compile` compiles main
    # and test sources but RUNS NO TESTS, so it needs no database, no Docker and no network, and
    # therefore CANNOT BE MASKED by an environmental failure. The test-running modes die early on the
    # Jdbi* tests (which need a DB nobody runs locally) and never reach the later modules — so a red
    # build everyone has learned to scroll past hides every compile error behind it. That masking is
    # the second half of internal work item 404, and moving this script to the repo root did not fix it.
    #
    # It also builds no Flutter (skipFrontend defaults true), so the toolchain is JDK + Maven + `git`.
    #
    # ⚠️ `git` IS A REAL DEPENDENCY OF THIS GATE, and it is the one environmental failure that CAN mask a
    # compile error here. `git-commit-id-plugin` is configured `useNativeGit=true` (pom.xml — see the
    # comment there for why JGit cannot be used: it breaks in every linked worktree) and binds `revision`
    # to `initialize`, so with no `git` on PATH the reactor dies on the first module and no class is
    # compiled. That failure is loud and names the plugin, but it arrives at the same moment a real
    # compile error would, which is exactly the masking shape described above.
    #
    # Accepted, because the alternative is worse and the exposure is small: any environment holding this
    # source got it with `git`, and the plugin exists to read git metadata, so a build with no `git` has
    # nothing for it to report regardless. Do not "fix" a missing-git failure by pointing
    # `dotGitDirectory` at a common dir — that stamps artifacts with a commit this tree never built.
    assert_gate_covers_repo
    mvn -q clean test-compile
    echo "compile gate: every module compiles (main + test sources); no tests run"
    echo "coverage:     pom.xml / pubspec.yaml through depth 8 is reactor-built, wizard-lifecycle-built, or explicitly excluded"
    ;;
  quick)
    # Fast feedback loop: unit tests only, no coverage threshold, no analyzer gate.
    mvn clean install \
        -P quick-profile \
        "${MVN_ARGS[@]}"
    ;;
  develop)
    # Full build with coverage + checkstyle/spotbugs gate — use before pushing.
    assert_notice_current
    mvn clean install site \
        -P develop-profile \
        "${MVN_ARGS[@]}"
    ;;
  staging)
    # Everything develop does, plus appbase-stacktest and the config-wizard console/web IT suite.
    # Both are genuinely built now: they are root siblings, and this script's reactor is the root.
    assert_notice_current
    mvn clean install site \
        -P staging-profile,appbase-config-wizard-it-profile \
        "${MVN_ARGS[@]}"
    ;;
  release)
    # Release build: strict rules, no SNAPSHOTs, javadoc/DOAP/versions-check, all staging checks.
    assert_notice_current
    mvn clean install site site:stage \
        -P release-profile,appbase-config-wizard-it-profile \
        "${MVN_ARGS[@]}"
    ;;
  *)
    echo "ERROR: unknown mode '$MODE'" >&2
    echo "" >&2
    usage
    exit 1
    ;;
esac
