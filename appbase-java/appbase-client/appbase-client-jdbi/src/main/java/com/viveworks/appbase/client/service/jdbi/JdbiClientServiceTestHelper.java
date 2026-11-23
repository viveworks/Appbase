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
package com.viveworks.appbase.client.service.jdbi;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.jdbc.DefaultJdbcClientService;
import com.viveworks.appbase.client.service.jdbc.JdbcClientService;
import com.viveworks.appbase.client.service.jdbc.JdbcClientServiceTestHelper;
import com.viveworks.appbase.client.service.jdbc.configuration.DefaultJdbcClientServiceConfiguration;
import com.viveworks.appbase.client.service.jdbi.configuration.DefaultJdbiClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * ⚠️ <b>This helper does NOT delegate to {@link JdbcClientServiceTestHelper}'s no-arg overload — it
 * repeats that lookup against the same two constants and calls its OWN two-arg overload.</b> That is
 * how it escaped P-050 N02: wiring the other helper's no-arg overload never reached this file. Recorded
 * because the shape is invisible at a glance, and the next routing change will have to look here too.
 *
 * <p>Of its two lookups only the jdbc one is routed; see the comments at each and
 * {@code JdbiConfigTwinResolutionTest} (P-050 N13, issue internal work item 953).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class JdbiClientServiceTestHelper {
    public static final String JDBI_CLIENT_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "jdbiClient";
    private static final String JDBI_CLIENT_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-jdbi.yaml";

    private JdbiClientServiceTestHelper() {
    }

    public static DefaultJdbiClientService defaultJdbiClientService() {
        final String jdbcConfigFile = AppbaseTestInfraProfile.configFile(
                JdbcClientServiceTestHelper.JDBC_CONFIG_FILE_PROPERTY,
                JdbcClientServiceTestHelper.JDBC_CONFIG_CLASSPATH_FILE);
        // NOT routed, deliberately: appbase-client-jdbi.yaml declares no host, so it can have no twin
        // (config-twin-drift.sh refuses a twin whose host line does not differ), and resolve() derives
        // a twin name unconditionally -- it would name a file nothing ships. See N13, issue internal work item 953, and
        // JdbiConfigTwinResolutionTest, which asserts that hazard by execution.
        final String jdbiConfigFile = System.getProperty(
                JDBI_CLIENT_CONFIG_FILE_PROPERTY,
                JDBI_CLIENT_CONFIG_CLASSPATH_FILE);
        return defaultJdbiClientService(jdbcConfigFile, jdbiConfigFile);
    }

    public static DefaultJdbiClientService defaultJdbiClientService(
            final String jdbcConfigClasspathFile,
            final String jdbiConfigClasspathFile)
    {
        final AppbaseCore appbaseCore = appbaseCore(jdbcConfigClasspathFile, jdbiConfigClasspathFile);

        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        final JdbcClientService jdbcClientService = new DefaultJdbcClientService(defaultAppbaseClientCore);
        return new DefaultJdbiClientService(defaultAppbaseClientCore, jdbcClientService);
    }

    public static AppbaseCore appbaseCore() {
        final String jdbcConfigFile = AppbaseTestInfraProfile.configFile(
                JdbcClientServiceTestHelper.JDBC_CONFIG_FILE_PROPERTY,
                JdbcClientServiceTestHelper.JDBC_CONFIG_CLASSPATH_FILE);
        // NOT routed, deliberately -- same reason as defaultJdbiClientService() above.
        final String jdbiConfigFile = System.getProperty(
                JDBI_CLIENT_CONFIG_FILE_PROPERTY,
                JDBI_CLIENT_CONFIG_CLASSPATH_FILE);
        return appbaseCore(jdbcConfigFile, jdbiConfigFile);
    }

    public static AppbaseCore appbaseCore(
            final String jdbcConfigClasspathFile,
            final String jdbiConfigClasspathFile)
    {
        AppbaseUtil.notNull(jdbcConfigClasspathFile, "jdbcConfigClasspathFile");
        AppbaseUtil.notNull(jdbiConfigClasspathFile, "jdbiConfigClasspathFile");

        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultJdbiClientService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultJdbcClientServiceConfiguration.class,
                jdbcConfigClasspathFile);
        final ClasspathConfigurationStore jdbcClasspathConfigurationStore =
                new ClasspathConfigurationStore(jdbcConfigClasspathFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(jdbcClasspathConfigurationStore);

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultJdbiClientServiceConfiguration.class,
                jdbiConfigClasspathFile);
        final ClasspathConfigurationStore jdbiClasspathConfigurationStore =
                new ClasspathConfigurationStore(jdbiConfigClasspathFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(jdbiClasspathConfigurationStore);

        configurationService.load();

        return appbaseCore;
    }
}
