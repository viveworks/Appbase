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

package com.viveworks.appbase.client.service.redis.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Behaviour of a Lettuce connection when it is not yet connected or while reconnecting.
 *
 * <p>Maps to {@code io.lettuce.core.ClientOptions.DisconnectedBehavior}. The text value
 * returned by {@link #getText()} matches the Lettuce enum constant name so that
 * {@code ClientOptions.DisconnectedBehavior.valueOf(getText())} resolves correctly.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum DisconnectedBehavior {

    /** Use the Lettuce default behaviour (currently {@code REJECT_COMMANDS}). */
    DEFAULT("DEFAULT"),

    /** Accept and queue commands while disconnected. */
    ACCEPT_COMMANDS("ACCEPT_COMMANDS"),

    /** Reject (throw) commands immediately while disconnected. */
    REJECT_COMMANDS("REJECT_COMMANDS");

    private final String text;

    DisconnectedBehavior(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.text = text;
    }

    /**
     * Returns the text value matching the Lettuce {@code ClientOptions.DisconnectedBehavior} enum name.
     *
     * @return text value
     */
    public String getText() {
        return text;
    }
}
