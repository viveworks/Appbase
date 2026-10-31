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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Detail view of a discovered Appbase configuration class, including all introspected fields.
 *
 * <p>Returned by {@code GET /api/configs/{fqcn}} to provide the full field list and
 * the serialized default instance for use by the web UI.
 *
 * @param fqcn          fully qualified class name
 * @param fields        ordered list of fields introspected via Jackson; never {@code null}, may be empty
 * @param defaultValues serialized representation of the default instance (a JSON-compatible object
 *                      map), or {@code null} when no default instance could be constructed
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Schema(description = "Full detail view of a discovered Appbase configuration class. "
        + "Contains the ordered list of configurable fields and the class's default instance "
        + "as a JSON map that the UI uses to pre-populate form fields.")
public record ConfigDetailDto(
        @Schema(description = "Fully qualified class name of this configuration class",
                example = "com.viveworks.appbase.client.service.kafka.configuration.DefaultKafkaClientConfiguration")
        String fqcn,

        @Schema(description = "Ordered list of configurable fields, each describing its type, "
                + "whether it is required, its default value, and whether it is itself a nested configuration object")
        List<FieldSummaryDto> fields,

        @Schema(description = "JSON-compatible map of property names to their default values, "
                + "taken from the class's default instance. Null when the class has no zero-argument "
                + "constructor and a default instance cannot be created.",
                nullable = true,
                example = "{\"bootstrapServers\":\"localhost:9092\",\"groupId\":\"appbase-default\"}")
        @Nullable Object defaultValues
) {
}
