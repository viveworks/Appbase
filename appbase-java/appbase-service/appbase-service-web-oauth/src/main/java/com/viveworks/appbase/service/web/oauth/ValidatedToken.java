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

package com.viveworks.appbase.service.web.oauth;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * A successfully validated bearer token: the verified claims a request may act on.
 *
 * @param subject   the {@code sub} claim
 * @param issuer    the verified {@code iss}
 * @param audience  the {@code aud} set
 * @param expiresAt the {@code exp}
 * @param claims    the full claim set
 * @param tokenKey  a namespace-free {@link TokenKeyHasher} output over the raw token, never the raw token
 *                  itself; this is not the {@link TokenCache} key, which binds the OAuth configuration
 *                  namespace before hashing
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public record ValidatedToken(
        String subject,
        String issuer,
        Set<String> audience,
        Instant expiresAt,
        Map<String, Object> claims,
        String tokenKey) implements ValidationResult {
}
