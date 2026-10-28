# Configuration reference — Release 1.0.0

This page describes how an Appbase service finds and reads its configuration. Every key and default
below is taken from the source file cited next to it. If this page and the source disagree, the
source is right and this page is a defect.

For the configuration of each module and adapter, use the module and service catalogue. To generate a
valid file without writing YAML by hand, use the [configuration wizard](#the-configuration-wizard).

## Build prerequisites

| Requirement | Value | Where it is enforced |
|---|---|---|
| JDK | **25 or later** | `jdk.version` in [`pom.xml`](../pom.xml), the Maven Enforcer `requireJavaVersion [25,)` rule, and the preflight in [`build.sh`](../build.sh), which refuses to start on an older JDK |
| Maven | **3.6.3 or later** | the Maven Enforcer `requireMavenVersion [3.6.3,]` rule in [`pom.xml`](../pom.xml) |
| git | any recent version | Maven's `git-commit-id-plugin` stamps the native checkout metadata |
| Python | Python 3, available as **`python3`** | the API-inventory check called unconditionally by [`build.sh`](../build.sh) |
| ripgrep | available as **`rg`** | the bounded manifest-coverage scan in [`build.sh`](../build.sh), which fails closed when the scan cannot run |
| bash | any version supported by [`build.sh`](../build.sh), including macOS bash 3.2 | the repository build and wizard entry points are bash scripts |
| Flutter | only for the configuration wizard's web UI | off by default. Turn it on with `./build.sh --wizard-frontend` |
| Docker | only for backend integration tests and the broader stacktest | see [Integration test switches](#integration-test-switches) |

Coordinates: group `com.viveworks.appbase`, version `1.0.0-SNAPSHOT`. There is no published Maven
repository yet (see [Known limitations](Known-Limitations.md)), so install from source with Maven.

## Choosing the configuration file

At startup, Appbase core can read **one YAML file**
(`appbase-java/appbase-core/src/main/java/com/viveworks/appbase/core/util/AppbaseConstants.java`).

| Setting | System property | Environment variable | Default |
|---|---|---|---|
| Configuration file | `appbase.configuration.file.path` | `APPBASE_CONFIGURATION_FILE_PATH` | optional `classpath:///appbase-core.yaml` for the bootstrap, if present; otherwise built-in core defaults |
| Environment name | `appbase.environment` | `APPBASE_ENVIRONMENT` | empty |
| Base directory | `appbase.base.directory` | — | `/opt/appbase` (`C:\Program Files\Appbase` on Windows) |
| Bootstrap node name | `appbase.bootstrap.configuration.node.name` | `APPBASE_BOOTSTRAP_CONFIGURATION_NODE_NAME` | none — `YamlUtil` selects the node when this is unset |
| Configuration-service node name | `appbase.configurationServiceConfiguration.yamlNodeName` | `APPBASE_CONFIGURATION_SERVICE_CONFIGURATION_YAML_NODE_NAME` | `defaultConfigurationServiceConfiguration` |
| Mediator-service node name | `appbase.mediatorServiceConfiguration.yamlNodeName` | `APPBASE_MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME` | `defaultMediatorServiceConfiguration` |

- **Precedence.** When both are set, the system property wins. The environment variable is used only
  when the property is blank (`AppbaseUtil.propertyOrEnv`).
- **Paths.** A value that starts with `classpath://` is loaded from the classpath. Any other value is
  read as a filesystem path (`YamlUtil`).
- **An explicitly selected file must load.** If the path selected by the system property or
  environment variable is missing, unreadable, unparsable, or contains no bootstrap configuration,
  `DefaultAppbaseCore` refuses startup with `AppbaseBootstrapConfigurationException`. Check the
  selected path and YAML when that exception occurs; it does not fall back to defaults. When neither
  setting is present, the no-argument constructor probes the optional bundled
  `classpath:///appbase-core.yaml` and loads it if present; if that file is absent, core uses its
  built-in defaults. This contract is implemented in
  [`DefaultAppbaseCore`](../appbase-java/appbase-core/src/main/java/com/viveworks/appbase/core/DefaultAppbaseCore.java)
  and [`DefaultAppbaseCoreBootstrap`](../appbase-java/appbase-core/src/main/java/com/viveworks/appbase/core/bootstrap/DefaultAppbaseCoreBootstrap.java),
  and covered by
  [`DefaultAppbaseCoreTest`](../appbase-java/appbase-core/src/test/java/com/viveworks/appbase/core/DefaultAppbaseCoreTest.java).

## File format

The file is YAML, parsed with Jackson. Each service reads its own top-level node. The bootstrap
loader, `AppbaseCoreServices.builder().fromYaml(path)`, expects a top-level
`appbaseCoreServicesConfiguration` node:

```yaml
appbaseCoreServicesConfiguration:
  logServiceConfiguration:
    logbackXml: null
    defaultLogAccessorConfiguration:
      enabled: true
```

Complete, working files in the tree:

- [`appbase-stacktest/src/main/resources/appbase-stacktest-bootstrap.yaml`](../appbase-stacktest/src/main/resources/appbase-stacktest-bootstrap.yaml),
  the configuration the stacktest boots with
- [`appbase-java/appbase-bootstrap/appbase-bootstrap-core/src/test/resources/appbase-test-config.yaml`](../appbase-java/appbase-bootstrap/appbase-bootstrap-core/src/test/resources/appbase-test-config.yaml)
- [`appbase-java/appbase-core/src/test/resources/appbase-core.yaml`](../appbase-java/appbase-core/src/test/resources/appbase-core.yaml)

> ⚠️ **YAML values are not interpolated.** `${VAR}` and `${VAR:-default}` reach your code as literal
> text. This behaviour is pinned by
> [`YamlEnvSubstitutionSpikeTest`](../appbase-java/appbase-core/src/test/java/com/viveworks/appbase/core/util/jackson/YamlEnvSubstitutionSpikeTest.java).
> To vary a value between environments, choose a different file with
> `APPBASE_CONFIGURATION_FILE_PATH` rather than putting placeholders in one file. Separately, the
> configuration service's system store exposes every system property and environment variable to
> accessors at run time.

## Declaring a service's configuration

A configuration class implements `AppbaseConfiguration`, which extends `Validatable`, so a
configuration validates itself when the service loads it. A managed service's configuration
implements `AppbaseManagedServiceConfiguration`, or extends
`AbstractAppbaseManagedServiceConfiguration`, which supplies a Jackson builder. Both types live in
`appbase-core` under `com.viveworks.appbase.core.service`.

## Choosing the server

No bootstrap module has a `main` method. An application assembles its services with the builders
(`AppbaseCoreServices`, `AppbaseClientServices`, `AppbaseServices`). It then starts one
`AppbaseWebServer` implementation, and the module you depend on decides which one:

| Module | Server builder |
|---|---|
| `appbase-bootstrap-servlet-jetty` | `JettyServletContainer.builder()` |
| `appbase-bootstrap-servlet-tomcat` | `TomcatServletContainer.builder()` |
| `appbase-bootstrap-servlet-undertow` | `UndertowServletContainer.builder()` |
| `appbase-bootstrap-reactor-netty` | `ReactorNettyAppbaseWebServer.builder()` |

Optional Guice modules wrap the same builders. The reference bootstrap is
[`StackTestMain`](../appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/StackTestMain.java),
which chooses a server with `--container jetty|undertow|tomcat` (Jetty by default). It states its own
limitation: under Tomcat and Undertow it wires only the core services.

## The configuration wizard

`appbase-config-wizard` generates a valid YAML file for an Appbase configuration class, either
interactively in the console or through a web UI. See its
[README](../appbase-config-wizard/README.md).

| Mode | Command | Notes |
|---|---|---|
| Console | `appbase-config-wizard/bin/start-console.sh` | builds the JAR first if it is missing (`-DskipTests`, so the test-jars other modules need are still built); see [Example 4](Examples.md#4-generate-a-configuration-file-with-the-wizard) |
| Web | `appbase-config-wizard/bin/start-web.sh [port]` | port 8080 by default. Stop it with `bin/stop-web.sh`. Serving the UI requires a build with `--wizard-frontend` |

The generated file is written to the path you give, or to `$TMPDIR/appbase-<ClassName>-<timestamp>.yaml`.

## Integration test switches

Backend integration tests are **off by default**. [`build.sh`](../build.sh) turns each one on from an
environment variable, for example:

```sh
AEROSPIKE_CLIENT_SERVICE_TESTS_ENABLED=true ./build.sh develop
KAFKA_CLIENT_SERVICE_TESTS_ENABLED=true ./build.sh develop
```

For the supported adapters, the switches in `INT_FLAGS` on this revision are:
`AEROSPIKE_`, `CASSANDRA_`, `DRUID_`, `ELASTICSEARCH_`, `FIREBASE_AUTH_`, `JDBC_`, `JDBI_`, `JPA_`,
`KAFKA_`, `R2DBC_`, `REDIS_`, `S3_`, `VITESS_` and `ZOOKEEPER_CLIENT_SERVICE_TESTS_ENABLED`. The
switches that also exist for deferred adapters are listed in
[Known limitations](Known-Limitations.md). All of these tests need Docker and a backend, so on
this page they are **NOT MEASURED**.
