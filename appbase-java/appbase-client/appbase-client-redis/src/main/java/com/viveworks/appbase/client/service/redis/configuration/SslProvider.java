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

package com.viveworks.appbase.client.service.redis.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * SSL/TLS provider for Redis TLS connections.
 *
 * <p>Maps to {@code io.lettuce.core.SslOptions.SslProvider}. The text value returned by
 * {@link #getText()} matches the Lettuce enum constant name so that
 * {@code SslOptions.SslProvider.valueOf(getText())} resolves correctly.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum SslProvider {

    /** Use the JDK's built-in {@code SSLContext}. */
    JDK("JDK"),

    /** Use Netty's OpenSSL (via netty-tcnative). Offers higher throughput. */
    OPENSSL("OPENSSL");

    private final String text;

    SslProvider(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.text = text;
    }

    /**
     * Returns the text value matching the Lettuce {@code SslOptions.SslProvider} enum name.
     *
     * @return text value
     */
    public String getText() {
        return text;
    }
}
