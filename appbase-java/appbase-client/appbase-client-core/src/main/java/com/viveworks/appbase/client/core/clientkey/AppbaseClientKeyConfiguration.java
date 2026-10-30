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

package com.viveworks.appbase.client.core.clientkey;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@code Jackson} serializable / deserializable configuration representation of {@link AppbaseClientKey}. This is used
 * instead of implementations {@link AppbaseClientKey} since the latter uses
 * {@link AtomicReference}s for fields.
 *
 * @author Navaneeth Mysore G
 * @param <V> {@link ComparableValue} type
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AppbaseClientKeyConfiguration.Builder.class)
public final class AppbaseClientKeyConfiguration<V extends ComparableValue> {
    private final V value;

    @SuppressWarnings("unchecked")
    private AppbaseClientKeyConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.value, "builder.value");

        value = (V) builder.value;
    }

    /** Returns the exact value reference supplied to the builder; it is not defensively copied.
     * Mutating a mutable value after construction can therefore change this configuration's equality and hash code.
     * @return retained comparable value represented by the configured client key
     */
    public V getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AppbaseClientKeyConfiguration<?> that = (AppbaseClientKeyConfiguration<?>) o;
        return Objects.equals(value, that.value);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AppbaseClientKeyConfiguration{" +
                "value=" + value +
                '}';
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder that retains the client-key value supplied to it.
     *
     * @author Navaneeth Mysore G
     * @param <V> {@link ComparableValue} type
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder<V extends ComparableValue> {
        private V value;

        private Builder() {
        }

        public Builder value(final V value) {
            this.value = value;
            return this;
        }

        public AppbaseClientKeyConfiguration build() {
            return new AppbaseClientKeyConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
