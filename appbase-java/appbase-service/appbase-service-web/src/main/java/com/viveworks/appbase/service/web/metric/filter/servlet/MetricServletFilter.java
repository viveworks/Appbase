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

package com.viveworks.appbase.service.web.metric.filter.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.service.web.filter.servlet.AbstractAppbaseServletFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Timer;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MetricServletFilter extends AbstractAppbaseServletFilter {
    private static final long serialVersionUID = 6017292114927881539L;

    public MetricServletFilter() {
    }

    @Override
    protected void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain,
            final AppbaseCore appbaseCore) throws IOException, ServletException
    {
        final MetricService metricService = appbaseCore.getMetricService();
        final String timerName = metricService.name(
                request.getRequestURI(),
                request.getMethod().toUpperCase() + MetricService.METRIC_NAME_SEPARATOR + "timer");
        final Timer.Context timer = metricService.getMetricRegistry().timer(timerName).time();
        try {
            chain.doFilter(request, response);
        } finally {
            timer.stop();
        }
    }
}
