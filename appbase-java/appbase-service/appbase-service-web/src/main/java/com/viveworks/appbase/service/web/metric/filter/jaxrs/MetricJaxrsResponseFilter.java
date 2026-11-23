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

package com.viveworks.appbase.service.web.metric.filter.jaxrs;

import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.metrics.Timer;

import java.lang.reflect.Method;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Provider
public class MetricJaxrsResponseFilter extends AbstractAppbaseJaxrsEntity implements ContainerResponseFilter {
    @Context
    private ResourceInfo resourceInfo;

    @Context
    private ServletContext servletContext;

    @Override
    public void filter(
            final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext)
    {
        final Timer.Context timer = (Timer.Context) requestContext
                .getProperty(AppbaseServiceConstants.METRIC_FILTER_TIMER_CONTEXT_PROPERTY);

        try {
            final int status = responseContext.getStatus();
            if (status > 399 && status < 600) {
                final Method resourceMethod = resourceInfo.getResourceMethod();
                final Class<?> resourceClass = resourceInfo.getResourceClass();

                if (resourceClass != null && resourceMethod != null) {
                    final MetricService metricService = appbaseCore(servletContext).getMetricService();
                    final String name = metricService.name(
                            resourceClass,
                            resourceMethod.getName() + MetricService.METRIC_NAME_SEPARATOR +
                                    status + MetricService.METRIC_NAME_SEPARATOR +
                                    "error");
                    metricService.getMetricRegistry().counter(name).inc();
                }
            }
        } finally {
            if (timer != null) {
                timer.stop();
            }
        }
    }
}
