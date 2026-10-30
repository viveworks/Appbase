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

import java.net.URI;

/**
 * The YAML-bound configuration for the OAuth filter and validation engine.
 *
 * @param jwksUri                   the HTTPS provider JWKS endpoint
 * @param issuer                    the expected {@code iss}
 * @param clientId                  this relying party's client id
 * @param audienceClaim             the expected {@code aud} VALUE (this relying party's identifier, e.g.
 *                                  the client id) that the token's audience must contain — NOT a claim
 *                                  name. Despite the field name, {@link NimbusTokenValidator} checks
 *                                  {@code aud.contains(audienceClaim)}.
 * @param tokenExpiryMarginSeconds  clock-skew margin applied to {@code exp}/{@code nbf} checks
 * @param cacheTtlSeconds           the upper bound on how long a validated token is cached
 * @param tokenKeyHashing           which {@link TokenKeyHasher} derives cache keys; {@code null} means
 *                                  {@link TokenKeyHashing#SHA256}
 * @param tokenKeyHmacKeyRef        the config-secret reference for the HMAC key; required only under
 *                                  {@link TokenKeyHashing#HMAC_SHA256}
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public record OAuthConfiguration(
        URI jwksUri,
        String issuer,
        String clientId,
        String audienceClaim,
        long tokenExpiryMarginSeconds,
        long cacheTtlSeconds,
        TokenKeyHashing tokenKeyHashing,
        String tokenKeyHmacKeyRef) {
}
