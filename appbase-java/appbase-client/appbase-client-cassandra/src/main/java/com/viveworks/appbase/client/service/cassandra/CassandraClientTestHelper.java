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

package com.viveworks.appbase.client.service.cassandra;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.cassandra.configuration.DefaultCassandraClientServiceConfiguration;
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
public class CassandraClientTestHelper {
    public static final String CASSANDRA_CLIENT_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "cassandraClient";
    private static final String CASSANDRA_CLIENT_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-cassandra.yaml";

    private CassandraClientTestHelper() {
    }

    public static DefaultCassandraClientService defaultCassandraClientService() {
        final AppbaseCore appbaseCore = appbaseCore();
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        return new DefaultCassandraClientService(defaultAppbaseClientCore);
    }

    public static AppbaseCore appbaseCore() {
        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultCassandraClientService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        final String configFile = AppbaseTestInfraProfile.configFile(CASSANDRA_CLIENT_CONFIG_FILE_PROPERTY, CASSANDRA_CLIENT_CONFIG_CLASSPATH_FILE);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultCassandraClientServiceConfiguration.class, configFile);
        final ClasspathConfigurationStore cassandraClasspathConfigurationStore =
                new ClasspathConfigurationStore(configFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(cassandraClasspathConfigurationStore);

        configurationService.load();

        return appbaseCore;
    }
}
