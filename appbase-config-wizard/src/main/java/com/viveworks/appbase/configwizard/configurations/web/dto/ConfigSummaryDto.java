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

package com.viveworks.appbase.configwizard.configurations.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Summary view of a discovered Appbase configuration class.
 *
 * <p>Returned by {@code GET /api/configs} to provide a lightweight listing of all
 * scanned configuration classes without per-field detail.
 *
 * @param fqcn       fully qualified class name (e.g.
 *                   {@code com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientIntegrationServiceConfiguration})
 * @param simpleName unqualified class name (e.g. {@code DefaultRedisClientIntegrationServiceConfiguration})
 * @param layer      top-level layer segment derived from the package (e.g. "client", "core", "service")
 * @param module     technology segment derived from the package (e.g. "redis", "kafka", "aerospike")
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Schema(description = "Summary view of a discovered Appbase configuration class. "
        + "Used in the list response to give a lightweight overview before the client fetches full field detail.")
public record ConfigSummaryDto(
        @Schema(description = "Fully qualified class name",
                example = "com.viveworks.appbase.client.service.kafka.configuration.DefaultKafkaClientConfiguration")
        String fqcn,

        @Schema(description = "Unqualified class name, suitable for display in a UI list",
                example = "DefaultKafkaClientConfiguration")
        String simpleName,

        @Schema(description = "Top-level architectural layer derived from the package hierarchy",
                example = "client",
                allowableValues = {"core", "client", "service"})
        String layer,

        @Schema(description = "Technology module name derived from the package hierarchy",
                example = "kafka")
        String module
) {
}
