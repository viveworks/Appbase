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

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.exchange.AppbaseServiceHandler;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exact-match route table: {@code (METHOD, path)} to a handler.
 *
 * <p>Deliberately no path-pattern language, no wildcards, no path variables. The twelve ported
 * endpoints and the R5 showcase are the only consumers, and every one of them mounts at a fixed
 * path taken from {@code AppbaseServiceConstants}. Add pattern matching when a route needs it, not
 * before.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AppbaseRouteRegistry {

    private final Map<String, AppbaseServiceHandler> handlerByKey = new ConcurrentHashMap<>();

    /**
     * @param method HTTP method, case-insensitive
     * @param path exact request path
     * @param handler handler to invoke
     * @throws IllegalStateException if the route is already registered
     */
    public void route(final String method, final String path, final AppbaseServiceHandler handler) {
        AppbaseUtil.notBlank(method, "method");
        AppbaseUtil.notBlank(path, "path");
        AppbaseUtil.notNull(handler, "handler");

        final AppbaseServiceHandler previous = handlerByKey.putIfAbsent(key(method, path), handler);
        if (previous != null) {
            throw new IllegalStateException("Route already registered: " + method + " " + path);
        }
    }

    /**
     * @param method HTTP method, case-insensitive
     * @param path exact request path
     * @return the handler, or {@code null} when no route matches
     */
    @Nullable
    public AppbaseServiceHandler lookup(final String method, final String path) {
        return handlerByKey.get(key(method, path));
    }

    /**
     * @param method HTTP method, case-insensitive
     * @param path exact request path
     * @return {@code true} if a route was registered at {@code (method, path)} and has been
     *     removed, {@code false} if none matched
     */
    public boolean remove(final String method, final String path) {
        AppbaseUtil.notBlank(method, "method");
        AppbaseUtil.notBlank(path, "path");

        return handlerByKey.remove(key(method, path)) != null;
    }

    private static String key(final String method, final String path) {
        return method.toUpperCase(Locale.ROOT) + " " + path;
    }
}
