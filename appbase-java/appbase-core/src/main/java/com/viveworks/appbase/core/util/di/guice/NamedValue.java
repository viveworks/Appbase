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

package com.viveworks.appbase.core.util.di.guice;

import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.inject.Named;
import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * Default implementation of {@link Named}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class NamedValue implements Named, Serializable {
    private static final long serialVersionUID = -4865624797309311748L;

    private final String value;

    public NamedValue(final String value) {
        AppbaseUtil.notNull(value, "value");

        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Named.class;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Named)) {
            return false;
        }

        final Named other = (Named) o;
        return value.equals(other.value());
    }

    @Override
    @SuppressWarnings("checkstyle:MagicNumber")
    public int hashCode() {
        return (127 * "value".hashCode()) ^ value.hashCode();
    }

    @Override
    public String toString() {
        return "NamedValue{" +
                "value='" + value + '\'' +
                '}';
    }
}
