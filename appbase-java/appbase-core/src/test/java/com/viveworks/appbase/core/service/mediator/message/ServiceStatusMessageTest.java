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

package com.viveworks.appbase.core.service.mediator.message;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ServiceStatusMessageTest {
    @Test
    void testServiceStatusMessage() {
        final ServiceStatusMessage serviceStatusMessage = new ServiceStatusMessage();
        serviceStatusMessage.setServiceType(MediatorService.class);
        serviceStatusMessage.setServiceStatus(ServiceStatus.STARTED);

        final Map<String, String> context = new HashMap<>();
        context.put("a", "b");
        serviceStatusMessage.addAllContext(context);

        Assertions.assertEquals(ServiceStatus.STARTED, serviceStatusMessage.getServiceStatus());
        Assertions.assertEquals(MediatorService.class, serviceStatusMessage.getServiceType());

        final ServiceStatusMessage serviceStatusMessage1 = new ServiceStatusMessage(serviceStatusMessage);
        Assertions.assertEquals(serviceStatusMessage, serviceStatusMessage1);
        Assertions.assertEquals(serviceStatusMessage.hashCode(), serviceStatusMessage1.hashCode());
        Assertions.assertEquals(serviceStatusMessage.toString(), serviceStatusMessage1.toString());

        serviceStatusMessage.clean();
        Assertions.assertTrue(serviceStatusMessage.getContext().isEmpty());
        Assertions.assertNull(serviceStatusMessage.getServiceType());
        Assertions.assertNull(serviceStatusMessage.getServiceStatus());
    }
}
