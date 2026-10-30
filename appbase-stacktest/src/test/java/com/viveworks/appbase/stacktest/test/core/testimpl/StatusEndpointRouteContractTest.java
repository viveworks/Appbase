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

package com.viveworks.appbase.stacktest.test.core.testimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins each status stack-test arm to the endpoint and HTTP method it is intended to exercise.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
class StatusEndpointRouteContractTest {

    @Test
    void jaxrsArmUsesHeadOnStatusResource() {
        final StatusEndpointTestImpl.StatusRequest request =
            StatusEndpointTestImpl.overallStatusRequest(
                true, "http://localhost/status", "http://localhost/status-servlet");

        assertEquals("http://localhost/status/overallStatus", request.url());
        assertTrue(request.head());
    }

    @Test
    void servletArmUsesGetOnStatusServlet() {
        final StatusEndpointTestImpl.StatusRequest request =
            StatusEndpointTestImpl.overallStatusRequest(
                false, "http://localhost/status", "http://localhost/status-servlet");

        assertEquals("http://localhost/status-servlet", request.url());
        assertFalse(request.head());
    }
}
