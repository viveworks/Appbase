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

package com.viveworks.appbase.bootstrap.client.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServiceFactory;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.bootstrap.client.ClientServiceOrder;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.client.service.clamav.ClamAVClientService;
import com.viveworks.appbase.core.AppbaseException;
import java.util.Objects;

/**
 * Built-in factory for {@link ClamAVClientService}.
 *
 * <p>Only pre-built construction is supported. {@link com.viveworks.appbase.client.service.clamav.client.ClamAVClientSharedResources}
 * is an interface requiring external setup (thread pools, connections) that cannot be
 * constructed from config alone. Build a {@link com.viveworks.appbase.client.service.clamav.DefaultClamAVClientService}
 * externally and pass it here.
 */
public final class ClamAVClientServiceFactory implements AppbaseClientServiceFactory<ClamAVClientService> {

    private final ClamAVClientService predefinedService;

    /** Wraps a pre-built {@link ClamAVClientService} instance. */
    public ClamAVClientServiceFactory(final ClamAVClientService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
    }

    @Override
    public Class<ClamAVClientService> serviceType() {
        return ClamAVClientService.class;
    }

    @Override
    public int initializationOrder() {
        return ClientServiceOrder.CLAMAV.order();
    }

    @Override
    public ClamAVClientService create(final AppbaseCoreServices coreServices,
                                       final AppbaseClientCore appbaseClientCore) throws AppbaseException {
        return predefinedService;
    }
}
