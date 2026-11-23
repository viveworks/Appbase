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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.DefaultAppbaseCore;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.di.guice.AppbaseCoreGuiceModule;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.codec.YamlConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorServiceConfiguration;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.lucene.LuceneService;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ClientCoreTestHelper {
    public static final String CORE_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "appbaseCore";
    private static final String CORE_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-core.yaml";

    public static final String CLIENT_CORE_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "appbaseClientCore";
    private static final String CLIENT_CORE_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-core.yaml";

    static {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientCore.registerObjectMapperSubTypes();
    }

    public static AppbaseClientCoreGuiceModule appbaseClientCoreGuiceModule() {
        return new AppbaseClientCoreGuiceModule();
    }

    public static AppbaseCoreGuiceModule appbaseCoreGuiceModule() {
        final AppbaseCore appbaseCore = appbaseCore();
        return new AppbaseCoreGuiceModule(appbaseCore);
    }

    public static DefaultAppbaseClientCore defaultAppbaseClientCore(final AppbaseCore appbaseCore) {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        return new DefaultAppbaseClientCore(appbaseCore);
    }

    private static AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry() {
        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry = new DefaultAppbaseCoreBootstrapRegistry();
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultServiceRegistryConfiguration.class,
                DefaultServiceRegistryConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultExceptionServiceConfiguration.class,
                DefaultExceptionServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultTracerServiceConfiguration.class,
                DefaultTracerServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultReactorServiceConfiguration.class,
                DefaultReactorServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationCodecByMimeType(
                new YamlConfigurationCodec(), MimeType.YAML);

        final String coreConfigFile =
                System.getProperty(CORE_CONFIG_FILE_PROPERTY, CORE_CONFIG_CLASSPATH_FILE);
        final String clientCoreConfigFile =
                System.getProperty(CLIENT_CORE_CONFIG_FILE_PROPERTY, CLIENT_CORE_CONFIG_CLASSPATH_FILE);

        appbaseCoreBootstrapRegistry.registerConfigurationTypeByRawConfigurationName(
                DefaultAppbaseClientCoreConfiguration.class, clientCoreConfigFile);


        final ClasspathConfigurationStore coreConfigConfigurationStore =
                new ClasspathConfigurationStore(coreConfigFile, MimeType.YAML);
        appbaseCoreBootstrapRegistry.registerConfigurationStore(coreConfigConfigurationStore);

        final ClasspathConfigurationStore clientCoreConfigConfigurationStore =
                new ClasspathConfigurationStore(clientCoreConfigFile, MimeType.YAML);
        appbaseCoreBootstrapRegistry.registerConfigurationStore(clientCoreConfigConfigurationStore);

        appbaseCoreBootstrapRegistry.setExceptionMessageContextCodec(new DefaultExceptionMessageContextCodec());

        return appbaseCoreBootstrapRegistry;
    }

    private static AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap(
            final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry)
    {
        AppbaseUtil.notNull(appbaseCoreBootstrapRegistry, "appbaseCoreBootstrapRegistry");

        final String coreConfigFile = System.getProperty(CORE_CONFIG_FILE_PROPERTY, CORE_CONFIG_CLASSPATH_FILE);
        return new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, coreConfigFile);
    }

    private static AppbaseCore appbaseCore(
            final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap)
    {
        AppbaseUtil.notNull(appbaseCoreBootstrap, "appbaseCoreBootstrap");

        final DefaultAppbaseCore defaultAppbaseCore = new DefaultAppbaseCore(appbaseCoreBootstrap);
        addShutdownHook(defaultAppbaseCore.getLogService());

        return defaultAppbaseCore;
    }

    public static AppbaseCore appbaseCore() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();

        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry =
                appbaseCoreBootstrapRegistry();
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap =
                appbaseCoreBootstrap(appbaseCoreBootstrapRegistry);

        return appbaseCore(appbaseCoreBootstrap);
    }

    private static void addShutdownHook(final LogService logService) {
        AppbaseUtil.notNull(logService, "logService");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (logService instanceof DefaultLogService) {
                final DefaultLogService defaultLogService = (DefaultLogService) logService;
                final LuceneService luceneService = defaultLogService.getLuceneService();
                if (luceneService != null) {
                    luceneService.shutdown();
                }
            }
        }));
    }
}
