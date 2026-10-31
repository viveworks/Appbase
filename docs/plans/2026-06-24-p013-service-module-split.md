# P-013 Implementation Plan — appbase-service Module Split

**Date:** 2026-06-24
**Spec:** `docs/specs/P-013-service-module-split-spec.md`
**Prereq:** P-012 merged ✅ (PR internal work item 204)
**Branch:** create from develop after Gate 0

---

## What we're doing

Split `appbase-service-core` (228 files) into focused modules — separating technology-specific implementations from domain interfaces. `appbase-service-supportmessage` also splits.

### Final module structure

```
appbase-service (parent pom)
├── appbase-service-core
├── appbase-service-password
├── appbase-service-web
├── appbase-service-user
│   (interfaces + data + web endpoints — no client deps)
├── appbase-service-user-account-firebaseauth
├── appbase-service-user-session-aerospike     ← NEW: Aerospike session store impl
├── appbase-service-user-account-jdbi          ← NEW: JDBI account store impl
├── appbase-service-user-account-vitess        ← NEW: Vitess-specific wiring / health
├── appbase-service-supportmessage
│   (interfaces + data + web endpoints — no JDBI dep)
└── appbase-service-supportmessage-jdbi        ← NEW: JDBI supportmessage store impl
```

### Dependency graph (no cycles)

```
appbase-service-core
    ▲
    ├── appbase-service-password
    ├── appbase-service-web
    │       ▲
    │       └── appbase-service-user
    │               (UserSessionStore, UserAccountStore interfaces + data + web)
    │               ▲
    │               ├── appbase-service-user-account-firebaseauth
    │               ├── appbase-service-user-session-aerospike
    │               └── appbase-service-user-account-jdbi
    │                       ▲
    │                       └── appbase-service-user-account-vitess
    │
    └── appbase-service-supportmessage
            (SupportMessageStore interface + data + web)
            ▲
            └── appbase-service-supportmessage-jdbi
```

### What stays in `appbase-service-user` after split

`appbase-service-user` becomes interface + data + web only — **no client deps**:
- `UserAccountStore.java`, `UserAccountStoreException.java` (interface)
- `UserSessionStore.java`, `UserSessionStoreException.java` (interface)
- All `service.user.account.data.*`
- All `service.user.session.data.*`
- All `service.user.account.web.*`
- All `service.user.session.web.*`
- Web filters, endpoints

### What stays in `appbase-service-supportmessage` after split

- `SupportMessageStore.java` (interface)
- `SupportMessage.java`, `SupportMessageService.java`, `SupportMessageListener.java`
- `DefaultSupportMessageService.java`, `DefaultSupportMessageServiceConfiguration.java`
- All `data.*`
- All web endpoints (`endpoint/jaxrs/`, `endpoint/servlet/`)

---

## Package assignments for new tech-specific modules

| Module | Package | Files moving there |
|---|---|---|
| `appbase-service-user-session-aerospike` | `com.viveworks.appbase.service.user.session.aerospike` | `DefaultUserSessionStore`, `DefaultUserSessionStoreConfiguration`, `DefaultUserSessionStoreHealthChecker` |
| `appbase-service-user-account-jdbi` | `com.viveworks.appbase.service.user.account.jdbi` | `JdbiUserAccountStore`, `JdbiUserAccountStoreConfiguration`, `JdbiUserAccountStoreHealthChecker` |
| `appbase-service-user-account-vitess` | `com.viveworks.appbase.service.user.account.vitess` | Vitess-specific wiring (see Task 5d note) |
| `appbase-service-supportmessage-jdbi` | `com.viveworks.appbase.service.supportmessage.jdbi` | `JdbiSupportMessageStore`, `JdbiSupportMessageStoreConfiguration`, `JdbiSupportMessageStoreHealthChecker` |

**Package rename note:** Moving files to tech-specific packages requires updating the `package` declaration in each file AND updating all import sites. Class names like `DefaultUserSessionStore` may optionally be renamed to `AerospikeUserSessionStore` for clarity — decide at implementation time.

---

## Gate 0 — Baseline

Run before creating the branch. Record test count.

```bash
cd appbase-java
mvn clean install -pl appbase-service -am --no-transfer-progress -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS, 18 tests in service-core.

---

## Task 1 — Source changes in service-core (break circular compile deps)

These 4 targeted edits allow service-core to compile without referencing classes that will live in downstream modules.

**File:** `appbase-service-core/src/main/java/com/viveworks/appbase/service/AppbaseServiceConstants.java`

```java
// Line 81 — removes compile-time dep on service-web (MetricJaxrsRequestFilter class literal)
// BEFORE: MetricJaxrsRequestFilter.class.getSimpleName() + "-timerContext"
// AFTER:  "MetricJaxrsRequestFilter" + "-timerContext"

// Line 106 — removes compile-time dep on service-user (UserSession class literal)
// BEFORE: UserSession.class.getName()
// AFTER:  "com.viveworks.appbase.service.user.session.data.UserSession"
```

**File:** `.../service/web/endpoint/jaxrs/AbstractAppbaseJaxrsEntity.java`
- Remove imports: `AuthenticationServiceException`, `UserAccountServiceException`

**File:** `.../service/web/endpoint/servlet/AbstractAppbaseSyncServlet.java`
- Same import removals

**Verify:** `mvn compile -pl appbase-service/appbase-service-core -am -q` → must pass.

> `AppbaseHttpServletRequestWrapper.java` stays in service-core until Task 5 creates service-user.

---

## Task 2 — Create new module pom.xml files

No source files yet. Establish module structure and dep declarations.

### 2a — `appbase-service-password/pom.xml`
```xml
<artifactId>appbase-service-password</artifactId>
<dependencies>
    <dependency>appbase-service-core</dependency>
    <dependency>bcprov-jdk18on</dependency>
</dependencies>
```

### 2b — `appbase-service-web/pom.xml`
```xml
<artifactId>appbase-service-web</artifactId>
<dependencies>
    <dependency>appbase-service-core</dependency>
    <!-- Jakarta Servlet + JAX-RS come transitively via appbase-core -->
</dependencies>
```

### 2c — `appbase-service-user/pom.xml`
```xml
<artifactId>appbase-service-user</artifactId>
<dependencies>
    <dependency>appbase-service-core</dependency>
    <dependency>appbase-service-password</dependency>
    <dependency>appbase-service-web</dependency>
    <!-- NO client deps — implementations are in tech-specific sub-modules -->
</dependencies>
```

### 2d — `appbase-service-user-account-firebaseauth/pom.xml`
```xml
<artifactId>appbase-service-user-account-firebaseauth</artifactId>
<dependencies>
    <dependency>appbase-service-user</dependency>
    <dependency>appbase-client-firebase</dependency>
</dependencies>
```

### 2e — `appbase-service-user-session-aerospike/pom.xml`
```xml
<artifactId>appbase-service-user-session-aerospike</artifactId>
<dependencies>
    <dependency>appbase-service-user</dependency>
    <dependency>appbase-client-aerospike</dependency>
</dependencies>
```

### 2f — `appbase-service-user-account-jdbi/pom.xml`
```xml
<artifactId>appbase-service-user-account-jdbi</artifactId>
<dependencies>
    <dependency>appbase-service-user</dependency>
    <dependency>appbase-client-jdbi</dependency>
</dependencies>
```

### 2g — `appbase-service-user-account-vitess/pom.xml`
```xml
<artifactId>appbase-service-user-account-vitess</artifactId>
<dependencies>
    <dependency>appbase-service-user-account-jdbi</dependency>  <!-- or appbase-service-user directly -->
    <dependency>appbase-client-vitess</dependency>
</dependencies>
```
> ⚠️ **Needs verification (Task 5d):** Vitess is a JDBC driver. If there is no Vitess-specific service code (only connection wiring done at bootstrap level), this module may end up very thin. Assess during implementation.

### 2h — `appbase-service-supportmessage-jdbi/pom.xml`
```xml
<artifactId>appbase-service-supportmessage-jdbi</artifactId>
<dependencies>
    <dependency>appbase-service-supportmessage</dependency>
    <dependency>appbase-client-jdbi</dependency>
</dependencies>
```

### 2i — Update `appbase-service/pom.xml` (parent)

Add modules in build order:
```
password → web → user → user-account-firebaseauth → user-session-aerospike
→ user-account-jdbi → user-account-vitess → supportmessage → supportmessage-jdbi
```

Add `<dependencyManagement>` entries for all 8 new artifacts.
Remove comment "appbase-service-password and appbase-service-user merged into appbase-service-core".

Each new pom needs: parent ref, name, url, scm, distributionManagement, bundle plugin (Export-Package for its own packages only).

---

## Task 3 — Move service-password files

Move 5 source files:
```
appbase-service-core/src/main/java/.../service/password/ →
appbase-service-password/src/main/java/.../service/password/
```

Move tests:
```
appbase-service-core/src/test/java/.../service/password/ →
appbase-service-password/src/test/java/.../service/password/
```

**Update service-core pom:** remove `bcprov-jdk18on`.

**Verify:** `mvn compile -pl appbase-service/appbase-service-password -am -q`

---

## Task 4 — Move service-web files + package renames

This is the most complex task. 13 packages renamed. Do the pre-move grep sweep first.

```bash
# Find all import sites for packages being renamed (run before touching any files)
for pkg in "service\.metric\.web\." "service\.trace\.web\." "service\.exceptionhandler\.web\." \
           "service\.authorization\.web\." "service\.health\.web\." "service\.appbase\.web\."; do
  echo "=== $pkg ==="; grep -r "$pkg" appbase-java --include="*.java" -l
done
```

### 4a — Move unchanged packages (service-core → service-web, no rename)
- `service.web.cors`
- `service.web.endpoint.jaxrs`
- `service.web.endpoint.servlet`
- `service.web.filter`
- `service.web.filter.jaxrs`
- `service.web.filter.servlet` — ALL files **except** `AppbaseHttpServletRequestWrapper.java`
- `service.web.filter.servlet.sync`

### 4b — Move renamed packages

| Source (service-core) | Destination (service-web) |
|---|---|
| `service.metric.web.filter.jaxrs` | `service.web.metric.filter.jaxrs` |
| `service.metric.web.filter.servlet` | `service.web.metric.filter.servlet` |
| `service.trace.web.filter.jaxrs` | `service.web.trace.filter.jaxrs` |
| `service.trace.web.filter.servlet` | `service.web.trace.filter.servlet` |
| `service.exceptionhandler.web.filter.servlet` | `service.web.exceptionhandler.filter.servlet` |
| `service.exceptionhandler.web.jaxrs` | `service.web.exceptionhandler.jaxrs` |
| `service.authorization.web.filter` | `service.web.authorization.filter` |
| `service.authorization.web.filter.jaxrs` | `service.web.authorization.filter.jaxrs` |
| `service.authorization.web.filter.servlet` | `service.web.authorization.filter.servlet` |
| `service.health.web.endpoint.jaxrs` | `service.web.health.endpoint.jaxrs` |
| `service.health.web.endpoint.servlet` | `service.web.health.endpoint.servlet` |
| `service.appbase.web.endpoint.jaxrs` | `service.web.endpoint.jaxrs.appbase` |
| `service.appbase.web.endpoint.servlet` | `service.web.endpoint.servlet.appbase` |

For each renamed package: move files, update `package` declaration, update all import sites found in the pre-move grep.

**Verify:** `mvn compile -pl appbase-service/appbase-service-web -am -q`

---

## Task 5 — Move service-user files (interfaces + data + web only)

Move to `appbase-service-user/src/main/java/` — interfaces and data only, NOT tech-specific impls:

```
service.user.account               (UserAccountService interface, DefaultUserAccountService)
service.user.account.data
service.user.account.store         (UserAccountStore interface + UserAccountStoreException ONLY)
service.user.account.web.*
service.user.session.data
service.user.session.store         (UserSessionStore interface + UserSessionStoreException ONLY)
service.user.session.web.*
```

Move `AppbaseHttpServletRequestWrapper.java`:
```
service-core: service.web.filter.servlet.AppbaseHttpServletRequestWrapper
→
service-user: service.web.filter.servlet.AppbaseHttpServletRequestWrapper
```
Same package, different module. Creates split package across service-web and service-user jars — acceptable on flat classpath; note for OSGi review.

Move tests that cover interfaces/service (not impl):
```
appbase-service-core/src/test/.../service/user/account/
appbase-service-core/src/test/.../service/user/session/
→ appbase-service-user/src/test/.../
```
Tests for JDBI store / Aerospike store move to their respective modules (Tasks 5b/5c).

**Update service-core pom:** remove `appbase-client-aerospike`, `appbase-client-jdbi`, `appbase-client-vitess`.
> Verify first: `grep -r "aerospike\|Aerospike" appbase-service-core/src/main/java/com/viveworks/appbase/service/health/` — if `AerospikeHealthChecker` exists there, keep `appbase-client-aerospike` in service-core.

**Verify:** `mvn compile -pl appbase-service/appbase-service-user -am -q`

---

## Task 5b — Move Aerospike session store → service-user-session-aerospike

Move from `service.user.session.store` (currently in service-core) to `service.user.session.aerospike` (in service-user-session-aerospike):

Files:
- `DefaultUserSessionStore.java` → `service.user.session.aerospike.DefaultUserSessionStore` (or `AerospikeUserSessionStore`)
- `DefaultUserSessionStoreConfiguration.java`
- `DefaultUserSessionStoreHealthChecker.java`

Move tests: `DefaultUserSessionStoreTest`, `DefaultUserSessionStoreHealthCheckerTest` → service-user-session-aerospike.

**Verify:** `mvn compile -pl appbase-service/appbase-service-user-session-aerospike -am -q`

---

## Task 5c — Move JDBI account store → service-user-account-jdbi

Move from `service.user.account.store` (currently in service-core) to `service.user.account.jdbi` (in service-user-account-jdbi):

Files:
- `JdbiUserAccountStore.java`
- `JdbiUserAccountStoreConfiguration.java`
- `JdbiUserAccountStoreHealthChecker.java`

Move tests: `JdbiUserAccountStoreTest`, `JdbiUserAccountStoreHealthCheckerTest` → service-user-account-jdbi.

**Verify:** `mvn compile -pl appbase-service/appbase-service-user-account-jdbi -am -q`

---

## Task 5d — Assess + populate service-user-account-vitess

Vitess is a JDBC-compatible driver — there may be little or no Vitess-specific service code. Assess:

```bash
grep -r "[Vv]itess" appbase-java/appbase-service/appbase-service-core/src/main/java --include="*.java" -l
```

**If Vitess-specific service code exists** (e.g., Vitess connection factory, Vitess-specific DAO, Vitess health checker): move to `service.user.account.vitess` package in this module.

**If no Vitess-specific service code** (only a JDBC connection wired at bootstrap): this module holds bootstrap/Guice wiring that binds Vitess datasource to `JdbiUserAccountStore` — a thin assembly module. Document in pom description.

**Verify:** `mvn compile -pl appbase-service/appbase-service-user-account-vitess -am -q`

---

## Task 6 — Move service-user-account-firebaseauth files

Move `service.user.account.firebaseauth` (from service-user, moved there in Task 5) → `appbase-service-user-account-firebaseauth`:

```
service-user: service.user.account.firebaseauth
→
service-user-account-firebaseauth: service.user.account.firebaseauth
```

Package unchanged. No import updates needed (callers reference the `UserAccountService` interface, not the Firebase impl).

**Verify:** `mvn compile -pl appbase-service/appbase-service-user-account-firebaseauth -am -q`

---

## Task 7 — Split appbase-service-supportmessage

Current `appbase-service-supportmessage` bundles interface + JDBI impl. Apply the same pattern.

**Keep in `appbase-service-supportmessage`:**
- `SupportMessageStore.java` (interface)
- `SupportMessage.java`, `SupportMessageService.java`, `SupportMessageListener.java`
- `DefaultSupportMessageService.java`, `DefaultSupportMessageServiceConfiguration.java`
- `data.*`, `endpoint/jaxrs/`, `endpoint/servlet/`

**Move to `appbase-service-supportmessage-jdbi`** (new package `service.supportmessage.jdbi`):
- `JdbiSupportMessageStore.java`
- `JdbiSupportMessageStoreConfiguration.java`
- `JdbiSupportMessageStoreHealthChecker.java`

Move tests: `JdbiSupportMessageStoreTest`, `JdbiSupportMessageStoreHealthCheckerTest` → service-supportmessage-jdbi.

**Update `appbase-service-supportmessage/pom.xml`:** remove `appbase-client-jdbi` dep.

**Verify:**
```bash
mvn compile -pl appbase-service/appbase-service-supportmessage,appbase-service/appbase-service-supportmessage-jdbi -am -q
```

---

## Task 8 — Update appbase-bootstrap pom

Add new deps:
```xml
<dependency>appbase-service-password</dependency>
<dependency>appbase-service-web</dependency>
<dependency>appbase-service-user</dependency>
<dependency>appbase-service-user-account-firebaseauth</dependency>
<dependency>appbase-service-user-session-aerospike</dependency>
<dependency>appbase-service-user-account-jdbi</dependency>
<dependency>appbase-service-user-account-vitess</dependency>
<dependency>appbase-service-supportmessage-jdbi</dependency>
```

Remove stale comment about merged modules. Check `AppbaseCoreServicesModule.java` for any imports from old package locations that need updating.

---

## Gate 1 — Full Verification

```bash
cd appbase-java
mvn clean install -pl appbase-service,appbase-bootstrap -am --no-transfer-progress 2>&1 | tail -30
```

Checklist:
- [ ] All 10 service modules in reactor output
- [ ] BUILD SUCCESS
- [ ] Total test count ≥ baseline from Gate 0
- [ ] No compile errors (especially: renamed web packages, moved store packages)
- [ ] `appbase-bootstrap` compiles clean
- [ ] No split-package OSGi warnings (if found, assess impact)

---

## Task 9 — GitHub issue + PR

1. Create GitHub issue: "P-013: Split appbase-service-core into focused sub-modules"
2. Branch from develop: `<issue#>/Java/Service/Refactor/Modularize-service-modules`
3. One commit per task for clean history
4. PR vs develop; include Gate 1 reactor output in description

---

## Dep matrix (post-split)

```
appbase-service-core                ← appbase-core, appbase-client-{core,firebase?,aws?,zk?,aerospike?}, micrometer-statsd, jackson-cbor
                                      (verify which client deps stay after health.checkers audit)
appbase-service-password            ← appbase-service-core, bcprov-jdk18on
appbase-service-web                 ← appbase-service-core
appbase-service-user                ← appbase-service-core, -password, -web  (NO client deps)
appbase-service-user-account-firebaseauth   ← appbase-service-user, appbase-client-firebase
appbase-service-user-session-aerospike ← appbase-service-user, appbase-client-aerospike
appbase-service-user-account-jdbi   ← appbase-service-user, appbase-client-jdbi
appbase-service-user-account-vitess ← appbase-service-user-account-jdbi, appbase-client-vitess
appbase-service-supportmessage      ← appbase-service-core  (NO client deps)
appbase-service-supportmessage-jdbi ← appbase-service-supportmessage, appbase-client-jdbi
appbase-bootstrap                   ← all of the above
```

---

## Risk register

| Risk | Severity | Mitigation |
|---|---|---|
| Package renames (Task 4) — missed import sites | High | Grep all 13 old packages before touching files |
| `appbase-service-user-account-vitess` may be empty | Medium | Assess at Task 5d; if thin, document as assembly-only module |
| Split package `service.web.filter.servlet` | Medium | Acceptable on flat classpath; note for OSGi review |
| service-core client dep audit (firebase/aws/zk/aerospike) | Medium | Grep health.checkers before removing deps from core |
| OSGi Export-Package in each new module | Low | Bundle plugin must export only own packages |
| Bootstrap Guice module old import paths | Medium | Grep `AppbaseCoreServicesModule` after Task 8 |
