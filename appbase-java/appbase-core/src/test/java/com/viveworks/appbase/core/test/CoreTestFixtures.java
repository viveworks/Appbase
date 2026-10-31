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

package com.viveworks.appbase.core.test;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.DefaultAppbaseCore;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.di.guice.AppbaseCoreGuiceModule;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedService;
import com.viveworks.appbase.core.service.configuration.AppbaseManagedService;
import com.viveworks.appbase.core.service.configuration.AppbaseManagedServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.Configuration;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.ConfigurationValue;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.MimeType;
import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.configuration.accessor.DefaultConfigurationAccessor;
import com.viveworks.appbase.core.service.configuration.accessor.codec.ConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.codec.PropertiesConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.codec.YamlConfigurationCodec;
import com.viveworks.appbase.core.service.configuration.accessor.store.filesystem.ClasspathConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.jpa.JpaConfigurationStore;
import com.viveworks.appbase.core.service.configuration.accessor.store.jpa.JpaConfigurationStoreConfiguration;
import com.viveworks.appbase.core.service.exception.DefaultExceptionServiceConfiguration;
import com.viveworks.appbase.core.service.exception.accessor.DefaultExceptionAccessor;
import com.viveworks.appbase.core.service.exception.accessor.DefaultExceptionAccessorConfiguration;
import com.viveworks.appbase.core.service.exception.store.DefaultExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.DefaultStackTraceHashGenerator;
import com.viveworks.appbase.core.service.exception.store.EntityManagerConfiguration;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageContextCodec;
import com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails;
import com.viveworks.appbase.core.service.exception.store.jpa.JpaExceptionStore;
import com.viveworks.appbase.core.service.exception.store.jpa.JpaExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.jmx.AbstractAppbaseServiceMXBean;
import com.viveworks.appbase.core.service.jmx.DefaultJmxServiceConfiguration;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.log.accessor.DefaultLogAccessorConfiguration;
import com.viveworks.appbase.core.service.log.accessor.LogRecord;
import com.viveworks.appbase.core.service.log.accessor.ThrowableRecord;
import com.viveworks.appbase.core.service.log.appender.disruptor.DisruptorMediatorEventDelegatingAppenderConfiguration;
import com.viveworks.appbase.core.service.log.appender.lucene.LuceneAppenderConfiguration;
import com.viveworks.appbase.core.service.log.common.TurboLog;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndex;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.filter.burst.BurstTurboLogEventFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.duplicate.DuplicateLogTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.event.DisruptorLogAndTurboLogEventFilterDelegatorConfiguration;
import com.viveworks.appbase.core.service.log.filter.event.listener.lucene.LuceneLogAndTurboLogEventFilterListenerConfiguration;
import com.viveworks.appbase.core.service.log.filter.metrics.MetricsTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.jmx.JmxLogServiceMXBean;
import com.viveworks.appbase.core.service.mediator.DefaultMediator;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.MessageHandler;
import com.viveworks.appbase.core.service.mediator.Schedulable;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventFactory;
import com.viveworks.appbase.core.service.mediator.event.delegator.DefaultMediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.event.delegator.DisruptorMediatorEventHandlerDelegator;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageObjectFactory;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.mediator.message.ServiceStatusMessage;
import com.viveworks.appbase.core.service.mediator.queues.DefaultMessageQueues;
import com.viveworks.appbase.core.service.mediator.queues.DefaultMessageQueuesCleaner;
import com.viveworks.appbase.core.service.mediator.queues.DefaultMessageQueuesConfiguration;
import com.viveworks.appbase.core.service.mediator.queues.MessageQueueType;
import com.viveworks.appbase.core.service.mediator.queues.MessageQueues;
import com.viveworks.appbase.core.service.mediator.queues.MessageQueuesCleaner;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.DefaultMetricServiceConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.read.DefaultReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundaryConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.DefaultReactorServiceConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.registry.DefaultServiceRegistryConfiguration;
import com.viveworks.appbase.core.service.tracer.DefaultTracerService;
import com.viveworks.appbase.core.service.tracer.configuration.DefaultTracerServiceConfiguration;
import com.viveworks.appbase.core.service.validation.DefaultValidationService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.disruptor.DefaultDisruptorService;
import com.viveworks.appbase.core.util.disruptor.DefaultExceptionHandler;
import com.viveworks.appbase.core.util.disruptor.DisruptorService;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;
import com.viveworks.appbase.core.util.objectpool.ObjectPool;
import com.viveworks.appbase.core.util.objectpool.Poolable;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPool;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.junit.jupiter.api.Assertions;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.Mockito.mock;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class CoreTestFixtures {
    public static final int BOUND_MESSAGE_QUEUE_SIZE = 4;
    public static final long OLD_MESSAGE_THRESHOLD_MILLIS = 1000L;
    public static final int CONTAINER_SIZE = 16;
    private static MediatorEventDelegatorRegistry sharedMediatorEventDelegatorRegistry;
    private static DisruptorService<MediatorEvent> sharedDisruptorService;

    static {
        sharedMediatorEventDelegatorRegistry = new DefaultMediatorEventDelegatorRegistry();
        sharedDisruptorService = new DefaultDisruptorService<>(
                DefaultDisruptorServiceConfiguration.builder().build(),
                new MediatorEventFactory(),
                new DefaultExceptionHandler<>(),
                new DisruptorMediatorEventHandlerDelegator(sharedMediatorEventDelegatorRegistry));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sharedDisruptorService.shutdown();
            sharedMediatorEventDelegatorRegistry.clearMediatorEventListeners();
        }));
    }

    private CoreTestFixtures() {
    }

    private static void addShutdownHook(final LogService logService) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (logService instanceof DefaultLogService) {
                final DefaultLogService defaultLogService = (DefaultLogService) logService;
                final LuceneService luceneService = defaultLogService.getLuceneService();
                if (luceneService != null) {
                    luceneService.shutdown();
                }
            }
        }));
    }

    public static DisruptorService<MediatorEvent> getSharedDisruptorService() {
        return sharedDisruptorService;
    }

    public static MediatorEventDelegatorRegistry getMediatorEventDelegatorRegistry() {
        return new DefaultMediatorEventDelegatorRegistry();
    }

    public static DefaultLogService getPlainLogService() {
        final DefaultLogServiceConfiguration defaultLogServiceConfiguration =
                DefaultLogServiceConfiguration
                .builder()
                .duplicateLogTurboFilterHandlerConfiguration(
                        DuplicateLogTurboFilterHandlerConfiguration
                                .builder()
                                .enabled(false)
                                .build())
                .defaultLogAccessorConfiguration(
                        DefaultLogAccessorConfiguration
                                .builder()
                                .enabled(false)
                                .build())
                .luceneLogAndTurboLogEventFilterListenerConfiguration(
                        LuceneLogAndTurboLogEventFilterListenerConfiguration
                                .builder()
                                .enabled(false)
                                .build())
                .burstLogTurboFilterHandlerConfiguration(
                        BurstTurboLogEventFilterHandlerConfiguration
                                .builder()
                                .enabled(false)
                                .build())
                .disruptorLogAndTurboLogEventFilterDelegatorConfiguration(
                        DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                                .builder()
                                .enabled(false)
                                .build())
                .luceneAppenderConfiguration(
                        LuceneAppenderConfiguration
                                .builder()
                                .enabled(false)
                                .build())
                .metricsTurboFilterHandlerConfiguration(
                        MetricsTurboFilterHandlerConfiguration
                                .builder()
                                .enabled(false)
                                .build())
                .disruptorMediatorEventDelegatingAppenderConfiguration(
                        DisruptorMediatorEventDelegatingAppenderConfiguration
                                .builder()
                                .enabled(false)
                                .build())
                .build();
        return new DefaultLogService(getMediatorEventDelegatorRegistry(), defaultLogServiceConfiguration);
    }

    public static DefaultLogService getLogService() {
        return new DefaultLogService(
                getMediatorEventDelegatorRegistry(),
                DefaultLogServiceConfiguration.builder().build());
    }

    public static MediatorService getMediatorService(
            final LogService logService,
            final MetricService metricService,
            final ValidationService validationService)
    {
        final String classpathFile1 = "classpath:///appbaseBootstrapConfiguration.yaml";
        System.setProperty(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, classpathFile1);
        return new DefaultMediatorService(
                logService,
                metricService,
                getSharedDisruptorService(),
                getMediatorEventDelegatorRegistry(),
                validationService);
    }

    public static DefaultMetricService getMetricService(final LogService logService) {
        return new DefaultMetricService(
                logService,
                DefaultMetricServiceConfiguration.builder().build());
    }

    public static ValidationService getValidationService(
            final LogService logService,
            final MetricService metricService)
    {
        return new DefaultValidationService(logService, metricService);
    }

    /**
     * A {@link ReactiveReadBoundary} backed by a mocked {@link ReactorService} and default configuration
     * (blank {@code offloadSchedulerId}, so it resolves to {@link reactor.core.scheduler.Schedulers#boundedElastic()}
     * without ever calling the mock) — sufficient for any test that needs a real, working boundary but has
     * no need of a fully bootstrapped {@link ReactorService}.
     *
     * @return a working {@link ReactiveReadBoundary}
     */
    public static ReactiveReadBoundary getReactiveReadBoundary() {
        return new DefaultReactiveReadBoundary(
                ReactiveReadBoundaryConfiguration.builder().build(),
                mock(ReactorService.class));
    }

    public static ConfigurationAccessorContext getConfigurationAccessorContext(
            final LogService logService,
            final MetricService metricService)
    {
        final Map<MimeType, ConfigurationCodec> configurationCodecByMimeTypeMapping = new HashMap<>();
        configurationCodecByMimeTypeMapping.put(MimeType.PROPERTIES, new PropertiesConfigurationCodec());

        final YamlConfigurationCodec yamlConfigurationCodec = new YamlConfigurationCodec();
        configurationCodecByMimeTypeMapping.put(MimeType.YAML, yamlConfigurationCodec);

        final Map<String, Class<? extends AppbaseConfiguration>>
                configurationTypeByRawConfigurationNameMapping = new HashMap<>();

        return new ConfigurationAccessorContext(
                configurationCodecByMimeTypeMapping,
                configurationTypeByRawConfigurationNameMapping,
                new ConcurrentHashMap<>());
    }

    public static ConfigurationKey getConfigurationKey() {
        return ConfigurationKey.fromPropertyKey("zzz");
    }

    public static ConfigurationValue<String> getStringConfigurationValue() {
        return ConfigurationValue.fromValue("localhost");
    }

    public static RawConfiguration getRawConfiguration() {
        return RawConfiguration
                .builder()
                .name("name")
                .value("{'key': 'value'}".getBytes())
                .mimeType(MimeType.JSON)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static Configuration getConfiguration() {
        final ConfigurationKey configurationKey = getConfigurationKey();
        final ConfigurationValue<String> configurationValue = getStringConfigurationValue();
        final RawConfiguration rawConfiguration = getRawConfiguration();
        return Configuration
                .builder()
                .configurationKey(configurationKey)
                .configurationValue(configurationValue)
                .rawConfiguration(rawConfiguration)
                .build();
    }

    public static DefaultConfigurationService getEmptyDefaultConfigurationService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService)
    {
        final ConfigurationAccessor configurationAccessor = new DefaultConfigurationAccessor(
                logService,
                metricService,
                new HashMap<>(),
                new HashMap<>(),
                new ArrayList<>(),
                new HashMap<>());
        return new DefaultConfigurationService(
                logService,
                metricService,
                mediatorService,
                validationService,
                configurationAccessor,
                DefaultConfigurationServiceConfiguration
                        .builder()
                        .enableSchedulableConfigurationAccessor(false)
                        .configurationAccessorSchedulableConfiguration(null)
                        .build());
    }

    public static DefaultAppbaseCore getAppbaseCore(
            final LogService logService, final String classpathFile)
    {
        System.setProperty(
                AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, classpathFile);
        final ClasspathConfigurationStore classpathConfigurationStore =
                new ClasspathConfigurationStore(classpathFile, MimeType.YAML);

        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry = new DefaultAppbaseCoreBootstrapRegistry();
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultServiceRegistryConfiguration.class,
                DefaultServiceRegistryConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultExceptionServiceConfiguration.class,
                DefaultExceptionServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultTracerServiceConfiguration.class,
                DefaultTracerServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultReactorServiceConfiguration.class,
                DefaultReactorServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationTypeByYamlNodeNameToDecode(
                DefaultJmxServiceConfiguration.class,
                DefaultJmxServiceConfiguration.DEFAULT_YAML_NODE_NAME);
        appbaseCoreBootstrapRegistry.registerConfigurationCodecByMimeType(
                new YamlConfigurationCodec(), MimeType.YAML);
        appbaseCoreBootstrapRegistry.registerConfigurationStore(classpathConfigurationStore);
        appbaseCoreBootstrapRegistry.setExceptionMessageContextCodec(new DefaultExceptionMessageContextCodec());
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap =
                new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, classpathFile);

        final DefaultAppbaseCore defaultAppbaseCore = new DefaultAppbaseCore(appbaseCoreBootstrap);
        addShutdownHook(logService);
        return defaultAppbaseCore;
    }

    public static DefaultAppbaseCore getAppbaseCore(final LogService logService) {
        return getAppbaseCore(logService, "classpath:///appbaseBootstrapConfiguration.yaml");
    }

    public static AppbaseCoreGuiceModule getCoreModule(final LogService logService) {
        final AppbaseCore appbaseCore = getAppbaseCore(logService);
        return new AppbaseCoreGuiceModule(appbaseCore);
    }

    public static DefaultAppbaseCore getAppbaseCoreWithJpaExceptionStore(final LogService logService) {
        return getAppbaseCore(logService, "classpath:///appbaseBootstrapConfigurationWithJpa.yaml");
    }

    public static Mediator getMediator(
            final LogService logService,
            final MetricService metricService,
            final ValidationService validationService)
    {
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (t, e) -> {
            throw new AppbaseRuntimeException(
                    "Uncaught exception in mediator's scheduler task [thread=" + t.getName() + "]", e);
        };
        final ThreadFactory threadFactory = AppbaseUtil.newThreadFactory(
                ThreadFactoryConfiguration
                .builder()
                .nameFormat("appbase-core-mediator-%d")
                .daemon(false)
                .enableThreadAffinity(false)
                .build(),
                uncaughtExceptionHandler);
        final ScheduledExecutorService scheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor(threadFactory);

        final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                DefaultMediatorServiceConfiguration.builder().build();

        final ObjectPool<Message> messageObjectPool =
                new ManyToManyQueuedFixedObjectPool<>(
                        metricService,
                        defaultMediatorServiceConfiguration.getMessageObjectPoolCapacity(), new MessageObjectFactory());

        final MessageQueues messageQueues =
                new DefaultMessageQueues(
                        metricService,
                        defaultMediatorServiceConfiguration.getDefaultMessageQueuesConfiguration());

        final ConcurrentMap<MessageType, MessageHandler> messageHandlerByMessageTypeMap = new ConcurrentHashMap<>();

        final EnumSet<MessageType> ignoredMessageTypeSet = EnumSet.of(MessageType.EXCEPTION);

        final Queue<AppbaseRuntimeException> appbaseRuntimeExceptionQueue =
                new ManyToManyConcurrentArrayQueue<>(
                        defaultMediatorServiceConfiguration.getAppbaseRuntimeExceptionQueueCapacity());
        final ReentrantLock sharedMessageQueueLock = new ReentrantLock();

        final DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration =
                defaultMediatorServiceConfiguration.getDefaultMessageQueuesConfiguration();

        final MessageQueuesCleaner messageQueuesCleaner = new DefaultMessageQueuesCleaner(
                metricService,
                messageQueues,
                messageHandlerByMessageTypeMap,
                defaultMessageQueuesConfiguration.getExpiredMessageThresholdMillis(),
                sharedMessageQueueLock,
                new ManyToManyConcurrentArrayQueue<>(
                        defaultMediatorServiceConfiguration.getRemovedMessageQueueCapacity()),
                appbaseRuntimeExceptionQueue,
                SchedulableConfiguration.builder().build());

        final MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry =
                new DefaultMediatorEventDelegatorRegistry();
        final DisruptorService<MediatorEvent> disruptorService = new DefaultDisruptorService<>(
                DefaultDisruptorServiceConfiguration.builder().build(),
                new MediatorEventFactory(),
                new DefaultExceptionHandler<>(),
                new DisruptorMediatorEventHandlerDelegator(mediatorEventDelegatorRegistry));
        return DefaultMediator
                .builder()
                .logService(logService)
                .metricService(metricService)
                .validatorService(validationService)
                .appbaseRuntimeExceptionQueue(appbaseRuntimeExceptionQueue)
                .ignoredMessageTypeSet(ignoredMessageTypeSet)
                .messageHandlerByMessageTypeMap(messageHandlerByMessageTypeMap)
                .messageObjectPool(messageObjectPool)
                .messageQueues(messageQueues)
                .messageQueuesCleaner(messageQueuesCleaner)
                .sharedScheduledExecutorService(scheduledExecutorService)
                .sharedMessageQueueLock(sharedMessageQueueLock)
                .sharedExecutorService(Executors.newSingleThreadExecutor())
                .sharedDisruptorService(disruptorService)
                .sharedMediatorEventDelegatorRegistry(mediatorEventDelegatorRegistry)
                .build();
    }

    public static DefaultMediatorServiceConfiguration getMessageQueuesConfiguration() {
        return DefaultMediatorServiceConfiguration
                .builder()
                .messageQueuesConfiguration(DefaultMessageQueuesConfiguration.builder().build())
                .appbaseRuntimeExceptionQueueCapacity(4)
                .droppedMessageQueueCapacity(4)
                .ignoredMessageTypeSet(Set.of())
                .messageObjectPoolCapacity(4)
                .sharedExecutorServiceConfiguration(ExecutorServiceConfiguration.builder().build())
                .sharedScheduledExecutorServiceConfiguration(ScheduledExecutorServiceConfiguration.builder().build())
                .build();
    }

    public static DefaultMessageQueuesConfiguration getBoundMessageQueuesConfiguration() {
        return DefaultMessageQueuesConfiguration
                .builder()
                .messageQueueType(MessageQueueType.BOUND)
                .boundMessageQueueCapacity(BOUND_MESSAGE_QUEUE_SIZE)
                .build();
    }

    public static DefaultMessageQueuesConfiguration getUnboundMessageQueuesConfiguration() {
        return DefaultMessageQueuesConfiguration
                .builder()
                .messageQueueType(MessageQueueType.UNBOUND)
                .build();
    }

    public static ExceptionMessage getExceptionMessage() {
        final ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setSourceType(CoreTestFixtures.class);

        final AppbaseException appbaseException = new AppbaseException();
        exceptionMessage.setThrowable(appbaseException);
        exceptionMessage.setTimestamp(System.currentTimeMillis());
        exceptionMessage.setThreadName(AppbaseUtil.threadName());
        exceptionMessage.setMessage("Exception message " + Math.random());

        final Map<String, String> context = new HashMap<>();
        context.put("a", "b");
        exceptionMessage.addAllContext(context);

        return exceptionMessage;
    }

    public static ExceptionMessage getExceptionMessage(final long timestamp) {
        final ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setSourceType(CoreTestFixtures.class);

        final AppbaseException appbaseException = new AppbaseException();
        exceptionMessage.setThrowable(appbaseException);
        exceptionMessage.setTimestamp(timestamp);
        exceptionMessage.setThreadName(AppbaseUtil.threadName());
        exceptionMessage.setMessage("Exception message " + Math.random());

        final Map<String, String> context = new HashMap<>();
        context.put("a", "b");
        exceptionMessage.addAllContext(context);

        return exceptionMessage;
    }

    public static ServiceStatusMessage getServiceStatusMessage() {
        final ServiceStatusMessage serviceStatusMessage = new ServiceStatusMessage();
        serviceStatusMessage.setServiceStatus(ServiceStatus.STARTED);
        serviceStatusMessage.setServiceType(MediatorService.class);

        return serviceStatusMessage;
    }

    public static Message getMessageWithServiceStatusMessage() {
        final ServiceStatusMessage serviceStatusMessage = new ServiceStatusMessage();
        serviceStatusMessage.setServiceStatus(ServiceStatus.STARTED);
        serviceStatusMessage.setServiceType(MediatorService.class);

        final Map<String, String> context = new HashMap<>();
        context.put("a", "b");
        serviceStatusMessage.addAllContext(context);

        return new Message(
                System.currentTimeMillis(),
                MessageType.SERVICE_STATUS,
                null,
                serviceStatusMessage);
    }

    public static ExceptionMessageContextCodec getContextCodec() {
        return new DefaultExceptionMessageContextCodec();
    }

    // A UNIQUE in-memory H2 URL per call (internal work item 1073). The shared name "TestAppbaseDB1" with DB_CLOSE_DELAY=-1
    // kept one DB alive for the whole JVM, so two test classes creating the same schema in a shared fork
    // collided ("already exists") order-dependently. A UUID suffix isolates each call's DB.
    private static String uniqueH2Url(final String name) {
        return "jdbc:h2:mem:" + name + "_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=KEY,VALUE";
    }

    public static JpaExceptionStoreConfiguration getJpaExceptionStoreConfiguration() {
        final Map<String, String> entityManagerFactoryProperties = new HashMap<>();
        entityManagerFactoryProperties.put(
                "jakarta.persistence.jdbc.url", uniqueH2Url("TestAppbaseDB1"));
        final Map<String, String> entityManagerProperties = new HashMap<>();
        entityManagerProperties.put("k2", "v2");
        final EntityManagerConfiguration entityManagerConfiguration = new EntityManagerConfiguration(
                "pun", entityManagerFactoryProperties, entityManagerProperties);

        return JpaExceptionStoreConfiguration
                .builder()
                .entityManagerConfiguration(entityManagerConfiguration)
                .discriminator("discriminator")
                .discriminatorColumnName("discriminator")
                .enabled(true)
                .idColumnName("id")
                .messageColumnName("message")
                .sourceTypeNameColumnName("sourceTypeName")
                .stackTraceColumnName("stackTrace")
                .stackTraceHashColumnName("stackTraceHash")
                .stackTraceSerializationMaxLength(2048)
                .threadNameColumnName("threadName")
                .throwableMessageColumnName("throwableMessage")
                .throwableTypeNameColumnName("throwableTypeName")
                .timestampColumnName("timestamp")
                .build();
    }

    public static JpaConfigurationStore getJpaConfigurationStore(final MetricService metricService,
                                                                 final ValidationService validationService)
    {
        final Map<String, String> entityManagerFactoryProperties = new HashMap<>();
        entityManagerFactoryProperties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        entityManagerFactoryProperties.put("jakarta.persistence.schema-generation.database.action", "create");
        entityManagerFactoryProperties.put("jakarta.persistence.validation.mode", "none");
        entityManagerFactoryProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        entityManagerFactoryProperties.put("hibernate.flushMode", "AUTO");
        entityManagerFactoryProperties.put("show_sql", "true");
        entityManagerFactoryProperties.put("format_sql", "true");
        entityManagerFactoryProperties.put("use_sql_comments", "true");
        entityManagerFactoryProperties.put(
                "jakarta.persistence.jdbc.url",
                // Stable name shared with JpaConfigurationStoreTest's reload EM (its only caller), so the
                // store and the test's separate EntityManager address the SAME DB. One caller => no
                // cross-class collision, so a UUID isn't needed here (internal work item 1073).
                "jdbc:h2:mem:TestAppbaseDBJpaConfigStore;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=KEY,VALUE");
        final EntityManager entityManager = Persistence
                .createEntityManagerFactory("testAppbasePersistenceUnit1", entityManagerFactoryProperties)
                .createEntityManager();
        final EntityManagerConfiguration entityManagerConfiguration =
                new EntityManagerConfiguration("testAppbasePersistenceUnit1", null, null);
        final JpaConfigurationStoreConfiguration jpaConfigurationStoreConfiguration =
                JpaConfigurationStoreConfiguration
                .builder()
                .entityManagerConfiguration(entityManagerConfiguration)
                .build();
        return new JpaConfigurationStore(
                metricService, validationService, entityManager, jpaConfigurationStoreConfiguration);
    }

    public static JpaExceptionStore getJpaExceptionStore(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService)
    {
        final Map<String, String> entityManagerFactoryProperties = new HashMap<>();
        entityManagerFactoryProperties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        entityManagerFactoryProperties.put("jakarta.persistence.schema-generation.database.action", "create");
        entityManagerFactoryProperties.put("jakarta.persistence.validation.mode", "none");
        entityManagerFactoryProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        entityManagerFactoryProperties.put("hibernate.flushMode", "AUTO");
        entityManagerFactoryProperties.put("show_sql", "true");
        entityManagerFactoryProperties.put("format_sql", "true");
        entityManagerFactoryProperties.put("use_sql_comments", "true");
        entityManagerFactoryProperties.put(
                "jakarta.persistence.jdbc.url",
                uniqueH2Url("TestAppbaseExcDB1"));
        final EntityManager entityManager = Persistence
                .createEntityManagerFactory("testAppbasePersistenceUnit1", entityManagerFactoryProperties)
                .createEntityManager();
        return new JpaExceptionStore(
                logService,
                metricService,
                mediatorService,
                validationService,
                entityManager,
                new DefaultStackTraceHashGenerator(),
                getJpaExceptionStoreConfiguration(),
                getContextCodec());
    }

    public static DefaultExceptionServiceConfiguration getDefaultExceptionServiceConfiguration(
            final LogService logService)
    {
        final AppbaseCore appbaseCore = getAppbaseCoreWithJpaExceptionStore(logService);
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        return configurationService.getTypedConfiguration(DefaultExceptionServiceConfiguration.class);
    }

    public static DefaultExceptionServiceConfiguration getDefaultExceptionServiceConfiguration() {
        final Map<String, String> entityManagerFactoryProperties = new HashMap<>();
        entityManagerFactoryProperties.put(
                "jakarta.persistence.jdbc.url", uniqueH2Url("TestAppbaseDB2"));
        final JpaExceptionStoreConfiguration jpaExceptionStoreConfiguration =
                JpaExceptionStoreConfiguration
                        .builder()
                        .enabled(true)
                        .entityManagerConfiguration(
                                new EntityManagerConfiguration(
                                        "testAppbasePersistenceUnit2",
                                        entityManagerFactoryProperties,
                                        null))
                        .build();
        final DefaultExceptionAccessorConfiguration defaultExceptionAccessorConfiguration =
                DefaultExceptionAccessorConfiguration
                .builder()
                .exceptionStoreConfigurations(List.of(jpaExceptionStoreConfiguration))
                .build();
        return DefaultExceptionServiceConfiguration
                .builder()
                .defaultExceptionAccessorConfiguration(defaultExceptionAccessorConfiguration)
                .build();
    }

    public static DefaultExceptionAccessor getExceptionAccessor(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ValidationService validationService)
    {
        final DefaultConfigurationService defaultConfigurationService =
                getEmptyDefaultConfigurationService(logService, metricService, mediatorService, validationService);
        defaultConfigurationService.putTypedConfiguration(
                DefaultExceptionServiceConfiguration.class,
                getDefaultExceptionServiceConfiguration());
        final DefaultExceptionAccessor exceptionAccessor =
                new DefaultExceptionAccessor(
                        logService,
                        metricService,
                        mediatorService,
                        validationService,
                        defaultConfigurationService,
                        new DefaultExceptionMessageContextCodec());
        return exceptionAccessor;
    }

    public static DefaultTracerService getDefaultTracingService(final LogService logService) {
        final AppbaseCore appbaseCore = getAppbaseCore(logService);
        return (DefaultTracerService) appbaseCore.getTracerService();
    }

    public static TurboLog getTurboLogWithThrowable() {
        return TurboLog
                .builder()
                .timestamp(System.currentTimeMillis())
                .formattedMessage("Formatted error message: {}")
                .logLevel(LogLevel.INFO)
                .throwable(new AppbaseException())
                .logger(LoggerFactory.getLogger(CoreTestFixtures.class))
                .marker(null)
                .argumentArray(null)
                .build();
    }

    public static DefaultLuceneLogIndex getDefaultLuceneLogIndex(
            final LogService logService,
            final MetricService metricService,
            final ValidationService validationService,
            final MediatorService mediatorService,
            final DefaultLuceneService luceneService,
            final DefaultLuceneLogIndexConfiguration defaultLuceneLogIndexConfiguration)
    {
        return new DefaultLuceneLogIndex(
                logService,
                metricService,
                validationService,
                mediatorService,
                luceneService,
                defaultLuceneLogIndexConfiguration);
    }

    public static LogRecord getLogRecord1() {
        return getLogRecord(
                "Formatted Message1: {}",
                "Formatted Message1: ARG",
                "Throwable message1",
                "Cause Message1",
                "Key1",
                "Value1",
                "MARKER1",
                System.currentTimeMillis(),
                CoreTestFixtures.class.getName(),
                LogLevel.INFO);
    }

    public static LogRecord getLogRecord2() {
        return getLogRecord(
                "Formatted Message2: {}",
                "Formatted Message2: ARG",
                "Throwable message2",
                "Cause Message2",
                "Key2",
                "Value2",
                "MARKER2",
                System.currentTimeMillis(),
                CoreTestFixtures.class.getName(),
                LogLevel.INFO);
    }

    public static LogRecord getLogRecord3() {
        return getLogRecord(
                "Formatting 1: {}",
                "Formatting 1: ARG",
                "Throwable message3",
                "Cause Message3",
                "Key3",
                "Value3",
                "MARKER3",
                System.currentTimeMillis(),
                CoreTestFixtures.class.getName(),
                LogLevel.INFO);
    }

    public static LogRecord getLogRecord4() {
        return getLogRecord(
                "Another msg fmt: {}",
                "Another msg fmt: ARG",
                "Throwable message4",
                "Cause Message4",
                "Key4",
                "Value4",
                "MARKER4",
                System.currentTimeMillis(),
                CoreTestFixtures.class.getName(),
                LogLevel.INFO);
    }

    public static LogRecord getLogRecord(final String formattedMessage,
                                         final String message,
                                         final String throwableMessage,
                                         final String throwableCauseMessage,
                                         final String mdcKey,
                                         final String mdcValue,
                                         final String markerName,
                                         final long timestamp,
                                         final String loggerName,
                                         final LogLevel logLevel)
    {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        final StringBuilder callerDataBuilder = new StringBuilder("");
        for (final StackTraceElement stackTraceElement : stackTrace) {
            callerDataBuilder.append(stackTraceElement.toString());
        }
        final String callerData = callerDataBuilder.toString();
        final Throwable throwable = new AppbaseException(
                throwableMessage, new AppbaseRuntimeException(throwableCauseMessage));
        final String throwableClassName = throwable.getClass().getName();

        final ThrowableRecord throwableRecord = ThrowableRecord
                .builder()
                .stackTrace(callerDataBuilder.toString())
                .message(throwableMessage)
                .className(throwableClassName)
                .cause(ThrowableRecord
                        .builder()
                        .message(throwableCauseMessage)
                        .className(throwable.getCause().getClass().getName())
                        .stackTrace(callerData)
                        .build())
                .build();
        final String threadName = Thread.currentThread().getName();
        return LogRecord
                .builder()
                .threadName(threadName)
                .mdcPropertyMap(Map.of(mdcKey, mdcValue))
                .callerData(callerData)
                .throwableRecord(throwableRecord)
                .markerName(markerName)
                .loggerName(loggerName)
                .formattedMessage(formattedMessage)
                .message(message)
                .logLevel(logLevel)
                .threadName(threadName)
                .timestamp(timestamp)
                .build();
    }

    public static AppbaseCoreBootstrapConfiguration getLuceneDisabledBootstrapConfiguration() {
        return DefaultAppbaseCoreBootstrapConfiguration
                .builder()
                .defaultLuceneServiceConfiguration(
                        DefaultLuceneServiceConfiguration
                                .builder()
                                .enabled(false)
                                .build())
                .defaultLogServiceConfiguration(DefaultLogServiceConfiguration
                        .builder()
                        .luceneAppenderConfiguration(
                                LuceneAppenderConfiguration
                                        .builder()
                                        .enabled(false)
                                        .build())
                        .disruptorLogAndTurboLogEventFilterDelegatorConfiguration(
                                DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                                        .builder()
                                        .enabled(false)
                                        .build())
                        .luceneLogAndTurboLogEventFilterListenerConfiguration(
                                LuceneLogAndTurboLogEventFilterListenerConfiguration
                                        .builder()
                                        .enabled(false)
                                        .build())
                        .defaultLogAccessorConfiguration(
                                DefaultLogAccessorConfiguration
                                        .builder()
                                        .enabled(false)
                                        .build())
                        .luceneLogIndexConfiguration(
                                DefaultLuceneLogIndexConfiguration
                                        .builder()
                                        .enabled(false)
                                        .build())
                        .build())
                .defaultJmxServiceConfiguration(
                        DefaultJmxServiceConfiguration
                                .builder()
                                .excludedMxBeanRegistrations(Set.of(JmxLogServiceMXBean.class))
                                .build())
                .build();
    }

    public static void checkExceptionMessageDetails(
            final ExceptionMessage exceptionMessage,
            final ExceptionMessageDetails exceptionMessageDetails)
    {
        Assertions.assertEquals(
                exceptionMessage.getSourceType().getName(),
                exceptionMessageDetails.getSourceTypeName());
        Assertions.assertEquals(
                exceptionMessage.getMessage(),
                exceptionMessageDetails.getMessage());
        Assertions.assertEquals(
                exceptionMessage.getThreadName(),
                exceptionMessageDetails.getThreadName());
        Assertions.assertEquals(
                exceptionMessage.getTimestamp(),
                exceptionMessageDetails.getTimestamp());
    }

    public interface TestService extends AppbaseManagedService {
        boolean isReloaded();

        boolean isShutdown();
    }

    public static class TestsSchedulable implements Schedulable<Void> {
        private final AtomicBoolean scheduled;

        public TestsSchedulable() {
            this.scheduled = new AtomicBoolean(false);
        }

        @Override
        public Void schedule() {
            this.scheduled.set(true);
            return null;
        }

        @SuppressWarnings("unused")
        public AtomicBoolean getScheduled() {
            return scheduled;
        }

        @Override
        public SchedulableConfiguration getSchedulableConfiguration() {
            return SchedulableConfiguration.builder().build();
        }
    }

    public static class TestMessageHandler implements MessageHandler {
        private final AtomicBoolean handled = new AtomicBoolean(false);

        @Override
        public void onMessage(final Message message) {
            handled.set(true);
        }

        public AtomicBoolean getHandled() {
            return handled;
        }
    }

    public static class ThrowingMessageHandler implements MessageHandler {
        @Override
        public void onMessage(final Message message) {
            throw new NullPointerException();
        }
    }

    public static class TestPoolableObjectFactory implements ObjectFactory<TestPoolableObject> {
        @Override
        public TestPoolableObject newObject() {
            return new TestPoolableObject();
        }
    }

    public static class TestPoolableObject implements Poolable {
        private String name;

        /**
         * Set all fields.
         *
         * @param name Name
         */
        public void setAll(final String name) {

        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TestPoolableObject that = (TestPoolableObject) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "TestPoolableObject{" +
                    "name='" + name + '\'' +
                    '}';
        }

        @Override
        public boolean isValid() {
            return name != null;
        }

        @Override
        public boolean isClean() {
            return name == null;
        }

        @Override
        public void clean() {
            name = null;
        }
    }

    public static class DefaultTestService extends AbstractAppbaseManagedService implements TestService {
        private final AtomicBoolean reloadedRef = new AtomicBoolean(false);
        private final AtomicBoolean shutdownRef = new AtomicBoolean(false);
        private final AtomicReference<TestServiceConfiguration> testServiceConfigurationRef;

        public DefaultTestService(final AppbaseCore appbaseCore,
                                  final TestServiceConfiguration testServiceConfiguration)
        {
            super(appbaseCore, TestServiceConfiguration.class, true, null);

            AppbaseUtil.notNull(testServiceConfiguration, "testServiceConfiguration");
            this.testServiceConfigurationRef = new AtomicReference<>(testServiceConfiguration);
            statusRef.set(ServiceStatus.STARTED);
        }

        @Override
        protected void initImpl(final AppbaseManagedServiceConfiguration configuration) throws AppbaseException {
            this.testServiceConfigurationRef.set((TestServiceConfiguration) configuration);
            reloadedRef.set(true);
        }

        @Override
        protected void shutdownImpl() {
            shutdownRef.set(true);
        }

        @Override
        @SuppressWarnings("unchecked")
        public TestServiceConfiguration getConfiguration() {
            return testServiceConfigurationRef.get();
        }

        @Override
        public boolean isReloaded() {
            return reloadedRef.get();
        }

        @Override
        public boolean isShutdown() {
            return shutdownRef.get();
        }
    }

    public static class TestServiceConfiguration implements AppbaseManagedServiceConfiguration {
        private final String host;

        public TestServiceConfiguration(final String host) {
            this.host = host;
        }

        @Override
        public boolean isAutoStart() {
            return true;
        }

        @SuppressWarnings("unused")
        public String getHost() {
            return host;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final TestServiceConfiguration that = (TestServiceConfiguration) o;
            return Objects.equals(host, that.host);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host);
        }

        @Override
        public String toString() {
            return "TestServiceConfiguration{" +
                    "host='" + host + '\'' +
                    '}';
        }
    }

    /**
     * Grants every {@link AbstractAppbaseServiceMXBean} permission tier an AUD-08 fixture needs to
     * {@code reload()}/{@code shutdown()} a real MXBean in a test without hitting the deny-by-default
     * (owner decision DQ-1, 2026-10-01) that now applies when nothing is explicitly configured.
     *
     * <p>{@code GLOBAL_ALL_OPERATIONS}, {@code RELOAD_OPERATIONS} and {@code SHUTDOWN_OPERATION} share
     * one property key across every {@link AbstractAppbaseServiceMXBean} subclass (the key is derived
     * from the common {@code AppbaseServiceMXBean} interface, not the concrete class), so each is
     * granted once per {@code configurationService}. The per-bean "all operations" tier is keyed by the
     * concrete MXBean class's simple name, so it is granted once per {@code mxBeanType} passed in.
     *
     * @param configurationService the {@link ConfigurationService} backing the MXBean(s) under test
     * @param mxBeanTypes           every concrete {@link AbstractAppbaseServiceMXBean} subclass this
     *                              fixture will {@code reload()} or {@code shutdown()}
     */
    public static void grantJmxOperationPermissions(
            final ConfigurationService configurationService, final Class<?>... mxBeanTypes)
    {
        configurationService.putBoolean(
                AbstractAppbaseServiceMXBean.GLOBAL_ALL_OPERATIONS_PERMISSION_PROPERTY, true);
        configurationService.putBoolean(
                AbstractAppbaseServiceMXBean.RELOAD_OPERATIONS_PERMISSION_PROPERTY, true);
        configurationService.putBoolean(
                AbstractAppbaseServiceMXBean.SHUTDOWN_OPERATION_PERMISSION_PROPERTY, true);
        for (final Class<?> mxBeanType : mxBeanTypes) {
            configurationService.putBoolean(
                    AppbaseConstants.PERMISSION_PREFIX_PROPERTY +
                            mxBeanType.getSimpleName().toLowerCase() +
                            AppbaseConstants.PERMISSION_SUFFIX_PROPERTY +
                            AppbaseConstants.ALL_OPERATIONS_PROPERTY,
                    true);
        }
    }
}
