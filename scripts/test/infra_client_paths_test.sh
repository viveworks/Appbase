#!/bin/bash
# @author Navaneeth Mysore G
# @author Claude

set -eu

REPO_ROOT=$(cd -- "$(dirname "$0")/../.." >/dev/null 2>&1; pwd -P)
TEST_TMP=$(mktemp -d "${TMPDIR:-/tmp}/appbase-infra-paths.XXXXXX")
trap 'rm -rf "$TEST_TMP"' EXIT HUP INT TERM

FAKE_BIN=$TEST_TMP/bin
CALLS=$TEST_TMP/calls
mkdir -p "$FAKE_BIN"

cat > "$FAKE_BIN/bash" <<'EOF'
#!/bin/bash
if [ ! -f "$1" ]; then
    exit 127
fi
printf '%s|%s\n' "$PWD" "$1" >> "$APPBASE_TEST_CALLS"
if [ -n "${APPBASE_TEST_FAIL_BASENAME:-}" ] && [ "$(basename -- "$1")" = "$APPBASE_TEST_FAIL_BASENAME" ]; then
    exit 37
fi
EOF
cat > "$FAKE_BIN/sleep" <<'EOF'
#!/bin/bash
exit 0
EOF
chmod +x "$FAKE_BIN/bash" "$FAKE_BIN/sleep"

assert_script_calls() {
    script=$1
    expected=$2
    # Optional: APPBASE_STACKTEST_VITESS_BACKEND override ("" leaves it unset -> docker default).
    backend_env=${3:-}
    : > "$CALLS"

    APPBASE_STACKTEST_VITESS_BACKEND=$backend_env \
        APPBASE_CHILD_BASH=$FAKE_BIN/bash \
        APPBASE_TEST_CALLS=$CALLS \
        PATH=$FAKE_BIN:$PATH \
        /bin/bash "$REPO_ROOT/$script"

    if ! diff -u "$expected" "$CALLS"; then
        echo "FAIL: $script resolved an unexpected client infra path" >&2
        return 1
    fi
}

CLIENT_ROOT=$REPO_ROOT/appbase-java/appbase-client
STACK_EXPECT=$TEST_TMP/stack-expected
SERVICE_EXPECT=$TEST_TMP/service-expected

cat > "$STACK_EXPECT" <<EOF
$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql|$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql/start-postgresql.sh
$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper|$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper/start-zookeeper.sh
$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike|$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike/start-aerospike.sh
$CLIENT_ROOT/appbase-client-druid/infra/docker/druid|$CLIENT_ROOT/appbase-client-druid/infra/docker/druid/start-druid.sh
$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch|$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch/start-elasticsearch.sh
$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase|$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase/start-firebase.sh
$CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess|$CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess/start-vitess.sh
EOF
assert_script_calls appbase-stacktest/infra/start-infra.sh "$STACK_EXPECT"
cp "$STACK_EXPECT" "$TEST_TMP/stack-start"

cat > "$STACK_EXPECT" <<EOF
$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike|$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike/run-aerospike.sh
$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper|$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper/run-zookeeper.sh
$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql|$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql/run-postgresql.sh
$CLIENT_ROOT/appbase-client-druid/infra/docker/druid|$CLIENT_ROOT/appbase-client-druid/infra/docker/druid/run-druid.sh
$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch|$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch/run-elasticsearch.sh
$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase|$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase/run-firebase.sh
$CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess|$CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess/remove-vitess.sh
$CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess|$CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess/run-vitess.sh
EOF
assert_script_calls appbase-stacktest/infra/run-infra.sh "$STACK_EXPECT"
cp "$STACK_EXPECT" "$TEST_TMP/stack-run"

cat > "$STACK_EXPECT" <<EOF
$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike|$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike/stop-aerospike.sh
$CLIENT_ROOT/appbase-client-druid/infra/docker/druid|$CLIENT_ROOT/appbase-client-druid/infra/docker/druid/stop-druid.sh
$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch|$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch/stop-elasticsearch.sh
$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase|$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase/stop-firebase.sh
$CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess|$CLIENT_ROOT/appbase-client-vitess/infra/docker/vitess/stop-vitess.sh
$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql|$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql/stop-postgresql.sh
$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper|$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper/stop-zookeeper.sh
EOF
assert_script_calls appbase-stacktest/infra/stop-infra.sh "$STACK_EXPECT"
cp "$STACK_EXPECT" "$TEST_TMP/stack-stop"

# ── APPBASE_STACKTEST_VITESS_BACKEND=local opt-in: StackTest's three orchestrators must still reach
# the original local-binary scripts for a maintainer whose machine has Vitess installed (internal work item 2655). ──────
cat > "$STACK_EXPECT" <<EOF
$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql|$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql/start-postgresql.sh
$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper|$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper/start-zookeeper.sh
$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike|$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike/start-aerospike.sh
$CLIENT_ROOT/appbase-client-druid/infra/docker/druid|$CLIENT_ROOT/appbase-client-druid/infra/docker/druid/start-druid.sh
$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch|$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch/start-elasticsearch.sh
$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase|$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase/start-firebase.sh
$REPO_ROOT/appbase-stacktest/infra/vitess|$REPO_ROOT/appbase-stacktest/infra/vitess/local-vitess-start.sh
EOF
assert_script_calls appbase-stacktest/infra/start-infra.sh "$STACK_EXPECT" local

cat > "$STACK_EXPECT" <<EOF
$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike|$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike/run-aerospike.sh
$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper|$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper/run-zookeeper.sh
$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql|$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql/run-postgresql.sh
$CLIENT_ROOT/appbase-client-druid/infra/docker/druid|$CLIENT_ROOT/appbase-client-druid/infra/docker/druid/run-druid.sh
$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch|$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch/run-elasticsearch.sh
$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase|$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase/run-firebase.sh
$REPO_ROOT/appbase-stacktest/infra/vitess|$REPO_ROOT/appbase-stacktest/infra/vitess/local-vitess-purge.sh
$REPO_ROOT/appbase-stacktest/infra/vitess|$REPO_ROOT/appbase-stacktest/infra/vitess/local-vitess-start.sh
EOF
assert_script_calls appbase-stacktest/infra/run-infra.sh "$STACK_EXPECT" local

cat > "$STACK_EXPECT" <<EOF
$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike|$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike/stop-aerospike.sh
$CLIENT_ROOT/appbase-client-druid/infra/docker/druid|$CLIENT_ROOT/appbase-client-druid/infra/docker/druid/stop-druid.sh
$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch|$CLIENT_ROOT/appbase-client-elasticsearch/infra/docker/elasticsearch/stop-elasticsearch.sh
$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase|$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase/stop-firebase.sh
$REPO_ROOT/appbase-stacktest/infra/vitess|$REPO_ROOT/appbase-stacktest/infra/vitess/local-vitess-stop.sh
$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql|$CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql/stop-postgresql.sh
$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper|$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper/stop-zookeeper.sh
EOF
assert_script_calls appbase-stacktest/infra/stop-infra.sh "$STACK_EXPECT" local

# An unrecognized backend value must fail closed, in ALL THREE orchestrators -- each carries its own
# copy of the same case statement, so each needs its own proof (fresh CALLS each time so no prior
# invocation is mistaken for one). A nonzero exit alone is NOT sufficient: it must also make ZERO
# child-process calls -- i.e. fail before mutating ANY backend, not after bringing up (run-infra.sh),
# starting (start-infra.sh), or partially stopping (stop-infra.sh) the other six.
for orch in run-infra.sh start-infra.sh stop-infra.sh; do
    : > "$CALLS"
    if APPBASE_STACKTEST_VITESS_BACKEND=bogus \
        APPBASE_CHILD_BASH=$FAKE_BIN/bash APPBASE_TEST_CALLS=$CALLS PATH=$FAKE_BIN:$PATH \
        /bin/bash "$REPO_ROOT/appbase-stacktest/infra/$orch" >/dev/null 2>&1; then
        echo "FAIL: APPBASE_STACKTEST_VITESS_BACKEND=bogus did not fail closed in $orch" >&2
        exit 1
    fi
    if [ -s "$CALLS" ]; then
        echo "FAIL: APPBASE_STACKTEST_VITESS_BACKEND=bogus failed closed in $orch but only after $(wc -l < "$CALLS" | tr -d ' ') child call(s) -- validation must run before any side effect:" >&2
        cat "$CALLS" >&2
        exit 1
    fi
done

cat > "$SERVICE_EXPECT" <<EOF
$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike|$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike/start-aerospike.sh
$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper|$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper/start-zookeeper.sh
$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema|$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema/local-vitess-start.sh
$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase|$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase/start-firebase.sh
EOF
assert_script_calls appbase-java/appbase-service/infra/start-infra.sh "$SERVICE_EXPECT"
cp "$SERVICE_EXPECT" "$TEST_TMP/service-start"

cat > "$SERVICE_EXPECT" <<EOF
$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike|$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike/run-aerospike.sh
$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper|$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper/run-zookeeper.sh
$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema|$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema/local-vitess-purge.sh
$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema|$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema/local-vitess-start.sh
$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase|$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase/run-firebase.sh
EOF
assert_script_calls appbase-java/appbase-service/infra/run-infra.sh "$SERVICE_EXPECT"
cp "$SERVICE_EXPECT" "$TEST_TMP/service-run"

cat > "$SERVICE_EXPECT" <<EOF
$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike|$CLIENT_ROOT/appbase-client-aerospike/infra/docker/aerospike/stop-aerospike.sh
$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema|$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema/local-vitess-stop.sh
$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper|$CLIENT_ROOT/appbase-client-zookeeper/infra/docker/zookeeper/stop-zookeeper.sh
$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase|$CLIENT_ROOT/appbase-client-firebase/infra/docker/firebase/stop-firebase.sh
EOF
assert_script_calls appbase-java/appbase-service/infra/stop-infra.sh "$SERVICE_EXPECT"
cp "$SERVICE_EXPECT" "$TEST_TMP/service-stop"

for surface in stack service; do
    cut -d '|' -f 1 "$TEST_TMP/$surface-start" | sort -u > "$TEST_TMP/$surface-start-dirs"
    cut -d '|' -f 1 "$TEST_TMP/$surface-run" | sort -u > "$TEST_TMP/$surface-run-dirs"
    cut -d '|' -f 1 "$TEST_TMP/$surface-stop" | sort -u > "$TEST_TMP/$surface-stop-dirs"
    diff -u "$TEST_TMP/$surface-start-dirs" "$TEST_TMP/$surface-run-dirs"
    diff -u "$TEST_TMP/$surface-start-dirs" "$TEST_TMP/$surface-stop-dirs"
done

# Mechanically resolve the leaf helpers' local dependency roots and each actual source operand.
resolve_relative_dir() {
    script=$1
    variable=$2
    script_dir=$(cd "$(dirname -- "$script")" >/dev/null 2>&1 && pwd -P)
    relative=$(awk -v variable="$variable" '
        $0 ~ "^export " variable "=" {
            value=$0
            sub(/^.*\}\//, "", value)
            sub(/"$/, "", value)
            print value
        }
    ' "$script")
    if [ -z "$relative" ]; then
        echo "FAIL: could not resolve $variable in $script" >&2
        return 1
    fi
    (cd "$script_dir/$relative" >/dev/null 2>&1 && pwd -P)
}

assert_source_operands_resolve() {
    script=$1
    variable=$2
    resolved_dir=$3
    operands="$TEST_TMP/source-operands"
    sed -n 's/^source "\(.*\)"$/\1/p' "$script" > "$operands"
    if [ ! -s "$operands" ]; then
        echo "FAIL: no source operands found in $script" >&2
        return 1
    fi

    prefix='${'"$variable"'}/'
    while IFS= read -r operand; do
        case "$operand" in
            "$prefix"*) dependency=${operand#"$prefix"} ;;
            *)
                echo "FAIL: source operand does not use $variable in $script: $operand" >&2
                return 1
                ;;
        esac
        if [ ! -f "$resolved_dir/$dependency" ]; then
            echo "FAIL: source operand does not resolve in $script: $resolved_dir/$dependency" >&2
            return 1
        fi
    done < "$operands"
}

STACK_VITESS_DIR=$(resolve_relative_dir \
    "$REPO_ROOT/appbase-stacktest/infra/vitess/local-vitess-start.sh" \
    APPBASE_CLIENT_VITESS_BASEDIR)
SERVICE_SCHEMA_DIR=$(resolve_relative_dir \
    "$REPO_ROOT/appbase-stacktest/infra/vitess/local-vitess-start.sh" \
    APPBASE_SERVICE_VITESS_BASEDIR)
SERVICE_VITESS_DIR=$(resolve_relative_dir \
    "$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema/local-vitess-start.sh" \
    APPBASE_VITESS_SCRIPTS_BASEDIR)

assert_source_operands_resolve \
    "$REPO_ROOT/appbase-stacktest/infra/vitess/local-vitess-start.sh" \
    APPBASE_CLIENT_VITESS_BASEDIR "$STACK_VITESS_DIR"
assert_source_operands_resolve \
    "$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema/local-vitess-start.sh" \
    APPBASE_VITESS_SCRIPTS_BASEDIR "$SERVICE_VITESS_DIR"
test -f "$SERVICE_SCHEMA_DIR/001-sharded.sql"

for helper in local-vitess-stop.sh local-vitess-purge.sh; do
    resolved=$(resolve_relative_dir "$REPO_ROOT/appbase-stacktest/infra/vitess/$helper" \
        APPBASE_VITESS_SCRIPTS_BASEDIR)
    test "$resolved" = "$STACK_VITESS_DIR"
    resolved=$(resolve_relative_dir "$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema/$helper" \
        APPBASE_VITESS_SCRIPTS_BASEDIR)
    test "$resolved" = "$SERVICE_VITESS_DIR"
    assert_source_operands_resolve "$REPO_ROOT/appbase-stacktest/infra/vitess/$helper" \
        APPBASE_VITESS_SCRIPTS_BASEDIR "$STACK_VITESS_DIR"
    assert_source_operands_resolve \
        "$CLIENT_ROOT/appbase-client-vitess/infra/local/service-schema/$helper" \
        APPBASE_VITESS_SCRIPTS_BASEDIR "$SERVICE_VITESS_DIR"
done

# Run a copied orchestrator from an unrelated cwd with its entire fixture rooted in a path
# containing spaces. This catches unquoted directory and child-script expansions.
SPACE_ROOT="$TEST_TMP/repo with spaces"
mkdir -p "$SPACE_ROOT"
SPACE_ROOT=$(cd "$SPACE_ROOT" >/dev/null 2>&1 && pwd -P)
SPACE_STACK_DIR="$SPACE_ROOT/appbase-stacktest/infra"
SPACE_CLIENT_ROOT="$SPACE_ROOT/appbase-java/appbase-client"
mkdir -p "$SPACE_STACK_DIR"
cp "$REPO_ROOT/appbase-stacktest/infra/start-infra.sh" "$SPACE_STACK_DIR/start-infra.sh"

make_fixture_from_inventory() {
    inventory=$1
    while IFS='|' read -r child_dir child_script; do
        case "$child_dir" in
            "$CLIENT_ROOT"/*)
                fixture_dir="$SPACE_CLIENT_ROOT${child_dir#$CLIENT_ROOT}"
                fixture_script="$SPACE_CLIENT_ROOT${child_script#$CLIENT_ROOT}"
                ;;
            "$REPO_ROOT"/*)
                fixture_dir="$SPACE_ROOT${child_dir#$REPO_ROOT}"
                fixture_script="$SPACE_ROOT${child_script#$REPO_ROOT}"
                ;;
            *)
                echo "FAIL: inventory path is outside known roots: $child_dir" >&2
                return 1
                ;;
        esac
        mkdir -p "$fixture_dir"
        : > "$fixture_script"
    done < "$inventory"
}

make_fixture_from_inventory "$TEST_TMP/stack-start"
sed -e "s|$CLIENT_ROOT|$SPACE_CLIENT_ROOT|g" -e "s|$REPO_ROOT|$SPACE_ROOT|g" \
    "$TEST_TMP/stack-start" > "$TEST_TMP/space-start-expected"
: > "$CALLS"
(
    cd "$TEST_TMP"
    APPBASE_CHILD_BASH="$FAKE_BIN/bash" APPBASE_TEST_CALLS="$CALLS" PATH="$FAKE_BIN:$PATH" \
        /bin/bash "$SPACE_STACK_DIR/start-infra.sh"
)
diff -u "$TEST_TMP/space-start-expected" "$CALLS"

# A missing client directory must fail before any child invocation.
rm -rf "$SPACE_CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql"
: > "$CALLS"
if APPBASE_CHILD_BASH="$FAKE_BIN/bash" APPBASE_TEST_CALLS="$CALLS" PATH="$FAKE_BIN:$PATH" \
    /bin/bash "$SPACE_STACK_DIR/start-infra.sh" >/dev/null 2>&1; then
    echo "FAIL: missing client directory did not fail" >&2
    exit 1
fi
test ! -s "$CALLS"

# A missing child script and an explicit child failure must both stop the sequence immediately.
make_fixture_from_inventory "$TEST_TMP/stack-start"
rm "$SPACE_CLIENT_ROOT/appbase-client-jdbc/infra/docker/postgresql/start-postgresql.sh"
: > "$CALLS"
if APPBASE_CHILD_BASH="$FAKE_BIN/bash" APPBASE_TEST_CALLS="$CALLS" PATH="$FAKE_BIN:$PATH" \
    /bin/bash "$SPACE_STACK_DIR/start-infra.sh" >/dev/null 2>&1; then
    echo "FAIL: missing child script did not fail" >&2
    exit 1
fi
test ! -s "$CALLS"

make_fixture_from_inventory "$TEST_TMP/stack-start"
: > "$CALLS"
set +e
APPBASE_CHILD_BASH="$FAKE_BIN/bash" APPBASE_TEST_CALLS="$CALLS" \
    APPBASE_TEST_FAIL_BASENAME=start-zookeeper.sh PATH="$FAKE_BIN:$PATH" \
    /bin/bash "$SPACE_STACK_DIR/start-infra.sh" >/dev/null 2>&1
failure_rc=$?
set -e
if [ "$failure_rc" -ne 37 ]; then
    echo "FAIL: child exit 37 propagated as $failure_rc" >&2
    exit 1
fi
test "$(wc -l < "$CALLS" | tr -d ' ')" = 2

echo "PASS: all infra orchestration resolves client-owned paths"
