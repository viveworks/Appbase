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

package com.viveworks.appbase.configwizard.configurations.scan;

import javax.annotation.Nullable;

/**
 * Metadata for a discovered Appbase configuration class.
 *
 * <p>{@code layer} is derived from the top-level package segment after
 * {@code com.viveworks.appbase.} — e.g. "client", "service", "core".
 *
 * <p>{@code module} is the technology segment — e.g. "aerospike", "kafka", "redis".
 *
 * <p>{@code defaultInstance} is the object produced by instantiating the builder
 * (no-arg) and calling {@code build()} — or for {@code @JsonCreator} patterns, a
 * Jackson-constructed instance with all defaults. Null if construction fails.
 *
 * @param fqcn            fully qualified class name, e.g.
 *                        {@code com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientIntegrationServiceConfiguration}
 * @param simpleName      unqualified class name, e.g.
 *                        {@code DefaultRedisClientIntegrationServiceConfiguration}
 * @param layer           top-level layer segment, e.g. "client", "core", "service"
 * @param module          technology segment, e.g. "redis", "kafka", "aerospike"
 * @param configClass     the loaded {@link Class} object
 * @param defaultInstance a default-constructed instance; {@code null} if construction fails
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public record ConfigClassInfo(
        String fqcn,
        String simpleName,
        String layer,
        String module,
        Class<?> configClass,
        @Nullable Object defaultInstance
) {
}
