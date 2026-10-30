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

package com.viveworks.appbase.bootstrap.jetty.guice;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.jetty.server.JettyServletContainer;

import java.util.Objects;

/**
 * Guice module that binds {@link JettyServletContainer}.
 *
 * <p>Usage:
 * <pre>{@code
 * Injector injector = Guice.createInjector(
 *     new AppbaseCoreServicesModule(core),
 *     new AppbaseClientServicesModule(clientServices),
 *     new AppbaseServicesModule(appbaseServices),
 *     new JettyAppbaseServerModule(jettyServer));
 * JettyServletContainer server = injector.getInstance(JettyServletContainer.class);
 * server.start();
 * server.join();
 * }</pre>
 *
 * @author Navaneeth Mysore G
 */
public final class JettyAppbaseServerModule extends AbstractModule {

    private final JettyServletContainer server;

    /**
     * Constructs a module backed by an already-built (but not yet started) {@link JettyServletContainer}.
     *
     * @param server the fully-configured server instance
     */
    public JettyAppbaseServerModule(final JettyServletContainer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    protected void configure() {
        bind(JettyServletContainer.class).toInstance(server);
    }
}
