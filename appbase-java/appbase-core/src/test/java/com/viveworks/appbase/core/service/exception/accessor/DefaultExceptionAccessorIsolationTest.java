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

package com.viveworks.appbase.core.service.exception.accessor;

import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultExceptionAccessorIsolationTest {

    @Test
    void testExtra_independentAccessorLifecyclesDoNotShareState() throws Exception {
        try (ExceptionAccessorLifecycle lifecycleA = new ExceptionAccessorLifecycle();
             ExceptionAccessorLifecycle lifecycleB = new ExceptionAccessorLifecycle())
        {
            lifecycleA.accessor.writeExceptionMessage(CoreTestFixtures.getExceptionMessage());

            assertStoredMessageCount(lifecycleA.accessor, 1);
            assertStoredMessageCount(lifecycleB.accessor, 0);

            final DefaultExceptionServiceConfiguration reloadConfigurationA =
                    CoreTestFixtures.getDefaultExceptionServiceConfiguration();
            final DefaultExceptionServiceConfiguration reloadConfigurationB =
                    CoreTestFixtures.getDefaultExceptionServiceConfiguration();
            lifecycleA.accessor.reload(reloadConfigurationA.getDefaultExceptionAccessorConfiguration());
            lifecycleB.accessor.reload(reloadConfigurationB.getDefaultExceptionAccessorConfiguration());

            assertStoredMessageCount(lifecycleA.accessor, 0);
            assertStoredMessageCount(lifecycleB.accessor, 0);

            lifecycleA.accessor.writeExceptionMessage(CoreTestFixtures.getExceptionMessage());
            assertStoredMessageCount(lifecycleA.accessor, 1);
            assertStoredMessageCount(lifecycleB.accessor, 0);
        }
    }

    private static void assertStoredMessageCount(
            final DefaultExceptionAccessor accessor,
            final int expectedCount) throws Exception
    {
        final Map<String, Collection<ExceptionMessageDetails>> storedMessages =
                accessor.readAllExceptionMessageDetails();
        Assertions.assertEquals(1, storedMessages.size());
        storedMessages.values().forEach(messages -> Assertions.assertEquals(expectedCount, messages.size()));
    }

    private static final class ExceptionAccessorLifecycle implements AutoCloseable {
        private final LogService logService;
        private final MetricService metricService;
        private final ValidationService validationService;
        private final MediatorService mediatorService;
        private final DefaultExceptionAccessor accessor;

        private ExceptionAccessorLifecycle() {
            logService = CoreTestFixtures.getPlainLogService();
            metricService = CoreTestFixtures.getMetricService(logService);
            validationService = CoreTestFixtures.getValidationService(logService, metricService);
            mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);
            accessor = CoreTestFixtures.getExceptionAccessor(
                    logService, metricService, mediatorService, validationService);
        }

        @Override
        public void close() {
            accessor.shutdown();
            mediatorService.shutdown();
            validationService.shutdown();
            metricService.shutdown();
            logService.shutdown();
        }
    }
}
