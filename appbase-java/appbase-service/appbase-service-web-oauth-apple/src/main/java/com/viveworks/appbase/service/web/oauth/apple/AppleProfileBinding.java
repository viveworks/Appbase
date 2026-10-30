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

package com.viveworks.appbase.service.web.oauth.apple;

import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import java.util.Map;
import java.util.Objects;

/**
 * Binds a validated Apple id_token to an {@link AppleUserProfile}. Apple exposes no userinfo endpoint, so
 * the profile is exactly the minimum-scope claims the id_token already carries — {@code sub}, {@code email}
 * and {@code email_verified} (spec §11 assumption 1). Read-only: this binder surfaces no field or call
 * that would revoke a token or mutate the account.
 *
 * <p>The identity is taken from the token's verified {@code sub}, not re-read from the raw claim map, so a
 * profile can only ever describe the subject the core engine actually validated. Apple serialises
 * {@code email_verified} as either a JSON boolean or the string {@code "true"}; both are read, and an
 * absent claim is treated as unverified.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class AppleProfileBinding {

    private AppleProfileBinding() {
    }

    /**
     * @param token a token already validated by the core engine
     * @return the minimum-scope profile carried by the token's claims
     */
    public static AppleUserProfile from(ValidatedToken token) {
        Objects.requireNonNull(token, "token");
        Map<String, Object> claims = token.claims();
        return new AppleUserProfile(
                token.subject(),
                asString(claims.get("email")),
                asBoolean(claims.get("email_verified")));
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value instanceof String string && Boolean.parseBoolean(string);
    }
}
