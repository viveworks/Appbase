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

package com.viveworks.appbase.service.web.oauth.github;

/**
 * The authenticated call to GitHub's {@code /user} endpoint that validates an opaque access token.
 *
 * <p>GitHub is not OIDC: there is no {@code id_token} and no JWKS, so a token cannot be verified locally.
 * This seam performs the one network round trip the {@link GitHubTokenValidator} makes, and is the point
 * where a stubbed transport replaces the real HTTP client so the validator is testable without a network.
 * The production implementation is {@link JdkGitHubUserApi}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface GitHubUserApi {

    /**
     * Performs {@code GET /user} with the given token as the bearer credential.
     *
     * @param bearerToken the raw opaque access token to authenticate the call with
     * @return the provider's HTTP status and response body
     * @throws GitHubUserApiException if the endpoint cannot be reached (connection failure, timeout,
     *                                interrupted): a transport failure, distinct from an HTTP rejection
     */
    Response fetch(String bearerToken);

    /**
     * A GitHub {@code /user} HTTP response: the status code and the raw body.
     *
     * @param statusCode the HTTP status returned by the provider
     * @param body       the raw response body (a JSON user object on success)
     */
    record Response(int statusCode, String body) {
    }
}
