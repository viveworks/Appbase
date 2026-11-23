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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Factory for {@link java.util.concurrent.ExecutorService} using
 * {@link ExecutorServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public final class ExecutorServiceFactory {
    private ExecutorServiceFactory() {
    }

    /**
     * Create a {@link ExecutorService}.
     *
     * @param executorServiceConfiguration Configuration to use for the new executor service
     * @param uncaughtExceptionHandler Thread unknown exception handler to use
     * @return Executor service
     */
    public static ExecutorService newExecutorService(
            final ExecutorServiceConfiguration executorServiceConfiguration,
            final Thread.UncaughtExceptionHandler uncaughtExceptionHandler)
    {
        AppbaseUtil.notNull(executorServiceConfiguration, "scheduledExecutorServiceConfiguration");
        ExecutorServiceConfigurationValidator.validate(
                executorServiceConfiguration.getExecutorServiceType(),
                executorServiceConfiguration.getFixedThreadPoolSize(),
                executorServiceConfiguration.getThreadFactoryConfiguration());

        final ExecutorServiceType executorServiceType = executorServiceConfiguration.getExecutorServiceType();
        final ThreadFactory threadFactory = AppbaseUtil.newThreadFactory(
                executorServiceConfiguration.getThreadFactoryConfiguration(),
                uncaughtExceptionHandler);

        switch (executorServiceType) {
            case SINGLE_THREADED:
                return Executors.newSingleThreadExecutor(threadFactory);
            case FIXED:
                return Executors.newFixedThreadPool(
                        executorServiceConfiguration.getFixedThreadPoolSize(),
                        threadFactory);
            case CACHED:
                default:
                    return Executors.newCachedThreadPool(threadFactory);
        }
    }
}
