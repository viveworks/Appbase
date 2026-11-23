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

import com.viveworks.appbase.service.web.oauth.CaffeineTokenCache;
import com.viveworks.appbase.service.web.oauth.OAuthFilter;
import com.viveworks.appbase.service.web.oauth.Sha256TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;

/**
 * Servlet entry point for GitHub opaque-token authentication.
 *
 * <p>GitHub access tokens have no JWT signature or JWKS endpoint, so this filter deliberately does not
 * manufacture an OIDC configuration. It delegates validation and short-lived caching to
 * {@link GitHubTokenValidator}, attaches the same request attribute as {@link OAuthFilter}, and forwards
 * only a provider-authenticated identity.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class GitHubOAuthFilter implements jakarta.servlet.Filter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final char CACHE_NAMESPACE_SEPARATOR = '\u0000';

    private final GitHubTokenValidator validator;

    /** Creates the production filter backed by GitHub's configured {@code /user} endpoint. */
    public GitHubOAuthFilter(GitHubOAuthConfiguration configuration) {
        this.validator = new GitHubTokenValidator(configuration);
    }

    /** Test seam for deterministic offline provider responses; intentionally not public API. */
    GitHubOAuthFilter(GitHubOAuthConfiguration configuration, GitHubUserApi userApi) {
        this.validator = new GitHubTokenValidator(configuration, new Sha256TokenKeyHasher(),
                new CaffeineTokenCache(), userApi, Clock.systemUTC());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String rawToken = bearerToken(request.getHeader("Authorization"));
        if (rawToken == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (validator.validate(rawToken) instanceof ValidatedToken validated) {
            request.setAttribute(OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE, validated);
            chain.doFilter(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private static String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || authorizationHeader.length() <= BEARER_PREFIX.length()
                || !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() || token.indexOf(CACHE_NAMESPACE_SEPARATOR) >= 0 ? null : token;
    }
}
