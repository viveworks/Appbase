# P-027 R4 — Reactive store twins + reactive JPA · Design

**Epic:** internal work item 390 (P-027 R4), sub-issue of the
P-027 programme internal work item 277.
**Release decision:** R4 delivers reactive store twins through Aerospike and R2DBC; Hibernate-Reactive remains unshipped.
**Date:** 2026-08-07. **Author:** Claude (Agent 1), with the owner.

This design settles the two open decisions the release spec flagged and records the decomposition. It is
the input to the implementation plan (`writing-plans`).

## Problem

The reactive web face covers the data clients (the ten R2a driver twins) but not the **service-layer
stores built on top of them**. Until the **user-account** and **session** stores have reactive twins,
the reactive face cannot serve that surface natively — it offloads or does not expose it. R4 closes that
gap for those two stores and adds a genuinely non-blocking SQL persistence path.

## Decisions (settled with the owner, 2026-08-07)

### D1 — "Reactive JPA" = an R2DBC-backed twin (release-spec reading (a))

JPA's API (`EntityManager`, query execution, transactions) is blocking end to end; there is no async JPA
specification, so a *native* reactive JPA twin cannot be built. Of the three readings the spec offered,
we take **(a)**: build a natively non-blocking SQL store on **R2DBC**, which is already in the tree
(`appbase-client-r2dbc` — `ReactiveR2dbcAccessor`, `ReactiveSqlSession`).

- The `appbase-client-jpa` client **stays blocking by design.** This is a **permanent 1.0.0 limitation**,
  recorded as such — *not* a phase-2 deferral.
- **Zero new external dependencies.** `appbase-client-r2dbc` already declares `r2dbc-spi`, `r2dbc-pool`,
  and the MariaDB / PostgreSQL / H2 drivers, plus reactor. The new service module depends only on
  internal modules.

### D2 — Scope = stores + a thin proof consumer (reading (iii))

The spec's "Done when" lists only the stores, but the R1 quotation it cites is about ~30 servlets — so
delivering stores alone ships something *nothing consumes reactively*. We add a **thin proof consumer**:
1–2 native reactive read endpoints wired through the twins. Not the full ~30-servlet port (deferred).

### D3 — Real backend for the proof = H2 (`r2dbc-h2`)

The MariaDB image the scripts name is deleted from Docker Hub (release-spec Unknown #3, the same wall
that blocks J-018). H2 via `r2dbc-h2` (already a dependency) is a real database engine, embedded, with no
Docker Hub dependency — so the proof runs in this environment. MariaDB / PostgreSQL ITs may be *written*
but are **gated and registered in J-018** with the reason they cannot run offline.

## Architecture

Convention in the service-user family is **one module per backing store**
(`-account-jdbi`, `-account-vitess`, `-account-firebaseauth`, `-session-aerospike`).

| Module | Change | Holds |
|---|---|---|
| `appbase-service-user` | extend | Reactive store interface(s) — the `Mono`/`Flux` contract, uniform with the ten R2a twins |
| `appbase-service-user-session-aerospike` | extend | Reactive session store twin, **beside** the blocking one (R3 pattern), on `ReactiveAerospikeAccessor` — natively non-blocking |
| `appbase-service-user-account-r2dbc` | **new** | Reactive account store twin on `ReactiveR2dbcAccessor` — the D1 reactive SQL path |
| reactor-netty bootstrap | extend | 1–2 native reactive read endpoints (the D2 proof consumer) |

**Twin contract:** same accessor / acquisition / keyed shape as the ten R2a twins; no capability dropped
that the blocking store had. **Boundary:** reactive reads ride the N01 core boundary
(`DefaultReactiveReadBoundary`, bounded scheduler + timeout) **only** where the underlying client blocks;
the R2DBC account path is native and does not offload. Endpoint code adds **no** `.timeout` — timeout
lives in the boundary.

## Decomposition (tasks under internal work item 390)

| # | Issue | Task | Est |
|---|---|---|---|
| N01 | internal work item 1109 | Reactive session store twin (Aerospike, native) | 6h |
| N02 | internal work item 1110 | Reactive account store twin (R2DBC) — the reactive JPA path | 12h |
| N03 | internal work item 1111 | Thin proof consumer — 1–2 native reactive endpoints | 4h |
| N04 | internal work item 1112 | Prove the R2DBC twin against a real backend (H2) | 6h |
| N05 | internal work item 1113 | BlockHound-clean + 90% coverage + epic close-out | 4h |
| | | **Total** | **~32h** |

## Testing

- **Unit / behaviour:** failing test first, mutation-verified, per store twin. Assert observable behaviour,
  never source text.
- **Proof against a real backend:** N04, H2 via `r2dbc-h2`, executing here. Evidence captured, not asserted.
- **BlockHound:** N05, over the reactive session + account read paths, mutation-verified (break the
  mechanism, confirm the named test goes red).
- **Coverage:** 90% instruction bar on the new reactive surface.
- **Gated ITs:** any MariaDB / PostgreSQL IT added is either run here or registered in J-018 with its
  reason — it does not silently join J-018's unrun pile.

## Deferred to phase 2 (follow-on epic, owner-approved when reached)

- **The full ~30-servlet port** — R4 ports only 1–2 as proof (N03); the rest stay blocking.
- **Other account backings' reactive twins** — R4 twins R2DBC account + Aerospike session only;
  `-account-vitess` and `-account-firebaseauth` reactive twins deferred.
- **Write-path reactive endpoints** — the proof consumer is read-only; reactive writes deferred.
- **MariaDB / PostgreSQL real-backend ITs** — written but gated, registered in J-018.

*Not deferred, stated for clarity:* **native reactive JPA** is a permanent 1.0.0 limitation (D1).

## Risks

- **R2DBC ↔ JDBI schema parity.** The account store's JDBI schema must be reproduced against R2DBC/H2
  faithfully, or the twin diverges from the blocking store. N02 acceptance requires contract uniformity;
  N04 proves it against a real engine.
- **Twin-contract drift.** Ten twins already set the shape; "works" is not the bar, "uniform" is. Reviewed
  against an existing twin in N01/N02.
- **Milestone note.** N01–N05 are milestoned Release 1.0.0 to match epic internal work item 390 and the P02 feature-completion
  spec, diverging from R3's tasks (milestoned 2.0.0). Flagged to the owner; one-command fix if 2.0.0 is intended.
