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

package com.viveworks.appbase.stacktest;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs the capture fixture's {@link CoreSwaggerCaptureStackTest.LoopbackReadOnlyFilter} in a real in-process Jetty on
 * 127.0.0.1 (no browser, no opt-in property): only GET and HEAD with the exact Host reach the servlet, the rest get 403.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
class CoreSwaggerCaptureFilterTest {

    private final AtomicInteger reached = new AtomicInteger();
    private Server server;
    private int port;

    @BeforeEach
    void start() throws Exception {
        server = new Server();
        final ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);
        final ServletContextHandler context = new ServletContextHandler();
        context.addFilter(new FilterHolder(new CoreSwaggerCaptureStackTest.LoopbackReadOnlyFilter()), "/*",
                EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(final HttpServletRequest request, final HttpServletResponse response) {
                reached.incrementAndGet();
            }
        }), "/*");
        server.setHandler(context);
        server.start();
        port = connector.getLocalPort();
    }

    @AfterEach
    void stop() throws Exception {
        server.stop();
    }

    /** Status line of the answer to a raw request, so the Host header can be chosen freely. */
    private String status(final String method, final String host) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(10_000);
            socket.getOutputStream().write((method + " /x HTTP/1.1\r\nHost: " + host
                    + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)).readLine();
        }
    }

    // EXTRA: the Host and method guard of the capture fixture is executed, not only read.
    @Test
    void test_EXTRA_getAndHeadWithTheExactHostReachTheServlet() throws IOException {
        Assertions.assertEquals("HTTP/1.1 200 OK", status("GET", "127.0.0.1:" + port));
        Assertions.assertEquals("HTTP/1.1 200 OK", status("HEAD", "127.0.0.1:" + port));
        Assertions.assertEquals(2, reached.get());
    }

    // EXTRA: a wrong Host (DNS-rebinding name, no port, name instead of address, another port) is refused.
    @Test
    void test_EXTRA_wrongHostIsForbiddenAndNeverReachesTheServlet() throws IOException {
        for (String host : new String[]{"evil.example", "127.0.0.1", "localhost:" + port, "127.0.0.1:" + (port + 1)}) {
            Assertions.assertEquals("HTTP/1.1 403 Forbidden", status("GET", host), host);
        }
        Assertions.assertEquals(0, reached.get());
    }

    // EXTRA: POST, PUT and DELETE are refused even with the exact Host.
    @Test
    void test_EXTRA_postPutDeleteAreForbiddenAndNeverReachTheServlet() throws IOException {
        for (String method : new String[]{"POST", "PUT", "DELETE"}) {
            Assertions.assertEquals("HTTP/1.1 403 Forbidden", status(method, "127.0.0.1:" + port), method);
        }
        Assertions.assertEquals(0, reached.get());
    }
}
