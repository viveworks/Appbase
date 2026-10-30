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

package com.viveworks.appbase.bootstrap.reactornetty.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link AppbaseServiceExchange} over reactor-netty's {@link HttpServerRequest}/{@link
 * HttpServerResponse}.
 *
 * <p>{@link #writeStream(Flux)} hands the {@link Flux} straight to reactor-netty's {@code send},
 * which honours the transport's write demand -- it does NOT collect the elements first. Buffering
 * here would defeat back-pressure end to end. The body is framed as NDJSON (newline-delimited JSON,
 * {@code application/x-ndjson}): each element is serialised independently and followed by a single
 * {@code '\n'}. Concatenating bare JSON values under an {@code application/json} Content-Type is NOT
 * a parseable document -- {@code {"a":1}{"a":2}} has no valid top-level grammar production, so a
 * client trusting that header cannot parse it. NDJSON stays parseable line-by-line as each element
 * arrives, which is the entire point of streaming a response instead of buffering it.
 *
 * <p>{@link #readBody()} uses {@code aggregate().asByteArray()}, which never blocks: the body
 * arrives as it arrives and the returned {@link Mono} completes when the last chunk lands.
 *
 * <p>{@link #queryParam(String)} decodes the query string with a {@link QueryStringDecoder} built
 * once from {@link HttpServerRequest#uri()}. reactor-netty's {@code request.param(name)} resolves a
 * PATH parameter from the route matcher, not a query-string parameter, so it is deliberately not
 * used here: with an exact-match route table there are no path parameters and it would return
 * {@code null} for every query parameter.
 *
 * <p>{@link #cookie(String)} iterates {@code request.cookies()} defensively because that map is keyed
 * by {@link CharSequence} (often an {@code AsciiString}), so a plain {@code Map.get(String)} can
 * miss when {@code String.equals} is asymmetric against the stored key type.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class NettyWebExchange implements AppbaseServiceExchange {

    private final HttpServerRequest request;

    private final HttpServerResponse response;

    private final ObjectMapper objectMapper;

    private final QueryStringDecoder queryStringDecoder;

    private final String path;

    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * @param request reactor-netty request
     * @param response reactor-netty response
     * @param objectMapper mapper used by {@link #writeJson(Object)}
     * @throws com.viveworks.appbase.service.path.AppbasePathException if {@code request.uri()}'s path
     *         portion cannot be reduced to a canonical key -- see {@link #path()}
     */
    public NettyWebExchange(
            final HttpServerRequest request,
            final HttpServerResponse response,
            final ObjectMapper objectMapper) {
        this.request = AppbaseUtil.notNull(request, "request");
        this.response = AppbaseUtil.notNull(response, "response");
        this.objectMapper = AppbaseUtil.notNull(objectMapper, "objectMapper");
        this.queryStringDecoder = new QueryStringDecoder(request.uri());
        // rawPath(), not fullPath(): canonicalization must see the ENCODED path. fullPath() has
        // already decoded %2F into a separator, and no later step can tell it from a real one.
        this.path = AppbasePathCanonicalizer.canonicalize(queryStringDecoder.rawPath());
    }

    @Override
    public String method() {
        return request.method().name();
    }

    @Override
    public String path() {
        return path;
    }

    @Nullable
    @Override
    public String queryParam(final String name) {
        final List<String> values = queryStringDecoder.parameters().get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    @Nullable
    @Override
    public String header(final String name) {
        return request.requestHeaders().get(name);
    }

    @Nullable
    @Override
    public String cookie(final String name) {
        for (final Map.Entry<CharSequence, Set<Cookie>> entry : request.cookies().entrySet()) {
            if (entry.getKey() != null && name.contentEquals(entry.getKey())) {
                return entry.getValue().stream().findFirst().map(Cookie::value).orElse(null);
            }
        }
        return null;
    }

    @Override
    public void status(final int code) {
        response.status(HttpResponseStatus.valueOf(code));
    }

    @Override
    public void responseHeader(final String name, final String value) {
        response.header(name, value);
    }

    @Override
    public Mono<Void> writeJson(final Object body) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(body))
                .flatMap(bytes -> {
                    response.header("Content-Type", "application/json");
                    return response.sendByteArray(Mono.just(bytes)).then();
                });
    }

    @Override
    public <T> Mono<Void> writeStream(final Flux<T> body) {
        // Must be set before the first byte is written -- reactor-netty flushes headers with the
        // first chunk of the body, so unlike writeJson this cannot be deferred into the map/flatMap
        // that does the serialising.
        response.header("Content-Type", "application/x-ndjson");
        return response.sendByteArray(
                body.map(element -> {
                    try {
                        final byte[] json = objectMapper.writeValueAsBytes(element);
                        final byte[] line = new byte[json.length + 1];
                        System.arraycopy(json, 0, line, 0, json.length);
                        line[json.length] = '\n';
                        return line;
                    } catch (final Exception ex) {
                        throw new IllegalStateException("Failed to serialise a streamed element", ex);
                    }
                })).then();
    }

    @Override
    public Mono<byte[]> readBody() {
        return request.receive().aggregate().asByteArray().defaultIfEmpty(new byte[0]);
    }

    @Override
    public Map<String, Object> attributes() {
        return attributes;
    }
}
