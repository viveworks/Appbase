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

package com.viveworks.appbase.bootstrap.core.factory;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.core.CoreServiceOrder;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistry;
import com.viveworks.appbase.core.service.registry.microprofile.MicroprofileServiceRegistryImpl;

/**
 * Factory that creates the {@link MicroprofileServiceRegistry}.
 *
 * <p>No separate configuration is needed; the implementation delegates to the
 * {@link ServiceRegistry} already present in the partial container.
 */
public final class MicroprofileServiceRegistryFactory
        implements AppbaseCoreServiceFactory<MicroprofileServiceRegistry> {

    /** Creates the factory. */
    public MicroprofileServiceRegistryFactory() { }

    @Override
    public Class<MicroprofileServiceRegistry> serviceType() {
        return MicroprofileServiceRegistry.class;
    }

    @Override
    public int initializationOrder() {
        return CoreServiceOrder.MICROPROFILE_SERVICE_REGISTRY.order();
    }

    @Override
    public MicroprofileServiceRegistry create(AppbaseCoreServices partial) throws AppbaseException {
        ServiceRegistry registry = partial.get(ServiceRegistry.class);
        return new MicroprofileServiceRegistryImpl(registry);
    }
}
