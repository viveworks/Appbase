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

package com.viveworks.appbase.client.service.clamav.client.result;

import com.viveworks.appbase.core.util.objectpool.ObjectFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ClamAVPingResult}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVPingResult implements ClamAVPingResult {
    private final AtomicReference<Boolean> pingSuccessRef;

    public DefaultClamAVPingResult() {
        pingSuccessRef = new AtomicReference<>();
    }

    public DefaultClamAVPingResult(final boolean pingSuccess) {
        this();
        init(pingSuccess);
    }

    @Override
    public void init(final boolean pingSuccess) {
        this.pingSuccessRef.set(pingSuccess);
    }

    @Override
    public boolean isPingSuccess() {
        return pingSuccessRef.get();
    }

    @Override
    public boolean isValid() {
        return pingSuccessRef.get() != null;
    }

    @Override
    public boolean isClean() {
        return pingSuccessRef.get() == null;
    }

    @Override
    public void clean() {
        pingSuccessRef.set(null);
    }

    @Override
    public String toString() {
        return "DefaultClamAVPingResult{" +
                "pingSuccess=" + pingSuccessRef.get() +
                "} ";
    }



    /**
     * {@link ObjectFactory} for {@link ClamAVPingResult}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public static class Factory implements ObjectFactory<ClamAVPingResult> {
        @Override
        public ClamAVPingResult newObject() {
            return new DefaultClamAVPingResult();
        }
    }
}
