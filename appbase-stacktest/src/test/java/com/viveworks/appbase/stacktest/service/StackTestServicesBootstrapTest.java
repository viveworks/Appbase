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

package com.viveworks.appbase.stacktest.service;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that {@link StackTestServices} exposes the Phase B constructor that
 * accepts pre-built {@link AppbaseClientServices} and {@link AppbaseServices} containers.
 */
class StackTestServicesBootstrapTest {

    @Test
    void bootstrapConstructor_hasCorrectSignature() throws NoSuchMethodException {
        assertNotNull(StackTestServices.class.getDeclaredConstructor(
            AppbaseClientServices.class,
            AppbaseServices.class
        ));
    }

    /**
     * Verifies the delegation chain is wired: null args reach AppbaseUtil.notNull,
     * confirming the constructor validates inputs. Full integration test requires live infrastructure.
     */
    @Test
    void bootstrapConstructor_rejectsNullArguments() {
        assertThrows(AppbaseRuntimeException.class, () ->
                new StackTestServices(null, null));
    }
}
