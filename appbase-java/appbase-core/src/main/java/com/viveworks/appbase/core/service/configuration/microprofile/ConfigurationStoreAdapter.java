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

package com.viveworks.appbase.core.service.configuration.microprofile;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.configuration.util.ConfigurationUtil;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An adapter from {@link ConfigSource} to {@link ConfigurationStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ConfigurationStoreAdapter implements ConfigurationStore {
    private final ConfigSource configSource;
    private final AtomicBoolean shutdown;

    public ConfigurationStoreAdapter(final ConfigSource configSource) {
        AppbaseUtil.notNull(configSource, "configSource");

        this.configSource = configSource;
        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public Collection<RawConfiguration> readRawConfigurations(
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException
    {
        final Map<String, String> properties = configSource.getProperties();
        final Collection<RawConfiguration> rawConfigurations =
                ConfigurationUtil.properties2RawConfigurations(properties);
        return rawConfigurations;
    }

    @Override
    public String getId() {
        return AppbaseUtil.configurationStoreIdPrefix(this) + configSource.getName();
    }

    @Override
    public void writeRawConfiguration(
            final RawConfiguration rawConfiguration,
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException
    {
        // no-op
    }

    @Override
    public boolean supportsWrite() {
        return false;
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        shutdown.set(true);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }
}
