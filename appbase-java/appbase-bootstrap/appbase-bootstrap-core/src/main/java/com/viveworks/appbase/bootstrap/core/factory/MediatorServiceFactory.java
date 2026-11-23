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

package com.viveworks.appbase.bootstrap.core.factory;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.core.CoreServiceOrder;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventFactory;
import com.viveworks.appbase.core.service.mediator.event.delegator.DefaultMediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.event.delegator.DisruptorMediatorEventHandlerDelegator;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.disruptor.DefaultDisruptorService;
import com.viveworks.appbase.core.util.disruptor.DefaultExceptionHandler;
import com.viveworks.appbase.core.util.disruptor.DisruptorService;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Factory that creates the {@link MediatorService}.
 *
 * <p>The mediator requires a {@link DefaultMediatorEventDelegatorRegistry} that is also shared
 * with the {@link LogService}. This registry is retrieved from the {@link LogServiceFactory}
 * that was used to create the log service, so both services operate on the same instance.
 *
 * <p>A {@link DefaultDisruptorService} is created inside {@link #create} using default
 * {@link DefaultDisruptorServiceConfiguration} values (1 handler thread, 1024-slot ring buffer).
 * Supply a custom factory via
 * {@link com.viveworks.appbase.bootstrap.core.AppbaseCoreServices.Builder#with} to override.
 *
 * @see LogServiceFactory
 */
public final class MediatorServiceFactory implements AppbaseCoreServiceFactory<MediatorService> {

    private final LogServiceFactory logFactory;
    @Nullable
    private final DefaultMediatorServiceConfiguration config;

    /**
     * Creates the factory.
     *
     * @param logFactory the log service factory whose shared registry this mediator will use (must not be null)
     * @param config     optional mediator configuration; {@code null} uses service defaults
     */
    public MediatorServiceFactory(LogServiceFactory logFactory,
                                  @Nullable DefaultMediatorServiceConfiguration config) {
        this.logFactory = Objects.requireNonNull(logFactory, "logFactory");
        this.config     = config;
    }

    @Override
    public Class<MediatorService> serviceType() {
        return MediatorService.class;
    }

    @Override
    public int initializationOrder() {
        return CoreServiceOrder.MEDIATOR.order();
    }

    @Override
    public MediatorService create(AppbaseCoreServices partial) throws AppbaseException {
        LogService      log        = partial.get(LogService.class);
        MetricService   metric     = partial.get(MetricService.class);
        ValidationService validation = partial.get(ValidationService.class);

        DefaultMediatorEventDelegatorRegistry registry = logFactory.getMediatorEventDelegatorRegistry();

        DefaultDisruptorServiceConfiguration disruptorConfig =
                DefaultDisruptorServiceConfiguration.builder().build();
        int handlerCount = disruptorConfig.getEventHandlerCount();
        DisruptorMediatorEventHandlerDelegator[] handlers =
                new DisruptorMediatorEventHandlerDelegator[handlerCount];
        for (int i = 0; i < handlerCount; i++) {
            handlers[i] = new DisruptorMediatorEventHandlerDelegator(registry);
        }

        DisruptorService<MediatorEvent> disruptor = new DefaultDisruptorService<>(
                disruptorConfig,
                new MediatorEventFactory(),
                new DefaultExceptionHandler<>(),
                handlers);

        return new DefaultMediatorService(log, metric, validation, disruptor, registry, config);
    }
}
