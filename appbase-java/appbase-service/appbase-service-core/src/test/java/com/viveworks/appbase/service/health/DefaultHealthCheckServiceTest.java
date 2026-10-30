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

package com.viveworks.appbase.service.health;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.service.DummyHealthChecker1;
import com.viveworks.appbase.service.DummyHealthChecker2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultHealthCheckServiceTest {
    private static AppbaseCore appbaseCore;
    private static DefaultHealthCheckService defaultHealthCheckService;

    @BeforeAll
    static void beforeAll() {
        appbaseCore = ClientCoreTestHelper.appbaseCore();

        final MediatorService mediatorService = appbaseCore.getMediatorService();
        defaultHealthCheckService = new DefaultHealthCheckService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                mediatorService,
                appbaseCore.getServiceRegistry(),
                mediatorService.getMediator().getSharedScheduledExecutorService(),
                DefaultHealthCheckServiceConfiguration.builder().build());
    }

    @Test
    void testCheckHealth() {
        defaultHealthCheckService.addHealthChecker(new DummyHealthChecker1());
        defaultHealthCheckService.checkHealth(true);
        final HealthChecks healthChecks0 = defaultHealthCheckService.checkHealth();
        Assertions.assertTrue(healthChecks0.isHealthy());

        final List<HealthCheck> healthCheckList0 = healthChecks0.getHealthChecks();
        Assertions.assertNotNull(healthCheckList0);
        Assertions.assertFalse(healthCheckList0.isEmpty());
        Assertions.assertEquals(1, healthCheckList0.size());
        final HealthCheck healthCheck01 = healthCheckList0.get(0);
        Assertions.assertEquals(DummyHealthChecker1.class.getName(), healthCheck01.getServiceName());
        Assertions.assertTrue(healthCheck01.isHealthy());

        final HealthChecks healthChecks1 = defaultHealthCheckService.checkHealth(true);
        Assertions.assertTrue(healthChecks1.isHealthy());

        final List<HealthCheck> healthCheckList1 = healthChecks1.getHealthChecks();
        Assertions.assertNotNull(healthCheckList1);
        Assertions.assertFalse(healthCheckList1.isEmpty());
        Assertions.assertEquals(1, healthCheckList1.size());
        final HealthCheck healthCheck11 = healthCheckList1.get(0);
        Assertions.assertEquals(DummyHealthChecker1.class.getName(), healthCheck11.getServiceName());
        Assertions.assertTrue(healthCheck11.isHealthy());

        defaultHealthCheckService.addHealthChecker(new DummyHealthChecker2());
        final HealthChecks healthChecks2 = defaultHealthCheckService.checkHealth(true);
        Assertions.assertFalse(healthChecks2.isHealthy());
        final List<HealthCheck> healthCheckList2 = healthChecks2.getHealthChecks();
        Assertions.assertNotNull(healthCheckList2);
        Assertions.assertFalse(healthCheckList2.isEmpty());
        Assertions.assertEquals(2, healthCheckList2.size());
        final HealthCheck healthCheck21 = healthCheckList2.get(0);
        Assertions.assertEquals(DummyHealthChecker1.class.getName(), healthCheck21.getServiceName());
        Assertions.assertTrue(healthCheck21.isHealthy());
        final HealthCheck healthCheck22 = healthCheckList2.get(1);
        Assertions.assertEquals(DummyHealthChecker2.class.getName(), healthCheck22.getServiceName());
        Assertions.assertFalse(healthCheck22.isHealthy());
    }

    @Test
    void testAddHealthChecker() {
        defaultHealthCheckService.addHealthChecker(new DummyHealthChecker1());
        Assertions.assertTrue(defaultHealthCheckService.existsHealthChecker(DummyHealthChecker1.class));
        Assertions.assertFalse(defaultHealthCheckService.existsHealthChecker(DummyHealthChecker2.class));

        defaultHealthCheckService.removeHealthChecker(DummyHealthChecker1.class);
        Assertions.assertFalse(defaultHealthCheckService.existsHealthChecker(DummyHealthChecker1.class));
    }

    @Test
    void testExistsHealthChecker() {
        Assertions.assertFalse(defaultHealthCheckService.existsHealthChecker(DummyHealthChecker2.class));
        defaultHealthCheckService.addHealthChecker(new DummyHealthChecker2());
        Assertions.assertTrue(defaultHealthCheckService.existsHealthChecker(DummyHealthChecker2.class));
        defaultHealthCheckService.removeHealthChecker(DummyHealthChecker2.class);
    }

    @Test
    void testRemoveHealthChecker() {
        defaultHealthCheckService.addHealthChecker(new DummyHealthChecker2());
        Assertions.assertTrue(defaultHealthCheckService.existsHealthChecker(DummyHealthChecker2.class));
        defaultHealthCheckService.removeHealthChecker(DummyHealthChecker2.class);
        Assertions.assertFalse(defaultHealthCheckService.existsHealthChecker(DummyHealthChecker2.class));
    }

    @Test
    void testScheduledFutureCancelledOnShutdown() {
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            final DefaultHealthCheckService service = new DefaultHealthCheckService(
                    appbaseCore.getLogService(),
                    appbaseCore.getMetricService(),
                    appbaseCore.getMediatorService(),
                    appbaseCore.getServiceRegistry(),
                    executor,
                    DefaultHealthCheckServiceConfiguration.builder().build());

            Assertions.assertNotNull(service.getSchedulerFuture(), "schedulerFuture must be set after construction");
            Assertions.assertFalse(service.getSchedulerFuture().isCancelled(), "future must not be cancelled before shutdown");

            service.shutdown();

            Assertions.assertTrue(service.getSchedulerFuture().isCancelled(), "future must be cancelled after shutdown");
        } finally {
            executor.shutdownNow();
        }
    }
}