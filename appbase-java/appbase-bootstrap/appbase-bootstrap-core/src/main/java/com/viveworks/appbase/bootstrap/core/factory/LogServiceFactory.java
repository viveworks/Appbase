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

package com.viveworks.appbase.bootstrap.core.factory;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.core.CoreServiceOrder;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.event.delegator.DefaultMediatorEventDelegatorRegistry;

import java.util.Objects;

/**
 * Factory that creates the {@link LogService} using a {@link DefaultMediatorEventDelegatorRegistry}.
 *
 * <p>The registry is created eagerly at construction time so that {@link MediatorServiceFactory}
 * can share the same instance — both services must use the same registry for log events to
 * flow through the mediator bus.
 *
 * @see MediatorServiceFactory
 */
public final class LogServiceFactory implements AppbaseCoreServiceFactory<LogService> {

    private final DefaultLogServiceConfiguration config;
    private final DefaultMediatorEventDelegatorRegistry registry;

    /**
     * Creates the factory with the given log configuration.
     * The shared {@link DefaultMediatorEventDelegatorRegistry} is created immediately.
     *
     * @param config log service configuration (must not be null)
     */
    public LogServiceFactory(DefaultLogServiceConfiguration config) {
        this.config   = Objects.requireNonNull(config, "config");
        this.registry = new DefaultMediatorEventDelegatorRegistry();
    }

    /**
     * Returns the shared mediator event delegator registry.
     * Pass this to {@link MediatorServiceFactory} so both services share the same registry.
     *
     * @return the registry created at construction time (never null)
     */
    DefaultMediatorEventDelegatorRegistry getMediatorEventDelegatorRegistry() {
        return registry;
    }

    @Override
    public Class<LogService> serviceType() {
        return LogService.class;
    }

    @Override
    public int initializationOrder() {
        return CoreServiceOrder.LOG.order();
    }

    @Override
    public LogService create(AppbaseCoreServices partial) throws AppbaseException {
        return new DefaultLogService(registry, config);
    }
}
