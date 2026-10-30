/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
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

package com.viveworks.appbase.service.web.oauth;

import static com.viveworks.appbase.service.web.oauth.JwksTestServer.publicSet;
import static com.viveworks.appbase.service.web.oauth.JwksTestServer.rsaKey;
import static com.viveworks.appbase.service.web.oauth.JwksTestServer.signRs256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class OAuthFilterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String RAW_TOKEN = "eyJhbGciOiJSUzI1NiJ9.payload.signature";
    private static final TokenKeyHasher HASHER = new Sha256TokenKeyHasher();
    private static final String ISSUER = "https://issuer.example";
    private static final String CLIENT_ID = "client-1";
    private static final String AUDIENCE_CLAIM = "aud";

    private final TokenValidator validator = mock(TokenValidator.class);
    private final TokenCache cache = mock(TokenCache.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    private static OAuthConfiguration config(final String clientId, final long cacheTtlSeconds) {
        return config(URI.create("https://issuer.example/jwks"), clientId, 30L, cacheTtlSeconds);
    }

    private static OAuthConfiguration config(final URI jwksUri, final String clientId,
            final long marginSeconds, final long cacheTtlSeconds)
    {
        return new OAuthConfiguration(jwksUri, ISSUER, clientId, AUDIENCE_CLAIM,
                marginSeconds, cacheTtlSeconds, TokenKeyHashing.SHA256, null);
    }

    private OAuthFilter filter(final Duration cacheTtl) {
        return new OAuthFilter(validator, cache, HASHER, config(CLIENT_ID, cacheTtl.toSeconds()),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ValidatedToken validToken(final Instant expiresAt) {
        return new ValidatedToken("sub", ISSUER, Set.of("api"), expiresAt, Map.of(), "unused-in-n01");
    }

    private void authorization(final String headerValue) {
        when(request.getHeader("Authorization")).thenReturn(headerValue);
    }

    @Test
    void rejectsRequestWithNoAuthorizationHeader() throws Exception {
        authorization(null);

        filter(Duration.ofMinutes(5)).doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
        verifyNoInteractions(validator);
    }

    @Test
    void rejectsRequestWhoseSchemeIsNotBearer() throws Exception {
        authorization("Basic dXNlcjpwYXNz");

        filter(Duration.ofMinutes(5)).doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
        verifyNoInteractions(validator);
    }

    @Test
    void rejectsABearerTokenContainingTheNamespaceSeparator() throws Exception {
        // Defense in depth: a real JWT is base64url and never contains a NUL. Rather than trust every
        // servlet container to strip control bytes, a token carrying one is rejected outright.
        // Mutation: drop the separator check in bearerToken() and this goes red (the token is validated).
        authorization("Bearer abc" + Character.MIN_VALUE + "def");

        filter(Duration.ofMinutes(5)).doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
        verifyNoInteractions(validator);
    }

    @Test
    void servesCacheHitWithoutRevalidating() throws Exception {
        authorization("Bearer " + RAW_TOKEN);
        final ValidatedToken cached = validToken(NOW.plusSeconds(100));
        when(cache.get(anyString())).thenReturn(Optional.of(cached));

        filter(Duration.ofMinutes(5)).doFilter(request, response, chain);

        verify(request).setAttribute(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE, cached);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(validator);
    }

    @Test
    void rejectsInvalidTokenAndDoesNotContinueTheChain() throws Exception {
        authorization("Bearer " + RAW_TOKEN);
        when(cache.get(anyString())).thenReturn(Optional.empty());
        when(validator.validate(RAW_TOKEN))
                .thenReturn(new ValidationFailure(ValidationFailure.Reason.BAD_SIGNATURE, "bad sig"));

        filter(Duration.ofMinutes(5)).doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
        verify(cache, never()).put(any(), any(), any());
    }

    @Test
    void cachesAndForwardsAValidatedTokenOnCacheMiss() throws Exception {
        authorization("Bearer " + RAW_TOKEN);
        when(cache.get(anyString())).thenReturn(Optional.empty());
        final ValidatedToken validated = validToken(NOW.plusSeconds(100));
        when(validator.validate(RAW_TOKEN)).thenReturn(validated);

        // cacheTtl (300s) is larger than exp-now (100s), so the entry ttl must be the token's own life.
        filter(Duration.ofSeconds(300)).doFilter(request, response, chain);

        final ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(cache).put(key.capture(), eq(validated), ttl.capture());
        assertEquals(Duration.ofSeconds(100), ttl.getValue());
        // The cache key is a hashed digest, never the replayable raw token.
        assertTrue(key.getValue().matches("[0-9a-f]{64}"));
        assertNotEquals(RAW_TOKEN, key.getValue());
        verify(request).setAttribute(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE, validated);
        verify(chain).doFilter(request, response);
    }

    @Test
    void cacheTtlCapsAShortLivedEntry() throws Exception {
        authorization("Bearer " + RAW_TOKEN);
        when(cache.get(anyString())).thenReturn(Optional.empty());
        final ValidatedToken validated = validToken(NOW.plusSeconds(500));
        when(validator.validate(RAW_TOKEN)).thenReturn(validated);

        // exp-now (500s) exceeds cacheTtl (60s), so the entry ttl is capped at cacheTtl.
        filter(Duration.ofSeconds(60)).doFilter(request, response, chain);

        final ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(cache).put(anyString(), eq(validated), ttl.capture());
        assertEquals(Duration.ofSeconds(60), ttl.getValue());
    }

    @Test
    void doesNotCacheATokenAlreadyPastExpiry() throws Exception {
        authorization("Bearer " + RAW_TOKEN);
        when(cache.get(anyString())).thenReturn(Optional.empty());
        // The validator accepts a token whose exp is already in the past, within the configured
        // clock-skew margin (tokenExpiryMarginSeconds). exp-now is then negative, so caching it would
        // hand a negative Duration to the backend — undefined for Caffeine, a crash for Redis/Aerospike.
        // Mutation: cache unconditionally and this goes red.
        final ValidatedToken validated = validToken(NOW.minusSeconds(5));
        when(validator.validate(RAW_TOKEN)).thenReturn(validated);

        filter(Duration.ofSeconds(300)).doFilter(request, response, chain);

        verify(cache, never()).put(any(), any(), any());
        // The request is still authenticated and forwarded — only the pointless cache write is skipped.
        verify(request).setAttribute(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE, validated);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doesNotServeACachedTokenToADifferentlyConfiguredFilter() throws Exception {
        // Two filters with different OAuthConfigurations (different client id) share ONE cache backend.
        // A token validated and cached by filter A must never satisfy filter B's cache lookup, or B would
        // skip its own validation. Mutation: key the cache on the raw token alone and this goes red.
        final TokenCache shared = new CaffeineTokenCache();

        final TokenValidator validatorA = mock(TokenValidator.class);
        when(validatorA.validate(RAW_TOKEN)).thenReturn(validToken(NOW.plusSeconds(300)));
        final OAuthFilter filterA = new OAuthFilter(validatorA, shared, HASHER, config("client-A", 300L),
                Clock.fixed(NOW, ZoneOffset.UTC));
        filterA.doFilter(bearerRequest(), mock(HttpServletResponse.class), mock(FilterChain.class));

        final TokenValidator validatorB = mock(TokenValidator.class);
        when(validatorB.validate(RAW_TOKEN))
                .thenReturn(new ValidationFailure(ValidationFailure.Reason.AUDIENCE_MISMATCH, "wrong aud"));
        final HttpServletResponse responseB = mock(HttpServletResponse.class);
        final FilterChain chainB = mock(FilterChain.class);
        final OAuthFilter filterB = new OAuthFilter(validatorB, shared, HASHER, config("client-B", 300L),
                Clock.fixed(NOW, ZoneOffset.UTC));
        filterB.doFilter(bearerRequest(), responseB, chainB);

        verify(validatorB).validate(RAW_TOKEN);
        verify(responseB).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chainB, never()).doFilter(any(), any());
    }

    @Test
    void sharedCacheDoesNotCrossJwksTrustRoots() throws Exception {
        final RSAKey signingKey = rsaKey("same-kid");
        final RSAKey otherRootKey = rsaKey("same-kid");
        try (JwksTestServer firstRoot = new JwksTestServer(publicSet(signingKey));
                JwksTestServer secondRoot = new JwksTestServer(publicSet(otherRootKey)))
        {
            final String jwt = signedToken(signingKey, Instant.now().plusSeconds(300), null);
            final OAuthConfiguration firstConfig = config(firstRoot.uri(), CLIENT_ID, 30L, 300L);
            final OAuthConfiguration secondConfig = config(secondRoot.uri(), CLIENT_ID, 30L, 300L);
            assertRealValidatorsDoNotShare(firstRoot.validator(firstConfig), firstConfig,
                    secondRoot.validator(secondConfig), secondConfig, jwt);
        }
    }

    @Test
    void sharedCacheDoesNotCrossClockSkewPoliciesForFutureNbf() throws Exception {
        // A real signed JWT has nbf 45s ahead. Nimbus accepts under 60s skew but rejects under 0s.
        final RSAKey key = rsaKey("future-nbf");
        try (JwksTestServer root = new JwksTestServer(publicSet(key))) {
            final Instant now = Instant.now();
            final String jwt = signedToken(key, now.plusSeconds(300), now.plusSeconds(45));
            final OAuthConfiguration firstConfig = config(root.uri(), CLIENT_ID, 60L, 300L);
            final OAuthConfiguration secondConfig = config(root.uri(), CLIENT_ID, 0L, 300L);
            assertRealValidatorsDoNotShare(root.validator(firstConfig), firstConfig,
                    root.validator(secondConfig), secondConfig, jwt);
        }
    }

    @Test
    void sharedCacheDoesNotCrossCacheTtlPolicies() throws Exception {
        assertSharedCacheIsolated(
                config(URI.create("https://issuer.example/jwks"), CLIENT_ID, 30L, 300L),
                config(URI.create("https://issuer.example/jwks"), CLIENT_ID, 30L, 5L),
                ValidationFailure.Reason.BAD_SIGNATURE);
    }

    @Test
    void sharedCacheDoesNotCrossShiftedFieldsContainingDelimiter() throws Exception {
        // OAuthConfiguration does not reject NUL in identifiers. Delimiter-only serialization would
        // map these different (issuer, clientId) tuples to the same cache key.
        final URI jwks = URI.create("https://issuer.example/jwks");
        final OAuthConfiguration first = new OAuthConfiguration(jwks, ISSUER + "\0other", CLIENT_ID,
                AUDIENCE_CLAIM, 30L, 300L, TokenKeyHashing.SHA256, null);
        final OAuthConfiguration second = new OAuthConfiguration(jwks, ISSUER, "other\0" + CLIENT_ID,
                AUDIENCE_CLAIM, 30L, 300L, TokenKeyHashing.SHA256, null);
        assertSharedCacheIsolated(first, second, ValidationFailure.Reason.BAD_SIGNATURE);
    }

    private static String signedToken(final RSAKey key, final Instant expiresAt,
            final Instant notBefore) throws Exception
    {
        final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().subject("sub").issuer(ISSUER)
                .audience(AUDIENCE_CLAIM).expirationTime(Date.from(expiresAt));
        if (notBefore != null) {
            claims.notBeforeTime(Date.from(notBefore));
        }
        return signRs256(key, claims.build());
    }

    private static void assertRealValidatorsDoNotShare(final TokenValidator firstValidator,
            final OAuthConfiguration firstConfig, final TokenValidator secondValidator,
            final OAuthConfiguration secondConfig, final String jwt) throws Exception
    {
        final TokenCache shared = new CaffeineTokenCache();
        final FilterChain firstChain = mock(FilterChain.class);
        new OAuthFilter(firstValidator, shared, HASHER, firstConfig).doFilter(
                bearerRequest(jwt), mock(HttpServletResponse.class), firstChain);
        verify(firstChain).doFilter(any(), any());

        final HttpServletResponse secondResponse = mock(HttpServletResponse.class);
        final FilterChain secondChain = mock(FilterChain.class);
        new OAuthFilter(secondValidator, shared, HASHER, secondConfig).doFilter(
                bearerRequest(jwt), secondResponse, secondChain);
        verify(secondResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(secondChain, never()).doFilter(any(), any());
    }

    private static void assertSharedCacheIsolated(final OAuthConfiguration firstConfig,
            final OAuthConfiguration secondConfig, final ValidationFailure.Reason rejection) throws Exception
    {
        final TokenCache shared = new CaffeineTokenCache();
        final TokenValidator firstValidator = mock(TokenValidator.class);
        when(firstValidator.validate(RAW_TOKEN)).thenReturn(validToken(NOW.plusSeconds(300)));
        new OAuthFilter(firstValidator, shared, HASHER, firstConfig, Clock.fixed(NOW, ZoneOffset.UTC))
                .doFilter(bearerRequest(), mock(HttpServletResponse.class), mock(FilterChain.class));

        final TokenValidator secondValidator = mock(TokenValidator.class);
        when(secondValidator.validate(RAW_TOKEN)).thenReturn(new ValidationFailure(rejection, "rejected"));
        final HttpServletResponse secondResponse = mock(HttpServletResponse.class);
        final FilterChain secondChain = mock(FilterChain.class);
        new OAuthFilter(secondValidator, shared, HASHER, secondConfig, Clock.fixed(NOW, ZoneOffset.UTC))
                .doFilter(bearerRequest(), secondResponse, secondChain);

        verify(secondValidator).validate(RAW_TOKEN);
        verify(secondResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verify(secondChain, never()).doFilter(any(), any());
    }

    /**
     * A10-12 regression: a burst of concurrent requests carrying the SAME uncached token must trigger
     * exactly one validation, not one per request. A gated validator holds the first (loader) request
     * inside {@code validate()} while the rest arrive; without single-flight de-duplication every request
     * would be inside {@code validate()} at that point, so the count would be the thread count.
     */
    @Test
    void concurrentRequestsForTheSameUncachedTokenValidateItOnlyOnce() throws Exception {
        final int threads = 8;
        final AtomicInteger validateCount = new AtomicInteger();
        final CountDownLatch enteredValidate = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        // The rendezvous is INSIDE the cache's initial get(), not before filter entry: every worker's
        // first cache MISS blocks here until all `threads` have arrived, so all requests have observed
        // the miss before any validation can complete. This is the determinism reviewer F1-F3 asked for
        // -- it removes the schedule where a leader validates and caches before a follower even calls
        // get(), which let the old barrier-before-filter test pass without single-flight. The owner's
        // re-check get() (invocation number > threads) does not wait.
        final CyclicBarrier allMissed = new CyclicBarrier(threads);
        final AtomicInteger gets = new AtomicInteger();
        final CaffeineTokenCache backing = new CaffeineTokenCache();
        final TokenCache barrierCache = new TokenCache() {
            @Override
            @SuppressWarnings("checkstyle:IllegalCatch")
            public Optional<ValidatedToken> get(final String tokenKey) {
                final Optional<ValidatedToken> observed = backing.get(tokenKey);
                if (gets.getAndIncrement() < threads) {
                    try {
                        allMissed.await(5, TimeUnit.SECONDS);
                    } catch (final Exception exception) {
                        throw new RuntimeException(exception);
                    }
                }
                return observed;
            }

            @Override
            public void put(final String tokenKey, final ValidatedToken token, final Duration ttl) {
                backing.put(tokenKey, token, ttl);
            }
        };

        final TokenValidator gatedValidator = mock(TokenValidator.class);
        when(gatedValidator.validate(RAW_TOKEN)).thenAnswer(invocation -> {
            validateCount.incrementAndGet();
            enteredValidate.countDown();
            release.await(5, TimeUnit.SECONDS);
            return validToken(NOW.plusSeconds(300));
        });

        final OAuthFilter oauthFilter = new OAuthFilter(gatedValidator, barrierCache, HASHER,
                config(CLIENT_ID, 300L), Clock.fixed(NOW, ZoneOffset.UTC));

        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final List<Future<?>> results = new ArrayList<>();
        final AtomicInteger forwarded = new AtomicInteger();
        try {
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    final FilterChain requestChain = mock(FilterChain.class);
                    doAnswer(invocation -> {
                        forwarded.incrementAndGet();
                        return null;
                    }).when(requestChain).doFilter(any(), any());
                    oauthFilter.doFilter(bearerRequest(), mock(HttpServletResponse.class), requestChain);
                    return null;
                }));
            }

            // All workers are past the shared miss (the barrier). The single owner is held inside
            // validate() while the rest wait on its in-flight result; releasing it lets them all complete.
            assertTrue(enteredValidate.await(5, TimeUnit.SECONDS), "no request reached validation");
            release.countDown();

            for (Future<?> result : results) {
                result.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, validateCount.get(),
                "a burst of the same uncached token, all past the cache miss, must be validated exactly once");
        assertEquals(threads, forwarded.get(), "every request must continue after shared validation succeeds");
    }

    /**
     * A10-12 / reviewer F1-F2 regression: the single-flight owner must re-check the cache after winning
     * {@code putIfAbsent}. A request can pass the initial cache miss, be descheduled, and only reach
     * {@code putIfAbsent} after an earlier leader validated, cached the token and removed its in-flight
     * entry. Without a re-check the late owner validates again. Modelled deterministically with a staged
     * cache: empty on the pre-check, populated on the owner's re-check -- the validator must never run.
     */
    @Test
    void theSingleFlightOwnerReChecksTheCacheBeforeValidating() throws Exception {
        final TokenCache stagedCache = mock(TokenCache.class);
        final ValidatedToken alreadyCached = validToken(NOW.plusSeconds(300));
        when(stagedCache.get(anyString())).thenReturn(Optional.empty(), Optional.of(alreadyCached));
        final TokenValidator neverCalled = mock(TokenValidator.class);

        final OAuthFilter oauthFilter = new OAuthFilter(neverCalled, stagedCache, HASHER,
                config(CLIENT_ID, 300L), Clock.fixed(NOW, ZoneOffset.UTC));
        oauthFilter.doFilter(bearerRequest(), response, chain);

        verify(neverCalled, never()).validate(anyString());
        verify(chain).doFilter(any(), any());
    }

    private static HttpServletRequest bearerRequest() {
        return bearerRequest(RAW_TOKEN);
    }

    private static HttpServletRequest bearerRequest(final String rawToken) {
        final HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn("Bearer " + rawToken);
        return req;
    }
}
