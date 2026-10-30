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

package com.viveworks.appbase.configwizard.web;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mints the unguessable per-run browser token used by the A10-01 (internal work item 1449) defence. The wizard server
 * generates one token at startup, sets it as a same-site cookie, and requires it back in a header on
 * every {@code /api} request (see {@link BrowserTokenRequestFilter}). The value must be unpredictable
 * to an attacker page, so it is 256 bits from {@link SecureRandom}, URL-safe-base64 encoded without
 * padding so it rides in a cookie and header unescaped.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class BrowserToken {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private BrowserToken() {
    }

    /**
     * @return a freshly randomised 256-bit token, URL-safe-base64 encoded without padding
     */
    public static String newToken() {
        final byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }
}
