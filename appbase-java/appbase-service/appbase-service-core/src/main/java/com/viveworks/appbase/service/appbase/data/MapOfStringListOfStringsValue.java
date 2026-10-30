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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
  * Jackson value object carrying a nullable mapping whose list values use an explicit JSON wrapper. Values passed to
  * the constructor are retained rather than copied,
 * so later caller mutations are observable through the accessor; equality and hashing delegate to it.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class MapOfStringListOfStringsValue {
    @Nullable
    @JsonSerialize(keyAs = String.class, contentAs = ListOfStringsValue.class)
    @JsonDeserialize(keyAs = String.class, contentAs = ListOfStringsValue.class)
    private final Map<String, ListOfStringsValue> value;

    /**
     * Binds the JSON property directly to this wrapper's retained wrapped map.
     *
     * @param value MapOfStringListOfStringsValue payload to retain; may be null
     */
    @JsonCreator
    public MapOfStringListOfStringsValue(
            @Nullable
            @JsonProperty("value")
            final Map<String, ListOfStringsValue> value)
    {
        this.value = value;
    }

    /**
     * Converts each source entry into its nested wrapper. A null source produces an empty mutable map;
     * null entry values remain represented by wrappers containing null.
     *
     * @param source plain collection map to adapt; may be null
     * @return a new wrapper around newly allocated nested values
     */
    public static MapOfStringListOfStringsValue from(final Map<String, List<String>> source) {
        final Map<String, ListOfStringsValue> value = new HashMap<>();

        if (source != null) {
            source.forEach((s, strings) ->
                    value.put(s, new ListOfStringsValue(strings)));
        }

        return new MapOfStringListOfStringsValue(value);
    }

    /**
     * Exposes the retained wrapped map; callers receive the live reference, not a copy.
     * @return the stored MapOfStringListOfStringsValue payload, possibly null
     */
    @Nullable
    public Map<String, ListOfStringsValue> getValue() {
        return value;
    }

    /**
     * Compares MapOfStringListOfStringsValue instances by the wrapped map.
     * @param o candidate MapOfStringListOfStringsValue wrapper
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

        final MapOfStringListOfStringsValue that = (MapOfStringListOfStringsValue) o;
        return Objects.equals(value, that.value);
    }

    /**
     * Computes a hash from the wrapped wrapped map.
     * @return MapOfStringListOfStringsValue payload-based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Formats MapOfStringListOfStringsValue and its stored payload for diagnostic output.
     * @return diagnostic representation containing the MapOfStringListOfStringsValue payload
     */
    @Override
    public String toString() {
        return "MapOfStringListOfStringsValue{" +
                "value=" + value +
                '}';
    }
}
