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

package com.viveworks.appbase.core.service.tracer;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AbstractAppbaseService;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationAddedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationChangedEvent;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListenerException;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationRemovedEvent;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.propagation.JaegerPropagationConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.propagation.JaegerPropagationType;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.InMemoryReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.JaegerReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.RemoteReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.GuaranteedThroughputSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.JaegerSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.PerOperationSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ProbabilisticSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.RateLimitingSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.RemoteControlledSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.scopemanager.JaegerScopeManagerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.scopemanager.MDCScopeManagerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.scopemanager.ThreadLocalScopeManagerConfiguration;
import com.viveworks.appbase.core.service.tracer.reporter.CompositeJaegerReporter;
import com.viveworks.appbase.core.service.tracer.reporter.LoggingJaegerReporter;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.MDCScopeManager;
import io.jaegertracing.internal.metrics.Metrics;
import io.jaegertracing.internal.propagation.B3TextMapCodec;
import io.jaegertracing.internal.propagation.TraceContextCodec;
import io.jaegertracing.internal.reporters.InMemoryReporter;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.internal.samplers.GuaranteedThroughputSampler;
import io.jaegertracing.internal.samplers.HttpSamplingManager;
import io.jaegertracing.internal.samplers.PerOperationSampler;
import io.jaegertracing.internal.samplers.ProbabilisticSampler;
import io.jaegertracing.internal.samplers.RateLimitingSampler;
import io.jaegertracing.internal.samplers.RemoteControlledSampler;
import io.jaegertracing.internal.samplers.http.OperationSamplingParameters;
import io.jaegertracing.internal.samplers.http.PerOperationSamplingParameters;
import io.jaegertracing.internal.samplers.http.ProbabilisticSamplingStrategy;
import io.jaegertracing.spi.MetricsFactory;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import io.jaegertracing.spi.SamplingManager;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.jaegertracing.thrift.internal.senders.UdpSender;
import io.opentracing.ScopeManager;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.util.ThreadLocalScopeManager;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.microprofile.metrics.Counter;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link TracerService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultTracerService extends AbstractAppbaseService
        implements TracerService, ConfigurationListener
{
    private final ValidationService validationService;
    private final AtomicReference<DefaultTracerServiceConfiguration> defaultTracerServiceConfigurationRef;
    private final MetricsFactory metricsFactory;
    private final AtomicReference<Tracer> tracerRef;
    private final Counter configurationAddedCounter;
    private final Counter configurationChangedCounter;
    private final Counter configurationRemovedCounter;
    private final ReentrantLock initLock;

    public DefaultTracerService(final LogService logService,
                                final MetricService metricService,
                                final MediatorService mediatorService,
                                final ValidationService validationService,
                                final ConfigurationService configurationService)
    {
        super(logService, metricService, mediatorService);
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(configurationService, "configurationService");

        this.validationService = validationService;

        DefaultTracerServiceConfiguration defaultTracerServiceConfiguration =
                configurationService.getTypedConfiguration(DefaultTracerServiceConfiguration.class);
        if (defaultTracerServiceConfiguration == null) {
            final TracerServiceConfiguration tracerServiceConfiguration =
                    configurationService.getTypedConfiguration(TracerServiceConfiguration.class);
            if (tracerServiceConfiguration instanceof DefaultTracerServiceConfiguration) {
                defaultTracerServiceConfiguration = (DefaultTracerServiceConfiguration) tracerServiceConfiguration;
            } else {
                throw new AppbaseRuntimeException("Cannot find " +
                        DefaultTracerServiceConfiguration.class.getName() +
                        " in " +
                        ConfigurationService.class.getName());
            }
        }

        AppbaseUtil.validate(validationService, defaultTracerServiceConfiguration);
        this.defaultTracerServiceConfigurationRef = new AtomicReference<>(defaultTracerServiceConfiguration);

        this.metricsFactory = new DefaultJaegerMetricsFactory(logService, metricService);
        this.tracerRef = new AtomicReference<>();
        this.initLock = new ReentrantLock();
        this.configurationAddedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationAdded", "counter"));
        this.configurationChangedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationChanged", "counter"));
        this.configurationRemovedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "configurationRemoved", "counter"));
        try {
            init(defaultTracerServiceConfiguration);
        } catch (TTransportException tte) {
            throw new AppbaseRuntimeException("Error while initializing " + getClass().getName(), tte);
        }
        statusRef.set(ServiceStatus.STARTED);
        configurationService.registerConfigurationListenerByKey(
                this, ConfigurationKey.fromTypeKey(DefaultTracerServiceConfiguration.class));
    }

    private void init(final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration) throws TTransportException {
        AppbaseUtil.notNull(defaultTracerServiceConfiguration, "defaultTracerServiceConfiguration");
        AppbaseUtil.validate(validationService, defaultTracerServiceConfiguration);

        final String serviceName = defaultTracerServiceConfiguration.getServiceName();
        final Configuration jaegerConfiguration = new Configuration(serviceName);

        final List<JaegerReporterConfiguration> jaegerReporterConfigurations =
                defaultTracerServiceConfiguration.getJaegerReporterConfigurations();
        if (jaegerReporterConfigurations == null || jaegerReporterConfigurations.isEmpty()) {
            throw new AppbaseRuntimeException("At least 1 Jaeger reporter must be configured");
        }

        final JaegerPropagationConfiguration jaegerPropagationConfiguration =
                defaultTracerServiceConfiguration.getJaegerPropagationConfiguration();
        AppbaseUtil.validate(validationService, jaegerPropagationConfiguration);

        final Set<JaegerPropagationType> jaegerPropagationTypes =
                jaegerPropagationConfiguration.getJaegerPropagationTypes();

        boolean codecFound = false;
        if (jaegerPropagationTypes.contains(JaegerPropagationType.W3C_TRACE_CONTEXT)) {
            final TraceContextCodec traceContextCodec = new TraceContextCodec.Builder().build();
            final Configuration.CodecConfiguration codecConfiguration = new Configuration.CodecConfiguration();
            codecConfiguration.withCodec(Format.Builtin.HTTP_HEADERS, traceContextCodec);
            jaegerConfiguration.withCodec(codecConfiguration);
            codecFound = true;
        }
        if (jaegerPropagationTypes.contains(JaegerPropagationType.B3)) {
            final B3TextMapCodec b3TextMapCodec = new B3TextMapCodec.Builder().build();
            final Configuration.CodecConfiguration codecConfiguration = new Configuration.CodecConfiguration();
            codecConfiguration.withCodec(Format.Builtin.HTTP_HEADERS, b3TextMapCodec);
            jaegerConfiguration.withCodec(codecConfiguration);
            codecFound = true;
        }

        if (!codecFound) {
            throw new AppbaseRuntimeException("Unsupported trace codecs found " + jaegerPropagationTypes +
                    ". Only " + JaegerPropagationType.W3C_TRACE_CONTEXT + " and " + JaegerPropagationType.B3 + " supported");
        }

        final JaegerTracer.Builder tracerBuilder = jaegerConfiguration.getTracerBuilder();
        final ScopeManager scopeManager;
        final JaegerScopeManagerConfiguration jaegerScopeManagerConfiguration =
                defaultTracerServiceConfiguration.getJaegerScopeManagerConfiguration();
        if (ThreadLocalScopeManagerConfiguration.class.isAssignableFrom(jaegerScopeManagerConfiguration.getClass())) {
            scopeManager = new ThreadLocalScopeManager();
        } else if (MDCScopeManagerConfiguration.class.isAssignableFrom(jaegerScopeManagerConfiguration.getClass())) {
            final MDCScopeManagerConfiguration mdcScopeManagerConfiguration =
                    (MDCScopeManagerConfiguration) jaegerScopeManagerConfiguration;
            scopeManager = new MDCScopeManager
                    .Builder()
                    .withMDCSampledKey(mdcScopeManagerConfiguration.getMdcSampledKey())
                    .withMDCSpanIdKey(mdcScopeManagerConfiguration.getMdcSpanIdKey())
                    .withMDCTraceIdKey(mdcScopeManagerConfiguration.getMdcTraceIdKey())
                    .build();
        } else {
            throw new AppbaseRuntimeException("Unsupported " + jaegerScopeManagerConfiguration.getClass().getName() +
                    ". Only " + ThreadLocalScopeManagerConfiguration.class.getName() + " and " +
                    MDCScopeManagerConfiguration.class.getName() + " supported");
        }
        tracerBuilder.withScopeManager(scopeManager);

        if (jaegerReporterConfigurations.size() > 1) {
            final CompositeJaegerReporter compositeJaegerReporter =
                    new CompositeJaegerReporter(logService, metricService, mediatorService);
            for (final JaegerReporterConfiguration jaegerReporterConfiguration : jaegerReporterConfigurations) {
                final Reporter reporter = newReporter(jaegerReporterConfiguration, jaegerConfiguration);
                compositeJaegerReporter.addReporter(reporter);
            }
            tracerBuilder.withReporter(compositeJaegerReporter);
        } else {
            final JaegerReporterConfiguration jaegerReporterConfiguration = jaegerReporterConfigurations.get(0);
            final Reporter reporter = newReporter(jaegerReporterConfiguration, jaegerConfiguration);
            tracerBuilder.withReporter(reporter);
        }

        final JaegerSamplerConfiguration jaegerSamplerConfiguration =
                defaultTracerServiceConfiguration.getJaegerSamplerConfiguration();
        final Sampler sampler = newSampler(jaegerSamplerConfiguration);
        tracerBuilder.withSampler(sampler);

        if (defaultTracerServiceConfiguration.isUse128BitTraceId()) {
            tracerBuilder.withTraceId128Bit();
        }

        if (defaultTracerServiceConfiguration.isExpandExceptionLogs()) {
            tracerBuilder.withExpandExceptionLogs();
        }

        final Map<String, String> tags = defaultTracerServiceConfiguration.getTags();
        if (tags != null) {
            tracerBuilder.withTags(tags);
        }

        tracerBuilder.withManualShutdown();
        final JaegerTracer jaegerTracer = tracerBuilder.build();
        shutdown();
        tracerRef.set(jaegerTracer);
        defaultTracerServiceConfigurationRef.set(defaultTracerServiceConfiguration);
    }

    private Sampler newSampler(final JaegerSamplerConfiguration jaegerSamplerConfiguration) {
        AppbaseUtil.notNull(jaegerSamplerConfiguration, "jaegerSamplerConfiguration");

        if (jaegerSamplerConfiguration
                .getClass()
                .isAssignableFrom(ConstSamplerConfiguration.class))
        {
            final ConstSamplerConfiguration constSamplerConfiguration =
                    (ConstSamplerConfiguration) jaegerSamplerConfiguration;
            AppbaseUtil.validate(validationService, constSamplerConfiguration);

            return new ConstSampler(constSamplerConfiguration.isDecision());
        } else if (jaegerSamplerConfiguration
                .getClass()
                .isAssignableFrom(GuaranteedThroughputSamplerConfiguration.class))
        {
            final GuaranteedThroughputSamplerConfiguration guaranteedThroughputSamplerConfiguration =
                    (GuaranteedThroughputSamplerConfiguration) jaegerSamplerConfiguration;
            AppbaseUtil.validate(validationService, guaranteedThroughputSamplerConfiguration);

            return new GuaranteedThroughputSampler(
                    guaranteedThroughputSamplerConfiguration.getSamplingRate(),
                    guaranteedThroughputSamplerConfiguration.getLowerBound());
        } else if (jaegerSamplerConfiguration.getClass().isAssignableFrom(PerOperationSamplerConfiguration.class)) {
            final PerOperationSamplerConfiguration perOperationSamplerConfiguration =
                    (PerOperationSamplerConfiguration) jaegerSamplerConfiguration;
            AppbaseUtil.validate(validationService, perOperationSamplerConfiguration);

            final List<PerOperationSamplingParameters> perOperationStrategies = new ArrayList<>();
            final Map<String, Double> samplingRateByOperationNameMap =
                    perOperationSamplerConfiguration.getSamplingRateByOperationNameMap();
            samplingRateByOperationNameMap.forEach((operationName, samplingRate) -> {
                final ProbabilisticSamplingStrategy probabilisticSamplingStrategy =
                        new ProbabilisticSamplingStrategy(samplingRate);
                final PerOperationSamplingParameters perOperationSamplingParameters =
                        new PerOperationSamplingParameters(operationName, probabilisticSamplingStrategy);
                perOperationStrategies.add(perOperationSamplingParameters);
            });

            final double defaultSamplingProbability = perOperationSamplerConfiguration.getDefaultSamplingProbability();
            final double defaultLowerBoundTracesPerSecond =
                    perOperationSamplerConfiguration.getDefaultLowerBoundTracesPerSecond();
            final OperationSamplingParameters operationSamplingParameters =
                    new OperationSamplingParameters(
                            defaultSamplingProbability, defaultLowerBoundTracesPerSecond, perOperationStrategies);

            final int maxOperations = perOperationSamplerConfiguration.getMaxOperations();
            return new PerOperationSampler(maxOperations, operationSamplingParameters);
        } else if (jaegerSamplerConfiguration
                .getClass()
                .isAssignableFrom(ProbabilisticSamplerConfiguration.class))
        {
            final ProbabilisticSamplerConfiguration probabilisticSamplerConfiguration =
                    (ProbabilisticSamplerConfiguration) jaegerSamplerConfiguration;
            AppbaseUtil.validate(validationService, probabilisticSamplerConfiguration);

            return new ProbabilisticSampler(probabilisticSamplerConfiguration.getSamplingRate());
        } else if (jaegerSamplerConfiguration
                .getClass()
                .isAssignableFrom(RateLimitingSamplerConfiguration.class))
        {
            final RateLimitingSamplerConfiguration rateLimitingSamplerConfiguration =
                    (RateLimitingSamplerConfiguration) jaegerSamplerConfiguration;
            AppbaseUtil.validate(validationService, rateLimitingSamplerConfiguration);

            return new RateLimitingSampler(rateLimitingSamplerConfiguration.getMaxTracesPerSecond());
        } else if (jaegerSamplerConfiguration
                .getClass()
                .isAssignableFrom(RemoteControlledSamplerConfiguration.class))
        {
            final RemoteControlledSamplerConfiguration remoteControlledSamplerConfiguration =
                    (RemoteControlledSamplerConfiguration) jaegerSamplerConfiguration;
            AppbaseUtil.validate(validationService, remoteControlledSamplerConfiguration);

            final Metrics metrics = new Metrics(
                    metricsFactory,
                    remoteControlledSamplerConfiguration.getMetricsPrefix());
            final SamplingManager samplingManager =
                    new HttpSamplingManager(remoteControlledSamplerConfiguration.getHostPort());
            return new RemoteControlledSampler
                    .Builder(remoteControlledSamplerConfiguration.getServiceName())
                    .withMetrics(metrics)
                    .withPollingInterval(remoteControlledSamplerConfiguration.getPollingIntervalMillis())
                    .withSamplingManager(samplingManager)
                    .build();
        }

        throw new AppbaseRuntimeException(
                "Unsupported sampler configuration: " + jaegerSamplerConfiguration.getClass().getName());
    }

    private Reporter newReporter(
            final JaegerReporterConfiguration jaegerReporterConfiguration,
            final Configuration jaegerConfiguration)
            throws TTransportException
    {
        AppbaseUtil.notNull(jaegerReporterConfiguration, "jaegerReporterConfiguration");

        if (jaegerReporterConfiguration
                .getClass()
                .isAssignableFrom(InMemoryReporterConfiguration.class))
        {
            final InMemoryReporterConfiguration inMemoryReporterConfiguration =
                    (InMemoryReporterConfiguration) jaegerReporterConfiguration;
            AppbaseUtil.validate(validationService, inMemoryReporterConfiguration);

            if (inMemoryReporterConfiguration.isEnabled()) {
                return new InMemoryReporter();
            }
        } else if (jaegerReporterConfiguration
                .getClass()
                .isAssignableFrom(LoggingReporterConfiguration.class))
        {
            final LoggingReporterConfiguration loggingReporterConfiguration =
                    (LoggingReporterConfiguration) jaegerReporterConfiguration;
            AppbaseUtil.validate(validationService, loggingReporterConfiguration);

            if (loggingReporterConfiguration.isEnabled()) {
                return new LoggingJaegerReporter(logService);
            }
        } else if (jaegerReporterConfiguration.getClass().isAssignableFrom(RemoteReporterConfiguration.class)) {
            final RemoteReporterConfiguration remoteReporterConfiguration =
                    (RemoteReporterConfiguration) jaegerReporterConfiguration;
            AppbaseUtil.validate(validationService, remoteReporterConfiguration);

            final String agentHost = remoteReporterConfiguration.getAgentHost();
            final int agentPort = remoteReporterConfiguration.getAgentPort();
            final int maxPacketSizeBytes = remoteReporterConfiguration.getMaxPacketSizeBytes();
            Sender sender = null;
            if (!AppbaseUtil.isBlank(agentHost)) {
                sender = new UdpSender(agentHost, agentPort, maxPacketSizeBytes);
            }

            final String collectorEndpoint = remoteReporterConfiguration.getCollectorEndpoint();
            if (!AppbaseUtil.isBlank(collectorEndpoint)) {
                final HttpSender.Builder builder = new HttpSender.Builder(collectorEndpoint);
                builder.withMaxPacketSize(maxPacketSizeBytes);

                final String authPassword = remoteReporterConfiguration.getAuthPassword();
                final String authUsername = remoteReporterConfiguration.getAuthUsername();
                final String authToken = remoteReporterConfiguration.getAuthToken();
                if (authUsername != null && authPassword != null) {
                    builder.withAuth(authUsername, authPassword);
                } else if (!AppbaseUtil.isBlank(authToken)) {
                    builder.withAuth(authToken);
                }
                sender = builder.build();
            }

            if (sender == null) {
                throw new AppbaseRuntimeException(
                        "Unable to create Jaeger sender using " + remoteReporterConfiguration);
            }

            jaegerConfiguration.withMetricsFactory(metricsFactory);
            final Metrics metrics = new Metrics(metricsFactory, remoteReporterConfiguration.getMetricsPrefix());
            return new RemoteReporter.Builder()
                    .withCloseEnqueueTimeout(remoteReporterConfiguration.getCloseEnqueueTimeoutMillis())
                    .withFlushInterval(remoteReporterConfiguration.getFlushIntervalMillis())
                    .withMaxQueueSize(remoteReporterConfiguration.getMaxQueueSize())
                    .withSender(sender)
                    .withMetrics(metrics)
                    .build();
        }

        throw new AppbaseRuntimeException(
                "Unsupported reporter configuration: " + jaegerReporterConfiguration.getClass().getName());
    }

    private void initOnNewConfiguration(
            final ConfigurationKey configurationKey,
            final ConfigurationValue<?> configurationValue,
            final Counter counter) throws ConfigurationListenerException
    {
        if (configurationKey == null) {
            throw new ConfigurationListenerException("Configuration key must not be null");
        }
        if (configurationValue == null) {
            throw new ConfigurationListenerException(
                    "Configuration value must not be null for key " + configurationKey);
        }

        final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();
        final Object value = configurationValue.getValue();
        if (typeKey == null) {
            throw new ConfigurationListenerException(
                    "Type key must not be null for " + configurationKey);
        }

        if (value == null) {
            throw new ConfigurationListenerException(
                    "Value must not be null for " + configurationKey);
        }

        if (!typeKey.equals(DefaultTracerServiceConfiguration.class)) {
            throw new ConfigurationListenerException(
                    "Value must be of type " +
                            DefaultTracerServiceConfiguration.class.getName() +
                            ". It is of type " +
                            value.getClass().getName());
        }

        final DefaultTracerServiceConfiguration defaultTracerServiceConfiguration =
                (DefaultTracerServiceConfiguration) value;
        try {
            init(defaultTracerServiceConfiguration);
        } catch (TTransportException tte) {
            throw new AppbaseRuntimeException("Error while initializing " + getClass().getName(), tte);
        }
        updateStatus(ServiceStatus.STARTED);
        counter.inc();
    }

    @Override
    public void onConfigurationAdded(
            final ConfigurationAddedEvent configurationAddedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationAddedEvent, "configurationAddedEvent");
        AppbaseUtil.validate(validationService, configurationAddedEvent);

        logger.info("Received event: {}", configurationAddedEvent);
        initLock.lock();
        try {
            final ConfigurationKey configurationKey = configurationAddedEvent.getConfigurationKey();
            final ConfigurationValue<?> configurationValue = configurationAddedEvent.getConfigurationValue();
            initOnNewConfiguration(configurationKey, configurationValue, configurationAddedCounter);
        } finally {
            initLock.unlock();
        }
    }

    @Override
    public void onConfigurationChanged(
            final ConfigurationChangedEvent configurationChangedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationChangedEvent, "configurationChangedEvent");
        AppbaseUtil.validate(validationService, configurationChangedEvent);

        logger.info("Received event: {}", configurationChangedEvent);
        initLock.lock();
        try {
            final ConfigurationKey configurationKey = configurationChangedEvent.getConfigurationKey();
            final ConfigurationValue<?> newConfigurationValue = configurationChangedEvent.getNewConfigurationValue();
            initOnNewConfiguration(configurationKey, newConfigurationValue, configurationChangedCounter);
        } finally {
            initLock.unlock();
        }
    }

    @Override
    public void onConfigurationRemoved(
            final ConfigurationRemovedEvent configurationRemovedEvent) throws ConfigurationListenerException
    {
        AppbaseUtil.notNull(configurationRemovedEvent, "configurationRemovedEvent");
        AppbaseUtil.validate(validationService, configurationRemovedEvent);

        logger.info("Received event: {}", configurationRemovedEvent);
        initLock.lock();
        try {
            final ConfigurationKey configurationKey = configurationRemovedEvent.getConfigurationKey();
            final Class<? extends AppbaseConfiguration> typeKey = configurationKey.getTypeKey();

            if (typeKey == null || !typeKey.equals(DefaultTracerServiceConfiguration.class)) {
                throw new ConfigurationListenerException(
                        "Type key cannot be null and must be of type " +
                                DefaultTracerServiceConfiguration.class.getName());
            }

            shutdown();
            configurationRemovedCounter.inc();
        } finally {
            initLock.unlock();
        }
    }

    @Override
    public Tracer getTracer() {
        return tracerRef.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultTracerServiceConfiguration getConfiguration() {
        return defaultTracerServiceConfigurationRef.get();
    }

    @Override
    protected <C extends AppbaseConfiguration> void reloadImpl(
            final C configuration) throws ServiceReloadException
    {
        AppbaseUtil.notNull(configuration, "configuration");

        if (!DefaultTracerServiceConfiguration.class.isAssignableFrom(configuration.getClass())) {
            throw new ServiceReloadException("The configuration " +
                    configuration.getClass().getName() +
                    " is not of type " +
                    DefaultTracerServiceConfiguration.class.getName());
        }

        initLock.lock();
        try {
            init((DefaultTracerServiceConfiguration) configuration);
        } catch (TTransportException tte) {
            throw new AppbaseRuntimeException("Error while initializing " + getClass().getName(), tte);
        } finally {
            initLock.unlock();
        }
    }

    @Override
    protected void shutdownImpl() {
        final Tracer tracer = tracerRef.get();
        if (tracer != null) {
            tracer.close();
        }
    }
}
