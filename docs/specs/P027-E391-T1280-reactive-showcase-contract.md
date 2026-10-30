# P-027 · E391 · N01 — Reactive showcase contract

**Issue:** internal work item 1280
**Date:** 2026-08-12
**Decision:** use the existing test surfaces now; P-008 is not a start gate (owner decision on internal work item 391).

## Selected substrate

| Concern | Existing surface selected | Evidence |
|---|---|---|
| Real HTTP edge | `appbase-bootstrap-reactor-netty`'s ephemeral-port `ReactorNettyAppbaseWebServerHttpTest` | It drives a real `HttpClient` through a real socket and has no external dependency. |
| Native account read | `R2dbcReactiveUserAccountStore` backed by its embedded H2 R2DBC fixture | Its test suite proves reads against a real in-memory reactive database, and its existing native endpoint consumes `ReactiveUserAccountStore`. |
| Native activity records | `NativeLogEndpoint` → `LogAccessor` → `ReactiveLuceneLogStore` | The existing route already uses reactive log reads directly, without an offload wrapper. |
| Non-blocking guard | `ReactorNettyAppbaseWebServerBlockHoundTest` | It installs BlockHound and has an armed event-loop blocking control. |
| Showcase home | `appbase-stacktest` | The existing integration/test artifact will gain the reactor-netty dependency and ordinary showcase arm in N04; no new test runner is introduced. |

`appbase-stacktest` currently contains servlet container dependencies only. It cannot itself run a
reactor-netty scenario today; treating it as if it could would create a green claim over no runtime. The
showcase therefore starts with the tested reactor-netty module and, in N04, adds the existing module as a
dependency of the standard stacktest artifact. That is integration, not a parallel/private harness.

## Endpoint contract

N02 adds one fixed-path `GET /reactive-showcase/activity` route. The fixed path preserves
`AppbaseRouteRegistry`'s exact-match design.

| Request | Outcome |
|---|---|
| Valid `email`, `appbaseRequesterId`, timestamps, and `pageSize` for a known account | `200` NDJSON stream: one allowlisted account event (email only) followed by at least two activity-record events. |
| Missing/blank `email` or `appbaseRequesterId` | `400 {"status":400,"message":"Invalid parameter"}`. |
| Unknown account | `404 {"status":404,"message":"Not Found"}`. |
| Missing authorization or reactive-account service | `503` with the existing safe unavailable message; no exception detail. |
| Authorization denial | `403 {"status":403,"message":"Forbidden"}`. |
| Activity source fails after NDJSON has started | The `200` NDJSON stream ends with exactly one safe `{"type":"error","status":503,"message":"Activity feed unavailable"}` event; no implementation detail is exposed. |

The request supplies `inclusiveStartTimestamp`, `exclusiveEndTimestamp`, and bounded `pageSize` for the
activity query. The account lookup uses `ReactiveUserAccountStore#readByEmailReactive`; activity records
use the record-emitting `LogAccessor` reactive read. The handler composes those publishers and writes NDJSON via
`writeStream` without `block*`, a synchronous accessor, manual `subscribe`, `Future#get`, or scheduler
offload.

The record publisher is cancellation-aware: cancellation of the response cancels the log publisher, and
the Lucene implementation observes that cancellation between documents. A failure before a response is
written remains an HTTP safe error; once an NDJSON response has started, HTTP status cannot be changed, so
the safe terminal error event above is the explicit wire contract.

Because activity records are global log records rather than account-scoped audit entries, N02 requires
both the existing account-read permission and the existing `GET /log` read permission. This preserves the
same log authorization boundary even when the endpoint is invoked outside the normal web-filter chain.
The direct check preserves the normal cookie → header → query session-ID precedence. Password hashes and
password-reset material are never serialized into the account event.

## Red-first test and command

N02 begins by adding a failing real-socket test beside
`ReactorNettyAppbaseWebServerHttpTest`: register the route with a controlled `ReactiveUserAccountStore`
and reactive log fixture, issue `GET /reactive-showcase/activity`, and assert the two-record streamed
envelope. Controlled publishers establish the HTTP contract; N03 then adds the end-to-end cancellation
and BlockHound proof.

The baseline and targeted command is:

```sh
cd appbase-java
mvn test -pl appbase-bootstrap/appbase-bootstrap-reactor-netty -am
```

N04 adds the corresponding ordinary `appbase-stacktest` arm and records its command after the module
dependency is present.

## Scope guard

This selection does not make servlet stacktest code reactive, add path templates, add OAuth, or create a
new example module. It joins existing verified reactive components into the one reader-facing flow internal work item 391
owns.
