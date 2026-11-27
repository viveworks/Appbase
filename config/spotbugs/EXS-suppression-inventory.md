<!--
  @author Navaneeth Mysore G
  @author Claude
-->
# EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS — full occurrence inventory & per-surface classification

This is the auditable per-occurrence basis for the detector-wide suppression of
`EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS` in
[`config/spotbugs/exclude.xml`](exclude.xml). The file's own discipline permits a blanket, bug-pattern-wide `<Match>`
**only** when every occurrence of that pattern across the whole reactor is
enumerated and individually classified as a non-defect. This directory retains
that enumeration as reviewable, tracked, reproducible artifacts rather than as a
prose summary that cannot be re-derived.

## Artifacts

| File | What it is |
|---|---|
| [`exs-inventory.tsv`](exs-inventory.tsv) | **Machine ground truth** — the raw per-occurrence rows (`class`, `method`, `signature`, `longmessage`) harvested from the SpotBugs report XML. Not hand-edited. |
| [`classify-exs-inventory.awk`](classify-exs-inventory.awk) | Deterministic classifier: a pure function of `(class, method)` that adds `layer`, `kind`, `surface_class` columns. |
| [`exs-inventory-classified.tsv`](exs-inventory-classified.tsv) | The retained **per-occurrence classification** — regenerable byte-for-byte from the two files above. |

Regenerate and verify the classification from the PR itself:

```sh
awk -f config/spotbugs/classify-exs-inventory.awk config/spotbugs/exs-inventory.tsv \
  | diff - config/spotbugs/exs-inventory-classified.tsv && echo "classification reproduced"
```

## How the raw inventory was produced (reproducible recount recipe)

Full-reactor SpotBugs run with the EXS entry **un-excluded**, at report
`threshold=Low, effort=Max` (EXS is a `sb-contrib` **STYLE, rank 15** detector;
its findings are filtered out above the Low report level, which is why an
earlier threshold-High recount under-counted). The occurrences are then read
from every module's `target/spotbugsXml.xml`:

```sh
# 1. run the analyzer with EXS reporting turned on (diagnostic, NOT the gate)
mvn -B -DskipTests -Dspotbugs.threshold=Low -Dspotbugs.effort=Max \
    -Dspotbugs.excludeFilterFile= com.github.spotbugs:spotbugs-maven-plugin:spotbugs
# (rc=1 by design: the un-excluded STYLE detector reports the very findings the
#  real gate — threshold High, this EXS <Match> active — is configured to skip.)

# 2. harvest every EXS BugInstance from every report module into the raw TSV
#    (class, method, signature, longmessage), one row per occurrence.
```

> The **real analyzer gate is unaffected**: it runs at `threshold=High` with the
> EXS `<Match>` active, so it reports zero EXS findings. Step 1 above is a
> diagnostic enumeration only.

Exact-head machine evidence for this fix wave lives in the quality ledger
and in the lane checkpoint logs (`agent2-1132-exs-occurrences-v2.tsv` →
`exs-inventory.tsv`; `agent2-1132-fixwave-heavy-v2.out` §C, `report files=26
EXS TOTAL=128`). `exs-inventory.tsv` is a byte-identical copy of that machine
TSV, independently re-verified here: 128 data rows, every row carries the
`converts checked exception to unchecked` `longmessage`, the per-class
distribution equals the independent §C tally, and the per-layer split equals the
[`exclude.xml`](exclude.xml) comment (below).

## Count integrity — 128 occurrences = 123 distinct surfaces

The **128** is a count of EXS occurrences across the **26** SpotBugs report
modules. Four `(class, method, signature)` surfaces are analysed in more than one
report module and therefore appear more than once, contributing **5** extra
occurrences over **123** distinct surfaces (`123 + 5 = 128`):

| Distinct surface | occurrences |
|---|---|
| `core.util.lucene.DefaultLuceneService.newDirectory(...)` | 3 |
| `core.util.lucene.DefaultLuceneService.<init>(...)` | 2 |
| `service.web.endpoint.jaxrs.appbase.JmxJaxrsResource.mxBeanEntry(...)` | 2 |
| `service.web.oauth.HmacSha256TokenKeyHasher.newMac()` | 2 |

This multiplicity does not affect a **detector-wide** suppression (every surface
is classified a non-defect regardless), but it is disclosed so the denominator is
auditable rather than mistaken for 128 distinct methods.

## Surface classes and the rationale that justifies suppression

Every occurrence is a deliberate, idiomatic checked→unchecked conversion at a
boundary whose invocation contract does not carry the checked type. None is a
defect; "fixing" any would break a contract or force `throws Throwable` (strictly
worse). The classifier assigns exactly one `surface_class` to each row; each maps
to one recorded rationale below. Representative sources were read and verified
(cited per class).

### `jaxrs-resource-method` — 65
JAX-RS resource methods (`…service.web.endpoint.jaxrs.*`). Each is a `@Path`/
`@GET`/… method invoked by the JAX-RS runtime; it catches a checked backend
exception (e.g. `MBeanException`) and rethrows a JAX-RS `WebApplicationException`
(e.g. `InternalServerErrorException`) that the runtime maps to an HTTP status.
Softening to the framework's response exception is the idiomatic contract — a
checked `throws` would not be dispatched by JAX-RS and the runtime provides no
checked-exception channel. *Verified: `CoreJaxrsResource.reloadService` catches
`MBeanException` → `throw new InternalServerErrorException(e)`.*

### `boundary-constructor` — 39
Service/boundary constructors (`<init>`) invoked through the platform's
service-construction path — for **36 of the 39** (24 distinct classes) the uniform
`super(...)` chain into `AbstractAppbase*Service`; for the remaining **3** the
equivalent factory-instantiation path, in classes that `implement` their service
interface directly with no `AbstractAppbase*Service` superclass
(`DefaultExceptionAccessor.<init>` ×1, `DefaultLuceneService.<init>` ×2). **None of
these paths declares or handles a checked exception**, so initialisation failures
are wrapped as the platform's **unchecked** `AppbaseRuntimeException` and the
construction surface need not declare or handle a per-service checked type. (The
`super(...)`-chain mechanism is not universal across the bucket; the invariant that
justifies the classification is that the construction/DI surface declares no
checked type, which holds for all 39.)

> **A Java constructor *can* legally declare a checked exception** (`throws` on a
> constructor is valid Java). The softening here is a deliberate **design
> contract**, *not* a language limitation — the earlier claim that these
> constructors "cannot propagate a checked exception" was wrong and is corrected
> here and in [`exclude.xml`](exclude.xml). The real constraint is that the
> construction/DI surface declares no checked exception, so propagating one would
> break every caller of that path.

*Verified — the `super(...)`-chain case: `DefaultRedisClientIntegrationService.<init>`
(`extends AbstractAppbaseClientIntegrationService`) catches `AppbaseClientException`
→ `throw new AppbaseRuntimeException("Error initializing …", ace)`; the constructor
could legally have declared `throws AppbaseClientException` but deliberately does
not. The direct-factory case: `DefaultExceptionAccessor` (`implements
ExceptionAccessor`, no `AbstractAppbase*Service` superclass) catches
`ExceptionStoreFactoryException` → `throw new AppbaseRuntimeException(…)` at
construction — same softening, no `super(...)` chain.*

### `client-integration-service-method` — 1 · `client-core-cloud-util-method` — 3 · `core-service-or-util-method` — 13 · `service-util-method` — 1
Non-constructor methods (the 18 rows in these four surface classes) that implement
a service/accessor/utility interface method whose signature declares no checked
exception, or run in a field-initialiser / functional-supplier context that
declares none. The actual members (from `exs-inventory-classified.tsv`) are:
lifecycle/config methods `ConfigSourceAdapter.reload`,
`DefaultAppbaseCoreBootstrap.registerIndividualConfiguration` /
`newDefaultConfigurationService`, `AbstractAppbaseManagedService.startOnCreate`,
`DefaultTracerService.initOnNewConfiguration`,
`DefaultMetricService.initJmxMeterRegistryIfEnabled`; accessor/util helpers
`AbstractAppbaseAccessor.execute`, `AppbaseClientUtil.waitAsConfigured` /
`waitUninterruptiblyAsConfigured`, `AppbaseServiceUtil.traceIdKey`,
`DefaultClamAVMediatorEventListener.handleCustomEvent`; and util factories
`DefaultLuceneService.newDirectory` / `newIndexWriter`, `AppbaseNetUtil.hostname`,
`SslClientContextFactory` / `SslServerContextFactory.getInstance`. (The
`unregister*` / `shutdown*` / record `read*`/`remove*` methods live in the JAX-RS
resources and are counted in the 65-row `jaxrs-resource-method` bucket, not here.)
Each catches the checked backend/IO/`GeneralSecurity` exception and rethrows the
platform's unchecked type; adding a checked `throws` would violate the interface
contract or the supplier context.

### `web-oauth-crypto-codec` — 6
Token codec / key-hasher / validator methods (`…service.web.oauth.*`:
`HmacSha256TokenKeyHasher.newMac`, `Sha256TokenKeyHasher.newSha256`,
`ValidatedTokenCodec.encode`/`decode`, `NimbusTokenValidator.jwkSource`). Each
implements a codec/hasher interface (`TokenKeyHasher`, …) and catches a checked
JCA/parse exception (`NoSuchAlgorithmException`, `InvalidKeyException`,
`ParseException`) that cannot occur for the fixed, validated standard
algorithm/inputs, rethrowing an unchecked `IllegalStateException`. *Verified:
`HmacSha256TokenKeyHasher.newMac()` catches `NoSuchAlgorithmException`/
`InvalidKeyException` for hard-coded `"HmacSHA256"` → `IllegalStateException`.*

## Per-layer totals (equal to the `exclude.xml` comment)

| Layer | occurrences |
|---|---|
| web-service (`…service.web` — jaxrs 65 + oauth 6) | 71 |
| client integration services (`…client.service.*`) | 28 |
| core (util 9 / service 6 / bootstrap 2) | 17 |
| client core / cloud / util | 11 |
| service util | 1 |
| **total** | **128** |

By method kind: **39** constructors + **89** framework/interface-invoked methods
= 128.
