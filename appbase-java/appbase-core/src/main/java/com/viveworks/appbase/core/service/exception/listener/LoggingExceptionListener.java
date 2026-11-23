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

package com.viveworks.appbase.core.service.exception.listener;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.message.ExceptionMessage;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;

/**
 * {@link ExceptionListener} implementation that simply logs an {@link ExceptionMessage}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class LoggingExceptionListener implements ExceptionListener {
    private final Logger logger;
    private final Timer loggerTimer;

    public LoggingExceptionListener(final LogService logService, final MetricService metricService) {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");

        logger = logService.getLogger(getClass());
        loggerTimer = metricService
                .getMetricRegistry()
                .timer(metricService.name(getClass(), "logger", "timer"));
    }

    @Override
    public void onException(final ExceptionMessage exceptionMessage) {
        final Timer.Context context = loggerTimer.time();
        try {
            logger.error("Timestamp={}, Thread name={}, Source type={}, Message={}, Context={}",
                    exceptionMessage.getTimestamp(),
                    exceptionMessage.getThreadName(),
                    exceptionMessage.getSourceType().getName(),
                    exceptionMessage.getMessage(),
                    exceptionMessage.getContext(),
                    exceptionMessage.getThrowable());
        } finally {
            context.stop();
        }
    }

    public Timer getLoggerTimer() {
        return loggerTimer;
    }
}
