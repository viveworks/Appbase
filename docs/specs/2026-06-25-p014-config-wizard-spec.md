# Spec: appbase-config-wizard

**Date:** 2026-06-25  
**Status:** Design approved — ready for implementation plan

---

## What it is

A command-line tool (`appbase-config-wizard`) that lets a developer interactively build a valid YAML configuration file for any Appbase configuration class. The user picks a config class, fills in values field-by-field via prompted input, and the tool writes a ready-to-use YAML file.

Eliminates the current workflow of reading source code → manually writing YAML → discovering wrong field names at startup.

---

## Decisions

### 1. Implementation language and module

**Java shaded (fat) JAR** — only option that can introspect the actual Jackson metadata, instantiate builders, and guarantee the output serialises cleanly.

- Runs on the same JVM, same classpath → can actually instantiate config classes and Jackson builders
- Jackson introspection works natively — no parsing needed
- Output is guaranteed valid (Jackson serialises the built object)
- Built as shaded JAR: `java -jar appbase-config-wizard.jar`
- Must be rebuilt after config class changes (same contract as any compiled JVM tool)

**Module path:** `appbase-java/appbase-java-tools/appbase-config-wizard/`

---

### 2. Config class discovery

**Runtime classpath scan with ClassGraph**

- Scans all JARs on the classpath for classes implementing `AppbaseConfiguration` or extending `AbstractAppbaseManagedServiceConfiguration`
- Fast, handles nested JARs, no index file needed
- Zero build overhead — no annotation processors or manifest generation

---

### 3. Field introspection

Both config patterns must be handled:

| Pattern | Used by | Introspection |
|---|---|---|
| `@JsonCreator` + `@JsonProperty` | `AppbaseConfiguration` impls (service-core, service-layer) | Jackson `BeanDescription` via `SerializationConfig` |
| Builder + `@JsonDeserialize(builder=…)` + `@JsonPOJOBuilder` | `AbstractAppbaseManagedServiceConfiguration` subclasses | Jackson `BeanDescription` via `DeserializationConfig` + builder introspection |

Jackson's `ObjectMapper.getDeserializationConfig().introspect(type)` returns a `BeanDescription` that lists all settable properties, their Java types, and default values from the builder — covers both patterns without custom parsing.

For defaults: instantiate `Builder()` (no-arg) → call `build()` → Jackson-serialize → the result is the "defaults" YAML. Display defaults alongside the prompt.

---

### 4. Nested configuration handling

When a property's type is itself an `AppbaseConfiguration` or `AbstractAppbaseManagedServiceConfiguration` subclass → recurse into it with an indented sub-prompt.

- Offer a `[use defaults]` shortcut to skip and accept the default-constructed nested config
- Depth limit: 3 levels (configs don't go deeper in practice)
- `@Nullable` fields show `(optional)` and accept empty input to leave them null

---

### 5. Interactive CLI framework

**Picocli + JLine 3**

- Tab completion on class names and field names
- Arrow-key history, `?` for help
- Clean ANSI output
- Handles filtering and pick menus with readline-style input

Dependencies: `picocli` + `jline3`

---

### 6. Filtering / search

Users can filter the class list at the pick-a-config step:

| Filter | Example input | Matches |
|---|---|---|
| Module / tech | `aerospike` | `*aerospike*` in package or class name |
| Layer | `client`, `service`, `core` | top-level package segment |
| Free keyword | `session`, `password` | substring in simple class name |

Implementation: normalise all config class names into `{layer}.{tech}.{SimpleClassName}` tuples at scan time; filter on user input substring.

---

### 7. YAML output

- Jackson `YAMLMapper` (already in the codebase) serialises the built object
- Output target: user-specified path, or `$TMPDIR/appbase-<ClassName>-<timestamp>.yaml` if not specified
- Print the path to stdout so the user can immediately `cat` or copy it

---

## Proposed Architecture

```
appbase-java/
  appbase-java-tools/
    pom.xml                         ← aggregator; parent=appbase-parent
    appbase-config-wizard/
      pom.xml                       ← parent=appbase-java-tools; shade plugin; main class
      src/main/java/…/tool/
        YamlConfigTool.java         ← @Command main class (Picocli entry point)
        ConfigScanner.java          ← ClassGraph scan; returns List<ConfigClassInfo>
        ConfigClassInfo.java        ← {className, module, layer, tech, defaultInstance}
        FieldPrompter.java          ← walks BeanDescription; prompts per field; recurses on nested
        YamlWriter.java             ← YAMLMapper.writeValue(outputPath, builtObject)
        menu/
          FilterMenu.java           ← JLine 3 completion + numbered filter+pick menu
          PromptUtil.java           ← readLine with default, type coercion, nullable handling
```

`YamlConfigTool` orchestration:

```
1. scan()           → all config classes on classpath
2. filterMenu()     → user narrows list; picks one class
3. showDefaults()   → instantiate default; print existing YAML
4. fieldPrompter()  → per-field prompts, recurse on nested configs
5. build()          → construct the object (via Jackson builder or @JsonCreator)
6. validate()       → Hibernate Validator; surface violations before write
7. write()          → YAMLMapper → output path
8. print path
```

---

## Configuration Object Evolution

The tool adapts to config class changes automatically — **zero tool changes required** — because it uses runtime introspection rather than hardcoded field lists or codegen.

| Evolution event | Tool behaviour |
|---|---|
| New field added to a config | Jackson finds it; prompt appears automatically |
| Field removed | Prompt disappears |
| Field renamed | New name appears in prompts; old YAML files break (expected) |
| Field type changed | Jackson introspects new type; prompt adapts |
| New config class added | ClassGraph finds it at next run |
| Config class deleted | Disappears from scan results |

**One requirement:** `appbase-config-wizard` must be compiled in the same Maven build as the config modules. Because it lives under `appbase-java/appbase-java-tools/`, `mvn clean install` always compiles all modules together — the fat JAR always contains the current compiled config classes.

---

## Module Setup

### `appbase-java/appbase-java-tools/pom.xml` (new aggregator)
```xml
<parent>appbase-parent</parent>
<artifactId>appbase-java-tools</artifactId>
<packaging>pom</packaging>
<modules>
  <module>appbase-config-wizard</module>
</modules>
```

### Root `appbase-java/pom.xml`
Add `<module>appbase-java-tools</module>` at the end of the modules list.

### `appbase-config-wizard/pom.xml`
- `<parent>appbase-java-tools</parent>`
- Runtime deps (shaded): `appbase-bootstrap`, `appbase-stacktest`, `io.github.classgraph:classgraph`, `info.picocli:picocli`, `org.jline:jline`
- `maven-shade-plugin` with `mainClass=com.viveworks.appbase.tools.yaml.YamlConfigTool`

---

### 8. Web Interface

**Trigger:** `--web` flag (or `--web-port PORT`, default 8090). When omitted, tool runs in pure console mode (existing behaviour unchanged).

**Server:** `JettyAppbaseWebServer` from `appbase-bootstrap-jetty`. No auth, no session — localhost tool only. Boots on the specified port, serves the Next.js static export from classpath (`/static/web/`), mounts the REST API at `/api/`. Prints the URL to console and optionally opens the browser.

**Frontend:** Next.js (static export — `next export` → `out/`) with Tailwind CSS and `next-themes` (system default; toggle button top-right). Bundled into the shaded JAR at build time via `maven-exec-plugin` + `maven-resources-plugin`. No separate deploy needed — fat JAR IS the server.

**Theme:** `next-themes` with `defaultTheme="system"`. Theme toggle button fixed top-right. Light/dark tokens via Tailwind's `dark:` variant.

**Showcase intent:** The tool demonstrates Appbase's own embedded Jetty, JAX-RS, and Jackson stack in a real user-facing product. README leads with "powered by Appbase."

---

#### REST API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/configs` | All discovered config classes (name, module, layer, tech) |
| `GET` | `/api/configs/search?q=&layer=&module=&tech=` | Filtered list — substring match on any field |
| `GET` | `/api/configs/{className}` | Class detail: fields, types, defaults, nested refs |
| `POST` | `/api/configs/{className}/build` | Build + validate; body = `{fieldName: value, …}` map |
| `GET` | `/api/configs/{className}/yaml` | Download last-built YAML as file attachment |

---

#### Frontend Components

```
src/
  app/
    page.tsx           ← split-pane: left = config browser, right = editor/preview
    layout.tsx         ← theme provider, top nav ("Appbase Config Tool"), theme toggle
  components/
    ConfigBrowser/
      SearchBar.tsx    ← filters: free text, layer dropdown, module dropdown, tech dropdown
      ConfigList.tsx   ← flat list of matching config classes; click to open
    ConfigEditor/
      ConfigTree.tsx   ← recursive tree of fields; collapsible nested configs
      FieldInput.tsx   ← type-aware input: text/number/bool/enum/nested
      DefaultBadge.tsx ← shows default value alongside each field
      NullableTag.tsx  ← "(optional)" label for @Nullable fields
    YamlPreview.tsx    ← live YAML panel (re-renders on every field change)
    ThemeToggle.tsx    ← sun/moon icon button, fixed top-right
    DownloadButton.tsx ← triggers GET /api/configs/{class}/yaml
```

**Tree view:** `ConfigTree` renders nested configs as collapsible tree nodes with indent. "Use defaults" shortcut collapses a subtree. Depth limit: 3 levels (mirrors CLI).

**Search:** `SearchBar` sends `?q=&layer=&module=&tech=` to `/api/configs/search`. Results update live (debounced 200ms). Filter chips visible below the search box.

---

#### Build Pipeline

```
appbase-java/appbase-java-tools/
  appbase-config-wizard/
    pom.xml                    ← existing; gains exec + resources phases
    frontend/                  ← Next.js project root
      package.json
      next.config.js           ← output: 'export'
      src/                     ← components above
    src/main/java/…/tool/
      web/
        ConfigWebServer.java   ← boots JettyAppbaseWebServer; mounts /api + /static
        ConfigApiResource.java ← JAX-RS @Path("/api/configs") resource
        ConfigApiDto.java      ← request/response record types
```

Maven build sequence (in `appbase-config-wizard/pom.xml`):
1. `frontend-maven-plugin`: `npm install` + `npm run build` (Next.js static export to `frontend/out/`)
2. `maven-resources-plugin`: copy `frontend/out/` → `src/main/resources/static/web/`
3. `maven-shade-plugin`: shade everything including the copied static assets

---

## Effort Estimate

### Console-only (baseline)

| Phase | Scope | Estimate |
|---|---|---|
| Module scaffold + POM setup | aggregator + tool POM + shade plugin | XS (<1h) |
| Classpath scan | `ConfigScanner` + `ConfigClassInfo` | S (2–4h) |
| Filter/pick menu with JLine | `FilterMenu` + tab completion | S (2–4h) |
| Field prompting (flat configs) | `FieldPrompter` for `@JsonProperty` + builder | M (1–2d) |
| Nested config recursion | Depth-limited recursion in `FieldPrompter` | S (2–4h) |
| YAML write + validation | `YamlWriter` + Hibernate Validator | XS (<1h) |
| **Console subtotal** | | **M (2–3d)** |

### Web interface (additional)

| Phase | Scope | Estimate |
|---|---|---|
| `--web` flag + `ConfigWebServer` | Jetty boot, static serving, `AppbaseApplication` wire-up | XS (2–3h) |
| `ConfigApiResource` (JAX-RS) | 5 endpoints, DTOs, validation error mapping | S (3–5h) |
| Next.js scaffold + Tailwind + next-themes | Project init, layout, theme toggle | S (3–4h) |
| `ConfigBrowser` (SearchBar + list) | Filters: text, layer, module, tech; live debounce | S (3–4h) |
| `ConfigTree` (tree view, collapsible) | Recursive nested field tree, depth limit, "use defaults" | S (4–6h) |
| `FieldInput` (type-aware inputs) | text / number / bool / enum / nested variants | S (4–6h) |
| `YamlPreview` (live YAML panel) | Re-render on change, copy-to-clipboard, syntax highlight | S (2–3h) |
| `DownloadButton` + file save | Trigger YAML download | XS (1h) |
| Maven build integration | frontend-maven-plugin + resources copy + shade | XS (2h) |
| API unit tests + IT (Jetty start + HTTP) | | S (3–4h) |
| **Web subtotal** | | **L (2–3d)** |

### Grand total

| Scope | Estimate |
|---|---|
| Console + web | **L–XL (4–6d)** |

> Console and web share the `ConfigScanner`, `ConfigClassInfo`, and `FieldPrompter` model layer — the MVC split means no duplication between the two views.
