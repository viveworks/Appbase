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

package com.viveworks.appbase.core.service;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.message.MessageFactory;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An abstract class for concrete service implementation to extend. It provides re-usable logic of house-keeping of
 * service states.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
@SuppressWarnings("WeakerAccess")
public abstract class AbstractAppbaseService implements AppbaseService {
    protected final AtomicReference<ServiceStatus> statusRef;
    protected final ReentrantLock lifecycleReentrantLock;

    protected final LogService logService;
    protected final MetricService metricService;
    protected final MediatorService mediatorService;
    protected final Logger logger;

    private final Timer reloadTimer;
    private final Timer shutdownTimer;
    private final Counter reloadErrorCounter;
    private final Counter shutdownErrorCounter;

    /**
     * Initializes lifecycle state at {@link ServiceStatus#STARTING} and registers reload and shutdown metrics.
     * The lifecycle lock serializes later reload and shutdown operations.
     *
     * @param logService source of the concrete service logger
     * @param metricService registry used for lifecycle timers and error counters
     * @param mediatorService recipient of status and exception messages
     * @throws com.viveworks.appbase.core.AppbaseRuntimeException if any service argument is {@code null}
     */
    public AbstractAppbaseService(final LogService logService,
                                  final MetricService metricService,
                                  final MediatorService mediatorService)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(mediatorService, "mediatorService");

        this.logService = logService;
        this.metricService = metricService;
        this.mediatorService = mediatorService;
        this.logger = logService.getLogger(getClass());

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
        this.statusRef = new AtomicReference<>(ServiceStatus.STARTING);
    }


    /**
     * Reload the service using the new configuration.
     *
     * @param configuration The new configuration to use for reload
     * @param <C> Configuration type
     * @throws ServiceReloadException if there were any exceptions during reload
     */
    protected abstract <C extends AppbaseConfiguration> void reloadImpl(
            C configuration) throws ServiceReloadException;


    /**
     * Shutdown the service.
     */
    protected abstract void shutdownImpl();


    @Override
    @GuardedBy("lifecycleReentrantLock")
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <C extends AppbaseConfiguration> void reload(final C configuration) throws ServiceReloadException {
        AppbaseUtil.notNull(configuration, "configuration");

        final Timer.Context context = reloadTimer.time();
        lifecycleReentrantLock.lock();
        try {
            final ServiceStatus currentStatus = statusRef.get();
            if (currentStatus == ServiceStatus.RELOADING) {
                return;
            }
            // AUD-14 / DQ-5: shutdown is terminal. A reload must not silently resurrect resources
            // that shutdownImpl() has already closed, nor advertise that service as healthy again.
            // An explicit restart operation can be introduced separately if the product needs one.
            if (currentStatus == ServiceStatus.SHUTTING_DOWN ||
                    currentStatus == ServiceStatus.SHUTDOWN ||
                    currentStatus == ServiceStatus.SHUTDOWN_FAILED)
            {
                throw new ServiceReloadException(
                        "Cannot reload " + getClass().getName() + " from terminal status " + currentStatus);
            }
            logger.info("Reloading {} using {}", getClass().getName(), configuration);
            updateStatus(ServiceStatus.RELOADING);

            try {
                reloadImpl(configuration);
                updateStatus(ServiceStatus.STARTED);
            } catch (final Exception exception) {
                final String message = String.format("Error while reloading %s: %s",
                        getClass().getName(), exception.toString());
                handleException(message, exception);

                reloadErrorCounter.inc();
                updateStatus(ServiceStatus.RELOAD_FAILED);
                throw new ServiceReloadException(
                        "Service reload failed for configuration: " + configuration, exception);
            }
        } finally {
            context.stop();
            lifecycleReentrantLock.unlock();
        }
    }

    @Override
    @GuardedBy("lifecycleReentrantLock")
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings({"checkstyle:IllegalCatch"})
    public void shutdown() throws AppbaseRuntimeException {
        final Timer.Context context = shutdownTimer.time();
        lifecycleReentrantLock.lock();
        try {
            // The guard must be read and acted on under the lock, not before it (Lens 3 L3-02): a
            // pre-lock read of statusRef is a check-then-act race against a concurrent caller, and it
            // only recognized SHUTTING_DOWN, never the terminal SHUTDOWN it leaves behind -- so a
            // second call made after the first had fully completed fell through and re-invoked
            // shutdownImpl(), double-closing whatever resources a subclass owns.
            final ServiceStatus currentStatus = statusRef.get();
            if (currentStatus == ServiceStatus.SHUTTING_DOWN || currentStatus == ServiceStatus.SHUTDOWN) {
                context.stop();
                return;
            }

            logger.info("Shutting down {}", getClass().getName());
            updateStatus(ServiceStatus.SHUTTING_DOWN);
            shutdownImpl();
            updateStatus(ServiceStatus.SHUTDOWN);
            context.stop();
        } catch (final Exception exception) {
            final String message = String.format("Error while shutting down %s: %s",
                    getClass().getName(), exception.toString());
            handleException(message, exception);

            shutdownErrorCounter.inc();
            updateStatus(ServiceStatus.SHUTDOWN_FAILED);
            context.stop();
        } finally {
            lifecycleReentrantLock.unlock();
        }
    }

    @Override
    public ServiceStatus getStatus() {
        return statusRef.get();
    }

    @Override
    public boolean isShutdown() {
        return getStatus() == ServiceStatus.SHUTDOWN;
    }

    protected void handleException(final String message, final Exception exception) {
        try {
            final Message exceptionMessage = MessageFactory.exceptionMessage(
                    mediatorService,
                    getClass(),
                    message,
                    AppbaseUtil.threadName(),
                    exception);
            mediatorService.getMediator().asyncHandleMessage(exceptionMessage, true);
        } catch (MediatorException | RejectedExecutionException publicationFailure) {
            logMediatorPublicationFailure(
                    "Error sending exception message to mediator: {}", exception, publicationFailure);
        }
    }

    protected void updateStatus(final ServiceStatus serviceStatus) {
        try {
            final Message serviceStatusMessage = MessageFactory.serviceStatusMessage(
                    mediatorService, getClass(), serviceStatus);
            logger.info("Updating status: {}", serviceStatus);
            mediatorService.getMediator().asyncHandleMessage(serviceStatusMessage, true);
        } catch (MediatorException | RejectedExecutionException publicationFailure) {
            logMediatorPublicationFailure(
                    "Error sending service status message to mediator: {}", serviceStatus, publicationFailure);
        }
        statusRef.set(serviceStatus);
    }

    private void logMediatorPublicationFailure(
            final String message, final Object detail, final Exception publicationFailure)
    {
        try {
            logger.error(message, detail, publicationFailure);
        } catch (RejectedExecutionException loggingFailure) {
            // A mediator-backed logging appender can reject for the same bounded teardown reason.
            publicationFailure.addSuppressed(loggingFailure);
        }
    }
}
