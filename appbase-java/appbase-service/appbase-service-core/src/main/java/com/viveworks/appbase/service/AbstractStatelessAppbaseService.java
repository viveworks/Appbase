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
package com.viveworks.appbase.service;

import com.viveworks.appbase.core.service.AbstractAppbaseService;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Base for services whose configuration reference does not change after construction. Construction
 * validates the registry and configuration, invokes subclass registration, and marks the service
 * {@link ServiceStatus#STARTED}; reload and shutdown therefore perform no additional work.
 *
 * @param <C> concrete immutable-by-reference configuration exposed by the service
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public abstract class AbstractStatelessAppbaseService<C extends AppbaseConfiguration>
        extends AbstractAppbaseService
{
    /** Configuration reference returned throughout this service's lifetime. */
    protected final C serviceConfiguration;

    /** Registry used by {@link #registerService()} during construction. */
    protected final ServiceRegistry serviceRegistry;

    /**
     * Initializes and immediately registers a started service.
     *
     * @param logService logging collaborator passed to the core service base
     * @param metricService metrics collaborator passed to the core service base
     * @param mediatorService mediator collaborator passed to the core service base
     * @param serviceRegistry registry in which the subclass registers itself
     * @param serviceConfiguration configuration retained for the lifetime of this instance
     */
    public AbstractStatelessAppbaseService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final C serviceConfiguration)
    {
        super(logService, metricService, mediatorService);

        AppbaseUtil.notNull(serviceConfiguration, "serviceConfiguration");
        AppbaseUtil.notNull(serviceRegistry, "serviceRegistry");

        this.serviceConfiguration = serviceConfiguration;
        this.serviceRegistry = serviceRegistry;
        registerService();
        updateStatus(ServiceStatus.STARTED);
    }

    /**
     * Registers this instance with {@link #serviceRegistry}. Called from the base constructor before
     * the status changes to {@link ServiceStatus#STARTED}, so implementations must not depend on
     * subclass initialization performed after {@code super(...)} returns.
     */
    protected abstract void registerService();

    /**
     * Intentionally ignores reload requests because this base retains its construction-time configuration.
     * @param configuration replacement configuration, which is not applied
     * @param <CC> replacement configuration type
     */
    @Override
    protected <CC extends AppbaseConfiguration> void reloadImpl(
            final CC configuration) throws ServiceReloadException
    {
        // no-op
    }

    /** Performs no shutdown work; subclasses using resources must provide their own lifecycle base. */
    @Override
    protected void shutdownImpl() {
        // no-op
    }

    /**
     * Returns the configuration reference supplied at construction; reload does not replace it.
     *
     * @return this service's retained configuration
     */
    @Override
    @SuppressWarnings("unchecked")
    public C getConfiguration() {
        return serviceConfiguration;
    }
}
