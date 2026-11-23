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

package com.viveworks.appbase.service.web.endpoint.servlet;

import javax.annotation.Nullable;
import java.io.InputStream;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class FileDownloadInfo {
    @Nullable
    private final String filename;

    @Nullable
    private final String mimeType;

    @Nullable
    private final String traceId;

    @Nullable
    private final InputStream inputStream;

    public FileDownloadInfo(
            @Nullable
            final String filename,
            @Nullable
            final String mimeType,
            @Nullable
            final String traceId,
            @Nullable
            final InputStream inputStream)
    {
        this.filename = filename;
        this.mimeType = mimeType;
        this.traceId = traceId;
        this.inputStream = inputStream;
    }

    @Nullable
    public String getFilename() {
        return filename;
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    @Nullable
    public String getTraceId() {
        return traceId;
    }

    @Nullable
    public InputStream getInputStream() {
        return inputStream;
    }
}
