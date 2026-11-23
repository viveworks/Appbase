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

package com.viveworks.appbase.core.service.configuration.accessor;

import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * A class that implements {@link Comparable} for {@link Class} objects, so it can be added to a set
 * that requires it (for ex, {@link java.util.NavigableSet}).
 *
 * @author Navaneeth Mysore G
 * @param <T> The type of the class
 * @since 1.0.0
 */
@Immutable
public final class ComparableType<T> implements Comparable<ComparableType<T>> {
    private final Class<T> type;

    public ComparableType(final Class<T> type) {
        AppbaseUtil.notNull(type, "type");

        this.type = type;
    }

    /**
     * Return an instance of this class.
     *
     * @param type The wrapped class
     * @param <T> The type of the wrapped class
     * @return An instance of this class
     */
    public static <T> ComparableType<T> from(final Class<T> type) {
        return new ComparableType<>(type);
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public int compareTo(final ComparableType<T> o) {
        return Integer.compare(hashCode(), o.hashCode());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ComparableType<?> that = (ComparableType<?>) o;
        return Objects.equals(type.getName(), that.type.getName());
    }

    @Override
    public int hashCode() {
        return type.getName().hashCode();
    }

    @Override
    public String toString() {
        return "ComparableType{" +
                "type=" + type +
                '}';
    }
}
