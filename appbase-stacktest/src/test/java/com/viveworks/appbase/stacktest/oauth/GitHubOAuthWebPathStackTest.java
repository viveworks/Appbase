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

package com.viveworks.appbase.stacktest.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.viveworks.appbase.service.web.oauth.OAuthFilter;
import com.viveworks.appbase.service.web.oauth.ValidatedToken;
import com.viveworks.appbase.service.web.oauth.github.GitHubOAuthConfiguration;
import com.viveworks.appbase.service.web.oauth.github.GitHubOAuthDefaults;
import com.viveworks.appbase.service.web.oauth.github.GitHubOAuthFilter;
import com.viveworks.appbase.service.web.oauth.github.GitHubUserApi;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Real servlet-path coverage for GitHub's opaque-token public entry point. The GitHub transport is an
 * offline deterministic stub; {@code JdkGitHubUserApiTest} separately proves the real HTTP transport.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class GitHubOAuthWebPathStackTest {

    private static final String ACCEPTED = "gho_accepted";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private static final GitHubOAuthConfiguration CONFIGURATION = new GitHubOAuthConfiguration(
            URI.create("https://api.github.invalid/user"), GitHubOAuthDefaults.ISSUER,
            "stacktest-client", 60);

    @Test
    @Timeout(15)
    void missingBearerTokenIsRejectedBeforeCallingGitHub() throws Exception {
        Fixture fixture = new Fixture();
        try (ServerHandle server = start(fixture)) {
            assertEquals(401, request(server.url(), null).statusCode());
            assertEquals(0, fixture.providerCalls.get());
            assertEquals(0, fixture.downstreamCalls.get());
        }
    }

    @Test
    @Timeout(15)
    void githubRejectedTokenNeverReachesTheApplication() throws Exception {
        Fixture fixture = new Fixture();
        try (ServerHandle server = start(fixture)) {
            assertEquals(401, request(server.url(), "gho_revoked").statusCode());
            assertEquals(1, fixture.providerCalls.get());
            assertEquals(0, fixture.downstreamCalls.get());
        }
    }

    @Test
    @Timeout(15)
    void githubAcceptedIdentityReachesTheApplicationOverHttp() throws Exception {
        Fixture fixture = new Fixture();
        try (ServerHandle server = start(fixture)) {
            HttpResponse<String> response = request(server.url(), ACCEPTED);
            assertEquals(200, response.statusCode());
            assertEquals("github-user:42", response.body());
            assertEquals(1, fixture.downstreamCalls.get());
        }
    }

    @Test
    @Timeout(15)
    void repeatedTokenUsesTheBoundedValidatorCache() throws Exception {
        Fixture fixture = new Fixture();
        try (ServerHandle server = start(fixture)) {
            assertEquals(200, request(server.url(), ACCEPTED).statusCode());
            assertEquals(200, request(server.url(), ACCEPTED).statusCode());
            assertEquals(1, fixture.providerCalls.get(),
                    "the second request must use the validator cache, not call GitHub again");
            assertEquals(2, fixture.downstreamCalls.get());
        }
    }

    private static ServerHandle start(Fixture fixture) throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addFilter(new FilterHolder(offlineFilter(fixture.userApi())),
                "/github", EnumSet.of(DispatcherType.REQUEST));
        context.addServlet(new ServletHolder(fixture.servlet()), "/github");
        server.setHandler(context);
        try {
            server.start();
        } catch (Exception startupFailure) {
            try {
                server.stop();
            } catch (Exception cleanupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
            try {
                server.destroy();
            } catch (RuntimeException cleanupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
            throw startupFailure;
        }
        return new ServerHandle(server, "http://127.0.0.1:" + connector.getLocalPort() + "/github");
    }

    /** Keeps the arbitrary transport seam package-private in production while permitting this arm. */
    private static GitHubOAuthFilter offlineFilter(GitHubUserApi userApi) throws Exception {
        Constructor<GitHubOAuthFilter> constructor = GitHubOAuthFilter.class.getDeclaredConstructor(
                GitHubOAuthConfiguration.class, GitHubUserApi.class);
        constructor.setAccessible(true);
        return constructor.newInstance(CONFIGURATION, userApi);
    }

    private static HttpResponse<String> request(String url, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static final class Fixture {
        private final AtomicInteger providerCalls = new AtomicInteger();
        private final AtomicInteger downstreamCalls = new AtomicInteger();

        GitHubUserApi userApi() {
            return token -> {
                providerCalls.incrementAndGet();
                return ACCEPTED.equals(token)
                        ? new GitHubUserApi.Response(200, "{\"id\":42,\"login\":\"octocat\"}")
                        : new GitHubUserApi.Response(401, "{\"message\":\"Bad credentials\"}");
            };
        }

        HttpServlet servlet() {
            return new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws IOException {
                    downstreamCalls.incrementAndGet();
                    ValidatedToken token = (ValidatedToken) request.getAttribute(
                            OAuthFilter.VALIDATED_TOKEN_ATTRIBUTE);
                    response.setStatus(200);
                    response.getWriter().write("github-user:" + token.subject());
                }
            };
        }
    }

    private record ServerHandle(Server server, String url) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            Exception failure = null;
            try {
                server.stop();
            } catch (Exception stopFailure) {
                failure = stopFailure;
            } finally {
                try {
                    server.destroy();
                } catch (RuntimeException destroyFailure) {
                    if (failure == null) {
                        failure = destroyFailure;
                    } else {
                        failure.addSuppressed(destroyFailure);
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
