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

import com.google.inject.util.Providers;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Abstract generic {@code Guice} module to inject two types with instances or {@link Provider}s.
 *
 * @author Navaneeth Mysore G
 * @param <T> First type to bind
 * @param <U> Second type to bind
 * @since 1.0.0
 */
public abstract class AbstractGeneric2GuiceModule<T, U> extends AbstractGuiceModule {
    private final Class<T> oneBindingType;
    private final Class<? extends T> oneBindingImplementationType;
    private final T oneInstance;
    private final Provider<? extends T> oneInstanceProvider;

    private final Class<U> twoBindingType;
    private final Class<? extends U> twoBindingImplementationType;
    private final U twoInstance;
    private final Provider<? extends U> twoInstanceProvider;

    /**
     * This constructor is used to allow {@code null} bindings in rare cases.
     */
    protected AbstractGeneric2GuiceModule(final Class<T> oneBindingType, final Class<U> twoBindingType) {
        super(null, null);

        AppbaseUtil.notNull(oneBindingType, "oneBindingType");
        AppbaseUtil.notNull(twoBindingType, "twoBindingType");

        this.oneBindingType = oneBindingType;
        this.oneInstance = null;
        this.oneInstanceProvider = null;
        this.oneBindingImplementationType = null;

        this.twoBindingType = twoBindingType;
        this.twoInstance = null;
        this.twoInstanceProvider = null;
        this.twoBindingImplementationType = null;
    }




    public AbstractGeneric2GuiceModule(
            final Class<T> oneBindingType,
            final T oneInstance,
            final Class<U> twoBindingType,
            final U twoInstance)
    {
        super(null, null);

        AppbaseUtil.notNull(oneBindingType, "oneBindingType");
        AppbaseUtil.notNull(oneInstance, "oneInstance");
        AppbaseUtil.notNull(twoBindingType, "twoBindingType");
        AppbaseUtil.notNull(twoInstance, "twoInstance");

        this.oneBindingType = oneBindingType;
        this.oneInstance = oneInstance;
        this.oneInstanceProvider = null;
        this.oneBindingImplementationType = null;

        this.twoBindingType = twoBindingType;
        this.twoInstance = twoInstance;
        this.twoInstanceProvider = null;
        this.twoBindingImplementationType = null;
    }

    public AbstractGeneric2GuiceModule(
            final Class<T> oneBindingType,
            final T oneInstance,
            final Class<U> twoBindingType,
            final U twoInstance,
            final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(oneBindingType, "oneBindingType");
        AppbaseUtil.notNull(oneInstance, "oneInstance");
        AppbaseUtil.notNull(twoBindingType, "twoBindingType");
        AppbaseUtil.notNull(twoInstance, "twoInstance");

        this.oneBindingType = oneBindingType;
        this.oneInstance = oneInstance;
        this.oneInstanceProvider = null;
        this.oneBindingImplementationType = null;

        this.twoBindingType = twoBindingType;
        this.twoInstance = twoInstance;
        this.twoInstanceProvider = null;
        this.twoBindingImplementationType = null;
    }

    public AbstractGeneric2GuiceModule(
            final Class<T> oneBindingType,
            final T oneInstance,
            final Class<U> twoBindingType,
            final U twoInstance,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(oneBindingType, "oneBindingType");
        AppbaseUtil.notNull(oneInstance, "oneInstance");
        AppbaseUtil.notNull(twoBindingType, "twoBindingType");
        AppbaseUtil.notNull(twoInstance, "twoInstance");

        this.oneBindingType = oneBindingType;
        this.oneInstance = oneInstance;
        this.oneInstanceProvider = null;
        this.oneBindingImplementationType = null;

        this.twoBindingType = twoBindingType;
        this.twoInstance = twoInstance;
        this.twoInstanceProvider = null;
        this.twoBindingImplementationType = null;
    }




    public AbstractGeneric2GuiceModule(
            final Class<T> oneBindingType,
            final Provider<? extends T> oneInstanceProvider,
            final Class<U> twoBindingType,
            final Provider<? extends U> twoInstanceProvider)
    {
        super(null, null);

        AppbaseUtil.notNull(oneBindingType, "oneBindingType");
        AppbaseUtil.notNull(oneInstanceProvider, "oneInstanceProvider");
        AppbaseUtil.notNull(twoBindingType, "twoBindingType");
        AppbaseUtil.notNull(twoInstanceProvider, "twoInstanceProvider");

        this.oneBindingType = oneBindingType;
        this.oneInstanceProvider = oneInstanceProvider;
        this.oneInstance = null;
        this.oneBindingImplementationType = null;

        this.twoBindingType = twoBindingType;
        this.twoInstanceProvider = twoInstanceProvider;
        this.twoInstance = null;
        this.twoBindingImplementationType = null;
    }

    public AbstractGeneric2GuiceModule(
            final Class<T> oneBindingType,
            final Provider<? extends T> oneInstanceProvider,
            final Class<U> twoBindingType,
            final Provider<? extends U> twoInstanceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(oneBindingType, "oneBindingType");
        AppbaseUtil.notNull(oneInstanceProvider, "oneInstanceProvider");
        AppbaseUtil.notNull(twoBindingType, "twoBindingType");
        AppbaseUtil.notNull(twoInstanceProvider, "twoInstanceProvider");

        this.oneBindingType = oneBindingType;
        this.oneInstanceProvider = oneInstanceProvider;
        this.oneInstance = null;
        this.oneBindingImplementationType = null;

        this.twoBindingType = twoBindingType;
        this.twoInstanceProvider = twoInstanceProvider;
        this.twoInstance = null;
        this.twoBindingImplementationType = null;
    }

    public AbstractGeneric2GuiceModule(
            final Class<T> oneBindingType,
            final Provider<? extends T> oneInstanceProvider,
            final Class<U> twoBindingType,
            final Provider<? extends U> twoInstanceProvider,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(oneBindingType, "oneBindingType");
        AppbaseUtil.notNull(oneInstanceProvider, "oneInstanceProvider");
        AppbaseUtil.notNull(twoBindingType, "twoBindingType");
        AppbaseUtil.notNull(twoInstanceProvider, "twoInstanceProvider");

        this.oneBindingType = oneBindingType;
        this.oneInstanceProvider = oneInstanceProvider;
        this.oneInstance = null;
        this.oneBindingImplementationType = null;

        this.twoBindingType = twoBindingType;
        this.twoInstanceProvider = twoInstanceProvider;
        this.twoInstance = null;
        this.twoBindingImplementationType = null;
    }




    public AbstractGeneric2GuiceModule(
            final Class<T> oneBindingType,
            final Class<? extends T> oneBindingImplementationType,
            final Class<U> twoBindingType,
            final Class<? extends U> twoBindingImplementationType)
    {
        super(null, null);

        AppbaseUtil.notNull(oneBindingType, "oneBindingType");
        AppbaseUtil.notNull(oneBindingImplementationType, "oneBindingImplementationType");
        AppbaseUtil.notNull(twoBindingType, "twoBindingType");
        AppbaseUtil.notNull(twoBindingImplementationType, "twoBindingImplementationType");

        this.oneBindingType = oneBindingType;
        this.oneBindingImplementationType = oneBindingImplementationType;
        this.oneInstanceProvider = null;
        this.oneInstance = null;

        this.twoBindingType = twoBindingType;
        this.twoBindingImplementationType = twoBindingImplementationType;
        this.twoInstanceProvider = null;
        this.twoInstance = null;
    }

    public AbstractGeneric2GuiceModule(
            final Class<T> oneBindingType,
            final Class<? extends T> oneBindingImplementationType,
            final Class<U> twoBindingType,
            final Class<? extends U> twoBindingImplementationType,
            final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(oneBindingType, "oneBindingType");
        AppbaseUtil.notNull(oneBindingImplementationType, "oneBindingImplementationType");
        AppbaseUtil.notNull(twoBindingType, "twoBindingType");
        AppbaseUtil.notNull(twoBindingImplementationType, "twoBindingImplementationType");

        this.oneBindingType = oneBindingType;
        this.oneBindingImplementationType = oneBindingImplementationType;
        this.oneInstanceProvider = null;
        this.oneInstance = null;

        this.twoBindingType = twoBindingType;
        this.twoBindingImplementationType = twoBindingImplementationType;
        this.twoInstanceProvider = null;
        this.twoInstance = null;
    }

    public AbstractGeneric2GuiceModule(
            final Class<T> oneBindingType,
            final Class<? extends T> oneBindingImplementationType,
            final Class<U> twoBindingType,
            final Class<? extends U> twoBindingImplementationType,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(oneBindingType, "oneBindingType");
        AppbaseUtil.notNull(oneBindingImplementationType, "oneBindingImplementationType");
        AppbaseUtil.notNull(twoBindingType, "twoBindingType");
        AppbaseUtil.notNull(twoBindingImplementationType, "twoBindingImplementationType");

        this.oneBindingType = oneBindingType;
        this.oneBindingImplementationType = oneBindingImplementationType;
        this.oneInstanceProvider = null;
        this.oneInstance = null;

        this.twoBindingType = twoBindingType;
        this.twoBindingImplementationType = twoBindingImplementationType;
        this.twoInstanceProvider = null;
        this.twoInstance = null;
    }


    @Override
    protected void configure() {
        if (oneInstance == null && oneInstanceProvider == null && oneBindingImplementationType == null) {
            bind(oneBindingType).toProvider(Providers.of(null)).asEagerSingleton();
        } else {
            if (qualifierBindingAnnotation != null || qualifierBindingName != null) {
                final Annotation annotation = bindingAnnotation();

                if (oneInstance != null) {
                    bind(oneBindingType).annotatedWith(annotation).toInstance(oneInstance);
                } else if (oneInstanceProvider != null) {
                    bind(oneBindingType).annotatedWith(annotation).toProvider(oneInstanceProvider).asEagerSingleton();
                } else {
                    bind(oneBindingType).annotatedWith(annotation).to(oneBindingImplementationType).asEagerSingleton();
                }
            } else {
                if (oneInstance != null) {
                    bind(oneBindingType).toInstance(oneInstance);
                } else if (oneInstanceProvider != null) {
                    bind(oneBindingType).toProvider(oneInstanceProvider).asEagerSingleton();
                } else {
                    bind(oneBindingType).to(oneBindingImplementationType).asEagerSingleton();
                }
            }
        }


        if (twoInstance == null && twoInstanceProvider == null && twoBindingImplementationType == null) {
            bind(twoBindingType).toProvider(Providers.of(null)).asEagerSingleton();
        } else {
            if (qualifierBindingAnnotation != null || qualifierBindingName != null) {
                final Annotation annotation = bindingAnnotation();

                if (twoInstance != null) {
                    bind(twoBindingType).annotatedWith(annotation).toInstance(twoInstance);
                } else if (twoInstanceProvider != null) {
                    bind(twoBindingType).annotatedWith(annotation).toProvider(twoInstanceProvider).asEagerSingleton();
                } else {
                    bind(twoBindingType).annotatedWith(annotation).to(twoBindingImplementationType).asEagerSingleton();
                }
            } else {
                if (twoInstance != null) {
                    bind(twoBindingType).toInstance(twoInstance);
                } else if (twoInstanceProvider != null) {
                    bind(twoBindingType).toProvider(twoInstanceProvider).asEagerSingleton();
                } else {
                    bind(twoBindingType).to(twoBindingImplementationType).asEagerSingleton();
                }
            }
        }
    }
}
