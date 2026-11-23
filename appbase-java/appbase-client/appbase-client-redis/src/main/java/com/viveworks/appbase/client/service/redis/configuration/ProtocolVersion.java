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
 * Redis serialization protocol version.
 *
 * <p>Maps to {@code io.lettuce.core.protocol.ProtocolVersion}. The text value returned by
 * {@link #getText()} matches the Lettuce enum constant name so that
 * {@code io.lettuce.core.protocol.ProtocolVersion.valueOf(getText())} resolves correctly.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum ProtocolVersion {

    /** Redis Serialization Protocol version 2 (classic, text-based inline). */
    RESP2("RESP2"),

    /** Redis Serialization Protocol version 3 (binary attributes, push messages). */
    RESP3("RESP3");

    private final String text;

    ProtocolVersion(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.text = text;
    }

    /**
     * Returns the text value matching the Lettuce {@code io.lettuce.core.protocol.ProtocolVersion} enum name.
     *
     * @return text value
     */
    public String getText() {
        return text;
    }
}
