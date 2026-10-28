# Console Integration Tests

End-to-end PTY tests for the `appbase-config-wizard` interactive console wizard.
Each test spawns a real JVM, drives it via `pexpect`, and asserts on terminal output.

## Running

```bash
cd appbase-config-wizard/test/console
python3 -m pytest -v
```

The JAR must be built first:

```bash
mvn clean install -pl appbase-config-wizard \
    -P quick-profile
```

## Test Inventory — 76 collected cases across 7 files

The six live-JAR modules collect 47 cases; `test_prompt_patterns.py` adds 29
pure prompt/helper cases that need no JAR.

The comprehensive C20 sweep remains all-config by default
(`WIZARD_SMOKE_MAX=0`). Each fresh JVM has a 180-second startup deadline
(`WIZARD_STARTUP_TIMEOUT`), followed by a fresh monotonic 120-second interaction
deadline (`WIZARD_CONFIG_TIMEOUT`). The complete sweep has a 3600-second deadline
(`WIZARD_SWEEP_TIMEOUT`). Deadline expiry fails with completed/total and
untested counts; it never silently truncates the sweep or reports a pass.

| Environment variable | Default | C20 effect |
|----------------------|---------|------------|
| `WIZARD_STARTUP_TIMEOUT` | `180` seconds/startup | Fails if the fresh JVM does not reach the FilterMenu |
| `WIZARD_CONFIG_TIMEOUT` | `120` seconds/config | Fails the current configuration when its monotonic deadline expires |
| `WIZARD_SWEEP_TIMEOUT` | `3600` seconds | Fails the sweep with completed/total and untested counts when its deadline expires |

### `test_navigation.py` — FilterMenu navigation (C1–C6, C21–C22)

| ID | Test | What it verifies |
|----|------|-----------------|
| C1 | `TestBanner::test_banner_visible` | Startup prints the cyan "Appbase Config Wizard" banner |
| C2 | `TestConfigList::test_module_tag_visible` | Config list renders with `[module]` colour tags |
| C3 | `TestFilterMatch::test_filter_redis` | Typing `redis` narrows the list to Redis entries only |
| C4 | `TestFilterNoMatch::test_filter_no_match` | Typing an unknown term shows a "no matches" message |
| C5 | `TestSelectByNumber::test_select_first_entry` | Typing `1` after filtering selects the first entry and shows field prompts |
| C6 | `TestQuit::test_quit_prints_goodbye` | Typing `q` at the FilterMenu prints "Goodbye." and exits with code 0 |
| C21 | `TestOutOfRangeSelection::test_out_of_range_entry_number` | Typing `9999` produces an error message and the Configurations sub-menu reappears — no crash |
| C22 | `TestFilterReset::test_blank_selects_highlighted_entry` | Typing `redis` then pressing Enter selects the highlighted Redis row and advances to its fields |

### `test_output.py` — YAML output and session flow (C7, C8, C11–C13, C23–C24)

| ID | Test | What it verifies |
|----|------|-----------------|
| C7 | `TestAllDefaultsAcceptance::test_accept_all_defaults` | Pressing Enter for every field writes valid YAML |
| C8 | `TestCustomStringField::test_custom_host_in_yaml` | Entering a custom host value causes it to appear in the output YAML |
| C11 | `TestOutputFlag::test_output_flag_writes_to_specified_path` | The `-o /path/file.yaml` flag writes to the exact path supplied |
| C12 | `TestBackToMenu::test_back_to_menu_shows_list_again` | Answering Y to "Create another?" loops back to the FilterMenu |
| C13 | `TestQuitAfterWrite::test_quit_after_write_exits_cleanly` | Answering n to "Create another?" exits the wizard cleanly with code 0 |
| C23 | `TestYamlFieldAssertions::test_zookeeper_yaml_has_expected_keys` | ZooKeeper default YAML contains at least one of `hostPortConfigurations`/`sessionTimeoutConfiguration`/`retryPolicyConfiguration` — catches wrong serialization key names |
| C24 | `TestMultipleConfigsSession::test_two_configs_in_one_session` | Create ZooKeeper config, loop back via Y, create Redis config — two distinct `.yaml` files written in one JVM invocation |
| C31 | `TestRedisYamlFieldAssertions::test_redis_yaml_has_expected_keys` | Redis default YAML contains at least one of `host`/`port`/`database`/`clientName`/`sslEnabled`/`disconnectedBehavior`/`autoReconnect` — regression guard for Redis field serialization |
| C32 | `TestKafkaYamlFieldAssertions::test_kafka_yaml_has_expected_keys` | Kafka producer YAML contains the required `bootstrapServerConfigurations` key — regression guard for Kafka field serialization |

### `test_fields.py` — Field validation and re-prompt (C9–C10, C25–C28, C30)

| ID | Test | What it verifies |
|----|------|-----------------|
| C9 | `TestInvalidIntField::test_invalid_int_then_valid` | Entering `abc` for an `(int)` field triggers an error and re-prompt; entering `9090` then succeeds |
| C10 | `TestInvalidBoolField::test_invalid_bool_then_valid` | Entering `maybe` for a `(bool)` field triggers an error and re-prompt; entering `y` then succeeds |
| C25 | `TestEnumDirectSelection::test_enum_option_selected_directly` | Selects the exact Redis integration config, finds an enum with ≥2 options, and sends `2` (number) on the first try — verifies numeric enum selection |
| C26 | `TestSigintMidForm::test_sigint_mid_form_exits` | Sending Ctrl-C (SIGINT) mid-field-prompt causes the wizard to terminate (pexpect receives EOF within timeout) |
| C27 | `TestBlankMandatoryField::test_blank_field_no_crash` | Selects the exact exception config with a no-default field; sends blank and asserts wizard does not crash (re-prompt, next field, or YAML preview are all valid) |
| C28 | `TestEnumNameSelection::test_enum_option_selected_by_name` | Selects the exact Redis integration config and sends the enum option's string NAME — verifies name-based enum selection produces no validation error |
| C30 | `TestSigintNestedForm::test_sigint_during_nested_exits` | Navigates directly to ZooKeeper (known to have nested sub-configs), enters the nested sub-form via `y`, then sends Ctrl-C — wizard must terminate (EOF) |

> C9 skips if the first config has no integer field; C10 targets the ZooKeeper
> integration config's boolean field deterministically.
> C25/C28 skip if their exact Redis config has no qualifying enum; C27 skips if
> its exact exception config no longer has a no-default field within 50 prompts.
> C30 skips only if ZooKeeper is not in the build or exposes no nested sub-config prompt.

### `test_import.py` — Import-from-directory flow (C-IMPORT-*, C29)

| Test / ID | What it verifies |
|-----------|-----------------|
| `test_scan_directories_prompt_appears` | Navigating to Import shows the directory-scan prompt |
| `test_invalid_directory_shows_error` | Entering a non-existent path shows an error message |
| `test_empty_directory_shows_no_yaml_message` | An existing empty directory reports no YAML files found |
| `test_directory_with_yaml_files_lists_them` | A directory containing `.yaml` files causes them to be listed |
| C-IMPORT-COMPLETE · `TestImportFlowCompletion::test_import_yaml_file_writes_output` | Full import: select file → confirm inferred type → skip edit → save to session dir → `Written to:` appears |
| C-IMPORT-MALFORMED · `TestImportMalformedYaml::test_malformed_yaml_shows_unknown_type_and_cancels` | Malformed YAML listed as `(unknown)` type; blank FQCN entry cancels gracefully without crash |
| C-IMPORT-UNKNOWN · `TestImportUnknownFields::test_yaml_with_unknown_fields_does_not_crash` | YAML with no matching config fields — type inference returns no/low candidates; cancel paths work |
| C-IMPORT-SESSION-DIR · `TestSessionDirPrompt::test_no_output_dir_flag_prompts_for_dir` | Without `--output-dir`, wizard prompts `Output directory [<tmpdir>]:` before main menu; supplied path is used for YAML output |
| C29 · `TestImportWithFieldEdit::test_import_yaml_with_field_editing` | Full import flow but answering `y` to "Edit fields?" — field prompts appear; accepting defaults produces valid YAML; verifies the field-edit code path is reachable and non-crashing |

### `test_smoke.py` — Regression sweeps (C14–C16)

| ID | Test | What it verifies |
|----|------|-----------------|
| C14 | `TestZooKeeperConfig::test_zookeeper_all_defaults` | ZooKeeper config (has `boolean`/`int` fields) completes without crashing |
| C15-kafka | `TestNumericBooleanDefaults::test_config_with_typed_defaults[kafka]` | Kafka config (has `Duration` fields) produces valid YAML |
| C15-elasticsearch | `TestNumericBooleanDefaults::test_config_with_typed_defaults[elasticsearch]` | Elasticsearch config produces valid YAML |
| C15-redis | `TestNumericBooleanDefaults::test_config_with_typed_defaults[redis]` | Redis config produces valid YAML |
| C15-cassandra | `TestNumericBooleanDefaults::test_config_with_typed_defaults[cassandra]` | Cassandra config produces valid YAML |
| C16 | `TestAllConfigsSweep::test_all_configs_write_yaml` | First `SMOKE_MAX_CONFIGS` (15) entries in the full list all produce parseable YAML — primary regression guard |

### `test_nested.py` — Nested-field and comprehensive sweeps (C17–C20)

| ID | Test | What it verifies |
|----|------|-----------------|
| C17 | `TestNestedFieldDefaults::test_nested_defaults_flow` | Answering `y` to a nested-field "use defaults?" prompt skips sub-fields and continues |
| C18 | `TestNestedFieldCustom::test_nested_fill_and_continue` | Answering `n` enters the sub-form; `+-- fieldName --` / `+-- end fieldName --` markers appear; sub-fields are filled; parent continues after |
| C19 | `TestMultiLevelNesting::test_multi_level_nested_zookeeper` | ZooKeeper config with 2+ nesting levels completes without error |
| C20 | `TestComprehensiveAllConfigs::test_all_configs_comprehensive` | Every entry in the full config list (no cap by default; set `WIZARD_SMOKE_MAX=N` to cap) produces valid YAML including nested sub-forms |

## Architecture notes

- **PTY driver**: `pexpect` spawns a real JVM; no mocks.
- **Pattern matching**: `child.expect([...])` returns on the earliest-matching pattern; ordering matters.
- **Nested form markers**: rendered as ASCII `+-- name (type) --` / `+-- end name --` (JLine3 converts Unicode box-drawing chars to VT100 DEC sequences that Python regex cannot match).
- **`(int)` / `(bool)` annotations**: int and bool prompts carry explicit type tags so tests can distinguish them from enum/filter-menu numeric prompts.
- **Duration fields**: `YAMLMapper` must have `findAndRegisterModules()` called; otherwise `java.time.Duration` fields crash Jackson serialization.
- **ConfigScanner filtering**: configs are excluded from the wizard list unless they pass a two-stage Jackson round-trip: (1) `convertValue(emptyMap, clazz)` succeeds (guards against builder-only classes with no Jackson creator), and (2) `YAMLMapper.writeValueAsString(defaultInstance)` succeeds (guards against classes with wildcard generics or unserializable field types).

## Key constants (configurable at top of each file)

| Constant | File | Default | Meaning |
|----------|------|---------|---------|
| `_MAX_FIELDS` | `test_smoke.py` | 400 | Max Enter presses per config before failing |
| `_SMOKE_MAX_CONFIGS` | `test_smoke.py` | 15 | Max configs tested in C16 sweep |
| `_MAX_FIELDS` | `test_nested.py` | 400 | Max Enter presses per config in C20 |
| `WIZARD_SMOKE_MAX` | env var | 0 (unlimited) | Cap for C20 sweep in CI |
| `WIZARD_STARTUP_TIMEOUT` | env var | 180 seconds/startup | Fresh-JVM startup deadline before interaction timing begins |
| `WIZARD_CONFIG_TIMEOUT` | env var | 120 seconds/config | Per-config monotonic deadline; expiry fails C20 |
| `WIZARD_SWEEP_TIMEOUT` | env var | 3600 seconds | Whole-sweep monotonic deadline; expiry fails with completed/total and untested counts, never truncates to a pass |

## Coverage status

All identified coverage gaps have been addressed:

| Gap area | Covered by |
|----------|-----------|
| Banner, menu, filter navigation | C1–C6, C21–C22 |
| Field validation: int, bool, enum (numeric), enum (name), blank | C9, C10, C25, C27, C28 |
| SIGINT: at top-level field, within nested sub-form | C26, C30 |
| Nested sub-config: defaults, custom, multi-level, all-configs | C17–C20 |
| Import: scan, invalid dir, empty dir, YAML list, full flow, malformed, unknown, session-dir, with-edit | C-IMPORT-*, C29 |
| YAML output: all-defaults, custom value, -o flag, back, quit, ZooKeeper keys, Redis keys, Kafka keys | C7, C8, C11–C13, C23, C31, C32 |
| Two-config session | C24 |
| Regression sweeps | C14–C16, C20 |
