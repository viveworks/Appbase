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

package com.viveworks.appbase.client.service.r2dbc;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.r2dbc.configuration.DefaultR2dbcClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Test-only bootstrap helper mirroring {@code JdbcClientServiceTestHelper}. Unlike the JDBC
 * twin's default classpath config (MariaDB-backed, gated behind a system property), this
 * module's default classpath config points at an in-memory H2 R2DBC URL so it loads
 * unconditionally in CI with no external infra.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class R2dbcClientServiceTestHelper {
    public static final String R2DBC_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "r2dbcClient";
    public static final String R2DBC_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-r2dbc.yaml";

    private R2dbcClientServiceTestHelper() {
    }

    public static DefaultR2dbcClientService defaultR2dbcClientService() {
        final String configFile = System.getProperty(R2DBC_CONFIG_FILE_PROPERTY, R2DBC_CONFIG_CLASSPATH_FILE);
        return defaultR2dbcClientService(configFile);
    }

    public static DefaultR2dbcClientService defaultR2dbcClientService(final String r2dbcConfigClasspathFile) {
        final AppbaseCore appbaseCore = appbaseCore(r2dbcConfigClasspathFile);
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        return new DefaultR2dbcClientService(defaultAppbaseClientCore);
    }

    public static AppbaseCore appbaseCore() {
        final String configFile = System.getProperty(R2DBC_CONFIG_FILE_PROPERTY, R2DBC_CONFIG_CLASSPATH_FILE);
        return appbaseCore(configFile);
    }

    public static AppbaseCore appbaseCore(final String r2dbcConfigClasspathFile) {
        AppbaseUtil.notNull(r2dbcConfigClasspathFile, "r2dbcConfigClasspathFile");

        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultR2dbcClientService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultR2dbcClientServiceConfiguration.class,
                r2dbcConfigClasspathFile);
        final ClasspathConfigurationStore classpathConfigurationStore =
                new ClasspathConfigurationStore(r2dbcConfigClasspathFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(classpathConfigurationStore);

        configurationService.load();

        return appbaseCore;
    }
}
