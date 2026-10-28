#!/usr/bin/env bash
# Observable-behaviour tests for check-client-javadocs.py.
# Every negative control must fail with ITS OWN diagnostic, so an unrelated failure (an absent
# fixture type, an empty inventory) cannot mask a removed guard (internal work item 1401 A1-M5).
set -u

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CHECK="$ROOT/scripts/check-client-javadocs.py"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/client-javadocs.XXXXXX")" || exit 1
trap 'rm -rf "$WORK"' EXIT HUP INT TERM
passed=0
failed=0

# expect_fail <name> <diagnostic> <command...>: the command must fail AND print the diagnostic.
expect_fail() {
  name=$1; diagnostic=$2; shift 2
  if "$@" >"$WORK/out" 2>&1; then
    printf 'not ok - %s (unexpected success)\n' "$name"
    failed=$((failed + 1))
  elif grep -q -- "$diagnostic" "$WORK/out"; then
    printf 'ok - %s\n' "$name"
    passed=$((passed + 1))
  else
    printf 'not ok - %s (failed without the diagnostic %s)\n' "$name" "$diagnostic"
    cat "$WORK/out"
    failed=$((failed + 1))
  fi
}

expect_pass() {
  name=$1; shift
  if "$@" >"$WORK/out" 2>&1; then
    printf 'ok - %s\n' "$name"
    passed=$((passed + 1))
  else
    printf 'not ok - %s\n' "$name"
    cat "$WORK/out"
    failed=$((failed + 1))
  fi
}

check_real_scope() {
  if grep -R -n --include='*.java' 'Creates a Appbase' \
      "$ROOT/appbase-java/appbase-client/appbase-client-core/src/main/java"; then
    printf 'invalid indefinite article in Appbase constructor summary\n'
    return 1
  fi
  python3 "$CHECK" --root "$ROOT"
}

fixture() {
  dir=$1 classification=$2 types=$3
  mkdir -p "$dir/docs/api-inventory" "$dir/module/src/main/java/example/api"
  cat >"$dir/docs/api-inventory/inventory.json" <<EOF
{"artifacts":[{"packages":[{"package":"example.api","classification":"$classification","public_types":$types}]}]}
EOF
}

# A source type that is correctly documented, so a control that removes ONE guard sees only that failure.
good_example() {
  cat >"$1/module/src/main/java/example/api/Example.java" <<'EOF'
package example.api;
/** Documented example. */
public class Example {
    /** Documents the callable contract. */
    @Deprecated
    public void call() {}
}
EOF
}

expect_fail "missing inventory fails closed" 'INVENTORY RED missing' \
  python3 "$CHECK" --root "$WORK/missing" --packages example.api

fixture "$WORK/empty" supported-public '[]'
good_example "$WORK/empty"
expect_fail "empty public type inventory fails closed" 'empty public_types inventory' \
  python3 "$CHECK" --root "$WORK/empty" --packages example.api

fixture "$WORK/classification" internal-implementation '["Example"]'
good_example "$WORK/classification"
expect_fail "non-supported classification fails closed" 'not supported-public' \
  python3 "$CHECK" --root "$WORK/classification" --packages example.api

fixture "$WORK/zero" supported-public '["Example"]'
expect_fail "an inventory type with no source file fails closed" 'absent types' \
  python3 "$CHECK" --root "$WORK/zero" --packages example.api

fixture "$WORK/notype" supported-public '["Example"]'
cat >"$WORK/notype/module/src/main/java/example/api/Example.java" <<'EOF'
package example.api;
public class Example {
    /** Documented member. */
    public void call() {}
}
EOF
expect_fail "missing type Javadoc is rejected" 'missing type Javadoc' \
  python3 "$CHECK" --root "$WORK/notype" --packages example.api

fixture "$WORK/nomember" supported-public '["Example"]'
cat >"$WORK/nomember/module/src/main/java/example/api/Example.java" <<'EOF'
package example.api;
/** Documented example. */
public class Example {
    public void call() {}
}
EOF
expect_fail "missing member Javadoc is rejected" 'missing explicit member Javadoc' \
  python3 "$CHECK" --root "$WORK/nomember" --packages example.api

fixture "$WORK/override" supported-public '["Example"]'
cat >"$WORK/override/module/src/main/java/example/api/Example.java" <<'EOF'
package example.api;
/** Documented example. */
public class Example {
    @Override
    public String toString() { return "x"; }
}
EOF
expect_fail "an override without its own Javadoc is rejected" 'missing explicit member Javadoc' \
  python3 "$CHECK" --root "$WORK/override" --packages example.api

fixture "$WORK/placement" supported-public '["Example"]'
cat >"$WORK/placement/module/src/main/java/example/api/Example.java" <<'EOF'
package example.api;
/** Documented example. */
public class Example {
    @Deprecated
    /** Invalidly placed method documentation. */
    public void call() {}
}
EOF
expect_fail "Javadoc after an annotation is rejected" 'appears after an annotation' \
  python3 "$CHECK" --root "$WORK/placement" --packages example.api

# A1-H1: an Allman (brace on its own line) header must keep the declaration state, so the undocumented
# method inside is still found.
fixture "$WORK/allman" supported-public '["Example"]'
cat >"$WORK/allman/module/src/main/java/example/api/Example.java" <<'EOF'
package example.api;
/** Documented example. */
public class Example
{
    public void call() {}
}
EOF
expect_fail "an undocumented member of an Allman-brace type is rejected" 'missing explicit member Javadoc' \
  python3 "$CHECK" --root "$WORK/allman" --packages example.api

fixture "$WORK/allman-good" supported-public '["Example"]'
cat >"$WORK/allman-good/module/src/main/java/example/api/Example.java" <<'EOF'
package example.api;
/** Documented example. */
public class Example
{
    /** Documents the callable contract. */
    public void call() {}
}
EOF
expect_pass "a documented Allman-brace type is accepted" python3 "$CHECK" --root "$WORK/allman-good" --packages example.api

# A multi-line field declaration must not swallow the next callable's Javadoc state.
fixture "$WORK/field" supported-public '["Example"]'
cat >"$WORK/field/module/src/main/java/example/api/Example.java" <<'EOF'
package example.api;
/** Documented example. */
public class Example {
    protected final java.util.Map<String, Boolean>
            wrapped = new java.util.HashMap<>();

    /** Documents the constructor contract. */
    public Example() {}
}
EOF
expect_pass "a wrapped field does not mask the next documented callable" python3 "$CHECK" --root "$WORK/field" --packages example.api

fixture "$WORK/good" supported-public '["Example"]'
good_example "$WORK/good"
expect_pass "Javadoc before an annotation is accepted" python3 "$CHECK" --root "$WORK/good" --packages example.api

fixture "$WORK/copied" supported-public '["FirstException","SecondException","ThirdException","FourthException"]'
for type in FirstException SecondException ThirdException FourthException; do
  cat >"$WORK/copied/module/src/main/java/example/api/$type.java" <<EOF
package example.api;
/** Creates a $type for a failed operation.
 * @return diagnostic failure object
 */
public class $type {}
EOF
done
expect_fail "identifier-substituted short and tag prose is rejected" 'repeated 4' \
  python3 "$CHECK" --root "$WORK/copied" --packages example.api

# The normal launcher also validates the repository's actual four-package scope;
# fixture success alone must not be reported as acceptance evidence.
expect_pass "real four-package client Javadoc scope passes" check_real_scope

printf '%s passed, %s failed\n' "$passed" "$failed"
[ "$failed" -eq 0 ]
