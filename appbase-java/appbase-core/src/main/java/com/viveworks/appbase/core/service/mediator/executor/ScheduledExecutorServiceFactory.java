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

package com.viveworks.appbase.core.service.mediator.executor;

import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Factory for {@link java.util.concurrent.ScheduledExecutorService} using
 * {@link ScheduledExecutorServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public final class ScheduledExecutorServiceFactory {
    private ScheduledExecutorServiceFactory() {
    }

    /**
     * Create a new {@link ScheduledExecutorService}.
     *
     * @param scheduledExecutorServiceConfiguration Scheduled executor service configuration to use
     * @param uncaughtExceptionHandler Unknown exception handler to use
     * @return Scheduled executor service
     */
    public static ScheduledExecutorService newScheduledExecutorService(
            final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration,
            final Thread.UncaughtExceptionHandler uncaughtExceptionHandler)
    {
        AppbaseUtil.notNull(scheduledExecutorServiceConfiguration, "scheduledExecutorServiceConfiguration");
        ScheduledExecutorServiceConfigurationValidator.validate(
                scheduledExecutorServiceConfiguration.getScheduledExecutorServiceType(),
                scheduledExecutorServiceConfiguration.getThreadPoolSize(),
                scheduledExecutorServiceConfiguration.getThreadFactoryConfiguration());

        final ScheduledExecutorServiceType scheduledExecutorServiceType =
                scheduledExecutorServiceConfiguration.getScheduledExecutorServiceType();
        final ThreadFactory threadFactory = AppbaseUtil.newThreadFactory(
                scheduledExecutorServiceConfiguration.getThreadFactoryConfiguration(),
                uncaughtExceptionHandler);

        switch (scheduledExecutorServiceType) {
            case SINGLE_THREADED:
                return Executors.newSingleThreadScheduledExecutor(threadFactory);
            case THREAD_POOL:
                default:
                    return Executors.newScheduledThreadPool(
                            scheduledExecutorServiceConfiguration.getThreadPoolSize(),
                            threadFactory);
        }
    }
}
