# Part 7 — REST documentation (OpenAPI and Swagger)

[Guide index](README.md) · previous: [Docker infrastructure](06-docker-infra.md) · next: [agent integration](08-agent-integration.md)

Appbase's servlet-container builders keep the core OpenAPI document **off by default**. Jetty,
Tomcat, and Undertow applications may opt in explicitly after enabling the core servlet bootstrap:

```java
JettyServletContainer.builder()
    .from(appbaseServices)
    .withAppbaseCoreServlets()
    .withAppbaseCoreOpenApi()
    .build();
```

The flag alone is inert without `withAppbaseCoreServlets()`. When both are enabled,
`/openapi.json` describes the effective core servlet set while preserving caller-supplied Jersey
registrations. The route has no built-in authentication; protect it with application filters, a
gateway, or network policy when route shape is sensitive.

## Core REST surface

Core endpoints use `/appbase-core/restapi/v1`; application-service endpoints use
`/appbase-service/restapi/v1`. The source of truth for those prefixes and their path constants is
`appbase-java/appbase-service/appbase-service-core/src/main/java/com/viveworks/appbase/service/AppbaseServiceConstants.java`.
The web module supplies resources for configuration, exceptions, logs, JMX, metrics, mediation,
service-registry state, status, health checks, user accounts and support messages. Which resources are
reachable depends on the modules assembled by the application and its authorization configuration.

`JettyServletContainer.Builder.withSwaggerUI(...)` serves static UI files supplied by the application.
It does not generate an OpenAPI document or prove that any endpoint works. Tomcat and Undertow do not
offer the same convenience method.

A gap remains: a stock core service does not currently provide a ready-to-capture Swagger UI.
