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

package com.viveworks.appbase.client.service.jdbc;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.jdbc.configuration.DefaultJdbcClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * ⚠️ <b>ONLY the no-arg overloads route through {@link AppbaseTestInfraProfile}, and that is
 * deliberate</b> (P-050 N13, issue internal work item 953). The arg-taking overloads are the EXPLICIT path: a caller that
 * names its own resource has given a narrower, more specific instruction than the profile switch, and
 * routing inside them would let the wider switch win. That inversion shipped once in N02 and was caught
 * by the internal work item 921 security review.
 *
 * <p>A caller needing both — its own resource AND profile routing — wraps at the call site:
 * {@code defaultJdbcClientService(AppbaseTestInfraProfile.resolve(MY_RESOURCE))}. Four tests in this
 * repo do exactly that.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class JdbcClientServiceTestHelper {
    public static final String JDBC_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "jdbcClient";
    public static final String JDBC_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-jdbc.yaml";

    private JdbcClientServiceTestHelper() {
    }

    public static DefaultJdbcClientService defaultJdbcClientService() {
        final String configFile =
                AppbaseTestInfraProfile.configFile(JDBC_CONFIG_FILE_PROPERTY, JDBC_CONFIG_CLASSPATH_FILE);
        return defaultJdbcClientService(configFile);
    }

    public static DefaultJdbcClientService defaultJdbcClientService(final String jdbcConfigClasspathFile) {
        final AppbaseCore appbaseCore = appbaseCore(jdbcConfigClasspathFile);
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        return new DefaultJdbcClientService(defaultAppbaseClientCore);
    }

    public static AppbaseCore appbaseCore() {
        final String configFile =
                AppbaseTestInfraProfile.configFile(JDBC_CONFIG_FILE_PROPERTY, JDBC_CONFIG_CLASSPATH_FILE);
        return appbaseCore(configFile);
    }

    public static AppbaseCore appbaseCore(final String jdbcConfigClasspathFile) {
        AppbaseUtil.notNull(jdbcConfigClasspathFile, "jdbcConfigClasspathFile");

        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultJdbcClientService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultJdbcClientServiceConfiguration.class,
                jdbcConfigClasspathFile);
        final ClasspathConfigurationStore classpathConfigurationStore =
                new ClasspathConfigurationStore(jdbcConfigClasspathFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(classpathConfigurationStore);

        configurationService.load();

        return appbaseCore;
    }
}
