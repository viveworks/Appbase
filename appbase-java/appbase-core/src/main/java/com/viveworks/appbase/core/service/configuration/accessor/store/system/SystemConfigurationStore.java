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

package com.viveworks.appbase.core.service.configuration.accessor.store.system;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodecException;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.ConfigurationStoreException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A configuration store to read / write from / to system properties and read-only system environment variables.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressFBWarnings({
        "CRLF_INJECTION_LOGS",
        "IMC_IMMATURE_CLASS_NO_TOSTRING",
        "DRE_DECLARED_RUNTIME_EXCEPTION",
        "WEM_WEAK_EXCEPTION_MESSAGING"
})
@ThreadSafe
public class SystemConfigurationStore implements ConfigurationStore {
    private static final String COMMENT = "Written by " + SystemConfigurationStore.class.getName();

    private final Logger logger;
    private final String id;
    private final Histogram systemPropertiesSizeHistogram;
    private final Histogram environmentVariablesSizeHistogram;
    private final Histogram readBytesHistogram;
    private final Timer readTimer;
    private final Histogram writeBytesHistogram;
    private final Timer writeTimer;

    private final AtomicBoolean shutdown;

    public SystemConfigurationStore(final LogService logService, final MetricService metricService) {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");

        this.logger = logService.getLogger(getClass());
        this.logger.info("Initializing {}", getClass().getName());

        this.id = AppbaseUtil.configurationStoreId(this);
        this.systemPropertiesSizeHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "systemProperties", "size", "histogram"));
        this.environmentVariablesSizeHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "environmentVariables", "size", "histogram"));
        this.readBytesHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "readBytes", "histogram"));
        this.readTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "read", "timer"));
        this.writeBytesHistogram = metricService
                .getMetricRegistry()
                .histogram(metricService.name(getClass(), "writeBytes", "histogram"));
        this.writeTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "write", "timer"));

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Collection<RawConfiguration> readRawConfigurations(
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException
    {
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        final Timer.Context context = readTimer.time();
        try {
            logger.debug("Reading raw configurations");
            final Collection<RawConfiguration> rawConfigurations = new ArrayList<>();
            final Properties properties = new Properties();
            final Properties systemProperties = System.getProperties();
            if (systemProperties != null) {
                properties.putAll(systemProperties);
                systemPropertiesSizeHistogram.update(systemProperties.size());
            }

            final Map<String, String> environment = System.getenv();
            if (environment != null) {
                properties.putAll(environment);
                environmentVariablesSizeHistogram.update(environment.size());
            }

            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                properties.store(byteArrayOutputStream, COMMENT);
            } catch (IOException ioe) {
                throw new ConfigurationStoreException("Failed to write system properties", ioe);
            }

            final byte[] bytes = byteArrayOutputStream.toByteArray();
            final RawConfiguration rawConfiguration = RawConfiguration
                    .builder()
                    .timestamp(System.currentTimeMillis())
                    .mimeType(MimeType.PROPERTIES)
                    .value(bytes)
                    .name(SystemConfigurationStore.class.getName())
                    .build();
            rawConfigurations.add(rawConfiguration);
            readBytesHistogram.update(bytes.length);

            return rawConfigurations;
        } finally {
            context.stop();
        }
    }

    @Override
    public void writeRawConfiguration(
            final RawConfiguration rawConfiguration,
            final ConfigurationAccessorContext configurationAccessorContext) throws ConfigurationStoreException
    {
        AppbaseUtil.notNull(rawConfiguration, "rawConfiguration");
        AppbaseUtil.notNull(configurationAccessorContext, "configurationAccessorContext");

        final Timer.Context context = writeTimer.time();
        try {
            logger.debug("Writing raw configuration with name: {}", rawConfiguration.getName());
            final ConfigurationCodec configurationConverter =
                    configurationAccessorContext.findConfigurationCodec(MimeType.PROPERTIES);
            if (configurationConverter == null) {
                throw new ConfigurationStoreException("Cannot find converter for properties");
            }

            final Collection<Configuration> configurations;
            try {
                configurations = configurationConverter.decode(
                        List.of(rawConfiguration), configurationAccessorContext);
            } catch (ConfigurationCodecException cce) {
                throw new ConfigurationStoreException(
                        "Error decoding raw configuration " + rawConfiguration.getName(), cce);
            }

            for (final Configuration configuration : configurations) {
                final ConfigurationKey configurationKey = configuration.getConfigurationKey();
                final ConfigurationValue configurationValue = configuration.getConfigurationValue();
                final Object value = configurationValue.getValue();
                if (value != null) {
                    System.setProperty(configurationKey.getPropertyKey(), (String) value);
                } else {
                    throw new ConfigurationStoreException("Configuration key and value must not be null");
                }
            }

            final byte[] value = rawConfiguration.getValue();
            if (value != null) {
                writeBytesHistogram.update(value.length);
            }
        } finally {
            context.stop();
        }
    }

    @Override
    public boolean supportsWrite() {
        return true;
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        shutdown.set(true);
    }

    @Override
    public String toString() {
        return "SystemConfigurationStore{" +
                "id='" + id + '\'' +
                '}';
    }
}
