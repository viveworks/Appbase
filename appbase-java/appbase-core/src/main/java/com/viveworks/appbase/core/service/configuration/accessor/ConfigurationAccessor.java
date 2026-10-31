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

package com.viveworks.appbase.core.service.configuration.accessor;

import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import org.eclipse.microprofile.config.spi.ConfigSource;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Represents a configuration accessor that can read and write {@link Configuration}s.
 *
 * <p>
 *     Typically an accessor uses {@link ConfigurationStore} to read and write {@link RawConfiguration}s. It uses
 *     {@link ConfigurationCodec}s to decode / de-serialize raw configurations into configurations and vice-versa.
 * </p>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ConfigurationAccessor extends Shutdownable, ConfigurationAccessorRegistry {
    /**
     * Read configurations.
     *
     * @return Configurations read
     * @throws ConfigurationAccessorException if read failed
     */
    Collection<Configuration> readConfigurations() throws ConfigurationAccessorException;

    /**
     * Reactively read configurations — the non-blocking mirror of {@link #readConfigurations()}, fanning
     * across every registered {@link ConfigurationStore} through the N01 reactive read boundary (config is
     * offload-only; there is no Lucene-backed reactive variant) and returning the same flat, decoded
     * {@link Configuration} collection. Unlike the blocking read it is side-effect-free — it does not
     * update the read-size histograms or reload the microprofile config sources (mirrors the reactive
     * read paths of the Log and Exception accessors).
     *
     * @return a {@link Mono} that emits the decoded configurations across all stores, or errors
     */
    Mono<Collection<Configuration>> readConfigurationsReactive();


    /**
     * Write configuration.
     *
     * @param configurationKey Configuration key to write
     * @param configurationValue Configuration value to write
     * @param mimeType Mime type to write the configuration as
     * @throws ConfigurationAccessorException if write failed
     */
    void writeConfiguration(
            ConfigurationKey configurationKey,
            ConfigurationValue<?> configurationValue,
            MimeType mimeType) throws ConfigurationAccessorException;




    /**
     * Support <code>Microprofile</code> {@link ConfigSource}s.
     *
     * @return Config sources
     */
    Iterable<ConfigSource> getConfigSources();
}
