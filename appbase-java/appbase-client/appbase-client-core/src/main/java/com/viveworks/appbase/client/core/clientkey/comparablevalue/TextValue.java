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

package com.viveworks.appbase.client.core.clientkey.comparablevalue;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a text value that can be used with {@link com.viveworks.appbase.client.core.clientkey.AppbaseClientKey}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class TextValue implements PoolableComparableValue {
    public static final TextValue PRIMARY = new TextValue("primary");
    public static final TextValue SECONDARY = new TextValue("secondary");

    private final AtomicReference<String> textRef;

    public TextValue() {
        this.textRef = new AtomicReference<>();
    }

    public TextValue(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.textRef = new AtomicReference<>(text);
    }

    public void init(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.textRef.set(text);
    }

    public String getText() {
        return textRef.get();
    }

    @Override
    public boolean isValid() {
        return textRef.get() != null;
    }

    @Override
    public boolean isClean() {
        return textRef.get() == null;
    }

    @Override
    public void clean() {
        textRef.set(null);
    }

    @Override
    public int compareTo(@NotNull final Object other) {
        AppbaseUtil.notNull(other, "other");

        if (!TextValue.class.isAssignableFrom(other.getClass())) {
            throw new AppbaseRuntimeException("other must be of type " + TextValue.class);
        }

        final TextValue textValue = (TextValue) other;
        AppbaseUtil.valid(this, "this");
        AppbaseUtil.valid(textValue, "textValue");

        return getText().compareTo(textValue.getText());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TextValue textValue = (TextValue) o;
        return Objects.equals(textRef.get(), textValue.textRef.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(textRef.get());
    }

    @Override
    public String toString() {
        return "TextValue{" +
                "text=" + textRef.get() +
                '}';
    }

    /**
     * {@link ObjectFactory} for {@link TextValue}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public static class Factory implements ObjectFactory<TextValue> {
        @Override
        public TextValue newObject() {
            return new TextValue();
        }
    }
}
