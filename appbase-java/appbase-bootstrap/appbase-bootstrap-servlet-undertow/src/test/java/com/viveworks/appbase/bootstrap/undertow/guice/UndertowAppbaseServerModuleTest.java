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
package com.viveworks.appbase.bootstrap.undertow.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.guice.AppbaseClientServicesModule;
import com.viveworks.appbase.bootstrap.guice.AppbaseCoreServicesModule;
import com.viveworks.appbase.bootstrap.guice.AppbaseServicesModule;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.bootstrap.undertow.server.UndertowServletContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Unit test for {@link UndertowAppbaseServerModule}.
 *
 * <p>Verifies that the module correctly binds {@link UndertowServletContainer} so that
 * {@link Injector#getInstance} returns the same instance that was passed to the module
 * constructor.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
class UndertowAppbaseServerModuleTest {

    @Test
    void undertowModule_bindsUndertowServletContainer() {
        final AppbaseCoreServices   mockCore     = mock(AppbaseCoreServices.class);
        final AppbaseClientServices mockClients  = mock(AppbaseClientServices.class);
        final AppbaseServices       mockServices = mock(AppbaseServices.class);
        final UndertowServletContainer mockServer = mock(UndertowServletContainer.class);

        final Injector injector = Guice.createInjector(
            new AppbaseCoreServicesModule(mockCore),
            new AppbaseClientServicesModule(mockClients),
            new AppbaseServicesModule(mockServices),
            new UndertowAppbaseServerModule(mockServer));

        assertSame(mockServer, injector.getInstance(UndertowServletContainer.class));
    }
}
