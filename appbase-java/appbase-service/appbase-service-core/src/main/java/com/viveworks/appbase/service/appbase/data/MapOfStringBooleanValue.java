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

package com.viveworks.appbase.service.appbase.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
  * Jackson value object carrying a nullable mapping from names to Boolean flags. The supplied map reference is retained
  * rather than copied,
 * so later caller mutations are observable through the accessor; equality and hashing delegate to it.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class MapOfStringBooleanValue {
    @Nullable
    @JsonSerialize(keyAs = String.class, contentAs = Boolean.class)
    @JsonDeserialize(keyAs = String.class, contentAs = Boolean.class)
    private final Map<String, Boolean> value;

    /**
     * Binds the JSON property directly to this wrapper's retained map reference.
     *
     * @param value MapOfStringBooleanValue payload to retain; may be null
     */
    @JsonCreator
    public MapOfStringBooleanValue(
            @Nullable
            @JsonProperty("value")
            final Map<String, Boolean> value)
    {
        this.value = value;
    }

    /**
     * Wraps the supplied map reference without defensive copying.
     *
     * @param value payload to retain
     * @return a new MapOfStringBooleanValue referencing that payload
     */
    public static MapOfStringBooleanValue from(final Map<String, Boolean> value) {
        return new MapOfStringBooleanValue(value);
    }

    /**
     * Exposes the retained map reference; callers receive the live reference, not a copy.
     * @return the stored MapOfStringBooleanValue payload, possibly null
     */
    @Nullable
    public Map<String, Boolean> getValue() {
        return value;
    }

    /**
     * Compares MapOfStringBooleanValue instances by the wrapped map reference.
     * @param o candidate MapOfStringBooleanValue wrapper
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

        final MapOfStringBooleanValue that = (MapOfStringBooleanValue) o;
        return Objects.equals(value, that.value);
    }

    /**
     * Computes a hash from the wrapped map reference.
     * @return MapOfStringBooleanValue payload-based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Formats MapOfStringBooleanValue and its stored payload for diagnostic output.
     * @return diagnostic representation containing the MapOfStringBooleanValue payload
     */
    @Override
    public String toString() {
        return "MapOfStringBooleanValue{" +
                "value=" + value +
                '}';
    }
}
