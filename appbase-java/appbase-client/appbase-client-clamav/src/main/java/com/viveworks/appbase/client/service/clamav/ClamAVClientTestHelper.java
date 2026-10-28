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

package com.viveworks.appbase.client.service.clamav;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.service.clamav.client.DefaultClamAVClient;
import com.viveworks.appbase.client.service.clamav.client.DefaultClamAVClientSharedResources;
import com.viveworks.appbase.client.service.clamav.client.DefaultClamAVMediatorEventListener;
import com.viveworks.appbase.client.service.clamav.client.configuration.DefaultClamAVClientConfiguration;
import com.viveworks.appbase.client.service.clamav.client.configuration.DefaultClamAVClientSharedResourcesConfiguration;
import com.viveworks.appbase.client.service.clamav.configuration.DefaultClamAVClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ClamAVClientTestHelper {
    public static final String CLAMAV_CLIENT_SHARED_RESOURCES_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "clamavClientSharedResources";
    private static final String CLAMAV_CLIENT_SHARED_RESOURCES_CONFIGURATION =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-clamav-shared-resources.yaml";

    public static final String CLAMAV_CLIENT_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "clamavClient";
    private static final String CLAMAV_CLIENT_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-clamav.yaml";

    private ClamAVClientTestHelper() {
    }

    public static AppbaseCore appbaseCore() {
        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultClamAVClientService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        final String sharedResourcesConfigFile = System.getProperty(
                CLAMAV_CLIENT_SHARED_RESOURCES_CONFIG_FILE_PROPERTY,
                CLAMAV_CLIENT_SHARED_RESOURCES_CONFIGURATION);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultClamAVClientSharedResourcesConfiguration.class, sharedResourcesConfigFile);
        final ClasspathConfigurationStore clamAVSharedResourcesConfigurationStore =
                new ClasspathConfigurationStore(sharedResourcesConfigFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(clamAVSharedResourcesConfigurationStore);

        final String configFile = System.getProperty(
                CLAMAV_CLIENT_CONFIG_FILE_PROPERTY,
                CLAMAV_CLIENT_CONFIG_CLASSPATH_FILE);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultClamAVClientServiceConfiguration.class, configFile);
        final ClasspathConfigurationStore clamAVClasspathConfigurationStore =
                new ClasspathConfigurationStore(configFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(clamAVClasspathConfigurationStore);

        configurationService.load();

        return appbaseCore;
    }

    public static AppbaseClientCore appbaseClientCore() {
        final AppbaseCore appbaseCore = appbaseCore();
        return ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
    }

    public static DefaultClamAVClientSharedResources defaultClamAVClientSharedResources() {
        final AppbaseClientCore appbaseClientCore = appbaseClientCore();
        return new DefaultClamAVClientSharedResources(appbaseClientCore);
    }

    public static DefaultClamAVMediatorEventListener defaultClamAVMediatorEventListener() {
        final DefaultClamAVClientSharedResources defaultClamAVClientSharedResources = defaultClamAVClientSharedResources();
        final DefaultClamAVClientServiceConfiguration defaultClamAVClientServiceConfiguration =
                defaultClamAVClientServiceConfiguration(defaultClamAVClientSharedResources);
        if (defaultClamAVClientServiceConfiguration == null) {
            throw new AppbaseRuntimeException("Cannot find " + DefaultClamAVClientServiceConfiguration.class.getName());
        }

        final DefaultClamAVClientConfiguration defaultClamAVClientConfiguration = defaultClamAVClientServiceConfiguration
                .getDefaultDefaultClamAVClientIntegrationServiceConfiguration()
                .getDefaultClamAVClientConfiguration();
        return new DefaultClamAVMediatorEventListener(defaultClamAVClientSharedResources, defaultClamAVClientConfiguration);
    }

    public static DefaultClamAVClient defaultClamAVClient() {
        final AppbaseClientCore appbaseClientCore = appbaseClientCore();
        final DefaultClamAVClientSharedResources defaultClamAVClientSharedResources =
                new DefaultClamAVClientSharedResources(appbaseClientCore);
        final DefaultClamAVClientServiceConfiguration defaultClamAVClientServiceConfiguration =
                defaultClamAVClientServiceConfiguration(defaultClamAVClientSharedResources);
        final DefaultClamAVClientConfiguration defaultClamAVClientConfiguration = defaultClamAVClientServiceConfiguration
                .getDefaultDefaultClamAVClientIntegrationServiceConfiguration()
                .getDefaultClamAVClientConfiguration();
        final DefaultClamAVClient defaultClamAVClient = new DefaultClamAVClient(
                appbaseClientCore, defaultClamAVClientSharedResources, defaultClamAVClientConfiguration);
        defaultClamAVClient.reload();

        return defaultClamAVClient;
    }

    public static DefaultClamAVClientServiceConfiguration defaultClamAVClientServiceConfiguration(
            final DefaultClamAVClientSharedResources defaultClamAVClientSharedResources)
    {
        return defaultClamAVClientSharedResources
                .getAppbaseClientCore()
                .getAppbaseCore()
                .getConfigurationService()
                .getTypedConfiguration(DefaultClamAVClientServiceConfiguration.class);
    }
}
