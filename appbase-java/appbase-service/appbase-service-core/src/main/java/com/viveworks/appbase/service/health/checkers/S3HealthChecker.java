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
package com.viveworks.appbase.service.health.checkers;

import com.viveworks.appbase.client.cloud.aws.s3.S3Clients;
import com.viveworks.appbase.client.cloud.aws.s3.S3ClientsIntegrationService;
import com.viveworks.appbase.client.cloud.aws.s3.S3ClientsService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.health.HealthCheck;
import com.viveworks.appbase.service.health.HealthChecker;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class S3HealthChecker implements HealthChecker {
    private final S3ClientsIntegrationService s3ClientsIntegrationService;
    private final String bucketName;

    public S3HealthChecker(
            final S3ClientsIntegrationService s3ClientsIntegrationService,
            final String bucketName)
    {
        AppbaseUtil.notNull(s3ClientsIntegrationService, "s3ClientsIntegrationService");
        AppbaseUtil.notBlank(bucketName, "bucketName");

        this.s3ClientsIntegrationService = s3ClientsIntegrationService;
        this.bucketName = bucketName;
    }

    @Override
    public HealthCheck runHealthCheck() {
        final S3Clients s3Clients = s3ClientsIntegrationService.getClient();
        final S3Client s3Client = s3Clients.getS3Client();

        boolean success = false;
        try {
            final HeadBucketRequest headBucketRequest = HeadBucketRequest
                    .builder()
                    .bucket(bucketName)
                    .build();
            final HeadBucketResponse headBucketResponse = s3Client.headBucket(headBucketRequest);
            if (headBucketResponse.sdkHttpResponse().isSuccessful()) {
                success = true;
            }
        } catch (NoSuchBucketException nsbe) {
            // no-op, success is already false
        }

        return success ?
                HealthCheck.success(S3ClientsService.class.getName()) :
                HealthCheck.failed(S3ClientsService.class.getName());
    }
}
