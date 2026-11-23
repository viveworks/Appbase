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

package com.viveworks.appbase.client.service.clamav.client.event;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.handler.AbstractMutableAppbaseHandlerRequest;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ClamAVRequestEvent}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVRequestEvent extends AbstractMutableAppbaseHandlerRequest implements ClamAVRequestEvent {
    private final AtomicReference<Class<? extends ClamAVRequestEventType>> clamAVRequestEventTypeRef;
    private final ClamAVPingEvent clamAVPingEvent;
    private final ClamAVScanPathEvent clamAVScanPathEvent;
    private final ClamAVScanStreamEvent clamAVScanStreamEvent;

    public DefaultClamAVRequestEvent() {
        this.clamAVRequestEventTypeRef = new AtomicReference<>();
        this.clamAVPingEvent = new DefaultClamAVPingEvent();
        this.clamAVScanPathEvent = new DefaultClamAVScanPathEvent();
        this.clamAVScanStreamEvent = new DefaultClamAVScanStreamEvent();
    }

    public DefaultClamAVRequestEvent(
            final ClamAVPingEvent clamAVPingEvent,
            final ClamAVScanPathEvent clamAVScanPathEvent,
            final ClamAVScanStreamEvent clamAVScanStreamEvent)
    {
        AppbaseUtil.notNull(clamAVPingEvent, "clamAVPingEvent");
        AppbaseUtil.notNull(clamAVScanPathEvent, "clamAVScanPathEvent");
        AppbaseUtil.notNull(clamAVScanStreamEvent, "clamAVScanStreamEvent");

        this.clamAVRequestEventTypeRef = new AtomicReference<>();
        this.clamAVPingEvent = clamAVPingEvent;
        this.clamAVScanPathEvent = clamAVScanPathEvent;
        this.clamAVScanStreamEvent = clamAVScanStreamEvent;
    }

    public DefaultClamAVRequestEvent(
            final Class<? extends ClamAVRequestEventType> clamAVRequestEventType,
            final ClamAVPingEvent clamAVPingEvent,
            final ClamAVScanPathEvent clamAVScanPathEvent,
            final ClamAVScanStreamEvent clamAVScanStreamEvent)
    {
        AppbaseUtil.notNull(clamAVRequestEventType, "clamAVRequestEventType");
        AppbaseUtil.notNull(clamAVPingEvent, "clamAVPingEvent");
        AppbaseUtil.notNull(clamAVScanPathEvent, "clamAVScanPathEvent");
        AppbaseUtil.notNull(clamAVScanStreamEvent, "clamAVScanStreamEvent");

        this.clamAVRequestEventTypeRef = new AtomicReference<>(clamAVRequestEventType);
        this.clamAVPingEvent = clamAVPingEvent;
        this.clamAVScanPathEvent = clamAVScanPathEvent;
        this.clamAVScanStreamEvent = clamAVScanStreamEvent;
    }

    @Override
    public void init(final Class<? extends ClamAVRequestEventType> clamAVRequestEventType) {
        AppbaseUtil.notNull(clamAVRequestEventType, "clamAVRequestEventType");

        this.clamAVRequestEventTypeRef.set(clamAVRequestEventType);
    }

    @Override
    public Class<? extends ClamAVRequestEventType> getClamAVRequestEventType() {
        return clamAVRequestEventTypeRef.get();
    }

    @Override
    public ClamAVPingEvent getClamAVPingEvent() {
        return clamAVPingEvent;
    }

    @Override
    public ClamAVScanPathEvent getClamAVScanPathEvent() {
        return clamAVScanPathEvent;
    }

    @Override
    public ClamAVScanStreamEvent getClamAVScanStreamEvent() {
        return clamAVScanStreamEvent;
    }

    @Override
    public boolean isValid() {
        final Class<? extends ClamAVRequestEventType> clamAVRequestEventType = clamAVRequestEventTypeRef.get();
        if (clamAVRequestEventType == null) {
            return false;
        }

        if (clamAVRequestEventType.isAssignableFrom(ClamAVPingEvent.class)) {
            return getClamAVPingEvent().isValid();
        } else if (clamAVRequestEventType.isAssignableFrom(ClamAVScanPathEvent.class)) {
            return getClamAVScanPathEvent().isValid();
        } else if (clamAVRequestEventType.isAssignableFrom(ClamAVScanStreamEvent.class)) {
            return getClamAVScanStreamEvent().isValid();
        }

        return false;
    }

    @Override
    public boolean isClean() {
        return super.isClean() && clamAVRequestEventTypeRef.get() == null;
    }

    @Override
    public void clean() {
        this.clamAVRequestEventTypeRef.set(null);
        this.clamAVPingEvent.reset();
        this.clamAVScanPathEvent.reset();
        this.clamAVScanStreamEvent.reset();

        super.clean();
    }

    @Override
    public String toString() {
        return "DefaultClamAVRequestEvent{" +
                "clamAVRequestEventTypeRef=" + clamAVRequestEventTypeRef.get() +
                ", clamAVPingEvent=" + clamAVPingEvent +
                ", clamAVScanPathEvent=" + clamAVScanPathEvent +
                ", clamAVScanStreamEvent=" + clamAVScanStreamEvent +
                "} " + super.toString();
    }

    /**
     * {@link ObjectFactory} for {@link ClamAVRequestEvent}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public static class Factory implements ObjectFactory<ClamAVRequestEvent> {
        @Override
        public ClamAVRequestEvent newObject() {
            return new DefaultClamAVRequestEvent();
        }
    }
}
