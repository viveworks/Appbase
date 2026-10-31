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

import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.DefaultAppbaseCore;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.guice.AbstractGuiceModule;
import com.viveworks.appbase.core.util.di.guice.DefaultGuiceInjectorService;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.lucene.LuceneService;

import java.lang.annotation.Annotation;

/**
 * Guice module for services provided by <code>core</code> ({@link AppbaseCore}).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseCoreGuiceModule extends AbstractGuiceModule {
    private final AppbaseCore appbaseCore;
    private final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap;
    private final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration;

    /**
     * Builds the default core whose services this module installs. Guice keys remain unqualified.
     */
    public AppbaseCoreGuiceModule() {
        super(null, null);

        this.appbaseCore = new DefaultAppbaseCore();
        this.appbaseCoreBootstrap = null;
        this.appbaseCoreBootstrapConfiguration = null;
    }

    /**
     * Builds the default core whose services this module installs. Guice keys carry the annotation qualifier.
     *
     * @param qualifierBindingAnnotation annotation qualifier applied to all bindings
     */
    public AppbaseCoreGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(qualifierBindingAnnotation);

        this.appbaseCore = new DefaultAppbaseCore();
        this.appbaseCoreBootstrap = null;
        this.appbaseCoreBootstrapConfiguration = null;
    }

    /**
     * Builds the default core whose services this module installs. Guice keys carry the named qualifier.
     *
     * @param qualifierBindingName name qualifier applied to all bindings
     */
    public AppbaseCoreGuiceModule(final String qualifierBindingName) {
        super(qualifierBindingName);

        this.appbaseCore = new DefaultAppbaseCore();
        this.appbaseCoreBootstrap = null;
        this.appbaseCoreBootstrapConfiguration = null;
    }

    /**
     * Builds a core through the supplied bootstrap strategy. Guice keys remain unqualified.
     *
     * @param appbaseCoreBootstrap bootstrap strategy used to construct the core
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCoreBootstrap} is {@code null}
     */
    public AppbaseCoreGuiceModule(final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap) {
        super(null, null);

        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");

        this.appbaseCore = new DefaultAppbaseCore(appbaseCoreBootstrap);
        this.appbaseCoreBootstrap = appbaseCoreBootstrap;
        this.appbaseCoreBootstrapConfiguration = null;
    }

    /**
     * Builds a core through the supplied bootstrap strategy. Guice keys carry the annotation qualifier.
     *
     * @param appbaseCoreBootstrap bootstrap strategy used to construct the core
     * @param qualifierBindingAnnotation annotation qualifier applied to all bindings
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCoreBootstrap} is {@code null}
     */
    public AppbaseCoreGuiceModule(
            final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap,
            final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");

        this.appbaseCore = new DefaultAppbaseCore(appbaseCoreBootstrap);
        this.appbaseCoreBootstrap = appbaseCoreBootstrap;
        this.appbaseCoreBootstrapConfiguration = null;
    }

    /**
     * Builds a core through the supplied bootstrap strategy. Guice keys carry the named qualifier.
     *
     * @param appbaseCoreBootstrap bootstrap strategy used to construct the core
     * @param qualifierBindingName name qualifier applied to all bindings
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCoreBootstrap} is {@code null}
     */
    public AppbaseCoreGuiceModule(
            final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap,
            final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");

        this.appbaseCore = new DefaultAppbaseCore(appbaseCoreBootstrap);
        this.appbaseCoreBootstrap = appbaseCoreBootstrap;
        this.appbaseCoreBootstrapConfiguration = null;
    }

    /**
     * Builds a core using an explicit bootstrap configuration. Guice keys remain unqualified.
     *
     * @param appbaseCoreBootstrap bootstrap strategy used to construct the core
     * @param appbaseCoreBootstrapConfiguration configuration supplied to the bootstrap strategy
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCoreBootstrap} or
     *         {@code appbaseCoreBootstrapConfiguration} is {@code null}
     */
    public AppbaseCoreGuiceModule(final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap,
                                  final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration)
    {
        super(null, null);

        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");
        AppbaseUtil.notNull(appbaseCoreBootstrapConfiguration, "appbaseCoreBootstrapConfiguration");

        this.appbaseCoreBootstrap = appbaseCoreBootstrap;
        this.appbaseCoreBootstrapConfiguration = appbaseCoreBootstrapConfiguration;
        this.appbaseCore = new DefaultAppbaseCore(appbaseCoreBootstrap, appbaseCoreBootstrapConfiguration);
    }

    /**
     * Builds a core using an explicit bootstrap configuration. Guice keys carry the annotation qualifier.
     *
     * @param appbaseCoreBootstrap bootstrap strategy used to construct the core
     * @param appbaseCoreBootstrapConfiguration configuration supplied to the bootstrap strategy
     * @param qualifierBindingAnnotation annotation qualifier applied to all bindings
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCoreBootstrap} or
     *         {@code appbaseCoreBootstrapConfiguration} is {@code null}
     */
    public AppbaseCoreGuiceModule(final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap,
                                  final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration,
                                  final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");
        AppbaseUtil.notNull(appbaseCoreBootstrapConfiguration, "appbaseCoreBootstrapConfiguration");

        this.appbaseCoreBootstrap = appbaseCoreBootstrap;
        this.appbaseCoreBootstrapConfiguration = appbaseCoreBootstrapConfiguration;
        this.appbaseCore = new DefaultAppbaseCore(appbaseCoreBootstrap, appbaseCoreBootstrapConfiguration);
    }

    /**
     * Builds a core using an explicit bootstrap configuration. Guice keys carry the named qualifier.
     *
     * @param appbaseCoreBootstrap bootstrap strategy used to construct the core
     * @param appbaseCoreBootstrapConfiguration configuration supplied to the bootstrap strategy
     * @param qualifierBindingName name qualifier applied to all bindings
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCoreBootstrap} or
     *         {@code appbaseCoreBootstrapConfiguration} is {@code null}
     */
    public AppbaseCoreGuiceModule(final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap,
                                  final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration,
                                  final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");
        AppbaseUtil.notNull(appbaseCoreBootstrapConfiguration, "appbaseCoreBootstrapConfiguration");

        this.appbaseCoreBootstrap = appbaseCoreBootstrap;
        this.appbaseCoreBootstrapConfiguration = appbaseCoreBootstrapConfiguration;
        this.appbaseCore = new DefaultAppbaseCore(appbaseCoreBootstrap, appbaseCoreBootstrapConfiguration);
    }

    /**
     * Binds the service objects exposed by an existing core. Guice keys remain unqualified.
     *
     * @param appbaseCore existing core that provides the bound services
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCore} is {@code null}
     */
    public AppbaseCoreGuiceModule(final AppbaseCore appbaseCore) {
        super(null, null);

        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        this.appbaseCore = appbaseCore;
        this.appbaseCoreBootstrap = null;
        this.appbaseCoreBootstrapConfiguration = null;
    }

    /**
     * Binds the service objects exposed by an existing core. Guice keys carry the annotation qualifier.
     *
     * @param appbaseCore existing core that provides the bound services
     * @param qualifierBindingAnnotation annotation qualifier applied to all bindings
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCore} is {@code null}
     */
    public AppbaseCoreGuiceModule(final AppbaseCore appbaseCore,
                                  final Annotation qualifierBindingAnnotation)
    {
        super(qualifierBindingAnnotation);

        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        this.appbaseCore = appbaseCore;
        this.appbaseCoreBootstrap = null;
        this.appbaseCoreBootstrapConfiguration = null;
    }

    /**
     * Binds the service objects exposed by an existing core. Guice keys carry the named qualifier.
     *
     * @param appbaseCore existing core that provides the bound services
     * @param qualifierBindingName name qualifier applied to all bindings
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if {@code appbaseCore} is {@code null}
     */
    public AppbaseCoreGuiceModule(final AppbaseCore appbaseCore,
                                  final String qualifierBindingName)
    {
        super(qualifierBindingName);

        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        this.appbaseCore = appbaseCore;
        this.appbaseCoreBootstrap = null;
        this.appbaseCoreBootstrapConfiguration = null;
    }

    @Override
    @SuppressWarnings("checkstyle:WhitespaceAround")
    protected void configure() {
        if (qualifierBindingAnnotation != null || qualifierBindingName != null) {
            final Annotation annotation = bindingAnnotation();

            bind(LogService.class).annotatedWith(annotation).toInstance(appbaseCore.getLogService());
            bind(MetricService.class).annotatedWith(annotation).toInstance(appbaseCore.getMetricService());
            bind(ValidationService.class).annotatedWith(annotation).toInstance(appbaseCore.getValidationService());
            bind(MediatorService.class).annotatedWith(annotation).toInstance(appbaseCore.getMediatorService());
            bind(ConfigurationService.class).annotatedWith(annotation).toInstance(appbaseCore.getConfigurationService());
            bind(ServiceRegistry.class).annotatedWith(annotation).toInstance(appbaseCore.getServiceRegistry());
            bind(ExceptionService.class).annotatedWith(annotation).toInstance(appbaseCore.getExceptionService());
            bind(TracerService.class).annotatedWith(annotation).toInstance(appbaseCore.getTracerService());
            bind(ReactorService.class).annotatedWith(annotation).toInstance(appbaseCore.getReactorService());
            bind(JmxService.class).annotatedWith(annotation).toInstance(appbaseCore.getJmxService());

            final LuceneService luceneService = appbaseCore.getLuceneService();
            if (luceneService != null) {
                bind(LuceneService.class).annotatedWith(annotation).toInstance(luceneService);
            } else {
                bind(LuceneService.class).annotatedWith(annotation).toProvider(Providers.of(null)).asEagerSingleton();
            }

            if (appbaseCoreBootstrap != null) {
                bind(new TypeLiteral<AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration>>() {})
                        .annotatedWith(annotation)
                        .toInstance(appbaseCoreBootstrap);
            } else {
                bind(new TypeLiteral<AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration>>() {})
                        .annotatedWith(annotation)
                        .toProvider(Providers.of(null))
                        .asEagerSingleton();
            }

            if (appbaseCoreBootstrapConfiguration != null) {
                bind(AppbaseCoreBootstrapConfiguration.class)
                        .annotatedWith(annotation)
                        .toInstance(appbaseCoreBootstrapConfiguration);
                bind(AppbaseCoreBootstrapConfiguration.class)
                        .toInstance(appbaseCoreBootstrapConfiguration);
            } else {
                bind(AppbaseCoreBootstrapConfiguration.class)
                        .toProvider(Providers.of(null))
                        .asEagerSingleton();
            }

            bind(GuiceInjectorService.class)
                    .annotatedWith(annotation)
                    .to(DefaultGuiceInjectorService.class)
                    .asEagerSingleton();
            bind(AppbaseCore.class)
                    .annotatedWith(annotation)
                    .to(DefaultAppbaseCore.class)
                    .asEagerSingleton();
        } else {
            bind(LogService.class).toInstance(appbaseCore.getLogService());
            bind(MetricService.class).toInstance(appbaseCore.getMetricService());
            bind(ValidationService.class).toInstance(appbaseCore.getValidationService());
            bind(MediatorService.class).toInstance(appbaseCore.getMediatorService());
            bind(ConfigurationService.class).toInstance(appbaseCore.getConfigurationService());
            bind(ServiceRegistry.class).toInstance(appbaseCore.getServiceRegistry());
            bind(ExceptionService.class).toInstance(appbaseCore.getExceptionService());
            bind(TracerService.class).toInstance(appbaseCore.getTracerService());
            bind(ReactorService.class).toInstance(appbaseCore.getReactorService());
            bind(JmxService.class).toInstance(appbaseCore.getJmxService());

            final LuceneService luceneService = appbaseCore.getLuceneService();
            if (luceneService != null) {
                bind(LuceneService.class).toInstance(luceneService);
            } else {
                bind(LuceneService.class).toProvider(Providers.of(null)).asEagerSingleton();
            }

            if (appbaseCoreBootstrap != null) {
                bind(new TypeLiteral<AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration>>() {})
                        .toInstance(appbaseCoreBootstrap);
            } else {
                bind(new TypeLiteral<AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration>>() {})
                        .toProvider(Providers.of(null))
                        .asEagerSingleton();
            }

            if (appbaseCoreBootstrapConfiguration != null) {
                bind(AppbaseCoreBootstrapConfiguration.class).toInstance(appbaseCoreBootstrapConfiguration);
            } else {
                bind(AppbaseCoreBootstrapConfiguration.class).toProvider(Providers.of(null)).asEagerSingleton();
            }

            bind(GuiceInjectorService.class).to(DefaultGuiceInjectorService.class).asEagerSingleton();
            bind(AppbaseCore.class).to(DefaultAppbaseCore.class).asEagerSingleton();
        }
    }

    /**
     * Returns the core whose service instances are installed by this module.
     *
     * @return the constructed or caller-supplied core
     */
    public AppbaseCore getAppbaseCore() {
        return appbaseCore;
    }
}
