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

package com.viveworks.appbase.client.service.kafka.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Property value for {@code security.protocol}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum SecurityProtocol {
    PLAINTEXT("PLAINTEXT"),

    SSL("SSL"),

    SASL_PLAINTEXT("SASL_PLAINTEXT"),

    SASL_SSL("SASL_SSL");

    private final String text;

    SecurityProtocol(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.text = text.trim();
    }

    public String getText() {
        return text;
    }
}
