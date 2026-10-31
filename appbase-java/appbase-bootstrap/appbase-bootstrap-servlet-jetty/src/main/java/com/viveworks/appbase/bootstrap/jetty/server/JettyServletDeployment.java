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
package com.viveworks.appbase.bootstrap.jetty.server;

import com.viveworks.appbase.bootstrap.servlet.AppbaseServletDeployment;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Jetty implementation of {@link AppbaseServletDeployment}, wrapping a
 * {@link ServletContextHandler}. Servlets are added via {@link ServletHolder};
 * filters via {@link FilterHolder} bound for REQUEST + FORWARD dispatch (the
 * dispatch profile the appbase filters require).
 *
 * @author Navaneeth Mysore G
 */
public final class JettyServletDeployment implements AppbaseServletDeployment {

    private static final EnumSet<DispatcherType> REQUEST_FORWARD =
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD);

    private final ServletContextHandler context;

    /**
     * Creates a deployment over the given Jetty servlet context.
     *
     * @param context the Jetty servlet context handler to populate
     */
    public JettyServletDeployment(final ServletContextHandler context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Adds a servlet to the Jetty {@link ServletContextHandler} under the given URL patterns.
     *
     * @param name        the servlet name
     * @param servlet     the servlet class
     * @param urlPatterns one or more URL patterns to map the servlet to
     */
    @Override
    public void addServlet(final String name, final Class<? extends Servlet> servlet,
                           final String... urlPatterns) {
        for (final String pattern : urlPatterns) {
            context.addServlet(new ServletHolder(name, servlet), pattern);
        }
    }

    /**
     * Adds a filter to the Jetty {@link ServletContextHandler} under the given URL patterns,
     * bound for both REQUEST and FORWARD dispatch.
     *
     * @param name        the filter name
     * @param filter      the filter class
     * @param urlPatterns one or more URL patterns to map the filter to
     */
    @Override
    public void addFilter(final String name, final Class<? extends Filter> filter,
                          final String... urlPatterns) {
        final FilterHolder holder = new FilterHolder(filter);
        holder.setName(name);
        for (final String pattern : urlPatterns) {
            context.addFilter(holder, pattern, REQUEST_FORWARD);
        }
    }
}
