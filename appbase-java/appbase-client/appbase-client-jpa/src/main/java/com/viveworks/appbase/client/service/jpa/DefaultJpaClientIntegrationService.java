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

package com.viveworks.appbase.client.service.jpa;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.jpa.configuration.DefaultJpaClientIntegrationServiceConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.exception.store.EntityManagerConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link JpaClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultJpaClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <EntityManager, TextValue, DefaultJpaClientIntegrationServiceConfiguration>
        implements JpaClientIntegrationService
{
    private final AtomicReference<EntityManager> entityManagerRef;
    private final AtomicReference<EntityManagerFactory> entityManagerFactoryRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultJpaClientIntegrationServiceConfiguration defaultJpaClientIntegrationServiceConfiguration;



    public DefaultJpaClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<EntityManager, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final EntityManager entityManager)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, EntityManager.class);

        AppbaseUtil.notNull(entityManager, "entityManager");

        this.entityManagerRef = new AtomicReference<>(entityManager);
        this.entityManagerFactoryRef = new AtomicReference<>();
        this.defaultJpaClientIntegrationServiceConfiguration = null;
    }

    public DefaultJpaClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<EntityManager, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultJpaClientIntegrationServiceConfiguration defaultJpaClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, EntityManager.class);

        AppbaseUtil.notNull(
                defaultJpaClientIntegrationServiceConfiguration, "defaultJpaClientIntegrationServiceConfiguration");

        this.entityManagerRef = new AtomicReference<>();
        this.entityManagerFactoryRef = new AtomicReference<>();
        this.defaultJpaClientIntegrationServiceConfiguration = defaultJpaClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultJpaClientIntegrationServiceConfiguration, "defaultJpaClientIntegrationServiceConfiguration");

        final EntityManagerConfiguration entityManagerConfiguration =
                defaultJpaClientIntegrationServiceConfiguration.getEntityManagerConfiguration();
        final String persistenceUnitName =
                entityManagerConfiguration.getPersistenceUnitName();

        final Map<String, String> entityManagerFactoryProperties =
                entityManagerConfiguration.getEntityManagerFactoryProperties();
        final EntityManagerFactory entityManagerFactory;
        if (entityManagerFactoryProperties != null) {
            entityManagerFactory = Persistence
                    .createEntityManagerFactory(persistenceUnitName, entityManagerFactoryProperties);
        } else {
            entityManagerFactory = Persistence
                    .createEntityManagerFactory(persistenceUnitName);
        }

        try {
            final EntityManager entityManager;
            final Map<String, String> entityManagerProperties = entityManagerConfiguration.getEntityManagerProperties();
            if (entityManagerProperties != null) {
                entityManager = entityManagerFactory.createEntityManager(entityManagerProperties);
            } else {
                entityManager = entityManagerFactory.createEntityManager();
            }

            entityManagerFactoryRef.set(entityManagerFactory);
            entityManagerRef.set(entityManager);
        } catch (RuntimeException initializationFailure) {
            try {
                entityManagerFactory.close();
            } catch (RuntimeException cleanupFailure) {
                initializationFailure.addSuppressed(cleanupFailure);
            }
            throw initializationFailure;
        }
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultJpaClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        RuntimeException failure = null;
        final EntityManager entityManager = entityManagerRef.getAndSet(null);
        try {
            if (entityManager != null) {
                entityManager.clear();
                entityManager.close();
            }
        } catch (RuntimeException closeFailure) {
            failure = closeFailure;
        } finally {
            final EntityManagerFactory entityManagerFactory = entityManagerFactoryRef.getAndSet(null);
            try {
                if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                    entityManagerFactory.close();
                }
            } catch (RuntimeException closeFailure) {
                if (failure == null) {
                    failure = closeFailure;
                } else {
                    failure.addSuppressed(closeFailure);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public EntityManager getClient() {
        return entityManagerRef.get();
    }

    @Override
    public boolean isHealthy() {
        final EntityManager entityManager = getClient();
        if (entityManager == null) {
            return false;
        }

        return entityManager.isOpen();
    }
}
