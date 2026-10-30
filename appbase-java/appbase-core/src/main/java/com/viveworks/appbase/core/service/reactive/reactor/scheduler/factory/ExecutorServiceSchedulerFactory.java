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
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceFactory;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.SchedulerFactory;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.slf4j.Logger;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Factory to create {@link Scheduler} using {@link Schedulers#fromExecutorService(ExecutorService, String)}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class ExecutorServiceSchedulerFactory implements SchedulerFactory<ExecutorServiceSchedulerConfiguration> {
    private final Logger logger;
    private final MetricService metricService;

    public ExecutorServiceSchedulerFactory(final LogService logService, final MetricService metricService) {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");

        this.logger = logService.getLogger(getClass());
        this.metricService = metricService;
    }

    @Override
    public Scheduler newScheduler(final ExecutorServiceSchedulerConfiguration schedulerConfiguration) {
        AppbaseUtil.notNull(schedulerConfiguration, "schedulerConfiguration");

        final ScheduledExecutorService scheduledExecutorService =
                ScheduledExecutorServiceFactory.newScheduledExecutorService(
                        schedulerConfiguration.getScheduledExecutorServiceConfiguration(),
                        AppbaseUtil.defaultUncaughtExceptionHandler(logger));
        metricService.registerExecutorServiceMetrics(
                scheduledExecutorService, schedulerConfiguration.getExecutorName());

        return Schedulers
                .fromExecutorService(scheduledExecutorService, schedulerConfiguration.getExecutorName());
    }
}
