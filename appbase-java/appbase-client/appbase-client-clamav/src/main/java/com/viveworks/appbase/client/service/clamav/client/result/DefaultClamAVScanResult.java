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
 * Default implementation of {@link ClamAVScanResult}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVScanResult implements ClamAVScanResult {
    private final AtomicReference<Boolean> virusFoundRef;

    public DefaultClamAVScanResult() {
        this.virusFoundRef = new AtomicReference<>();
    }

    public DefaultClamAVScanResult(final boolean virusFound) {
        this();

        init(virusFound);
    }

    @Override
    public void init(final boolean virusFound) {
        this.virusFoundRef.set(virusFound);
    }

    @Override
    public boolean isVirusFound() {
        return virusFoundRef.get();
    }

    @Override
    public boolean isValid() {
        return virusFoundRef.get() != null;
    }

    @Override
    public boolean isClean() {
        return virusFoundRef.get() == null;
    }

    @Override
    public void clean() {
        virusFoundRef.set(null);
    }

    @Override
    public String toString() {
        return "DefaultClamAVScanResult{" +
                "virusFound=" + virusFoundRef.get() +
                "} ";
    }



    /**
     * {@link ObjectFactory} for {@link ClamAVScanResult}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public static class Factory implements ObjectFactory<ClamAVScanResult> {
        @Override
        public ClamAVScanResult newObject() {
            return new DefaultClamAVScanResult();
        }
    }
}
