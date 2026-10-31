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

package com.viveworks.appbase.core.service.mediator.executor;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ScheduledExecutorServiceFactoryTest {
    @Test
    void testNewScheduledExecutorService() {
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
                (t, e) -> {
                    throw new RuntimeException("Exception in thread " + t.getName(), e);
                };

        final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration1 =
                ScheduledExecutorServiceConfiguration.builder().build();
        final ScheduledExecutorService scheduledExecutorService1 =
                ScheduledExecutorServiceFactory.newScheduledExecutorService(
                        scheduledExecutorServiceConfiguration1,
                        uncaughtExceptionHandler);
        Assertions.assertNotNull(scheduledExecutorService1);
        AppbaseUtil.shutdownExecutor(scheduledExecutorService1);

        final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration2 =
                ScheduledExecutorServiceConfiguration
                        .builder()
                        .scheduledExecutorType(ScheduledExecutorServiceType.SINGLE_THREADED)
                        .threadFactoryConfiguration(
                                ThreadFactoryConfiguration
                                        .builder()
                                        .nameFormat("appbase-scheduled-executor-%d")
                                        .build())
                        .build();
        final ScheduledExecutorService scheduledExecutorService2 =
                ScheduledExecutorServiceFactory.newScheduledExecutorService(
                        scheduledExecutorServiceConfiguration2,
                        uncaughtExceptionHandler);
        Assertions.assertNotNull(scheduledExecutorService2);
        AppbaseUtil.shutdownExecutor(scheduledExecutorService2);

        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    ScheduledExecutorServiceConfiguration
                            .builder()
                            .scheduledExecutorType(ScheduledExecutorServiceType.THREAD_POOL)
                            .threadPoolSize(0)
                            .threadFactoryConfiguration(
                                    ThreadFactoryConfiguration
                                            .builder()
                                            .nameFormat("appbase-scheduled-executor-%d")
                                            .build())
                            .build();
                });

        final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration4 =
                ScheduledExecutorServiceConfiguration
                        .builder()
                        .scheduledExecutorType(ScheduledExecutorServiceType.THREAD_POOL)
                        .threadPoolSize(4)
                        .threadFactoryConfiguration(
                                ThreadFactoryConfiguration
                                        .builder()
                                        .nameFormat("appbase-scheduled-executor-%d")
                                        .build())
                        .build();
        final ScheduledExecutorService scheduledExecutorService4 =
                ScheduledExecutorServiceFactory.newScheduledExecutorService(
                        scheduledExecutorServiceConfiguration4,
                        uncaughtExceptionHandler);
        Assertions.assertNotNull(scheduledExecutorService4);
        AppbaseUtil.shutdownExecutor(scheduledExecutorService4);
    }
}
