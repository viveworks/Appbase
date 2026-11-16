# Client Service Recipe

Execute this file top-to-bottom. Replace `Xxx`/`xxx`/`T`/`V` with confirmed values before generating any file. All generated Java files must start with the Apache 2.0 copyright header (copy from any file in `appbase-client/appbase-client-aerospike/src/main/java/`).

Reference module: `appbase-client/appbase-client-aerospike/`

---

## Step 0: External library dependency (execute only if Q4 ≠ `none`)

🔴 **NEVER put a `<version>` in a module POM.** Versions are governed by the **repo-root `pom.xml`**'s
`<dependencyManagement>`, which is authoritative. A version inlined in a sub-module escapes that
governance and drifts silently from every other module using the same library.

If Q4 selected an external library (`groupId:artifactId:version`):

**0.1 Add version property to the REPO-ROOT `pom.xml`** (in `<properties>`, under the `Client` heading):
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

**0.3 Add to `appbase-client/appbase-client-<xxx>/pom.xml` `<dependencies>`** (no `<version>` — managed by the root pom):
```xml
<dependency>
    <groupId>GROUP_ID</groupId>
    <artifactId>ARTIFACT_ID</artifactId>
</dependency>
```

Reference: `appbase-client-aerospike` declares `com.aerospike:aerospike-client` with no `<version>`; the
property `<aerospike-client.version>` and the managed entry both live in the repo-root `pom.xml`.

---

## Step 1: Create the Maven sub-module

### 1a. `appbase-client/appbase-client-<xxx>/pom.xml`

```xml
<?xml version='1.0' encoding='UTF-8'?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                      http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.viveworks.appbase</groupId>
        <artifactId>appbase-client</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>appbase-client-<xxx></artifactId>
    <packaging>jar</packaging>
    <name>Appbase Client Xxx</name>
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
            <artifactId>appbase-client-core</artifactId>
        </dependency>
        <!-- TODO: replace with the chosen SDK dependency from Step 0.
             NO <version> — it is managed by the repo-root pom (Step 0.1/0.2). -->
        <dependency>
            <groupId>groupId</groupId>
            <artifactId>artifactId</artifactId>
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
                            <packageName>com.viveworks.appbase.client.service.<xxx></packageName>
                        </manifest>
                    </archive>
                    <instructions>
                        <Export-Package>com.viveworks.appbase.client.service.<xxx></Export-Package>
                    </instructions>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 1b. Modify `appbase-client/pom.xml`

In the `<modules>` section, add (in alphabetical order):
```xml
<module>appbase-client-<xxx></module>
```

---

## Step 2: Java source files

All files under: `appbase-client/appbase-client-<xxx>/src/main/java/com/viveworks/appbase/client/service/<xxx>/`

### 2.1 `XxxClientService.java`

```java
package com.viveworks.appbase.client.service.<xxx>;

import com.viveworks.appbase.client.core.AppbaseClientService;

public interface XxxClientService extends AppbaseClientService<T, V> {
    @Override
    XxxClientIntegrationService getClientIntegrationService()
            throws XxxClientIntegrationService.NotFoundException;

    @Override
    XxxClientIntegrationService getClientIntegrationService(V key)
            throws XxxClientIntegrationService.NotFoundException;
}
```

### 2.2 `XxxClientIntegrationService.java`

```java
package com.viveworks.appbase.client.service.<xxx>;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;

public interface XxxClientIntegrationService extends AppbaseClientIntegrationService<T, V> {
    // Add service-specific operation methods here.
    // Example: void ping() throws XxxClientException;
}
```

### 2.3 `DefaultXxxClientService.java`

```java
package com.viveworks.appbase.client.service.<xxx>;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.service.<xxx>.configuration.DefaultXxxClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;

import java.util.List;

public class DefaultXxxClientService
        extends AbstractAppbaseClientService<T, V, DefaultXxxClientServiceConfiguration>
        implements XxxClientService {

    public DefaultXxxClientService(final AppbaseCore appbaseCore) {
        super(appbaseCore, DefaultXxxClientServiceConfiguration.class);
        startOnCreate();
    }

    public DefaultXxxClientService(final AppbaseCore appbaseCore,
                                    final DefaultXxxClientServiceConfiguration configuration) {
        super(appbaseCore, DefaultXxxClientServiceConfiguration.class, configuration);
        startOnCreate();
    }

    static { registerObjectMapperSubtypes(); }

    public static void registerObjectMapperSubtypes() {
        // Register Jackson subtypes for polymorphic config deserialization.
        // Example: AppbaseCoreObjectMapperSubTypes.register(DefaultXxxClientServiceConfiguration.class);
    }

    @Override
    protected V defaultAppbaseClientKey() {
        throw new UnsupportedOperationException(
                "TODO: implement — return default key (e.g. TextValue.of(\"default\"))");
    }

    @Override
    protected List<XxxClientIntegrationService> appbaseClientIntegrationServices(
            final DefaultXxxClientServiceConfiguration configuration) {
        throw new UnsupportedOperationException(
                "TODO: implement — build integration service list from configuration");
    }
}
```

### 2.4 `DefaultXxxClientIntegrationService.java`

```java
package com.viveworks.appbase.client.service.<xxx>;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.service.<xxx>.configuration.DefaultXxxClientIntegrationServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;

public class DefaultXxxClientIntegrationService
        extends AbstractAppbaseClientIntegrationService<T, V, DefaultXxxClientIntegrationServiceConfiguration>
        implements XxxClientIntegrationService {

    public DefaultXxxClientIntegrationService(
            final AppbaseCore appbaseCore,
            final DefaultXxxClientIntegrationServiceConfiguration configuration) {
        super(appbaseCore, DefaultXxxClientIntegrationServiceConfiguration.class, configuration);
    }

    @Override
    protected T initClient(final DefaultXxxClientIntegrationServiceConfiguration configuration) {
        throw new UnsupportedOperationException(
                "TODO: implement — connect to Xxx using the SDK");
    }

    @Override
    protected void shutdownClient(final T client) {
        throw new UnsupportedOperationException(
                "TODO: implement — close the Xxx SDK client");
    }
}
```

### Step 2.R: Config property review *(complete this before generating 2.5–2.8)*

**1. Discover properties from the chosen SDK**

Look at the SDK dependency added in Step 1 (the `groupId:artifactId` chosen for this client). Find its configuration builder, options class, connection-factory constructor, or client-configuration object. List **every** configurable property the SDK exposes — not just host/port. For each property record: name, Java type, SDK default (from source or Javadoc), and a one-line description.

Good places to look:
- A class named `*Options`, `*Config`, `*Configuration`, `*Settings`, `*ClientOptions`
- The client/connection builder's setter methods
- The SDK README or connection-string parameter reference

**2. Merge with seed suggestions and present the combined menu**

Merge the SDK-discovered properties with the seed list below (de-duplicate by name). Present the merged table to the user:

> "I found the following configurable properties in [SDK name]. These are the ones I can expose in `DefaultXxxClientIntegrationServiceConfiguration`. Check which ones you want to include, remove any you don't need, or add properties that are missing from this list."

*Seed suggestions (extend with SDK discovery — do not limit to this list):*

| # | Property | Java Type | `@Nullable`? | Default | Notes |
|---|---|---|---|---|---|
| 1 | `host` | `String` | No | `"localhost"` | Primary target host |
| 2 | `port` | `int` | No | *(SDK default)* | TCP port |
| 3 | `username` | `String` | Yes | `null` | Auth username or ACL user |
| 4 | `password` | `String` | Yes | `null` | Auth credential |
| 5 | `ssl` | `boolean` | No | `false` | Enable TLS/SSL |
| 6 | `sslVerifyPeer` | `boolean` | No | `true` | Verify server cert (when `ssl=true`) |
| 7 | `connectTimeoutMs` | `long` | No | `5000` | Connection establishment timeout |
| 8 | `readTimeoutMs` | `long` | No | `30000` | Per-operation read timeout |
| 9 | `commandTimeoutMs` | `long` | No | `30000` | Command/write timeout |
| 10 | `maxConnections` | `int` | No | `10` | Max connections in pool |
| 11 | `minIdleConnections` | `int` | No | `1` | Min idle connections in pool |
| 12 | `maxIdleTimeMs` | `long` | No | `60000` | Idle connection eviction threshold |
| 13 | `autoReconnect` | `boolean` | No | `true` | Reconnect after dropped connection |
| 14 | `pingBeforeActivate` | `boolean` | No | `false` | Ping on borrow to verify liveness |
| 15 | `retryCount` | `int` | No | `3` | Retry attempts on transient failures |
| 16 | `retryDelayMs` | `long` | No | `100` | Delay between retries |

**3. Accept user input**

The user may:
- Confirm the full list as-is
- Remove specific properties by name or number
- Add properties not in the list (provide: name, Java type, default, `@Nullable`?)
- Rename, retype, or change defaults on any entry

**Wait for final user confirmation before generating any configuration class.** Generate `DefaultXxxClientIntegrationServiceConfiguration` with exactly the confirmed properties — do not add fields the user did not confirm.

---

### 2.5–2.8 Configuration files in `configuration/`

**2.5 `XxxClientServiceConfiguration.java`** — marker interface:
```java
package com.viveworks.appbase.client.service.<xxx>.configuration;
import com.viveworks.appbase.client.core.AppbaseClientServiceConfiguration;
public interface XxxClientServiceConfiguration extends AppbaseClientServiceConfiguration {}
```

**2.6 `DefaultXxxClientServiceConfiguration.java`** — `@Immutable`, Jackson Builder:
```java
package com.viveworks.appbase.client.service.<xxx>.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@JsonDeserialize(builder = DefaultXxxClientServiceConfiguration.Builder.class)
public final class DefaultXxxClientServiceConfiguration implements XxxClientServiceConfiguration {

    private final DefaultXxxClientIntegrationServiceConfiguration defaultConfiguration;
    @Nullable private final Set<DefaultXxxClientIntegrationServiceConfiguration> keyedConfigurations;

    private DefaultXxxClientServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder.defaultConfiguration, "builder.defaultConfiguration");
        this.defaultConfiguration = builder.defaultConfiguration;
        this.keyedConfigurations = builder.keyedConfigurations == null ? null
                : Collections.unmodifiableSet(builder.keyedConfigurations);
    }

    public DefaultXxxClientIntegrationServiceConfiguration getDefaultConfiguration() {
        return defaultConfiguration;
    }

    @Nullable
    public Set<DefaultXxxClientIntegrationServiceConfiguration> getKeyedConfigurations() {
        return keyedConfigurations;
    }

    public static Builder builder() { return new Builder(); }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private DefaultXxxClientIntegrationServiceConfiguration defaultConfiguration;
        @Nullable private Set<DefaultXxxClientIntegrationServiceConfiguration> keyedConfigurations;

        public Builder defaultConfiguration(DefaultXxxClientIntegrationServiceConfiguration v) {
            this.defaultConfiguration = v; return this;
        }
        public Builder keyedConfigurations(Set<DefaultXxxClientIntegrationServiceConfiguration> v) {
            this.keyedConfigurations = v; return this;
        }
        public DefaultXxxClientServiceConfiguration build() {
            return new DefaultXxxClientServiceConfiguration(this);
        }
    }
}
```

**2.7 `XxxClientIntegrationServiceConfiguration.java`** — marker interface:
```java
package com.viveworks.appbase.client.service.<xxx>.configuration;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationServiceConfiguration;
public interface XxxClientIntegrationServiceConfiguration
        extends AppbaseClientIntegrationServiceConfiguration {}
```

**2.8 `DefaultXxxClientIntegrationServiceConfiguration.java`** — `@Immutable`, Jackson Builder, connection fields:
```java
package com.viveworks.appbase.client.service.<xxx>.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import javax.annotation.concurrent.Immutable;

@Immutable
@JsonDeserialize(builder = DefaultXxxClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultXxxClientIntegrationServiceConfiguration
        implements XxxClientIntegrationServiceConfiguration {

    private final V clientKey;
    private final HostPortConfiguration hostPortConfiguration;
    // TODO: add extra connection fields (timeout, auth, etc.) from SDK docs

    private DefaultXxxClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder.clientKey, "builder.clientKey");
        AppbaseUtil.notNull(builder.hostPortConfiguration, "builder.hostPortConfiguration");
        this.clientKey = builder.clientKey;
        this.hostPortConfiguration = builder.hostPortConfiguration;
    }

    public V getClientKey() { return clientKey; }
    public HostPortConfiguration getHostPortConfiguration() { return hostPortConfiguration; }
    public static Builder builder() { return new Builder(); }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private V clientKey;
        private HostPortConfiguration hostPortConfiguration;

        public Builder clientKey(V v) { this.clientKey = v; return this; }
        public Builder hostPortConfiguration(HostPortConfiguration v) {
            this.hostPortConfiguration = v; return this;
        }
        public DefaultXxxClientIntegrationServiceConfiguration build() {
            return new DefaultXxxClientIntegrationServiceConfiguration(this);
        }
    }
}
```

---

## Step 3: Bootstrap factory

### 3.1 `appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/client/factory/XxxClientServiceFactory.java`

```java
package com.viveworks.appbase.bootstrap.client.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServiceFactory;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.client.ClientServiceOrder;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.service.<xxx>.DefaultXxxClientService;
import com.viveworks.appbase.client.service.<xxx>.XxxClientService;
import com.viveworks.appbase.client.service.<xxx>.configuration.DefaultXxxClientServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.store.ConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import javax.annotation.Nullable;

public final class XxxClientServiceFactory implements AppbaseClientServiceFactory<XxxClientService> {

    @Nullable private final XxxClientService preBuilt;
    @Nullable private final DefaultXxxClientServiceConfiguration configuration;

    public XxxClientServiceFactory(final XxxClientService preBuilt) {
        AppbaseUtil.notNull(preBuilt, "preBuilt");
        this.preBuilt = preBuilt;
        this.configuration = null;
    }

    public XxxClientServiceFactory(final DefaultXxxClientServiceConfiguration configuration) {
        AppbaseUtil.notNull(configuration, "configuration");
        this.preBuilt = null;
        this.configuration = configuration;
    }

    public XxxClientServiceFactory(final String classpathYamlPath) {
        this(readConfiguration(classpathYamlPath));
    }

    public XxxClientServiceFactory(final ConfigurationStore store) {
        this(readConfiguration(store));
    }

    public XxxClientServiceFactory() { this("xxx-client-service.yaml"); }

    @Override public Class<XxxClientService> serviceType() { return XxxClientService.class; }
    @Override public int initializationOrder() { return ClientServiceOrder.XXX.order(); }

    @Override
    public void registerConfiguration(final AppbaseCoreServices coreServices) {
        DefaultXxxClientService.registerObjectMapperSubtypes();
    }

    @Override
    public XxxClientService create(final AppbaseCoreServices coreServices,
                                    final AppbaseClientServices partialClientServices) {
        if (preBuilt != null) return preBuilt;
        return new DefaultXxxClientService(coreServices.appbaseCore(), configuration);
    }

    private static DefaultXxxClientServiceConfiguration readConfiguration(final String path) {
        throw new UnsupportedOperationException(
                "TODO: implement — read DefaultXxxClientServiceConfiguration from " + path);
    }

    private static DefaultXxxClientServiceConfiguration readConfiguration(
            final ConfigurationStore store) {
        throw new UnsupportedOperationException(
                "TODO: implement — read DefaultXxxClientServiceConfiguration from ConfigurationStore");
    }
}
```

---

## Step 4: Tests

**4.1 `appbase-client/appbase-client-<xxx>/src/test/…/<xxx>/DefaultXxxClientServiceTest.java`** — unit test:
```java
// Arrange: DefaultXxxClientService with mock AppbaseCore + stubbed configuration (pre-built path)
// Act: start(); getClient(); shutdown()
// Assert: no exception; service reaches STARTED state
```

**4.2 `appbase-bootstrap/src/test/…/factory/XxxClientServiceFactoryTest.java`** — unit test:
```java
// Assert: serviceType() == XxxClientService.class
// Assert: initializationOrder() == ClientServiceOrder.XXX.order()
// Assert: create() with pre-built returns the same instance
```

**4.3 `appbase-client/appbase-client-<xxx>/src/test/…/<xxx>/XxxClientServiceIT.java`** — integration test (disabled):
```java
@Disabled("Enable after implementing DefaultXxxClientIntegrationService.initClient()")
class XxxClientServiceIT {
    // Start Testcontainers container for Xxx
    // Build service → write → read → assert round-trip
    // Stop container
}
```

---

## Step 5: Infra Docker scripts

Create `appbase-client/appbase-client-<xxx>/infra/docker/<xxx>/` with the following files.
Reference pattern: `appbase-client/appbase-client-aerospike/infra/docker/aerospike/`

**`env-xxx.sh`**:
```bash
#!/bin/bash
export HOSTNAME="xxx.local"
export CONTAINER_NAME="xxx.local"
export TAG="viveworks.com/infra/local/xxx"
```

**`Dockerfile`**: Based on the official Docker image for `xxx`. TODO: replace with the correct base image and any custom setup (ports, config, entrypoint).

**`build-xxx.sh`**:
```bash
#!/bin/bash
BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-xxx.sh
docker build -t ${TAG} .
```

**`run-xxx.sh`**:
```bash
#!/bin/bash
BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-xxx.sh
docker run \
    --detach \
    --hostname ${HOSTNAME} \
    --name ${CONTAINER_NAME} \
    --publish PORT:PORT \
    ${TAG}
# TODO: replace PORT with the actual port(s) exposed by the container
```

**`start-xxx.sh`**:
```bash
#!/bin/bash
BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-xxx.sh
docker start ${CONTAINER_NAME}
```

**`stop-xxx.sh`**:
```bash
#!/bin/bash
BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-xxx.sh
docker stop ${CONTAINER_NAME}
```

**`remove-xxx.sh`**:
```bash
#!/bin/bash
BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-xxx.sh
docker stop ${CONTAINER_NAME}
docker rm ${CONTAINER_NAME}
```

**`exec-xxx.sh`**:
```bash
#!/bin/bash
BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-xxx.sh
docker exec -it -u 0 ${CONTAINER_NAME} bash
```

**`README.md`**:
~~~~markdown
# Xxx Docker Container

- **First time:**

  ```shell
  ./build-xxx.sh
  ./run-xxx.sh
  ```

- **Subsequent start / stop:**

  ```shell
  ./start-xxx.sh
  ./stop-xxx.sh
  ```

- **Bash shell:**

  ```shell
  ./exec-xxx.sh
  ```

- **Remove permanently:**

  ```shell
  ./remove-xxx.sh
  ```
~~~~

---

## Step 6: Modify existing files

**5.1 `appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/client/ClientServiceOrder.java`**
Add the next available enum entry (inspect existing entries for the current max integer):
```java
XXX(N),  // replace N with next integer in sequence
```

**5.2 `appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/StackTestInfra.java`**
Add a check method and call it from `checkAll()`:
```java
private void checkXxx() {
    stackTestServices.getClientServices().get(XxxClientService.class).getClient();
    logger.info("Xxx client check OK");
}
```

---

## Step 7: YAML configurations (only if fully implemented — no stubs remaining)

If every method is fully implemented (no `UnsupportedOperationException` stubs), generate the following YAML files. YAML field names map 1-to-1 to `@JsonProperty` / `@JsonPOJOBuilder` fields on the configuration classes.

**`appbase-client/appbase-client-<xxx>/src/test/resources/appbase-client-<xxx>.yaml`** — integration test config:
```yaml
---
# TODO: populate with realistic values that match a locally running Xxx container
autoStart: true
defaultConfiguration:
  clientKey: "default"
  hostPortConfiguration:
    host: "localhost"
    port: PORT
  # TODO: add all fields from DefaultXxxClientIntegrationServiceConfiguration
keyedConfigurations: null
```

**`appbase-stacktest/src/main/resources/appbase-stacktest-<xxx>.yaml`** — stack test runtime config:
```yaml
---
# TODO: populate with values matching the stack test environment
autoStart: true
defaultConfiguration:
  clientKey: "default"
  hostPortConfiguration:
    host: "localhost"
    port: PORT
  # TODO: add all fields from DefaultXxxClientIntegrationServiceConfiguration
keyedConfigurations: null
```

---

## Implementation decision

Before generating code, decide:
```
Can I fully implement initClient() and shutdownClient() from the chosen library's public API?
  YES → Implement fully; write "Full implementation generated — review SDK calls" at end
  NO  → Generate stubs; list each stub with its TODO at end
```

## Step 8: Appbase Config Wizard smoke test

The `appbase-config-wizard` tool automatically scans the classpath for
Appbase configuration classes. Any new `DefaultXxxClientIntegrationServiceConfiguration`
added in Step 2 must appear in the wizard and produce valid YAML.

**8.1 Add a pexpect console test**

In `appbase-config-wizard/test/console/test_smoke.py`, add `'<xxx>'`
to the parametrize list in `TestNumericBooleanDefaults`:

```python
@pytest.mark.parametrize('filter_term', [
    'kafka', 'elasticsearch', 'redis', 'cassandra', '<xxx>',  # add this
])
```

If the config has nested objects, also add a dedicated class (see `TestZooKeeperConfig`
as pattern) that answers `'n'` to nested-field prompts and verifies the `└── end` marker
and valid YAML output.

**8.2 Web API regression**

The existing `W-nested` Playwright test in `api.spec.ts` covers all configs automatically.
After rebuilding the JAR, run:

```bash
curl -s http://localhost:8080/api/configurations | jq '[.[].simpleName] | map(select(test("<Xxx>";"i")))'
# Expected: ["DefaultXxxClientIntegrationServiceConfiguration"]
```

**8.3 Nested sub-form (web UI)**

If the config has nested fields (e.g. `HostPortConfiguration`), verify in the Flutter web UI:
- Nested field card shows **"Set Values"** button, not a text input.
- Clicking opens a sub-form with breadcrumb `ConfigName › fieldName`.
- "Apply" saves the nested values; "Cancel" discards them.

---

## Stub quality contract

- All files compile with zero errors before any stubs are filled in.
- Every stub throws `UnsupportedOperationException("TODO: implement — <one-line description>")`.
- Unit tests pass using the pre-built constructor path. Integration test is `@Disabled`.
- At end of output: list every stub not fully implemented.
