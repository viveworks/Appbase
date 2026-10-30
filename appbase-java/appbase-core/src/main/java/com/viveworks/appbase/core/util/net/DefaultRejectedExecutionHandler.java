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

package com.viveworks.appbase.core.util.net;

import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import org.eclipse.microprofile.metrics.Counter;

/**
 * Default implementation of {@link java.util.concurrent.RejectedExecutionHandler}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultRejectedExecutionHandler implements RejectedExecutionHandler {
    private final Counter counter;

    public DefaultRejectedExecutionHandler(final MetricService metricService, final String counterName) {
        AppbaseUtil.notNull(metricService, "metricService");

        this.counter = metricService.getMetricRegistry().counter(counterName);
    }

    @Override
    public void rejected(final Runnable task, final SingleThreadEventExecutor executor) {
        counter.inc();
    }
}
