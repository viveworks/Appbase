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

package com.viveworks.appbase.stacktest.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import com.sun.net.httpserver.HttpServer;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.Test;

/**
 * internal work item 1329 review regression: the status/GET helpers ({@code httpGetStatusAndBody}, {@code httpCodeOfHead})
 * must RELEASE their pooled connection on every call — consume the entity / use a response handler — or
 * repeated stack-test arms exhaust the pool and hang. This was the connection-retention finding on the
 * repaired {@code /status} arm.
 *
 * <p>Runs the helpers against a local {@link HttpServer} through an Apache client whose connection pool
 * holds exactly ONE connection. If any call retained its connection, the very next acquire could not be
 * satisfied and would fail with a connection-request timeout; all iterations succeeding proves each call
 * releases. Reverting the helpers to read the status without consuming the entity makes this test fail
 * (source-level mutation reasoning; no executed mutation receipt is claimed here).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
class HttpHelperConnectionReleaseTest {

    @Test
    void statusHelpersReleaseTheirPooledConnection() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/status", exchange -> {
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
            } else {
                final byte[] body = "{\"started\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();

        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(1);
        cm.setDefaultMaxPerRoute(1);
        final CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .setDefaultRequestConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(2000)
                .setConnectTimeout(2000)
                .setSocketTimeout(2000)
                .build())
            .build();
        try {
            final int port = server.getAddress().getPort();
            final String url = "http://127.0.0.1:" + port + "/status";
            final AbstractTest arm = new AbstractTest(
                mock(AppbaseServices.class, RETURNS_DEEP_STUBS),
                httpClient,
                mock(CookieStore.class),
                url) { };

            // More iterations than the pool holds a connection for: each call must release or the next
            // acquire times out (ConnectionPoolTimeoutException) and this test fails.
            for (int i = 0; i < 5; i++) {
                final AbstractTest.StatusAndBody get = arm.httpGetStatusAndBody(url);
                assertEquals(200, get.statusCode(), "GET status on iteration " + i);
                assertTrue(get.body() != null && !get.body().isBlank(), "GET body non-empty on iteration " + i);
                assertEquals(405, arm.httpCodeOfHead(url), "HEAD status on iteration " + i);
            }
        } finally {
            httpClient.close();
            server.stop(0);
        }
    }
}
