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

package com.viveworks.appbase.service.health;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.service.DummyAppbaseService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ServiceRegistryHealthCheckerTest {
    private static AppbaseCore appbaseCore;
    private static ServiceRegistryHealthChecker serviceRegistryHealthChecker;

    @BeforeAll
    static void beforeAll() {
        appbaseCore = ClientCoreTestHelper.appbaseCore();
        serviceRegistryHealthChecker = new ServiceRegistryHealthChecker(appbaseCore.getServiceRegistry());
    }

    @Test
    void testRunHealthCheck() {
        final DummyAppbaseService dummyAppbaseService = new DummyAppbaseService();
        final ServiceRegistry serviceRegistry = appbaseCore.getServiceRegistry();
        serviceRegistry.registerAppbaseService(dummyAppbaseService);
        serviceRegistry.pollAndUpdateStatuses();

        final HealthCheck healthCheck1 = serviceRegistryHealthChecker.runHealthCheck();
        Assertions.assertEquals(ServiceRegistry.class.getName(), healthCheck1.getServiceName());
        Assertions.assertTrue(healthCheck1.isHealthy());

        dummyAppbaseService.shutdown();
        serviceRegistry.pollAndUpdateStatuses();

        final HealthCheck healthCheck2 = serviceRegistryHealthChecker.runHealthCheck();
        Assertions.assertEquals(DummyAppbaseService.class.getName(), healthCheck2.getServiceName());
        Assertions.assertFalse(healthCheck2.isHealthy());
    }

    /**
     * Lens 3 F5 / Lens 5 (third pass) R-R1 regression: with no registered services, the loop over
     * {@code getAppbaseServices()} never executes a single iteration and the method falls out of
     * the loop to its final {@code return new HealthCheck(..., true)} -- reporting UP having
     * checked nothing. The pre-existing {@code testRunHealthCheck} arm above never constructs an
     * empty registry (the shared {@code appbaseCore} always has the dummy service registered), so
     * this branch was never reached by any test.
     */
    @Test
    void testRunHealthCheckReportsUnhealthyWhenNoServicesAreRegistered() {
        final ServiceRegistry emptyServiceRegistry = Mockito.mock(ServiceRegistry.class);
        Mockito.when(emptyServiceRegistry.getAppbaseServices()).thenReturn(Collections.emptyList());
        final ServiceRegistryHealthChecker emptyRegistryHealthChecker =
                new ServiceRegistryHealthChecker(emptyServiceRegistry);

        final HealthCheck healthCheck = emptyRegistryHealthChecker.runHealthCheck();

        Assertions.assertFalse(healthCheck.isHealthy(),
                "an empty service registry must not report UP by falling out of the loop having "
                        + "checked nothing");
    }
}