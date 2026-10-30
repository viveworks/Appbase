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

package com.viveworks.appbase.service.web.reactive;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilter;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterChain;
import com.viveworks.appbase.service.exchange.AppbaseServiceFilterOrder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import reactor.core.publisher.Mono;

/**
 * Records a per-route timer on the same {@code MetricRegistry} the servlet {@code
 * MetricServletFilter} uses, so both faces report into one place. The timer name is built with
 * {@code MetricService.name(path, METHOD.timer)}, exactly the naming scheme {@code
 * MetricServletFilter} uses ({@code metricService.name(request.getRequestURI(),
 * request.getMethod().toUpperCase() + SEPARATOR + "timer")}) -- NOT an ad-hoc {@code "web." +
 * method + "." + path} scheme -- so route timers from both faces land under the same metric name.
 *
 * <p>Unlike the servlet filter, this filter additionally increments a {@code
 * <timerName>.error} counter on failure. The servlet filter has no such counter; this is a
 * reactive-only addition, not a disagreement, so it is kept.
 *
 * <p>The timer is started on subscription and stopped on ANY terminal signal -- complete, error, or
 * cancel -- via {@code doFinally}. A cancelled request that only stopped the timer on completion
 * would leak the timer context.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class MetricWebFilter implements AppbaseServiceFilter {

    private static final String TIMER_SUFFIX = "timer";

    private static final String ERROR_SUFFIX = "error";

    private final AppbaseCore appbaseCore;

    /**
     * @param appbaseCore Appbase core, providing the metric service
     */
    public MetricWebFilter(final AppbaseCore appbaseCore) {
        this.appbaseCore = AppbaseUtil.notNull(appbaseCore, "appbaseCore");
    }

    @Override
    public Mono<Void> filter(final AppbaseServiceExchange exchange, final AppbaseServiceFilterChain chain) {
        return Mono.defer(() -> {
            final MetricService metricService = appbaseCore.getMetricService();
            final MetricRegistry metricRegistry = metricService.getMetricRegistry();
            final String timerName = metricService.name(
                    exchange.path(),
                    exchange.method().toUpperCase() + MetricService.METRIC_NAME_SEPARATOR + TIMER_SUFFIX);
            final Timer.Context timerContext = metricRegistry.timer(timerName).time();

            return chain.next(exchange)
                    .doOnError(throwable -> metricRegistry
                            .counter(timerName + MetricService.METRIC_NAME_SEPARATOR + ERROR_SUFFIX)
                            .inc())
                    .doFinally(signalType -> timerContext.stop());
        });
    }

    @Override
    public int order() {
        return AppbaseServiceFilterOrder.METRIC;
    }
}
