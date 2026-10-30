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

package com.viveworks.appbase.core.di.guice;

import com.viveworks.appbase.core.microprofile.AppbaseCoreMicroprofile;
import com.viveworks.appbase.core.microprofile.DefaultAppbaseCoreMicroprofile;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationServiceImpl;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistryImpl;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.guice.AbstractGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for services provided by <code>core</code> ({@link AppbaseCoreMicroprofile}).
 *
 * <p>
 *     <b>NOTE:</b> Although, this module does not depend on {@link AppbaseCoreGuiceModule} being
 *     injected before, {@link AppbaseCoreGuiceModule} injects
 *     {@link com.viveworks.appbase.core.AppbaseCore} which is required for most other parts and must be injected before
 *     this module.
 * </p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseCoreMicroprofileGuiceModule extends AbstractGuiceModule {
    private final MicroprofileConfigurationService microprofileConfigurationService;
    private final Provider<? extends MicroprofileConfigurationService> microprofileConfigurationServiceProvider;
    private final MicroprofileServiceRegistry microprofileServiceRegistry;
    private final Provider<? extends MicroprofileServiceRegistry> microprofileServiceRegistryProvider;

    /**
     * Selects the default implementations for both MicroProfile services. The installed keys remain unqualified.
     */
    public AppbaseCoreMicroprofileGuiceModule() {
        super(null, null);

        this.microprofileConfigurationService = null;
        this.microprofileConfigurationServiceProvider = null;
        this.microprofileServiceRegistry = null;
        this.microprofileServiceRegistryProvider = null;
    }

    /**
     * Selects the default implementations for both MicroProfile services. Each installed key carries the annotation
     * qualifier.
     *
     * @param qualifierBindingAnnotation annotation qualifier applied to all bindings
     */
    public AppbaseCoreMicroprofileGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(qualifierBindingAnnotation);

        this.microprofileConfigurationService = null;
        this.microprofileConfigurationServiceProvider = null;
        this.microprofileServiceRegistry = null;
        this.microprofileServiceRegistryProvider = null;
    }

    /**
     * Selects the default implementations for both MicroProfile services. Each installed key carries the named qualifier.
     *
     * @param qualifierBindingName name qualifier applied to all bindings
     */
    public AppbaseCoreMicroprofileGuiceModule(final String qualifierBindingName) {
        super(qualifierBindingName);

        this.microprofileConfigurationService = null;
        this.microprofileConfigurationServiceProvider = null;
        this.microprofileServiceRegistry = null;
        this.microprofileServiceRegistryProvider = null;
    }

    /**
     * Binds the supplied MicroProfile service instances. The installed keys remain unqualified.
     *
     * @param microprofileConfigurationService configuration-service instance to bind
     * @param microprofileServiceRegistry service-registry instance to bind
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either service instance is {@code null}
     */
    public AppbaseCoreMicroprofileGuiceModule(
            final MicroprofileConfigurationService microprofileConfigurationService,
            final MicroprofileServiceRegistry microprofileServiceRegistry)
    {
        super(null, null);

        AppbaseUtil.notNull(microprofileConfigurationService, "microprofileConfigurationService");
        AppbaseUtil.notNull(microprofileServiceRegistry, "microprofileServiceRegistry");

        this.microprofileConfigurationService = microprofileConfigurationService;
        this.microprofileConfigurationServiceProvider = null;
        this.microprofileServiceRegistry = microprofileServiceRegistry;
        this.microprofileServiceRegistryProvider = null;
    }

    /**
     * Binds the supplied MicroProfile service instances. Each installed key carries the annotation qualifier.
     *
     * @param microprofileConfigurationService configuration-service instance to bind
     * @param microprofileServiceRegistry service-registry instance to bind
     * @param qualifierBindingAnnotation annotation qualifier applied to all bindings
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either service instance is {@code null}
     */
    public AppbaseCoreMicroprofileGuiceModule(
            final MicroprofileConfigurationService microprofileConfigurationService,
            final MicroprofileServiceRegistry microprofileServiceRegistry,
            final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(microprofileConfigurationService, "microprofileConfigurationService");
        AppbaseUtil.notNull(microprofileServiceRegistry, "microprofileServiceRegistry");

        this.microprofileConfigurationService = microprofileConfigurationService;
        this.microprofileConfigurationServiceProvider = null;
        this.microprofileServiceRegistry = microprofileServiceRegistry;
        this.microprofileServiceRegistryProvider = null;
    }

    /**
     * Binds the supplied MicroProfile service instances. Each installed key carries the named qualifier.
     *
     * @param microprofileConfigurationService configuration-service instance to bind
     * @param microprofileServiceRegistry service-registry instance to bind
     * @param qualifierBindingName name qualifier applied to all bindings
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either service instance is {@code null}
     */
    public AppbaseCoreMicroprofileGuiceModule(
            final MicroprofileConfigurationService microprofileConfigurationService,
            final MicroprofileServiceRegistry microprofileServiceRegistry,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(microprofileConfigurationService, "microprofileConfigurationService");
        AppbaseUtil.notNull(microprofileServiceRegistry, "microprofileServiceRegistry");

        this.microprofileConfigurationService = microprofileConfigurationService;
        this.microprofileConfigurationServiceProvider = null;
        this.microprofileServiceRegistry = microprofileServiceRegistry;
        this.microprofileServiceRegistryProvider = null;
    }

    /**
     * Uses the supplied providers for the two eager MicroProfile service singletons. The installed keys remain unqualified.
     *
     * @param microprofileConfigurationServiceProvider provider for the configuration-service singleton
     * @param microprofileServiceRegistryProvider provider for the service-registry singleton
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either provider is {@code null}
     */
    public AppbaseCoreMicroprofileGuiceModule(
            final Provider<? extends MicroprofileConfigurationService> microprofileConfigurationServiceProvider,
            final Provider<? extends MicroprofileServiceRegistry> microprofileServiceRegistryProvider)
    {
        super(null, null);

        AppbaseUtil.notNull(microprofileConfigurationServiceProvider, "microprofileConfigurationServiceProvider");
        AppbaseUtil.notNull(microprofileServiceRegistryProvider, "microprofileServiceRegistryProvider");

        this.microprofileConfigurationServiceProvider = microprofileConfigurationServiceProvider;
        this.microprofileServiceRegistryProvider = microprofileServiceRegistryProvider;
        this.microprofileConfigurationService = null;
        this.microprofileServiceRegistry = null;
    }

    /**
     * Uses the supplied providers for the two eager MicroProfile service singletons. Each installed key carries the
     * annotation qualifier.
     *
     * @param microprofileConfigurationServiceProvider provider for the configuration-service singleton
     * @param microprofileServiceRegistryProvider provider for the service-registry singleton
     * @param qualifierBindingAnnotation annotation qualifier applied to all bindings
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either provider is {@code null}
     */
    public AppbaseCoreMicroprofileGuiceModule(
            final Provider<? extends MicroprofileConfigurationService> microprofileConfigurationServiceProvider,
            final Provider<? extends MicroprofileServiceRegistry> microprofileServiceRegistryProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(microprofileConfigurationServiceProvider, "microprofileConfigurationServiceProvider");
        AppbaseUtil.notNull(microprofileServiceRegistryProvider, "microprofileServiceRegistryProvider");

        this.microprofileConfigurationServiceProvider = microprofileConfigurationServiceProvider;
        this.microprofileServiceRegistryProvider = microprofileServiceRegistryProvider;
        this.microprofileConfigurationService = null;
        this.microprofileServiceRegistry = null;
    }

    /**
     * Uses the supplied providers for the two eager MicroProfile service singletons. Each installed key carries the named
     * qualifier.
     *
     * @param microprofileConfigurationServiceProvider provider for the configuration-service singleton
     * @param microprofileServiceRegistryProvider provider for the service-registry singleton
     * @param qualifierBindingName name qualifier applied to all bindings
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if either provider is {@code null}
     */
    public AppbaseCoreMicroprofileGuiceModule(
            final Provider<? extends MicroprofileConfigurationService> microprofileConfigurationServiceProvider,
            final Provider<? extends MicroprofileServiceRegistry> microprofileServiceRegistryProvider,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(microprofileConfigurationServiceProvider, "microprofileConfigurationServiceProvider");
        AppbaseUtil.notNull(microprofileServiceRegistryProvider, "microprofileServiceRegistryProvider");

        this.microprofileConfigurationServiceProvider = microprofileConfigurationServiceProvider;
        this.microprofileServiceRegistryProvider = microprofileServiceRegistryProvider;
        this.microprofileConfigurationService = null;
        this.microprofileServiceRegistry = null;
    }

    @Override
    protected void configure() {
        if (qualifierBindingAnnotation != null || qualifierBindingName != null) {
            final Annotation annotation = bindingAnnotation();

            if (microprofileConfigurationService != null) {
                bind(MicroprofileConfigurationService.class)
                        .annotatedWith(annotation)
                        .toInstance(microprofileConfigurationService);
            } else {
                if (microprofileConfigurationServiceProvider != null) {
                    bind(MicroprofileConfigurationService.class)
                            .annotatedWith(annotation)
                            .toProvider(microprofileConfigurationServiceProvider)
                            .asEagerSingleton();
                } else {
                    bind(MicroprofileConfigurationService.class)
                            .annotatedWith(annotation)
                            .to(MicroprofileConfigurationServiceImpl.class)
                            .asEagerSingleton();
                }
            }

            if (microprofileServiceRegistry != null) {
                bind(MicroprofileServiceRegistry.class)
                        .annotatedWith(annotation)
                        .toInstance(microprofileServiceRegistry);
            } else {
                if (microprofileServiceRegistryProvider != null) {
                    bind(MicroprofileServiceRegistry.class)
                            .annotatedWith(annotation)
                            .toProvider(microprofileServiceRegistryProvider)
                            .asEagerSingleton();
                } else {
                    bind(MicroprofileServiceRegistry.class)
                            .annotatedWith(annotation)
                            .to(MicroprofileServiceRegistryImpl.class)
                            .asEagerSingleton();
                }
            }

            bind(AppbaseCoreMicroprofile.class)
                    .annotatedWith(annotation)
                    .to(DefaultAppbaseCoreMicroprofile.class)
                    .asEagerSingleton();
        } else {
            if (microprofileConfigurationService != null) {
                bind(MicroprofileConfigurationService.class).toInstance(microprofileConfigurationService);
            } else {
                if (microprofileConfigurationServiceProvider != null) {
                    bind(MicroprofileConfigurationService.class)
                            .toProvider(microprofileConfigurationServiceProvider)
                            .asEagerSingleton();
                } else {
                    bind(MicroprofileConfigurationService.class)
                            .to(MicroprofileConfigurationServiceImpl.class)
                            .asEagerSingleton();
                }
            }

            if (microprofileServiceRegistry != null) {
                bind(MicroprofileServiceRegistry.class).toInstance(microprofileServiceRegistry);
            } else {
                if (microprofileServiceRegistryProvider != null) {
                    bind(MicroprofileServiceRegistry.class)
                            .toProvider(microprofileServiceRegistryProvider)
                            .asEagerSingleton();
                } else {
                    bind(MicroprofileServiceRegistry.class)
                            .to(MicroprofileServiceRegistryImpl.class)
                            .asEagerSingleton();
                }
            }

            bind(AppbaseCoreMicroprofile.class).to(DefaultAppbaseCoreMicroprofile.class).asEagerSingleton();
        }
    }
}
