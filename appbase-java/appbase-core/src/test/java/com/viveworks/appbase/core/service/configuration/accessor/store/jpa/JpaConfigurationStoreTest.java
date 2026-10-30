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

package com.viveworks.appbase.core.service.configuration.accessor.store.jpa;

import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.DefaultConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.codec.YamlConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationEntity;
import com.viveworks.appbase.core.service.exception.store.EntityManagerConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class JpaConfigurationStoreTest {
    private static LogService logService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static MediatorService mediatorService;
    private static JpaConfigurationStore jpaConfigurationStore;
    private static ConfigurationAccessor configurationAccessor;
    private static ConfigurationService configurationService;

    @BeforeAll
    static void setUp() throws Exception {
        final String classpathFile1 = "classpath:///appbaseBootstrapConfiguration.yaml";
        System.setProperty(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, classpathFile1);
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = new DefaultMediatorService(
                logService,
                metricService,
                CoreTestFixtures.getSharedDisruptorService(),
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                validationService);
        jpaConfigurationStore =
                CoreTestFixtures.getJpaConfigurationStore(metricService, validationService);
        configurationAccessor = new DefaultConfigurationAccessor(
                logService,
                metricService,
                Map.of(), Map.of(), List.of(), Map.of());

        configurationAccessor.registerConfigurationStore(jpaConfigurationStore);
        configurationAccessor.registerConfigurationCodecByMimeType(new YamlConfigurationCodec(), MimeType.YAML);
        configurationAccessor.registerConfigurationTypeByRawConfigurationName(
                DefaultConfigurationServiceConfiguration.class,
                DefaultConfigurationServiceConfiguration.class.getName());
        configurationService = new DefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService,
                configurationAccessor,
                null);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (configurationService != null) {
            configurationService.shutdown();
        }

        if (configurationAccessor != null) {
            configurationAccessor.shutdown();
        }

        if (jpaConfigurationStore != null) {
            jpaConfigurationStore.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testConfigurationStore() throws Exception {
        Assertions.assertNotNull(jpaConfigurationStore);

        final ConfigurationAccessorContext configurationAccessorContext =
                CoreTestFixtures.getConfigurationAccessorContext(logService, metricService);

        final Map<String, String> context = new HashMap<>();
        context.put(ConfigurationEntity.DISCRIMINATOR_RAW_CONFIGURATION_CONTEXT_KEY, "discriminator");
        context.put(
                ConfigurationEntity.KEY_JAVA_TYPE_NAME_RAW_CONFIGURATION_CONTEXT_KEY,
                DefaultConfigurationServiceConfiguration.class.getName());
        final DefaultConfigurationServiceConfiguration defaultConfigurationServiceConfiguration =
                DefaultConfigurationServiceConfiguration.builder().build();
        final byte[] bytes = YamlUtil
                .getYamlMapper()
                .writerFor(DefaultConfigurationServiceConfiguration.class)
                .writeValueAsBytes(defaultConfigurationServiceConfiguration);
        final long timestamp = System.currentTimeMillis();
        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .name(DefaultConfigurationServiceConfiguration.class.getName())
                .value(bytes)
                .mimeType(MimeType.YAML)
                .context(context)
                .timestamp(timestamp)
                .build();
        Assertions.assertDoesNotThrow(() -> {
            jpaConfigurationStore.writeRawConfiguration(rawConfiguration, configurationAccessorContext);
        });

        final Map<String, String> entityManagerFactoryProperties = new HashMap<>();
        entityManagerFactoryProperties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        entityManagerFactoryProperties.put("jakarta.persistence.schema-generation.database.action", "create");
        entityManagerFactoryProperties.put("jakarta.persistence.validation.mode", "none");
        entityManagerFactoryProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        entityManagerFactoryProperties.put("hibernate.flushMode", "AUTO");
        entityManagerFactoryProperties.put("show_sql", "true");
        entityManagerFactoryProperties.put("format_sql", "true");
        entityManagerFactoryProperties.put("use_sql_comments", "true");
        entityManagerFactoryProperties.put(
                "jakarta.persistence.jdbc.url",
                // class-unique DB name so this class never shares a schema with another (internal work item 1073).
                "jdbc:h2:mem:TestAppbaseDBJpaConfigStore;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=KEY,VALUE");
        final EntityManager entityManager = Persistence
                .createEntityManagerFactory("testAppbasePersistenceUnit1", entityManagerFactoryProperties)
                .createEntityManager();
        Assertions.assertDoesNotThrow(() -> jpaConfigurationStore.reloadEntityManager(entityManager));

        final EntityManagerConfiguration entityManagerConfiguration =
                new EntityManagerConfiguration("testAppbasePersistenceUnit1", null, null);
        final JpaConfigurationStoreConfiguration jpaConfigurationStoreConfiguration =
                JpaConfigurationStoreConfiguration
                        .builder()
                        .entityManagerConfiguration(entityManagerConfiguration)
                        .build();
        Assertions.assertDoesNotThrow(() ->
                jpaConfigurationStore.reloadJpaConfigurationStoreConfiguration(
                        jpaConfigurationStoreConfiguration));

        final Collection<RawConfiguration> rawConfigurations =
                jpaConfigurationStore.readRawConfigurations(configurationAccessorContext);
        Assertions.assertNotNull(rawConfigurations);
        Assertions.assertFalse(rawConfigurations.isEmpty());
        Assertions.assertEquals(1, rawConfigurations.size());

        final RawConfiguration result = rawConfigurations.stream().findFirst().orElse(null);
        Assertions.assertNotNull(result);

        final String name = result.getName();
        Assertions.assertEquals(DefaultConfigurationServiceConfiguration.class.getName(), name);

        final byte[] value = result.getValue();
        final DefaultConfigurationServiceConfiguration defaultConfigurationServiceConfiguration1 = YamlUtil
                .getYamlMapper()
                .readerFor(DefaultConfigurationServiceConfiguration.class)
                .readValue(value);
        Assertions.assertEquals(defaultConfigurationServiceConfiguration, defaultConfigurationServiceConfiguration1);

        final MimeType mimeType = result.getMimeType();
        Assertions.assertEquals(MimeType.YAML.getValue(), mimeType.getValue());

        final Map<String, String> context1 = result.getContext();
        Assertions.assertEquals(
                context.get(ConfigurationEntity.DISCRIMINATOR_RAW_CONFIGURATION_CONTEXT_KEY),
                context1.get(ConfigurationEntity.DISCRIMINATOR_RAW_CONFIGURATION_CONTEXT_KEY));
        Assertions.assertEquals(
                context.get(ConfigurationEntity.KEY_JAVA_TYPE_NAME_RAW_CONFIGURATION_CONTEXT_KEY),
                context1.get(ConfigurationEntity.KEY_JAVA_TYPE_NAME_RAW_CONFIGURATION_CONTEXT_KEY));

        final String lastUpdatedTimestampStr =
                context1.get(ConfigurationEntity.LAST_UPDATED_TIMESTAMP_RAW_CONFIGURATION_CONTEXT_KEY);
        final SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(ConfigurationEntity.LAST_UPDATED_TIMESTAMP_FORMAT);
        final Date parsedDate = simpleDateFormat.parse(lastUpdatedTimestampStr);
        final long lastUpdatedTimestamp = parsedDate.toInstant().toEpochMilli();
        Assertions.assertEquals(timestamp, lastUpdatedTimestamp);

        configurationService.load();
        final Map<String, String> configurationContext = configurationService.getConfigurationContext(
                ConfigurationKey.fromTypeKey(DefaultConfigurationServiceConfiguration.class));
        Assertions.assertNotNull(configurationContext);
        Assertions.assertFalse(configurationContext.isEmpty());
        Assertions.assertEquals(
                DefaultConfigurationServiceConfiguration.class.getName(),
                configurationContext.get(ConfigurationEntity.KEY_JAVA_TYPE_NAME_RAW_CONFIGURATION_CONTEXT_KEY));
        Assertions.assertEquals(
                "discriminator",
                configurationContext.get(ConfigurationEntity.DISCRIMINATOR_RAW_CONFIGURATION_CONTEXT_KEY));
        Assertions.assertTrue(
                Integer.parseInt(configurationContext.get(
                        ConfigurationEntity.ID_RAW_CONFIGURATION_CONTEXT_KEY)) > -1);
        Assertions.assertNotNull(configurationContext.get(
                ConfigurationEntity.LAST_UPDATED_TIMESTAMP_RAW_CONFIGURATION_CONTEXT_KEY));

        Assertions.assertTrue(jpaConfigurationStore.supportsWrite());
        Assertions.assertDoesNotThrow(jpaConfigurationStore::shutdown);
    }
}
