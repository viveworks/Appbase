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

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Provides synchronous and asynchronous object operations within a configured base directory.
 * Object identifiers are resolved beneath {@link #keyPrefix()}; a {@code null} identifier passed to
 * {@link #key(String)} denotes the prefix itself.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ObjectStoreAccessor {
    String S3_BUCKET_SEPARATOR = "/";

    /** @param objectId identifier resolved beneath the accessor's key prefix
     * @return object contents as a stream that the caller must close
     * @throws IOException if the object cannot be read */
    InputStream readObjectAsInputStream(String objectId) throws IOException;

    /** @param objectId identifier resolved beneath the accessor's key prefix
     * @return path of the local file containing the downloaded object
     * @throws IOException if the object cannot be downloaded */
    Path readObjectAsFile(String objectId) throws IOException;

    /** @param objectId identifier resolved beneath the accessor's key prefix
     * @return object contents
     * @throws IOException if the object cannot be read */
    byte[] readObjectAsBytes(String objectId) throws IOException;

    /** @param objectId identifier resolved beneath the accessor's key prefix
     * @return object contents as a byte buffer
     * @throws IOException if the object cannot be read */
    ByteBuffer readObjectAsByteBuffer(String objectId) throws IOException;

    /** @param sourcePath path of the local file to upload
     * @param objectId identifier resolved beneath the accessor's key prefix
     * @return the resolved object key
     * @throws IOException if the source cannot be read or uploaded */
    String writeObject(Path sourcePath, String objectId) throws IOException;

    /** @param bytes object contents to upload
     * @param objectId identifier resolved beneath the accessor's key prefix
     * @return the resolved object key
     * @throws IOException if the bytes cannot be uploaded */
    String writeObject(byte[] bytes, String objectId) throws IOException;

    /** @param byteBuffer object contents to upload; the AWS SDK copies the entire buffer, ignores its
     * current position, and leaves the original position unchanged
     * @param objectId identifier resolved beneath the accessor's key prefix
     * @return the resolved object key
     * @throws IOException if the buffer cannot be uploaded */
    String writeObject(ByteBuffer byteBuffer, String objectId) throws IOException;

    /** @param objectId identifier resolved beneath the accessor's key prefix
     * @return whether the object was deleted
     * @throws IOException if deletion fails */
    boolean deleteObject(String objectId) throws IOException;

    /** @return the created base-directory key
     * @throws IOException if the directory marker cannot be created */
    String createBaseDirectory() throws IOException;

    /** @return whether the base directory exists
     * @throws IOException if its state cannot be read */
    boolean existsBaseDirectory() throws IOException;

    /** @return whether the base directory was deleted
     * @throws IOException if deletion fails */
    boolean deleteBaseDirectory() throws IOException;


    /** @param objectId identifier resolved beneath the accessor's key prefix
     * @return future containing the object stream; read failures complete it exceptionally */
    CompletableFuture<InputStream> asyncReadObjectAsInputStream(String objectId);

    /** @param objectId identifier resolved beneath the accessor's key prefix
     * @return future containing the downloaded local path; download failures complete it exceptionally */
    CompletableFuture<Path> asyncReadObjectAsFile(String objectId);

    /** @param objectId identifier resolved beneath the accessor's key prefix
     * @return future containing the object bytes; read failures complete it exceptionally */
    CompletableFuture<byte[]> asyncReadObjectAsBytes(String objectId);

    /** @param objectId identifier resolved beneath the accessor's key prefix
     * @return future containing the object byte buffer; read failures complete it exceptionally */
    CompletableFuture<ByteBuffer> asyncReadObjectAsByteBuffer(String objectId);

    /** @param sourcePath path of the local file to upload
     * @param objectId identifier resolved beneath the accessor's key prefix
     * @return future containing the resolved object key; read or upload failures complete it exceptionally */
    CompletableFuture<String> asyncWriteObject(Path sourcePath, String objectId);

    /** @param bytes object contents to upload
     * @param objectId identifier resolved beneath the accessor's key prefix
     * @return future containing the resolved object key; upload failures complete it exceptionally */
    CompletableFuture<String> asyncWriteObject(byte[] bytes, String objectId);

    /** @param byteBuffer object contents to upload; the AWS SDK copies the entire buffer, ignores its
     * current position, and leaves the original position unchanged
     * @param objectId identifier resolved beneath the accessor's key prefix
     * @return future containing the resolved object key; upload failures complete it exceptionally */
    CompletableFuture<String> asyncWriteObject(ByteBuffer byteBuffer, String objectId);

    /** @param objectId identifier resolved beneath the accessor's key prefix
     * @return future indicating whether the object was deleted; deletion failures complete it exceptionally */
    CompletableFuture<Boolean> asyncDeleteObject(String objectId);

    /** @return future containing the created base-directory key; creation failures complete it exceptionally */
    CompletableFuture<String> asyncCreateBaseDirectory();

    /** @return future indicating whether the base directory exists; lookup failures complete it exceptionally */
    CompletableFuture<Boolean> asyncExistsBaseDirectory();

    /** @return future indicating whether the base directory was deleted; deletion failures complete it exceptionally */
    CompletableFuture<Boolean> asyncDeleteBaseDirectory();


    /** @return local subdirectory used for file-download destinations */
    String readDestinationSubDirectoryName();

    /** @return configured key prefix for this accessor */
    String keyPrefix();

    /**
     * Resolves an object identifier beneath {@link #keyPrefix()}.
     *
     * @param objectId object identifier, or {@code null} for the prefix itself
     * @return resolved S3 object key
     */
    String key(@Nullable final String objectId);
}
