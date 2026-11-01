# P-013 — appbase-service Sub-Module Split

**Prereq:** P-012 merged (internal work item 204).  
**Goal:** Split `appbase-service-core` monolith (228 files) into 5 focused modules.  
**Constraint:** Minimal source changes only — 4 targeted lines across 3 files.

---

## Proposed Module Structure

```
appbase-service (parent pom)
├── appbase-service-core
├── appbase-service-password
├── appbase-service-web
├── appbase-service-user
├── appbase-service-user-firebaseauth
└── appbase-service-supportmessage
```

### Dependency graph (no cycles)

```
appbase-service-core
    ▲
    ├── appbase-service-password
    ├── appbase-service-web
    │       ▲
    │       └── appbase-service-user ──► appbase-service-password
    │                   ▲
    │                   ├── appbase-service-user-firebaseauth
    │                   └── appbase-service-supportmessage
    └── (appbase-client-* via root BOM)
```

---

## Module 1 — `appbase-service-core`

Pure infrastructure. No web, no user, no password.

| Package | Description |
|---|---|
| `com.viveworks.appbase.service` | Abstract service base classes, constants, util, exceptions |
| `com.viveworks.appbase.service.authorization` | AuthorizationService interface, AuthorizationContext, exceptions (no web filters) |
| `com.viveworks.appbase.service.cluster` | Cluster management |
| `com.viveworks.appbase.service.health` | HealthCheckService, HealthCheckServiceGuiceModule |
| `com.viveworks.appbase.service.health.checkers` | Generic infra checkers: S3, ZooKeeper, Firebase, Aerospike |
| `com.viveworks.appbase.service.id` | ID generation |
| `com.viveworks.appbase.service.appbase.data` | AppbaseService data types (no web) |

**Source changes required (2 lines in AppbaseServiceConstants.java):**
```java
// Line 81 — removes compile-time dep on metric/web package:
// BEFORE: MetricJaxrsRequestFilter.class.getSimpleName() + "-timerContext"
// AFTER:  "MetricJaxrsRequestFilter-timerContext"

// Line 106 — removes compile-time dep on user/session package:
// BEFORE: UserSession.class.getName()
// AFTER:  "com.viveworks.appbase.service.user.session.data.UserSession"
```

---

## Module 2 — `appbase-service-password`

| Package | Description |
|---|---|
| `com.viveworks.appbase.service.password` | PasswordService, DefaultPasswordService, PasswordHash, PasswordHashes, HashAlgorithm |

**Depends on:** `appbase-service-core` (DefaultPasswordService extends AbstractStatelessAppbaseService)  
**Source changes:** None.

---

## Module 3 — `appbase-service-web`

Web infrastructure. Cross-cutting filters, common web abstractions, health/appbase endpoints.  
Packages **renamed** to consolidate under `service.web.*`.

| Current package (in service-core) | New package (in service-web) |
|---|---|
| `service.web.cors` | `service.web.cors` _(unchanged)_ |
| `service.web.endpoint.jaxrs` | `service.web.endpoint.jaxrs` _(unchanged)_ |
| `service.web.endpoint.servlet` | `service.web.endpoint.servlet` _(unchanged)_ |
| `service.web.filter` | `service.web.filter` _(unchanged)_ |
| `service.web.filter.jaxrs` | `service.web.filter.jaxrs` _(unchanged)_ |
| `service.web.filter.servlet` | `service.web.filter.servlet` _(unchanged)_ |
| `service.web.filter.servlet.sync` | `service.web.filter.servlet.sync` _(unchanged)_ |
| `service.metric.web.filter.jaxrs` | **`service.web.metric.filter.jaxrs`** |
| `service.metric.web.filter.servlet` | **`service.web.metric.filter.servlet`** |
| `service.trace.web.filter.jaxrs` | **`service.web.trace.filter.jaxrs`** |
| `service.trace.web.filter.servlet` | **`service.web.trace.filter.servlet`** |
| `service.exceptionhandler.web.filter.servlet` | **`service.web.exceptionhandler.filter.servlet`** |
| `service.exceptionhandler.web.jaxrs` | **`service.web.exceptionhandler.jaxrs`** |
| `service.authorization.web.filter` | **`service.web.authorization.filter`** |
| `service.authorization.web.filter.jaxrs` | **`service.web.authorization.filter.jaxrs`** |
| `service.authorization.web.filter.servlet` | **`service.web.authorization.filter.servlet`** |
| `service.health.web.endpoint.jaxrs` | **`service.web.health.endpoint.jaxrs`** |
| `service.health.web.endpoint.servlet` | **`service.web.health.endpoint.servlet`** |
| `service.appbase.web.endpoint.jaxrs` | **`service.web.endpoint.jaxrs.appbase`** |
| `service.appbase.web.endpoint.servlet` | **`service.web.endpoint.servlet.appbase`** |

**Depends on:** `appbase-service-core`  
**Source changes (2 lines):**
- `AbstractAppbaseJaxrsEntity` — remove import of `AuthenticationServiceException`, `UserAccountServiceException` (those move to service-user; base class handles generic `AppbaseException`)
- `AbstractAppbaseSyncServlet` — same removal  
- `AppbaseHttpServletRequestWrapper` — moves entirely to `appbase-service-user` (it references `UserSession` directly)

---

## Module 4 — `appbase-service-user`

User domain: account, session, their web endpoints, user-specific health checkers.  
Firebase Auth implementation extracted to its own module (see Module 5).  
Packages **unchanged** — files move from service-core without rename.

| Package | Description |
|---|---|
| `com.viveworks.appbase.service.user.account` | UserAccountService interface, DefaultUserAccountService |
| `com.viveworks.appbase.service.user.account.data` | UserAccount, GuestUser, request/response data, validators |
| `com.viveworks.appbase.service.user.account.store` | JdbiUserAccountStore, store interfaces |
| `com.viveworks.appbase.service.user.account.web.endpoint.jaxrs` | JAX-RS account endpoints |
| `com.viveworks.appbase.service.user.account.web.endpoint.servlet` | Servlet account endpoints |
| `com.viveworks.appbase.service.user.account.web.filter` | Account web filters (base) |
| `com.viveworks.appbase.service.user.account.web.filter.jaxrs` | JAX-RS account filters |
| `com.viveworks.appbase.service.user.account.web.filter.servlet` | Servlet account filters |
| `com.viveworks.appbase.service.user.session.data` | UserSession, UserSessionData, validators |
| `com.viveworks.appbase.service.user.session.store` | DefaultUserSessionStore, store interfaces |
| `com.viveworks.appbase.service.user.session.web.filter` | Session web filters (base) |
| `com.viveworks.appbase.service.user.session.web.filter.jaxrs` | JAX-RS session filters |
| `com.viveworks.appbase.service.user.session.web.filter.servlet` | Servlet session filters |
| `com.viveworks.appbase.service.user.health.checkers` | JdbiUserAccountStoreHealthChecker, DefaultUserSessionStoreHealthChecker |

**Depends on:** `appbase-service-core`, `appbase-service-password`, `appbase-service-web`

---

## Module 5 — `appbase-service-user-firebaseauth`

Firebase Auth implementation for user accounts. Separate from `appbase-service-user` so the user module has no hard dep on Firebase Admin SDK — a project that uses a different identity provider never pulls Firebase.

| Package | Description |
|---|---|
| `com.viveworks.appbase.service.user.account.firebaseauth` | Firebase Auth integration — implements interfaces from `appbase-service-user` |

**Depends on:** `appbase-service-user` (for UserAccountService interface + data types), `appbase-client-firebase`  
**Source changes:** None — package name unchanged, file just moves to a different jar.  
**Bootstrap wiring:** `AppbaseBootstrap` / Guice module in this artifact binds `FirebaseAuthUserAccountService` → `UserAccountService`. `appbase-bootstrap` adds this as a dep.

---

## Module 6 — `appbase-service-supportmessage`

Unchanged from current P-012 state.

| Package | Description |
|---|---|
| `com.viveworks.appbase.service.supportmessage` | SupportMessageStore, endpoints |
| `com.viveworks.appbase.service.supportmessage.data` | SupportMessage data types |
| `com.viveworks.appbase.service.supportmessage.store` | JdbiSupportMessageStore |
| `com.viveworks.appbase.service.supportmessage.web` | SupportMessage web endpoints |

**Depends on:** `appbase-service-core`, `appbase-client-jdbi`

---

## Source Changes Summary

| File | Change | Reason |
|---|---|---|
| `AppbaseServiceConstants.java` line 81 | `MetricJaxrsRequestFilter.class.getSimpleName()` → string literal | Breaks core→web compile dep |
| `AppbaseServiceConstants.java` line 106 | `UserSession.class.getName()` → string literal | Breaks core→user compile dep |
| `AbstractAppbaseJaxrsEntity.java` | Remove user exception imports | Breaks web→user cycle |
| `AbstractAppbaseSyncServlet.java` | Remove user exception imports | Breaks web→user cycle |
| `AppbaseHttpServletRequestWrapper.java` | Move to `appbase-service-user` | Owns `UserSession`; belongs in user module |

**Total: 4 lines changed across 3 files + 1 file moved.**

---

## `appbase-bootstrap` impact

Existing `appbase-bootstrap/pom.xml` adds 4 new deps and removes 0:
```xml
<dependency>appbase-service-password</dependency>          <!-- new -->
<dependency>appbase-service-web</dependency>               <!-- new -->
<dependency>appbase-service-user</dependency>              <!-- new -->
<dependency>appbase-service-user-firebaseauth</dependency> <!-- new -->
<!-- appbase-service-core and appbase-service-supportmessage already present -->
```
