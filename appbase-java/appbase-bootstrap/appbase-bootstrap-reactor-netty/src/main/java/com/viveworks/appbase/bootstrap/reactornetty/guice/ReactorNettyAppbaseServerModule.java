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

package com.viveworks.appbase.bootstrap.reactornetty.guice;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.reactornetty.server.ReactorNettyAppbaseWebServer;
import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Binds a pre-built {@link ReactorNettyAppbaseWebServer} instance, mirroring
 * {@code JettyAppbaseServerModule}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactorNettyAppbaseServerModule extends AbstractModule {

    private final ReactorNettyAppbaseWebServer reactorNettyAppbaseWebServer;

    /**
     * @param reactorNettyAppbaseWebServer the server instance to bind
     */
    public ReactorNettyAppbaseServerModule(final ReactorNettyAppbaseWebServer reactorNettyAppbaseWebServer) {
        this.reactorNettyAppbaseWebServer =
                AppbaseUtil.notNull(reactorNettyAppbaseWebServer, "reactorNettyAppbaseWebServer");
    }

    @Override
    protected void configure() {
        bind(ReactorNettyAppbaseWebServer.class).toInstance(reactorNettyAppbaseWebServer);
    }
}
