"""
Pytest fixtures for appbase-config-wizard console integration tests.

Provides the `wizard` fixture that spawns the config-wizard JAR via pexpect,
and the `wizard_with_output` fixture that spans it with an explicit -o flag.
"""
import glob
import os
import re
import signal

import pexpect
import pytest

# ---------------------------------------------------------------------------
# JAR path resolution
# ---------------------------------------------------------------------------
_HERE = os.path.dirname(os.path.abspath(__file__))
_TARGET_DIR = os.path.join(_HERE, '..', '..', 'target')

# CONFIG_WIZARD_JAR is how Maven invokes this (${project.build.directory}/${project.build.finalName}.jar,
# correct for any project version). Without it -- a manual, non-Maven run -- the project version
# is not always the literal SNAPSHOT string this once hardcoded (a real release build is not), so
# fall back to picking the built jar by size rather than guessing a filename: the real executable
# artifact is far larger than the sources/shaded/javadoc jars sitting next to it, a signal that
# holds regardless of what version string is in the filename.
_JAR_CANDIDATES = [
    path for path in glob.glob(os.path.join(_TARGET_DIR, 'appbase-config-wizard-*.jar'))
    if not path.endswith(('-sources.jar', '-shaded.jar', '-javadoc.jar'))
]
_JAR_CANDIDATES.sort(key=os.path.getsize, reverse=True)
_DEFAULT_JAR = _JAR_CANDIDATES[0] if _JAR_CANDIDATES else None

_resolved_jar = os.environ.get('CONFIG_WIZARD_JAR', _DEFAULT_JAR)
if not _resolved_jar or not os.path.isfile(_resolved_jar):
    raise RuntimeError(
        f"No appbase-config-wizard jar found (JAR={_resolved_jar!r}). Build it first "
        f"(mvn package) under {_TARGET_DIR}, or set CONFIG_WIZARD_JAR to the exact path.")
JAR: str = _resolved_jar

# JVM flags required for JLine3 + unsafe-access suppression on JDK 25
JVM_FLAGS = [
    '--enable-native-access=ALL-UNNAMED',
    '--sun-misc-unsafe-memory-access=allow',
]
WIZARD_ENV = dict(os.environ, TERM='xterm-256color')

_READLINE_BOUNDARY = r'(?:(?<=\n)|(?<=\r)|(?<=\x1b=)|(?<=\x1b\[\?2004h))'
FILTER_PROMPT = r'Filter \[Enter=select highlighted, number=pick, q=quit\]: '
FIELD_PROMPT = (
    _READLINE_BOUNDARY
    + r'  [A-Za-z_$][\w$]*(?: \([^\r\n)]*\))?(?: \[[^\r\n]*\])?: '
)
FIELD_WITH_DEFAULT_PROMPT = (
    _READLINE_BOUNDARY
    + r'  [A-Za-z_$][\w$]*(?: \([^\r\n)]*\))? \[[^\r\n]*\]: '
)
ANY_READLINE_PROMPT = (
    _READLINE_BOUNDARY
    + r'(?:  (?:[A-Za-z_$][\w$]*(?: \([^\r\n)]*\))?'
      r'(?: — use defaults\?)?(?: \[[^\r\n]*\])?'
      r'|Use defaults and skip\? \[y\]|Key|Value'
      r'|Delete (?:entry|value) number): |  > )'
)
INT_FIELD_PROMPT = (
    _READLINE_BOUNDARY + r'  [A-Za-z_$][\w$]* \(int\) \[[^\]\r\n]*\]: '
)
BOOL_FIELD_PROMPT = (
    _READLINE_BOUNDARY + r'  [A-Za-z_$][\w$]* \(bool\) \[[^\]\r\n]*\]: '
)
DEFAULTS_SKIP_PROMPT = (
    _READLINE_BOUNDARY + r'  Use defaults and skip\? \[y\]: '
)
NESTED_PROMPT = (
    _READLINE_BOUNDARY
    + r'  [A-Za-z_$][\w$]* \([^\r\n)]*\) — use defaults\? \[y\]: '
)
_ANSI_ESCAPE = re.compile(
    r'\x1b(?:\[[0-?]*[ -/]*[@-~]'       # CSI
    r'|\][^\x07\x1b]*(?:\x07|\x1b\\)'  # OSC, terminated by BEL or ST
    r'|[()][0-2A-Z]'                    # DEC character-set selection
    r'|[=>]'                            # keypad modes
    r'|[@-_])'                          # remaining two-byte escapes
)


def count_filter_rows(text: str) -> int:
    """Count FilterMenu rows after removing terminal colour sequences."""
    rendered = _ANSI_ESCAPE.sub('', text or '')
    return len(re.findall(
        r'^\s*(?:→\s*)?\d+\.\s+\[', rendered, re.MULTILINE))


def named_field_prompt(names: tuple[str, ...]) -> str:
    """Return a boundary-anchored pattern for one of the named fields."""
    alternatives = '|'.join(re.escape(name) for name in names)
    return (
        _READLINE_BOUNDARY + rf'  (?i:{alternatives})'
        + r'(?: \([^\r\n)]*\))?(?: \[[^\r\n]*\])?: '
    )


def expect_filter_prompt(child: pexpect.spawn, timeout: int = 30) -> None:
    """Synchronize on the complete, stable FilterMenu prompt."""
    child.expect(FILTER_PROMPT, timeout=timeout)


def choose_menu_item(child: pexpect.spawn, choice: str,
                     timeout: int = 30) -> None:
    """Wait for ``Choice:`` before sending one byte to its ``readChar``."""
    child.expect(r'Choice: ', timeout=timeout)
    child.send(choice)


def drain_first_prompt(child: pexpect.spawn, timeout: int = 20) -> bool:
    """Accept the first FieldPrompter readLine and confirm the drain executed."""
    child.expect(ANY_READLINE_PROMPT, timeout=timeout)
    child.sendline('')
    return True


def select_filtered_config(child: pexpect.spawn, exact_name: str,
                           timeout: int = 30) -> None:
    """Filter, prove the exact displayed class is unique, and select it."""
    child.sendline(exact_name)
    expect_filter_prompt(child, timeout=timeout)
    rendered = _ANSI_ESCAPE.sub('', child.before or '')
    assert not re.search(r'no matches for', rendered, re.IGNORECASE), (
        f'Filter unexpectedly matched no rows for {exact_name!r}'
    )
    matches = re.findall(
        rf'^\s*(?:→\s*)?(\d+)\.\s+\[[^]]+\]\s+{re.escape(exact_name)}\s*$',
        rendered,
        re.MULTILINE,
    )
    assert len(matches) == 1, (
        f'Expected one exact filtered result for {exact_name!r}; '
        f'found display numbers {matches!r}'
    )
    child.sendline(matches[0])

# ---------------------------------------------------------------------------
# Terminal dimensions — wide enough for full module column output AND long
# "Written to:" paths without PTY line-wrapping (wrapping truncates the path
# at the column boundary, breaking os.path.isfile checks in tests).
# ---------------------------------------------------------------------------
TERM_ROWS = 40
TERM_COLS = 400


def _close_child(child: pexpect.spawn) -> None:
    """
    Kill pexpect child robustly.

    ``child.close(force=True)`` can raise ``ExceptionPexpect: Could not
    terminate the child`` on macOS / Python 3.14 when ptyprocess's internal
    waitpid loop fails to reap the JVM after SIGKILL.  Fall back to a direct
    ``os.kill(SIGKILL)`` + ``os.waitpid(WNOHANG)`` to avoid test ERRORs from
    cleanup code that is not part of the test's logic.
    """
    try:
        child.close(force=True)
    except Exception:
        try:
            if child.pid:
                os.kill(child.pid, signal.SIGKILL)
                os.waitpid(child.pid, os.WNOHANG)
        except (ProcessLookupError, ChildProcessError, OSError):
            pass


def _spawn(extra_args: list[str] | None = None) -> pexpect.spawn:
    """
    Spawn the config-wizard JAR as a child process with a pseudo-tty.

    Parameters
    ----------
    extra_args:
        Additional CLI arguments appended after the JAR path (e.g. ``['-o', path]``).

    Returns
    -------
    pexpect.spawn
        Configured child process; timeout is 60 s, encoding is UTF-8.
    """
    args = JVM_FLAGS + ['-jar', JAR, 'console'] + (extra_args or [])
    child = pexpect.spawn(
        'java',
        args,
        encoding='utf-8',
        timeout=60,
        dimensions=(TERM_ROWS, TERM_COLS),
        env=WIZARD_ENV,
    )
    return child


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture
def wizard(tmp_path):
    """
    Spawn the wizard in interactive console mode.

    Passes ``--output-dir <tmp>`` so the new session output-dir prompt is
    bypassed automatically — tests don't care where the file lands.

    Yields the pexpect child process; kills it (with SIGKILL) on teardown.
    """
    child = _spawn(extra_args=['--output-dir', str(tmp_path)])
    yield child
    _close_child(child)


@pytest.fixture
def wizard_with_output(tmp_path):
    """
    Spawn the wizard with ``-o <tmp_file>`` so YAML is written to a temp path.

    Yields a tuple ``(child, output_path)`` where *output_path* is a
    ``pathlib.Path`` that will contain the generated YAML after the wizard
    exits successfully.
    """
    out_file = tmp_path / 'admin-out.yaml'
    child = _spawn(extra_args=['-o', str(out_file)])
    yield child, out_file
    _close_child(child)


@pytest.fixture
def wizard_explicit_output():
    """
    Spawn the config-wizard JAR writing to a fixed known path (``/tmp/admin-test-out.yaml``).

    Matches test C11 in the spec.  Yields ``(child, path_str)``.
    """
    out_path = '/tmp/admin-test-out.yaml'
    child = _spawn(extra_args=['-o', out_path])
    yield child, out_path
    _close_child(child)
