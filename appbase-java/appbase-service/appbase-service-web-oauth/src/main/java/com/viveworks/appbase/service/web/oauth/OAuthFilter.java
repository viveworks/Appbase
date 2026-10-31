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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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

/**
 * A container-neutral {@link jakarta.servlet.Filter} that authenticates requests by validating their
 * bearer token, per spec §4.4. One implementation runs identically on Jetty, Undertow and Tomcat; a
 * reactive {@code WebFilter} twin performs the same lifecycle over the same SPIs.
 *
 * <p>Lifecycle: no bearer token → 401; a {@link TokenCache} hit is served without re-validation; a
 * {@link ValidationFailure} → 401 with the chain untouched; a fresh {@link ValidatedToken} is cached for
 * {@code min(cacheTtl, exp − now)}, attached to the request, and the chain continues.
 *
 * <p>The cache key is bound to this filter's own {@link OAuthConfiguration} validation and cache policy
 * (JWKS trust root, issuer, client id, audience, clock-skew margin and cache TTL), not to the raw token
 * alone. Two differently-configured filters therefore never collide on a shared {@link TokenCache}
 * backend, so a token validated under one policy cannot bypass another filter's validation.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class OAuthFilter implements jakarta.servlet.Filter {

    /** Request attribute under which a {@link ValidatedToken} is exposed to downstream handlers. */
    public static final String VALIDATED_TOKEN_ATTRIBUTE =
            "com.viveworks.appbase.service.web.oauth.validatedToken";

    private static final String BEARER_PREFIX = "Bearer ";
    // A NUL byte cannot appear in a JWT (base64url + '.'); reject it as invalid token input.
    // Config fields may contain NUL, so cache-key fields use length prefixes instead of this separator.
    private static final char SEPARATOR = Character.MIN_VALUE;

    // How long a request will wait on another request's in-flight validation of the SAME token before
    // giving up and validating independently. Bounds the single-flight coupling so a slow validation can
    // never make an unrelated request hang indefinitely on it.
    private static final Duration SINGLE_FLIGHT_WAIT = Duration.ofSeconds(10);

    private final TokenValidator validator;
    private final TokenCache cache;
    private final TokenKeyHasher hasher;
    private final Duration cacheTtl;
    private final String cacheNamespace;
    private final Clock clock;

    // Single-flight de-duplication of concurrent validations of the same uncached token. Keyed by the
    // namespaced cache key; each entry lives only for the duration of one validation and is removed on
    // completion, so the map holds at most the set of tokens being validated concurrently right now.
    private final ConcurrentMap<String, CompletableFuture<Optional<ValidatedToken>>> inFlightValidations =
            new ConcurrentHashMap<>();

    /**
     * Creates an OAuth filter with the system UTC clock.
     *
     * @param validator     validates a raw token on a cache miss
     * @param cache         stores validated tokens between requests
     * @param hasher        derives the cache key from the raw token
     * @param configuration the OAuth configuration; supplies the cache TTL and the identity this filter
     *                      binds its cache keys to
     */
    public OAuthFilter(final TokenValidator validator, final TokenCache cache, final TokenKeyHasher hasher,
            final OAuthConfiguration configuration)
    {
        this(validator, cache, hasher, configuration, Clock.systemUTC());
    }

    OAuthFilter(final TokenValidator validator, final TokenCache cache, final TokenKeyHasher hasher,
            final OAuthConfiguration configuration, final Clock clock)
    {
        this.validator = validator;
        this.cache = cache;
        this.hasher = hasher;
        this.cacheTtl = Duration.ofSeconds(configuration.cacheTtlSeconds());
        this.cacheNamespace = cacheNamespace(configuration);
        this.clock = clock;
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
            final FilterChain chain) throws IOException, ServletException
    {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        final String rawToken = bearerToken(request.getHeader("Authorization"));
        if (rawToken == null) {
            unauthorized(response);
            return;
        }

        final String tokenKey = cacheKey(rawToken);
        final Optional<ValidatedToken> cached = cache.get(tokenKey);
        if (cached.isPresent()) {
            forward(request, response, chain, cached.get());
            return;
        }

        final Optional<ValidatedToken> validated = validateSingleFlight(rawToken, tokenKey);
        if (validated.isPresent()) {
            forward(request, response, chain, validated.get());
        } else {
            unauthorized(response);
        }
    }

    /**
     * Validates an uncached token at most once across concurrent requests that carry it. The first
     * request to arrive performs the validation (and caches the result); any other request for the SAME
     * token that arrives while that validation is in flight waits for its result instead of issuing its
     * own remote call — the fix for the cold-cache stampede that multiplied provider calls and consumed
     * request threads under a burst of the same token.
     *
     * <p>The coupling is bounded and self-healing: a waiter gives up after {@link #SINGLE_FLIGHT_WAIT}
     * (or if the in-flight validation fails or is interrupted) and validates independently, so a slow or
     * failed validation degrades to the old one-call-per-request behaviour rather than hanging or failing
     * an unrelated request. The in-flight entry is always removed on completion, so a failure never
     * poisons later requests.
     *
     * @param rawToken the bearer token to validate
     * @param tokenKey the namespaced cache key for {@code rawToken}
     * @return the validated token, or empty when validation failed
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private Optional<ValidatedToken> validateSingleFlight(final String rawToken, final String tokenKey) {
        final CompletableFuture<Optional<ValidatedToken>> mine = new CompletableFuture<>();
        final CompletableFuture<Optional<ValidatedToken>> inFlight =
                inFlightValidations.putIfAbsent(tokenKey, mine);
        if (inFlight == null) {
            try {
                // Re-check the cache after acquiring ownership. A request can pass the initial cache miss,
                // be descheduled, and only reach putIfAbsent after an earlier leader validated, cached the
                // token and removed its in-flight entry -- without this re-check the late owner would
                // validate the same token again (reviewer F1/F2).
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
            // The in-flight validation failed or is taking too long: fall back to validating this
            // request independently rather than blocking on, or failing with, another request.
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

    private void forward(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain chain, final ValidatedToken token) throws IOException, ServletException
    {
        request.setAttribute(VALIDATED_TOKEN_ATTRIBUTE, token);
        chain.doFilter(request, response);
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

    private static String bearerToken(final String authorizationHeader) {
        if (authorizationHeader == null ||
                authorizationHeader.length() <= BEARER_PREFIX.length() ||
                !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length()))
        {
            return null;
        }
        final String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        // Reject a NUL-bearing token rather than relying on the servlet container to strip control bytes.
        if (token.isEmpty() || token.indexOf(SEPARATOR) >= 0) {
            return null;
        }
        return token;
    }

    private static void unauthorized(final HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
