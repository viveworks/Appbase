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

package com.viveworks.appbase.core.service.configuration.accessor.store;

import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;

import java.util.Collection;

/**
 * Provides read and write operations on a configuration store.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ConfigurationStore extends Shutdownable {
    /**
     * Get the unique ID that represents this store.
     *
     * @return Unique ID
     */
    String getId();

    /**
     * Read raw configurations from the store.
     *
     * @param configurationAccessorContext Context / metadata used to read raw configurations
     * @return Raw configurations
     * @throws ConfigurationStoreException if the read failed
     */
    Collection<RawConfiguration> readRawConfigurations(
            ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException;

    /**
     * Write raw configuration to the store.
     *
     * @param configurationAccessorContext Context / metadata used to write raw configurations
     * @param rawConfiguration Raw configuration to write
     * @throws ConfigurationStoreException if the write failed
     */
    void writeRawConfiguration(
            RawConfiguration rawConfiguration,
            ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException;


    /**
     * Check if the store allows writing the configuration.
     *
     * @return true if write is allowed, false otherwise
     */
    boolean supportsWrite();
}
