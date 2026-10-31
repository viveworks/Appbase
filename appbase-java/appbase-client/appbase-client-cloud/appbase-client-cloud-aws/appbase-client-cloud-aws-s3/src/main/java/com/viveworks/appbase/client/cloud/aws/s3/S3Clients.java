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
package com.viveworks.appbase.client.cloud.aws.s3;

import com.viveworks.appbase.core.service.Shutdownable;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Holds the synchronous and asynchronous AWS SDK clients created for one S3 integration.
 * Closing the owning service shuts down both clients.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface S3Clients extends Shutdownable {
    /**
     * Returns the synchronous AWS SDK S3 client.
     *
     * @return synchronous S3 client
     */
    S3Client getS3Client();

    /**
     * Returns the asynchronous AWS SDK S3 client.
     *
     * @return asynchronous S3 client
     */
    S3AsyncClient getS3AsyncClient();
}
