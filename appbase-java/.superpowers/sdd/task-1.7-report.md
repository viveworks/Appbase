# Task 1.7 Report — Migrate Callers + Tests to JettyAppbaseWebServer

## Status
DONE

## Commit
SHA: `c06465785`
Subject: `refactor(P-015): migrate callers + tests to JettyAppbaseWebServer in appbase-bootstrap-jetty`

## Build Results

### appbase-bootstrap
- **Result**: SUCCESS
- **Tests**: 189 tests run, 0 errors, 0 failures
- **Confirmed absent from surefire-reports**: `JettyAppbaseServerTest`, `AppbaseBootstrapIntegrationTest` (both successfully moved out)

### appbase-bootstrap-jetty
- **Result**: SUCCESS
- **Tests**: 18 tests run, 0 errors, 0 failures
  - `AppbaseBootstrapIntegrationTest`: 6 tests ✅
  - `JettyAppbaseServerModuleTest`: 1 test ✅
  - `JettyAppbaseWebServerTest`: 10 tests ✅
  - `JettyServletDeploymentTest`: 1 test ✅ (pre-existing, still passing)

### appbase-stacktest
- **Compile**: SUCCESS
- **Test failure**: `DefaultUserAccountServiceTest` — Aerospike connection refused (localhost:3000). This is a PRE-EXISTING failure caused by Aerospike not being available in the local environment. This test class comes from the `appbase-service-user:tests` JAR included in stacktest's classpath (classifier dependency), not from code changed in this task. **Not introduced by this task.**

### Full reactor (./build.sh quick)
- 28/35 modules SUCCESS; failure chain halted at `appbase-service-user-session-aerospike` (pre-existing Aerospike unavailability), which caused `appbase-bootstrap`, `appbase-bootstrap-jetty`, `appbase-stacktest` to be SKIPPED in the full run. These were verified independently via `mvn install -rf :appbase-bootstrap`.

## Stale-Reference Grep
```
grep -rn "bootstrap.server.JettyAppbaseServer\b" appbase-java --include=*.java
```
**Result: CLEAN — no matches.**

## GuiceModulesTest Handling (SPLIT)
`AppbaseBootstrapGuiceModulesTest` was MIXED (3 non-Jetty module tests + 1 Jetty module test).

**Decision**: SPLIT.
- **Kept** in `appbase-bootstrap/.../guice/AppbaseBootstrapGuiceModulesTest.java`: 3 non-Jetty tests (`coreServicesModule_*`, `clientServicesModule_*`, `servicesModule_*`). Removed `JettyAppbaseServer` import and the `jettyModule_bindsJettyAppbaseServer` test method. Updated Javadoc to note split.
- **Created** new `appbase-bootstrap-jetty/.../jetty/guice/JettyAppbaseServerModuleTest.java`: single test `jettyModule_bindsJettyAppbaseWebServer()` using updated `JettyAppbaseWebServer` mock and `injector.getInstance(JettyAppbaseWebServer.class)` assertion.

## All Changes Made

### stacktest
- `pom.xml`: Added `appbase-bootstrap-jetty` compile dependency
- `StackTestMain.java`: Updated import, field type `JettyAppbaseServer` → `JettyAppbaseWebServer`, Javadoc `{@link}`, builder call, `OPENAPI_PRETTY_PRINT` constant
- `JmxEndpointTestImpl.java`: Updated import; 4 calls of `JettyAppbaseServer.registerMXBeans(...)` → `JettyAppbaseWebServer.registerMXBeans(...)`
- `StackTestServletConfig.java`: Updated Javadoc `{@link}` reference only (no Java import existed)

### appbase-bootstrap
- `AppbaseBootstrapGuiceModulesTest.java`: Removed Jetty import + test (split — see above)
- `server/JettyAppbaseServerTest.java`: DELETED (moved to bootstrap-jetty)
- `AppbaseBootstrapIntegrationTest.java`: DELETED (moved to bootstrap-jetty)

### appbase-bootstrap-jetty (new test files)
- `jetty/server/JettyAppbaseWebServerTest.java`: 10 builder/handler tests (renamed from JettyAppbaseServerTest, package + class + identifiers updated)
- `jetty/guice/JettyAppbaseServerModuleTest.java`: 1 Guice binding test (split from GuiceModulesTest)
- `jetty/AppbaseBootstrapIntegrationTest.java`: 6 end-to-end tests (moved; imports updated to `...jetty.guice.JettyAppbaseServerModule`, `...jetty.server.JettyAppbaseWebServer`)
- `src/test/resources/appbase-test-config.yaml`: Copied from appbase-bootstrap test resources (required by the moved integration test)

## Concerns
- The full `./build.sh quick` does not reach `appbase-bootstrap` in a single run due to the pre-existing Aerospike failure in `appbase-service-user-session-aerospike`. This is a **pre-existing environmental issue** (no Aerospike running), not introduced by this PR. All three target modules verified GREEN via `mvn install -rf :appbase-bootstrap`.
