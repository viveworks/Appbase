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

import com.viveworks.appbase.client.cloud.aws.s3.S3ClientsService;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractObjectStoreAccessor implements ObjectStoreAccessor {
    protected final Logger logger;
    protected final S3ClientsService s3ClientsService;
    protected final String bucketName;
    protected final String readDestinationBasePathname;

    private final Timer readObjectAsInputStreamTimer;
    private final Timer readObjectAsFileTimer;
    private final Timer readObjectAsBytesTimer;
    private final Timer readObjectAsByteBufferTimer;
    private final Timer writeObjectPathTimer;
    private final Timer writeObjectBytesTimer;
    private final Timer writeObjectByteBufferTimer;
    private final Timer deleteObjectTimer;
    private final Timer createBaseDirectoryTimer;
    private final Timer existsBaseDirectoryTimer;
    private final Timer deleteBaseDirectoryTimer;

    private final Timer asyncReadObjectAsInputStreamTimer;
    private final Timer asyncReadObjectAsFileTimer;
    private final Timer asyncReadObjectAsBytesTimer;
    private final Timer asyncReadObjectAsByteBufferTimer;
    private final Timer asyncWriteObjectPathTimer;
    private final Timer asyncWriteObjectBytesTimer;
    private final Timer asyncWriteObjectByteBufferTimer;
    private final Timer asyncDeleteObjectTimer;
    private final Timer asyncCreateBaseDirectoryTimer;
    private final Timer asyncExistsBaseDirectoryTimer;
    private final Timer asyncDeleteBaseDirectoryTimer;

    protected AbstractObjectStoreAccessor(
            final AppbaseCore appbaseCore,
            final S3ClientsService s3ClientsService,
            final String bucketName,
            final String readDestinationBasePathname)
    {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(s3ClientsService, "s3ClientsService");
        AppbaseUtil.notBlank(bucketName, "bucketName");
        AppbaseUtil.notBlank(readDestinationBasePathname, "readDestinationBasePathname");

        this.logger = appbaseCore
                .getLogService()
                .getLogger(getClass());
        this.s3ClientsService = s3ClientsService;
        this.bucketName = bucketName.trim().toLowerCase();
        this.readDestinationBasePathname = readDestinationBasePathname;

        final MetricService metricService = appbaseCore.getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.readObjectAsInputStreamTimer =
                metricRegistry.timer(metricService.name(getClass(), "readObjectAsInputStreamTimer"));
        this.readObjectAsFileTimer =
                metricRegistry.timer(metricService.name(getClass(), "readObjectAsFileTimer"));
        this.readObjectAsBytesTimer =
                metricRegistry.timer(metricService.name(getClass(), "readObjectAsBytesTimer"));
        this.readObjectAsByteBufferTimer =
                metricRegistry.timer(metricService.name(getClass(), "readObjectAsByteBufferTimer"));
        this.writeObjectPathTimer =
                metricRegistry.timer(metricService.name(getClass(), "writeObjectPathTimer"));
        this.writeObjectBytesTimer =
                metricRegistry.timer(metricService.name(getClass(), "writeObjectBytesTimer"));
        this.writeObjectByteBufferTimer =
                metricRegistry.timer(metricService.name(getClass(), "writeObjectByteBufferTimer"));
        this.deleteObjectTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteObjectTimer"));
        this.createBaseDirectoryTimer =
                metricRegistry.timer(metricService.name(getClass(), "createBaseDirectoryTimer"));
        this.existsBaseDirectoryTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsBaseDirectoryTimer"));
        this.deleteBaseDirectoryTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteBaseDirectoryTimer"));
        this.asyncReadObjectAsInputStreamTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncReadObjectAsInputStreamTimer"));
        this.asyncReadObjectAsFileTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncReadObjectAsFileTimer"));
        this.asyncReadObjectAsBytesTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncReadObjectAsBytesTimer"));
        this.asyncReadObjectAsByteBufferTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncReadObjectAsByteBufferTimer"));
        this.asyncWriteObjectPathTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncWriteObjectPathTimer"));
        this.asyncWriteObjectBytesTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncWriteObjectBytesTimer"));
        this.asyncWriteObjectByteBufferTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncWriteObjectByteBufferTimer"));
        this.asyncDeleteObjectTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncDeleteObjectTimer"));
        this.asyncCreateBaseDirectoryTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncCreateBaseDirectoryTimer"));
        this.asyncExistsBaseDirectoryTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncExistsBaseDirectoryTimer"));
        this.asyncDeleteBaseDirectoryTimer =
                metricRegistry.timer(metricService.name(getClass(), "asyncDeleteBaseDirectoryTimer"));
    }

    @Override
    public InputStream readObjectAsInputStream(final String objectId) throws IOException {
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = readObjectAsInputStreamTimer.time();
        try {
            final GetObjectRequest getObjectRequest = getObjectRequest(objectId);
            return s3Client().getObject(getObjectRequest);
        } finally {
            timer.stop();
        }
    }

    @Override
    public Path readObjectAsFile(final String objectId) throws IOException {
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = readObjectAsFileTimer.time();
        try {
            final Path destinationPath = readFileDestinationPath(objectId);
            final GetObjectRequest getObjectRequest = getObjectRequest(objectId);

            s3Client().getObject(getObjectRequest, destinationPath);
            return destinationPath;
        } finally {
            timer.stop();
        }
    }

    @Override
    public byte[] readObjectAsBytes(String objectId) throws IOException {
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = readObjectAsBytesTimer.time();
        try {
            final GetObjectRequest getObjectRequest = getObjectRequest(objectId);
            return s3Client().getObjectAsBytes(getObjectRequest).asByteArray();
        } finally {
            timer.stop();
        }
    }

    @Override
    public ByteBuffer readObjectAsByteBuffer(String objectId) throws IOException {
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = readObjectAsByteBufferTimer.time();
        try {
            final GetObjectRequest getObjectRequest = getObjectRequest(objectId);
            return s3Client().getObjectAsBytes(getObjectRequest).asByteBuffer();
        } finally {
            timer.stop();
        }
    }

    @Override
    public String writeObject(final Path sourcePath, final String objectId) throws IOException {
        AppbaseUtil.notNull(sourcePath, "sourcePath");
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = writeObjectPathTimer.time();
        try {
            return writeObject(objectId, RequestBody.fromFile(sourcePath));
        } finally {
            timer.stop();
        }
    }

    @Override
    public String writeObject(final byte[] bytes, final String objectId) throws IOException {
        AppbaseUtil.notNull(bytes, "bytes");
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = writeObjectBytesTimer.time();
        try {
            return writeObject(objectId, RequestBody.fromBytes(bytes));
        } finally {
            timer.stop();
        }
    }

    @Override
    public String writeObject(ByteBuffer byteBuffer, String objectId) throws IOException {
        AppbaseUtil.notNull(byteBuffer, "byteBuffer");
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = writeObjectByteBufferTimer.time();
        try {
            return writeObject(objectId, RequestBody.fromByteBuffer(byteBuffer));
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteObject(final String objectId) throws IOException {
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = deleteObjectTimer.time();
        try {
            final DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest
                    .builder()
                    .bucket(bucketName())
                    .key(key(objectId))
                    .build();
            s3Client().deleteObject(deleteObjectRequest);
            return true;
        } finally {
            timer.stop();
        }
    }

    @Override
    public String createBaseDirectory() throws IOException {
        final Timer.Context timer = createBaseDirectoryTimer.time();
        try {
            final String key = key(null);
            final PutObjectRequest putObjectRequest = PutObjectRequest
                    .builder()
                    .bucket(bucketName())
                    .key(key)
                    .build();
            logger.debug("PutObjectRequest={}", putObjectRequest);

            s3Client().putObject(putObjectRequest, RequestBody.fromBytes(new byte[0]));
            return key;
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean existsBaseDirectory() throws IOException {
        final Timer.Context timer = existsBaseDirectoryTimer.time();
        try {
            final GetObjectRequest getObjectRequest = getObjectRequest(null);
            logger.debug("GetObjectRequest={}", getObjectRequest);

            try {
                s3Client().getObject(getObjectRequest, ResponseTransformer.toBytes());
            } catch (NoSuchKeyException noSuchKeyException) {
                return false;
            }

            return true;
        } finally {
            timer.stop();
        }
    }

    @Override
    public CompletableFuture<InputStream> asyncReadObjectAsInputStream(final String objectId) {
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = asyncReadObjectAsInputStreamTimer.time();
        final CompletableFuture<InputStream> result = new CompletableFuture<>();

        final CompletableFuture<ResponseBytes<GetObjectResponse>> clientFuture = asyncReadObject(objectId);
        clientFuture.whenComplete((responseBytes, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
                return;
            }

            result.complete(responseBytes.asInputStream());
        });

        return result.whenComplete((inputStream, throwable) -> {
            timer.stop();
        });
    }

    @Override
    public CompletableFuture<Path> asyncReadObjectAsFile(final String objectId) {
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = asyncReadObjectAsFileTimer.time();
        final CompletableFuture<Path> result = new CompletableFuture<>();

        final Path destinationPath = readFileDestinationPath(objectId);
        final GetObjectRequest getObjectRequest = getObjectRequest(objectId);
        logger.debug("GetObjectRequest as file: {}", getObjectRequest);
        final CompletableFuture<GetObjectResponse> clientFuture = s3AsyncClient()
                .getObject(getObjectRequest, AsyncResponseTransformer.toFile(destinationPath));
        clientFuture.whenComplete((getObjectResponse, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
                return;
            }

            result.complete(destinationPath);
        });

        return result.whenComplete((path, throwable) -> {
            timer.stop();
        });
    }

    @Override
    public CompletableFuture<byte[]> asyncReadObjectAsBytes(final String objectId) {
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = asyncReadObjectAsBytesTimer.time();
        final CompletableFuture<byte[]> result = new CompletableFuture<>();

        final CompletableFuture<ResponseBytes<GetObjectResponse>> clientFuture = asyncReadObject(objectId);
        clientFuture.whenComplete((responseBytes, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
                return;
            }

            result.complete(responseBytes.asByteArray());
        });

        return result.whenComplete((bytes, throwable) -> {
            timer.stop();
        });
    }

    @Override
    public CompletableFuture<ByteBuffer> asyncReadObjectAsByteBuffer(final String objectId) {
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = asyncReadObjectAsByteBufferTimer.time();
        final CompletableFuture<ByteBuffer> result = new CompletableFuture<>();

        final CompletableFuture<ResponseBytes<GetObjectResponse>> clientFuture = asyncReadObject(objectId);
        clientFuture.whenComplete((responseBytes, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
                return;
            }

            result.complete(responseBytes.asByteBuffer());
        });

        return result.whenComplete((byteBuffer, throwable) -> {
            timer.stop();
        });
    }

    @Override
    public CompletableFuture<String> asyncWriteObject(final Path sourcePath, final String objectId) {
        AppbaseUtil.notNull(sourcePath, "sourcePath");
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = asyncWriteObjectPathTimer.time();
        return asyncWriteObject(objectId, AsyncRequestBody.fromFile(sourcePath))
                .whenComplete((s, throwable) -> timer.stop());
    }

    @Override
    public CompletableFuture<String> asyncWriteObject(final byte[] bytes, final String objectId) {
        AppbaseUtil.notNull(bytes, "bytes");
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = asyncWriteObjectBytesTimer.time();
        return asyncWriteObject(objectId, AsyncRequestBody.fromBytes(bytes))
                .whenComplete((s, throwable) -> timer.stop());
    }

    @Override
    public CompletableFuture<String> asyncWriteObject(final ByteBuffer byteBuffer, final String objectId) {
        AppbaseUtil.notNull(byteBuffer, "byteBuffer");
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = asyncWriteObjectByteBufferTimer.time();
        return asyncWriteObject(objectId, AsyncRequestBody.fromByteBuffer(byteBuffer))
                .whenComplete((s, throwable) -> timer.stop());
    }

    @Override
    public CompletableFuture<Boolean> asyncDeleteObject(final String objectId) {
        AppbaseUtil.notBlank(objectId, "objectId");

        final Timer.Context timer = asyncDeleteObjectTimer.time();
        final DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest
                .builder()
                .bucket(bucketName())
                .key(key(objectId))
                .build();

        return asyncDeleteObject(deleteObjectRequest)
                .whenComplete((aBoolean, throwable) -> timer.stop());
    }

    @Override
    public CompletableFuture<String> asyncCreateBaseDirectory() {
        final Timer.Context timer = asyncCreateBaseDirectoryTimer.time();
        return asyncWriteObject(null, AsyncRequestBody.fromBytes(new byte[0]))
                .whenComplete((s, throwable) -> timer.stop());
    }

    @Override
    public CompletableFuture<Boolean> asyncExistsBaseDirectory() {
        final Timer.Context timer = asyncExistsBaseDirectoryTimer.time();
        final CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        final CompletableFuture<ResponseBytes<GetObjectResponse>> clientFuture = asyncReadObject(null);
        clientFuture.whenComplete((responseBytes, throwable) -> {
            if (throwable != null) {
                if (NoSuchKeyException.class.isAssignableFrom(throwable.getClass())) {
                    resultFuture.complete(false);
                    return;
                }

                resultFuture.completeExceptionally(throwable);
                return;
            }

            resultFuture.complete(true);
        });

        return resultFuture.whenComplete((aBoolean, throwable) -> timer.stop());
    }

    @Override
    public CompletableFuture<Boolean> asyncDeleteBaseDirectory() {
        final Timer.Context timer = asyncDeleteBaseDirectoryTimer.time();
        final String baseDirectory = key(null);
        logger.debug("Deleting base directory={}", baseDirectory);

        final DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest
                .builder()
                .bucket(bucketName())
                .key(baseDirectory)
                .build();

        return asyncDeleteObject(deleteObjectRequest)
                .whenComplete((aBoolean, throwable) -> timer.stop());
    }

    @Override
    public boolean deleteBaseDirectory() throws IOException {
        final Timer.Context timer = deleteBaseDirectoryTimer.time();
        try {
            final String baseDirectory = key(null);
            logger.debug("Deleting base directory={}", baseDirectory);

            final DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest
                    .builder()
                    .bucket(bucketName())
                    .key(baseDirectory)
                    .build();

            try {
                s3Client().deleteObject(deleteObjectRequest);
            } catch (Exception ex) {
                return false;
            }

            return true;
        } finally {
            timer.stop();
        }
    }

    @Override
    public String key(@Nullable final String objectId) {
        String finalKeyPrefix;

        final String keyPrefix = keyPrefix().trim();
        if (!keyPrefix.startsWith(ObjectStoreAccessor.S3_BUCKET_SEPARATOR)) {
            finalKeyPrefix = ObjectStoreAccessor.S3_BUCKET_SEPARATOR + keyPrefix;
        } else {
            finalKeyPrefix = keyPrefix;
        }

        if (!finalKeyPrefix.endsWith(ObjectStoreAccessor.S3_BUCKET_SEPARATOR)) {
            finalKeyPrefix = finalKeyPrefix.trim() + ObjectStoreAccessor.S3_BUCKET_SEPARATOR;
        }

        return ((objectId == null) ? finalKeyPrefix : (finalKeyPrefix + objectId)).trim().toLowerCase();
    }

    protected S3AsyncClient s3AsyncClient() {
        return s3ClientsService.getClient().getS3AsyncClient();
    }

    protected S3Client s3Client() {
        return s3ClientsService.getClient().getS3Client();
    }

    protected String bucketName() {
        return bucketName;
    }

    protected String writeObject(final String objectId, final RequestBody requestBody) {
        AppbaseUtil.notBlank(objectId, "objectId");
        AppbaseUtil.notNull(requestBody, "requestBody");

        final String key = key(objectId);
        final PutObjectRequest putObjectRequest = PutObjectRequest
                .builder()
                .bucket(bucketName())
                .key(key)
                .build();
        logger.debug("PutObjectRequest={}", putObjectRequest);

        s3Client().putObject(putObjectRequest, requestBody);

        return key;
    }

    protected CompletableFuture<ResponseBytes<GetObjectResponse>> asyncReadObject(@Nullable final String objectId) {
        final GetObjectRequest getObjectRequest = getObjectRequest(objectId);
        logger.debug("GetObjectRequest={}", getObjectRequest);
        return s3AsyncClient().getObject(getObjectRequest, AsyncResponseTransformer.toBytes());
    }

    protected CompletableFuture<String> asyncWriteObject(final String objectId, final AsyncRequestBody asyncRequestBody) {
        final CompletableFuture<String> resultFuture = new CompletableFuture<>();

        final String key = key(objectId);
        final PutObjectRequest putObjectRequest = PutObjectRequest
                .builder()
                .bucket(bucketName())
                .key(key)
                .build();
        logger.debug("PutObjectRequest={}", putObjectRequest);

        final CompletableFuture<PutObjectResponse> clientFuture = s3AsyncClient()
                .putObject(putObjectRequest, asyncRequestBody);
        clientFuture.whenComplete((putObjectResponse, throwable) -> {
            if (throwable != null) {
                resultFuture.completeExceptionally(throwable);
                return;
            }

            resultFuture.complete(key);
        });

        return resultFuture;
    }

    protected CompletableFuture<Boolean> asyncDeleteObject(final DeleteObjectRequest deleteObjectRequest) {
        final CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        logger.debug("DeleteObjectRequest={}", deleteObjectRequest);

        final CompletableFuture<DeleteObjectResponse> clientFuture = s3AsyncClient().deleteObject(deleteObjectRequest);

        clientFuture.whenComplete((deleteObjectResponse, throwable) -> {
            if (throwable != null) {
                resultFuture.completeExceptionally(throwable);
                return;
            }

            resultFuture.complete(true);
        });

        return resultFuture;
    }

    protected Path readFileDestinationPath(final String objectId) {
        AppbaseUtil.notBlank(objectId, "objectId");

        // AUD-33 (CWE-22 family): joining objectId in with no canonicalisation or confinement
        // check let a traversal-shaped id (e.g. "../escaped.bin") resolve OUTSIDE
        // readDestinationBasePathname. No in-repo caller reaches this public accessor today, but
        // it is a public library API, not dead code. Resolve against the canonical root,
        // normalize, and require the result to remain a descendant of that root.
        final Path basePath = Path.of(readDestinationBasePathname).toAbsolutePath().normalize();
        final Path destinationPath = basePath
                .resolve(readDestinationSubDirectoryName())
                .resolve(objectId)
                .normalize();

        if (!destinationPath.startsWith(basePath)) {
            throw new AppbaseRuntimeException(
                    "objectId must resolve to a destination beneath " + basePath + ": " + objectId);
        }

        return destinationPath;
    }

    protected GetObjectRequest getObjectRequest(final String objectId) {
        return GetObjectRequest
                .builder()
                .bucket(bucketName())
                .key(key(objectId))
                .build();
    }
}
