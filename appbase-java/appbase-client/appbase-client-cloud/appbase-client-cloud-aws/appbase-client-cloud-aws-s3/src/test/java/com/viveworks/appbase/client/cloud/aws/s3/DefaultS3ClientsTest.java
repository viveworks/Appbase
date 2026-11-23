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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DefaultS3ClientsTest {
    @Mock
    private S3Client s3Client;
    @Mock
    private S3AsyncClient s3AsyncClient;

    @Test
    void testConstructorNullS3Client() {
        assertThrows(Exception.class, () -> new DefaultS3Clients(null, s3AsyncClient));
    }

    @Test
    void testConstructorNullS3AsyncClient() {
        assertThrows(Exception.class, () -> new DefaultS3Clients(s3Client, null));
    }

    @Test
    void testGetters() {
        final DefaultS3Clients clients = new DefaultS3Clients(s3Client, s3AsyncClient);
        assertSame(s3Client, clients.getS3Client());
        assertSame(s3AsyncClient, clients.getS3AsyncClient());
    }

    @Test
    void testIsShutdownInitiallyFalse() {
        final DefaultS3Clients clients = new DefaultS3Clients(s3Client, s3AsyncClient);
        assertFalse(clients.isShutdown());
    }

    @Test
    void testShutdownClosesBothClientsAndSetsFlag() {
        final DefaultS3Clients clients = new DefaultS3Clients(s3Client, s3AsyncClient);
        clients.shutdown();
        verify(s3Client).close();
        verify(s3AsyncClient).close();
        assertTrue(clients.isShutdown());
    }

    @Test
    void testShutdownIsIdempotent() {
        final DefaultS3Clients clients = new DefaultS3Clients(s3Client, s3AsyncClient);
        clients.shutdown();
        clients.shutdown();
        verify(s3Client).close();
        verify(s3AsyncClient).close();
    }
}
