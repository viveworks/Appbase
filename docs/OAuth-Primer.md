# OAuth 2.0 / OIDC / JWT — a primer, and how this module implements it

This document explains, from first principles, how OAuth 2.0, OpenID Connect (OIDC), JSON Web Tokens
(JWT), JSON Web Signatures (JWS) and JSON Web Key Sets (JWKS) actually work — and then how the
`appbase-service-web-oauth` module implements them. It assumes **no prior background**: if you have never
seen a bearer token before, start at the top and read straight through.

**What this document is not.** It is a conceptual primer only. It deliberately does **not** duplicate:

- the **public-API reference** (P-006) — the Javadoc/signature-level contract of each class;
- the **usage guide** (P-010) — the step-by-step "configure and deploy it in your app" chapter.

When you want "what does method X return", read the API reference. When you want "how do I switch Google
on in my service", read the usage guide. When you want "*why* does any of this work, and what is it
protecting me from" — you are in the right place.

---

## 1. The problem OAuth solves

A user wants to use your service. Your service needs to know **who they are**, but you do not want to
store their password — storing passwords is a liability, and users do not want yet another one. So you
delegate: a trusted **identity provider** (Google, Apple, Microsoft, Okta, GitHub) authenticates the
user, and hands your service a **token** that proves "this provider vouches for this user".

Three parties, and it helps to name them:

- **Resource server** — your service. It holds the thing the user wants (their account, their data). In
  this module, the resource server is what the OAuth *filter* protects.
- **Authorization server / identity provider** — Google, Apple, etc. It authenticates the user and
  issues tokens.
- **Client** — the front-end or app the user drives, which shuttles the user to the provider and back.

OAuth 2.0 is the framework for issuing and presenting those tokens. **OIDC** is a thin layer on top of
OAuth 2.0 that standardises an *identity* token (the **ID token**) so the resource server learns *who*
the user is, not merely *that* they authorised something.

---

## 2. The authorization-code flow, in context

The dominant flow is the **authorization-code flow**. The user is redirected to the provider, logs in
there (your service never sees the password), and the provider redirects back with a short-lived
**authorization code**. The client exchanges that code — over a back channel — for tokens.

```mermaid
sequenceDiagram
    actor User
    participant Client as Client (app / SPA)
    participant IdP as Identity Provider<br/>(Google / Apple / …)
    participant RS as Resource Server<br/>(your service + OAuth filter)

    User->>Client: "Sign in with Google"
    Client->>IdP: redirect: authorize?client_id&redirect_uri&scope
    User->>IdP: authenticate (password / passkey / MFA)
    IdP-->>Client: redirect back with authorization CODE
    Client->>IdP: exchange CODE (+ client secret) for tokens
    IdP-->>Client: id_token (JWT) + access_token
    Client->>RS: request + "Authorization: Bearer <id_token>"
    RS->>RS: validate token (this module)
    RS-->>Client: 200 (or 401 if the token fails validation)
```

**Where this module lives:** the last two steps. This module does **not** implement the redirect dance or
the code exchange — that is the client's and the provider's job. It implements the **resource-server
side**: given an incoming `Authorization: Bearer <token>`, decide whether to trust it, and if so, who it
represents. Everything below is about that decision.

---

## 3. What a token actually is: JWT, JWS, JWKS

Most providers issue the ID token as a **JWT** — a JSON Web Token. A JWT is three
Base64URL-encoded parts joined by dots:

```
<header>.<payload>.<signature>
```

### 3.1 A worked example, decoded

Take this token (line-wrapped for readability; a real one has no line breaks):

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImFiYzEyMyJ9
.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhdWQiOiJteS1jbGllbnQtaWQi
LCJzdWIiOiIxMDgyNzM2NDkxODIiLCJleHAiOjE3ODYwMDAwMDAsImlhdCI6MTc4NTk5NjQwMH0
.<signature-bytes>
```

The **header** (first part) decodes to:

```json
{ "alg": "RS256", "typ": "JWT", "kid": "abc123" }
```

- `alg` — the signature algorithm. `RS256` = RSA signature with SHA-256. **This field is a security
  decision, not a hint** (see §6.1).
- `kid` — the **key id**. It names *which* of the provider's public keys signed this token. This is the
  hook that makes key rotation work (§5).

The **payload** (second part) decodes to a set of **claims**:

```json
{
  "iss": "https://accounts.google.com",
  "aud": "my-client-id",
  "sub": "108273649182",
  "exp": 1786000000,
  "iat": 1785996400
}
```

- `iss` (issuer) — who minted the token. Must be exactly the provider you expect.
- `aud` (audience) — **who the token is for**. Your client id. A token minted for a *different* audience
  must be refused even if its signature is perfect (§6.3).
- `sub` (subject) — the stable user id. This is the identity you act on.
- `exp` (expiry) / `iat` (issued-at) — Unix timestamps bounding the token's validity window.

The **signature** (third part) is computed by the provider over `header.payload` using the provider's
**private** key. You verify it with the provider's **public** key.

### 3.2 JWS — how the signature is trusted

**JWS** (JSON Web Signature) is the standard that defines that signature. The security property is
asymmetric: only the provider holds the private key, so only the provider can *produce* a valid
signature, but *anyone* with the public key can *verify* it. You never need a shared secret with the
provider to check a token — you need the provider's public key.

### 3.3 JWKS — where the public key comes from

The provider publishes its public keys at a well-known HTTPS endpoint as a **JWKS** — a JSON Web Key Set.
A JWKS is a JSON document listing one or more keys, each tagged with the `kid` that appears in token
headers:

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "abc123",
      "alg": "RS256",
      "n": "0vx7ag…very-long-modulus…KqJm",
      "e": "AQAB"
    },
    {
      "kty": "RSA", "use": "sig", "kid": "def456",
      "alg": "RS256", "n": "nZG9k…", "e": "AQAB"
    }
  ]
}
```

To verify a token, you read its header `kid`, fetch the matching key from the JWKS, and check the
signature against it. That is the whole trust chain: **token header → `kid` → JWKS key → verify**.

---

## 4. The request lifecycle in this module

When a request arrives, the OAuth **filter** (`OAuthFilter` for servlets; a reactive `WebFilter` twin for
reactor-netty) runs this lifecycle. The security-critical validation is delegated to `NimbusTokenValidator`
(for JWT providers) and cached in a `TokenCache`.

```mermaid
sequenceDiagram
    participant Req as Incoming request
    participant Filter as OAuthFilter
    participant Cache as TokenCache
    participant Validator as NimbusTokenValidator
    participant JWKS as JWKS cache (provider keys)
    participant App as Application handler

    Req->>Filter: Authorization: Bearer <token>
    alt no bearer token
        Filter-->>Req: 401
    else has token
        Filter->>Filter: key = hash(namespace ‖ token)
        Filter->>Cache: get(key)
        alt cache hit
            Cache-->>Filter: ValidatedToken
            Filter->>App: attach token, continue chain
        else cache miss
            Filter->>Validator: validate(token)
            Validator->>JWKS: public key for header.kid
            JWKS-->>Validator: key (refetch if kid is new)
            Validator->>Validator: verify signature, then exp/nbf, iss, aud
            alt valid
                Validator-->>Filter: ValidatedToken
                Filter->>Cache: put(key, token, min(cacheTtl, exp−now))
                Filter->>App: attach token, continue chain
            else invalid
                Validator-->>Filter: ValidationFailure(reason)
                Filter-->>Req: 401 (chain untouched)
            end
        end
    end
```

Two design points worth calling out, because they are where subtle bugs live:

- **The cache key is namespaced to the filter's own identity.** It is `hash(cacheNamespace ‖ SEPARATOR ‖
  rawToken)`, where the namespace is derived from *this* filter's `OAuthConfiguration` (issuer, client
  id). Two differently-configured filters therefore compute **different** keys for the same raw token, so
  a token validated for one audience can never be served from a shared cache to a filter that would have
  rejected it. (The `ValidatedToken.tokenKey` field is a *different*, namespace-free hash and is **not**
  the cache key — a shared-cache backend keying on it alone would reintroduce a cross-audience bypass.)
- **A cache hit skips re-validation.** That is the point of the cache — but it is why the entry's TTL is
  `min(cacheTtl, exp − now)`: an entry must never outlive the token's own expiry, or a cache hit would
  serve an expired token.

### 4.1 The `OAuthConfiguration` that drives it

A provider is configured by an `OAuthConfiguration`. The example below is illustrative — each key maps to
an `OAuthConfiguration` record component (`jwksUri`, `issuer`, `clientId`, `audienceClaim`,
`tokenExpiryMarginSeconds`, `cacheTtlSeconds`, `tokenKeyHashing`); the exact YAML key spelling is fixed by
the service's config binding and documented in the usage guide (P-010):

```yaml
oauth:
  jwks-uri: "https://www.googleapis.com/oauth2/v3/certs"   # HTTPS, always
  issuer: "https://accounts.google.com"                     # expected iss
  client-id: "my-client-id"                                 # this relying party
  audience-claim: "my-client-id"                            # expected aud VALUE
  token-expiry-margin-seconds: 60                           # clock-skew allowance
  cache-ttl-seconds: 300                                    # upper bound on cache lifetime
  token-key-hashing: SHA256                                 # cache-key hasher
```

For the OIDC providers (`google`, `okta`, `microsoft`, `apple`) most of this is pre-filled from the
provider's published discovery values — a relying party supplies only its client id. Validation itself is
always the same core Nimbus engine; a provider module adds only defaults and, where the provider needs
it, a small amount of provider-specific glue (see §5.3).

### 4.2 The incoming header

The token is presented in the HTTP `Authorization` header with the `Bearer` scheme:

```http
GET /api/account HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImFiYzEyMyJ9.eyJpc3MiOiJodHRwczov…
```

No token, or a header that is not `Bearer <token>`, is a 401 before any validation runs.

---

## 5. Key rotation — why the JWKS is a moving target

Providers **rotate** their signing keys: periodically they start signing with a new key (a new `kid`) and
retire the old one. If your resource server cached the JWKS once at startup and never refreshed it, the
first token signed with a freshly-rotated key would fail to verify — the `kid` would not be in your cached
set — and every user would get a spurious 401.

```mermaid
sequenceDiagram
    participant Provider as Identity Provider
    participant JWKS as JWKS cache (in the validator)
    participant Validator as NimbusTokenValidator

    Note over Provider: signing with kid=abc123
    Validator->>JWKS: token has kid=abc123 → key present → verify OK

    Note over Provider: rotation: now signing with kid=def456<br/>(abc123 still published for overlap)
    Validator->>JWKS: token has kid=def456 → MISS
    JWKS->>Provider: refetch JWKS (rate-limited)
    Provider-->>JWKS: keys now include def456
    JWKS-->>Validator: key found → verify OK

    Note over Provider: abc123 eventually retired
    Note over JWKS: refresh-ahead keeps the set current;<br/>tokens on the retired key then fail as expected
```

This module gets rotation right **for free** because it does not hand-roll key handling: it uses Nimbus's
`JWKSourceBuilder`, which maintains a **`kid`-keyed, refresh-ahead, rate-limited** key set that **refetches
on a cache miss**. So a token signed with a key published *after* the last refresh still validates. This is
exactly the "signature verification is the security-critical part, so do not hand-write it" principle in
action.

### 5.1 Three shapes of provider

Not every provider is a JWT/JWKS provider. This module supports three shapes:

1. **OIDC providers** (`google`, `okta`, `microsoft`, `apple`) — issue a JWT ID token; validated by the
   core `NimbusTokenValidator` against the provider's JWKS, exactly as above.
2. **GitHub** — issues an **opaque** access token (not a JWT; there is nothing to verify locally). Its
   module validates by making an authenticated call to GitHub's `/user` endpoint, cached under a short
   TTL. Different mechanism, same `TokenValidator` interface.
3. **Apple**, additionally — is the one provider that requires the relying party to **mint its own client
   secret**: a short-lived **ES256**-signed JWT (`AppleClientSecretGenerator`). Apple caps that secret at
   six months, so it must be rotated. This is the mirror image of validation — here *we* are the signer —
   and it is why the Apple module carries a signing key where the others do not.

---

## 6. Security pitfalls — why each rejection matters

The validator returns a typed `ValidationFailure.Reason` for every rejection, and the filter maps every
one to **HTTP 401**. Each reason exists because there is a real attack it stops. This section is the
"*why*" behind the rejection matrix.

### 6.1 `alg: none` → `MALFORMED`

The JWT header names its own algorithm. The original sin of JWT libraries was **honouring `alg: none`** — a
token that declares "I am unsigned" and is accepted anyway. An attacker then forges any claims they like,
sets `alg` to `none`, sends no signature, and is trusted.

**How this module blocks it:** an unsigned token has an empty signature segment, so it does not even parse
as a *signed* JWT — it is rejected as `MALFORMED` before any key or claim work. A token that instead
carries a real signature under a non-RS256 `alg` is rejected by the pinned key selector
(`JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys)`) as `BAD_SIGNATURE` (see §6.2). Either way there
is no code path that trusts an unsigned or wrong-algorithm token. (The `ValidationFailure.Reason` enum also
reserves `UNSUPPORTED_ALGORITHM`, but the current RS256-pinned engine rejects these tokens earlier, as
`MALFORMED`/`BAD_SIGNATURE`, so that value is not produced today.)

### 6.2 Algorithm confusion (RS256 → HS256) → `BAD_SIGNATURE`

A subtler attack: the provider signs with RSA (`RS256`, asymmetric). An attacker takes the provider's
**public** key (it is public, by design) and re-signs a forged token with `HS256` — HMAC, a *symmetric*
algorithm that uses the same key to sign and verify. A naive library that reads `alg` from the token and
then "uses the configured key" would use the RSA public key as an HMAC secret — which the attacker also
knows — and the forgery verifies.

**How this module blocks it:** same mechanism as §6.1. The algorithm is fixed by the *verifier's*
configuration (`RS256`), not read from the attacker-controlled header. An `HS256` token never reaches a
verifier. There is also no shared key material between the RS256 validation path and the ES256 Apple
client-secret path, so the two cannot be confused.

### 6.3 Audience confusion → `AUDIENCE_MISMATCH`

A token can be perfectly signed by the right provider and still not be for you. If service A and service B
both trust Google, a token minted for A's `aud` is a valid Google token — but presenting it to B must
fail, or A could impersonate its users to B.

**How this module blocks it:** after signature verification, the validator explicitly checks that the
token's `aud` set **contains the expected audience value** (`audience.contains(expectedAudience)`), and
rejects otherwise. Audience is a *value* check against *this* resource server's configured client id — a
signed-but-wrong-audience token is refused. (The cache-key namespacing in §4 is the second layer of this
same defence.)

### 6.4 Expiry and not-before → `EXPIRED`

A token is a bearer credential: whoever holds it is trusted. So its lifetime must be bounded, or a
once-captured token is valid forever.

**How this module blocks it:** the validator rejects a token whose `exp` has passed (with a symmetric
clock-skew margin), a token that is not yet valid (`nbf` in the future), **and** — importantly — a token
with **no `exp` claim at all**. A missing expiry is treated as a rejection, not as an unbounded lifetime
(RFC 9068 makes `exp` required on a JWT access token). The cache TTL is separately clamped to `exp − now`
so a cache hit can never outlive the token.

### 6.5 Untrusted issuer → `UNTRUSTED_ISSUER`

The `iss` claim says who minted the token. If you do not pin it, a token from *any* provider whose key you
happen to fetch could be accepted.

**How this module blocks it:** the validator checks `iss` equals the exact expected issuer configured for
this provider. For the OIDC providers the issuer is a fixed, published constant (e.g.
`https://appleid.apple.com`), not a caller-supplied value that could be spoofed.

### 6.6 Key rotation done wrong → `PROVIDER_UNREACHABLE` vs a stuck cache

If key handling were hand-rolled and cached-once, a rotated key would cause mass spurious 401s (§5). If
instead the JWKS endpoint is unreachable at the moment of a needed refetch, the honest answer is a typed
`PROVIDER_UNREACHABLE`, distinct from "bad signature" — the token is not proven bad, the *provider's keys*
are momentarily unavailable, and that distinction matters for operations.

### The rejection matrix, at a glance

| Reason (`ValidationFailure.Reason`) | Attack / condition it stops |
|---|---|
| `MALFORMED` | not a well-formed signed JWT — including an unsigned `alg: none` token |
| `BAD_SIGNATURE` | forged / tampered token, algorithm-confusion (RS256→HS256), any non-RS256 `alg` |
| `EXPIRED` | replayed old token, missing/future validity window |
| `UNTRUSTED_ISSUER` | token from a provider you did not configure |
| `AUDIENCE_MISMATCH` | valid token minted for a *different* resource server |
| `PROVIDER_UNREACHABLE` | JWKS momentarily unfetchable (not a bad token) |
| `UNSUPPORTED_ALGORITHM` | reserved — not produced by the current RS256-pinned engine (see §6.1) |

Every one is a 401 to the caller; the *reason* is what the resource server records, never the raw token or
signature.

---

## 7. Where to go next

- **Configure a provider in your service:** the usage guide (P-010).
- **The exact class/method contracts:** the public-API reference (P-006).
- **The security rationale in depth, and the audit lenses:** apply the threat, secure-code,
  business-logic, input/output, test-suite, and contract lenses described throughout this primer.

The one thing to carry away: the resource server's job is a **decision** — trust this token, or return
401 — and every field of a JWT, every line of the JWKS, and every rejection reason above exists to make
that decision both correct and impossible to trick.
