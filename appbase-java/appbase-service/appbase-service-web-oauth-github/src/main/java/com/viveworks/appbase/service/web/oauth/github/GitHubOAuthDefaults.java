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

import java.util.Objects;

/**
 * Pre-filled GitHub defaults. GitHub is not OIDC, so there is no discovery document or JWKS endpoint to
 * pin; the analogue is the public REST {@code /user} endpoint plus a deliberately short cache TTL. A
 * relying party need only supply its own OAuth App client id — the rest of the
 * {@link GitHubOAuthConfiguration} is filled from GitHub's well-known values.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class GitHubOAuthDefaults {

    /** The authority stamped onto a validated token; identifies GitHub. */
    public static final String ISSUER = "https://github.com";

    /**
     * Default short cache TTL (seconds). Kept short because an opaque GitHub token can be revoked upstream
     * at any moment and the cache is the window in which a revoked token is still served.
     */
    public static final long DEFAULT_CACHE_TTL_SECONDS = 60L;

    private GitHubOAuthDefaults() {
    }

    /**
     * Builds a {@link GitHubOAuthConfiguration} from GitHub's defaults for the given relying party.
     *
     * @param clientId this relying party's GitHub OAuth App client id
     * @return a configuration pointed at GitHub's public {@code /user} endpoint with a short cache TTL
     */
    public static GitHubOAuthConfiguration configuration(String clientId) {
        Objects.requireNonNull(clientId, "clientId");
        return new GitHubOAuthConfiguration(
                GitHubOAuthConfiguration.DEFAULT_USER_API_URI,
                ISSUER,
                clientId,
                DEFAULT_CACHE_TTL_SECONDS);
    }
}
