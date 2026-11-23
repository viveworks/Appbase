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

package com.viveworks.appbase.client.cloud.aws.s3.accessor;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link S3AccessorException}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class S3AccessorExceptionTest {

    @Test
    void mapsBareS3ExceptionToS3AccessorException() {
        final S3Exception s3Exception = (S3Exception) S3Exception.builder().message("boom").build();
        final Throwable mapped = S3AccessorException.map(
                s3Exception, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(S3AccessorException.class, mapped);
        assertSame(s3Exception, mapped.getCause());
    }

    @Test
    void mapsBareNoSuchKeyExceptionToS3AccessorException() {
        final NoSuchKeyException noSuchKeyException =
                (NoSuchKeyException) NoSuchKeyException.builder().message("missing").build();
        final Throwable mapped = S3AccessorException.map(
                noSuchKeyException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(S3AccessorException.class, mapped);
        assertSame(noSuchKeyException, mapped.getCause());
    }

    @Test
    void unwrapsCompletionExceptionWrappingSdkException() {
        final AwsServiceException sdkException = (AwsServiceException) AwsServiceException.builder().message("x").build();
        final CompletionException completionException = new CompletionException(sdkException);
        final Throwable mapped = S3AccessorException.map(
                completionException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(S3AccessorException.class, mapped);
        assertSame(sdkException, mapped.getCause());
    }

    @Test
    void unwrapsExecutionExceptionWrappingSdkException() {
        final AwsServiceException sdkException = (AwsServiceException) AwsServiceException.builder().message("x").build();
        final ExecutionException executionException = new ExecutionException(sdkException);
        final Throwable mapped = S3AccessorException.map(
                executionException, throwable -> new IllegalStateException("fallback-not-used"));
        assertInstanceOf(S3AccessorException.class, mapped);
        assertSame(sdkException, mapped.getCause());
    }

    @Test
    void delegatesNonSdkThrowableToFallbackWithUnwrappedCause() {
        final RuntimeException foreign = new RuntimeException("x");
        final CompletionException completionException = new CompletionException(foreign);
        final IllegalStateException fallbackResult = new IllegalStateException("fallback");
        final Throwable mapped = S3AccessorException.map(
                completionException, throwable -> {
                    assertSame(foreign, throwable);
                    return fallbackResult;
                });
        assertSame(fallbackResult, mapped);
    }
}
