# P-014 — appbase-config-wizard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `appbase-config-wizard` — a fat JAR that lets developers interactively build valid YAML config files for any Appbase config class, with both a console CLI and an embedded web UI.

**Architecture:** MVC split — Model layer (`ConfigScanner`, `ConfigClassInfo`, Jackson introspection into `FieldInfo`), Console View (Picocli + JLine3), Web View (Next.js static export served by `JettyAppbaseWebServer`). A `--web` / `--web-port` flag on the CLI entry point activates the web server instead of the interactive console. Model layer is shared between both views with zero duplication.

**Tech Stack:** Java 25 · Maven 3.6+ · ClassGraph 4.8.174 (classpath scan) · Picocli 4.7.6 + JLine 3.26.3 (console) · Jackson YAMLMapper (output) · Hibernate Validator (validation) · `JettyAppbaseWebServer` + Jersey 3.x (web server + REST) · Next.js 14 + Tailwind CSS 3 + next-themes 0.3 (frontend) · `frontend-maven-plugin` 1.15 (Node in Maven) · maven-shade-plugin (fat JAR)

**Spec:** `docs/specs/2026-06-25-p014-config-wizard-spec.md`

---

## Global Constraints

- Java 25 source/target. Maven 3.6+. All new dependency versions in `appbase-java/pom.xml` `<dependencyManagement>` only — never hardcoded in the tool POM.
- Module path: `appbase-java/appbase-java-tools/appbase-config-wizard/`
- Aggregator POM: `appbase-java/appbase-java-tools/pom.xml` — `parent=appbase-parent`, `<packaging>pom</packaging>`, one `<module>appbase-config-wizard</module>`
- Tool POM: `parent=appbase-java-tools`; shade `mainClass=com.viveworks.appbase.tools.yaml.YamlConfigTool`; fat JAR is the **primary** artifact (not a `-shaded` classifier)
- Java package root: `com.viveworks.appbase.tools.yaml`; sub-packages: `.scan`, `.field`, `.menu`, `.web`, `.web.dto`
- Every Java class: `@author Navaneeth Mysore G` on every class; `@since 1.0.0`
- Use `JettyAppbaseWebServer` from `appbase-bootstrap-jetty` (renamed in P-015 Phase 1 — do NOT use the old `JettyAppbaseServer`)
- `--web` flag: optional; `--web-port` default `8090`; no auth, no session — localhost tool only
- REST API base path: `/api`; resource at `com.viveworks.appbase.tools.yaml.web.ConfigApiResource`; classpath static assets at `/static/web/` served by Jetty `DefaultServlet` at `/*`
- YAML output: `$TMPDIR/appbase-<SimpleClassName>-<timestamp>.yaml` when no `--output` path supplied
- Nested config depth limit: 3 levels; `@Nullable` fields show `(optional)` and accept empty input → `null`
- Next.js: `output: 'export'`; frontend source in `frontend/` inside the tool module root; Node 20.x + npm 10.x via `frontend-maven-plugin`; `next-themes` `defaultTheme="system"`; Tailwind `darkMode: 'class'`; theme toggle fixed top-right
- Hibernate Validator validates the built object before YAML write (both console and REST paths)
- Classpath scan targets: `AppbaseConfiguration` implementors + `AbstractAppbaseManagedServiceConfiguration` subclasses (non-abstract, non-interface only)
- OSGi: `maven-bundle-plugin`; `Export-Package: com.viveworks.appbase.tools.yaml.*`; `Import-Package: !.,*`
- Profiles: `dev-profile`, `jacoco-no-rules-profile` for local builds; add `maven-failsafe-plugin-profile` for IT
- Every Java class Javadoc: `@author Navaneeth Mysore G` first, then `@author Claude Sonnet 4.6` second; `@since 1.0.0`

---

## File Structure

```
appbase-java/
  pom.xml                                                  ← MODIFY: add <module>appbase-java-tools</module>
  appbase-java-tools/
    pom.xml                                                ← NEW aggregator
    appbase-config-wizard/
      pom.xml                                              ← NEW tool POM (shade + bundle + frontend)
      LICENSE                                              ← copy from appbase-bootstrap
      NOTICE                                               ← copy from appbase-bootstrap
      README.adoc                                          ← NEW setup, start/stop instructions, "powered by Appbase"
      bin/
        run-console.sh                                     ← NEW builds JAR if absent; runs console mode; passes $@ to JAR
        run-web.sh                                         ← NEW builds JAR if absent; starts web server in background; writes PID to .wizard-web.pid
        stop-web.sh                                        ← NEW reads .wizard-web.pid; kills process; removes PID file
      frontend/                                            ← NEW Next.js project root
        package.json
        next.config.js
        tailwind.config.js
        tsconfig.json
        src/app/
          page.tsx                                         ← split-pane layout (left=browser, right=editor+preview)
          layout.tsx                                       ← ThemeProvider, top nav, ThemeToggle
          globals.css
        src/components/
          ConfigBrowser/
            SearchBar.tsx                                  ← free-text + layer/module/tech dropdowns; 200ms debounce
            ConfigList.tsx                                 ← flat list of config classes; click → open editor
          ConfigEditor/
            ConfigTree.tsx                                 ← recursive collapsible field tree; depth ≤ 3
            FieldInput.tsx                                 ← type-aware: text/number/bool/enum/nested
            DefaultBadge.tsx                               ← shows default value alongside each field
            NullableTag.tsx                                ← "(optional)" label for @Nullable fields
          YamlPreview.tsx                                  ← live YAML panel; re-renders on every field change
          ThemeToggle.tsx                                  ← sun/moon button; calls useTheme()
          DownloadButton.tsx                               ← triggers GET /api/configs/{class}/yaml
        src/lib/
          api.ts                                           ← typed client wrapping /api/configs/*
      src/main/java/com/viveworks/appbase/tools/yaml/
        YamlConfigTool.java                                ← NEW Picocli @Command entry point
        scan/
          ConfigClassInfo.java                             ← NEW record (className, simpleClassName, layer, module, configClass, defaultInstance)
          ConfigScanner.java                               ← NEW ClassGraph scan; search() filter
        field/
          FieldInfo.java                                   ← NEW record (name, javaType, defaultValue, nullable, nestedClass)
          BuiltConfig.java                                 ← NEW record (ConfigClassInfo, builtInstance)
          FieldPrompter.java                               ← NEW: introspect() + prompt() + buildFromMap()
        menu/
          FilterMenu.java                                  ← NEW JLine3 numbered pick menu + tab completion
          PromptUtil.java                                  ← NEW readLine helpers with default, coercion, nullable
        web/
          ConfigWebServer.java                             ← NEW Jetty boot; mounts /api/* + static /*
          ConfigApiResource.java                           ← NEW JAX-RS @Path("/api/configs") — 5 endpoints
          dto/
            ConfigClassDto.java                            ← NEW record
            ConfigFieldDto.java                            ← NEW record
            BuildRequestDto.java                           ← NEW record
            BuildResultDto.java                            ← NEW record
        YamlWriter.java                                    ← NEW YAMLMapper.writeValue(path, instance)
      src/test/java/com/viveworks/appbase/tools/yaml/
        scan/ConfigScannerTest.java                        ← NEW unit
        field/FieldPrompterTest.java                       ← NEW unit (introspect only; no terminal)
        YamlWriterTest.java                                ← NEW unit (round-trip YAML)
        web/ConfigApiResourceIT.java                       ← NEW IT (real Jetty, ephemeral port)
```

---

### Task 1: Maven module scaffold + parent depMgmt

**Files:**
- Modify: `appbase-java/pom.xml` — add `<module>appbase-java-tools</module>` + three new `<dependency>` entries in `<dependencyManagement>`
- Create: `appbase-java/appbase-java-tools/pom.xml`
- Create: `appbase-java/appbase-java-tools/appbase-config-wizard/pom.xml`
- Create: `appbase-java/appbase-java-tools/appbase-config-wizard/LICENSE`
- Create: `appbase-java/appbase-java-tools/appbase-config-wizard/NOTICE`

**Interfaces:**
- Produces: `mvn clean install -pl appbase-java-tools/appbase-config-wizard -am -Dmaven.test.skip=true -P dev-profile` → BUILD SUCCESS with no source files

- [ ] **Step 1: Add three new deps to `appbase-java/pom.xml` `<dependencyManagement>`**

In `appbase-java/pom.xml`, inside `<dependencyManagement><dependencies>`, add alongside other third-party deps:

```xml
<!-- appbase-config-wizard console + scan deps -->
<dependency>
    <groupId>io.github.classgraph</groupId>
    <artifactId>classgraph</artifactId>
    <version>4.8.174</version>
</dependency>
<dependency>
    <groupId>info.picocli</groupId>
    <artifactId>picocli</artifactId>
    <version>4.7.6</version>
</dependency>
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>3.26.3</version>
</dependency>
```

- [ ] **Step 2: Add `<module>appbase-java-tools</module>` to root reactor**

In `appbase-java/pom.xml`, inside the `<modules>` block, add after `<module>appbase-stacktest</module>`:

```xml
<module>appbase-java-tools</module>
```

- [ ] **Step 3: Create aggregator POM**

```xml
<!-- appbase-java/appbase-java-tools/pom.xml -->
<?xml version='1.0' encoding='UTF-8'?>
<!--
  Copyright 2019 The original author or authors of Appbase
  [standard Apache 2.0 header — copy from appbase-bootstrap-jetty/pom.xml]
-->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                      http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.viveworks.appbase</groupId>
        <artifactId>appbase-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>appbase-java-tools</artifactId>
    <name>Appbase Java Tools</name>
    <packaging>pom</packaging>
    <modules>
        <module>appbase-config-wizard</module>
    </modules>
</project>
```

- [ ] **Step 4: Create tool POM**

```xml
<!-- appbase-java/appbase-java-tools/appbase-config-wizard/pom.xml -->
<?xml version='1.0' encoding='UTF-8'?>
<!-- [standard Apache 2.0 header] -->
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.viveworks.appbase</groupId>
        <artifactId>appbase-java-tools</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>appbase-config-wizard</artifactId>
    <name>Appbase YAML Config Wizard</name>
    <description>Interactive CLI + web tool for building valid YAML configuration files for Appbase services</description>

    <dependencies>
        <!-- Appbase runtime — config classes from all modules on fat-JAR classpath -->
        <dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-bootstrap</artifactId><version>${appbase.version}</version></dependency>
        <dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-bootstrap-jetty</artifactId><version>${appbase.version}</version></dependency>
        <dependency><groupId>com.viveworks.appbase</groupId><artifactId>appbase-stacktest</artifactId><version>${appbase.version}</version></dependency>
        <!-- Classpath scan -->
        <dependency><groupId>io.github.classgraph</groupId><artifactId>classgraph</artifactId></dependency>
        <!-- Console -->
        <dependency><groupId>info.picocli</groupId><artifactId>picocli</artifactId></dependency>
        <dependency><groupId>org.jline</groupId><artifactId>jline</artifactId></dependency>
        <!-- Test -->
        <dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.mockito</groupId><artifactId>mockito-junit-jupiter</artifactId><scope>test</scope></dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- OSGi manifest -->
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <configuration>
                    <archive><manifest>
                        <addClasspath>${archive.addClasspath}</addClasspath>
                        <addDefaultImplementationEntries>${archive.addDefaultImplementationEntries}</addDefaultImplementationEntries>
                        <packageName>com.viveworks.appbase.tools.yaml</packageName>
                    </manifest></archive>
                    <instructions>
                        <Export-Package>com.viveworks.appbase.tools.yaml.*</Export-Package>
                        <Import-Package>!.,*</Import-Package>
                    </instructions>
                </configuration>
            </plugin>

            <!-- Fat JAR — primary artifact (not -shaded classifier) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <shadedArtifactAttached>false</shadedArtifactAttached>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.viveworks.appbase.tools.yaml.YamlConfigTool</mainClass>
                                </transformer>
                                <!-- Merge META-INF/services for Jersey SPI -->
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                                <!-- Merge Jackson module registrations -->
                                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                    <resource>META-INF/jackson-module-registration</resource>
                                </transformer>
                            </transformers>
                            <filters>
                                <!-- Drop signature files to avoid JAR verification failures -->
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- frontend-maven-plugin and maven-resources-plugin copy — added in Task 8 -->
        </plugins>
    </build>
</project>
```

- [ ] **Step 5: Copy LICENSE and NOTICE**

```bash
cp appbase-java/appbase-bootstrap/LICENSE appbase-java/appbase-java-tools/appbase-config-wizard/
cp appbase-java/appbase-bootstrap/NOTICE  appbase-java/appbase-java-tools/appbase-config-wizard/
```

- [ ] **Step 6: Verify empty module builds**

```bash
cd appbase-java
mvn clean install -pl appbase-java-tools/appbase-config-wizard -am \
    -Dmaven.test.skip=true -P dev-profile,jacoco-no-rules-profile
```
Expected: `BUILD SUCCESS` — no source files compiled yet but module resolves.

- [ ] **Step 7: Commit**

```bash
git add appbase-java/pom.xml appbase-java/appbase-java-tools/
git commit -m "build(P-014): scaffold appbase-java-tools aggregator + appbase-config-wizard module; add classgraph/picocli/jline to depMgmt"
```

---

### Task 2: Config discovery — `ConfigScanner` + `ConfigClassInfo`

**Files:**
- Create: `src/main/java/.../scan/ConfigClassInfo.java`
- Create: `src/main/java/.../scan/ConfigScanner.java`
- Create: `src/test/java/.../scan/ConfigScannerTest.java`

**Interfaces:**
- Consumes: `AppbaseConfiguration` and `AbstractAppbaseManagedServiceConfiguration` from `appbase-bootstrap`
- Produces: `ConfigScanner.scan() → List<ConfigClassInfo>`; `ConfigScanner.search(q, layer, module, tech) → List<ConfigClassInfo>`

```java
/**
 * Metadata for a discovered Appbase configuration class.
 *
 * <p>{@code layer} is derived from the top-level package segment after
 * {@code com.viveworks.appbase.} — e.g. "client", "service", "core".
 *
 * <p>{@code module} is the technology segment — e.g. "aerospike", "kafka", "redis".
 *
 * <p>{@code defaultInstance} is the object produced by instantiating the builder
 * (no-arg) and calling {@code build()} — or for {@code @JsonCreator} patterns, a
 * Jackson-constructed instance with all defaults. Null if construction fails.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public record ConfigClassInfo(
    String className,           // FQN, e.g. "com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientIntegrationServiceConfiguration"
    String simpleClassName,     // "DefaultRedisClientIntegrationServiceConfiguration"
    String layer,               // "client"
    String module,              // "redis"
    Class<?> configClass,       // loaded Class object
    @Nullable Object defaultInstance  // default-constructed instance; null on failure
) {}

/**
 * Discovers Appbase configuration classes on the runtime classpath using ClassGraph.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public final class ConfigScanner {

    /**
     * Scans the full classpath for concrete classes implementing {@link AppbaseConfiguration}
     * or subclassing {@link AbstractAppbaseManagedServiceConfiguration}.
     * Abstract classes and interfaces are excluded.
     *
     * @return all matches, sorted by module then simpleClassName
     */
    public List<ConfigClassInfo> scan() {
        // ClassGraph().enableClassInfo().scan() → getClassesImplementing + getSubclasses
        // filter: !isAbstract && !isInterface
        // map each ClassInfo → toConfigClassInfo(ci.loadClass())
        // sort by module, then simpleClassName
        // toList()
    }

    /**
     * Returns a filtered subset of {@link #scan()} results.
     *
     * @param query  case-insensitive substring matched against simpleClassName; null = no filter
     * @param layer  exact match on {@link ConfigClassInfo#layer()}; null = any
     * @param module exact match on {@link ConfigClassInfo#module()}; null = any
     * @param tech   additional substring on simpleClassName; null = any
     */
    public List<ConfigClassInfo> search(
            @Nullable String query,
            @Nullable String layer,
            @Nullable String module,
            @Nullable String tech) {
        // stream scan(), apply each non-null predicate, toList()
    }

    /**
     * Derives layer/module from the class's package name.
     * Builds a defaultInstance via reflection (try builder first, then ObjectMapper default).
     */
    private ConfigClassInfo toConfigClassInfo(Class<?> clazz) {
        // package segments after "com.viveworks.appbase." → layer = segment[0], module = segment[2] (e.g. "client.service.redis")
        // default instance: try clazz.getDeclaredMethod("builder").invoke(null).getClass().getMethod("build").invoke(builder)
        //                   catch all, fall back to ObjectMapper.convertValue(new HashMap<>(), clazz), or null
    }
}
```

- [ ] **Step 1: Write failing test**

```java
// src/test/java/.../scan/ConfigScannerTest.java
class ConfigScannerTest {

    private final ConfigScanner scanner = new ConfigScanner();

    @Test
    void scan_discoversAtLeastOneConfigClass() {
        // Aerospike/Redis/etc. are on classpath via appbase-stacktest dep
        assertFalse(scanner.scan().isEmpty());
    }

    @Test
    void scan_excludesAbstractClassesAndInterfaces() {
        var results = scanner.scan();
        assertTrue(results.stream().noneMatch(r -> Modifier.isAbstract(r.configClass().getModifiers())));
        assertTrue(results.stream().noneMatch(r -> r.configClass().isInterface()));
    }

    @Test
    void search_filtersByLayer() {
        var results = scanner.search(null, "client", null, null);
        assertTrue(results.stream().allMatch(r -> "client".equals(r.layer())));
    }

    @Test
    void search_filtersByQuery() {
        var results = scanner.search("redis", null, null, null);
        assertTrue(results.stream().allMatch(r -> r.simpleClassName().toLowerCase().contains("redis")));
    }

    @Test
    void scan_eachInfoHasNonNullFields() {
        var first = scanner.scan().get(0);
        assertNotNull(first.className());
        assertNotNull(first.simpleClassName());
        assertNotNull(first.layer());
        assertNotNull(first.module());
        assertNotNull(first.configClass());
        // defaultInstance may be null — no assertion
    }
}
```

- [ ] **Step 2: Run — expect FAIL** (`ConfigScanner` does not exist yet)

```bash
cd appbase-java
mvn test -pl appbase-java-tools/appbase-config-wizard -Dtest=ConfigScannerTest -P dev-profile,jacoco-no-rules-profile
```
Expected: COMPILE ERROR — `ConfigScanner cannot be found`

- [ ] **Step 3: Implement `ConfigClassInfo` record and `ConfigScanner`** following the stubs above

- [ ] **Step 4: Run — expect PASS**

```bash
mvn test -pl appbase-java-tools/appbase-config-wizard -Dtest=ConfigScannerTest -P dev-profile,jacoco-no-rules-profile
```
Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add appbase-java/appbase-java-tools/
git commit -m "feat(P-014): add ConfigScanner + ConfigClassInfo with ClassGraph classpath scan"
```

---

### Task 3: Field introspection — `FieldInfo`, `BuiltConfig`, `FieldPrompter.introspect()`, `YamlWriter`

This task covers the Jackson introspection model only — no console prompting yet (that is Task 4). It also adds `YamlWriter` because writing YAML requires a built object, and a round-trip test (`introspect → build → write → parse back`) validates the whole model layer together.

**Files:**
- Create: `src/main/java/.../field/FieldInfo.java`
- Create: `src/main/java/.../field/BuiltConfig.java`
- Create: `src/main/java/.../field/FieldPrompter.java` (introspect + buildFromMap only; prompt() added in Task 4)
- Create: `src/main/java/.../YamlWriter.java`
- Create: `src/test/java/.../field/FieldPrompterTest.java`
- Create: `src/test/java/.../YamlWriterTest.java`

**Interfaces:**
- Consumes: `ConfigClassInfo` from Task 2
- Produces:
  - `FieldPrompter.introspect(ConfigClassInfo) → List<FieldInfo>`
  - `FieldPrompter.buildFromMap(ConfigClassInfo, Map<String,Object>) → BuiltConfig` (used by REST path)
  - `YamlWriter.write(BuiltConfig, @Nullable Path) → Path`

```java
/**
 * Metadata for one Jackson-visible property on an Appbase config class.
 *
 * @param name         Jackson property name as it will appear in YAML (e.g. "commandTimeoutMs")
 * @param javaType     resolved Jackson JavaType (carries generics info)
 * @param typeName     human-readable type string shown in prompts (e.g. "String", "int", "boolean")
 * @param defaultValue string representation derived from the defaultInstance, or null
 * @param nullable     true when the field is annotated {@code @Nullable}
 * @param nestedClass  non-null when this field's type is itself an {@link AppbaseConfiguration} —
 *                     the prompter recurses into it
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public record FieldInfo(
    String name,
    JavaType javaType,
    String typeName,
    @Nullable String defaultValue,
    boolean nullable,
    @Nullable Class<?> nestedClass
) {}

/**
 * Holds the Java object built for a specific config class.
 *
 * @param info     the config class metadata
 * @param instance the built, populated instance ready for YAML serialisation
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public record BuiltConfig(ConfigClassInfo info, Object instance) {}

/**
 * Responsible for:
 * <ol>
 *   <li>Introspecting Appbase config classes via Jackson {@link BeanDescription} to produce
 *       a {@link FieldInfo} list.</li>
 *   <li>Building a config instance from a {@link Map} of field values (REST path).</li>
 *   <li>Prompting the user interactively via JLine3 (console path — added in Task 4).</li>
 * </ol>
 *
 * <p>Both Jackson patterns are supported:
 * <ul>
 *   <li>{@code @JsonCreator} + {@code @JsonProperty} (AppbaseConfiguration impls in service-core)</li>
 *   <li>Builder + {@code @JsonDeserialize(builder=…)} + {@code @JsonPOJOBuilder}
 *       (AbstractAppbaseManagedServiceConfiguration subclasses)</li>
 * </ul>
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public final class FieldPrompter {

    private final ObjectMapper mapper;

    public FieldPrompter(final ObjectMapper mapper) {
        // store mapper; set up YAML mapper for default serialisation
    }

    /**
     * Introspects {@code info.configClass()} via {@code mapper.getDeserializationConfig().introspect()}.
     *
     * <p>For each settable property:
     * <ul>
     *   <li>Resolves the Java type to a human-readable {@code typeName}</li>
     *   <li>Derives {@code defaultValue} by serialising the corresponding field from
     *       {@code info.defaultInstance()} (null → shown as "null" or "(none)")</li>
     *   <li>Sets {@code nullable=true} when the property member has {@code @Nullable}</li>
     *   <li>Sets {@code nestedClass} when the property type implements {@link AppbaseConfiguration}</li>
     * </ul>
     *
     * @return ordered list matching Jackson property order
     */
    public List<FieldInfo> introspect(final ConfigClassInfo info) {
        // BeanDescription desc = mapper.getDeserializationConfig().introspect(mapper.constructType(info.configClass()))
        // desc.findProperties().stream() → map toFieldInfo(prop, info.defaultInstance())
    }

    /**
     * Builds a {@link BuiltConfig} from a string/object map (for the REST endpoint).
     *
     * <p>Converts {@code fieldValues} to the target type via
     * {@code mapper.convertValue(fieldValues, info.configClass())}.
     *
     * @throws IllegalArgumentException when conversion fails due to a type mismatch
     */
    public BuiltConfig buildFromMap(
            final ConfigClassInfo info,
            final Map<String, Object> fieldValues) {
        // mapper.convertValue(fieldValues, info.configClass()) → new BuiltConfig(info, instance)
    }
}

/**
 * Serialises a {@link BuiltConfig} to YAML via Jackson {@link YAMLMapper}.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public final class YamlWriter {

    private final YAMLMapper yamlMapper;

    public YamlWriter(final YAMLMapper yamlMapper) { /* store */ }

    /**
     * Writes the config instance to {@code outputPath}, or to a temp file when null.
     * The temp file name is {@code appbase-<SimpleClassName>-<epoch-millis>.yaml}
     * in {@code System.getProperty("java.io.tmpdir")}.
     *
     * @return the path actually written
     * @throws IOException on write failure
     */
    public Path write(final BuiltConfig config, @Nullable final Path outputPath) throws IOException {
        // resolve path: outputPath != null ? outputPath : tempPath(config.info())
        // yamlMapper.writeValue(path.toFile(), config.instance())
        // return path
    }
}
```

- [ ] **Step 1: Write failing tests**

```java
// FieldPrompterTest
class FieldPrompterTest {

    private final ConfigScanner scanner = new ConfigScanner();
    private final FieldPrompter prompter = new FieldPrompter(new ObjectMapper());

    @Test
    void introspect_redisConfig_returnsExpectedFields() {
        // Find DefaultRedisClientIntegrationServiceConfiguration — it has builder pattern
        var info = scanner.scan().stream()
                .filter(c -> c.simpleClassName().contains("DefaultRedisClient"))
                .findFirst().orElseThrow();
        var fields = prompter.introspect(info);
        assertFalse(fields.isEmpty());
        // hostPortConfiguration must appear (it is a nested AppbaseConfiguration)
        assertTrue(fields.stream().anyMatch(f -> "hostPortConfiguration".equals(f.name())));
        assertTrue(fields.stream().anyMatch(f -> f.nestedClass() != null));
        // database must be nullable
        assertTrue(fields.stream()
                .filter(f -> "database".equals(f.name()))
                .findFirst().orElseThrow().nullable());
    }

    @Test
    void buildFromMap_redisConfig_roundTrips() throws Exception {
        var info = scanner.scan().stream()
                .filter(c -> c.simpleClassName().contains("DefaultRedisClient"))
                .findFirst().orElseThrow();
        var fieldValues = new HashMap<String, Object>();
        fieldValues.put("hostPortConfiguration", Map.of("host", "myhost", "port", 1234));
        fieldValues.put("ssl", true);
        var built = prompter.buildFromMap(info, fieldValues);
        assertNotNull(built.instance());
    }
}

// YamlWriterTest
class YamlWriterTest {

    @Test
    void write_toTempFile_producesReadableYaml(@TempDir Path tmp) throws Exception {
        var scanner = new ConfigScanner();
        var prompter = new FieldPrompter(new ObjectMapper());
        var writer = new YamlWriter(new YAMLMapper());

        var info = scanner.scan().stream()
                .filter(c -> c.simpleClassName().contains("DefaultRedisClient"))
                .findFirst().orElseThrow();
        var fieldValues = Map.of("ssl", false);
        var built = prompter.buildFromMap(info, fieldValues);

        var written = writer.write(built, tmp.resolve("out.yaml"));

        // File must exist and contain the YAML key for at least one known field
        assertTrue(Files.exists(written));
        var content = Files.readString(written);
        assertTrue(content.contains("ssl:"), "Expected 'ssl:' in:\n" + content);
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
mvn test -pl appbase-java-tools/appbase-config-wizard \
    -Dtest="FieldPrompterTest,YamlWriterTest" -P dev-profile,jacoco-no-rules-profile
```
Expected: COMPILE ERROR

- [ ] **Step 3: Implement `FieldInfo`, `BuiltConfig`, `FieldPrompter` (introspect + buildFromMap), `YamlWriter`**

- [ ] **Step 4: Run — expect PASS**

```bash
mvn test -pl appbase-java-tools/appbase-config-wizard \
    -Dtest="FieldPrompterTest,YamlWriterTest" -P dev-profile,jacoco-no-rules-profile
```
Expected: `Tests run: 3, Failures: 0`

- [ ] **Step 5: Commit**

```bash
git add appbase-java/appbase-java-tools/
git commit -m "feat(P-014): add FieldPrompter (introspect+buildFromMap), FieldInfo, BuiltConfig, YamlWriter"
```

---

### Task 4: Console prompting — `FilterMenu`, `PromptUtil`, `FieldPrompter.prompt()`

**Files:**
- Create: `src/main/java/.../menu/FilterMenu.java`
- Create: `src/main/java/.../menu/PromptUtil.java`
- Modify: `src/main/java/.../field/FieldPrompter.java` — add `prompt()` method

**Interfaces:**
- Consumes: `ConfigScanner` (Task 2), `FieldPrompter.introspect()` (Task 3); JLine3 `Terminal`
- Produces: `FilterMenu.pick() → ConfigClassInfo`; `FieldPrompter.prompt(ConfigClassInfo, Terminal, int) → BuiltConfig`

```java
/**
 * JLine3 filter-and-pick menu for selecting a config class.
 *
 * <p>Presents a numbered list of all classes from {@link ConfigScanner#scan()}.
 * The user can type a filter string to narrow the list; the list re-prints
 * with matching entries only. Tab completes on module or class name fragments.
 * Entering a number selects that entry.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public final class FilterMenu {

    private final ConfigScanner scanner;
    private final Terminal terminal;
    private final PrintWriter out;

    public FilterMenu(final ConfigScanner scanner, final Terminal terminal) { /* … */ }

    /**
     * Displays the filterable list and blocks until the user makes a selection.
     *
     * @return the selected {@link ConfigClassInfo}
     * @throws IOException on terminal I/O failure
     */
    public ConfigClassInfo pick() throws IOException {
        // Build LineReader with completer = new StringsCompleter(all simpleClassNames + modules)
        // Loop:
        //   print numbered list of current filtered results
        //   readLine("Filter [Enter=show all, number=select, q=quit]: ")
        //   if input is number → return results.get(n-1)
        //   if "q" → throw UserAbortException
        //   else → update filter, re-print
    }
}

/**
 * JLine3 readline helpers for per-field prompting.
 *
 * <p>Each {@code read*} method:
 * <ol>
 *   <li>Prints {@code fieldName [defaultValue] (optional)?: } to the terminal</li>
 *   <li>Reads a line; empty input returns the default (or null for nullable)</li>
 *   <li>Coerces the string to the target type; re-prompts on parse error</li>
 * </ol>
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public final class PromptUtil {

    private final LineReader reader;
    private final PrintWriter out;

    public PromptUtil(final LineReader reader) { /* … */ }

    /** @return trimmed string, or {@code defaultValue} when input is blank */
    public @Nullable String readString(String fieldName, @Nullable String defaultValue, boolean nullable) throws IOException { /* … */ }

    /** Loops on bad integer input until a valid int is entered. */
    public int readInt(String fieldName, @Nullable Integer defaultValue) throws IOException { /* … */ }

    /** Accepts "true"/"false"/"yes"/"no"/"y"/"n" (case-insensitive); loops on other input. */
    public boolean readBoolean(String fieldName, @Nullable Boolean defaultValue) throws IOException { /* … */ }

    /** For enum fields: prints numbered choices; returns selected constant name. */
    public String readEnum(String fieldName, List<String> choices, @Nullable String defaultValue) throws IOException { /* … */ }
}
```

Add `prompt()` to the existing `FieldPrompter` class:

```java
// Inside FieldPrompter — add this method in Task 4
/**
 * Interactively prompts the user for each field of {@code info.configClass()}.
 *
 * <p>Shows the existing defaults (from {@code info.defaultInstance()}) next to each prompt.
 * Recurses into nested {@link AppbaseConfiguration} fields up to {@code depth} 3;
 * at depth 3 offers "{@code [use defaults]}" to skip without prompting sub-fields.
 *
 * @param info     config class to build
 * @param terminal JLine3 terminal (caller owns lifecycle)
 * @param depth    current recursion depth; caller passes 0
 * @return the fully built config
 */
public BuiltConfig prompt(
        final ConfigClassInfo info,
        final Terminal terminal,
        final int depth) throws Exception {
    // introspect(info) → List<FieldInfo>
    // for each field:
    //   if depth >= 3 && field.nestedClass() != null → use defaultInstance sub-object
    //   elif field.nestedClass() != null → offer "[use defaults]" shortcut; if declined recurse
    //   elif field.nullable() → readNullable via PromptUtil; allow empty
    //   else → PromptUtil.readString / readInt / readBoolean / readEnum based on typeName
    // assemble fieldValues map → buildFromMap(info, fieldValues)
}
```

Console flow is tested manually (terminal I/O); unit test covers `introspect` only (covered in Task 3). No additional unit tests needed here — the integration is validated end-to-end in Task 5.

- [ ] **Step 1: Implement `FilterMenu`, `PromptUtil`, `FieldPrompter.prompt()`**

- [ ] **Step 2: Manual smoke test** — run the tool in console mode, select a config class, fill in a few fields, check output YAML

```bash
cd appbase-java
mvn package -pl appbase-java-tools/appbase-config-wizard -am -Dmaven.test.skip=true -P dev-profile,jacoco-no-rules-profile
java -jar appbase-java-tools/appbase-config-wizard/target/appbase-config-wizard-*.jar
```
Expected: numbered config-class list prints; filtering narrows it; after selection, per-field prompts appear; YAML file path is printed at end.

- [ ] **Step 3: Commit**

```bash
git add appbase-java/appbase-java-tools/
git commit -m "feat(P-014): add FilterMenu, PromptUtil, FieldPrompter.prompt() — interactive console view"
```

---

### Task 5: CLI entry point — `YamlConfigTool` (console path complete)

**Files:**
- Create: `src/main/java/.../YamlConfigTool.java`

**Interfaces:**
- Consumes: `ConfigScanner`, `FilterMenu`, `FieldPrompter`, `YamlWriter` (Tasks 2–4)
- Produces: `java -jar appbase-config-wizard.jar` → interactive session → YAML file written; `--help` prints usage

```java
/**
 * Picocli entry point for {@code appbase-config-wizard}.
 *
 * <p>Console mode (default):
 * <ol>
 *   <li>Scan classpath for config classes</li>
 *   <li>Show filter-and-pick menu</li>
 *   <li>Print current defaults as YAML preview</li>
 *   <li>Prompt field-by-field</li>
 *   <li>Validate via Hibernate Validator; surface violations before write</li>
 *   <li>Write YAML; print path</li>
 * </ol>
 *
 * <p>Web mode ({@code --web}): scan → boot {@link ConfigWebServer} → print URL → block.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
@Command(
    name = "appbase-config-wizard",
    mixinStandardHelpOptions = true,
    versionProvider = YamlConfigTool.VersionProvider.class,
    description = "Interactively build YAML configuration files for any Appbase service."
)
public final class YamlConfigTool implements Callable<Integer> {

    @Option(names = {"--web"}, description = "Start embedded web UI instead of console prompts")
    private boolean web = false;

    @Option(names = {"--web-port"}, description = "Port for web UI (default: ${DEFAULT-VALUE})", defaultValue = "8090")
    private int webPort;

    @Option(names = {"-o", "--output"}, description = "YAML output file path (default: $TMPDIR/appbase-<Class>-<ts>.yaml)")
    @Nullable
    private Path outputPath;

    public static void main(final String[] args) {
        System.exit(new CommandLine(new YamlConfigTool()).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        return web ? runWeb() : runConsole();
    }

    /**
     * Orchestrates the interactive console flow:
     * scan → pick → showDefaults → prompt → validate → write → printPath.
     * Returns 0 on success, 1 on validation failure, 2 on user abort.
     */
    private int runConsole() throws Exception { /* … */ }

    /**
     * Boots {@link ConfigWebServer} on {@link #webPort}, prints the URL, blocks until Ctrl-C.
     * Returns 0 only if the server starts successfully.
     */
    private int runWeb() throws Exception { /* stub in this task; wired fully in Task 6 */ }

    /** Reads version from MANIFEST.MF Implementation-Version. */
    static class VersionProvider implements IVersionProvider {
        @Override public String[] getVersion() { /* … */ }
    }
}
```

- [ ] **Step 1: Implement `YamlConfigTool`** — `runConsole()` fully; `runWeb()` prints "Web mode not yet implemented" and returns 0 (stub wired in Task 6)

- [ ] **Step 2: Build fat JAR and verify `--help`**

```bash
mvn package -pl appbase-java-tools/appbase-config-wizard -am -Dmaven.test.skip=true -P dev-profile,jacoco-no-rules-profile
java -jar appbase-java-tools/appbase-config-wizard/target/appbase-config-wizard-*.jar --help
```
Expected:
```
Usage: appbase-config-wizard [-hV] [--web] [--web-port=<webPort>] [-o=<outputPath>]
Interactively build YAML configuration files for any Appbase service.
  ...
```

- [ ] **Step 3: End-to-end console smoke test** — select Redis config, fill in two fields, verify file

- [ ] **Step 4: Run all existing tests**

```bash
mvn test -pl appbase-java-tools/appbase-config-wizard -P dev-profile,jacoco-no-rules-profile
```
Expected: `ConfigScannerTest` (5), `FieldPrompterTest` (2), `YamlWriterTest` (1) all PASS.

- [ ] **Step 5: Commit** — this is the console-only milestone

```bash
git add appbase-java/appbase-java-tools/
git commit -m "feat(P-014): add YamlConfigTool CLI entry point — console path complete"
```

---

### Task 6: Web backend — `ConfigWebServer`, `ConfigApiResource`, DTOs

**Files:**
- Create: `src/main/java/.../web/ConfigWebServer.java`
- Create: `src/main/java/.../web/ConfigApiResource.java`
- Create: `src/main/java/.../web/dto/ConfigClassDto.java`
- Create: `src/main/java/.../web/dto/ConfigFieldDto.java`
- Create: `src/main/java/.../web/dto/BuildRequestDto.java`
- Create: `src/main/java/.../web/dto/BuildResultDto.java`
- Modify: `src/main/java/.../YamlConfigTool.java` — wire `runWeb()` to `ConfigWebServer.start()`
- Create: `src/test/java/.../web/ConfigApiResourceIT.java`

**Interfaces:**
- Consumes: `ConfigScanner`, `FieldPrompter`, `YamlWriter` (Tasks 2–3); `JettyAppbaseWebServer` from `appbase-bootstrap-jetty`
- Produces: `ConfigWebServer.start(port)` + `.stop()` + `.url()`; REST endpoints at `/api/configs/**`

```java
/**
 * Boots a minimal embedded Jetty server (no auth, no session — localhost tool only).
 *
 * <p>Mounts:
 * <ul>
 *   <li>{@code /api/*} → Jersey {@link ServletContainer} with {@link ConfigApiResource}</li>
 *   <li>{@code /*} → Jetty {@code DefaultServlet} serving {@code /static/web/} from classpath</li>
 * </ul>
 *
 * <p>Uses {@link JettyAppbaseWebServer.Builder} with a minimal {@code AppbaseServices}
 * (core services only — no auth, no external clients).
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public final class ConfigWebServer {

    private final ConfigScanner scanner;
    private final FieldPrompter fieldPrompter;
    private final YamlWriter writer;
    @Nullable private Server jettyServer; // raw Jetty Server for ephemeral-port access in tests
    private int actualPort;

    public ConfigWebServer(
            final ConfigScanner scanner,
            final FieldPrompter fieldPrompter,
            final YamlWriter writer) { /* … */ }

    /**
     * Starts the server on {@code port} (0 = ephemeral; actual port retrievable via {@link #url()}).
     * Blocks until {@link #stop()} is called.
     */
    public void start(final int port) throws Exception { /* … */ }

    public void stop() throws Exception { /* jettyServer.stop() */ }

    /** @return {@code "http://localhost:<actual port>"} */
    public String url() { return "http://localhost:" + actualPort; }
}

/**
 * JAX-RS resource serving the config wizard REST API.
 * All endpoints are unauthenticated.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
@Path("/configs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class ConfigApiResource {

    private final ConfigScanner scanner;
    private final FieldPrompter fieldPrompter;
    private final YamlWriter writer;
    // In-memory cache: className → last BuiltConfig (for /yaml download)
    private final Map<String, BuiltConfig> lastBuilt = new ConcurrentHashMap<>();

    @Inject
    public ConfigApiResource(final ConfigScanner scanner, final FieldPrompter fieldPrompter, final YamlWriter writer) { /* … */ }

    /** GET /api/configs → list all discovered config classes (no field detail) */
    @GET
    public List<ConfigClassDto> listAll() { /* scanner.scan() → map toDto(info, false) */ }

    /** GET /api/configs/search?q=&layer=&module=&tech= */
    @GET @Path("/search")
    public List<ConfigClassDto> search(
            @QueryParam("q") @Nullable String query,
            @QueryParam("layer") @Nullable String layer,
            @QueryParam("module") @Nullable String module,
            @QueryParam("tech") @Nullable String tech) {
        /* scanner.search(q,layer,module,tech) → map toDto(info, false) */
    }

    /** GET /api/configs/{className} — detail with full field list and defaults */
    @GET @Path("/{className}")
    public ConfigClassDto detail(@PathParam("className") final String className) {
        /* find by simpleClassName or FQN; fieldPrompter.introspect() → toDto(info, true) */
        /* 404 when not found */
    }

    /** POST /api/configs/{className}/build — build + validate; body = {fieldValues: {...}} */
    @POST @Path("/{className}/build")
    public BuildResultDto build(
            @PathParam("className") final String className,
            final BuildRequestDto request) {
        /* fieldPrompter.buildFromMap(info, request.fieldValues())
           Validator.validate(built.instance()) → violations list
           if valid: writer.write(built, null) → store in lastBuilt; include YAML in response
           return BuildResultDto(valid, violations, yaml) */
    }

    /** GET /api/configs/{className}/yaml → download last-built YAML as file attachment */
    @GET @Path("/{className}/yaml")
    @Produces("application/x-yaml")
    public Response downloadYaml(@PathParam("className") final String className) {
        /* look up lastBuilt.get(className) → read file → Response.ok(stream).header("Content-Disposition",...) */
        /* 404 if no prior /build call */
    }
}

// DTOs — all records carry @author Navaneeth Mysore G + @author Claude Sonnet 4.6 + @since 1.0.0
public record ConfigClassDto(
    String className, String simpleClassName, String layer, String module,
    List<ConfigFieldDto> fields  // empty when fetched without detail
) {}
public record ConfigFieldDto(
    String name, String typeName, @Nullable String defaultValue, boolean nullable, boolean nested
) {}
public record BuildRequestDto(Map<String, Object> fieldValues) {}
public record BuildResultDto(boolean valid, List<String> violations, @Nullable String yaml) {}
```

- [ ] **Step 1: Write IT**

```java
// ConfigApiResourceIT.java
// Uses @BeforeAll/@AfterAll to start/stop ConfigWebServer on port 0 (ephemeral)
// Uses java.net.http.HttpClient

class ConfigApiResourceIT {

    private static ConfigWebServer server;
    private static HttpClient http;
    private static String base;

    @BeforeAll
    static void startServer() throws Exception {
        var scanner = new ConfigScanner();
        var prompter = new FieldPrompter(new ObjectMapper());
        var writer = new YamlWriter(new YAMLMapper());
        server = new ConfigWebServer(scanner, prompter, writer);
        // Start in background thread; server.url() available immediately after start() binds port
        new Thread(() -> { try { server.start(0); } catch (Exception e) { throw new RuntimeException(e); } }).start();
        // Wait up to 5s for server to bind
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> server.url().contains("localhost:"));
        base = server.url() + "/api/configs";
        http = HttpClient.newHttpClient();
    }

    @AfterAll
    static void stopServer() throws Exception { server.stop(); }

    @Test
    void listAll_returns200AndNonEmptyArray() throws Exception {
        var resp = http.send(HttpRequest.newBuilder(URI.create(base)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().startsWith("["), "Expected JSON array, got: " + resp.body().substring(0, 50));
        assertTrue(resp.body().length() > 10);
    }

    @Test
    void search_byLayer_returnsFilteredResults() throws Exception {
        var resp = http.send(HttpRequest.newBuilder(URI.create(base + "/search?layer=client")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
    }

    @Test
    void detail_redis_returnsFieldList() throws Exception {
        // Find the Redis config's simpleClassName from listAll first
        // GET /api/configs/{simpleClassName} → expect fields array non-empty
    }

    @Test
    void build_redis_returnsValidYaml() throws Exception {
        // POST /api/configs/{className}/build with minimal fieldValues
        // Assert valid=true, yaml non-null, yaml contains "ssl:"
    }

    @Test
    void downloadYaml_afterBuild_returns200WithAttachment() throws Exception {
        // After build test, GET /api/configs/{className}/yaml → 200, Content-Type contains yaml
    }
}
```

Note: add `awaitility` to test scope in the tool POM:
```xml
<dependency><groupId>org.awaitility</groupId><artifactId>awaitility</artifactId><scope>test</scope></dependency>
```
Check if `awaitility` version is in parent depMgmt; add if missing.

- [ ] **Step 2: Run — expect FAIL**

```bash
mvn test -pl appbase-java-tools/appbase-config-wizard -Dtest=ConfigApiResourceIT -P dev-profile,jacoco-no-rules-profile,maven-failsafe-plugin-profile
```

- [ ] **Step 3: Implement `ConfigWebServer`, `ConfigApiResource`, DTOs**

- [ ] **Step 4: Wire `runWeb()` in `YamlConfigTool`**

```java
private int runWeb() throws Exception {
    var server = new ConfigWebServer(new ConfigScanner(), new FieldPrompter(new ObjectMapper()), new YamlWriter(new YAMLMapper()));
    System.out.printf("Starting Appbase Config Wizard web UI on port %d …%n", webPort);
    new Thread(() -> { try { server.start(webPort); } catch (Exception e) { throw new RuntimeException(e); } }).start();
    // brief wait then print URL
    Thread.sleep(1000);
    System.out.println("Open: " + server.url());
    System.out.println("Press Ctrl-C to stop.");
    Thread.currentThread().join(); // block forever; JVM exits on SIGINT
    return 0;
}
```

- [ ] **Step 5: Run IT — expect PASS**

```bash
mvn test -pl appbase-java-tools/appbase-config-wizard -Dtest=ConfigApiResourceIT -P dev-profile,jacoco-no-rules-profile
```
Expected: 5 IT tests pass.

- [ ] **Step 6: Smoke test `--web`**

```bash
java -jar appbase-java-tools/appbase-config-wizard/target/appbase-config-wizard-*.jar --web --web-port 8091 &
curl -s http://localhost:8091/api/configs | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d), 'configs found')"
kill %1
```
Expected: `N configs found` (N > 0)

- [ ] **Step 7: Commit**

```bash
git add appbase-java/appbase-java-tools/
git commit -m "feat(P-014): add ConfigWebServer, ConfigApiResource, DTOs — web backend complete"
```

---

### Task 7: Next.js frontend

**Files:** all under `appbase-java/appbase-java-tools/appbase-config-wizard/frontend/`

- Create: `package.json`
- Create: `next.config.js`
- Create: `tailwind.config.js`
- Create: `tsconfig.json`
- Create: `src/app/globals.css`
- Create: `src/app/layout.tsx`
- Create: `src/app/page.tsx`
- Create: `src/lib/api.ts`
- Create: `src/components/ThemeToggle.tsx`
- Create: `src/components/ConfigBrowser/SearchBar.tsx`
- Create: `src/components/ConfigBrowser/ConfigList.tsx`
- Create: `src/components/ConfigEditor/ConfigTree.tsx`
- Create: `src/components/ConfigEditor/FieldInput.tsx`
- Create: `src/components/ConfigEditor/DefaultBadge.tsx`
- Create: `src/components/ConfigEditor/NullableTag.tsx`
- Create: `src/components/YamlPreview.tsx`
- Create: `src/components/DownloadButton.tsx`

**Interfaces:**
- Consumes: REST API at `/api/configs/**` (Task 6)
- Produces: `npm run build` → `out/` with `index.html`; functional browser UI

```json
// package.json (key fields)
{
  "name": "appbase-config-wizard-web",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "lint": "next lint"
  },
  "dependencies": {
    "next": "^14.2.3",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "next-themes": "^0.3.0"
  },
  "devDependencies": {
    "typescript": "^5.4.5",
    "tailwindcss": "^3.4.3",
    "autoprefixer": "^10.4.19",
    "postcss": "^8.4.38",
    "@types/react": "^18.3.3",
    "@types/react-dom": "^18.3.0",
    "@types/node": "^20.14.0"
  }
}
```

```javascript
// next.config.js
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',        // static export to out/
  trailingSlash: true,     // /page/ → /page/index.html (needed for DefaultServlet)
};
module.exports = nextConfig;
```

```javascript
// tailwind.config.js
/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',                         // next-themes sets class="dark" on <html>
  content: ['./src/**/*.{ts,tsx}'],
  theme: { extend: {} },
  plugins: [],
};
```

```typescript
// src/lib/api.ts — typed client for all REST endpoints
export interface ConfigClassDto {
  className: string; simpleClassName: string; layer: string; module: string;
  fields: ConfigFieldDto[];
}
export interface ConfigFieldDto {
  name: string; typeName: string; defaultValue: string | null; nullable: boolean; nested: boolean;
}
export interface BuildRequestDto { fieldValues: Record<string, unknown>; }
export interface BuildResultDto { valid: boolean; violations: string[]; yaml: string | null; }

const BASE = '/api/configs';
export const api = {
  listAll: (): Promise<ConfigClassDto[]> => fetch(BASE).then(r => r.json()),
  search: (q = '', layer = '', module = '', tech = ''): Promise<ConfigClassDto[]> =>
    fetch(`${BASE}/search?${new URLSearchParams({ q, layer, module, tech })}`).then(r => r.json()),
  detail: (className: string): Promise<ConfigClassDto> =>
    fetch(`${BASE}/${className}`).then(r => r.json()),
  build: (className: string, req: BuildRequestDto): Promise<BuildResultDto> =>
    fetch(`${BASE}/${className}/build`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(req),
    }).then(r => r.json()),
  yamlUrl: (className: string): string => `${BASE}/${className}/yaml`,
};
```

```tsx
// src/app/layout.tsx
// Wraps children in ThemeProvider (next-themes, defaultTheme="system").
// Top nav: left = "Appbase Config Wizard" title + "powered by Appbase" tagline;
//          right = <ThemeToggle /> (fixed via ml-auto, not CSS position:fixed).
// <html suppressHydrationWarning> required by next-themes.
```

```tsx
// src/app/page.tsx
// Client component.
// State: selectedClass (ConfigClassDto | null), fieldValues (Record<string,unknown>), buildResult
// Layout: flex row, 50/50 split.
//   Left: <SearchBar onSearch → api.search() → <ConfigList onSelect → setSelectedClass/>/>
//   Right: selectedClass ?
//            <ConfigTree fields={selectedClass.fields} values={fieldValues} onChange={setFieldValues} depth={0}/>
//            <DownloadButton className={selectedClass.className}/>
//            <YamlPreview values={fieldValues} onBuild={handleBuild} buildResult={buildResult}/>
//          : <p>Select a config class to get started.</p>
```

```tsx
// src/components/ConfigBrowser/SearchBar.tsx
// Props: onResults(results: ConfigClassDto[]) => void
// State: q, layer, module, tech (all string)
// Uses useEffect with 200ms debounce on any filter change → api.search()
// Renders: text input for q; three <select> dropdowns for layer/module/tech;
//          active filters shown as dismissible chips below the input.
```

```tsx
// src/components/ConfigBrowser/ConfigList.tsx
// Props: items: ConfigClassDto[]; selected: ConfigClassDto | null; onSelect
// Renders a scrollable <ul>; each <li> shows simpleClassName + layer badge;
// clicking a row calls onSelect and triggers api.detail() to fetch fields.
```

```tsx
// src/components/ConfigEditor/ConfigTree.tsx
// Props: fields: ConfigFieldDto[]; values: Record<string,unknown>; onChange; depth: number
// For each field:
//   if field.nested → <details><summary>{field.name}</summary><ConfigTree depth={depth+1} .../></details>
//   else → <FieldInput .../> + <DefaultBadge .../> + (field.nullable ? <NullableTag/> : null)
// depth >= 3 and field.nested → show "[use defaults]" button instead of recursing
```

```tsx
// src/components/ConfigEditor/FieldInput.tsx
// Props: field: ConfigFieldDto; value: unknown; onChange(v: unknown): void
// Switch on field.typeName:
//   "boolean" → <input type="checkbox">
//   "int" | "long" → <input type="number" step="1">
//   "String" → <input type="text">
//   contains "[]" or enum → <select> (options from field metadata TBD via future /enum endpoint, fall back to text)
//   nested → nothing (ConfigTree handles recursion above)
```

```tsx
// src/components/YamlPreview.tsx
// Props: values: Record<string,unknown>; onBuild(values): Promise<BuildResultDto>; buildResult: BuildResultDto | null
// Renders:
//   <pre> block showing JSON.stringify(values, null, 2) as live preview (until a build is run)
//   "Generate YAML" button → calls onBuild → shows result.yaml in <pre> or violation list
//   "Copy" button → navigator.clipboard.writeText(yaml)
```

```tsx
// src/components/ThemeToggle.tsx
// Uses useTheme() from next-themes.
// Renders a <button> with sun icon (light mode) or moon icon (dark mode).
// onClick: setTheme(theme === 'dark' ? 'light' : 'dark')
// Icons: plain SVG or Unicode (☀ / ☾) — no icon library dependency needed.
```

```tsx
// src/components/DownloadButton.tsx
// Props: className: string
// Renders <a href={api.yamlUrl(className)} download> wrapping a "Download YAML" button.
// Only shown after a successful build (caller conditionally renders).
```

- [ ] **Step 1: Scaffold Next.js project**

```bash
cd appbase-java/appbase-java-tools/appbase-config-wizard
mkdir -p frontend/src/app frontend/src/lib frontend/src/components/ConfigBrowser frontend/src/components/ConfigEditor
```

Write all config files: `package.json`, `next.config.js`, `tailwind.config.js`, `tsconfig.json`, `src/app/globals.css`.

`globals.css` minimal:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- [ ] **Step 2: Install dependencies**

```bash
cd frontend
npm install
```
Expected: `node_modules/` created; no errors.

- [ ] **Step 3: Create all component + page files** following the stubs above

- [ ] **Step 4: Verify build**

```bash
cd frontend
npm run build
```
Expected: `out/` directory created with `index.html` and `_next/` assets. No TypeScript errors.

- [ ] **Step 5: Manual smoke test with live server**

Start the Java web server (built in Task 6), then open the frontend in dev mode:
```bash
# Terminal 1: Java backend
java -jar ../target/appbase-config-wizard-*.jar --web --web-port 8091 &
# Terminal 2: Next.js dev
npm run dev -- --port 3001   # proxying /api to localhost:8091 is optional for dev; use network tab
```
Verify: page loads, search returns results, theme toggle switches dark/light, clicking a config shows fields.

- [ ] **Step 6: Commit**

```bash
git add appbase-java/appbase-java-tools/appbase-config-wizard/frontend/
git commit -m "feat(P-014): add Next.js frontend (ConfigBrowser, ConfigEditor, YamlPreview, ThemeToggle)"
```

---

### Task 8: Maven build pipeline integration + final verification

This task wires `frontend-maven-plugin` + `maven-resources-plugin` so that `mvn package` on the module produces a fat JAR containing both the Java classes and the compiled Next.js static export.

**Files:**
- Modify: `appbase-java/appbase-java-tools/appbase-config-wizard/pom.xml` — add two plugins
- Modify: `appbase-java/CHANGELOG.adoc`

**Interfaces:**
- Produces: `mvn clean package` → fat JAR includes `/static/web/index.html`; `--web` serves the real frontend from jar

- [ ] **Step 1: Add `frontend-maven-plugin` to tool POM**

In `appbase-java/appbase-java-tools/appbase-config-wizard/pom.xml`, inside `<build><plugins>`:

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.0</version>
    <configuration>
        <workingDirectory>frontend</workingDirectory>
        <nodeVersion>v20.14.0</nodeVersion>
        <npmVersion>10.7.0</npmVersion>
    </configuration>
    <executions>
        <execution>
            <id>install-node-and-npm</id>
            <goals><goal>install-node-and-npm</goal></goals>
            <phase>initialize</phase>
        </execution>
        <execution>
            <id>npm-install</id>
            <goals><goal>npm</goal></goals>
            <phase>initialize</phase>
            <configuration><arguments>install</arguments></configuration>
        </execution>
        <execution>
            <id>npm-build</id>
            <goals><goal>npm</goal></goals>
            <phase>generate-resources</phase>
            <configuration><arguments>run build</arguments></configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 2: Add `maven-resources-plugin` copy execution**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-frontend-build</id>
            <phase>process-resources</phase>
            <goals><goal>copy-resources</goal></goals>
            <configuration>
                <outputDirectory>${project.build.outputDirectory}/static/web</outputDirectory>
                <overwrite>true</overwrite>
                <resources>
                    <resource>
                        <directory>frontend/out</directory>
                        <filtering>false</filtering>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 3: Add `.gitignore` entry for generated Node + build dirs**

In `appbase-java/appbase-java-tools/appbase-config-wizard/`:
```
frontend/node_modules/
frontend/.next/
frontend/out/
```

- [ ] **Step 4: Full module build**

```bash
cd appbase-java
mvn clean package -pl appbase-java-tools/appbase-config-wizard -am -P dev-profile,jacoco-no-rules-profile
```
Expected: Node 20 downloaded (first run), `npm install` runs, `npm run build` runs, Java compiles, shade produces fat JAR. BUILD SUCCESS.

- [ ] **Step 5: Verify frontend is in fat JAR**

```bash
jar tf appbase-java-tools/appbase-config-wizard/target/appbase-config-wizard-*.jar | grep "static/web/index.html"
```
Expected: `static/web/index.html` line printed.

- [ ] **Step 6: End-to-end web test from fat JAR**

```bash
java -jar appbase-java-tools/appbase-config-wizard/target/appbase-config-wizard-*.jar --web --web-port 9999 &
sleep 2
curl -s -o /dev/null -w "%{http_code}" http://localhost:9999/          # expect 200
curl -s -o /dev/null -w "%{http_code}" http://localhost:9999/api/configs  # expect 200
kill %1
```
Expected: `200` on both lines.

- [ ] **Step 7: Full reactor build — no regressions**

```bash
cd appbase-java
./build.sh quick
```
Expected: BUILD SUCCESS; all existing tests pass; new module's tests pass.

- [ ] **Step 8: Create `bin/run-console.sh`**

```bash
#!/usr/bin/env bash
# run-console.sh — builds the fat JAR if absent, then runs the config wizard in console mode.
# Usage: ./bin/run-console.sh [--output /path/to/out.yaml]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
JAR="${MODULE_DIR}/target/appbase-config-wizard-1.0.0-SNAPSHOT.jar"

if [ ! -f "${JAR}" ]; then
    echo "[run-console] JAR not found — building (this may take a minute on first run)..."
    cd "${MODULE_DIR}"
    mvn clean package -am -Dmaven.test.skip=true -P dev-profile,jacoco-no-rules-profile
fi

exec java -jar "${JAR}" "$@"
```

Make executable: `chmod +x bin/run-console.sh`

- [ ] **Step 9: Create `bin/run-web.sh`**

```bash
#!/usr/bin/env bash
# run-web.sh — builds the fat JAR if absent, starts the web server in the background.
# Usage: ./bin/run-web.sh [--web-port 8090]
# The server URL is printed after startup. Stop with ./bin/stop-web.sh.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
JAR="${MODULE_DIR}/target/appbase-config-wizard-1.0.0-SNAPSHOT.jar"
PID_FILE="${MODULE_DIR}/.wizard-web.pid"

if [ -f "${PID_FILE}" ]; then
    EXISTING_PID=$(cat "${PID_FILE}")
    if kill -0 "${EXISTING_PID}" 2>/dev/null; then
        echo "[run-web] Web server already running (PID ${EXISTING_PID})."
        echo "          Stop it first with: ./bin/stop-web.sh"
        exit 1
    else
        echo "[run-web] Removing stale PID file."
        rm -f "${PID_FILE}"
    fi
fi

if [ ! -f "${JAR}" ]; then
    echo "[run-web] JAR not found — building (this may take a minute on first run)..."
    cd "${MODULE_DIR}"
    mvn clean package -am -Dmaven.test.skip=true -P dev-profile,jacoco-no-rules-profile
fi

# Start in background; redirect output to a log file for inspection
LOG="${MODULE_DIR}/.wizard-web.log"
java -jar "${JAR}" --web "$@" > "${LOG}" 2>&1 &
echo $! > "${PID_FILE}"

# Wait up to 10 s for "Open:" line to appear in log (printed by YamlConfigTool.runWeb())
echo "[run-web] Starting..."
for i in $(seq 1 20); do
    sleep 0.5
    if grep -q "^Open:" "${LOG}" 2>/dev/null; then
        grep "^Open:" "${LOG}"
        echo "[run-web] PID $(cat "${PID_FILE}") — stop with: ./bin/stop-web.sh"
        exit 0
    fi
done

echo "[run-web] Server did not print URL within 10s. Check ${LOG} for errors."
exit 1
```

Make executable: `chmod +x bin/run-web.sh`

- [ ] **Step 10: Create `bin/stop-web.sh`**

```bash
#!/usr/bin/env bash
# stop-web.sh — gracefully stops the web server started by run-web.sh.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PID_FILE="${MODULE_DIR}/.wizard-web.pid"

if [ ! -f "${PID_FILE}" ]; then
    echo "[stop-web] No PID file found at ${PID_FILE}."
    echo "           Either the server is not running or was started manually."
    exit 1
fi

PID=$(cat "${PID_FILE}")

if kill -0 "${PID}" 2>/dev/null; then
    kill "${PID}"
    rm -f "${PID_FILE}"
    echo "[stop-web] Stopped web server (PID ${PID})."
else
    echo "[stop-web] Process ${PID} not found — removing stale PID file."
    rm -f "${PID_FILE}"
    exit 1
fi
```

Make executable: `chmod +x bin/stop-web.sh`

- [ ] **Step 11: Create `README.adoc`**

```adoc
= Appbase Config Wizard
:toc:

Powered by https://example.invalid/scratch-rehearsal[Appbase] — demonstrating
embedded Jetty, JAX-RS, and Jackson in a real developer tool.

Interactively builds valid YAML configuration files for any Appbase service.
Pick a config class, fill in values field-by-field, and get a ready-to-use YAML file —
no more guessing field names from source code.

== Prerequisites

* Java 25+ on `$PATH`
* Maven 3.6+ on `$PATH` (only needed for the first run — scripts auto-build the JAR)

== Quick Start

=== Console mode (interactive CLI)

[source,shell]
----
./bin/run-console.sh
----

Optional: specify an output path:

[source,shell]
----
./bin/run-console.sh --output /path/to/my-service.yaml
----

Use `--help` to see all options:

[source,shell]
----
./bin/run-console.sh --help
----

=== Web UI

Start the embedded web server (default port 8090):

[source,shell]
----
./bin/run-web.sh
----

Custom port:

[source,shell]
----
./bin/run-web.sh --web-port 9090
----

The URL is printed after startup, e.g. `Open: http://localhost:8090`.
Open it in any browser to use the visual config builder.

Stop the server:

[source,shell]
----
./bin/stop-web.sh
----

== Running directly from the JAR

After an initial build, the fat JAR can be run directly:

[source,shell]
----
java -jar target/appbase-config-wizard-1.0.0-SNAPSHOT.jar            # console
java -jar target/appbase-config-wizard-1.0.0-SNAPSHOT.jar --web      # web UI
java -jar target/appbase-config-wizard-1.0.0-SNAPSHOT.jar --help     # usage
----

== Manual build

[source,shell]
----
cd appbase-java
mvn clean package -pl appbase-java-tools/appbase-config-wizard -am \
    -Dmaven.test.skip=true -P dev-profile,jacoco-no-rules-profile
----

== Web API

The web server exposes a REST API at `/api`:

[cols="1,2,3"]
|===
| Method | Path | Description

| GET | `/api/configs` | All discovered config classes
| GET | `/api/configs/search?q=&layer=&module=&tech=` | Filtered list (substring match)
| GET | `/api/configs/{className}` | Class detail with fields and defaults
| POST | `/api/configs/{className}/build` | Build + validate; body = `{"fieldValues":{...}}`
| GET | `/api/configs/{className}/yaml` | Download last-built YAML
|===

== Architecture

Built on Appbase's own stack:

* *Classpath scan*: ClassGraph discovers all `AppbaseConfiguration` implementors at runtime
* *Field introspection*: Jackson `BeanDescription` reads property names, types, and defaults
  for both `@JsonCreator` and builder-pattern configs
* *Console view*: Picocli + JLine3 for tab completion, history, and ANSI output
* *Web view*: `JettyAppbaseWebServer` + Jersey 3 REST + Next.js 14 static export (Tailwind + next-themes)
* *Output*: `YAMLMapper` serialises the built object — output is guaranteed valid

== Configuration class evolution

No tool changes are needed when config classes change.
New fields appear in prompts automatically; removed fields disappear.
Requires a rebuild of the fat JAR (`run-console.sh` / `run-web.sh` do this automatically when the JAR is absent).
```

- [ ] **Step 12: Update `CHANGELOG.adoc`**

Add a new entry under `## 1.0.0-SNAPSHOT`:
```
<date> - P-014 appbase-config-wizard: interactive CLI + embedded web UI for building Appbase YAML config files;
  fat JAR with ClassGraph scan, Picocli + JLine3 console, JettyAppbaseWebServer + Jersey REST API,
  Next.js 14 + Tailwind + next-themes static frontend; bin/ scripts with auto-build + PID-based web stop.
```

- [ ] **Step 13: Commit**

```bash
git add appbase-java/appbase-java-tools/ appbase-java/pom.xml appbase-java/CHANGELOG.adoc
git commit -m "build(P-014): wire frontend-maven-plugin + shade; full fat JAR with web UI; add bin/ scripts and README"
```

---

## Test Summary

| Test class | Type | Covers |
|---|---|---|
| `ConfigScannerTest` | Unit | ClassGraph finds configs; search filters by layer/module/query |
| `FieldPrompterTest` | Unit | Jackson introspects Redis config; builder + @JsonCreator patterns; buildFromMap round-trips |
| `YamlWriterTest` | Unit | Written YAML is valid; round-trip back to same instance |
| `ConfigApiResourceIT` | IT (real Jetty) | listAll, search, detail, build, downloadYaml — 5 endpoints; ephemeral port |

Console prompting (`FilterMenu`, `PromptUtil`, `FieldPrompter.prompt()`) is exercised by manual smoke tests in Tasks 4 and 5. Automated terminal I/O simulation is out of scope.

---

## Effort Estimate

| Task | Description | Estimate |
|---|---|---|
| 1 | Module scaffold + POMs | XS (<1h) |
| 2 | ConfigScanner + ConfigClassInfo | S (2–4h) |
| 3 | FieldPrompter (introspect + buildFromMap) + YamlWriter | M (1–2d) |
| 4 | FilterMenu + PromptUtil + FieldPrompter.prompt() | S (4–6h) |
| 5 | YamlConfigTool CLI entry point | S (2–3h) |
| 6 | ConfigWebServer + REST API | S (3–5h) |
| 7 | Next.js frontend | L (2–3d) |
| 8 | Maven pipeline integration | S (2–3h) |
| **Total** | | **L–XL (4–6d)** |

> Console-only milestone: Tasks 1–5 can be merged as a PR independently. Tasks 6–8 are a second PR (web UI layer).
