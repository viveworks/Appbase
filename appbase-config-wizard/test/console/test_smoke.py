"""
Console smoke tests — regression guard for all config classes.

Covers:
  C14  ZooKeeper config: filter by 'zookeeper', accept all defaults, verify YAML written.
       Regression guard for the boolean/int default value crash
       (FieldPrompter omitting non-CharSequence types from the values map).
  C15  Configs with numeric/boolean fields: select configs with known int/bool
       defaults (Kafka, Elasticsearch, etc.) and accept all defaults.
  C16  All-configs sweep: run through every numbered entry in the list, accept
       all defaults, verify 'Written to:' appears and decline back-to-menu.
       Limited to SMOKE_MAX_CONFIGS entries to keep CI run-time reasonable.
"""
import os
import re as _re

import pexpect
import pytest
import yaml

from conftest import (ANY_READLINE_PROMPT, JAR, JVM_FLAGS, WIZARD_ENV, _close_child, choose_menu_item, count_filter_rows, drain_first_prompt,
                      expect_filter_prompt, select_filtered_config)


def _safe_load(text: str):
    """Parse YAML after stripping Jackson polymorphic tags (!!Class and !<Class> formats)."""
    return yaml.safe_load(_re.sub(r'!(?:!\S+|<[^>]+>)', '', text))

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

_MAX_FIELDS = 400        # max Enter presses per config
_SMOKE_MAX_CONFIGS = 15  # max configs to test in all-configs sweep (time budget)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _spawn_wizard(extra_args=None):
    args = JVM_FLAGS + ['-jar', JAR, 'console'] + (extra_args or [])
    return pexpect.spawn(
        'java', args, encoding='utf-8', timeout=90, dimensions=(40, 200),
        env=WIZARD_ENV,
    )


def _wait_for_list(child):
    """
    Navigate from banner through all 3 menu levels to reach the Create FilterMenu.

    Uses send() (no newline) for single-char menu choices so the newline
    doesn't bleed into the next reader in the chain (readLine vs readChar).
    After this call the child is positioned at the FilterMenu ready for input.
    """
    child.expect('Appbase Config Wizard', timeout=60)
    child.expect(r'\[', timeout=30)                  # main menu
    choose_menu_item(child, '1')                                   # → Configurations (no '\n' bleed)
    child.expect(r'\[1\]', timeout=15)               # Configurations sub-menu
    choose_menu_item(child, '1')                                   # → Create FilterMenu (no '\n' bleed)
    expect_filter_prompt(child, timeout=30)         # FilterMenu filter prompt ready


def _accept_all_defaults_and_quit(child):
    """
    Accept all field defaults until 'Written to:' appears, then return.

    Handles four prompt types:
    - YAML Preview header → wait for 'Write this config', send 'Y' (single byte)
    - Enum retry (no default, invalid input) → compound pattern consumes error + re-prompt
    - Simple field / nested prompt → send empty (accepts default)
    - Written to: → drain path line and return (caller's finally closes the child)
    """
    for _ in range(_MAX_FIELDS):
        idx = child.expect(
            [
                r'(?i)written to:',                                              # 0
                r'YAML Preview',                                                 # 1
                r'(?i)cannot (?:build|preview|write) configuration[^\r\n]*',     # 2
                r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 3
                ANY_READLINE_PROMPT,                        # 4
                pexpect.EOF,                                                     # 5
                pexpect.TIMEOUT,                                                 # 6
                r'Add entry',                                                    # 7 — Map sub-menu
                r'Add value',                                                    # 8 — List/Set sub-menu
                r'\(number out of range — enter 1-\d+\)',                       # 9
            ],
            timeout=25,
        )
        if idx == 0:
            child.expect(r'[^\r\n]+', timeout=10)   # drain path
            return   # file is on disk; caller's finally: _close_child(child) handles cleanup
        elif idx == 1:
            child.expect(r'Write this config', timeout=15)
            child.send('Y')
        elif idx == 2:
            pytest.fail(f'Wizard refused configuration: {child.match.group(0)}')
        elif idx == 3:
            child.sendline('1')   # pick first enum option when no default exists
        elif idx == 4:
            child.sendline('')
        elif idx in (7, 8):      # Map/List/Set sub-menu — skip empty collection (Done = 2)
            child.expect(r'> ', timeout=5)
            child.sendline('2')
        elif idx == 9:
            pytest.fail('Selected configuration number was reported out of range.')
        elif idx == 5:
            pytest.fail("Process exited before 'Written to:'.")
        else:
            pytest.fail("Timed out waiting for field prompt or 'Written to:'.")
    pytest.fail(f"'Written to:' not seen after {_MAX_FIELDS} inputs.")


# ---------------------------------------------------------------------------
# C14 — ZooKeeper config (regression for boolean/int defaults)
# ---------------------------------------------------------------------------

class TestZooKeeperConfig:
    """C14: ZooKeeper config must produce valid YAML without crashing."""

    def test_zookeeper_all_defaults(self, tmp_path):
        """
        Filter the list for 'zookeeper', select the first match, accept all
        defaults, verify a valid YAML file is written.

        Regression guard: ZooKeeper config has boolean (saslEnabled) and int
        (sessionTimeoutMs) fields. The old code crashed because non-CharSequence
        defaults leaked into the Jackson convertValue map.
        """
        out = tmp_path / 'zookeeper-out.yaml'
        child = _spawn_wizard(extra_args=['-o', str(out)])
        try:
            _wait_for_list(child)
            select_filtered_config(
                child, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
            drain_first_prompt(child)

            _accept_all_defaults_and_quit(child)
        finally:
            _close_child(child)

        assert os.path.isfile(str(out)), f'ZooKeeper YAML not written to: {out}'
        data = _safe_load(out.read_text())
        assert data is not None, 'ZooKeeper YAML file is empty.'
        assert isinstance(data, (dict, list)), f'Unexpected YAML type: {type(data)}'


# ---------------------------------------------------------------------------
# C15 — Configs known to have int/bool defaults
# ---------------------------------------------------------------------------

class TestNumericBooleanDefaults:
    """C15: Configs with numeric or boolean defaults must not crash."""

    @pytest.mark.parametrize(('filter_term', 'class_name'), [
        ('kafka', 'DefaultKafkaProducerClientIntegrationServiceConfiguration'),
        ('elasticsearch', 'DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration'),
        ('redis', 'DefaultRedisClientIntegrationServiceConfiguration'),
        ('cassandra', 'DefaultCassandraClientIntegrationServiceConfiguration'),
    ])
    def test_config_with_typed_defaults(self, filter_term, class_name, tmp_path):
        """
        Filter by the given term, pick the first result, accept all defaults.
        These configs are known to have int/bool fields that previously caused
        Jackson convertValue failures.
        """
        out = tmp_path / f'{filter_term}-out.yaml'
        child = _spawn_wizard(extra_args=['-o', str(out)])
        try:
            _wait_for_list(child)
            select_filtered_config(child, class_name)
            try:
                drain_first_prompt(child)
            except (pexpect.EOF, pexpect.TIMEOUT):
                pytest.fail(f'C15 (TestNumericBooleanDefaults::test_config_with_typed_defaults[{filter_term}]): {class_name} has no promptable fields.')

            _accept_all_defaults_and_quit(child)
        finally:
            _close_child(child)

        assert os.path.isfile(str(out)), f'{filter_term} YAML not written to: {out}'
        data = _safe_load(out.read_text())
        assert data is not None, f'{filter_term} YAML is empty.'


# ---------------------------------------------------------------------------
# C16 — All-configs sweep (numbered entries 1..SMOKE_MAX_CONFIGS)
# ---------------------------------------------------------------------------

class TestAllConfigsSweep:
    """C16: First SMOKE_MAX_CONFIGS entries in the list all produce valid YAML."""

    def test_all_configs_write_yaml(self, tmp_path):
        """
        Iterate over config entries 1 through SMOKE_MAX_CONFIGS.  For each:
        1. Spawn the wizard with a fresh -o path.
        2. Select by number.
        3. Accept all field defaults.
        4. Verify the YAML file exists and is parseable.
        5. Decline back-to-menu (n) to exit cleanly.

        This is the primary regression guard: if any config fails to produce
        YAML (crash, exception, missing 'Written to:'), the test fails and
        names the offending entry number.
        """
        failures = []
        tested = 0

        # First, discover how many entries exist by scanning the list
        probe = _spawn_wizard(extra_args=['--output-dir', str(tmp_path)])
        try:
            _wait_for_list(probe)
            entry_count = count_filter_rows(probe.before or '')
            assert entry_count >= 2, 'initial FilterMenu listing was not captured'
        finally:
            _close_child(probe)

        limit = min(entry_count, _SMOKE_MAX_CONFIGS)

        for n in range(1, limit + 1):
            out = tmp_path / f'smoke-{n}.yaml'
            child = _spawn_wizard(extra_args=['-o', str(out)])
            try:
                _wait_for_list(child)
                child.sendline(str(n))

                try:
                    _accept_all_defaults_and_quit(child)
                except BaseException as exc:
                    if isinstance(exc, (KeyboardInterrupt, SystemExit)):
                        raise
                    failures.append(f'Config #{n}: {exc}')
                    continue
            finally:
                _close_child(child)

            if not os.path.isfile(str(out)):
                failures.append(f'Config #{n}: YAML file not written to {out}')
                continue

            try:
                data = _safe_load(out.read_text())
            except yaml.YAMLError as exc:
                    failures.append(f'Config #{n}: invalid YAML — {exc}')
                    continue

            if data is None:
                failures.append(f'Config #{n}: YAML file is empty')
            else:
                tested += 1

        assert tested >= 1, 'C16 did not exercise any configuration'
        assert not failures, (
            f'{len(failures)} config(s) failed during smoke sweep:\n'
            + '\n'.join(failures)
        )
