"""
Console integration tests — output / YAML-generation scenarios.

Covers:
  C7  All-defaults acceptance: pick config #1, press Enter for every field,
      expect 'Written to:' in output, file exists, YAML is valid.
  C8  Custom string field: pick a config, enter a custom host value, verify
      the value appears in the generated YAML.
  C11 -o flag: wizard writes YAML to the path given by -o; file exists and
      is valid YAML.
  C12 Back-to-menu: after writing, answer Y → menu appears again (loop).
  C13 Quit-after-write: after writing, answer n → exits cleanly (code 0).
  C23 ZooKeeper YAML keys: default ZooKeeper YAML contains known field keys.
  C24 Two configs in one JVM session: two distinct YAML files are written.
  C31 Redis YAML keys: default Redis YAML contains known field keys.
  C32 Kafka YAML keys: default Kafka YAML contains known field keys.
"""
import os
import re as _re

import pexpect
import pytest
import yaml

from conftest import (ANY_READLINE_PROMPT, JAR, JVM_FLAGS, NESTED_PROMPT, WIZARD_ENV, choose_menu_item,
                      _close_child, drain_first_prompt, expect_filter_prompt,
                      named_field_prompt, select_filtered_config)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

_MAX_FIELDS = 80  # safety cap to avoid infinite loops


def _strip_ansi(text: str) -> str:
    """Remove ANSI escape sequences from *text*."""
    return _re.sub(r'\x1b\[[^a-zA-Z]*[a-zA-Z]', '', text)


def _accept_all_defaults(child: pexpect.spawn) -> None:
    """
    Press Enter for every field prompt until 'Written to:' appears or EOF.

    Handles four prompt types:
    - Simple field prompts (ends in ': ') → send empty (uses default)
    - Enum prompts with no default → 'invalid' error triggers → send '1'
    - Nested 'use defaults? [y]' prompts → empty also returns 'y' (the default)
    - YAML Preview header → explicitly wait for askWrite prompt, send 'Y'
      as a single byte (no trailing newline, so no bleed into askContinue)

    Returns immediately after matching 'Written to:'.  The caller is
    responsible for handling the subsequent 'Create another?' prompt from
    askContinue.

    Raises
    ------
    pytest.fail
        If the wizard does not produce 'Written to:' within _MAX_FIELDS presses.
    """
    for _ in range(_MAX_FIELDS):
        idx = child.expect(
            [
                r'(?i)written to:',                                              # 0 — success
                r'YAML Preview',                                                 # 1 — preview header
                r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 2 — enum retry + re-prompt
                ANY_READLINE_PROMPT,                        # 3 — field prompt
                pexpect.EOF,                                                     # 4
                pexpect.TIMEOUT,                                                 # 5
                r'Add entry',                                                    # 6 — Map sub-menu
                r'Add value',                                                    # 7 — List/Set sub-menu
            ],
            timeout=20,
        )
        if idx == 0:
            return  # success
        elif idx == 1:
            # YAML preview has been printed; next call is askWrite (single-char
            # readChar).  Send 'Y' as one byte — no trailing newline to bleed
            # into the askContinue readChar that follows the write.
            child.expect(r'Write this config', timeout=15)
            child.send('Y')
        elif idx == 2:
            # Compound pattern consumed both the error message and the re-prompt.
            # Sending '1' now goes directly to the blocked readEnum, not to a
            # stale buffer entry — no extra '\n' queued for later readers.
            child.sendline('1')   # pick first enum option when default is absent
        elif idx == 3:
            child.sendline('')
        elif idx in (6, 7):       # Map/List/Set sub-menu — skip empty collection (Done = 2)
            child.expect(r'> ', timeout=5)
            child.sendline('2')
        elif idx == 4:
            pytest.fail("Process exited before 'Written to:' was seen.")
        else:
            pytest.fail("Timed out waiting for field prompt or 'Written to:'.")
    pytest.fail(f"'Written to:' not seen after {_MAX_FIELDS} field inputs.")


def _select_first_config(child: pexpect.spawn, filter_term: str = 'zookeeper') -> None:
    """
    Navigate from the banner through all 3 menu levels and select the first
    config matching *filter_term*.

    App has 3 input layers:
      1. Main menu        → single-char readChar()
      2. Configurations   → single-char readChar()
      3. FilterMenu       → full-line LineReader.readLine()

    send() (no newline) is used for layers 1-2 so the newline doesn't bleed
    into the next reader in the chain.  FilterMenu uses JLine3 LineReader which
    accepts '\n' as Enter; sendline() is used for FilterMenu input.

    Defaults to 'zookeeper' — tests that need a different config (e.g. C8 uses
    'redis' to find a top-level string host field) pass filter_term explicitly.
    """
    child.expect('Appbase Config Wizard', timeout=60)
    child.expect(r'\[', timeout=30)                                   # main menu
    choose_menu_item(child, '1')                                                    # → Configurations
    child.expect(r'\[1\]', timeout=15)                                # Configurations sub-menu
    choose_menu_item(child, '1')                                                    # → Create FilterMenu
    expect_filter_prompt(child, timeout=30)                               # FilterMenu prompt ready
    if filter_term != 'zookeeper':
        raise ValueError('deterministic helper only supports ZooKeeper')
    select_filtered_config(
        child, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
    drain_first_prompt(child)


# ---------------------------------------------------------------------------
# C7 — All-defaults acceptance
# ---------------------------------------------------------------------------

class TestAllDefaultsAcceptance:
    """C7: Accept all defaults → file written, YAML is valid."""

    def test_accept_all_defaults(self, wizard, tmp_path):
        """
        Select the first config, press Enter for every field, then verify:
        1. 'Written to: <path>' line is printed.
        2. The file exists in the output directory.
        3. The file contains parseable YAML (non-empty dict/list).

        Note: the path is read from the output dir rather than parsing the
        'Written to:' terminal line because JLine3's width-aware ANSI writer
        can wrap styled text at the terminal column boundary, truncating the
        path before the final digits and '.yaml' extension.  Pytest reuses the
        same tmp_path instance across the wizard fixture and this test method.
        """
        _select_first_config(wizard)
        _accept_all_defaults(wizard)
        # Drain the 'Written to: …' line (may be truncated due to PTY width).
        wizard.expect(r'[^\r\n]+', timeout=10)
        yaml_files = sorted(tmp_path.glob('*.yaml'))
        assert yaml_files, f'No .yaml file written to output dir: {tmp_path}'
        raw = yaml_files[0].read_text()
        cleaned = _re.sub(r'!(?:!\S+|<[^>]+>)', '', raw)
        data = yaml.safe_load(cleaned)
        assert data is not None, 'Generated YAML file is empty.'
        assert isinstance(data, (dict, list)), (
            f'Expected dict or list from YAML, got: {type(data)}'
        )


# ---------------------------------------------------------------------------
# C8 — Custom string field
# ---------------------------------------------------------------------------

class TestCustomStringField:
    """C8: Enter a custom string value → it appears in the output YAML."""

    _CUSTOM_HOST = 'test-host-custom.example.com'
    # Word-boundary pattern: matches 'host' exactly but NOT 'hostPortConfigurations'
    # (which is a nested container in ZooKeeper, not a simple string field).
    # Matches top-level string fields in common configs: host, username, password,
    # clientName, sslKeystore, redirectUrl, etc.  url\b (no leading \b) catches camelCase
    # endings like "redirectUrl" or "serverUrl".
    _HOST_FIELD_NAMES = ('host', 'hostname', 'server', 'address', 'url', 'username',
                         'password', 'clientname', 'keystore', 'namespace', 'bootstrap')

    def test_custom_host_in_yaml(self, wizard_with_output):
        """
        Filter to a Redis config (which has a top-level 'host' string field),
        walk through fields, detect the host prompt by word-boundary pattern,
        enter a custom value, then check the custom value is present in the
        written YAML file.

        Uses Redis rather than ZooKeeper because ZooKeeper's host field is nested
        inside hostPortConfigurations (a list of HostPortConfiguration objects),
        which means the field only appears inside a sub-form after declining
        'use defaults?'  — making word-boundary detection still match false
        on the container prompt name.
        """
        child, out_path = wizard_with_output
        # Redis has a top-level 'host' string field; skip if not found
        idx_cfg = child.expect(
            [r'Appbase Config Wizard', pexpect.TIMEOUT], timeout=60
        )
        if idx_cfg != 0:
            pytest.fail('C8 (TestCustomStringField::test_custom_host_in_yaml): Appbase Config Wizard banner not found within 60s.')
        child.expect(r'\[', timeout=30)
        choose_menu_item(child, '1')
        child.expect(r'\[1\]', timeout=15)
        choose_menu_item(child, '1')
        expect_filter_prompt(child, timeout=30)
        select_filtered_config(child, 'DefaultRedisClientIntegrationServiceConfiguration')
        try:
            drain_first_prompt(child)
        except (pexpect.EOF, pexpect.TIMEOUT):
            pytest.fail('C8 (TestCustomStringField::test_custom_host_in_yaml): DefaultRedisClientIntegrationServiceConfiguration has no promptable fields.')

        entered_custom = False
        for _ in range(_MAX_FIELDS):
            idx = child.expect(
                [
                    r'(?i)written to:',                                              # 0 done
                    r'YAML Preview',                                                 # 1 preview
                    NESTED_PROMPT,                                          # 2 nested default
                    named_field_prompt(self._HOST_FIELD_NAMES),                                        # 3 host field
                    r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 4 enum retry
                    ANY_READLINE_PROMPT,                        # 5 other field
                    pexpect.EOF,                                                     # 6
                    pexpect.TIMEOUT,                                                 # 7
                    r'Add entry',                                                    # 8 — Map sub-menu
                    r'Add value',                                                    # 9 — List/Set sub-menu
                ],
                timeout=20,
            )
            if idx == 0:
                break
            elif idx == 1:
                child.expect(r'Write this config', timeout=15)
                child.send('Y')
            elif idx == 2:
                child.sendline('y')   # accept nested defaults, skip sub-form
            elif idx == 3 and not entered_custom:
                child.sendline(self._CUSTOM_HOST)
                entered_custom = True
            elif idx == 3:
                child.sendline('')   # host pattern — always a real prompt
            elif idx == 5:
                child.sendline('')
            elif idx == 4:
                child.sendline('1')
            elif idx in (8, 9):
                child.expect(r'> ', timeout=5)
                child.sendline('2')
            else:
                pytest.fail('Wizard exited or timed out before completion.')

        if not entered_custom:
            pytest.fail('C8 (TestCustomStringField::test_custom_host_in_yaml): no top-level host/string field found in DefaultRedisClientIntegrationServiceConfiguration.')

        assert out_path.exists(), f'Output file not found: {out_path}'
        content = out_path.read_text()
        assert self._CUSTOM_HOST in content, (
            f'Custom host "{self._CUSTOM_HOST}" not found in generated YAML:\n{content}'
        )
        data = yaml.safe_load(content)
        assert data is not None, 'Generated YAML is empty.'


# ---------------------------------------------------------------------------
# C11 — -o / --output flag
# ---------------------------------------------------------------------------

class TestOutputFlag:
    """C11: -o /tmp/wizard-test-out.yaml flag causes YAML to be written there."""

    def test_output_flag_writes_to_specified_path(self, wizard_explicit_output):
        """
        The wizard is spawned with -o /tmp/wizard-test-out.yaml.
        After accepting all defaults, the file at that path must:
        1. Exist.
        2. Contain valid YAML.
        3. The 'Written to:' line must reference the requested path.
        """
        child, out_path = wizard_explicit_output
        _select_first_config(child)

        for _ in range(_MAX_FIELDS):
            idx = child.expect(
                [
                    r'(?i)written to:',                                   # 0 — success
                    r'YAML Preview',                                      # 1 — preview header
                    r'(?i)(invalid.*enter|enter a number|not a valid)',    # 2 — enum retry
                    ANY_READLINE_PROMPT,              # 3 — field prompt
                    pexpect.EOF,                                           # 4
                    pexpect.TIMEOUT,                                       # 5
                    r'Add entry',                                          # 6 — Map sub-menu
                    r'Add value',                                          # 7 — List/Set sub-menu
                ],
                timeout=20,
            )
            if idx == 0:
                child.expect(r'[^\r\n]+', timeout=10)
                reported_path = _strip_ansi(child.match.group(0)).strip()
                assert out_path in reported_path, (
                    f'Expected wizard to write to "{out_path}" '
                    f'but reported: "{reported_path}"'
                )
                break
            elif idx == 1:
                child.expect(r'Write this config', timeout=15)
                child.send('Y')
            elif idx == 2:
                child.sendline('1')
            elif idx == 3:
                child.sendline('')
            elif idx in (6, 7):
                child.expect(r'> ', timeout=5)
                child.sendline('2')
            else:
                pytest.fail('Wizard exited or timed out before writing output.')

        assert os.path.isfile(out_path), f'Output not found at: {out_path}'
        with open(out_path) as fh:
            raw = fh.read()
        data = yaml.safe_load(_re.sub(r'!(?:!\S+|<[^>]+>)', '', raw))
        assert data is not None, 'YAML file is empty.'


# ---------------------------------------------------------------------------
# C12 — Back to menu after write (loop behaviour)
# ---------------------------------------------------------------------------

class TestBackToMenu:
    """C12: After writing, answering Y to 'Create another?' loops to FilterMenu."""

    def test_back_to_menu_shows_list_again(self, wizard):
        """
        After 'Written to:' is printed, the wizard asks
        'Create another? [Y / n to go back]'.
        Answering Y (single char) causes runCreate() to loop, re-displaying
        the FilterMenu config list (identified by the 'q=quit' prompt).
        """
        _select_first_config(wizard)
        _accept_all_defaults(wizard)

        # Consume the path portion of the 'Written to:' line
        wizard.expect(r'[^\r\n]+', timeout=10)

        # askContinue: 'Create another? [Y / n to go back] '
        wizard.expect(r'(?i)create another', timeout=15)
        wizard.send('Y')   # single char — no trailing newline bleed

        # runCreate() loops → FilterMenu reappears with config list
        expect_filter_prompt(wizard, timeout=30)


# ---------------------------------------------------------------------------
# C13 — Quit after write
# ---------------------------------------------------------------------------

class TestQuitAfterWrite:
    """C13: Answering n to 'Create another?' navigates back and exits cleanly."""

    def test_quit_after_write_exits_cleanly(self, wizard):
        """
        After writing YAML, answer 'n' to 'Create another? [Y / n to go back]'.

        Flow:
          n → runCreate() returns → Configurations sub-menu ([B] Back to main menu)
          B → ConfigurationsConsoleFeature.run() returns → main menu ([E] Exit)
          E → ConfigWizardConsoleApp.run() breaks → prints 'Goodbye.' → exit code 0
        """
        _select_first_config(wizard)
        _accept_all_defaults(wizard)

        # Consume path
        wizard.expect(r'[^\r\n]+', timeout=10)

        # Decline to create another → runCreate() returns
        wizard.expect(r'(?i)create another', timeout=15)
        wizard.send('n')   # single char to askContinue

        # Configurations sub-menu: "  [B] Back to main menu"
        wizard.expect(r'(?i)back to main menu', timeout=10)
        choose_menu_item(wizard, 'B')

        # Main menu: "  [E] Exit"
        wizard.expect(r'\[E\]', timeout=10)
        choose_menu_item(wizard, 'E')

        wizard.expect(r'(?i)goodbye', timeout=10)
        wizard.expect(pexpect.EOF, timeout=15)
        exit_code = wizard.wait()
        assert exit_code == 0, (
            f'Expected exit code 0 after declining back-to-menu but got {exit_code}'
        )


# ---------------------------------------------------------------------------
# C23 — YAML field/value assertions
# ---------------------------------------------------------------------------

class TestYamlFieldAssertions:
    """C23: ZooKeeper default YAML contains known field keys."""

    def test_zookeeper_yaml_has_expected_keys(self, tmp_path):
        """
        Create a ZooKeeper config with all defaults, parse the written YAML,
        and assert that at least one well-known ZooKeeper field key is present.

        Known ZooKeeper config fields (from DefaultZooKeeperClientIntegrationServiceConfiguration):
          connectString, sessionTimeoutMs, connectionTimeoutMs

        This catches regressions where a field is serialized under a wrong key
        (e.g. 'connect_string' instead of 'connectString') that would cause
        runtime deserialization failures.
        """
        out = tmp_path / 'zk-fields.yaml'
        args = JVM_FLAGS + ['-jar', JAR, 'console', '-o', str(out)]
        child = pexpect.spawn('java', args, encoding='utf-8', timeout=90,
                              dimensions=(40, 200), env=WIZARD_ENV)
        try:
            child.expect('Appbase Config Wizard', timeout=60)
            child.expect(r'\[', timeout=30)
            choose_menu_item(child, '1')
            child.expect(r'\[1\]', timeout=15)
            choose_menu_item(child, '1')
            expect_filter_prompt(child, timeout=30)
            select_filtered_config(
                child, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
            drain_first_prompt(child)
            _accept_all_defaults(child)
            child.expect(r'[^\r\n]+', timeout=10)
        finally:
            _close_child(child)

        assert out.exists(), f'C23: ZooKeeper YAML not written to {out}'
        raw = _re.sub(r'!(?:!\S+|<[^>]+>)', '', out.read_text())
        data = yaml.safe_load(raw)
        assert data is not None and isinstance(data, dict), 'C23: YAML is not a dict'

        # Keys observed in DefaultZooKeeperClientIntegrationServiceConfiguration YAML output.
        # Uses nested timeout *objects* (not flat Ms integers) and hostPortConfigurations list.
        required_keys = {'hostPortConfigurations', 'retryPolicyConfiguration'}
        assert required_keys <= data.keys(), (
            f'C23: Missing ZooKeeper keys {required_keys - data.keys()}; '
            f'found YAML keys: {set(data.keys())}'
        )


# ---------------------------------------------------------------------------
# C24 — Two configs in one JVM session
# ---------------------------------------------------------------------------

class TestMultipleConfigsSession:
    """C24: Create two different configs end-to-end in a single JVM invocation."""

    def test_two_configs_in_one_session(self, wizard, tmp_path):
        """
        After writing the first config (ZooKeeper), answer 'Y' to 'Create another?'
        to loop back to the FilterMenu, then create a second config (Redis).
        Verify that two distinct .yaml files are written to the session directory.

        This catches regressions in the askContinue→runCreate loop where session
        state from the first config leaks into the second.
        """
        # ── Config 1: ZooKeeper ──────────────────────────────────────────────
        _select_first_config(wizard)
        _accept_all_defaults(wizard)
        wizard.expect(r'[^\r\n]+', timeout=10)   # drain 'Written to:' path

        # ── Loop back ────────────────────────────────────────────────────────
        wizard.expect(r'(?i)create another', timeout=15)
        wizard.send('Y')   # single char to askContinue

        # ── Config 2: Redis ──────────────────────────────────────────────────
        expect_filter_prompt(wizard, timeout=30)   # FilterMenu reappeared
        select_filtered_config(
            wizard, 'DefaultRedisClientIntegrationServiceConfiguration')
        drain_first_prompt(wizard)
        _accept_all_defaults(wizard)
        wizard.expect(r'[^\r\n]+', timeout=10)   # drain second 'Written to:' path

        # ── Verify two YAML files ────────────────────────────────────────────
        yaml_files = sorted(tmp_path.glob('*.yaml'))
        assert len(yaml_files) >= 2, (
            f'C24: Expected 2 YAML files in {tmp_path}, found: {[f.name for f in yaml_files]}'
        )
        for f in yaml_files[:2]:
            raw = _re.sub(r'!(?:!\S+|<[^>]+>)', '', f.read_text())
            data = yaml.safe_load(raw)
            assert data is not None, f'C24: YAML file {f.name} is empty or invalid'


# ---------------------------------------------------------------------------
# C31 — Redis YAML key assertions
# ---------------------------------------------------------------------------

class TestRedisYamlFieldAssertions:
    """C31: Redis default YAML contains known field keys."""

    def test_redis_yaml_has_expected_keys(self, tmp_path):
        """
        Create a Redis config with all defaults, parse the written YAML, and
        assert that at least one well-known Redis field key is present.

        Known Redis config fields (from DefaultRedisClientIntegrationServiceConfiguration):
          host, port, database, clientName, sslEnabled, disconnectedBehavior, autoReconnect

        This catches regressions where a field is serialized under a wrong key
        (e.g. 'ssl_enabled' instead of 'sslEnabled') that would break runtime
        deserialization.

        Skips if no Redis config is registered in the current build.
        """
        out = tmp_path / 'redis-fields.yaml'
        args = JVM_FLAGS + ['-jar', JAR, 'console', '-o', str(out)]
        child = pexpect.spawn('java', args, encoding='utf-8', timeout=90,
                              dimensions=(40, 200), env=WIZARD_ENV)
        try:
            child.expect('Appbase Config Wizard', timeout=60)
            child.expect(r'\[', timeout=30)
            choose_menu_item(child, '1')
            child.expect(r'\[1\]', timeout=15)
            choose_menu_item(child, '1')
            expect_filter_prompt(child, timeout=30)
            select_filtered_config(
                child, 'DefaultRedisClientIntegrationServiceConfiguration')
            drain_first_prompt(child)
            _accept_all_defaults(child)
            child.expect(r'[^\r\n]+', timeout=10)
        finally:
            _close_child(child)

        assert out.exists(), f'C31: Redis YAML not written to {out}'
        raw = _re.sub(r'!(?:!\S+|<[^>]+>)', '', out.read_text())
        data = yaml.safe_load(raw)
        assert data is not None and isinstance(data, dict), 'C31: YAML is not a dict'

        known_keys = {
            'host', 'port', 'database', 'clientName',
            'sslEnabled', 'disconnectedBehavior', 'autoReconnect',
        }
        found = known_keys & data.keys()
        assert found, (
            f'C31: None of {known_keys} found in Redis YAML keys: {set(data.keys())}'
        )


# ---------------------------------------------------------------------------
# C32 — Kafka YAML key assertions
# ---------------------------------------------------------------------------

class TestKafkaYamlFieldAssertions:
    """C32: Kafka default YAML contains known field keys."""

    def test_kafka_yaml_has_expected_keys(self, tmp_path):
        """
        Create a Kafka config with all defaults, parse the written YAML, and
        assert that its required bootstrap configuration key is present.

        DefaultKafkaProducerClientIntegrationServiceConfiguration requires
        bootstrapServerConfigurations.

        This catches regressions where a field is serialized under a wrong key
        (e.g. 'bootstrap_servers' instead of 'bootstrapServers') that would break
        runtime deserialization.

        Skips if no Kafka config is registered in the current build.
        """
        out = tmp_path / 'kafka-fields.yaml'
        args = JVM_FLAGS + ['-jar', JAR, 'console', '-o', str(out)]
        child = pexpect.spawn('java', args, encoding='utf-8', timeout=90,
                              dimensions=(40, 200), env=WIZARD_ENV)
        try:
            child.expect('Appbase Config Wizard', timeout=60)
            child.expect(r'\[', timeout=30)
            choose_menu_item(child, '1')
            child.expect(r'\[1\]', timeout=15)
            choose_menu_item(child, '1')
            expect_filter_prompt(child, timeout=30)
            select_filtered_config(
                child, 'DefaultKafkaProducerClientIntegrationServiceConfiguration')
            drain_first_prompt(child)
            _accept_all_defaults(child)
            child.expect(r'[^\r\n]+', timeout=10)
        finally:
            _close_child(child)

        assert out.exists(), f'C32: Kafka YAML not written to {out}'
        raw = _re.sub(r'!(?:!\S+|<[^>]+>)', '', out.read_text())
        data = yaml.safe_load(raw)
        assert data is not None and isinstance(data, dict), 'C32: YAML is not a dict'

        required_keys = {'bootstrapServerConfigurations'}
        assert required_keys <= data.keys(), (
            f'C32: Missing {required_keys - data.keys()} from Kafka YAML keys: '
            f'{set(data.keys())}'
        )
