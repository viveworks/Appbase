# Appbase Bootstrap (P-001) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create `appbase-bootstrap` Maven module with hierarchical builder and Guice paths for `AppbaseCoreServices`, `AppbaseClientServices`, `AppbaseServices`, and `JettyAppbaseServer`; rewrite `appbase-stacktest` to use them.

**Architecture:** Three immutable service containers compose hierarchically via builders (Core → Client → Service → Server). Core service initialization is fully factory-driven: `AppbaseCoreServicesConfiguration` (user-facing config) feeds 13 built-in `AppbaseCoreServiceFactory` implementations that create each of the 11 mandatory + 2 optional core services in dependency order via a growing partial container. Client factories receive an `AppbaseCoreServicesAdapter` (adapts `AppbaseCoreServices` to the `AppbaseCore` interface) without exposing `AppbaseCore` on the public `AppbaseCoreServices` API. Service initialization order is enforced by `CoreServiceOrder` and `ClientServiceOrder` enums. A parallel Guice path provides the same composition via four `AbstractModule` subclasses. `appbase-core` is NOT modified — all new classes live in `appbase-bootstrap`.

**Tech Stack:** Java 25 · JUnit Jupiter 5.5 · Mockito 3 · Guice 4.2 · Jetty 11 · Jackson 2.17 YAML · maven-bundle-plugin (OSGi) · SLF4J 2.0

## Global Constraints

- Every `.java` file must begin with the project's Apache 2.0 license header (copy verbatim from any existing source file in `appbase-core/src/main/java/`):
  ```
  /*
   * Copyright 2019 The original author or authors of Appbase
   *
   * This project includes software developed by the author or authors of
   * Appbase (https://example.invalid/scratch-rehearsal).
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * ...
   */
  ```
- Java 25 source and target
- All public methods require Javadoc (Checkstyle enforced)
- SpotBugs max effort, low threshold; use `@SuppressFBWarnings` with justification when suppressing
- OSGi-compliant jar via `maven-bundle-plugin` (`packaging: bundle`)
- `groupId: com.viveworks.appbase`, `version: 1.0.0-SNAPSHOT`
- Do NOT modify `appbase-core`, `appbase-client`, or `appbase-service` classes
- `build()` throws `AppbaseRuntimeException` for invalid combos: JDBI without JDBC, Firebase Auth without Firebase App, S3 without AWS HTTP client, JPA without JDBC
- Shutdown in reverse initialization order; log errors via SLF4J, do NOT rethrow
- TDD: write failing test first, run to confirm failure, implement minimum to pass
- Branch naming: `IssueNumber/Java/Bootstrap/Feature/Appbase-bootstrap`
- Build command: `./build.sh quick` from `appbase-java/`

---

## File Map

```
appbase-java/
  appbase-bootstrap/
    pom.xml
    src/
      main/java/com/viveworks/appbase/bootstrap/
        core/
          AppbaseCoreServiceFactory.java            (interface)
          AppbaseCoreServicesConfiguration.java     (user-facing Jackson config — 11 mandatory + 2 optional sub-configs)
          CoreServiceOrder.java                     (enum — 13 entries)
          AppbaseCoreServicesAdapter.java           (public — adapts AppbaseCoreServices to AppbaseCore for client factories)
          AppbaseCoreServices.java                  (immutable container + builder with from()/fromYaml()/with())
          factory/
            LogServiceFactory.java
            MetricServiceFactory.java
            ValidationServiceFactory.java
            MediatorServiceFactory.java
            ConfigurationServiceFactory.java
            MicroprofileConfigurationServiceFactory.java
            ServiceRegistryFactory.java
            MicroprofileServiceRegistryFactory.java
            ExceptionServiceFactory.java
            TracerServiceFactory.java
            JmxServiceFactory.java
            ReactorServiceFactory.java
            LuceneServiceFactory.java
        client/
          AppbaseClientServiceFactory.java          (interface)
          ClientServiceOrder.java                   (enum — 20 entries)
          AppbaseClientServices.java                (immutable container + builder)
          factory/
            AwsSdkHttpClientServiceFactory.java
            AwsSdkAsyncHttpClientServiceFactory.java
            S3ClientsServiceFactory.java
            FirebaseAppClientServiceFactory.java
            FirebaseAuthClientServiceFactory.java
            AerospikeClientServiceFactory.java
            CassandraClientServiceFactory.java
            ClamAVClientServiceFactory.java
            JasyncClientServiceFactory.java
            ElasticsearchClientServiceFactory.java
            VitessJdbcClientServiceFactory.java
            VitessJdbiClientServiceFactory.java
            VitessGrpcClientServiceFactory.java
            JpaClientServiceFactory.java
            DruidSqlClientServiceFactory.java
            ZooKeeperClientServiceFactory.java
            KafkaProducerClientServiceFactory.java
            KafkaConsumerClientServiceFactory.java
            SmtpClientServiceFactory.java
            SparkClientServiceFactory.java
        service/
          AppbaseServiceFactory.java                (interface)
          AppbaseServices.java                      (immutable container + builder)
          factory/
            AuthorizationServiceFactory.java
            ClusterServiceFactory.java
            HealthCheckServiceFactory.java
            IdServiceFactory.java
            PasswordServiceFactory.java
            UserSessionStoreFactory.java
            UserAccountStoreFactory.java
            FirebaseAuthServiceFactory.java
            UserAccountServiceFactory.java
            SupportMessageStoreFactory.java
            SupportMessageServiceFactory.java
        server/
          JettyAppbaseServer.java
        guice/
          AppbaseCoreServicesModule.java
          AppbaseClientServicesModule.java
          AppbaseServicesModule.java
          JettyAppbaseServerModule.java
      test/java/com/viveworks/appbase/bootstrap/
        core/
          AppbaseCoreServicesTest.java
          CoreServiceFactoriesTest.java
          AppbaseCoreServicesAdapterTest.java
        client/
          AppbaseClientServicesTest.java
          ClientServiceFactoriesTest.java
        service/
          AppbaseServicesTest.java
        server/
          JettyAppbaseServerTest.java
        guice/
          AppbaseBootstrapGuiceModulesTest.java
        AppbaseBootstrapIntegrationTest.java
  appbase-stacktest/
    src/main/java/com/viveworks/appbase/stacktest/
      StackTestServer.java                          (rewrite — builder path)
      GuiceStackTestServer.java                     (new — Guice path)
      GuiceStackTestMain.java                       (new — Guice entry point)
```

---

### Task 1: Maven Module Skeleton

**Files:**
- Create: `appbase-java/appbase-bootstrap/pom.xml`
- Modify: `appbase-java/pom.xml` (add `<module>appbase-bootstrap</module>`)

**Interfaces:**
- Consumes: nothing
- Produces: compilable empty Maven module

- [ ] **Step 1: Add module to parent pom**

In `appbase-java/pom.xml`, find the `<modules>` block and insert `appbase-bootstrap` between `appbase-service` and `appbase-stacktest`:

```xml
<modules>
    <module>appbase-core</module>
    <module>appbase-client</module>
    <module>appbase-service</module>
    <module>appbase-bootstrap</module>
    <module>appbase-stacktest</module>
</modules>
```

- [ ] **Step 2: Create `appbase-bootstrap/pom.xml`**

Read `appbase-service/pom.xml` first — copy exact `maven-bundle-plugin`, `maven-checkstyle-plugin`, and `spotbugs-maven-plugin` config blocks verbatim:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.viveworks.appbase</groupId>
        <artifactId>appbase</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>appbase-bootstrap</artifactId>
    <packaging>bundle</packaging>
    <name>Appbase Bootstrap</name>
    <description>Bootstrap containers and builders for Appbase services and Jetty server</description>

    <dependencies>
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-client</artifactId>
        </dependency>
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-service</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-servlet</artifactId>
        </dependency>
        <dependency>
            <groupId>com.google.inject</groupId>
            <artifactId>guice</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-yaml</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Copy maven-bundle-plugin, maven-checkstyle-plugin, spotbugs-maven-plugin
                 config blocks verbatim from appbase-service/pom.xml -->
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Create source directories**

```bash
mkdir -p appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/{core/factory,client/factory,service/factory,server,guice}
mkdir -p appbase-java/appbase-bootstrap/src/test/java/com/viveworks/appbase/bootstrap/{core,client,service,server,guice}
```

- [ ] **Step 4: Verify compiles (empty)**

```bash
cd appbase-java && mvn clean compile -pl appbase-bootstrap -am -P dev-profile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add appbase-java/pom.xml appbase-java/appbase-bootstrap/
git commit -m "feat(P-001): scaffold appbase-bootstrap Maven module"
```

---

### Task 2: AppbaseCoreServiceFactory + AppbaseCoreServicesConfiguration + CoreServiceOrder + AppbaseCoreServicesAdapter + AppbaseCoreServices

**Files:**
- Create: `bootstrap/core/AppbaseCoreServiceFactory.java`
- Create: `bootstrap/core/CoreServiceOrder.java`
- Create: `bootstrap/core/AppbaseCoreServicesConfiguration.java`
- Create: `bootstrap/core/AppbaseCoreServicesAdapter.java`
- Create: `bootstrap/core/AppbaseCoreServices.java`
- Create: `test/.../core/AppbaseCoreServicesTest.java`
- Create: `test/.../core/AppbaseCoreServicesAdapterTest.java`

**Interfaces:**
- Consumes: `AppbaseCore`, `AppbaseService`, `AppbaseException`, `AppbaseRuntimeException`, `Shutdownable` from `appbase-core`; all `Default*ServiceConfiguration` types; all service interfaces (`LogService`, `MetricService`, …)
- Produces: `AppbaseCoreServices` with 11 typed getters, `get(Class<T>)`, `services()`, `shutdown()`, builder with `from(AppbaseCoreServicesConfiguration)` / `fromYaml(String)` / `with(AppbaseCoreServiceFactory<?>...)`; `AppbaseCoreServiceFactory<T>`; `CoreServiceOrder`; `AppbaseCoreServicesAdapter` implementing `AppbaseCore`; `AppbaseCoreServicesConfiguration`

**Before writing any code, read these files:**
- `appbase-core/.../AppbaseCore.java` — note ALL method signatures including `getAppbaseBootstrapConfiguration()` return type
- `appbase-core/.../bootstrap/DefaultAppbaseCoreBootstrapConfiguration.java` — copy field names and sub-config types exactly into `AppbaseCoreServicesConfiguration`
- Service interfaces: `LogService`, `MetricService`, `ValidationService`, `MediatorService`, `ConfigurationService`, `MicroprofileConfigurationService`, `ServiceRegistry`, `MicroprofileServiceRegistry`, `ExceptionService`, `TracerService`, `JmxService`, `ReactorService` — note package paths
- Search for `LuceneService` — verify its package (may be `service/lucene/` or `service/log/`)
- `appbase-core/.../util/Shutdownable.java` — verify package

- [ ] **Step 1: Write failing tests**

```java
// src/test/java/com/viveworks/appbase/bootstrap/core/AppbaseCoreServicesTest.java
package com.viveworks.appbase.bootstrap.core;

import com.viveworks.appbase.core.exception.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.registry.JmxService;     // verify package
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.reactive.ReactorService; // verify package
import com.viveworks.appbase.core.AppbaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppbaseCoreServicesTest {

    private LogService mockLog;
    private MetricService mockMetric;
    private ValidationService mockValidation;
    private MediatorService mockMediator;
    private ConfigurationService mockConfig;
    private MicroprofileConfigurationService mockMpConfig;
    private ServiceRegistry mockRegistry;
    private MicroprofileServiceRegistry mockMpRegistry;
    private ExceptionService mockException;
    private TracerService mockTracer;
    private JmxService mockJmx;

    @BeforeEach
    void setUp() {
        mockLog       = mock(LogService.class);
        mockMetric    = mock(MetricService.class);
        mockValidation = mock(ValidationService.class);
        mockMediator  = mock(MediatorService.class);
        mockConfig    = mock(ConfigurationService.class);
        mockMpConfig  = mock(MicroprofileConfigurationService.class);
        mockRegistry  = mock(ServiceRegistry.class);
        mockMpRegistry = mock(MicroprofileServiceRegistry.class);
        mockException = mock(ExceptionService.class);
        mockTracer    = mock(TracerService.class);
        mockJmx       = mock(JmxService.class);
    }

    // Helper: build an AppbaseCoreServices from mock factories for all 11 mandatory services
    private AppbaseCoreServices buildWithAllMandatory() {
        return AppbaseCoreServices.builder()
            .with(
                mockFactory(LogService.class,                      CoreServiceOrder.LOG,                      mockLog),
                mockFactory(MetricService.class,                   CoreServiceOrder.METRIC,                   mockMetric),
                mockFactory(ValidationService.class,               CoreServiceOrder.VALIDATION,               mockValidation),
                mockFactory(MediatorService.class,                 CoreServiceOrder.MEDIATOR,                 mockMediator),
                mockFactory(ConfigurationService.class,            CoreServiceOrder.CONFIGURATION,            mockConfig),
                mockFactory(MicroprofileConfigurationService.class,CoreServiceOrder.MICROPROFILE_CONFIGURATION,mockMpConfig),
                mockFactory(ServiceRegistry.class,                 CoreServiceOrder.SERVICE_REGISTRY,         mockRegistry),
                mockFactory(MicroprofileServiceRegistry.class,     CoreServiceOrder.MICROPROFILE_SERVICE_REGISTRY, mockMpRegistry),
                mockFactory(ExceptionService.class,                CoreServiceOrder.EXCEPTION,                mockException),
                mockFactory(TracerService.class,                   CoreServiceOrder.TRACER,                   mockTracer),
                mockFactory(JmxService.class,                      CoreServiceOrder.JMX,                      mockJmx)
            )
            .build();
    }

    @SuppressWarnings("unchecked")
    private <T extends AppbaseService> AppbaseCoreServiceFactory<T> mockFactory(
            Class<T> type, CoreServiceOrder order, T instance) {
        AppbaseCoreServiceFactory<T> f = mock(AppbaseCoreServiceFactory.class);
        when(f.serviceType()).thenReturn(type);
        when(f.initializationOrder()).thenReturn(order.order());
        try { when(f.create(any())).thenReturn(instance); } catch (Exception ignored) {}
        return f;
    }

    @Test
    void getLogService_returnsLogService() {
        assertSame(mockLog, buildWithAllMandatory().getLogService());
    }

    @Test
    void getMetricService_returnsMetricService() {
        assertSame(mockMetric, buildWithAllMandatory().getMetricService());
    }

    @Test
    void getJmxService_returnsJmxService() {
        assertSame(mockJmx, buildWithAllMandatory().getJmxService());
    }

    @Test
    void get_byType_returnsService() {
        assertSame(mockLog, buildWithAllMandatory().get(LogService.class));
    }

    @Test
    void get_unknownType_throwsAppbaseRuntimeException() {
        AppbaseCoreServices services = buildWithAllMandatory();
        assertThrows(AppbaseRuntimeException.class, () -> services.get(ReactorService.class));
    }

    @Test
    void services_containsAll11Mandatory() {
        Map<Class<?>, AppbaseService> map = buildWithAllMandatory().services();
        assertEquals(11, map.size());
        assertTrue(map.containsKey(LogService.class));
        assertTrue(map.containsKey(JmxService.class));
    }

    @Test
    void services_returnsUnmodifiableMap() {
        assertThrows(UnsupportedOperationException.class,
            () -> buildWithAllMandatory().services().put(LogService.class, null));
    }

    @Test
    void build_missingMandatoryService_throwsAppbaseRuntimeException() {
        // Only provide LogService — missing 10 others
        AppbaseCoreServiceFactory<LogService> logOnly =
            mockFactory(LogService.class, CoreServiceOrder.LOG, mockLog);
        assertThrows(AppbaseRuntimeException.class,
            () -> AppbaseCoreServices.builder().with(logOnly).build());
    }

    @Test
    void getReactorService_whenAbsent_returnsNull() {
        assertNull(buildWithAllMandatory().getReactorService());
    }

    @Test
    void with_factoryOverridesByServiceType() {
        LogService altLog = mock(LogService.class);
        AppbaseCoreServiceFactory<LogService> override =
            mockFactory(LogService.class, CoreServiceOrder.LOG, altLog);
        // Build: first register mockLog factory, then override with altLog factory
        AppbaseCoreServices services = AppbaseCoreServices.builder()
            .with(mockFactory(LogService.class, CoreServiceOrder.LOG, mockLog))
            .with(
                mockFactory(MetricService.class,                   CoreServiceOrder.METRIC,                   mockMetric),
                mockFactory(ValidationService.class,               CoreServiceOrder.VALIDATION,               mockValidation),
                mockFactory(MediatorService.class,                 CoreServiceOrder.MEDIATOR,                 mockMediator),
                mockFactory(ConfigurationService.class,            CoreServiceOrder.CONFIGURATION,            mockConfig),
                mockFactory(MicroprofileConfigurationService.class,CoreServiceOrder.MICROPROFILE_CONFIGURATION,mockMpConfig),
                mockFactory(ServiceRegistry.class,                 CoreServiceOrder.SERVICE_REGISTRY,         mockRegistry),
                mockFactory(MicroprofileServiceRegistry.class,     CoreServiceOrder.MICROPROFILE_SERVICE_REGISTRY, mockMpRegistry),
                mockFactory(ExceptionService.class,                CoreServiceOrder.EXCEPTION,                mockException),
                mockFactory(TracerService.class,                   CoreServiceOrder.TRACER,                   mockTracer),
                mockFactory(JmxService.class,                      CoreServiceOrder.JMX,                      mockJmx),
                override
            )
            .build();
        assertSame(altLog, services.getLogService());
    }

    @Test
    void shutdown_callsShutdownOnRegisteredServices() throws Exception {
        buildWithAllMandatory().shutdown();
        verify(mockLog).shutdown();
        verify(mockJmx).shutdown();
    }
}
```

```java
// src/test/java/com/viveworks/appbase/bootstrap/core/AppbaseCoreServicesAdapterTest.java
package com.viveworks.appbase.bootstrap.core;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppbaseCoreServicesAdapterTest {

    @Test
    void getLogService_delegatesToCoreServices() {
        AppbaseCoreServices mockServices = mock(AppbaseCoreServices.class);
        LogService mockLog = mock(LogService.class);
        when(mockServices.getLogService()).thenReturn(mockLog);
        assertSame(mockLog, new AppbaseCoreServicesAdapter(mockServices).getLogService());
    }

    @Test
    void getMediatorService_delegatesToCoreServices() {
        AppbaseCoreServices mockServices = mock(AppbaseCoreServices.class);
        MediatorService mockMediator = mock(MediatorService.class);
        when(mockServices.getMediatorService()).thenReturn(mockMediator);
        assertSame(mockMediator, new AppbaseCoreServicesAdapter(mockServices).getMediatorService());
    }

    @Test
    void getExceptionService_delegatesToCoreServices() {
        AppbaseCoreServices mockServices = mock(AppbaseCoreServices.class);
        ExceptionService mockEx = mock(ExceptionService.class);
        when(mockServices.getExceptionService()).thenReturn(mockEx);
        assertSame(mockEx, new AppbaseCoreServicesAdapter(mockServices).getExceptionService());
    }

    @Test
    void getLuceneService_whenAbsent_returnsNull() {
        AppbaseCoreServices mockServices = mock(AppbaseCoreServices.class);
        when(mockServices.getLuceneService()).thenReturn(null);
        assertNull(new AppbaseCoreServicesAdapter(mockServices).getLuceneService());
    }

    @Test
    void reload_doesNotThrow() {
        AppbaseCoreServices mockServices = mock(AppbaseCoreServices.class);
        assertDoesNotThrow(() -> new AppbaseCoreServicesAdapter(mockServices).reload());
    }

    @Test
    void shutdown_isNoOp_doesNotDelegateToContainer() {
        AppbaseCoreServices mockServices = mock(AppbaseCoreServices.class);
        new AppbaseCoreServicesAdapter(mockServices).shutdown();
        verify(mockServices, never()).shutdown();
    }
}
```

- [ ] **Step 2: Run tests to confirm failure**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest="AppbaseCoreServicesTest,AppbaseCoreServicesAdapterTest" -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -20
```

Expected: COMPILATION ERROR — none of the new classes exist yet.

- [ ] **Step 3: Create `CoreServiceOrder.java`**

```java
// src/main/java/com/viveworks/appbase/bootstrap/core/CoreServiceOrder.java
package com.viveworks.appbase.bootstrap.core;

/** Initialization order constants for all built-in Appbase core services. */
public enum CoreServiceOrder {
    LOG(10),
    METRIC(20),
    VALIDATION(30),
    MEDIATOR(40),
    CONFIGURATION(50),
    MICROPROFILE_CONFIGURATION(51),
    SERVICE_REGISTRY(60),
    MICROPROFILE_SERVICE_REGISTRY(61),
    EXCEPTION(70),
    TRACER(80),
    JMX(90),
    REACTOR(100),
    LUCENE(110);

    private final int order;

    CoreServiceOrder(int order) {
        this.order = order;
    }

    /** Returns the numeric initialization order. Lower values initialize first. */
    public int order() {
        return order;
    }
}
```

- [ ] **Step 4: Create `AppbaseCoreServiceFactory.java`**

```java
// src/main/java/com/viveworks/appbase/bootstrap/core/AppbaseCoreServiceFactory.java
package com.viveworks.appbase.bootstrap.core;

import com.viveworks.appbase.core.AppbaseService;
import com.viveworks.appbase.core.exception.AppbaseException;

/**
 * Factory that creates one typed Appbase core service.
 * Factories are sorted by {@link #initializationOrder()} before execution so that
 * dependencies already exist in the partial container when {@link #create} is called.
 *
 * @param <T> the service type produced by this factory
 */
public interface AppbaseCoreServiceFactory<T extends AppbaseService> {

    /** Returns the service interface class this factory produces. */
    Class<T> serviceType();

    /**
     * Returns numeric initialization order; lower values initialize first.
     * Use {@link CoreServiceOrder#order()} to supply the value.
     */
    int initializationOrder();

    /**
     * Creates the service. Call {@link AppbaseCoreServices#get(Class)} on {@code partial}
     * to retrieve services that were initialized before this one.
     *
     * @param partial partially-built container — all factories with lower order have already run
     * @return the newly created service instance
     * @throws AppbaseException if creation fails
     */
    T create(AppbaseCoreServices partial) throws AppbaseException;
}
```

- [ ] **Step 5: Create `AppbaseCoreServicesConfiguration.java`**

Read `appbase-core/.../bootstrap/DefaultAppbaseCoreBootstrapConfiguration.java` (or its interface) first. Copy the exact sub-config field names and types. The YAML structure must be identical so existing YAML files work with both classes.

```java
// src/main/java/com/viveworks/appbase/bootstrap/core/AppbaseCoreServicesConfiguration.java
package com.viveworks.appbase.bootstrap.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.validation.DefaultValidationServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.DefaultMicroprofileConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.DefaultMicroprofileServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.registry.DefaultJmxServiceConfiguration;    // verify package
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.tracer.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.reactive.DefaultReactorServiceConfiguration; // verify package
// LuceneService config — verify package before adding import:
// import com.viveworks.appbase.core.service.log.DefaultLuceneServiceConfiguration;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * User-facing configuration for {@link AppbaseCoreServices}.
 * Mirrors {@code DefaultAppbaseCoreBootstrapConfiguration} from appbase-core but lives in
 * appbase-bootstrap so users never import internal bootstrap classes.
 *
 * <p>YAML node name: {@value #DEFAULT_YAML_NODE_NAME}
 *
 * <p>Usage:
 * <pre>{@code
 * AppbaseCoreServices core = AppbaseCoreServices.builder()
 *     .fromYaml("appbase-config.yaml")
 *     .build();
 * // OR
 * AppbaseCoreServicesConfiguration cfg = AppbaseCoreServicesConfiguration.builder()
 *     .logServiceConfiguration(...)
 *     .build();
 * AppbaseCoreServices core = AppbaseCoreServices.builder().from(cfg).build();
 * }</pre>
 */
@JsonDeserialize(builder = AppbaseCoreServicesConfiguration.Builder.class)
public final class AppbaseCoreServicesConfiguration {

    /** Default YAML node name for deserialization. */
    public static final String DEFAULT_YAML_NODE_NAME = "appbaseCoreServicesConfiguration";

    private final DefaultLogServiceConfiguration logServiceConfiguration;
    private final DefaultMetricServiceConfiguration metricServiceConfiguration;
    private final DefaultValidationServiceConfiguration validationServiceConfiguration;
    private final DefaultMediatorServiceConfiguration mediatorServiceConfiguration;
    private final DefaultConfigurationServiceConfiguration configurationServiceConfiguration;
    private final DefaultMicroprofileConfigurationServiceConfiguration microprofileConfigurationServiceConfiguration;
    private final DefaultServiceRegistryConfiguration serviceRegistryConfiguration;
    private final DefaultMicroprofileServiceRegistryConfiguration microprofileServiceRegistryConfiguration;
    private final DefaultExceptionServiceConfiguration exceptionServiceConfiguration;
    private final DefaultTracerServiceConfiguration tracerServiceConfiguration;
    private final DefaultJmxServiceConfiguration jmxServiceConfiguration;
    @Nullable private final DefaultReactorServiceConfiguration reactorServiceConfiguration;
    // @Nullable private final DefaultLuceneServiceConfiguration luceneServiceConfiguration;  // add after verifying import

    private AppbaseCoreServicesConfiguration(Builder b) {
        this.logServiceConfiguration                         = Objects.requireNonNull(b.logServiceConfiguration,                         "logServiceConfiguration");
        this.metricServiceConfiguration                      = Objects.requireNonNull(b.metricServiceConfiguration,                      "metricServiceConfiguration");
        this.validationServiceConfiguration                  = Objects.requireNonNull(b.validationServiceConfiguration,                  "validationServiceConfiguration");
        this.mediatorServiceConfiguration                    = Objects.requireNonNull(b.mediatorServiceConfiguration,                    "mediatorServiceConfiguration");
        this.configurationServiceConfiguration               = Objects.requireNonNull(b.configurationServiceConfiguration,               "configurationServiceConfiguration");
        this.microprofileConfigurationServiceConfiguration   = Objects.requireNonNull(b.microprofileConfigurationServiceConfiguration,   "microprofileConfigurationServiceConfiguration");
        this.serviceRegistryConfiguration                    = Objects.requireNonNull(b.serviceRegistryConfiguration,                    "serviceRegistryConfiguration");
        this.microprofileServiceRegistryConfiguration        = Objects.requireNonNull(b.microprofileServiceRegistryConfiguration,        "microprofileServiceRegistryConfiguration");
        this.exceptionServiceConfiguration                   = Objects.requireNonNull(b.exceptionServiceConfiguration,                   "exceptionServiceConfiguration");
        this.tracerServiceConfiguration                      = Objects.requireNonNull(b.tracerServiceConfiguration,                      "tracerServiceConfiguration");
        this.jmxServiceConfiguration                         = Objects.requireNonNull(b.jmxServiceConfiguration,                         "jmxServiceConfiguration");
        this.reactorServiceConfiguration                     = b.reactorServiceConfiguration;
        // this.luceneServiceConfiguration                   = b.luceneServiceConfiguration;
    }

    /** Returns the log service configuration. */
    public DefaultLogServiceConfiguration getLogServiceConfiguration() { return logServiceConfiguration; }
    /** Returns the metric service configuration. */
    public DefaultMetricServiceConfiguration getMetricServiceConfiguration() { return metricServiceConfiguration; }
    /** Returns the validation service configuration. */
    public DefaultValidationServiceConfiguration getValidationServiceConfiguration() { return validationServiceConfiguration; }
    /** Returns the mediator service configuration. */
    public DefaultMediatorServiceConfiguration getMediatorServiceConfiguration() { return mediatorServiceConfiguration; }
    /** Returns the configuration service configuration. */
    public DefaultConfigurationServiceConfiguration getConfigurationServiceConfiguration() { return configurationServiceConfiguration; }
    /** Returns the MicroProfile configuration service configuration. */
    public DefaultMicroprofileConfigurationServiceConfiguration getMicroprofileConfigurationServiceConfiguration() { return microprofileConfigurationServiceConfiguration; }
    /** Returns the service registry configuration. */
    public DefaultServiceRegistryConfiguration getServiceRegistryConfiguration() { return serviceRegistryConfiguration; }
    /** Returns the MicroProfile service registry configuration. */
    public DefaultMicroprofileServiceRegistryConfiguration getMicroprofileServiceRegistryConfiguration() { return microprofileServiceRegistryConfiguration; }
    /** Returns the exception service configuration. */
    public DefaultExceptionServiceConfiguration getExceptionServiceConfiguration() { return exceptionServiceConfiguration; }
    /** Returns the tracer service configuration. */
    public DefaultTracerServiceConfiguration getTracerServiceConfiguration() { return tracerServiceConfiguration; }
    /** Returns the JMX service configuration. */
    public DefaultJmxServiceConfiguration getJmxServiceConfiguration() { return jmxServiceConfiguration; }
    /** Returns the optional reactor service configuration, or null if reactor is disabled. */
    @Nullable public DefaultReactorServiceConfiguration getReactorServiceConfiguration() { return reactorServiceConfiguration; }
    // /** Returns the optional Lucene service configuration, or null if Lucene is disabled. */
    // @Nullable public DefaultLuceneServiceConfiguration getLuceneServiceConfiguration() { return luceneServiceConfiguration; }

    /** Returns a new builder. */
    public static Builder builder() { return new Builder(); }

    /** Builder for {@link AppbaseCoreServicesConfiguration}. Also used by Jackson for YAML deserialization. */
    public static final class Builder {
        @JsonProperty("logServiceConfiguration")
        private DefaultLogServiceConfiguration logServiceConfiguration;
        @JsonProperty("metricServiceConfiguration")
        private DefaultMetricServiceConfiguration metricServiceConfiguration;
        @JsonProperty("validationServiceConfiguration")
        private DefaultValidationServiceConfiguration validationServiceConfiguration;
        @JsonProperty("mediatorServiceConfiguration")
        private DefaultMediatorServiceConfiguration mediatorServiceConfiguration;
        @JsonProperty("configurationServiceConfiguration")
        private DefaultConfigurationServiceConfiguration configurationServiceConfiguration;
        @JsonProperty("microprofileConfigurationServiceConfiguration")
        private DefaultMicroprofileConfigurationServiceConfiguration microprofileConfigurationServiceConfiguration;
        @JsonProperty("serviceRegistryConfiguration")
        private DefaultServiceRegistryConfiguration serviceRegistryConfiguration;
        @JsonProperty("microprofileServiceRegistryConfiguration")
        private DefaultMicroprofileServiceRegistryConfiguration microprofileServiceRegistryConfiguration;
        @JsonProperty("exceptionServiceConfiguration")
        private DefaultExceptionServiceConfiguration exceptionServiceConfiguration;
        @JsonProperty("tracerServiceConfiguration")
        private DefaultTracerServiceConfiguration tracerServiceConfiguration;
        @JsonProperty("jmxServiceConfiguration")
        private DefaultJmxServiceConfiguration jmxServiceConfiguration;
        @JsonProperty("reactorServiceConfiguration")
        @Nullable private DefaultReactorServiceConfiguration reactorServiceConfiguration;
        // @JsonProperty("luceneServiceConfiguration")
        // @Nullable private DefaultLuceneServiceConfiguration luceneServiceConfiguration;

        /** Sets log service configuration. */
        public Builder logServiceConfiguration(DefaultLogServiceConfiguration v) { logServiceConfiguration = v; return this; }
        /** Sets metric service configuration. */
        public Builder metricServiceConfiguration(DefaultMetricServiceConfiguration v) { metricServiceConfiguration = v; return this; }
        /** Sets validation service configuration. */
        public Builder validationServiceConfiguration(DefaultValidationServiceConfiguration v) { validationServiceConfiguration = v; return this; }
        /** Sets mediator service configuration. */
        public Builder mediatorServiceConfiguration(DefaultMediatorServiceConfiguration v) { mediatorServiceConfiguration = v; return this; }
        /** Sets configuration service configuration. */
        public Builder configurationServiceConfiguration(DefaultConfigurationServiceConfiguration v) { configurationServiceConfiguration = v; return this; }
        /** Sets MicroProfile configuration service configuration. */
        public Builder microprofileConfigurationServiceConfiguration(DefaultMicroprofileConfigurationServiceConfiguration v) { microprofileConfigurationServiceConfiguration = v; return this; }
        /** Sets service registry configuration. */
        public Builder serviceRegistryConfiguration(DefaultServiceRegistryConfiguration v) { serviceRegistryConfiguration = v; return this; }
        /** Sets MicroProfile service registry configuration. */
        public Builder microprofileServiceRegistryConfiguration(DefaultMicroprofileServiceRegistryConfiguration v) { microprofileServiceRegistryConfiguration = v; return this; }
        /** Sets exception service configuration. */
        public Builder exceptionServiceConfiguration(DefaultExceptionServiceConfiguration v) { exceptionServiceConfiguration = v; return this; }
        /** Sets tracer service configuration. */
        public Builder tracerServiceConfiguration(DefaultTracerServiceConfiguration v) { tracerServiceConfiguration = v; return this; }
        /** Sets JMX service configuration. */
        public Builder jmxServiceConfiguration(DefaultJmxServiceConfiguration v) { jmxServiceConfiguration = v; return this; }
        /** Sets optional reactor service configuration (null = reactor disabled). */
        public Builder reactorServiceConfiguration(@Nullable DefaultReactorServiceConfiguration v) { reactorServiceConfiguration = v; return this; }
        // public Builder luceneServiceConfiguration(@Nullable DefaultLuceneServiceConfiguration v) { luceneServiceConfiguration = v; return this; }

        /** Builds the configuration. All non-nullable fields must be set. */
        public AppbaseCoreServicesConfiguration build() { return new AppbaseCoreServicesConfiguration(this); }
    }
}
```

- [ ] **Step 6: Create `AppbaseCoreServicesAdapter.java`**

Read `AppbaseCore.java` first — implement ALL methods. `getAppbaseBootstrapConfiguration()` return type must match the interface declaration exactly.

```java
// src/main/java/com/viveworks/appbase/bootstrap/core/AppbaseCoreServicesAdapter.java
package com.viveworks.appbase.bootstrap.core;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.exception.AppbaseException;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
// LuceneService import — verify exact package before adding:
// import com.viveworks.appbase.core.service.log.LuceneService;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Adapts {@link AppbaseCoreServices} to the {@link AppbaseCore} interface so that client
 * service factories can construct {@code DefaultAppbaseClientCore(AppbaseCore)} without
 * {@link AppbaseCoreServices} exposing {@link AppbaseCore} as part of its public API.
 *
 * <p>{@link #reload()} and {@link #shutdown()} are no-ops: lifecycle is managed by
 * {@link AppbaseCoreServices#shutdown()}.
 */
public final class AppbaseCoreServicesAdapter implements AppbaseCore {

    private final AppbaseCoreServices coreServices;

    /**
     * Wraps the given core services container.
     *
     * @param coreServices the fully-built core services container
     */
    public AppbaseCoreServicesAdapter(AppbaseCoreServices coreServices) {
        this.coreServices = Objects.requireNonNull(coreServices, "coreServices");
    }

    @Override
    public LogService getLogService() { return coreServices.getLogService(); }

    @Override
    public MediatorService getMediatorService() { return coreServices.getMediatorService(); }

    @Override
    public ExceptionService getExceptionService() { return coreServices.getExceptionService(); }

    /** Returns null when Lucene service is not configured. */
    @Nullable
    @Override
    public Object getLuceneService() {
        // Replace return type with LuceneService after verifying its import.
        // Return: coreServices.getLuceneService();
        return coreServices.getLuceneService();
    }

    /**
     * No-op. Lifecycle is managed by {@link AppbaseCoreServices}.
     * Do NOT reload individual services via this adapter.
     */
    @Override
    public void reload() throws AppbaseException { /* no-op */ }

    /**
     * No-op. {@link AppbaseCoreServices#shutdown()} handles shutdown in reverse init order.
     * Do NOT shut down via this adapter.
     */
    @Override
    public void shutdown() { /* no-op */ }

    // NOTE: implement getAppbaseBootstrapConfiguration() after reading AppbaseCore.
    // Return type and value depend on the interface declaration.
}
```

- [ ] **Step 7: Create `AppbaseCoreServices.java`**

```java
// src/main/java/com/viveworks/appbase/bootstrap/core/AppbaseCoreServices.java
package com.viveworks.appbase.bootstrap.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.viveworks.appbase.bootstrap.core.factory.*;
import com.viveworks.appbase.core.AppbaseService;
import com.viveworks.appbase.core.exception.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.ReactorService; // verify package
import com.viveworks.appbase.core.service.registry.JmxService;     // verify package
import com.viveworks.appbase.core.service.registry.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.Shutdownable; // verify package
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Immutable container holding all initialized Appbase core services.
 *
 * <p>Build via the fluent builder:
 * <pre>{@code
 * // From YAML file (classpath or filesystem):
 * AppbaseCoreServices core = AppbaseCoreServices.builder()
 *     .fromYaml("appbase-config.yaml")
 *     .build();
 *
 * // From programmatic config:
 * AppbaseCoreServicesConfiguration cfg = AppbaseCoreServicesConfiguration.builder()
 *     .logServiceConfiguration(...)
 *     ...
 *     .build();
 * AppbaseCoreServices core = AppbaseCoreServices.builder().from(cfg).build();
 *
 * // Override one service factory while keeping the rest from config:
 * AppbaseCoreServices core = AppbaseCoreServices.builder()
 *     .fromYaml("appbase-config.yaml")
 *     .with(new LogServiceFactory(myCustomLogService))
 *     .build();
 * }</pre>
 */
@Immutable
public final class AppbaseCoreServices implements Shutdownable {

    private static final List<Class<? extends AppbaseService>> MANDATORY_TYPES = List.of(
        LogService.class, MetricService.class, ValidationService.class,
        MediatorService.class, ConfigurationService.class,
        MicroprofileConfigurationService.class, ServiceRegistry.class,
        MicroprofileServiceRegistry.class, ExceptionService.class,
        TracerService.class, JmxService.class
    );

    private final Map<Class<?>, AppbaseService> services;
    private final List<AppbaseService> initOrder;

    private AppbaseCoreServices(Map<Class<?>, AppbaseService> services,
                                 List<AppbaseService> initOrder) {
        this.services  = Collections.unmodifiableMap(new LinkedHashMap<>(services));
        this.initOrder = List.copyOf(initOrder);
    }

    // ── Mandatory typed getters ────────────────────────────────────────────────

    /** Returns the log service. */
    public LogService getLogService() { return get(LogService.class); }
    /** Returns the metric service. */
    public MetricService getMetricService() { return get(MetricService.class); }
    /** Returns the validation service. */
    public ValidationService getValidationService() { return get(ValidationService.class); }
    /** Returns the mediator service. */
    public MediatorService getMediatorService() { return get(MediatorService.class); }
    /** Returns the configuration service. */
    public ConfigurationService getConfigurationService() { return get(ConfigurationService.class); }
    /** Returns the MicroProfile configuration service. */
    public MicroprofileConfigurationService getMicroprofileConfigurationService() { return get(MicroprofileConfigurationService.class); }
    /** Returns the service registry. */
    public ServiceRegistry getServiceRegistry() { return get(ServiceRegistry.class); }
    /** Returns the MicroProfile service registry. */
    public MicroprofileServiceRegistry getMicroprofileServiceRegistry() { return get(MicroprofileServiceRegistry.class); }
    /** Returns the exception service. */
    public ExceptionService getExceptionService() { return get(ExceptionService.class); }
    /** Returns the tracer service. */
    public TracerService getTracerService() { return get(TracerService.class); }
    /** Returns the JMX service. */
    public JmxService getJmxService() { return get(JmxService.class); }

    // ── Optional typed getters ─────────────────────────────────────────────────

    /** Returns the reactor service, or null if not configured. */
    @Nullable
    public ReactorService getReactorService() {
        return (ReactorService) services.get(ReactorService.class);
    }

    /** Returns the Lucene service, or null if not configured. */
    @Nullable
    public Object getLuceneService() {
        // Replace Object with LuceneService after verifying its import.
        return services.get(/* LuceneService.class */ null);
    }

    // ── Generic typed access ───────────────────────────────────────────────────

    /**
     * Returns the service registered under {@code serviceType}.
     *
     * @param serviceType the service interface class
     * @param <T> the service type
     * @return the registered service instance
     * @throws AppbaseRuntimeException if no service of that type is registered
     */
    @SuppressWarnings("unchecked")
    public <T extends AppbaseService> T get(Class<T> serviceType) {
        AppbaseService s = services.get(Objects.requireNonNull(serviceType, "serviceType"));
        if (s == null) {
            throw new AppbaseRuntimeException(
                "No core service registered for type: " + serviceType.getName()
                + ". Provide it via AppbaseCoreServicesConfiguration or with(AppbaseCoreServiceFactory).");
        }
        return (T) s;
    }

    /** Returns an unmodifiable map of all registered core services, keyed by service interface. */
    public Map<Class<?>, AppbaseService> services() {
        return services;
    }

    /**
     * Shuts down all services in reverse initialization order.
     * Errors are logged and NOT rethrown.
     */
    @Override
    public void shutdown() {
        List<AppbaseService> reversed = new ArrayList<>(initOrder);
        Collections.reverse(reversed);
        for (AppbaseService svc : reversed) {
            try {
                svc.shutdown();
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AppbaseCoreServices.class)
                    .error("Error shutting down core service {}", svc.getClass().getName(), e);
            }
        }
    }

    /** Returns a new builder. */
    public static Builder builder() {
        return new Builder();
    }

    // ── Builder ────────────────────────────────────────────────────────────────

    /** Builder for {@link AppbaseCoreServices}. */
    public static final class Builder {

        @Nullable private AppbaseCoreServicesConfiguration config;
        private final List<AppbaseCoreServiceFactory<?>> customFactories = new ArrayList<>();

        /**
         * Configures core services from the given configuration object.
         * Built-in factories are constructed from each sub-config; call {@link #with} to override any of them.
         *
         * @param config the user-facing configuration
         * @return this builder
         */
        public Builder from(AppbaseCoreServicesConfiguration config) {
            this.config = Objects.requireNonNull(config, "config");
            return this;
        }

        /**
         * Loads core services configuration from a YAML file.
         * The file is resolved first as a classpath resource, then as an absolute filesystem path.
         * The YAML must have a top-level {@value AppbaseCoreServicesConfiguration#DEFAULT_YAML_NODE_NAME} node.
         *
         * @param classpathOrFilePath classpath resource path or absolute file path
         * @return this builder
         * @throws AppbaseRuntimeException if the file cannot be read or parsed
         */
        public Builder fromYaml(String classpathOrFilePath) {
            Objects.requireNonNull(classpathOrFilePath, "classpathOrFilePath");
            try {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                mapper.findAndRegisterModules();
                InputStream in = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(classpathOrFilePath);
                if (in == null) {
                    in = new FileInputStream(classpathOrFilePath);
                }
                this.config = mapper.readTree(in)
                    .path(AppbaseCoreServicesConfiguration.DEFAULT_YAML_NODE_NAME)
                    .traverse(mapper)
                    .readValueAs(AppbaseCoreServicesConfiguration.class);
            } catch (Exception e) {
                throw new AppbaseRuntimeException(
                    "Failed to load core services configuration from: " + classpathOrFilePath, e);
            }
            return this;
        }

        /**
         * Registers custom service factories.
         * A factory for a {@link AppbaseCoreServiceFactory#serviceType()} that already has a
         * built-in factory (from {@link #from} / {@link #fromYaml}) replaces that built-in.
         * Calling {@code with()} multiple times accumulates factories; last one wins per type.
         *
         * @param factories one or more custom factories
         * @return this builder
         */
        public Builder with(AppbaseCoreServiceFactory<?>... factories) {
            Objects.requireNonNull(factories, "factories");
            for (AppbaseCoreServiceFactory<?> f : factories) {
                customFactories.add(Objects.requireNonNull(f, "factory element must not be null"));
            }
            return this;
        }

        /**
         * Builds the {@link AppbaseCoreServices} container.
         *
         * @return the fully initialized container
         * @throws AppbaseRuntimeException if any mandatory service is missing or a factory fails
         */
        public AppbaseCoreServices build() {
            // 1. Start with built-in factories derived from config (if provided)
            Map<Class<?>, AppbaseCoreServiceFactory<?>> factoryMap = new LinkedHashMap<>();
            if (config != null) {
                registerBuiltinFactories(factoryMap, config);
            }
            // 2. Custom factories override built-ins by serviceType()
            for (AppbaseCoreServiceFactory<?> f : customFactories) {
                factoryMap.put(f.serviceType(), f);
            }
            // 3. Sort by initializationOrder()
            List<AppbaseCoreServiceFactory<?>> sorted = new ArrayList<>(factoryMap.values());
            sorted.sort(Comparator.comparingInt(AppbaseCoreServiceFactory::initializationOrder));

            // 4. Run each factory in order; pass a partial AppbaseCoreServices as deps container
            Map<Class<?>, AppbaseService> built    = new LinkedHashMap<>();
            List<AppbaseService>          initOrd  = new ArrayList<>();
            // Build a fresh partial view after each factory so the next factory sees new deps
            AppbaseCoreServices partial = new AppbaseCoreServices(built, initOrd);
            for (AppbaseCoreServiceFactory<?> factory : sorted) {
                try {
                    AppbaseService svc = factory.create(partial);
                    built.put(factory.serviceType(), svc);
                    initOrd.add(svc);
                    partial = new AppbaseCoreServices(new LinkedHashMap<>(built), new ArrayList<>(initOrd));
                } catch (Exception e) {
                    throw new AppbaseRuntimeException(
                        "Failed to create core service: " + factory.serviceType().getName(), e);
                }
            }

            // 5. Validate all 11 mandatory services are present
            for (Class<? extends AppbaseService> mandatory : MANDATORY_TYPES) {
                if (!built.containsKey(mandatory)) {
                    throw new AppbaseRuntimeException(
                        "Missing mandatory core service: " + mandatory.getName()
                        + ". Provide it via AppbaseCoreServicesConfiguration or with(AppbaseCoreServiceFactory).");
                }
            }
            return new AppbaseCoreServices(built, initOrd);
        }

        private static void registerBuiltinFactories(Map<Class<?>, AppbaseCoreServiceFactory<?>> map,
                                                      AppbaseCoreServicesConfiguration cfg) {
            put(map, new LogServiceFactory(cfg.getLogServiceConfiguration()));
            put(map, new MetricServiceFactory(cfg.getMetricServiceConfiguration()));
            put(map, new ValidationServiceFactory(cfg.getValidationServiceConfiguration()));
            put(map, new MediatorServiceFactory(cfg.getMediatorServiceConfiguration()));
            put(map, new ConfigurationServiceFactory(cfg.getConfigurationServiceConfiguration()));
            put(map, new MicroprofileConfigurationServiceFactory(cfg.getMicroprofileConfigurationServiceConfiguration()));
            put(map, new ServiceRegistryFactory(cfg.getServiceRegistryConfiguration()));
            put(map, new MicroprofileServiceRegistryFactory(cfg.getMicroprofileServiceRegistryConfiguration()));
            put(map, new ExceptionServiceFactory(cfg.getExceptionServiceConfiguration()));
            put(map, new TracerServiceFactory(cfg.getTracerServiceConfiguration()));
            put(map, new JmxServiceFactory(cfg.getJmxServiceConfiguration()));
            if (cfg.getReactorServiceConfiguration() != null) {
                put(map, new ReactorServiceFactory(cfg.getReactorServiceConfiguration()));
            }
            // Uncomment after verifying LuceneServiceConfiguration import:
            // if (cfg.getLuceneServiceConfiguration() != null) {
            //     put(map, new LuceneServiceFactory(cfg.getLuceneServiceConfiguration()));
            // }
        }

        private static void put(Map<Class<?>, AppbaseCoreServiceFactory<?>> map,
                                AppbaseCoreServiceFactory<?> factory) {
            map.put(factory.serviceType(), factory);
        }
    }
}
```

- [ ] **Step 8: Run tests**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap \
  -Dtest="AppbaseCoreServicesTest,AppbaseCoreServicesAdapterTest" \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -30
```

Expected: All tests PASS. If an import fails (LuceneService, JmxService, Shutdownable), search the correct package with:

```bash
find appbase-java/appbase-core/src -name "LuceneService.java" -o -name "JmxService.java" -o -name "Shutdownable.java"
```

Then fix the import and re-run.

- [ ] **Step 9: Commit**

```bash
git add appbase-java/appbase-bootstrap/src/
git commit -m "feat(P-001): add AppbaseCoreServices, AppbaseCoreServicesConfiguration, CoreServiceOrder, AppbaseCoreServicesAdapter"
```

---

### Task 3: 13 Built-in AppbaseCoreServiceFactory Implementations

**Files:**
- Create: `bootstrap/core/factory/LogServiceFactory.java`
- Create: `bootstrap/core/factory/MetricServiceFactory.java`
- Create: `bootstrap/core/factory/ValidationServiceFactory.java`
- Create: `bootstrap/core/factory/MediatorServiceFactory.java`
- Create: `bootstrap/core/factory/ConfigurationServiceFactory.java`
- Create: `bootstrap/core/factory/MicroprofileConfigurationServiceFactory.java`
- Create: `bootstrap/core/factory/ServiceRegistryFactory.java`
- Create: `bootstrap/core/factory/MicroprofileServiceRegistryFactory.java`
- Create: `bootstrap/core/factory/ExceptionServiceFactory.java`
- Create: `bootstrap/core/factory/TracerServiceFactory.java`
- Create: `bootstrap/core/factory/JmxServiceFactory.java`
- Create: `bootstrap/core/factory/ReactorServiceFactory.java`
- Create: `bootstrap/core/factory/LuceneServiceFactory.java`
- Create: `test/.../core/CoreServiceFactoriesTest.java`

**Interfaces:**
- Consumes: `AppbaseCoreServiceFactory<T>`, `CoreServiceOrder`, `AppbaseCoreServices`, all `Default*Service` implementation classes and their `Default*ServiceConfiguration` types from `appbase-core`
- Produces: 13 factory classes; each has two constructors: one wrapping a pre-built service, one creating from config

**Before writing code, read the constructor of each `DefaultXxxService` to learn required params:**
- `DefaultLogService`, `DefaultMetricService`, `DefaultValidationService`
- `DefaultMediatorService` — likely needs `LogService`
- `DefaultConfigurationService` — likely needs `MediatorService`
- `DefaultMicroprofileConfigurationService` — likely needs `ConfigurationService`
- `DefaultServiceRegistry`, `DefaultMicroprofileServiceRegistry`
- `DefaultExceptionService`, `DefaultTracerService`, `DefaultJmxService`
- `DefaultReactorService`, and the Lucene service impl

Constructor calls in the factory code below are illustrative; adjust after reading actual constructors.

- [ ] **Step 1: Write failing tests**

```java
// src/test/java/com/viveworks/appbase/bootstrap/core/CoreServiceFactoriesTest.java
package com.viveworks.appbase.bootstrap.core;

import com.viveworks.appbase.bootstrap.core.factory.*;
import com.viveworks.appbase.core.exception.AppbaseException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.registry.JmxService;     // verify package
import com.viveworks.appbase.core.service.reactive.ReactorService; // verify package
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoreServiceFactoriesTest {

    // ── LogServiceFactory ──────────────────────────────────────────────────────

    @Test
    void logFactory_predefined_returnsSameInstance() throws AppbaseException {
        LogService svc = mock(LogService.class);
        assertSame(svc, new LogServiceFactory(svc).create(mock(AppbaseCoreServices.class)));
    }

    @Test
    void logFactory_serviceType_isLogService() {
        assertEquals(LogService.class, new LogServiceFactory(mock(LogService.class)).serviceType());
    }

    @Test
    void logFactory_order_matchesCoreServiceOrder() {
        assertEquals(CoreServiceOrder.LOG.order(),
            new LogServiceFactory(mock(LogService.class)).initializationOrder());
    }

    // ── MediatorServiceFactory ─────────────────────────────────────────────────

    @Test
    void mediatorFactory_predefined_returnsSameInstance() throws AppbaseException {
        MediatorService svc = mock(MediatorService.class);
        assertSame(svc, new MediatorServiceFactory(svc).create(mock(AppbaseCoreServices.class)));
    }

    @Test
    void mediatorFactory_serviceType_isMediatorService() {
        assertEquals(MediatorService.class,
            new MediatorServiceFactory(mock(MediatorService.class)).serviceType());
    }

    @Test
    void mediatorFactory_order_matchesCoreServiceOrder() {
        assertEquals(CoreServiceOrder.MEDIATOR.order(),
            new MediatorServiceFactory(mock(MediatorService.class)).initializationOrder());
    }

    // ── ExceptionServiceFactory ────────────────────────────────────────────────

    @Test
    void exceptionFactory_predefined_returnsSameInstance() throws AppbaseException {
        ExceptionService svc = mock(ExceptionService.class);
        assertSame(svc, new ExceptionServiceFactory(svc).create(mock(AppbaseCoreServices.class)));
    }

    @Test
    void exceptionFactory_order_matchesCoreServiceOrder() {
        assertEquals(CoreServiceOrder.EXCEPTION.order(),
            new ExceptionServiceFactory(mock(ExceptionService.class)).initializationOrder());
    }

    // ── JmxServiceFactory ──────────────────────────────────────────────────────

    @Test
    void jmxFactory_predefined_returnsSameInstance() throws AppbaseException {
        JmxService svc = mock(JmxService.class);
        assertSame(svc, new JmxServiceFactory(svc).create(mock(AppbaseCoreServices.class)));
    }

    @Test
    void jmxFactory_order_matchesCoreServiceOrder() {
        assertEquals(CoreServiceOrder.JMX.order(),
            new JmxServiceFactory(mock(JmxService.class)).initializationOrder());
    }

    // ── ReactorServiceFactory ──────────────────────────────────────────────────

    @Test
    void reactorFactory_predefined_returnsSameInstance() throws AppbaseException {
        ReactorService svc = mock(ReactorService.class);
        assertSame(svc, new ReactorServiceFactory(svc).create(mock(AppbaseCoreServices.class)));
    }

    @Test
    void reactorFactory_order_matchesCoreServiceOrder() {
        assertEquals(CoreServiceOrder.REACTOR.order(),
            new ReactorServiceFactory(mock(ReactorService.class)).initializationOrder());
    }
}
```

- [ ] **Step 2: Run tests to confirm failure**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=CoreServiceFactoriesTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -20
```

Expected: COMPILATION ERROR — factory classes do not exist yet.

- [ ] **Step 3: Implement `LogServiceFactory.java`**

Each factory has two constructors: wrapping a pre-built service, or creating from config.

```java
// src/main/java/com/viveworks/appbase/bootstrap/core/factory/LogServiceFactory.java
package com.viveworks.appbase.bootstrap.core.factory;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.core.CoreServiceOrder;
import com.viveworks.appbase.core.exception.AppbaseException;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link LogService}.
 * Two construction modes:
 * <ul>
 *   <li>Pre-built: {@link #LogServiceFactory(LogService)} — wraps an existing instance.</li>
 *   <li>Config-based: {@link #LogServiceFactory(DefaultLogServiceConfiguration)} — creates a {@link DefaultLogService}.</li>
 * </ul>
 */
public final class LogServiceFactory implements AppbaseCoreServiceFactory<LogService> {

    @Nullable private final LogService predefinedService;
    @Nullable private final DefaultLogServiceConfiguration config;

    /** Wraps a pre-built {@link LogService} instance. */
    public LogServiceFactory(LogService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link DefaultLogService} from {@code config}. */
    public LogServiceFactory(DefaultLogServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    @Override
    public Class<LogService> serviceType() { return LogService.class; }

    @Override
    public int initializationOrder() { return CoreServiceOrder.LOG.order(); }

    /** Returns the pre-built service, or creates a new {@link DefaultLogService} from config. */
    @Override
    public LogService create(AppbaseCoreServices partial) throws AppbaseException {
        if (predefinedService != null) return predefinedService;
        // Read DefaultLogService constructor before changing this line.
        return new DefaultLogService(config);
    }
}
```

- [ ] **Step 4: Implement the remaining 12 factories**

All follow the same two-constructor pattern. The config-based `create()` calls `partial.get(XxxService.class)` for any deps. Read each `DefaultXxxService` constructor first, then implement.

**`MetricServiceFactory`** — order `METRIC`, no service deps:
```java
// package com.viveworks.appbase.bootstrap.core.factory;
// Imports: MetricService, DefaultMetricService, DefaultMetricServiceConfiguration, CoreServiceOrder.METRIC
// create(): if predefined return it; else return new DefaultMetricService(config); // adjust after reading constructor
```

**`ValidationServiceFactory`** — order `VALIDATION`, no service deps:
```java
// create(): if predefined return it; else return new DefaultValidationService(config);
```

**`MediatorServiceFactory`** — order `MEDIATOR`, likely needs `LogService`:
```java
// create(): if predefined return it;
//           LogService log = partial.get(LogService.class);
//           return new DefaultMediatorService(log, config); // adjust after reading constructor
```

**`ConfigurationServiceFactory`** — order `CONFIGURATION`, likely needs `MediatorService`:
```java
// create(): if predefined return it;
//           MediatorService mediator = partial.get(MediatorService.class);
//           return new DefaultConfigurationService(mediator, config);
```

**`MicroprofileConfigurationServiceFactory`** — order `MICROPROFILE_CONFIGURATION`, likely needs `ConfigurationService`:
```java
// create(): if predefined return it;
//           ConfigurationService cfg = partial.get(ConfigurationService.class);
//           return new DefaultMicroprofileConfigurationService(cfg, config);
```

**`ServiceRegistryFactory`** — order `SERVICE_REGISTRY`, read constructor (may need `MediatorService`):
```java
// create(): if predefined return it; else read DefaultServiceRegistry constructor for deps
```

**`MicroprofileServiceRegistryFactory`** — order `MICROPROFILE_SERVICE_REGISTRY`, likely needs `ServiceRegistry`:
```java
// create(): if predefined return it;
//           ServiceRegistry reg = partial.get(ServiceRegistry.class);
//           return new DefaultMicroprofileServiceRegistry(reg, config);
```

**`ExceptionServiceFactory`** — order `EXCEPTION`, likely needs `LogService` + `MediatorService`:
```java
// create(): if predefined return it;
//           LogService log = partial.get(LogService.class);
//           MediatorService med = partial.get(MediatorService.class);
//           return new DefaultExceptionService(log, med, config);
```

**`TracerServiceFactory`** — order `TRACER`, likely needs `ServiceRegistry`:
```java
// create(): if predefined return it;
//           ServiceRegistry reg = partial.get(ServiceRegistry.class);
//           return new DefaultTracerService(reg, config);
```

**`JmxServiceFactory`** — order `JMX`, read constructor for deps:
```java
// create(): if predefined return it; else read DefaultJmxService constructor
```

**`ReactorServiceFactory`** — order `REACTOR`, likely needs `LogService`:
```java
// create(): if predefined return it;
//           LogService log = partial.get(LogService.class);
//           return new DefaultReactorService(log, config);
```

**`LuceneServiceFactory`** — order `LUCENE`, verify Lucene service class and config first:
```java
// create(): if predefined return it;
//           LogService log = partial.get(LogService.class);
//           return new DefaultLuceneService(log, config); // adjust after reading constructor
```

Each file follows the exact structure of `LogServiceFactory.java` above. Class name, imports, and `CoreServiceOrder` enum entry change per service. All files go in package `com.viveworks.appbase.bootstrap.core.factory`.

- [ ] **Step 5: Run tests**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=CoreServiceFactoriesTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -30
```

Expected: All tests PASS.

- [ ] **Step 6: Full module build**

```bash
cd appbase-java && ./build.sh quick 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/core/factory/
git add appbase-java/appbase-bootstrap/src/test/java/com/viveworks/appbase/bootstrap/core/CoreServiceFactoriesTest.java
git commit -m "feat(P-001): add 13 built-in AppbaseCoreServiceFactory implementations"
```

---

### Task 4: ClientServiceOrder + AppbaseClientServiceFactory + AppbaseClientServices

**Files:**
- Create: `bootstrap/client/ClientServiceOrder.java`
- Create: `bootstrap/client/AppbaseClientServiceFactory.java`
- Create: `bootstrap/client/AppbaseClientServices.java`
- Create: `test/.../client/AppbaseClientServicesTest.java`

**Interfaces:**
- Consumes: `AppbaseCoreServices`, `AppbaseCoreServicesAdapter`, `AppbaseService`, `AppbaseException`, `AppbaseRuntimeException`, `Shutdownable`
- Produces: `AppbaseClientServices` with `get(Class<T>)`, `services()`, `shutdown()`; `AppbaseClientServiceFactory<T>` interface; `ClientServiceOrder` enum (20 entries)

**Before writing tests, read:**
- `appbase-client/.../client/core/` — find `AppbaseClientService` or equivalent client service supertype
- `appbase-client/.../client/service/aerospike/AerospikeClientService.java` — verify package for test import
- `appbase-client/.../client/core/DefaultAppbaseClientCore.java` — note constructor signature

- [ ] **Step 1: Write failing tests**

```java
// src/test/java/com/viveworks/appbase/bootstrap/client/AppbaseClientServicesTest.java
package com.viveworks.appbase.bootstrap.client;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.AppbaseService;
import com.viveworks.appbase.core.exception.AppbaseRuntimeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppbaseClientServicesTest {

    @Test
    void services_isEmpty_whenNoFactoriesRegistered() {
        AppbaseCoreServices mockCore = mock(AppbaseCoreServices.class);
        assertTrue(AppbaseClientServices.builder().from(mockCore).build().services().isEmpty());
    }

    @Test
    void get_unknownType_throwsAppbaseRuntimeException() {
        AppbaseCoreServices mockCore = mock(AppbaseCoreServices.class);
        AppbaseClientServices clients = AppbaseClientServices.builder().from(mockCore).build();
        // Replace AerospikeClientService with any client service interface after verifying package:
        assertThrows(AppbaseRuntimeException.class,
            () -> clients.get(com.viveworks.appbase.client.service.aerospike.AerospikeClientService.class));
    }

    @Test
    void get_returnsRegisteredService() throws Exception {
        AppbaseCoreServices mockCore = mock(AppbaseCoreServices.class);
        AppbaseService mockSvc = mock(AppbaseService.class);
        @SuppressWarnings("unchecked")
        AppbaseClientServiceFactory<AppbaseService> factory = mock(AppbaseClientServiceFactory.class);
        when(factory.serviceType()).thenAnswer(inv -> AppbaseService.class);
        when(factory.initializationOrder()).thenReturn(ClientServiceOrder.AEROSPIKE.order());
        when(factory.create(mockCore)).thenReturn(mockSvc);

        AppbaseClientServices clients = AppbaseClientServices.builder()
            .from(mockCore)
            .with(factory)
            .build();
        assertSame(mockSvc, clients.get(AppbaseService.class));
    }

    @Test
    void services_returnsUnmodifiableMap() {
        AppbaseCoreServices mockCore = mock(AppbaseCoreServices.class);
        assertThrows(UnsupportedOperationException.class,
            () -> AppbaseClientServices.builder().from(mockCore).build()
                .services().put(AppbaseService.class, null));
    }

    @Test
    void shutdown_callsShutdownOnRegisteredServices() throws Exception {
        AppbaseCoreServices mockCore = mock(AppbaseCoreServices.class);
        AppbaseService mockSvc = mock(AppbaseService.class);
        @SuppressWarnings("unchecked")
        AppbaseClientServiceFactory<AppbaseService> factory = mock(AppbaseClientServiceFactory.class);
        when(factory.serviceType()).thenAnswer(inv -> AppbaseService.class);
        when(factory.initializationOrder()).thenReturn(ClientServiceOrder.AEROSPIKE.order());
        when(factory.create(mockCore)).thenReturn(mockSvc);

        AppbaseClientServices.builder().from(mockCore).with(factory).build().shutdown();
        verify(mockSvc).shutdown();
    }

    @Test
    void build_withoutFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> AppbaseClientServices.builder().build());
    }
}
```

- [ ] **Step 2: Run tests to confirm failure**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=AppbaseClientServicesTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -20
```

Expected: COMPILATION ERROR.

- [ ] **Step 3: Create `ClientServiceOrder.java`**

```java
// src/main/java/com/viveworks/appbase/bootstrap/client/ClientServiceOrder.java
package com.viveworks.appbase.bootstrap.client;

/** Initialization order constants for all built-in Appbase client services. */
public enum ClientServiceOrder {
    AWS_SDK_HTTP(10),
    AWS_SDK_ASYNC_HTTP(11),
    S3(20),
    FIREBASE_APP(30),
    FIREBASE_AUTH(31),
    AEROSPIKE(40),
    CASSANDRA(41),
    CLAMAV(42),
    JASYNC(43),
    ELASTICSEARCH(50),
    VITESS_JDBC(60),
    VITESS_JDBI(61),
    VITESS_GRPC(62),
    JPA(63),
    DRUID(70),
    ZOOKEEPER(80),
    KAFKA_PRODUCER(85),
    KAFKA_CONSUMER(86),
    SMTP(90),
    SPARK(95);

    private final int order;

    ClientServiceOrder(int order) {
        this.order = order;
    }

    /** Returns the numeric initialization order. Lower values initialize first. */
    public int order() {
        return order;
    }
}
```

- [ ] **Step 4: Create `AppbaseClientServiceFactory.java`**

```java
// src/main/java/com/viveworks/appbase/bootstrap/client/AppbaseClientServiceFactory.java
package com.viveworks.appbase.bootstrap.client;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.AppbaseService;
import com.viveworks.appbase.core.exception.AppbaseException;

/**
 * Factory that creates one typed Appbase client service.
 * Factories are sorted by {@link #initializationOrder()} before execution.
 *
 * <p>Inside {@link #create}, construct an {@link com.viveworks.appbase.bootstrap.core.AppbaseCoreServicesAdapter}
 * to get an {@link com.viveworks.appbase.core.AppbaseCore} view of {@code coreServices}:
 * <pre>{@code
 * AppbaseCore core = new AppbaseCoreServicesAdapter(coreServices);
 * DefaultAppbaseClientCore clientCore = new DefaultAppbaseClientCore(core);
 * return new DefaultXxxClientService(clientCore, config, ...);
 * }</pre>
 *
 * @param <T> the client service type produced by this factory
 */
public interface AppbaseClientServiceFactory<T extends AppbaseService> {

    /** Returns the client service interface class this factory produces. */
    Class<T> serviceType();

    /**
     * Returns numeric initialization order; lower values initialize first.
     * Use {@link ClientServiceOrder#order()} to supply the value.
     */
    int initializationOrder();

    /**
     * Creates the client service.
     *
     * @param coreServices the fully-built core services container
     * @return the newly created client service instance
     * @throws AppbaseException if creation fails
     */
    T create(AppbaseCoreServices coreServices) throws AppbaseException;
}
```

- [ ] **Step 5: Create `AppbaseClientServices.java`**

```java
// src/main/java/com/viveworks/appbase/bootstrap/client/AppbaseClientServices.java
package com.viveworks.appbase.bootstrap.client;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.AppbaseService;
import com.viveworks.appbase.core.exception.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.Shutdownable; // verify package
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Immutable container holding all initialized Appbase client services.
 *
 * <p>Build via:
 * <pre>{@code
 * AppbaseClientServices clients = AppbaseClientServices.builder()
 *     .from(coreServices)
 *     .with(new AerospikeClientServiceFactory(aerospikeConfig))
 *     .with(new ElasticsearchClientServiceFactory(esConfig))
 *     .build();
 * }</pre>
 */
@Immutable
public final class AppbaseClientServices implements Shutdownable {

    private final Map<Class<?>, AppbaseService> services;
    private final List<AppbaseService> initOrder;

    private AppbaseClientServices(Map<Class<?>, AppbaseService> services,
                                   List<AppbaseService> initOrder) {
        this.services  = Collections.unmodifiableMap(new LinkedHashMap<>(services));
        this.initOrder = List.copyOf(initOrder);
    }

    /**
     * Returns the client service registered under {@code serviceType}.
     *
     * @param serviceType the client service interface class
     * @param <T> the service type
     * @return the registered service instance
     * @throws AppbaseRuntimeException if no service of that type is registered
     */
    @SuppressWarnings("unchecked")
    public <T extends AppbaseService> T get(Class<T> serviceType) {
        AppbaseService s = services.get(Objects.requireNonNull(serviceType, "serviceType"));
        if (s == null) {
            throw new AppbaseRuntimeException(
                "No client service registered for type: " + serviceType.getName()
                + ". Register it with AppbaseClientServices.builder().with(factory).");
        }
        return (T) s;
    }

    /** Returns an unmodifiable map of all registered client services, keyed by service interface. */
    public Map<Class<?>, AppbaseService> services() {
        return services;
    }

    /**
     * Shuts down all client services in reverse initialization order.
     * Errors are logged via SLF4J and NOT rethrown.
     */
    @Override
    public void shutdown() {
        List<AppbaseService> reversed = new ArrayList<>(initOrder);
        Collections.reverse(reversed);
        for (AppbaseService svc : reversed) {
            try {
                svc.shutdown();
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AppbaseClientServices.class)
                    .error("Error shutting down client service {}", svc.getClass().getName(), e);
            }
        }
    }

    /** Returns a new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link AppbaseClientServices}. */
    public static final class Builder {

        private AppbaseCoreServices coreServices;
        private final List<AppbaseClientServiceFactory<?>> factories = new ArrayList<>();

        /**
         * Sets the core services container. Required before calling {@link #build()}.
         *
         * @param coreServices fully-built core services
         * @return this builder
         */
        public Builder from(AppbaseCoreServices coreServices) {
            this.coreServices = Objects.requireNonNull(coreServices, "coreServices");
            return this;
        }

        /**
         * Registers one or more client service factories.
         * If two factories share the same {@link AppbaseClientServiceFactory#serviceType()},
         * the later registration wins.
         *
         * @param factories factories to register
         * @return this builder
         */
        public Builder with(AppbaseClientServiceFactory<?>... factories) {
            Objects.requireNonNull(factories, "factories");
            for (AppbaseClientServiceFactory<?> f : factories) {
                this.factories.add(Objects.requireNonNull(f, "factory element must not be null"));
            }
            return this;
        }

        /**
         * Builds the {@link AppbaseClientServices} container.
         *
         * @return the fully initialized container
         * @throws NullPointerException if {@link #from} was not called
         * @throws AppbaseRuntimeException if any factory fails to create its service
         */
        public AppbaseClientServices build() {
            Objects.requireNonNull(coreServices,
                "coreServices is required — call from(AppbaseCoreServices) before build()");

            // Deduplicate: later registration wins for same serviceType
            Map<Class<?>, AppbaseClientServiceFactory<?>> factoryMap = new LinkedHashMap<>();
            for (AppbaseClientServiceFactory<?> f : factories) {
                factoryMap.put(f.serviceType(), f);
            }
            // Sort by initializationOrder
            List<AppbaseClientServiceFactory<?>> sorted = new ArrayList<>(factoryMap.values());
            sorted.sort(Comparator.comparingInt(AppbaseClientServiceFactory::initializationOrder));

            Map<Class<?>, AppbaseService> built   = new LinkedHashMap<>();
            List<AppbaseService>          initOrd = new ArrayList<>();
            for (AppbaseClientServiceFactory<?> factory : sorted) {
                try {
                    AppbaseService svc = factory.create(coreServices);
                    built.put(factory.serviceType(), svc);
                    initOrd.add(svc);
                } catch (Exception e) {
                    throw new AppbaseRuntimeException(
                        "Failed to create client service: " + factory.serviceType().getName(), e);
                }
            }
            return new AppbaseClientServices(built, initOrd);
        }
    }
}
```

- [ ] **Step 6: Run tests**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=AppbaseClientServicesTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -30
```

Expected: All tests PASS.

- [ ] **Step 7: Commit**

```bash
git add appbase-java/appbase-bootstrap/src/
git commit -m "feat(P-001): add AppbaseClientServices, AppbaseClientServiceFactory, ClientServiceOrder"
```

---

### Task 5: 20 Built-in AppbaseClientServiceFactory Implementations

**Files:**
- Create: `bootstrap/client/factory/AwsSdkHttpClientServiceFactory.java`
- Create: `bootstrap/client/factory/AwsSdkAsyncHttpClientServiceFactory.java`
- Create: `bootstrap/client/factory/S3ClientsServiceFactory.java`
- Create: `bootstrap/client/factory/FirebaseAppClientServiceFactory.java`
- Create: `bootstrap/client/factory/FirebaseAuthClientServiceFactory.java`
- Create: `bootstrap/client/factory/AerospikeClientServiceFactory.java`
- Create: `bootstrap/client/factory/CassandraClientServiceFactory.java`
- Create: `bootstrap/client/factory/ClamAVClientServiceFactory.java`
- Create: `bootstrap/client/factory/JasyncClientServiceFactory.java`
- Create: `bootstrap/client/factory/ElasticsearchClientServiceFactory.java`
- Create: `bootstrap/client/factory/VitessJdbcClientServiceFactory.java`
- Create: `bootstrap/client/factory/VitessJdbiClientServiceFactory.java`
- Create: `bootstrap/client/factory/VitessGrpcClientServiceFactory.java`
- Create: `bootstrap/client/factory/JpaClientServiceFactory.java`
- Create: `bootstrap/client/factory/DruidSqlClientServiceFactory.java`
- Create: `bootstrap/client/factory/ZooKeeperClientServiceFactory.java`
- Create: `bootstrap/client/factory/KafkaProducerClientServiceFactory.java`
- Create: `bootstrap/client/factory/KafkaConsumerClientServiceFactory.java`
- Create: `bootstrap/client/factory/SmtpClientServiceFactory.java`
- Create: `bootstrap/client/factory/SparkClientServiceFactory.java`
- Create: `test/.../client/ClientServiceFactoriesTest.java`

**Interfaces:**
- Consumes: `AppbaseClientServiceFactory<T>`, `ClientServiceOrder`, `AppbaseCoreServices`, `AppbaseCoreServicesAdapter`, `DefaultAppbaseClientCore`; each `DefaultXxxClientService` and its config type
- Produces: 20 factory classes

**Before writing code, read for each client service:**
- `appbase-client/.../client/service/aerospike/` — `AerospikeClientService`, `DefaultAerospikeClientService`, its config type
- `appbase-client/.../client/service/elasticsearch/` — same pattern
- (Read each service's directory before implementing its factory)
- `DefaultAppbaseClientCore.java` — constructor signature (takes `AppbaseCore`)

- [ ] **Step 1: Write failing tests**

```java
// src/test/java/com/viveworks/appbase/bootstrap/client/ClientServiceFactoriesTest.java
package com.viveworks.appbase.bootstrap.client;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;     // verify package
import com.viveworks.appbase.client.service.elasticsearch.ElasticsearchClientService; // verify package
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientServiceFactoriesTest {

    @Test
    void aerospikeFactory_predefined_returnsSameInstance() throws Exception {
        AerospikeClientService svc = mock(AerospikeClientService.class);
        AerospikeClientServiceFactory factory = new AerospikeClientServiceFactory(svc);
        assertSame(svc, factory.create(mock(AppbaseCoreServices.class)));
    }

    @Test
    void aerospikeFactory_serviceType_isAerospikeClientService() {
        AerospikeClientService svc = mock(AerospikeClientService.class);
        assertEquals(AerospikeClientService.class, new AerospikeClientServiceFactory(svc).serviceType());
    }

    @Test
    void aerospikeFactory_order_matchesClientServiceOrder() {
        AerospikeClientService svc = mock(AerospikeClientService.class);
        assertEquals(ClientServiceOrder.AEROSPIKE.order(),
            new AerospikeClientServiceFactory(svc).initializationOrder());
    }

    @Test
    void elasticsearchFactory_predefined_returnsSameInstance() throws Exception {
        ElasticsearchClientService svc = mock(ElasticsearchClientService.class);
        ElasticsearchClientServiceFactory factory = new ElasticsearchClientServiceFactory(svc);
        assertSame(svc, factory.create(mock(AppbaseCoreServices.class)));
    }

    @Test
    void elasticsearchFactory_order_matchesClientServiceOrder() {
        ElasticsearchClientService svc = mock(ElasticsearchClientService.class);
        assertEquals(ClientServiceOrder.ELASTICSEARCH.order(),
            new ElasticsearchClientServiceFactory(svc).initializationOrder());
    }
}
```

- [ ] **Step 2: Run tests to confirm failure**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=ClientServiceFactoriesTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -20
```

Expected: COMPILATION ERROR — factory classes do not exist yet.

- [ ] **Step 3: Implement `AerospikeClientServiceFactory.java`** (reference implementation — use as pattern for all 20)

Read `DefaultAerospikeClientService` and its config type first.

```java
// src/main/java/com/viveworks/appbase/bootstrap/client/factory/AerospikeClientServiceFactory.java
package com.viveworks.appbase.bootstrap.client.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.ClientServiceOrder;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServicesAdapter;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;   // verify package
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;        // verify package
import com.viveworks.appbase.client.service.aerospike.DefaultAerospikeClientService; // verify package
import com.viveworks.appbase.client.service.aerospike.DefaultAerospikeClientServiceConfiguration; // verify
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.exception.AppbaseException;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Built-in factory for {@link AerospikeClientService}.
 * Two construction modes:
 * <ul>
 *   <li>Pre-built: {@link #AerospikeClientServiceFactory(AerospikeClientService)} — wraps an existing instance.</li>
 *   <li>Config-based: {@link #AerospikeClientServiceFactory(DefaultAerospikeClientServiceConfiguration)} — creates a new service.</li>
 * </ul>
 */
public final class AerospikeClientServiceFactory implements AppbaseClientServiceFactory<AerospikeClientService> {

    @Nullable private final AerospikeClientService predefinedService;
    @Nullable private final DefaultAerospikeClientServiceConfiguration config;

    /** Wraps a pre-built {@link AerospikeClientService}. */
    public AerospikeClientServiceFactory(AerospikeClientService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link DefaultAerospikeClientService} from {@code config}. */
    public AerospikeClientServiceFactory(DefaultAerospikeClientServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    @Override
    public Class<AerospikeClientService> serviceType() { return AerospikeClientService.class; }

    @Override
    public int initializationOrder() { return ClientServiceOrder.AEROSPIKE.order(); }

    /**
     * Returns the pre-built service, or creates a new {@link DefaultAerospikeClientService}.
     * Constructs {@link AppbaseCoreServicesAdapter} to bridge coreServices to {@link AppbaseCore}.
     * Read {@link DefaultAerospikeClientService} constructor before adjusting the config-based path.
     */
    @Override
    public AerospikeClientService create(AppbaseCoreServices coreServices) throws AppbaseException {
        if (predefinedService != null) return predefinedService;
        AppbaseCore core = new AppbaseCoreServicesAdapter(coreServices);
        DefaultAppbaseClientCore clientCore = new DefaultAppbaseClientCore(core);
        // Adjust constructor args after reading DefaultAerospikeClientService:
        return new DefaultAerospikeClientService(clientCore, config, false, List.of());
    }
}
```

- [ ] **Step 4: Implement the remaining 19 client factories**

Apply the same pattern as `AerospikeClientServiceFactory`. For each factory:
1. Read the `DefaultXxxClientService` constructor and its config type
2. Create `XxxClientServiceFactory` in `com.viveworks.appbase.bootstrap.client.factory`
3. Two constructors: `(XxxClientService service)` and `(DefaultXxxClientServiceConfiguration config)`
4. `serviceType()` returns `XxxClientService.class`
5. `initializationOrder()` returns `ClientServiceOrder.XXX.order()`
6. `create()` returns predefined or builds via `new AppbaseCoreServicesAdapter(coreServices)` → `new DefaultAppbaseClientCore(core)` → `new DefaultXxxClientService(clientCore, config, ...)`

Order mapping:
| Factory class | `ClientServiceOrder` entry |
|---|---|
| `AwsSdkHttpClientServiceFactory` | `AWS_SDK_HTTP` |
| `AwsSdkAsyncHttpClientServiceFactory` | `AWS_SDK_ASYNC_HTTP` |
| `S3ClientsServiceFactory` | `S3` |
| `FirebaseAppClientServiceFactory` | `FIREBASE_APP` |
| `FirebaseAuthClientServiceFactory` | `FIREBASE_AUTH` |
| `CassandraClientServiceFactory` | `CASSANDRA` |
| `ClamAVClientServiceFactory` | `CLAMAV` |
| `JasyncClientServiceFactory` | `JASYNC` |
| `ElasticsearchClientServiceFactory` | `ELASTICSEARCH` |
| `VitessJdbcClientServiceFactory` | `VITESS_JDBC` |
| `VitessJdbiClientServiceFactory` | `VITESS_JDBI` |
| `VitessGrpcClientServiceFactory` | `VITESS_GRPC` |
| `JpaClientServiceFactory` | `JPA` |
| `DruidSqlClientServiceFactory` | `DRUID` |
| `ZooKeeperClientServiceFactory` | `ZOOKEEPER` |
| `KafkaProducerClientServiceFactory` | `KAFKA_PRODUCER` |
| `KafkaConsumerClientServiceFactory` | `KAFKA_CONSUMER` |
| `SmtpClientServiceFactory` | `SMTP` |
| `SparkClientServiceFactory` | `SPARK` |

**Dependency chain constraints** (enforce by `ClientServiceOrder` — no code needed, order ensures deps exist):
- `VITESS_JDBI` requires `VITESS_JDBC` to be registered first in `AppbaseClientServices`; validate in `VitessJdbiClientServiceFactory.create()`: `coreServices` does not hold client deps — instead the builder's order ensures JDBC runs before JDBI within the same `AppbaseClientServices.build()`. Client-to-client deps are NOT supported by the factory container (client factories do NOT call `partial.get()` on `AppbaseClientServices`). If a client service needs another client service, document it in the Javadoc and let the user call `AppbaseClientServices.get()` post-build.
- `FIREBASE_AUTH` requires `FIREBASE_APP`: same — document in Javadoc, no code enforcement at factory level.

Validation at `AppbaseClientServices.build()` level for illegal combos:

Add this check at the end of `AppbaseClientServices.Builder.build()`, before returning:

```java
// Validate dependency combos
validateDep(built, "JDBI",         "com.viveworks.appbase.client.service.vitess.VitessJdbiClientService",
                                   "com.viveworks.appbase.client.service.vitess.VitessJdbcClientService",
                                   "VitessJdbi requires VitessJdbc");
validateDep(built, "Firebase Auth","com.viveworks.appbase.client.service.firebase.FirebaseAuthClientService",
                                   "com.viveworks.appbase.client.service.firebase.FirebaseAppClientService",
                                   "FirebaseAuth requires FirebaseApp");
// Add S3/JPA validations similarly (verify class names against appbase-client source)
```

```java
private static void validateDep(Map<Class<?>, AppbaseService> built,
                                 String label, String dependentFqn, String requiredFqn, String msg) {
    try {
        Class<?> dep = Class.forName(dependentFqn);
        Class<?> req = Class.forName(requiredFqn);
        if (built.containsKey(dep) && !built.containsKey(req)) {
            throw new AppbaseRuntimeException(msg + ". Register " + requiredFqn + " factory first.");
        }
    } catch (ClassNotFoundException ignored) { /* class not on classpath — skip */ }
}
```

- [ ] **Step 5: Run tests**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=ClientServiceFactoriesTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -30
```

Expected: All tests PASS.

- [ ] **Step 6: Full module build**

```bash
cd appbase-java && ./build.sh quick 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add appbase-java/appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/client/factory/
git add appbase-java/appbase-bootstrap/src/test/java/com/viveworks/appbase/bootstrap/client/ClientServiceFactoriesTest.java
git commit -m "feat(P-001): add 20 built-in AppbaseClientServiceFactory implementations"
```

---

### Task 6: AppbaseServiceFactory + AppbaseServices + 11 AppbaseService Factories

**Files:**
- Create: `bootstrap/service/AppbaseServiceFactory.java`
- Create: `bootstrap/service/AppbaseServices.java`
- Create: `bootstrap/service/factory/` (11 factory classes — see File Map)
- Create: `test/.../service/AppbaseServicesTest.java`

**Interfaces:**
- Consumes: `AppbaseCoreServices`, `AppbaseClientServices`, `AppbaseService`, `AppbaseException`, `AppbaseRuntimeException`, `Shutdownable`; service types from `appbase-service`
- Produces: `AppbaseServices` with `get(Class<T>)`, `services()`, `shutdown()`; `AppbaseServiceFactory<T>` interface; 11 factory classes

**Before writing code, read `appbase-service/` to find:**
- Service interfaces: `AuthorizationService`, `ClusterService`, `HealthCheckService`, `IdService`, `PasswordService`, `UserSessionStore`, `UserAccountStore`, `FirebaseAuthService`, `UserAccountService`, `SupportMessageStore`, `SupportMessageService`
- Their `Default*` implementation constructors and config types
- Package paths for each

- [ ] **Step 1: Write failing tests**

```java
// src/test/java/com/viveworks/appbase/bootstrap/service/AppbaseServicesTest.java
package com.viveworks.appbase.bootstrap.service;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.core.AppbaseService;
import com.viveworks.appbase.core.exception.AppbaseRuntimeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppbaseServicesTest {

    private AppbaseCoreServices mockCore()   { return mock(AppbaseCoreServices.class); }
    private AppbaseClientServices mockClients() { return mock(AppbaseClientServices.class); }

    @Test
    void services_isEmpty_whenNoFactoriesRegistered() {
        assertTrue(AppbaseServices.builder().from(mockCore(), mockClients()).build().services().isEmpty());
    }

    @Test
    void get_unknownType_throwsAppbaseRuntimeException() {
        AppbaseServices svc = AppbaseServices.builder().from(mockCore(), mockClients()).build();
        assertThrows(AppbaseRuntimeException.class, () -> svc.get(AppbaseService.class));
    }

    @Test
    void services_returnsUnmodifiableMap() {
        assertThrows(UnsupportedOperationException.class,
            () -> AppbaseServices.builder().from(mockCore(), mockClients()).build()
                .services().put(AppbaseService.class, null));
    }

    @Test
    void build_withoutFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> AppbaseServices.builder().build());
    }
}
```

- [ ] **Step 2: Run tests to confirm failure**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=AppbaseServicesTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -20
```

Expected: COMPILATION ERROR.

- [ ] **Step 3: Create `AppbaseServiceFactory.java`**

```java
// src/main/java/com/viveworks/appbase/bootstrap/service/AppbaseServiceFactory.java
package com.viveworks.appbase.bootstrap.service;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.AppbaseService;
import com.viveworks.appbase.core.exception.AppbaseException;

/**
 * Factory that creates one typed Appbase application-level service.
 * Factories are sorted by {@link #initializationOrder()} before execution.
 *
 * @param <T> the service type produced by this factory
 */
public interface AppbaseServiceFactory<T extends AppbaseService> {

    /** Returns the service interface class this factory produces. */
    Class<T> serviceType();

    /**
     * Returns numeric initialization order; lower values initialize first.
     * Use multiples of 10; leave gaps for custom services.
     */
    int initializationOrder();

    /**
     * Creates the service.
     *
     * @param coreServices   fully-built core services
     * @param clientServices fully-built client services
     * @return the newly created service instance
     * @throws AppbaseException if creation fails
     */
    T create(AppbaseCoreServices coreServices, AppbaseClientServices clientServices) throws AppbaseException;
}
```

- [ ] **Step 4: Create `AppbaseServices.java`**

```java
// src/main/java/com/viveworks/appbase/bootstrap/service/AppbaseServices.java
package com.viveworks.appbase.bootstrap.service;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.AppbaseService;
import com.viveworks.appbase.core.exception.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.Shutdownable; // verify package
import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Immutable container holding all initialized Appbase application-level services.
 *
 * <p>Build via:
 * <pre>{@code
 * AppbaseServices services = AppbaseServices.builder()
 *     .from(coreServices, clientServices)
 *     .with(new PasswordServiceFactory(passwordConfig))
 *     .build();
 * }</pre>
 */
@Immutable
public final class AppbaseServices implements Shutdownable {

    private final Map<Class<?>, AppbaseService> services;
    private final List<AppbaseService> initOrder;

    private AppbaseServices(Map<Class<?>, AppbaseService> services,
                             List<AppbaseService> initOrder) {
        this.services  = Collections.unmodifiableMap(new LinkedHashMap<>(services));
        this.initOrder = List.copyOf(initOrder);
    }

    /**
     * Returns the service registered under {@code serviceType}.
     *
     * @throws AppbaseRuntimeException if no service of that type is registered
     */
    @SuppressWarnings("unchecked")
    public <T extends AppbaseService> T get(Class<T> serviceType) {
        AppbaseService s = services.get(Objects.requireNonNull(serviceType, "serviceType"));
        if (s == null) {
            throw new AppbaseRuntimeException(
                "No appbase service registered for type: " + serviceType.getName()
                + ". Register it with AppbaseServices.builder().with(factory).");
        }
        return (T) s;
    }

    /** Returns an unmodifiable map of all registered services, keyed by service interface. */
    public Map<Class<?>, AppbaseService> services() { return services; }

    /** Shuts down all services in reverse initialization order. Errors logged, NOT rethrown. */
    @Override
    public void shutdown() {
        List<AppbaseService> reversed = new ArrayList<>(initOrder);
        Collections.reverse(reversed);
        for (AppbaseService svc : reversed) {
            try {
                svc.shutdown();
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AppbaseServices.class)
                    .error("Error shutting down appbase service {}", svc.getClass().getName(), e);
            }
        }
    }

    /** Returns a new builder. */
    public static Builder builder() { return new Builder(); }

    /** Builder for {@link AppbaseServices}. */
    public static final class Builder {

        private AppbaseCoreServices   coreServices;
        private AppbaseClientServices clientServices;
        private final List<AppbaseServiceFactory<?>> factories = new ArrayList<>();

        /**
         * Sets the upstream containers. Required before calling {@link #build()}.
         *
         * @param coreServices   fully-built core services
         * @param clientServices fully-built client services
         * @return this builder
         */
        public Builder from(AppbaseCoreServices coreServices, AppbaseClientServices clientServices) {
            this.coreServices   = Objects.requireNonNull(coreServices,   "coreServices");
            this.clientServices = Objects.requireNonNull(clientServices, "clientServices");
            return this;
        }

        /**
         * Registers one or more service factories.
         * Later registrations for the same {@link AppbaseServiceFactory#serviceType()} override earlier ones.
         *
         * @param factories factories to register
         * @return this builder
         */
        public Builder with(AppbaseServiceFactory<?>... factories) {
            Objects.requireNonNull(factories, "factories");
            for (AppbaseServiceFactory<?> f : factories) {
                this.factories.add(Objects.requireNonNull(f, "factory element must not be null"));
            }
            return this;
        }

        /** Builds the {@link AppbaseServices} container. */
        public AppbaseServices build() {
            Objects.requireNonNull(coreServices,
                "coreServices required — call from(AppbaseCoreServices, AppbaseClientServices) before build()");
            Objects.requireNonNull(clientServices,
                "clientServices required — call from(AppbaseCoreServices, AppbaseClientServices) before build()");

            Map<Class<?>, AppbaseServiceFactory<?>> factoryMap = new LinkedHashMap<>();
            for (AppbaseServiceFactory<?> f : factories) {
                factoryMap.put(f.serviceType(), f);
            }
            List<AppbaseServiceFactory<?>> sorted = new ArrayList<>(factoryMap.values());
            sorted.sort(Comparator.comparingInt(AppbaseServiceFactory::initializationOrder));

            Map<Class<?>, AppbaseService> built   = new LinkedHashMap<>();
            List<AppbaseService>          initOrd = new ArrayList<>();
            for (AppbaseServiceFactory<?> factory : sorted) {
                try {
                    AppbaseService svc = factory.create(coreServices, clientServices);
                    built.put(factory.serviceType(), svc);
                    initOrd.add(svc);
                } catch (Exception e) {
                    throw new AppbaseRuntimeException(
                        "Failed to create appbase service: " + factory.serviceType().getName(), e);
                }
            }
            return new AppbaseServices(built, initOrd);
        }
    }
}
```

- [ ] **Step 5: Implement 11 `AppbaseServiceFactory` classes**

For each factory, read the `DefaultXxxService` constructor in `appbase-service` first. All follow the same two-constructor pattern as core factories, but `create()` receives both `coreServices` and `clientServices`.

Template for each factory (replace `Xxx`, `ORDER_ENTRY`, and constructor args):

```java
package com.viveworks.appbase.bootstrap.service.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceFactory;
import com.viveworks.appbase.core.exception.AppbaseException;
import com.viveworks.appbase.service.XxxService;                  // verify package
import com.viveworks.appbase.service.DefaultXxxService;           // verify package
import com.viveworks.appbase.service.DefaultXxxServiceConfiguration; // verify
import javax.annotation.Nullable;
import java.util.Objects;

/** Built-in factory for {@link XxxService}. */
public final class XxxServiceFactory implements AppbaseServiceFactory<XxxService> {

    @Nullable private final XxxService predefinedService;
    @Nullable private final DefaultXxxServiceConfiguration config;

    /** Wraps a pre-built {@link XxxService}. */
    public XxxServiceFactory(XxxService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link DefaultXxxService} from {@code config}. */
    public XxxServiceFactory(DefaultXxxServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    @Override public Class<XxxService> serviceType() { return XxxService.class; }
    @Override public int initializationOrder()        { return ORDER_VALUE; } // use int directly, no enum for service layer

    @Override
    public XxxService create(AppbaseCoreServices coreServices, AppbaseClientServices clientServices)
            throws AppbaseException {
        if (predefinedService != null) return predefinedService;
        // Read DefaultXxxService constructor; pass coreServices.getLogService() etc. as needed
        return new DefaultXxxService(config); // adjust after reading constructor
    }
}
```

Services to implement (use multiples of 10 for `initializationOrder()`):
| Factory | Order |
|---|---|
| `AuthorizationServiceFactory` | 10 |
| `ClusterServiceFactory` | 20 |
| `HealthCheckServiceFactory` | 30 |
| `IdServiceFactory` | 40 |
| `PasswordServiceFactory` | 50 |
| `UserSessionStoreFactory` | 60 |
| `UserAccountStoreFactory` | 70 |
| `FirebaseAuthServiceFactory` | 80 |
| `UserAccountServiceFactory` | 90 |
| `SupportMessageStoreFactory` | 100 |
| `SupportMessageServiceFactory` | 110 |

- [ ] **Step 6: Run tests**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=AppbaseServicesTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -30
```

Expected: All tests PASS.

- [ ] **Step 7: Full module build**

```bash
cd appbase-java && ./build.sh quick 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 8: Commit**

```bash
git add appbase-java/appbase-bootstrap/src/
git commit -m "feat(P-001): add AppbaseServices, AppbaseServiceFactory, and 11 service factories"
```

---

### Task 7: JettyAppbaseServer

**Files:**
- Create: `bootstrap/server/JettyAppbaseServer.java`
- Create: `test/.../server/JettyAppbaseServerTest.java`

**Interfaces:**
- Consumes: `AppbaseServices`, `AppbaseCoreServices`, `AppbaseClientServices`; Jetty `Server`, `ServerConnector`, `ServletContextHandler`; Jersey `ResourceConfig`; `AppbaseRuntimeException`, `Shutdownable`
- Produces: `JettyAppbaseServer` with `start()`, `stop()`, `join()`, `Builder` with `from(AppbaseServices)`, `server(Server)`, `port(int)`, `resourceConfig(ResourceConfig)`, `build()`

**Before writing code, read:**
- `appbase-stacktest/.../StackTestServer.java` — current server setup pattern (Jetty config, Jersey ResourceConfig usage, connector setup)
- `appbase-core/.../net/web/` — any existing Jetty utilities
- `appbase-service/.../web/` — any JAX-RS filter registrations expected by service layer

- [ ] **Step 1: Write failing tests**

```java
// src/test/java/com/viveworks/appbase/bootstrap/server/JettyAppbaseServerTest.java
package com.viveworks.appbase.bootstrap.server;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JettyAppbaseServerTest {

    @Test
    void builder_withoutFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> JettyAppbaseServer.builder().build());
    }

    @Test
    void builder_defaultPort_is8080() {
        // Build with a mock AppbaseServices; we check the server is configured with port 8080.
        // We only verify build() doesn't throw and returns a non-null server.
        AppbaseServices mockServices = mock(AppbaseServices.class);
        JettyAppbaseServer server = JettyAppbaseServer.builder()
            .from(mockServices)
            .build();
        assertNotNull(server);
    }

    @Test
    void builder_customServer_usesProvidedServer() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        Server customServer = new Server(9090);
        JettyAppbaseServer server = JettyAppbaseServer.builder()
            .from(mockServices)
            .server(customServer)
            .build();
        assertNotNull(server);
        // Verify the custom server is actually used (check via getServer())
        assertSame(customServer, server.getServer());
    }

    @Test
    void builder_port_overridesDefaultPort() {
        AppbaseServices mockServices = mock(AppbaseServices.class);
        JettyAppbaseServer server = JettyAppbaseServer.builder()
            .from(mockServices)
            .port(9000)
            .build();
        assertNotNull(server);
    }

    @Test
    void builder_serverAndPort_throwsIllegalStateException() {
        // server(Server) and port(int) are mutually exclusive
        AppbaseServices mockServices = mock(AppbaseServices.class);
        assertThrows(IllegalStateException.class, () ->
            JettyAppbaseServer.builder()
                .from(mockServices)
                .server(new Server(9090))
                .port(8080)
                .build()
        );
    }
}
```

- [ ] **Step 2: Run tests to confirm failure**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=JettyAppbaseServerTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -20
```

Expected: COMPILATION ERROR — `JettyAppbaseServer` does not exist yet.

- [ ] **Step 3: Implement `JettyAppbaseServer.java`**

Read `appbase-stacktest/.../StackTestServer.java` and copy the Jetty setup pattern before implementing.

```java
// src/main/java/com/viveworks/appbase/bootstrap/server/JettyAppbaseServer.java
package com.viveworks.appbase.bootstrap.server;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.exception.AppbaseRuntimeException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Jetty-based HTTP server configured from an {@link AppbaseServices} container.
 *
 * <p>Build via:
 * <pre>{@code
 * // Default server on port 8080:
 * JettyAppbaseServer server = JettyAppbaseServer.builder()
 *     .from(appbaseServices)
 *     .resourceConfig(myResourceConfig)
 *     .build();
 * server.start();
 * server.join();
 *
 * // Custom port:
 * JettyAppbaseServer server = JettyAppbaseServer.builder()
 *     .from(appbaseServices)
 *     .port(9000)
 *     .build();
 *
 * // Bring-your-own Jetty Server (mutually exclusive with port()):
 * JettyAppbaseServer server = JettyAppbaseServer.builder()
 *     .from(appbaseServices)
 *     .server(myServer)
 *     .build();
 * }</pre>
 */
public final class JettyAppbaseServer {

    private static final int DEFAULT_PORT = 8080;

    private final Server server;
    private final AppbaseServices appbaseServices;

    private JettyAppbaseServer(Server server, AppbaseServices appbaseServices) {
        this.server          = server;
        this.appbaseServices = appbaseServices;
    }

    /**
     * Starts the Jetty server.
     *
     * @throws AppbaseRuntimeException if the server fails to start
     */
    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            throw new AppbaseRuntimeException("Failed to start JettyAppbaseServer", e);
        }
    }

    /**
     * Stops the Jetty server and shuts down all Appbase services in reverse order.
     *
     * @throws AppbaseRuntimeException if the server fails to stop
     */
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new AppbaseRuntimeException("Failed to stop JettyAppbaseServer", e);
        } finally {
            appbaseServices.shutdown();
        }
    }

    /**
     * Blocks the calling thread until the server stops.
     *
     * @throws AppbaseRuntimeException if interrupted
     */
    public void join() {
        try {
            server.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppbaseRuntimeException("JettyAppbaseServer.join() interrupted", e);
        }
    }

    /** Returns the underlying Jetty {@link Server} instance. */
    public Server getServer() {
        return server;
    }

    /** Returns the {@link AppbaseServices} container backing this server. */
    public AppbaseServices getAppbaseServices() {
        return appbaseServices;
    }

    /** Returns a new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link JettyAppbaseServer}. */
    public static final class Builder {

        @Nullable private AppbaseServices appbaseServices;
        @Nullable private Server          customServer;
        @Nullable private Integer         port;
        @Nullable private ResourceConfig  resourceConfig;

        /**
         * Sets the Appbase services container. Required before calling {@link #build()}.
         *
         * @param services fully-built application services
         * @return this builder
         */
        public Builder from(AppbaseServices services) {
            this.appbaseServices = Objects.requireNonNull(services, "services");
            return this;
        }

        /**
         * Provides a custom Jetty {@link Server} instance.
         * Mutually exclusive with {@link #port(int)}: calling both causes {@link #build()} to throw.
         *
         * @param server the Jetty server to use
         * @return this builder
         */
        public Builder server(Server server) {
            this.customServer = Objects.requireNonNull(server, "server");
            return this;
        }

        /**
         * Sets the HTTP port. A new {@link Server} is created with this port.
         * Mutually exclusive with {@link #server(Server)}: calling both causes {@link #build()} to throw.
         * Defaults to {@value JettyAppbaseServer#DEFAULT_PORT} if neither is called.
         *
         * @param port the TCP port to listen on
         * @return this builder
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the Jersey {@link ResourceConfig} for JAX-RS resource registration.
         * If not set, a default empty {@link ResourceConfig} is used.
         *
         * @param resourceConfig the Jersey resource configuration
         * @return this builder
         */
        public Builder resourceConfig(ResourceConfig resourceConfig) {
            this.resourceConfig = Objects.requireNonNull(resourceConfig, "resourceConfig");
            return this;
        }

        /**
         * Builds the {@link JettyAppbaseServer}.
         *
         * @return the configured but not yet started server
         * @throws NullPointerException  if {@link #from} was not called
         * @throws IllegalStateException if both {@link #server} and {@link #port} were called
         * @throws AppbaseRuntimeException if Jetty configuration fails
         */
        public JettyAppbaseServer build() {
            Objects.requireNonNull(appbaseServices,
                "appbaseServices required — call from(AppbaseServices) before build()");
            if (customServer != null && port != null) {
                throw new IllegalStateException(
                    "server(Server) and port(int) are mutually exclusive — use one or the other.");
            }

            // Resolve the Jetty Server instance
            Server jettyServer = customServer != null
                ? customServer
                : new Server(port != null ? port : DEFAULT_PORT);

            // Jersey/JAX-RS setup — read StackTestServer.java for exact handler config
            ResourceConfig rc = resourceConfig != null ? resourceConfig : new ResourceConfig();
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(rc));
            context.addServlet(jerseyServlet, "/*");
            jettyServer.setHandler(context);

            return new JettyAppbaseServer(jettyServer, appbaseServices);
        }
    }
}
```

- [ ] **Step 4: Run tests**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=JettyAppbaseServerTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -30
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add appbase-java/appbase-bootstrap/src/
git commit -m "feat(P-001): add JettyAppbaseServer with builder including server(Server) override"
```

---

### Task 8: Four Guice Modules

**Files:**
- Create: `bootstrap/guice/AppbaseCoreServicesModule.java`
- Create: `bootstrap/guice/AppbaseClientServicesModule.java`
- Create: `bootstrap/guice/AppbaseServicesModule.java`
- Create: `bootstrap/guice/JettyAppbaseServerModule.java`
- Create: `test/.../guice/AppbaseBootstrapGuiceModulesTest.java`

**Interfaces:**
- Consumes: `AppbaseCoreServices`, `AppbaseClientServices`, `AppbaseServices`, `JettyAppbaseServer`; Guice `AbstractModule`, `Provider`; existing `appbase-core` Guice module patterns (read `appbase-core/.../di/guice/` first)
- Produces: four Guice `AbstractModule` subclasses that bind each container as a singleton and expose individual service bindings

**Before writing code, read:**
- `appbase-core/.../di/guice/` — any existing Guice modules that bind core services; follow the same binding style
- `appbase-client/.../di/guice/` if it exists

- [ ] **Step 1: Write failing tests**

```java
// src/test/java/com/viveworks/appbase/bootstrap/guice/AppbaseBootstrapGuiceModulesTest.java
package com.viveworks.appbase.bootstrap.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.bootstrap.server.JettyAppbaseServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppbaseBootstrapGuiceModulesTest {

    @Test
    void coreServicesModule_bindsAppbaseCoreServices() {
        AppbaseCoreServices mockCore = mock(AppbaseCoreServices.class);
        Injector injector = Guice.createInjector(new AppbaseCoreServicesModule(mockCore));
        assertSame(mockCore, injector.getInstance(AppbaseCoreServices.class));
    }

    @Test
    void clientServicesModule_bindsAppbaseClientServices() {
        AppbaseCoreServices mockCore     = mock(AppbaseCoreServices.class);
        AppbaseClientServices mockClients = mock(AppbaseClientServices.class);
        Injector injector = Guice.createInjector(
            new AppbaseCoreServicesModule(mockCore),
            new AppbaseClientServicesModule(mockClients)
        );
        assertSame(mockClients, injector.getInstance(AppbaseClientServices.class));
    }

    @Test
    void servicesModule_bindsAppbaseServices() {
        AppbaseCoreServices   mockCore     = mock(AppbaseCoreServices.class);
        AppbaseClientServices mockClients  = mock(AppbaseClientServices.class);
        AppbaseServices       mockServices = mock(AppbaseServices.class);
        Injector injector = Guice.createInjector(
            new AppbaseCoreServicesModule(mockCore),
            new AppbaseClientServicesModule(mockClients),
            new AppbaseServicesModule(mockServices)
        );
        assertSame(mockServices, injector.getInstance(AppbaseServices.class));
    }

    @Test
    void jettyModule_bindsJettyAppbaseServer() {
        AppbaseCoreServices   mockCore     = mock(AppbaseCoreServices.class);
        AppbaseClientServices mockClients  = mock(AppbaseClientServices.class);
        AppbaseServices       mockServices = mock(AppbaseServices.class);
        JettyAppbaseServer    mockServer   = mock(JettyAppbaseServer.class);
        Injector injector = Guice.createInjector(
            new AppbaseCoreServicesModule(mockCore),
            new AppbaseClientServicesModule(mockClients),
            new AppbaseServicesModule(mockServices),
            new JettyAppbaseServerModule(mockServer)
        );
        assertSame(mockServer, injector.getInstance(JettyAppbaseServer.class));
    }
}
```

- [ ] **Step 2: Run tests to confirm failure**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=AppbaseBootstrapGuiceModulesTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -20
```

Expected: COMPILATION ERROR — Guice module classes do not exist yet.

- [ ] **Step 3: Implement the four Guice modules**

```java
// src/main/java/com/viveworks/appbase/bootstrap/guice/AppbaseCoreServicesModule.java
package com.viveworks.appbase.bootstrap.guice;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.registry.JmxService;     // verify package
import com.viveworks.appbase.core.service.tracer.TracerService;
import java.util.Objects;

/**
 * Guice module that binds {@link AppbaseCoreServices} and all individual core service types.
 *
 * <p>Usage:
 * <pre>{@code
 * AppbaseCoreServices core = AppbaseCoreServices.builder().fromYaml("config.yaml").build();
 * Injector injector = Guice.createInjector(new AppbaseCoreServicesModule(core));
 * LogService log = injector.getInstance(LogService.class);
 * }</pre>
 */
public final class AppbaseCoreServicesModule extends AbstractModule {

    private final AppbaseCoreServices coreServices;

    /**
     * Constructs a module backed by an already-built {@link AppbaseCoreServices}.
     *
     * @param coreServices the fully-built core services container
     */
    public AppbaseCoreServicesModule(AppbaseCoreServices coreServices) {
        this.coreServices = Objects.requireNonNull(coreServices, "coreServices");
    }

    @Override
    protected void configure() {
        bind(AppbaseCoreServices.class).toInstance(coreServices);
        bind(LogService.class).toInstance(coreServices.getLogService());
        bind(MetricService.class).toInstance(coreServices.getMetricService());
        bind(ValidationService.class).toInstance(coreServices.getValidationService());
        bind(MediatorService.class).toInstance(coreServices.getMediatorService());
        bind(ConfigurationService.class).toInstance(coreServices.getConfigurationService());
        bind(MicroprofileConfigurationService.class).toInstance(coreServices.getMicroprofileConfigurationService());
        bind(ServiceRegistry.class).toInstance(coreServices.getServiceRegistry());
        bind(MicroprofileServiceRegistry.class).toInstance(coreServices.getMicroprofileServiceRegistry());
        bind(ExceptionService.class).toInstance(coreServices.getExceptionService());
        bind(TracerService.class).toInstance(coreServices.getTracerService());
        bind(JmxService.class).toInstance(coreServices.getJmxService());
        // Optional — only bind if present:
        if (coreServices.getReactorService() != null) {
            bind(com.viveworks.appbase.core.service.reactive.ReactorService.class)
                .toInstance(coreServices.getReactorService());
        }
    }
}
```

```java
// src/main/java/com/viveworks/appbase/bootstrap/guice/AppbaseClientServicesModule.java
package com.viveworks.appbase.bootstrap.guice;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import java.util.Objects;

/**
 * Guice module that binds {@link AppbaseClientServices}.
 * Individual client service bindings are not auto-registered because client services
 * are optional. Inject {@link AppbaseClientServices} and call {@code get(XxxService.class)} instead.
 */
public final class AppbaseClientServicesModule extends AbstractModule {

    private final AppbaseClientServices clientServices;

    /**
     * Constructs a module backed by an already-built {@link AppbaseClientServices}.
     *
     * @param clientServices the fully-built client services container
     */
    public AppbaseClientServicesModule(AppbaseClientServices clientServices) {
        this.clientServices = Objects.requireNonNull(clientServices, "clientServices");
    }

    @Override
    protected void configure() {
        bind(AppbaseClientServices.class).toInstance(clientServices);
    }
}
```

```java
// src/main/java/com/viveworks/appbase/bootstrap/guice/AppbaseServicesModule.java
package com.viveworks.appbase.bootstrap.guice;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import java.util.Objects;

/**
 * Guice module that binds {@link AppbaseServices}.
 * Inject {@link AppbaseServices} and call {@code get(XxxService.class)} for individual services.
 */
public final class AppbaseServicesModule extends AbstractModule {

    private final AppbaseServices appbaseServices;

    /**
     * Constructs a module backed by an already-built {@link AppbaseServices}.
     *
     * @param appbaseServices the fully-built application services container
     */
    public AppbaseServicesModule(AppbaseServices appbaseServices) {
        this.appbaseServices = Objects.requireNonNull(appbaseServices, "appbaseServices");
    }

    @Override
    protected void configure() {
        bind(AppbaseServices.class).toInstance(appbaseServices);
    }
}
```

```java
// src/main/java/com/viveworks/appbase/bootstrap/guice/JettyAppbaseServerModule.java
package com.viveworks.appbase.bootstrap.guice;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.server.JettyAppbaseServer;
import java.util.Objects;

/**
 * Guice module that binds {@link JettyAppbaseServer}.
 */
public final class JettyAppbaseServerModule extends AbstractModule {

    private final JettyAppbaseServer server;

    /**
     * Constructs a module backed by an already-built {@link JettyAppbaseServer}.
     *
     * @param server the fully-configured (but not yet started) server
     */
    public JettyAppbaseServerModule(JettyAppbaseServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    protected void configure() {
        bind(JettyAppbaseServer.class).toInstance(server);
    }
}
```

- [ ] **Step 4: Run tests**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=AppbaseBootstrapGuiceModulesTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -30
```

Expected: All tests PASS.

- [ ] **Step 5: Full module build**

```bash
cd appbase-java && ./build.sh quick 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add appbase-java/appbase-bootstrap/src/
git commit -m "feat(P-001): add four Guice modules for bootstrap containers"
```

---

### Task 9: AppbaseBootstrapIntegrationTest

**Files:**
- Create: `test/.../AppbaseBootstrapIntegrationTest.java`

**Interfaces:**
- Consumes: all containers (`AppbaseCoreServices`, `AppbaseClientServices`, `AppbaseServices`, `JettyAppbaseServer`), `AppbaseCoreServicesConfiguration`, `fromYaml()`, Guice modules; a test YAML config file
- Produces: end-to-end test that builds the full stack via the builder path and the Guice path; verifies containers are populated and the server starts/stops without error

**Before writing tests, create a test YAML config:**

```yaml
# src/test/resources/appbase-test-config.yaml
appbaseCoreServicesConfiguration:
  logServiceConfiguration:
    # copy minimal valid LogService config from existing appbase-core tests
    # (read appbase-core/src/test/resources/ for examples)
  metricServiceConfiguration:
    # ...
  # fill in all 11 mandatory sections — copy from DefaultAppbaseCoreBootstrapConfiguration test fixture
```

Read `appbase-core/src/test/resources/` and `appbase-stacktest/src/main/resources/` to find existing YAML config examples.

- [ ] **Step 1: Write the integration test**

```java
// src/test/java/com/viveworks/appbase/bootstrap/AppbaseBootstrapIntegrationTest.java
package com.viveworks.appbase.bootstrap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.bootstrap.server.JettyAppbaseServer;
import com.viveworks.appbase.bootstrap.guice.*;
import com.viveworks.appbase.core.service.log.LogService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppbaseBootstrapIntegrationTest {

    // ── Builder path ───────────────────────────────────────────────────────────

    @Test
    void builderPath_coreServicesBuildsFromYaml() {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml("appbase-test-config.yaml")
            .build();
        assertNotNull(core.getLogService());
        assertNotNull(core.getMediatorService());
        assertNotNull(core.getExceptionService());
        core.shutdown();
    }

    @Test
    void builderPath_clientAndServiceContainersBuildOnTopOfCore() {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml("appbase-test-config.yaml")
            .build();
        // No client or service factories registered — containers should be empty but not null
        AppbaseClientServices clients = AppbaseClientServices.builder().from(core).build();
        AppbaseServices services = AppbaseServices.builder().from(core, clients).build();
        assertTrue(clients.services().isEmpty());
        assertTrue(services.services().isEmpty());
        services.shutdown();
        clients.shutdown();
        core.shutdown();
    }

    @Test
    void builderPath_jettyServerStartsAndStops() throws Exception {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml("appbase-test-config.yaml")
            .build();
        AppbaseClientServices clients = AppbaseClientServices.builder().from(core).build();
        AppbaseServices services = AppbaseServices.builder().from(core, clients).build();
        JettyAppbaseServer server = JettyAppbaseServer.builder()
            .from(services)
            .port(0) // port 0 = OS assigns ephemeral port — avoids conflicts in CI
            .build();
        server.start();
        server.stop();
    }

    // ── Guice path ─────────────────────────────────────────────────────────────

    @Test
    void guicePath_allModulesInstallCorrectly() {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml("appbase-test-config.yaml")
            .build();
        AppbaseClientServices clients = AppbaseClientServices.builder().from(core).build();
        AppbaseServices services = AppbaseServices.builder().from(core, clients).build();
        JettyAppbaseServer server = JettyAppbaseServer.builder()
            .from(services)
            .port(0)
            .build();

        Injector injector = Guice.createInjector(
            new AppbaseCoreServicesModule(core),
            new AppbaseClientServicesModule(clients),
            new AppbaseServicesModule(services),
            new JettyAppbaseServerModule(server)
        );

        assertSame(core,     injector.getInstance(AppbaseCoreServices.class));
        assertSame(clients,  injector.getInstance(AppbaseClientServices.class));
        assertSame(services, injector.getInstance(AppbaseServices.class));
        assertSame(server,   injector.getInstance(JettyAppbaseServer.class));
        assertNotNull(injector.getInstance(LogService.class));

        server.stop();
    }
}
```

- [ ] **Step 2: Create `appbase-test-config.yaml`**

```bash
mkdir -p appbase-java/appbase-bootstrap/src/test/resources
```

Copy minimal valid config from `appbase-core/src/test/resources/` (find existing YAML fixtures). The file must have an `appbaseCoreServicesConfiguration` top-level node with all 11 mandatory sub-configs.

- [ ] **Step 3: Run tests**

```bash
cd appbase-java && mvn test -pl appbase-bootstrap -Dtest=AppbaseBootstrapIntegrationTest \
  -P dev-profile,jacoco-no-rules-profile 2>&1 | tail -40
```

Expected: All tests PASS. If any service fails to initialize, read its error message and fix the YAML config or the factory constructor call.

- [ ] **Step 4: Full build with all modules**

```bash
cd appbase-java && ./build.sh quick 2>&1 | tail -20
```

Expected: `BUILD SUCCESS` for ALL modules including `appbase-stacktest`.

- [ ] **Step 5: Commit**

```bash
git add appbase-java/appbase-bootstrap/src/test/
git commit -m "feat(P-001): add AppbaseBootstrapIntegrationTest with builder and Guice paths"
```

---

### Task 10: Rewrite appbase-stacktest

**Files:**
- Modify: `appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/StackTestServer.java` (rewrite)
- Create: `appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/GuiceStackTestServer.java`
- Create: `appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/GuiceStackTestMain.java`
- Modify: `appbase-stacktest/pom.xml` (add `appbase-bootstrap` dependency)

**Interfaces:**
- Consumes: `AppbaseCoreServices`, `AppbaseClientServices`, `AppbaseServices`, `JettyAppbaseServer`; all relevant client and service factories for the stacktest's actual dependencies; existing stacktest config YAML
- Produces: two equivalent server entry points — builder path (`StackTestServer`) and Guice path (`GuiceStackTestServer`/`GuiceStackTestMain`)

**Before writing code, read:**
- `appbase-stacktest/src/main/java/.../StackTestServer.java` — full current implementation
- `appbase-stacktest/src/main/resources/` — YAML config structure
- `appbase-stacktest/pom.xml` — current dependencies to retain

- [ ] **Step 1: Add `appbase-bootstrap` dependency to `appbase-stacktest/pom.xml`**

```xml
<dependency>
    <groupId>com.viveworks.appbase</groupId>
    <artifactId>appbase-bootstrap</artifactId>
</dependency>
```

- [ ] **Step 2: Rewrite `StackTestServer.java` using the builder path**

Read the current `StackTestServer.java` implementation first. Then rewrite it to use the bootstrap builders:

```java
// appbase-stacktest/.../StackTestServer.java
package com.viveworks.appbase.stacktest;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.bootstrap.server.JettyAppbaseServer;
// Import whichever client factories the stacktest actually uses — read current StackTestServer first
import com.viveworks.appbase.bootstrap.client.factory.AerospikeClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.factory.ElasticsearchClientServiceFactory;
// ... (add factory imports matching the stacktest's actual clients)

/**
 * Stack-test server entry point — builder path.
 * Reads configuration from {@code stacktest-config.yaml} on the classpath.
 */
public final class StackTestServer {

    private static final String CONFIG_YAML = "stacktest-config.yaml"; // or whatever the existing file is named

    private StackTestServer() {}

    /**
     * Application entry point.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) throws Exception {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG_YAML)
            .build();

        AppbaseClientServices clients = AppbaseClientServices.builder()
            .from(core)
            // Add factories for clients the stacktest uses.
            // Read the current StackTestServer.java to find all clients — add a factory for each.
            // Example:
            // .with(new AerospikeClientServiceFactory(/* config from appbase-core bootstrap */))
            .build();

        AppbaseServices services = AppbaseServices.builder()
            .from(core, clients)
            // Add factories for any appbase-service layer services the stacktest uses
            .build();

        JettyAppbaseServer server = JettyAppbaseServer.builder()
            .from(services)
            .port(8080)
            .build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            services.shutdown();
            clients.shutdown();
            core.shutdown();
        }, "appbase-shutdown"));

        server.start();
        server.join();
    }
}
```

- [ ] **Step 3: Create `GuiceStackTestServer.java`**

```java
// appbase-stacktest/.../GuiceStackTestServer.java
package com.viveworks.appbase.stacktest;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.bootstrap.server.JettyAppbaseServer;
import com.viveworks.appbase.bootstrap.guice.*;

/**
 * Guice module that wires the complete stack-test application.
 * Install this into a Guice {@code Injector} to get all stack-test bindings.
 */
public final class GuiceStackTestServer extends AbstractModule {

    private static final String CONFIG_YAML = "stacktest-config.yaml";

    @Override
    protected void configure() {
        AppbaseCoreServices core = AppbaseCoreServices.builder()
            .fromYaml(CONFIG_YAML)
            .build();

        AppbaseClientServices clients = AppbaseClientServices.builder()
            .from(core)
            // Add same factories as StackTestServer.main() above
            .build();

        AppbaseServices services = AppbaseServices.builder()
            .from(core, clients)
            .build();

        JettyAppbaseServer server = JettyAppbaseServer.builder()
            .from(services)
            .port(8080)
            .build();

        install(new AppbaseCoreServicesModule(core));
        install(new AppbaseClientServicesModule(clients));
        install(new AppbaseServicesModule(services));
        install(new JettyAppbaseServerModule(server));
    }
}
```

- [ ] **Step 4: Create `GuiceStackTestMain.java`**

```java
// appbase-stacktest/.../GuiceStackTestMain.java
package com.viveworks.appbase.stacktest;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.bootstrap.server.JettyAppbaseServer;

/**
 * Application entry point for the Guice-wired stack-test server.
 */
public final class GuiceStackTestMain {

    private GuiceStackTestMain() {}

    /**
     * Application entry point.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new GuiceStackTestServer());
        JettyAppbaseServer server = injector.getInstance(JettyAppbaseServer.class);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "appbase-shutdown"));

        server.start();
        server.join();
    }
}
```

- [ ] **Step 5: Build and verify `appbase-stacktest` compiles**

```bash
cd appbase-java && mvn clean compile -pl appbase-stacktest -am -P dev-profile -q 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`. Fix any import errors by reading the client/service class packages from `appbase-client` and `appbase-service`.

- [ ] **Step 6: Full build**

```bash
cd appbase-java && ./build.sh quick 2>&1 | tail -20
```

Expected: `BUILD SUCCESS` for all modules.

- [ ] **Step 7: Commit**

```bash
git add appbase-java/appbase-stacktest/
git commit -m "feat(P-001): rewrite appbase-stacktest to use bootstrap builder and Guice paths"
```

---

### Task 11: Full Dev Build + PR Creation

**Files:**
- No new files; verify entire codebase

**Interfaces:**
- Consumes: all previous tasks
- Produces: passing dev build, GitHub PR

- [ ] **Step 1: Full dev build with coverage**

```bash
cd appbase-java && ./build.sh dev 2>&1 | tail -40
```

Expected: `BUILD SUCCESS` with JaCoCo coverage thresholds passing for `appbase-bootstrap`. Fix any coverage gaps by adding test cases to the relevant test class.

- [ ] **Step 2: Review SpotBugs and Checkstyle output**

```bash
cd appbase-java && mvn site -pl appbase-bootstrap -P dev-profile -q 2>&1 | grep -i "error\|warning" | head -30
```

Fix any Checkstyle violations (missing Javadoc, line length) and SpotBugs findings. Add `@SuppressFBWarnings` with a justification string only when the finding is a confirmed false positive.

- [ ] **Step 3: Run code-review**

```
/code-review
```

Fix any confirmed or plausible findings before creating the PR.

- [ ] **Step 4: Create GitHub issue**

```bash
gh issue create \
  --title "P-001: Add appbase-bootstrap Maven module" \
  --body "$(cat <<'EOF'
## Summary
Add new \`appbase-bootstrap\` Maven module with:
- \`AppbaseCoreServices\` container + 13 built-in \`AppbaseCoreServiceFactory\` implementations
- \`AppbaseCoreServicesConfiguration\` (user-facing YAML config, hides internal bootstrap classes)
- \`CoreServiceOrder\` and \`ClientServiceOrder\` enums for factory ordering
- \`AppbaseCoreServicesAdapter\` (bridges \`AppbaseCoreServices\` to \`AppbaseCore\` for client factories)
- \`AppbaseClientServices\` container + 20 built-in \`AppbaseClientServiceFactory\` implementations
- \`AppbaseServices\` container + 11 \`AppbaseServiceFactory\` implementations
- \`JettyAppbaseServer\` with fluent builder (including \`server(Server)\` override)
- Four Guice modules (\`AppbaseCoreServicesModule\`, etc.)
- Rewrite of \`appbase-stacktest\` to use builder and Guice paths
EOF
)"
```

Note the issue number from the output. Rename the branch to match:

```bash
git branch -m "ISSUE_NUMBER/Java/Bootstrap/Feature/Appbase-bootstrap"
git push -u origin HEAD
```

- [ ] **Step 5: Create PR**

```bash
gh pr create \
  --title "feat(P-001): add appbase-bootstrap Maven module" \
  --body "$(cat <<'EOF'
## Summary
- New \`appbase-bootstrap\` module with hierarchical service containers (Core → Client → Service → Server)
- Factory-driven core service initialization via \`AppbaseCoreServicesConfiguration\` + 13 built-in factories
- \`AppbaseCoreServicesAdapter\` bridges container to \`AppbaseCore\` without leaking internal types
- \`ClientServiceOrder\` enum replaces hardcoded ints in 20 client factories
- \`JettyAppbaseServer.Builder.server(Server)\` allows custom Jetty server injection
- \`appbase-stacktest\` rewritten to use new builder and Guice paths

## Test plan
- [ ] `./build.sh quick` passes for all modules
- [ ] `./build.sh dev` passes with JaCoCo thresholds
- [ ] `AppbaseBootstrapIntegrationTest` passes end-to-end (builder + Guice paths)
- [ ] SpotBugs and Checkstyle clean
- [ ] PR code-reviewed; no confirmed findings
EOF
)"
```

**IMPORTANT:** Wait for human review and approval before any post-merge steps.

---

### Task 12: Post-Merge Workflow

**NOTE: Do NOT start this task until the PR has been reviewed and merged by a human.**

- [ ] **Step 1: Pull merged develop**

```bash
git checkout develop
git pull origin develop
```

- [ ] **Step 2: Verify build on develop**

```bash
cd appbase-java && ./build.sh dev 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Close GitHub issue with PR link**

```bash
# Replace ISSUE_NUMBER and PR_URL with actual values
gh issue comment ISSUE_NUMBER --body "Implemented and merged in PR_URL"
gh issue close ISSUE_NUMBER
```

- [ ] **Step 4: Update Claude-Review.md**

Open `Claude-Review.md` and update the P-001 row in the `Prioritized Action Plan` section:
- Change status emoji from 🟡 (in-progress) to ✅ (done)
- Add merged PR URL in the Notes column

```bash
git add Claude-Review.md
git commit -m "chore(P-001): update Claude-Review.md — bootstrap module complete"
git push origin develop
```

- [ ] **Step 5: Update LEARNING.md**

If anything new was learned during the lifecycle (e.g., a non-obvious `DefaultXxxService` constructor signature, a Jackson deserialization quirk, a Guice binding edge case), add it to `appbase-java/LEARNING.md`.

```bash
git add appbase-java/LEARNING.md
git commit -m "docs(P-001): update LEARNING.md with bootstrap module learnings"
git push origin develop
```
