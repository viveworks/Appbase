/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.service.web.oauth.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.viveworks.appbase.service.web.oauth.Sha256TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenCache;
import com.viveworks.appbase.service.web.oauth.TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import com.viveworks.appbase.service.web.oauth.ValidationFailure;
import com.viveworks.appbase.service.web.oauth.ValidationResult;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Behaviour of the GitHub opaque-token validator (task internal work item 1197, spec §5 GitHub, §6 N07 row): a valid token
 * validates against a stubbed {@code /user}, the network call happens only on a cache miss, a revoked
 * token is served from cache only within the short TTL and rejected once it expires, and an unreachable
 * provider is a {@code PROVIDER_UNREACHABLE} rejection rather than an invalid-token verdict.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
class GitHubTokenValidatorTest {

    private static final String ISSUER = "https://github.com";
    private static final String CLIENT_ID = "Iv1.client-abc";
    private static final long SHORT_TTL_SECONDS = 30;
    private static final String USER_JSON = "{\"id\":42,\"login\":\"octocat\",\"name\":\"The Octocat\"}";

    private static GitHubOAuthConfiguration config() {
        return new GitHubOAuthConfiguration(
                GitHubOAuthConfiguration.DEFAULT_USER_API_URI, ISSUER, CLIENT_ID, SHORT_TTL_SECONDS);
    }

    @Test
    void validTokenYieldsValidatedTokenFromUserEndpoint() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        StubUserApi api = StubUserApi.returning(200, USER_JSON);
        TokenKeyHasher hasher = new Sha256TokenKeyHasher();
        GitHubTokenValidator validator =
                new GitHubTokenValidator(config(), hasher, new FakeTokenCache(clock), api, clock);

        ValidationResult result = validator.validate("gho_valid-token");

        ValidatedToken token = assertInstanceOf(ValidatedToken.class, result);
        assertEquals("42", token.subject());
        assertEquals(ISSUER, token.issuer());
        assertTrue(token.audience().isEmpty(),
                "GitHub /user authenticates the user, not the app, so no audience may be asserted");
        assertEquals("octocat", token.claims().get("login"));
        assertEquals(hasher.hash("gho_valid-token"), token.tokenKey());
        assertEquals(clock.instant().plusSeconds(SHORT_TTL_SECONDS), token.expiresAt());
    }

    @Test
    void networkCallHappensOnlyOnCacheMiss() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        StubUserApi api = StubUserApi.returning(200, USER_JSON);
        GitHubTokenValidator validator = new GitHubTokenValidator(
                config(), new Sha256TokenKeyHasher(), new FakeTokenCache(clock), api, clock);

        validator.validate("gho_token");
        ValidationResult second = validator.validate("gho_token");

        assertInstanceOf(ValidatedToken.class, second);
        assertEquals(1, api.callCount(), "second validation should be served from cache, no network");
    }

    @Test
    void revokedTokenServedFromCacheWithinShortTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        StubUserApi api = StubUserApi.returning(200, USER_JSON);
        GitHubTokenValidator validator = new GitHubTokenValidator(
                config(), new Sha256TokenKeyHasher(), new FakeTokenCache(clock), api, clock);

        validator.validate("gho_token");
        api.setResponse(401, "{\"message\":\"Bad credentials\"}"); // revoked upstream
        clock.advance(Duration.ofSeconds(SHORT_TTL_SECONDS - 1)); // still inside the short TTL

        ValidationResult result = validator.validate("gho_token");

        assertInstanceOf(ValidatedToken.class, result);
        assertEquals(1, api.callCount(), "within the TTL the revoked token is still served from cache");
    }

    @Test
    void revokedTokenRejectedAfterShortTtlExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        StubUserApi api = StubUserApi.returning(200, USER_JSON);
        GitHubTokenValidator validator = new GitHubTokenValidator(
                config(), new Sha256TokenKeyHasher(), new FakeTokenCache(clock), api, clock);

        validator.validate("gho_token");
        api.setResponse(401, "{\"message\":\"Bad credentials\"}"); // revoked upstream
        clock.advance(Duration.ofSeconds(SHORT_TTL_SECONDS + 1)); // past the short TTL

        ValidationResult result = validator.validate("gho_token");

        ValidationFailure failure = assertInstanceOf(ValidationFailure.class, result);
        assertEquals(ValidationFailure.Reason.BAD_SIGNATURE, failure.reason());
        assertEquals(2, api.callCount(), "after the TTL the cache misses and the provider is re-checked");
    }

    @Test
    void upstreamRejectionIsNotCached() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        StubUserApi api = StubUserApi.returning(401, "{\"message\":\"Bad credentials\"}");
        GitHubTokenValidator validator = new GitHubTokenValidator(
                config(), new Sha256TokenKeyHasher(), new FakeTokenCache(clock), api, clock);

        validator.validate("gho_token");
        validator.validate("gho_token");

        assertEquals(2, api.callCount(), "a rejection must not be cached: every call re-checks the provider");
    }

    @Test
    void providerUnreachableYieldsProviderUnreachableReason() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        StubUserApi api = StubUserApi.throwing();
        GitHubTokenValidator validator = new GitHubTokenValidator(
                config(), new Sha256TokenKeyHasher(), new FakeTokenCache(clock), api, clock);

        ValidationResult result = validator.validate("gho_token");

        ValidationFailure failure = assertInstanceOf(ValidationFailure.class, result);
        assertEquals(ValidationFailure.Reason.PROVIDER_UNREACHABLE, failure.reason());
    }

    @Test
    void differentClientIdsDoNotShareCacheEntries() {
        // A shared TokenCache backend (the designed N03 end state) must not let a token validated for one
        // GitHub app be served from cache to a differently-configured validator that would reject it —
        // the cross-audience bypass fixed in internal work item 1237. GitHub's /user does not check client_id, so the
        // namespace-bound cache key is the only guard.
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        TokenCache sharedCache = new FakeTokenCache(clock);
        GitHubTokenValidator appA = new GitHubTokenValidator(
                new GitHubOAuthConfiguration(GitHubOAuthConfiguration.DEFAULT_USER_API_URI, ISSUER, "app-A", 30),
                new Sha256TokenKeyHasher(), sharedCache, StubUserApi.returning(200, USER_JSON), clock);
        StubUserApi appBApi = StubUserApi.returning(401, "{\"message\":\"Bad credentials\"}");
        GitHubTokenValidator appB = new GitHubTokenValidator(
                new GitHubOAuthConfiguration(GitHubOAuthConfiguration.DEFAULT_USER_API_URI, ISSUER, "app-B", 30),
                new Sha256TokenKeyHasher(), sharedCache, appBApi, clock);

        appA.validate("gho_token"); // A caches a success under its own namespace
        ValidationResult resultForB = appB.validate("gho_token");

        assertInstanceOf(ValidationFailure.class, resultForB);
        assertEquals(1, appBApi.callCount(), "B must run its own validation, not read A's cache entry");
    }

    @Test
    void tokenKeyOnValidatedTokenStaysNamespaceFree() {
        // The ValidatedToken.tokenKey SPI field is contractually the namespace-FREE hash of the raw token,
        // distinct from the namespace-bound cache key. See ValidatedToken javadoc.
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        TokenKeyHasher hasher = new Sha256TokenKeyHasher();
        GitHubTokenValidator validator =
                new GitHubTokenValidator(config(), hasher, new FakeTokenCache(clock),
                        StubUserApi.returning(200, USER_JSON), clock);

        ValidatedToken token = (ValidatedToken) validator.validate("gho_token");

        assertEquals(hasher.hash("gho_token"), token.tokenKey());
    }

    @Test
    void rateLimitedResponseYieldsProviderUnreachable() {
        // GitHub returns 403 for rate-limit exhaustion, SAML-SSO and IP-allowlist blocks — none of which
        // mean the token is invalid. It must not be reported as a rejected token.
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        StubUserApi api = StubUserApi.returning(403, "{\"message\":\"API rate limit exceeded\"}");
        GitHubTokenValidator validator = new GitHubTokenValidator(
                config(), new Sha256TokenKeyHasher(), new FakeTokenCache(clock), api, clock);

        ValidationResult result = validator.validate("gho_token");

        ValidationFailure failure = assertInstanceOf(ValidationFailure.class, result);
        assertEquals(ValidationFailure.Reason.PROVIDER_UNREACHABLE, failure.reason());
    }

    @Test
    void malformedUserBodyYieldsProviderUnreachable() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        StubUserApi api = StubUserApi.returning(200, "this is not json");
        GitHubTokenValidator validator = new GitHubTokenValidator(
                config(), new Sha256TokenKeyHasher(), new FakeTokenCache(clock), api, clock);

        ValidationResult result = validator.validate("gho_token");

        ValidationFailure failure = assertInstanceOf(ValidationFailure.class, result);
        assertEquals(ValidationFailure.Reason.PROVIDER_UNREACHABLE, failure.reason());
    }

    @Test
    void nonNumericIdIsRejectedNotAcceptedAsSubject() {
        // GitHub always returns a numeric id. A non-number (a string that could carry an injection
        // payload, or an object) must not become the ValidatedToken subject.
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        for (String body : new String[] {"{\"id\":\"42; DROP TABLE users\"}", "{\"id\":{\"nested\":1}}"}) {
            GitHubTokenValidator validator = new GitHubTokenValidator(
                    config(), new Sha256TokenKeyHasher(), new FakeTokenCache(clock),
                    StubUserApi.returning(200, body), clock);

            ValidationResult result = validator.validate("gho_token");

            ValidationFailure failure = assertInstanceOf(ValidationFailure.class, result,
                    "non-numeric id must be rejected, not accepted: " + body);
            assertEquals(ValidationFailure.Reason.PROVIDER_UNREACHABLE, failure.reason());
        }
    }

    @Test
    void rawTokenContainingSeparatorIsRejected() {
        // The cache key is hash(namespace ‖ SEPARATOR ‖ token). A token carrying the SEPARATOR could
        // shift that boundary, so the validator rejects it itself rather than trusting callers to strip it.
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        StubUserApi api = StubUserApi.returning(200, USER_JSON);
        GitHubTokenValidator validator = new GitHubTokenValidator(
                config(), new Sha256TokenKeyHasher(), new FakeTokenCache(clock), api, clock);

        ValidationResult result = validator.validate("gho_\u0000_token");

        ValidationFailure failure = assertInstanceOf(ValidationFailure.class, result);
        assertEquals(ValidationFailure.Reason.MALFORMED, failure.reason());
        assertEquals(0, api.callCount(), "a malformed token is rejected before any network call");
    }

    @Test
    void rejectsNonPositiveCacheTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        GitHubOAuthConfiguration zeroTtl = new GitHubOAuthConfiguration(
                GitHubOAuthConfiguration.DEFAULT_USER_API_URI, ISSUER, CLIENT_ID, 0);

        assertThrows(IllegalArgumentException.class, () -> new GitHubTokenValidator(
                zeroTtl, new Sha256TokenKeyHasher(), new FakeTokenCache(clock),
                StubUserApi.returning(200, USER_JSON), clock));
    }

    @Test
    void rejectsNonHttpsUserApiUri() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        GitHubOAuthConfiguration insecure = new GitHubOAuthConfiguration(
                URI.create("http://api.github.com/user"), ISSUER, CLIENT_ID, SHORT_TTL_SECONDS);

        assertThrows(IllegalArgumentException.class, () -> new GitHubTokenValidator(
                insecure, new Sha256TokenKeyHasher(), new FakeTokenCache(clock),
                StubUserApi.returning(200, USER_JSON), clock));
    }

    // --- test doubles ---------------------------------------------------------------------------

    /** A hand-advanced clock so TTL eviction is deterministic without sleeping. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    /** A {@link TokenCache} whose entries expire against the shared {@link MutableClock}. */
    private static final class FakeTokenCache implements TokenCache {
        private final Clock clock;
        private final Map<String, Entry> entries = new HashMap<>();

        FakeTokenCache(Clock clock) {
            this.clock = clock;
        }

        @Override
        public Optional<ValidatedToken> get(String tokenKey) {
            Entry entry = entries.get(tokenKey);
            if (entry == null || !clock.instant().isBefore(entry.expiry)) {
                return Optional.empty();
            }
            return Optional.of(entry.token);
        }

        @Override
        public void put(String tokenKey, ValidatedToken token, Duration ttl) {
            entries.put(tokenKey, new Entry(token, clock.instant().plus(ttl)));
        }

        private record Entry(ValidatedToken token, Instant expiry) {
        }
    }

    /** A stubbed {@link GitHubUserApi} that counts calls and can be reconfigured or made to fail. */
    private static final class StubUserApi implements GitHubUserApi {
        private int statusCode;
        private String body;
        private boolean throwTransport;
        private int callCount;

        static StubUserApi returning(int statusCode, String body) {
            StubUserApi api = new StubUserApi();
            api.statusCode = statusCode;
            api.body = body;
            return api;
        }

        static StubUserApi throwing() {
            StubUserApi api = new StubUserApi();
            api.throwTransport = true;
            return api;
        }

        void setResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        int callCount() {
            return callCount;
        }

        @Override
        public Response fetch(String bearerToken) {
            callCount++;
            if (throwTransport) {
                throw new GitHubUserApiException("stubbed transport failure", new RuntimeException());
            }
            return new Response(statusCode, body);
        }
    }
}
