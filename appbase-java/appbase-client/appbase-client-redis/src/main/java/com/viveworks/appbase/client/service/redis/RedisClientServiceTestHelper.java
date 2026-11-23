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

package com.viveworks.appbase.client.service.redis;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.client.core.DefaultAppbaseClientCore;
import com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientServiceConfiguration;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Test helper for {@link RedisClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 * @since 1.0.0
 */
public class RedisClientServiceTestHelper {
    public static final String REDIS_CLIENT_CONFIG_FILE_PROPERTY =
            AppbaseConstants.TEST_HELPER_CONFIG_FILE_PROPERTY_PREFIX + "redisClient";
    public static final String REDIS_CLIENT_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-redis.yaml";

    private RedisClientServiceTestHelper() {
    }

    /**
     * Resolve the configuration resource: an explicitly named file, else the infra profile's choice.
     *
     * <p>⚠️ <b>The explicit property is checked FIRST, and that ordering is load-bearing.</b> Written as
     * {@code System.getProperty(KEY, AppbaseTestInfraProfile.resolve(DEFAULT))} the default argument is
     * evaluated <i>eagerly</i>, so an unrecognised profile would throw even for a caller who had named
     * its own file — a narrower switch overriding a wider, more specific instruction. Extracted as a
     * method so a test can exercise the real path instead of re-implementing the expression.
     *
     * @return the configuration resource this helper will load
     */
    public static String configFile() {
        return AppbaseTestInfraProfile.configFile(
                REDIS_CLIENT_CONFIG_FILE_PROPERTY, REDIS_CLIENT_CONFIG_CLASSPATH_FILE);
    }

    /**
     * Build a {@link DefaultRedisClientService} using the default classpath config (or a system-property override).
     *
     * @return configured and started service
     */
    public static DefaultRedisClientService defaultRedisClientService() {
        final String configFile = configFile();
        return defaultRedisClientService(configFile);
    }

    /**
     * Build a {@link DefaultRedisClientService} using the given classpath config file.
     *
     * @param redisConfigClasspathFile classpath URI of the YAML config
     * @return configured and started service
     */
    public static DefaultRedisClientService defaultRedisClientService(final String redisConfigClasspathFile) {
        final AppbaseCore appbaseCore = appbaseCore(redisConfigClasspathFile);
        final DefaultAppbaseClientCore defaultAppbaseClientCore =
                ClientCoreTestHelper.defaultAppbaseClientCore(appbaseCore);
        AppbaseUtil.notNull(defaultAppbaseClientCore, "defaultAppbaseClientCore");

        return new DefaultRedisClientService(defaultAppbaseClientCore);
    }

    /**
     * Build an {@link AppbaseCore} registered with the Redis client configuration.
     *
     * @return started core
     */
    public static AppbaseCore appbaseCore() {
        final String configFile = configFile();
        return appbaseCore(configFile);
    }

    /**
     * Build an {@link AppbaseCore} registered with the Redis client configuration from the given file.
     *
     * @param redisConfigClasspathFile classpath URI of the YAML config
     * @return started core
     */
    public static AppbaseCore appbaseCore(final String redisConfigClasspathFile) {
        AppbaseUtil.notNull(redisConfigClasspathFile, "redisConfigClasspathFile");

        final AppbaseCore appbaseCore = ClientCoreTestHelper.appbaseCore();
        DefaultRedisClientService.registerObjectMapperSubtypes();

        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final ConfigurationAccessor configurationAccessor = configurationService.getConfigurationAccessor();

        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultRedisClientServiceConfiguration.class,
                redisConfigClasspathFile);
        final ClasspathConfigurationStore classpathConfigurationStore =
                new ClasspathConfigurationStore(redisConfigClasspathFile, MimeType.YAML);
        configurationAccessor.registerConfigurationStore(classpathConfigurationStore);

        configurationService.load();

        return appbaseCore;
    }
}
