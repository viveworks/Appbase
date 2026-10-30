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

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationEntity;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link ConfigurationStore} implementation that uses <code>JPA</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:IllegalCatch")
public class JpaConfigurationStore implements ConfigurationStore {
    private final String id;
    private final ValidationService validationService;
    private final AtomicReference<EntityManager> entityManagerRef;
    private final AtomicReference<JpaConfigurationStoreConfiguration> jpaConfigurationStoreConfigurationRef;

    private final Timer readTimer;
    private final Counter readErrorCounter;

    private final Timer writeTimer;
    private final Counter writeErrorCounter;

    private final AtomicBoolean shutdown;

    public JpaConfigurationStore(final MetricService metricService,
                                 final ValidationService validationService,
                                 final EntityManager entityManager,
                                 final JpaConfigurationStoreConfiguration jpaConfigurationStoreConfiguration)
    {
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(entityManager, "entityManager");
        AppbaseUtil.notNull(jpaConfigurationStoreConfiguration, "jpaConfigurationStoreConfiguration");
        AppbaseUtil.validate(validationService, jpaConfigurationStoreConfiguration);

        this.validationService = validationService;
        this.entityManagerRef = new AtomicReference<>(entityManager);
        this.jpaConfigurationStoreConfigurationRef = new AtomicReference<>(jpaConfigurationStoreConfiguration);
        this.id = AppbaseUtil.configurationStoreIdPrefix(this) +
                jpaConfigurationStoreConfiguration.getEntityManagerConfiguration().getPersistenceUnitName();

        this.readTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "read", "timer"));
        this.readErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "read", "error", "counter"));

        this.writeTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "write", "timer"));
        this.writeErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "write", "error", "counter"));

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Collection<RawConfiguration> readRawConfigurations(
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException
    {
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        final Timer.Context context = readTimer.time();
        final Collection<RawConfiguration> rawConfigurations = new ArrayList<>();
        final EntityManager entityManager = entityManagerRef.get();
        try {
            entityManager.getTransaction().begin();
            try {
                final CriteriaQuery<ConfigurationEntity> criteriaQuery = entityManager
                        .getCriteriaBuilder()
                        .createQuery(ConfigurationEntity.class);
                final Root<ConfigurationEntity> root = criteriaQuery.from(ConfigurationEntity.class);
                criteriaQuery.select(root);
                final Collection<ConfigurationEntity> resultList = entityManager
                        .createQuery(criteriaQuery)
                        .getResultList();

                final SimpleDateFormat simpleDateFormat =
                        new SimpleDateFormat(ConfigurationEntity.LAST_UPDATED_TIMESTAMP_FORMAT);
                if (resultList != null) {
                    for (final ConfigurationEntity configurationEntity : resultList) {
                        AppbaseUtil.validate(
                                validationService,
                                configurationEntity,
                                ConfigurationEntity.ReadChecks.class);

                        final Map<String, String> rawConfigurationContext = new HashMap<>();
                        final Integer configurationEntityId = configurationEntity.getId();
                        if (configurationEntityId != null) {
                            rawConfigurationContext.put(
                                    ConfigurationEntity.ID_RAW_CONFIGURATION_CONTEXT_KEY,
                                    configurationEntityId.toString());
                        }

                        final String discriminator = configurationEntity.getDiscriminator();
                        if (discriminator != null && !discriminator.isBlank()) {
                            rawConfigurationContext.put(
                                    ConfigurationEntity.DISCRIMINATOR_RAW_CONFIGURATION_CONTEXT_KEY,
                                    discriminator.trim());
                        }
                        final String keyJavaTypeName = configurationEntity.getKeyJavaTypeName();
                        if (keyJavaTypeName != null && !keyJavaTypeName.isBlank()) {
                            rawConfigurationContext.put(
                                    ConfigurationEntity.KEY_JAVA_TYPE_NAME_RAW_CONFIGURATION_CONTEXT_KEY,
                                    keyJavaTypeName.trim());
                        }
                        final Date lastUpdatedTimestamp = configurationEntity.getLastUpdatedTimestamp();
                        if (lastUpdatedTimestamp != null) {
                            rawConfigurationContext.put(
                                    ConfigurationEntity.LAST_UPDATED_TIMESTAMP_RAW_CONFIGURATION_CONTEXT_KEY,
                                    simpleDateFormat.format(lastUpdatedTimestamp));
                        }

                        final RawConfiguration.Builder rawConfigurationBuilder = RawConfiguration.builder();
                        final String key = configurationEntity.getKey();
                        if (key != null && !key.isBlank()) {
                            rawConfigurationBuilder.name(key.trim());
                        }

                        final String value = configurationEntity.getValue();
                        if (value != null && !value.isBlank()) {
                            rawConfigurationBuilder.value(value.trim().getBytes(StandardCharsets.UTF_8));
                        }

                        final String valueMimeType = configurationEntity.getValueMimeType();
                        if (valueMimeType != null) {
                            rawConfigurationBuilder.mimeType(new MimeType(valueMimeType.trim()));
                        }
                        rawConfigurationBuilder.timestamp(System.currentTimeMillis());
                        rawConfigurationBuilder.context(Map.copyOf(rawConfigurationContext));
                        rawConfigurations.add(rawConfigurationBuilder.build());
                    }
                }
            } catch (Exception ex) {
                readErrorCounter.inc();
                throw new ConfigurationStoreException("Error reading raw configurations", ex);
            } finally {
                entityManager.getTransaction().commit();
            }
        } finally {
            context.stop();
        }

        return rawConfigurations;
    }

    @Override
    public void writeRawConfiguration(
            final RawConfiguration rawConfiguration,
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException
    {
        AppbaseUtil.notNull(rawConfiguration, "rawConfiguration");
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        final Timer.Context context = writeTimer.time();

        final EntityManager entityManager = entityManagerRef.get();
        try {
            final Map<String, String> rawConfigurationContext = rawConfiguration.getContext();
            if (rawConfigurationContext == null) {
                throw new ConfigurationStoreException("Raw configuration context cannot be null");
            }

            final String discriminator =
                    rawConfigurationContext.get(ConfigurationEntity.DISCRIMINATOR_RAW_CONFIGURATION_CONTEXT_KEY);
            if (discriminator == null) {
                throw new ConfigurationStoreException(
                        ConfigurationEntity.DISCRIMINATOR_RAW_CONFIGURATION_CONTEXT_KEY +
                                " is not set in the context");
            }

            entityManager.getTransaction().begin();
            try {
                final ConfigurationEntity configurationEntity = new ConfigurationEntity();

                final String name = rawConfiguration.getName();
                configurationEntity.setKey(name);

                final byte[] value = rawConfiguration.getValue();
                configurationEntity.setValue(new String(value, StandardCharsets.UTF_8));

                final MimeType mimeType = rawConfiguration.getMimeType();
                configurationEntity.setValueMimeType(mimeType.getValue());

                final long timestamp = rawConfiguration.getTimestamp();
                final Date timestampDate = Date.from(Instant.ofEpochMilli(timestamp));
                configurationEntity.setLastUpdatedTimestamp(timestampDate);

                configurationEntity.setDiscriminator(discriminator);

                final String keyJavaTypeName =
                        rawConfigurationContext.get(
                                ConfigurationEntity.KEY_JAVA_TYPE_NAME_RAW_CONFIGURATION_CONTEXT_KEY);
                if (keyJavaTypeName != null) {
                    configurationEntity.setKeyJavaTypeName(keyJavaTypeName.trim());
                }

                AppbaseUtil.validate(validationService, configurationEntity, ConfigurationEntity.WriteChecks.class);
                entityManager.persist(configurationEntity);
            } catch (Exception ex) {
                writeErrorCounter.inc();
                final String message = String.format(
                        "Error writing raw configuration [name=%s]", rawConfiguration.getName());
                throw new ConfigurationStoreException(message);
            } finally {
                entityManager.getTransaction().commit();
            }
        } finally {
            context.stop();
        }
    }

    @Override
    public boolean supportsWrite() {
        return true;
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        this.shutdown.set(true);
    }

    /**
     * Reload the {@link EntityManager}.
     *
     * @param entityManager Entity manager
     */
    public void reloadEntityManager(final EntityManager entityManager) {
        AppbaseUtil.notNull(entityManager, "entityManager");

        this.entityManagerRef.set(entityManager);
    }

    /**
     * Reload the {@link JpaConfigurationStoreConfiguration}.
     *
     * @param jpaConfigurationStoreConfiguration JpaConfigurationStoreConfiguration
     */
    public void reloadJpaConfigurationStoreConfiguration(
            final JpaConfigurationStoreConfiguration jpaConfigurationStoreConfiguration)
    {
        AppbaseUtil.notNull(jpaConfigurationStoreConfiguration, "jpaConfigurationStoreConfiguration");
        AppbaseUtil.validate(validationService, jpaConfigurationStoreConfiguration);

        this.jpaConfigurationStoreConfigurationRef.set(jpaConfigurationStoreConfiguration);
    }

    @Override
    public String toString() {
        return "JpaConfigurationStore{" +
                "id=" + id +
                ", jpaConfigurationStoreConfigurationRef=" + jpaConfigurationStoreConfigurationRef.get() +
                '}';
    }
}
