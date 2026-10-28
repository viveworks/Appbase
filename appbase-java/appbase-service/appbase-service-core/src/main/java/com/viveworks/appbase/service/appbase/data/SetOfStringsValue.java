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

import java.util.Objects;
import java.util.Set;

/**
 * Jackson value object carrying a set of strings. The supplied set reference is retained rather than copied,
 * so later caller mutations are observable through the accessor; equality and hashing delegate to it.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class SetOfStringsValue {
    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final Set<String> value;

    /**
     * Binds the JSON property directly to this wrapper's retained set reference.
     *
     * @param value payload to retain
     */
    @JsonCreator
    public SetOfStringsValue(@JsonProperty("value") final Set<String> value) {
        this.value = value;
    }

    /**
     * Exposes the retained set reference; callers receive the live reference, not a copy.
     * @return the stored payload
     */
    public Set<String> getValue() {
        return value;
    }

    /**
     * Wraps the supplied set reference without defensive copying.
     *
     * @param value payload to retain
     * @return a new SetOfStringsValue referencing that payload
     */
    public static SetOfStringsValue from(final Set<String> value) {
        return new SetOfStringsValue(value);
    }

    /**
     * Compares SetOfStringsValue instances by the wrapped set reference.
     * @param o candidate SetOfStringsValue wrapper
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

        final SetOfStringsValue that = (SetOfStringsValue) o;
        return Objects.equals(value, that.value);
    }

    /**
     * Computes a hash from the wrapped set reference.
     * @return SetOfStringsValue payload-based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Formats SetOfStringsValue and its stored payload for diagnostic output.
     * @return diagnostic representation containing the SetOfStringsValue payload
     */
    @Override
    public String toString() {
        return "SetOfStringsValue{" +
                "value=" + value +
                '}';
    }
}
