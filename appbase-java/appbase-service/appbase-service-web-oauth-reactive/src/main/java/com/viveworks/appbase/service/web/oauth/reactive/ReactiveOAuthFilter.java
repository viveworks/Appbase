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

import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.web.oauth.OAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.OAuthFilter;
import com.viveworks.appbase.service.web.oauth.TokenCache;
import com.viveworks.appbase.service.web.oauth.TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenValidator;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * The non-blocking twin of the servlet {@code OAuthFilter}: a {@code WebFilter}-shaped filter that runs
 * the identical spec §4.4 lifecycle over the same {@link TokenValidator} / {@link TokenCache} SPIs, so
 * {@code appbase-bootstrap-reactor-netty} is not shipped auth-less (spec §4.5, decision D6). Only the
 * filter shell differs from the servlet path; the validation core is shared and unchanged.
 *
 * <p>Lifecycle: no bearer token → 401; a {@link TokenCache} hit is served without re-validation; a
 * {@code ValidationFailure} → 401 with the chain untouched; a fresh {@link ValidatedToken} is cached for
 * {@code min(cacheTtl, exp − now)}, attached to the exchange, and the chain continues. The cache key is
 * bound to this filter's {@link OAuthConfiguration} validation and cache policy (JWKS trust root,
 * issuer, client id, audience, clock-skew margin and cache TTL), so a token cached under a different
 * policy cannot satisfy this filter's lookup on a shared backend.
 *
 * <p>The blocking SPI calls ({@code validate}, cache {@code get}/{@code put}) run on
 * {@link Schedulers#boundedElastic()}, off the reactor event loop; that non-blocking property is proved
 * with BlockHound in S02.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class ReactiveOAuthFilter {

    private static final int HTTP_UNAUTHORIZED = 401;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    // A NUL byte cannot appear in a JWT (base64url + '.'); reject it as invalid token input.
    // Config fields may contain NUL, so cache-key fields use length prefixes. Parity with OAuthFilter.
    private static final char SEPARATOR = Character.MIN_VALUE;

    // How long a request waits on another request's in-flight validation of the SAME token before
    // validating independently. Bounds the single-flight coupling. Parity with OAuthFilter.
    private static final Duration SINGLE_FLIGHT_WAIT = Duration.ofSeconds(10);

    private final TokenValidator validator;
    private final TokenCache cache;
    private final TokenKeyHasher hasher;
    private final Duration cacheTtl;
    private final String cacheNamespace;
    private final Clock clock;

    // Single-flight de-duplication of concurrent validations of the same uncached token, so a burst of
    // one token issues one provider call rather than one per request. Parity with OAuthFilter; the work
    // here runs on boundedElastic, so the same blocking coordination applies.
    private final ConcurrentMap<String, CompletableFuture<Optional<ValidatedToken>>> inFlightValidations =
            new ConcurrentHashMap<>();

    /**
     * Creates a reactive OAuth filter with the system UTC clock.
     *
     * @param validator     validates a raw token on a cache miss
     * @param cache         stores validated tokens between requests
     * @param hasher        derives the cache key from the raw token
     * @param configuration the OAuth configuration; supplies the cache TTL and the identity this filter
     *                      binds its cache keys to
     */
    public ReactiveOAuthFilter(final TokenValidator validator, final TokenCache cache, final TokenKeyHasher hasher,
            final OAuthConfiguration configuration)
    {
        this(validator, cache, hasher, configuration, Clock.systemUTC());
    }

    ReactiveOAuthFilter(final TokenValidator validator, final TokenCache cache, final TokenKeyHasher hasher,
            final OAuthConfiguration configuration, final Clock clock)
    {
        this.validator = validator;
        this.cache = cache;
        this.hasher = hasher;
        this.cacheTtl = Duration.ofSeconds(configuration.cacheTtlSeconds());
        this.cacheNamespace = cacheNamespace(configuration);
        this.clock = clock;
    }

    /**
     * Authenticate the exchange by validating its bearer token, then either continue the chain or set a
     * 401. Mirrors {@code OAuthFilter.doFilter} over the reactive request/response. The blocking
     * validation and cache work is offloaded to {@link Schedulers#boundedElastic()} so it never runs on
     * the event loop.
     *
     * @param exchange the request/response exchange
     * @param chain    the continuation for an authenticated request
     * @return a {@link Mono} that completes when the request has been handled or rejected
     */
    public Mono<Void> filter(final AppbaseServiceExchange exchange, final ReactiveWebFilterChain chain) {
        final String rawToken = bearerToken(exchange.header(AUTHORIZATION_HEADER));
        if (rawToken == null) {
            exchange.status(HTTP_UNAUTHORIZED);
            return Mono.empty();
        }
        // authenticate() returns an Optional (never null), so fromCallable always emits an onNext the
        // flatMap can branch on. Do NOT reduce this to a Mono<Void> pipeline + switchIfEmpty for the
        // reject: a Mono<Void> completes with no onNext even on success, so switchIfEmpty would fire the
        // 401 after every authenticated request too.
        return Mono.fromCallable(() -> authenticate(rawToken))
                .subscribeOn(Schedulers.boundedElastic())
                // Fail CLOSED: a validator/cache exception (e.g. the Nimbus engine on adversarial claims)
                // must reject with 401, not propagate as an unhandled error — the servlet twin gets a
                // container safety net for free, the reactive path has none. Placed BEFORE flatMap and
                // scoped to the authentication step, so a genuine downstream chain error is NOT masked.
                .onErrorResume(error -> {
                    exchange.status(HTTP_UNAUTHORIZED);
                    return Mono.empty();
                })
                .flatMap(result -> {
                    if (result.isPresent()) {
                        exchange.attributes().put(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE, result.get());
                        return chain.filter(exchange);
                    }
                    exchange.status(HTTP_UNAUTHORIZED);
                    return Mono.<Void>empty();
                });
    }

    /**
     * Resolve the raw token to a present {@link ValidatedToken}, or an empty {@link Optional} to reject.
     * A cache hit is served without re-validation; a cache miss validates and caches a fresh token for
     * {@code min(cacheTtl, exp − now)} when that is positive. Blocking — invoked only off the event loop.
     */
    private Optional<ValidatedToken> authenticate(final String rawToken) {
        final String tokenKey = cacheKey(rawToken);
        final Optional<ValidatedToken> cached = cache.get(tokenKey);
        if (cached.isPresent()) {
            return cached;
        }
        return validateSingleFlight(rawToken, tokenKey);
    }

    /**
     * Validates an uncached token at most once across concurrent requests carrying it: the first request
     * validates (and caches), any other request for the SAME token that arrives while that validation is
     * in flight waits for its result rather than issuing its own remote call. Parity with {@code
     * OAuthFilter#validateSingleFlight}. Bounded and self-healing: a waiter that times out (or observes a
     * failed/interrupted in-flight validation) validates independently, and the in-flight entry is always
     * removed on completion so a failure never poisons later requests.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private Optional<ValidatedToken> validateSingleFlight(final String rawToken, final String tokenKey) {
        final CompletableFuture<Optional<ValidatedToken>> mine = new CompletableFuture<>();
        final CompletableFuture<Optional<ValidatedToken>> inFlight =
                inFlightValidations.putIfAbsent(tokenKey, mine);
        if (inFlight == null) {
            try {
                // Re-check the cache after acquiring ownership -- a prior leader may have validated and
                // cached this token, then removed its in-flight entry, in the window between our cache
                // miss and this putIfAbsent. Parity with OAuthFilter (reviewer F1/F2).
                final Optional<ValidatedToken> cached = cache.get(tokenKey);
                final Optional<ValidatedToken> result = cached.isPresent() ?
                        cached :
                        validateAndCache(rawToken, tokenKey);
                mine.complete(result);
                return result;
            } catch (final RuntimeException exception) {
                mine.completeExceptionally(exception);
                throw exception;
            } finally {
                inFlightValidations.remove(tokenKey, mine);
            }
        }

        try {
            return inFlight.get(SINGLE_FLIGHT_WAIT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            return validateAndCache(rawToken, tokenKey);
        } catch (final ExecutionException | TimeoutException exception) {
            return validateAndCache(rawToken, tokenKey);
        }
    }

    private Optional<ValidatedToken> validateAndCache(final String rawToken, final String tokenKey) {
        if (validator.validate(rawToken) instanceof ValidatedToken validated) {
            final Duration ttl = cacheEntryTtl(validated);
            // Skip the cache for a token at or past its expiry (possible within the clock-skew margin):
            // a non-positive ttl is pointless to store and is rejected outright by some backends.
            if (ttl.compareTo(Duration.ZERO) > 0) {
                cache.put(tokenKey, validated, ttl);
            }
            return Optional.of(validated);
        }
        return Optional.empty();
    }

    /**
     * The cache key: a hash over this filter's identity namespace and the raw token. Binding the
     * namespace in means a token cached by a differently-configured filter can never satisfy this
     * filter's cache lookup, even on a shared backend.
     */
    private String cacheKey(final String rawToken) {
        return hasher.hash(cacheNamespace + keyPart(rawToken));
    }

    private Duration cacheEntryTtl(final ValidatedToken token) {
        final Duration untilExpiry = Duration.between(clock.instant(), token.expiresAt());
        return untilExpiry.compareTo(cacheTtl) < 0 ? untilExpiry : cacheTtl;
    }

    private static String cacheNamespace(final OAuthConfiguration configuration) {
        return keyPart(configuration.jwksUri().toString()) + keyPart(configuration.issuer()) +
                keyPart(configuration.clientId()) + keyPart(configuration.audienceClaim()) +
                keyPart(Long.toString(configuration.tokenExpiryMarginSeconds())) +
                keyPart(Long.toString(configuration.cacheTtlSeconds()));
    }

    private static String keyPart(final String value) {
        final String nonNull = Objects.requireNonNull(value, "OAuth cache-key field must not be null");
        return nonNull.length() + ":" + nonNull;
    }

    /**
     * Extract the bearer token from an {@code Authorization} header, or {@code null} if the header is
     * absent, not the {@code Bearer} scheme, empty, or carries a NUL {@link #SEPARATOR}. Parity
     * with {@code OAuthFilter.bearerToken}.
     */
    private static String bearerToken(final String authorizationHeader) {
        if (authorizationHeader == null ||
                authorizationHeader.length() <= BEARER_PREFIX.length() ||
                !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length()))
        {
            return null;
        }
        final String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty() || token.indexOf(SEPARATOR) >= 0) {
            return null;
        }
        return token;
    }
}
