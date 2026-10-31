#!/usr/bin/env python3
"""Structural and execution-bound checks for the release-rehearsal CI wiring."""

from __future__ import annotations

import argparse
import ast
import copy
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import tempfile
import textwrap

import yaml


TARGET = "ci_release_rehearsal_test.sh"
PRIVATE_RESULT = "${{ needs.release-profile-rehearsal.result }}"
EXECUTION_TIMEOUT_SECONDS = 15


def fail(message: str) -> None:
    raise ValueError(message)


def minimal_environment(home: Path | None = None) -> dict[str, str]:
    """Return no ambient credentials/state; parsed commands still use host tools via PATH."""
    env = {
        "PATH": os.environ.get("PATH", "/usr/bin:/bin"),
        "LANG": "C",
        "LC_ALL": "C",
        "PYTHONIOENCODING": "utf-8",
    }
    if home is not None:
        home.mkdir(parents=True, exist_ok=True)
        env["HOME"] = str(home)
    return env


def load_workflow(path: str) -> dict:
    text = sys.stdin.read() if path == "-" else Path(path).read_text(encoding="utf-8")
    data = yaml.safe_load(text)
    if not isinstance(data, dict) or not isinstance(data.get("jobs"), dict):
        fail("workflow is not a YAML mapping with jobs")
    return data


def job_step(workflow: dict, job_name: str, predicate) -> dict:
    job = workflow["jobs"].get(job_name)
    if not isinstance(job, dict) or not isinstance(job.get("steps"), list):
        fail(f"missing structured job: {job_name}")
    matches = [step for step in job["steps"] if isinstance(step, dict) and predicate(step)]
    if len(matches) != 1:
        fail(f"expected one matching step in {job_name}, found {len(matches)}")
    return matches[0]


def extract_python_heredoc(command: str) -> str:
    match = re.search(
        r"^\s*python3\s+-\s+<<'PY'\s*\n(?P<body>.*?)^\s*PY\s*$",
        command,
        re.MULTILINE | re.DOTALL,
    )
    if not match:
        fail("summary run scalar has no bounded python3 <<'PY' program")
    return textwrap.dedent(match.group("body"))


def is_env_release_check(node: ast.AST) -> bool:
    if not isinstance(node, ast.Compare) or len(node.ops) != 1 or len(node.comparators) != 1:
        return False
    left = node.left
    return (
        isinstance(node.ops[0], ast.Eq)
        and isinstance(node.comparators[0], ast.Constant)
        and node.comparators[0].value == "true"
        and isinstance(left, ast.Subscript)
        and isinstance(left.value, ast.Attribute)
        and isinstance(left.value.value, ast.Name)
        and left.value.value.id == "os"
        and left.value.attr == "environ"
        and isinstance(left.slice, ast.Constant)
        and left.slice.value == "RUN_RELEASE"
    )


def is_release_result_comparison(node: ast.AST) -> bool:
    if not isinstance(node, ast.Compare) or len(node.ops) != 1 or len(node.comparators) != 1:
        return False
    if not isinstance(node.ops[0], ast.NotEq):
        return False
    left = node.left
    right = node.comparators[0]
    return (
        isinstance(left, ast.Subscript)
        and isinstance(left.value, ast.Name)
        and left.value.id == "data"
        and isinstance(left.slice, ast.Constant)
        and left.slice.value == "result"
        and isinstance(right, ast.IfExp)
        and isinstance(right.test, ast.Subscript)
        and isinstance(right.test.value, ast.Name)
        and right.test.value.id == "selected"
        and isinstance(right.test.slice, ast.Name)
        and right.test.slice.id == "name"
        and isinstance(right.body, ast.Constant)
        and right.body.value == "success"
        and isinstance(right.orelse, ast.Constant)
        and right.orelse.value == "skipped"
    )


def public_summary_program(command: str) -> tuple[str, set[str]]:
    program = extract_python_heredoc(command)
    tree = ast.parse(program)
    selected_node = None
    failed_node = None
    for statement in tree.body:
        if not isinstance(statement, ast.Assign) or len(statement.targets) != 1:
            continue
        target = statement.targets[0]
        if isinstance(target, ast.Name) and target.id == "selected":
            selected_node = statement.value
        if isinstance(target, ast.Name) and target.id == "failed":
            failed_node = statement.value
    if not isinstance(selected_node, ast.Dict):
        fail("selected gate map is not a module-scope dictionary assignment")
    selected: set[str] = set()
    release_bound = False
    for key, value in zip(selected_node.keys, selected_node.values):
        if not isinstance(key, ast.Constant) or not isinstance(key.value, str):
            fail("selected gate map contains a non-literal key")
        selected.add(key.value)
        if key.value == "release-profile-rehearsal":
            release_bound = is_env_release_check(value)
    if not release_bound:
        fail("release-profile-rehearsal is not bound to RUN_RELEASE at module scope")
    if not isinstance(failed_node, ast.DictComp):
        fail("failed gate set is not a module-scope dictionary comprehension")
    if not any(is_release_result_comparison(node) for node in ast.walk(failed_node)):
        fail("failed gate comprehension does not compare result with selected status")
    return program, selected


def execute_public_summary(command: str) -> None:
    program, selected = public_summary_program(command)
    needs = {name: {"result": "success"} for name in selected}
    env = minimal_environment()
    for name in (
        "RUN_HOSTED", "RUN_SHELL", "RUN_CONSOLE", "RUN_EMULATOR", "RUN_STACKTEST",
        "RUN_L04", "RUN_L10",
    ):
        env[name] = "true"
    env["RUN_RELEASE"] = "true"
    env["NEEDS_JSON"] = json.dumps(needs)
    good = subprocess.run(
        [sys.executable, "-c", program], env=env, capture_output=True, text=True,
        timeout=EXECUTION_TIMEOUT_SECONDS,
    )
    if good.returncode != 0:
        fail("parsed public summary program rejected an all-success fixture")
    needs["release-profile-rehearsal"]["result"] = "failure"
    env["NEEDS_JSON"] = json.dumps(needs)
    bad = subprocess.run(
        [sys.executable, "-c", program], env=env, capture_output=True, text=True,
        timeout=EXECUTION_TIMEOUT_SECONDS,
    )
    if bad.returncode == 0:
        fail("parsed public summary program ignored a failed selected rehearsal")


def execute_private_summary(step: dict) -> None:
    env_map = step.get("env")
    command = step.get("run")
    if not isinstance(env_map, dict) or not isinstance(command, str):
        fail("private summary step lacks structured env/run fields")
    if env_map.get("RELEASE_REHEARSAL_RESULT") != PRIVATE_RESULT:
        fail("private summary env is not bound to the rehearsal job result")
    with tempfile.TemporaryDirectory(prefix="rehearsal-summary-") as raw:
        root = Path(raw)
        verdict = root / ".github/scripts/ci-verify-verdict.sh"
        verdict.parent.mkdir(parents=True)
        verdict.write_text("#!/usr/bin/env bash\nprintf '%s\\n' 'verdict=PASS' 'tested_sha=sentinel' 'tier_tested_shas=sentinel'\n")
        verdict.chmod(0o755)
        summary = root / "summary.md"
        env = minimal_environment(root / "home")
        env.update({str(key): "false" for key in env_map})
        env.update({
            "MODULES": "NONE", "RUN_UNIT": "false", "RUN_CONSOLE": "false",
            "RUN_RELEASE_REHEARSAL": "true", "RELEASE_REHEARSAL_RESULT": "SENTINEL_RESULT",
            "CONSOLE_RESULT": "success", "CONSOLE_READY": "true", "INVENTORY_RESULT": "success",
            "COMPILE_RESULT": "skipped", "UNIT_RESULT": "skipped", "SHELL_RESULT": "skipped",
            "CHANGED_RESULT": "success", "STATIC_ACCEPTANCE_RESULT": "skipped",
            "GITHUB_STEP_SUMMARY": str(summary), "ImageVersion": "fixture",
        })
        result = subprocess.run(
            ["bash", "-c", command], cwd=root, env=env, capture_output=True, text=True,
            timeout=EXECUTION_TIMEOUT_SECONDS,
        )
        if result.returncode != 0:
            fail(f"parsed private summary command failed fixture execution: {result.stderr[-300:]}")
        release_rows = [
            line for line in summary.read_text(encoding="utf-8").splitlines()
            if line.startswith("| release-profile-rehearsal |")
        ]
        if len(release_rows) != 1:
            fail(f"private summary emitted {len(release_rows)} release-profile rows")
        cells = [cell.strip() for cell in release_rows[0].strip("|").split("|")]
        if len(cells) != 3 or cells[-1] != "SENTINEL_RESULT":
            fail("private release-profile row did not consume the rehearsal result")


def execute_offline(command: str, source_root: Path | None = None) -> None:
    with tempfile.TemporaryDirectory(prefix="rehearsal-offline-") as raw:
        root = Path(raw)
        scripts = root / "scripts"
        tests = scripts / "test"
        tests.mkdir(parents=True)
        sentinel = root / ".rehearsal-executed"
        names = set(re.findall(r"[A-Za-z0-9_]+_test\.sh", command))
        names.add(TARGET)
        for name in names:
            path = tests / name
            if name == TARGET:
                path.write_text("#!/usr/bin/env bash\nprintf executed > .rehearsal-executed\n")
            else:
                path.write_text("#!/usr/bin/env bash\nexit 0\n")
            path.chmod(0o755)
        first_line = command.lstrip().splitlines()[0]
        first_token = shlex.split(first_line, posix=True)[0]
        command_path = Path(first_token)
        if not command_path.is_absolute() and len(command_path.parts) > 1:
            if source_root is None:
                fail("offline command executable does not exist in the source tree")
            source_executable = (source_root / command_path).resolve()
            if source_root.resolve() not in source_executable.parents or not source_executable.is_file():
                fail("offline command executable does not exist in the source tree")
            executable = (root / command_path).resolve()
            if root.resolve() not in executable.parents:
                fail("offline command executable escapes the fixture root")
            executable.parent.mkdir(parents=True, exist_ok=True)
            executable.write_bytes(source_executable.read_bytes())
            executable.chmod(source_executable.stat().st_mode & 0o777)
        env = minimal_environment(root / "home")
        result = subprocess.run(
            ["bash", "-c", command], cwd=root, env=env, capture_output=True, text=True,
            timeout=EXECUTION_TIMEOUT_SECONDS,
        )
        if result.returncode != 0:
            fail(f"parsed offline command failed fixture execution: {result.stderr[-300:]}")
        if not sentinel.is_file():
            fail("parsed offline command did not execute the rehearsal suite")


def verify_workflow(workflow: dict, source_root: Path | None = None) -> None:
    summary = job_step(workflow, "summary", lambda step: "run" in step and (
        "RELEASE_REHEARSAL_RESULT" in step.get("env", {}) or "NEEDS_JSON" in step.get("env", {})
    ))
    if "NEEDS_JSON" in summary.get("env", {}):
        execute_public_summary(summary["run"])
    else:
        execute_private_summary(summary)
    offline = job_step(workflow, "offline-shell-suites", lambda step: TARGET in str(step.get("run", "")))
    execute_offline(offline["run"], source_root)


def public_fixture(program: str, offline: str) -> dict:
    return yaml.safe_load(textwrap.dedent(f"""
        jobs:
          summary:
            steps:
              - env:
                  NEEDS_JSON: fixture
                run: |
{textwrap.indent(program, '                  ')}
          offline-shell-suites:
            steps:
              - run: |
{textwrap.indent(offline, '                  ')}
    """))


PUBLIC_PROGRAM = """\
python3 - <<'PY'
import json, os
needs = json.loads(os.environ['NEEDS_JSON'])
selected = {
    'release-profile-rehearsal': os.environ['RUN_RELEASE'] == 'true',
}
failed = {name: data['result'] for name, data in needs.items()
          if data['result'] != ('success' if selected[name] else 'skipped')}
if failed:
    raise SystemExit('public CI failure: ' + repr(failed))
PY
"""

PUBLIC_OFFLINE = """\
set -euo pipefail
for suite in ci_release_rehearsal_test.sh; do
  bash "scripts/test/$suite"
done
"""


def planted_control(name: str) -> None:
    workflow = public_fixture(PUBLIC_PROGRAM, PUBLIC_OFFLINE)
    verify_workflow(workflow)
    mutant = copy.deepcopy(workflow)
    control_source_root = None
    if name == "disconnected-summary":
        mutant["jobs"]["summary"]["steps"][0]["run"] = PUBLIC_PROGRAM.replace(
            "failed = {", "def unused():\n    failed = {"
        ).replace("\n          if data", "\n              if data")
    elif name == "runner-not-executed":
        mutant["jobs"]["offline-shell-suites"]["steps"][0]["run"] = (
            "set -euo pipefail\necho ci_release_rehearsal_test.sh\n"
        )
    elif name == "docstring-markers":
        body = PUBLIC_PROGRAM.split("python3 - <<'PY'\n", 1)[1].rsplit("PY\n", 1)[0]
        mutant["jobs"]["summary"]["steps"][0]["run"] = (
            "python3 - <<'PY'\n\"\"\"\n" + body + "\"\"\"\nPY\n"
        )
    elif name == "private-decoy-row":
        step = {
            "env": {"RELEASE_REHEARSAL_RESULT": PRIVATE_RESULT},
            "run": (
                'verdict_rc=0\n'
                'release_rehearsal_receipt_result="$RELEASE_REHEARSAL_RESULT"\n'
                'printf "| release-profile-rehearsal | sentinel | %s |\\n" '
                '"$release_rehearsal_receipt_result" >> "$GITHUB_STEP_SUMMARY"\n'
                'exit "$verdict_rc"\n'
            ),
        }
        mutant["jobs"]["summary"]["steps"] = [step]
        step["run"] = step["run"].replace(
            'release_rehearsal_receipt_result="$RELEASE_REHEARSAL_RESULT"',
            'release_rehearsal_receipt_result="success"',
            1,
        ).replace(
            'exit "$verdict_rc"',
            'echo "| decoy | decoy | $RELEASE_REHEARSAL_RESULT |" >> "$GITHUB_STEP_SUMMARY"\nexit "$verdict_rc"',
            1,
        )
    elif name == "runner-verifier-env":
        mutant["jobs"]["offline-shell-suites"]["steps"][0]["run"] = (
            'set -euo pipefail\nif [[ -n "${REHEARSAL_SENTINEL:-}" ]]; then\n'
            '  bash scripts/test/ci_release_rehearsal_test.sh\nfi\n'
        )
    elif name == "missing-runner":
        missing = str(Path("scripts") / "missing-fixture-runner.sh")
        mutant["jobs"]["offline-shell-suites"]["steps"][0]["run"] = f"{missing} {TARGET}\n"
        control_source_root = Path(__file__).resolve().parents[2]
    elif name == "existing-non-runner":
        helper = str(Path(".github") / "scripts" / "reactor-closure.sh")
        mutant["jobs"]["offline-shell-suites"]["steps"][0]["run"] = f"{helper} {TARGET}\n"
        control_source_root = Path(__file__).resolve().parents[2]
    else:
        fail(f"unknown control: {name}")
    try:
        verify_workflow(mutant, control_source_root)
    except (ValueError, SyntaxError):
        return
    fail(f"planted control did not fail closed: {name}")


def verify_summary_fragment(text: str) -> None:
    command = "python3 - <<'PY'\n" + textwrap.dedent(text) + "\nPY\n"
    public_summary_program(command)


def verify_offline_fragment(text: str) -> None:
    dedented = textwrap.dedent(text).lstrip("\n ")
    command = dedented
    if re.search(r"(?m)^\s*-\s+(?:name:|run:)", dedented):
        try:
            document = yaml.safe_load("steps:\n" + textwrap.indent(dedented, "  "))
            steps = document.get("steps", []) if isinstance(document, dict) else []
            matches = [step.get("run") for step in steps if isinstance(step, dict) and TARGET in str(step.get("run", ""))]
            if len(matches) != 1 or not isinstance(matches[0], str):
                fail("fragment has no unique step that runs the rehearsal suite")
            command = matches[0]
        except yaml.YAMLError:
            pass
    execute_offline(command)


def main() -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    verify = sub.add_parser("verify")
    verify.add_argument("workflow", help="workflow path, or - for stdin")
    control = sub.add_parser("control")
    control.add_argument("name", choices=(
        "disconnected-summary", "runner-not-executed", "docstring-markers",
        "private-decoy-row", "runner-verifier-env", "missing-runner", "existing-non-runner",
    ))
    sub.add_parser("summary-fragment")
    sub.add_parser("offline-fragment")
    args = parser.parse_args()
    try:
        if args.command == "verify":
            source_root = None if args.workflow == "-" else Path(args.workflow).resolve().parents[2]
            verify_workflow(load_workflow(args.workflow), source_root)
        elif args.command == "control":
            planted_control(args.name)
        elif args.command == "summary-fragment":
            verify_summary_fragment(sys.stdin.read())
        else:
            verify_offline_fragment(sys.stdin.read())
    except (OSError, ValueError, SyntaxError, KeyError, subprocess.SubprocessError) as error:
        print(f"release-rehearsal-contract: REFUSING: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
