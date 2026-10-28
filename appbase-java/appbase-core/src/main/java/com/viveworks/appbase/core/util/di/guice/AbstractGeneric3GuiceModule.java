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

package com.viveworks.appbase.core.util.di.guice;

import com.google.inject.util.Providers;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Abstract generic {@code Guice} module to inject three types with instances or {@link Provider}s.
 *
 * @author Navaneeth Mysore G
 * @param <T> First type to bind
 * @param <U> Second type to bind
 * @param <V> Third type to bind
 * @since 1.0.0
 */
public abstract class AbstractGeneric3GuiceModule<T, U, V> extends AbstractGeneric2GuiceModule<T, U> {
    private final Class<V> threeBindingType;
    private final Class<? extends V> threeBindingImplementationType;
    private final V threeInstance;
    private final Provider<? extends V> threeInstanceProvider;

    /**
     * This constructor is used to allow {@code null} bindings in rare cases.
     */
    protected AbstractGeneric3GuiceModule(
            final Class<T> oneBindingType,
            final Class<U> twoBindingType,
            final Class<V> threeBindingType)
    {
        super(oneBindingType, twoBindingType);

        AppbaseUtil.notNull(threeBindingType, "threeBindingType");

        this.threeBindingType = threeBindingType;
        this.threeInstance = null;
        this.threeInstanceProvider = null;
        this.threeBindingImplementationType = null;
    }




    public AbstractGeneric3GuiceModule(
            final Class<T> oneBindingType,
            final T oneInstance,
            final Class<U> twoBindingType,
            final U twoInstance,
            final Class<V> threeBindingType,
            final V threeInstance)
    {
        super(oneBindingType, oneInstance, twoBindingType, twoInstance);

        AppbaseUtil.notNull(threeBindingType, "threeBindingType");
        AppbaseUtil.notNull(threeInstance, "threeInstance");

        this.threeBindingType = threeBindingType;
        this.threeInstance = threeInstance;
        this.threeInstanceProvider = null;
        this.threeBindingImplementationType = null;
    }

    public AbstractGeneric3GuiceModule(
            final Class<T> oneBindingType,
            final T oneInstance,
            final Class<U> twoBindingType,
            final U twoInstance,
            final Class<V> threeBindingType,
            final V threeInstance,
            final Annotation qualifierBindingAnnotation)
    {
        super(oneBindingType, oneInstance, twoBindingType, twoInstance, qualifierBindingAnnotation);

        AppbaseUtil.notNull(threeBindingType, "threeBindingType");
        AppbaseUtil.notNull(threeInstance, "threeInstance");

        this.threeBindingType = threeBindingType;
        this.threeInstance = threeInstance;
        this.threeInstanceProvider = null;
        this.threeBindingImplementationType = null;
    }

    public AbstractGeneric3GuiceModule(
            final Class<T> oneBindingType,
            final T oneInstance,
            final Class<U> twoBindingType,
            final U twoInstance,
            final Class<V> threeBindingType,
            final V threeInstance,
            final String qualifierBindingName)
    {
        super(oneBindingType, oneInstance, twoBindingType, twoInstance, qualifierBindingName);

        AppbaseUtil.notNull(threeBindingType, "threeBindingType");
        AppbaseUtil.notNull(threeInstance, "threeInstance");

        this.threeBindingType = threeBindingType;
        this.threeInstance = threeInstance;
        this.threeInstanceProvider = null;
        this.threeBindingImplementationType = null;
    }




    public AbstractGeneric3GuiceModule(
            final Class<T> oneBindingType,
            final Provider<? extends T> oneInstanceProvider,
            final Class<U> twoBindingType,
            final Provider<? extends U> twoInstanceProvider,
            final Class<V> threeBindingType,
            final Provider<? extends V> threeInstanceProvider)
    {
        super(oneBindingType, oneInstanceProvider, twoBindingType, twoInstanceProvider);

        AppbaseUtil.notNull(threeBindingType, "threeBindingType");
        AppbaseUtil.notNull(threeInstanceProvider, "threeInstanceProvider");

        this.threeBindingType = threeBindingType;
        this.threeInstanceProvider = threeInstanceProvider;
        this.threeInstance = null;
        this.threeBindingImplementationType = null;
    }

    public AbstractGeneric3GuiceModule(
            final Class<T> oneBindingType,
            final Provider<? extends T> oneInstanceProvider,
            final Class<U> twoBindingType,
            final Provider<? extends U> twoInstanceProvider,
            final Class<V> threeBindingType,
            final Provider<? extends V> threeInstanceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(oneBindingType, oneInstanceProvider, twoBindingType, twoInstanceProvider, qualifierBindingAnnotation);

        AppbaseUtil.notNull(threeBindingType, "threeBindingType");
        AppbaseUtil.notNull(threeInstanceProvider, "threeInstanceProvider");

        this.threeBindingType = threeBindingType;
        this.threeInstanceProvider = threeInstanceProvider;
        this.threeInstance = null;
        this.threeBindingImplementationType = null;
    }

    public AbstractGeneric3GuiceModule(
            final Class<T> oneBindingType,
            final Provider<? extends T> oneInstanceProvider,
            final Class<U> twoBindingType,
            final Provider<? extends U> twoInstanceProvider,
            final Class<V> threeBindingType,
            final Provider<? extends V> threeInstanceProvider,
            final String qualifierBindingName)
    {
        super(oneBindingType, oneInstanceProvider, twoBindingType, twoInstanceProvider, qualifierBindingName);

        AppbaseUtil.notNull(threeBindingType, "threeBindingType");
        AppbaseUtil.notNull(threeInstanceProvider, "threeInstanceProvider");

        this.threeBindingType = threeBindingType;
        this.threeInstanceProvider = threeInstanceProvider;
        this.threeInstance = null;
        this.threeBindingImplementationType = null;
    }




    public AbstractGeneric3GuiceModule(
            final Class<T> oneBindingType,
            final Class<? extends T> oneBindingImplementationType,
            final Class<U> twoBindingType,
            final Class<? extends U> twoBindingImplementationType,
            final Class<V> threeBindingType,
            final Class<? extends V> threeBindingImplementationType)
    {
        super(oneBindingType, oneBindingImplementationType, twoBindingType, twoBindingImplementationType);

        AppbaseUtil.notNull(threeBindingType, "threeBindingType");
        AppbaseUtil.notNull(threeBindingImplementationType, "threeBindingImplementationType");

        this.threeBindingType = threeBindingType;
        this.threeBindingImplementationType = threeBindingImplementationType;
        this.threeInstanceProvider = null;
        this.threeInstance = null;
    }

    public AbstractGeneric3GuiceModule(
            final Class<T> oneBindingType,
            final Class<? extends T> oneBindingImplementationType,
            final Class<U> twoBindingType,
            final Class<? extends U> twoBindingImplementationType,
            final Class<V> threeBindingType,
            final Class<? extends V> threeBindingImplementationType,
            final Annotation qualifierBindingAnnotation)
    {
        super(oneBindingType,
                oneBindingImplementationType,
                twoBindingType,
                twoBindingImplementationType,
                qualifierBindingAnnotation);

        AppbaseUtil.notNull(threeBindingType, "threeBindingType");
        AppbaseUtil.notNull(threeBindingImplementationType, "threeBindingImplementationType");

        this.threeBindingType = threeBindingType;
        this.threeBindingImplementationType = threeBindingImplementationType;
        this.threeInstanceProvider = null;
        this.threeInstance = null;
    }

    public AbstractGeneric3GuiceModule(
            final Class<T> oneBindingType,
            final Class<? extends T> oneBindingImplementationType,
            final Class<U> twoBindingType,
            final Class<? extends U> twoBindingImplementationType,
            final Class<V> threeBindingType,
            final Class<? extends V> threeBindingImplementationType,
            final String qualifierBindingName)
    {
        super(oneBindingType,
                oneBindingImplementationType,
                twoBindingType,
                twoBindingImplementationType,
                qualifierBindingName);

        AppbaseUtil.notNull(threeBindingType, "threeBindingType");
        AppbaseUtil.notNull(threeBindingImplementationType, "threeBindingImplementationType");

        this.threeBindingType = threeBindingType;
        this.threeBindingImplementationType = threeBindingImplementationType;
        this.threeInstanceProvider = null;
        this.threeInstance = null;
    }


    @Override
    protected void configure() {
        super.configure();

        if (threeInstance == null && threeInstanceProvider == null && threeBindingImplementationType == null) {
            bind(threeBindingType).toProvider(Providers.of(null)).asEagerSingleton();
        } else {
            if (qualifierBindingAnnotation != null || qualifierBindingName != null) {
                final Annotation annotation = bindingAnnotation();

                if (threeInstance != null) {
                    bind(threeBindingType).annotatedWith(annotation).toInstance(threeInstance);
                } else if (threeInstanceProvider != null) {
                    bind(threeBindingType).annotatedWith(annotation).toProvider(threeInstanceProvider).asEagerSingleton();
                } else {
                    bind(threeBindingType).annotatedWith(annotation).to(threeBindingImplementationType).asEagerSingleton();
                }
            } else {
                if (threeInstance != null) {
                    bind(threeBindingType).toInstance(threeInstance);
                } else if (threeInstanceProvider != null) {
                    bind(threeBindingType).toProvider(threeInstanceProvider).asEagerSingleton();
                } else {
                    bind(threeBindingType).to(threeBindingImplementationType).asEagerSingleton();
                }
            }
        }
    }
}
