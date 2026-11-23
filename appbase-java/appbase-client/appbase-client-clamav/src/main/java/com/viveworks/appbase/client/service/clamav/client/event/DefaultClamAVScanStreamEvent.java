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

import com.viveworks.appbase.client.service.clamav.client.handler.ClamAVScanResultHandler;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ClamAVScanStreamEvent}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVScanStreamEvent implements ClamAVScanStreamEvent {
    private final AtomicReference<InputStream> inputStreamRef;
    private final AtomicReference<Boolean> closeInputStreamRef;
    private final AtomicReference<ClamAVScanResultHandler> clamAVScanResultHandlerRef;

    public DefaultClamAVScanStreamEvent() {
        this.inputStreamRef = new AtomicReference<>();
        this.closeInputStreamRef = new AtomicReference<>();
        this.clamAVScanResultHandlerRef = new AtomicReference<>();
    }

    public DefaultClamAVScanStreamEvent(
            final InputStream inputStream,
            final boolean closeInputStream,
            final ClamAVScanResultHandler clamAVScanResultHandler)
    {
        this();
        init(inputStream, closeInputStream, clamAVScanResultHandler);
    }

    @Override
    public void init(
            final InputStream inputStream,
            final boolean closeInputStream,
            final ClamAVScanResultHandler clamAVScanResultHandler)
    {
        AppbaseUtil.notNull(inputStream, "inputStream");
        AppbaseUtil.notNull(clamAVScanResultHandler, "clamAVScanResultHandler");

        this.inputStreamRef.set(inputStream);
        this.closeInputStreamRef.set(closeInputStream);
        this.clamAVScanResultHandlerRef.set(clamAVScanResultHandler);
    }

    @Override
    public InputStream getInputStream() {
        return inputStreamRef.get();
    }

    @Override
    public boolean closeInputStream() {
        return closeInputStreamRef.get();
    }

    @Override
    public ClamAVScanResultHandler getClamAVScanResultHandler() {
        return clamAVScanResultHandlerRef.get();
    }

    @Override
    public boolean isValid() {
        return inputStreamRef.get() != null && closeInputStreamRef.get() != null && clamAVScanResultHandlerRef.get() != null;
    }

    @Override
    public void reset() {
        this.inputStreamRef.set(null);
        this.closeInputStreamRef.set(null);
        this.clamAVScanResultHandlerRef.set(null);
    }

    @Override
    public String toString() {
        final ClamAVScanResultHandler clamAVScanResultHandler = clamAVScanResultHandlerRef.get();

        return "DefaultClamAVScanStreamEvent{" +
                "inputStreamRef=" + inputStreamRef +
                ", closeInputStream=" + closeInputStreamRef.get() +
                ", clamAVScanStreamResultHandlerRef=" +
                (clamAVScanResultHandler != null ? clamAVScanResultHandler.getClass().getName() : null) +
                '}';
    }
}
