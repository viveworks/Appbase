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

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientRuntimeException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.junit.jupiter.api.Assertions;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class S3ClientServiceTestUtil {
    private S3ClientServiceTestUtil() {
    }

    public static void s3ClientServiceTest(
            final S3ClientsService s3ClientsService,
            final S3ClientsIntegrationService s3ClientsIntegrationService) throws Exception
    {
        s3ClientServiceTestImpl(s3ClientsService, s3ClientsIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        s3ClientsIntegrationService.shutdown();

        s3ClientsIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
            @Override
            public void onSuccess() {
                repaired.setReference(true);
            }

            @Override
            public void onFailure() {
                repaired.setReference(false);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                throw new AppbaseRuntimeException(throwable);
            }
        });
        Assertions.assertTrue(repaired.getReference());

        s3ClientServiceTestImpl(s3ClientsService, s3ClientsIntegrationService);
        s3ClientsService.shutdown();

        Assertions.assertNull(s3ClientsIntegrationService.getClient());
        Assertions.assertFalse(s3ClientsIntegrationService.isHealthy());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void s3ClientServiceTestImpl(
            final S3ClientsService s3ClientsService,
            final S3ClientsIntegrationService s3ClientsIntegrationService) throws Exception
    {
        Assertions.assertNotNull(s3ClientsService);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = s3ClientsService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(s3ClientsIntegrationService);

        final S3Clients s3Clients = s3ClientsIntegrationService.getClient();
        Assertions.assertNotNull(s3Clients);

        testS3Client(s3Clients.getS3Client());
        testS3AsyncClient(s3Clients.getS3AsyncClient());

        Assertions.assertTrue(s3ClientsIntegrationService.isHealthy());
        Assertions.assertTrue(s3ClientsIntegrationService.isRepairEnabled());
        Assertions.assertEquals(S3Clients.class, s3ClientsIntegrationService.getClientType());
    }

    private static void testS3Client(final S3Client s3Client) throws Exception {
        Assertions.assertNotNull(s3Client);

        final String bucketName = "appbase-unit-test-" + UUID.randomUUID();
        try {
            final ListBucketsResponse listBucketsResponse1 = s3Client.listBuckets();
            Assertions.assertNotNull(listBucketsResponse1);

            final CreateBucketResponse createBucketResponse = s3Client
                    .createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            Assertions.assertNotNull(createBucketResponse);
            Assertions.assertNotNull(createBucketResponse.location());

            final ListBucketsResponse listBucketsResponse2 = s3Client.listBuckets();
            assertBucketFound(listBucketsResponse2, bucketName);
        } finally {
            final DeleteBucketResponse deleteBucketResponse = s3Client
                    .deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
            Assertions.assertNotNull(deleteBucketResponse);
        }
    }

    private static void testS3AsyncClient(final S3AsyncClient s3AsyncClient) throws Exception {
        Assertions.assertNotNull(s3AsyncClient);

        final String bucketName = "appbase-unit-test-" + UUID.randomUUID();
        try {
            final ListBucketsResponse listBucketsResponse1 = s3AsyncClient
                    .listBuckets()
                    .get(30, TimeUnit.SECONDS);
            Assertions.assertNotNull(listBucketsResponse1);


            final CreateBucketResponse createBucketResponse = s3AsyncClient
                    .createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
                    .get(30, TimeUnit.SECONDS);
            Assertions.assertNotNull(createBucketResponse);
            Assertions.assertNotNull(createBucketResponse.location());


            final ListBucketsResponse listBucketsResponse2 = s3AsyncClient
                    .listBuckets()
                    .get(30, TimeUnit.SECONDS);
            assertBucketFound(listBucketsResponse2, bucketName);
        } catch (Exception ex) {
            throw new AppbaseClientRuntimeException(ex);
        } finally {
            final DeleteBucketResponse deleteBucketResponse = s3AsyncClient
                    .deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build())
                    .get(30, TimeUnit.SECONDS);
            Assertions.assertNotNull(deleteBucketResponse);
        }
    }

    private static void assertBucketFound(final ListBucketsResponse listBucketsResponse, final String bucketName) {
        Assertions.assertNotNull(listBucketsResponse);

        final List<Bucket> buckets = listBucketsResponse.buckets();
        boolean foundBucket = false;
        for (final Bucket bucket : buckets) {
            if (bucket.name().contains(bucketName)) {
                foundBucket = true;
                break;
            }
        }
        Assertions.assertTrue(foundBucket);
    }
}
