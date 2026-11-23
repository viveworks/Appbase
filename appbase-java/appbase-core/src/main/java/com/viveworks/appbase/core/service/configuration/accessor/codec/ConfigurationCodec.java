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

package com.viveworks.appbase.core.service.configuration.accessor.codec;

import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;

import java.util.Collection;
import java.util.Map;

/**
 * Used to encode (serialize) configurations and decode (de-serialize) raw configurations.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ConfigurationCodec {
    /**
     * Decode (de-serialize) raw configuration into configuration.
     *
     * @param rawConfigurations Raw configurations to decode
     * @param configurationAccessorContext Context / metadata used to decode
     * @return Decoded configurations
     * @throws ConfigurationCodecException if the decode failed
     */
    Collection<Configuration> decode(
            Collection<RawConfiguration> rawConfigurations,
            ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationCodecException;


    /**
     * Encode (serialize) configuration into raw configuration.
     *
     * @param configurationMap Configurations to encode
     * @param configurationAccessorContext  Context / metadata used to encode
     * @return Encoded raw configurations
     * @throws ConfigurationCodecException if the encode failed
     */
    Collection<RawConfiguration> encode(
            Map<ConfigurationKey, ConfigurationValue<?>> configurationMap,
            ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationCodecException;
}
