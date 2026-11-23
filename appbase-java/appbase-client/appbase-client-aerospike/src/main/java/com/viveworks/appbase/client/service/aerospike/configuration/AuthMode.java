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

package com.viveworks.appbase.client.service.aerospike.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Authentication mode for Aerospike client connections.
 *
 * <p>Maps to {@code com.aerospike.client.policy.AuthMode}.</p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum AuthMode {
    /**
     * Use internal authentication only. Hashed password is stored on the server.
     */
    INTERNAL("INTERNAL"),

    /**
     * Use external authentication (e.g. LDAP). Requires TLS to send clear password.
     */
    EXTERNAL("EXTERNAL"),

    /**
     * Use external authentication without TLS. For testing purposes only.
     */
    EXTERNAL_INSECURE("EXTERNAL_INSECURE"),

    /**
     * Use PKI (certificate-based) authentication.
     */
    PKI("PKI");

    private final String text;

    AuthMode(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.text = text.trim();
    }

    public String getText() {
        return text;
    }
}
