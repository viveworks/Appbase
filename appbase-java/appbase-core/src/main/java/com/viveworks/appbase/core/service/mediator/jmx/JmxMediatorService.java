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

package com.viveworks.appbase.core.service.mediator.jmx;

import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.MediatorServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseConstants;

import javax.annotation.Nullable;

/**
 * Default implementation of {@link JmxMediatorServiceMXBean}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxMediatorService
        extends AbstractAppbaseServiceMXBean<MediatorService, MediatorServiceConfiguration>
        implements JmxMediatorServiceMXBean
{
    private static final String ENV_PREFIX = AppbaseConstants.PERMISSION_PREFIX_ENV +
            JmxMediatorService.class.getSimpleName().toUpperCase() +
            AppbaseConstants.PERMISSION_SUFFIX_ENV;
    public static final String ALL_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "ALL_OPERATIONS";

    private static final String PROPERTY_PREFIX = AppbaseConstants.PERMISSION_PREFIX_PROPERTY +
            JmxMediatorService.class.getSimpleName().toLowerCase() +
            AppbaseConstants.PERMISSION_SUFFIX_PROPERTY;
    public static final String ALL_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "allOperations";

    public JmxMediatorService(final LogService logService,
                              final MediatorService appbaseService,
                              final Class<? extends MediatorServiceConfiguration> appbaseServiceConfigurationType,
                              final ConfigurationService configurationService)
    {
        super(logService, appbaseService, appbaseServiceConfigurationType, configurationService);
    }

    @Override
    public boolean isInitWithConfigurationFile() {
        return appbaseService.isInitWithConfigurationFile();
    }

    @Override
    public boolean isInitWithDefaultConfiguration() {
        return appbaseService.isInitWithDefaultConfiguration();
    }

    @Nullable
    @Override
    public String getConfigurationFilePath() {
        return appbaseService.getConfigurationFilePath();
    }
}
