#!/usr/bin/env bash
# Discoverable regression suite for the internal work item 1400 bootstrap Javadoc acceptance check.

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CHECK="$ROOT/scripts/test/bootstrap_javadoc_test.py"
SOURCE="$ROOT/docs/api-inventory/inventory.json"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

PASS=0
FAIL=0

ok() { printf '  ok   %s\n' "$1"; PASS=$((PASS + 1)); }
bad() { printf '  FAIL %s\n' "$1"; FAIL=$((FAIL + 1)); }

run_case() {
  name="$1"
  want="$2"
  pattern="$3"
  shift 3
  out="$WORK/out"
  "$@" >"$out" 2>&1
  rc=$?
  if [ "$rc" -eq "$want" ] && grep -q "$pattern" "$out"; then
    ok "$name"
  else
    bad "$name (wanted rc=$want and /$pattern/, got rc=$rc)"
    sed -n '1,8p' "$out" | sed 's/^/       /'
  fi
}

run_isolated_guard_case() {
  name="$1"
  fixture="$2"
  pattern="$3"
  out="$WORK/out"
  python3 - "$CHECK" "$fixture" >"$out" 2>&1 <<'PY'
import importlib.util
import json
import sys

check_path, inventory_path = sys.argv[1:]
spec = importlib.util.spec_from_file_location("bootstrap_javadoc", check_path)
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)
real_scanner = check.load_scanner()
fixture = json.load(open(inventory_path, encoding="utf-8"))


class FixtureScanner:
    scan_java_file = staticmethod(real_scanner.scan_java_file)

    @staticmethod
    def build(_root):
        return fixture, []


# Make the fixture authoritative for this arm.  A fresh-scan mismatch therefore
# cannot mask whether the zero-contribution/zero-total guard itself fired.
check.load_scanner = FixtureScanner
raise SystemExit(check.main(["--inventory", inventory_path]))
PY
  rc=$?
  if [ "$rc" -eq 1 ] && grep -q "$pattern" "$out" \
      && ! grep -q 'scope differs from a fresh authoritative scan' "$out"; then
    ok "$name"
  else
    bad "$name (wanted isolated rc=1 and /$pattern/, got rc=$rc)"
    sed -n '1,8p' "$out" | sed 's/^/       /'
  fi
}

run_isolated_tree_case() {
  # Builds a one-module temp source tree, makes a fixture inventory over it authoritative (no
  # fresh-scan drift diagnostic can mask the guard under test) and requires rc=1 plus the
  # diagnostic that belongs to the presence guard named by the arm.
  name="$1"
  mode="$2"
  pattern="$3"
  out="$WORK/out"
  tree="$WORK/tree-$mode"
  python3 - "$CHECK" "$tree" "$mode" >"$out" 2>&1 <<'PY'
import importlib.util
import json
import os
import sys

check_path, tree, mode = sys.argv[1:]
spec = importlib.util.spec_from_file_location("bootstrap_javadoc", check_path)
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)
real_scanner = check.load_scanner()

source_dir = os.path.join(tree, "mod", "src", "main", "java", "p")
os.makedirs(source_dir)
documented = "/** Documented. */\n"
bodies = {
    "missing-type": documented + "public class Present {\n}\n",
    "undocumented-type": "public class Present {\n}\n",
    "undocumented-member": documented + "public class Present {\n    public void run() {\n    }\n}\n",
    "no-artifacts": documented + "public class Present {\n}\n",
}
with open(os.path.join(source_dir, "Present.java"), "w", encoding="utf-8") as f:
    f.write("package p;\n\n" + bodies[mode])
listed = "Absent" if mode == "missing-type" else "Present"
fixture = {"artifacts": [{
    "id": "fixture", "doc_issue": "1400", "support": "supported-public", "module_path": "mod",
    "packages": [{"package": "p", "classification": "supported-public", "public_types": [listed]}],
}]}
if mode == "no-artifacts":
    fixture["artifacts"] = []


class FixtureScanner:
    scan_java_file = staticmethod(real_scanner.scan_java_file)

    @staticmethod
    def build(_root):
        return fixture, []


check.ROOT = check.Path(tree)
check.load_scanner = FixtureScanner
inventory = os.path.join(tree, "inventory.json")
json.dump(fixture, open(inventory, "w", encoding="utf-8"))
raise SystemExit(check.main(["--inventory", inventory]))
PY
  rc=$?
  if [ "$rc" -eq 1 ] && grep -q "$pattern" "$out" \
      && ! grep -q 'scope differs from a fresh authoritative scan' "$out"; then
    ok "$name"
  else
    bad "$name (wanted isolated rc=1 and /$pattern/, got rc=$rc)"
    sed -n '1,8p' "$out" | sed 's/^/       /'
  fi
}

mutate() {
  mode="$1"
  dest="$2"
  python3 - "$SOURCE" "$dest" "$mode" <<'PY'
import json
import sys

source, dest, mode = sys.argv[1:]
data = json.load(open(source, encoding="utf-8"))
artifacts = [
    artifact for artifact in data["artifacts"]
    if str(artifact.get("doc_issue")) == "1400"
    and artifact["support"] == "supported-public"
]
if mode == "empty-artifact":
    for package in artifacts[0]["packages"]:
        package["public_types"] = []
elif mode == "empty-scope":
    for artifact in artifacts:
        for package in artifact["packages"]:
            package["public_types"] = []
elif mode == "new-type-drift":
    package = next(p for p in artifacts[0]["packages"] if p["public_types"])
    package["public_types"] = package["public_types"][:-1]
json.dump(data, open(dest, "w", encoding="utf-8"), indent=2)
PY
}

printf '%s\n' 'bootstrap-javadoc acceptance arms'
run_case 'baseline checks all 87 authoritative types' 0 '87 types, 469 members' python3 "$CHECK"

for mode in empty-artifact empty-scope new-type-drift; do
  fixture="$WORK/$mode.json"
  mutate "$mode" "$fixture"
  run_case "$mode is rejected" 1 'scope differs from a fresh authoritative scan' \
    python3 "$CHECK" --inventory "$fixture"
done

fixture="$WORK/empty-artifact.json"
run_isolated_guard_case 'zero-contribution guard is isolated' "$fixture" \
  'expected artifact contributes zero public types'
fixture="$WORK/empty-scope.json"
run_isolated_guard_case 'zero-total guard is isolated' "$fixture" \
  'supported-public bootstrap scope contains zero public types'

run_isolated_tree_case 'missing-type guard is isolated' missing-type \
  'inventory type p.Absent was not found'
run_isolated_tree_case 'class-Javadoc guard is isolated' undocumented-type \
  'public type Present has no class-level Javadoc'
run_isolated_tree_case 'member-Javadoc guard is isolated' undocumented-member \
  'public type Present has 1 undocumented public member'
run_isolated_tree_case 'no-artifacts guard is isolated' no-artifacts \
  'inventory contains no supported-public artifacts assigned to internal work item 1400'

printf 'passed: %s failed: %s\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
