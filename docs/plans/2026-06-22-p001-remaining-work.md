# P-001 Remaining Work — Post internal work item 190

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the stacktest simplification: rewrite `StackTestMain` to use bootstrap builders, delete `stacktest/AppbaseServices.java` and `StackTestServer.java`, migrate all 150+ references to bootstrap types.

**Architecture:** After internal work item 190 merges, bootstrap has all necessary infrastructure. This plan wires stacktest to use it end-to-end.

**Tech Stack:** Java 25, Maven multi-module, Jetty 11, Jersey 3.1, Mockito 3

## Global Constraints

- JDK 25, Maven 3.6+
- Build via `./build.sh quick` from `appbase-java/`; tests pass before each commit
- TDD: write failing test first, then implementation
- No backwards-compat shims — change callers directly
- Branch naming: `IssueNumber/Java/Stacktest/Refactor/Title`
- PR targeting `develop`; await human merge before post-merge steps

---

## Status: What internal work item 190 Delivers

| Area | Status |
|---|---|
| `appbase-bootstrap` module | ✅ done |
| Builder pattern (Core/Client/AppbaseServices/Jetty) | ✅ done |
| 20 client service factories in bootstrap | ✅ done |
| 11 service factories in bootstrap (domain factories confirmed OK — see Phase 0 note) | ✅ done |
| 4 Guice modules | ✅ done |
| `registerConfiguration()` hook + `withDefaultExceptionStores()` | ✅ done |
| `JettyAppbaseServer.withDefaultMXBeans/withServletContext/withAdditionalHandler` | ✅ done |
| `stacktest/AppbaseServices.java` **rewritten to use bootstrap internally** | ✅ done |
| Factory coverage audit + renames | ❌ not done |
| `StackTestMain` startup rewritten to bootstrap builders | ❌ not done |
| `StackTestServer.java` deleted | ❌ not done |
| `stacktest/AppbaseServices.java` deleted | ❌ not done |

---

## Phase 0 — Factory Coverage Audit + Renames (Issue: create new GitHub issue)

**Branch:** `XXX/Java/Bootstrap/Refactor/Factory-coverage-and-renames`

**Context:**
- Domain factories (`UserSessionStoreFactory`, `UserAccountStoreFactory`, etc.) belong in `appbase-bootstrap` — they depend on `appbase-service`, not stacktest. This was already the correct placement in internal work item 190.
- Factory names must reflect the concrete implementation they instantiate so users can easily find the right factory for a given technology.
- Every `AppbaseClientService` impl in `appbase-client` and every `AppbaseService` impl in `appbase-service` must have a corresponding factory in bootstrap.

### Factory Rename Table

All files live in `appbase-bootstrap/src/main/java/com/viveworks/appbase/bootstrap/service/factory/`.

| Current name | New name | Concrete impl |
|---|---|---|
| `ClusterServiceFactory` | `StaticClusterServiceFactory` | `StaticClusterService` |
| `UserSessionStoreFactory` | `AerospikeUserSessionStoreFactory` | `DefaultUserSessionStore` (Aerospike-backed) |
| `UserAccountStoreFactory` | `JdbiUserAccountStoreFactory` | `JdbiUserAccountStore` |
| `UserAccountServiceFactory` | `DefaultUserAccountServiceFactory` | `DefaultUserAccountService` |
| `SupportMessageStoreFactory` | `JdbiSupportMessageStoreFactory` | `JdbiSupportMessageStore` |
| `SupportMessageServiceFactory` | `DefaultSupportMessageServiceFactory` | `DefaultSupportMessageService` |
| `FirebaseAuthServiceFactory` | `DefaultFirebaseAuthServiceFactory` | `DefaultFirebaseAuthService` |
| `HealthCheckServiceFactory` | `DefaultHealthCheckServiceFactory` | `DefaultHealthCheckService` |
| `IdServiceFactory` | `DefaultIdServiceFactory` | `DefaultIdService` |
| `PasswordServiceFactory` | `DefaultPasswordServiceFactory` | `DefaultPasswordService` |
| `AuthorizationServiceFactory` | `DefaultAuthorizationServiceFactory` | `DefaultAuthorizationService` |

---

### Task 0.1: Audit all AppbaseClientService implementations

Find any `appbase-client` services missing a factory in `appbase-bootstrap/client/factory/`.

- [ ] Run:
  ```bash
  find appbase-java/appbase-client/src/main/java -name "Default*.java" | \
    xargs grep -l "implements.*AppbaseClientService\|extends.*Abstract.*ClientService" | \
    sed 's|.*/||;s|\.java||' | sort
  ```
- [ ] Cross-reference against existing factories:
  ```
  20 existing factories: Aerospike, AwsSdkAsyncHttp, AwsSdkHttp, Cassandra, ClamAV,
  DruidSql, Elasticsearch, FirebaseApp, FirebaseAuth, Jasync, Jpa,
  KafkaConsumer, KafkaProducer, S3Clients, Smtp, Spark,
  VitessGrpc, VitessJdbc, VitessJdbi, ZooKeeper
  ```
- [ ] Known gap: `ElasticsearchRestClient` / `DefaultElasticsearchRestClient` (high-level; wraps low-level `ElasticsearchClientService`) — **add factory `ElasticsearchRestClientFactory`** if confirmed present:
  ```bash
  find appbase-java/appbase-client/src/main/java -name "DefaultElasticsearchRestClient.java"
  ```
- [ ] For any other missing services found, create a factory following the pattern of `ElasticsearchClientServiceFactory.java`.
- [ ] **Write test** for each new factory: classpath-file constructor → `registerConfiguration()` registers stores → `create()` returns non-null service instance.
- [ ] **Run — verify PASS**
- [ ] **Commit**: `feat(bootstrap): add missing client service factories (ElasticsearchRest, ...)`

---

### Task 0.2: Audit all AppbaseService implementations in appbase-service

Confirm the rename table above is complete — no new services added since internal work item 190.

- [ ] Run:
  ```bash
  find appbase-java/appbase-service/src/main/java \
    -name "Default*.java" -o -name "Static*.java" -o -name "Jdbi*.java" | \
    grep -v "Test\|Abstract\|Configuration\|Filter\|Checker\|Factory" | \
    sed 's|.*/||;s|\.java||' | sort
  ```
- [ ] Confirm each has a factory in `appbase-bootstrap/service/factory/`. Add any missing.
- [ ] **Commit** if new factories added.

---

### Task 0.3: Rename service factories

Rename all 11 service factories per the table in Task 0.

- [ ] For each factory in the table: rename file, update class declaration, update all import references.
  - Start with the simplest rename (one grep to find callers):
    ```bash
    grep -rn "ClusterServiceFactory\|UserSessionStoreFactory\|UserAccountStoreFactory" \
      appbase-java/ --include="*.java" | grep -v "\.class"
    ```
- [ ] **Run build** after each rename:
  ```bash
  mvn clean install -pl appbase-bootstrap -P dev-profile,jacoco-no-rules-profile
  ```
- [ ] **Commit** per factory or in one batch: `refactor(bootstrap): rename service factories with implementation-specific prefixes`

---

### Task 0.4: PR for Phase 0

- [ ] Create PR targeting `develop`.
- [ ] Run `code-review` plugin; fix confirmed/plausible findings.
- [ ] Await human merge.

---

## Phase A — Rewrite StackTestMain (Issue: create new GitHub issue)

**Prerequisite:** Phase 0 merged (so factory names are correct in StackTestMain rewrite).

**Branch:** `XXX/Java/Stacktest/Refactor/Rewrite-StackTestMain-bootstrap`

**Note on `stacktest/AppbaseServices.java` complexity:** This class uses bootstrap internally but still wires many clients and app services directly. The complexity is acceptable — this class will be deleted entirely in Phase B. No cleanup needed in Phase A.

**Files:**
- Modify: `appbase-java/appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/service/StackTestServices.java`
- Modify: `appbase-java/appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/StackTestMain.java`

---

### Task A.1: Update StackTestServices to accept bootstrap types

Currently `StackTestServices(stacktest.AppbaseServices)`. Change to accept `AppbaseCoreServices`, `AppbaseClientServices`, and bootstrap's `AppbaseServices`.

- [ ] **Read** `StackTestServices.java` — list all getters and what services they delegate to.
- [ ] **Write failing test** — instantiate `StackTestServices` with bootstrap types, assert getters return correct services.
- [ ] **Run — verify FAIL**
- [ ] **Implement** new constructor(s). Keep old constructor as `@Deprecated` if any test still uses it; update callers in the same commit.
- [ ] **Run — verify PASS**
- [ ] **Commit**: `refactor(stacktest): StackTestServices accepts bootstrap service containers`

---

### Task A.2: Verify or add ElasticsearchRestClientFactory

(Only needed if not already added in Phase 0.)

- [ ] Check `stacktest/AppbaseServices.java` for any `ElasticsearchRestClient` getter.
- [ ] If present and factory missing: create `appbase-bootstrap/src/main/java/.../client/factory/ElasticsearchRestClientFactory.java` — classpath-file constructor, `registerConfiguration()`, `create()` delegates to `DefaultElasticsearchRestClient(lowLevelService)`. Add unit test. Commit.
- [ ] If absent: skip.

---

### Task A.3: Rewrite StackTestMain

Replace:
```java
final AppbaseServices localAppbaseServices = new AppbaseServices();
stackTestServices = new StackTestServices(localAppbaseServices);
stackTestServer = new StackTestServer(stackTestServices);
```

With the full bootstrap builder chain:
```java
AppbaseCoreServices coreServices = AppbaseCoreServices.builder()
    .fromYaml("classpath:///appbase-stacktest-bootstrap.yaml")
    .withDefaultExceptionStores()
    .build();

AppbaseClientServices clientServices = AppbaseClientServices.builder()
    .from(coreServices)
    .with(new AerospikeClientServiceFactory("classpath:///aerospike.yaml"))
    .with(new ElasticsearchClientServiceFactory("classpath:///elasticsearch.yaml"))
    // ... other client services used by stacktest
    .build();

bootstrap.AppbaseServices appbaseServices = bootstrap.AppbaseServices.builder()
    .from(coreServices)
    .from(clientServices)
    // ... app service factories
    .build();

stackTestServices = new StackTestServices(coreServices, clientServices, appbaseServices);

JettyAppbaseServer server = JettyAppbaseServer.builder()
    .from(appbaseServices)
    .withDefaultMXBeans()
    .withServletContext(ctx -> {
        // servlet + filter registration (moved from StackTestServer.start())
    })
    .build();
```

**Steps:**
- [ ] **Read** `StackTestServer.java` lines 200–700 to extract complete servlet/filter registration block.
- [ ] **Write test**: `StackTestMain.startServices()` completes without NPE when bootstrap config YAML is present. Mock out external infra (Aerospike, ES, etc.) via environment guard already in StackTestMain.
- [ ] **Run — verify FAIL** (StackTestServer still in use)
- [ ] **Implement** new `StackTestMain` body.
- [ ] **Run — verify PASS**
- [ ] **Commit**: `refactor(stacktest): rewrite StackTestMain to use bootstrap builder chain`

---

### Task A.4: Full build check

- [ ] `mvn clean install -pl appbase-bootstrap -P dev-profile,jacoco-no-rules-profile` — 199+ tests PASS
- [ ] `mvn clean install -pl appbase-stacktest -P dev-profile,jacoco-no-rules-profile` — PASS
- [ ] **Commit** any fixup needed.

---

### Task A.5: PR for Phase A

- [ ] Create PR targeting `develop`.
- [ ] Run `code-review` plugin; fix confirmed/plausible findings.
- [ ] Await human merge.

---

## Phase B — Full Deletion: stacktest/AppbaseServices + StackTestServer (Path B)

**Prerequisite:** Phase A merged.

**Branch:** `XXX/Java/Stacktest/Refactor/Delete-AppbaseServices-StackTestServer`

This is large (~150 files) but mechanical. All callers use typed getters like `appbaseServices.getAerospikeClientService()` — replace with `clientServices.get(AerospikeClientService.class)` or a bootstrap accessor.

**Files:**
- Delete: `appbase-java/appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/AppbaseServices.java`
- Delete: `appbase-java/appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/StackTestServer.java`
- Modify: ~138 files referencing `stacktest.AppbaseServices`
- Modify: ~19 files referencing `StackTestServer`

---

### Task B.1: Audit all references

- [ ] Run:
  ```bash
  grep -rn "stacktest\.AppbaseServices\|import.*stacktest.*AppbaseServices\|new AppbaseServices()" \
    appbase-java/appbase-stacktest/src --include="*.java" | sort -t: -k1,1 > /tmp/appbase-refs.txt
  ```
- [ ] Run:
  ```bash
  grep -rn "StackTestServer" appbase-java/appbase-stacktest/src --include="*.java" > /tmp/server-refs.txt
  ```
- [ ] Review both files. Group callers by type:
  - API test classes (`XxxApiTest`) — call `runTests(appbaseServices, ...)`
  - Servlet/filter classes — take `AppbaseServices` as constructor arg
  - JAX-RS resources — same

---

### Task B.2: Replace AppbaseServices in test classes

All `XxxApiTest.runTests(appbaseServices, ...)` callers. Replace `stacktest.AppbaseServices` parameter with bootstrap's `AppbaseServices` (or `AppbaseClientServices` directly if only client services are used).

- [ ] **Write failing compile check**: remove import of `stacktest.AppbaseServices` from one test class, fix it, confirm build passes.
- [ ] **Migrate all test classes** in batches (group by domain: aerospike tests, ES tests, etc.).
- [ ] **Commit** per batch: `refactor(stacktest): migrate XxxApiTest callers to bootstrap AppbaseServices`

---

### Task B.3: Replace AppbaseServices in servlet/JAX-RS classes

`AbstractStackTestServlet` and concrete servlets take `StackTestServices` or `AppbaseServices`. Replace with bootstrap types.

- [ ] **Read** `AbstractStackTestServlet.java` constructor signature.
- [ ] **Write test**: `AbstractStackTestServlet` constructed with bootstrap services, getters return correct values.
- [ ] **Migrate** `AbstractStackTestServlet` + all concrete servlet subclasses.
- [ ] **Commit**: `refactor(stacktest): migrate servlets to bootstrap service containers`

---

### Task B.4: Verify zero remaining references

- [ ] Run:
  ```bash
  grep -rn "stacktest\.AppbaseServices\|StackTestServer" appbase-java/appbase-stacktest/src --include="*.java"
  ```
  Must return empty.

---

### Task B.5: Delete the files

- [ ] `git rm appbase-java/appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/AppbaseServices.java`
- [ ] `git rm appbase-java/appbase-stacktest/src/main/java/com/viveworks/appbase/stacktest/StackTestServer.java`
- [ ] Build:
  ```bash
  ./build.sh quick
  ```
  (ignoring pre-existing appbase-core/appbase-service failures documented in internal work item 190)
- [ ] **Commit**: `refactor(stacktest): delete AppbaseServices.java and StackTestServer.java — bootstrap replaces both`

---

### Task B.6: PR for Phase B

- [ ] Create PR targeting `develop`.
- [ ] Run `code-review` plugin; fix findings.
- [ ] Await human merge.

---

## Out of Scope (Deferred)

- `GuiceStackTestServer.java` / `GuiceStackTestMain.java` — P-001 spec mentioned a Guice-based stacktest demo. Not needed for functional correctness; create separate issue if desired.

---

## File Change Summary

### Phase 0
- Renamed: 11 service factories in `appbase-bootstrap/service/factory/`
- Possibly new: `ElasticsearchRestClientFactory.java` + factories for any other missing client services

### Phase A
- Modified: `StackTestServices.java`
- Modified: `StackTestMain.java`

### Phase B
- Deleted: `stacktest/AppbaseServices.java`
- Deleted: `StackTestServer.java`
- Modified: ~138 test + servlet + JAX-RS files
