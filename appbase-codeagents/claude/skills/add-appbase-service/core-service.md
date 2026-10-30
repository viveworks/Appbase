# Core Service Recipe

> ⚠️ **EXPERIMENTAL** — This recipe has not been validated end-to-end. It covers the deepest integration layer (touches `appbase-core`, `appbase-service-web`, `appbase-bootstrap`, and `appbase-stacktest`). Treat generated scaffolding as a starting point and verify against existing core services before committing.

Execute this file top-to-bottom. Replace `Xxx`/`xxx` with confirmed values before generating any file. All generated Java files must start with the Apache 2.0 copyright header (copy from any file in `appbase-core/src/main/java/`).

Core services have the deepest integration footprint — they touch `appbase-core`, `appbase-service-web`, `appbase-bootstrap`, and `appbase-stacktest`.

---

## Step 0: External library dependency (execute only if Q4 ≠ `none`)

If Q4 selected an external library (`groupId:artifactId:version`):

**0.1 Add version property to the REPO-ROOT `pom.xml`** (in `<properties>`):
```xml
<xxx.version>VERSION</xxx.version>
```

**0.2 Add to the REPO-ROOT `pom.xml` `<dependencyManagement>`**:
```xml
<!-- Xxx -->
<dependency>
    <groupId>GROUP_ID</groupId>
    <artifactId>ARTIFACT_ID</artifactId>
    <version>${xxx.version}</version>
</dependency>
```

**0.3 Add to `appbase-core/pom.xml` `<dependencies>`** (no `<version>` — managed by root pom):
```xml
<dependency>
    <groupId>GROUP_ID</groupId>
    <artifactId>ARTIFACT_ID</artifactId>
</dependency>
```

---

## Step 1: Core service files

All files under: `appbase-core/src/main/java/com/viveworks/appbase/core/service/<xxx>/`

### 1.1 `XxxService.java`

```java
package com.viveworks.appbase.core.service.<xxx>;
import com.viveworks.appbase.core.service.AppbaseService;

public interface XxxService extends AppbaseService {
    // Declare the public API methods of this service.
    // Example: void doSomething() throws XxxServiceException;
}
```

### 1.2 `DefaultXxxService.java`

Extends `AbstractAppbaseManagedService<DefaultXxxServiceConfiguration>`.

```java
package com.viveworks.appbase.core.service.<xxx>;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedService;

public class DefaultXxxService
        extends AbstractAppbaseManagedService<DefaultXxxServiceConfiguration>
        implements XxxService {

    public DefaultXxxService(final AppbaseCore appbaseCore) {
        super(appbaseCore, DefaultXxxServiceConfiguration.class, true, null);
    }

    @Override
    protected void initImpl(final DefaultXxxServiceConfiguration configuration)
            throws AppbaseException {
        throw new UnsupportedOperationException(
                "TODO: implement — initialise the library/resource using configuration fields");
    }

    @Override
    protected void shutdownImpl() {
        throw new UnsupportedOperationException(
                "TODO: implement — release library/resource");
    }

    @Override
    protected void reloadImpl(final DefaultXxxServiceConfiguration configuration)
            throws AppbaseException {
        throw new UnsupportedOperationException(
                "TODO: implement — reload resources on configuration change");
    }

    @Override
    protected void registerService() {
        throw new UnsupportedOperationException(
                "TODO: implement — register with discovery/registry if needed");
    }
}
```

### Step 1.R: Config property review *(complete this before generating 1.3)*

Before generating the configuration class, present the following suggested-property menu to the user.

**Say to the user:**

> "Here are suggested configuration properties for `DefaultXxxServiceConfiguration`. This is a starting-point menu — you can confirm these as-is, remove any you don't need, or add your own properties that aren't listed. Tell me your final list and I'll generate the configuration class from it."

| # | Property | Java Type | `@Nullable`? | Default | Notes |
|---|---|---|---|---|---|
| 1 | `autoStart` | `boolean` | No | `true` | Inherited from `AbstractAppbaseManagedServiceConfiguration` — include unless overriding |
| 2 | `corePoolSize` | `int` | No | `2` | Min threads in the managed thread pool |
| 3 | `maxPoolSize` | `int` | No | `10` | Max threads in the managed thread pool |
| 4 | `keepAliveMs` | `long` | No | `60000` | Idle thread keep-alive before termination |
| 5 | `queueCapacity` | `int` | No | `100` | Work-queue depth before task rejection |
| 6 | `operationTimeoutMs` | `long` | No | `30000` | Timeout for individual operations (ms) |
| 7 | `maxRetries` | `int` | No | `3` | Retry attempts on transient failures |
| 8 | `retryDelayMs` | `long` | No | `100` | Delay between retries (ms) |

The user may confirm the list as-is, remove entries by name or number, or add new ones (provide: name, Java type, default, `@Nullable`?).

**Wait for final user confirmation before generating 1.3.** Generate `DefaultXxxServiceConfiguration` with exactly the confirmed properties.

---

### 1.3 `DefaultXxxServiceConfiguration.java`

Must extend `AbstractAppbaseManagedServiceConfiguration` — `AbstractAppbaseManagedService<C extends AppbaseManagedServiceConfiguration>` requires this. The Builder must extend `AbstractAppbaseManagedServiceConfiguration.Builder<T>`. See `DefaultAppbaseClientCoreSharedResourcesConfiguration` for a canonical example.

```java
package com.viveworks.appbase.core.service.<xxx>;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedServiceConfiguration;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.concurrent.Immutable;

@Immutable
@DefaultXxxServiceConfiguration.Valid
@JsonDeserialize(builder = DefaultXxxServiceConfiguration.Builder.class)
public final class DefaultXxxServiceConfiguration extends AbstractAppbaseManagedServiceConfiguration {

    // TODO: add configuration fields (e.g. capacity, threadPoolSize, etc.)

    private DefaultXxxServiceConfiguration(final Builder builder) {
        super(builder);
        // TODO: assign builder fields to instance fields
    }

    public static Builder builder() { return new Builder(); }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder
            extends AbstractAppbaseManagedServiceConfiguration.Builder<Builder> {
        // TODO: add fields mirroring the configuration fields above
        @Override
        public DefaultXxxServiceConfiguration build() {
            return new DefaultXxxServiceConfiguration(this);
        }
    }

    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultXxxServiceConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "Invalid DefaultXxxServiceConfiguration";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
}
```

### 1.4 `DefaultXxxServiceConfigurationValidator.java`

```java
package com.viveworks.appbase.core.service.<xxx>;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DefaultXxxServiceConfigurationValidator
        implements ConstraintValidator<DefaultXxxServiceConfiguration.Valid,
                                       DefaultXxxServiceConfiguration> {

    @Override
    public boolean isValid(final DefaultXxxServiceConfiguration value,
                           final ConstraintValidatorContext ctx) {
        // TODO: add field-level validation (e.g. capacity > 0)
        return value != null;
    }
}
```

---

## Step 2: JMX files

All files under: `appbase-core/src/main/java/com/viveworks/appbase/core/service/<xxx>/jmx/`

### 2.1 `XxxServiceMXBean.java`

```java
package com.viveworks.appbase.core.service.<xxx>.jmx;

import com.viveworks.appbase.core.service.jmx.AppbaseServiceMXBean;
import javax.management.MXBean;

@MXBean
public interface XxxServiceMXBean extends AppbaseServiceMXBean {
    // Declare JMX-visible attributes (getters) and operations (void methods or simple return types).
    // Example: String getStatus(); void reset() throws MBeanException;
}
```

### 2.2 `JmxXxxService.java`

```java
package com.viveworks.appbase.core.service.<xxx>.jmx;

import com.viveworks.appbase.core.service.<xxx>.DefaultXxxServiceConfiguration;
import com.viveworks.appbase.core.service.<xxx>.XxxService;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.core.service.log.LogService;

public class JmxXxxService extends AbstractAppbaseServiceMXBean implements XxxServiceMXBean {

    private final XxxService xxxService;

    public JmxXxxService(final LogService logService,
                          final XxxService xxxService,
                          final Class<DefaultXxxServiceConfiguration> configurationClass,
                          final ConfigurationService configurationService) {
        super(logService, configurationClass, configurationService);
        this.xxxService = xxxService;
    }

    // Implement XxxServiceMXBean methods delegating to xxxService.
    // Example: @Override public String getStatus() { return xxxService.getStatus().name(); }
}
```

---

## Step 3: Management endpoints (in appbase-service-web)

All files under:
`appbase-service/appbase-service-web/src/main/java/com/viveworks/appbase/service/web/endpoint/`

Reference the existing servlets in `.../servlet/appbase/` (e.g. `LogServlet`, `MetricServlet`) and JAX-RS resources in `.../jaxrs/appbase/` for the exact patterns to follow.

### 3.1 `servlet/appbase/XxxServlet.java`

```java
package com.viveworks.appbase.service.web.endpoint.servlet.appbase;

import com.viveworks.appbase.core.service.<xxx>.XxxService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class XxxServlet extends AbstractAppbaseSyncServlet {

    public static final String DEFAULT_PATH = AppbaseServiceConstants.DEFAULT_XXX_SERVLET_PATH;

    private final XxxService xxxService;

    public XxxServlet(final XxxService xxxService, /* other AbstractAppbaseSyncServlet deps */ ) {
        // pass required deps to super constructor — copy signature from LogServlet or MetricServlet
        this.xxxService = xxxService;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        throw new UnsupportedOperationException(
                "TODO: implement — return XxxService status as JSON");
    }
}
```

### 3.2 `jaxrs/appbase/XxxJaxrsResource.java`

```java
package com.viveworks.appbase.service.web.endpoint.jaxrs.appbase;

import com.viveworks.appbase.core.service.<xxx>.XxxService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path(AppbaseServiceConstants.DEFAULT_XXX_RESTAPI_PATH)
@Tag(name = "Xxx", description = "XxxService management endpoints")
public class XxxJaxrsResource {

    private final XxxService xxxService;

    public XxxJaxrsResource(final XxxService xxxService) {
        this.xxxService = xxxService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get XxxService status")
    public Response getStatus() {
        throw new UnsupportedOperationException(
                "TODO: implement — return JSON representation of XxxService status");
    }
}
```

---

## Step 4: Bootstrap factory

### 4.1 `appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/core/factory/XxxServiceFactory.java`

```java
package com.viveworks.appbase.bootstrap.core.factory;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.service.<xxx>.DefaultXxxService;
import com.viveworks.appbase.core.service.<xxx>.DefaultXxxServiceConfiguration;
import com.viveworks.appbase.core.service.<xxx>.XxxService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import javax.annotation.Nullable;

public final class XxxServiceFactory implements AppbaseCoreServiceFactory<XxxService> {

    @Nullable private final XxxService preBuilt;
    @Nullable private final DefaultXxxServiceConfiguration configuration;

    public XxxServiceFactory(final XxxService preBuilt) {
        AppbaseUtil.notNull(preBuilt, "preBuilt");
        this.preBuilt = preBuilt;
        this.configuration = null;
    }

    public XxxServiceFactory(final DefaultXxxServiceConfiguration configuration) {
        AppbaseUtil.notNull(configuration, "configuration");
        this.preBuilt = null;
        this.configuration = configuration;
    }

    @Override public Class<XxxService> serviceType() { return XxxService.class; }

    @Override
    public int initializationOrder() {
        // Set to next integer after the highest existing factory order.
        // Inspect factories in this package to find the current max.
        throw new UnsupportedOperationException(
                "TODO: implement — set to next integer after the highest existing factory order");
    }

    @Override
    public XxxService create(final AppbaseCoreServices partial) {
        if (preBuilt != null) return preBuilt;
        return new DefaultXxxService(partial.appbaseCore());
    }
}
```

---

## Step 5: Guice DI module

### 5.1 `appbase-core/src/main/java/com/viveworks/appbase/core/di/guice/XxxServiceGuiceModule.java`

```java
package com.viveworks.appbase.core.di.guice;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.core.service.<xxx>.DefaultXxxService;
import com.viveworks.appbase.core.service.<xxx>.XxxService;

public class XxxServiceGuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(XxxService.class).to(DefaultXxxService.class);
    }
}
```

---

## Step 6: Tests

**6.1** `appbase-core/src/test/…/<xxx>/DefaultXxxServiceTest.java`
```java
// Arrange: mock AppbaseCore; build DefaultXxxService with valid configuration stub
// Act: start() → Assert: STARTED state
// Act: shutdown() → Assert: STOPPED state
```

**6.2** `appbase-core/src/test/…/<xxx>/DefaultXxxServiceConfigurationValidatorTest.java`
```java
// Assert: valid configuration → isValid returns true
// Assert: null → isValid returns false
```

**6.3** `appbase-core/src/test/…/<xxx>/jmx/JmxXxxServiceTest.java`
```java
// Arrange: mock XxxService; build JmxXxxService
// Act: call each MXBean attribute getter/operation
// Assert: delegates to underlying XxxService
```

**6.4** `appbase-core/src/test/…/<xxx>/di/guice/XxxServiceGuiceModuleTest.java`
```java
// Assert: Guice injector resolves XxxService to DefaultXxxService
```

---

## Step 7: Modify existing files (7 files)

### M1. `appbase-bootstrap/…/core/AppbaseCoreServices.java` — 4 changes

1. Add `XxxService.class` to `MANDATORY_TYPES` list (omit if the service is optional).
2. Add typed getter: `public XxxService getXxxService() { return get(XxxService.class); }`
3. In the `CoreAdapter` **private inner class**, add delegation:
   `@Override public XxxService getXxxService() { return c.getXxxService(); }`
4. In `registerBuiltinFactories()`, add:
   `put(map, new XxxServiceFactory(cfg.getXxxServiceConfiguration()));`

### M2. `appbase-bootstrap/…/core/AppbaseCoreServicesConfiguration.java`

Add field, builder entry, and constructor assignment following the `reactorServiceConfiguration` pattern:
```java
@Nullable private final DefaultXxxServiceConfiguration xxxServiceConfiguration;
```

### M3. `appbase-bootstrap/…/server/JettyAppbaseServer.java` — `registerMXBeans()` try block

```java
if (!jmxService.isMXBeanRegistered(XxxServiceMXBean.class)) {
    jmxService.registerMXBean(new JmxXxxService(
        logService, appbaseCore.getXxxService(),
        DefaultXxxServiceConfiguration.class, configurationService));
}
```

### M4. `appbase-service/appbase-service-core/…/AppbaseServiceConstants.java`

```java
public static final String DEFAULT_XXX_SERVLET_PATH  = "/xxx";
public static final String DEFAULT_XXX_RESTAPI_PATH  =
        DEFAULT_APPBASE_CORE_RESTAPI_PREFIX + "/xxx";
```

### M5. `appbase-bootstrap/…/server/AppbaseServletRegistrar.java` — `addCoreServlets()`

```java
addServlet(ctx, XxxServlet.class,
        AppbaseServiceConstants.DEFAULT_XXX_SERVLET_PATH, excludes, pathOverrides);
```

### M6. `appbase-core/…/AppbaseCore.java` (if interface-based)

```java
XxxService getXxxService();
```

### M7. `appbase-core/…/bootstrap/DefaultAppbaseCore.java`

Register `XxxService` in the startup/shutdown chain following the pattern of existing core services.

---

## YAML configurations (only if fully implemented — no stubs remaining)

If every method is fully implemented, generate the following YAML file. Field names map to `@JsonProperty` / Builder fields on `DefaultXxxServiceConfiguration`.

**`appbase-core/src/test/resources/appbase-core-xxx.yaml`** — unit test config:
```yaml
---
# DefaultXxxServiceConfiguration fields
# TODO: add all fields with realistic test values
```

*(If `DefaultXxxServiceConfiguration` uses `AbstractAppbaseManagedServiceConfiguration`, include `autoStart: true` at the top level.)*

---

## Stub quality contract

- All files compile with zero errors before any stubs are filled in.
- Every stub throws `UnsupportedOperationException("TODO: implement — <one-line description>")`.
- Unit tests pass with mocked/pre-built paths.
- At end of output: list every stub not fully implemented.
