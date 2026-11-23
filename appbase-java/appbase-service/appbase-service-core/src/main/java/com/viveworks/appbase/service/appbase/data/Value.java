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

import java.util.Objects;

/**
  * Jackson value object carrying an arbitrary JSON-compatible value. The supplied object reference is retained rather
  * than copied,
 * so later caller mutations are observable through the accessor; equality and hashing delegate to it.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class Value {
    private final Object value;

    /**
     * Binds the JSON property directly to this wrapper's retained object reference.
     *
     * @param value payload to retain
     */
    @JsonCreator
    public Value(
            @JsonProperty("value")
            final Object value)
    {
        this.value = value;
    }

    /**
     * Exposes the retained object reference; callers receive the live reference, not a copy.
     * @return the stored payload
     */
    public Object getValue() {
        return value;
    }

    /**
     * Compares Value instances by the wrapped object reference.
     * @param o candidate Value wrapper
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

        final Value value1 = (Value) o;
        return Objects.equals(value, value1.value);
    }

    /**
     * Computes a hash from the wrapped object reference.
     * @return Value payload-based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Formats Value and its stored payload for diagnostic output.
     * @return diagnostic representation containing the Value payload
     */
    @Override
    public String toString() {
        return "Value{" +
                "value=" + value +
                '}';
    }
}
