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

package com.viveworks.appbase.service.web.endpoint.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceException;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;
import org.slf4j.Logger;

/**
 * Get {@link AppbaseCore} from context. This will need a {@link ContextResolver} implementation like:
 *
 * <pre>{@code
 *     @Provider
 *     public class JaxRsAppbaseCoreProvider implements ContextResolver<AppbaseCore> {
 *          @Override
 *          public AppbaseCore getContext(final Class<?> type) {
 *              return appbaseCoreHere;
 *          }
 *     }
 * }</pre>
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public abstract class AbstractAppbaseJaxrsEntity {
    @Context
    private Providers providers;

    protected AbstractAppbaseJaxrsEntity() {
    }

    protected AbstractAppbaseJaxrsEntity(final Providers providers) {
        this.providers = providers;
    }

    protected AppbaseCore appbaseCore(final ServletContext servletContext) {
        if (providers != null) {
            final AppbaseCore appbaseCore = appbaseCoreFromContext();
            if (appbaseCore != null) {
                return appbaseCore;
            }
        }

        if (servletContext != null) {
            final AppbaseCore appbaseCore = AppbaseServiceUtil.appbaseCore(servletContext);
            if (appbaseCore != null) {
                return appbaseCore;
            }
        }

        throw new InternalServerErrorException("AppbaseCore not initialized");
    }

    protected Logger logger(final ServletContext servletContext) {
        return appbaseCore(servletContext).getLogService().getLogger(getClass());
    }

    protected AppbaseCore appbaseCoreFromContext() {
        final ContextResolver<AppbaseCore> contextResolver =
                providers.getContextResolver(AppbaseCore.class, MediaType.WILDCARD_TYPE);
        final AppbaseCore appbaseCore = contextResolver.getContext(AppbaseCore.class);

        if (appbaseCore == null) {
            throw new InternalServerErrorException("AppbaseCore not initialized");
        }

        return appbaseCore;
    }

    protected Response notFoundResponse() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    protected Response noContentResponse() {
        return Response.noContent().build();
    }

    protected Response okResponse(final Object entity) {
        return Response.ok(entity).build();
    }

    protected Response badRequest() {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    protected void handleAppbaseServiceException(
            final ServletContext servletContext,
            final AppbaseServiceException appbaseServiceException)
    {
        AppbaseUtil.notNull(appbaseServiceException, "appbaseServiceException");

        logger(servletContext).error("Handling exception: ", appbaseServiceException);

        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final MetricService metricService = appbaseCore.getMetricService();
        final String name = metricService.name(getClass(), "appbaseServiceException", "counter");
        metricService.getMetricRegistry().counter(name).inc();

        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }

    protected void handleUserAccountServiceException(
            final ServletContext servletContext,
            final AppbaseServiceException userAccountServiceException)
    {
        AppbaseUtil.notNull(userAccountServiceException, "userAccountServiceException");

        logger(servletContext).error("Handling exception: ", userAccountServiceException);

        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final MetricService metricService = appbaseCore.getMetricService();
        final String name = metricService.name(getClass(), "userAccountServiceException", "counter");
        metricService.getMetricRegistry().counter(name).inc();

        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }

    protected void handleAuthorizationServiceException(
            final ServletContext servletContext,
            final AuthorizationServiceException authorizationServiceException)
            throws WebApplicationException
    {
        AppbaseUtil.notNull(authorizationServiceException, "authorizationServiceException");

        logger(servletContext).error("Handling exception: ", authorizationServiceException);

        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final MetricService metricService = appbaseCore.getMetricService();
        final String name = metricService.name(getClass(), "authorizationServiceException", "counter");
        metricService.getMetricRegistry().counter(name).inc();

        throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    protected void handleAuthenticationServiceException(
            final ServletContext servletContext,
            final AppbaseServiceException authenticationServiceException)
            throws WebApplicationException
    {
        AppbaseUtil.notNull(authenticationServiceException, "authenticationServiceException");

        logger(servletContext).error("Handling exception: ", authenticationServiceException);

        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final MetricService metricService = appbaseCore.getMetricService();
        final String name = metricService.name(getClass(), "authenticationServiceException", "counter");
        metricService.getMetricRegistry().counter(name).inc();

        throw new WebApplicationException(
                AppbaseServiceConstants.USER_AUTHENTICATION_TOKEN, Response.Status.UNAUTHORIZED);
    }
}
