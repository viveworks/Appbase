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

package com.viveworks.appbase.core.service.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.classic.spi.TurboFilterList;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.accessor.DefaultLogAccessor;
import com.viveworks.appbase.core.service.log.accessor.DefaultLogAccessorConfiguration;
import com.viveworks.appbase.core.service.log.accessor.DefaultLogAccessorConfigurationValidator;
import com.viveworks.appbase.core.service.log.accessor.LogAccessor;
import com.viveworks.appbase.core.service.log.appender.disruptor.DisruptorMediatorEventDelegatingAppender;
import com.viveworks.appbase.core.service.log.appender.disruptor.DisruptorMediatorEventDelegatingAppenderConfiguration;
import com.viveworks.appbase.core.service.log.appender.disruptor.MediatorEventDelegatingAppender;
import com.viveworks.appbase.core.service.log.appender.lucene.LuceneAppender;
import com.viveworks.appbase.core.service.log.appender.lucene.LuceneAppenderConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndex;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndex;
import com.viveworks.appbase.core.service.log.common.lucene.LuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.filter.burst.BurstTurboLogEventFilter;
import com.viveworks.appbase.core.service.log.filter.burst.BurstTurboLogEventFilterHandler;
import com.viveworks.appbase.core.service.log.filter.burst.BurstTurboLogEventFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.duplicate.DuplicateLogTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.duplicate.DuplicateTurboLogEventFilter;
import com.viveworks.appbase.core.service.log.filter.duplicate.DuplicateTurboLogEventFilterHandler;
import com.viveworks.appbase.core.service.log.filter.event.DisruptorLogAndTurboLogEventFilterDelegator;
import com.viveworks.appbase.core.service.log.filter.event.DisruptorLogAndTurboLogEventFilterDelegatorConfiguration;
import com.viveworks.appbase.core.service.log.filter.event.LogAndTurboLogEventFilterDelegator;
import com.viveworks.appbase.core.service.log.filter.event.LogEventFilter;
import com.viveworks.appbase.core.service.log.filter.event.LogEventFilterListener;
import com.viveworks.appbase.core.service.log.filter.event.TurboLogEventFilter;
import com.viveworks.appbase.core.service.log.filter.event.TurboLogEventFilterListener;
import com.viveworks.appbase.core.service.log.filter.event.listener.lucene.LuceneLogAndTurboLogEventFilterListener;
import com.viveworks.appbase.core.service.log.filter.event.listener.lucene.LuceneLogAndTurboLogEventFilterListenerConfiguration;
import com.viveworks.appbase.core.service.log.filter.metrics.MetricsTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.metrics.MetricsTurboLogEventFilter;
import com.viveworks.appbase.core.service.log.filter.metrics.MetricsTurboLogEventFilterHandler;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.LogStoreFactory;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStore;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreFactory;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.event.delegator.MediatorEventDelegatorRegistry;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.lucene.LuceneService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link LogService}. <code>Logback</code> is used as the SLF4J binding
 * to do the log.
 *
 * <h2>Filters and Appenders</h2>
 * <p>
 *     Filter events (both normal and turbo log events) are mainly handed off to a
 *     {@link DisruptorLogAndTurboLogEventFilterDelegator}. {@link LogEventFilterListener}s and
 *     {@link TurboLogEventFilterListener}s can be registered, so event handling is non-blocking and asynchronous.
 *     A {@link LogEventFilter} is attached to each appender and a {@link TurboLogEventFilter} is attach globally
 *     to manage this event delegation.
 * </p>
 *
 * <p>
 *     Appender events are mainly handed off to a {@link DisruptorMediatorEventDelegatingAppender}. Other appenders
 *     can be added to this appender, so event handling is non-blocking and asynchronous.
 * </p>
 *
 * <p>Appenders that are added (if enabled):</p>
 * <ul>
 *  <li>{@link DisruptorMediatorEventDelegatingAppender}</li>
 *  <li>{@link LuceneAppender} added to the above, not to a logger directly</li>
 * </ul>
 *
 * <p>Turbo log filers that are added globally are (if enabled):</p>
 * <ul>
 *  <li>{@link MetricsTurboLogEventFilter}</li>
 *  <li>{@link BurstTurboLogEventFilter}</li>
 *  <li>{@link DuplicateTurboLogEventFilter}</li>
 *  <li>{@link TurboLogEventFilter}, which uses {@link DisruptorLogAndTurboLogEventFilterDelegator}</li>
 * </ul>
 *
 * <p>Filters that are added are (if enabled):</p>
 * <ul>
 *  <li>
 *      {@link LuceneLogAndTurboLogEventFilterListener}, uses
 *      {@link DisruptorLogAndTurboLogEventFilterDelegator}, if
 *      {@link LuceneLogAndTurboLogEventFilterListenerConfiguration} has indexing of log and turbo log events
 *      enabled.
 *  </li>
 * </ul>
 *
 *
 * <h2>Log accessor</h2>
 * {@link DefaultLogAccessor} is initialized and a {@link LuceneLogStore} is registered with it.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_TOSTRING")
public class DefaultLogService implements LogService, LoggerContextListener {
    private static final boolean RESET_RESISTANT = true;

    private final Logger logger;
    private final AtomicReference<MetricService> metricServiceRef;
    private final AtomicReference<ValidationService> validationServiceRef;
    private final AtomicReference<MediatorService> mediatorServiceRef;
    private final AtomicReference<LuceneService> luceneServiceRef;

    private final MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry;
    private final AtomicReference<DefaultLogServiceConfiguration> logServiceConfigurationRef;
    private final AtomicReference<ServiceStatus> statusRef;

    private final AtomicReference<LogAccessor> logAccessorRef;
    private final AtomicReference<LogAndTurboLogEventFilterDelegator> logAndTurboLogEventFilterDelegatorRef;
    private final AtomicReference<LogEventFilter> logEventFilterRef;
    private final AtomicReference<TurboLogEventFilter> turboLogEventFilterRef;
    private final AtomicReference<MediatorEventDelegatingAppender> mediatorEventDelegatingAppenderRef;
    private final AtomicReference<MetricsTurboLogEventFilter> metricsTurboLogEventFilterRef;
    private final AtomicReference<BurstTurboLogEventFilter> burstTurboLogEventFilterRef;
    private final AtomicReference<DuplicateTurboLogEventFilter> duplicateTurboLogEventFilterRef;
    private final AtomicReference<LuceneLogIndex> luceneLogIndexRef;
    private final AtomicReference<LuceneAppender> luceneAppenderRef;
    private final AtomicReference<LuceneLogAndTurboLogEventFilterListener> luceneLogAndTurboLogEventFilterListenerRef;
    private final AtomicReference<LuceneLogStore> luceneLogStoreRef;
    private final ConcurrentMap<String, Class<?>> addedTurboFiltersTypeByNameMap;

    private final AtomicInteger reloadErrorCounter;
    private final AtomicInteger shutdownErrorCounter;
    private final AtomicInteger metricServiceNullErrorCounter;
    private final AtomicInteger validationServiceNullErrorCounter;
    private final AtomicInteger mediatorServiceNullErrorCounter;
    private final AtomicInteger luceneServiceNullErrorCounter;

    private final AtomicReference<Counter> onStartErrorCounterRef;
    private final AtomicReference<Counter> onResetErrorCounterRef;
    private final AtomicReference<Counter> onStopErrorCounterRef;
    private final AtomicReference<Counter> onLevelChangeCounterRef;
    private final AtomicReference<Timer> reloadTimerRef;
    private final AtomicReference<Timer> shutdownTimerRef;
    private final ReentrantLock lifecycleReentrantLock;
    private final AtomicBoolean shutdown;

    public DefaultLogService(
            final MediatorEventDelegatorRegistry mediatorEventDelegatorRegistry,
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration)
    {
        AppbaseUtil.notNull(mediatorEventDelegatorRegistry, "mediatorEventDelegatorRegistry");
        AppbaseUtil.notNull(defaultLogServiceConfiguration, "defaultLogServiceConfiguration");
        DefaultLogServiceConfigurationValidator.validate(
                defaultLogServiceConfiguration.getDefaultLogAccessorConfiguration(),
                defaultLogServiceConfiguration.getMetricsTurboFilterHandlerConfiguration(),
                defaultLogServiceConfiguration.getBurstTurboLogEventFilterHandlerConfiguration(),
                defaultLogServiceConfiguration.getDisruptorMediatorEventDelegatingAppenderConfiguration(),
                defaultLogServiceConfiguration.getLuceneLogIndexConfiguration(),
                defaultLogServiceConfiguration.getLuceneAppenderConfiguration(),
                defaultLogServiceConfiguration.getDisruptorLogAndTurboLogEventFilterDelegatorConfiguration(),
                defaultLogServiceConfiguration.getDuplicateLogTurboFilterHandlerConfiguration(),
                defaultLogServiceConfiguration.getLuceneLogAndTurboLogEventFilterListenerConfiguration());

        this.metricServiceRef = new AtomicReference<>();
        this.validationServiceRef = new AtomicReference<>();
        this.mediatorServiceRef = new AtomicReference<>();
        this.luceneServiceRef = new AtomicReference<>();

        this.mediatorEventDelegatorRegistry = mediatorEventDelegatorRegistry;
        this.logServiceConfigurationRef = new AtomicReference<>();
        this.logServiceConfigurationRef.set(defaultLogServiceConfiguration);
        this.logger = getLogger(getClass());

        logAccessorRef = new AtomicReference<>();
        logAndTurboLogEventFilterDelegatorRef = new AtomicReference<>();
        logEventFilterRef = new AtomicReference<>();
        turboLogEventFilterRef = new AtomicReference<>();
        mediatorEventDelegatingAppenderRef = new AtomicReference<>();
        metricsTurboLogEventFilterRef = new AtomicReference<>();
        burstTurboLogEventFilterRef = new AtomicReference<>();
        duplicateTurboLogEventFilterRef = new AtomicReference<>();
        luceneLogIndexRef = new AtomicReference<>();
        luceneAppenderRef = new AtomicReference<>();
        luceneLogAndTurboLogEventFilterListenerRef = new AtomicReference<>();
        luceneLogStoreRef = new AtomicReference<>();
        addedTurboFiltersTypeByNameMap = new ConcurrentHashMap<>();

        this.reloadErrorCounter = new AtomicInteger(0);
        this.shutdownErrorCounter = new AtomicInteger(0);
        this.metricServiceNullErrorCounter = new AtomicInteger(0);
        this.validationServiceNullErrorCounter = new AtomicInteger(0);
        this.mediatorServiceNullErrorCounter = new AtomicInteger(0);
        this.luceneServiceNullErrorCounter = new AtomicInteger(0);

        this.onStartErrorCounterRef = new AtomicReference<>();
        this.onResetErrorCounterRef = new AtomicReference<>();
        this.onStopErrorCounterRef = new AtomicReference<>();
        this.onLevelChangeCounterRef = new AtomicReference<>();
        this.reloadTimerRef = new AtomicReference<>();
        this.shutdownTimerRef = new AtomicReference<>();
        this.statusRef = new AtomicReference<>(ServiceStatus.STARTED);
        this.lifecycleReentrantLock = new ReentrantLock();
        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public Logger getLogger(final Class<?> type) {
        return LoggerFactory.getLogger(type);
    }

    @Override
    @GuardedBy("lifecycleReentrantLock")
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <C extends AppbaseConfiguration> void reload(final C configuration) throws ServiceReloadException {
        AppbaseUtil.notNull(configuration, "configuration");

        if (!DefaultLogServiceConfiguration.class.isAssignableFrom(configuration.getClass())) {
            throw new ServiceReloadException("The configuration " +
                    configuration.getClass().getName() +
                    " is not of type " +
                    DefaultLogServiceConfiguration.class.getName());
        }

        final long startTimestamp = System.nanoTime();
        lifecycleReentrantLock.lock();
        try {
            if (statusRef.get() == ServiceStatus.RELOADING) {
                return;
            }

            logger.info("Reloading {}", getClass().getName());
            statusRef.set(ServiceStatus.RELOADING);
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration =
                    (DefaultLogServiceConfiguration) configuration;
            DefaultLogServiceConfigurationValidator.validate(
                    defaultLogServiceConfiguration.getDefaultLogAccessorConfiguration(),
                    defaultLogServiceConfiguration.getMetricsTurboFilterHandlerConfiguration(),
                    defaultLogServiceConfiguration.getBurstTurboLogEventFilterHandlerConfiguration(),
                    defaultLogServiceConfiguration.getDisruptorMediatorEventDelegatingAppenderConfiguration(),
                    defaultLogServiceConfiguration.getLuceneLogIndexConfiguration(),
                    defaultLogServiceConfiguration.getLuceneAppenderConfiguration(),
                    defaultLogServiceConfiguration.getDisruptorLogAndTurboLogEventFilterDelegatorConfiguration(),
                    defaultLogServiceConfiguration.getDuplicateLogTurboFilterHandlerConfiguration(),
                    defaultLogServiceConfiguration.getLuceneLogAndTurboLogEventFilterListenerConfiguration());
            logServiceConfigurationRef.set(defaultLogServiceConfiguration);

            shutdown();
            final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            initFiltersAndAppenders(loggerContext);

            statusRef.set(ServiceStatus.STARTED);
        } catch (Exception ex) {
            reloadErrorCounter.incrementAndGet();
            statusRef.set(ServiceStatus.RELOAD_FAILED);
            logger.error("Error while reloading service {}", getClass().getName(), ex);
        } finally {
            final long endTimestamp = System.nanoTime();
            final Timer timer = reloadTimerRef.get();
            if (timer != null) {
                timer.update(Duration.ofMillis(endTimestamp - startTimestamp));
            }
            lifecycleReentrantLock.unlock();
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    @GuardedBy("lifecycleReentrantLock")
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        final long startTimestamp = System.currentTimeMillis();
        lifecycleReentrantLock.lock();
        try {
            if (statusRef.get() == ServiceStatus.SHUTTING_DOWN) {
                return;
            }

            statusRef.set(ServiceStatus.SHUTTING_DOWN);
            logger.info("Shutting down {}", getClass().getName());
            final LogAccessor logAccessor = logAccessorRef.get();
            if (logAccessor != null) {
                logAccessor.shutdown();
                logAccessorRef.set(null);
            }

            logAndTurboLogEventFilterDelegatorRef.set(null);
            logEventFilterRef.set(null);
            turboLogEventFilterRef.set(null);
            mediatorEventDelegatingAppenderRef.set(null);
            metricsTurboLogEventFilterRef.set(null);
            burstTurboLogEventFilterRef.set(null);
            duplicateTurboLogEventFilterRef.set(null);

            luceneAppenderRef.set(null);
            luceneLogAndTurboLogEventFilterListenerRef.set(null);
            luceneLogStoreRef.set(null);
            final LuceneLogIndex luceneLogIndex = luceneLogIndexRef.get();
            if (luceneLogIndex != null) {
                final LuceneService luceneService = luceneLogIndex.getLuceneService();
                if (luceneService != null) {
                    luceneService.shutdown();
                    luceneLogIndexRef.set(null);
                }
            }
            statusRef.set(ServiceStatus.SHUTDOWN);
        } catch (Exception ex) {
            shutdownErrorCounter.incrementAndGet();
            statusRef.set(ServiceStatus.SHUTDOWN_FAILED);
            logger.error("Error while shutting down service {}", getClass().getName(), ex);
        } finally {
            final long endTimestamp = System.nanoTime();
            final Timer timer = shutdownTimerRef.get();
            if (timer != null) {
                timer.update(Duration.ofMillis(endTimestamp - startTimestamp));
            }
            lifecycleReentrantLock.unlock();
        }
    }

    /**
     * Initialize filters and appenders.
     *
     * @param loggerContext Logback {@link LoggerContext}
     * @throws LogServiceException if there are any errors
     */
    public void initFiltersAndAppenders(final LoggerContext loggerContext) throws LogServiceException {
        final MetricService metricService = metricServiceRef.get();
        if (metricService == null) {
            metricServiceNullErrorCounter.incrementAndGet();
            throw new AppbaseRuntimeException("metricService must not be null");
        }

        final MediatorService mediatorService = mediatorServiceRef.get();
        if (mediatorService == null) {
            mediatorServiceNullErrorCounter.incrementAndGet();
            throw new AppbaseRuntimeException("mediatorService must not be null");
        }

        final ValidationService validationService = validationServiceRef.get();
        if (validationService == null) {
            validationServiceNullErrorCounter.incrementAndGet();
            throw new AppbaseRuntimeException("validationService must not be null");
        }

        final DefaultLogServiceConfiguration defaultLogServiceConfiguration = getConfiguration();
        final LuceneLogStoreConfiguration luceneLogStoreConfiguration =
                checkConfiguration(defaultLogServiceConfiguration);

        initDefaultLogAccessor(metricService, defaultLogServiceConfiguration);
        initMetricsTurboFilterHandler(metricService, defaultLogServiceConfiguration);
        initBurstTurboEventFilterHandler(mediatorService, defaultLogServiceConfiguration);
        initDuplicateLogTurboFilterHandler(mediatorService, defaultLogServiceConfiguration);
        initLogAndTurboLogEventFilterDelegator(mediatorService, defaultLogServiceConfiguration);
        initDisruptorMediatorEventDelegatingAppender(
                mediatorService,
                defaultLogServiceConfiguration);
        initLuceneDependents(
                metricService,
                validationService,
                mediatorService,
                defaultLogServiceConfiguration,
                luceneLogStoreConfiguration);
        updateLogbackContext(loggerContext);
    }

    private LuceneLogStoreConfiguration checkConfiguration(
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration)
    {
        final DisruptorMediatorEventDelegatingAppenderConfiguration
                disruptorMediatorEventDelegatingAppenderConfiguration =
                defaultLogServiceConfiguration.getDisruptorMediatorEventDelegatingAppenderConfiguration();
        final LuceneLogAndTurboLogEventFilterListenerConfiguration
                luceneLogAndTurboLogEventFilterListenerConfiguration =
                defaultLogServiceConfiguration.getLuceneLogAndTurboLogEventFilterListenerConfiguration();
        final LuceneAppenderConfiguration luceneAppenderConfiguration =
                defaultLogServiceConfiguration.getLuceneAppenderConfiguration();

        if (disruptorMediatorEventDelegatingAppenderConfiguration != null &&
                !disruptorMediatorEventDelegatingAppenderConfiguration.isEnabled())
        {
            if (luceneAppenderConfiguration != null && luceneAppenderConfiguration.isEnabled()) {
                throw new AppbaseRuntimeException(
                        DisruptorMediatorEventDelegatingAppenderConfiguration.class.getName() +
                                " is disabled, but " +
                                LuceneAppenderConfiguration.class.getName() +
                                " requires it to be enabled.");
            }
        }

        final LuceneLogIndexConfiguration luceneLogIndexConfiguration =
                defaultLogServiceConfiguration.getLuceneLogIndexConfiguration();
        if (luceneLogIndexConfiguration != null && !luceneLogIndexConfiguration.isEnabled()) {
            if (luceneAppenderConfiguration != null && luceneAppenderConfiguration.isEnabled()) {
                throw new AppbaseRuntimeException(
                        LuceneLogIndexConfiguration.class.getName() +
                                " is disabled, but " +
                                LuceneAppenderConfiguration.class.getName() +
                                " requires it to be enabled.");
            }

            if (luceneLogAndTurboLogEventFilterListenerConfiguration != null &&
                    luceneLogAndTurboLogEventFilterListenerConfiguration.isEnabled())
            {
                throw new AppbaseRuntimeException(
                        LuceneLogIndexConfiguration.class.getName() +
                                " is disabled, but " +
                                LuceneLogAndTurboLogEventFilterListenerConfiguration.class.getName() +
                                " requires it to be enabled.");
            }
        }

        final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration =
                defaultLogServiceConfiguration.getDisruptorLogAndTurboLogEventFilterDelegatorConfiguration();
        if (disruptorLogAndTurboLogEventFilterDelegatorConfiguration != null &&
                !disruptorLogAndTurboLogEventFilterDelegatorConfiguration.isEnabled())
        {
            if (luceneLogAndTurboLogEventFilterListenerConfiguration != null &&
                    luceneLogAndTurboLogEventFilterListenerConfiguration.isEnabled())
            {
                throw new AppbaseRuntimeException(
                        DisruptorLogAndTurboLogEventFilterDelegatorConfiguration.class.getName() +
                                " is disabled, but " +
                                LuceneLogAndTurboLogEventFilterListenerConfiguration.class.getName() +
                                " requires it to be enabled.");
            }
        }

        LuceneLogStoreConfiguration luceneLogStoreConfiguration = null;
        final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration =
                defaultLogServiceConfiguration.getDefaultLogAccessorConfiguration();
        if (defaultLogAccessorConfiguration != null && defaultLogAccessorConfiguration.isEnabled()) {
            DefaultLogAccessorConfigurationValidator.validate(
                    defaultLogAccessorConfiguration.isEnabled(),
                    defaultLogAccessorConfiguration.getLogStoreConfigurations());

            final List<LogStoreConfiguration> logStoreConfigurations =
                    defaultLogAccessorConfiguration.getLogStoreConfigurations();

            if (logStoreConfigurations != null) {
                for (final LogStoreConfiguration logStoreConfiguration : logStoreConfigurations) {
                    if (logStoreConfiguration instanceof LuceneLogStoreConfiguration) {
                        luceneLogStoreConfiguration = (LuceneLogStoreConfiguration) logStoreConfiguration;
                        if (luceneLogStoreConfiguration.isEnabled()) {
                            if (luceneLogIndexConfiguration != null && !luceneLogIndexConfiguration.isEnabled()) {
                                throw new AppbaseRuntimeException(
                                        LuceneLogIndexConfiguration.class.getName() +
                                                " is disabled, but " +
                                                LuceneLogStoreConfiguration.class.getName() +
                                                " requires it to be enabled.");
                            }
                        }
                    }
                }
            }
        }

        final LuceneService luceneService = luceneServiceRef.get();
        if ((luceneLogAndTurboLogEventFilterListenerConfiguration != null &&
                luceneLogAndTurboLogEventFilterListenerConfiguration.isEnabled()) ||
                (luceneAppenderConfiguration != null && luceneAppenderConfiguration.isEnabled()) ||
                (luceneLogStoreConfiguration != null && luceneLogStoreConfiguration.isEnabled()) ||
                (luceneLogIndexConfiguration != null && luceneLogIndexConfiguration.isEnabled()))
        {
            if (luceneService == null) {
                throw new AppbaseRuntimeException("luceneService must not be null");
            }
        }

        return luceneLogStoreConfiguration;
    }

    private void initDefaultLogAccessor(
            final MetricService metricService,
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration)
    {
        final DefaultLogAccessorConfiguration defaultLogAccessorConfiguration =
                defaultLogServiceConfiguration.getDefaultLogAccessorConfiguration();

        if (defaultLogAccessorConfiguration != null && defaultLogAccessorConfiguration.isEnabled()) {
            DefaultLogAccessorConfigurationValidator.validate(
                    defaultLogAccessorConfiguration.isEnabled(),
                    defaultLogAccessorConfiguration.getLogStoreConfigurations());

            logger.info("Initializing {}", DefaultLogAccessor.class.getName());

            final LogAccessor logAccessor = this.logAccessorRef.get();
            if (logAccessor != null) {
                logAccessor.shutdown();
            }

            final DefaultLogAccessor defaultLogAccessor =
                    new DefaultLogAccessor(this, metricService, defaultLogAccessorConfiguration);
            this.logAccessorRef.set(defaultLogAccessor);
        }
    }

    private void initMetricsTurboFilterHandler(
            final MetricService metricService,
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration)
    {
        final MetricsTurboFilterHandlerConfiguration metricsTurboFilterHandlerConfiguration =
                defaultLogServiceConfiguration.getMetricsTurboFilterHandlerConfiguration();
        if (metricsTurboFilterHandlerConfiguration != null &&
                metricsTurboFilterHandlerConfiguration.isEnabled())
        {
            logger.info("Initializing {}", MetricsTurboLogEventFilterHandler.class.getName());
            final MetricsTurboLogEventFilterHandler metricsTurboLogEventFilterHandler =
                    new MetricsTurboLogEventFilterHandler(metricService, metricsTurboFilterHandlerConfiguration);

            logger.info("Initializing {}", MetricsTurboLogEventFilter.class.getName());
            final MetricsTurboLogEventFilter metricsTurboLogEventFilter =
                    new MetricsTurboLogEventFilter(metricsTurboLogEventFilterHandler);
            metricsTurboLogEventFilterRef.set(metricsTurboLogEventFilter);
        }
    }

    private void initBurstTurboEventFilterHandler(
            final MediatorService mediatorService,
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration)
    {
        final BurstTurboLogEventFilterHandlerConfiguration burstTurboLogEventFilterHandlerConfiguration =
                defaultLogServiceConfiguration.getBurstTurboLogEventFilterHandlerConfiguration();
        if (burstTurboLogEventFilterHandlerConfiguration != null &&
                burstTurboLogEventFilterHandlerConfiguration.isEnabled())
        {
            logger.info("Initializing {}", BurstTurboLogEventFilterHandler.class.getName());
            final BurstTurboLogEventFilterHandler burstTurboLogEventFilterHandler =
                    new BurstTurboLogEventFilterHandler(burstTurboLogEventFilterHandlerConfiguration, mediatorService);

            logger.info("Initializing {}", BurstTurboLogEventFilter.class.getName());
            final BurstTurboLogEventFilter burstTurboLogEventFilter =
                    new BurstTurboLogEventFilter(burstTurboLogEventFilterHandler);
            burstTurboLogEventFilterRef.set(burstTurboLogEventFilter);
        }
    }

    private void initDuplicateLogTurboFilterHandler(
            final MediatorService mediatorService,
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration)
    {
        final DuplicateLogTurboFilterHandlerConfiguration duplicateLogTurboFilterHandlerConfiguration =
                defaultLogServiceConfiguration.getDuplicateLogTurboFilterHandlerConfiguration();
        if (duplicateLogTurboFilterHandlerConfiguration != null &&
                duplicateLogTurboFilterHandlerConfiguration.isEnabled())
        {
            logger.info("Initializing {}", DuplicateTurboLogEventFilterHandler.class.getName());
            final DuplicateTurboLogEventFilterHandler duplicateTurboLogEventFilterHandler =
                    new DuplicateTurboLogEventFilterHandler(
                            mediatorService, duplicateLogTurboFilterHandlerConfiguration);

            logger.info("Initializing {}", DuplicateTurboLogEventFilter.class.getName());
            final DuplicateTurboLogEventFilter duplicateTurboLogEventFilter =
                    new DuplicateTurboLogEventFilter(duplicateTurboLogEventFilterHandler);
            duplicateTurboLogEventFilterRef.set(duplicateTurboLogEventFilter);
        }
    }

    private void initLogAndTurboLogEventFilterDelegator(
            final MediatorService mediatorService,
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration)
    {
        final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration =
                defaultLogServiceConfiguration.getDisruptorLogAndTurboLogEventFilterDelegatorConfiguration();
        if (disruptorLogAndTurboLogEventFilterDelegatorConfiguration != null &&
                disruptorLogAndTurboLogEventFilterDelegatorConfiguration.isEnabled())
        {
            logger.info("Initializing {}", DisruptorLogAndTurboLogEventFilterDelegator.class.getName());
            final DisruptorLogAndTurboLogEventFilterDelegator disruptorLogAndTurboLogEventFilterDelegator =
                    new DisruptorLogAndTurboLogEventFilterDelegator(
                            disruptorLogAndTurboLogEventFilterDelegatorConfiguration,
                            mediatorService.getMediator().getSharedDisruptorService(),
                            mediatorEventDelegatorRegistry);
            logAndTurboLogEventFilterDelegatorRef.set(disruptorLogAndTurboLogEventFilterDelegator);

            logger.info("Initializing {}", LogEventFilter.class.getName());
            final LogEventFilter logEventFilter =
                    new LogEventFilter(disruptorLogAndTurboLogEventFilterDelegator);
            logEventFilterRef.set(logEventFilter);

            logger.info("Initializing {}", TurboLogEventFilter.class.getName());
            final TurboLogEventFilter turboLogEventFilter =
                    new TurboLogEventFilter(disruptorLogAndTurboLogEventFilterDelegator);
            turboLogEventFilterRef.set(turboLogEventFilter);
        }
    }

    private void initDisruptorMediatorEventDelegatingAppender(
            final MediatorService mediatorService,
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration)
    {
        final DisruptorMediatorEventDelegatingAppenderConfiguration
                disruptorMediatorEventDelegatingAppenderConfiguration =
                defaultLogServiceConfiguration.getDisruptorMediatorEventDelegatingAppenderConfiguration();
        if (disruptorMediatorEventDelegatingAppenderConfiguration != null &&
                disruptorMediatorEventDelegatingAppenderConfiguration.isEnabled())
        {
            logger.info("Initializing {}", DisruptorMediatorEventDelegatingAppender.class.getName());
            final DisruptorMediatorEventDelegatingAppender disruptorMediatorEventDelegatingAppender =
                    new DisruptorMediatorEventDelegatingAppender(
                            disruptorMediatorEventDelegatingAppenderConfiguration,
                            mediatorService.getMediator().getSharedDisruptorService(),
                            mediatorEventDelegatorRegistry);
            mediatorEventDelegatingAppenderRef.set(disruptorMediatorEventDelegatingAppender);
        }
    }

    private void initLuceneAppender(
            final LuceneAppenderConfiguration luceneAppenderConfiguration,
            final LuceneLogIndex luceneLogIndex)
    {
        if (luceneAppenderConfiguration != null && luceneAppenderConfiguration.isEnabled()) {
            logger.info("Initializing {}", LuceneAppender.class.getName());
            final LuceneAppender luceneAppender = new LuceneAppender(luceneLogIndex);
            mediatorEventDelegatingAppenderRef.get().addAppender(luceneAppender);
            luceneAppenderRef.set(luceneAppender);
        }
    }

    private void initLuceneLogAndTurboLogEventFilterListener(
            final LuceneLogAndTurboLogEventFilterListenerConfiguration
                    luceneLogAndTurboLogEventFilterListenerConfiguration,
            final LuceneLogIndex luceneLogIndex)
    {
        if (luceneLogAndTurboLogEventFilterListenerConfiguration != null &&
                luceneLogAndTurboLogEventFilterListenerConfiguration.isEnabled())
        {
            logger.info("Initializing {}", LuceneLogAndTurboLogEventFilterListener.class.getName());
            final LuceneLogAndTurboLogEventFilterListener luceneLogAndTurboLogEventFilterListener =
                    new LuceneLogAndTurboLogEventFilterListener(luceneLogIndex);

            logger.info("Initializing {}", LogAndTurboLogEventFilterDelegator.class.getName());
            final LogAndTurboLogEventFilterDelegator logAndTurboLogEventFilterDelegator =
                    logAndTurboLogEventFilterDelegatorRef.get();

            logger.info("Registering {} to {} as {}",
                    LuceneLogAndTurboLogEventFilterListener.class.getName(),
                    LogAndTurboLogEventFilterDelegator.class.getName(),
                    LogEventFilterListener.class.getName());
            if (luceneLogAndTurboLogEventFilterListenerConfiguration.isIndexLogEvent()) {
                logAndTurboLogEventFilterDelegator.registerLogEventListener(luceneLogAndTurboLogEventFilterListener);
            }

            logger.info("Registering {} to {} as {}",
                    LuceneLogAndTurboLogEventFilterListener.class.getName(),
                    LogAndTurboLogEventFilterDelegator.class.getName(),
                    TurboLogEventFilterListener.class.getName());
            if (luceneLogAndTurboLogEventFilterListenerConfiguration.isIndexTurboLog()) {
                logAndTurboLogEventFilterDelegator.registerTurboLogEventListener(
                        luceneLogAndTurboLogEventFilterListener);
            }

            luceneLogAndTurboLogEventFilterListenerRef.set(luceneLogAndTurboLogEventFilterListener);
        }
    }

    private void initLuceneLogStore(
            final MetricService metricService,
            final MediatorService mediatorService,
            final LuceneLogIndex luceneLogIndex,
            final LuceneLogStoreConfiguration luceneLogStoreConfiguration)
            throws LogServiceException
    {
        if (luceneLogStoreConfiguration != null && luceneLogStoreConfiguration.isEnabled()) {
            final ExecutorService executorService = mediatorService.getMediator().getSharedExecutorService();

            logger.info("Initializing {}", LuceneLogStoreFactory.class.getName());
            final LuceneLogStoreFactory luceneLogStoreFactory =
                    new LuceneLogStoreFactory(this, metricService, luceneLogIndex, executorService);

            logger.info("Initializing {}", LuceneLogStore.class.getName());
            final LuceneLogStore luceneLogStore = new LuceneLogStore(
                    this,
                    metricService,
                    luceneLogIndex,
                    executorService,
                    luceneLogStoreConfiguration);

            final LogAccessor logAccessor = getLogAccessor();
            logger.info("Registering {} with {}", LuceneLogStoreFactory.class.getName(), LogAccessor.class.getName());
            logAccessor.registerLogStoreFactoryByConfigurationType(
                    LuceneLogStoreConfiguration.class,
                    (LogStoreFactory) luceneLogStoreFactory);

            logger.info("Registering {} with {}", LuceneLogStore.class.getName(), LogAccessor.class.getName());
            logAccessor.registerLogStore(luceneLogStore);

            luceneLogStoreRef.set(luceneLogStore);
        }
    }

    private void initLuceneDependents(
            final MetricService metricService,
            final ValidationService validationService,
            final MediatorService mediatorService,
            final DefaultLogServiceConfiguration defaultLogServiceConfiguration,
            final LuceneLogStoreConfiguration luceneLogStoreConfiguration) throws LogServiceException
    {
        final LuceneLogIndexConfiguration luceneLogIndexConfiguration =
                defaultLogServiceConfiguration.getLuceneLogIndexConfiguration();

        if (luceneLogIndexConfiguration != null && luceneLogIndexConfiguration.isEnabled()) {
            LuceneLogIndex luceneLogIndex = luceneLogIndexRef.get();
            if (luceneLogIndex == null) {
                logger.info("Initializing {}", DefaultLuceneLogIndex.class.getName());
                luceneLogIndex = new DefaultLuceneLogIndex(
                        this,
                        metricService,
                        validationService,
                        mediatorService,
                        luceneServiceRef.get(),
                        luceneLogIndexConfiguration);
                luceneLogIndexRef.set(luceneLogIndex);
            }

            final LuceneAppenderConfiguration luceneAppenderConfiguration =
                    defaultLogServiceConfiguration.getLuceneAppenderConfiguration();
            initLuceneAppender(luceneAppenderConfiguration, luceneLogIndex);

            final LuceneLogAndTurboLogEventFilterListenerConfiguration
                    luceneLogAndTurboLogEventFilterListenerConfiguration =
                    defaultLogServiceConfiguration.getLuceneLogAndTurboLogEventFilterListenerConfiguration();
            initLuceneLogAndTurboLogEventFilterListener(
                    luceneLogAndTurboLogEventFilterListenerConfiguration, luceneLogIndex);
            initLuceneLogStore(metricService, mediatorService, luceneLogIndex, luceneLogStoreConfiguration);
        }
    }

    private void updateLogbackContext(final LoggerContext loggerContext) {
        final TurboFilterList turboFilterList = loggerContext.getTurboFilterList();
        if (turboFilterList != null) {
            final MetricsTurboLogEventFilter metricsTurboLogEventFilter = metricsTurboLogEventFilterRef.get();
            final BurstTurboLogEventFilter burstTurboLogEventFilter = burstTurboLogEventFilterRef.get();
            final DuplicateTurboLogEventFilter duplicateTurboLogEventFilter = duplicateTurboLogEventFilterRef.get();
            final TurboLogEventFilter turboLogEventFilter = turboLogEventFilterRef.get();

            for (int i = 0; i < turboFilterList.size(); ++i) {
                final TurboFilter turboFilter = turboFilterList.get(i);
                if (turboFilter != null) {
                    if ((metricsTurboLogEventFilter != null && turboFilter instanceof MetricsTurboLogEventFilter) ||
                            (burstTurboLogEventFilter != null && turboFilter instanceof BurstTurboLogEventFilter) ||
                            (turboLogEventFilter != null && turboFilter instanceof TurboLogEventFilter) ||
                            (duplicateTurboLogEventFilter != null &&
                                    turboFilter instanceof DuplicateTurboLogEventFilter))
                    {
                        logger.info("Removing {} from {}",
                                turboFilter.getClass().getName(), turboFilterList.getClass().getName());
                        turboFilterList.remove((int) i);
                    }
                }
            }

            if (metricsTurboLogEventFilter != null &&
                    !addedTurboFiltersTypeByNameMap.containsKey(metricsTurboLogEventFilter.getClass().getName()))
            {
                logger.info("Adding {} to {}",
                        metricsTurboLogEventFilter.getClass().getName(), turboFilterList.getClass().getName());
                turboFilterList.add(metricsTurboLogEventFilter);
                addedTurboFiltersTypeByNameMap.put(
                        metricsTurboLogEventFilter.getClass().getName(), metricsTurboLogEventFilter.getClass());
            }

            if (burstTurboLogEventFilter != null &&
                    !addedTurboFiltersTypeByNameMap.containsKey(burstTurboLogEventFilter.getClass().getName()))
            {
                logger.info("Adding {} to {}",
                        burstTurboLogEventFilter.getClass().getName(), turboFilterList.getClass().getName());
                turboFilterList.add(burstTurboLogEventFilter);
                addedTurboFiltersTypeByNameMap.put(
                        burstTurboLogEventFilter.getClass().getName(), burstTurboLogEventFilter.getClass());
            }

            if (duplicateTurboLogEventFilter != null &&
                    !addedTurboFiltersTypeByNameMap.containsKey(duplicateTurboLogEventFilter.getClass().getName()))
            {
                logger.info("Adding {} to {}",
                       duplicateTurboLogEventFilter.getClass().getName(), turboFilterList.getClass().getName());
                turboFilterList.add(duplicateTurboLogEventFilter);
                addedTurboFiltersTypeByNameMap.put(
                        duplicateTurboLogEventFilter.getClass().getName(), duplicateTurboLogEventFilter.getClass());
            }

            if (turboLogEventFilter != null &&
                    !addedTurboFiltersTypeByNameMap.containsKey(turboLogEventFilter.getClass().getName()))
            {
                logger.info("Adding {} to {}",
                        turboLogEventFilter.getClass().getName(), turboFilterList.getClass().getName());
                turboFilterList.add(turboLogEventFilter);
                addedTurboFiltersTypeByNameMap.put(
                        turboLogEventFilter.getClass().getName(), turboLogEventFilter.getClass());
            }
        }

        final MediatorEventDelegatingAppender mediatorEventDelegatingAppender =
                mediatorEventDelegatingAppenderRef.get();
        if (mediatorEventDelegatingAppender != null) {
            mediatorEventDelegatingAppender.setContext(loggerContext);
        }

        final LogEventFilter logEventFilter = logEventFilterRef.get();
        final List<ch.qos.logback.classic.Logger> loggerList = loggerContext.getLoggerList();
        if (loggerList != null) {
            loggerList.forEach(attachedLogger -> {
                if (attachedLogger != null) {
                    final Iterator<Appender<ILoggingEvent>> appenderIterator =
                            attachedLogger.iteratorForAppenders();

                    boolean addMediatorEventDelegatingAppender = false;
                    if (appenderIterator.hasNext()) {
                        final Appender<ILoggingEvent> appender = appenderIterator.next();
                        if (appender != null) {
                            final List<Filter<ILoggingEvent>> copyOfAttachedFiltersList =
                                    appender.getCopyOfAttachedFiltersList();
                            boolean addFilter = true;
                            for (final Filter<ILoggingEvent> filter : copyOfAttachedFiltersList) {
                                if (filter instanceof LogEventFilter) {
                                    addFilter = false;
                                    break;
                                }
                            }

                            final String appenderName = appender.getName();
                            if (addFilter && (logEventFilter != null)) {
                                logger.info("Adding {} to appender {}",
                                        logEventFilter.getClass().getName(), appenderName);
                                appender.addFilter(logEventFilter);
                            }

                            if (mediatorEventDelegatingAppender != null &&
                                    !mediatorEventDelegatingAppender.isAttached(appender) &&
                                    !(appender instanceof MediatorEventDelegatingAppender))
                            {
                                logger.info("Adding appender {} to {}",
                                        appenderName, mediatorEventDelegatingAppender.getClass().getName());
                                mediatorEventDelegatingAppender.addAppender(appender);
                                addMediatorEventDelegatingAppender = true;
                            }
                        }
                    }

                    if (addMediatorEventDelegatingAppender) {
                        logger.info("Adding {} to logger {}",
                                MediatorEventDelegatingAppender.class.getName(), attachedLogger.getName());
                        attachedLogger.addAppender(mediatorEventDelegatingAppender);

                        final Iterator<Appender<ILoggingEvent>> appenderIterator2 =
                                attachedLogger.iteratorForAppenders();
                        while (appenderIterator2.hasNext()) {
                            final Appender<ILoggingEvent> appender = appenderIterator2.next();
                            if (appender != null && !(appender instanceof MediatorEventDelegatingAppender)) {
                                logger.info("Detaching appender {} from logger {}", appender, attachedLogger.getName());
                                attachedLogger.detachAppender(appender);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Set {@link MetricService}.
     *
     * @param metricService Metric service
     */
    public void setMetricService(final MetricService metricService) {
        AppbaseUtil.notNull(metricService, "metricService");
        metricServiceRef.set(metricService);

        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "reload", "error", "gauge"),
                (Gauge<Integer>) reloadErrorCounter::get);
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "shutdown", "error", "gauge"),
                (Gauge<Integer>) shutdownErrorCounter::get);
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "metricService", "null", "error", "gauge"),
                (Gauge<Integer>) metricServiceNullErrorCounter::get);
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "validationService", "null", "error", "gauge"),
                (Gauge<Integer>) validationServiceNullErrorCounter::get);
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "mediatorService", "null", "error", "gauge"),
                (Gauge<Integer>) mediatorServiceNullErrorCounter::get);
        metricService.getMetricRegistry().register(
                metricService.name(getClass(), "luceneService", "null", "error", "gauge"),
                (Gauge<Integer>) luceneServiceNullErrorCounter::get);

        final Counter onStartErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "onStart", "error", "counter"));
        onStartErrorCounterRef.set(onStartErrorCounter);

        final Counter onResetErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "onReset", "error", "counter"));
        onResetErrorCounterRef.set(onResetErrorCounter);

        final Counter onStopErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "onStop", "error", "counter"));
        onStopErrorCounterRef.set(onStopErrorCounter);

        final Counter onLevelChangeErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "onLevelChange", "error", "counter"));
        onLevelChangeCounterRef.set(onLevelChangeErrorCounter);

        final Timer reloadTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "reload", "timer"));
        reloadTimerRef.set(reloadTimer);

        final Timer shutdownTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "shutdown", "timer"));
        shutdownTimerRef.set(shutdownTimer);
    }

    /**
     * Set {@link ValidationService}.
     *
     * @param validationService Validation service
     */
    public void setValidationService(final ValidationService validationService) {
        AppbaseUtil.notNull(validationService, "validationService");

        validationServiceRef.set(validationService);
    }

    /**
     * Set {@link MediatorService}.
     *
     * @param mediatorService Mediator service
     */
    public void setMediatorService(final MediatorService mediatorService) {
        AppbaseUtil.notNull(mediatorService, "mediatorService");

        mediatorServiceRef.set(mediatorService);
    }

    /**
     * Set {@link LuceneService}.
     *
     * @param luceneService Lucene service
     */
    public void setLuceneService(final LuceneService luceneService) {
        AppbaseUtil.notNull(luceneService, "luceneService");

        luceneServiceRef.set(luceneService);
    }

    @Override
    public boolean isResetResistant() {
        return RESET_RESISTANT;
    }

    @SuppressWarnings("IllegalCatch")
    private void onLoggingContextEvent(
            final LoggerContext loggerContext, final Counter counter, final String eventName)
    {
        final DefaultLogServiceConfiguration configuration = getConfiguration();
        if (configuration != null) {
            try {
                initFiltersAndAppenders(loggerContext);
            } catch (Exception ex) {
                logger.error("Error on event {}", eventName, ex);
                if (counter != null) {
                    counter.inc();
                }
            }
        }
    }

    @Override
    public void onStart(final LoggerContext context) {
        onLoggingContextEvent(context, onStartErrorCounterRef.get(), "start");
    }

    @Override
    public void onReset(final LoggerContext context) {
        onLoggingContextEvent(context, onResetErrorCounterRef.get(), "clean");
    }

    @Override
    public void onStop(final LoggerContext context) {
        onLoggingContextEvent(context, onStopErrorCounterRef.get(), "stop");
    }

    @Override
    public void onLevelChange(final ch.qos.logback.classic.Logger logger, final Level level) {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        onLoggingContextEvent(loggerContext, onLevelChangeCounterRef.get(), "levelChange");
    }

    @Override
    public ServiceStatus getStatus() {
        return statusRef.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultLogServiceConfiguration getConfiguration() {
        return logServiceConfigurationRef.get();
    }

    @Override
    public LogAccessor getLogAccessor() {
        return logAccessorRef.get();
    }

    @Nullable
    @Override
    public LogAndTurboLogEventFilterDelegator getLogAndTurboLogEventFilterDelegator() {
        return logAndTurboLogEventFilterDelegatorRef.get();
    }

    @Nullable
    @Override
    public MediatorEventDelegatingAppender getMediatorEventDelegatingAppender() {
        return mediatorEventDelegatingAppenderRef.get();
    }

    public MediatorEventDelegatorRegistry getMediatorEventDelegatorRegistry() {
        return mediatorEventDelegatorRegistry;
    }

    public MetricsTurboLogEventFilter getMetricsTurboLogEventFilter() {
        return metricsTurboLogEventFilterRef.get();
    }

    public BurstTurboLogEventFilter getBurstTurboLogEventFilter() {
        return burstTurboLogEventFilterRef.get();
    }

    public DuplicateTurboLogEventFilter getDuplicateTurboLogEventFilter() {
        return duplicateTurboLogEventFilterRef.get();
    }

    public LuceneService getLuceneService() {
        return luceneServiceRef.get();
    }

    public LuceneLogIndex getLuceneLogIndex() {
        return luceneLogIndexRef.get();
    }

    public LuceneAppender getLuceneAppender() {
        return luceneAppenderRef.get();
    }

    public LuceneLogAndTurboLogEventFilterListener getLuceneLogAndTurboLogEventFilterListener() {
        return luceneLogAndTurboLogEventFilterListenerRef.get();
    }

    public LogEventFilter getLogEventFilter() {
        return logEventFilterRef.get();
    }

    public TurboLogEventFilter getTurboLogEventFilter() {
        return turboLogEventFilterRef.get();
    }

    public LuceneLogStore getLuceneLogStore() {
        return luceneLogStoreRef.get();
    }
}
