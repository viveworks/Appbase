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
import com.viveworks.appbase.core.service.log.jmx.JmxLogService;
import com.viveworks.appbase.core.service.log.jmx.JmxLogServiceMXBean;
import com.viveworks.appbase.service.web.endpoint.jaxrs.appbase.LogJaxrsResource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class LogPageSizeHttpTest {
    private static final class TestServlet extends LogServlet {
        private final AppbaseCore core = mock(AppbaseCore.class, RETURNS_DEEP_STUBS);
        private final Timer timer = mock(Timer.class);

        TestServlet() {
            logger = mock(Logger.class);
            when(timer.time()).thenReturn(mock(Timer.Context.class));
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

    private static final class TestJaxrs extends LogJaxrsResource {
        private final JmxLogServiceMXBean bean = mock(JmxLogServiceMXBean.class);

        @Override
        protected JmxLogServiceMXBean jmxLogServiceMXBean(final ServletContext context) {
            return bean;
        }
    }

    @Test
    void servletRejectsZeroAndAboveMaximumBeforeJmxRead() throws Exception {
        final TestServlet servlet = new TestServlet();
        final JmxLogService bean = mock(JmxLogService.class);
        when(servlet.core.getJmxService().getMXBean(JmxLogService.class)).thenReturn(bean);
        for (final String pageSize : new String[] {"0", "1001"}) {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getParameter("operation")).thenReturn("logRecordsAt");
            when(request.getParameter("timestamp")).thenReturn("1");
            when(request.getParameter("pageSize")).thenReturn(pageSize);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            servlet.doGet(request, response);
            verify(response).sendError(eq(400), eq("Invalid pageSize"));
        }
        verify(bean, never()).readLogRecordsAt(anyLong(), anyInt());

        final HttpServletRequest maxRequest = mock(HttpServletRequest.class);
        when(maxRequest.getParameter("operation")).thenReturn("logRecordsAt");
        when(maxRequest.getParameter("timestamp")).thenReturn("1");
        when(maxRequest.getParameter("pageSize")).thenReturn("1000");
        final HttpServletResponse maxResponse = mock(HttpServletResponse.class);
        when(maxResponse.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(bean.readLogRecordsAt(1L, 1000)).thenReturn(Map.of());
        servlet.doGet(maxRequest, maxResponse);
        verify(bean).readLogRecordsAt(1L, 1000);
    }

    @Test
    void jaxrsRejectsZeroAndAboveMaximumBeforeJmxRead() throws Exception {
        final TestJaxrs resource = new TestJaxrs();
        Assertions.assertThrows(BadRequestException.class,
                () -> resource.readLogRecordsAt(1L, 0, null));
        Assertions.assertThrows(BadRequestException.class,
                () -> resource.readLogRecordsAt(1L, 1001, null));
        verify(resource.bean, never()).readLogRecordsAt(anyLong(), anyInt());

        when(resource.bean.readLogRecordsAt(1L, 1000)).thenReturn(Map.of());
        Assertions.assertEquals(200, resource.readLogRecordsAt(1L, 1000, null).getStatus());
        verify(resource.bean).readLogRecordsAt(1L, 1000);
    }
}
