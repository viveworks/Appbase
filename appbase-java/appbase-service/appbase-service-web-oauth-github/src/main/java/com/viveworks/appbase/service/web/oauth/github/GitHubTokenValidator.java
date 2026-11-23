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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.service.web.oauth.CaffeineTokenCache;
import com.viveworks.appbase.service.web.oauth.Sha256TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenCache;
import com.viveworks.appbase.service.web.oauth.TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.TokenValidator;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import com.viveworks.appbase.service.web.oauth.ValidationFailure;
import com.viveworks.appbase.service.web.oauth.ValidationResult;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The GitHub {@link TokenValidator}: validates an opaque access token by an authenticated call to the
 * provider's {@code /user} endpoint (spec §5 GitHub, §6 N07 row).
 *
 * <p>GitHub is not OIDC — opaque tokens, no {@code id_token}, no JWKS — so the token cannot be verified
 * locally and this cannot reuse the Nimbus engine. A valid {@code /user} response is cached for the
 * operator-configured {@code cacheTtlSeconds}: an opaque token can be revoked upstream at any moment, and
 * the cache is exactly the window in which a revoked token is still accepted, so the operator sets this
 * short to bound that window. The validator enforces only that it is positive; keeping it short is the
 * operator's responsibility. The network call therefore happens only on a cache miss.
 *
 * <p>Failures are typed for the filter's negative-case matrix. Only an HTTP <b>401</b> — GitHub's
 * "Bad credentials" — is treated as an invalid token: it maps to {@link ValidationFailure.Reason#BAD_SIGNATURE}
 * (the opaque-token analogue of a token that fails provider-side authentication) and is never cached.
 * Every other non-success outcome maps to {@link ValidationFailure.Reason#PROVIDER_UNREACHABLE}, a
 * fail-closed "could not obtain a definitive accept/reject" verdict: a transport failure, an HTTP
 * <b>403</b> (which on GitHub means rate-limit exhaustion, SAML-SSO or IP-allowlist blocks — not a bad
 * token), any other unexpected status, or a 200 whose body is not a recognisable user object. This
 * deliberately avoids reporting a currently-valid token as invalid when the real cause is transient.
 *
 * <p><b>Identity, not audience.</b> GitHub's {@code /user} authenticates the user, not the OAuth app —
 * it returns 200 for any currently-valid token, whatever app or PAT minted it — so this validator cannot
 * confirm the token was issued to {@code clientId}. It therefore establishes the {@code subject} (the
 * verified GitHub user id) and asserts <em>no</em> audience: the returned {@link ValidatedToken} carries
 * an empty audience set, and a caller must not treat {@link ValidatedToken#audience()} as a
 * provider-verified app binding for GitHub. (Provider-verified app binding would require GitHub's
 * client-authenticated token endpoint and the app's client secret; that is out of this validator's scope.)
 *
 * <p>The cache key is bound to a fixed type tag plus this validator's own identity ({@code issuer},
 * {@code clientId}, {@code userApiUri}) before hashing, mirroring {@code OAuthFilter}, so two
 * differently-configured validators — or a differently-typed module — sharing a {@link TokenCache}
 * backend never collide, and a raw token carrying the separator is rejected rather than shifting the key
 * boundary. This cache key is distinct from the namespace-free {@link ValidatedToken#tokenKey()} the SPI
 * contract documents.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class GitHubTokenValidator implements TokenValidator {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** NUL separates namespace components so no concatenation of them can be forged from token bytes. */
    private static final char SEPARATOR = '\u0000';
    /**
     * Prefixes the cache-key pre-image so this module's keys can never collide with another module's on a
     * shared {@link TokenCache} backend, even if their namespace components happen to match byte-for-byte
     * (e.g. an OIDC filter whose {@code audienceClaim} equals this validator's {@code userApiUri}).
     */
    private static final String NAMESPACE_TYPE_TAG = "github-v1";

    private final TokenKeyHasher hasher;
    private final TokenCache cache;
    private final GitHubUserApi userApi;
    private final Clock clock;
    private final String issuer;
    private final Duration cacheTtl;
    private final String cacheNamespace;

    /**
     * Wires the production dependencies: a SHA-256 cache-key hasher, an in-memory {@link CaffeineTokenCache}
     * private to this validator, and the JDK-based {@link JdkGitHubUserApi}.
     *
     * @param configuration the GitHub configuration; supplies the {@code /user} endpoint and the short TTL
     */
    public GitHubTokenValidator(GitHubOAuthConfiguration configuration) {
        this(configuration, new Sha256TokenKeyHasher(), new CaffeineTokenCache(),
                new JdkGitHubUserApi(configuration.userApiUri(), Duration.ofSeconds(10)), Clock.systemUTC());
    }

    /**
     * @param configuration the GitHub configuration
     * @param hasher        derives the cache key from the raw token; never the raw token itself
     * @param cache         the validated-token cache, private to this validator and short-lived
     * @param userApi       the {@code /user} transport (stubbed in tests)
     * @param clock         the time source, so the synthesized expiry and TTL are testable
     */
    GitHubTokenValidator(GitHubOAuthConfiguration configuration, TokenKeyHasher hasher, TokenCache cache,
            GitHubUserApi userApi, Clock clock) {
        requireHttps(configuration.userApiUri());
        requirePositiveTtl(configuration.cacheTtlSeconds());
        this.hasher = hasher;
        this.cache = cache;
        this.userApi = userApi;
        this.clock = clock;
        this.issuer = configuration.issuer();
        this.cacheTtl = Duration.ofSeconds(configuration.cacheTtlSeconds());
        this.cacheNamespace = NAMESPACE_TYPE_TAG + SEPARATOR + configuration.issuer() + SEPARATOR
                + configuration.clientId() + SEPARATOR + configuration.userApiUri();
    }

    private static void requireHttps(URI userApiUri) {
        if (!"https".equalsIgnoreCase(userApiUri.getScheme())) {
            throw new IllegalArgumentException(
                    "userApiUri must use HTTPS so the bearer token is not sent in clear text: " + userApiUri);
        }
    }

    private static void requirePositiveTtl(long cacheTtlSeconds) {
        if (cacheTtlSeconds <= 0) {
            throw new IllegalArgumentException(
                    "cacheTtlSeconds must be positive; a revoked token is served for at most this long: "
                            + cacheTtlSeconds);
        }
    }

    @Override
    public ValidationResult validate(String rawToken) {
        if (rawToken == null || rawToken.isEmpty() || rawToken.indexOf(SEPARATOR) >= 0) {
            // The cache key is hash(namespace ‖ SEPARATOR ‖ token); a token carrying the SEPARATOR could
            // shift that boundary. Reject it here rather than trusting every caller of the SPI to strip it.
            return new ValidationFailure(ValidationFailure.Reason.MALFORMED, "token is empty or malformed");
        }
        String tokenKey = hasher.hash(rawToken);                                 // namespace-free SPI field
        String cacheKey = hasher.hash(cacheNamespace + SEPARATOR + rawToken);    // namespace-bound cache key
        Optional<ValidatedToken> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        GitHubUserApi.Response response;
        try {
            response = userApi.fetch(rawToken);
        } catch (GitHubUserApiException e) {
            return new ValidationFailure(ValidationFailure.Reason.PROVIDER_UNREACHABLE,
                    "GitHub /user endpoint unreachable");
        }

        int status = response.statusCode();
        if (status == 401) {
            // GitHub's "Bad credentials" — the only status that means the token itself is invalid.
            return new ValidationFailure(ValidationFailure.Reason.BAD_SIGNATURE, "GitHub rejected the token");
        }
        if (status != 200) {
            // 403 (rate-limit / SAML-SSO / IP-allowlist), 5xx, or any other status: no definitive verdict,
            // so fail closed without declaring the token invalid.
            return new ValidationFailure(ValidationFailure.Reason.PROVIDER_UNREACHABLE,
                    "GitHub /user returned HTTP " + status);
        }

        Map<String, Object> user = parseUser(response.body());
        // GitHub always returns a numeric account id. Require a Number so a hostile or misbehaving endpoint
        // cannot supply a crafted string or object as the subject (which downstream code may trust as a
        // clean numeric id). A non-Number is not a recognisable user object.
        if (user == null || !(user.get("id") instanceof Number)) {
            return new ValidationFailure(ValidationFailure.Reason.PROVIDER_UNREACHABLE,
                    "GitHub /user response was not a recognisable user object");
        }

        Instant expiresAt = clock.instant().plus(cacheTtl);
        // No audience is asserted: GitHub's /user authenticates the user, not the app, so this validator
        // cannot confirm the token was issued to clientId. It establishes identity (subject), not audience;
        // a caller must not treat audience() as a provider-verified app binding for GitHub.
        ValidatedToken token = new ValidatedToken(
                String.valueOf(user.get("id")), issuer, Set.of(), expiresAt, user, tokenKey);
        cache.put(cacheKey, token, cacheTtl);
        return token;
    }

    private static Map<String, Object> parseUser(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = JSON.readValue(body, Map.class);
            return parsed;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return null;
        }
    }
}
