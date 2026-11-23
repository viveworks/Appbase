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

package com.viveworks.appbase.core.service.validation;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.ServiceReloadException;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link ValidationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
@SuppressFBWarnings(value = "IMC_IMMATURE_CLASS_NO_TOSTRING")
public class DefaultValidationService implements ValidationService {
    private final AtomicReference<Validator> validatorRef;
    private final AtomicReference<DefaultValidationServiceConfiguration> validatorServiceConfigurationRef;

    private final Logger logger;
    private final AtomicReference<ServiceStatus> statusRef;
    private final ReentrantLock lifecycleReentrantLock;

    private final Timer reloadTimer;
    private final Timer shutdownTimer;
    private final Counter reloadErrorCounter;
    private final Counter shutdownErrorCounter;

    @SuppressFBWarnings("PCOA_PARTIALLY_CONSTRUCTED_OBJECT_ACCESS")
    public DefaultValidationService(final LogService logService,
                                    final MetricService metricService)
    {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");

        this.logger = logService.getLogger(getClass());

        this.validatorServiceConfigurationRef = new AtomicReference<>();
        this.validatorServiceConfigurationRef.set(new DefaultValidationServiceConfiguration());

        final Validator validator = newDefaultValidator();
        this.validatorRef = new AtomicReference<>();
        this.validatorRef.set(validator);

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

    public DefaultValidationService(final LogService logService,
                                    final MetricService metricService,
                                    final Validator validator)
    {
        AppbaseUtil.notNull(validator, "validation");

        this.logger = logService.getLogger(getClass());
        this.validatorServiceConfigurationRef = new AtomicReference<>();
        this.validatorServiceConfigurationRef.set(new DefaultValidationServiceConfiguration());

        this.validatorRef = new AtomicReference<>();
        this.validatorRef.set(validator);

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

    protected Validator newDefaultValidator() {
        final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        return validatorFactory.getValidator();
    }

    @Override
    public Validator getValidator() {
        return validatorRef.get();
    }


    @Override
    @GuardedBy("lifecycleReentrantLock")
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public <C extends AppbaseConfiguration> void reload(final C configuration) throws ServiceReloadException {
        AppbaseUtil.notNull(configuration, "configuration");

        if (!DefaultValidationServiceConfiguration.class.isAssignableFrom(configuration.getClass())) {
            throw new ServiceReloadException("The configuration " +
                    configuration.getClass().getName() +
                    " is not of type " +
                    DefaultValidationServiceConfiguration.class.getName());
        }

        final Timer.Context context = reloadTimer.time();
        lifecycleReentrantLock.lock();
        try {
            if (statusRef.get() == ServiceStatus.RELOADING) {
                return;
            }

            logger.info("Reloading {}", getClass().getName());
            statusRef.set(ServiceStatus.RELOADING);

            final Validator validator = newDefaultValidator();
            validatorRef.set(validator);
            validatorServiceConfigurationRef.set((DefaultValidationServiceConfiguration) configuration);

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
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void shutdown() throws AppbaseRuntimeException {
        if (statusRef.get() == ServiceStatus.SHUTTING_DOWN) {
            return;
        }

        final Timer.Context context = shutdownTimer.time();
        lifecycleReentrantLock.lock();
        try {
            logger.info("Shutting down {}", getClass().getName());
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

    @Override
    @SuppressWarnings("unchecked")
    public DefaultValidationServiceConfiguration getConfiguration() {
        return validatorServiceConfigurationRef.get();
    }
}
