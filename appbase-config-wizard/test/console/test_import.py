"""
Console import tests.

Covers:
  C-IMPORT-SCAN:          Navigate to Import, enter a directory path, verify YAML files listed.
  C-IMPORT-COMPLETE:      Full import flow — select file, confirm type, save, file written.
  C-IMPORT-MALFORMED:     Malformed YAML handled gracefully (no crash).
  C-IMPORT-UNKNOWN-FIELDS: YAML with no matching config fields handled gracefully.
  C-IMPORT-SESSION-DIR:   Without --output-dir the wizard prompts for output directory.
  C29:                    Import with field editing (answer y to 'Edit fields?').

@author Navaneeth Mysore G
"""
import pexpect
import pytest

from conftest import (ANY_READLINE_PROMPT, JAR, JVM_FLAGS, WIZARD_ENV, _close_child, _spawn,
                      choose_menu_item, drain_first_prompt, expect_filter_prompt,
                      select_filtered_config)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _navigate_to_import_prompt(child: pexpect.spawn) -> None:
    """
    Navigate from the banner through the main menu to the Import "Source directory:" prompt.

    Both menus use one-byte ``readChar`` input. Synchronize on each exact
    ``Choice:`` prompt before sending its byte so adjacent choices cannot race.
    """
    child.expect('Appbase Config Wizard', timeout=60)
    choose_menu_item(child, '1')                                      # → Configurations
    choose_menu_item(child, '2')                                      # → Import
    child.expect(r'(?i)source directory', timeout=15)                # Import ready for input


# ---------------------------------------------------------------------------
# C-IMPORT-SCAN: Directory scan prompt
# ---------------------------------------------------------------------------

class TestImportDirectoryScan:
    """C-IMPORT-SCAN: The Import option shows a directory-scan prompt."""

    def test_scan_directories_prompt_appears(self, wizard):
        """
        Navigate to the Import menu option and verify the directory prompt appears.

        Steps:
        1. Spawn admin (banner visible).
        2. Enter Configurations sub-menu via '1'.
        3. Choose Import via '2'.
        4. Verify 'Source directory:' prompt is printed.
        5. Send empty string to abort and return to the sub-menu.
        """
        _navigate_to_import_prompt(wizard)
        # Send empty string — runImport returns immediately on blank input
        wizard.sendline('')
        # We should be back in the Configurations sub-menu loop
        wizard.expect(r'(?i)(configurations|\[1\]|\[B\])', timeout=15)

    def test_invalid_directory_shows_error(self, wizard):
        """
        Enter a non-existent path — the admin prints a 'Not a directory' error.

        Steps:
        1. Navigate to Import flow.
        2. Enter '/this/path/does/not/exist/xyzxyz'.
        3. Expect 'Not a directory' message.
        4. The admin returns to the Configurations sub-menu.
        """
        _navigate_to_import_prompt(wizard)
        wizard.sendline('/this/path/does/not/exist/xyzxyz')
        wizard.expect(r'(?i)not a directory', timeout=10)
        # After the error, the admin loops back to the Configurations sub-menu
        wizard.expect(r'(?i)(configurations|\[1\]|\[B\])', timeout=15)

    def test_empty_directory_shows_no_yaml_message(self, tmp_path):
        """
        Enter a real but empty directory — the admin reports no YAML files.

        Uses a fresh spawn with --output-dir so the session-dir prompt is skipped.
        """
        child = _spawn(extra_args=['--output-dir', str(tmp_path)])
        try:
            empty_dir = tmp_path / 'empty-scan-dir'
            empty_dir.mkdir()

            _navigate_to_import_prompt(child)
            child.sendline(str(empty_dir))
            child.expect(r'(?i)(no yaml|no .yaml files)', timeout=15)
        finally:
            _close_child(child)

    def test_directory_with_yaml_files_lists_them(self, tmp_path):
        """
        Enter a directory containing YAML files — admin lists them.

        Writes two dummy YAML files into a temp directory, starts Import,
        points it at that directory, then verifies the file names appear.
        """
        child = _spawn(extra_args=['--output-dir', str(tmp_path)])
        try:
            scan_dir = tmp_path / 'yaml-scan-dir'
            scan_dir.mkdir()
            (scan_dir / 'redis.yaml').write_text('host: localhost\n')
            (scan_dir / 'kafka.yaml').write_text('bootstrapServers: localhost:9092\n')

            _navigate_to_import_prompt(child)
            child.sendline(str(scan_dir))
            # At least one of the YAML files should appear in the listing
            child.expect(r'(?i)(redis\.yaml|kafka\.yaml|\[1\])', timeout=15)
            # Send 'B' to cancel file selection and return
            child.sendline('B')
        finally:
            _close_child(child)


# ---------------------------------------------------------------------------
# C-IMPORT-COMPLETE: Full import flow — select file, confirm type, save
# ---------------------------------------------------------------------------

class TestImportFlowCompletion:
    """C-IMPORT-COMPLETE: Import a YAML file end-to-end and verify output is written."""

    def test_import_yaml_file_writes_output(self, tmp_path):
        """
        Full import flow:
          1. Write a realistic ZooKeeper YAML to a scan directory.
          2. Navigate to Import, point at the scan directory.
          3. Select the file ([1]).
          4. Confirm the inferred type (Enter → accept top candidate).
          5. Decline field editing ('n' — single-char readChar).
          6. Choose 'Save to session directory' (Enter → default [2]).
          7. Assert 'Written to:' appears and a .yaml file is in the output dir.

        This is the first end-to-end test of the import-apply code path.
        """
        scan_dir = tmp_path / 'import-scan'
        out_dir  = tmp_path / 'import-out'
        scan_dir.mkdir()
        out_dir.mkdir()

        # Use actual ZooKeeper Java field names so type inference matches.
        # connectString/sessionTimeoutMs are Curator property names, not Java field names.
        # The real fields are: namespace, disableBlockUntilConnected, canBeReadOnly.
        (scan_dir / 'zookeeper.yaml').write_text(
            'namespace: myapp\ndisableBlockUntilConnected: false\ncanBeReadOnly: false\n'
        )

        child = _spawn(extra_args=['--output-dir', str(out_dir)])
        try:
            _navigate_to_import_prompt(child)
            child.sendline(str(scan_dir))

            # File list appears with [1] zookeeper.yaml
            child.expect(r'(?i)(zookeeper\.yaml|\[1\])', timeout=15)
            # Prompt: "Select file: "
            child.expect(r'(?i)select file', timeout=10)
            child.sendline('1')

            # Three possible outcomes after file selection:
            #   a) "── Type Candidates ──" header followed by a list → "Select type [1]: "
            #   b) "Select type [1]: " directly (header already passed the buffer)
            #   c) "No type inferred. Enter FQCN (or blank to cancel):"
            # Detect which case we're in so we don't double-consume the terminal.
            idx0 = child.expect(
                [r'(?i)type candidates', r'(?i)select type', r'(?i)no type inferred', pexpect.TIMEOUT],
                timeout=15,
            )
            if idx0 == 3:
                pytest.fail('C-IMPORT-COMPLETE (TestImportFlowCompletion::test_import_yaml_file_writes_output): no type candidates within 15s after selecting zookeeper.yaml.')
            if idx0 == 2:
                # "No type inferred" consumed — cancel, then fail
                child.sendline('')
                pytest.fail('C-IMPORT-COMPLETE (TestImportFlowCompletion::test_import_yaml_file_writes_output): type inference returned no candidates for zookeeper.yaml.')
            if idx0 == 0:
                # "Type Candidates" header matched; wait for the actual selection prompt
                idx1 = child.expect([r'(?i)select type', pexpect.TIMEOUT], timeout=20)
                if idx1 == 1:
                    pytest.fail('C-IMPORT-COMPLETE (TestImportFlowCompletion::test_import_yaml_file_writes_output): timed out waiting for the "Select type" prompt.')
            # idx0 == 1 → already at "Select type [1]: "
            child.sendline('')   # Accept top candidate (default [1])

            # "Edit fields? [y/N] " — single-char readChar, NOT a line reader
            child.expect(r'(?i)edit fields', timeout=10)
            child.send('n')   # single char, no newline bleed

            # "Save option [2]: " — TerminalIo.readLine
            child.expect(r'(?i)save option', timeout=10)
            child.sendline('')   # Accept default [2] = session directory

            # Written confirmation
            child.expect(r'(?i)written to:', timeout=30)
        finally:
            _close_child(child)

        yaml_files = list(out_dir.glob('*.yaml'))
        assert yaml_files, (
            f'C-IMPORT-COMPLETE: No .yaml file found in output dir {out_dir}'
        )


# ---------------------------------------------------------------------------
# C-IMPORT-MALFORMED: Malformed YAML in scan directory — graceful handling
# ---------------------------------------------------------------------------

class TestImportMalformedYaml:
    """C-IMPORT-MALFORMED: A syntactically invalid YAML file is handled gracefully."""

    def test_malformed_yaml_shows_unknown_type_and_cancels(self, tmp_path):
        """
        Place a syntactically invalid YAML file in the scan directory.

        The type inference engine catches parse errors and returns no candidates,
        so the file appears in the list as '(unknown)'.  Selecting it triggers
        'No type inferred. Enter FQCN (or blank to cancel):'.  Sending blank
        cancels gracefully — the wizard returns to the Configurations sub-menu
        without crashing.
        """
        scan_dir = tmp_path / 'bad-scan'
        scan_dir.mkdir()
        # Write syntactically invalid YAML
        (scan_dir / 'bad.yaml').write_text('{unclosed: [bracket\n')

        child = _spawn(extra_args=['--output-dir', str(tmp_path)])
        try:
            _navigate_to_import_prompt(child)
            child.sendline(str(scan_dir))

            # File still appears in the listing (with '(unknown)' type label)
            child.expect(r'(?i)(bad\.yaml|\[1\])', timeout=15)
            child.expect(r'(?i)select file', timeout=10)
            child.sendline('1')

            # 'No type inferred. Enter FQCN (or blank to cancel):'
            child.expect(r'(?i)(no type inferred|fqcn)', timeout=15)
            child.sendline('')   # blank → cancel

            # Configurations sub-menu reappears — no crash
            child.expect(r'(?i)(configurations|\[1\]|\[B\])', timeout=15)
        finally:
            _close_child(child)


# ---------------------------------------------------------------------------
# C-IMPORT-UNKNOWN-FIELDS: YAML with no matching config fields — graceful
# ---------------------------------------------------------------------------

class TestImportUnknownFields:
    """C-IMPORT-UNKNOWN-FIELDS: YAML with fields matching no config class is handled gracefully."""

    def test_yaml_with_unknown_fields_does_not_crash(self, tmp_path):
        """
        Place a valid-syntax YAML file whose field names do not match any known
        Appbase configuration class.

        The type inference engine returns no candidates → same 'No type inferred'
        path as malformed YAML, but with different root cause (no field overlap
        vs parse error).  Blank FQCN cancels cleanly.
        """
        scan_dir = tmp_path / 'unknown-scan'
        scan_dir.mkdir()
        # Valid YAML, but field names that match no Appbase config class
        (scan_dir / 'mystery.yaml').write_text(
            'alienField: hello\nxyzzyProp: 42\nnoMatchAtAll: true\n'
        )

        child = _spawn(extra_args=['--output-dir', str(tmp_path)])
        try:
            _navigate_to_import_prompt(child)
            child.sendline(str(scan_dir))

            child.expect(r'(?i)(mystery\.yaml|\[1\])', timeout=15)
            child.expect(r'(?i)select file', timeout=10)
            child.sendline('1')

            # Either no type inferred, or a low-confidence candidate is offered.
            # In either case, cancelling with blank / 'B' must not crash.
            idx = child.expect(
                [
                    r'(?i)(no type inferred|fqcn)',        # 0 no candidates
                    r'(?i)(type candidates|select type)',  # 1 candidates shown
                    pexpect.TIMEOUT,
                ],
                timeout=15,
            )
            if idx == 0:
                child.sendline('')   # blank → cancel
            elif idx == 1:
                child.sendline('B')  # cancel from type selection
            else:
                pytest.fail('C-IMPORT-UNKNOWN-FIELDS: timed out at type selection step.')

            # Wizard must return to sub-menu without crashing
            child.expect(r'(?i)(configurations|\[1\]|\[B\])', timeout=15)
        finally:
            _close_child(child)


# ---------------------------------------------------------------------------
# C-IMPORT-SESSION-DIR: No --output-dir flag → interactive output dir prompt
# ---------------------------------------------------------------------------

class TestSessionDirPrompt:
    """C-IMPORT-SESSION-DIR: Without --output-dir the wizard prompts for an output directory."""

    def test_no_output_dir_flag_prompts_for_dir(self, tmp_path):
        """
        Spawn the wizard WITHOUT the '--output-dir' flag.

        ConfigWizardApp.resolveSessionPath() detects that no --output-dir was supplied
        and prints 'Output directory [<tmpdir>]: ' before showing the main menu.

        The test:
        1. Verifies the prompt appears.
        2. Supplies a writable temp directory.
        3. Creates the first ZooKeeper config using all defaults.
        4. Asserts a .yaml file was written to the chosen directory.
        """
        args = JVM_FLAGS + ['-jar', JAR, 'console']   # no --output-dir
        child = pexpect.spawn('java', args, encoding='utf-8', timeout=90,
                              dimensions=(40, 200), env=WIZARD_ENV)
        try:
            # resolveSessionPath() runs BEFORE ConfigWizardConsoleApp prints the banner,
            # so the directory prompt appears first — before 'Appbase Config Wizard'.
            child.expect(r'(?i)output directory', timeout=60)
            child.sendline(str(tmp_path))

            # Banner and main menu appear after the path is accepted
            child.expect('Appbase Config Wizard', timeout=30)
            child.expect(r'\[', timeout=15)
            choose_menu_item(child, '1')                             # Configurations
            child.expect(r'\[1\]', timeout=15)
            choose_menu_item(child, '1')                             # Create
            expect_filter_prompt(child, timeout=30)

            select_filtered_config(
                child, 'DefaultZooKeeperClientIntegrationServiceConfiguration')
            drain_first_prompt(child)

            # Accept all defaults via the same helper used by other output tests
            for _ in range(80):
                i = child.expect(
                    [
                        r'(?i)written to:',
                        r'YAML Preview',
                        r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',
                        ANY_READLINE_PROMPT,
                        pexpect.EOF,
                        pexpect.TIMEOUT,
                        r'Add entry',                                                    # 6 — Map sub-menu
                        r'Add value',                                                    # 7 — List/Set sub-menu
                    ],
                    timeout=20,
                )
                if i == 0:
                    break
                elif i == 1:
                    child.expect(r'Write this config', timeout=15)
                    child.send('Y')
                elif i == 2:
                    child.sendline('1')
                elif i == 3:
                    child.sendline('')
                elif i in (6, 7):
                    child.expect(r'> ', timeout=5)
                    child.sendline('2')
                else:
                    pytest.fail('C-IMPORT-SESSION-DIR: wizard exited/timed-out before writing.')
            child.expect(r'[^\r\n]+', timeout=10)   # drain path
        finally:
            _close_child(child)

        yaml_files = list(tmp_path.glob('*.yaml'))
        assert yaml_files, (
            f'C-IMPORT-SESSION-DIR: No .yaml file written to session dir {tmp_path}'
        )


# ---------------------------------------------------------------------------
# C29 — Import with field editing (answer 'y' to 'Edit fields?')
# ---------------------------------------------------------------------------

class TestImportWithFieldEdit:
    """C29: Import with field editing (answer y to Edit fields) accepts field prompts."""

    def test_import_yaml_with_field_editing(self, tmp_path):
        """
        Full import flow with field editing enabled:
          1. Write a ZooKeeper YAML to a scan directory.
          2. Navigate to Import, point at the scan directory.
          3. Select the file and confirm the inferred type.
          4. Answer 'y' to 'Edit fields?' — field prompts appear.
          5. Accept defaults for all fields (up to 50).
          6. Confirm YAML Preview write ('Y').
          7. Assert 'Written to:' appears and a .yaml file is in the output dir.

        This specifically tests the field-editing code path of the import flow,
        which is distinct from the no-edit path tested by C-IMPORT-COMPLETE.
        """
        scan_dir = tmp_path / 'edit-scan'
        out_dir  = tmp_path / 'edit-out'
        scan_dir.mkdir()
        out_dir.mkdir()

        (scan_dir / 'zookeeper.yaml').write_text(
            'namespace: myapp\ndisableBlockUntilConnected: false\ncanBeReadOnly: false\n'
        )

        child = _spawn(extra_args=['--output-dir', str(out_dir)])
        try:
            _navigate_to_import_prompt(child)
            child.sendline(str(scan_dir))

            # File list appears with [1] zookeeper.yaml
            child.expect(r'(?i)(zookeeper\.yaml|\[1\])', timeout=15)
            child.expect(r'(?i)select file', timeout=10)
            child.sendline('1')

            # Three possible outcomes after file selection (same as C-IMPORT-COMPLETE)
            idx0 = child.expect(
                [r'(?i)type candidates', r'(?i)select type', r'(?i)no type inferred', pexpect.TIMEOUT],
                timeout=15,
            )
            if idx0 == 3:
                pytest.fail('C29 (TestImportWithFieldEdit::test_import_yaml_with_field_editing): no type candidates within 15s after selecting zookeeper.yaml.')
            if idx0 == 2:
                child.sendline('')
                pytest.fail('C29 (TestImportWithFieldEdit::test_import_yaml_with_field_editing): type inference returned no candidates for zookeeper.yaml.')
            if idx0 == 0:
                idx1 = child.expect([r'(?i)select type', pexpect.TIMEOUT], timeout=20)
                if idx1 == 1:
                    pytest.fail('C29 (TestImportWithFieldEdit::test_import_yaml_with_field_editing): timed out waiting for the "Select type" prompt.')
            child.sendline('')   # Accept top candidate (default [1])

            # 'Edit fields? [y/N]' — single-char readChar, NOT a line reader
            child.expect(r'(?i)edit fields', timeout=10)
            child.send('y')   # single char — enter field editing mode

            # Field prompts appear; accept defaults, handle enums, confirm YAML preview.
            # Also handle 'save option' prompt if present (when output dir is pre-set,
            # the wizard may write directly without an intermediate save-option dialog).
            for _ in range(50):
                i = child.expect(
                    [
                        r'(?i)written to:',                                                           # 0 done
                        r'YAML Preview',                                                              # 1 preview gate
                        r'(?i)(invalid.*enter|enter a number|not a valid)[^\r\n]*\r?\n[^\r\n]*: ',   # 2 enum retry
                        ANY_READLINE_PROMPT,                                    # 3 field prompt
                        pexpect.EOF,                                                                  # 4
                        pexpect.TIMEOUT,                                                              # 5
                        r'Add entry',                                                                 # 6 — Map sub-menu
                        r'Add value',                                                                 # 7 — List/Set sub-menu
                        r'Save option',                                                               # 8 — import save-dest dialog (TerminalIo.readLine, no \x1b=)
                    ],
                    timeout=20,
                )
                if i == 0:
                    break
                elif i == 1:
                    child.expect(r'Write this config', timeout=15)
                    child.send('Y')   # single char — readChar for YAML confirm
                elif i == 2:
                    child.sendline('1')   # pick first enum option when no default
                elif i == 3:
                    child.sendline('')
                elif i in (6, 7):
                    child.expect(r'> ', timeout=5)
                    child.sendline('2')
                elif i == 8:
                    child.sendline('')    # accept default [2] = session dir (blank → option 2)
                else:
                    pytest.fail('C29: wizard exited or timed out during field editing.')

            child.expect(r'[^\r\n]+', timeout=10)   # drain 'Written to:' path
        finally:
            _close_child(child)

        yaml_files = list(out_dir.glob('*.yaml'))
        assert yaml_files, f'C29: No .yaml file found in output dir {out_dir}'
