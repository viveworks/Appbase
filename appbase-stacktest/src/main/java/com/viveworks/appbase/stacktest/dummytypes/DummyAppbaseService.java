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
package com.viveworks.appbase.stacktest.dummytypes;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DummyAppbaseService implements AppbaseService {
    private final AtomicBoolean reloaded;
    private final AtomicBoolean shutdown;

    public DummyAppbaseService() {
        this.reloaded = new AtomicBoolean(false);
        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public <C extends AppbaseConfiguration> void reload(
            final C configuration) throws ServiceReloadException
    {
        reloaded.set(true);
    }

    @Override
    public ServiceStatus getStatus() {
        return (shutdown.get()) ? ServiceStatus.SHUTDOWN : ServiceStatus.STARTED;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends AppbaseConfiguration> C getConfiguration() {
        return (C) new DummyAppbaseServiceConfiguration1("localhost", 8080);
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        this.shutdown.set(true);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    public boolean isReloaded() {
        return reloaded.get();
    }
}
