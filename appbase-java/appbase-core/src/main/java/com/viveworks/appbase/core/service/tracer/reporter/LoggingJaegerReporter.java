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

package com.viveworks.appbase.core.service.tracer.reporter;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.spi.Reporter;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;

/**
 * {@link Reporter} implementation that logs spans.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class LoggingJaegerReporter implements Reporter {
    private final Logger logger;

    public LoggingJaegerReporter(final LogService logService) {
        AppbaseUtil.notNull(logService, "logService");

        this.logger = logService.getLogger(getClass());
    }

    @Override
    public void report(final JaegerSpan span) {
        logger.info("Span: {}", span);
    }

    @Override
    public void close() {
        // no-op
    }
}
