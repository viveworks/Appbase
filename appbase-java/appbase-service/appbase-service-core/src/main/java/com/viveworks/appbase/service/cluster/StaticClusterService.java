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

package com.viveworks.appbase.service.cluster;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;

/**
 * Stateless cluster service that retains a fixed home data center from immutable configuration.
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class StaticClusterService
        extends AbstractStatelessAppbaseService<StaticClusterServiceConfiguration>
        implements ClusterService
{
    private final String homeDataCenter;

    /**
     * Initializes and registers a cluster service using the configured home data center.
     * @param logService logging service
     * @param metricService metrics service
     * @param mediatorService mediator service
     * @param serviceRegistry service registry
     * @param staticClusterServiceConfiguration cluster configuration
     */
    public StaticClusterService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final StaticClusterServiceConfiguration staticClusterServiceConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, staticClusterServiceConfiguration);

        AppbaseUtil.notNull(staticClusterServiceConfiguration, "staticClusterServiceConfiguration");

        this.homeDataCenter = staticClusterServiceConfiguration.getHomeDataCenter();
    }

    /** Registers this instance under the cluster interface and its concrete type. */
    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(ClusterService.class, this);
        serviceRegistry.registerAppbaseService(StaticClusterService.class, this);
    }

    /** @return the construction-time home data-center name */
    @Override
    public String getHomeDataCenter() {
        return homeDataCenter;
    }
}
