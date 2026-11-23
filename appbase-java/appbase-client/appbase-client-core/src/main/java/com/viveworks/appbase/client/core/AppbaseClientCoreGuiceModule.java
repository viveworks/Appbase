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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.guice.AbstractGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * {@code Guice} module for {@link com.viveworks.appbase.client.core}.
 *
 * <p>
 *     <b>NOTE:</b> This module depends on {@link com.viveworks.appbase.core.di.guice.AppbaseCoreGuiceModule}. Some
 *     client-specific modules also depend on other modules provided by {@code appbase-core}.
 * </p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseClientCoreGuiceModule extends AbstractGuiceModule {
    private final AppbaseClientCore appbaseClientCore;
    private final Provider<? extends AppbaseClientCore> appbaseClientCoreProvider;

    /** Binds the unqualified core service to {@link DefaultAppbaseClientCore}. */
    public AppbaseClientCoreGuiceModule() {
        super(null, null);

        this.appbaseClientCore = null;
        this.appbaseClientCoreProvider = null;
    }

    /** @param qualifierBindingAnnotation annotation qualifying the default core binding */
    public AppbaseClientCoreGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(qualifierBindingAnnotation);

        this.appbaseClientCore = null;
        this.appbaseClientCoreProvider = null;
    }

    /** @param qualifierBindingName name qualifying the default core binding */
    public AppbaseClientCoreGuiceModule(final String qualifierBindingName) {
        super(qualifierBindingName);

        this.appbaseClientCore = null;
        this.appbaseClientCoreProvider = null;
    }

    /** @param appbaseClientCore caller-owned instance bound as the unqualified core service */
    public AppbaseClientCoreGuiceModule(final AppbaseClientCore appbaseClientCore) {
        super(null, null);

        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");

        this.appbaseClientCore = appbaseClientCore;
        this.appbaseClientCoreProvider = null;
    }

    /**
     * @param appbaseClientCore caller-owned core instance
     * @param qualifierBindingAnnotation annotation qualifying that instance
     */
    public AppbaseClientCoreGuiceModule(
            final AppbaseClientCore appbaseClientCore, final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");

        this.appbaseClientCore = appbaseClientCore;
        this.appbaseClientCoreProvider = null;
    }

    /**
     * @param appbaseClientCore caller-owned core instance
     * @param qualifierBindingName name qualifying that instance
     */
    public AppbaseClientCoreGuiceModule(
            final AppbaseClientCore appbaseClientCore,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");

        this.appbaseClientCore = appbaseClientCore;
        this.appbaseClientCoreProvider = null;
    }

    /** @param appbaseClientCoreProvider provider of the core; it is bound as an eager singleton, so it is called when the injector is created */
    public AppbaseClientCoreGuiceModule(
            final Provider<? extends AppbaseClientCore> appbaseClientCoreProvider)
    {
        super(null, null);

        AppbaseUtil.notNull(appbaseClientCoreProvider, "appbaseClientCoreProvider");

        this.appbaseClientCoreProvider = appbaseClientCoreProvider;
        this.appbaseClientCore = null;
    }

    /**
     * @param appbaseClientCoreProvider provider of the core; it is bound as an eager singleton, so it is called when the injector is created
     * @param qualifierBindingAnnotation annotation qualifying the provider binding
     */
    public AppbaseClientCoreGuiceModule(
            final Provider<? extends AppbaseClientCore> appbaseClientCoreProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(appbaseClientCoreProvider, "appbaseClientCoreProvider");

        this.appbaseClientCoreProvider = appbaseClientCoreProvider;
        this.appbaseClientCore = null;
    }

    /**
     * @param appbaseClientCoreProvider provider of the core; it is bound as an eager singleton, so it is called when the injector is created
     * @param qualifierBindingName name qualifying the provider binding
     */
    public AppbaseClientCoreGuiceModule(
            final Provider<? extends AppbaseClientCore> appbaseClientCoreProvider,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(appbaseClientCoreProvider, "appbaseClientCoreProvider");

        this.appbaseClientCoreProvider = appbaseClientCoreProvider;
        this.appbaseClientCore = null;
    }

    /** {@inheritDoc} */
    @Override
    protected void configure() {
        final Annotation bindingAnnotationOrNull = bindingAnnotationOrNull();

        bindType(AppbaseClientCore.class,
                bindingAnnotationOrNull,
                appbaseClientCore,
                appbaseClientCoreProvider,
                DefaultAppbaseClientCore.class);
    }
}
