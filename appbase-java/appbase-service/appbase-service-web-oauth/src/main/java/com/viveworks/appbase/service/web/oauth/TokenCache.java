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

import java.time.Duration;
import java.util.Optional;

/**
 * Caches validated tokens so a repeated bearer token skips re-validation until it expires.
 *
 * <p>Keyed by the {@link TokenKeyHasher} digest of the raw token — never the raw token — so a dump of
 * the store leaks claims, not usable credentials. The built-in {@link CaffeineTokenCache} is the
 * {@link java.util.ServiceLoader} default; the external Aerospike and Redis backends need a configured
 * client, so each is bound through its own Guice module rather than through {@code ServiceLoader}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface TokenCache {

    /**
     * @param tokenKey the {@link TokenKeyHasher} digest of the raw token
     * @return the cached {@link ValidatedToken}, or empty on a miss or after expiry
     */
    Optional<ValidatedToken> get(String tokenKey);

    /**
     * Stores a validated token under its key for at most {@code ttl}.
     *
     * @param tokenKey the {@link TokenKeyHasher} digest of the raw token
     * @param token    the validated token
     * @param ttl      how long the entry may live
     */
    void put(String tokenKey, ValidatedToken token, Duration ttl);
}
