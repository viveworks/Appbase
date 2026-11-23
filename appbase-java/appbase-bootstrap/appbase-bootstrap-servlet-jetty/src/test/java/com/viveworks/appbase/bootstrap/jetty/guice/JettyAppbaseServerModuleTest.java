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

package com.viveworks.appbase.bootstrap.jetty.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.guice.AppbaseClientServicesModule;
import com.viveworks.appbase.bootstrap.guice.AppbaseCoreServicesModule;
import com.viveworks.appbase.bootstrap.guice.AppbaseServicesModule;
import com.viveworks.appbase.bootstrap.jetty.server.JettyServletContainer;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Unit test for {@link JettyAppbaseServerModule}.
 *
 * <p>Verifies that the module correctly binds {@link JettyServletContainer} so that
 * {@link Injector#getInstance} returns the same instance that was passed to the module constructor.
 *
 * @author Navaneeth Mysore G
 */
class JettyAppbaseServerModuleTest {

    @Test
    void jettyModule_bindsJettyServletContainer() {
        AppbaseCoreServices   mockCore     = mock(AppbaseCoreServices.class);
        AppbaseClientServices mockClients  = mock(AppbaseClientServices.class);
        AppbaseServices       mockServices = mock(AppbaseServices.class);
        JettyServletContainer mockServer   = mock(JettyServletContainer.class);
        Injector injector = Guice.createInjector(
            new AppbaseCoreServicesModule(mockCore),
            new AppbaseClientServicesModule(mockClients),
            new AppbaseServicesModule(mockServices),
            new JettyAppbaseServerModule(mockServer));
        assertSame(mockServer, injector.getInstance(JettyServletContainer.class));
    }
}
