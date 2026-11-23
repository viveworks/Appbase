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

package com.viveworks.appbase.bootstrap.guice;

import com.google.inject.AbstractModule;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;

import java.util.Objects;

/**
 * Guice module that binds {@link AppbaseClientServices}.
 *
 * <p>Individual client service bindings are intentionally omitted because client services are
 * optional. Inject {@link AppbaseClientServices} and call {@code get(XxxService.class)} to
 * retrieve individual services.
 *
 * <p>Usage:
 * <pre>{@code
 * Injector injector = Guice.createInjector(
 *     new AppbaseCoreServicesModule(core),
 *     new AppbaseClientServicesModule(clientServices));
 * AppbaseClientServices clients = injector.getInstance(AppbaseClientServices.class);
 * AerospikeClientService aerospike = clients.get(AerospikeClientService.class);
 * }</pre>
 */
public final class AppbaseClientServicesModule extends AbstractModule {

    private final AppbaseClientServices clientServices;

    /**
     * Constructs a module backed by an already-built {@link AppbaseClientServices}.
     *
     * @param clientServices the fully-built client services container
     */
    public AppbaseClientServicesModule(final AppbaseClientServices clientServices) {
        this.clientServices = Objects.requireNonNull(clientServices, "clientServices");
    }

    @Override
    protected void configure() {
        bind(AppbaseClientServices.class).toInstance(clientServices);
    }
}
