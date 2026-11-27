# Authorization and path matching

How Appbase matches an incoming request path against configured security rules — authorization
allow/block lists, CORS headers, session creation, and filter enablement. This is the seed of the
user-facing configuration guide (P-010); it is written to be lifted into it.

> Delivered by P-027 R1. The behaviour described here is uniform across all three
> transport faces — servlet, JAX-RS, and reactor-netty.

## The one rule to remember

**Every security rule is keyed on the *canonical* form of the request path, and your configuration
keys are canonicalized the same way when they load.** Both sides of every lookup share one key
space, so a rule you write matches the request you meant — regardless of how the client encoded the
URL or which transport served it.

Write your path keys in plain, decoded, absolute form: `/admin`, `/api/users`. Do not percent-encode
them, do not add a context-path prefix, do not add a trailing slash unless you mean a different key.

## What canonicalization does to a path

Given the request path (each face supplies it in its own encoding; canonicalization erases the
difference), Appbase:

1. **Rejects a raw `?` or `#`.** A query string or fragment is not part of a security key.
2. **Rejects the encoded separators `%2F` (`/`), `%5C` (`\`), and the encoded null `%00`.** These are
   the octets an attacker uses to smuggle a path boundary past a rule; a request carrying one is
   answered with `400 Bad Request`, never matched.
3. **Collapses duplicate slashes:** `//admin` → `/admin`.
4. **Decodes percent-escapes once,** UTF-8. `/%61dmin` → `/admin`. A literal `+` stays a `+`
   (`/a+b` → `/a+b`) — it is not treated as a space.
5. **Rejects a malformed escape** (`%zz`, a truncated `%2`) or a **residual `%`** after decoding —
   `400 Bad Request`.
6. **Resolves `.` and `..` segments** per RFC 3986: `/x/../admin` → `/admin`, `/x/./y` → `/x/y`. A
   trailing `.` or `..` leaves the directory slash: `/admin/.` → `/admin/`.
7. **Ensures a leading `/`;** an empty path becomes `/`.

Two things it deliberately does **not** do, because they would change which requests your rule
matches:

- **No trailing-slash folding.** `/admin` and `/admin/` are *different* keys. A rule on `/admin` does
  not cover `/admin/`.
- **No case folding.** `/Admin` and `/admin` are different keys.

A configuration key that cannot be canonicalized — for example a CORS or session key you wrote as
`/a%2Fb` — **fails at startup**, with a message naming the key. A rule that could never match any
request is treated as a mistake, not accepted silently.

The `*` wildcard sentinel is never canonicalized; it means "any path" wherever a path key is
accepted.

## Authorization semantics

Authorization is decided by two optional sets of rules — an **allowlist** and a **blocklist** — plus
a fallback *context authorizer* your application supplies. The outcome for a request:

| Configuration | Path matches | Result |
|---|---|---|
| neither list configured | — | delegate to the context authorizer |
| blocklist only | listed | **deny** |
| blocklist only | not listed | delegate to the context authorizer |
| allowlist configured | listed | allow |
| allowlist configured | **not listed** | **deny** |
| both configured | in blocklist | **deny** (blocklist wins) |
| both configured | in allowlist only | allow |
| both configured | in neither | **deny** |
| `*` in blocklist | any | **deny** |
| `*` in allowlist, path unlisted | any | allow |

Read the two lists as opposites:

- A **blocklist** denies what it names and allows the rest ("the rest" being whatever the context
  authorizer decides). Use it to carve exceptions out of an otherwise-open surface.
- An **allowlist** allows what it names and denies the rest. Use it to expose a closed surface. **An
  allowlist always denies a path it does not list** — that is the whole point of an allowlist, and it
  is what makes it safe to rely on.

**When neither list is configured, authorization delegates rather than denies.** This is deliberate:
the authorization filter binds to every path (`/*`) on every deployment, so denying-when-unconfigured
would reject every request the first time an app boots before any rule is written. Configure at least
one list — or supply a restrictive context authorizer — to close that surface.

This delegation determines the authorization **decision**; it is not a response-policy bypass. If
the authorizer returns `false`, all three transport faces refuse the request. They use an explicit
`AuthorizationServiceExceptionConfiguration` action when one is configured (forbidden, redirect,
or custom status), and otherwise return `403 Forbidden`. An absent or actionless response policy
never turns a denied request into an allowed one.

## Why this matters (the defect this replaced)

Before P-027 R1, the three transport faces spelled the same request three different ways, and
configuration keys were matched as raw strings. A blocklist entry for `/admin` did not stop a request
for `/%61dmin`; an allowlist silently authorized paths it did not list; the same config behaved
differently on servlet, JAX-RS, and reactor-netty. All of that is closed: one canonical key space,
one set of semantics, every face.
