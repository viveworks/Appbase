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

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.jmx.AppbaseServiceMXBean;

import javax.management.MBeanException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DummyMXBean implements AppbaseServiceMXBean {
    private final AtomicBoolean reloaded;
    private final AtomicBoolean shutdown;

    public DummyMXBean() {
        this.reloaded = new AtomicBoolean(false);
        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public ServiceStatus getStatus() {
        return (shutdown.get()) ? ServiceStatus.SHUTDOWN : ServiceStatus.STARTED;
    }

    @Override
    public void reload() throws MBeanException {
        reloaded.set(true);
    }

    @Override
    public void reload(final String appbaseServiceConfigurationYaml) throws MBeanException {
        reloaded.set(true);
    }

    @Override
    public void shutdown() {
        this.shutdown.set(true);
    }
}
