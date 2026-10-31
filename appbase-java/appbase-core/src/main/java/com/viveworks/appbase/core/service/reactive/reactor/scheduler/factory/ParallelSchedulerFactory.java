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

package com.viveworks.appbase.core.service.reactive.reactor.scheduler.factory;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.SchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ParallelSchedulerConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.slf4j.Logger;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ThreadFactory;

/**
 * Factory to create {@link Scheduler} using {@link Schedulers#newParallel(int, ThreadFactory)}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class ParallelSchedulerFactory implements SchedulerFactory<ParallelSchedulerConfiguration> {
    private final Logger logger;

    public ParallelSchedulerFactory(final LogService logService) {
        AppbaseUtil.notNull(logService, "logService");

        logger = logService.getLogger(getClass());
    }

    @Override
    public Scheduler newScheduler(final ParallelSchedulerConfiguration schedulerConfiguration) {
        AppbaseUtil.notNull(schedulerConfiguration, "schedulerConfiguration");

        final ThreadFactory threadFactory = AppbaseUtil.newThreadFactory(
                schedulerConfiguration.getThreadFactoryConfiguration(),
                AppbaseUtil.defaultUncaughtExceptionHandler(logger));

        final Scheduler scheduler = Schedulers.newParallel(schedulerConfiguration.getParallelism(), threadFactory);
        scheduler.start();

        return scheduler;
    }
}
