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

package com.viveworks.appbase.core.service.configuration.accessor.codec;

import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.ThreadSafe;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * Encoder (serializer) / Decoder (de-serializer) for {@link Properties}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class PropertiesConfigurationCodec implements ConfigurationCodec {
    private static final String COMMENT = "Written by " + PropertiesConfigurationCodec.class.getName();

    @Override
    @SuppressFBWarnings("PCAIL_POSSIBLE_CONSTANT_ALLOCATION_IN_LOOP")
    public Collection<Configuration> decode(
            final Collection<RawConfiguration> rawConfigurations,
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationCodecException
    {
        AppbaseUtil.notNull(rawConfigurations, "rawConfigurations");
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        final Collection<Configuration> configurations = new ArrayList<>();

        for (final RawConfiguration rawConfiguration : rawConfigurations) {
            final byte[] rawConfigurationValue = rawConfiguration.getValue();
            final Properties properties = new Properties();
            try {
                properties.load(new ByteArrayInputStream(rawConfigurationValue));
            } catch (IOException ioe) {
                throw new ConfigurationCodecException(
                        "Error while decoding properties for " + rawConfiguration.getName(), ioe);
            }

            properties.forEach((k, v) -> {
                final ConfigurationKey configurationKey = ConfigurationKey.fromPropertyKey((String) k);
                final ConfigurationValue<?> configurationValue = ConfigurationValue.fromValue(v);
                final Configuration configuration = Configuration
                        .builder()
                        .configurationKey(configurationKey)
                        .configurationValue(configurationValue)
                        .rawConfiguration(rawConfiguration)
                        .build();
                configurations.add(configuration);
            });
        }

        return configurations;
    }

    @Override
    @SuppressFBWarnings("WEM_WEAK_EXCEPTION_MESSAGING")
    public Collection<RawConfiguration> encode(
            final Map<ConfigurationKey, ConfigurationValue<?>> configurationMap,
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationCodecException
    {
        AppbaseUtil.notNull(configurationMap, "configurationMap");
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        final Collection<RawConfiguration> rawConfigurations = new ArrayList<>();

        final Properties properties = new Properties();
        for (final Map.Entry<ConfigurationKey, ConfigurationValue<?>> entry : configurationMap.entrySet()) {
            final ConfigurationKey configurationKey = entry.getKey();
            final String propertyKey = configurationKey.getPropertyKey();

            final ConfigurationValue<?> configurationValue = entry.getValue();
            final Object value = configurationValue.getValue();

            if (value != null) {
                if (Properties.class.isAssignableFrom(value.getClass())) {
                    properties.putAll((Map<?, ?>) value);
                } else {
                    properties.setProperty(propertyKey, (String) value);
                }
            }
        }

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            properties.store(byteArrayOutputStream, COMMENT);
        } catch (IOException ioe) {
            throw new ConfigurationCodecException("Failed to write properties", ioe);
        }

        final long timestamp = System.currentTimeMillis();
        final byte[] value = byteArrayOutputStream.toByteArray();
        final RawConfiguration rawConfiguration = RawConfiguration
                .builder()
                .mimeType(MimeType.PROPERTIES)
                .timestamp(timestamp)
                .value(value)
                .name(Properties.class.getName())
                .build();
        rawConfigurations.add(rawConfiguration);

        return rawConfigurations;
    }
}
