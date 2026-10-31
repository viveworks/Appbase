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

package com.viveworks.appbase.stacktest;

import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.ReadSupportMessagesServlet;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.SendSupportMessageServlet;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.SupportMessageByIdServlet;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.UpdateMessageServlet;
import com.viveworks.appbase.service.supportmessage.endpoint.servlet.UpdateStatusServlet;
import com.viveworks.appbase.service.web.authorization.filter.jaxrs.AuthorizationJaxrsRequestFilter;
import com.viveworks.appbase.service.web.authorization.filter.servlet.AuthorizationServletFilter;
import com.viveworks.appbase.service.user.account.web.filter.IdpUserTokenCheckFilterConfiguration;
import com.viveworks.appbase.service.user.account.web.filter.jaxrs.IdpUserTokenCheckJaxrsRequestFilter;
import com.viveworks.appbase.service.user.account.web.filter.servlet.IdpUserTokenCheckServletFilter;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionExpiredExceptionConfiguration;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionFilterConfiguration;
import com.viveworks.appbase.service.user.session.web.filter.UserSessionNotFoundExceptionConfiguration;
import com.viveworks.appbase.service.user.session.web.filter.jaxrs.UserSessionJaxrsRequestFilter;
import com.viveworks.appbase.service.user.session.web.filter.jaxrs.UserSessionJaxrsResponseFilter;
import com.viveworks.appbase.service.user.session.web.filter.servlet.UserSessionServletFilter;
import com.viveworks.appbase.service.web.cors.CorsFilterConfiguration;
import com.viveworks.appbase.service.web.filter.AppbaseFilterConfig;
import com.viveworks.appbase.stacktest.dummytypes.DummyAppbaseServiceConfiguration1;
import com.viveworks.appbase.stacktest.http.servlet.CustomerOrdersServlet;
import com.viveworks.appbase.stacktest.http.servlet.CustomerPhotoServlet;
import com.viveworks.appbase.stacktest.http.servlet.CustomerSearchServlet;
import com.viveworks.appbase.stacktest.http.servlet.CustomerServlet;
import com.viveworks.appbase.stacktest.http.servlet.CustomerStatisticsServlet;
import com.viveworks.appbase.stacktest.http.servlet.OrderSearchServlet;
import com.viveworks.appbase.stacktest.http.servlet.OrderServlet;
import com.viveworks.appbase.stacktest.http.servlet.ProductSearchServlet;
import com.viveworks.appbase.stacktest.http.servlet.ProductServlet;
import com.viveworks.appbase.stacktest.http.servlet.SearchServlet;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import com.viveworks.appbase.stacktest.test.service.authorization.testimpl.AuthorizationFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.exceptionhandler.testimpl.ExceptionHandlerTestImpl;
import com.viveworks.appbase.stacktest.test.service.metric.testimpl.MetricFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.origin.testimpl.CorsFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.syncfilter.testimpl.AppbaseSyncServletFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.traceid.testimpl.TraceIdFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.user.session.testImpl.IdpUserTokenCheckFilterTestImpl;
import com.viveworks.appbase.stacktest.test.service.user.session.testImpl.UserSessionFilterTestImpl;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Map;
import java.util.Set;

/**
 * Configures the Jetty {@link ServletContextHandler} for the stack test server.
 *
 * <p>All servlet and filter registration previously done inside {@code StackTestServer}
 * is centralised here so that {@code StackTestMain} can use the bootstrap
 * {@link com.viveworks.appbase.bootstrap.jetty.server.JettyServletContainer} builder without
 * coupling to the deleted {@code StackTestServer}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class StackTestServletConfig {

    private StackTestServletConfig() {}

    /**
     * Wires the servlet context: sets attributes, configures filter/session entities,
     * injects test fixtures, and registers all servlets and filters.
     *
     * @param ctx              the servlet context to configure
     * @param stackTestServices domain service container
     * @param appbaseServices  bootstrap application services container
     * @throws IOException if the multipart upload directory cannot be created
     */
    public static void configure(
            final ServletContextHandler ctx,
            final StackTestServices stackTestServices,
            final AppbaseServices appbaseServices) throws IOException
    {
        AppbaseUtil.notNull(ctx, "ctx");
        AppbaseUtil.notNull(stackTestServices, "stackTestServices");
        AppbaseUtil.notNull(appbaseServices, "appbaseServices");

        final AppbaseCore appbaseCore = appbaseServices.getCoreServices().appbaseCore();

        ctx.setAttribute(StackTestServices.class.getName(), stackTestServices);
        ctx.setAttribute(AppbaseServiceConstants.APPBASE_CORE_ATTRIBUTE, appbaseCore);

        configureEntities(appbaseCore);
        addFixtures(appbaseCore);

        addStackTestServlets(ctx);
        addAppbaseStacktestServlets(ctx);
    }

    /**
     * Puts test fixtures into the configuration service.
     *
     * @param appbaseCore the core instance whose configuration service receives the fixtures
     */
    public static void addFixtures(final AppbaseCore appbaseCore) {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        configurationService.putString("testPropertyKey1", "testPropertyValue1");
        configurationService.putTypedConfiguration(
                DummyAppbaseServiceConfiguration1.class,
                new DummyAppbaseServiceConfiguration1("localhost", 8080));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void configureEntities(final AppbaseCore appbaseCore) {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();

        configurationService.putTypedConfiguration(
                CorsFilterConfiguration.class,
                CorsFilterConfiguration.builder().build());

        final SetOfStringsValue sessionEnabledPaths = SetOfStringsValue.from(Set.of(
                StackTestConstants.USER_SESSION_FILTER_TEST_RESOURCE_PATH,
                StackTestConstants.USER_SESSION_FILTER_TEST_SERVLET_PATH,
                StackTestConstants.IDP_USER_TOKEN_CHECK_FILTER_TEST_RESOURCE_PATH,
                StackTestConstants.IDP_USER_TOKEN_CHECK_FILTER_TEST_SERVLET_PATH,
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_PATH,
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_PATH,
                StackTestConstants.AUTHORIZATION_FILTER_UNAUTHORIZED_RESOURCE_PATH,
                StackTestConstants.AUTHORIZATION_FILTER_UNAUTHORIZED_SERVLET_PATH,
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_UNAUTHORIZED_METHOD_PATH,
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_UNAUTHORIZED_METHOD_PATH,
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_METHOD_PATH,
                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_METHOD_PATH));

        configurationService.putTypedConfiguration(
                AppbaseFilterConfig.class,
                AppbaseFilterConfig.builder()
                        .allowedPaths(Set.of(
                                TraceIdFilterTestImpl.TraceIdTestResource.class.getName(),
                                StackTestConstants.TRACE_ID_TEST_SERVLET_PATH,
                                StackTestConstants.TRACE_ID_TEST_RESOURCE_PATH,
                                MetricFilterTestImpl.MetricTestResource1.class.getName(),
                                MetricFilterTestImpl.MetricTestResource2.class.getName(),
                                StackTestConstants.METRIC_TEST_RESOURCE1_PATH,
                                StackTestConstants.METRIC_TEST_RESOURCE2_PATH,
                                StackTestConstants.METRIC_TEST_SERVLET1_PATH,
                                ExceptionHandlerTestImpl.ExceptionHandlerTestResource.class.getName(),
                                StackTestConstants.EXCEPTION_HANDLER_TEST_SERVLET_PATH,
                                StackTestConstants.EXCEPTION_HANDLER_TEST_RESOURCE_PATH,
                                CorsFilterTestImpl.CorsTestResource.class.getName(),
                                StackTestConstants.CORS_TEST_SERVLET_PATH,
                                StackTestConstants.CORS_TEST_RESOURCE_PATH,
                                StackTestConstants.APPBASE_SYNC_SERVLET_FILTER_TEST_SERVLET_PATH,
                                IdpUserTokenCheckFilterTestImpl.IdpUserTokenCheckTestResource.class.getName(),
                                StackTestConstants.IDP_USER_TOKEN_CHECK_FILTER_TEST_RESOURCE_PATH,
                                StackTestConstants.IDP_USER_TOKEN_CHECK_FILTER_TEST_SERVLET_PATH,
                                AuthorizationFilterTestImpl.AuthorizedResourceTest.class.getName(),
                                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_PATH,
                                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_PATH,
                                AuthorizationFilterTestImpl.UnauthorizedResourceTest.class.getName(),
                                StackTestConstants.AUTHORIZATION_FILTER_UNAUTHORIZED_RESOURCE_PATH,
                                StackTestConstants.AUTHORIZATION_FILTER_UNAUTHORIZED_SERVLET_PATH,
                                AuthorizationFilterTestImpl.AuthorizedResourceUnauthorizedMethodTest.class.getName(),
                                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_UNAUTHORIZED_METHOD_PATH,
                                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_UNAUTHORIZED_METHOD_PATH,
                                AuthorizationFilterTestImpl.AuthorizedResourceMethodTest.class.getName(),
                                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_RESOURCE_METHOD_PATH,
                                StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_METHOD_PATH))
                        .allowedPathsByFilterTypeName(Map.of(
                                UserSessionJaxrsRequestFilter.class.getName(), sessionEnabledPaths,
                                UserSessionJaxrsResponseFilter.class.getName(), sessionEnabledPaths,
                                UserSessionServletFilter.class.getName(), sessionEnabledPaths,
                                IdpUserTokenCheckJaxrsRequestFilter.class.getName(), sessionEnabledPaths,
                                IdpUserTokenCheckServletFilter.class.getName(), sessionEnabledPaths,
                                AuthorizationJaxrsRequestFilter.class.getName(), sessionEnabledPaths,
                                AuthorizationServletFilter.class.getName(), sessionEnabledPaths))
                        .build());

        configurationService.putTypedConfiguration(
                UserSessionNotFoundExceptionConfiguration.class,
                UserSessionNotFoundExceptionConfiguration.builder().respondBadRequest(true).build());

        configurationService.putTypedConfiguration(
                UserSessionFilterConfiguration.class,
                UserSessionFilterConfiguration.builder()
                        .createSessionIfAbsentByPathMap(Map.of(AppbaseServiceConstants.WILDCARD, true))
                        .build());

        configurationService.putTypedConfiguration(
                UserSessionExpiredExceptionConfiguration.class,
                UserSessionExpiredExceptionConfiguration.builder().renewSession(true).build());

        configurationService.putTypedConfiguration(
                IdpUserTokenCheckFilterConfiguration.class,
                IdpUserTokenCheckFilterConfiguration.builder().respondForbidden(true).build());

        configurationService.putTypedConfiguration(
                AuthorizationServiceExceptionConfiguration.class,
                AuthorizationServiceExceptionConfiguration.builder().respondForbidden(true).build());
    }

    private static void addStackTestServlets(final ServletContextHandler ctx) throws IOException {
        addServlet(ctx, CustomerServlet.class, StackTestConstants.STACK_TEST_CUSTOMER_PATH, false, null);
        addServlet(ctx, CustomerSearchServlet.class, StackTestConstants.STACK_TEST_CUSTOMER_SEARCH_PATH, false, null);
        addServlet(ctx, CustomerOrdersServlet.class, StackTestConstants.STACK_TEST_CUSTOMER_ORDERS_PATH, false, null);

        final Path uploadDir = Files.createTempDirectory(CustomerPhotoServlet.class.getSimpleName());
        uploadDir.toFile().deleteOnExit();
        addMultipartServlet(ctx, CustomerPhotoServlet.class, StackTestConstants.STACK_TEST_CUSTOMER_PHOTO_PATH, false,
                new MultipartConfigElement(
                        uploadDir.toFile().getAbsolutePath(),
                        1024L * 1024 * 20,
                        1024L * 1024 * 100,
                        1024 * 1024 * 10));

        addServlet(ctx, CustomerStatisticsServlet.class, StackTestConstants.STACK_TEST_CUSTOMER_STATISTICS_PATH, false, null);
        addServlet(ctx, OrderServlet.class, StackTestConstants.STACK_TEST_ORDER_PATH, false, null);
        addServlet(ctx, OrderSearchServlet.class, StackTestConstants.STACK_TEST_ORDER_SEARCH_PATH, false, null);
        addServlet(ctx, ProductServlet.class, StackTestConstants.STACK_TEST_PRODUCT_PATH, false, null);
        addServlet(ctx, ProductSearchServlet.class, StackTestConstants.STACK_TEST_PRODUCT_SEARCH_PATH, false, null);
        addServlet(ctx, SearchServlet.class, StackTestConstants.STACK_TEST_SEARCH_PATH, false, null);
    }

    private static void addAppbaseStacktestServlets(final ServletContextHandler ctx) {
        addServlet(ctx, ReadSupportMessagesServlet.class, ReadSupportMessagesServlet.DEFAULT_PATH, false, null);
        addServlet(ctx, SendSupportMessageServlet.class, SendSupportMessageServlet.DEFAULT_PATH, false, null);
        addServlet(ctx, SupportMessageByIdServlet.class, SupportMessageByIdServlet.DEFAULT_PATH, false, null);
        addServlet(ctx, UpdateMessageServlet.class, UpdateMessageServlet.DEFAULT_PATH, false, null);
        addServlet(ctx, UpdateStatusServlet.class, UpdateStatusServlet.DEFAULT_PATH, false, null);
        addServlet(ctx, MetricFilterTestImpl.MetricTestServlet1.class, StackTestConstants.METRIC_TEST_SERVLET1_PATH, false, null);
        addServlet(ctx, TraceIdFilterTestImpl.TraceIdTestServlet.class, StackTestConstants.TRACE_ID_TEST_SERVLET_PATH, false, null);
        addServlet(ctx, CorsFilterTestImpl.CorsTestServlet.class, StackTestConstants.CORS_TEST_SERVLET_PATH, false, null);
        addServlet(ctx, ExceptionHandlerTestImpl.ExceptionHandlerTestServlet.class, StackTestConstants.EXCEPTION_HANDLER_TEST_SERVLET_PATH, false, null);
        addServlet(ctx, AppbaseSyncServletFilterTestImpl.AppbaseSyncFilterTestTestServlet.class, StackTestConstants.APPBASE_SYNC_SERVLET_FILTER_TEST_SERVLET_PATH, false, null);
        addServlet(ctx, UserSessionFilterTestImpl.UserSessionTestServlet.class, StackTestConstants.USER_SESSION_FILTER_TEST_SERVLET_PATH, false, null);
        addServlet(ctx, IdpUserTokenCheckFilterTestImpl.IdpUserTokenCheckTestServlet.class, StackTestConstants.IDP_USER_TOKEN_CHECK_FILTER_TEST_SERVLET_PATH, false, null);
        addServlet(ctx, AuthorizationFilterTestImpl.AuthorizedServletTest.class, StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_PATH, false, null);
        addServlet(ctx, AuthorizationFilterTestImpl.UnauthorizedServletTest.class, StackTestConstants.AUTHORIZATION_FILTER_UNAUTHORIZED_SERVLET_PATH, false, null);
        addServlet(ctx, AuthorizationFilterTestImpl.AuthorizedServletUnauthorizedMethodTest.class, StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_UNAUTHORIZED_METHOD_PATH, false, null);
        addServlet(ctx, AuthorizationFilterTestImpl.AuthorizedServletMethodTest.class, StackTestConstants.AUTHORIZATION_FILTER_AUTHORIZED_SERVLET_METHOD_PATH, false, null);
    }

    private static void addServlet(
            final ServletContextHandler ctx,
            final Class<? extends Servlet> servletType,
            final String pathSpec,
            final boolean asyncSupported,
            @Nullable final Map<String, String> initParameters)
    {
        final ServletHolder holder = ctx.addServlet(servletType, pathSpec);
        if (initParameters != null) {
            holder.setInitParameters(initParameters);
        }
        holder.setAsyncSupported(asyncSupported);
    }

    private static void addMultipartServlet(
            final ServletContextHandler ctx,
            final Class<? extends Servlet> servletType,
            final String pathSpec,
            final boolean asyncSupported,
            final MultipartConfigElement multipartConfig)
    {
        final ServletHolder holder = ctx.addServlet(servletType, pathSpec);
        holder.setAsyncSupported(asyncSupported);
        holder.getRegistration().setMultipartConfig(multipartConfig);
    }

}
