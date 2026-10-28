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
package com.viveworks.appbase.bootstrap.undertow.server;

import com.viveworks.appbase.bootstrap.servlet.AppbaseServletDeployment;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;

import java.util.Objects;

/**
 * Undertow implementation of {@link AppbaseServletDeployment}, populating a
 * {@link DeploymentInfo}. Servlets use {@link Servlets#servlet}; filters use
 * {@link Servlets#filter} plus a URL mapping for REQUEST dispatch.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
public final class UndertowServletDeployment implements AppbaseServletDeployment {

    private final DeploymentInfo deploymentInfo;

    /**
     * Creates a deployment over the given Undertow {@link DeploymentInfo}.
     *
     * @param deploymentInfo the deployment descriptor to populate
     */
    public UndertowServletDeployment(final DeploymentInfo deploymentInfo) {
        this.deploymentInfo = Objects.requireNonNull(deploymentInfo, "deploymentInfo");
    }

    @Override
    public void addServlet(final String name, final Class<? extends Servlet> servlet,
                           final String... urlPatterns) {
        deploymentInfo.addServlet(Servlets.servlet(name, servlet).addMappings(urlPatterns));
    }

    @Override
    public void addFilter(final String name, final Class<? extends Filter> filter,
                          final String... urlPatterns) {
        deploymentInfo.addFilter(Servlets.filter(name, filter));
        for (final String pattern : urlPatterns) {
            deploymentInfo.addFilterUrlMapping(name, pattern, DispatcherType.REQUEST);
        }
    }
}
