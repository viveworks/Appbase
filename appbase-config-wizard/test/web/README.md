# Appbase Config Wizard — Playwright Web Tests

End-to-end and API integration tests for the `appbase-config-wizard` web interface.

## Prerequisites

- Node.js 18+
- Chromium browser (installed automatically by Playwright)
- Appbase Config Wizard server running at `http://localhost:8080`

## Setup

```bash
cd appbase-config-wizard/test/web
npm install
npx playwright install chromium
```

## Running Tests

```bash
# Start the server first
../../bin/start-web.sh

# Run all tests
CONFIG_WIZARD_URL=http://localhost:8080 npm test

# Run specific spec file
CONFIG_WIZARD_URL=http://localhost:8080 npm test -- tests/api.spec.ts

# Run tests headed (visible browser)
CONFIG_WIZARD_URL=http://localhost:8080 npm run test:headed

# View HTML report after run
npm run test:report
```

## Test Inventory — 75 tests across 8 files

### `navigation.spec.ts` — Page navigation and filter (W1–W3)

| ID | Test | What it verifies |
|----|------|-----------------|
| W1 | `page title shows "Appbase Config Wizard"` | Document title is "Appbase Config Wizard" — AppBar branding correct |
| W2 | `config list shows ≥10 items` | Config list has ≥10 entries; typing a filter narrows the list; unknown term shows empty state |
| W3 | `back button returns to config list` | Clicking Back from a config form returns to the main list |

### `form.spec.ts` — Config form and YAML output (W4–W7, W13)

| ID | Test | What it verifies |
|----|------|-----------------|
| W4 | `clicking a config opens a form` | Clicking a config item opens a detail form (field inputs appear) |
| W5 | `form fields render (textbox, switch, slider)` | At least one textbox, switch, or slider is visible in the form |
| W6a | `clicking Build Config does not show an error` | Submitting defaults produces no Flutter error widget |
| W6b | `page still responds after form submission` | Page is interactive after Build Config clicked |
| W7 | `Reset button is present` | Form has a visible Reset button |
| W13a | `Build Config shows YAML output screen` | After Build, a YAML preview screen appears |
| W13b | `YAML output screen shows Write to File button` | *(graceful skip if YAML screen not reachable)* |
| W13c | `clicking Write to File shows confirmation` | *(graceful skip if Write to File button not found)* |
| W13d | `back from YAML output returns to form or list` | Back button from YAML screen navigates to form or list |

> W13b and W13c skip when the server lacks write permissions; W13d always runs.

### `import.spec.ts` — Import screen (W-IMPORT-*)

| ID | Test | What it verifies |
|----|------|-----------------|
| W-IMPORT-TAB | `Import button visible in AppBar` | AppBar contains an Import navigation button |
| W-IMPORT-SCREEN | `Import screen opens with text input` | Clicking Import shows a directory-path text field |
| W-IMPORT-BACK | `Back button returns to config list` | Back from Import screen returns to the main list |

### `api.spec.ts` — REST API endpoints (W8–W9)

| ID | Test | What it verifies |
|----|------|-----------------|
| W8 | `GET /api/configurations returns ≥10 configs` | List endpoint returns JSON array ≥10 items; each has `fqcn` and `simpleName` |
| W9 | `GET /api/configurations/{fqcn} returns field metadata` | Detail endpoint returns `fields` array; each has `fieldName` and `typeName` |

### `all-configs.spec.ts` — All-configs regression sweep (W10–W15, W-NESTED-APPLY-PERSIST)

| ID | Test | What it verifies |
|----|------|-----------------|
| W10 | `every config class loads without error` | Navigating to each config form shows no "Error loading config" text |
| W11 | `text is selectable on form screens` | CSS `pointer-events` is not globally suppressed — text can be selected |
| W12a | `a config with nested fields shows Set Values button` | At least one config has a nested field with a "Set Values" button |
| W12b | `clicking Set Values opens sub-form with Apply button` | Clicking Set Values opens a sub-form containing an Apply button |
| W14 | `Set Values → close → Apply button gone` | After entering and leaving a sub-form, the Apply button disappears (breadcrumb pop) |
| W15a | `API verifies all configs build without Jackson errors` | POST /api/configurations/{fqcn}/build for every config returns no `errorMessage` (via REST, not UI) |
| W15b | `first visible config builds to YAML in the UI` | Build Config via the Flutter UI produces a YAML preview screen |
| W-NESTED-APPLY-PERSIST | `Set Values → Apply → Build Config does not show an error` | After entering a nested sub-form, clicking Apply, then Build Config, no error widget appears — verifies nested-field writes persist into the build payload |

### `api-coverage.spec.ts` — REST API coverage for all configs (W-API-*)

| ID | Test | What it verifies |
|----|------|-----------------|
| W-API-DETAIL | `detail endpoint returns valid FieldSummaryDto for every config` | For each of the ~82 config classes, GET /api/configurations/{fqcn} returns `fields` with `fieldName`/`typeName` — no 404/500 |
| W-API-BUILD | `build endpoint returns non-empty YAML for every config` | POST /api/configurations/{fqcn}/build returns non-empty `yaml` without `errorMessage` for every config |
| W-API-WRITE | `write endpoint returns a path in the response` | POST /api/configurations/{fqcn}/write with a /tmp path returns `path` in JSON response |
| W-API-MODULES | `configs present for core modules` | List endpoint contains at least one entry for kafka, redis, zookeeper, elasticsearch, cassandra |
| W-API-ENUM-BUILD | `build with non-default enum value succeeds` | Finds a config with an enum field (≥2 values), POSTs /build with the second enum value, asserts YAML contains the field+value or is an accepted error — validates enum serialization round-trip |
| W-API-VALIDATION | `build with empty body returns validation error or clean YAML` | POST /build with empty body on a config that has required non-default fields — asserts no raw Jackson stack trace (error message must be human-readable or YAML is written) |

### `filter-coverage.spec.ts` — Filter, field types, YAML output (W-FILTER-*, W-CONFIG-COUNT, W-FIELD-*, W-RESET, W-YAML-CONTENT, W-WRITE-FILEPATH)

| ID | Test | What it verifies |
|----|------|-----------------|
| W-CONFIG-COUNT | `total config count ≥40` | List endpoint returns ≥40 entries — regression guard for accidental exclusion of config classes |
| W-FILTER-MODULE | `search by module name reduces list` | Typing a module name (e.g. "kafka") reduces the displayed count compared to the unfiltered list |
| W-FIELD-BOOL | `boolean fields render as toggle or checkbox` | A config with boolean fields shows at least one switch or checkbox widget |
| W-FIELD-DEFAULT | `config with defaults renders pre-filled inputs` | A config with default values shows at least one pre-filled input in the form |
| W-RESET | `Reset button present on config forms` | First reachable config form has a Reset button |
| W-YAML-CONTENT | `YAML output has valid structure` | Build output screen shows text containing `:` (YAML key–value separator) |
| W-WRITE-FILEPATH | `YAML screen shows Write to File button` | *(graceful skip if YAML screen unreachable)* |

### `swagger.spec.ts` — Swagger / OpenAPI UI (W-SWAGGER)

| ID | Test | What it verifies |
|----|------|-----------------|
| W-SWAGGER | `swagger-ui loads and lists /api/configurations paths` | Navigates to `/api/swagger-ui/index.html`, waits for a successful `/api/openapi.json` response, and verifies the rendered operations include `configurations` |

## Flutter CanvasKit + Playwright Notes

The Flutter web app uses the CanvasKit renderer, which renders via WebGL.
The accessibility DOM (`flt-semantics` elements) is built **lazily** — only
after Chrome's accessibility engine is queried.

`waitForFlutter()` in `helpers.ts` triggers this by calling
`page.accessibility.snapshot()` which sends a CDP `Accessibility.getFullAXTree`
call. Flutter observes this and builds the semantic tree. Without this step,
all `flt-semantics` selectors return 0 results.

The `--force-renderer-accessibility` Chrome flag (set in `playwright.config`)
must also be present to enable the accessibility engine in headless mode.

## CI Integration

```yaml
- name: Run Playwright Tests
  run: |
    cd appbase-config-wizard
    bin/start-web.sh
    cd test/web
    npm ci
    npx playwright install --with-deps chromium
    CONFIG_WIZARD_URL=http://localhost:8080 npm test
```
