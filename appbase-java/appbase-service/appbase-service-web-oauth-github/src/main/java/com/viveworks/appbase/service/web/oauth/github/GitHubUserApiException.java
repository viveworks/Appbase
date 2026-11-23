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

/**
 * Signals that GitHub's {@code /user} endpoint could not be reached — a connection failure, timeout, or
 * interrupted call — as opposed to an HTTP rejection, which is returned as a {@link GitHubUserApi.Response}
 * with a non-2xx status. The {@link GitHubTokenValidator} maps this to a {@code PROVIDER_UNREACHABLE}
 * rejection so a transient outage is never mistaken for an invalid token.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class GitHubUserApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message a short, non-sensitive description of the transport failure
     * @param cause   the underlying I/O or interruption cause
     */
    public GitHubUserApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
