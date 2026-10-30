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

package com.viveworks.appbase.core.service.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents a mime type.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public final class MimeType implements Comparable<MimeType> {
    public static final MimeType PROPERTIES = new MimeType("application/java.util.properties");
    public static final MimeType YAML = new MimeType("application/yaml");
    public static final MimeType JSON = new MimeType("application/json");
    public static final MimeType KEY_VALUE = new MimeType("application/key-value");

    private final String value;

    /**
     * Creates a MIME type after requiring a nonblank textual value.
     *
     * @param value MIME type text
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code value} is null or blank
     */
    @JsonCreator
    public MimeType(@JsonProperty("value") final String value) {
        AppbaseUtil.notBlank(value, "value");

        this.value = value;
    }

    /**
     * Returns the nonblank MIME type text supplied at construction.
     *
     * @return the nonblank MIME type text supplied at construction
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MimeType mimeType = (MimeType) o;
        return Objects.equals(value, mimeType.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public int compareTo(final MimeType o) {
        return Integer.compare(hashCode(), o.hashCode());
    }

    @Override
    public String toString() {
        return "MimeType{" +
                "value='" + value + '\'' +
                '}';
    }
}
