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

package com.viveworks.appbase.client.service.vitess;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.vitess.configuration.DefaultVitessGrpcClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.FileConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Test bootstrap helpers for the Vitess gRPC client service.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class VitessGrpcClientServiceTestHelper {
    private static final String VITESS_GRPC_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-vitess-grpc.yaml";

    private VitessGrpcClientServiceTestHelper() {
    }

    public static DefaultVitessGrpcClientService defaultVitessGrpcClientService() {
        return defaultVitessGrpcClientService(AppbaseTestInfraProfile.resolve(VITESS_GRPC_CONFIG_CLASSPATH_FILE));
    }

    /**
     * Builds the test service from a classpath configuration.
     *
     * @param vitessGrpcConfigClasspathFile classpath configuration name
     * @return the configured test service
     */
    public static DefaultVitessGrpcClientService defaultVitessGrpcClientService(
            final String vitessGrpcConfigClasspathFile)
    {
        final AppbaseCore appbaseCore = appbaseCore(vitessGrpcConfigClasspathFile);
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        return new DefaultVitessGrpcClientService(defaultAppbaseClientCore);
    }

    /**
     * Builds the test service from a real file. This is used for per-run configuration mutations:
     * adding a file to a running test class loader is not a portable classpath contract.
     *
     * @param vitessGrpcConfigFile configuration file created by the test
     * @return the configured test service
     * @throws AppbaseRuntimeException if the configuration path cannot be resolved
     */
    public static DefaultVitessGrpcClientService defaultVitessGrpcClientService(
            final Path vitessGrpcConfigFile)
    {
        AppbaseUtil.notNull(vitessGrpcConfigFile, "vitessGrpcConfigFile");
        final String rawConfigurationName;
        try {
            rawConfigurationName = vitessGrpcConfigFile.toRealPath().toString();
        } catch (final IOException exception) {
            throw new AppbaseRuntimeException("Cannot resolve Vitess test configuration " + vitessGrpcConfigFile,
                    exception);
        }
        final FileConfigurationStore configurationStore =
                new FileConfigurationStore(rawConfigurationName, MimeType.YAML);
        final AppbaseCore appbaseCore = appbaseCore(rawConfigurationName, configurationStore);
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        return new DefaultVitessGrpcClientService(defaultAppbaseClientCore);
    }

    public static AppbaseCore appbaseCore() {
        return appbaseCore(AppbaseTestInfraProfile.resolve(VITESS_GRPC_CONFIG_CLASSPATH_FILE));
    }

    public static AppbaseCore appbaseCore(final String vitessGrpcConfigClasspathFile) {
        AppbaseUtil.notNull(vitessGrpcConfigClasspathFile, "vitessGrpcConfigClasspathFile");

        return appbaseCore(vitessGrpcConfigClasspathFile,
                new ClasspathConfigurationStore(vitessGrpcConfigClasspathFile, MimeType.YAML));
    }

    private static AppbaseCore appbaseCore(final String rawConfigurationName,
                                           final ConfigurationStore configurationStore)
    {
        AppbaseUtil.notBlank(rawConfigurationName, "rawConfigurationName");
        AppbaseUtil.notNull(configurationStore, "configurationStore");

        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultVitessGrpcClientService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultVitessGrpcClientServiceConfiguration.class,
                rawConfigurationName);
        configurationAccessor.registerConfigurationStore(configurationStore);

        configurationService.load();

        return appbaseCore;
    }
}
