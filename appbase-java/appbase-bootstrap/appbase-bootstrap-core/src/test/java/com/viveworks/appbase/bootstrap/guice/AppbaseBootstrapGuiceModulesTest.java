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

package com.viveworks.appbase.bootstrap.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the non-Jetty bootstrap Guice modules.
 *
 * <p>Each test verifies that the module correctly binds its container type so that
 * {@link Injector#getInstance} returns the same instance that was passed to the module constructor.
 *
 * <p>The Jetty-specific module test lives in
 * {@code appbase-bootstrap-servlet-jetty} as
 * {@code com.viveworks.appbase.bootstrap.jetty.guice.JettyAppbaseServerModuleTest}.
 */
class AppbaseBootstrapGuiceModulesTest {

    @Test
    void coreServicesModule_bindsAppbaseCoreServices() {
        AppbaseCoreServices mockCore = mock(AppbaseCoreServices.class);
        Injector injector = Guice.createInjector(new AppbaseCoreServicesModule(mockCore));
        assertSame(mockCore, injector.getInstance(AppbaseCoreServices.class));
    }

    @Test
    void clientServicesModule_bindsAppbaseClientServices() {
        AppbaseCoreServices   mockCore    = mock(AppbaseCoreServices.class);
        AppbaseClientServices mockClients = mock(AppbaseClientServices.class);
        Injector injector = Guice.createInjector(
            new AppbaseCoreServicesModule(mockCore),
            new AppbaseClientServicesModule(mockClients));
        assertSame(mockClients, injector.getInstance(AppbaseClientServices.class));
    }

    @Test
    void servicesModule_bindsAppbaseServices() {
        AppbaseCoreServices   mockCore     = mock(AppbaseCoreServices.class);
        AppbaseClientServices mockClients  = mock(AppbaseClientServices.class);
        AppbaseServices       mockServices = mock(AppbaseServices.class);
        Injector injector = Guice.createInjector(
            new AppbaseCoreServicesModule(mockCore),
            new AppbaseClientServicesModule(mockClients),
            new AppbaseServicesModule(mockServices));
        assertSame(mockServices, injector.getInstance(AppbaseServices.class));
    }

}
