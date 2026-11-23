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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An adapter from {@link ConfigurationStore} to {@link ConfigSource}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ConfigSourceAdapter implements ConfigSource {
    private final ConfigurationStore configurationStore;
    private final ConfigurationAccessorContext configurationAccessorContext;
    private final AtomicReference<Map<String, String>> propertiesRef;

    public ConfigSourceAdapter(final ConfigurationStore configurationStore,
                               final ConfigurationAccessorContext configurationAccessorContext)
    {
        AppbaseUtil.notNull(configurationStore, "configurationStore");
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        this.configurationStore = configurationStore;
        this.configurationAccessorContext = configurationAccessorContext;
        this.propertiesRef = new AtomicReference<>(new ConcurrentHashMap<>());
        reload();
    }

    /**
     * Reload properties from the underlying {@link ConfigurationStore}.
     */
    public void reload() {
        try {
            final Collection<RawConfiguration> rawConfigurations =
                    configurationStore.readRawConfigurations(configurationAccessorContext);

            final Map<String, String> newProperties = ConfigurationUtil.rawConfigurations2Properties(rawConfigurations);
            if (newProperties != null) {
                reload(newProperties);
            }
        } catch (ConfigurationStoreException cse) {
            throw new AppbaseRuntimeException("Error while reading raw configurations from " + configurationStore, cse);
        }
    }

    /**
     * Reload properties using the the <code>properties</code> argument.
     *
     * @param properties New properties to reload to.
     */
    public void reload(final Map<String, String> properties) {
        AppbaseUtil.notNull(properties, "properties");

        final Map<String, String> currentProperties = propertiesRef.get();
        propertiesRef.set(properties);
        currentProperties.clear();
    }

    @Override
    public Map<String, String> getProperties() {
        return propertiesRef.get();
    }

    @Override
    public String getValue(final String propertyName) {
        return getProperties().get(propertyName);
    }

    @Override
    public String getName() {
        return configurationStore.getClass().getName();
    }
}
