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

package com.viveworks.appbase.core.service.mediator.event.delegator;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.service.mediator.event.CustomEvent;
import com.viveworks.appbase.core.service.mediator.event.MediatorEvent;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Validatable;
import com.viveworks.appbase.core.util.handler.AppbaseHandlerRequest;
import org.eclipse.microprofile.metrics.Counter;
import org.slf4j.Logger;

/**
 * Abstract implementation of {@link MediatorEventListener} that handles custom event of type {@link AppbaseHandlerRequest}.
 *
 * @author Navaneeth Mysore G
 * @param <V> {@link AppbaseHandlerRequest}
 * @since 1.0.0
 */
public abstract class AbstractMediatorCustomEventListener<V extends Validatable>
        implements MediatorEventListener, Shutdownable
{
    protected final AppbaseCore appbaseCore;
    protected final Logger logger;
    protected final Counter nullCustomEventErrorCounter;
    protected final Counter nullEventErrorCounter;
    protected final Counter eventNotFoundErrorCounter;
    protected final Counter invalidEventErrorCounter;

    public AbstractMediatorCustomEventListener(final AppbaseCore appbaseCore) {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        this.appbaseCore = appbaseCore;
        this.logger = appbaseCore
                .getLogService()
                .getLogger(getClass());

        final MetricService metricService = appbaseCore.getMetricService();
        this.nullCustomEventErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "nullCustomEvent", "error", "counter"));
        this.nullEventErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "nullEvent", "error", "counter"));
        this.eventNotFoundErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "eventNotFound", "error", "counter"));
        this.invalidEventErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "invalidEvent", "error", "counter"));
    }

    protected abstract Class<V> getEventType();

    protected abstract void handleCustomEvent(V event);


    @Override
    @SuppressWarnings("unchecked")
    public void onMediatorEvent(final MediatorEvent mediatorEvent) {
        AppbaseUtil.notNull(mediatorEvent, "mediatorEvent");

        final CustomEvent customEvent = mediatorEvent.getCustomEvent();

        if (customEvent == null) {
            logger.warn("No custom event found in {}", mediatorEvent);
            nullCustomEventErrorCounter.inc();
            return;
        }

        final Object event = customEvent.getEvent();
        if (event == null) {
            logger.warn("No event found in custom event {}", mediatorEvent);
            nullEventErrorCounter.inc();
            return;
        }

        final Class<V> eventType = getEventType();
        if (!eventType.isAssignableFrom(event.getClass())) {
            logger.warn("Event is not of type: " + eventType);
            eventNotFoundErrorCounter.inc();
            return;
        }

        final V request = (V) event;
        if (!request.isValid()) {
            logger.warn("Invalid event: {}", event);
            invalidEventErrorCounter.inc();
            return;
        }

        handleCustomEvent(request);
    }
}
