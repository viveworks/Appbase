# Part 3 — Service modules

[Guide index](README.md) · previous: [client integrations](02-client-integrations.md) · next: [bootstrap flavors](04-bootstrap-flavors.md)

An Appbase **service** is application-level behaviour built on the core and client layers: authorization,
health, user accounts, sessions, OAuth token validation, support messages. The tree has **17 service
modules** under `appbase-java/appbase-service/`, including **5 OAuth providers** (Apple, GitHub, Google,
Microsoft, Okta). All five OAuth provider modules are in `appbase-bom/pom.xml`. Three former
source-empty shells have since been removed from the reactor.

Everything below was read from the tree. Nothing here was run against live infrastructure. The
concepts behind OAuth are in the [OAuth primer](../OAuth-Primer.md), which this part does not repeat.

## The 17 modules

| # | Module | Source files | What it holds |
|---|---|---|---|
| 1 | `appbase-service-core` | 56 | authorization, ids, password hashing, health-check engine, cluster service, filter chain |
| 2 | `appbase-service-web` | 55 | servlet and JAX-RS endpoints for the core services, filters (CORS and others), health endpoint |
| 3 | `appbase-service-web-oauth` | 16 | bearer-token filter, JWT validation, token cache interface and in-memory cache |
| 4 | `appbase-service-web-oauth-reactive` | 2 | the same filter for the reactive stack |
| 5 | `appbase-service-web-oauth-google` | 4 | Google defaults and profile/group parsing |
| 6 | `appbase-service-web-oauth-okta` | 4 | Okta defaults and profile/group parsing |
| 7 | `appbase-service-web-oauth-github` | 9 | GitHub token validation through the GitHub user API |
| 8 | `appbase-service-web-oauth-microsoft` | 4 | Microsoft Entra defaults and profile/group parsing |
| 9 | `appbase-service-web-oauth-apple` | 4 | Apple defaults, profile parsing, client-secret generator |
| 10 | `appbase-service-web-oauth-tokencache-aerospike` | 3 | shared token cache on Aerospike |
| 11 | `appbase-service-web-oauth-tokencache-redis` | 3 | shared token cache on Redis |
| 12 | `appbase-service-user` | 124 | user-account and user-session services, session filter, user-account REST resource |
| 13 | `appbase-service-user-account-firebaseauth` | 7 | Firebase-backed identity checks |
| 14 | `appbase-service-user-session-aerospike` | 4 | session store on Aerospike |
| 15 | `appbase-service-user-account-jdbi` | 3 | blocking user-account store (JDBI) |
| 16 | `appbase-service-user-account-r2dbc` | 3 | reactive user-account store (R2DBC) |
| 17 | `appbase-service-supportmessage` | 30 | support-message service, REST resource, JDBI store |

Counts are `*.java` files under `src/main`. The Maven artifactId is the module name. The groupId is a
placeholder, **pending rename** ([part 1](01-install-and-first-service.md)). The Release-1 service
modules are managed by `appbase-bom/pom.xml`; the deferred R2DBC user-account module is not.

## Registering services

Application services are registered through factories in `appbase-bootstrap-core` under
`appbase-java/appbase-bootstrap/appbase-bootstrap-core/src/main/java/com/viveworks/appbase/bootstrap/service/factory/`,
passed to `AppbaseServices.builder().from(core).withClientServices(clients).with(…)`. The tree has these
twelve factories:

| Factory | Service it creates | Needs |
|---|---|---|
| `DefaultAuthorizationServiceFactory` | `AuthorizationService` | nothing (added for you, see below) |
| `DefaultHealthCheckServiceFactory` | `HealthCheckService` | nothing |
| `DefaultIdServiceFactory` | `IdService` | nothing |
| `DefaultPasswordServiceFactory` | `PasswordService` | nothing |
| `StaticClusterServiceFactory` | `ClusterService` | nothing |
| `AerospikeUserSessionStoreFactory` | `UserSessionStore` | an Aerospike client |
| `JdbiUserAccountStoreFactory` | `UserAccountStore` | a `JdbiClientService` |
| `R2dbcUserAccountStoreFactory` | `ReactiveUserAccountStore` | an R2DBC client |
| `DefaultFirebaseAuthServiceFactory` | `FirebaseAuthService` | a Firebase auth client |
| `DefaultUserAccountServiceFactory` | `UserAccountService` | reads `IdService`, `ClusterService`, `AuthorizationService`, `UserSessionStore`, `UserAccountStore`, `FirebaseAuthService` and `PasswordService` from the partly built container, so register those first (its Javadoc says which it requires) |
| `DefaultSupportMessageServiceFactory` | `SupportMessageService` | reads `SupportMessageStore` and `IdService` from the partly built container |
| `JdbiSupportMessageStoreFactory` | `SupportMessageStore` | a `JdbiClientService` |

Factories run in their declared initialization order, and each receives the partly built container. The
working chain from the repository's stack test (it needs the Docker infrastructure of
[part 6](06-docker-infra.md), and `StackTestContextAuthorizer` is a test class) is:

<!-- snippet-source: appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/StackTestMain.java:241-269 -->
```java
appbaseServices = AppbaseServices.builder()
        .from(coreServices)
        .withClientServices(clientServices)
        .withContextAuthorizer(new StackTestContextAuthorizer())
        .with(new StaticClusterServiceFactory(
                StaticClusterServiceConfiguration.builder()
                        .homeDataCenter("dev")
                        .build()))
        .with(new DefaultHealthCheckServiceFactory(
                DefaultHealthCheckServiceConfiguration.builder().build()))
        .with(new DefaultIdServiceFactory(DefaultIdServiceConfiguration.builder().build()))
        .with(new DefaultPasswordServiceFactory(new DefaultPasswordServiceConfiguration()))
        .with(new AerospikeUserSessionStoreFactory(
                DefaultUserSessionStoreConfiguration.builder().build()))
        .with(new JdbiUserAccountStoreFactory(
                JdbiUserAccountStoreConfiguration.builder().build(),
                clientServices.get(JdbiClientService.class)))
        .with(new DefaultFirebaseAuthServiceFactory(
                DefaultFirebaseAuthServiceConfiguration.builder()
                        .userTokenVerificationEnabled(true)
                        .build()))
        .with(new DefaultUserAccountServiceFactory(new DefaultUserAccountServiceConfiguration()))
        .with(new JdbiSupportMessageStoreFactory(
                JdbiSupportMessageStoreConfiguration.builder().build(),
                clientServices.get(JdbiClientService.class)))
        .with(new DefaultSupportMessageServiceFactory(
                new DefaultSupportMessageServiceConfiguration()))
        .withAllJmxOperationsEnabled()
        .build();
```

Source: `appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/StackTestMain.java`, lines 241-269.

Account stack tests use Firebase Auth emulator-issued ID tokens with token verification enabled.
The emulator's unsigned tokens are test-only and do not establish production authentication security.
Set `FIREBASE_AUTH_EMULATOR_HOST` to the emulator's bare `host:port` (for example,
`localhost:9099`) when running them.

## Authorization: secure by default

`AppbaseServices.builder()` is **secure by default**. Unless you supply your own authorizer with
`withContextAuthorizer(…)`, `build()` wires an `AuthorizationService` that **denies every request except
the public paths**, which default to `{"/health"}`. The web faces answer HTTP 403 on denial.

- `secureByDefault(false)` selects a permit-all authorizer. Use it only for local experiments.
- `publicPaths(Set.of(…))` changes the allowed paths.
- A bare container therefore holds exactly one service, the `AuthorizationService`. This is asserted in
  `appbase-java/appbase-bootstrap/appbase-bootstrap-servlet-jetty/src/test/java/com/viveworks/appbase/bootstrap/jetty/AppbaseBootstrapIntegrationTest.java`.

How request paths are matched for authorization, CORS, sessions and filters is documented in
[authorization and path matching](../configuration/authorization-and-path-matching.md).

## Core, web and health

- **`appbase-service-core`** holds `AuthorizationService` and its authorizers
  (`SecureByDefaultContextAuthorizer`, `PermitAllContextAuthorizer`), `IdService`, `PasswordService`
  (hash algorithms and `PasswordHash`), `ClusterService`, the request filter chain, and the
  `HealthCheckService` with built-in checkers for S3, ZooKeeper and Firebase. The REST path constants
  are in `appbase-java/appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/AppbaseServiceConstants.java`.
- **`appbase-service-web`** holds the servlet and JAX-RS endpoints that expose the core services, plus
  filters. Part 7 lists the REST paths.
- There is no separate password module to depend on; use `PasswordService` from `appbase-service-core`.

## OAuth bearer-token validation

`appbase-service-web-oauth` validates `Authorization: Bearer …` tokens. Its main types are
`OAuthFilter` (a servlet filter), `OAuthConfiguration`, `NimbusTokenValidator`, `TokenCache` and
`CaffeineTokenCache`. `appbase-service-web-oauth-reactive` provides `ReactiveOAuthFilter`.

**The bootstrap does not register the filter for you.** No main-code class in `appbase-bootstrap-*` adds
`OAuthFilter`. You construct it and register it with your servlet container, as the repository's
cross-container test does. The configuration is a record:

| Field | Meaning |
|---|---|
| `jwksUri` | where the issuer publishes its signing keys |
| `issuer` | the expected `iss` |
| `clientId` | your client identifier |
| `audienceClaim` | the expected `aud` **value** (not a claim name) |
| `tokenExpiryMarginSeconds` | clock-skew margin on `exp` and `nbf` |
| `cacheTtlSeconds` | upper bound on how long a validated token is cached |
| `tokenKeyHashing` | how cache keys are derived: `SHA256` (default when null) or `HMAC_SHA256` |
| `tokenKeyHmacKeyRef` | secret reference for the HMAC key, needed only with `HMAC_SHA256` |

Minimal wiring, copied from the cross-container test:

<!-- snippet-source: appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/oauth/OAuthCrossContainerTest.java:134-137 -->
```java
    OAuthConfiguration configuration = new OAuthConfiguration(URI.create("https://issuer.example/jwks"),
            ISSUER, "client-1", "aud", 30L, 300L, TokenKeyHashing.SHA256, null);
    return new OAuthFilter(STUB_VALIDATOR, new CaffeineTokenCache(), new Sha256TokenKeyHasher(),
            configuration);
```

Source: `appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/oauth/OAuthCrossContainerTest.java`, lines 134-137.

and its registration on Jetty:

<!-- snippet-source: appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/oauth/OAuthCrossContainerTest.java:175-181 -->
```java
        Server server = new Server(0);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(downstream), "/*");
        context.addFilter(new FilterHolder(oauthFilter), "/*", EnumSet.of(DispatcherType.REQUEST));
        server.setHandler(context);
        server.start();
```

Source: `appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/oauth/OAuthCrossContainerTest.java`, lines 175-181.

> **Test double, not production.** In that test the first argument of `OAuthFilter` is `STUB_VALIDATOR`, a
> stub that accepts one fixed string. It exists to prove the three servlet containers behave the same.
> A real deployment uses the validator that checks signatures against the provider's keys
> (`NimbusTokenValidator`). That test is opt-in: run it with `-Dappbase-stacktest.skipTests=false`.
> Jetty, Tomcat and Undertow registrations are all in
> `appbase-stacktest/src/test/java/com/viveworks/appbase/stacktest/oauth/OAuthCrossContainerTest.java`.

### The five providers

Each provider module supplies a `…OAuthDefaults.configuration(…)` factory for the record above, and
read-only parsers for the provider's profile (and, for most, group) responses. The profile binders only
parse; the caller must check that the profile's subject matches the validated token's subject.

| Module | Defaults class and arguments | Issuer / key source |
|---|---|---|
| Google | `GoogleOAuthDefaults.configuration(clientId)` | `https://accounts.google.com`; keys at `https://www.googleapis.com/oauth2/v3/certs` |
| Apple | `AppleOAuthDefaults.configuration(clientId)` | `https://appleid.apple.com`; keys at `https://appleid.apple.com/auth/keys` |
| Okta | `OktaOAuthDefaults.configuration(issuer, audience, clientId)` | your HTTPS issuer; keys at `<issuer>/v1/keys` |
| Microsoft | `EntraOAuthDefaults.configuration(tenantId, audience, clientId)` | `https://login.microsoftonline.com/<tenant>/v2.0`; keys at `/discovery/v2.0/keys` |
| GitHub | `GitHubOAuthDefaults.configuration(clientId)` | not a JWT provider: `GitHubTokenValidator` checks the token through `https://api.github.com/user` (default cache TTL 60 seconds) |

The Microsoft module's classes are named `Entra…`. The Okta and Entra defaults validate their issuer or tenant input and
reject malformed values. Apple differs in one way: it has no static client secret.
`AppleClientSecretGenerator` signs a short-lived ES256 token with your Apple private key. Apple caps its
lifetime at six months and you must re-mint it before it expires; the class performs no rotation. You
supply the key and store it securely. Provider account setup (consoles, secrets, redirect URIs) is
external and **not covered here**.

### Shared token caches

`CaffeineTokenCache` is an in-process cache. For several instances sharing validated-token state, use one
of two modules, each of which binds a `TokenCache` through a Guice module that takes the matching client:

- `appbase-service-web-oauth-tokencache-aerospike`: `AerospikeTokenCacheGuiceModule`, over an Aerospike client.
- `appbase-service-web-oauth-tokencache-redis`: `RedisTokenCacheGuiceModule(RedisClientService)`, over a Redis client.

Cache keys are hashed (SHA-256 by default, or HMAC-SHA-256 with a secret reference), so raw tokens are
not used as keys.

## User accounts and sessions

`appbase-service-user` is the largest module. It defines `UserAccountService`, the stores' interfaces, the
session store interfaces and their reactive twins, `UserSessionFilterConfiguration`, and the
`UserAccountJaxrsResource`. The backends are separate modules, so you choose one by dependency:

| Concern | Module | Class | Registered with |
|---|---|---|---|
| Identity provider checks | `appbase-service-user-account-firebaseauth` | `DefaultFirebaseAuthService` (implements `FirebaseAuthService`, an `IdpTokenVerifier`) | `DefaultFirebaseAuthServiceFactory` |
| Blocking account store | `appbase-service-user-account-jdbi` | `JdbiUserAccountStore`, `JdbiUserAccountStoreConfiguration` | `JdbiUserAccountStoreFactory` |
| Reactive account store | `appbase-service-user-account-r2dbc` | `R2dbcReactiveUserAccountStore`, `R2dbcReactiveUserAccountStoreConfiguration` | `R2dbcUserAccountStoreFactory` |
| Session store | `appbase-service-user-session-aerospike` | `DefaultUserSessionStore`, `DefaultReactiveUserSessionStore`, `DefaultUserSessionStoreConfiguration` | `AerospikeUserSessionStoreFactory` |

**Vitess.** There is no separate Vitess user-account module or store class and no Vitess
factory. Vitess is reachable only as a client ([part 2](02-client-integrations.md)). The JDBI user-account
module ships test configuration for a Vitess-backed JDBC and JDBI client (`appbase-service-vitess-jdbc.yaml`
and `appbase-service-vitess-jdbi.yaml`), so a Vitess-backed account store is the JDBI store over a
Vitess client. That arrangement is what the stack test builds. This guide does not run it.

User-account and session HTTP endpoints go through `UserAccountJaxrsResource` and the session filter;
the REST paths are in [part 7](07-swagger.md). The reactive server exposes the user-session and
user-account reads natively (its README lists them).

## Password

`PasswordService` (`DefaultPasswordService`, `PasswordHash`, `HashAlgorithm`) is in
`appbase-service-core`. Register it with `DefaultPasswordServiceFactory`; there is no separate
password service module.

## Support messages

`appbase-service-supportmessage` holds `SupportMessageService`, `DefaultSupportMessageService`, a
`SupportMessageStore`, a JDBI store (`JdbiSupportMessageStoreConfiguration`) and
`SupportMessageJaxrsResource`. Register the store with `JdbiSupportMessageStoreFactory` (it takes a
`JdbiClientService`) and the service with `DefaultSupportMessageServiceFactory`. There is no
separate support-message JDBI module.

## Removed placeholders

The three empty password, support-message JDBI, and Vitess user-account shells are no longer in the
reactor or BOM. Use the implemented services described above instead of depending on old coordinates.
