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
package com.viveworks.appbase.bootstrap.tomcat.server;

import com.viveworks.appbase.bootstrap.servlet.AppbaseServletDeployment;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import java.util.Objects;

/**
 * Tomcat implementation of {@link AppbaseServletDeployment}, populating a
 * Catalina {@link Context}. Servlets register via {@link Tomcat#addServlet}
 * plus a servlet mapping; filters via {@link FilterDef}/{@link FilterMap}.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
public final class TomcatServletDeployment implements AppbaseServletDeployment {

    private final Context context;

    /**
     * Creates a deployment over the given Tomcat {@link Context}.
     *
     * @param context the Catalina context to populate
     */
    public TomcatServletDeployment(final Context context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Registers a servlet under {@code name} mapped to {@code urlPatterns} on the
     * Tomcat {@link org.apache.catalina.Context}. Each URL pattern is added as a
     * decoded servlet mapping.
     *
     * @param name        unique servlet registration name
     * @param servlet     servlet class to register (by class name)
     * @param urlPatterns one or more URL patterns to map
     */
    @Override
    public void addServlet(final String name, final Class<? extends Servlet> servlet,
                           final String... urlPatterns) {
        Tomcat.addServlet(context, name, servlet.getName());
        for (final String pattern : urlPatterns) {
            context.addServletMappingDecoded(pattern, name);
        }
    }

    /**
     * Registers a filter under {@code name} for {@code urlPatterns} on the Tomcat
     * {@link org.apache.catalina.Context}. Each URL pattern is added as a separate
     * {@link org.apache.tomcat.util.descriptor.web.FilterMap}.
     *
     * @param name        unique filter registration name
     * @param filter      filter class to register (by class name)
     * @param urlPatterns one or more URL patterns to map
     */
    @Override
    public void addFilter(final String name, final Class<? extends Filter> filter,
                          final String... urlPatterns) {
        final FilterDef def = new FilterDef();
        def.setFilterName(name);
        def.setFilterClass(filter.getName());
        context.addFilterDef(def);
        for (final String pattern : urlPatterns) {
            final FilterMap map = new FilterMap();
            map.setFilterName(name);
            map.addURLPattern(pattern);
            context.addFilterMap(map);
        }
    }
}
