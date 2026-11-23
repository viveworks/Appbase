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

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;

/**
 * Container-neutral servlet/filter registration. Each embedded container
 * (Jetty {@code ServletContextHandler}, Undertow {@code DeploymentInfo},
 * Tomcat {@code Context}) provides an implementation that maps these calls
 * onto its native programmatic API.
 *
 * <p>All filters registered through {@link #addFilter} bind for the standard
 * request dispatch; per-path filter behaviour is controlled by
 * {@code AppbaseFilterConfig} in the configuration service, not here.
 *
 * @author Navaneeth Mysore G
 */
public interface AppbaseServletDeployment {

    /**
     * Registers a servlet under {@code name} mapped to {@code urlPatterns}.
     *
     * @param name        unique servlet registration name
     * @param servlet     servlet class to instantiate
     * @param urlPatterns one or more URL patterns to map
     */
    void addServlet(String name, Class<? extends Servlet> servlet, String... urlPatterns);

    /**
     * Registers a filter under {@code name} mapped to {@code urlPatterns} for
     * the standard request dispatch.
     *
     * @param name        unique filter registration name
     * @param filter      filter class to instantiate
     * @param urlPatterns one or more URL patterns to map
     */
    void addFilter(String name, Class<? extends Filter> filter, String... urlPatterns);
}
