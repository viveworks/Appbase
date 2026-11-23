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

package com.viveworks.appbase.service.appbase.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
  * Jackson value object carrying a nullable mapping between string keys and values. The supplied string map is retained
  * rather than copied,
 * so later caller mutations are observable through the accessor; equality and hashing delegate to it.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class MapOfStringStringValue {
    @Nullable
    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> value;

    /**
     * Binds the JSON property directly to this wrapper's retained string map.
     *
     * @param value MapOfStringStringValue payload to retain; may be null
     */
    @JsonCreator
    public MapOfStringStringValue(
            @Nullable
            @JsonProperty("value")
            final Map<String, String> value)
    {
        this.value = value;
    }

    /**
     * Wraps the supplied string map without defensive copying.
     *
     * @param value payload to retain
     * @return a new MapOfStringStringValue referencing that payload
     */
    public static MapOfStringStringValue from(final Map<String, String> value) {
        return new MapOfStringStringValue(value);
    }

    /**
     * Exposes the retained string map; callers receive the live reference, not a copy.
     * @return the stored MapOfStringStringValue payload, possibly null
     */
    @Nullable
    public Map<String, String> getValue() {
        return value;
    }

    /**
     * Compares MapOfStringStringValue instances by the wrapped string map.
     * @param o candidate MapOfStringStringValue wrapper
     * @return true when both wrappers carry equal payloads
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MapOfStringStringValue that = (MapOfStringStringValue) o;
        return Objects.equals(value, that.value);
    }

    /**
     * Computes a hash from the wrapped string map.
     * @return MapOfStringStringValue payload-based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Formats MapOfStringStringValue and its stored payload for diagnostic output.
     * @return diagnostic representation containing the MapOfStringStringValue payload
     */
    @Override
    public String toString() {
        return "MapOfStringStringValue{" +
                "value=" + value +
                '}';
    }
}
