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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.viveworks.appbase.service.web.oauth.Sha256TokenKeyHasher;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import com.viveworks.appbase.service.web.oauth.ValidationResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * The GitHub provider defaults: pre-fill the {@code /user} endpoint, issuer and a short cache TTL for a
 * relying party that supplies only its client id, and produce a configuration a {@link GitHubTokenValidator}
 * accepts and uses to validate a token — proven offline through the package-private transport seam.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
class GitHubOAuthDefaultsTest {

    private static final String CLIENT_ID = "Iv1.0123456789abcdef";

    @Test
    void configurationPinsGitHubsPublicEndpoints() {
        GitHubOAuthConfiguration cfg = GitHubOAuthDefaults.configuration(CLIENT_ID);

        assertEquals(GitHubOAuthConfiguration.DEFAULT_USER_API_URI, cfg.userApiUri());
        assertEquals(GitHubOAuthDefaults.ISSUER, cfg.issuer());
        assertEquals(CLIENT_ID, cfg.clientId());
        assertTrue(cfg.cacheTtlSeconds() > 0, "the cache TTL must be positive");
    }

    @Test
    void configurationDrivesAWorkingValidator() {
        GitHubOAuthConfiguration cfg = GitHubOAuthDefaults.configuration(CLIENT_ID);
        Clock clock = Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC);
        GitHubTokenValidator validator = new GitHubTokenValidator(
                cfg, new Sha256TokenKeyHasher(), new MapTokenCache(),
                bearer -> new GitHubUserApi.Response(200, "{\"id\":42,\"login\":\"octocat\"}"), clock);

        ValidationResult result = validator.validate("gho_token");

        ValidatedToken token = assertInstanceOf(ValidatedToken.class, result);
        assertEquals("42", token.subject());
        assertEquals(GitHubOAuthDefaults.ISSUER, token.issuer());
    }

    /** Minimal always-miss cache: this test exercises the defaults, not caching. */
    private static final class MapTokenCache implements com.viveworks.appbase.service.web.oauth.TokenCache {
        @Override
        public java.util.Optional<ValidatedToken> get(String tokenKey) {
            return java.util.Optional.empty();
        }

        @Override
        public void put(String tokenKey, ValidatedToken token, Duration ttl) {
        }
    }
}
