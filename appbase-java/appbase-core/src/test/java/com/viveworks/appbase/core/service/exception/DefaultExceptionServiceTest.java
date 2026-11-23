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

package com.viveworks.appbase.core.service.exception;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationException;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.exception.accessor.ExceptionAccessor;
import com.viveworks.appbase.core.service.exception.listener.ExceptionListener;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageFactory;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Gauge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultExceptionServiceTest {
    private LogService logService;
    private MetricService metricService;
    private AppbaseCore appbaseCore;
    private DefaultExceptionService exceptionService;

    @BeforeEach
    void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        appbaseCore = CoreTestFixtures.getAppbaseCoreWithJpaExceptionStore(logService);
        exceptionService = (DefaultExceptionService) appbaseCore.getExceptionService();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }

        if (exceptionService != null) {
            exceptionService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testService() throws Exception {
        runTests(exceptionService, appbaseCore.getMediatorService());
        exceptionService.reload(exceptionService.getConfiguration());
        runTests(exceptionService, appbaseCore.getMediatorService());
        exceptionService.shutdown();
        final ServiceStatus status = exceptionService.getStatus();
        Assertions.assertEquals(ServiceStatus.SHUTDOWN, status);
    }

    @Test
    void testCollections() throws Exception {
        exceptionService.clearGlobalExceptionListeners();
        exceptionService.clearExceptionListenersByExceptionTypeMap();

        final TypeExceptionListener typeExceptionListener = new TypeExceptionListener();
        exceptionService.registerExceptionListenerByExceptionType(ConfigurationException.class, typeExceptionListener);
        final Map<String, Collection<ExceptionListener>> exceptionListenersByExceptionTypeMap1 =
                exceptionService.getExceptionListenersByExceptionTypeMap();
        Assertions.assertNotNull(exceptionListenersByExceptionTypeMap1);
        Assertions.assertEquals(1, exceptionListenersByExceptionTypeMap1.size());
        Assertions.assertTrue(exceptionListenersByExceptionTypeMap1
                .containsKey(ConfigurationException.class.getName()));

        exceptionService.unregisterExceptionListenerByExceptionType(
                ConfigurationException.class, TypeExceptionListener.class);
        final Map<String, Collection<ExceptionListener>> exceptionListenersByExceptionTypeMap2 =
                exceptionService.getExceptionListenersByExceptionTypeMap();
        Assertions.assertNotNull(exceptionListenersByExceptionTypeMap2);
        Assertions.assertTrue(exceptionListenersByExceptionTypeMap2.isEmpty());

        exceptionService.registerExceptionListenerByExceptionType(ConfigurationException.class, typeExceptionListener);
        final Map<String, Collection<ExceptionListener>> exceptionListenersByExceptionTypeMap3 =
                exceptionService.getExceptionListenersByExceptionTypeMap();
        Assertions.assertNotNull(exceptionListenersByExceptionTypeMap3);
        Assertions.assertEquals(1, exceptionListenersByExceptionTypeMap3.size());

        final DefaultExceptionServiceConfiguration configuration = exceptionService.getConfiguration();
        final ConfigurationAddedEvent configurationAddedEvent = new ConfigurationAddedEvent(
                ConfigurationKey.fromTypeKey(DefaultExceptionServiceConfiguration.class),
                ConfigurationValue.fromValue(configuration),
                System.currentTimeMillis());
        Assertions.assertDoesNotThrow(() -> exceptionService.onConfigurationAdded(configurationAddedEvent));

        final ConfigurationChangedEvent configurationChangedEvent = new ConfigurationChangedEvent(
                ConfigurationKey.fromTypeKey(DefaultExceptionServiceConfiguration.class),
                ConfigurationValue.fromValue(configuration),
                ConfigurationValue.fromValue(configuration),
                System.currentTimeMillis());
        Assertions.assertDoesNotThrow(() -> exceptionService.onConfigurationChanged(configurationChangedEvent));

        final ConfigurationRemovedEvent configurationRemovedEvent = new ConfigurationRemovedEvent(
                ConfigurationKey.fromTypeKey(DefaultExceptionServiceConfiguration.class),
                System.currentTimeMillis());
        Assertions.assertDoesNotThrow(() -> exceptionService.onConfigurationRemoved(configurationRemovedEvent));

        exceptionService.clearExceptionListenersByExceptionTypeMap();
        final Map<String, Collection<ExceptionListener>> exceptionListenersByExceptionTypeMap4 =
                exceptionService.getExceptionListenersByExceptionTypeMap();
        Assertions.assertNotNull(exceptionListenersByExceptionTypeMap4);
        Assertions.assertTrue(exceptionListenersByExceptionTypeMap4.isEmpty());


        final GlobalExceptionListener globalExceptionListener = new GlobalExceptionListener();
        exceptionService.registerGlobalExceptionListener(globalExceptionListener);
        final Collection<ExceptionListener> globalExceptionListeners1 = exceptionService.getGlobalExceptionListeners();
        Assertions.assertNotNull(globalExceptionListeners1);
        Assertions.assertEquals(1, globalExceptionListeners1.size());

        exceptionService.unregisterGlobalExceptionListener(GlobalExceptionListener.class);
        final Collection<ExceptionListener> globalExceptionListeners2 = exceptionService.getGlobalExceptionListeners();
        Assertions.assertNotNull(globalExceptionListeners2);
        Assertions.assertTrue(globalExceptionListeners2.isEmpty());

        exceptionService.registerGlobalExceptionListener(globalExceptionListener);
        final Collection<ExceptionListener> globalExceptionListeners3 = exceptionService.getGlobalExceptionListeners();
        Assertions.assertNotNull(globalExceptionListeners3);
        Assertions.assertEquals(1, globalExceptionListeners3.size());

        exceptionService.clearGlobalExceptionListeners();
        final Collection<ExceptionListener> globalExceptionListeners4 = exceptionService.getGlobalExceptionListeners();
        Assertions.assertNotNull(globalExceptionListeners4);
        Assertions.assertTrue(globalExceptionListeners4.isEmpty());
    }

    void runTests(
            final DefaultExceptionService exceptionService,
            final MediatorService mediatorService) throws Exception
    {
        Assertions.assertNotNull(exceptionService);

        final ExceptionAccessor exceptionAccessor = exceptionService.getExceptionAccessor();
        Assertions.assertNotNull(exceptionAccessor);

        final ConfigurationException configurationException = new ConfigurationException("config exception");
        final Message exceptionMessage = MessageFactory.exceptionMessage(
                mediatorService,
                getClass(),
                "Test exception message 1",
                AppbaseUtil.threadName(),
                configurationException);
        exceptionService.registerExceptionListenerByExceptionType(
                ConfigurationException.class, new TypeExceptionListener());
        exceptionService.registerGlobalExceptionListener(new GlobalExceptionListener());
        mediatorService.getMediator().asyncHandleMessage(exceptionMessage, true);
        Thread.sleep(3000);

        final DefaultExceptionServiceConfiguration configuration = exceptionService.getConfiguration();
        Assertions.assertNotNull(configuration);

        final Gauge exceptionListenersByExceptionTypeNameMapSizeGauge =
                exceptionService.getExceptionListenersByExceptionTypeNameMapSizeGauge();
        Assertions.assertNotNull(exceptionListenersByExceptionTypeNameMapSizeGauge);

        final Gauge globalExceptionListenerByTypeNameMapSizeGauge =
                exceptionService.getGlobalExceptionListenerByTypeNameMapSizeGauge();
        Assertions.assertNotNull(globalExceptionListenerByTypeNameMapSizeGauge);
    }

    class TypeExceptionListener implements ExceptionListener {
        private final Logger logger;

        TypeExceptionListener() {
            logger = logService.getLogger(getClass());
        }

        @Override
        public void onException(final ExceptionMessage exceptionMessage) {
            logger.info("Handling exception message: {}", exceptionMessage);
        }
    }

    class GlobalExceptionListener implements ExceptionListener {
        private final Logger logger;

        GlobalExceptionListener() {
            logger = logService.getLogger(getClass());
        }

        @Override
        public void onException(final ExceptionMessage exceptionMessage) {
            logger.info("Handling exception message: {}", exceptionMessage);
        }
    }
}
