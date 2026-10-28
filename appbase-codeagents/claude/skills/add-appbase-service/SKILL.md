---
name: add-appbase-service
description: Use when adding a new service to the Appbase platform across any of the three layers (Core, Client, or Service-layer). Triggers elicitation of service name, package, layer, and library, then scaffolds all boilerplate files and Maven sub-modules.
---

# add-appbase-service

Scaffolds all boilerplate for a new Appbase service. Handles elicitation, then delegates to the correct layer recipe.

## Elicitation

Ask the following questions **one at a time**. Do not proceed to the next question until the current one is answered.

### Questions for all layers

**Q1 — Service name (PascalCase noun)**
Examples: `Redis`, `Cache`, `RateLimiter`, `Scheduler`
> What is the PascalCase name for this service?

**Q2 — Java package suffix (lowercase)**
Examples: `redis`, `cache`, `ratelimiter`, `scheduler`
> What lowercase package suffix should be used?

**Q3 — Target layer**
- **Core** — cross-cutting infrastructure service in `appbase-core` (e.g. `CacheService`, `SchedulerService`). Adds MXBean, config validator, management servlet + JAX-RS endpoint in `appbase-service-web`.
- **Client** — external SDK wrapper. Creates a new `appbase-client-<xxx>` Maven sub-module (e.g. `appbase-client-redis`, `appbase-client-kafka`).
- **Service** — business service. Creates a new `appbase-service-<xxx>` Maven sub-module (e.g. `appbase-service-supportmessage`). Optionally creates tech-specific sub-modules (e.g. `appbase-service-<xxx>-jdbi`).
- **Service-core** — shared platform utility added directly to `appbase-service-core` (no new sub-module). Use for lightweight stateless services like `PasswordService`, `IdService`.
- **Client-core** — shared client infrastructure added directly to `appbase-client-core` (no new sub-module). Use for capabilities all client sub-modules depend on, like pool managers or shared resources.
> Which layer? (Core / Client / Service / Service-core / Client-core)

**Q4 — Library/SDK**
Search Maven Central for the top 2–3 candidates. Present each with: artifact ID, latest stable version, and one-line rationale. For Service layer: ask whether a library is needed at all (skip if the service composes existing client services only).
> After presenting candidates: Which library should be used? (Or type your own `groupId:artifactId:version`, or `none`)

### Additional questions for Client layer only

**Q5 — Raw SDK client type `T`**
The Java class returned by the SDK representing an open connection or client handle.
Examples: `CuratorFramework`, `AerospikeClient`, `StatefulRedisConnection<String,String>`
> What is the fully-qualified or simple class name of the raw SDK client type `T`?

**Q6 — Key value type `V`**
Default: `TextValue`. Use `ClusterIdValue` if the service is cluster-keyed (one client per cluster ID).
> Key value type? (TextValue / ClusterIdValue / other)

### Additional questions for Service layer only (not Service-core or Client-core)

**Q7 — Modularization**
Some services split into a generic interface module and one or more technology-specific implementation modules.
Example: `user` + `user-account-jdbi` + `user-session-aerospike`. The JDBI support-message store instead lives inside `appbase-service-supportmessage`; the former support-message JDBI and Vitess account skeletons were removed under internal work item 1135 L-13.
> Does this service need technology-specific sub-modules? If yes, list each technology (e.g. `jdbi`, `vitess`, `aerospike`). If no, answer `none`.

**Q8 — HTTP endpoints**
Some services expose HTTP endpoints (per-operation servlets + JAX-RS resource). Services that only compose other services internally (no public HTTP API) can omit `appbase-service-web` and skip endpoint scaffolding.
> Does this service need HTTP endpoints (servlets / JAX-RS)? (yes / no)

## Confirmation

After all questions are answered, print a summary:

```
Service name:       XxxService
Package suffix:     xxx
Layer:              Client | Core | Service | Service-core | Client-core
Library:            groupId:artifactId:version (or none)
[Client only]
  Client type T:    com.example.XxxClient
  Key value type:   TextValue
[Service only]
  Tech sub-modules: jdbi, vitess (or none)
  HTTP endpoints:   yes | no
```

Ask: **"Proceed with these parameters? (yes / correct \<field\>)"**

## Dispatch

Once confirmed, read and execute the appropriate sub-file from this directory:

| Layer | Recipe file | Status |
|---|---|---|
| **Client** | `client-service.md` | ✅ Tested |
| **Service** | `service-layer.md` | ✅ Tested (Q7=none only; tech sub-modules untested) |
| **Service-core** | `service-core-extension.md` | ✅ Tested |
| **Core** | `core-service.md` | ⚠️ EXPERIMENTAL — recipe untested end-to-end |
| **Client-core** | `client-core-extension.md` | ⚠️ EXPERIMENTAL — recipe untested end-to-end |
| **Container** | `add-appbase-servlet-container.md` | ✅ Tested (Jetty/Undertow/Tomcat) |

Substitute every occurrence of `Xxx` / `xxx` / `T` / `V` with the confirmed values before generating any file.
