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

/**
 * Derives the {@link TokenCache} key for a raw bearer token.
 *
 * <p>The returned key stands in for the token inside the cache so that the raw, replayable credential is
 * never stored: a dump of the cache leaks claims, not usable tokens. Two implementations ship —
 * {@link Sha256TokenKeyHasher} (the default) and {@link HmacSha256TokenKeyHasher} (keyed) — selected by
 * {@link TokenKeyHashing}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface TokenKeyHasher {

    /**
     * Returns the {@link TokenCache} key for the given raw token. The result is stable for a given token
     * and never the raw token itself.
     *
     * @param rawToken the raw bearer token; never {@code null}
     * @return the cache key
     */
    String hash(String rawToken);
}
