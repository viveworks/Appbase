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

import com.viveworks.appbase.client.service.clamav.client.ClamAVClient;
import com.viveworks.appbase.core.service.mediator.event.MediatorEventType;
import com.viveworks.appbase.core.util.Validatable;
import com.viveworks.appbase.core.util.handler.AppbaseHandlerRequest;

/**
 * Represents a {@link MediatorEventType} used by {@link ClamAVClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ClamAVRequestEvent extends MediatorEventType, Validatable, AppbaseHandlerRequest {
    void init(Class<? extends ClamAVRequestEventType> clamAVRequestEventType);

    Class<? extends ClamAVRequestEventType> getClamAVRequestEventType();

    ClamAVPingEvent getClamAVPingEvent();

    ClamAVScanPathEvent getClamAVScanPathEvent();

    ClamAVScanStreamEvent getClamAVScanStreamEvent();
}
