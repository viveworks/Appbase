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

package com.viveworks.appbase.client.service.aerospike.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Read policy for SC (strong consistency) namespaces.
 *
 * <p>Maps to {@code com.aerospike.client.policy.ReadModeSC}.</p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum ReadModeSC {
    /**
     * Ensures this client only sees an increasing sequence of record versions. Server reads from master only.
     */
    SESSION("SESSION"),

    /**
     * Ensures ALL clients only see an increasing sequence of record versions. Server reads from master only.
     */
    LINEARIZE("LINEARIZE"),

    /**
     * Server may read from master or any full (non-migrating) replica.
     */
    ALLOW_REPLICA("ALLOW_REPLICA"),

    /**
     * Server may read from master, any full replica, or from unavailable partitions.
     */
    ALLOW_UNAVAILABLE("ALLOW_UNAVAILABLE");

    private final String text;

    ReadModeSC(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.text = text.trim();
    }

    public String getText() {
        return text;
    }
}
