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

package com.viveworks.appbase.bootstrap.guice;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.microprofile.MicroprofileConfigurationService;
import com.viveworks.appbase.core.service.exception.ExceptionService;
import com.viveworks.appbase.core.service.jmx.JmxService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.lucene.LuceneService;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Guice module that binds {@link AppbaseCoreServices} and each individual core service type.
 *
 * <p>Individual service bindings are registered only when the corresponding getter returns
 * a non-null instance, so this module is safe to use even when a partial core container
 * is supplied (e.g. in unit tests). Inject {@link AppbaseCoreServices} to access any service
 * generically via {@link AppbaseCoreServices#get(Class)}.
 *
 * <p>Usage:
 * <pre>{@code
 * AppbaseCoreServices core = AppbaseCoreServices.builder().build();
 * Injector injector = Guice.createInjector(new AppbaseCoreServicesModule(core));
 * LogService log = injector.getInstance(LogService.class);
 * }</pre>
 */
public final class AppbaseCoreServicesModule extends AbstractModule {

    private final AppbaseCoreServices coreServices;

    /**
     * Constructs a module backed by an already-built {@link AppbaseCoreServices}.
     *
     * @param coreServices the fully-built core services container
     */
    public AppbaseCoreServicesModule(final AppbaseCoreServices coreServices) {
        this.coreServices = Objects.requireNonNull(coreServices, "coreServices");
    }

    @Override
    protected void configure() {
        bind(AppbaseCoreServices.class).toInstance(coreServices);

        bindIfPresent(AppbaseCore.class,                       coreServices.appbaseCore());
        bindIfPresent(LogService.class,                        coreServices.getLogService());
        bindIfPresent(MetricService.class,                     coreServices.getMetricService());
        bindIfPresent(ValidationService.class,                 coreServices.getValidationService());
        bindIfPresent(MediatorService.class,                   coreServices.getMediatorService());
        bindIfPresent(ConfigurationService.class,              coreServices.getConfigurationService());
        bindIfPresent(MicroprofileConfigurationService.class,  coreServices.getMicroprofileConfigurationService());
        bindIfPresent(ServiceRegistry.class,                   coreServices.getServiceRegistry());
        bindIfPresent(MicroprofileServiceRegistry.class,       coreServices.getMicroprofileServiceRegistry());
        bindIfPresent(ExceptionService.class,                  coreServices.getExceptionService());
        bindIfPresent(TracerService.class,                     coreServices.getTracerService());
        bindIfPresent(JmxService.class,                        coreServices.getJmxService());
        bindIfPresent(ReactorService.class,                    coreServices.getReactorService());
        bindIfPresent(LuceneService.class,                     coreServices.getLuceneService());
    }

    /** Binds {@code type} to {@code instance} only when {@code instance} is non-null. */
    private <T> void bindIfPresent(final Class<T> type, @Nullable final T instance) {
        if (instance != null) {
            bind(type).toInstance(instance);
        }
    }
}
