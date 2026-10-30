# Service-Core Extension Recipe

Execute this file top-to-bottom. Replace `Xxx`/`xxx` with confirmed values before generating any file. All generated Java files must start with the Apache 2.0 copyright header (copy from any file in `appbase-service/appbase-service-core/src/main/java/`).

This recipe adds a new service directly to the existing `appbase-service-core` module. No new Maven sub-module is created.

Reference patterns:
- `appbase-service/appbase-service-core/.../password/DefaultPasswordService.java`
- `appbase-service/appbase-service-core/.../id/DefaultIdService.java`
- `appbase-bootstrap/.../service/factory/DefaultPasswordServiceFactory.java`

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

**0.3 Add to `appbase-service/appbase-service-core/pom.xml` `<dependencies>`** (no `<version>` — managed by root pom):
```xml
<dependency>
    <groupId>GROUP_ID</groupId>
    <artifactId>ARTIFACT_ID</artifactId>
</dependency>
```

---

## Step 1: Java source files

All files under: `appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/<xxx>/`

### 1.1 `XxxService.java`

```java
package com.viveworks.appbase.service.<xxx>;

import com.viveworks.appbase.core.service.AppbaseService;

public interface XxxService extends AppbaseService {
    // Declare the public API methods of this service.
    // Example: String generate() throws XxxServiceException;
}
```

### 1.2 `DefaultXxxService.java`

Extends `AbstractStatelessAppbaseService<DefaultXxxServiceConfiguration>`.
`registerService()` is called by the superclass constructor — no explicit `start()` needed.

```java
package com.viveworks.appbase.service.<xxx>;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;

public class DefaultXxxService
        extends AbstractStatelessAppbaseService<DefaultXxxServiceConfiguration>
        implements XxxService
{
    // TODO: add fields initialised from configuration or injected via constructor

    public DefaultXxxService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final DefaultXxxServiceConfiguration serviceConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, serviceConfiguration);
        // TODO: initialise fields from serviceConfiguration
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(XxxService.class, this);
        serviceRegistry.registerAppbaseService(DefaultXxxService.class, this);
    }

    // TODO: implement XxxService operation methods
}
```

### Step 1.R: Config property review *(complete this before generating 1.3)*

Before generating the configuration class, present the following suggested-property menu to the user.

**Say to the user:**

> "Here are suggested configuration properties for `DefaultXxxServiceConfiguration`. This is a starting-point menu — you can confirm these as-is, remove any you don't need, or add your own properties that aren't listed. Tell me your final list and I'll generate the configuration class from it."

| # | Property | Java Type | `@Nullable`? | Default | Notes |
|---|---|---|---|---|---|
| 1 | `enabled` | `boolean` | No | `true` | Toggle the service off without removing it from bootstrap |
| 2 | `maxRetries` | `int` | No | `3` | Retry attempts on transient operation failures |
| 3 | `retryDelayMs` | `long` | No | `100` | Delay between retries (ms) |
| 4 | `operationTimeoutMs` | `long` | No | `30000` | Timeout per service operation (ms) |
| 5 | `cacheEnabled` | `boolean` | No | `false` | Enable in-memory result cache |
| 6 | `cacheTtlMs` | `long` | No | `60000` | Cache entry TTL (when `cacheEnabled=true`) |
| 7 | `cacheMaxSize` | `int` | No | `1000` | Max cached entries (when `cacheEnabled=true`) |

The user may confirm the list as-is, remove entries by name or number, or add new ones (provide: name, Java type, default, `@Nullable`?).

**Wait for final user confirmation before generating 1.3.** Generate `DefaultXxxServiceConfiguration` with exactly the confirmed properties.

---

### 1.3 `DefaultXxxServiceConfiguration.java`

Must implement `AppbaseConfiguration` — required by `AbstractStatelessAppbaseService<C extends AppbaseConfiguration>`.
Use `@JsonCreator`/`@JsonProperty` if configuration fields exist; default constructor is sufficient if there are none.

```java
package com.viveworks.appbase.service.<xxx>;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.service.AppbaseConfiguration;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class DefaultXxxServiceConfiguration implements AppbaseConfiguration {

    // TODO: add configuration fields (e.g. capacity, timeoutMs, etc.)

    /** Creates configuration with defaults (used when no YAML block is present). */
    public DefaultXxxServiceConfiguration() {
        // TODO: assign defaults
    }

    @JsonCreator
    public DefaultXxxServiceConfiguration(
            // TODO: add @JsonProperty parameters for each field
            )
    {
        // TODO: assign fields; apply defaults for null parameters
    }

    // TODO: add getters for each field
}
```

---

## Step 2: Bootstrap factory

### 2.1 `appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/service/factory/DefaultXxxServiceFactory.java`

```java
package com.viveworks.appbase.bootstrap.service.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceFactory;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceOrder;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.service.<xxx>.DefaultXxxService;
import com.viveworks.appbase.service.<xxx>.DefaultXxxServiceConfiguration;
import com.viveworks.appbase.service.<xxx>.XxxService;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link XxxService}.
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #DefaultXxxServiceFactory(XxxService)}</li>
 *   <li>Config-based: {@link #DefaultXxxServiceFactory(DefaultXxxServiceConfiguration)}</li>
 *   <li>Default: {@link #DefaultXxxServiceFactory()} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 */
public final class DefaultXxxServiceFactory implements AppbaseServiceFactory<XxxService> {

    @Nullable private final XxxService predefinedService;
    @Nullable private final DefaultXxxServiceConfiguration config;

    /** Wraps a pre-built {@link XxxService} instance. */
    public DefaultXxxServiceFactory(final XxxService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link DefaultXxxService} from {@code config}. */
    public DefaultXxxServiceFactory(final DefaultXxxServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link DefaultXxxService} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
     */
    public DefaultXxxServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<XxxService> serviceType() {
        return XxxService.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.XXX.order();
    }

    @Override
    public XxxService create(final AppbaseCoreServices coreServices,
                              final AppbaseClientServices clientServices,
                              final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultXxxServiceConfiguration.class, config);
        }
        final DefaultXxxServiceConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(DefaultXxxServiceConfiguration.class);
        return new DefaultXxxService(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            cfg);
    }
}
```

---

## Step 3: Tests

**3.1** `appbase-service/appbase-service-core/src/test/…/<xxx>/DefaultXxxServiceTest.java`:
```java
// Arrange: build DefaultXxxServiceConfiguration with defaults
//          mock LogService, MetricService, MediatorService, ServiceRegistry
// Act:     construct DefaultXxxService (superclass constructor calls registerService() + STARTED)
// Assert:  service registered for XxxService.class and DefaultXxxService.class
//          For each operation: Act: call xxxService.op(); Assert: expected result
```

**3.2** `appbase-bootstrap/src/test/…/factory/DefaultXxxServiceFactoryTest.java`:
```java
// Assert: serviceType() == XxxService.class
// Assert: initializationOrder() == AppbaseServiceOrder.XXX.order()
// Assert: create() with pre-built returns the same instance
```

---

## Step 4: Modify existing files (2 files)

### M1. `appbase-bootstrap/…/service/AppbaseServiceOrder.java`

Add next enum entry (inspect existing entries to find the current max integer, then add 10):
```java
/** {@link com.viveworks.appbase.service.<xxx>.XxxService} */
XXX(N),  // replace N with next multiple-of-10 after the current max
```

Note dependency order in the Javadoc of `AppbaseServiceOrder`: if `XxxService` depends on any existing service (e.g. `IdService`), its order must be higher.

### M2. `appbase-bootstrap/…/service/AppbaseServices.java` — no change needed

`AppbaseServices.Builder.with(factory)` is the caller's responsibility — the factory is registered at the application startup point, not inside the framework. Document the intended usage in a Javadoc note on the factory class.

---

## YAML configurations (only if fully implemented — no stubs remaining)

If every method is fully implemented, generate the following YAML file. Field names map to `@JsonProperty` fields on `DefaultXxxServiceConfiguration`.

**`appbase-service/appbase-service-core/src/test/resources/<xxx>.yaml`** — unit test config:
```yaml
---
# DefaultXxxServiceConfiguration fields
# TODO: add all fields with realistic test values (or omit to use defaults)
```

*(If `DefaultXxxServiceConfiguration` has no mandatory fields and sensible defaults exist, this file may be omitted — tests can use the no-arg constructor.)*

---

## Appbase Config Wizard smoke test

If `DefaultXxxServiceConfiguration` implements `AppbaseConfiguration` or uses
`@JsonDeserialize(builder=...)`, add a console smoke test:

In `appbase-config-wizard/test/console/test_smoke.py`,
add `'<xxx>'` to the `TestNumericBooleanDefaults` parametrize list. If the
config has nested sub-objects, add a dedicated class (see `TestZooKeeperConfig`).

For nested fields, the web UI must show **"Set Values"** buttons rather than
text inputs. Verify by running the wizard web mode and opening the config form.

---

## Stub quality contract

- All files compile with zero errors before any stubs are filled in.
- Every stub throws `UnsupportedOperationException("TODO: implement — <one-line description>")`.
- Unit tests pass.
- At end of output: list every stub not fully implemented.
