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

package com.viveworks.appbase.client.service.zookeeper.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * ZooKeeper connection handling policy type.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum ConnectionHandlingPolicyType {
    STANDARD("standard"),

    LEGACY("legacy");

    private final String text;

    ConnectionHandlingPolicyType(final String text) {
        AppbaseUtil.notNull(text, "text");
        this.text = text.trim();
    }

    @JsonValue
    public String getText() {
        return text;
    }

    /**
     * Deserializes from the text value (e.g. {@code "standard"}).
     *
     * @param value text value
     * @return matching enum constant
     * @throws IllegalArgumentException if no match found
     */
    @JsonCreator
    public static ConnectionHandlingPolicyType fromText(final String value) {
        AppbaseUtil.notNull(value, "value");
        for (final ConnectionHandlingPolicyType type : values()) {
            if (type.text.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ConnectionHandlingPolicyType: " + value);
    }
}
