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

package com.viveworks.appbase.bootstrap.reactornetty.endpoint;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;

import java.util.Map;

/**
 * Request-parameter validation and error-response shape shared by every endpoint dispatcher in this
 * package -- {@link OffloadedEndpoints} (offloaded reads) and {@link NativeLogEndpoint} (the native
 * {@code /log} route). Extracted so the 400 mapping, the bounded-parameter check and the error-body
 * shape are defined ONCE and used identically by both, rather than risking the two drifting apart.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class EndpointReads {

    private EndpointReads() {
    }

    /**
     * @param exchange the request/response exchange
     * @param name query parameter name
     * @return the non-blank parameter value
     * @throws DispatchException a 400, if the parameter is missing or blank -- mirroring the
     *     servlets' {@code nonBlankParameter}/{@code sendInvalidParameter}
     */
    static String requireParam(final AppbaseServiceExchange exchange, final String name) {
        final String value = exchange.queryParam(name);
        if (AppbaseUtil.isBlank(value)) {
            throw new DispatchException(400, "Invalid parameter: " + name);
        }

        return value;
    }

    /**
     * @param exchange the request/response exchange
     * @param name query parameter name
     * @param maxPageSize the inclusive upper bound
     * @return the parsed parameter value, guaranteed to lie in {@code [1, maxPageSize]}
     * @throws DispatchException a 400, if the parameter is missing or blank (mirrors {@link
     *     #requireParam}), or once parsed is below 1 or above {@code maxPageSize} -- rejected
     *     before the read it would otherwise bound ever runs. A non-numeric value throws
     *     {@link NumberFormatException} unmapped, the same faithful-to-the-servlet reproduction the
     *     other numeric parameters get.
     */
    static int requireBoundedInt(
            final AppbaseServiceExchange exchange, final String name, final int maxPageSize) {
        final int value = Integer.parseInt(requireParam(exchange, name));
        if (value < 1 || value > maxPageSize) {
            throw new DispatchException(400, "Invalid parameter: " + name);
        }

        return value;
    }

    static Map<String, Object> errorBody(final int status, final String message) {
        return Map.of("status", status, "message", message);
    }

    /**
     * Signals a controlled, non-exceptional 400/404 response -- a missing or unsupported {@code
     * operation}, a missing required parameter, or a "not found" read -- as opposed to an
     * unexpected failure, which is left to propagate untouched and is mapped to 500 (or 503 for a
     * timeout) further up the chain.
     */
    static final class DispatchException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final int status;

        DispatchException(final int status, final String message) {
            super(message);
            this.status = status;
        }

        int status() {
            return status;
        }
    }
}
