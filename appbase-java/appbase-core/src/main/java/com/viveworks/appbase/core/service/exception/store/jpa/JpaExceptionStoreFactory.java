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

package com.viveworks.appbase.core.service.exception.store.jpa;

import com.viveworks.appbase.core.service.exception.store.DefaultStackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.EntityManagerConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory;
import com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactoryException;
import com.viveworks.appbase.core.service.exception.store.StackTraceHashGenerator;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.ThreadSafe;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import java.util.Map;

/**
 * Factory for {@link JpaExceptionStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class JpaExceptionStoreFactory
        implements ExceptionStoreFactory<JpaExceptionStore, JpaExceptionStoreConfiguration>
{
    private final LogService logService;
    private final MetricService metricService;
    private final MediatorService mediatorService;
    private final ValidationService validationService;
    private final ExceptionMessageContextCodec exceptionMessageContextCodec;

    public JpaExceptionStoreFactory(final LogService logService,
                                    final MetricService metricService,
                                    final MediatorService mediatorService,
                                    final ValidationService validationService,
                                    final ExceptionMessageContextCodec exceptionMessageContextCodec)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(exceptionMessageContextCodec, "exceptionMessageContextCodec");

        this.logService = logService;
        this.metricService = metricService;
        this.mediatorService = mediatorService;
        this.validationService = validationService;
        this.exceptionMessageContextCodec = exceptionMessageContextCodec;
    }

    @Override
    public JpaExceptionStore newExceptionStore(
            final JpaExceptionStoreConfiguration exceptionStoreConfiguration) throws ExceptionStoreFactoryException
    {
        AppbaseUtil.notNull(exceptionStoreConfiguration, "exceptionStoreConfiguration");
        AppbaseUtil.validate(validationService, exceptionStoreConfiguration);

        if (exceptionStoreConfiguration.isEnabled()) {
            final StackTraceHashGenerator stackTraceHashGenerator =
                    new DefaultStackTraceHashGenerator(
                            exceptionStoreConfiguration.getStackTraceSerializationMaxLength());

            final EntityManagerConfiguration entityManagerConfiguration =
                    exceptionStoreConfiguration.getEntityManagerConfiguration();
            final String persistenceUnitName = entityManagerConfiguration.getPersistenceUnitName();
            final Map<String, String> entityManagerFactoryProperties =
                    entityManagerConfiguration.getEntityManagerFactoryProperties();
            final Map<String, String> entityManagerProperties =
                    entityManagerConfiguration.getEntityManagerProperties();

            final EntityManager entityManager = Persistence
                    .createEntityManagerFactory(persistenceUnitName, entityManagerFactoryProperties)
                    .createEntityManager(entityManagerProperties);
            return new JpaExceptionStore(
                    logService,
                    metricService,
                    mediatorService,
                    validationService,
                    entityManager,
                    stackTraceHashGenerator,
                    exceptionStoreConfiguration,
                    exceptionMessageContextCodec);
        }

        throw new ExceptionStoreFactoryException("The store " +
                JpaExceptionStore.class.getName() + " is disabled: " + exceptionStoreConfiguration);
    }
}
