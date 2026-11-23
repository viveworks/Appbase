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
package com.viveworks.appbase.client.cloud.aws.s3;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultS3Clients implements S3Clients {
    private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    /**
     * Creates a DefaultS3Clients using the supplied configuration and collaborators.
     */
    public DefaultS3Clients(final S3Client s3Client, final S3AsyncClient s3AsyncClient) {
        AppbaseUtil.notNull(s3Client, "s3Client");
        AppbaseUtil.notNull(s3AsyncClient, "s3AsyncClient");

        this.s3Client = s3Client;
        this.s3AsyncClient = s3AsyncClient;
    }

    @Override
    public S3Client getS3Client() {
        return s3Client;
    }

    @Override
    public S3AsyncClient getS3AsyncClient() {
        return s3AsyncClient;
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }
        try {
            s3Client.close();
        } finally {
            s3AsyncClient.close();
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }
}
