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

package com.viveworks.appbase.client.service.aerospike;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.aerospike.configuration.DefaultAerospikeClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AerospikeClientTestHelper {
    public static final String AEROSPIKE_CLIENT_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "aerospikeClient";
    private static final String AEROSPIKE_CLIENT_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-aerospike.yaml";

    private AerospikeClientTestHelper() {
    }

    public static DefaultAerospikeClientService defaultAerospikeClientService(final AppbaseCore appbaseCore) {
        final String configFile = AppbaseTestInfraProfile.configFile(AEROSPIKE_CLIENT_CONFIG_FILE_PROPERTY, AEROSPIKE_CLIENT_CONFIG_CLASSPATH_FILE);
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        loadConfig(appbaseCore);

        return new DefaultAerospikeClientService(defaultAppbaseClientCore);
    }

    public static DefaultAerospikeClientService defaultAerospikeClientService(
            final AppbaseClientCore appbaseClientCore)
    {
        return new DefaultAerospikeClientService(appbaseClientCore);
    }

    public static DefaultAerospikeClientService defaultAerospikeClientService() {
        final AppbaseCore appbaseCore = appbaseCore();
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        loadConfig(appbaseCore);

        return new DefaultAerospikeClientService(defaultAppbaseClientCore);
    }

    public static AppbaseCore appbaseCore() {
        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultAerospikeClientService.registerObjectMapperSubtypes();


        return appbaseCore;
    }

    private static void loadConfig(final AppbaseCore appbaseCore) {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();
        final String configFile = AppbaseTestInfraProfile.configFile(AEROSPIKE_CLIENT_CONFIG_FILE_PROPERTY, AEROSPIKE_CLIENT_CONFIG_CLASSPATH_FILE);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultAerospikeClientServiceConfiguration.class,
                configFile);
        final ClasspathConfigurationStore aerospikeClasspathConfigurationStore =
                new ClasspathConfigurationStore(configFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(aerospikeClasspathConfigurationStore);

        configurationService.load();
    }
}
