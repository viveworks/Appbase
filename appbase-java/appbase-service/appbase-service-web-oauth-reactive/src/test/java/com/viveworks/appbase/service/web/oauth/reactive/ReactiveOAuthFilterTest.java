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

package com.viveworks.appbase.service.web.oauth.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.web.oauth.CaffeineTokenCache;
import com.viveworks.appbase.service.web.oauth.OAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.OAuthFilter;
import com.viveworks.appbase.service.web.oauth.Sha256TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenCache;
import com.viveworks.appbase.service.web.oauth.TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenKeyHashing;
import com.viveworks.appbase.service.web.oauth.TokenValidator;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import com.viveworks.appbase.service.web.oauth.ValidationFailure;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Parity suite for the reactive {@code WebFilter} twin: the same accept/reject cases as the servlet
 * {@code OAuthFilter}, asserted over the reactive request/response ({@link AppbaseServiceExchange}) and a
 * {@link ReactiveWebFilterChain}. The non-blocking guarantee (no blocking call on the event loop) is
 * proved separately in S02.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ReactiveOAuthFilterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String RAW_TOKEN = "eyJhbGciOiJSUzI1NiJ9.payload.signature";
    private static final TokenKeyHasher HASHER = new Sha256TokenKeyHasher();
    private static final String ISSUER = "https://issuer.example";
    private static final String CLIENT_ID = "client-1";
    private static final String AUDIENCE_CLAIM = "aud";

    private final TokenValidator validator = mock(TokenValidator.class);
    private final TokenCache cache = mock(TokenCache.class);
    private final AppbaseServiceExchange exchange = mock(AppbaseServiceExchange.class);
    private final ReactiveWebFilterChain chain = mock(ReactiveWebFilterChain.class);

    private static OAuthConfiguration config(final String clientId, final long cacheTtlSeconds) {
        return config(URI.create("https://issuer.example/jwks"), clientId, 30L, cacheTtlSeconds);
    }

    private static OAuthConfiguration config(final URI jwksUri, final String clientId,
            final long marginSeconds, final long cacheTtlSeconds)
    {
        return new OAuthConfiguration(jwksUri, ISSUER, clientId, AUDIENCE_CLAIM,
                marginSeconds, cacheTtlSeconds, TokenKeyHashing.SHA256, null);
    }

    private ReactiveOAuthFilter filter(final Duration cacheTtl) {
        return new ReactiveOAuthFilter(validator, cache, HASHER, config(CLIENT_ID, cacheTtl.toSeconds()),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ValidatedToken validToken(final Instant expiresAt) {
        return new ValidatedToken("sub", ISSUER, Set.of("api"), expiresAt, Map.of(), "unused-in-n01");
    }

    private void authorization(final String headerValue) {
        when(exchange.header("Authorization")).thenReturn(headerValue);
    }

    /**
     * A10-12 / reviewer F1-F2 regression (reactive twin): the single-flight owner must re-check the
     * cache after winning {@code putIfAbsent}. Modelled deterministically with a staged cache: empty on
     * the pre-check, populated on the owner's re-check -- the validator must never run and the request is
     * served from the cache.
     */
    @Test
    void theSingleFlightOwnerReChecksTheCacheBeforeValidating() {
        authorization("Bearer " + RAW_TOKEN);
        final TokenCache stagedCache = mock(TokenCache.class);
        final ValidatedToken alreadyCached = validToken(NOW.plusSeconds(300));
        when(stagedCache.get(anyString())).thenReturn(Optional.empty(), Optional.of(alreadyCached));
        final TokenValidator neverCalled = mock(TokenValidator.class);
        final Map<String, Object> attributes = new HashMap<>();
        when(exchange.attributes()).thenReturn(attributes);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        final ReactiveOAuthFilter oauthFilter = new ReactiveOAuthFilter(neverCalled, stagedCache, HASHER,
                config(CLIENT_ID, 300L), Clock.fixed(NOW, ZoneOffset.UTC));

        StepVerifier.create(oauthFilter.filter(exchange, chain)).verifyComplete();

        verify(neverCalled, never()).validate(anyString());
        verify(chain).filter(exchange);
        assertEquals(alreadyCached, attributes.get(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE));
    }

    /**
     * A10-12 regression (reactive twin): a burst of concurrent exchanges carrying the SAME uncached token
     * must trigger exactly one validation. A gated validator holds the first (loader) request inside
     * {@code validate()} while the rest arrive; without single-flight de-duplication every request would
     * be inside {@code validate()} at that point.
     */
    @Test
    void concurrentRequestsForTheSameUncachedTokenValidateItOnlyOnce() throws Exception {
        final int threads = 8;
        final AtomicInteger validateCount = new AtomicInteger();
        final CountDownLatch enteredValidate = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        // Rendezvous INSIDE the initial cache miss (not before filter entry): every worker's first get()
        // blocks until all `threads` have arrived, so all requests have observed the miss before any
        // validation can complete. Determinism reviewer F1-F3 asked for -- without it a leader could
        // validate+cache before a follower called get(), passing the test without single-flight. The
        // owner's re-check get() (invocation > threads) does not wait.
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

        final ReactiveOAuthFilter oauthFilter = new ReactiveOAuthFilter(gatedValidator, barrierCache, HASHER,
                config(CLIENT_ID, 300L), Clock.fixed(NOW, ZoneOffset.UTC));

        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final List<Future<?>> results = new ArrayList<>();
        final AtomicInteger forwarded = new AtomicInteger();
        try {
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    final AppbaseServiceExchange ex = mock(AppbaseServiceExchange.class);
                    when(ex.header("Authorization")).thenReturn("Bearer " + RAW_TOKEN);
                    when(ex.attributes()).thenReturn(new HashMap<>());
                    final ReactiveWebFilterChain ch = mock(ReactiveWebFilterChain.class);
                    when(ch.filter(ex)).thenAnswer(invocation -> {
                        forwarded.incrementAndGet();
                        return Mono.empty();
                    });
                    oauthFilter.filter(ex, ch).block(Duration.ofSeconds(15));
                    return null;
                }));
            }

            assertTrue(enteredValidate.await(5, TimeUnit.SECONDS), "no request reached validation");
            release.countDown();

            for (Future<?> result : results) {
                result.get(20, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, validateCount.get(),
                "a burst of the same uncached token, all past the cache miss, must be validated exactly once");
        assertEquals(threads, forwarded.get(), "every exchange must continue after shared validation succeeds");
    }

    @Test
    void rejectsRequestWithNoAuthorizationHeader() {
        authorization(null);

        StepVerifier.create(filter(Duration.ofMinutes(5)).filter(exchange, chain)).verifyComplete();

        verify(exchange).status(401);
        verify(chain, never()).filter(any());
        verifyNoInteractions(validator);
    }

    @Test
    void rejectsRequestWhoseSchemeIsNotBearer() {
        authorization("Basic dXNlcjpwYXNz");

        StepVerifier.create(filter(Duration.ofMinutes(5)).filter(exchange, chain)).verifyComplete();

        verify(exchange).status(401);
        verify(chain, never()).filter(any());
        verifyNoInteractions(validator);
    }

    @Test
    void rejectsABearerTokenContainingTheNamespaceSeparator() {
        // Parity with the servlet filter: a real JWT is base64url and never contains a NUL. A token
        // carrying one is rejected outright rather than trusting the transport to strip control bytes.
        authorization("Bearer abc" + Character.MIN_VALUE + "def");

        StepVerifier.create(filter(Duration.ofMinutes(5)).filter(exchange, chain)).verifyComplete();

        verify(exchange).status(401);
        verify(chain, never()).filter(any());
        verifyNoInteractions(validator);
    }

    @Test
    void servesCacheHitWithoutRevalidating() {
        authorization("Bearer " + RAW_TOKEN);
        final ValidatedToken cached = validToken(NOW.plusSeconds(100));
        when(cache.get(anyString())).thenReturn(Optional.of(cached));
        final Map<String, Object> attributes = new HashMap<>();
        when(exchange.attributes()).thenReturn(attributes);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter(Duration.ofMinutes(5)).filter(exchange, chain)).verifyComplete();

        assertEquals(cached, attributes.get(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE));
        verify(chain).filter(exchange);
        // The authenticated path must NEVER touch the response status — a Mono<Void> chain completes
        // "empty" in the signal sense, so a switchIfEmpty-based reject would wrongly fire a 401 here.
        verify(exchange, never()).status(anyInt());
        verifyNoInteractions(validator);
    }

    @Test
    void rejectsInvalidTokenAndDoesNotContinueTheChain() {
        authorization("Bearer " + RAW_TOKEN);
        when(cache.get(anyString())).thenReturn(Optional.empty());
        when(validator.validate(RAW_TOKEN))
                .thenReturn(new ValidationFailure(ValidationFailure.Reason.BAD_SIGNATURE, "bad sig"));

        StepVerifier.create(filter(Duration.ofMinutes(5)).filter(exchange, chain)).verifyComplete();

        verify(exchange).status(401);
        verify(chain, never()).filter(any());
        verify(cache, never()).put(any(), any(), any());
    }

    @Test
    void failsClosedWithUnauthorizedWhenValidationThrows() {
        // A security control must fail CLOSED: if the validator (or cache) throws — plausible for the
        // Nimbus engine on adversarial/malformed claims — the filter must reject with 401, not let the
        // error propagate. The chain must never run. Mutation: drop onErrorResume and this goes red
        // (the Mono terminates with onError and status(401) is never set).
        authorization("Bearer " + RAW_TOKEN);
        when(cache.get(anyString())).thenReturn(Optional.empty());
        when(validator.validate(RAW_TOKEN)).thenThrow(new RuntimeException("validator boom"));

        StepVerifier.create(filter(Duration.ofMinutes(5)).filter(exchange, chain)).verifyComplete();

        verify(exchange).status(401);
        verify(chain, never()).filter(any());
    }

    @Test
    void doesNotMaskADownstreamChainErrorAsUnauthorized() {
        // The fail-closed onErrorResume is scoped to authentication only. A genuine error from the
        // downstream chain (an authenticated request) must propagate, not be swallowed into a 401.
        // Mutation: move onErrorResume after flatMap and this goes red (status(401) gets set instead).
        authorization("Bearer " + RAW_TOKEN);
        when(cache.get(anyString())).thenReturn(Optional.empty());
        when(validator.validate(RAW_TOKEN)).thenReturn(validToken(NOW.plusSeconds(100)));
        when(exchange.attributes()).thenReturn(new HashMap<>());
        when(chain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("downstream boom")));

        StepVerifier.create(filter(Duration.ofMinutes(5)).filter(exchange, chain))
                .verifyErrorMessage("downstream boom");

        verify(exchange, never()).status(401);
    }

    @Test
    void cachesAndForwardsAValidatedTokenOnCacheMiss() {
        authorization("Bearer " + RAW_TOKEN);
        when(cache.get(anyString())).thenReturn(Optional.empty());
        final ValidatedToken validated = validToken(NOW.plusSeconds(100));
        when(validator.validate(RAW_TOKEN)).thenReturn(validated);
        final Map<String, Object> attributes = new HashMap<>();
        when(exchange.attributes()).thenReturn(attributes);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // cacheTtl (300s) is larger than exp-now (100s), so the entry ttl must be the token's own life.
        StepVerifier.create(filter(Duration.ofSeconds(300)).filter(exchange, chain)).verifyComplete();

        final ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(cache).put(key.capture(), eq(validated), ttl.capture());
        assertEquals(Duration.ofSeconds(100), ttl.getValue());
        // The cache key is a hashed digest, never the replayable raw token.
        assertTrue(key.getValue().matches("[0-9a-f]{64}"));
        assertNotEquals(RAW_TOKEN, key.getValue());
        assertEquals(validated, attributes.get(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE));
        verify(chain).filter(exchange);
        verify(exchange, never()).status(anyInt());
    }

    @Test
    void cacheTtlCapsAShortLivedEntry() {
        authorization("Bearer " + RAW_TOKEN);
        when(cache.get(anyString())).thenReturn(Optional.empty());
        final ValidatedToken validated = validToken(NOW.plusSeconds(500));
        when(validator.validate(RAW_TOKEN)).thenReturn(validated);
        when(exchange.attributes()).thenReturn(new HashMap<>());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // exp-now (500s) exceeds cacheTtl (60s), so the entry ttl is capped at cacheTtl.
        StepVerifier.create(filter(Duration.ofSeconds(60)).filter(exchange, chain)).verifyComplete();

        final ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(cache).put(anyString(), eq(validated), ttl.capture());
        assertEquals(Duration.ofSeconds(60), ttl.getValue());
    }

    @Test
    void doesNotCacheATokenAlreadyPastExpiry() {
        authorization("Bearer " + RAW_TOKEN);
        when(cache.get(anyString())).thenReturn(Optional.empty());
        // The validator accepts a token whose exp is already in the past, within the clock-skew margin.
        // exp-now is then negative, so caching it would hand a negative Duration to the backend.
        final ValidatedToken validated = validToken(NOW.minusSeconds(5));
        when(validator.validate(RAW_TOKEN)).thenReturn(validated);
        final Map<String, Object> attributes = new HashMap<>();
        when(exchange.attributes()).thenReturn(attributes);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter(Duration.ofSeconds(300)).filter(exchange, chain)).verifyComplete();

        verify(cache, never()).put(any(), any(), any());
        // The request is still authenticated and forwarded — only the pointless cache write is skipped.
        assertEquals(validated, attributes.get(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE));
        verify(chain).filter(exchange);
        verify(exchange, never()).status(anyInt());
    }

    @Test
    void doesNotServeACachedTokenToADifferentlyConfiguredFilter() {
        // Two filters with different OAuthConfigurations (different client id) share ONE cache backend.
        // A token validated and cached by filter A must never satisfy filter B's cache lookup, or B would
        // skip its own validation. The cache key binds the filter's identity namespace, not the raw token.
        final TokenCache shared = new CaffeineTokenCache();

        final TokenValidator validatorA = mock(TokenValidator.class);
        when(validatorA.validate(RAW_TOKEN)).thenReturn(validToken(NOW.plusSeconds(300)));
        final ReactiveOAuthFilter filterA = new ReactiveOAuthFilter(validatorA, shared, HASHER,
                config("client-A", 300L), Clock.fixed(NOW, ZoneOffset.UTC));
        StepVerifier.create(filterA.filter(bearerExchange(), okChain())).verifyComplete();

        final TokenValidator validatorB = mock(TokenValidator.class);
        when(validatorB.validate(RAW_TOKEN))
                .thenReturn(new ValidationFailure(ValidationFailure.Reason.AUDIENCE_MISMATCH, "wrong aud"));
        final AppbaseServiceExchange exchangeB = bearerExchange();
        final ReactiveOAuthFilter filterB = new ReactiveOAuthFilter(validatorB, shared, HASHER,
                config("client-B", 300L), Clock.fixed(NOW, ZoneOffset.UTC));
        StepVerifier.create(filterB.filter(exchangeB, okChain())).verifyComplete();

        verify(validatorB).validate(RAW_TOKEN);
        verify(exchangeB).status(401);
    }

    @Test
    void sharedCacheDoesNotCrossJwksTrustRoots() {
        assertSharedCacheIsolated(
                config(URI.create("https://issuer.example/keys-a"), CLIENT_ID, 30L, 300L),
                config(URI.create("https://issuer.example/keys-b"), CLIENT_ID, 30L, 300L),
                ValidationFailure.Reason.BAD_SIGNATURE);
    }

    @Test
    void sharedCacheDoesNotCrossClockSkewPoliciesForFutureNbf() {
        // The servlet regression exercises Nimbus with a real signed JWT. This twin isolates the filter:
        // its policy-aware validators evaluate nbf against the configured margin at the same fixed time.
        final Instant notBefore = NOW.plusSeconds(45);
        final TokenValidator tolerant = token -> NOW.isBefore(notBefore.minusSeconds(60)) ?
                new ValidationFailure(ValidationFailure.Reason.EXPIRED, "future nbf") :
                validToken(NOW.plusSeconds(300));
        final TokenValidator strict = token -> NOW.isBefore(notBefore) ?
                new ValidationFailure(ValidationFailure.Reason.EXPIRED, "future nbf") :
                validToken(NOW.plusSeconds(300));
        assertSharedCacheIsolated(
                config(URI.create("https://issuer.example/jwks"), CLIENT_ID, 60L, 300L),
                config(URI.create("https://issuer.example/jwks"), CLIENT_ID, 0L, 300L),
                tolerant, strict);
    }

    @Test
    void sharedCacheDoesNotCrossCacheTtlPolicies() {
        assertSharedCacheIsolated(
                config(URI.create("https://issuer.example/jwks"), CLIENT_ID, 30L, 300L),
                config(URI.create("https://issuer.example/jwks"), CLIENT_ID, 30L, 5L),
                ValidationFailure.Reason.BAD_SIGNATURE);
    }

    @Test
    void sharedCacheDoesNotCrossShiftedFieldsContainingDelimiter() {
        final URI jwks = URI.create("https://issuer.example/jwks");
        final OAuthConfiguration first = new OAuthConfiguration(jwks, ISSUER + "\0other", CLIENT_ID,
                AUDIENCE_CLAIM, 30L, 300L, TokenKeyHashing.SHA256, null);
        final OAuthConfiguration second = new OAuthConfiguration(jwks, ISSUER, "other\0" + CLIENT_ID,
                AUDIENCE_CLAIM, 30L, 300L, TokenKeyHashing.SHA256, null);
        assertSharedCacheIsolated(first, second, ValidationFailure.Reason.BAD_SIGNATURE);
    }

    private static void assertSharedCacheIsolated(final OAuthConfiguration firstConfig,
            final OAuthConfiguration secondConfig, final ValidationFailure.Reason rejection)
    {
        final TokenValidator firstValidator = mock(TokenValidator.class);
        when(firstValidator.validate(RAW_TOKEN)).thenReturn(validToken(NOW.plusSeconds(300)));
        final TokenValidator secondValidator = mock(TokenValidator.class);
        when(secondValidator.validate(RAW_TOKEN)).thenReturn(new ValidationFailure(rejection, "rejected"));
        assertSharedCacheIsolated(firstConfig, secondConfig, firstValidator, secondValidator);
        verify(secondValidator).validate(RAW_TOKEN);
    }

    private static void assertSharedCacheIsolated(final OAuthConfiguration firstConfig,
            final OAuthConfiguration secondConfig, final TokenValidator firstValidator,
            final TokenValidator secondValidator)
    {
        final TokenCache shared = new CaffeineTokenCache();
        final ReactiveOAuthFilter first = new ReactiveOAuthFilter(firstValidator, shared, HASHER,
                firstConfig, Clock.fixed(NOW, ZoneOffset.UTC));
        final ReactiveWebFilterChain firstChain = okChain();
        StepVerifier.create(first.filter(bearerExchange(), firstChain)).verifyComplete();
        verify(firstChain).filter(any());

        final AppbaseServiceExchange secondExchange = bearerExchange();
        final ReactiveWebFilterChain secondChain = okChain();
        final ReactiveOAuthFilter second = new ReactiveOAuthFilter(secondValidator, shared, HASHER,
                secondConfig, Clock.fixed(NOW, ZoneOffset.UTC));
        StepVerifier.create(second.filter(secondExchange, secondChain)).verifyComplete();

        verify(secondExchange).status(401);
        verify(secondChain, never()).filter(any());
    }

    private static AppbaseServiceExchange bearerExchange() {
        final AppbaseServiceExchange ex = mock(AppbaseServiceExchange.class);
        when(ex.header("Authorization")).thenReturn("Bearer " + RAW_TOKEN);
        when(ex.attributes()).thenReturn(new HashMap<>());
        return ex;
    }

    private static ReactiveWebFilterChain okChain() {
        final ReactiveWebFilterChain c = mock(ReactiveWebFilterChain.class);
        when(c.filter(any())).thenReturn(Mono.empty());
        return c;
    }
}
