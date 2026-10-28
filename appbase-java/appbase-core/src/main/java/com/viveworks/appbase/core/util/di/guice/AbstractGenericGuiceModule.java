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
 * Abstract generic {@code Guice} module to inject one type with all options.
 *
 * @author Navaneeth Mysore G
 * @param <T> Binding type
 * @since 1.0.0
 */
public abstract class AbstractGenericGuiceModule<T> extends AbstractGuiceModule {
    private final Class<T> bindingType;
    private final T instance;
    private final Provider<? extends T> instanceProvider;
    private final Class<? extends T> implementationType;

    protected AbstractGenericGuiceModule(final Class<T> bindingType) {
        super(null, null);

        AppbaseUtil.notNull(bindingType, "bindingType");

        this.bindingType = bindingType;
        this.instance = null;
        this.instanceProvider = null;
        this.implementationType = null;
    }

    public AbstractGenericGuiceModule(final Class<T> bindingType, final T instance) {
        super(null, null);

        AppbaseUtil.notNull(bindingType, "bindingType");
        AppbaseUtil.notNull(instance, "instance");

        this.bindingType = bindingType;
        this.instance = instance;
        this.instanceProvider = null;
        this.implementationType = null;
    }

    public AbstractGenericGuiceModule(
            final Class<T> bindingType,
            final T instance,
            final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(bindingType, "bindingType");
        AppbaseUtil.notNull(instance, "instance");

        this.bindingType = bindingType;
        this.instance = instance;
        this.instanceProvider = null;
        this.implementationType = null;
    }

    public AbstractGenericGuiceModule(
            final Class<T> bindingType,
            final T instance,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(bindingType, "bindingType");
        AppbaseUtil.notNull(instance, "instance");

        this.bindingType = bindingType;
        this.instance = instance;
        this.instanceProvider = null;
        this.implementationType = null;
    }




    public AbstractGenericGuiceModule(
            final Class<T> bindingType,
            final Provider<? extends T> instanceProvider)
    {
        super(null, null);

        AppbaseUtil.notNull(bindingType, "bindingType");
        AppbaseUtil.notNull(instanceProvider, "instanceProvider");

        this.bindingType = bindingType;
        this.instanceProvider = instanceProvider;
        this.instance = null;
        this.implementationType = null;
    }

    public AbstractGenericGuiceModule(
            final Class<T> bindingType,
            final Provider<? extends T> instanceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(bindingType, "bindingType");
        AppbaseUtil.notNull(instanceProvider, "instanceProvider");

        this.bindingType = bindingType;
        this.instanceProvider = instanceProvider;
        this.instance = null;
        this.implementationType = null;
    }

    public AbstractGenericGuiceModule(
            final Class<T> bindingType,
            final Provider<? extends T> instanceProvider,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(bindingType, "bindingType");
        AppbaseUtil.notNull(instanceProvider, "instanceProvider");

        this.bindingType = bindingType;
        this.instanceProvider = instanceProvider;
        this.instance = null;
        this.implementationType = null;
    }




    public AbstractGenericGuiceModule(
            final Class<T> bindingType,
            final Class<? extends T> implementationType)
    {
        super(null, null);

        AppbaseUtil.notNull(bindingType, "bindingType");
        AppbaseUtil.notNull(implementationType, "implementationType");

        this.bindingType = bindingType;
        this.implementationType = implementationType;
        this.instanceProvider = null;
        this.instance = null;
    }

    public AbstractGenericGuiceModule(
            final Class<T> bindingType,
            final Class<? extends T> implementationType,
            final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(bindingType, "bindingType");
        AppbaseUtil.notNull(implementationType, "implementationType");

        this.bindingType = bindingType;
        this.implementationType = implementationType;
        this.instanceProvider = null;
        this.instance = null;
    }

    public AbstractGenericGuiceModule(
            final Class<T> bindingType,
            final Class<? extends T> implementationType,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(bindingType, "bindingType");
        AppbaseUtil.notNull(implementationType, "implementationType");

        this.bindingType = bindingType;
        this.implementationType = implementationType;
        this.instanceProvider = null;
        this.instance = null;
    }




    @Override
    protected void configure() {
        if (instance == null && instanceProvider == null && implementationType == null) {
            bind(bindingType).toProvider(Providers.of(null)).asEagerSingleton();
            return;
        }

        if (qualifierBindingAnnotation != null || qualifierBindingName != null) {
            final Annotation annotation = bindingAnnotation();

            if (instance != null) {
                bind(bindingType).annotatedWith(annotation).toInstance(instance);
            } else if (instanceProvider != null) {
                bind(bindingType).annotatedWith(annotation).toProvider(instanceProvider).asEagerSingleton();
            } else {
                bind(bindingType).annotatedWith(annotation).to(implementationType).asEagerSingleton();
            }
        } else {
            if (instance != null) {
                bind(bindingType).toInstance(instance);
            } else if (instanceProvider != null) {
                bind(bindingType).toProvider(instanceProvider).asEagerSingleton();
            } else {
                bind(bindingType).to(implementationType).asEagerSingleton();
            }
        }
    }
}
