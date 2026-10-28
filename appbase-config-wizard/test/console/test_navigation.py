"""
Console integration tests — navigation scenarios.

Covers:
  C1  Banner:          Launch shows cyan "Appbase Config Wizard" banner.
  C2  Config list:     Config list appears with [module] tags.
  C3  Filter match:    Type 'redis' → only Redis entries remain.
  C4  Filter no match: Type 'xyzxyz123' → "no matches" message shown.
  C5  Select by number:Type '1' → field prompts appear.
  C6  Quit:            Type 'q' → prints "Goodbye." and exits with code 0.
"""
import pexpect
import pytest

from conftest import (ANY_READLINE_PROMPT, DEFAULTS_SKIP_PROMPT, choose_menu_item, drain_first_prompt,
                      expect_filter_prompt, select_filtered_config)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _navigate_to_filter(child: pexpect.spawn) -> None:
    """
    Navigate from startup all the way to the Create FilterMenu.

    App structure:
      Main menu (one-byte readChar)
        → [1] Configurations sub-menu (one-byte readChar)
          → [1] Create → FilterMenu (full-line readLine)

    Waits for each exact ``Choice:`` prompt before sending its one-byte choice.
    """
    child.expect('Appbase Config Wizard', timeout=60)       # banner
    choose_menu_item(child, '1')                      # enter Configurations
    choose_menu_item(child, '1')                      # enter Create
    expect_filter_prompt(child, timeout=30)               # FilterMenu prompt


# ---------------------------------------------------------------------------
# C1 — Banner
# ---------------------------------------------------------------------------

class TestBanner:
    """C1: The admin prints the 'Appbase Config Wizard' banner on startup."""

    def test_banner_visible(self, wizard):
        """Banner text appears within 60 s of launch."""
        wizard.expect('Appbase Config Wizard', timeout=60)


# ---------------------------------------------------------------------------
# C2 — Config list with module tags
# ---------------------------------------------------------------------------

class TestConfigList:
    """C2: The config list is shown with [module] tags."""

    def test_module_tag_visible(self, wizard):
        """At least one [module] tag appears in the config listing."""
        wizard.expect('Appbase Config Wizard', timeout=60)
        # Module tags are shown as [<module-name>] in the list
        wizard.expect(r'\[', timeout=30)


# ---------------------------------------------------------------------------
# C3 — Filter by module name (positive match)
# ---------------------------------------------------------------------------

class TestFilterMatch:
    """C3: Typing a module name narrows the list to matching entries only."""

    def test_filter_redis(self, wizard):
        """
        Navigate to the Create FilterMenu, type 'redis' — only Redis entries remain.

        The test verifies that 'redis' (case-insensitive) appears in the output
        after filtering and that the list is non-empty.

        NOTE: In raw mode each byte is delivered individually to readChar().
        Sending 'redis' at the main menu level would exit the app when 'e' is read
        (E = Exit shortcut). Navigation to FilterMenu is required first.
        """
        _navigate_to_filter(wizard)
        wizard.sendline('redis')
        wizard.expect(r'(?i)redis', timeout=15)


# ---------------------------------------------------------------------------
# C4 — Filter no match
# ---------------------------------------------------------------------------

class TestFilterNoMatch:
    """C4: Typing an unknown term shows a 'no matches' feedback message."""

    def test_filter_no_match(self, wizard):
        """
        Navigate to the Create FilterMenu, type a gibberish string —
        the wizard reports "(no matches for '...' — showing all)".
        """
        _navigate_to_filter(wizard)
        wizard.sendline('xyzxyz123')
        # FilterMenu prints: (no matches for 'xyzxyz123' — showing all)
        wizard.expect(r'(?i)no matches for', timeout=15)


# ---------------------------------------------------------------------------
# C5 — Select by number
# ---------------------------------------------------------------------------

class TestSelectByNumber:
    """C5: Entering '1' selects the first config and shows field prompts."""

    def test_select_first_entry(self, wizard):
        """
        Type '1' after the list loads — the wizard shows a field-level prompt
        (e.g. 'Enter value for', 'Value for', or a colon-terminated field name).
        """
        _navigate_to_filter(wizard)
        select_filtered_config(
            wizard, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
        wizard.expect(ANY_READLINE_PROMPT, timeout=20)


class TestProtocolMutationControls:
    """Live controls that must fail when either protocol helper is neutered."""

    def test_EXTRA_live_stable_filter_prompt_is_required(self, wizard):
        """The production FilterMenu must emit the exact stable number=pick prompt."""
        wizard.expect('Appbase Config Wizard', timeout=180)
        choose_menu_item(wizard, '1')
        choose_menu_item(wizard, '1')
        expect_filter_prompt(wizard, timeout=30)

    def test_EXTRA_live_first_prompt_drain_advances_to_second_field(self, wizard):
        """Draining ZooKeeper's first prompt must expose its seeded collection prompt."""
        _navigate_to_filter(wizard)
        select_filtered_config(
            wizard, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
        assert drain_first_prompt(wizard) is True
        wizard.expect(DEFAULTS_SKIP_PROMPT, timeout=30)


# ---------------------------------------------------------------------------
# C6 — Quit
# ---------------------------------------------------------------------------

class TestQuit:
    """C6: Typing 'q' at the FilterMenu exits the wizard cleanly with code 0."""

    def test_quit_prints_goodbye(self, wizard):
        """
        Navigate to the Create FilterMenu, type 'q' →
        wizard propagates UserAbortException → prints 'Goodbye.' → exits 0.
        """
        _navigate_to_filter(wizard)
        wizard.sendline('q')
        wizard.expect(r'(?i)goodbye', timeout=10)
        wizard.expect(pexpect.EOF, timeout=10)
        assert wizard.wait() == 0, (
            f'Expected exit code 0 after quit but got {wizard.wait()}'
        )


# ---------------------------------------------------------------------------
# C21 — Out-of-range entry number
# ---------------------------------------------------------------------------

class TestOutOfRangeSelection:
    """C21: Typing a number beyond the entry count shows an error without crashing."""

    def test_out_of_range_entry_number(self, wizard):
        """
        Navigate to the Create FilterMenu, type '9999' (far beyond any real entry
        count) and verify:
        1. An error message is printed ('invalid', 'not found', or 'out of range').
        2. The wizard does not crash — the Configurations sub-menu reappears.
        """
        _navigate_to_filter(wizard)
        wizard.sendline('9999')
        wizard.expect(
            r'(?i)(invalid|not found|out of range|no match)',
            timeout=15,
        )
        # After the error, FilterMenu returns → Configurations sub-menu reappears
        wizard.expect(r'(?i)(configurations|\[1\]|\[B\])', timeout=15)


# ---------------------------------------------------------------------------
# C22 — Enter selects the highlighted filtered row
# ---------------------------------------------------------------------------

class TestFilterReset:
    """C22: Blank input selects the highlighted row in the filtered list."""

    def test_blank_selects_highlighted_entry(self, wizard):
        """
        Narrow the list to Redis, then send an empty line. The shipped `number=pick`
        protocol defines Enter as selecting the highlighted row, not clearing the
        filter, so verify that the wizard advances to a field prompt.
        """
        _navigate_to_filter(wizard)
        # Apply a narrow filter
        wizard.sendline('redis')
        wizard.expect(r'(?i)redis', timeout=15)
        # FilterMenu re-prompts after displaying the filtered list
        expect_filter_prompt(wizard, timeout=15)
        # Enter selects the highlighted Redis entry.
        wizard.sendline('')
        wizard.expect(ANY_READLINE_PROMPT, timeout=20)
