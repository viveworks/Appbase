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

package com.viveworks.appbase.core.dependency;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.ProcessingException;
import org.glassfish.jersey.jetty.servlet.JettyWebContainerFactory;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * jersey-container-jetty-servlet exports {@link JettyWebContainerFactory} publicly, and its
 * {@code create(URI, Class, Map)} overload unconditionally delegates to a private helper whose
 * only statement constructs a {@link ProcessingException} from
 * {@code org.glassfish.jersey.jetty.internal.LocalizationMessages}, a class supplied only by
 * jersey-container-jetty-http. A dependency fix that excludes jersey-container-jetty-http
 * wholesale (rather than only its incompatible Jetty coordinates) leaves this exported factory
 * exported but unusable: invoking it throws {@link NoClassDefFoundError} instead of Jersey's own
 * intended {@link ProcessingException}. This regression proves the retained classpath resolves
 * {@code LocalizationMessages} and the factory fails the way Jersey intends.
 *
 * @author Claude
 * @since 1.0.0
 */
class JerseyJettyContainerLinkageTest {

    @Test
    void create_withServletClassOverload_throwsProcessingExceptionNotLinkageError() {
        final ProcessingException exception = assertThrows(ProcessingException.class, () ->
            JettyWebContainerFactory.create(
                URI.create("http://localhost:0/"),
                NoOpServlet.class,
                Map.of()));
        assertNotNull(exception.getMessage());
    }

    public static class NoOpServlet implements Servlet {
        @Override
        public void init(final jakarta.servlet.ServletConfig config) {
        }

        @Override
        public jakarta.servlet.ServletConfig getServletConfig() {
            return null;
        }

        @Override
        public void service(final jakarta.servlet.ServletRequest req, final jakarta.servlet.ServletResponse res) {
        }

        @Override
        public String getServletInfo() {
            return "";
        }

        @Override
        public void destroy() {
        }
    }
}
