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

package com.viveworks.appbase.service.web.endpoint.servlet.appbase;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class StatusServletTest {

    /**
     * {@code /status} is read via GET (see {@link StatusServlet#doGet}). HEAD must not run that read.
     * With {@code isHeadSupported()} returning {@code false}, the base {@code doHead} template sends
     * 405 Method Not Allowed -- consistent with the reactive {@code /status} face, where HEAD is not
     * a registered route.
     */
    @Test
    void headIsNotSupportedSoTheBaseServletSends405() {
        Assertions.assertFalse(new StatusServlet().isHeadSupported(),
                "HEAD /status must not be supported; the base servlet then returns 405");
    }
}
