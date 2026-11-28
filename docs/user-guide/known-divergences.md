# Known divergences between the docs and the tree

[Guide index](README.md)

Found while writing this guide. Each belongs to the document, script or module named, not to this guide.
This guide routes around every one of them, and **edits none of them**.

## Scripts, builds and docs that disagree with the tree

| Where | What it says | What the tree does |
|---|---|---|
| `build.sh` (`quick` mode) | fast unit-test feedback with the canonical build prerequisites | the full reactor has observed order-sensitive core failures and reaches Jdbi database, Elasticsearch client, and bootstrap/config test groups that fail without their expected environment. Use the part 1 `-DskipTests` install for the no-infrastructure path |
| `appbase-config-wizard/bin/start-console.sh`, `appbase-config-wizard/bin/start-web.sh` | build the jar automatically when it is missing | the fallback build uses `-Dmaven.test.skip=true`, which never builds `appbase-client-core`'s test-jar. `appbase-client-clamav` (pulled in through `appbase-bootstrap-core`) needs it, so the build fails from a clean checkout. Build first (part 1, or `-DskipTests`) |
| `appbase-config-wizard/README.md`, `appbase-config-wizard/bin/start-web.sh` | the script "builds the JAR automatically" and the web UI works | the automatic build skips the Flutter frontend, so the web page is blank. Build with `--wizard-frontend` first (part 5) |
| `appbase-config-wizard/frontend/README.md` | `mvn install -pl appbase-config-wizard` runs `flutter build web` | only with `-Dappbase-config-wizard.skipFrontend=false` (which `--wizard-frontend` sets) |
| `README.adoc` | modules `appbase-net`, `appbase-platform`, `appbase-flutter`, `appbase-react` | none of these exist. The root README is being rewritten |
| `appbase-java/README.adoc` | run `./build.sh quick` from `appbase-java/` | `build.sh` is at the repository root, and the mode flag is `--mode` |
| Root README | a maintainer-only container guide is described as "921 lines" | that private guide has 1012 lines; it is not part of the public User Guide |
| `docs/OAuth-Primer.md` section 7 | configuring a provider is deferred to a future usage guide and API reference | [part 3](03-service-modules.md) of this guide now carries the provider configuration step; the primer was not edited |
| `appbase-java/appbase-bootstrap/appbase-bootstrap-reactor-netty/README.md` | "When `appbase-bootstrap-servlet` exists, link it here" | `appbase-bootstrap-servlet` exists (the shared servlet layer) |
| `appbase-java/appbase-client/appbase-client-vitess/infra/docker/vitess/README.adoc` | the Docker container "seems unstable" | recorded as the README says; not reproduced here |
| `appbase-codeagents/claude/skills/add-appbase-service/*.md` | reference modules at `appbase-client/appbase-client-aerospike/` and similar | those paths do not exist at the repository root. The modules are under `appbase-java/` |

## Local infrastructure scripts

| Where | What it says | What the tree does |
|---|---|---|
| `appbase-java/appbase-client/appbase-client-core/infra/docker/common-all.sh` | `MINIO_DIR` is `appbase-java/appbase-client/appbase-client-aws/infra/docker/minio` | no such directory exists |

The stacktest, service, and Vitess orchestration paths were corrected. Part 6 gives the
per-client commands that now exist; the MinIO path above remains unresolved.

## REST documentation

| Where | What it says | What the tree does |
|---|---|---|
| `JettyServletContainer.Builder.withSwaggerUI(…)` and `appbase-stacktest` | the stack test serves Swagger UI from `/swagger-ui/dist` | `appbase-stacktest/src/main/resources/swagger-ui/dist` does not exist, and no core-service main code (`appbase-core`, `appbase-service-*`) registers an OpenAPI resource (the configuration wizard's own web server does), so a stock core service does not serve `/openapi.json` |

### Images in this guide

| Where | What it could be read as | What it is |
|---|---|---|
| Swagger UI screenshots | full-page and per-endpoint-group captures from a local stacktest run | **NOT MEASURED**; no images are committed or claimed by this guide revision |
| wizard screenshots | screenshots of the configuration wizard | none are committed here: the console screenshots are delivered in the stacked follow-up pull request, and no web-form screenshot exists |

## Counts quoted in the original task description that differ from the tree

| The brief says | The tree has |
|---|---|
| 19 client integrations, listed as 18 names plus core | 19 client directories: the 18 named integrations and `appbase-client-core` |
| `appbase-service` has 21 sub-modules | 17 service modules (the `<module>` entries of `appbase-java/appbase-service/pom.xml`; three empty skeletons were removed) |
| OAuth for 6 providers | 5 provider modules: Apple, GitHub, Google, Microsoft and Okta |
| `appbase-bootstrap` has 5 flavors | 6 bootstrap modules (core, the shared servlet layer, Jetty, Tomcat, Undertow and Reactor Netty), which give four web-server flavors |

The guide states the counts the tree has.

## Documents that do not exist

| Reference | State |
|---|---|
| [Module and Service Catalogue](../Module-and-Service-Catalog.md) | landed; it is the source of truth for module support status |
| shipped-artifact inventory | not cited as existing here |

## Bootstrap test named in the brief

| Reference | State |
|---|---|
| `StackTestServicesBootstrapTest` | the original brief names it as a source for the bootstrap snippets, but it only checks the `StackTestServices` constructor signature and null-argument rejection; it contains no code that bootstraps a service. Part 4 takes its snippets from `StackTestMain` and the other tests it cites; the guide mentions this class only in this row |
