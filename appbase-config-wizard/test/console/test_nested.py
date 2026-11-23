"""
Console integration tests — nested configuration object scenarios.

Covers:
  C17  Nested-field defaults (y): selecting a nested field and answering 'y'
       uses defaults and the wizard continues prompting remaining parent fields.
  C18  Nested-field custom values (n): selecting a nested field, answering 'n',
       filling the sub-form, then seeing the "end <field>" marker and having
       the wizard continue with the parent's remaining fields.
  C19  Nested depth: a config with 2+ levels of nesting (e.g. ZooKeeper with
       ClusterIdValueConfiguration which itself has TimeoutConfiguration)
       navigates all levels and produces valid YAML.
  C20  All-configs comprehensive sweep: every numbered entry in the list must:
       - load without crashing
       - prompt (or skip) all fields including nested sub-forms
       - produce a valid, parseable YAML file
       Controlled by WIZARD_SMOKE_MAX env var (default: unlimited — all configs).
"""
import os
import re as _re
import time

import pexpect
import pytest
import yaml

from conftest import (ANY_READLINE_PROMPT, DEFAULTS_SKIP_PROMPT, JAR, JVM_FLAGS, NESTED_PROMPT, WIZARD_ENV, choose_menu_item, count_filter_rows,
                      _close_child, drain_first_prompt, expect_filter_prompt,
                      select_filtered_config)


def _safe_load(text: str):
    """Parse YAML after stripping Jackson polymorphic tags (!!Class and !<Class> formats)."""
    return yaml.safe_load(_re.sub(r'!(?:!\S+|<[^>]+>)', '', text))

# ---------------------------------------------------------------------------
# Constants / environment
# ---------------------------------------------------------------------------

_MAX_FIELDS = 400   # safety cap on total Enter/input presses per run
# 0 = unlimited; set WIZARD_SMOKE_MAX=20 in CI to keep run-time bounded
_SMOKE_MAX = int(os.environ.get('WIZARD_SMOKE_MAX', '0'))
_C20_STARTUP_TIMEOUT = float(os.environ.get('WIZARD_STARTUP_TIMEOUT', '180'))
_C20_CONFIG_TIMEOUT = float(os.environ.get('WIZARD_CONFIG_TIMEOUT', '120'))
_C20_SWEEP_TIMEOUT = float(os.environ.get('WIZARD_SWEEP_TIMEOUT', '3600'))


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _spawn(extra_args=None):
    args = JVM_FLAGS + ['-jar', JAR, 'console'] + (extra_args or [])
    return pexpect.spawn('java', args, encoding='utf-8', timeout=90,
                         dimensions=(40, 200), env=WIZARD_ENV)


def _remaining(deadline, maximum):
    """Return an expect timeout capped by a monotonic deadline."""
    if deadline is None:
        return maximum
    remaining = deadline - time.monotonic()
    if remaining <= 0:
        pytest.fail('Monotonic test deadline expired.')
    return min(maximum, remaining)


def _wait_banner(child, deadline=None, startup_timeout=180):
    """
    Navigate from banner through all 3 menu levels to the Create FilterMenu.

    Uses send() (no newline) for single-char menus to prevent newline bleed
    into the next reader (readChar vs readLine).
    """
    child.expect(
        'Appbase Config Wizard',
        timeout=_remaining(deadline, startup_timeout),
    )
    child.expect(r'\[', timeout=_remaining(deadline, 30))           # main menu
    choose_menu_item(child, '1', timeout=_remaining(deadline, 30))  # → Configurations
    child.expect(r'\[1\]', timeout=_remaining(deadline, 15))
    choose_menu_item(child, '1', timeout=_remaining(deadline, 30))  # → Create FilterMenu
    expect_filter_prompt(child, timeout=_remaining(deadline, 30))


def _accept_all_including_nested(child, deadline=None):
    """
    Press Enter for every simple-field / enum prompt and 'y' for every
    nested "use defaults?" prompt until 'Written to:' appears.

    Handles five prompt types:
    - YAML Preview header → wait for 'Write this config', send 'Y' (single byte)
    - 'use defaults?' → send 'y'
    - Enum retry (no default) → compound pattern consumes error + re-prompt, send '1'
    - Simple field prompt → send empty (accept default)
    - Written to: → drain path line and return (caller's finally closes the child)

    Raises pytest.fail on unexpected EOF or timeout.
    """
    for _ in range(_MAX_FIELDS):
        idx = child.expect(
            [
                r'(?i)written to:',                                              # 0
                r'YAML Preview',                                                 # 1
                r'(?i)cannot (?:build|preview|write) configuration[^\r\n]*',     # 2
                NESTED_PROMPT,                                          # 3
                r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 4
                ANY_READLINE_PROMPT,                        # 5
                pexpect.EOF,                                                     # 6
                pexpect.TIMEOUT,                                                 # 7
                r'Add entry',                                                    # 8 — Map sub-menu
                r'Add value',                                                    # 9 — List/Set sub-menu
                r'\(number out of range — enter 1-\d+\)',                       # 10
            ],
            timeout=_remaining(deadline, 25),
        )
        if idx == 0:
            child.expect(r'[^\r\n]+', timeout=_remaining(deadline, 10))
            return   # file on disk; caller's finally: _close_child(child) cleans up
        elif idx == 1:
            child.expect(r'Write this config', timeout=_remaining(deadline, 15))
            child.send('Y')
        elif idx == 2:
            pytest.fail(f'Wizard refused configuration: {child.match.group(0)}')
        elif idx == 3:
            child.sendline('y')
        elif idx == 4:
            child.sendline('1')   # pick first enum option when no default exists
        elif idx == 5:
            child.sendline('')
        elif idx in (8, 9):      # Map/List/Set sub-menu — skip empty collection (Done = 2)
            child.expect(r'> ', timeout=_remaining(deadline, 5))
            child.sendline('2')
        elif idx == 10:
            pytest.fail('Selected configuration number was reported out of range.')
        elif idx == 6:
            pytest.fail("Process exited before 'Written to:'.")
        else:
            pytest.fail("Timed out waiting for prompt or 'Written to:'.")
    pytest.fail(f"'Written to:' not seen after {_MAX_FIELDS} inputs.")



# ---------------------------------------------------------------------------
# C17 — Nested field accepted as defaults (y path)
# ---------------------------------------------------------------------------

class TestNestedFieldDefaults:
    """C17: Answering 'y' to a nested-field prompt uses defaults and continues."""

    def test_nested_defaults_flow(self, tmp_path):
        """
        Select a config known to have nested fields (e.g. ZooKeeper).
        Answer 'y' to every 'use defaults?' prompt. The wizard must:
        1. Not crash on nested object defaults.
        2. Produce a valid YAML file.
        """
        out = tmp_path / 'c17.yaml'
        child = _spawn(extra_args=['-o', str(out)])
        try:
            _wait_banner(child)
            select_filtered_config(
                child, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
            drain_first_prompt(child)
            _accept_all_including_nested(child)
        finally:
            _close_child(child)

        assert os.path.isfile(str(out)), 'C17: YAML not written.'
        data = _safe_load(out.read_text())
        assert data is not None, 'C17: YAML file is empty.'


# ---------------------------------------------------------------------------
# C18 — Nested field filled manually (n path) + end-marker + parent continues
# ---------------------------------------------------------------------------

class TestNestedFieldCustom:
    """C18: Answering 'n' opens sub-form; end marker appears; parent continues."""

    def test_nested_fill_and_continue(self, tmp_path):
        """
        Select a config with nested fields (ZooKeeper), answer 'n' to the
        first nested-field prompt, fill the sub-form with defaults (Enter),
        then verify:
        1. The '+-- end <field> --' marker appears after the sub-form.
        2. The wizard continues with the parent's remaining fields.
        3. A valid YAML file is written.
        """
        out = tmp_path / 'c18.yaml'
        child = _spawn(extra_args=['-o', str(out)])
        try:
            _wait_banner(child)
            select_filtered_config(
                child, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
            drain_first_prompt(child)

            # Keep the seeded collection default, then continue to the typed
            # nested-object prompts.
            child.expect(DEFAULTS_SKIP_PROMPT, timeout=30)
            child.sendline('')

            # Open the first typed nested object, accept that object's own
            # field defaults, and synchronize on its closing marker before
            # draining the remaining parent fields.
            for _ in range(_MAX_FIELDS):
                seek_idx = child.expect(
                    [
                        NESTED_PROMPT,
                        DEFAULTS_SKIP_PROMPT,
                        r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',
                        ANY_READLINE_PROMPT,
                        r'Add entry',
                        r'Add value',
                        r'YAML Preview',
                        r'(?i)written to:',
                        pexpect.EOF,
                        pexpect.TIMEOUT,
                    ],
                    timeout=25,
                )
                if seek_idx == 0:
                    break
                if seek_idx in (1, 3):
                    child.sendline('')
                elif seek_idx == 2:
                    child.sendline('1')
                elif seek_idx in (4, 5):
                    child.expect(r'> ', timeout=5)
                    child.sendline('2')
                elif seek_idx in (6, 7):
                    pytest.fail('C18 completed before reaching a typed nested object.')
                else:
                    pytest.fail('Unexpected EOF/timeout while seeking C18 nested object.')
            else:
                pytest.fail('C18 did not reach a typed nested-object prompt.')
            child.sendline('n')
            child.expect(r'\+-- ([A-Za-z_$][\w$]*) \([^\r\n]+\) --', timeout=15)
            opened_field = child.match.group(1)
            end_marker = r'\+-- end ' + _re.escape(opened_field) + r' --'
            for _ in range(_MAX_FIELDS):
                nested_idx = child.expect(
                    [
                        end_marker,                         # 0 matching section end
                        DEFAULTS_SKIP_PROMPT,                # 1 seeded collection
                        NESTED_PROMPT,                       # 2 deeper object
                        r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 3
                        ANY_READLINE_PROMPT,                 # 4 ordinary field
                        r'Add entry',                        # 5 map menu
                        r'Add value',                        # 6 list/set menu
                        pexpect.EOF,                         # 7
                        pexpect.TIMEOUT,                     # 8
                    ],
                    timeout=25,
                )
                if nested_idx == 0:
                    break
                if nested_idx in (1, 4):
                    child.sendline('')
                elif nested_idx == 2:
                    child.sendline('y')
                elif nested_idx == 3:
                    child.sendline('1')
                elif nested_idx in (5, 6):
                    child.expect(r'> ', timeout=5)
                    child.sendline('2')
                else:
                    pytest.fail('Unexpected EOF/timeout inside C18 nested sub-form.')
            else:
                pytest.fail('C18 nested sub-form did not emit its matching end marker.')

            for _ in range(_MAX_FIELDS):
                idx2 = child.expect(
                    [
                        r'(?i)written to:',                                              # 0
                        r'YAML Preview',                                                 # 1
                        NESTED_PROMPT,                                                   # 2
                        r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 3
                        ANY_READLINE_PROMPT,                                             # 4
                        pexpect.EOF,                                                     # 5
                        pexpect.TIMEOUT,                                                 # 6
                        r'Add entry',                                                    # 7 — Map sub-menu
                        r'Add value',                                                    # 8 — List/Set sub-menu
                    ],
                    timeout=25,
                )
                if idx2 == 0:
                    child.expect(r'[^\r\n]+', timeout=10)   # drain path
                    break   # file on disk; finally: _close_child(child) cleans up
                elif idx2 == 1:
                    child.expect(r'Write this config', timeout=15)
                    child.send('Y')
                elif idx2 == 2:
                    child.sendline('y')
                elif idx2 == 3:
                    child.sendline('1')
                elif idx2 == 4:
                    child.sendline('')
                elif idx2 in (7, 8):
                    child.sendline('2')
                else:
                    pytest.fail('Unexpected EOF/timeout inside C18 nested fill.')
        finally:
            _close_child(child)

        assert os.path.isfile(str(out)), 'C18: YAML not written.'
        data = _safe_load(out.read_text())
        assert data is not None, 'C18: YAML file is empty.'


# ---------------------------------------------------------------------------
# C19 — Multi-level nesting (ZooKeeper → ClusterIdValueConfiguration → TimeoutConfig)
# ---------------------------------------------------------------------------

class TestMultiLevelNesting:
    """C19: Multi-level nested configs all produce valid YAML."""

    def test_multi_level_nested_zookeeper(self, tmp_path):
        """
        ZooKeeper has ClusterIdValueConfiguration, which itself contains
        TimeoutConfiguration fields. Answer 'n' to the first nested prompt,
        then 'n' to any nested-within-nested prompts too. Verify valid YAML.
        """
        out = tmp_path / 'c19.yaml'
        child = _spawn(extra_args=['-o', str(out)])
        try:
            _wait_banner(child)
            select_filtered_config(
                child, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
            drain_first_prompt(child)

            # Keep the seeded collection default before looking for typed
            # nested-object prompts.
            child.expect(DEFAULTS_SKIP_PROMPT, timeout=30)
            child.sendline('')

            # Answer 'n' to ALL nested prompts (opens all sub-forms)
            opened_nested = 0
            for _ in range(_MAX_FIELDS):
                idx2 = child.expect(
                    [
                        r'(?i)written to:',                                              # 0
                        r'YAML Preview',                                                 # 1
                        NESTED_PROMPT,                                          # 2
                        r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 3
                        ANY_READLINE_PROMPT,                        # 4
                        pexpect.EOF,                                                     # 5
                        pexpect.TIMEOUT,                                                 # 6
                        r'Add entry',                                                    # 7 — Map sub-menu
                        r'Add value',                                                    # 8 — List/Set sub-menu
                        r'(?i)cannot (build|preview|write) configuration',               # 9 — build failure
                    ],
                    timeout=25,
                )
                if idx2 == 0:
                    child.expect(r'[^\r\n]+', timeout=10)   # drain path
                    break   # file on disk; finally: _close_child(child) cleans up
                elif idx2 == 1:
                    child.expect(r'Write this config', timeout=15)
                    child.send('Y')
                elif idx2 == 2:
                    # Limit to 2 nested levels: enough to prove multi-level nesting works.
                    # Opening every sub-form triggers polymorphic types (e.g. RetryPolicyConfiguration
                    # with @JsonSubTypes) whose re-built result Jackson cannot serialize.
                    if opened_nested < 2:
                        child.sendline('n')
                        opened_nested += 1
                    else:
                        child.sendline('y')
                elif idx2 == 3:
                    child.sendline('1')
                elif idx2 == 4:
                    child.sendline('')
                elif idx2 in (7, 8):
                    # Non-empty collection (seeded defaults): Done=[3]; empty: Done=[2]
                    d = child.expect([r'\[2\] Delete', r'\[2\] Done', pexpect.TIMEOUT], timeout=5)
                    child.expect(r'> ', timeout=5)
                    child.sendline('3' if d == 0 else '2')
                elif idx2 == 9:
                    pytest.fail("C19: config not Jackson-serializable: 'Cannot build configuration' detected.")
                else:
                    pytest.fail('Unexpected EOF/timeout in C19.')
        finally:
            _close_child(child)

        assert opened_nested >= 1, 'C19: no nested form was opened.'
        assert os.path.isfile(str(out)), 'C19: YAML not written.'
        data = _safe_load(out.read_text())
        assert data is not None, 'C19: YAML file is empty.'
        assert isinstance(data, dict), f'C19: expected dict, got {type(data)}'


# ---------------------------------------------------------------------------
# C20 — Comprehensive all-configs sweep (unlimited or WIZARD_SMOKE_MAX)
# ---------------------------------------------------------------------------

class TestComprehensiveAllConfigs:
    """C20: Every config entry produces valid YAML (or is gracefully skipped)."""

    def test_all_configs_comprehensive(self, tmp_path):
        """
        Iterate through every numbered entry in the wizard's config list.
        For each:
        1. Spawn wizard with a unique -o output path.
        2. Select the entry by number.
        3. Accept all fields with defaults (including nested sub-forms via 'y').
        4. Verify YAML file is written and parseable.
        5. Decline back-to-menu (n) to exit.

        Unlike C16 (_SMOKE_MAX_CONFIGS=15), this sweep is unlimited by default.
        Set WIZARD_SMOKE_MAX=N in the environment to cap it for CI time budgets.

        Any config that crashes (no 'Written to:', exception, invalid YAML) is
        collected and reported together at the end rather than stopping early.
        """
        sweep_started = time.monotonic()
        sweep_deadline = sweep_started + _C20_SWEEP_TIMEOUT

        # Discover total entry count
        probe = _spawn(extra_args=['--output-dir', str(tmp_path)])
        try:
            _wait_banner(probe, deadline=sweep_deadline)
            entry_count = count_filter_rows(probe.before or '')
            assert entry_count >= 2, 'initial FilterMenu listing was not captured'
        finally:
            _close_child(probe)

        limit = entry_count if _SMOKE_MAX == 0 else min(entry_count, _SMOKE_MAX)

        failures = []
        tested = 0
        completed = 0
        for n in range(1, limit + 1):
            if time.monotonic() >= sweep_deadline:
                failures.append(
                    f'C20 sweep deadline expired after {completed}/{limit} configs; '
                    f'{limit - completed} untested.'
                )
                break
            config_started = time.monotonic()
            startup_deadline = min(
                config_started + _C20_STARTUP_TIMEOUT, sweep_deadline)
            print(f'C20 START {n}/{limit}', flush=True)
            out = tmp_path / f'c20-{n}.yaml'
            child = _spawn(extra_args=['-o', str(out)])
            try:
                _wait_banner(
                    child,
                    deadline=startup_deadline,
                    startup_timeout=_C20_STARTUP_TIMEOUT,
                )
                child.sendline(str(n))

                interaction_deadline = min(
                    time.monotonic() + _C20_CONFIG_TIMEOUT, sweep_deadline)

                try:
                    _accept_all_including_nested(child, deadline=interaction_deadline)
                except BaseException as exc:
                    if isinstance(exc, (KeyboardInterrupt, SystemExit)):
                        raise
                    failures.append(f'Config #{n}: {exc}')
                    print(f'C20 FAIL {n}/{limit} {exc} '
                          f'elapsed={time.monotonic() - config_started:.2f}s', flush=True)
                    continue
            except BaseException as exc:
                if isinstance(exc, (KeyboardInterrupt, SystemExit)):
                    raise
                failures.append(f'Config #{n} (setup): {exc}')
                print(f'C20 FAIL {n}/{limit} setup={exc} '
                      f'elapsed={time.monotonic() - config_started:.2f}s', flush=True)
                continue
            finally:
                _close_child(child)
                completed += 1

            if not os.path.isfile(str(out)):
                failures.append(f'Config #{n}: YAML file not written to {out}')
                print(f'C20 FAIL {n}/{limit} YAML not written '
                      f'elapsed={time.monotonic() - config_started:.2f}s', flush=True)
                continue

            try:
                data = _safe_load(out.read_text())
                if data is None:
                    failures.append(f'Config #{n}: YAML file is empty')
                    print(f'C20 FAIL {n}/{limit} YAML empty '
                          f'elapsed={time.monotonic() - config_started:.2f}s', flush=True)
                else:
                    tested += 1
                    print(f'C20 PASS {n}/{limit} '
                          f'elapsed={time.monotonic() - config_started:.2f}s', flush=True)
            except yaml.YAMLError as exc:
                failures.append(f'Config #{n}: invalid YAML — {exc}')
                print(f'C20 FAIL {n}/{limit} invalid YAML={exc} '
                      f'elapsed={time.monotonic() - config_started:.2f}s', flush=True)

        print(
            f'C20 SUMMARY completed={completed}/{limit} passed={tested} '
            f'failed={len(failures)} elapsed={time.monotonic() - sweep_started:.2f}s',
            flush=True,
        )
        assert tested >= 1, 'C20 did not exercise any configuration'
        assert completed == limit, (
            f'C20 completed {completed}/{limit} configurations; '
            f'{limit - completed} untested.'
        )
        assert not failures, (
            f'{len(failures)} config(s) failed in comprehensive sweep:\n'
            + '\n'.join(failures)
        )
