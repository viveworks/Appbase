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

package com.viveworks.appbase.client.service.aerospike.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Defines the algorithm used to determine the target node for a command.
 *
 * <p>Maps to {@code com.aerospike.client.policy.Replica}.</p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum ReplicaPolicy {
    /**
     * Try node containing master partition first. If connection fails, try replicated partition nodes.
     */
    SEQUENCE("SEQUENCE"),

    /**
     * Try node on the same rack as the client first. Falls back to SEQUENCE.
     */
    PREFER_RACK("PREFER_RACK"),

    /**
     * Distribute reads across all nodes in cluster in round-robin fashion.
     */
    RANDOM("RANDOM"),

    /**
     * Use node containing key's master partition.
     */
    MASTER("MASTER"),

    /**
     * Distribute reads across master and replicated partition nodes in round-robin fashion.
     */
    MASTER_PROLES("MASTER_PROLES");

    private final String text;

    ReplicaPolicy(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.text = text.trim();
    }

    public String getText() {
        return text;
    }
}
