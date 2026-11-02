# Known limitations — Release 1.0.0

This page lists what Release 1.0.0 does **not** promise, so the rest of the documentation can be read
without guessing. It follows the owner-approved Release 1 support and deferred-capability boundary.

The status words used here and in the [README](../README.md) are:

| Status | Meaning |
|---|---|
| **Supported** | Exercised through a meaningful integration or end-to-end test against a real backend or faithful emulator. |
| **Deferred** | Not part of the Release 1 promise; tracked for Release 2. It may still have code in the tree. |
| **Not supported** | Not advertised or published as working. |
| **NOT MEASURED** | Documented, but no gate has run it yet. It is not claimed to work. |

> ⚠️ **This page is written by hand, and it is interim.** Release 1 generates its known limitations
> from authoritative inputs (internal work item 1140 /
> internal work item 1387). When that output lands, it
> supersedes this page. Until then this page must agree with the baseline sections cited above.

## Deferred capabilities (Release 2)

These are deferred from the Release 1 support promise. Their artifacts may exist in the reactor, but
they are **not supported** in Release 1. They are tracked by the adapter-and-extension registry
internal work item 1925 under the Release 2 Plan of Record
internal work item 1957.

| Capability | Artifact in the tree | Release 1 status |
|---|---|---|
| ClamAV virus scanning | `appbase-client-clamav` | Deferred — not supported |
| SMTP mail | `appbase-client-smtp` | Deferred — not supported |
| Jasync SQL driver | `appbase-client-jasync` | Deferred — not supported; R2DBC is the supported reactive SQL path |
| R2DBC user-account store | `appbase-service-user-account-r2dbc` | Deferred — not supported (owner ruling, internal work item 1141); the R2DBC client is supported |
| Spark | `appbase-client-spark` | Deferred — not supported |
| General gRPC service bootstrap | — | Deferred. The Vitess gRPC *client* path **is** supported |
| Meta, X and AWS Cognito OAuth providers | — | Deferred — not supported |
| Cloud adapters beyond S3 (for example DynamoDB, SQS) | — | Deferred — not supported |
| Reactive, account and ORM extensions beyond the implemented R2DBC path | — | Deferred — not supported |

## Empty artifact shells

The three former POM-only service shells are not supported; they have been removed from the
reactor and repository. Release publication checks enforce their absence.
Password behavior remains in the core service; the existing JDBI store remains in the
support-message service. No separate account bundle for Vitess is claimed for Release 1.

## Build and packaging limits on this revision

- **The deferred modules are still built, even though the BOM leaves them out.**
  [`appbase-bom/pom.xml`](../appbase-bom/pom.xml) pins the Release 1 surface. It lists every leaf module
  except the deferred adapters (`appbase-client-clamav`, `appbase-client-smtp`, `appbase-client-jasync`,
  `appbase-client-spark`), and the deferred R2DBC user-account store
  (`appbase-service-user-account-r2dbc`). `scripts/test/bom_release1_test.sh` fails if
  the BOM drifts from that set. Those deferred modules are still reactor modules, so a full build
  produces them. Keeping them out of the published artifacts is the public-export gate
  internal work item 1009. Importing the BOM does not make a
  deferred module supported.
- **`appbase-bootstrap-core` builds against the deferred modules, but does not hand them on.**
  [`appbase-bootstrap-core`](../appbase-java/appbase-bootstrap/appbase-bootstrap-core/pom.xml)
  depends on the five retained excluded artifacts, and it carries
  factories for the deferred ClamAV, SMTP, Jasync and Spark clients. Since the owner's ruling on
  internal work item 2504 (2026-09-27), those dependencies are `<optional>true</optional>`. They are on
  bootstrap-core's own build classpath, so those factories still compile, but they no longer reach a
  consumer of `appbase-bootstrap-core` or `appbase-bootstrap-reactor-netty`. The factories are
  registered only by explicit caller code (`AppbaseClientServices.builder().with(…)`), so a consumer
  who never registers one never loads a deferred class. A consumer who does register one must add the
  deferred artifact to its own build, and that adapter is still not supported in Release 1.
  `EXTRA_no_transitive_deferred` in `scripts/test/bom_release1_test.sh` fails if a Release 1 module
  depends on an excluded artifact again without `<optional>true</optional>`.
- **Integration-test switches still exist for deferred adapters.** `build.sh` still defines
  the deferred adapters' switches `CLAMAV_`, `JASYNC_`, `SMTP_` and `SPARK_CLIENT_SERVICE_TESTS_ENABLED`. Turning them on proves
  nothing about Release 1 support, because these adapters are deferred.
- **Non-Jetty servers wire only the core services in the reference bootstrap.** `StackTestMain`
  records this for Tomcat and Undertow.

## Documentation and evidence limits

- **Artifact classification is checked; release evidence is not complete.** The generated
  [shipped-artifact inventory](api-inventory/README.md) (internal work item 1399) and the mechanically checked
  [module and service catalogue](Module-and-Service-Catalog.md) (internal work item 2418) have landed. The
  [README capability matrix](../README.md#release-1-support) is checked against their
  supported-public artifact classifications, not just reactor POMs and the lean baseline.
  That check does not establish each capability's live integration evidence or a complete,
  revision-pinned release-evidence index. Generating the supported-status view and known
  limitations from all authoritative evidence remains open in
  internal work item 1387; this page remains interim
  until that work lands.
- **There is no published API reference yet.** Supported public Java and Dart API documentation is
  owned by internal work item 1141. Internal and deferred
  types may be `public` in Java, and that does not make them supported.
- **There is no clean-machine User Guide yet.** It is planned as
  internal work item 1416. Until it passes, no path from a
  clean machine to a running service is claimed.
- **Examples that need a real backend, Docker or credentials are NOT MEASURED.** Each entry in
  [Examples](Examples.md) states whether it was run.
- **External or credentialed deployment is NOT MEASURED.** A local run never counts as evidence that
  an external deploy works.

## Not in Release 1 at all

- **Maven Central publication is not claimed yet.** The public export and distribution path is the
  Release 1 gate internal work item 1009. Until it passes,
  build and install from source.
- **Per-module READMEs, a Maven Site and broad documentation consolidation are Release 2**
  (internal work item 1417,
  internal work item 1418,
  internal work item 1419).
- **The brand is a placeholder** (internal work item 1415).
  The product name may still change.

## Where the Release 2 work lives

This page links to Release 2 work. It does not copy it. The authoritative structure is the
Release 2 Plan of Record internal work item 1957. The
public release notes are generated and added with the publication commit; no release-notes file is
present in the public tree before publication. Those notes preserve the durable next-release
decisions.
