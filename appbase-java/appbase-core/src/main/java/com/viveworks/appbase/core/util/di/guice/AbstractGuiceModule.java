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

import com.google.inject.AbstractModule;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * Represents a {@code Guice module}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractGuiceModule extends AbstractModule {
    protected final Annotation qualifierBindingAnnotation;
    protected final String qualifierBindingName;

    public AbstractGuiceModule(
            @Nullable final Annotation qualifierBindingAnnotation,
            @Nullable final String qualifierBindingName)
    {
        this.qualifierBindingAnnotation = qualifierBindingAnnotation;
        this.qualifierBindingName = qualifierBindingName;
    }

    public AbstractGuiceModule(final Annotation qualifierBindingAnnotation) {
        AppbaseUtil.notNull(qualifierBindingAnnotation, "qualifierBindingAnnotation");

        this.qualifierBindingAnnotation = qualifierBindingAnnotation;
        this.qualifierBindingName = null;
    }

    public AbstractGuiceModule(final String qualifierBindingName) {
        AppbaseUtil.notNull(qualifierBindingName, "qualifierBindingName");

        this.qualifierBindingName = qualifierBindingName;
        this.qualifierBindingAnnotation = null;
    }

    protected Annotation bindingAnnotation() {
        return Objects.requireNonNullElseGet(qualifierBindingAnnotation, () -> new NamedValue(qualifierBindingName));
    }

    protected Annotation bindingAnnotationOrNull() {
        if (qualifierBindingAnnotation == null && qualifierBindingName == null) {
            return null;
        }

        return bindingAnnotation();
    }

    protected <T> void bindType(
            final Class<T> bindingType,
            final Annotation bindingAnnotation,
            final T instance,
            final Provider<? extends T> instanceProvider,
            final Class<? extends T> implementationType)
    {
        AppbaseUtil.notNull(bindingType, "bindingType");

        if (bindingAnnotation != null) {
            if (instance != null) {
                bind(bindingType).annotatedWith(bindingAnnotation).toInstance(instance);
            } else if (instanceProvider != null) {
                bind(bindingType).annotatedWith(bindingAnnotation).toProvider(instanceProvider).asEagerSingleton();
            } else if (implementationType != null) {
                bind(bindingType).annotatedWith(bindingAnnotation).to(implementationType).asEagerSingleton();
            } else {
                throw new AppbaseRuntimeException(
                        "Failed to bind [" + bindingType + ", " + bindingAnnotation + "]" +
                                ", since all required arguments were null");
            }
        } else {
            if (instance != null) {
                bind(bindingType).toInstance(instance);
            } else if (instanceProvider != null) {
                bind(bindingType).toProvider(instanceProvider).asEagerSingleton();
            } else if (implementationType != null) {
                bind(bindingType).to(implementationType).asEagerSingleton();
            } else {
                throw new AppbaseRuntimeException(
                        "Failed to bind [" + bindingType + "]" + ", since all required arguments were null");
            }
        }
    }
}
