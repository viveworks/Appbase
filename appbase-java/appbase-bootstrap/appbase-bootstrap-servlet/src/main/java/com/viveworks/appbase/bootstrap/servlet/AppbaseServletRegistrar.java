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

package com.viveworks.appbase.bootstrap.servlet;

import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.ConfigurationServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.CoreServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.ExceptionServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.JmxServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.LogServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.MediatorServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.MetricServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.ServiceRegistryServlet;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.StatusServlet;
import com.viveworks.appbase.service.web.authorization.filter.servlet.AuthorizationServletFilter;
import com.viveworks.appbase.service.web.exceptionhandler.filter.servlet.ServletExceptionHandlerFilter;
import com.viveworks.appbase.service.web.health.endpoint.servlet.HealthCheckServlet;
import com.viveworks.appbase.service.web.metric.filter.servlet.MetricServletFilter;
import com.viveworks.appbase.service.web.trace.filter.servlet.TraceIdServletFilter;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpConfirmPasswordResetServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpCreateUserWithEmailPasswordServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpEmailPasswordSignInServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpGuestSignInServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpPasswordResetSubmitServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpRefreshAuthenticationTokenServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.AfterIdpSignOutServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.CheckTokenServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.CreateUnauthenticatedSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.DeleteAccountByAppbaseUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.DeleteAccountByIdpUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.DeleteGuestUserServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.DeleteSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.DeleteUnauthenticatedSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ExistsAccountByAppbaseUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ExistsAccountByEmailServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ExistsAccountByIdpUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.IsExpiredSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadAccountByAppbaseUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadAccountByEmailServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadAccountByIdpUserIdServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadGuestUserSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.ReadUnauthenticatedSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.RenewSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.UpdateAccountServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.UpdateGuestUserSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.UpdateSessionServlet;
import com.viveworks.appbase.service.user.account.web.endpoint.servlet.UpdateUnauthenticatedSessionServlet;
import com.viveworks.appbase.service.user.account.web.filter.servlet.IdpUserTokenCheckServletFilter;
import com.viveworks.appbase.service.user.session.web.filter.servlet.UserSessionServletFilter;
import com.viveworks.appbase.service.web.cors.CorsServletFilter;
import com.viveworks.appbase.service.web.filter.servlet.sync.AppbaseSyncServletFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Registers the standard set of appbase-provided servlets and filters onto an
 * {@link AppbaseServletDeployment}. Called by servlet container bootstrap code when the
 * corresponding {@code withAppbaseCoreServlets()} / {@code withAppbaseServiceServlets()} /
 * {@code withAppbaseServiceFilters()} builder methods are called.
 *
 * <p>By default all standard servlets and filters are registered at their default paths.
 * Pass an {@code excludes} set to omit specific servlet or filter types, and a
 * {@code pathOverrides} map to mount specific servlets at custom paths instead of their
 * defaults. Filter path overrides are not supported here — use {@code AppbaseFilterConfig}
 * in the {@link com.viveworks.appbase.core.service.configuration.ConfigurationService} to
 * control which paths each filter observes.
 *
 * @author Navaneeth Mysore G
 */
public final class AppbaseServletRegistrar {

    private AppbaseServletRegistrar() {}

    /**
     * Registers the nine core appbase servlets (configuration, core, exception, JMX,
     * log, mediator, metric, service-registry, status) onto {@code deployment}, skipping any
     * type listed in {@code excludes} and mounting any type listed in {@code pathOverrides}
     * at the given path instead of the default.
     *
     * @param deployment    the servlet deployment to configure
     * @param excludes      servlet types to skip; must not be {@code null}
     * @param pathOverrides per-type path overrides; must not be {@code null}
     */
    public static void addCoreServlets(final AppbaseServletDeployment deployment,
                                       final Set<Class<?>> excludes,
                                       final Map<Class<?>, String> pathOverrides) {
        addServlet(deployment, ConfigurationServlet.class,   AppbaseServiceConstants.DEFAULT_CONFIGURATION_SERVLET_PATH,   excludes, pathOverrides);
        addServlet(deployment, CoreServlet.class,            AppbaseServiceConstants.DEFAULT_CORE_SERVLET_PATH,            excludes, pathOverrides);
        addServlet(deployment, ExceptionServlet.class,       AppbaseServiceConstants.DEFAULT_EXCEPTION_SERVLET_PATH,       excludes, pathOverrides);
        addServlet(deployment, JmxServlet.class,             AppbaseServiceConstants.DEFAULT_JMX_SERVLET_PATH,             excludes, pathOverrides);
        addServlet(deployment, LogServlet.class,             AppbaseServiceConstants.DEFAULT_LOG_SERVLET_PATH,             excludes, pathOverrides);
        addServlet(deployment, MediatorServlet.class,        AppbaseServiceConstants.DEFAULT_MEDIATOR_SERVLET_PATH,        excludes, pathOverrides);
        addServlet(deployment, MetricServlet.class,          AppbaseServiceConstants.DEFAULT_METRIC_SERVLET_PATH,          excludes, pathOverrides);
        addServlet(deployment, ServiceRegistryServlet.class, AppbaseServiceConstants.DEFAULT_SERVICE_REGISTRY_SERVLET_PATH, excludes, pathOverrides);
        addServlet(deployment, StatusServlet.class,          AppbaseServiceConstants.DEFAULT_STATUS_SERVLET_PATH,          excludes, pathOverrides);
    }

    /**
     * Registers the standard appbase service filters (sync, exception handler, CORS,
     * metric, trace-id, user-session, IDP token check, authorization) onto {@code deployment},
     * skipping any type listed in {@code excludes}.
     *
     * <p>Filters always bind to {@code "/*"}; use {@code AppbaseFilterConfig} in the
     * configuration service to control per-path behavior.
     *
     * @param deployment the servlet deployment to configure
     * @param excludes   filter types to skip; must not be {@code null}
     */
    public static void addServiceFilters(final AppbaseServletDeployment deployment,
                                         final Set<Class<?>> excludes) {
        addFilter(deployment, AppbaseSyncServletFilter.class,       excludes);
        addFilter(deployment, ServletExceptionHandlerFilter.class,  excludes);
        addFilter(deployment, CorsServletFilter.class,              excludes);
        addFilter(deployment, MetricServletFilter.class,            excludes);
        addFilter(deployment, TraceIdServletFilter.class,           excludes);
        addFilter(deployment, UserSessionServletFilter.class,       excludes);
        addFilter(deployment, IdpUserTokenCheckServletFilter.class, excludes);
        addFilter(deployment, AuthorizationServletFilter.class,     excludes);
    }

    /**
     * Registers the standard appbase service servlets (health check and all user
     * account/session servlets) onto {@code deployment}, skipping any type listed in
     * {@code excludes} and mounting any type listed in {@code pathOverrides} at a
     * custom path instead of its default.
     *
     * @param deployment    the servlet deployment to configure
     * @param excludes      servlet types to skip; must not be {@code null}
     * @param pathOverrides per-type path overrides; must not be {@code null}
     */
    public static void addServiceServlets(final AppbaseServletDeployment deployment,
                                          final Set<Class<?>> excludes,
                                          final Map<Class<?>, String> pathOverrides) {
        addServlet(deployment, HealthCheckServlet.class,                        HealthCheckServlet.DEFAULT_PATH,                        excludes, pathOverrides);
        addServlet(deployment, AfterIdpCreateUserWithEmailPasswordServlet.class, AfterIdpCreateUserWithEmailPasswordServlet.DEFAULT_PATH, excludes, pathOverrides);
        addServlet(deployment, AfterIdpEmailPasswordSignInServlet.class,         AfterIdpEmailPasswordSignInServlet.DEFAULT_PATH,         excludes, pathOverrides);
        addServlet(deployment, AfterIdpGuestSignInServlet.class,                 AfterIdpGuestSignInServlet.DEFAULT_PATH,                 excludes, pathOverrides);
        addServlet(deployment, AfterIdpPasswordResetSubmitServlet.class,         AfterIdpPasswordResetSubmitServlet.DEFAULT_PATH,         excludes, pathOverrides);
        addServlet(deployment, AfterIdpConfirmPasswordResetServlet.class,        AfterIdpConfirmPasswordResetServlet.DEFAULT_PATH,        excludes, pathOverrides);
        addServlet(deployment, AfterIdpRefreshAuthenticationTokenServlet.class,  AfterIdpRefreshAuthenticationTokenServlet.DEFAULT_PATH,  excludes, pathOverrides);
        addServlet(deployment, AfterIdpSignOutServlet.class,                     AfterIdpSignOutServlet.DEFAULT_PATH,                     excludes, pathOverrides);
        addServlet(deployment, CheckTokenServlet.class,                          CheckTokenServlet.DEFAULT_PATH,                          excludes, pathOverrides);
        addServlet(deployment, CreateUnauthenticatedSessionServlet.class,        CreateUnauthenticatedSessionServlet.DEFAULT_PATH,        excludes, pathOverrides);
        addServlet(deployment, DeleteAccountByAppbaseUserIdServlet.class,        DeleteAccountByAppbaseUserIdServlet.DEFAULT_PATH,        excludes, pathOverrides);
        addServlet(deployment, DeleteAccountByIdpUserIdServlet.class,            DeleteAccountByIdpUserIdServlet.DEFAULT_PATH,            excludes, pathOverrides);
        addServlet(deployment, DeleteGuestUserServlet.class,                     DeleteGuestUserServlet.DEFAULT_PATH,                     excludes, pathOverrides);
        addServlet(deployment, DeleteSessionServlet.class,                       DeleteSessionServlet.DEFAULT_PATH,                       excludes, pathOverrides);
        addServlet(deployment, DeleteUnauthenticatedSessionServlet.class,        DeleteUnauthenticatedSessionServlet.DEFAULT_PATH,        excludes, pathOverrides);
        addServlet(deployment, ExistsAccountByAppbaseUserIdServlet.class,        ExistsAccountByAppbaseUserIdServlet.DEFAULT_PATH,        excludes, pathOverrides);
        addServlet(deployment, ExistsAccountByEmailServlet.class,                ExistsAccountByEmailServlet.DEFAULT_PATH,                excludes, pathOverrides);
        addServlet(deployment, ExistsAccountByIdpUserIdServlet.class,            ExistsAccountByIdpUserIdServlet.DEFAULT_PATH,            excludes, pathOverrides);
        addServlet(deployment, IsExpiredSessionServlet.class,                    IsExpiredSessionServlet.DEFAULT_PATH,                    excludes, pathOverrides);
        addServlet(deployment, ReadAccountByAppbaseUserIdServlet.class,          ReadAccountByAppbaseUserIdServlet.DEFAULT_PATH,          excludes, pathOverrides);
        addServlet(deployment, ReadAccountByEmailServlet.class,                  ReadAccountByEmailServlet.DEFAULT_PATH,                  excludes, pathOverrides);
        addServlet(deployment, ReadAccountByIdpUserIdServlet.class,              ReadAccountByIdpUserIdServlet.DEFAULT_PATH,              excludes, pathOverrides);
        addServlet(deployment, ReadGuestUserSessionServlet.class,                ReadGuestUserSessionServlet.DEFAULT_PATH,                excludes, pathOverrides);
        addServlet(deployment, ReadSessionServlet.class,                         ReadSessionServlet.DEFAULT_PATH,                         excludes, pathOverrides);
        addServlet(deployment, ReadUnauthenticatedSessionServlet.class,          ReadUnauthenticatedSessionServlet.DEFAULT_PATH,          excludes, pathOverrides);
        addServlet(deployment, RenewSessionServlet.class,                        RenewSessionServlet.DEFAULT_PATH,                        excludes, pathOverrides);
        addServlet(deployment, UpdateAccountServlet.class,                       UpdateAccountServlet.DEFAULT_PATH,                       excludes, pathOverrides);
        addServlet(deployment, UpdateGuestUserSessionServlet.class,              UpdateGuestUserSessionServlet.DEFAULT_PATH,              excludes, pathOverrides);
        addServlet(deployment, UpdateSessionServlet.class,                       UpdateSessionServlet.DEFAULT_PATH,                       excludes, pathOverrides);
        addServlet(deployment, UpdateUnauthenticatedSessionServlet.class,        UpdateUnauthenticatedSessionServlet.DEFAULT_PATH,        excludes, pathOverrides);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void addServlet(final AppbaseServletDeployment deployment,
                                   final Class<? extends Servlet> servletClass,
                                   final String defaultPath,
                                   final Set<Class<?>> excludes,
                                   final Map<Class<?>, String> pathOverrides) {
        if (!excludes.contains(servletClass)) {
            deployment.addServlet(servletClass.getSimpleName(), servletClass,
                                  pathOverrides.getOrDefault(servletClass, defaultPath));
        }
    }

    private static void addFilter(final AppbaseServletDeployment deployment,
                                  final Class<? extends Filter> filterClass,
                                  final Set<Class<?>> excludes) {
        if (!excludes.contains(filterClass)) {
            deployment.addFilter(filterClass.getSimpleName(), filterClass, "/*");
        }
    }

    /**
     * Returns an empty (no exclusions, no path overrides) customization constant.
     * Convenience for callers that want all defaults.
     *
     * @return an immutable empty set
     */
    public static Set<Class<?>> emptyExcludes() {
        return Collections.emptySet();
    }

    /**
     * Returns an empty path-overrides map constant.
     *
     * @return an immutable empty map
     */
    public static Map<Class<?>, String> emptyPathOverrides() {
        return Collections.emptyMap();
    }
}
