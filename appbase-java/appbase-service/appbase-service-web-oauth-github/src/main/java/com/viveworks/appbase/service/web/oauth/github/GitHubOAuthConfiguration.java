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

package com.viveworks.appbase.service.web.oauth.github;

import java.net.URI;

/**
 * Configuration for the GitHub opaque-token validator.
 *
 * <p>GitHub does not fit the OIDC {@code OAuthConfiguration}: there is no JWKS endpoint, no {@code iss}
 * or {@code aud} claim to check locally — validation is a call to the {@code /user} endpoint. So this is
 * a small, provider-specific record rather than a reuse of the JWKS-oriented one.
 *
 * <p>{@code cacheTtlSeconds} is deliberately short. A validated {@code /user} result is cached only
 * briefly, because an opaque token can be revoked upstream at any time and the cache is the window during
 * which a revoked token is still served (spec §6, N07 row). It bounds that window; it does not extend it.
 *
 * @param userApiUri      the HTTPS {@code /user} endpoint (default {@code https://api.github.com/user};
 *                        override only for a vetted GitHub Enterprise host). Must use HTTPS so the bearer
 *                        token is never sent in clear text. <b>This endpoint is a fully trusted authority:
 *                        the validated token's subject and claims are taken verbatim from its response.
 *                        Never derive it from tenant, user, or any other lower-trust input</b> — an
 *                        attacker-controlled endpoint can answer {@code /user} with any identity and thus
 *                        forge a validated token for any user.
 * @param issuer          the issuer stamped onto a validated token; identifies GitHub as the authority
 * @param clientId        this resource server's registered OAuth app client id; becomes the token audience
 * @param cacheTtlSeconds the short upper bound on how long a validated {@code /user} result is cached
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public record GitHubOAuthConfiguration(
        URI userApiUri,
        String issuer,
        String clientId,
        long cacheTtlSeconds) {

    /** GitHub's public REST API {@code /user} endpoint. */
    public static final URI DEFAULT_USER_API_URI = URI.create("https://api.github.com/user");
}
