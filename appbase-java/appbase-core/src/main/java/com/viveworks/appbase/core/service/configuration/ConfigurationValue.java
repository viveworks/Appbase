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

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents a configuration value. The {@link #value} can be <code>null</code>.
 *
 * @param <T> the type of the configuration value
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public final class ConfigurationValue<T> {
    @Nullable
    private final T value;

    /**
     * Wraps a decoded configuration value without rejecting {@code null}.
     *
     * @param value decoded value, or {@code null} when the configured value is absent
     */
    @JsonCreator
    public ConfigurationValue(@Nullable
                              @JsonProperty("value")
                              final T value)
    {
        this.value = value;
    }

    /**
     * Construct an instance of this class using the given value.
     *
     * @param value Value (can be null)
     * @param <S> Value type
     * @return Instance of this class
     */
    public static <S> ConfigurationValue<S> fromValue(@Nullable final S value) {
        return new ConfigurationValue<>(value);
    }

    /**
     * Returns the wrapped decoded value.
     *
     * @return the value supplied at construction, possibly {@code null}
     */
    @Nullable
    public T getValue() {
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
        final ConfigurationValue<?> that = (ConfigurationValue<?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ConfigurationValue{" +
                "value=" + value +
                '}';
    }
}
