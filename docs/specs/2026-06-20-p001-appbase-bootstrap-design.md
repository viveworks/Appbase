# P-001 Design Spec — appbase-bootstrap

**Date:** 2026-06-20  
**Status:** Draft — awaiting user approval  
**Ticket:** P-001 · Bootstrap Appbase services and web server startup in an easy & elegant way  
**Scope:** New `appbase-bootstrap` Maven module + stacktest rewrite

---

## 1. Problem

Bootstrapping an Appbase server currently requires ~400 lines of boilerplate in `AppbaseServices.java`
and `StackTestServer.java` (in `appbase-stacktest`). There is no reusable, public API for bootstrapping
the framework. Every adopter must copy-paste the stacktest pattern.

---

## 2. Goals

- Provide a clean, minimal API to bootstrap all of Appbase with sensible defaults
- Support two initialization styles: **Builder** (programmatic / YAML) and **Guice modules**
- Be extensible: adding new client or service types requires zero changes to the bootstrap classes
- Be layered: callers can bootstrap only the layers they need (core-only, core+clients, full stack with Jetty)
- Rewrite `appbase-stacktest` to use the new API as the validation gate

---

## 3. Module Structure

### `appbase-core` — unchanged

`appbase-core` is **not modified**. Existing `AppbaseCoreBootstrappedServices` and all related
classes stay as-is. `appbase-bootstrap` wraps them via delegation.

### New module: `appbase-bootstrap`

```
appbase-java/
  appbase-bootstrap/
    pom.xml
    src/main/java/com/viveworks/appbase/bootstrap/
      core/
        AppbaseCoreServices.java            # immutable wrapper — delegates to AppbaseCoreBootstrappedServices
        AppbaseCoreServiceFactory.java      # factory interface for optional core services
      client/
        AppbaseClientServices.java          # immutable container
        AppbaseClientServiceFactory.java    # factory interface
      service/
        AppbaseServices.java                # immutable container
        AppbaseServiceFactory.java          # factory interface
      server/
        JettyAppbaseServer.java             # server lifecycle
      guice/
        AppbaseCoreServicesModule.java
        AppbaseClientServicesModule.java
        AppbaseServicesModule.java
        JettyAppbaseServerModule.java
    src/test/java/com/viveworks/appbase/bootstrap/
      core/
        AppbaseCoreServicesBuilderTest.java
      client/
        AppbaseClientServicesBuilderTest.java
        AppbaseClientServicesFactoryOrderTest.java
      service/
        AppbaseServicesBuilderTest.java
      server/
        JettyAppbaseServerBuilderTest.java
      guice/
        AppbaseCoreServicesModuleTest.java
        AppbaseClientServicesModuleTest.java
        AppbaseServicesModuleTest.java
      integration/
        AppbaseBootstrapIntegrationTest.java  # real core bootstrap, no external infra
```

### Maven dependency chain

```
appbase-core → appbase-client → appbase-service → appbase-bootstrap → appbase-stacktest
```

`appbase-stacktest` depends on `appbase-bootstrap` only (not directly on the three lower modules).

### Dependencies in `appbase-bootstrap/pom.xml`

```xml
<dependency>appbase-core</dependency>
<dependency>appbase-client</dependency>
<dependency>appbase-service</dependency>
<dependency>com.google.inject:guice</dependency>   <!-- already transitive; explicit for module classes -->
```

OSGi bundle headers follow existing module pattern via `maven-bundle-plugin`.

---

## 4. Core Design Principle: Factory Pattern

Instead of explicit `withAerospike()`, `withElasticsearch()` methods (which require builder edits for every new service), all three bootstrap layers accept **factory objects**. The same extensibility pattern applies uniformly across core, client, and service layers.

```java
// Optional core services (e.g., LuceneService). Called AFTER mandatory core services
// are initialized; receives the complete AppbaseCoreServices as context.
public interface AppbaseCoreServiceFactory<T extends AppbaseService> {
    Class<T> serviceType();
    int initializationOrder();
    T create(AppbaseCoreServices coreServices) throws AppbaseException;
}

public interface AppbaseClientServiceFactory<T extends AppbaseService> {
    Class<T> serviceType();        // map key
    int initializationOrder();     // lower = initialized first
    T create(AppbaseCoreServices coreServices) throws AppbaseException;
}

public interface AppbaseServiceFactory<T extends AppbaseService> {
    Class<T> serviceType();
    int initializationOrder();
    T create(AppbaseCoreServices core,
             AppbaseClientServices clients) throws AppbaseException;
}
```

**Adding a new service at any layer** = write a new `XxxFactory`. Zero builder changes.

Built-in factories ship in `appbase-bootstrap` for all existing core, client, and service types.
`appbase-core` is **not modified** — `AppbaseCoreBootstrappedServices` is wrapped by the new
`AppbaseCoreServices` in `appbase-bootstrap` via delegation.

---

## 5. AppbaseCoreServices (new in appbase-bootstrap)

`AppbaseCoreServices` is a **new immutable wrapper** in `appbase-bootstrap` that delegates to the
existing `AppbaseCoreBootstrappedServices` from `appbase-core`. `appbase-core` is not modified.
All callers in `appbase-bootstrap` use `AppbaseCoreServices`; existing callers of
`AppbaseCoreBootstrappedServices` continue to work unchanged.

### Container

```java
// com.viveworks.appbase.bootstrap.core.AppbaseCoreServices
@Immutable
public final class AppbaseCoreServices implements Shutdownable {

    // Type-safe heterogeneous container (Effective Java Item 33)
    // Returns T or throws AppbaseRuntimeException("XxxService not initialized —
    //   add XxxCoreServiceFactory to AppbaseCoreServices.Builder")
    public <T extends AppbaseService> T get(Class<T> serviceType);

    // Unmodifiable view of all services — mandatory core + optional factory-registered
    public Map<Class<?>, AppbaseService> services();

    // Shuts down all services in REVERSE initialization order
    @Override
    public void shutdown();

    public static Builder builder() { ... }
}
```

All mandatory core services (`LogService`, `MetricService`, `ValidationService`, `MediatorService`,
`ConfigurationService`, `MicroprofileConfigurationService`, `ServiceRegistry`,
`MicroprofileServiceRegistry`, `ExceptionService`, `TracerService`, `ReactorService`, `JmxService`,
optional `LuceneService`) are populated into the map by the Builder from
`AppbaseCoreBootstrappedServices` and accessible via `get(LogService.class)` etc.

### Builder

```java
public static final class Builder {

    // Option A: wrap an already-bootstrapped core (caller controls bootstrap lifecycle)
    public Builder from(AppbaseCoreBootstrappedServices bootstrappedServices);

    // Option B: run DefaultAppbaseCoreBootstrap internally from YAML
    public Builder fromYaml(String classpathOrFilePath);

    // Optional additional core service factories (registered after mandatory core is ready)
    public Builder with(AppbaseCoreServiceFactory<?>... factories);

    public AppbaseCoreServices build();
}
```

Built-in factory: `LuceneServiceFactory` (registers the optional `LuceneService` from
`AppbaseCoreBootstrappedServices` into the map when present).

---

## 6. AppbaseClientServices

### Container

```java
@Immutable
public final class AppbaseClientServices implements Shutdownable {

    // Type-safe heterogeneous container (Effective Java Item 33)
    // Returns T or throws AppbaseRuntimeException("XxxService not initialized —
    //   add XxxClientServiceFactory to AppbaseClientServices.Builder")
    public <T extends AppbaseService> T get(Class<T> serviceType);

    // Unmodifiable view — for health checks, shutdown iteration
    public Map<Class<?>, AppbaseService> services();

    // Shuts down all services in REVERSE initialization order
    @Override
    public void shutdown();

    public static Builder builder() { ... }
}
```

### Builder

```java
public static final class Builder {

    // REQUIRED — core must already be bootstrapped
    public Builder coreServices(AppbaseCoreServices coreServices);

    // Option A: register one or more factories (programmatic config in factory constructor)
    public Builder with(AppbaseClientServiceFactory<?>... factories);

    // Option B: single YAML file — builder reads it and auto-instantiates
    //   the matching built-in factory for each present section.
    //   Absent YAML section = service not initialized (opt-in).
    public Builder fromYaml(String classpathOrFilePath);

    // Options A and B can be combined: YAML for most, programmatic override for one
    public AppbaseClientServices build();
}
```

### YAML schema (single file, all sections optional)

```yaml
aerospike:
  hosts: [...]

cassandra:
  contactPoints: [...]
  datacenter: datacenter1

clamav:
  host: localhost
  port: 3310

elasticsearch:
  hosts: [...]
  scheme: http

firebase:
  app:
    projectId: my-project
  auth:
    # auth config

jasync:
  host: localhost
  port: 3306
  database: mydb

jpa:
  persistenceUnit: appbase

kafka:
  producer:
    bootstrapServers: kafka:9092
  consumer:
    bootstrapServers: kafka:9092
    groupId: my-group

s3:
  region: us-east-1

smtp:
  host: smtp.example.com
  port: 587

spark:
  remote: sc://localhost:15002

vitess:
  jdbc:
    url: jdbc:vitess://...
  jdbi:
    poolSize: 10
  grpc:
    target: localhost:15991

druid:
  url: http://druid:8082

zookeeper:
  connectString: zk:2181

aws:
  httpClient:
    maxConnections: 50
  asyncHttpClient:
    maxConcurrency: 100
```

### Built-in factories (initialization order)

| Order | Factory | Service type |
|-------|---------|-------------|
| 10 | `AwsSdkHttpClientServiceFactory` | `AwsSdkHttpClientService` |
| 11 | `AwsSdkAsyncHttpClientServiceFactory` | `AwsSdkAsyncHttpClientService` |
| 20 | `S3ClientsServiceFactory` | `S3ClientsService` |
| 30 | `FirebaseAppClientServiceFactory` | `FirebaseAppClientService` |
| 31 | `FirebaseAuthClientServiceFactory` | `FirebaseAuthClientService` |
| 40 | `AerospikeClientServiceFactory` | `AerospikeClientService` |
| 41 | `CassandraClientServiceFactory` | `CassandraClientService` |
| 42 | `ClamAVClientServiceFactory` | `ClamAVClientService` |
| 43 | `JasyncClientServiceFactory` | `JasyncClientService` |
| 50 | `ElasticsearchClientServiceFactory` | `ElasticsearchLowLevelRestClientService` |
| 60 | `VitessJdbcClientServiceFactory` | `JdbcClientService` |
| 61 | `VitessJdbiClientServiceFactory` | `JdbiClientService` |
| 62 | `VitessGrpcClientServiceFactory` | `VitessGrpcClientService` |
| 63 | `JpaClientServiceFactory` | `JpaClientService` |
| 70 | `DruidSqlClientServiceFactory` | `DruidSqlClientService` |
| 80 | `ZooKeeperClientServiceFactory` | `ZooKeeperClientService` |
| 85 | `KafkaProducerClientServiceFactory` | `KafkaProducerClientService` |
| 86 | `KafkaConsumerClientServiceFactory` | `KafkaConsumerClientService` |
| 90 | `SmtpClientServiceFactory` | `SmtpClientService` |
| 95 | `SparkClientServiceFactory` | `SparkClientService` |

> **Note on `S3ClientsService`:** Verify this interface name during implementation — the discovery
> scan found 20 `AppbaseClientService` subtypes but did not explicitly list S3. If the interface
> has a different name (e.g., `AwsS3ClientService`), update accordingly.

**Enforcement:** `build()` throws `AppbaseRuntimeException` if:
- `VitessJdbiClientServiceFactory` registered without `VitessJdbcClientServiceFactory`
- `FirebaseAuthClientServiceFactory` registered without `FirebaseAppClientServiceFactory`
- `S3ClientsServiceFactory` registered without `AwsSdkHttpClientServiceFactory`
- `JpaClientServiceFactory` registered without `VitessJdbcClientServiceFactory` (JPA wraps JDBC)

### shutdown()

Calls `service.shutdown()` in **reverse** initialization order. Errors are logged, not rethrown
(same pattern as `DefaultAppbaseCore.shutdown()`).

---

## 7. AppbaseServices

### Container — same pattern as AppbaseClientServices

```java
@Immutable
public final class AppbaseServices implements Shutdownable {
    public <T extends AppbaseService> T get(Class<T> serviceType);
    public Map<Class<?>, AppbaseService> services();
    @Override public void shutdown();
    public static Builder builder() { ... }
}
```

### Builder

```java
public static final class Builder {

    // Option A: accept pre-built layers (most explicit)
    public Builder coreServices(AppbaseCoreServices coreServices);
    public Builder clientServices(AppbaseClientServices clientServices);

    // Option B: let AppbaseServices.Builder build AppbaseClientServices internally
    //   (cascades fromYaml or factories down to AppbaseClientServices.Builder)
    public Builder fromYaml(String classpathOrFilePath);

    // Register service factories (programmatic)
    public Builder with(AppbaseServiceFactory<?>... factories);

    public AppbaseServices build();
}
```

### Built-in service factories (initialization order)

| Order | Factory | Service type | Key dependencies |
|-------|---------|-------------|-----------------|
| 10 | `AuthorizationServiceFactory` | `AuthorizationService` | core only |
| 20 | `ClusterServiceFactory` | `ClusterService` | core only |
| 30 | `HealthCheckServiceFactory` | `HealthCheckService` | core only |
| 40 | `IdServiceFactory` | `IdService` | core only |
| 50 | `PasswordServiceFactory` | `PasswordService` | core only |
| 60 | `UserSessionStoreFactory` | `UserSessionStore` | AerospikeClientService |
| 70 | `UserAccountStoreFactory` | `UserAccountStore` | AerospikeClientService |
| 75 | `FirebaseAuthServiceFactory` | `FirebaseAuthService` | FirebaseAuthClientService + UserAccountStore |
| 80 | `UserAccountServiceFactory` | `UserAccountService` | FirebaseAuthService + UserAccountStore + UserSessionStore |
| 85 | `SupportMessageStoreFactory` | `SupportMessageStore` | AerospikeClientService |
| 90 | `SupportMessageServiceFactory` | `SupportMessageService` | SupportMessageStore |

`AppbaseServiceFactory.create()` receives both `AppbaseCoreServices` and
`AppbaseClientServices`, so each factory can pull the exact dependencies it needs via
`clients.get(SomeClientService.class)` — returns T or throws if not initialized.

---

## 8. JettyAppbaseServer

### Class

```java
public final class JettyAppbaseServer implements Startable, Shutdownable {

    public void start() throws AppbaseException;
    public void shutdown();
    public int port();           // actual bound port (useful when port=0)
    public AppbaseServices appbaseServices();

    public static Builder builder() { ... }
}
```

### Builder

```java
public static final class Builder {

    // Option A: accept pre-built AppbaseServices
    public Builder appbaseServices(AppbaseServices appbaseServices);

    // Option B: cascade — build AppbaseServices internally from YAML
    public Builder fromYaml(String classpathOrFilePath);

    // HTTP server config
    public Builder port(int port);                    // default: 8080
    public Builder requestLogging(boolean enabled);   // default: true (Slf4j)

    // Servlet/filter registration (matches Jetty API)
    public Builder addServlet(Class<? extends Servlet> servletClass, String pathSpec);
    public Builder addServlet(Servlet servlet, String pathSpec);
    public Builder addFilter(Class<? extends Filter> filterClass,
                             String pathSpec, EnumSet<DispatcherType> dispatches);
    public Builder addFilter(Filter filter,
                             String pathSpec, EnumSet<DispatcherType> dispatches);
    public Builder addServletContextAttribute(String name, Object value);

    // JAX-RS (Jersey) support
    public Builder enableJaxRs(String contextPath);           // mounts Jersey servlet
    public Builder addJaxRsResource(Class<?> resourceClass);  // registers JAX-RS resource

    public JettyAppbaseServer build();
}
```

### Lifecycle

```
build() → configures Jetty Server + connectors + handlers
start() → server.start() + registers shutdown hook
shutdown() → server.stop() → appbaseServices.shutdown() → clientServices.shutdown()
```

Shutdown order: Jetty → AppbaseServices (reverse init order) → AppbaseClientServices (reverse init order)
→ AppbaseCoreServices shutdown.

---

## 9. Guice Modules

Three Guice modules mirror the three builder layers. Each module `@Provides` its corresponding
container and depends on bindings from the module below.

```java
// Provides AppbaseCoreServices (wraps DefaultAppbaseCoreBootstrap)
public class AppbaseCoreServicesModule extends AbstractModule {
    public AppbaseCoreServicesModule(String yamlPath);
    public AppbaseCoreServicesModule(AppbaseCoreBootstrapConfiguration config);
}

// Provides AppbaseClientServices
// Requires AppbaseCoreServices binding (install AppbaseCoreServicesModule first)
public class AppbaseClientServicesModule extends AbstractModule {
    public AppbaseClientServicesModule(String yamlPath);
    public AppbaseClientServicesModule(AppbaseClientServiceFactory<?>... factories);
}

// Provides AppbaseServices
// Requires AppbaseClientServices binding (install AppbaseClientServicesModule first)
public class AppbaseServicesModule extends AbstractModule {
    public AppbaseServicesModule();                               // all defaults
    public AppbaseServicesModule(AppbaseServiceFactory<?>... factories);
}

// Provides JettyAppbaseServer
// Requires AppbaseServices binding (install AppbaseServicesModule first)
public class JettyAppbaseServerModule extends AbstractModule {
    public JettyAppbaseServerModule(int port);
    // servlet/filter registration via constructor or overrideable configure() hook
}
```

### Usage

```java
// Full stack with one YAML file
Injector injector = Guice.createInjector(
    new AppbaseCoreServicesModule("classpath:/appbase.yaml"),
    new AppbaseClientServicesModule("classpath:/appbase.yaml"),
    new AppbaseServicesModule(),
    new JettyAppbaseServerModule(8080)
);
JettyAppbaseServer server = injector.getInstance(JettyAppbaseServer.class);
server.start();
```

---

## 10. Testing Strategy

### Unit tests (no external infrastructure)

| Test class | What it verifies |
|-----------|-----------------|
| `AppbaseCoreServicesBuilderTest` | from(bootstrappedServices) populates map, get() returns service, services() unmodifiable, shutdown() reverses order |
| `AppbaseClientServicesBuilderTest` | factory registration, ordering, dependency enforcement (JDBI→JDBC), get() throws for absent service |
| `AppbaseClientServicesFactoryOrderTest` | all built-in factory orders are unique and in correct relative sequence |
| `AppbaseServicesBuilderTest` | cascades to AppbaseClientServices, factory ordering, shutdown order |
| `JettyAppbaseServerBuilderTest` | port default, request logging flag, servlet/filter registration |
| `AppbaseClientServicesModuleTest` | Guice binding resolves AppbaseClientServices |
| `AppbaseServicesModuleTest` | Guice binding resolves AppbaseServices |

All dependencies injected as mocks (Mockito).

### Integration test (no external infrastructure)

`AppbaseBootstrapIntegrationTest` — actually starts core services (LogService, MediatorService,
ConfigurationService, etc.) from classpath YAML. No Aerospike, ES, or Firebase required.

```java
@Test void testCoreAndClientServicesBootstrap_withNoFactories_succeeds() {
    // Boots AppbaseCoreServices.builder().fromYaml() — runs DefaultAppbaseCoreBootstrap internally
    // Creates AppbaseClientServices with zero factories (empty container)
    // Verifies services() returns empty map, shutdown() does not throw
}

@Test void testJettyAppbaseServer_startsAndStops() {
    // Boots core + empty client services + Jetty on port 0
    // Verifies server.port() > 0 after start
    // Verifies /health endpoint returns expected response
    // Calls shutdown(), verifies clean stop
}
```

### Stacktest validation (full live infrastructure — run before and after rewrite)

- **Before rewrite:** `./build.sh quick` on stacktest to verify current code compiles
- **After rewrite:** full live run with Docker infra (Aerospike, Vitess, Elasticsearch, Firebase emulator)
  testing both builder and Guice bootstrap paths

---

## 11. Stacktest Rewrite

### Phase 1 — verify current stacktest compiles

```
JAVA_HOME=.../temurin-25.jdk/Contents/Home ./build.sh quick
```

### Phase 2 — rewrite using Builder API

| Old class | New class | What changes |
|-----------|-----------|-------------|
| `AppbaseServices.java` | `AppbaseServices.java` (rewritten) | Replaces 400-line constructor with `AppbaseClientServices.builder()` + `AppbaseServices.builder()` |
| `StackTestServer.java` | `StackTestServer.java` (rewritten) | Replaces Jetty wiring with `JettyAppbaseServer.builder()` |

### Phase 3 — add Guice variant

New classes alongside the builder-based ones:
- `GuiceStackTestServer` — same server behaviour via Guice modules
- `GuiceStackTestMain` — entry point using Guice bootstrap

### Phase 4 — full live run

Run both `StackTestMain` (builder) and `GuiceStackTestMain` (Guice) against live infrastructure.
Both must pass all existing stacktest assertions.

---

## 12. Implementation Order (for writing-plans)

1. **Task 1** — Add `appbase-bootstrap` module to Maven build, empty skeleton, OSGi config, compiles
2. **Task 2** — Three factory interfaces (`AppbaseCoreServiceFactory`, `AppbaseClientServiceFactory`,
   `AppbaseServiceFactory`) + `AppbaseCoreServices` wrapper container + Builder (wraps
   `AppbaseCoreBootstrappedServices`, runs bootstrap internally from YAML, optional factory support)
3. **Task 3** — All built-in `AppbaseClientServiceFactory` implementations (one per client service)
4. **Task 4** — YAML loading in `AppbaseClientServices.Builder` (reads unified YAML, selects factories)
5. **Task 5** — `AppbaseServiceFactory` interface + `AppbaseServices` container + Builder
6. **Task 6** — All built-in `AppbaseServiceFactory` implementations (one per service)
7. **Task 7** — `JettyAppbaseServer` + Builder (servlet/filter API, lifecycle)
8. **Task 8** — Three Guice modules (`AppbaseCoreServicesModule`, `AppbaseClientServicesModule`, `AppbaseServicesModule`, `JettyAppbaseServerModule`)
9. **Task 9** — Unit tests (all builder and module tests)
10. **Task 10** — Integration test (`AppbaseBootstrapIntegrationTest`)
11. **Task 11** — Build verification: `./build.sh quick` on `appbase-bootstrap`
12. **Task 12** — Stacktest: verify current code compiles (`./build.sh quick` on stacktest)
13. **Task 13** — Stacktest rewrite Phase 2: builder-based `AppbaseServices` + `StackTestServer`
14. **Task 14** — Stacktest Phase 3: Guice variant (`GuiceStackTestServer` + `GuiceStackTestMain`)
15. **Task 15** — Full live stacktest run: both builder and Guice paths

---

## 13. Out of Scope

- `TcpClientService` and `WebClientService` — specialized HTTP/TCP clients not needed for standard bootstrap; add as follow-up factories if required
- Kubernetes/Docker-compose bootstrap helpers
- Spring integration
- Configuration hot-reload through the builder API

---

## Open Questions (resolved)

| # | Question | Decision |
|---|----------|---------|
| 1 | Optional vs direct return for `get()` | Direct return or throw — no Optional |
| 2 | Explicit `withXxx()` methods vs factory pattern | Factory pattern — builder never changes |
| 3 | Firebase: one or two factory methods | Two separate factories: App + Auth |
| 4 | JDBI without JDBC | `build()` throws `AppbaseRuntimeException` |
| 5 | Testing level for `appbase-bootstrap` | Unit tests + one lightweight integration test |
| 6 | Builder vs Guice vs unified config | Approach C: hierarchical composition, both paths |
| 7 | Rename AppbaseCoreBootstrappedServices? | No — new `AppbaseCoreServices` wrapper in `appbase-bootstrap` delegates to it; `appbase-core` unchanged |
| 8 | Factory interface for core layer? | Yes → `AppbaseCoreServiceFactory` for optional core services (e.g., LuceneService) |
| 9 | Which client services are in scope? | All 20 `AppbaseClientService` subtypes (incl. Cassandra, ClamAV, JAsync, JPA, Kafka, SMTP, Spark, VitessGrpc) |
| 10 | Which appbase-service services are in scope? | All 11: AuthorizationService + ClusterService + HealthCheckService + IdService + PasswordService + FirebaseAuthService + UserSessionStore + UserAccountStore + UserAccountService + SupportMessageStore + SupportMessageService |
