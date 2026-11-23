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

import com.viveworks.appbase.service.web.health.endpoint.servlet.HealthCheckServlet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TomcatServletDeployment}.
 *
 * @author Navaneeth Mysore G
 * @author Claude Sonnet 4.6
 */
final class TomcatServletDeploymentTest {

    @Test
    void addServlet_registersServletInContext(@TempDir final File baseDir) {
        final Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(baseDir.getAbsolutePath());
        final Context ctx = tomcat.addContext("", baseDir.getAbsolutePath());
        final TomcatServletDeployment deployment = new TomcatServletDeployment(ctx);

        deployment.addServlet("health", HealthCheckServlet.class, HealthCheckServlet.DEFAULT_PATH);

        assertNotNull(ctx.findChild("health"));
    }

    @Test
    void addFilter_registersFilterInContext(@TempDir final File baseDir) {
        final Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(baseDir.getAbsolutePath());
        final Context ctx = tomcat.addContext("", baseDir.getAbsolutePath());
        final TomcatServletDeployment deployment = new TomcatServletDeployment(ctx);

        deployment.addFilter("test-filter", TestFilter.class, "/*");

        final boolean registered = java.util.Arrays.stream(ctx.findFilterDefs())
            .anyMatch(def -> "test-filter".equals(def.getFilterName()));
        assertTrue(registered, "Filter 'test-filter' must be registered via FilterDef");
    }

    /** Minimal filter for test use only. */
    static final class TestFilter implements jakarta.servlet.Filter {
        @Override
        public void doFilter(final jakarta.servlet.ServletRequest req,
                             final jakarta.servlet.ServletResponse res,
                             final jakarta.servlet.FilterChain chain) {}
    }
}
