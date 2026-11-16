# Service-Layer Recipe

Execute this file top-to-bottom. Replace `Xxx`/`xxx` with confirmed values and `Op`/`op` with each operation name. All generated Java files must start with the Apache 2.0 copyright header (copy from any file in `appbase-service/appbase-service-supportmessage/src/main/java/`).

Reference modules:
- Part A pattern: `appbase-service/appbase-service-supportmessage/`
- Part B separate-module pattern: `appbase-service/appbase-service-user-account-jdbi/`

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

**0.3 Add to `appbase-service/appbase-service-<xxx>/pom.xml` `<dependencies>`** (no `<version>` — managed by root pom):
```xml
<dependency>
    <groupId>GROUP_ID</groupId>
    <artifactId>ARTIFACT_ID</artifactId>
</dependency>
```

---

## Part A: Generic module `appbase-service-<xxx>` (always execute)

### A1: Create the Maven sub-module

**A1.1 `appbase-service/appbase-service-<xxx>/pom.xml`**

```xml
<?xml version='1.0' encoding='UTF-8'?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                      http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.viveworks.appbase</groupId>
        <artifactId>appbase-service</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>appbase-service-<xxx></artifactId>
    <packaging>jar</packaging>
    <name>Appbase Service Xxx</name>
    <url>https://github.com/viveworks/Appbase/tree/develop/Java/${project.artifactId}</url>
    <scm>
        <url>https://github.com/viveworks/Appbase/tree/develop/Java/${project.artifactId}</url>
    </scm>
    <distributionManagement>
        <site>
            <id>${project.artifactId}</id>
            <url>https://github.com/viveworks/Appbase/tree/develop/Java/${project.artifactId}</url>
        </site>
    </distributionManagement>

    <dependencies>
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-service-core</artifactId>
        </dependency>
        <!-- Include appbase-service-web only if Q8 = yes (HTTP endpoints needed) -->
        <!-- If Q8 = no, omit the block below and skip Steps A3 and A5.2 entirely -->
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-service-web</artifactId>
        </dependency>
        <!-- If Q4 selected an external library, add it here (see Step 0) -->
        <!-- TODO: add appbase-client-<xxx> deps if this service wraps client services -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <packageName>com.viveworks.appbase.service.<xxx></packageName>
                        </manifest>
                    </archive>
                    <instructions>
                        <Export-Package>com.viveworks.appbase.service.<xxx></Export-Package>
                    </instructions>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**A1.2 Modify `appbase-service/pom.xml`**

In the `<modules>` section, add (in alphabetical order):
```xml
<module>appbase-service-<xxx></module>
```

---

### A2: Java source files

All files under: `appbase-service/appbase-service-<xxx>/src/main/java/com/viveworks/appbase/service/<xxx>/`

**A2.1 `XxxService.java`** — service interface:
```java
package com.viveworks.appbase.service.<xxx>;

import com.viveworks.appbase.core.service.AppbaseService;

public interface XxxService extends AppbaseService {
    // One method per operation.
    // Example: XxxResult create(XxxRequest request) throws XxxServiceException;
}
```

**A2.2 `DefaultXxxService.java`** — extends `AbstractStatelessAppbaseService`:

Constructor signature must match `AbstractStatelessAppbaseService(LogService, MetricService, MediatorService, ServiceRegistry, C)`. See `DefaultSupportMessageService` for the canonical example.

```java
package com.viveworks.appbase.service.<xxx>;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;

public class DefaultXxxService
        extends AbstractStatelessAppbaseService<DefaultXxxServiceConfiguration>
        implements XxxService {

    // TODO: add fields for injected store and/or client services

    public DefaultXxxService(
            // TODO: add store / client service parameters before the framework deps
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final DefaultXxxServiceConfiguration serviceConfiguration) {
        super(logService, metricService, mediatorService, serviceRegistry, serviceConfiguration);
        // TODO: assign injected store / client service fields
    }

    @Override
    protected void registerService() {
        // TODO: register with ServiceRegistry if needed; leave empty if not required
    }

    // TODO: implement each operation method — delegate to store or client services
}
```

**Step A2.R: Config property review** *(complete this before generating A2.3)*

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
| 8 | `circuitBreakerEnabled` | `boolean` | No | `false` | Enable Resilience4j circuit breaker |
| 9 | `failureThreshold` | `int` | No | `5` | Failures before circuit opens (when `circuitBreakerEnabled=true`) |
| 10 | `resetIntervalMs` | `long` | No | `30000` | Half-open wait after circuit opens (when `circuitBreakerEnabled=true`) |

The user may confirm the list as-is, remove entries by name or number, or add new ones (provide: name, Java type, default, `@Nullable`?).

**Wait for final user confirmation before generating A2.3.** Generate `DefaultXxxServiceConfiguration` with exactly the confirmed properties.

---

**A2.3 `DefaultXxxServiceConfiguration.java`** — `@Immutable`, Jackson Builder:
```java
package com.viveworks.appbase.service.<xxx>;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import javax.annotation.concurrent.Immutable;

@Immutable
@JsonDeserialize(builder = DefaultXxxServiceConfiguration.Builder.class)
public final class DefaultXxxServiceConfiguration implements AppbaseConfiguration {

    // TODO: add configuration fields (e.g. maxRetries, timeoutMs, etc.)

    private DefaultXxxServiceConfiguration(final Builder builder) {
        // TODO: assign builder fields to instance fields
    }

    public static Builder builder() { return new Builder(); }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        // TODO: add fields mirroring the configuration fields above
        public DefaultXxxServiceConfiguration build() {
            return new DefaultXxxServiceConfiguration(this);
        }
    }
}
```

**A2.4 `Xxx.java`** — domain entity:
```java
package com.viveworks.appbase.service.<xxx>;

// TODO: add fields representing the core domain object
// Use @Immutable + Jackson Builder following the SupportMessage pattern
public final class Xxx {
    // Example: private final String id; private final String content;
}
```

**A2.5–A2.N Data classes in `data/`**

For each operation `Op`, create request and response DTOs under `data/`:
```
OpXxxRequest.java   — @Immutable, Jackson Builder, fields for the operation input
OpXxxResponse.java  — @Immutable, Jackson Builder, fields for the operation result
```

**A2.N+1 `store/XxxStore.java`** — store interface (omit if the service composes client services without its own store):
```java
package com.viveworks.appbase.service.<xxx>.store;

public interface XxxStore {
    // One method per persistence operation.
    // Example: Xxx save(Xxx entity) throws XxxServiceException;
    //          Xxx findById(String id) throws XxxServiceException;
}
```

---

### A3: HTTP endpoints (execute only if Q8 = yes)

**A3.1 Per-operation servlet in `endpoint/servlet/`**

For each operation `Op`, create `OpXxxServlet.java` extending `AbstractAppbaseSyncServlet`:
```java
package com.viveworks.appbase.service.<xxx>.endpoint.servlet;

import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.<xxx>.XxxService;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class OpXxxServlet extends AbstractAppbaseSyncServlet {

    public static final String DEFAULT_PATH =
            AppbaseServiceConstants.DEFAULT_XXX_OP_SERVLET_PATH;

    private final XxxService xxxService;

    public OpXxxServlet(final XxxService xxxService, /* AbstractAppbaseSyncServlet deps */) {
        // copy constructor signature from an existing servlet (e.g. SendSupportMessageServlet)
        this.xxxService = xxxService;
    }

    // Override doPost() or doGet() depending on the operation type.
    // Use the postRequestType() / postImpl() pattern from AbstractAppbaseSyncServlet.
    // Example:
    // @Override protected Class<OpXxxRequest> postRequestType() { return OpXxxRequest.class; }
    // @Override protected Object postImpl(OpXxxRequest req) throws Exception {
    //     return xxxService.op(req);
    // }
}
```

**A3.2 `endpoint/jaxrs/XxxJaxrsResource.java`** — single JAX-RS class, one method per operation:
```java
package com.viveworks.appbase.service.<xxx>.endpoint.jaxrs;

import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.<xxx>.XxxService;
import com.viveworks.appbase.service.<xxx>.data.OpXxxRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path(AppbaseServiceConstants.DEFAULT_XXX_RESTAPI_PATH)
@Tag(name = "Xxx", description = "XxxService endpoints")
public class XxxJaxrsResource {

    private final XxxService xxxService;

    public XxxJaxrsResource(final XxxService xxxService) {
        this.xxxService = xxxService;
    }

    @POST
    @Path("/op")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Execute Op on XxxService")
    public Response op(final OpXxxRequest request) {
        throw new UnsupportedOperationException(
                "TODO: implement — delegate to xxxService.op() and return Response");
    }
    // Repeat for each additional operation
}
```

---

### A4: Tests

**A4.1** `appbase-service-<xxx>/src/test/…/<xxx>/DefaultXxxServiceTest.java`:
```java
// Arrange: mock store + AppbaseCore; build DefaultXxxService
// For each operation: Act: call xxxService.op(); Assert: delegates to store correctly
```

---

### A5: Modify existing files

**A5.1 `appbase-service/appbase-service-core/…/AppbaseServiceConstants.java`**

For each operation `Op`, add a servlet path constant:
```java
public static final String DEFAULT_XXX_OP_SERVLET_PATH = "/xxx/op";
```

Also add the JAX-RS base path:
```java
public static final String DEFAULT_XXX_RESTAPI_PATH =
        DEFAULT_APPBASE_SERVICE_RESTAPI_PREFIX + "/xxx";
```

**A5.2 `appbase-bootstrap/…/server/AppbaseServletRegistrar.java` — `addServiceServlets()` (only if Q8 = yes)**

For each operation servlet, add:
```java
addServlet(ctx, OpXxxServlet.class,
        AppbaseServiceConstants.DEFAULT_XXX_OP_SERVLET_PATH, excludes, pathOverrides);
```

---

## Part B: Technology-specific module `appbase-service-<xxx>-<tech>` (execute only if Q7 answered with technologies)

Repeat this entire Part B section for each technology listed in Q7 (e.g. `jdbi`, `vitess`, `aerospike`).

### B1: Create the tech-specific sub-module

**B1.1 `appbase-service/appbase-service-<xxx>-<tech>/pom.xml`**

```xml
<?xml version='1.0' encoding='UTF-8'?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                      http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.viveworks.appbase</groupId>
        <artifactId>appbase-service</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>appbase-service-<xxx>-<tech></artifactId>
    <packaging>jar</packaging>
    <name>Appbase Service Xxx Tech</name>
    <url>https://github.com/viveworks/Appbase/tree/develop/Java/${project.artifactId}</url>
    <scm>
        <url>https://github.com/viveworks/Appbase/tree/develop/Java/${project.artifactId}</url>
    </scm>
    <distributionManagement>
        <site>
            <id>${project.artifactId}</id>
            <url>https://github.com/viveworks/Appbase/tree/develop/Java/${project.artifactId}</url>
        </site>
    </distributionManagement>

    <dependencies>
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-service-<xxx></artifactId>
        </dependency>
        <!-- TODO: add the technology client dependency (e.g. appbase-client-jdbi, appbase-client-vitess) -->
        <dependency>
            <groupId>com.viveworks.appbase</groupId>
            <artifactId>appbase-client-<tech></artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <packageName>com.viveworks.appbase.service.<xxx>.<tech></packageName>
                        </manifest>
                    </archive>
                    <instructions>
                        <Export-Package>com.viveworks.appbase.service.<xxx>.<tech>.*</Export-Package>
                    </instructions>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**B1.2 Modify `appbase-service/pom.xml`**

In the `<modules>` section, add after `appbase-service-<xxx>`:
```xml
<module>appbase-service-<xxx>-<tech></module>
```

---

### B2: Concrete store implementation

**B2.1** `appbase-service/appbase-service-<xxx>-<tech>/src/main/java/com/viveworks/appbase/service/<xxx>/<tech>/TechXxxStore.java`

For a concrete JDBI store, inspect `appbase-service/appbase-service-supportmessage/src/main/java/com/viveworks/appbase/service/supportmessage/store/JdbiSupportMessageStore.java` (the support-message implementation lives in its parent service module):
```java
package com.viveworks.appbase.service.<xxx>.<tech>;

import com.viveworks.appbase.service.<xxx>.store.XxxStore;
// TODO: import the technology client service (e.g. JdbiClientService, VitessGrpcClientService)

public class TechXxxStore implements XxxStore {

    // TODO: inject the technology client service via constructor

    // TODO: implement each XxxStore method using the technology client
    //       Every method should throw UnsupportedOperationException("TODO: …") initially
}
```

---

### B3: Tests

**B3.1** `appbase-service-<xxx>-<tech>/src/test/…/<xxx>/<tech>/TechXxxStoreTest.java`:
```java
// Arrange: mock tech client service; build TechXxxStore
// For each store method: Act + Assert round-trip or delegation
```

---

## YAML configurations (only if fully implemented — no stubs remaining)

If every method is fully implemented, generate the following YAML files. Field names map to `@JsonProperty` / `@JsonPOJOBuilder` fields on the configuration classes.

**`appbase-service/appbase-service-<xxx>/src/test/resources/appbase-service-<xxx>.yaml`** — unit/IT test config:
```yaml
---
# DefaultXxxServiceConfiguration fields
# TODO: add all fields with realistic test values
```

**`appbase-stacktest/src/test/resources/appbase-service-<xxx>.yaml`** — stack test service config:
```yaml
---
# DefaultXxxServiceConfiguration fields for the stack test environment
# TODO: add all fields with stack-test-appropriate values
```

*(If Part B tech-specific modules are also fully implemented, add their test YAML to the corresponding sub-module's `src/test/resources/` as well.)*

---

## Appbase Config Wizard smoke test

If `DefaultXxxServiceConfiguration` implements `AppbaseConfiguration` or uses
`@JsonDeserialize(builder=...)`, it is automatically discoverable by
`appbase-config-wizard`. Add a console smoke test to verify:

In `appbase-config-wizard/test/console/test_smoke.py`,
add `'<xxx>'` to the `TestNumericBooleanDefaults` parametrize list. If the
config has nested sub-objects, add a dedicated class (see `TestZooKeeperConfig`).

After rebuilding the JAR, verify the class appears in the wizard:
```bash
curl -s http://localhost:8080/api/configurations | jq '[.[].simpleName] | map(select(test("<Xxx>";"i")))'
```

For nested fields, the Flutter web UI must show a **"Set Values"** card instead
of a text input. See `Step 8.3` in `client-service.md` for the full verification
checklist.

---

## Stub quality contract

- All files compile with zero errors before any stubs are filled in.
- Every stub throws `UnsupportedOperationException("TODO: implement — <one-line description>")`.
- Unit tests pass with mocked dependencies.
- At end of output: list every stub not fully implemented.
