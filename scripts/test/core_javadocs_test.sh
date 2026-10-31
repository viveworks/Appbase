#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
passed=0
failed=0

ok() { passed=$((passed + 1)); printf 'ok: %s\n' "$1"; }
bad() { failed=$((failed + 1)); printf 'not ok: %s\n' "$1" >&2; }

# Repetition arm: an eight-plus-word sentence that matches no banned phrase, so only the repetition
# guard can reject it, and the rejection must name the repetition.
repeat_fixture() { # <count>
    : >"$TMP/repeated.java"
    n=0
    while [ "$n" -lt "$1" ]; do
        printf '%s\n' '/** Computes the aggregate summary for the selected account within the current request context. */' >>"$TMP/repeated.java"
        n=$((n + 1))
    done
}
repeat_fixture 4
if python3 "$ROOT/scripts/check-core-javadocs.py" --boilerplate-fixture "$TMP/repeated.java" >/dev/null 2>"$TMP/repeated.err"; then
    bad 'repeated template is rejected by the repetition guard alone'
elif grep -q 'repeated 4 times' "$TMP/repeated.err" && ! grep -q 'banned phrase' "$TMP/repeated.err"; then
    ok 'repeated template is rejected by the repetition guard alone'
else
    bad 'repeated template is rejected by the repetition guard alone'
fi
repeat_fixture 3
if python3 "$ROOT/scripts/check-core-javadocs.py" --boilerplate-fixture "$TMP/repeated.java" >/dev/null 2>&1; then
    ok 'three copies of a sentence stay within the repetition ceiling'
else
    bad 'three copies of a sentence stay within the repetition ceiling'
fi

# Banned prose wrapped across comment lines must still be found.
cat >"$TMP/wrapped.java" <<'EOF'
/**
 * The returned value represents the state visible
 * when this method is invoked.
 */
EOF
if python3 "$ROOT/scripts/check-core-javadocs.py" --boilerplate-fixture "$TMP/wrapped.java" >/dev/null 2>"$TMP/wrapped.err"; then
    bad 'wrapped banned phrase is rejected'
elif grep -q 'banned phrase' "$TMP/wrapped.err"; then
    ok 'wrapped banned phrase is rejected'
else
    bad 'wrapped banned phrase is rejected'
fi

cat >"$TMP/placement.py" <<'EOF'
import importlib.util
import pathlib
import sys

spec = importlib.util.spec_from_file_location("inventory", sys.argv[1])
inventory = importlib.util.module_from_spec(spec)
spec.loader.exec_module(inventory)
source = pathlib.Path(sys.argv[2])
source.write_text("""package fixture;
@Deprecated
/** Misplaced documentation. */
public class Misplaced {
    @Deprecated
    /** Misplaced member documentation. */
    public Misplaced() {}
}
""", encoding="utf-8")
_, types = inventory.scan_java_file(str(source))
assert len(types) == 1
assert not types[0]["documented"]
assert types[0]["members"] == 1
assert types[0]["undocumented_members"] == 1
EOF
if python3 "$TMP/placement.py" "$ROOT/scripts/api-inventory.py" "$TMP/Misplaced.java"; then
    ok 'Javadoc after annotations is rejected'
else
    bad 'Javadoc after annotations is rejected'
fi

cat >"$TMP/multiline.py" <<'EOF'
import importlib.util
import pathlib
import sys

spec = importlib.util.spec_from_file_location("inventory", sys.argv[1])
inventory = importlib.util.module_from_spec(spec)
spec.loader.exec_module(inventory)
source = pathlib.Path(sys.argv[2])
mode = sys.argv[3]
if mode == "keeps-members":
    # N1: a multi-line annotation with nested annotations must neither lose later members
    # nor corrupt the type depth.
    source.write_text("""package fixture;
/** A type. */
public class Annotated {
    /** First. */
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ok"),
        @ApiResponse(responseCode = "404")
    })
    public void first() {}
    /** Second. */
    public void second() {}
    /** Third. */
    public void third() {}
}
""", encoding="utf-8")
    _, types = inventory.scan_java_file(str(source))
    assert types[0]["documented"], types
    assert types[0]["members"] == 3, types
    assert types[0]["undocumented_members"] == 0, types
else:
    # N4: Javadoc placed after a multi-line annotation is not attached to the member.
    source.write_text("""package fixture;
/** A type. */
public class Late {
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ok"),
        @ApiResponse(responseCode = "404"),
    })
    /** Misplaced. */
    public void late() {}
}
""", encoding="utf-8")
    _, types = inventory.scan_java_file(str(source))
    assert types[0]["members"] == 1, types
    assert types[0]["undocumented_members"] == 1, types
EOF
if python3 "$TMP/multiline.py" "$ROOT/scripts/api-inventory.py" "$TMP/Annotated.java" keeps-members; then
    ok 'multi-line nested annotation keeps every later member and the type depth'
else
    bad 'multi-line nested annotation keeps every later member and the type depth'
fi
if python3 "$TMP/multiline.py" "$ROOT/scripts/api-inventory.py" "$TMP/Late.java" late-doc; then
    ok 'Javadoc after a multi-line annotation is rejected'
else
    bad 'Javadoc after a multi-line annotation is rejected'
fi

# Real seven-package scope (A4-C6): the committed inventory and a fresh source scan agree and are documented.
if python3 "$ROOT/scripts/check-core-javadocs.py" >"$TMP/real.out" 2>&1 \
        && grep -q 'AC1402-1: PASS' "$TMP/real.out"; then
    ok 'real seven-package scope is documented'
else
    bad 'real seven-package scope is documented'
fi

printf 'core_javadocs_test: %s passed, %s failed\n' "$passed" "$failed"
[ "$failed" -eq 0 ]
