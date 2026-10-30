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

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;

import java.util.Objects;

/**
 * Guice module that binds {@link AppbaseServices}.
 *
 * <p>Individual application service bindings are intentionally omitted because application
 * services are optional. Inject {@link AppbaseServices} and call {@code get(XxxService.class)}
 * to retrieve individual services.
 *
 * <p>Usage:
 * <pre>{@code
 * Injector injector = Guice.createInjector(
 *     new AppbaseCoreServicesModule(core),
 *     new AppbaseClientServicesModule(clientServices),
 *     new AppbaseServicesModule(appbaseServices));
 * AppbaseServices services = injector.getInstance(AppbaseServices.class);
 * IdService idService = services.get(IdService.class);
 * }</pre>
 */
public final class AppbaseServicesModule extends AbstractModule {

    private final AppbaseServices appbaseServices;

    /**
     * Constructs a module backed by an already-built {@link AppbaseServices}.
     *
     * @param appbaseServices the fully-built application services container
     */
    public AppbaseServicesModule(final AppbaseServices appbaseServices) {
        this.appbaseServices = Objects.requireNonNull(appbaseServices, "appbaseServices");
    }

    @Override
    protected void configure() {
        bind(AppbaseServices.class).toInstance(appbaseServices);
    }
}
