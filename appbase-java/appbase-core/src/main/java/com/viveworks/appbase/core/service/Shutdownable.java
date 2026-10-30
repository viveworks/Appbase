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

package com.viveworks.appbase.core.service;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents an implementation that can be shutdown. This happens typically when an implementation acquires resource(s)
 * during construction and the resources need to be released during shutdown.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressFBWarnings(value = "DRE_DECLARED_RUNTIME_EXCEPTION")
public interface Shutdownable {
    /**
     * Shutdown.
     *
     * @throws AppbaseRuntimeException if shutdown failed
     */
    void shutdown() throws AppbaseRuntimeException;


    /**
     * Check if the implementation is shutdown.
     *
     * <p>
     *     Implementations of this interface (usually services) do not need to check for the shutdown state (by calling this
     *     method) in their service methods / APIs. Instead, the callers of the service methods / APIs can check for
     *     the state (is required, i.e if the application is designed to shut down services while being functional). This is
     *     because shutting down services usually does not happen too often and checking this state in each service method
     *     may be an overkill.
     * </p>
     *
     * @return true if shutdown, false otherwise
     */
    boolean isShutdown();
}
