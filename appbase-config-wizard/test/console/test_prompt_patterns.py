"""Pure-Python regression tests for pexpect prompt synchronization."""
import re
import time
from pathlib import Path

import pexpect
import pytest

from conftest import (ANY_READLINE_PROMPT, BOOL_FIELD_PROMPT, DEFAULTS_SKIP_PROMPT, FIELD_PROMPT,
                      FIELD_WITH_DEFAULT_PROMPT,
                      FILTER_PROMPT, INT_FIELD_PROMPT, NESTED_PROMPT,
                      _ANSI_ESCAPE, count_filter_rows, drain_first_prompt,
                      named_field_prompt, select_filtered_config)
from test_fields import C27_PROMPTS, c27_is_no_default
from test_nested import _wait_banner

REALISTIC_PREFIX = "\r\r\n\x1b[?2004l\x1b[?2004h"
_FIXTURE = Path(__file__).with_name('fixtures') / 'real_prompts_b249c5bfd.raw'


def _real_prompt_windows():
    """Return capture slices from the previous prompt end to each prompt end."""
    raw = _FIXTURE.read_bytes()
    ends = [match.end() for match in re.finditer(
        rb'\x1b\[\?2004h(?:Filter \[[^\]]*\]: |  [^\r\n]*?\]: )', raw,
    )]
    return raw, [
        raw[(ends[index - 1] if index else 0):end]
        for index, end in enumerate(ends)
    ]


@pytest.mark.parametrize('prompt', [
    '  zkHost [localhost]: ', '  sessionTimeoutMs (int) [30000]: ',
    '  enabled (bool) [true]: ', '  mode [DEFAULT]: ',
    '  clusterIdValueKeyConfiguration (ClusterIdValueConfiguration) — use defaults? [y]: ',
    '  Use defaults and skip? [y]: ', '  > ', '  Key: ', '  Value: ',
    '  Delete entry number: ', '  Delete value number: ',
])
def test_EXTRA_any_readline_prompt_matches_every_field_prompter_shape(prompt):
    assert re.search(ANY_READLINE_PROMPT, REALISTIC_PREFIX + prompt)


def test_EXTRA_field_prompt_matches_jline_control_prefix():
    assert re.search(FIELD_PROMPT, REALISTIC_PREFIX + '  zkHost [localhost]: ')


@pytest.mark.parametrize('prompt', [
    '  nested (Type) — use defaults? [y]: ',
    '  Use defaults and skip? [y]: ',
    'Filter [Enter=select highlighted, number=pick, q=quit]: ',
])
def test_EXTRA_typed_field_prompt_rejects_other_prompt_protocols(prompt):
    assert not re.search(FIELD_PROMPT, REALISTIC_PREFIX + prompt)


def test_EXTRA_filter_prompt_matches_visible_text_after_jline_prefix_and_specific_patterns_win():
    sample = REALISTIC_PREFIX + 'Filter [Enter=select highlighted, number=pick, q=quit]: '
    assert re.search(FILTER_PROMPT, sample)
    assert not re.search(ANY_READLINE_PROMPT, sample)

    cases = [
        (INT_FIELD_PROMPT, '  sessionTimeoutMs (int) [30000]: '),
        (BOOL_FIELD_PROMPT, '  enabled (bool) [true]: '),
        (NESTED_PROMPT, '  nested (Type) — use defaults? [y]: '),
        (DEFAULTS_SKIP_PROMPT, '  Use defaults and skip? [y]: '),
        (named_field_prompt(('host', 'hostname')), '  host [localhost]: '),
    ]
    for prefix in ('\r\n', REALISTIC_PREFIX):
        for specific, prompt in cases:
            searcher = pexpect.expect.searcher_re([
                re.compile(specific), re.compile(ANY_READLINE_PROMPT),
            ])
            buf = prefix + prompt
            assert searcher.search(buf, len(buf)) == 0


@pytest.mark.parametrize('line', [
    '\r\n\x1b[33m  Tip: value type is complex — enter values as JSON.\x1b[0m\r\n',
    '       Example for List/Set value:  ["READ","WRITE"]\r\n',
    '✓ Written to: /x\r\n',
    'Cannot build configuration: x\r\n',
])
def test_EXTRA_any_readline_prompt_rejects_status_and_help_lines(line):
    assert not re.search(ANY_READLINE_PROMPT, line)


def test_EXTRA_add_value_precedes_collection_prompt_after_tip():
    sample = (
        '\r\n\x1b[33m  Tip: value type is complex — enter values as JSON.\x1b[0m\r\n'
        '  Options:\r\n    [1] Add value\r\n'
        '    [2] Done (no values)\r\n  > '
    )
    searcher = pexpect.expect.searcher_re([
        re.compile(r'Add value'), re.compile(ANY_READLINE_PROMPT),
    ])
    assert searcher.search(sample, len(sample)) == 0


def test_EXTRA_count_filter_rows_strips_ansi_colours():
    sample = (
        '   1. \x1b[33m[m]\x1b[0m \x1b[1mA\x1b[0m\r\n'
        '   2. \x1b[33m[m]\x1b[0m \x1b[1mB\x1b[0m\r\n'
    )
    assert count_filter_rows(sample) == 2


def test_EXTRA_ansi_stripper_handles_csi_dec_and_osc_sequences():
    sample = (
        '\x1b(0\x1b[33m   1. \x1b]0;title\x07[m]\x1b[0m A\x1b(B\r\n'
        '\x1b=   2. [m] B\x1b>\r\n'
    )
    assert count_filter_rows(sample) == 2
    assert _ANSI_ESCAPE.sub('', sample) == '   1. [m] A\r\n   2. [m] B\r\n'


class _FakeChild:
    """Small pexpect-shaped double used to mutation-lock helper behaviour."""

    def __init__(self, before=''):
        self.before = before
        self.expected = []
        self.sent = []

    def expect(self, pattern, timeout):
        self.expected.append((pattern, timeout))
        return 0

    def sendline(self, value):
        self.sent.append(value)

    def send(self, value):
        self.sent.append(value)


def test_EXTRA_drain_first_prompt_waits_then_submits_empty_line():
    child = _FakeChild()
    drain_first_prompt(child, timeout=7)
    assert child.expected == [(ANY_READLINE_PROMPT, 7)]
    assert child.sent == ['']


def test_EXTRA_c20_configured_startup_timeout_is_not_reclamped_to_180():
    child = _FakeChild()

    _wait_banner(
        child,
        deadline=time.monotonic() + 300,
        startup_timeout=240,
    )

    banner_pattern, banner_timeout = child.expected[0]
    assert banner_pattern == 'Appbase Config Wizard'
    assert 180 < banner_timeout <= 240


def test_EXTRA_select_filtered_config_requires_unique_non_fallback_row():
    rendered = (
        '   1. \x1b[33m[zookeeper]\x1b[0m '
        '\x1b[1mDefaultZooKeeperClientIntegrationServiceConfiguration\x1b[0m\r\n'
    )
    child = _FakeChild(rendered)
    select_filtered_config(
        child, 'DefaultZooKeeperClientIntegrationServiceConfiguration', timeout=9)
    assert child.sent == [
        'DefaultZooKeeperClientIntegrationServiceConfiguration', '1']
    assert child.expected == [(FILTER_PROMPT, 9)]

    fallback = _FakeChild(
        "(no matches for 'typo' — showing all)\r\n" + rendered)
    with pytest.raises(AssertionError, match='matched no rows'):
        select_filtered_config(
            fallback, 'DefaultZooKeeperClientIntegrationServiceConfiguration')


def test_EXTRA_real_capture_filter_rows_and_selection():
    # Extra: lock the host capture's filtered row count and exact selection.
    raw, windows = _real_prompt_windows()
    assert b'\x1b=' not in raw
    filter_windows = [window for window in windows if b'Filter [' in window]
    assert len(filter_windows) == 2
    assert [count_filter_rows(window.decode()) for window in filter_windows] == [79, 1]

    rendered = re.sub(r'\x1b\[[^@-~]*[@-~]', '', filter_windows[1].decode())
    matches = re.findall(
        r'^\s*(?:→\s*)?(\d+)\.\s+\[[^]]+\]\s+'
        r'DefaultZooKeeperClientIntegrationServiceConfiguration\s*$',
        rendered,
        re.MULTILINE,
    )
    assert matches == ['1']
    for window in filter_windows:
        text = window.decode()
        assert re.search(FILTER_PROMPT, text)
        assert not re.search(ANY_READLINE_PROMPT, text)


def test_EXTRA_real_capture_searcher_ordering():
    # Extra: prove production prompt ordering against complete captured windows.
    raw, windows = _real_prompt_windows()

    nested_searcher = pexpect.expect.searcher_re([
        re.compile(NESTED_PROMPT), re.compile(ANY_READLINE_PROMPT),
    ])
    nested_windows = [window.decode() for window in windows
                      if b'\xe2\x80\x94 use defaults? [y]: ' in window]
    assert nested_windows
    for nested in nested_windows:
        assert nested_searcher.search(nested, len(nested)) == 0

    defaults = next(window.decode() for window in windows
                    if b'Use defaults and skip?' in window)
    defaults_searcher = pexpect.expect.searcher_re([
        re.compile(ANY_READLINE_PROMPT),
    ])
    assert defaults_searcher.search(defaults, len(defaults)) == 0
    assert defaults_searcher.start == defaults.rindex('  Use defaults')
    captured_lines = raw.decode().splitlines()
    tip = next(line for line in captured_lines if 'Tip:' in line)
    example = next(line for line in captured_lines if 'Example for' in line)
    assert not re.search(ANY_READLINE_PROMPT, tip)
    assert not re.search(ANY_READLINE_PROMPT, example)

    boolean = next(window.decode() for window in windows
                   if b'disableBlockUntilConnected' in window)
    c10_searcher = pexpect.expect.searcher_re([
        re.compile(BOOL_FIELD_PROMPT), re.compile(ANY_READLINE_PROMPT),
    ])
    assert c10_searcher.search(boolean, len(boolean)) == 0
    c27_searcher = pexpect.expect.searcher_re([
        re.compile(prompt) for prompt in C27_PROMPTS
    ])
    assert c27_searcher.search(boolean, len(boolean)) == 3
    # The pre-fix regex also returned 3 here, but matched inside the \x1b[?2004l escape; pin the start.
    assert c27_searcher.start == boolean.rindex('  disableBlockUntilConnected')
    winners = []
    for window in windows:
        text = window.decode()
        winners.append(c27_searcher.search(text, len(text)))
    assert winners == [-1, -1, 4, 7, 3, 4, 4, 4, 4, 4, 4]


def test_EXTRA_c27_no_default_uses_matched_prompt():
    # Extra: couple C27's no-default decision to its production list and helper.
    sample = '\x1b[?2004h  name: '
    searcher = pexpect.expect.searcher_re([
        re.compile(prompt) for prompt in C27_PROMPTS
    ])
    index = searcher.search(sample, len(sample))
    assert index == 7
    assert c27_is_no_default(searcher.match.group(0))
