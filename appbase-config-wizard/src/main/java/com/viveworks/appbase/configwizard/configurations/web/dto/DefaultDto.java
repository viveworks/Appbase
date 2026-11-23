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

package com.viveworks.appbase.configwizard.configurations.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Nullable;

/**
 * Response body for {@code GET /api/configs/{fqcn}/default}.
 *
 * <p>Contains the JSON-serialized default instance of the requested configuration class.
 * When the class has no constructable default (e.g. all fields required), {@code values} is
 * {@code null}.
 *
 * @param values a JSON-compatible object map (typically a {@code Map<String, Object>}) representing
 *               the default instance, or {@code null} when a default cannot be constructed
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Schema(description = "Response body for GET /{fqcn}/default. "
        + "Contains the serialized default instance of the requested configuration class as a flat JSON map.")
public record DefaultDto(
        @Schema(description = "JSON-compatible map of property names to their default values, "
                + "as produced by the class's default (zero-argument) constructor. "
                + "Null when the class cannot produce a default instance (e.g. all fields required).",
                nullable = true,
                example = "{\"bootstrapServers\":\"localhost:9092\",\"groupId\":\"appbase-default\",\"autoOffsetReset\":\"earliest\"}")
        @Nullable Object values
) {
}
