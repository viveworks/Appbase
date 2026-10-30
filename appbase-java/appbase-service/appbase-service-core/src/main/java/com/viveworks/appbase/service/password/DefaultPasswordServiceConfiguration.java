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

package com.viveworks.appbase.service.password;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.viveworks.appbase.core.service.AppbaseConfiguration;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Configuration for {@link DefaultPasswordService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize
public class DefaultPasswordServiceConfiguration implements AppbaseConfiguration {
    public static final int DEFAULT_MEMORY_KIB = 19456;
    public static final int DEFAULT_ITERATIONS = 2;
    public static final int DEFAULT_PARALLELISM = 1;
    public static final int DEFAULT_HASH_LENGTH = 32;

    private final int memoryKiB;
    private final int iterations;
    private final int parallelism;
    private final int hashLength;

    /** Creates configuration with OWASP 2023 Argon2id defaults. */
    public DefaultPasswordServiceConfiguration() {
        this(null, null, null, null);
    }

    @JsonCreator
    public DefaultPasswordServiceConfiguration(
            @JsonProperty("memoryKiB") @Nullable final Integer memoryKiB,
            @JsonProperty("iterations") @Nullable final Integer iterations,
            @JsonProperty("parallelism") @Nullable final Integer parallelism,
            @JsonProperty("hashLength") @Nullable final Integer hashLength)
    {
        this.memoryKiB = memoryKiB != null ? memoryKiB : DEFAULT_MEMORY_KIB;
        this.iterations = iterations != null ? iterations : DEFAULT_ITERATIONS;
        this.parallelism = parallelism != null ? parallelism : DEFAULT_PARALLELISM;
        this.hashLength = hashLength != null ? hashLength : DEFAULT_HASH_LENGTH;
    }

    /** @return Argon2id memory cost in KiB (m parameter). */
    public int getMemoryKiB() {
        return memoryKiB;
    }

    /** @return Argon2id time cost / iteration count (t parameter). */
    public int getIterations() {
        return iterations;
    }

    /** @return Argon2id parallelism degree (p parameter). */
    public int getParallelism() {
        return parallelism;
    }

    /** @return Argon2id output hash length in bytes. */
    public int getHashLength() {
        return hashLength;
    }
}
