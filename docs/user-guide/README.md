<img src="../branding/exports/icon-64.png" alt="" height="64">

# Appbase User Guide — Release 1

This guide takes you from a clean machine to Appbase running on it: **install → first service →
client integrations and service modules → bootstrap → configuration wizard → local infrastructure →
REST documentation → agent integration → developer MCP**. It describes only what is in this repository today, and it
says so wherever something you might expect is missing or broken.

> **Before you start — four facts that shape every step**
>
> - **Appbase is built from source.** The version is `1.0.0`. Appbase is not yet published to
>   Maven Central. Build and `mvn install` it locally. Publication is planned
>   for a later release.
> - **The groupId is not final.** Every dependency snippet uses the placeholder `GROUP_ID_PENDING_2546`.
>   The namespace decision has been made and **the rename is pending**. Read the real value
>   from `appbase-bom/pom.xml` in your checkout.
> - **No deploy flow ships yet.** The [Deploy section](#deploy--what-exists) below says exactly what
>   exists and what does not.
> - **Which modules are *supported* is not decided by this guide.** The shipped-artifact inventory
>   and the [Module and Service Catalogue](../Module-and-Service-Catalog.md) are the
>   authority. A module named here is in the build; consult the catalogue for its current support
>   classification.

## Release evidence

Release publication generates the Quality & Security Practices page from the release-evidence index.
That page is not present before the publication commit.

## Guide map

| Part | File | What it covers |
|---|---|---|
| 1 | [01-install-and-first-service.md](01-install-and-first-service.md) | Install, the BOM, the reactive showcase, the shape of an Appbase service |
| 2 | [02-client-integrations.md](02-client-integrations.md) | The core client framework and 18 client integrations |
| 3 | [03-service-modules.md](03-service-modules.md) | The 17 service modules: core, web, OAuth, user accounts, sessions, support messages |
| 4 | [04-bootstrap-flavors.md](04-bootstrap-flavors.md) | The 6 bootstrap modules: core, reactor-netty, servlet layer, Jetty, Tomcat, Undertow |
| 5 | [05-config-wizard.md](05-config-wizard.md) | The configuration wizard (console and web) |
| 6 | [06-docker-infra.md](06-docker-infra.md) | Local Docker infrastructure for client modules, and what is broken in the umbrella scripts |
| 7 | [07-swagger.md](07-swagger.md) | The REST surface of a core service, and the state of its OpenAPI documentation |
| 8 | [08-agent-integration.md](08-agent-integration.md) | The `appbase-codeagents/` directory: what each part is and how to use it |
| 9 | [09-developer-mcp.md](09-developer-mcp.md) | Connect popular coding agents to Appbase's source-derived API and convention lookup server |
| — | [known-divergences.md](known-divergences.md) | Places where docs, scripts or build files disagree with the tree |

Existing documents this guide links instead of repeating:
[OAuth primer](../OAuth-Primer.md) (concepts behind the OAuth modules),
[authorization and path matching](../configuration/authorization-and-path-matching.md) (path-match
rules for authorization, CORS, sessions and filters). Internal maintainer tooling is
**not covered here**.

**Contents of this page**

- [Acceptance environment and boundary](#acceptance-environment-and-boundary)
- [Deploy — what exists](#deploy--what-exists)
- [External (credentialed) deploy — NOT MEASURED](#external-credentialed-deploy--not-measured)
- [Verification status](#verification-status)
- [Where to go next](#where-to-go-next)

---

## Acceptance environment and boundary

### The acceptance environment — disposable and local

Every local step in parts 1 and 5 is meant to be followed, and verified, on a **clean, disposable
machine**. That can be a fresh VM, a throwaway cloud workstation, or a new container. Install the
canonical [build prerequisites](../Configuration-Reference.md#build-prerequisites): JDK 25+, Maven
3.6.3+, git, Python 3 as `python3`, ripgrep as `rg`, and bash.

It does **not** need Docker, a database, Elasticsearch, a cloud account, or any credential. Console mode
(part 5) needs no Flutter; only the optional web mode of part 5 does.
The local segment uses `localhost` only. This is true because the install deliberately compiles
tests but does not run them; `./build.sh --mode quick` is not the no-infrastructure install path.
Part 6 (Docker infrastructure) is **outside** the acceptance environment: it needs Docker and is a
maintainer and test aid.

### The boundary — where local proof stops

| Local segment (parts 1 and 5) | External segment |
|---|---|
| runs on the disposable machine only | touches infrastructure you do not throw away |
| binds `localhost`; no credentials | needs credentials, a registry, a cloud project, or a remote host |
| **must be executed** to count as followed | **NOT MEASURED** until a maintainer-authorized gate runs it |

A local run that reaches a running service proves the local segment and **nothing else**. It is never
evidence that a deploy works.

---

## Deploy — what exists

**Release 1 has no deploy flow, and this guide will not pretend otherwise.**

- The wizard's deploy feature (Docker, Kubernetes, GitHub Actions targets, export/import of
  configuration) is a **draft specification only**. Its internal design record is not part of the
  public User Guide, and none of its modules exist.
- Classes named `…ServletDeployment` in `appbase-java/appbase-bootstrap` are **in-process servlet
  wiring**, not a deployment mechanism.
- A maintainer's local development environment is **not** a way to deploy an Appbase service.

What you can do locally today is package your own service with Appbase on its classpath
([part 4](04-bootstrap-flavors.md)), then run it the way you run any Java application. That is your
application's deploy, not an Appbase feature, and this guide does not verify it.

---

## External (credentialed) deploy — NOT MEASURED

Anything that leaves the disposable machine belongs here: a container registry, a cloud project, a
remote host, a Kubernetes cluster, or any credential.

- **Status: NOT MEASURED.** There is no Appbase deploy flow to follow. When one exists, its
  end-to-end run is a **maintainer-authorized gate**. It stays NOT MEASURED until that gate runs and its
  evidence is linked here.
- The local segment passing does not change this status. A local run is not evidence for a deploy.
- Appbase's client modules talk to external services (Aerospike, Elasticsearch, Redis, S3, Firebase and
  others). Pointing them at real infrastructure is credentialed and is **not** covered by this guide.
  Their integration tests run against disposable Docker containers (part 6), which is a maintainer
  task, not a deploy.

---

## Verification status

| Acceptance criterion | Status |
|---|---|
| Swagger UI screenshots (full page + one per endpoint group) | NOT MEASURED: requires the authorized local stacktest capture described in [part 7](07-swagger.md); no images are claimed here |
| Configuration wizard console screenshots | NOT IN THIS PULL REQUEST: a recorded real console run is delivered in the stacked follow-up pull request; nothing here claims it ([part 5](05-config-wizard.md)) |
| Configuration wizard web-form screenshot | NOT CAPTURED: the web-form screenshot is NOT CAPTURED (the Flutter web UI was not built or run for a capture) ([part 5](05-config-wizard.md)) |
| Wizard jar build | NOT MEASURED in this guide revision; the clean-machine walkthrough remains deferred |
| External credentialed deploy | NOT MEASURED |
| Clean-machine walkthrough | DEFERRED to the release checklist |

**Walkthrough record: NOT MEASURED — host/CI walkthrough pending.** This guide was written against the
repository tree. What `scripts/test/user_guide_test.sh` checks: that every repository path named in backticks exists (and that the paths named as missing are still missing),
that every relative link and anchor resolves, that every `build.sh --mode` is real, that every code snippet matches the source lines it cites, the module counts, and a sample of ports and configuration keys against the cited source files.
The other commands, ports, class names and per-client claims were read from the source and were **not executed**. But nobody has yet
followed it on a clean machine. It is followability-unverified until that run is recorded here as
`Walkthrough record: PASSED <date> <evidence link>`.

### Verifier steps — the host/CI walkthrough

Run these on the disposable acceptance environment above. Record the output, and fix the guide wherever
reality differs.

1. **Fresh environment:** install every canonical
   [build prerequisite](../Configuration-Reference.md#build-prerequisites). Keep no previously
   installed Appbase artifacts in your local Maven repository, and provide no Docker or credentials.
2. `git clone …` and `cd Appbase`, then `java -version` and `mvn -version`, as in part 1.
3. `mvn clean install -DskipTests -P quick-profile` → expect `BUILD SUCCESS`. Record the wall time.
4. The reactive showcase command in part 1 → expect `ReactiveShowcaseStackTest` to pass, with
   `BUILD SUCCESS`.
5. `appbase-config-wizard/bin/start-console.sh console -o /tmp/wizard-out.yaml` → choose
   **Configurations → Create new YAML configuration**, complete one configuration class, then expect
   `/tmp/wizard-out.yaml` to exist and be non-empty.
6. *(Optional, needs Flutter)* The web-mode steps in part 5 → expect the UI to load at the printed
   URL, then `stop-web.sh` to stop it.
7. Record the result under this heading as `Walkthrough record: PASSED <date> <link to the run or
   issue comment>`, or list each divergence you fixed.

The clean-machine walkthrough is deferred to the
release checklist, to be run by the maintainers
before the 1.0.0 cut. Nothing in this guide establishes that walkthrough or an external deploy.

Parts 2, 3, 4, 6, 7, 8 and 9 are reference material and are **not** part of this walkthrough.

---

## Where to go next

The root [`README.md`](../../README.md) (being rewritten as the Release-1 storefront), the
[Module and Service Catalogue](../Module-and-Service-Catalog.md), the release notes, and the
configuration reference
[authorization and path matching](../configuration/authorization-and-path-matching.md).
