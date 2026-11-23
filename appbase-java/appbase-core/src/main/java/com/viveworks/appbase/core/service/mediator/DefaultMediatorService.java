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

package com.viveworks.appbase.core.service.mediator;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceFactory;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceFactory;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageObjectFactory;
import com.viveworks.appbase.core.service.mediator.message.MessageType;
import com.viveworks.appbase.core.service.mediator.queues.DefaultMessageQueues;
import com.viveworks.appbase.core.service.mediator.queues.DefaultMessageQueuesCleaner;
import com.viveworks.appbase.core.service.mediator.queues.DefaultMessageQueuesConfiguration;
import com.viveworks.appbase.core.service.mediator.queues.MessageQueues;
import com.viveworks.appbase.core.service.mediator.queues.MessageQueuesCleaner;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.disruptor.DisruptorService;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.core.util.objectpool.ObjectPool;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPool;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link MediatorService}.
 *
 * <p>
 * The service can be configured using YAML (represented by {@link DefaultMediatorServiceConfiguration}. The YAML file
 * path can be set with a system property, <b><code>appbase.configuration.filePath</code></b> or
 * environment variable <b><code>APPBASE_CONFIGURATION_FILE_PATH</code></b>. If the value of this
 * property starts with <b><code>classpath://</code></b>, it is read from the classpath, else it is read from the file
 * system. If this property or environment variable is missing, a default {@link DefaultMediatorServiceConfiguration}
 * is created.
 * </p>
 *
 * <blockquote>
 * <p>
 *     <b>NOTE:</b> System property has <i>higher precedence</i> over environment variable.
 * </p>
 * </blockquote>
 *
 * <p>
 * The YAML file can represent the {@link DefaultMediatorServiceConfiguration} standalone or
 * {@link DefaultMediatorServiceConfiguration} can be the composed in another object, as the first child. In this case
 * the YAML node name can be configured using system property
 * <b><code>appbase.mediatorServiceConfiguration.yamlNodeName</code></b> or environment variable
 * <b><code>APPBASE_MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME</code></b>. By default, its value is
 * <b><code>mediatorServiceConfiguration</code></b>.
 * </p>
 *
 * @see com.viveworks.appbase.core for code samples
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings("HES_EXECUTOR_NEVER_SHUTDOWN")
public class DefaultMediatorService implements MediatorService {
    private final LogService logService;
    private final MetricService metricService;
    private final ValidationService validationService;
    private final ExecutorService sharedExecutorService;
    private final ScheduledExecutorService sharedScheduledExecutorService;
    private final DisruptorService<MediatorEvent> sharedDisruptorService;
    private final MediatorEventDelegatorRegistry sharedMediatorEventDelegatorRegistry;
    private final Logger logger;

    private final AtomicReference<DefaultMediatorServiceConfiguration> mediatorServiceConfigurationRef;
    private final AtomicReference<Mediator> mediatorRef;
    private final boolean initWithConfigurationFile;
    private final boolean initWithDefaultConfiguration;
    @Nullable private final String configurationFilePath;

    private final ReentrantLock lifecycleReentrantLock;
    private final Timer reloadTimer;
    private final Timer shutdownTimer;
    private final Counter reloadErrorCounter;
    private final Counter shutdownErrorCounter;
    private final AtomicReference<ServiceStatus> statusRef;

    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS")
    public DefaultMediatorService(final LogService logService,
                                  final MetricService metricService,
                                  final ValidationService validationService,
                                  final DisruptorService<MediatorEvent> disruptorService,
                                  final MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry,
                                  @Nullable
                                  final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(disruptorService, "disruptorService");
        AppbaseUtil.notNull(mediatorEventDelegatorRegistry, "mediatorEventDelegatorRegistry");

        this.logger = logService.getLogger(getClass());
        this.logService = logService;
        this.metricService = metricService;
        this.validationService = validationService;
        this.sharedDisruptorService = disruptorService;
        this.sharedMediatorEventDelegatorRegistry = mediatorEventDelegatorRegistry;

        logger.info("Initializing {}", getClass().getName());
        DefaultMediatorServiceConfiguration defaultMediatorServiceConfigurationCopy =
                defaultMediatorServiceConfiguration;
        if (defaultMediatorServiceConfigurationCopy == null) {
            String configurationFilePathFromSystem = AppbaseUtil.propertyOrEnv(
                    AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY,
                    AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_ENV_KEY);
            if (configurationFilePathFromSystem != null) {
                configurationFilePathFromSystem = configurationFilePathFromSystem.trim();
            }

            if (!AppbaseUtil.isBlank(configurationFilePathFromSystem)) {
                this.initWithConfigurationFile = true;
                this.initWithDefaultConfiguration = false;
                this.configurationFilePath = configurationFilePathFromSystem;
                try {
                    logger.info("Reading {} from path {}",
                            DefaultMediatorServiceConfiguration.class.getName(),
                            configurationFilePath);
                    defaultMediatorServiceConfigurationCopy = YamlUtil.readConfigurationFromFile(
                            configurationFilePathFromSystem,
                            AppbaseConstants.MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_PROPERTY_NAME,
                            AppbaseConstants.MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME_ENV_NAME,
                            AppbaseConstants.DEFAULT_MEDIATOR_SERVICE_CONFIGURATION_YAML_NODE_NAME,
                            DefaultMediatorServiceConfiguration.class);
                } catch (final IOException ioException) {
                    logger.info("Configuration file {} does not exist. Using defaults",
                            configurationFilePathFromSystem);
                    defaultMediatorServiceConfigurationCopy = DefaultMediatorServiceConfiguration.builder().build();
                }
            } else {
                this.initWithConfigurationFile = false;
                this.initWithDefaultConfiguration = true;
                this.configurationFilePath = null;
                logger.info("Using defaults from {}", DefaultMediatorServiceConfiguration.class.getName());
                defaultMediatorServiceConfigurationCopy = DefaultMediatorServiceConfiguration.builder().build();
            }
        } else {
            this.initWithConfigurationFile = false;
            this.initWithDefaultConfiguration = false;
            this.configurationFilePath = null;
            logger.info("Using configuration {}", defaultMediatorServiceConfigurationCopy);
        }

        AppbaseUtil.validate(validationService, defaultMediatorServiceConfigurationCopy);
        this.mediatorRef = new AtomicReference<>();
        this.mediatorServiceConfigurationRef = new AtomicReference<>(defaultMediatorServiceConfigurationCopy);

        this.sharedExecutorService = ExecutorServiceFactory.newExecutorService(
                defaultMediatorServiceConfigurationCopy.getSharedExecutorServiceConfiguration(),
                uncaughtExceptionHandler(logService));
        metricService.registerExecutorServiceMetrics(
                sharedExecutorService,
                metricService.name(getClass(), "sharedExecutorService", "metric"));

        this.sharedScheduledExecutorService = ScheduledExecutorServiceFactory.newScheduledExecutorService(
                defaultMediatorServiceConfigurationCopy.getSharedScheduledExecutorServiceConfiguration(),
                uncaughtExceptionHandler(logService));
        metricService.registerExecutorServiceMetrics(
                sharedScheduledExecutorService,
                metricService.name(getClass(), "sharedScheduledExecutorService", "metric"));

        init(defaultMediatorServiceConfigurationCopy);

        this.reloadTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "reload", "timer"));
        this.shutdownTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "shutdown", "timer"));
        this.reloadErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "reload", "error", "counter"));
        this.shutdownErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "shutdown", "error", "counter"));

        this.lifecycleReentrantLock = new ReentrantLock();
        this.statusRef = new AtomicReference<>(ServiceStatus.STARTED);
    }

    public DefaultMediatorService(final LogService logService,
                                  final MetricService metricService,
                                  final DisruptorService<MediatorEvent> disruptorService,
                                  final MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry,
                                  final ValidationService validationService)
    {
        this(logService, metricService, validationService, disruptorService, mediatorEventDelegatorRegistry, null);
    }

    private static Thread.UncaughtExceptionHandler uncaughtExceptionHandler(final LogService logService) {
        AppbaseUtil.notNull(logService, "logService");

        return (t, e) -> {
            logService
                    .getLogger(DefaultMediatorService.class)
                    .error("Uncaught exception in mediator's scheduled executor task" +
                                    " [thread=" +
                                    t.getName() +
                                    "]",
                            e);
        };
    }

    private void init(final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration) {
        AppbaseUtil.notNull(defaultMediatorServiceConfiguration, "defaultMediatorServiceConfiguration");
        AppbaseUtil.validate(validationService, defaultMediatorServiceConfiguration);

        logger.info("Using configuration: {}", defaultMediatorServiceConfiguration);
        final DefaultMessageQueuesConfiguration defaultMessageQueuesConfiguration =
                defaultMediatorServiceConfiguration.getDefaultMessageQueuesConfiguration();
        AppbaseUtil.notNull(defaultMessageQueuesConfiguration, "defaultMessageQueuesConfiguration");

        final ObjectPool<Message> messageObjectPool = new ManyToManyQueuedFixedObjectPool<>(
                metricService,
                defaultMediatorServiceConfiguration.getMessageObjectPoolCapacity(),
                new MessageObjectFactory());
        final MessageQueues messageQueues = new DefaultMessageQueues(metricService, defaultMessageQueuesConfiguration);

        final Set<MessageType> ignoredMessageTypeSetConfiguration =
                defaultMediatorServiceConfiguration.getIgnoredMessageTypeSet();
        final EnumSet<MessageType> ignoredMessageTypeSet;
        if (ignoredMessageTypeSetConfiguration != null && !ignoredMessageTypeSetConfiguration.isEmpty()) {
            ignoredMessageTypeSet = EnumSet.copyOf(ignoredMessageTypeSetConfiguration);
        } else {
            ignoredMessageTypeSet = EnumSet.noneOf(MessageType.class);
        }

        final ConcurrentMap<MessageType, MessageHandler> messageHandlerByMessageTypeMap = new ConcurrentHashMap<>();
        final ReentrantLock sharedMessageQueueLock = new ReentrantLock();

        final int appbaseRuntimeExceptionQueueCapacity =
                defaultMediatorServiceConfiguration.getAppbaseRuntimeExceptionQueueCapacity();
        final Queue<AppbaseRuntimeException> appbaseRuntimeExceptionQueue =
                new ManyToManyConcurrentArrayQueue<>(appbaseRuntimeExceptionQueueCapacity);

        MessageQueuesCleaner messageQueuesCleaner = null;
        if (defaultMessageQueuesConfiguration.isRemoveExpiredMessages()) {
            final Queue<Message> removedMessageQueue =
                    new ManyToManyConcurrentArrayQueue<>(
                            defaultMediatorServiceConfiguration.getRemovedMessageQueueCapacity());
            final SchedulableConfiguration schedulableConfiguration = SchedulableConfiguration
                    .builder()
                    .initialDelay(defaultMessageQueuesConfiguration.getMessageQueuesCleanerInitialDelayMillis())
                    .delay(defaultMessageQueuesConfiguration.getMessageQueuesCleanerIntervalMillis())
                    .timeUnit(TimeUnit.MILLISECONDS)
                    .build();
            messageQueuesCleaner = new DefaultMessageQueuesCleaner(
                    metricService,
                    messageQueues,
                    messageHandlerByMessageTypeMap,
                    defaultMessageQueuesConfiguration.getExpiredMessageThresholdMillis(),
                    sharedMessageQueueLock,
                    removedMessageQueue,
                    appbaseRuntimeExceptionQueue,
                    schedulableConfiguration);
            sharedScheduledExecutorService.scheduleWithFixedDelay(
                    messageQueuesCleaner::schedule,
                    defaultMessageQueuesConfiguration.getMessageQueuesCleanerInitialDelayMillis(),
                    defaultMessageQueuesConfiguration.getMessageQueuesCleanerIntervalMillis(), TimeUnit.MILLISECONDS);
        }

        final Mediator mediator = DefaultMediator
                .builder()
                .logService(logService)
                .metricService(metricService)
                .validatorService(validationService)
                .appbaseRuntimeExceptionQueue(appbaseRuntimeExceptionQueue)
                .ignoredMessageTypeSet(ignoredMessageTypeSet)
                .sharedMessageQueueLock(sharedMessageQueueLock)
                .messageHandlerByMessageTypeMap(messageHandlerByMessageTypeMap)
                .objectPoolByPoolableTypeNameMap(new ConcurrentHashMap<>())
                .messageObjectPool(messageObjectPool)
                .messageQueues(messageQueues)
                .messageQueuesCleaner(messageQueuesCleaner)
                .sharedScheduledExecutorService(sharedScheduledExecutorService)
                .sharedExecutorService(sharedExecutorService)
                .sharedDisruptorService(sharedDisruptorService)
                .sharedMediatorEventDelegatorRegistry(sharedMediatorEventDelegatorRegistry)
                .build();
        this.mediatorRef.set(mediator);
    }

    @Override
    public Mediator getMediator() {
        return mediatorRef.get();
    }

    @Override
    public boolean isInitWithConfigurationFile() {
        return initWithConfigurationFile;
    }

    @Override
    public boolean isInitWithDefaultConfiguration() {
        return initWithDefaultConfiguration;
    }

    @Nullable
    @Override
    public String getConfigurationFilePath() {
        return configurationFilePath;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultMediatorServiceConfiguration getConfiguration() {
        return mediatorServiceConfigurationRef.get();
    }


    @Override
    @GuardedBy("lifecycleReentrantLock")
    @SuppressFBWarnings(value = {"MDM_WAIT_WITHOUT_TIMEOUT", "DRE_DECLARED_RUNTIME_EXCEPTION"})
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <C extends AppbaseConfiguration> void reload(final C configuration) throws ServiceReloadException {
        AppbaseUtil.notNull(configuration, "configuration");

        if (!DefaultMediatorServiceConfiguration.class.isAssignableFrom(configuration.getClass())) {
            throw new ServiceReloadException("The configuration " +
                    configuration.getClass().getName() +
                    " is not of type " +
                    DefaultMediatorServiceConfiguration.class.getName());
        }

        final Timer.Context context = reloadTimer.time();
        lifecycleReentrantLock.lock();
        try {
            if (statusRef.get() == ServiceStatus.RELOADING) {
                return;
            }

            logger.info("Reloading {}", getClass().getName());
            statusRef.set(ServiceStatus.RELOADING);

            final DefaultMediatorServiceConfiguration defaultMediatorServiceConfiguration =
                    (DefaultMediatorServiceConfiguration) configuration;
            AppbaseUtil.validate(validationService, defaultMediatorServiceConfiguration);
            init(defaultMediatorServiceConfiguration);
            this.mediatorServiceConfigurationRef.set(defaultMediatorServiceConfiguration);

            statusRef.set(ServiceStatus.STARTED);
            context.stop();
        } catch (final Exception exception) {
            reloadErrorCounter.inc();
            statusRef.set(ServiceStatus.RELOAD_FAILED);
            context.stop();
            throw new ServiceReloadException("Service reload failed for configuration: " + configuration, exception);
        } finally {
            lifecycleReentrantLock.unlock();
        }
    }

    @Override
    public boolean isShutdown() {
        return getStatus() == ServiceStatus.SHUTDOWN;
    }

    @Override
    @GuardedBy("lifecycleReentrantLock")
    @SuppressFBWarnings(value = {"MDM_WAIT_WITHOUT_TIMEOUT", "DRE_DECLARED_RUNTIME_EXCEPTION"})
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void shutdown() throws AppbaseRuntimeException {
        if (statusRef.get() == ServiceStatus.SHUTTING_DOWN) {
            return;
        }

        final Timer.Context context = shutdownTimer.time();
        lifecycleReentrantLock.lock();
        try {
            logger.info("Shutting down {}", getClass().getName());
            statusRef.set(ServiceStatus.SHUTTING_DOWN);

            mediatorRef.get().shutdown();

            statusRef.set(ServiceStatus.SHUTDOWN);
            context.stop();
        } catch (final Exception ex) {
            shutdownErrorCounter.inc();
            statusRef.set(ServiceStatus.SHUTDOWN_FAILED);
            logger.error("Error while shutting down service {}", getClass().getName(), ex);
            context.stop();
        } finally {
            lifecycleReentrantLock.unlock();
        }
    }

    @Override
    public ServiceStatus getStatus() {
        return statusRef.get();
    }
}
