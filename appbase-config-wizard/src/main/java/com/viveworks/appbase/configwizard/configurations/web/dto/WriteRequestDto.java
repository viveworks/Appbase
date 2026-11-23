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
import java.util.Map;

/**
 * Request body for {@code POST /api/configurations/{fqcn}/write}.
 *
 * <p>Carries both the field values used to build the configuration object and an optional
 * explicit output path. When {@code outputPath} is {@code null}, the YAML file is written
 * to a temp-file location chosen by {@link com.viveworks.appbase.configwizard.configurations.writer.YamlWriter}.
 *
 * @param fieldValues  map of Jackson property names to values; may be empty to use all defaults
 * @param outputPath   optional absolute file-system path for the output file;
 *                     {@code null} to use a generated temp path
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Schema(description = "Request body for POST /{fqcn}/write. "
        + "Carries the field values used to build the configuration object and an optional explicit output path.")
public record WriteRequestDto(
        @Schema(description = "Map of Jackson property names to their values. "
                + "Omitted keys fall back to the class default. Pass an empty object {} to use all defaults.",
                example = "{\"bootstrapServers\":\"kafka.prod.internal:9092\",\"groupId\":\"my-consumer-group\"}")
        Map<String, Object> fieldValues,

        @Schema(description = "Optional absolute file-system path for the output YAML file. "
                + "When null, the server writes to a generated temporary path and returns it in the response.",
                nullable = true,
                example = "/home/dev/configs/kafka.yaml")
        @Nullable String outputPath
) {
}
