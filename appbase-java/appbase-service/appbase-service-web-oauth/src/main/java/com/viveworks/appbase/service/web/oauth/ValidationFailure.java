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
 * A failed validation: why the token was rejected. The filter maps every reason to HTTP 401.
 *
 * @param reason the category of failure
 * @param detail a short, non-sensitive description (never the raw token or signature)
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public record ValidationFailure(Reason reason, String detail) implements ValidationResult {

    /** The categories of validation failure, each rejected with HTTP 401. */
    public enum Reason {
        EXPIRED,
        BAD_SIGNATURE,
        UNTRUSTED_ISSUER,
        AUDIENCE_MISMATCH,
        MALFORMED,
        UNSUPPORTED_ALGORITHM,
        PROVIDER_UNREACHABLE
    }
}
