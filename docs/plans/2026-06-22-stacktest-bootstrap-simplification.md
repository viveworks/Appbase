# Stacktest Bootstrap Simplification Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete `stacktest/AppbaseServices.java` and `StackTestServer.java`; replace with clean bootstrap builder calls so stacktest showcases the bootstrap module instead of duplicating it.

**Architecture:** Fill gaps in bootstrap (ObjectMapper auto-registration, config store hooks, exception store hook, MXBean wiring, servlet extensibility), create factory implementations for all 13 client services and 5 generic app services, simplify stacktest to a linear builder sequence in `StackTestMain`.

**Tech Stack:** Java 25, Maven multi-module, Jetty 11, Jersey 3.1, Micrometer, Jackson 2.17, JDBI3

## Global Constraints

- JDK 25, Maven 3.6+; no SNAPSHOTs except existing project deps
- Build via `./build.sh quick` from `appbase-java/`; tests pass before each commit
- TDD: write failing test first, then implementation
- Checkstyle enforced; Javadoc required on all public methods
- No backwards-compat shims — change callers directly
- Branch: new issue + branch per the project's branching convention
- PR targeting `develop`; await human merge before post-merge steps

---

## Phase 1 — Bootstrap: auto ObjectMapper subtype registration

### Task 1.1: `AppbaseCoreServices.Builder.fromYaml()` calls `AppbaseCoreObjectMapperSubTypes.register()`

**Files:**
- Modify: `appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/core/AppbaseCoreServices.java`
- Modify: `appbase-java/appbase-bootstrap/src/test/java/com/viveworks/appbase/bootstrap/AppbaseBootstrapIntegrationTest.java`

**Interfaces:**
- Consumes: `com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes.register()`
- Produces: `AppbaseCoreServices.Builder.fromYaml(String)` auto-calls `AppbaseCoreObjectMapperSubTypes.register()`

- [ ] **Step 1: Write failing test**

  In `AppbaseBootstrapIntegrationTest`, assert that `AppbaseCoreObjectMapperSubTypes` has been registered after building via `fromYaml()`. Use `ObjectMapper.getRegisteredModuleIds()` or attempt to deserialize a known subtype to verify.

- [ ] **Step 2: Run test — verify FAIL**

  Run: `mvn test -pl appbase-bootstrap -Dtest=AppbaseBootstrapIntegrationTest -P dev-profile`

- [ ] **Step 3: Implement**

  At the top of `AppbaseCoreServices.Builder.fromYaml(String path)`, add:
  ```java
  AppbaseCoreObjectMapperSubTypes.register();
  ```
  Add import for `com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes`.

- [ ] **Step 4: Run test — verify PASS**

- [ ] **Step 5: Commit**

  ```bash
  git add appbase-java/appbase-bootstrap/...
  git commit -m "feat(bootstrap): auto-register AppbaseCoreObjectMapperSubTypes in fromYaml()"
  ```

---

### Task 1.2: `AppbaseClientServices.Builder.build()` calls `AppbaseClientObjectMapperSubTypes.register()`

**Files:**
- Modify: `appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/client/AppbaseClientServices.java`
- Modify: `appbase-java/appbase-bootstrap/src/test/java/com/viveworks/appbase/bootstrap/AppbaseBootstrapIntegrationTest.java`

**Interfaces:**
- Consumes: `com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes.register()`
- Produces: `AppbaseClientServices.Builder.build()` auto-calls `AppbaseClientObjectMapperSubTypes.register()`

- [ ] **Step 1: Write failing test**

  Assert that after `AppbaseClientServices.builder().from(coreServices).build()`, client ObjectMapper subtypes are registered (e.g., attempt to deserialize a known client config subtype).

- [ ] **Step 2: Run test — verify FAIL**

- [ ] **Step 3: Implement**

  At the top of `AppbaseClientServices.Builder.build()`, add:
  ```java
  AppbaseClientObjectMapperSubTypes.register();
  ```

- [ ] **Step 4: Run test — verify PASS**

- [ ] **Step 5: Commit**

  ```bash
  git commit -m "feat(bootstrap): auto-register AppbaseClientObjectMapperSubTypes in build()"
  ```

---

## Phase 2 — Bootstrap: client service configuration registration hook

### Task 2.1: Add `registerConfiguration()` to `AppbaseClientServiceFactory`

**Files:**
- Modify: `appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/client/AppbaseClientServiceFactory.java`
- Modify: `appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/client/AppbaseClientServices.java`

**Interfaces:**
- Produces:
  ```java
  // New default method on AppbaseClientServiceFactory
  default void registerConfiguration(AppbaseCoreServices coreServices) {}
  ```
  `AppbaseClientServices.Builder.build()` calls `factory.registerConfiguration(core)` for each factory in `initializationOrder` order, then calls `core.getConfigurationService().load()` once after all registrations, before running any `factory.create()`.

- [ ] **Step 1: Write failing test**

  Create a test `AppbaseClientServiceFactory` implementation that records whether `registerConfiguration()` was called. Assert it is called exactly once during `build()`. Also assert `configurationService.load()` is called after all `registerConfiguration()` calls (verify via mock or spy).

- [ ] **Step 2: Run test — verify FAIL**

- [ ] **Step 3: Implement**

  In `AppbaseClientServiceFactory`:
  ```java
  /**
   * Called by the builder before {@link #create} to register configuration types and stores
   * needed by this factory. Override to register classpath stores and config type mappings.
   * The builder calls {@link ConfigurationService#load()} once after all factories have
   * registered.
   */
  default void registerConfiguration(AppbaseCoreServices coreServices) {}
  ```

  In `AppbaseClientServices.Builder.build()`, before the factory create loop, add:
  ```java
  // Phase 1: register all configuration
  for (AppbaseClientServiceFactory<?> factory : sorted) {
      factory.registerConfiguration(core);
  }
  core.getConfigurationService().load();

  // Phase 2: create services (existing loop)
  ```

- [ ] **Step 4: Run test — verify PASS**

- [ ] **Step 5: Build full bootstrap + stacktest**

  ```bash
  mvn clean install -pl appbase-bootstrap -P dev-profile,jacoco-no-rules-profile
  mvn clean install -pl appbase-stacktest -P dev-profile,jacoco-no-rules-profile
  ```
  Both must compile and pass tests.

- [ ] **Step 6: Commit**

  ```bash
  git commit -m "feat(bootstrap): add registerConfiguration() hook to AppbaseClientServiceFactory"
  ```

---

## Phase 3 — Bootstrap: exception store default + MXBean wiring

### Task 3.1: `AppbaseCoreServices.Builder.withDefaultExceptionStores()`

**Files:**
- Modify: `appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/core/AppbaseCoreServices.java`
- Modify: `appbase-java/appbase-bootstrap/src/test/java/com/viveworks/appbase/bootstrap/AppbaseBootstrapIntegrationTest.java`

**Interfaces:**
- Produces:
  ```java
  public Builder withDefaultExceptionStores()
  ```
  When called, `build()` registers an `InMemoryExceptionStore` via `exceptionService.getExceptionAccessor().registerExceptionStore(...)`.

- [ ] **Step 1: Write failing test**

  Assert that after `.withDefaultExceptionStores().build()`, `appbaseCore.getExceptionService().getExceptionAccessor()` has at least one registered store. Without the call, assert zero stores.

- [ ] **Step 2: Run test — verify FAIL**

- [ ] **Step 3: Implement**

  Add flag `private boolean registerDefaultExceptionStores = false;` to `Builder`. Add builder method:
  ```java
  public Builder withDefaultExceptionStores() {
      this.registerDefaultExceptionStores = true;
      return this;
  }
  ```
  In `build()`, after core is constructed:
  ```java
  if (registerDefaultExceptionStores) {
      InMemoryExceptionStore store = new InMemoryExceptionStore(
          InMemoryExceptionStoreConfiguration.builder().build());
      appbaseCore.getExceptionService()
          .getExceptionAccessor()
          .registerExceptionStore(store);
  }
  ```

- [ ] **Step 4: Run test — verify PASS**

- [ ] **Step 5: Commit**

  ```bash
  git commit -m "feat(bootstrap): add withDefaultExceptionStores() to AppbaseCoreServices.Builder"
  ```

---

### Task 3.2: `JettyAppbaseServer.Builder` extensibility — MXBeans, handlers, servlet context

**Files:**
- Modify: `appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/server/JettyAppbaseServer.java`
- Modify: `appbase-java/appbase-bootstrap/src/test/java/com/viveworks/appbase/bootstrap/server/JettyAppbaseServerTest.java` (or create)

**Interfaces:**
- Produces three new builder methods:
  ```java
  public Builder withDefaultMXBeans()
  public Builder withAdditionalHandler(org.eclipse.jetty.server.handler.AbstractHandler handler)
  public Builder withServletContext(java.util.function.Consumer<org.eclipse.jetty.servlet.ServletContextHandler> customizer)
  ```

  `withDefaultMXBeans()`: registers JmxConfigurationService, JmxExceptionService, JmxLogService, JmxMediatorService, JmxMetricService, JmxServiceRegistry, JmxJmxService, JmxAppbaseCore — same 8 beans as `stacktest/AppbaseServices.registerMXBeans()`.

  `withAdditionalHandler(handler)`: accumulated in a list; in `build()`, all handlers + main `ServletContextHandler` wrapped in `HandlerCollection`.

  `withServletContext(customizer)`: accumulated in a list; in `build()`, the main `ServletContextHandler` is passed to each customizer after it is created but before it is added to the server.

- [ ] **Step 1: Write failing tests — one per new method**

  - Test that `withDefaultMXBeans()` results in `jmxService.isMXBeanRegistered(JmxLogServiceMXBean.class)` returning `true` after `build()`.
  - Test that `withAdditionalHandler(mockHandler)` results in that handler being in the server's handler chain.
  - Test that `withServletContext(ctx -> ctx.setAttribute("key", "val"))` results in the attribute being set on the server's servlet context.

- [ ] **Step 2: Run tests — verify FAIL**

- [ ] **Step 3: Implement**

  Add to `Builder`:
  ```java
  private boolean registerDefaultMXBeans = false;
  private final List<AbstractHandler> additionalHandlers = new ArrayList<>();
  private final List<Consumer<ServletContextHandler>> servletContextCustomizers = new ArrayList<>();

  public Builder withDefaultMXBeans() { this.registerDefaultMXBeans = true; return this; }
  public Builder withAdditionalHandler(AbstractHandler h) { additionalHandlers.add(h); return this; }
  public Builder withServletContext(Consumer<ServletContextHandler> c) { servletContextCustomizers.add(c); return this; }
  ```

  In `build()`:
  ```java
  // After server and connector are set up:
  if (registerDefaultMXBeans) {
      registerDefaultMXBeans(appbaseServices.getAppbaseCore());
  }

  final ServletContextHandler servletContextHandler = new ServletContextHandler(...);
  servletContextCustomizers.forEach(c -> c.accept(servletContextHandler));

  if (additionalHandlers.isEmpty()) {
      server.setHandler(servletContextHandler);
  } else {
      HandlerCollection collection = new HandlerCollection();
      additionalHandlers.forEach(collection::addHandler);
      collection.addHandler(servletContextHandler);
      server.setHandler(collection);
  }
  ```

  Extract `registerDefaultMXBeans(AppbaseCore)` as a private static method, copied from `stacktest/AppbaseServices.registerMXBeans()`.

- [ ] **Step 4: Run tests — verify PASS**

- [ ] **Step 5: Full build**

  ```bash
  mvn clean install -pl appbase-bootstrap -P dev-profile,jacoco-no-rules-profile
  ```

- [ ] **Step 6: Commit**

  ```bash
  git commit -m "feat(bootstrap): add withDefaultMXBeans/withAdditionalHandler/withServletContext to JettyAppbaseServer"
  ```

---

## Phase 4 — New client service factory implementations (in `appbase-bootstrap`)

One task per factory. Each factory follows the same pattern:

```java
public final class XxxClientServiceFactory implements AppbaseClientServiceFactory<XxxClientService> {
    private final String configClasspathFile;

    public XxxClientServiceFactory(String configClasspathFile) {
        this.configClasspathFile = Objects.requireNonNull(configClasspathFile);
    }

    @Override public Class<XxxClientService> serviceType() { return XxxClientService.class; }
    @Override public int initializationOrder() { return /* ordering constant */; }

    @Override
    public void registerConfiguration(AppbaseCoreServices coreServices) {
        DefaultXxxClientService.registerObjectMapperSubtypes();
        ConfigurationAccessor accessor = coreServices.getConfigurationService().getConfigurationAccessor();
        accessor.registerConfigurationTypeByRawConfigurationName(
            DefaultXxxClientServiceConfiguration.class, configClasspathFile);
        accessor.registerConfigurationStore(
            new ClasspathConfigurationStore(configClasspathFile, MimeType.YAML));
    }

    @Override
    public XxxClientService create(AppbaseCoreServices core, AppbaseClientCore clientCore) {
        return new DefaultXxxClientService(clientCore);
    }
}
```

### Task 4.1–4.13: Create one factory per client service

**Factories to create** (all in `appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/client/factory/`):

| Task | Class | Notes |
|---|---|---|
| 4.1 | `AerospikeClientServiceFactory` | Single config file |
| 4.2 | `DruidSqlClientServiceFactory` | Two config files: shared-resources + service |
| 4.3 | `ElasticsearchLowLevelRestClientServiceFactory` | Single config file |
| 4.4 | `ElasticsearchRestClientFactory` | No config; wraps low-level service from `clientServices` |
| 4.5 | `AwsSdkHttpClientServiceFactory` | Single config file |
| 4.6 | `AwsSdkAsyncHttpClientServiceFactory` | Single config file |
| 4.7 | `S3ClientsServiceFactory` | Config file; depends on AWS HTTP + Async services (takes them as constructor args or retrieves via `AppbaseClientServices` during `create`) |
| 4.8 | `VitessJdbcClientServiceFactory` | Single config file |
| 4.9 | `VitessJdbiClientServiceFactory` | Config file; depends on `JdbcClientService` — retrieve from partial services map during `create` |
| 4.10 | `VitessGrpcClientServiceFactory` | Single config file |
| 4.11 | `ZooKeeperClientServiceFactory` | Single config file |
| 4.12 | `FirebaseAppClientServiceFactory` | Config file; sets system property for test helper |
| 4.13 | `FirebaseAuthClientServiceFactory` | Config file; depends on FirebaseApp |

For each factory:

- [ ] **Write unit test**: instantiate factory, call `registerConfiguration()` on a mock `AppbaseCoreServices`, assert correct config type and store registered; call `create()`, assert non-null service returned.
- [ ] **Implement** factory class.
- [ ] **Run test — verify PASS**
- [ ] **Commit** (can batch multiple factories per commit if logically grouped, e.g., AWS factories together)

---

## Phase 5 — New generic app service factory implementations (in `appbase-bootstrap`)

All in `appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/service/factory/`.

These implement `AppbaseServiceFactory<T>` (the existing interface on `bootstrap/service/AppbaseServices.Builder`).

### Task 5.1: `HealthCheckServiceFactory`

```java
public HealthCheckService create(bootstrap.service.AppbaseServices partial) {
    AppbaseCore core = partial.getAppbaseCore();
    return new DefaultHealthCheckService(
        core.getLogService(), core.getMetricService(),
        core.getMediatorService(), core.getServiceRegistry(),
        DefaultHealthCheckServiceConfiguration.builder().build());
}
```

Also registers in `ServiceRegistry`: `appbaseCore.getServiceRegistry().registerAppbaseService(HealthCheckService.class, service)`.

### Task 5.2: `IdServiceFactory`

Wraps `DefaultIdService` with default config.

### Task 5.3: `PasswordServiceFactory`

Wraps `DefaultPasswordService` with default config.

### Task 5.4: `StaticClusterServiceFactory`

Wraps `StaticClusterService` with default config.

### Task 5.5: `AuthorizationServiceFactory`

Constructor takes `ContextAuthorizer`:
```java
public AuthorizationServiceFactory(ContextAuthorizer contextAuthorizer) { ... }
```
Wraps `DefaultAuthorizationService`.

For each factory:

- [ ] **Write unit test**: mock `AppbaseServices partial`, call `create()`, assert correct service type returned.
- [ ] **Implement** factory class.
- [ ] **Run test — verify PASS**
- [ ] **Commit**

---

## Phase 6 — Simplify appbase-stacktest

### Task 6.1: Update `StackTestServices` to use bootstrap's `AppbaseServices`

**Files:**
- Modify: `appbase-java/appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/service/StackTestServices.java`

Currently `StackTestServices` takes stacktest's `AppbaseServices`. Change it to accept `bootstrap.service.AppbaseServices` + `AppbaseClientServices` separately (since stacktest domain services need direct client service access for JDBI handles etc.).

- [ ] **Read** `StackTestServices.java` to understand current getters.
- [ ] **Write test**: verify `StackTestServices` getters return correct values after construction with bootstrap services.
- [ ] **Implement** constructor change and update all getter delegation.
- [ ] **Run tests — PASS**
- [ ] **Commit**

---

### Task 6.2: Create stacktest-specific app service factories

**Files:**
- Create: `appbase-java/appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/factory/`

These implement `AppbaseServiceFactory<T>` (from bootstrap) but live in stacktest because they contain domain logic.

| Factory | Dependencies passed via constructor |
|---|---|
| `UserSessionStoreFactory` | `AerospikeClientService` |
| `UserAccountStoreFactory` | `PasswordService`, `JdbiClientService` (for handle management) |
| `FirebaseAuthServiceFactory` | Firebase config properties (system property names for test helper) |
| `UserAccountServiceFactory` | Requires other services from partial — reads them via `partial.get(XxxService.class)` |
| `SupportMessageServiceFactory` | `IdService`, `JdbiClientService` |

**JDBI Handle lifecycle note**: `UserAccountStoreFactory.create()` opens a `Handle` from the `JdbiClientService`. The factory registers a JVM shutdown hook to close it (matching current behavior). The `Handle` reference stays inside the factory instance.

- [ ] **Write tests** for each factory's `create()` method.
- [ ] **Implement** factory classes.
- [ ] **Run tests — PASS**
- [ ] **Commit**

---

### Task 6.3: Rewrite `StackTestMain` startup sequence

**Files:**
- Modify: `appbase-java/appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/StackTestMain.java`

Replace the current:
```java
final AppbaseServices localAppbaseServices = new AppbaseServices();
stackTestServices = new StackTestServices(localAppbaseServices);
stackTestServer = new StackTestServer(stackTestServices);
```

With the full bootstrap builder sequence (see target end-state in overview). The `withServletContext()` lambda contains the servlet/filter registration logic moved from `StackTestServer`.

- [ ] **Write test**: `StackTestMain.main()` without external infra should fail at infra check, not at bootstrap construction. Assert no `NullPointerException` thrown during service wiring phase.
- [ ] **Implement** new `StackTestMain.main()` body.
- [ ] **Run existing full test suite** (compile + test):
  ```bash
  mvn clean install -pl appbase-bootstrap -P dev-profile,jacoco-no-rules-profile
  mvn clean install -pl appbase-stacktest -P dev-profile,jacoco-no-rules-profile
  ```
- [ ] **Commit**

---

### Task 6.4: Delete `stacktest/AppbaseServices.java` and `StackTestServer.java`

- [ ] Verify no remaining references to `com.viveworks.appbase.stacktest.AppbaseServices` in the codebase.
  ```bash
  grep -r "stacktest.AppbaseServices" appbase-java --include="*.java"
  ```
- [ ] Verify no remaining references to `StackTestServer`.
  ```bash
  grep -r "StackTestServer" appbase-java --include="*.java"
  ```
- [ ] Delete both files.
- [ ] Build:
  ```bash
  mvn clean install -pl appbase-bootstrap,appbase-stacktest -P dev-profile,jacoco-no-rules-profile
  ```
- [ ] **Commit**

  ```bash
  git commit -m "refactor(stacktest): delete AppbaseServices.java and StackTestServer.java — replaced by bootstrap builder"
  ```

---

## Phase 7 — Final verification and PR

- [ ] Full build from root:
  ```bash
  ./build.sh quick
  ```
  All tests pass.

- [ ] Create GitHub issue for this refactor, create branch per convention.
- [ ] Create PR targeting `develop`.
- [ ] Run `code-review` plugin on the PR; fix any confirmed/plausible findings.
- [ ] Await human review and merge.

---

## File Change Summary

### New files (bootstrap)
- `appbase-bootstrap/.../client/factory/AerospikeClientServiceFactory.java`
- `appbase-bootstrap/.../client/factory/DruidSqlClientServiceFactory.java`
- `appbase-bootstrap/.../client/factory/ElasticsearchLowLevelRestClientServiceFactory.java`
- `appbase-bootstrap/.../client/factory/ElasticsearchRestClientFactory.java`
- `appbase-bootstrap/.../client/factory/AwsSdkHttpClientServiceFactory.java`
- `appbase-bootstrap/.../client/factory/AwsSdkAsyncHttpClientServiceFactory.java`
- `appbase-bootstrap/.../client/factory/S3ClientsServiceFactory.java`
- `appbase-bootstrap/.../client/factory/VitessJdbcClientServiceFactory.java`
- `appbase-bootstrap/.../client/factory/VitessJdbiClientServiceFactory.java`
- `appbase-bootstrap/.../client/factory/VitessGrpcClientServiceFactory.java`
- `appbase-bootstrap/.../client/factory/ZooKeeperClientServiceFactory.java`
- `appbase-bootstrap/.../client/factory/FirebaseAppClientServiceFactory.java`
- `appbase-bootstrap/.../client/factory/FirebaseAuthClientServiceFactory.java`
- `appbase-bootstrap/.../service/factory/HealthCheckServiceFactory.java`
- `appbase-bootstrap/.../service/factory/IdServiceFactory.java`
- `appbase-bootstrap/.../service/factory/PasswordServiceFactory.java`
- `appbase-bootstrap/.../service/factory/StaticClusterServiceFactory.java`
- `appbase-bootstrap/.../service/factory/AuthorizationServiceFactory.java`

### New files (stacktest)
- `appbase-stacktest/.../factory/UserSessionStoreFactory.java`
- `appbase-stacktest/.../factory/UserAccountStoreFactory.java`
- `appbase-stacktest/.../factory/FirebaseAuthServiceFactory.java`
- `appbase-stacktest/.../factory/UserAccountServiceFactory.java`
- `appbase-stacktest/.../factory/SupportMessageServiceFactory.java`

### Modified files (bootstrap)
- `AppbaseCoreServices.java` — `fromYaml()` auto-registers core subtypes; `.withDefaultExceptionStores()`
- `AppbaseClientServiceFactory.java` — add `registerConfiguration()` default method
- `AppbaseClientServices.java` — call `AppbaseClientObjectMapperSubTypes.register()`, invoke `registerConfiguration()`, call `configService.load()` in `build()`
- `JettyAppbaseServer.java` — `.withDefaultMXBeans()`, `.withAdditionalHandler()`, `.withServletContext()`

### Modified files (stacktest)
- `StackTestServices.java` — accept bootstrap's `AppbaseServices` + `AppbaseClientServices`
- `StackTestMain.java` — full bootstrap builder sequence

### Deleted files
- `stacktest/AppbaseServices.java`
- `StackTestServer.java`
