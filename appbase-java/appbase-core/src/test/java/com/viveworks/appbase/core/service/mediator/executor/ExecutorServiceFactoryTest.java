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
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ExecutorServiceFactoryTest {
    @Test
    void testNewExecutorService() {
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
                (t, e) -> {
                    throw new RuntimeException("Exception in thread " + t.getName(), e);
                };

        final ExecutorServiceConfiguration executorServiceConfiguration1 =
                ExecutorServiceConfiguration.builder().build();
        final ExecutorService executorService1 =
                ExecutorServiceFactory.newExecutorService(executorServiceConfiguration1, uncaughtExceptionHandler);
        Assertions.assertNotNull(executorService1);
        AppbaseUtil.shutdownExecutor(executorService1);

        final ExecutorServiceConfiguration executorServiceConfiguration2 = ExecutorServiceConfiguration
                .builder()
                .executorType(ExecutorServiceType.CACHED)
                .build();
        final ExecutorService executorService2 =
                ExecutorServiceFactory.newExecutorService(executorServiceConfiguration2, uncaughtExceptionHandler);
        Assertions.assertNotNull(executorService2);
        AppbaseUtil.shutdownExecutor(executorService2);


        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> {
                    ExecutorServiceConfiguration
                            .builder()
                            .executorType(ExecutorServiceType.FIXED)
                            .fixedThreadPoolSize(0)
                            .build();
                });

        final ExecutorServiceConfiguration executorServiceConfiguration4 = ExecutorServiceConfiguration
                .builder()
                .executorType(ExecutorServiceType.FIXED)
                .fixedThreadPoolSize(4)
                .build();
        final ExecutorService executorService4 =
                ExecutorServiceFactory.newExecutorService(executorServiceConfiguration4, uncaughtExceptionHandler);
        Assertions.assertNotNull(executorService4);
        AppbaseUtil.shutdownExecutor(executorService4);
    }
}
