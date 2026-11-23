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
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.filter.jaxrs.AbstractJaxrsRequestFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Timed;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Provider
@Priority(AppbaseServiceConstants.PRIORITY_HIGH)
public class MetricJaxrsRequestFilter extends AbstractJaxrsRequestFilter implements ContainerRequestFilter {
    public MetricJaxrsRequestFilter() {
    }

    public MetricJaxrsRequestFilter(final ResourceInfo resourceInfo) {
        AppbaseUtil.notNull(resourceInfo, "resourceInfo");

        this.resourceInfo = resourceInfo;
    }

    @Override
    protected void filterRequest(final ContainerRequestContext requestContext) throws IOException {
        final Method resourceMethod = resourceInfo.getResourceMethod();
        if (resourceMethod != null && resourceMethod.isAnnotationPresent(Timed.class)) {
            final MetricService metricService = appbaseCoreFromContext().getMetricService();
            final String name = metricService.name(
                    resourceInfo.getResourceClass(),
                    resourceMethod.getName() + MetricService.METRIC_NAME_SEPARATOR + "timer");
            final Timer timer = metricService.getMetricRegistry().timer(name);
            final Timer.Context context = timer.time();
            requestContext.setProperty(AppbaseServiceConstants.METRIC_FILTER_TIMER_CONTEXT_PROPERTY, context);
        }
    }
}
