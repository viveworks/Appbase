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

package com.viveworks.appbase.core.service.metric.jmx;

import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.metric.MetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricServiceException;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.micrometer.core.instrument.MeterRegistry;

import javax.management.MBeanException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation of {@link JmxMetricService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JmxMetricService
        extends AbstractAppbaseServiceMXBean<MetricService, MetricServiceConfiguration>
        implements JmxMetricServiceMXBean
{
    private static final String ENV_PREFIX = AppbaseConstants.PERMISSION_PREFIX_ENV +
            JmxMetricService.class.getSimpleName().toUpperCase() +
            AppbaseConstants.PERMISSION_SUFFIX_ENV;
    public static final String UNREGISTER_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "UNREGISTER_OPERATIONS";
    public static final String CLEAR_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "CLEAR_OPERATIONS";
    public static final String ALL_OPERATIONS_PERMISSION_ENV =
            ENV_PREFIX + "ALL_OPERATIONS";

    private static final String PROPERTY_PREFIX = AppbaseConstants.PERMISSION_PREFIX_PROPERTY +
            JmxMetricService.class.getSimpleName().toLowerCase() +
            AppbaseConstants.PERMISSION_SUFFIX_PROPERTY;
    public static final String ALL_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "allOperations";
    public static final String UNREGISTER_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "unregisterOperations";
    public static final String CLEAR_OPERATIONS_PERMISSION_PROPERTY =
            PROPERTY_PREFIX + "clearOperations";

    public static final boolean DEFAULT_UNREGISTER_OPERATIONS_PERMISSION = true;
    public static final boolean DEFAULT_CLEAR_OPERATIONS_PERMISSION = true;

    public JmxMetricService(final LogService logService,
                            final MetricService appbaseService,
                            final Class<? extends MetricServiceConfiguration> appbaseServiceConfigurationType,
                            final ConfigurationService configurationService)
    {
        super(logService, appbaseService, appbaseServiceConfigurationType, configurationService);
    }

    @Override
    public Set<String> getMicrometerRegistryNames() {
        final Set<String> result = new HashSet<>();

        final Set<MeterRegistry> micrometerRegistries = appbaseService.getMicrometerRegistries();
        if (micrometerRegistries != null && !micrometerRegistries.isEmpty()) {
            micrometerRegistries.forEach(meterRegistry -> {
                if (meterRegistry != null) {
                    result.add(meterRegistry.getClass().getName());
                }
            });
        }

        return Collections.unmodifiableSet(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterMicrometerRegistry(final String meterRegistryTypeName) throws MBeanException {
        checkUnregisterPermission("unregisterMicrometerRegistry");

        AppbaseUtil.notBlank(meterRegistryTypeName, "meterRegistryTypeName");

        try {
            final Class<? extends MeterRegistry> meterRegistryType =
                    (Class<? extends MeterRegistry>) Class.forName(meterRegistryTypeName);
            appbaseService.unregisterMicrometerRegistry(meterRegistryType);
        } catch (ClassNotFoundException cnfe) {
            throw new MBeanException(cnfe, "Error with type name: " + meterRegistryTypeName);
        }
    }

    @Override
    public void clearMicrometerRegistries() {
        checkClearPermission("clearMicrometerRegistries");

        appbaseService.clearMicrometerRegistries();
    }

    @Override
    public String serializeMetricRegistryAsJson() throws MBeanException {
        try {
            return appbaseService.serializeMetricRegistryAsJson();
        } catch (MetricServiceException mse) {
            throw new MBeanException(mse, "Error while serializing metric registry as JSON");
        }
    }

    private void checkUnregisterPermission(final String operationName) {
        checkPermission(
                UNREGISTER_OPERATIONS_PERMISSION_ENV,
                UNREGISTER_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_UNREGISTER_OPERATIONS_PERMISSION,
                operationName);
    }

    private void checkClearPermission(final String operationName) {
        checkPermission(
                CLEAR_OPERATIONS_PERMISSION_ENV,
                CLEAR_OPERATIONS_PERMISSION_PROPERTY,
                DEFAULT_CLEAR_OPERATIONS_PERMISSION,
                operationName);
    }
}
