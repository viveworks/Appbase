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
package com.viveworks.appbase.service.web.endpoint.servlet.appbase;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.configuration.DefaultConfigurationServiceConfiguration;
import com.viveworks.appbase.core.service.configuration.jmx.JmxConfigurationService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.DefaultMediatorServiceConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ConfigurationServletSecurityTest {
    private static final class TestServlet extends ConfigurationServlet {
        private final AppbaseCore core = mock(AppbaseCore.class, RETURNS_DEEP_STUBS);
        private final Timer timer = mock(Timer.class);

        TestServlet() {
            logger = mock(Logger.class);
            when(timer.time()).thenReturn(mock(Timer.Context.class));
        }

        Logger testLogger() {
            return logger;
        }

        @Override
        protected AppbaseCore appbaseCore() {
            return core;
        }

        @Override
        protected Timer timer(final String operation) {
            return timer;
        }

        @Override
        protected Counter counter(final String operation) {
            return mock(Counter.class);
        }
    }

    @Test
    void restrictedSecretPropertyAndTypeAreNotReturned() throws Exception {
        final TestServlet servlet = new TestServlet();
        final ConfigurationService configuration = mock(ConfigurationService.class);
        when(configuration.getBoolean(JmxConfigurationService.SECURE_CONFIG_OPERATIONS_PERMISSION_PROPERTY))
                .thenReturn(false);
        when(configuration.getSecurePropertyKeys()).thenReturn(Set.of("secret.key"));
        when(configuration.getString("secret.key")).thenReturn("credential-sentinel-2751");
        when(configuration.getSecureTypeNames())
                .thenReturn(Set.of(DefaultMediatorServiceConfiguration.class.getName()));
        when(configuration.getTypedConfiguration(DefaultMediatorServiceConfiguration.class))
                .thenReturn(DefaultMediatorServiceConfiguration.builder()
                        .messageObjectPoolCapacity(2751).build());
        final JmxConfigurationService bean = new JmxConfigurationService(
                mock(LogService.class), configuration,
                DefaultConfigurationServiceConfiguration.class, configuration);
        when(servlet.core.getJmxService().getMXBean(JmxConfigurationService.class)).thenReturn(bean);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final StringWriter propertyBody = new StringWriter();
        final AtomicInteger propertyStatus = new AtomicInteger(200);
        when(response.getWriter()).thenReturn(new PrintWriter(propertyBody));
        doAnswer(invocation -> {
            propertyStatus.set(invocation.getArgument(0));
            return null;
        }).when(response).setStatus(anyInt());
        doAnswer(invocation -> {
            propertyStatus.set(invocation.getArgument(0));
            return null;
        }).when(response).sendError(anyInt(), anyString());
        final HttpServletRequest propertyRequest = mock(HttpServletRequest.class);
        when(propertyRequest.getParameter("operation")).thenReturn("property");
        when(propertyRequest.getParameter("propertyKey")).thenReturn("secret.key");
        servlet.doGet(propertyRequest, response);

        final HttpServletResponse typeResponse = mock(HttpServletResponse.class);
        final StringWriter typeBody = new StringWriter();
        final AtomicInteger typeStatus = new AtomicInteger(200);
        when(typeResponse.getWriter()).thenReturn(new PrintWriter(typeBody));
        doAnswer(invocation -> {
            typeStatus.set(invocation.getArgument(0));
            return null;
        }).when(typeResponse).setStatus(anyInt());
        doAnswer(invocation -> {
            typeStatus.set(invocation.getArgument(0));
            return null;
        }).when(typeResponse).sendError(anyInt(), anyString());
        final HttpServletRequest typeRequest = mock(HttpServletRequest.class);
        when(typeRequest.getParameter("operation")).thenReturn("typedConfiguration");
        when(typeRequest.getParameter("typeName"))
                .thenReturn(DefaultMediatorServiceConfiguration.class.getName());
        servlet.doGet(typeRequest, typeResponse);

        if (Boolean.getBoolean("appbase.trace2751")) {
            System.out.printf("TRACE-2751 servlet property status=%d body=%s%n",
                    propertyStatus.get(), propertyBody.toString().replace("\n", "\\n"));
            System.out.printf("TRACE-2751 servlet typedConfiguration status=%d body=%s%n",
                    typeStatus.get(), typeBody.toString().replace("\n", "\\n"));
        }

        verify(response).sendError(eq(404), eq("Not Found"));
        Assertions.assertTrue(propertyBody.toString().isEmpty());
        Assertions.assertFalse(propertyBody.toString().contains("credential-sentinel-2751"));
        verify(typeResponse).sendError(eq(404), eq("Not Found"));
        Assertions.assertTrue(typeBody.toString().isEmpty());
        Assertions.assertFalse(typeBody.toString().contains("2751"));
    }

    @Test
    void coreCatchPathKeepsFailureOutOfResponseAndLogsItWithReference() throws Exception {
        final TestServlet servlet = new TestServlet();
        final JmxConfigurationService bean = mock(JmxConfigurationService.class);
        when(servlet.core.getJmxService().getMXBean(JmxConfigurationService.class)).thenReturn(bean);
        final IllegalStateException failure = new IllegalStateException(
                "credential=/secret/path/sentinel-2752");
        when(bean.getString("broken.key")).thenThrow(failure);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("operation")).thenReturn("property");
        when(request.getParameter("propertyKey")).thenReturn("broken.key");
        final HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doGet(request, response);

        final ArgumentCaptor<String> clientMessage = ArgumentCaptor.forClass(String.class);
        verify(response).sendError(eq(500), clientMessage.capture());
        Assertions.assertFalse(clientMessage.getValue().contains("sentinel-2752"));
        final ArgumentCaptor<Object> logId = ArgumentCaptor.forClass(Object.class);
        verify(servlet.testLogger()).error(eq("Internal server error [{}]"), logId.capture(), same(failure));
        Assertions.assertTrue(clientMessage.getValue().contains(logId.getValue().toString()));
    }
}
