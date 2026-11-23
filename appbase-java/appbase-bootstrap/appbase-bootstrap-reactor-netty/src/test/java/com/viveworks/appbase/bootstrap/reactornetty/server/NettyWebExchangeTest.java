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

package com.viveworks.appbase.bootstrap.reactornetty.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.service.path.AppbasePathException;
import org.junit.jupiter.api.Test;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link NettyWebExchange#path()} must return the {@link
 * com.viveworks.appbase.service.path.AppbasePathCanonicalizer} key, not the merely-decoded {@code
 * fullPath()} -- see the constructor Javadoc for why {@code rawPath()} is the required input.
 *
 * <p>{@link #pathIsCanonicalNotMerelyDecoded()} shares its wire path ({@code /%61dmin}) and expected
 * key ({@code /admin}) with the servlet face ({@code AuthorizationServletFilterTest}, Task 5) and the
 * JAX-RS face ({@code UserSessionJaxrsRequestFilterTest}, Task 6). This is the reactive third of the
 * three-face agreement (amendment A3): if any of the three literals drifts from the others, the faces
 * have drifted apart.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class NettyWebExchangeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void pathIsCanonicalNotMerelyDecoded() {
        final HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.uri()).thenReturn("/%61dmin?q=1");

        final NettyWebExchange exchange = new NettyWebExchange(request, mock(HttpServerResponse.class), objectMapper);

        // Same wire path and expected key as AuthorizationServletFilterTest (Task 5) and
        // UserSessionJaxrsRequestFilterTest (Task 6). Three faces, one key. If these literals
        // drift apart, the faces have drifted.
        assertEquals("/admin", exchange.path());
    }

    @Test
    void anEncodedSeparatorIsRejectedAtConstruction() {
        final HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.uri()).thenReturn("/a%2Fb");

        assertThrows(AppbasePathException.class,
                () -> new NettyWebExchange(request, mock(HttpServerResponse.class), objectMapper));
    }
}
