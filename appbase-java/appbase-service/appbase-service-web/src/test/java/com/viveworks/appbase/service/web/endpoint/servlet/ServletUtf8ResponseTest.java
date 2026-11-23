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
package com.viveworks.appbase.service.web.endpoint.servlet;

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.metrics.Counter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ServletUtf8ResponseTest {
    private static final class TestServlet extends AbstractAppbaseSyncServlet {
        @Override
        protected Counter counter(final String name) {
            return mock(Counter.class);
        }
    }

    @Test
    void internalErrorHidesDetailsAndLogsCorrelatedException() throws Exception {
        final TestServlet servlet = new TestServlet();
        servlet.logger = mock(Logger.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final IllegalStateException failure = new IllegalStateException(
                "credential=/secret/path/sentinel-2752");

        servlet.sendInternalServerError("operation", failure, response);

        final ArgumentCaptor<String> clientMessage = ArgumentCaptor.forClass(String.class);
        verify(response).sendError(eq(500), clientMessage.capture());
        Assertions.assertFalse(clientMessage.getValue().contains("sentinel-2752"));
        Assertions.assertFalse(clientMessage.getValue().contains("/secret/path"));

        final ArgumentCaptor<Object> logId = ArgumentCaptor.forClass(Object.class);
        verify(servlet.logger).error(eq("Internal server error [{}]"), logId.capture(), same(failure));
        Assertions.assertTrue(clientMessage.getValue().contains(logId.getValue().toString()));
    }

    @Test
    void jsonResponseUsesUtf8WithoutIncorrectContentLength() throws Exception {
        final String json = "{\"message\":\"caf\u00e9 \ud83d\ude80\"}";
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final Map<String, String> headers = new HashMap<>();
        final String[] encoding = {"ISO-8859-1"};
        final HttpServletResponse response = mock(HttpServletResponse.class);
        doAnswer(invocation -> {
            headers.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(response).setHeader(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        doAnswer(invocation -> {
            encoding[0] = invocation.getArgument(0);
            return null;
        }).when(response).setCharacterEncoding(org.mockito.ArgumentMatchers.anyString());
        when(response.getWriter()).thenAnswer(invocation ->
                new PrintWriter(bytes, false, Charset.forName(encoding[0])));

        new TestServlet().writeJson(json, response);

        Assertions.assertEquals(StandardCharsets.UTF_8.name(), encoding[0]);
        Assertions.assertArrayEquals(json.getBytes(StandardCharsets.UTF_8), bytes.toByteArray());
        Assertions.assertEquals(json, new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        final String declaredLength = headers.get("Content-Length");
        Assertions.assertTrue(declaredLength == null || Integer.parseInt(declaredLength) == bytes.size());
    }
}
