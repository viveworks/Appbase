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

package com.viveworks.appbase.bootstrap.servlet;

import com.viveworks.appbase.service.user.account.web.filter.servlet.IdpUserTokenCheckServletFilter;
import com.viveworks.appbase.service.user.session.web.filter.servlet.UserSessionServletFilter;
import com.viveworks.appbase.service.web.authorization.filter.servlet.AuthorizationServletFilter;
import com.viveworks.appbase.service.web.cors.CorsServletFilter;
import com.viveworks.appbase.service.web.endpoint.servlet.appbase.JmxServlet;
import com.viveworks.appbase.service.web.exceptionhandler.filter.servlet.ServletExceptionHandlerFilter;
import com.viveworks.appbase.service.web.filter.servlet.sync.AppbaseSyncServletFilter;
import com.viveworks.appbase.service.web.metric.filter.servlet.MetricServletFilter;
import com.viveworks.appbase.service.web.trace.filter.servlet.TraceIdServletFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies the registrar drives registration purely through {@link AppbaseServletDeployment}.
 *
 * @author Navaneeth Mysore G
 */
final class AppbaseServletRegistrarTest {

    /** Captures every addServlet/addFilter call without any container. */
    private static final class RecordingDeployment implements AppbaseServletDeployment {
        final List<Class<? extends Servlet>> servlets = new ArrayList<>();
        final List<Class<? extends Filter>>  filters  = new ArrayList<>();
        @Override public void addServlet(String n, Class<? extends Servlet> s, String... p) { servlets.add(s); }
        @Override public void addFilter(String n, Class<? extends Filter> f, String... p)   { filters.add(f); }
    }

    @Test
    void addCoreServlets_registersNineCoreServlets() {
        RecordingDeployment d = new RecordingDeployment();
        AppbaseServletRegistrar.addCoreServlets(
            d, AppbaseServletRegistrar.emptyExcludes(), AppbaseServletRegistrar.emptyPathOverrides());
        assertEquals(9, d.servlets.size());
    }

    @Test
    void addCoreServlets_honoursExcludes() {
        RecordingDeployment d = new RecordingDeployment();
        var excludes = new HashSet<Class<?>>();
        excludes.add(JmxServlet.class);
        AppbaseServletRegistrar.addCoreServlets(d, excludes, AppbaseServletRegistrar.emptyPathOverrides());
        assertEquals(8, d.servlets.size());
        assertFalse(d.servlets.contains(JmxServlet.class));
    }

    @Test
    void addServiceFilters_registersAllStandardFilters() {
        // Order is security-critical: authorization filter MUST be registered last.
        RecordingDeployment d = new RecordingDeployment();
        AppbaseServletRegistrar.addServiceFilters(d, AppbaseServletRegistrar.emptyExcludes());
        assertEquals(8, d.filters.size());
        List<Class<?>> expected = List.of(
            AppbaseSyncServletFilter.class,
            ServletExceptionHandlerFilter.class,
            CorsServletFilter.class,
            MetricServletFilter.class,
            TraceIdServletFilter.class,
            UserSessionServletFilter.class,
            IdpUserTokenCheckServletFilter.class,
            AuthorizationServletFilter.class
        );
        assertEquals(expected, new ArrayList<>(d.filters));
    }

    @Test
    void addServiceServlets_registersThirtyServiceServlets() {
        RecordingDeployment d = new RecordingDeployment();
        AppbaseServletRegistrar.addServiceServlets(
            d, AppbaseServletRegistrar.emptyExcludes(), AppbaseServletRegistrar.emptyPathOverrides());
        assertEquals(30, d.servlets.size());
    }
}
