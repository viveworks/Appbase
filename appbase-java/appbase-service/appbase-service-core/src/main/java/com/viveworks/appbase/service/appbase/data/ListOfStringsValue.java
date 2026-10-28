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
import java.util.List;
import java.util.Objects;

/**
  * Jackson value object carrying a nullable ordered list of strings. The supplied list reference is retained rather
  * than copied,
 * so later caller mutations are observable through the accessor; equality and hashing delegate to it.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ListOfStringsValue {
    @Nullable
    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final List<String> value;

    /**
     * Binds the JSON property directly to this wrapper's retained list reference.
     *
     * @param value ListOfStringsValue payload to retain; may be null
     */
    @JsonCreator
    public ListOfStringsValue(
            @Nullable
            @JsonProperty("value")
            final List<String> value)
    {
        this.value = value;
    }

    /**
     * Wraps the supplied list reference without defensive copying.
     *
     * @param value payload to retain
     * @return a new ListOfStringsValue referencing that payload
     */
    public static ListOfStringsValue from(final List<String> value) {
        return new ListOfStringsValue(value);
    }

    /**
     * Exposes the retained list reference; callers receive the live reference, not a copy.
     * @return the stored ListOfStringsValue payload, possibly null
     */
    @Nullable
    public List<String> getValue() {
        return value;
    }

    /**
     * Compares ListOfStringsValue instances by the wrapped list reference.
     * @param o candidate ListOfStringsValue wrapper
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

        final ListOfStringsValue that = (ListOfStringsValue) o;
        return Objects.equals(value, that.value);
    }

    /**
     * Computes a hash from the wrapped list reference.
     * @return ListOfStringsValue payload-based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Formats ListOfStringsValue and its stored payload for diagnostic output.
     * @return diagnostic representation containing the ListOfStringsValue payload
     */
    @Override
    public String toString() {
        return "ListOfStringsValue{" +
                "value=" + value +
                '}';
    }
}
