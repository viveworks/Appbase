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

import com.viveworks.appbase.service.exchange.AppbaseServiceExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal {@link AppbaseServiceExchange} test double, modelled on the {@code RecordingExchange}
 * used by the Task 3 web-filter tests: fixed method/path, status and {@link #writeJson(Object)}
 * body capture, everything else empty/no-op.
 *
 * <p>Non-final, with an overridable {@link #writeJson(Object)}: the Task 6 offload test needs to
 * capture the calling thread's name from inside a subclassed override, to prove the write happens
 * on the offload scheduler rather than the thread that invoked {@code handle}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class FakeExchange implements AppbaseServiceExchange {

    private final String method;

    private final String path;

    private final Map<String, Object> attributes = new HashMap<>();

    private final Map<String, String> queryParams = new HashMap<>();

    private int status = 200;

    private Object body;

    FakeExchange(final String method, final String path) {
        this.method = method;
        this.path = path;
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public String path() {
        return path;
    }

    /**
     * Sets a query parameter this exchange will serve from {@link #queryParam(String)}.
     *
     * @param name parameter name
     * @param value parameter value
     * @return this instance, for fluent test setup
     */
    FakeExchange withQueryParam(final String name, final String value) {
        queryParams.put(name, value);
        return this;
    }

    @Nullable
    @Override
    public String queryParam(final String name) {
        return queryParams.get(name);
    }

    @Nullable
    @Override
    public String header(final String name) {
        return null;
    }

    @Nullable
    @Override
    public String cookie(final String name) {
        return null;
    }

    @Override
    public void status(final int code) {
        this.status = code;
    }

    @Override
    public void responseHeader(final String name, final String value) {
        // not recorded: no test needs it
    }

    @Override
    public Mono<Void> writeJson(final Object body) {
        this.body = body;
        return Mono.empty();
    }

    @Override
    public <T> Mono<Void> writeStream(final Flux<T> body) {
        return body.then();
    }

    @Override
    public Mono<byte[]> readBody() {
        return Mono.just(new byte[0]);
    }

    @Override
    public Map<String, Object> attributes() {
        return attributes;
    }

    int status() {
        return status;
    }

    Object body() {
        return body;
    }
}
