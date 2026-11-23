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

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ClamAVScanPathEvent}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVScanPathEvent implements ClamAVScanPathEvent {
    private final AtomicReference<Path> pathRef;
    private final AtomicReference<ClamAVScanResultHandler> clamAVScanResultHandlerRef;

    public DefaultClamAVScanPathEvent() {
        this.pathRef = new AtomicReference<>();
        this.clamAVScanResultHandlerRef = new AtomicReference<>();
    }

    public DefaultClamAVScanPathEvent(final Path path, final ClamAVScanResultHandler clamAVScanResultHandler) {
        this();
        init(path, clamAVScanResultHandler);
    }

    @Override
    public void init(final Path path, final ClamAVScanResultHandler clamAVScanResultHandler) {
        AppbaseUtil.notNull(path, "path");
        AppbaseUtil.notNull(clamAVScanResultHandler, "clamAVScanResultHandler");

        this.pathRef.set(path);
        this.clamAVScanResultHandlerRef.set(clamAVScanResultHandler);
    }

    @Override
    public Path getPath() {
        return pathRef.get();
    }

    @Override
    public ClamAVScanResultHandler getClamAVScanResultHandler() {
        return clamAVScanResultHandlerRef.get();
    }

    @Override
    public boolean isValid() {
        return pathRef.get() != null && clamAVScanResultHandlerRef.get() != null;
    }

    @Override
    public void reset() {
        this.pathRef.set(null);
        this.clamAVScanResultHandlerRef.set(null);
    }

    @Override
    public String toString() {
        final ClamAVScanResultHandler clamAVScanResultHandler = clamAVScanResultHandlerRef.get();

        return "DefaultClamAVScanEvent{" +
                "pathRef=" + pathRef +
                ", clamAVScanResultHandler=" +
                (clamAVScanResultHandler != null ? clamAVScanResultHandler.getClass().getName() : null) +
                '}';
    }
}
