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

package com.viveworks.appbase.client.service.clamav.client.event;

import com.viveworks.appbase.client.service.clamav.client.handler.ClamAVPingResultHandler;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ClamAVPingEvent}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVPingEvent implements ClamAVPingEvent {
    private final AtomicReference<ClamAVPingResultHandler> clamAVPingResultHandlerRef;

    public DefaultClamAVPingEvent() {
        this.clamAVPingResultHandlerRef = new AtomicReference<>();
    }

    public DefaultClamAVPingEvent(final ClamAVPingResultHandler clamAVPingResultHandler) {
        this();
        init(clamAVPingResultHandler);
    }

    @Override
    public void init(final ClamAVPingResultHandler clamAVPingResultHandler) {
        AppbaseUtil.notNull(clamAVPingResultHandler, "clamAVPingResultHandler");

        this.clamAVPingResultHandlerRef.set(clamAVPingResultHandler);
    }

    @Override
    public ClamAVPingResultHandler getClamAVPingResultHandler() {
        return clamAVPingResultHandlerRef.get();
    }

    @Override
    public void reset() {
        clamAVPingResultHandlerRef.set(null);
    }

    @Override
    public boolean isValid() {
        return clamAVPingResultHandlerRef.get() != null;
    }

    @Override
    public String toString() {
        final ClamAVPingResultHandler clamAVPingResultHandler = clamAVPingResultHandlerRef.get();

        return "DefaultClamAVPingEvent{" +
                "clamAVPingResultHandler=" +
                (clamAVPingResultHandler != null ? clamAVPingResultHandler.getClass().getName() : null) +
                '}';
    }
}
