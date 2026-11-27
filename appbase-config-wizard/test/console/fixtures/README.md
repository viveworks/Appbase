# Console test fixtures: provenance

## `real_prompts_b249c5bfd.raw`

These are raw terminal bytes, as the pexpect child read them, from a real config-wizard session. They pin the prompt protocol (`number=pick`, the bracketed-paste prefix `\x1b[?2004h`, and the field-prompt shape) that the console tests match against.

| Property | Value |
|---|---|
| Fixture SHA-256 | `f8dc5f94c8995c66397574da52369c2b0683cd015f9cca76483033fc5a0827f9` (8793 bytes) |
| Source commit | `b249c5bfd570094d1721d3af9992cdd7648964d6` (the epic head at capture time) |
| JAR | `appbase-config-wizard/target/appbase-config-wizard-1.0.0-SNAPSHOT.jar`, SHA-256 `34078a262143cb9ba08ee7a2710190705acefefd3c937c41c55a3bbee5715bc6`, `Build-Jdk-Spec: 25` |
| Built | 2026-09-26 16:08Z, host, Temurin JDK 25, Maven 3.6.1, under the build lease, `-DskipTests install` of `appbase-config-wizard -am` |
| Captured | 2026-09-26 ~17:28Z, host, macOS pty |

### Capture procedure

The fixture was captured by spawning the JAR through this suite's own `conftest._spawn(extra_args=['--output-dir', <tmpdir>])` and teeing `child.logfile_read` to a file. The script then sends:

1. `1` at the first `Choice: `;
2. `1` at the second `Choice: `;
3. `ZooKeeperClientIntegrationService` at the `number=pick` filter prompt;
4. `1` at the second `number=pick` prompt;
5. Enter (accept the default) at up to 8 successive field prompts ending in `]: `.

The resulting bytes were copied into this directory unedited, and the SHA-256 above matches the capture file. The capture log (`summary-4.json`) reports `captured: true` with no prompt errors. The prompt string matches `FilterMenu.java:146` at the source commit.

### Limits

The fixture proves the prompt bytes of one real session at `b249c5bfd`. It does not replace a live run of the console suite at a PR head. That run is the ci-verify console tier, or the epic live campaign.
