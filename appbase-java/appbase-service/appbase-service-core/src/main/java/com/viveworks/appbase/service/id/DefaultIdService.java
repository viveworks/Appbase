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

package com.viveworks.appbase.service.id;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import org.apache.commons.text.RandomStringGenerator;

import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultIdService
        extends AbstractStatelessAppbaseService<DefaultIdServiceConfiguration>
        implements IdService
{
    private final RandomStringGenerator fourteenCharsGenerator;

    public DefaultIdService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final DefaultIdServiceConfiguration defaultIdServiceConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, defaultIdServiceConfiguration);

        this.fourteenCharsGenerator = new RandomStringGenerator
                .Builder()
                .withinRange(
                        defaultIdServiceConfiguration.getRandomStringMinimumCodePoint(),
                        defaultIdServiceConfiguration.getRandomStringMaximumCodePoint())
                .build();
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(IdService.class, this);
        serviceRegistry.registerAppbaseService(DefaultIdService.class, this);
    }

    @Override
    public String new14CharsId() {
        return fourteenCharsGenerator.generate(14);
    }

    @Override
    public String newId() {
        return UUID.randomUUID().toString();
    }
}
