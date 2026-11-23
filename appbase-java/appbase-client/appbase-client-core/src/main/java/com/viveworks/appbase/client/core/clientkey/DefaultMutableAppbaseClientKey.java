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

package com.viveworks.appbase.client.core.clientkey;

import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link MutableAppbaseClientKey}.
 *
 * @author Navaneeth Mysore G
 * @param <V> {@link ComparableValue} type
 * @since 1.0.0
 */
public class DefaultMutableAppbaseClientKey<V extends ComparableValue> implements MutableAppbaseClientKey<V> {
    private final AtomicReference<V> valueRef;

    /** Creates a clean pooled key whose value must be initialized before comparison or lookup. */
    public DefaultMutableAppbaseClientKey() {
        valueRef = new AtomicReference<>();
    }

    /**
     * Creates an immediately valid key around a comparable value.
     * @param value non-null identity used for equality, ordering, and pool lookup
     */
    public DefaultMutableAppbaseClientKey(final V value) {
        AppbaseUtil.notNull(value, "value");

        this.valueRef = new AtomicReference<>(value);
    }

    /** {@inheritDoc} */
    @Override
    public void initValue(final V value) {
        AppbaseUtil.notNull(value, "value");

        valueRef.set(value);
    }

    /** {@inheritDoc} */
    @Override
    public V getValue() {
        return valueRef.get();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid() {
        return getValue() != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isClean() {
        return getValue() == null;
    }

    /** {@inheritDoc} */
    @Override
    public void clean() {
        valueRef.set(null);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(@NotNull final Object other) {
        AppbaseUtil.notNull(other, "other");

        if (!AppbaseClientKey.class.isAssignableFrom(getClass())) {
            throw new AppbaseRuntimeException("other must be of type " + AppbaseClientKey.class.getName());
        }

        final AppbaseClientKey<V> appbaseClientKey = (AppbaseClientKey<V>) other;
        AppbaseUtil.valid(appbaseClientKey, "appbaseClientKey");
        AppbaseUtil.valid(this, "this");

        return getValue().compareTo(appbaseClientKey.getValue());
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultMutableAppbaseClientKey<V> that = (DefaultMutableAppbaseClientKey<V>) o;
        return Objects.equals(valueRef.get(), that.valueRef.get());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(valueRef.get());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DefaultMutableAppbaseClientKey{" +
                "valueRef=" + valueRef.get() +
                '}';
    }

    /**
     * {@link ObjectFactory} for {@link DefaultMutableAppbaseClientKey}.
     *
     * @author Navaneeth Mysore G
     * @param <V> {@link ComparableValue} type
     * @since 1.0.0
     */
    public static class Factory<V extends ComparableValue> implements ObjectFactory<MutableAppbaseClientKey<V>> {
        /** {@inheritDoc} */
        @Override
        public MutableAppbaseClientKey<V> newObject() {
            return new DefaultMutableAppbaseClientKey<>();
        }
    }
}
