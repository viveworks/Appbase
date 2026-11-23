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

package com.viveworks.appbase.client.service.druid;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.service.druid.sql.DefaultDruidSqlClientService;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlClientSharedResources;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DefaultDruidSqlClientSharedResourcesConfiguration;
import com.viveworks.appbase.client.service.druid.sql.configuration.DefaultDruidSqlClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DruidSqlClientTestHelper {
    public static final String DRUID_SQL_CLIENT_SHARED_RESOURCES_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "druidSqlClientSharedResources";
    private static final String DRUID_SQL_CLIENT_SHARED_RESOURCES_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-druid-sql-shared-resources.yaml";

    public static final String DRUID_SQL_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "druidSqlClient";
    private static final String DRUID_SQL_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-druid-sql.yaml";

    private DruidSqlClientTestHelper() {
    }

    public static AppbaseCore appbaseCore() {
        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultDruidSqlClientService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        final String sharedResourcesConfigFile = System.getProperty(
                DRUID_SQL_CLIENT_SHARED_RESOURCES_PROPERTY,
                DRUID_SQL_CLIENT_SHARED_RESOURCES_CLASSPATH_FILE);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultDruidSqlClientSharedResourcesConfiguration.class,
                sharedResourcesConfigFile);

        final String configFile = System.getProperty(DRUID_SQL_CONFIG_FILE_PROPERTY, DRUID_SQL_CONFIG_CLASSPATH_FILE);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultDruidSqlClientServiceConfiguration.class,
                configFile);

        final ClasspathConfigurationStore druidClientSharedResourcesConfigurationStore =
                new ClasspathConfigurationStore(sharedResourcesConfigFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(druidClientSharedResourcesConfigurationStore);

        final ClasspathConfigurationStore druidSqlClasspathConfigurationStore =
                new ClasspathConfigurationStore(configFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(druidSqlClasspathConfigurationStore);

        configurationService.load();

        return appbaseCore;
    }

    public static AppbaseClientCore appbaseClientCore() {
        final AppbaseCore appbaseCore = appbaseCore();
        return ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
    }

    public static DefaultDruidSqlClientSharedResources defaultDruidSqlClientSharedResources() {
        final AppbaseClientCore appbaseClientCore = appbaseClientCore();
        return new DefaultDruidSqlClientSharedResources(appbaseClientCore);
    }
}
