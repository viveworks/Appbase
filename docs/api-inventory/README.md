# Appbase shipped-artifact and supported-public API inventory

Introduced by internal work item 1399 (P06·E01 N01). This is
the **single record set** that the API documentation (internal work item 1400–internal work item 1406), the public catalogue (internal work item 2418), the
root README (internal work item 2419) and the User Guide (internal work item 1416) consume. None of them keeps its own module list.

## Files

| File | Written by | What it is |
|---|---|---|
| `classification.json` | **a human** | The decisions: each artifact's support state and handoff fields, the package split, module group → documentation issue, excluded trees, and `baseline`, which holds the release baseline's deferred list (§4.2/§4.3) transcribed with its source |
| `inventory.json` | `scripts/api-inventory.py generate` | **Generated. Never edit it by hand.** One record per artifact the build produces, joined with its classification |
| [`../spec/1.0.0/P06/decisions/E01-published-api-gap-list.md`](../spec/1.0.0/P06/decisions/E01-published-api-gap-list.md) | same | **Generated.** The measured work list and estimate for internal work item 1400–internal work item 1406 with each group's PR-cap flag and split note, the raw public-surface count with the Plan N01 Step 1 command that reproduces it, the root pom's configured `maven-javadoc-plugin`/doclint settings (the effective javadoc run is NOT_MEASURED; it belongs to internal work item 1405), and the open inventory issues |

```sh
python3 scripts/api-inventory.py generate                  # rewrite both generated files; exit 1 if any issue
python3 scripts/api-inventory.py check                     # the gate: exit 1 on drift or any issue
python3 scripts/api-inventory.py check-consumer README.md  # the AC7 gate for a consumer doc
bash scripts/test/inventory_test.sh                        # fixture arms, one per acceptance item
```

The generator uses the Python 3 standard library only, and its output is byte-deterministic: it
contains no timestamps or commit ids, and every collection is sorted.

## Epic-exit groundwork (internal work item 2541)

The lightweight coordinator composes the fresh inventory check with the full
catalogue comparison and validates the declared reader-path links:

```sh
python3 scripts/api-docs-acceptance.py \
  --report target/api-docs-acceptance.json
```

Exit `0` means every light static row passed. Exit `1` means a static gate or
link failed. Exit `2` means the runnable rows passed but generated documentation
or another declared destination remains **NOT MEASURED**. The checked-in link
matrix intentionally leaves Javadoc navigation pending until internal work item 1405; this command
does not claim to generate Javadoc, Dartdoc, or an epic-close pass. It invokes
full `catalogue-check.py` mode because `--catalogue-only` suppresses inventory-side
problems and cannot satisfy internal work item 2541's classification-agreement row.

## Where the artifact list comes from

It comes only from the build and export authorities. A capability that exists only in a spec, a plan
or a brainstorm has no record here, and a classification that names one fails the check (AC4).

1. **The Maven reactor.** The generator starts at the root `pom.xml` and follows `<modules>` recursively. Modules that only a profile adds are included and recorded with `reactor_profile`.
2. **The BOM.** This is any reactor module whose artifactId ends in `-bom` and that carries `<dependencyManagement>`. Its entries set `exported_by_bom`.
3. **Every `pubspec.yaml` inside a reactor module.** Each one becomes a `dart-package` record.
4. **Coverage of the whole tree.** Every `pom.xml` and `pubspec.yaml` on disk must be reached by 1–3 or sit under `excluded_trees`. Anything else is reported as `absent`.

## Record schema (`inventory.json`, `schema: 1`)

Top level: `schema`, `generator`, `classification`, `summary` (count per support state), `package_classification` (the package rule's ruling reference and the count per basis), `issues`
(sorted strings; an empty list means the gate is green), and `artifacts` (sorted by `id`).

| Field | Meaning |
|---|---|
| `id` | `groupId:artifactId` for Maven, `pub:<name>` for Dart. Unique; the check fails on duplicates |
| `type` | `maven-<packaging>` (today only `maven-jar` and `maven-pom` occur) or `dart-package` |
| `module_path` | The path from the repository root (`.` for the root pom) |
| `group_id`, `artifact_id`, `version` | The coordinates. `group_id` and `version` are inherited from `<parent>` when the pom omits them |
| `publish_to` | Dart records only |
| `reactor_profile` | `null`, or the id of the profile that adds the module |
| `exported_by_bom` | Whether a BOM pins it. Any reactor module whose artifactId ends in `-bom` and carries `<dependencyManagement>` counts; today that is only `appbase-bom` |
| `support` | `supported-public` · `internal-implementation` · `deferred-unsupported` · `generated-only` · **`unclassified`**. `unclassified` is an open decision, and it fails the check |
| `evidence` | Why the artifact has that support state, with a source you can check. For `supported-public` it must cite the release baseline (`§…`), a test (`test: …`) or a recorded owner ruling (`owner ruling: #<issue> comment <id>`) |
| `owning_module` | The reactor module that owns the artifact (its own path for Maven, the enclosing module for Dart) |
| `family`, `entry_points`, `backend`, `docs_destination`, `limitations` | Copied from `classification.json`, or `null` |
| `module_group`, `doc_issue` | Matched from `module_groups` by the longest path prefix, which is how a record maps to internal work item 1400–internal work item 1406 |
| `packages[]` | Java artifacts only. One entry per package: `package`, `classification` (`supported-public` · `implementation-detail` · `unclassified`), `reason`, `basis` (`explicit` · `rule` · `artifact`), `public_type_count`, `undocumented_types`, `public_members`, `undocumented_members`, `public_types` (the entry-point type names, listed for `supported-public` packages only) and `implementation_detail_types` (the package rule's type exclusions, when there are any) |
| `dart_surface` | Dart records only: `public_declarations` and `undocumented_declarations` |

Packages of an artifact that is not `supported-public` are always `implementation-detail` (`basis:
artifact`). Packages of a `supported-public` artifact take the **longest matching `prefix`** in
`classification.json`'s `packages` list (`basis: explicit`). If no prefix matches, the **package rule**
decides (`basis: rule`).

**The package rule is the owner-approved rule-based default recorded on 2026-09-27.** It lives in one
table in the generator, `PACKAGE_RULE`:

- a package with an `internal`, `impl` or `support` **segment** (`.internal.`, or `.internal` at the end)
  is `implementation-detail`;
- a class named `Default…Impl` or `…Helper` is `implementation-detail` even inside a supported-public
  package. It is listed in that package's `implementation_detail_types`, not in `public_types`, and is
  left out of the package's counts;
- everything else is `supported-public`.

By the ruling, a new package in a supported-public artifact is public unless it carries an implementation
segment or an explicit rule says otherwise, so a package meant to stay internal needs an explicit
`classification.json` entry. A package the rule still leaves unclassified is reported as an issue. The
top-level `package_classification` block counts how many packages each basis decided.

The Javadoc counts come from a static scan, not a compiler, so they are an estimate:

- A type or member counts as documented when a `/** … */` block closes right before it. Annotation-only lines between the two are allowed.
- `@Override` members inherit their documentation from the supertype, so they never count as gaps.
- Members are public methods and constructors, plus the implicitly public methods of interfaces.

## The check fails when…

- `inventory.json` or the work list differs from a fresh generation, or is missing (**drift**, AC1)
- a produced artifact has no valid support state, or two records share an id or module entry (AC2)
- a `pom.xml`/`pubspec.yaml` on disk is outside the reactor and not excluded (AC2)
- a package of a `supported-public` artifact is left unclassified by the explicit rules and the package rule (AC3)
- a classification names an artifact the build does not produce (AC4)
- an artifact is `supported-public` but the BOM does not export it, or it is `deferred-unsupported` but the BOM exports it (AC4)
- a `supported-public` record's `evidence` cites none of: a release-baseline section (`§4.1 …`), a test (`test: …`), or a recorded owner ruling (`owner ruling: #<issue> comment <id>`). BOM membership alone is not evidence of support (AC4). ⚠️ **The gate is syntactic:** it checks that a citation of that *form* is present (any `§` followed by a digit passes, as do `test:` and the owner-ruling pattern). It does not check that the cited section, test or comment exists or says what the evidence claims; a reviewer has to read that
- an artifact listed in `baseline.deferred` is classified as anything but `deferred-unsupported` or `unclassified` (AC4)
- a `supported-public` record lacks a handoff field: `family`, `entry_points`, `backend`, `docs_destination` or `limitations` (the issue's Classification clause)
- `docs/api-inventory/classification.json`, `inventory.json`, the work list, or any existing parent directory of them, is a symlink. It is refused before anything is read or written, including a dangling link and a link whose target is byte-identical (RA-A4 A4-I1). An explicit `generate --out FILE` is the caller's own choice and is not vetted
- an explicit package rule is malformed: its `classification` is not `supported-public` or `implementation-detail`, or it has no `prefix`. A package matched by such a rule stays `unclassified` and never falls back to the public default (RA-A4 A4-I2)
- `classification.json` repeats a key, since the last entry would win silently, or repeats a package prefix, or is not valid JSON (AC2)
- a module, `pom.xml`, `pubspec.yaml`, Dart `lib` directory, source root or source file (Java or Dart) resolves outside the repository, for example through a symlink. Such paths are refused and never read, because the inventory is written back into the repository

An input the generator cannot use at all stops it with one `api-inventory: ERROR …` line and exit 1, never a traceback: a malformed `pom.xml`, a pom containing a `<!DOCTYPE` or entity declaration in any encoding, UTF-16 included (refused by the XML parser the moment it reads the declaration, so no entity is ever expanded), and a `classification.json` that is not valid JSON, is nested too deeply to parse, or has the wrong shape (a top level that is not an object, an artifact entry that is not an object, a `baseline` that is not an object, an artifact `evidence`, `support`, `family`, `backend`, `docs_destination` or `limitations` that is not a string, an `entry_points` that is not a list of strings, a package rule `reason` that is not a string, or a module group without a `prefix` or `group`)

## Consumer contract (AC6 / AC7)

A consumer doc, such as the root README, the catalogue (internal work item 2418), the User Guide (internal work item 1416) or an API-docs
index, lists the supported-public artifacts **inside a marked region**:

```markdown
<!-- api-inventory:supported-public:begin -->
| Artifact | What it is |
|---|---|
| `com.viveworks.appbase:appbase-bom` | import this first |
| `com.viveworks.appbase:appbase-core` | … |
<!-- api-inventory:supported-public:end -->
```

`check-consumer FILE…` regenerates the inventory in memory and collects every backticked
`group:artifact` inside the region(s). It fails if:

- the inventory itself has any issue (the same list `check` reports), since a comparison against an invalid inventory proves nothing;

- a supported-public artifact is missing from the list;
- the list names an artifact that is not supported-public, or is not in the inventory at all;
- the file has no region, since a doc with no region would otherwise pass without being checked.

Passing the API-docs index **and** the catalogue in one call is the intended AC7 gate. Each would be
compared with the same records, so if one disagreed with the other, one would disagree with the
inventory and the check would fail. Prose outside the region is free text and is not checked.

**Current and future contract (owner Option A).** The operational caller currently runs
`check-consumer` for the existing `README.md` consumer only. A4-I4 is closed in this PR **only for that
operational caller and README consumer**. The catalogue consumer, API-index consumer, one operational
multi-consumer call, and per-consumer drift mutations do not exist here; they are carried by
internal work item 2507/internal work item 2418 in this same epic. Therefore the multi-consumer command below is a documented **FUTURE
contract**, not a claim about the current caller:

```sh
python3 scripts/api-inventory.py check && bash scripts/test/inventory_test.sh
python3 scripts/api-inventory.py check-consumer README.md <catalogue> <api-docs index>
```
