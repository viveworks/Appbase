# J-002: Argon2id Server-Side Password Hashing Design

**Date:** 2026-06-19
**Issue:** internal work item 134
**Scope:** Java only (`appbase-service`). Flutter unchanged.

---

## Problem

`DefaultPasswordService` uses single-pass SHA-512 (`MessageDigest`) for the server password hash.
`SHA512_ROUNDS = 80` is stored but never applied — digest is computed once.
Single-pass SHA-512 is GPU-crackable in minutes.

Additionally, `hashWithRandomSalt` has a latent bug: raw salt bytes are fed to `MessageDigest.update()`
but the Base64-encoded string is stored in `PasswordHash.salt`, making string-based verification impossible.

---

## Decision

Replace the **server-side hash only** with Argon2id via Bouncy Castle (`bcprov-jdk18on:1.80`).

- Client hash (Flutter, `hashWithEmail`) stays SHA-512 — Flutter is unchanged.
- `serverPasswordHash` in `PasswordHashes` switches to Argon2id.
- OWASP 2023 parameters: `m=19456` (19 MiB), `t=2`, `p=1`, 32-byte output.

---

## Model: PasswordHash for Argon2id

`PasswordHash` fields are reused without structural change:

| Field | SHA-512 value | Argon2id value |
|---|---|---|
| `hashAlgorithm` | `SHA512` | `ARGON2ID` (new enum value) |
| `hash` | Base64(sha512 output) | Full PHC string (`$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>`) |
| `salt` | Base64 salt | Base64url salt extracted from PHC |
| `shaRounds` | 80 | `t` parameter (e.g. `2`) |

Existing `PasswordHash.Validator` passes for Argon2id: `hash` non-blank ✓, `salt` non-blank ✓, `shaRounds=2` positive ✓.

Storing the full PHC string in `hash` makes the hash self-describing — verification needs no config at all.

---

## Serialization

### PasswordHash (single)

**SHA-512** (unchanged):
```
$sha512$rounds=80$<base64-salt>$<base64-hash>
```

**Argon2id** — serialize by returning `hash` field directly (it is already the PHC string):
```
$argon2id$v=19$m=19456,t=2,p=1$<base64url-salt>$<base64url-hash>
```

Deserialize: if string starts with `$argon2id$`, parse PHC parts; else use existing `PASSWORD_HASH_PATTERN` regex.

### PasswordHashes (client + server concatenated in DB)

**Legacy (both SHA-512):**
```
$sha512$rounds=80$<cSalt>$<cHash>$sha512$rounds=80$<sSalt>$<sHash>
```

**New (client SHA-512 + server Argon2id):**
```
$sha512$rounds=80$<cSalt>$<cHash>$argon2id$v=19$m=19456,t=2,p=1$<sSalt>$<sHash>
```

Serialize: `serializePasswordHashImpl(client) + serializePasswordHashImpl(server)` — Argon2id case returns PHC directly, SHA-512 case returns old `$algo$rounds=...` format, concatenation works.

Deserialize: `indexOf("$argon2id$") > 0` → split at that index (left = client SHA-512, right = server Argon2id). No hit → fall back to existing `PASSWORD_HASHES_PATTERN` regex (both SHA-512, legacy rows).

---

## New API on PasswordService

```java
boolean verify(String plaintext, PasswordHash storedHash);
boolean needsRehash(PasswordHash storedHash);
```

- `verify`: re-derives hash from PHC params embedded in `storedHash.hash`, compares with `MessageDigest.isEqual` (constant-time).
- `needsRehash`: returns `true` when `hashAlgorithm != ARGON2ID` (flags legacy SHA-512 server hashes).

---

## Configuration (DefaultPasswordServiceConfiguration)

```yaml
memoryKiB: 19456    # Argon2id m parameter (OWASP 2023)
iterations: 2       # Argon2id t parameter
parallelism: 1      # Argon2id p parameter
hashLength: 32      # Output bytes
```

Config is used only at **hash creation** time. Verification always reads params from the stored PHC string.

---

## Files Changed

| File | Change |
|---|---|
| `HashAlgorithm.java` | Add `ARGON2ID("argon2id", "argon2id")` |
| `PasswordService.java` | Update serialize/deserialize; add `verify()`, `needsRehash()` |
| `DefaultPasswordService.java` | Argon2id impl via Bouncy Castle; `verify()`, `needsRehash()` |
| `DefaultPasswordServiceConfiguration.java` | Add `memoryKiB`, `iterations`, `parallelism`, `hashLength` |
| `DefaultPasswordServiceTest.java` | TDD: round-trip, verify, needsRehash, legacy compat |
| `appbase-service/pom.xml` | Add `bcprov-jdk18on:1.80` |

No Flutter changes. No SQL schema changes (VARCHAR(1024) has ~800 chars headroom).

---

## Migration Path

- Existing SHA-512 server hashes: `needsRehash()` returns `true` — rehash on next successful login.
- `hashWithRandomSalt` legacy hashes (raw-bytes salt bug): cannot verify → require password reset.
- `hashWithEmail`/`hashWithSalt` legacy hashes: can still be deserialized and verified (SHA-512 path preserved).

---

## Testing (TDD)

1. `hashWithRandomSalt` → Argon2id PHC string in `hash` field
2. `verify(plaintext, storedHash)` → round-trip passes, wrong password fails, constant-time
3. `needsRehash(argon2idHash)` → false; `needsRehash(sha512Hash)` → true
4. `deserializePasswordHash("$argon2id$...")` → parses correctly
5. `deserializePasswordHashes(legacy)` → both SHA-512, backward compat
6. `deserializePasswordHashes(mixed)` → client SHA-512 + server Argon2id
7. `serializePasswordHashes` → round-trip for both legacy and mixed formats
