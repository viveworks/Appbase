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

package com.viveworks.appbase.service.web.oauth.github;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * The production {@link GitHubUserApi}: a {@code GET /user} over the JDK {@link HttpClient}, sending the
 * access token as the {@code Authorization: Bearer} credential and returning the status and body verbatim.
 *
 * <p>An HTTP rejection (401, 403, …) is a normal {@link Response} — the validator decides what it means.
 * A failure to reach the endpoint (connection refused, timeout, interrupted) is a
 * {@link GitHubUserApiException}, so a transient outage is never mistaken for an invalid token.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class JdkGitHubUserApi implements GitHubUserApi {

    private final URI userApiUri;
    private final Duration requestTimeout;
    private final HttpClient httpClient;

    /**
     * @param userApiUri     the {@code /user} endpoint to call
     * @param requestTimeout the per-request timeout
     */
    public JdkGitHubUserApi(URI userApiUri, Duration requestTimeout) {
        this.userApiUri = userApiUri;
        this.requestTimeout = requestTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .build();
    }

    @Override
    public Response fetch(String bearerToken) {
        HttpRequest request = HttpRequest.newBuilder(userApiUri)
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + bearerToken)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "appbase-oauth")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new GitHubUserApiException("could not reach GitHub /user endpoint", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitHubUserApiException("interrupted while calling GitHub /user endpoint", e);
        }
    }
}
