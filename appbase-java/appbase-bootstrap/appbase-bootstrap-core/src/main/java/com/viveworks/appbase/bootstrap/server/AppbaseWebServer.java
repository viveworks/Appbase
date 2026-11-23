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
package com.viveworks.appbase.bootstrap.server;

/**
 * Container-neutral lifecycle contract for an embedded Appbase web server.
 *
 * <p>Implementations (Jetty, Undertow, Tomcat) live in their own
 * {@code appbase-bootstrap-<container>} modules. This interface intentionally
 * exposes only the minimal lifecycle; container-specific extras (e.g. Jetty's
 * {@code join()}) remain on the concrete implementation.
 *
 * @author Navaneeth Mysore G
 */
public interface AppbaseWebServer {

    /**
     * Starts the embedded server and begins accepting connections.
     *
     * @throws Exception if the server fails to start
     */
    void start() throws Exception;

    /**
     * Stops the embedded server and releases its resources.
     *
     * @throws Exception if the server fails to stop cleanly
     */
    void stop() throws Exception;

    /**
     * Returns the TCP port the server listens on, or {@code -1} if not yet
     * determinable (e.g. a custom server that has not been started).
     *
     * @return the listen port, or {@code -1}
     */
    int getPort();

    /**
     * Returns whether the server is currently started.
     *
     * @return {@code true} if started
     */
    boolean isStarted();
}
