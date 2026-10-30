"""
Console integration tests — field-type validation scenarios.

Covers:
  C9  Invalid int      → re-prompt: enter 'abc', expect error, then '9090', continues.
  C10 Invalid bool     → re-prompt: enter 'maybe', expect error, then 'y', continues.
  C25 Enum direct      → detect numbered choices list, pick option 2 directly (no error path).
  C26 SIGINT           → Ctrl-C mid-form causes wizard to exit; process terminates.
  C27 Blank mandatory  → blank input for a field with no visible default does not crash wizard.
  C28 Enum by name     → submitting the option NAME (not number) is accepted without error.
  C30 SIGINT nested    → Ctrl-C during a nested sub-config prompt causes wizard to terminate.
  C33 Map interactive  → Map sub-menu: add key-value entry via [1] Add entry then Done.
  C34 List/Set interactive → List/Set sub-menu: add a value via [1] Add value then Done.
"""
import re as _re

import pexpect
import pytest

from conftest import (ANY_READLINE_PROMPT, BOOL_FIELD_PROMPT, DEFAULTS_SKIP_PROMPT, FIELD_WITH_DEFAULT_PROMPT,
                      INT_FIELD_PROMPT, NESTED_PROMPT, choose_menu_item, drain_first_prompt,
                      expect_filter_prompt, select_filtered_config)

_ANSI_RE = _re.compile(r'\x1b\[[^@-~]*[@-~]|\[[0-9;]*[mK]')

C27_PROMPTS = (
    r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',
    r'YAML Preview',
    r'(?i)(written to:|aborted)',
    FIELD_WITH_DEFAULT_PROMPT,
    NESTED_PROMPT,
    r'Add entry',
    r'Add value',
    ANY_READLINE_PROMPT,
)


def c27_is_no_default(after: str) -> bool:
    """Return whether C27's matched prompt has no visible bracketed default."""
    return '[' not in (after or '')


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _navigate_to_first_config(child: pexpect.spawn) -> None:
    """
    Navigate from the banner through all 3 menu levels and select config #1.

    App has 3 input layers:
      1. Main menu        → single-char readChar()
      2. Configurations   → single-char readChar()
      3. FilterMenu       → full-line LineReader.readLine()

    send() (no newline) for layers 1-2 prevents newline bleed into the next reader.
    """
    child.expect('Appbase Config Wizard', timeout=60)
    child.expect(r'\[', timeout=30)                                   # main menu
    choose_menu_item(child, '1')                                                    # → Configurations
    child.expect(r'\[1\]', timeout=15)                                # Configurations sub-menu
    choose_menu_item(child, '1')                                                    # → Create FilterMenu
    expect_filter_prompt(child, timeout=30)                                # FilterMenu ready
    child.sendline('1')                                                # select config #1
    drain_first_prompt(child, timeout=20)


def _navigate_back_to_filter(child: pexpect.spawn) -> None:
    """Return to the FilterMenu after aborting or declining a config write.

    After YAML write is declined the wizard returns to the Configurations
    sub-menu (prompt ``Choice: ``) rather than straight back to FilterMenu.
    This helper handles both cases so callers do not need to know which.
    """
    idx = child.expect([r'Filter', r'Choice\s*:'], timeout=30)
    if idx == 1:
        child.sendline('1')
        expect_filter_prompt(child, timeout=30)


def _skip_map_or_collection(child: pexpect.spawn) -> None:
    """Consume a Map/List/Set sub-menu and select Done with zero entries.

    Called when an inner scan loop matches ``Add entry`` or ``Add value``.
    Consumes through ``> `` and then reads ``child.before`` — which at that
    point contains the menu body between the header and the prompt — to detect
    whether a Delete option exists (shifting Done from [2] to [3]).
    """
    child.expect(r'> ', timeout=10)
    child.sendline(_done_option(child.before))


# ---------------------------------------------------------------------------
# C9 — Invalid integer → re-prompt
# ---------------------------------------------------------------------------

class TestInvalidIntField:
    """C9: Entering a non-integer for an int field triggers an error and re-prompt."""

    def test_invalid_int_then_valid(self, wizard):
        """
        Navigate to the first config, accept defaults until an int field, enter 'abc',
        expect an error/validation message, then enter '9090' and continue.

        The test verifies that:
        1. A validation error is reported after 'abc'.
        2. The prompt re-appears for the same field.
        3. Entering '9090' allows the wizard to advance.
        """
        _navigate_to_first_config(wizard)

        # Walk through fields accepting defaults until we hit an integer field.
        # Integer fields are identified by the '(int)' type annotation in the prompt,
        # e.g. "  sessionTimeoutMs (int) [30000]: ".  This is unambiguous — the old
        # broad pattern (number/timeout/etc.) falsely matched "number=select" in the
        # FilterMenu when the wizard returned there after completing a short config.
        found_int_field = False
        for _ in range(50):
            idx = wizard.expect(
                [
                    INT_FIELD_PROMPT,                                                     # 0 int field
                    r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 1 enum retry
                    r'YAML Preview',                                                     # 2 YAML preview gate
                    r'(?i)(written to:|aborted)',                                        # 3 done/abort
                    ANY_READLINE_PROMPT,                           # 4 other field
                    pexpect.EOF,                                                         # 5
                ],
                timeout=15,
            )
            if idx == 0:
                found_int_field = True
                break
            elif idx == 1:
                wizard.sendline('1')   # pick first enum option when no default exists
            elif idx == 2:
                # YAML preview — no int field found; decline write, then fail
                wizard.expect(r'Write this config', timeout=15)
                wizard.send('n')
                pytest.fail('C9: no (int) field found in first config: YAML preview reached first.')
            elif idx == 3:
                pytest.fail('C9: no (int) field found in first config: config completed first.')
            elif idx == 4:
                wizard.sendline('')
            else:
                pytest.fail('C9: no (int) field found in first config: wizard exited before one appeared.')

        if not found_int_field:
            pytest.fail('C9: no (int) field found in first config within 50 prompts.')

        # Now send invalid input
        wizard.sendline('abc')
        wizard.expect(r'(?i)(invalid|not a valid|must be|error|integer)', timeout=10)

        # The same field prompt should re-appear
        wizard.expect(ANY_READLINE_PROMPT, timeout=10)

        # Now send a valid integer
        wizard.sendline('9090')
        # The wizard should advance (either next field or completion)
        wizard.expect(
            r'(?i)(enter.value|value for|field:|: |written to:|aborted)',
            timeout=15,
        )


# ---------------------------------------------------------------------------
# C10 — Invalid boolean → re-prompt
# ---------------------------------------------------------------------------


class TestInvalidBoolField:
    """C10: Entering a non-boolean for a bool field triggers an error and re-prompt."""

    def test_invalid_bool_then_valid(self, wizard):
        """
        Navigate to the ZooKeeper integration config, find a boolean field, enter 'maybe',
        expect a validation error, then enter 'y' and continue.

        Boolean fields are identified by a hint like '(bool)', '(boolean)', 'true/false',
        'y/n', 'enabled', 'enable', 'disable', 'flag'.
        """
        wizard.expect('Appbase Config Wizard', timeout=60)
        choose_menu_item(wizard, '1')
        choose_menu_item(wizard, '1')
        expect_filter_prompt(wizard, timeout=30)
        select_filtered_config(
            wizard, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
        drain_first_prompt(wizard, timeout=20)

        found_bool_field = False
        for _ in range(50):
            idx = wizard.expect(
                [
                    BOOL_FIELD_PROMPT,        # 0 bool field
                    r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 1 enum retry
                    ANY_READLINE_PROMPT,                        # 2 other field
                    r'YAML Preview',                                                 # 3 no bool field
                    r'(?i)(written to:|aborted)',                                    # 4 done/abort
                    pexpect.EOF,                                                     # 5
                ],
                timeout=15,
            )
            if idx == 0:
                found_bool_field = True
                break
            if idx == 1:
                wizard.sendline('1')   # pick first enum option when no default exists
            elif idx == 2:
                wizard.sendline('')
            else:
                pytest.fail('C10: no boolean field found in DefaultZooKeeperClientIntegrationServiceConfiguration: wizard reached a non-field state first.')

        if not found_bool_field:
            pytest.fail('C10: no boolean field found in DefaultZooKeeperClientIntegrationServiceConfiguration within 50 prompts.')

        # Send invalid boolean value
        wizard.sendline('maybe')
        wizard.expect(r'(?i)(invalid|not a valid|must be|error|boolean|true|false)', timeout=10)

        # Re-prompt
        wizard.expect(ANY_READLINE_PROMPT, timeout=10)

        # Send valid boolean
        wizard.sendline('y')
        wizard.expect(
            r'(?i)(enter.value|value for|field:|: |YAML Preview|written to:|aborted)',
            timeout=15,
        )


# ---------------------------------------------------------------------------
# C25 — Enum direct selection (pick option 2 on first try, no error path)
# ---------------------------------------------------------------------------

class TestEnumDirectSelection:
    """C25: Selecting enum option 2 directly on first try produces no validation error."""

    def test_enum_option_selected_directly(self, wizard):
        """
        Navigate to a known Redis config, then walk fields until an enum
        with ≥2 options is found and pick option 2 directly.

        Redis configs have DisconnectedBehavior, SslProvider, and ProtocolVersion enum
        fields with ≥2 options.  Other configs may also have enums; whichever appears
        first is used.

        Fails if no enum field with ≥2 options is found within 50 prompts.
        """
        # Redis integration configuration has stable enum fields such as
        # DisconnectedBehavior/SslProvider, so select it deterministically.
        wizard.expect('Appbase Config Wizard', timeout=60)
        choose_menu_item(wizard, '1')
        choose_menu_item(wizard, '1')
        expect_filter_prompt(wizard, timeout=30)
        select_filtered_config(
            wizard, 'DefaultRedisClientIntegrationServiceConfiguration')
        drain_first_prompt(wizard)

        found_enum = False

        for _ in range(1):
            for __ in range(50):
                idx = wizard.expect(
                    [
                        r'choices:',                                                          # 0 enum list header
                        r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 1 enum retry
                        r'YAML Preview',                                                     # 2 no enum — try next
                        r'(?i)(written to:|aborted)',                                        # 3 unexpected write
                        NESTED_PROMPT,                                 # 4 nested prompt
                        r'Add entry',                                                        # 5 Map sub-menu
                        r'Add value',                                                        # 6 List/Set sub-menu
                        ANY_READLINE_PROMPT,                           # 7 any field prompt
                        pexpect.EOF,                                                         # 8
                    ],
                    timeout=15,
                )
                if idx == 0:
                    # Check if option '2.' is visible after the header.
                    try:
                        wizard.expect(r'\n\s+2\.', timeout=5)
                        found_enum = True
                    except pexpect.TIMEOUT:
                        # Only 1 option — consume selection prompt and keep walking
                        wizard.expect(r': ', timeout=10)
                        wizard.sendline('1')
                        continue
                    wizard.expect(r': ', timeout=10)
                    break   # exit inner loop with found_enum=True
                elif idx == 1:
                    wizard.sendline('1')
                elif idx in (2, 3):
                    # No enum in the known Redis config.
                    if idx == 2:
                        wizard.expect(r'Write this config', timeout=15)
                        wizard.send('n')   # single char — uses readChar in YAML confirm
                    break
                elif idx == 4:
                    wizard.sendline('y')   # keep nested defaults
                elif idx in (5, 6):
                    _skip_map_or_collection(wizard)
                elif idx == 7:
                    wizard.sendline('')
                else:
                    break   # EOF — outer loop will give up

            if found_enum:
                break

        if not found_enum:
            pytest.fail('C25: no enum field with ≥2 options found in DefaultRedisClientIntegrationServiceConfiguration.')

        # Pick option 2 directly — must NOT trigger the invalid/retry path
        wizard.sendline('2')
        idx2 = wizard.expect(
            [
                r'(?i)(invalid|not a valid|enter a number)',  # 0 validation error (bad)
                r'(?i)(enter.value|value for|field:|choices:|: |written to:)',  # 1 advanced (good)
                pexpect.EOF,                                                    # 2
                pexpect.TIMEOUT,                                                # 3
            ],
            timeout=15,
        )
        assert idx2 == 1, (
            "C25: Entering '2' for a valid enum option triggered a validation error "
            "or the wizard did not advance."
        )


# ---------------------------------------------------------------------------
# C26 — SIGINT / Ctrl-C mid-form terminates the wizard
# ---------------------------------------------------------------------------

class TestSigintMidForm:
    """C26: Ctrl-C during field prompting causes the wizard to terminate."""

    def test_sigint_mid_form_exits(self, wizard):
        """
        Navigate to the first config's field prompts, then send Ctrl-C (SIGINT).

        The wizard must terminate — pexpect receives EOF within a reasonable
        timeout.  This tests that the JVM does not deadlock or hang on interrupt.

        Exit code is not asserted (SIGINT typically yields non-zero, but the
        wizard may catch it and exit 0 via UserAbortException — either is valid).
        """
        _navigate_to_first_config(wizard)
        # Confirm we are inside a field prompt before sending interrupt
        wizard.expect(ANY_READLINE_PROMPT, timeout=20)
        wizard.sendintr()   # sends Ctrl-C to the PTY
        # Wizard must exit (EOF) — no hanging allowed
        wizard.expect(pexpect.EOF, timeout=20)


# ---------------------------------------------------------------------------
# C27 — Blank input for field with no visible default does not crash wizard
# ---------------------------------------------------------------------------

class TestBlankMandatoryField:
    """C27: Sending blank for a field without a visible default does not crash the wizard."""

    def test_blank_field_no_crash(self, wizard):
        """
        Select the exact exception configuration containing `redirectUrl`, a
        field whose prompt does NOT show a default (no '[...]' bracket). Once
        found, send blank input and verify the wizard reaches a valid next
        state (re-prompt, next field, or YAML preview) rather than crashing.

        Fields with defaults match ``FIELD_WITH_DEFAULT_PROMPT`` at index 3.
        Other readline prompts match ``ANY_READLINE_PROMPT`` at index 7, where
        the matched ``after`` text is checked for a bracketed default.

        Fails if that config no longer exposes such a field within 50 prompts.
        """
        wizard.expect('Appbase Config Wizard', timeout=60)
        choose_menu_item(wizard, '1')
        choose_menu_item(wizard, '1')
        expect_filter_prompt(wizard, timeout=30)
        select_filtered_config(
            wizard, 'AuthorizationServiceExceptionConfiguration')

        found_no_default = False
        for _ in range(1):
            for __ in range(50):
                idx = wizard.expect([*C27_PROMPTS, pexpect.EOF], timeout=15)
                if idx == 3:
                    wizard.sendline('')   # accept default, advance
                elif idx == 7:
                    if c27_is_no_default(wizard.after):
                        found_no_default = True
                        break   # found target field — exit inner loop
                    wizard.sendline('')
                elif idx == 0:
                    wizard.sendline('1')
                elif idx == 1:
                    # YAML Preview — no matching field in this config; decline and try next
                    wizard.expect(r'Write this config', timeout=15)
                    wizard.send('n')
                    break
                elif idx in (5, 6):
                    _skip_map_or_collection(wizard)
                elif idx == 4:
                    wizard.sendline('y')   # keep nested defaults
                else:
                    break   # EOF or done/abort

            if found_no_default:
                break

        if not found_no_default:
            pytest.fail(
                'C27: no field without a visible default found in AuthorizationServiceExceptionConfiguration within 50 prompts.'
            )

        # Send blank — wizard must not crash (EOF or TIMEOUT are failures)
        wizard.sendline('')
        idx2 = wizard.expect(
            [
                r'(?i)(invalid|required|must not|blank)[^\r\n]*\r?\n[^\r\n]*: ',  # 0 re-prompt (validation)
                ANY_READLINE_PROMPT,                          # 1 advanced to next field
                r'YAML Preview',                                                   # 2 jumped to preview
                pexpect.EOF,                                                       # 3 crash
                pexpect.TIMEOUT,                                                   # 4 timeout
            ],
            timeout=15,
        )
        assert idx2 != 3, 'C27: Wizard crashed (EOF) after blank input for no-default field'
        assert idx2 != 4, 'C27: Wizard timed out after blank input for no-default field'
        # idx2 in (0, 1, 2): re-prompt OR advance OR preview — all are valid wizard states


# ---------------------------------------------------------------------------
# C28 — Enum option selected by NAME (not number)
# ---------------------------------------------------------------------------

class TestEnumNameSelection:
    """C28: Selecting enum option 2 by its NAME (not number) produces no validation error."""

    def test_enum_option_selected_by_name(self, wizard):
        """
        Navigate to the exact Redis integration config, find an enum field with ≥2 options, capture
        the display name of option 2 from the choices list, then submit that
        string name at the selection prompt.

        The wizard must NOT produce a validation error — if it does, the test
        fails so the gap can be addressed (name-based enum selection broken).

        Fails if the known Redis config has no enum field with ≥2 options.
        """
        wizard.expect('Appbase Config Wizard', timeout=60)
        choose_menu_item(wizard, '1')
        choose_menu_item(wizard, '1')
        expect_filter_prompt(wizard, timeout=30)
        select_filtered_config(
            wizard, 'DefaultRedisClientIntegrationServiceConfiguration')
        drain_first_prompt(wizard)

        found_enum = False
        option_name = None
        for _cfg in range(1):
            for __ in range(50):
                idx = wizard.expect(
                    [
                        r'choices:',                                                          # 0 enum list header
                        r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 1 enum retry
                        r'YAML Preview',                                                     # 2 no enum — try next config
                        r'(?i)(written to:|aborted)',                                        # 3 unexpected write
                        NESTED_PROMPT,                                 # 4 nested prompt
                        r'Add entry',                                                        # 5 Map sub-menu
                        r'Add value',                                                        # 6 List/Set sub-menu
                        ANY_READLINE_PROMPT,                           # 7 any field prompt
                        pexpect.EOF,                                                         # 8
                    ],
                    timeout=15,
                )
                if idx == 0:
                    # Check if option '2.' is visible after the header and capture its name
                    try:
                        wizard.expect(r'\n\s+2\.\s*(\S+)', timeout=5)
                        # Strip ANSI SGR codes — JLine3 renders enum names with
                        # bold styling (\x1b[1m…\x1b[0m) that corrupts the string
                        option_name = _ANSI_RE.sub('', wizard.match.group(1)).strip()
                        found_enum = True
                    except pexpect.TIMEOUT:
                        # Only 1 option — consume selection prompt and keep walking
                        wizard.expect(r': ', timeout=10)
                        wizard.sendline('1')
                        continue
                    # Consume remaining choices display + selection prompt
                    wizard.expect(r': ', timeout=10)
                    break   # exit inner loop with found_enum=True
                elif idx == 1:
                    wizard.sendline('1')
                elif idx in (2, 3):
                    if idx == 2:
                        wizard.expect(r'Write this config', timeout=15)
                        wizard.send('n')   # single char — readChar in YAML confirm
                    break
                elif idx == 4:
                    wizard.sendline('y')   # keep nested defaults
                elif idx in (5, 6):
                    _skip_map_or_collection(wizard)
                elif idx == 7:
                    wizard.sendline('')
                else:
                    break   # EOF — outer loop will give up

            if found_enum:
                break

        if not found_enum or option_name is None:
            pytest.fail(
                'C28: no enum field with ≥2 options found in DefaultRedisClientIntegrationServiceConfiguration.'
            )

        # Submit option NAME (string) instead of the number '2'
        wizard.sendline(option_name)
        idx2 = wizard.expect(
            [
                r'(?i)(invalid|not a valid|enter a number)',                         # 0 error (bad)
                r'(?i)(enter.value|value for|field:|choices:|: |written to:)',       # 1 advanced (good)
                pexpect.EOF,                                                          # 2
                pexpect.TIMEOUT,                                                      # 3
            ],
            timeout=15,
        )
        assert idx2 == 1, (
            f'C28: Sending enum option name "{option_name}" triggered a validation error '
            'or the wizard did not advance.'
        )


# ---------------------------------------------------------------------------
# C30 — SIGINT during a nested sub-config prompt terminates the wizard
# ---------------------------------------------------------------------------

class TestSigintNestedForm:
    """C30: Ctrl-C during a nested sub-config prompt causes the wizard to terminate."""

    def test_sigint_during_nested_exits(self, wizard):
        """
        Navigate directly to ZooKeeper (known to have nested sub-config objects
        such as hostPortConfigurations and sessionTimeoutConfiguration), walk
        fields until a 'use defaults?' nested prompt appears, send 'y' to enter
        nested editing, wait for the first nested field prompt, then send
        Ctrl-C (SIGINT).

        The wizard must exit (EOF) within a reasonable timeout. This keeps the
        nested JLine interrupt path under the same strict contract as C26.

        Fails if ZooKeeper is not registered or has no nested field within 50 prompts.
        """
        wizard.expect('Appbase Config Wizard', timeout=60)
        wizard.expect(r'\[', timeout=30)
        choose_menu_item(wizard, '1')
        wizard.expect(r'\[1\]', timeout=15)
        choose_menu_item(wizard, '1')
        expect_filter_prompt(wizard, timeout=30)
        select_filtered_config(
            wizard, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
        drain_first_prompt(wizard)
        wizard.expect(DEFAULTS_SKIP_PROMPT, timeout=30)
        wizard.sendline('')  # preserve the seeded collection, then seek nested objects

        found_nested = False
        for _ in range(50):
            idx = wizard.expect(
                [
                    NESTED_PROMPT,                        # 0 nested prompt
                    r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 1 enum retry
                    r'YAML Preview',                                                     # 2 preview
                    r'(?i)(written to:|aborted)',                                        # 3 done/abort
                    ANY_READLINE_PROMPT,                           # 4 field prompt
                    pexpect.EOF,                                                         # 5
                ],
                timeout=15,
            )
            if idx == 0:
                wizard.sendline('n')   # readLine: open nested sub-config editing
                idx2 = wizard.expect(
                    [
                        ANY_READLINE_PROMPT,   # 0 first nested field
                        pexpect.TIMEOUT,                             # 1 timed out
                    ],
                    timeout=15,
                )
                if idx2 == 1:
                    pytest.fail(
                        'C30: no nested field prompt after entering ZooKeeper sub-config.'
                    )
                found_nested = True
                break
            elif idx == 1:
                wizard.sendline('1')
            elif idx in (2, 3, 5):
                break
            elif idx == 4:
                wizard.sendline('')

        if not found_nested:
            pytest.fail('C30: no nested sub-config prompt found in ZooKeeper config within 50 prompts.')

        wizard.sendintr()   # Ctrl-C inside nested form
        wizard.expect(pexpect.EOF, timeout=20)


# ---------------------------------------------------------------------------
# Helpers for collection/map menu navigation
# ---------------------------------------------------------------------------

def _done_option(before_text) -> str:
    """Return the Done menu option number for Map/List/Set menus.

    When the collection is non-empty the menu inserts a Delete option so Done
    shifts from [2] to [3].  Detecting 'Delete' in the output before '> '
    disambiguates the two cases without needing to count entries.

    ``before_text`` is ``child.before`` from pexpect which may be ``str`` or
    ``bytes`` depending on whether the spawn was configured with an encoding.
    """
    if isinstance(before_text, bytes):
        before_text = before_text.decode('utf-8', errors='replace')
    return '3' if 'Delete' in (before_text or '') else '2'


# ---------------------------------------------------------------------------
# C33 — Map field: interactive add / done flow
# ---------------------------------------------------------------------------

class TestMapFieldInteractive:
    """C33: Map field presents add/delete sub-menu; one entry can be added and confirmed."""

    def test_map_add_entry_then_done(self, wizard):
        """
        Navigate to JmxMeterRegistryConfiguration,
        walk fields until the Map sub-menu ([1] Add entry) appears, add one key-value
        pair using the sub-menu, then select Done.

        Verifies:
        1. The Map field presents an interactive sub-menu rather than a raw string prompt.
        2. Choosing [1] Add entry produces separate Key: and Value: prompts.
        3. After adding one entry the menu re-renders with Done as option [3].
        4. Selecting Done advances the wizard to the next field or YAML preview.

        Fails if the exact JMX config has no Map field within 60 prompts.
        """
        wizard.expect('Appbase Config Wizard', timeout=60)
        choose_menu_item(wizard, '1')
        choose_menu_item(wizard, '1')
        expect_filter_prompt(wizard, timeout=30)
        select_filtered_config(wizard, 'JmxMeterRegistryConfiguration')
        drain_first_prompt(wizard)

        found_map = False
        for _cfg in range(1):
            for _ in range(60):
                idx = wizard.expect(
                    [
                        r'Add entry',                                            # 0 Map sub-menu
                        r'Add value',                                            # 1 List/Set — skip
                        NESTED_PROMPT,           # 2 nested prompt
                        r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 3 enum retry
                        r'YAML Preview',                                         # 4 preview — next config
                        r'(?i)(written to:|aborted)',                            # 5 done/abort — next config
                        ANY_READLINE_PROMPT,               # 6 generic field
                        pexpect.EOF,                                             # 7
                    ],
                    timeout=20,
                )
                if idx == 0:
                    found_map = True
                    break
                elif idx == 1:
                    _skip_map_or_collection(wizard)
                elif idx == 2:
                    wizard.sendline('y')   # keep nested defaults
                elif idx == 3:
                    wizard.sendline('1')
                elif idx == 4:
                    wizard.expect(r'Write this config', timeout=15)
                    wizard.send('n')
                    break
                elif idx == 5:
                    break
                elif idx == 7:
                    break
                elif idx == 6:
                    wizard.sendline('')

            if found_map:
                break

        if not found_map:
            pytest.fail(
                'C33: no Map field found in JmxMeterRegistryConfiguration within 60 prompts.'
            )

        # Consume the rest of the printed menu (past 'Add entry') to reach '  > '
        wizard.expect(r'> ', timeout=10)
        # Choose [1] Add entry
        wizard.sendline('1')
        wizard.expect(r'Key\s*:', timeout=10)
        wizard.sendline('testKey')
        wizard.expect(r'Value\s*:', timeout=10)
        wizard.sendline('testValue')
        # After adding 1 entry the menu re-renders; Done is now option [3]
        wizard.expect(r'Add entry', timeout=15)
        wizard.expect(r'> ', timeout=10)
        wizard.sendline('3')
        # Wizard must advance to the next field or YAML preview
        idx2 = wizard.expect(
            [
                r'(?i)(enter.value|value for|field:|: |Add entry|Add value|choices:|YAML Preview|written to:)',
                pexpect.EOF,
                pexpect.TIMEOUT,
            ],
            timeout=20,
        )
        assert idx2 == 0, 'C33: Wizard did not advance after Map field Done.'


# ---------------------------------------------------------------------------
# C34 — List/Set field: interactive add / done flow
# ---------------------------------------------------------------------------

class TestCollectionFieldInteractive:
    """C34: List/Set field presents add/delete sub-menu; one value can be added and confirmed."""

    def test_collection_add_value_then_done(self, wizard):
        """
        Navigate directly to AppbaseFilterConfig (which has Set<String> allowedPaths and
        blockedPaths fields), walk its fields until the List/Set sub-menu ([1] Add value)
        appears, add one value, then select Done.

        Uses the same targeted-filter pattern as C30 (ZooKeeper): send a text filter,
        wait for the re-rendered Filter prompt, then send '1' to select the first match.

        AppbaseFilterConfig is targeted because it:
        - Directly implements AppbaseConfiguration (so it appears in the wizard)
        - Has simple Set<String> fields (allowedPaths, blockedPaths)
        - Uses the builder pattern, so its default instance is safe to construct

        Verifies:
        1. The List/Set field presents an interactive sub-menu rather than a raw string prompt.
        2. Choosing [1] Add value produces a Value: prompt.
        3. After adding one value the menu re-renders with Done as option [3].
        4. Selecting Done advances the wizard to the next field or YAML preview.

        Fails if AppbaseFilterConfig is not present in the build.
        """
        wizard.expect('Appbase Config Wizard', timeout=60)
        wizard.expect(r'\[', timeout=30)
        choose_menu_item(wizard, '1')
        wizard.expect(r'\[1\]', timeout=15)
        choose_menu_item(wizard, '1')
        # Use 'Filter \[Enter' to match ONLY the actual filter prompt, not config names
        # that happen to contain 'Filter' (e.g. AppbaseFilterConfig in the list).
        expect_filter_prompt(wizard, timeout=30)

        # Filter for AppbaseFilterConfig (same pattern as C30 for ZooKeeper)
        select_filtered_config(wizard, 'AppbaseFilterConfig')
        drain_first_prompt(wizard)

        # Walk AppbaseFilterConfig fields: Map fields first, then Set<String> allowedPaths
        found_collection = False
        for _ in range(30):
            idx = wizard.expect(
                [
                    r'Add value',                                                        # 0 List/Set sub-menu
                    r'Add entry',                                                        # 1 Map — skip
                    NESTED_PROMPT,                       # 2 nested
                    r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',  # 3 enum retry
                    r'YAML Preview',                                                     # 4 form done without Set
                    r'(?i)(written to:|aborted)',                                        # 5 form done
                    ANY_READLINE_PROMPT,                           # 6 generic field
                    pexpect.EOF,                                                         # 7
                ],
                timeout=15,
            )
            if idx == 0:
                found_collection = True
                break
            elif idx == 1:
                _skip_map_or_collection(wizard)
            elif idx == 2:
                wizard.sendline('y')   # keep nested defaults
            elif idx == 3:
                wizard.sendline('1')
            elif idx in (4, 5, 7):
                break
            elif idx == 6:
                wizard.sendline('')

        if not found_collection:
            pytest.fail(
                'C34: no Set<String> field found in AppbaseFilterConfig fields.'
            )

        # Consume the rest of the printed menu (past 'Add value') to reach '  > '
        wizard.expect(r'> ', timeout=10)
        # Choose [1] Add value
        wizard.sendline('1')
        wizard.expect(r'Value\s*:', timeout=10)
        wizard.sendline('testValue')
        # After adding 1 value the menu re-renders; Done is now option [3]
        wizard.expect(r'Add value', timeout=15)
        wizard.expect(r'> ', timeout=10)
        wizard.sendline('3')
        # Wizard must advance to the next field or YAML preview
        idx2 = wizard.expect(
            [
                r'(?i)(enter.value|value for|field:|: |Add entry|Add value|choices:|YAML Preview|written to:)',
                pexpect.EOF,
                pexpect.TIMEOUT,
            ],
            timeout=20,
        )
        assert idx2 == 0, 'C34: Wizard did not advance after List/Set field Done.'
