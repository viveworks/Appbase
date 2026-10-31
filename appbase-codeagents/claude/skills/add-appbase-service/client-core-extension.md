# Client-Core Extension Recipe

> ⚠️ **EXPERIMENTAL** — This recipe has not been validated end-to-end. It modifies `DefaultAppbaseClientCore`, `AppbaseClientCore`, and `DefaultAppbaseClientCoreConfiguration` — files shared by all client sub-modules. Verify generated changes carefully against existing patterns before committing.

Execute this file top-to-bottom. Replace `Xxx`/`xxx` with confirmed values before generating any file. All generated Java files must start with the Apache 2.0 copyright header (copy from any file in `appbase-client/appbase-client-core/src/main/java/`).

This recipe adds a new shared client capability directly to the existing `appbase-client-core` module. No new Maven sub-module is created. The new service is instantiated by `DefaultAppbaseClientCore` and exposed via the `AppbaseClientCore` interface.

Reference patterns:
- `appbase-client/appbase-client-core/.../AppbaseClientCoreSharedResources.java` — interface
- `appbase-client/appbase-client-core/.../DefaultAppbaseClientCoreSharedResources.java` — implementation
- `appbase-client/appbase-client-core/.../DefaultAppbaseClientCore.java` — how shared resources are wired

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

**0.3 Add to `appbase-client/appbase-client-core/pom.xml` `<dependencies>`** (no `<version>` — managed by root pom):
```xml
<dependency>
    <groupId>GROUP_ID</groupId>
    <artifactId>ARTIFACT_ID</artifactId>
</dependency>
```

---

## Step 1: Java source files

All files under: `appbase-client/appbase-client-core/src/main/java/com/viveworks/appbase/client/core/<xxx>/`

### 1.1 `XxxClientCoreService.java` — interface

```java
package com.viveworks.appbase.client.core.<xxx>;

import com.viveworks.appbase.core.service.AppbaseService;

public interface XxxClientCoreService extends AppbaseService {
    // Declare the public API methods of this service.
    // Example: XxxResult process(XxxInput input);

    // Expose the underlying resource if callers need direct access:
    // Example: SomeSdkType getXxxResource();
}
```

### 1.2 `DefaultXxxClientCoreService.java` — implementation

Extends `AbstractAppbaseManagedService<DefaultXxxClientCoreServiceConfiguration>`.
This is the full managed lifecycle base — implement `initImpl`, `shutdownImpl`, and optionally `reloadImpl`.

```java
package com.viveworks.appbase.client.core.<xxx>;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedService;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultXxxClientCoreService
        extends AbstractAppbaseManagedService<DefaultXxxClientCoreServiceConfiguration>
        implements XxxClientCoreService
{
    // TODO: replace with the actual resource type managed by this service
    private final AtomicReference<Object /* TODO: XxxResource */> resourceRef;

    public DefaultXxxClientCoreService(
            final AppbaseCore appbaseCore,
            final DefaultXxxClientCoreServiceConfiguration configuration)
    {
        super(appbaseCore,
              DefaultXxxClientCoreServiceConfiguration.class,
              configuration,
              false,
              null);
        this.resourceRef = new AtomicReference<>();
    }

    @Override
    protected void initImpl(final DefaultXxxClientCoreServiceConfiguration configuration)
            throws AppbaseException {
        throw new UnsupportedOperationException(
            "TODO: implement — initialise the resource using configuration fields; set resourceRef");
    }

    @Override
    protected void shutdownImpl() {
        final Object resource = resourceRef.get();
        if (resource != null) {
            throw new UnsupportedOperationException(
                "TODO: implement — close/release the resource");
        }
    }

    @Override
    protected void reloadImpl(final DefaultXxxClientCoreServiceConfiguration configuration)
            throws AppbaseException {
        // TODO: implement reload if the resource supports hot-reload; otherwise leave as no-op
    }

    @Override
    protected void registerService() {
        // no-op: client-core services are not registered in the service registry
    }

    // TODO: implement XxxClientCoreService operation methods
    // Example:
    // @Override
    // public XxxResult process(XxxInput input) {
    //     return resourceRef.get().process(input);
    // }
}
```

### Step 1.R: Config property review *(complete this before generating 1.3)*

Before generating the configuration class, present the following suggested-property menu to the user.

**1. Discover properties from the chosen SDK**

Look at the SDK dependency added in Step 0. Find its shared-resource configuration API — the options class, client factory, or pool configuration object used to tune the shared resource this client-core service manages. List every configurable property.

**2. Merge with seed suggestions and present the combined menu**

Merge discovered properties with the seed list below (de-duplicate by name). Present the merged table to the user:

> "Here are the configurable properties I found for `DefaultXxxClientCoreServiceConfiguration`. This is a starting-point menu — confirm, remove, or add your own properties."

*Seed suggestions (extend with SDK discovery — do not limit to this list):*

| # | Property | Java Type | `@Nullable`? | Default | Notes |
|---|---|---|---|---|---|
| 1 | `autoStart` | `boolean` | No | `true` | Inherited from `AbstractAppbaseManagedServiceConfiguration` — include unless overriding |
| 2 | `maxPoolSize` | `int` | No | `10` | Max connections or threads in the shared pool |
| 3 | `minIdleSize` | `int` | No | `1` | Min idle resources kept alive in the pool |
| 4 | `maxIdleTimeMs` | `long` | No | `60000` | Idle resource eviction threshold (ms) |
| 5 | `connectTimeoutMs` | `long` | No | `5000` | Resource acquisition / connection timeout (ms) |
| 6 | `operationTimeoutMs` | `long` | No | `30000` | Per-operation timeout (ms) |
| 7 | `keepAliveMs` | `long` | No | `30000` | Heartbeat / keep-alive interval (ms) |
| 8 | `maxRetries` | `int` | No | `3` | Retry attempts on transient failures |
| 9 | `retryDelayMs` | `long` | No | `100` | Delay between retries (ms) |

**3. Accept user input**

The user may confirm the list as-is, remove entries by name or number, or add new ones (provide: name, Java type, default, `@Nullable`?).

**Wait for final user confirmation before generating 1.3.** Generate `DefaultXxxClientCoreServiceConfiguration` with exactly the confirmed properties.

---

### 1.3 `DefaultXxxClientCoreServiceConfiguration.java`

Must extend `AbstractAppbaseManagedServiceConfiguration` — required by `AbstractAppbaseManagedService<C extends AppbaseManagedServiceConfiguration>`.

```java
package com.viveworks.appbase.client.core.<xxx>;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedServiceConfiguration;

import javax.annotation.concurrent.Immutable;

@Immutable
@JsonDeserialize(builder = DefaultXxxClientCoreServiceConfiguration.Builder.class)
public final class DefaultXxxClientCoreServiceConfiguration
        extends AbstractAppbaseManagedServiceConfiguration
{
    // TODO: add configuration fields (e.g. capacity, timeoutMs, etc.)

    private DefaultXxxClientCoreServiceConfiguration(final Builder builder) {
        super(builder);
        // TODO: assign builder fields to instance fields
    }

    public static Builder builder() {
        return new Builder();
    }

    // TODO: add getters for each field

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder
            extends AbstractAppbaseManagedServiceConfiguration.Builder<Builder>
    {
        // TODO: add fields mirroring the configuration fields above

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public DefaultXxxClientCoreServiceConfiguration build() {
            return new DefaultXxxClientCoreServiceConfiguration(this);
        }
    }
}
```

---

## Step 2: Tests

**2.1** `appbase-client/appbase-client-core/src/test/…/client/core/<xxx>/DefaultXxxClientCoreServiceTest.java`:
```java
// Arrange: mock AppbaseCore; build DefaultXxxClientCoreServiceConfiguration with defaults
// Act:     construct DefaultXxxClientCoreService; call start()
// Assert:  service reaches STARTED state (initImpl called, resourceRef set)
// Act:     call shutdown()
// Assert:  STOPPED state
```

---

## Step 3: Modify existing files (3 files)

### M1. `appbase-client/appbase-client-core/.../AppbaseClientCore.java`

Add getter to the interface:
```java
/**
 * Returns the {@link XxxClientCoreService}.
 *
 * @return the Xxx client core service (never null after start)
 */
XxxClientCoreService getXxxClientCoreService();
```

### M2. `appbase-client/appbase-client-core/.../DefaultAppbaseClientCore.java`

**a)** Add field (alongside `appbaseClientCoreSharedResourcesRef`):
```java
private final AtomicReference<XxxClientCoreService> xxxClientCoreServiceRef;
```

**b)** Initialise field in every constructor (alongside `sharedResourcesRef` init):
```java
this.xxxClientCoreServiceRef = new AtomicReference<>();
```

**c)** In `initImpl(DefaultAppbaseClientCoreConfiguration configuration)`, add (after shared resources init):
```java
final DefaultXxxClientCoreServiceConfiguration xxxConfig =
        configuration.getXxxClientCoreServiceConfiguration();
final DefaultXxxClientCoreService xxxService =
        new DefaultXxxClientCoreService(appbaseCore, xxxConfig);
xxxService.start();
xxxClientCoreServiceRef.set(xxxService);
```

**d)** In `shutdownImpl()`, add (before or after shared resources shutdown):
```java
final XxxClientCoreService xxxClientCoreService = xxxClientCoreServiceRef.get();
if (xxxClientCoreService != null) {
    xxxClientCoreService.shutdown();
}
```

**e)** Add getter implementing `AppbaseClientCore`:
```java
@Override
public XxxClientCoreService getXxxClientCoreService() {
    return xxxClientCoreServiceRef.get();
}
```

### M3. `appbase-client/appbase-client-core/.../DefaultAppbaseClientCoreConfiguration.java`

**a)** Add field (alongside `defaultAppbaseClientSharedResourcesConfiguration`):
```java
private final DefaultXxxClientCoreServiceConfiguration xxxClientCoreServiceConfiguration;
```

**b)** In the private constructor, assign from builder:
```java
this.xxxClientCoreServiceConfiguration = builder.xxxClientCoreServiceConfiguration;
```

**c)** Add getter:
```java
public DefaultXxxClientCoreServiceConfiguration getXxxClientCoreServiceConfiguration() {
    return xxxClientCoreServiceConfiguration;
}
```

**d)** In `Builder`, add field with default and setter:
```java
private DefaultXxxClientCoreServiceConfiguration xxxClientCoreServiceConfiguration =
        DefaultXxxClientCoreServiceConfiguration.builder().build();

public Builder xxxClientCoreServiceConfiguration(
        final DefaultXxxClientCoreServiceConfiguration v) {
    this.xxxClientCoreServiceConfiguration = v;
    return this;
}
```

---

## YAML configurations (only if fully implemented — no stubs remaining)

If every method is fully implemented, add the new service's configuration block to the existing client-core test YAML. Field names map to `Builder` setter names on `DefaultXxxClientCoreServiceConfiguration`.

**Modify `appbase-client/appbase-client-core/src/test/resources/appbase-client-core.yaml`** — add the new section:
```yaml
# Add inside DefaultAppbaseClientCoreConfiguration fields:
xxxClientCoreServiceConfiguration:
  autoStart: true
  # TODO: add all fields from DefaultXxxClientCoreServiceConfiguration
```

**Modify `appbase-stacktest/src/main/resources/appbase-stacktest-client-core.yaml`** — add the same section for the stack test environment.

---

## Stub quality contract

- All files compile with zero errors before any stubs are filled in.
- Every stub throws `UnsupportedOperationException("TODO: implement — <one-line description>")`.
- Unit tests pass.
- At end of output: list every stub not fully implemented.
